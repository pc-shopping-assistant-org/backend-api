package com.ecm.server.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {

    @Email(message = "Invalid email format")
    private String email;

    /** Optional phone identifier; OTP is still delivered to the account email. */
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Phone number must be a valid Vietnamese phone number")
    private String phone;

    @JsonIgnore
    @AssertTrue(message = "Email or phone is required")
    public boolean hasIdentifier() {
        return (email != null && !email.isBlank()) || (phone != null && !phone.isBlank());
    }

    @JsonIgnore
    public String getLoginIdentifier() {
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        return phone == null ? null : phone.trim();
    }
}
