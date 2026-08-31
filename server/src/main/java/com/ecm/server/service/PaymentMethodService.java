package com.ecm.server.service;

import com.ecm.server.dto.response.PaymentMethodResponse;

import java.util.List;

public interface PaymentMethodService {
    List<PaymentMethodResponse> getActivePaymentMethods();
}
