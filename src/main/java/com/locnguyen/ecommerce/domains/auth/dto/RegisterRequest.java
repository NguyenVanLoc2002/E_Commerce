package com.locnguyen.ecommerce.domains.auth.dto;

import com.locnguyen.ecommerce.common.validation.PhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Customer registration request")
public class
RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(example = "customer@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit")
    @Schema(example = "Password123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Schema(example = "Nguyen", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Schema(example = "Van Loc")
    private String lastName;

    @PhoneNumber
    @Schema(example = "0912345678")
    private String phoneNumber;
}
