package com.ecm.server.service;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.CancelOrderRequest;
import com.ecm.server.dto.request.CreateOrderRequest;
import com.ecm.server.dto.request.OrderFilterRequest;
import com.ecm.server.dto.response.OrderDetailResponse;

import java.util.UUID;

public interface OrderService {

    OrderDetailResponse createOrder(UUID accountId, CreateOrderRequest request);

    CursorPageResponse<OrderDetailResponse> getMyOrders(UUID accountId, OrderFilterRequest filter);

    OrderDetailResponse getOrderById(UUID accountId, UUID orderId);

    OrderDetailResponse cancelOrder(UUID accountId, UUID orderId, CancelOrderRequest request);
}
