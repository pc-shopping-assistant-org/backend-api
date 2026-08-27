package com.ecm.server.mapper;

import com.ecm.server.config.GlobalMapperConfig;
import com.ecm.server.dto.response.PaymentSummaryResponse;
import com.ecm.server.model.Payment;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface PaymentMapper {

    PaymentSummaryResponse toSummaryResponse(Payment entity);
}
