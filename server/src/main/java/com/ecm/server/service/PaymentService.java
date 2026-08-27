package com.ecm.server.service;

import com.ecm.server.dto.request.CreatePaymentIntentRequest;
import com.ecm.server.dto.response.PaymentDetailResponse;
import com.ecm.server.dto.response.PaymentIntentResponse;

import java.util.UUID;

public interface PaymentService {

    PaymentIntentResponse createPaymentIntent(UUID accountId, CreatePaymentIntentRequest request);

    PaymentDetailResponse getPaymentByOrderId(UUID accountId, UUID orderId);

    PaymentDetailResponse confirmCodPayment(UUID accountId, UUID paymentId);

    void handleStripeWebhook(String payload, String signatureHeader);
}
