package com.ecm.server.service;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.CreateDiscountRequest;
import com.ecm.server.dto.request.DiscountFilterRequest;
import com.ecm.server.dto.request.UpdateDiscountRequest;
import com.ecm.server.dto.response.DiscountDetailResponse;
import com.ecm.server.dto.response.DiscountSummaryResponse;

import java.util.UUID;

public interface AdminDiscountService {

    CursorPageResponse<DiscountSummaryResponse> getAdminDiscounts(DiscountFilterRequest filter);

    DiscountDetailResponse getDiscountById(UUID id);

    DiscountDetailResponse createDiscount(CreateDiscountRequest request, UUID adminId);

    DiscountDetailResponse updateDiscount(UUID id, UpdateDiscountRequest request, UUID adminId);

    void updateDiscountStatus(UUID id, String status, UUID adminId);

    void deleteDiscount(UUID id);
}
