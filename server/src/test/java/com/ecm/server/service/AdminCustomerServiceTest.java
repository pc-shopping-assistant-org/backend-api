package com.ecm.server.service;

import com.ecm.server.dto.request.CustomerFilterRequest;
import com.ecm.server.mapper.UserMapper;
import com.ecm.server.repository.AccountRepository;
import com.ecm.server.repository.CustomerAddressRepository;
import com.ecm.server.repository.CustomerRepository;
import com.ecm.server.repository.OrderRepository;
import com.ecm.server.service.impl.AdminCustomerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CustomerAddressRepository customerAddressRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private AdminCustomerServiceImpl service;

    @Test
    void getCustomersNormalizesStatusFilter() {
        when(customerRepository.findCustomersInitial(any(), eq("LOCKED"), any(Pageable.class)))
                .thenReturn(List.of());

        service.getCustomers(CustomerFilterRequest.builder()
                .status(" locked ")
                .limit(10)
                .build());

        verify(customerRepository).findCustomersInitial(any(), eq("LOCKED"), any(Pageable.class));
    }
}
