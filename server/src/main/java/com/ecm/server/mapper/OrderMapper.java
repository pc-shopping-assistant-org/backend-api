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

    @Mapping(target = "customerId", source = "customer.accountId")
    @Mapping(target = "customerName", expression = "java(customerName(entity))")
    @Mapping(target = "customerEmail", source = "customer.account.email")
    @Mapping(target = "items", source = "orderItems")
    @Mapping(target = "payments", source = "payments", qualifiedByName = "mapPaymentAttempts")
    @Mapping(target = "subtotalAmount", source = "subtotalAmount")
    @Mapping(target = "discountAmount", source = "discountAmount")
    @Mapping(target = "shippingFee", source = "shippingFee")
    @Mapping(target = "shippingMethodCode", source = "shippingMethod.code")
    OrderDetailResponse toDetailResponse(Order entity);

    List<OrderDetailResponse> toDetailResponseList(List<Order> entities);

    @Mapping(target = "invoiceId", expression = "java(\"INV-\" + entity.getId().toString().substring(0, 8).toUpperCase())")
    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "issuedAt", expression = "java(entity.getDeliveredAt() != null ? entity.getDeliveredAt() : entity.getOrderTime())")
    @Mapping(target = "customerName", expression = "java(customerName(entity))")
    @Mapping(target = "recipientName", source = "recipientName")
    @Mapping(target = "recipientPhone", source = "recipientPhone")
    @Mapping(target = "deliveryAddress", source = "deliveryAddress")
    @Mapping(target = "items", source = "orderItems")
    @Mapping(target = "subtotalAmount", source = "subtotalAmount")
    @Mapping(target = "discountAmount", source = "discountAmount")
    @Mapping(target = "shippingFee", source = "shippingFee")
    @Mapping(target = "paymentMethodCode", expression = "java(getLatestPaymentMethod(entity.getPayments()))")
    @Mapping(target = "paymentStatus", expression = "java(getLatestPaymentStatus(entity.getPayments()))")
    InvoiceResponse toInvoiceResponse(Order entity);

    /**
     * Preserve every payment attempt in creation order.  A failed attempt is
     * intentionally not overwritten by a later retry, so order detail and
     * admin detail can explain how an order reached its current state.
     */
    @Named("mapPaymentAttempts")
    default List<PaymentSummaryResponse> mapPaymentAttempts(Collection<Payment> payments) {
        if (payments == null || payments.isEmpty()) {
            return List.of();
        }
        return payments.stream()
                .sorted(Comparator
                        .comparing(Payment::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(Payment::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(p -> PaymentSummaryResponse.builder()
                        .id(p.getId())
                        .paymentMethodCode(p.getPaymentMethod() == null ? null : p.getPaymentMethod().getCode())
                        .amount(p.getAmount())
                        .paidAt(p.getPaidAt())
                        .providerTransactionCode(p.getProviderTransactionCode())
                        .status(p.getStatus())
                        .build())
                .toList();
    }

    default String getLatestPaymentMethod(Collection<Payment> payments) {
        if (payments == null || payments.isEmpty()) {
            return "COD";
        }
        return payments.stream()
                .max(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(p -> p.getPaymentMethod() == null ? null : p.getPaymentMethod().getCode())
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

    default String customerName(Order entity) {
        if (entity == null || entity.getCustomer() == null) {
            return null;
        }
        return UserMappingSupport.fullName(entity.getCustomer().getFirstName(), entity.getCustomer().getLastName());
    }
}
