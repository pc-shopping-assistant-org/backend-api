package com.ecm.server.service;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.DiscountFilterRequest;
import com.ecm.server.dto.request.ValidateDiscountRequest;
import com.ecm.server.dto.response.DiscountSummaryResponse;
import com.ecm.server.dto.response.DiscountValidationResponse;

public interface DiscountService {

    CursorPageResponse<DiscountSummaryResponse> getActiveDiscounts(DiscountFilterRequest filter);

    DiscountValidationResponse validateDiscount(ValidateDiscountRequest request);
}
