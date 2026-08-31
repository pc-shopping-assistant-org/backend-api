package com.ecm.server.service.impl;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CustomerFilterRequest;
import com.ecm.server.dto.request.UpdateUserStatusRequest;
import com.ecm.server.dto.response.CustomerDetailResponse;
import com.ecm.server.dto.response.CustomerAddressResponse;
import com.ecm.server.dto.response.CustomerOrderSummaryResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.UserMapper;
import com.ecm.server.model.Account;
import com.ecm.server.model.Customer;
import com.ecm.server.model.CustomerAddress;
import com.ecm.server.model.Order;
import com.ecm.server.repository.AccountRepository;
import com.ecm.server.repository.CustomerRepository;
import com.ecm.server.repository.CustomerAddressRepository;
import com.ecm.server.repository.OrderRepository;
import com.ecm.server.service.AdminCustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCustomerServiceImpl implements AdminCustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final UserMapper userMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<CustomerDetailResponse> getCustomers(CustomerFilterRequest request) {
        // 1. Query limit + 1 records using keyset cursor pagination
        int limit = request.getSanitizedLimit();
        int queryLimit = limit + 1;
        UUID cursorUuid = (request.getCursor() != null && !request.getCursor().isBlank())
                ? UUID.fromString(request.getCursor())
                : null;
        String keywordPattern = (request.getKeyword() != null && !request.getKeyword().isBlank())
                ? "%" + request.getKeyword().trim().toLowerCase() + "%"
                : null;
        String statusFilter = normalizeEnumFilter(request.getStatus());

        Pageable pageable = PageRequest.of(0, queryLimit);
        List<Customer> customers = (cursorUuid == null)
                ? customerRepository.findCustomersInitial(keywordPattern, statusFilter, pageable)
                : customerRepository.findCustomersAfterCursor(cursorUuid, keywordPattern, statusFilter, pageable);

        // 2. Transform customer entities to DTOs via MapStruct with order statistics
        List<CustomerDetailResponse> dtoList = customers.stream()
                .map(customer -> {
                    long totalOrders = orderRepository.countByCustomerId(customer.getAccountId());
                    long totalSpent = orderRepository.sumSpentByCustomerId(customer.getAccountId());
                    CustomerDetailResponse response = userMapper.toCustomerDetail(customer, totalOrders, totalSpent);
                    hydrateAddresses(customer, response);
                    return response;
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
        long totalOrders = orderRepository.countByCustomerId(customer.getAccountId());
        long totalSpent = orderRepository.sumSpentByCustomerId(customer.getAccountId());

        // 3. Map to detail DTO via MapStruct
        CustomerDetailResponse response = userMapper.toCustomerDetail(customer, totalOrders, totalSpent);
        hydrateAddresses(customer, response);
        return response;
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

        // 2. Harmonize status codes between customer profile and account tables
        String rawStatus = request.getStatus().toUpperCase();
        String accountStatus = "BLOCKED".equals(rawStatus) ? "LOCKED" : rawStatus;

        Account account = customer.getAccount();
        if (account != null) {
            account.setStatus(accountStatus);
            accountRepository.save(account);

            // 3. Update Redis blocked blacklist for stateless JWT revocation
            String blockedKey = "account:blocked:" + account.getId();
            try {
                if ("LOCKED".equalsIgnoreCase(accountStatus) || "BLOCKED".equalsIgnoreCase(accountStatus) || "DELETED".equalsIgnoreCase(accountStatus)) {
                    // Account locking is durable until an explicit ACTIVE
                    // update; a fixed TTL would silently re-enable old JWTs.
                    redisTemplate.opsForValue().set(blockedKey, "BLOCKED");
                } else if ("ACTIVE".equalsIgnoreCase(accountStatus)) {
                    redisTemplate.delete(blockedKey);
                }
            } catch (Exception ex) {
                // Redis is only a revocation hint. The account status update
                // must still commit when local staging runs PostgreSQL only;
                // the JWT filter checks the database source of truth.
                log.warn("Could not update account block cache for customer [{}]: {}", id, ex.getMessage());
            }
        }

        // 4. Log customer status update event
        log.info("Updated customer [{}] status to [{}] with reason: {}", id, accountStatus, request.getReason());
    }

    private void hydrateAddresses(Customer customer, CustomerDetailResponse response) {
        List<CustomerAddressResponse> addresses = customerAddressRepository
                .findByCustomerAccountIdOrderByIsDefaultDescCreatedAtAsc(customer.getAccountId())
                .stream()
                .map(this::toAddressResponse)
                .toList();
        response.setAddresses(addresses);
        addresses.stream().filter(CustomerAddressResponse::isDefault).findFirst()
                .ifPresent(address -> response.setAddress(address.getAddressLine()));
    }

    private CustomerAddressResponse toAddressResponse(CustomerAddress address) {
        return CustomerAddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .addressLine(address.getAddressLine())
                .isDefault(address.isDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    private String normalizeEnumFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
