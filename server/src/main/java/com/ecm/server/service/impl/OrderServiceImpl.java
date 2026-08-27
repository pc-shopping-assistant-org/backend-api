package com.ecm.server.service.impl;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.*;
import com.ecm.server.dto.response.DiscountValidationResponse;
import com.ecm.server.dto.response.OrderDetailResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.OrderMapper;
import com.ecm.server.model.*;
import com.ecm.server.repository.*;
import com.ecm.server.service.CartService;
import com.ecm.server.service.DiscountService;
import com.ecm.server.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String POLICY_DENY = "DENY";
    public static final int DEFAULT_LIMIT = 20;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CustomerRepository customerRepository;
    private final DiscountRepository discountRepository;
    private final PaymentRepository paymentRepository;
    private final DiscountService discountService;
    private final CartService cartService;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderDetailResponse createOrder(UUID accountId, CreateOrderRequest request) {
        // 1. Retrieve customer profile from authenticated account context
        Customer customer = customerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BusinessException(StatusCode.CUSTOMER_NOT_FOUND, "Customer profile not found"));

        // 2. Validate product variants and check inventory stock
        long subtotalAmount = 0L;
        List<OrderItemValidateDto> validateItems = new ArrayList<>();
        List<ProductVariant> variantsToUpdate = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            ProductVariant variant = productVariantRepository.findById(itemReq.getProductVariantId())
                    .filter(v -> STATUS_ACTIVE.equalsIgnoreCase(v.getStatus()))
                    .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_VARIANT_NOT_FOUND));

            if (POLICY_DENY.equalsIgnoreCase(variant.getInventoryPolicy()) && variant.getQuantity() < itemReq.getQuantity()) {
                throw new BusinessException(StatusCode.INSUFFICIENT_STOCK, "Insufficient stock for SKU: " + variant.getSku());
            }

            int unitPrice = (variant.getPriceSale() != null && variant.getPriceSale() > 0) ? variant.getPriceSale() : variant.getPrice();
            subtotalAmount += ((long) unitPrice * itemReq.getQuantity());

            validateItems.add(OrderItemValidateDto.builder()
                    .productVariantId(variant.getId())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .build());

            variantsToUpdate.add(variant);
        }

        // 3. Process and validate discount code if provided
        Discount discount = null;
        int totalDiscountAmount = 0;
        if (request.getDiscountCode() != null && !request.getDiscountCode().isBlank()) {
            DiscountValidationResponse discountRes = discountService.validateDiscount(ValidateDiscountRequest.builder()
                    .code(request.getDiscountCode())
                    .orderAmount(subtotalAmount)
                    .items(validateItems)
                    .build());

            if (Boolean.TRUE.equals(discountRes.getIsValid())) {
                discount = discountRepository.findById(discountRes.getDiscountId()).orElse(null);
                totalDiscountAmount = discountRes.getDiscountAmount();
            }
        }

        // 4. Compute delivery fee and final total order amount
        int shipAmount = 0;
        long finalTotalAmount = Math.max(0L, subtotalAmount + shipAmount - totalDiscountAmount);

        // 5. Persist Order entity
        Order order = Order.builder()
                .customer(customer)
                .discount(discount)
                .totalAmount(finalTotalAmount)
                .shipAmount(shipAmount)
                .discountAmount(totalDiscountAmount)
                .note(request.getNote())
                .deliveryAddress(request.getDeliveryAddress())
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .status(STATUS_PENDING)
                .build();
        Order savedOrder = orderRepository.save(order);

        // 6. Persist OrderItem records and deduct variant stock
        Set<OrderItem> savedOrderItems = new LinkedHashSet<>();
        for (int i = 0; i < request.getItems().size(); i++) {
            OrderItemRequest itemReq = request.getItems().get(i);
            ProductVariant variant = variantsToUpdate.get(i);
            int unitPrice = (variant.getPriceSale() != null && variant.getPriceSale() > 0) ? variant.getPriceSale() : variant.getPrice();

            // Calculate item-level discount share proportionally if applied
            int itemDiscount = 0;
            if (totalDiscountAmount > 0 && subtotalAmount > 0) {
                long lineSubtotal = (long) unitPrice * itemReq.getQuantity();
                itemDiscount = (int) Math.round((lineSubtotal * totalDiscountAmount) / (double) subtotalAmount);
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .productVariant(variant)
                    .quantity(itemReq.getQuantity())
                    .unitAmount(unitPrice)
                    .discount(discount)
                    .discountAmount(itemDiscount)
                    .status(STATUS_ACTIVE)
                    .build();
            savedOrderItems.add(orderItemRepository.save(orderItem));

            // Deduct stock quantity
            variant.setQuantity(Math.max(0, variant.getQuantity() - itemReq.getQuantity()));
            productVariantRepository.save(variant);

            // Remove ordered item from Redis cart
            cartService.removeCartItem(accountId, variant.getId());
        }
        savedOrder.setOrderItems(savedOrderItems);

        // 7. Initialize Payment record
        Payment payment = Payment.builder()
                .order(savedOrder)
                .method(request.getPaymentMethod().toUpperCase())
                .status(STATUS_PENDING)
                .build();
        Payment savedPayment = paymentRepository.save(payment);
        savedOrder.setPayments(new LinkedHashSet<>(List.of(savedPayment)));

        log.info("Created order [{}] with total [{}] for customer [{}]", savedOrder.getId(), finalTotalAmount, customer.getId());

        // 8. Return populated OrderDetailResponse
        return orderMapper.toDetailResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<OrderDetailResponse> getMyOrders(UUID accountId, OrderFilterRequest filter) {
        // 1. Resolve customer profile
        Customer customer = customerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BusinessException(StatusCode.CUSTOMER_NOT_FOUND));

        // 2. Query customer orders using keyset cursor pagination
        int pageSize = (filter.getLimit() != null && filter.getLimit() > 0) ? filter.getLimit() : DEFAULT_LIMIT;
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        String statusFilter = (filter.getStatus() != null && !filter.getStatus().isBlank()) ? filter.getStatus().trim().toUpperCase() : null;

        List<Order> orders = (filter.getCursor() == null)
                ? orderRepository.findCustomerOrdersInitial(customer.getId(), statusFilter, pageable)
                : orderRepository.findCustomerOrdersAfterCursor(customer.getId(), filter.getCursor(), statusFilter, pageable);

        // 3. Assemble and return cursor response envelope
        return CursorPageResponse.of(
                orders,
                pageSize,
                order -> order.getId().toString(),
                order -> {
                    var items = orderItemRepository.findByOrderIdWithDetails(order.getId());
                    order.setOrderItems(new LinkedHashSet<>(items));
                    var payments = paymentRepository.findByOrderId(order.getId());
                    order.setPayments(new LinkedHashSet<>(payments));
                    return orderMapper.toDetailResponse(order);
                }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderById(UUID accountId, UUID orderId) {
        // 1. Fetch order with customer details
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new BusinessException(StatusCode.ORDER_NOT_FOUND));

        // 2. Enforce authorization ownership
        if (order.getCustomer() == null || order.getCustomer().getAccount() == null
                || !accountId.equals(order.getCustomer().getAccount().getId())) {
            throw new BusinessException(StatusCode.FORBIDDEN, "You do not have permission to view this order");
        }

        // 3. Eager load order items and payments
        List<OrderItem> items = orderItemRepository.findByOrderIdWithDetails(orderId);
        order.setOrderItems(new LinkedHashSet<>(items));
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        order.setPayments(new LinkedHashSet<>(payments));

        // 4. Map and return detail DTO
        return orderMapper.toDetailResponse(order);
    }

    @Override
    @Transactional
    public OrderDetailResponse cancelOrder(UUID accountId, UUID orderId, CancelOrderRequest request) {
        // 1. Retrieve order and verify ownership
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new BusinessException(StatusCode.ORDER_NOT_FOUND));

        if (order.getCustomer() == null || order.getCustomer().getAccount() == null
                || !accountId.equals(order.getCustomer().getAccount().getId())) {
            throw new BusinessException(StatusCode.FORBIDDEN, "You do not have permission to cancel this order");
        }

        // 2. Validate state machine transition (only PENDING orders can be cancelled by customer)
        if (!STATUS_PENDING.equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException(StatusCode.INVALID_ORDER_STATE_TRANSITION, "Only PENDING orders can be cancelled. Current status is " + order.getStatus());
        }

        // 3. Revert inventory stock for all order items
        List<OrderItem> items = orderItemRepository.findByOrderIdWithDetails(orderId);
        for (OrderItem item : items) {
            ProductVariant variant = item.getProductVariant();
            if (variant != null) {
                variant.setQuantity(variant.getQuantity() + item.getQuantity());
                productVariantRepository.save(variant);
            }
            item.setStatus(STATUS_CANCELLED);
            orderItemRepository.save(item);
        }
        order.setOrderItems(new LinkedHashSet<>(items));

        // 4. Update order status to CANCELLED
        order.setStatus(STATUS_CANCELLED);
        if (request != null && request.getReason() != null && !request.getReason().isBlank()) {
            order.setNote((order.getNote() != null ? order.getNote() + " | " : "") + "Cancelled by customer: " + request.getReason());
        }
        Order savedOrder = orderRepository.save(order);

        log.info("Cancelled order [{}] by customer [{}]", orderId, accountId);

        // 5. Return updated order detail
        return orderMapper.toDetailResponse(savedOrder);
    }
}
