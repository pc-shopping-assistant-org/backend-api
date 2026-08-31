package com.ecm.server.dto.request;

import jakarta.validation.constraints.*;
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
public class CreateEmployeeRequest {

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    private String password;

    @NotNull(message = "Role ID is required")
    private UUID roleId;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Invalid phone number format")
    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "^(?i)(MALE|FEMALE)$", message = "Gender must be MALE or FEMALE")
    private String gender;

    private LocalDate birthday;

    @PositiveOrZero(message = "Salary must be greater than or equal to zero")
    @Builder.Default
    private Long salary = 0L;

    private LocalDate joinedAt;

    private String address;

    private UUID avatarFileId;
}
