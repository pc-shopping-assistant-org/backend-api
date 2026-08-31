package com.ecm.server.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
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
public class UpdateEmployeeRequest {

    @NotNull(message = "Role ID is required")
    private UUID roleId;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Invalid phone number format")
    private String phone;

    @Pattern(regexp = "^(?i)(MALE|FEMALE)$", message = "Gender must be MALE or FEMALE")
    private String gender;

    private LocalDate birthday;

    @PositiveOrZero(message = "Salary must be greater than or equal to zero")
    private Long salary;

    private LocalDate joinedAt;

    private String address;

    private UUID avatarFileId;
}
