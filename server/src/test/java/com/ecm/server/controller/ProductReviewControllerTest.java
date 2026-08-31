package com.ecm.server.controller;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.config.security.UserPrincipal;
import com.ecm.server.dto.request.CreateReviewRequest;
import com.ecm.server.dto.request.UpdateReviewRequest;
import com.ecm.server.dto.response.ProductRatingSummaryResponse;
import com.ecm.server.dto.response.ReviewResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.ProductReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductReviewControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID testAccountId = UUID.randomUUID();

    @Mock
    private ProductReviewService productReviewService;

    @InjectMocks
    private ProductReviewController productReviewController;

    @BeforeEach
    void setUp() {
        HandlerMethodArgumentResolver authResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return UserPrincipal.builder()
                        .accountId(testAccountId)
                        .username("customer@example.com")
                        .role("ROLE_CUSTOMER")
                        .build();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(productReviewController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authResolver)
                .build();
    }

    @Test
    void getProductReviews_shouldReturnCursorPage() throws Exception {
        UUID productId = UUID.randomUUID();
        ReviewResponse review = ReviewResponse.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .productName("iPhone 16 Pro Max")
                .customerId(UUID.randomUUID())
                .customerName("Nguyen Van A")
                .rating(5)
                .comment("Excellent phone!")
                .isVerifiedPurchase(true)
                .status("ACTIVE")
                .build();

        CursorPageResponse<ReviewResponse> pageResponse = CursorPageResponse.<ReviewResponse>builder()
                .items(List.of(review))
                .hasNext(false)
                .build();

        when(productReviewService.getProductReviews(eq(productId), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/products/{productId}/reviews", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].rating").value(5))
                .andExpect(jsonPath("$.data.items[0].isVerifiedPurchase").value(true));
    }

    @Test
    void getProductRatingSummary_shouldReturnSummary() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductRatingSummaryResponse summary = ProductRatingSummaryResponse.builder()
                .productId(productId)
                .averageRating(4.8)
                .totalReviews(25L)
                .ratingDistribution(Map.of(5, 20L, 4, 5L))
                .build();

        when(productReviewService.getProductRatingSummary(productId)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/products/{productId}/reviews/summary", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.averageRating").value(4.8))
                .andExpect(jsonPath("$.data.totalReviews").value(25));
    }

    @Test
    void createReview_shouldReturnCreatedReview() throws Exception {
        UUID productId = UUID.randomUUID();
        CreateReviewRequest request = CreateReviewRequest.builder()
                .orderItemId(UUID.randomUUID())
                .rating(5)
                .comment("Great battery life!")
                .build();

        ReviewResponse response = ReviewResponse.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .rating(5)
                .comment("Great battery life!")
                .isVerifiedPurchase(true)
                .status("ACTIVE")
                .build();

        when(productReviewService.createReview(eq(testAccountId), eq(productId), any(CreateReviewRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/products/{productId}/reviews", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("CREATED"))
                .andExpect(jsonPath("$.data.rating").value(5));
    }

    @Test
    void updateReview_shouldReturnUpdatedReview() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UpdateReviewRequest request = UpdateReviewRequest.builder()
                .rating(4)
                .comment("Updated: Camera is decent")
                .build();

        ReviewResponse response = ReviewResponse.builder()
                .id(reviewId)
                .productId(productId)
                .rating(4)
                .comment("Updated: Camera is decent")
                .isVerifiedPurchase(true)
                .status("ACTIVE")
                .build();

        when(productReviewService.updateReview(eq(testAccountId), eq(productId), eq(reviewId), any(UpdateReviewRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/products/{productId}/reviews/{reviewId}", productId, reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("UPDATED"))
                .andExpect(jsonPath("$.data.rating").value(4));
    }

    @Test
    void deleteReview_shouldReturnSuccess() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        doNothing().when(productReviewService).deleteReview(testAccountId, productId, reviewId);

        mockMvc.perform(delete("/api/v1/products/{productId}/reviews/{reviewId}", productId, reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("DELETED"));
    }
}
