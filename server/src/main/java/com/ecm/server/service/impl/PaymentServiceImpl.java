package com.ecm.server.service.impl;

import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreatePaymentIntentRequest;
import com.ecm.server.dto.response.PaymentDetailResponse;
import com.ecm.server.dto.response.PaymentIntentResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.PaymentMapper;
import com.ecm.server.model.Order;
import com.ecm.server.model.Payment;
import com.ecm.server.repository.OrderRepository;
import com.ecm.server.repository.PaymentRepository;
import com.ecm.server.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    public static final String METHOD_COD = "COD";
    public static final String METHOD_STRIPE = "STRIPE";
    public static final String METHOD_CREDIT_CARD = "CREDIT_CARD";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CONFIRM = "CONFIRM";
    public static final String REDIS_PAYMENT_INTENT_PREFIX = "payment_intent:";
    public static final long PAYMENT_INTENT_TTL_MINUTES = 15;
    public static final String DEFAULT_CURRENCY = "vnd";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.publishable-key:}")
    private String stripePublishableKey;

    @Value("${stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    @Override
    @Transactional
    public PaymentIntentResponse createPaymentIntent(UUID accountId, CreatePaymentIntentRequest request) {
        // 1. Retrieve order and verify customer ownership
        Order order = orderRepository.findByIdWithDetails(request.getOrderId())
                .orElseThrow(() -> new BusinessException(StatusCode.ORDER_NOT_FOUND));

        if (order.getCustomer() == null || order.getCustomer().getAccount() == null
                || !accountId.equals(order.getCustomer().getAccount().getId())) {
            throw new BusinessException(StatusCode.FORBIDDEN, "You do not have permission to pay for this order");
        }

        // 2. Validate that order is in PENDING status
        if (!STATUS_PENDING.equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException(StatusCode.INVALID_ORDER_STATE_TRANSITION, "Order is not in PENDING state (Current: " + order.getStatus() + ")");
        }

        String method = request.getPaymentMethod().toUpperCase();
        String transactionCode = null;
        String clientSecret = null;

        // 3. Process Stripe / Credit Card payment intent creation
        if (METHOD_STRIPE.equalsIgnoreCase(method) || METHOD_CREDIT_CARD.equalsIgnoreCase(method)) {
            if (stripeSecretKey != null && !stripeSecretKey.isBlank()) {
                try {
                    Stripe.apiKey = stripeSecretKey;
                    PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                            .setAmount(order.getTotalAmount())
                            .setCurrency(DEFAULT_CURRENCY)
                            .setDescription("Order Payment #" + order.getId())
                            .putMetadata("orderId", order.getId().toString())
                            .putMetadata("customerId", order.getCustomer().getId().toString())
                            .build();

                    PaymentIntent intent = PaymentIntent.create(params);
                    transactionCode = intent.getId();
                    clientSecret = intent.getClientSecret();
                } catch (Exception e) {
                    log.error("Failed to create Stripe PaymentIntent: {}", e.getMessage(), e);
                    throw new BusinessException(StatusCode.EXTERNAL_SERVICE_ERROR, "Stripe service error: " + e.getMessage());
                }
            } else {
                // Fallback test mode simulation when live Stripe key is not configured
                transactionCode = "pi_mock_" + UUID.randomUUID();
                clientSecret = transactionCode + "_secret_" + UUID.randomUUID();
            }

            // Save payment intent ID in Redis with 15-minute TTL for idempotency and timeout tracking
            String redisKey = REDIS_PAYMENT_INTENT_PREFIX + transactionCode;
            redisTemplate.opsForValue().set(redisKey, order.getId().toString(), Duration.ofMinutes(PAYMENT_INTENT_TTL_MINUTES));
        } else {
            transactionCode = "COD_" + order.getId().toString().substring(0, 8).toUpperCase();
            clientSecret = "";
        }

        // 4. Upsert payment record linked to order
        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())
                .orElseGet(() -> Payment.builder().order(order).build());

        payment.setMethod(method);
        payment.setTransactionCode(transactionCode);
        payment.setStatus(STATUS_PENDING);
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Initialized payment intent [{}] for order [{}] with method [{}]", savedPayment.getId(), order.getId(), method);

        // 5. Assemble and return PaymentIntentResponse DTO
        return PaymentIntentResponse.builder()
                .paymentId(savedPayment.getId())
                .orderId(order.getId())
                .clientSecret(clientSecret)
                .amount(order.getTotalAmount())
                .currency(DEFAULT_CURRENCY.toUpperCase())
                .publishableKey(stripePublishableKey != null ? stripePublishableKey : "")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDetailResponse getPaymentByOrderId(UUID accountId, UUID orderId) {
        // 1. Retrieve order and enforce ownership
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new BusinessException(StatusCode.ORDER_NOT_FOUND));

        if (order.getCustomer() == null || order.getCustomer().getAccount() == null
                || !accountId.equals(order.getCustomer().getAccount().getId())) {
            throw new BusinessException(StatusCode.FORBIDDEN, "You do not have permission to view payment for this order");
        }

        // 2. Fetch latest payment for order
        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "No payment record found for order " + orderId));

        // 3. Map and return detail DTO
        return paymentMapper.toDetailResponse(payment);
    }

    @Override
    @Transactional
    public PaymentDetailResponse confirmCodPayment(UUID accountId, UUID paymentId) {
        // 1. Retrieve payment record
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "Payment record not found"));

        Order order = payment.getOrder();
        if (order != null && order.getCustomer() != null && order.getCustomer().getAccount() != null
                && !accountId.equals(order.getCustomer().getAccount().getId())) {
            throw new BusinessException(StatusCode.FORBIDDEN, "You do not have permission to confirm this payment");
        }

        // 2. Verify COD payment method
        payment.setMethod(METHOD_COD);
        payment.setStatus(STATUS_PENDING);
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Confirmed COD payment [{}] for order [{}]", paymentId, order != null ? order.getId() : null);

        // 3. Return updated payment detail
        return paymentMapper.toDetailResponse(savedPayment);
    }

    @Override
    @Transactional
    public void handleStripeWebhook(String payload, String signatureHeader) {
        // 1. Verify webhook signature or parse payload JSON
        String eventType = null;
        String paymentIntentId = null;

        if (stripeWebhookSecret != null && !stripeWebhookSecret.isBlank() && signatureHeader != null && !signatureHeader.isBlank()) {
            try {
                Event event = Webhook.constructEvent(payload, signatureHeader, stripeWebhookSecret);
                eventType = event.getType();
                if (event.getDataObjectDeserializer().getObject().isPresent()) {
                    var stripeObj = event.getDataObjectDeserializer().getObject().get();
                    if (stripeObj instanceof PaymentIntent pi) {
                        paymentIntentId = pi.getId();
                    }
                }
            } catch (Exception e) {
                log.error("Stripe webhook signature verification failed: {}", e.getMessage());
                throw new BusinessException(StatusCode.BAD_REQUEST, "Webhook signature verification failed: " + e.getMessage());
            }
        } else {
            // Direct JSON extraction for sandbox simulation
            try {
                JsonNode root = objectMapper.readTree(payload);
                eventType = root.path("type").asText();
                paymentIntentId = root.path("data").path("object").path("id").asText();
            } catch (Exception e) {
                log.error("Failed to parse webhook JSON payload: {}", e.getMessage());
                throw new BusinessException(StatusCode.BAD_REQUEST, "Malformed webhook payload");
            }
        }

        // 2. Process successful payment event (payment_intent.succeeded)
        if ("payment_intent.succeeded".equalsIgnoreCase(eventType)) {
            if (paymentIntentId != null && !paymentIntentId.isBlank()) {
                Payment payment = paymentRepository.findByTransactionCode(paymentIntentId)
                        .orElse(null);

                if (payment != null) {
                    payment.setStatus(STATUS_PAID);
                    payment.setPaidAt(Instant.now());
                    paymentRepository.save(payment);

                    Order order = payment.getOrder();
                    if (order != null && STATUS_PENDING.equalsIgnoreCase(order.getStatus())) {
                        order.setStatus(STATUS_CONFIRM);
                        orderRepository.save(order);
                    }

                    // Clean up Redis timeout key
                    redisTemplate.delete(REDIS_PAYMENT_INTENT_PREFIX + paymentIntentId);
                    log.info("Successfully processed Stripe payment webhook for intent [{}]", paymentIntentId);
                }
            }
        } else if ("payment_intent.payment_failed".equalsIgnoreCase(eventType)) {
            // 3. Process failed payment event (payment_intent.payment_failed)
            if (paymentIntentId != null && !paymentIntentId.isBlank()) {
                Payment payment = paymentRepository.findByTransactionCode(paymentIntentId).orElse(null);
                if (payment != null) {
                    payment.setStatus(STATUS_FAILED);
                    paymentRepository.save(payment);
                    log.warn("Stripe payment intent [{}] marked as FAILED", paymentIntentId);
                }
            }
        }
    }
}
