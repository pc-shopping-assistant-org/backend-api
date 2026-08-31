package com.ecm.server.service;

import com.ecm.server.dto.request.CustomerAddressRequest;
import com.ecm.server.dto.response.CustomerAddressResponse;
import com.ecm.server.model.Customer;
import com.ecm.server.model.CustomerAddress;
import com.ecm.server.repository.CustomerAddressRepository;
import com.ecm.server.repository.CustomerRepository;
import com.ecm.server.service.impl.CustomerAddressServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAddressServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerAddressRepository addressRepository;

    @InjectMocks
    private CustomerAddressServiceImpl service;

    @Test
    void create_firstAddress_makesItDefault() {
        UUID accountId = UUID.randomUUID();
        Customer customer = Customer.builder().accountId(accountId).build();
        CustomerAddressRequest request = request(false);
        when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.of(customer));
        when(addressRepository.existsDefaultForCustomer(accountId)).thenReturn(false);
        when(addressRepository.save(any(CustomerAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerAddressResponse response = service.create(accountId, request);

        assertTrue(response.isDefault());
        verify(addressRepository).save(any(CustomerAddress.class));
    }

    @Test
    void create_requestedDefault_clearsExistingDefault() {
        UUID accountId = UUID.randomUUID();
        Customer customer = Customer.builder().accountId(accountId).build();
        CustomerAddress current = CustomerAddress.builder()
                .id(UUID.randomUUID()).customer(customer).isDefault(true)
                .recipientName("Old").phone("0987654321").addressLine("Old address")
                .build();
        when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.of(customer));
        when(addressRepository.existsDefaultForCustomer(accountId)).thenReturn(true);
        when(addressRepository.findByCustomerAccountIdAndIsDefaultTrue(accountId)).thenReturn(Optional.of(current));
        when(addressRepository.save(any(CustomerAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerAddressResponse response = service.create(accountId, request(true));

        assertTrue(response.isDefault());
        assertFalse(current.isDefault());
        verify(addressRepository).save(current);
    }

    @Test
    void delete_default_promotesOldestRemainingAddress() {
        UUID accountId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        Customer customer = Customer.builder().accountId(accountId).build();
        CustomerAddress current = CustomerAddress.builder()
                .id(addressId).customer(customer).isDefault(true)
                .recipientName("Current").phone("0987654321").addressLine("Current address")
                .build();
        CustomerAddress replacement = CustomerAddress.builder()
                .id(UUID.randomUUID()).customer(customer).isDefault(false)
                .recipientName("Replacement").phone("0987654321").addressLine("Replacement address")
                .build();
        when(addressRepository.findByIdAndCustomerAccountId(addressId, accountId)).thenReturn(Optional.of(current));
        when(addressRepository.findByCustomerAccountIdOrderByIsDefaultDescCreatedAtAsc(accountId))
                .thenReturn(List.of(replacement));

        service.delete(accountId, addressId);

        assertTrue(replacement.isDefault());
        verify(addressRepository).delete(current);
        verify(addressRepository).save(replacement);
    }

    private CustomerAddressRequest request(boolean isDefault) {
        return CustomerAddressRequest.builder()
                .recipientName(" Nguyen Van A ")
                .phone("0987654321")
                .addressLine(" 123 Main Street ")
                .isDefault(isDefault)
                .build();
    }
}
