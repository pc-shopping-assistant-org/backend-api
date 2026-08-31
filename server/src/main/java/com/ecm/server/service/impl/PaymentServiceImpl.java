package com.ecm.server.service.impl;

import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreatePaymentIntentRequest;
import com.ecm.server.dto.response.PaymentDetailResponse;
import com.ecm.server.dto.response.PaymentIntentResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.PaymentMapper;
import com.ecm.server.model.Order;
import com.ecm.server.model.Payment;
import com.ecm.server.model.PaymentMethod;
import com.ecm.server.repository.OrderRepository;
import com.ecm.server.repository.PaymentMethodRepository;
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
import java.util.LinkedHashSet;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    public static final String METHOD_COD = "COD";
    public static final String METHOD_STRIPE_CARD = "STRIPE_CARD";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String STATUS_PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    public static final String REDIS_PAYMENT_INTENT_PREFIX = "payment_intent:";
    public static final long PAYMENT_INTENT_TTL_MINUTES = 15;
    public static final String DEFAULT_CURRENCY = "vnd";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
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
        Order order = orderRepository.findByIdWithDetails(request.getOrderId())
                .orElseThrow(() -> new BusinessException(StatusCode.ORDER_NOT_FOUND));
        assertOrderOwner(order, accountId);

        String methodCode = normalizePaymentCode(request.getPaymentMethod());
        PaymentMethod method = paymentMethodRepository.findByCodeIgnoreCaseAndStatus(methodCode, "ACTIVE")
                .orElseThrow(() -> new BusinessException(StatusCode.BAD_REQUEST, "Payment method is not available"));
        if (METHOD_COD.equalsIgnoreCase(methodCode)) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "COD does not require a payment intent");
        }
        if (!STATUS_PENDING_PAYMENT.equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException(StatusCode.INVALID_ORDER_STATE_TRANSITION,
                    "Only PENDING_PAYMENT orders can create an online payment attempt");
        }

        String transactionCode;
        String clientSecret;
        if (stripeSecretKey != null && !stripeSecretKey.isBlank()) {
            try {
                Stripe.apiKey = stripeSecretKey;
                PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                        .setAmount(order.getTotalAmount())
                        .setCurrency(DEFAULT_CURRENCY)
                        .setDescription("Order Payment #" + order.getId())
                        .putMetadata("orderId", order.getId().toString())
                        .putMetadata("customerId", order.getCustomer().getAccountId().toString())
                        .build();
                PaymentIntent intent = PaymentIntent.create(params);
                transactionCode = intent.getId();
                clientSecret = intent.getClientSecret();
            } catch (Exception ex) {
                throw new BusinessException(StatusCode.EXTERNAL_SERVICE_ERROR, "Stripe service error");
            }
        } else {
            transactionCode = "pi_mock_" + UUID.randomUUID();
            clientSecret = transactionCode + "_secret_" + UUID.randomUUID();
        }

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(method)
                .amount(order.getTotalAmount())
                .providerTransactionCode(transactionCode)
                .status(STATUS_PENDING)
                .build();
        Payment savedPayment = paymentRepository.save(payment);
        redisTemplate.opsForValue().set(REDIS_PAYMENT_INTENT_PREFIX + transactionCode,
                order.getId().toString(), Duration.ofMinutes(PAYMENT_INTENT_TTL_MINUTES));

        return PaymentIntentResponse.builder()
                .paymentId(savedPayment.getId())
                .orderId(order.getId())
                .clientSecret(clientSecret)
                .amount(order.getTotalAmount())
                .currency(DEFAULT_CURRENCY.toUpperCase())
                .publishableKey(stripePublishableKey == null ? "" : stripePublishableKey)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDetailResponse getPaymentByOrderId(UUID accountId, UUID orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new BusinessException(StatusCode.ORDER_NOT_FOUND));
        assertOrderOwner(order, accountId);
        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "No payment attempt found for order"));
        return paymentMapper.toDetailResponse(payment);
    }

    @Override
    @Transactional
    public PaymentDetailResponse confirmCodPayment(UUID accountId, UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "Payment attempt not found"));
        Order order = payment.getOrder();
        if (order == null) {
            throw new BusinessException(StatusCode.ORDER_NOT_FOUND);
        }
        assertOrderOwner(order, accountId);
        if (payment.getPaymentMethod() == null || !METHOD_COD.equalsIgnoreCase(payment.getPaymentMethod().getCode())) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Payment attempt is not COD");
        }
        if (STATUS_PAID.equalsIgnoreCase(payment.getStatus())) {
            return paymentMapper.toDetailResponse(payment);
        }
        // COD is collected on delivery. A customer cannot mark a pending or
        // confirmed COD order as paid before the shipment reaches SHIPPING.
        if (!"SHIPPING".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException(StatusCode.INVALID_ORDER_STATE_TRANSITION,
                    "COD payment can be confirmed only while the order is SHIPPING");
        }
        payment.setStatus(STATUS_PAID);
        payment.setPaidAt(Instant.now());
        Payment saved = paymentRepository.save(payment);
        order.setStatus("COMPLETED");
        order.setDeliveredAt(Instant.now());
        orderRepository.save(order);
        return paymentMapper.toDetailResponse(saved);
    }

    @Override
    @Transactional
    public void handleStripeWebhook(String payload, String signatureHeader) {
        String eventType;
        String paymentIntentId;
        if (stripeWebhookSecret != null && !stripeWebhookSecret.isBlank()) {
            if (signatureHeader == null || signatureHeader.isBlank()) {
                throw new BusinessException(StatusCode.BAD_REQUEST, "Webhook signature is required");
            }
            try {
                Event event = Webhook.constructEvent(payload, signatureHeader, stripeWebhookSecret);
                eventType = event.getType();
                paymentIntentId = event.getDataObjectDeserializer().getObject()
                        .filter(PaymentIntent.class::isInstance)
                        .map(PaymentIntent.class::cast)
                        .map(PaymentIntent::getId)
                        .orElse(null);
            } catch (Exception ex) {
                throw new BusinessException(StatusCode.BAD_REQUEST, "Webhook signature verification failed");
            }
        } else {
            try {
                JsonNode root = objectMapper.readTree(payload);
                eventType = root.path("type").asText(null);
                paymentIntentId = root.path("data").path("object").path("id").asText(null);
            } catch (Exception ex) {
                throw new BusinessException(StatusCode.MALFORMED_JSON);
            }
        }

        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return;
        }
        Payment payment = paymentRepository.findByProviderTransactionCode(paymentIntentId).orElse(null);
        if (payment == null) {
            return;
        }
        if ("payment_intent.succeeded".equalsIgnoreCase(eventType)) {
            // Stripe may deliver the same event more than once.  A successful
            // attempt is terminal, so duplicate success notifications are
            // idempotent while a previously failed attempt cannot be reopened.
            if (STATUS_PAID.equalsIgnoreCase(payment.getStatus())) {
                redisTemplate.delete(REDIS_PAYMENT_INTENT_PREFIX + paymentIntentId);
                return;
            }
            // An intent can settle after the customer or an administrator has
            // cancelled the order.  The cancelled order is terminal in the
            // order state machine; do not create a paid payment that no longer
            // belongs to a fulfillable order.  Clearing the intent key keeps
            // Stripe retries idempotent while the provider reconciliation path
            // can handle any external refund separately.
            if (payment.getOrder() != null
                    && "CANCELLED".equalsIgnoreCase(payment.getOrder().getStatus())) {
                redisTemplate.delete(REDIS_PAYMENT_INTENT_PREFIX + paymentIntentId);
                log.warn("Ignoring successful payment intent [{}] for cancelled order [{}]",
                        paymentIntentId, payment.getOrder().getId());
                return;
            }
            if (!STATUS_PENDING.equalsIgnoreCase(payment.getStatus())) {
                throw new BusinessException(StatusCode.INVALID_ORDER_STATE_TRANSITION,
                        "Payment attempt status is terminal and cannot be marked PAID");
            }
            boolean anotherPaidAttempt = paymentRepository.findByOrderId(payment.getOrder().getId()).stream()
                    .anyMatch(other -> !other.getId().equals(payment.getId()) && STATUS_PAID.equalsIgnoreCase(other.getStatus()));
            if (anotherPaidAttempt) {
                throw new BusinessException(StatusCode.CONFLICT,
                        "An order can have at most one PAID payment attempt");
            }
            payment.setStatus(STATUS_PAID);
            payment.setPaidAt(Instant.now());
            paymentRepository.save(payment);
            Order order = payment.getOrder();
            if (order != null && STATUS_PENDING_PAYMENT.equalsIgnoreCase(order.getStatus())) {
                order.setStatus(STATUS_PENDING_CONFIRMATION);
                orderRepository.save(order);
            }
            redisTemplate.delete(REDIS_PAYMENT_INTENT_PREFIX + paymentIntentId);
        } else if ("payment_intent.payment_failed".equalsIgnoreCase(eventType)) {
            // Do not downgrade a paid attempt if a delayed failure event is
            // delivered after the success event; repeated failures are also
            // harmless and keep the attempt terminal.
            if (STATUS_PAID.equalsIgnoreCase(payment.getStatus())
                    || STATUS_FAILED.equalsIgnoreCase(payment.getStatus())) {
                return;
            }
            if (!STATUS_PENDING.equalsIgnoreCase(payment.getStatus())) {
                throw new BusinessException(StatusCode.INVALID_ORDER_STATE_TRANSITION,
                        "Payment attempt status is terminal and cannot be marked FAILED");
            }
            payment.setStatus(STATUS_FAILED);
            payment.setPaidAt(null);
            paymentRepository.save(payment);
        }
    }

    private void assertOrderOwner(Order order, UUID accountId) {
        if (order.getCustomer() == null || !accountId.equals(order.getCustomer().getAccountId())) {
            throw new BusinessException(StatusCode.FORBIDDEN, "You do not have permission to access this order");
        }
    }

    private String normalizePaymentCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }
}
