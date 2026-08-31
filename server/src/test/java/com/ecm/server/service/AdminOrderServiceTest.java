package com.ecm.server.service;

import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.UpdateOrderStatusRequest;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.model.Order;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.repository.OrderItemRepository;
import com.ecm.server.repository.OrderRepository;
import com.ecm.server.repository.PaymentRepository;
import com.ecm.server.repository.ProductVariantRepository;
import com.ecm.server.mapper.OrderMapper;
import com.ecm.server.service.impl.AdminOrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private AdminOrderServiceImpl service;

    @Test
    void pendingPaymentCannotBeConfirmedBeforePaymentIsPaid() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).status("PENDING_PAYMENT").build();
        when(orderRepository.findByIdWithDetailsForUpdate(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.updateOrderStatus(
                orderId,
                UpdateOrderStatusRequest.builder().status("PENDING_CONFIRMATION").build(),
                null));

        assertEquals(StatusCode.PAYMENT_FAILED, exception.getStatusCode());
    }
}
