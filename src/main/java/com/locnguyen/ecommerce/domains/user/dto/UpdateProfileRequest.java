package com.locnguyen.ecommerce.domains.user.dto;

import com.locnguyen.ecommerce.common.validation.PhoneNumber;
import com.locnguyen.ecommerce.domains.customer.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Update profile request — all fields are optional.
 * Only provided fields will be updated; null fields are ignored.
 */
@Data
@Schema(description = "Update user profile request — only provided fields are updated")
public class UpdateProfileRequest {

    // ─── User fields ─────────────────────────────────────────────────────────

    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Schema(example = "Nguyen")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Schema(example = "Van Loc")
    private String lastName;

    @PhoneNumber
    @Schema(example = "0912345678")
    private String phoneNumber;

    // ─── Customer fields ─────────────────────────────────────────────────────

    @Schema(description = "Gender", example = "MALE")
    private Gender gender;

    @Schema(description = "Date of birth", example = "2000-01-15")
    private LocalDate birthDate;
}
