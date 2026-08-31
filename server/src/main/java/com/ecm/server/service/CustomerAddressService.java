package com.ecm.server.service;

import com.ecm.server.dto.request.CustomerAddressRequest;
import com.ecm.server.dto.response.CustomerAddressResponse;

import java.util.List;
import java.util.UUID;

public interface CustomerAddressService {

    List<CustomerAddressResponse> list(UUID accountId);

    CustomerAddressResponse create(UUID accountId, CustomerAddressRequest request);

    CustomerAddressResponse update(UUID accountId, UUID addressId, CustomerAddressRequest request);

    void delete(UUID accountId, UUID addressId);

    CustomerAddressResponse setDefault(UUID accountId, UUID addressId);
}
