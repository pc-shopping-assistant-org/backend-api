package com.ecm.server.service;

import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.PaymentFilterRequest;
import com.ecm.server.dto.request.UpdatePaymentStatusRequest;
import com.ecm.server.dto.response.PaymentDetailResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.PaymentMapper;
import com.ecm.server.model.Order;
import com.ecm.server.model.Payment;
import com.ecm.server.model.PaymentMethod;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.repository.OrderRepository;
import com.ecm.server.repository.PaymentRepository;
import com.ecm.server.service.impl.AdminPaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private AdminPaymentServiceImpl service;

    @Test
    void customerKeywordCanBeUsedWhenSearchingTransactions() {
        when(paymentRepository.findAll(ArgumentMatchers.<Specification<Payment>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = service.getAdminPayments(PaymentFilterRequest.builder()
                .keyword("  Nguyen Van A  ")
                .limit(20)
                .build());

        assertEquals(0, result.getItems().size());
        verify(paymentRepository).findAll(ArgumentMatchers.<Specification<Payment>>any(), any(Pageable.class));
    }

    @Test
    void cannotReopenFailedAttempt() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = payment("FAILED", "STRIPE_CARD", "PENDING_PAYMENT");
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updatePaymentStatus(
                        paymentId,
                        UpdatePaymentStatusRequest.builder().status("PENDING").build(),
                        null)
        );

        assertEquals(StatusCode.INVALID_ORDER_STATE_TRANSITION, exception.getStatusCode());
    }

    @Test
    void cannotDowngradePaidAttempt() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = payment("PAID", "STRIPE_CARD", "PENDING_CONFIRMATION");
        payment.setPaidAt(Instant.parse("2026-08-31T00:00:00Z"));
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updatePaymentStatus(
                        paymentId,
                        UpdatePaymentStatusRequest.builder().status("FAILED").build(),
                        null)
        );

        assertEquals(StatusCode.INVALID_ORDER_STATE_TRANSITION, exception.getStatusCode());
    }

    @Test
    void repeatedPaidUpdateIsIdempotentAndKeepsPaidTimestamp() {
        UUID paymentId = UUID.randomUUID();
        Instant paidAt = Instant.parse("2026-08-31T00:00:00Z");
        Payment payment = payment("PAID", "STRIPE_CARD", "PENDING_CONFIRMATION");
        payment.setId(paymentId);
        payment.setPaidAt(paidAt);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByOrderId(payment.getOrder().getId())).thenReturn(List.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentMapper.toDetailResponse(payment)).thenReturn(PaymentDetailResponse.builder().build());

        service.updatePaymentStatus(
                paymentId,
                UpdatePaymentStatusRequest.builder().status("PAID").build(),
                null
        );

        assertEquals(paidAt, payment.getPaidAt());
        assertEquals("PAID", payment.getStatus());
    }

    @Test
    void cancelledOrderCannotAcceptPendingPaymentAsPaid() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = payment("PENDING", "STRIPE_CARD", "CANCELLED");
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updatePaymentStatus(
                        paymentId,
                        UpdatePaymentStatusRequest.builder().status("PAID").build(),
                        null)
        );

        assertEquals(StatusCode.INVALID_ORDER_STATE_TRANSITION, exception.getStatusCode());
    }

    private Payment payment(String status, String methodCode, String orderStatus) {
        return Payment.builder()
                .order(Order.builder().id(UUID.randomUUID()).status(orderStatus).build())
                .paymentMethod(PaymentMethod.builder().code(methodCode).build())
                .amount(100L)
                .status(status)
                .build();
    }
}
