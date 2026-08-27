package com.ecm.server.service.impl;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.OrderFilterRequest;
import com.ecm.server.dto.request.UpdateOrderStatusRequest;
import com.ecm.server.dto.response.InvoiceResponse;
import com.ecm.server.dto.response.OrderDetailResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.OrderMapper;
import com.ecm.server.model.Employee;
import com.ecm.server.model.Order;
import com.ecm.server.model.OrderItem;
import com.ecm.server.model.Payment;
import com.ecm.server.model.ProductVariant;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.repository.OrderItemRepository;
import com.ecm.server.repository.OrderRepository;
import com.ecm.server.repository.PaymentRepository;
import com.ecm.server.repository.ProductVariantRepository;
import com.ecm.server.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRM = "CONFIRM";
    public static final String STATUS_SHIPPING = "SHIPPING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final int DEFAULT_LIMIT = 20;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final PaymentRepository paymentRepository;
    private final EmployeeRepository employeeRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<OrderDetailResponse> getAdminOrders(OrderFilterRequest filter) {
        // 1. Prepare pagination and filtering parameters
        int pageSize = (filter.getLimit() != null && filter.getLimit() > 0) ? filter.getLimit() : DEFAULT_LIMIT;
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        String statusFilter = (filter.getStatus() != null && !filter.getStatus().isBlank()) ? filter.getStatus().trim().toUpperCase() : null;

        // 2. Fetch orders using keyset pagination
        List<Order> orders = (filter.getCursor() == null)
                ? orderRepository.findAdminOrdersInitial(filter.getCustomerId(), statusFilter, filter.getFromDate(), filter.getToDate(), pageable)
                : orderRepository.findAdminOrdersAfterCursor(filter.getCursor(), filter.getCustomerId(), statusFilter, filter.getFromDate(), filter.getToDate(), pageable);

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
    public OrderDetailResponse getAdminOrderById(UUID orderId) {
        // 1. Retrieve order with customer details
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new BusinessException(StatusCode.ORDER_NOT_FOUND));

        // 2. Eager load items and payments
        List<OrderItem> items = orderItemRepository.findByOrderIdWithDetails(orderId);
        order.setOrderItems(new LinkedHashSet<>(items));
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        order.setPayments(new LinkedHashSet<>(payments));

        // 3. Map and return detail DTO
        return orderMapper.toDetailResponse(order);
    }

    @Override
    @Transactional
    public OrderDetailResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request, UUID adminId) {
        // 1. Retrieve order
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new BusinessException(StatusCode.ORDER_NOT_FOUND));

        String currentStatus = order.getStatus().toUpperCase();
        String targetStatus = request.getStatus().toUpperCase();

        // 2. Validate state machine transition logic
        validateStateTransition(currentStatus, targetStatus);

        // 3. Handle stock reversion if transitioned to CANCELLED
        List<OrderItem> items = orderItemRepository.findByOrderIdWithDetails(orderId);
        if (STATUS_CANCELLED.equalsIgnoreCase(targetStatus) && !STATUS_CANCELLED.equalsIgnoreCase(currentStatus)) {
            for (OrderItem item : items) {
                ProductVariant variant = item.getProductVariant();
                if (variant != null) {
                    variant.setQuantity(variant.getQuantity() + item.getQuantity());
                    productVariantRepository.save(variant);
                }
                item.setStatus(STATUS_CANCELLED);
                orderItemRepository.save(item);
            }
        }

        // 4. Update delivery timestamp and payment status if COMPLETED
        if (STATUS_COMPLETED.equalsIgnoreCase(targetStatus)) {
            order.setDeliveredAt(Instant.now());
            List<Payment> payments = paymentRepository.findByOrderId(orderId);
            for (Payment payment : payments) {
                if ("PENDING".equalsIgnoreCase(payment.getStatus())) {
                    payment.setStatus("PAID");
                    payment.setPaidAt(Instant.now());
                    paymentRepository.save(payment);
                }
            }
        }

        // 5. Update audit fields and persist order
        UUID employeeId = resolveEmployeeId(adminId);
        order.setStatus(targetStatus);
        if (request.getReason() != null && !request.getReason().isBlank()) {
            order.setNote((order.getNote() != null ? order.getNote() + " | " : "") + "Status update: " + request.getReason());
        }
        order.setUpdatedBy(employeeId);
        Order savedOrder = orderRepository.save(order);

        savedOrder.setOrderItems(new LinkedHashSet<>(items));
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        savedOrder.setPayments(new LinkedHashSet<>(payments));

        log.info("Updated order [{}] status from [{}] to [{}] by employee [{}]", orderId, currentStatus, targetStatus, employeeId);

        // 6. Return updated order detail
        return orderMapper.toDetailResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getOrderInvoice(UUID orderId) {
        // 1. Fetch completed order
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new BusinessException(StatusCode.ORDER_NOT_FOUND));

        // 2. Load order items and payments
        List<OrderItem> items = orderItemRepository.findByOrderIdWithDetails(orderId);
        order.setOrderItems(new LinkedHashSet<>(items));
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        order.setPayments(new LinkedHashSet<>(payments));

        // 3. Map and return InvoiceResponse DTO
        return orderMapper.toInvoiceResponse(order);
    }

    private void validateStateTransition(String currentStatus, String targetStatus) {
        if (currentStatus.equalsIgnoreCase(targetStatus)) {
            return;
        }

        boolean isValid = switch (currentStatus) {
            case STATUS_PENDING -> STATUS_CONFIRM.equalsIgnoreCase(targetStatus) || STATUS_CANCELLED.equalsIgnoreCase(targetStatus);
            case STATUS_CONFIRM -> STATUS_SHIPPING.equalsIgnoreCase(targetStatus) || STATUS_CANCELLED.equalsIgnoreCase(targetStatus);
            case STATUS_SHIPPING -> STATUS_COMPLETED.equalsIgnoreCase(targetStatus) || STATUS_CANCELLED.equalsIgnoreCase(targetStatus);
            default -> false;
        };

        if (!isValid) {
            throw new BusinessException(
                    StatusCode.INVALID_ORDER_STATE_TRANSITION,
                    "Invalid order status transition from " + currentStatus + " to " + targetStatus
            );
        }
    }

    private UUID resolveEmployeeId(UUID accountId) {
        if (accountId == null) {
            return employeeRepository.findAll().stream().findFirst().map(Employee::getId).orElse(null);
        }
        return employeeRepository.findByAccountId(accountId)
                .map(Employee::getId)
                .or(() -> employeeRepository.findById(accountId).map(Employee::getId))
                .orElseGet(() -> employeeRepository.findAll().stream().findFirst().map(Employee::getId).orElse(null));
    }
}
