package com.ecm.server.service.impl;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CancelOrderRequest;
import com.ecm.server.dto.request.CreateOrderRequest;
import com.ecm.server.dto.request.OrderFilterRequest;
import com.ecm.server.dto.request.OrderItemRequest;
import com.ecm.server.dto.response.OrderDetailResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.OrderMapper;
import com.ecm.server.model.Cart;
import com.ecm.server.model.CartItem;
import com.ecm.server.model.Customer;
import com.ecm.server.model.CustomerAddress;
import com.ecm.server.model.Discount;
import com.ecm.server.model.Order;
import com.ecm.server.model.OrderItem;
import com.ecm.server.model.Payment;
import com.ecm.server.model.PaymentMethod;
import com.ecm.server.model.ProductVariant;
import com.ecm.server.model.ShippingMethod;
import com.ecm.server.repository.CartRepository;
import com.ecm.server.repository.CustomerAddressRepository;
import com.ecm.server.repository.CustomerRepository;
import com.ecm.server.repository.DiscountCategoryRepository;
import com.ecm.server.repository.DiscountRepository;
import com.ecm.server.repository.DiscountVariantRepository;
import com.ecm.server.repository.OrderItemRepository;
import com.ecm.server.repository.OrderRepository;
import com.ecm.server.repository.PaymentMethodRepository;
import com.ecm.server.repository.PaymentRepository;
import com.ecm.server.repository.ProductVariantRepository;
import com.ecm.server.repository.ShippingMethodRepository;
import com.ecm.server.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    public static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String STATUS_PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_SHIPPING = "SHIPPING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CART_ACTIVE = "ACTIVE";
    public static final String STATUS_CART_CONVERTED = "CONVERTED";
    public static final String STATUS_PAYMENT_PENDING = "PENDING";
    public static final String POLICY_DENY = "DENY";
    public static final int DEFAULT_LIMIT = 20;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final CartRepository cartRepository;
    private final DiscountRepository discountRepository;
    private final DiscountVariantRepository discountVariantRepository;
    private final DiscountCategoryRepository discountCategoryRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ShippingMethodRepository shippingMethodRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderDetailResponse createOrder(UUID accountId, CreateOrderRequest request) {
        Customer customer = customerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BusinessException(StatusCode.CUSTOMER_NOT_FOUND));
        // Lock the active cart for the whole checkout transaction.  The stock
        // row locks prevent overselling, while this cart lock prevents two
        // concurrent requests from converting the same cart twice.
        Cart cart = cartRepository.findActiveByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new BusinessException(StatusCode.BAD_REQUEST, "Active cart is required for checkout"));
        if (cart.getItems().isEmpty()) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Cart cannot be empty");
        }

        // Do not silently discard stale cart lines.  A cart can outlive a
        // product/variant being hidden, but checkout must either be cleaned up
        // explicitly or rejected so the request still represents the complete
        // ACTIVE cart and no item disappears during conversion.
        boolean hasUnavailableItem = cart.getItems().stream()
                .anyMatch(item -> item.getVariant() == null
                        || !STATUS_ACTIVE.equalsIgnoreCase(item.getVariant().getStatus())
                        || item.getVariant().getProduct() == null
                        || !STATUS_ACTIVE.equalsIgnoreCase(item.getVariant().getProduct().getStatus()));
        if (hasUnavailableItem) {
            throw new BusinessException(StatusCode.BAD_REQUEST,
                    "Cart contains an unavailable product; remove it before checkout");
        }

        Map<UUID, Integer> requestedQuantities = requestedQuantities(request.getItems());
        Map<UUID, CartItem> cartItems = new LinkedHashMap<>();
        for (CartItem cartItem : cart.getItems()) {
            if (cartItem.getVariant() != null && STATUS_ACTIVE.equalsIgnoreCase(cartItem.getVariant().getStatus())) {
                cartItems.put(cartItem.getVariant().getId(), cartItem);
            }
        }
        if (!cartItems.keySet().equals(requestedQuantities.keySet())) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Checkout items must match the active cart");
        }

        Instant now = Instant.now();
        List<LineCalculation> lines = new ArrayList<>();
        long subtotal = 0L;
        for (Map.Entry<UUID, Integer> entry : requestedQuantities.entrySet()) {
            CartItem cartItem = cartItems.get(entry.getKey());
            // Re-read the canonical stock row under a database lock. The
            // variant loaded with the cart may be stale when two checkouts run
            // concurrently; locking here prevents overselling.
            ProductVariant variant = productVariantRepository.findActiveByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_VARIANT_NOT_FOUND));
            int quantity = entry.getValue();
            if (quantity != cartItem.getQuantity()) {
                throw new BusinessException(StatusCode.BAD_REQUEST, "Checkout quantity is stale for SKU: " + variant.getSku());
            }
            if (quantity <= 0 || quantity > variant.getQuantity()) {
                throw new BusinessException(StatusCode.INSUFFICIENT_STOCK, "Insufficient stock for SKU: " + variant.getSku());
            }
            long unitPrice = variant.getListPrice();
            long itemGross;
            try {
                itemGross = Math.multiplyExact(unitPrice, (long) quantity);
            } catch (ArithmeticException ex) {
                throw new BusinessException(StatusCode.BAD_REQUEST, "Order line amount is too large");
            }
            Discount itemPromotion = findBestAutomaticPromotion(variant, itemGross, now);
            long itemDiscount = calculateDiscount(itemPromotion, itemGross);
            long itemNet = itemGross - itemDiscount;
            try {
                subtotal = Math.addExact(subtotal, itemNet);
            } catch (ArithmeticException ex) {
                throw new BusinessException(StatusCode.BAD_REQUEST, "Order subtotal is too large");
            }
            lines.add(new LineCalculation(variant, quantity, unitPrice, itemPromotion, itemDiscount));
        }

        Discount orderVoucher = null;
        long orderDiscount = 0L;
        if (request.getDiscountCode() != null && !request.getDiscountCode().isBlank()) {
            orderVoucher = discountRepository.findByCodeIgnoreCase(request.getDiscountCode().trim())
                    .orElseThrow(() -> new BusinessException(StatusCode.DISCOUNT_NOT_FOUND));
            validateOrderVoucher(orderVoucher, subtotal, now);
            orderDiscount = calculateDiscount(orderVoucher, subtotal);
        }

        ShippingMethod shippingMethod = resolveShippingMethod(request.getShippingMethodCode());
        long shippingFee = resolveShippingFee(shippingMethod.getCode());
        long total;
        try {
            total = Math.max(0L, Math.addExact(Math.subtractExact(subtotal, orderDiscount), shippingFee));
        } catch (ArithmeticException ex) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Order total is too large");
        }
        PaymentMethod paymentMethod = resolvePaymentMethod(request.getPaymentMethod());
        boolean onlinePayment = !"COD".equalsIgnoreCase(paymentMethod.getCode());

        CustomerAddress selectedAddress = resolveCustomerAddress(accountId, request.getCustomerAddressId());
        String recipientName = normalizeOrDefault(request.getRecipientName(),
                selectedAddress == null ? null : selectedAddress.getRecipientName());
        String recipientPhone = normalizeOrDefault(request.getRecipientPhone(),
                selectedAddress == null ? null : selectedAddress.getPhone());
        String deliveryAddress = normalizeOrDefault(request.getDeliveryAddress(),
                selectedAddress == null ? null : selectedAddress.getAddressLine());
        if (recipientName == null || recipientPhone == null || deliveryAddress == null) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Recipient and delivery address are required");
        }

        Order order = Order.builder()
                .customer(customer)
                .orderDiscount(orderVoucher)
                .shippingMethod(shippingMethod)
                .subtotalAmount(subtotal)
                // subtotal already contains each line's item promotion. The
                // order snapshot stores only the second discount layer so
                // the canonical total formula remains:
                // subtotal - discount_amount + shipping_fee.
                .discountAmount(orderDiscount)
                .shippingFee(shippingFee)
                .totalAmount(total)
                .note(request.getNote())
                .deliveryAddress(deliveryAddress)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .status(onlinePayment ? STATUS_PENDING_PAYMENT : STATUS_PENDING_CONFIRMATION)
                .build();
        Order savedOrder = orderRepository.save(order);

        Set<OrderItem> orderItems = new LinkedHashSet<>();
        for (LineCalculation line : lines) {
            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .productVariant(line.variant())
                    .quantity(line.quantity())
                    .unitPrice(line.unitPrice())
                    .itemDiscountRelation(line.promotion())
                    .itemDiscount(line.itemDiscount())
                    .status(STATUS_ACTIVE)
                    .build();
            orderItems.add(orderItemRepository.save(orderItem));
            line.variant().setQuantity(line.variant().getQuantity() - line.quantity());
            productVariantRepository.save(line.variant());
        }
        savedOrder.setOrderItems(orderItems);

        Payment payment = Payment.builder()
                .order(savedOrder)
                .paymentMethod(paymentMethod)
                .amount(total)
                .status(STATUS_PAYMENT_PENDING)
                .build();
        Payment savedPayment = paymentRepository.save(payment);
        savedOrder.setPayments(new LinkedHashSet<>(List.of(savedPayment)));

        cart.setStatus(STATUS_CART_CONVERTED);
        cartRepository.save(cart);
        log.info("Created order [{}] status [{}] total [{}] for customer [{}]", savedOrder.getId(), savedOrder.getStatus(), total, accountId);
        return orderMapper.toDetailResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<OrderDetailResponse> getMyOrders(UUID accountId, OrderFilterRequest filter) {
        customerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BusinessException(StatusCode.CUSTOMER_NOT_FOUND));
        int pageSize = filter.getLimit() != null && filter.getLimit() > 0 ? filter.getLimit() : DEFAULT_LIMIT;
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        String status = filter.getStatus() == null || filter.getStatus().isBlank() ? null : filter.getStatus().trim().toUpperCase();

        String keyword = filter.getKeyword() == null || filter.getKeyword().isBlank()
                ? null : filter.getKeyword().trim();
        if (keyword != null) {
            return findMyOrdersByKeyword(accountId, filter, keyword, status, pageSize);
        }

        List<Order> orders = filter.getCursor() == null
                ? orderRepository.findCustomerOrdersInitial(accountId, status, pageable)
                : orderRepository.findCustomerOrdersAfterCursor(accountId, filter.getCursor(), status, pageable);
        return CursorPageResponse.of(orders, pageSize, order -> order.getId().toString(), this::loadDetail);
    }

    /**
     * Invoice identifiers are a presentation read-model (INV- plus the first
     * eight UUID characters), while the order table stores the UUID. Resolve
     * the identifier inside the customer's own order set so a search can
     * never reveal another customer's order.
     */
    private CursorPageResponse<OrderDetailResponse> findMyOrdersByKeyword(
            UUID accountId,
            OrderFilterRequest filter,
            String keyword,
            String status,
            int pageSize
    ) {
        String candidate = keyword.toUpperCase(java.util.Locale.ROOT);
        if (candidate.startsWith("INV-")) {
            candidate = candidate.substring(4);
        }
        // UUID prefixes are compared against the hyphenless database text so
        // both canonical and compact UUID input forms resolve identically.
        candidate = candidate.replace("-", "");
        final String identifier = candidate;

        Pageable pageable = PageRequest.of(0, pageSize + 1);
        List<Order> matches = filter.getCursor() == null
                ? orderRepository.findCustomerOrdersByIdentifierInitial(accountId, identifier, status, pageable)
                : orderRepository.findCustomerOrdersByIdentifierAfterCursor(
                        accountId, identifier, status, filter.getCursor(), pageable);

        return CursorPageResponse.of(matches, pageSize, order -> order.getId().toString(), this::loadDetail);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderById(UUID accountId, UUID orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new BusinessException(StatusCode.ORDER_NOT_FOUND));
        if (order.getCustomer() == null || !accountId.equals(order.getCustomer().getAccountId())) {
            throw new BusinessException(StatusCode.FORBIDDEN, "You do not have permission to view this order");
        }
        return loadDetail(order);
    }

    @Override
    @Transactional
    public OrderDetailResponse cancelOrder(UUID accountId, UUID orderId, CancelOrderRequest request) {
        Order order = orderRepository.findByIdWithDetailsForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(StatusCode.ORDER_NOT_FOUND));
        if (order.getCustomer() == null || !accountId.equals(order.getCustomer().getAccountId())) {
            throw new BusinessException(StatusCode.FORBIDDEN, "You do not have permission to cancel this order");
        }
        if (!STATUS_PENDING_PAYMENT.equalsIgnoreCase(order.getStatus())
                && !STATUS_PENDING_CONFIRMATION.equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException(StatusCode.INVALID_ORDER_STATE_TRANSITION,
                    "Only pending orders can be cancelled. Current status is " + order.getStatus());
        }
        List<OrderItem> items = orderItemRepository.findByOrderIdWithDetails(orderId);
        for (OrderItem item : items) {
            ProductVariant variant = item.getProductVariant();
            if (variant != null && STATUS_ACTIVE.equalsIgnoreCase(item.getStatus())) {
                // The checkout path locks this same stock row before
                // decrementing.  Lock it here before restoring quantity so a
                // cancellation cannot lose a concurrent inventory update.
                ProductVariant lockedVariant = productVariantRepository.findByIdForUpdate(variant.getId())
                        .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_VARIANT_NOT_FOUND));
                lockedVariant.setQuantity(lockedVariant.getQuantity() + item.getQuantity());
                productVariantRepository.save(lockedVariant);
                item.setStatus("CANCELLED");
                orderItemRepository.save(item);
            }
        }
        order.setStatus(STATUS_CANCELLED);
        if (request != null && request.getReason() != null && !request.getReason().isBlank()) {
            order.setNote((order.getNote() == null ? "" : order.getNote() + " | ") + "Cancelled by customer: " + request.getReason().trim());
        }
        order.setOrderItems(new LinkedHashSet<>(items));
        return orderMapper.toDetailResponse(orderRepository.save(order));
    }

    private OrderDetailResponse loadDetail(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderIdWithDetails(order.getId());
        order.setOrderItems(new LinkedHashSet<>(items));
        order.setPayments(new LinkedHashSet<>(paymentRepository.findByOrderId(order.getId())));
        return orderMapper.toDetailResponse(order);
    }

    private Map<UUID, Integer> requestedQuantities(List<OrderItemRequest> requests) {
        Map<UUID, Integer> result = new LinkedHashMap<>();
        for (OrderItemRequest request : requests) {
            if (result.put(request.getProductVariantId(), request.getQuantity()) != null) {
                throw new BusinessException(StatusCode.BAD_REQUEST, "A variant may appear only once in an order");
            }
        }
        return result;
    }

    private Discount findBestAutomaticPromotion(ProductVariant variant, long lineGross, Instant now) {
        Discount best = null;
        for (Discount discount : discountRepository.findActiveAutomatic(now)) {
            if (!isApplicable(discount, variant)) {
                continue;
            }
            if (best == null || calculateDiscount(discount, lineGross) > calculateDiscount(best, lineGross)) {
                best = discount;
            }
        }
        return best;
    }

    private boolean isApplicable(Discount discount, ProductVariant variant) {
        return switch (discount.getApplicationScope()) {
            case "ALL_ITEMS" -> true;
            case "VARIANT" -> discountVariantRepository.findByDiscountIdDiscountId(discount.getId()).stream()
                    .anyMatch(target -> target.getVariant() != null && target.getVariant().getId().equals(variant.getId()));
            case "CATEGORY" -> discountCategoryRepository.findByDiscountIdDiscountId(discount.getId()).stream()
                    .anyMatch(target -> target.getCategory() != null && variant.getProduct() != null
                            && target.getCategory().getId().equals(variant.getProduct().getCategory().getId()));
            default -> false;
        };
    }

    private void validateOrderVoucher(Discount discount, long subtotal, Instant now) {
        if (discount.getCode() == null || !"ORDER".equalsIgnoreCase(discount.getApplicationScope())
                || !"ACTIVE".equalsIgnoreCase(discount.getStatus())
                || now.isBefore(discount.getStartAt()) || now.isAfter(discount.getEndAt())) {
            throw new BusinessException(StatusCode.DISCOUNT_EXPIRED, "Order voucher is not active");
        }
        if (subtotal < discount.getMinOrderAmount()) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Order subtotal does not meet voucher minimum amount");
        }
    }

    private long calculateDiscount(Discount discount, long baseAmount) {
        if (discount == null || baseAmount <= 0) {
            return 0L;
        }
        long value = "PERCENT".equalsIgnoreCase(discount.getDiscountType())
                ? Math.round((double) baseAmount * discount.getValue() / 100.0)
                : discount.getValue();
        return Math.min(Math.max(value, 0L), baseAmount);
    }

    private ShippingMethod resolveShippingMethod(String code) {
        String normalized = code == null || code.isBlank() ? "STANDARD" : code.trim().toUpperCase();
        return shippingMethodRepository.findByCodeIgnoreCaseAndStatus(normalized, "ACTIVE")
                .orElseThrow(() -> new BusinessException(StatusCode.BAD_REQUEST, "Shipping method is not available"));
    }

    private CustomerAddress resolveCustomerAddress(UUID accountId, UUID addressId) {
        if (addressId != null) {
            return customerAddressRepository.findByIdAndCustomerAccountId(addressId, accountId)
                    .orElseThrow(() -> new BusinessException(StatusCode.ADDRESS_NOT_FOUND));
        }
        return customerAddressRepository.findByCustomerAccountIdAndIsDefaultTrue(accountId).orElse(null);
    }

    private PaymentMethod resolvePaymentMethod(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        return paymentMethodRepository.findByCodeIgnoreCaseAndStatus(normalized, "ACTIVE")
                .orElseThrow(() -> new BusinessException(StatusCode.BAD_REQUEST, "Payment method is not available"));
    }

    private long resolveShippingFee(String shippingCode) {
        // Fees are deliberately snapshotted on orders; the method table is the
        // normalized catalogue while this mapping is the initial local tariff.
        return switch (shippingCode.toUpperCase()) {
            case "EXPRESS" -> 30_000L;
            case "SAME_DAY" -> 50_000L;
            default -> 0L;
        };
    }

    private String normalizeOrDefault(String requested, String fallback) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim();
        }
        return fallback == null || fallback.isBlank() ? null : fallback.trim();
    }

    private record LineCalculation(ProductVariant variant, int quantity, long unitPrice, Discount promotion,
                                   long itemDiscount) {
    }
}
