package com.ecm.server.service;

import com.ecm.server.dto.request.ChangePasswordRequest;
import com.ecm.server.dto.request.UpdateProfileRequest;
import com.ecm.server.dto.response.UserProfileResponse;

import java.util.UUID;

public interface UserProfileService {

    UserProfileResponse getProfile(UUID accountId);

    UserProfileResponse updateProfile(UUID accountId, UpdateProfileRequest request);

    void changePassword(UUID accountId, ChangePasswordRequest request);

    void requestChangePasswordOtp(UUID accountId);
}
