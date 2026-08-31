package com.ecm.server.service.impl;

import com.ecm.server.dto.response.PaymentMethodResponse;
import com.ecm.server.model.PaymentMethod;
import com.ecm.server.repository.PaymentMethodRepository;
import com.ecm.server.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getActivePaymentMethods() {
        return paymentMethodRepository.findByStatusOrderByCreatedAtAsc("ACTIVE").stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentMethodResponse toResponse(PaymentMethod method) {
        return PaymentMethodResponse.builder()
                .id(method.getId())
                .code(method.getCode())
                .name(method.getName())
                .status(method.getStatus())
                .createdAt(method.getCreatedAt())
                .updatedAt(method.getUpdatedAt())
                .build();
    }
}
