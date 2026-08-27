package com.ecm.server.service;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.CustomerFilterRequest;
import com.ecm.server.dto.request.UpdateUserStatusRequest;
import com.ecm.server.dto.response.CustomerDetailResponse;
import com.ecm.server.dto.response.CustomerOrderSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface AdminCustomerService {

    CursorPageResponse<CustomerDetailResponse> getCustomers(CustomerFilterRequest request);

    CustomerDetailResponse getCustomerById(UUID id);

    List<CustomerOrderSummaryResponse> getCustomerOrders(UUID id);

    void updateCustomerStatus(UUID id, UpdateUserStatusRequest request);
}
