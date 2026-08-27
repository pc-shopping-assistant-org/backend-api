package com.ecm.server.mapper;

import com.ecm.server.config.GlobalMapperConfig;
import com.ecm.server.dto.response.InvoiceResponse;
import com.ecm.server.dto.response.OrderDetailResponse;
import com.ecm.server.dto.response.PaymentSummaryResponse;
import com.ecm.server.model.Order;
import com.ecm.server.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Mapper(config = GlobalMapperConfig.class, uses = {OrderItemMapper.class, PaymentMapper.class})
public interface OrderMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.fullName")
    @Mapping(target = "customerEmail", source = "customer.email")
    @Mapping(target = "items", source = "orderItems")
    @Mapping(target = "payment", source = "payments", qualifiedByName = "mapLatestPayment")
    @Mapping(target = "subtotalAmount", expression = "java(entity.getTotalAmount() - entity.getShipAmount() + entity.getDiscountAmount())")
    OrderDetailResponse toDetailResponse(Order entity);

    List<OrderDetailResponse> toDetailResponseList(List<Order> entities);

    @Mapping(target = "invoiceId", expression = "java(\"INV-\" + entity.getId().toString().substring(0, 8).toUpperCase())")
    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "issuedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "customerName", source = "customer.fullName")
    @Mapping(target = "items", source = "orderItems")
    @Mapping(target = "subtotalAmount", expression = "java(entity.getTotalAmount() - entity.getShipAmount() + entity.getDiscountAmount())")
    @Mapping(target = "paymentMethod", expression = "java(getLatestPaymentMethod(entity.getPayments()))")
    @Mapping(target = "paymentStatus", expression = "java(getLatestPaymentStatus(entity.getPayments()))")
    InvoiceResponse toInvoiceResponse(Order entity);

    @Named("mapLatestPayment")
    default PaymentSummaryResponse mapLatestPayment(Collection<Payment> payments) {
        if (payments == null || payments.isEmpty()) {
            return null;
        }
        return payments.stream()
                .max(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(p -> PaymentSummaryResponse.builder()
                        .id(p.getId())
                        .method(p.getMethod())
                        .paidAt(p.getPaidAt())
                        .transactionCode(p.getTransactionCode())
                        .status(p.getStatus())
                        .build())
                .orElse(null);
    }

    default String getLatestPaymentMethod(Collection<Payment> payments) {
        if (payments == null || payments.isEmpty()) {
            return "COD";
        }
        return payments.stream()
                .max(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(Payment::getMethod)
                .orElse("COD");
    }

    default String getLatestPaymentStatus(Collection<Payment> payments) {
        if (payments == null || payments.isEmpty()) {
            return "PENDING";
        }
        return payments.stream()
                .max(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(Payment::getStatus)
                .orElse("PENDING");
    }
}
