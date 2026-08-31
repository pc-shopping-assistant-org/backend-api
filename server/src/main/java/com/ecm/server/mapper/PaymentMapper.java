package com.ecm.server.mapper;

import com.ecm.server.config.GlobalMapperConfig;
import com.ecm.server.dto.response.PaymentDetailResponse;
import com.ecm.server.dto.response.PaymentSummaryResponse;
import com.ecm.server.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
public interface PaymentMapper {

    @Mapping(target = "paymentMethodCode", source = "paymentMethod.code")
    @Mapping(target = "providerTransactionCode", source = "providerTransactionCode")
    PaymentSummaryResponse toSummaryResponse(Payment entity);

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "paymentMethodCode", source = "paymentMethod.code")
    @Mapping(target = "providerTransactionCode", source = "providerTransactionCode")
    @Mapping(target = "amount", source = "amount")
    PaymentDetailResponse toDetailResponse(Payment entity);

    List<PaymentDetailResponse> toDetailResponseList(List<Payment> entities);
}
