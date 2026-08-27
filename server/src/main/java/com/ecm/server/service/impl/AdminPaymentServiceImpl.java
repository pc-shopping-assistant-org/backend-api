package com.ecm.server.service.impl;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.PaymentFilterRequest;
import com.ecm.server.dto.request.UpdatePaymentStatusRequest;
import com.ecm.server.dto.response.PaymentDetailResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.PaymentMapper;
import com.ecm.server.model.Employee;
import com.ecm.server.model.Order;
import com.ecm.server.model.Payment;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.repository.OrderRepository;
import com.ecm.server.repository.PaymentRepository;
import com.ecm.server.service.AdminPaymentService;
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
    public static final String STATUS_CONFIRM = "CONFIRM";
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
            if (filter.getMethod() != null && !filter.getMethod().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("method")), filter.getMethod().trim().toUpperCase()));
            }
            if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), filter.getStatus().trim().toUpperCase()));
            }
            if (filter.getTransactionCode() != null && !filter.getTransactionCode().isBlank()) {
                predicates.add(cb.equal(root.get("transactionCode"), filter.getTransactionCode().trim()));
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
        String targetStatus = request.getStatus().toUpperCase();
        payment.setStatus(targetStatus);
        if (request.getTransactionCode() != null && !request.getTransactionCode().isBlank()) {
            payment.setTransactionCode(request.getTransactionCode().trim());
        }
        if (STATUS_PAID.equalsIgnoreCase(targetStatus)) {
            payment.setPaidAt(Instant.now());
        }
        payment.setUpdatedBy(employeeId);
        Payment savedPayment = paymentRepository.save(payment);

        // 3. Automatically advance order to CONFIRM if payment is marked PAID
        Order order = payment.getOrder();
        if (order != null && STATUS_PAID.equalsIgnoreCase(targetStatus) && STATUS_PENDING.equalsIgnoreCase(order.getStatus())) {
            order.setStatus(STATUS_CONFIRM);
            order.setUpdatedBy(employeeId);
            orderRepository.save(order);
        }

        log.info("Updated payment [{}] status to [{}] by employee [{}]", paymentId, targetStatus, employeeId);

        // 4. Return updated payment detail
        return paymentMapper.toDetailResponse(savedPayment);
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
