package com.ecm.server.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional logout payload.  The access token is carried by the Authorization
 * header; sending the refresh token lets the server revoke the whole token
 * pair instead of allowing a new access token to be minted after logout.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {

    @Size(max = 4096, message = "Refresh token is too long")
    private String refreshToken;
}
