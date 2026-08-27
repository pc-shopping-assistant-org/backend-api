package com.ecm.server.service;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.CreateSupplierRequest;
import com.ecm.server.dto.request.SupplierFilterRequest;
import com.ecm.server.dto.request.UpdateSupplierRequest;
import com.ecm.server.dto.response.SupplierResponse;

import java.util.UUID;

public interface SupplierService {

    CursorPageResponse<SupplierResponse> getSuppliers(SupplierFilterRequest request);

    SupplierResponse getSupplierById(UUID id);

    SupplierResponse createSupplier(CreateSupplierRequest request);

    SupplierResponse updateSupplier(UUID id, UpdateSupplierRequest request);

    void deleteSupplier(UUID id);
}
