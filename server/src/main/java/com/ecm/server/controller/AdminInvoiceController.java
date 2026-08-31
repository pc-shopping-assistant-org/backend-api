package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.InvoiceFilterRequest;
import com.ecm.server.dto.response.InvoiceResponse;
import com.ecm.server.service.AdminOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/admin/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE', 'ROLE_MANAGER')")
public class AdminInvoiceController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<InvoiceResponse>>> getInvoices(
            @Valid @ModelAttribute InvoiceFilterRequest filter
    ) {
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS,
                adminOrderService.getInvoices(filter)));
    }
}
