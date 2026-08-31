package com.ecm.server.service.impl;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.PaymentFilterRequest;
import com.ecm.server.dto.request.UpdatePaymentStatusRequest;
import com.ecm.server.dto.response.PaymentDetailResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.PaymentMapper;
import com.ecm.server.model.Account;
import com.ecm.server.model.Customer;
import com.ecm.server.model.Employee;
import com.ecm.server.model.Order;
import com.ecm.server.model.Payment;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.repository.OrderRepository;
import com.ecm.server.repository.PaymentRepository;
import com.ecm.server.service.AdminPaymentService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaymentServiceImpl implements AdminPaymentService {

    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String STATUS_PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    public static final String STATUS_PENDING = "PENDING";
    public static final int DEFAULT_LIMIT = 20;

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<PaymentDetailResponse> getAdminPayments(PaymentFilterRequest filter) {
        // 1. Prepare pagination and criteria parameters
        int pageSize = (filter.getLimit() != null && filter.getLimit() > 0) ? filter.getLimit() : DEFAULT_LIMIT;
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        // 2. Build dynamic specification with eager fetch joins to prevent N+1 query
        Specification<Payment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getCursor() != null) {
                predicates.add(cb.lessThan(root.get("id"), filter.getCursor()));
            }
            if (filter.getOrderId() != null) {
                predicates.add(cb.equal(root.get("order").get("id"), filter.getOrderId()));
            }
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String search = "%" + filter.getKeyword().trim().toLowerCase(java.util.Locale.ROOT) + "%";
                Join<Payment, Order> order = root.join("order", JoinType.INNER);
                Join<Order, Customer> customer = order.join("customer", JoinType.LEFT);
                Join<Customer, Account> account = customer.join("account", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(customer.get("firstName")), search),
                        cb.like(cb.lower(customer.get("lastName")), search),
                        cb.like(cb.lower(cb.concat(cb.concat(customer.get("firstName"), " "), customer.get("lastName"))), search),
                        cb.like(cb.lower(account.get("email")), search),
                        cb.like(cb.lower(account.get("phone")), search)
                ));
            }
            if (filter.getPaymentMethodCode() != null && !filter.getPaymentMethodCode().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.join("paymentMethod").get("code")), filter.getPaymentMethodCode().trim().toUpperCase()));
            }
            if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), filter.getStatus().trim().toUpperCase()));
            }
            if (filter.getProviderTransactionCode() != null && !filter.getProviderTransactionCode().isBlank()) {
                predicates.add(cb.equal(root.get("providerTransactionCode"), filter.getProviderTransactionCode().trim()));
            }
            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getToDate()));
            }

            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("order", JoinType.LEFT).fetch("customer", JoinType.LEFT);
            }
            query.orderBy(cb.desc(root.get("id")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Payment> payments = paymentRepository.findAll(spec, pageable).getContent();

        // 3. Assemble and return cursor response envelope
        return CursorPageResponse.of(
                payments,
                pageSize,
                payment -> payment.getId().toString(),
                paymentMapper::toDetailResponse
        );
    }

    @Override
    @Transactional
    public PaymentDetailResponse updatePaymentStatus(UUID paymentId, UpdatePaymentStatusRequest request, UUID adminId) {
        // 1. Retrieve payment record
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "Payment transaction not found"));

        // 2. Update status and audit fields
        UUID employeeId = resolveEmployeeId(adminId);
        String targetStatus = request.getStatus().trim().toUpperCase();
        validatePaymentTransition(payment, targetStatus);
        if (STATUS_PAID.equalsIgnoreCase(targetStatus)
                && payment.getOrder() != null
                && "CANCELLED".equalsIgnoreCase(payment.getOrder().getStatus())) {
            throw new BusinessException(StatusCode.INVALID_ORDER_STATE_TRANSITION,
                    "A cancelled order cannot accept a payment attempt");
        }
        if (STATUS_PAID.equalsIgnoreCase(targetStatus) && payment.getOrder() != null) {
            boolean anotherPaidAttempt = paymentRepository.findByOrderId(payment.getOrder().getId()).stream()
                    .anyMatch(other -> !other.getId().equals(paymentId) && STATUS_PAID.equalsIgnoreCase(other.getStatus()));
            if (anotherPaidAttempt) {
                throw new BusinessException(StatusCode.CONFLICT,
                        "An order can have at most one PAID payment attempt");
            }
            if ("COD".equalsIgnoreCase(payment.getPaymentMethod() == null ? null : payment.getPaymentMethod().getCode())
                    && !"SHIPPING".equalsIgnoreCase(payment.getOrder().getStatus())
                    && !"COMPLETED".equalsIgnoreCase(payment.getOrder().getStatus())) {
                throw new BusinessException(StatusCode.INVALID_ORDER_STATE_TRANSITION,
                        "COD payment can be marked PAID only after the order reaches SHIPPING");
            }
        }
        payment.setStatus(targetStatus);
        if (request.getProviderTransactionCode() != null && !request.getProviderTransactionCode().isBlank()) {
            payment.setProviderTransactionCode(request.getProviderTransactionCode().trim());
        }
        if (STATUS_PAID.equalsIgnoreCase(targetStatus) && payment.getPaidAt() == null) {
            payment.setPaidAt(Instant.now());
        } else if (!STATUS_PAID.equalsIgnoreCase(targetStatus)) {
            payment.setPaidAt(null);
        }
        payment.setUpdatedBy(employeeId);
        Payment savedPayment = paymentRepository.save(payment);

        // 3. Keep order and payment state machines synchronized.
        Order order = payment.getOrder();
        if (order != null && STATUS_PAID.equalsIgnoreCase(targetStatus)
                && STATUS_PENDING_PAYMENT.equalsIgnoreCase(order.getStatus())) {
            order.setStatus(STATUS_PENDING_CONFIRMATION);
            order.setUpdatedBy(employeeId);
            orderRepository.save(order);
        } else if (order != null && STATUS_PAID.equalsIgnoreCase(targetStatus)
                && "COD".equalsIgnoreCase(payment.getPaymentMethod() == null ? null : payment.getPaymentMethod().getCode())
                && "SHIPPING".equalsIgnoreCase(order.getStatus())) {
            order.setStatus("COMPLETED");
            order.setDeliveredAt(Instant.now());
            order.setUpdatedBy(employeeId);
            orderRepository.save(order);
        }

        log.info("Updated payment [{}] status to [{}] by employee [{}]", paymentId, targetStatus, employeeId);

        // 4. Return updated payment detail
        return paymentMapper.toDetailResponse(savedPayment);
    }

    /**
     * A payment row represents one immutable attempt.  Retries are modeled by
     * inserting another attempt, not by reopening an already terminal row.
     * Repeating the same status is intentionally idempotent so webhook/admin
     * retries do not fail unnecessarily.
     */
    private void validatePaymentTransition(Payment payment, String targetStatus) {
        String currentStatus = payment.getStatus() == null
                ? STATUS_PENDING
                : payment.getStatus().trim().toUpperCase();
        if (currentStatus.equals(targetStatus)) {
            return;
        }
        if (!STATUS_PENDING.equals(currentStatus)) {
            throw new BusinessException(StatusCode.INVALID_ORDER_STATE_TRANSITION,
                    "Payment attempt status is terminal and cannot transition from "
                            + currentStatus + " to " + targetStatus);
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
