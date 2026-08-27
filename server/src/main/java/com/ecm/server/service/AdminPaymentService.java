package com.ecm.server.service;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.PaymentFilterRequest;
import com.ecm.server.dto.request.UpdatePaymentStatusRequest;
import com.ecm.server.dto.response.PaymentDetailResponse;

import java.util.UUID;

public interface AdminPaymentService {

    CursorPageResponse<PaymentDetailResponse> getAdminPayments(PaymentFilterRequest filter);

    PaymentDetailResponse updatePaymentStatus(UUID paymentId, UpdatePaymentStatusRequest request, UUID adminId);
}
