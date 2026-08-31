package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.config.security.UserPrincipal;
import com.ecm.server.dto.request.ChangePasswordRequest;
import com.ecm.server.dto.request.UpdateProfileRequest;
import com.ecm.server.dto.response.UserProfileResponse;
import com.ecm.server.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/profile")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        UserProfileResponse response = userProfileService.getProfile(userPrincipal.getAccountId());
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserProfileResponse response = userProfileService.updateProfile(userPrincipal.getAccountId(), request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.UPDATED, response));
    }

    @PatchMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userProfileService.changePassword(userPrincipal.getAccountId(), request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, "Password changed successfully."));
    }

    @PostMapping("/change-password/otp")
    public ResponseEntity<ApiResponse<String>> requestChangePasswordOtp(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        userProfileService.requestChangePasswordOtp(principal.getAccountId());
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, "Password change OTP sent to email."));
    }
}
