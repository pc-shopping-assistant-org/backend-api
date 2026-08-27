package com.ecm.server.service.impl;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CustomerFilterRequest;
import com.ecm.server.dto.request.UpdateUserStatusRequest;
import com.ecm.server.dto.response.CustomerDetailResponse;
import com.ecm.server.dto.response.CustomerOrderSummaryResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.UserMapper;
import com.ecm.server.model.Account;
import com.ecm.server.model.Customer;
import com.ecm.server.model.Order;
import com.ecm.server.repository.AccountRepository;
import com.ecm.server.repository.CustomerRepository;
import com.ecm.server.repository.OrderRepository;
import com.ecm.server.service.AdminCustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCustomerServiceImpl implements AdminCustomerService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<CustomerDetailResponse> getCustomers(CustomerFilterRequest request) {
        // 1. Query limit + 1 records using keyset cursor pagination
        int limit = request.getSanitizedLimit();
        int queryLimit = limit + 1;
        UUID cursorUuid = (request.getCursor() != null && !request.getCursor().isBlank())
                ? UUID.fromString(request.getCursor())
                : null;

        List<Customer> customers = customerRepository.findCustomersByCursor(
                cursorUuid,
                request.getKeyword(),
                request.getStatus(),
                queryLimit
        );

        // 2. Transform customer entities to DTOs via MapStruct with order statistics
        List<CustomerDetailResponse> dtoList = customers.stream()
                .map(customer -> {
                    long totalOrders = orderRepository.countByCustomerId(customer.getId());
                    long totalSpent = orderRepository.sumSpentByCustomerId(customer.getId());
                    return userMapper.toCustomerDetail(customer, totalOrders, totalSpent);
                })
                .toList();

        // 3. Construct cursor page response
        return CursorPageResponse.of(dtoList, limit, item -> item.getId().toString());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDetailResponse getCustomerById(UUID id) {
        // 1. Retrieve customer entity by ID
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StatusCode.CUSTOMER_NOT_FOUND));

        // 2. Query customer order metrics
        long totalOrders = orderRepository.countByCustomerId(customer.getId());
        long totalSpent = orderRepository.sumSpentByCustomerId(customer.getId());

        // 3. Map to detail DTO via MapStruct
        return userMapper.toCustomerDetail(customer, totalOrders, totalSpent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerOrderSummaryResponse> getCustomerOrders(UUID id) {
        // 1. Validate customer existence
        if (!customerRepository.existsById(id)) {
            throw new BusinessException(StatusCode.CUSTOMER_NOT_FOUND);
        }

        // 2. Retrieve customer's historical orders
        List<Order> orders = orderRepository.findByCustomerIdOrderByOrderTimeDesc(id);

        // 3. Map order entities to summary DTO list via MapStruct
        return orders.stream()
                .map(userMapper::toOrderSummary)
                .toList();
    }

    @Override
    @Transactional
    public void updateCustomerStatus(UUID id, UpdateUserStatusRequest request) {
        // 1. Retrieve customer entity by ID
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StatusCode.CUSTOMER_NOT_FOUND));

        // 2. Synchronize status across customer and account entities
        String newStatus = request.getStatus().toUpperCase();
        customer.setStatus(newStatus);
        customerRepository.save(customer);

        Account account = customer.getAccount();
        if (account != null) {
            account.setStatus(newStatus);
            accountRepository.save(account);
        }

        // 3. Log customer status update event
        log.info("Updated customer [{}] status to [{}] with reason: {}", id, newStatus, request.getReason());
    }
}
