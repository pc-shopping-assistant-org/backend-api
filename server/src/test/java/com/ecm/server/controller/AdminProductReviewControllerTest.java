package com.ecm.server.controller;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.UpdateStatusRequest;
import com.ecm.server.dto.response.ReviewResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.AdminProductReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminProductReviewControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AdminProductReviewService adminProductReviewService;

    @InjectMocks
    private AdminProductReviewController adminProductReviewController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminProductReviewController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAdminReviews_shouldReturnCursorPage() throws Exception {
        ReviewResponse review = ReviewResponse.builder()
                .id(UUID.randomUUID())
                .productName("iPhone 16 Pro Max")
                .customerName("Nguyen Van A")
                .rating(5)
                .status("ACTIVE")
                .build();

        CursorPageResponse<ReviewResponse> pageResponse = CursorPageResponse.<ReviewResponse>builder()
                .items(List.of(review))
                .hasNext(false)
                .build();

        when(adminProductReviewService.getAdminReviews(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"));
    }

    @Test
    void updateReviewStatus_shouldReturnUpdatedReview() throws Exception {
        UUID reviewId = UUID.randomUUID();
        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .status("INACTIVE")
                .reason("Inappropriate language")
                .build();

        ReviewResponse response = ReviewResponse.builder()
                .id(reviewId)
                .status("INACTIVE")
                .build();

        when(adminProductReviewService.updateReviewStatus(eq(reviewId), any(UpdateStatusRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/reviews/{id}/status", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }
}
