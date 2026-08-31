package com.ecm.server.service.impl;

import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CustomerAddressRequest;
import com.ecm.server.dto.response.CustomerAddressResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.model.Customer;
import com.ecm.server.model.CustomerAddress;
import com.ecm.server.repository.CustomerAddressRepository;
import com.ecm.server.repository.CustomerRepository;
import com.ecm.server.service.CustomerAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Owns the saved-address lifecycle and keeps the one-default-address rule in
 * the same transaction as address changes. PostgreSQL also has a partial
 * unique index as the final line of defence against concurrent writes.
 */
@Service
@RequiredArgsConstructor
public class CustomerAddressServiceImpl implements CustomerAddressService {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerAddressResponse> list(UUID accountId) {
        requireCustomer(accountId);
        return addressRepository.findByCustomerAccountIdOrderByIsDefaultDescCreatedAtAsc(accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CustomerAddressResponse create(UUID accountId, CustomerAddressRequest request) {
        Customer customer = requireCustomer(accountId);
        boolean hasDefault = addressRepository.existsDefaultForCustomer(accountId);
        CustomerAddress address = CustomerAddress.builder()
                .customer(customer)
                .recipientName(normalize(request.getRecipientName()))
                .phone(normalize(request.getPhone()))
                .addressLine(normalize(request.getAddressLine()))
                .isDefault(request.isDefault() || !hasDefault)
                .build();
        if (address.isDefault()) {
            clearDefault(accountId);
        }
        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public CustomerAddressResponse update(UUID accountId, UUID addressId, CustomerAddressRequest request) {
        CustomerAddress address = findOwned(accountId, addressId);
        address.setRecipientName(normalize(request.getRecipientName()));
        address.setPhone(normalize(request.getPhone()));
        address.setAddressLine(normalize(request.getAddressLine()));
        if (request.isDefault()) {
            clearDefault(accountId);
            address.setDefault(true);
        } else if (address.isDefault() && !hasAnotherDefault(accountId, addressId)) {
            // A saved default cannot be unset without selecting a replacement.
            address.setDefault(true);
        } else {
            address.setDefault(false);
        }
        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void delete(UUID accountId, UUID addressId) {
        CustomerAddress address = findOwned(accountId, addressId);
        addressRepository.delete(address);
        if (address.isDefault()) {
            // Flush the delete before selecting a replacement so the deleted
            // default cannot be returned by the query in the same transaction.
            addressRepository.flush();
            addressRepository.findByCustomerAccountIdOrderByIsDefaultDescCreatedAtAsc(accountId)
                    .stream()
                    .findFirst()
                    .ifPresent(replacement -> {
                        replacement.setDefault(true);
                        addressRepository.save(replacement);
                    });
        }
    }

    @Override
    @Transactional
    public CustomerAddressResponse setDefault(UUID accountId, UUID addressId) {
        CustomerAddress address = findOwned(accountId, addressId);
        clearDefault(accountId);
        address.setDefault(true);
        return toResponse(addressRepository.save(address));
    }

    private Customer requireCustomer(UUID accountId) {
        return customerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BusinessException(StatusCode.CUSTOMER_NOT_FOUND));
    }

    private CustomerAddress findOwned(UUID accountId, UUID addressId) {
        return addressRepository.findByIdAndCustomerAccountId(addressId, accountId)
                .orElseThrow(() -> new BusinessException(StatusCode.ADDRESS_NOT_FOUND));
    }

    private void clearDefault(UUID accountId) {
        addressRepository.findByCustomerAccountIdAndIsDefaultTrue(accountId)
                .ifPresent(current -> {
                    current.setDefault(false);
                    addressRepository.save(current);
                    // Make the partial unique index transition deterministic
                    // before inserting/updating the replacement default.
                    addressRepository.flush();
                });
    }

    private boolean hasAnotherDefault(UUID accountId, UUID addressId) {
        return addressRepository.findByCustomerAccountIdAndIsDefaultTrue(accountId)
                .filter(current -> !current.getId().equals(addressId))
                .isPresent();
    }

    private CustomerAddressResponse toResponse(CustomerAddress address) {
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

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
