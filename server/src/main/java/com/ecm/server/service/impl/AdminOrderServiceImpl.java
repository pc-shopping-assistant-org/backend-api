package com.ecm.server.service.impl;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.OrderFilterRequest;
import com.ecm.server.dto.request.InvoiceFilterRequest;
import com.ecm.server.dto.request.UpdateOrderStatusRequest;
import com.ecm.server.dto.response.InvoiceResponse;
import com.ecm.server.dto.response.OrderDetailResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.OrderMapper;
import com.ecm.server.model.*;
import com.ecm.server.repository.*;
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

    public static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String STATUS_PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_SHIPPING = "SHIPPING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_PAID = "PAID";
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
        Order order = orderRepository.findByIdWithDetailsForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(StatusCode.ORDER_NOT_FOUND));

        String currentStatus = order.getStatus().toUpperCase();
        String targetStatus = request.getStatus().toUpperCase();

        // 2. Validate state machine transition logic
        validateStateTransition(currentStatus, targetStatus);

        // An online order may enter PENDING_CONFIRMATION only after a
        // payment attempt has actually reached PAID.  Without this guard an
        // administrator could bypass the payment state machine by manually
        // changing PENDING_PAYMENT to PENDING_CONFIRMATION.
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        if (STATUS_PENDING_PAYMENT.equalsIgnoreCase(currentStatus)
                && STATUS_PENDING_CONFIRMATION.equalsIgnoreCase(targetStatus)
                && payments.stream().noneMatch(payment -> STATUS_PAID.equalsIgnoreCase(payment.getStatus()))) {
            throw new BusinessException(StatusCode.PAYMENT_FAILED,
                    "A payment attempt must be PAID before order confirmation");
        }

        // 3. Handle stock reversion if transitioned to CANCELLED
        List<OrderItem> items = orderItemRepository.findByOrderIdWithDetails(orderId);
        if (STATUS_CANCELLED.equalsIgnoreCase(targetStatus) && !STATUS_CANCELLED.equalsIgnoreCase(currentStatus)) {
            for (OrderItem item : items) {
                ProductVariant variant = item.getProductVariant();
                if (variant != null) {
                    // Lock the canonical stock row as well as the order row so
                    // cancellation cannot race a checkout of another order
                    // for the same SKU.
                    ProductVariant lockedVariant = productVariantRepository.findByIdForUpdate(variant.getId())
                            .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_VARIANT_NOT_FOUND));
                    lockedVariant.setQuantity(lockedVariant.getQuantity() + item.getQuantity());
                    productVariantRepository.save(lockedVariant);
                }
                item.setStatus(STATUS_CANCELLED);
                orderItemRepository.save(item);
            }
        }

        // 4. Update delivery timestamp and COD payment status if COMPLETED
        if (STATUS_COMPLETED.equalsIgnoreCase(targetStatus)) {
            order.setDeliveredAt(Instant.now());
            for (Payment payment : payments) {
                if ("COD".equalsIgnoreCase(payment.getPaymentMethod() == null ? null : payment.getPaymentMethod().getCode())
                        && "PENDING".equalsIgnoreCase(payment.getStatus())) {
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
        if (!STATUS_COMPLETED.equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException(StatusCode.INVALID_ORDER_STATE_TRANSITION,
                    "An invoice is available only for a completed order");
        }

        // 2. Load order items and payments
        List<OrderItem> items = orderItemRepository.findByOrderIdWithDetails(orderId);
        order.setOrderItems(new LinkedHashSet<>(items));
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        order.setPayments(new LinkedHashSet<>(payments));

        // 3. Map and return InvoiceResponse DTO
        return orderMapper.toInvoiceResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<InvoiceResponse> getInvoices(InvoiceFilterRequest filter) {
        int pageSize = filter.getSanitizedLimit();
        String rawKeyword = filter.getKeyword() == null ? null : filter.getKeyword().trim();
        UUID orderId = null;
        if (rawKeyword != null && !rawKeyword.isBlank()) {
            String candidate = rawKeyword.toUpperCase().startsWith("INV-")
                    ? rawKeyword.substring(4) : rawKeyword;
            try {
                if (candidate.length() == 8) {
                    orderId = orderRepository.findCompletedOrderIdsByUuidPrefix(
                                    candidate, PageRequest.of(0, 1))
                            .stream()
                            .findFirst().orElse(null);
                } else {
                    orderId = UUID.fromString(candidate);
                }
            } catch (IllegalArgumentException ignored) {
                // Search by customer fields below when the keyword is not a UUID.
            }
        }
        String keyword = rawKeyword == null || rawKeyword.isBlank()
                || orderId != null ? null : "%" + rawKeyword.toLowerCase() + "%";
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        UUID cursor = parseCursor(filter.getCursor());
        List<Order> orders = cursor == null
                ? orderRepository.findInvoicesInitial(orderId, keyword, filter.getFromDate(), filter.getToDate(), pageable)
                : orderRepository.findInvoicesAfterCursor(cursor, orderId, keyword,
                filter.getFromDate(), filter.getToDate(), pageable);
        return CursorPageResponse.of(orders, pageSize, order -> order.getId().toString(), order -> {
            order.setOrderItems(new LinkedHashSet<>(orderItemRepository.findByOrderIdWithDetails(order.getId())));
            order.setPayments(new LinkedHashSet<>(paymentRepository.findByOrderId(order.getId())));
            return orderMapper.toInvoiceResponse(order);
        });
    }

    private void validateStateTransition(String currentStatus, String targetStatus) {
        if (currentStatus.equalsIgnoreCase(targetStatus)) {
            return;
        }

        boolean isValid = switch (currentStatus) {
            case STATUS_PENDING_PAYMENT ->
                    STATUS_PENDING_CONFIRMATION.equalsIgnoreCase(targetStatus) || STATUS_CANCELLED.equalsIgnoreCase(targetStatus);
            case STATUS_PENDING_CONFIRMATION ->
                    STATUS_CONFIRMED.equalsIgnoreCase(targetStatus) || STATUS_CANCELLED.equalsIgnoreCase(targetStatus);
            case STATUS_CONFIRMED ->
                    STATUS_SHIPPING.equalsIgnoreCase(targetStatus) || STATUS_CANCELLED.equalsIgnoreCase(targetStatus);
            case STATUS_SHIPPING ->
                    STATUS_COMPLETED.equalsIgnoreCase(targetStatus) || STATUS_CANCELLED.equalsIgnoreCase(targetStatus);
            default -> false;
        };

        if (!isValid) {
            throw new BusinessException(
                    StatusCode.INVALID_ORDER_STATE_TRANSITION,
                    "Invalid order status transition from " + currentStatus + " to " + targetStatus
            );
        }
    }

    private UUID parseCursor(String rawCursor) {
        if (rawCursor == null || rawCursor.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(rawCursor);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Cursor must be a valid UUID");
        }
    }

    private UUID resolveEmployeeId(UUID accountId) {
        if (accountId == null) {
            return employeeRepository.findAll().stream().findFirst().map(Employee::getAccountId).orElse(null);
        }
        return employeeRepository.findByAccountId(accountId)
                .map(Employee::getAccountId)
                .or(() -> employeeRepository.findById(accountId).map(Employee::getAccountId))
                .orElseGet(() -> employeeRepository.findAll().stream().findFirst().map(Employee::getAccountId).orElse(null));
    }
}
