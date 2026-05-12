package com.locnguyen.ecommerce.domains.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 6, max = 6)
    private String otp;
}
