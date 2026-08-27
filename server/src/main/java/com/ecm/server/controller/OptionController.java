package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreateOptionRequest;
import com.ecm.server.dto.request.UpdateOptionRequest;
import com.ecm.server.dto.response.OptionResponse;
import com.ecm.server.service.OptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OptionController {

    private final OptionService optionService;

    // Public Endpoints
    @GetMapping("/api/v1/options")
    public ResponseEntity<ApiResponse<List<OptionResponse>>> getOptions(@RequestParam(required = false) String type) {
        List<OptionResponse> response = optionService.getOptions(type);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/api/v1/options/{id}")
    public ResponseEntity<ApiResponse<OptionResponse>> getOptionById(@PathVariable UUID id) {
        OptionResponse response = optionService.getOptionById(id);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    // Admin Endpoints
    @PostMapping("/api/v1/admin/options")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<OptionResponse>> createOption(@Valid @RequestBody CreateOptionRequest request) {
        OptionResponse response = optionService.createOption(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(StatusCode.CREATED, response));
    }

    @PutMapping("/api/v1/admin/options/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<OptionResponse>> updateOption(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOptionRequest request
    ) {
        OptionResponse response = optionService.updateOption(id, request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.UPDATED, response));
    }

    @DeleteMapping("/api/v1/admin/options/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<String>> deleteOption(@PathVariable UUID id) {
        optionService.deleteOption(id);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, "Option deleted successfully."));
    }
}
