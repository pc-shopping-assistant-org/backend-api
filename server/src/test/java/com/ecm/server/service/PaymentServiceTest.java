package com.ecm.server.service;

import com.ecm.server.common.StatusCode;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.PaymentMapper;
import com.ecm.server.model.Customer;
import com.ecm.server.model.Order;
import com.ecm.server.model.Payment;
import com.ecm.server.model.PaymentMethod;
import com.ecm.server.repository.OrderRepository;
import com.ecm.server.repository.PaymentMethodRepository;
import com.ecm.server.repository.PaymentRepository;
import com.ecm.server.service.impl.PaymentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private PaymentMapper paymentMapper;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private PaymentServiceImpl service;

    @Test
    void codCannotBeCollectedBeforeShipping() {
        UUID accountId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status("PENDING_CONFIRMATION")
                .customer(Customer.builder().accountId(accountId).build())
                .build();
        Payment payment = Payment.builder()
                .id(paymentId)
                .order(order)
                .paymentMethod(PaymentMethod.builder().code("COD").build())
                .status("PENDING")
                .amount(100L)
                .build();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.confirmCodPayment(accountId, paymentId));

        assertEquals(StatusCode.INVALID_ORDER_STATE_TRANSITION, exception.getStatusCode());
    }

    @Test
    void codCollectionCompletesShippingOrder() {
        UUID accountId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status("SHIPPING")
                .customer(Customer.builder().accountId(accountId).build())
                .build();
        Payment payment = Payment.builder()
                .id(paymentId)
                .order(order)
                .paymentMethod(PaymentMethod.builder().code("COD").build())
                .status("PENDING")
                .amount(100L)
                .build();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentMapper.toDetailResponse(payment)).thenReturn(null);

        service.confirmCodPayment(accountId, paymentId);

        assertEquals("PAID", payment.getStatus());
        assertEquals("COMPLETED", order.getStatus());
    }
}
