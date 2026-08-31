package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.config.security.UserPrincipal;
import com.ecm.server.dto.request.CustomerAddressRequest;
import com.ecm.server.dto.response.CustomerAddressResponse;
import com.ecm.server.service.CustomerAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/addresses")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CustomerAddressController {

    private final CustomerAddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerAddressResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS,
                addressService.list(principal.getAccountId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CustomerAddressRequest request
    ) {
        return ResponseEntity.status(201).body(ApiResponse.success(StatusCode.CREATED,
                addressService.create(principal.getAccountId(), request)));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId,
            @Valid @RequestBody CustomerAddressRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(StatusCode.UPDATED,
                addressService.update(principal.getAccountId(), addressId, request)));
    }

    @PatchMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> setDefault(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId
    ) {
        return ResponseEntity.ok(ApiResponse.success(StatusCode.UPDATED,
                addressService.setDefault(principal.getAccountId(), addressId)));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId
    ) {
        addressService.delete(principal.getAccountId(), addressId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.DELETED, null));
    }
}
