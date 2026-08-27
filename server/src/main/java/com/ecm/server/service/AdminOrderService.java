package com.ecm.server.service;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.OrderFilterRequest;
import com.ecm.server.dto.request.UpdateOrderStatusRequest;
import com.ecm.server.dto.response.InvoiceResponse;
import com.ecm.server.dto.response.OrderDetailResponse;

import java.util.UUID;

public interface AdminOrderService {

    CursorPageResponse<OrderDetailResponse> getAdminOrders(OrderFilterRequest filter);

    OrderDetailResponse getAdminOrderById(UUID orderId);

    OrderDetailResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request, UUID adminId);

    InvoiceResponse getOrderInvoice(UUID orderId);
}
