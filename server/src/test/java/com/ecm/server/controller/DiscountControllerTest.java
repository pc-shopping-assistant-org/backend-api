package com.ecm.server.controller;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.DiscountFilterRequest;
import com.ecm.server.dto.request.OrderItemValidateDto;
import com.ecm.server.dto.request.ValidateDiscountRequest;
import com.ecm.server.dto.response.DiscountSummaryResponse;
import com.ecm.server.dto.response.DiscountValidationResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.DiscountService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DiscountControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DiscountService discountService;

    @InjectMocks
    private DiscountController discountController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(discountController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getActiveDiscounts_shouldReturnCursorPage() throws Exception {
        UUID discountId = UUID.randomUUID();
        DiscountSummaryResponse summary = DiscountSummaryResponse.builder()
                .id(discountId)
                .code("SUMMER2026")
                .title("Summer Mega Sale")
                .discountType("PERCENT")
                .value(15)
                .status("ACTIVE")
                .startAt(Instant.now().minusSeconds(3600))
                .endAt(Instant.now().plusSeconds(86400))
                .build();

        CursorPageResponse<DiscountSummaryResponse> page = CursorPageResponse.<DiscountSummaryResponse>builder()
                .items(List.of(summary))
                .hasNext(false)
                .size(1)
                .build();

        when(discountService.getActiveDiscounts(any(DiscountFilterRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/discounts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].code").value("SUMMER2026"));
    }

    @Test
    void validateDiscount_whenValid_shouldReturnValidationResponse() throws Exception {
        ValidateDiscountRequest request = ValidateDiscountRequest.builder()
                .code("SUMMER2026")
                .orderAmount(1000000L)
                .build();

        DiscountValidationResponse response = DiscountValidationResponse.builder()
                .isValid(true)
                .discountId(UUID.randomUUID())
                .code("SUMMER2026")
                .title("Summer Mega Sale")
                .discountAmount(150000L)
                .finalAmount(850000L)
                .message("Discount code applied successfully")
                .build();

        when(discountService.validateDiscount(any(ValidateDiscountRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/discounts/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.discountAmount").value(150000))
                .andExpect(jsonPath("$.data.finalAmount").value(850000));
    }

    @Test
    void validateDiscount_whenNestedItemIsInvalid_shouldReturnCanonicalValidationError() throws Exception {
        ValidateDiscountRequest request = ValidateDiscountRequest.builder()
                .code("SUMMER2026")
                .orderAmount(1000000L)
                .items(List.of(OrderItemValidateDto.builder()
                        .quantity(0)
                        .unitPrice(-1L)
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/discounts/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isNotEmpty());

        verify(discountService, never()).validateDiscount(any(ValidateDiscountRequest.class));
    }
}
