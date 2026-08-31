package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @Email(message = "Email must be a valid email address")
    private String email;

    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Phone number must be a valid Vietnamese phone number")
    private String phone;

    @Pattern(
            regexp = "^(?i)(MALE|FEMALE|OTHER)$",
            message = "Gender must be MALE, FEMALE, or OTHER"
    )
    private String gender;

    private LocalDate birthday;

    private String address;

    private UUID avatarFileId;
}
