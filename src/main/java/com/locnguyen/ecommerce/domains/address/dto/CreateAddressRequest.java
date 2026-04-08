package com.locnguyen.ecommerce.domains.address.dto;

import com.locnguyen.ecommerce.common.validation.PhoneNumber;
import com.locnguyen.ecommerce.domains.address.enums.AddressType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Create address request")
public class CreateAddressRequest {

    @NotBlank(message = "Receiver name is required")
    @Size(max = 100, message = "Receiver name must not exceed 100 characters")
    @Schema(example = "Nguyen Van Loc", requiredMode = Schema.RequiredMode.REQUIRED)
    private String receiverName;

    @NotBlank(message = "Phone number is required")
    @PhoneNumber
    @Schema(example = "0912345678", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;

    @NotBlank(message = "Street address is required")
    @Size(max = 255, message = "Street address must not exceed 255 characters")
    @Schema(example = "123 Nguyen Hue, Ben Nghe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String streetAddress;

    @NotBlank(message = "Ward is required")
    @Size(max = 100, message = "Ward must not exceed 100 characters")
    @Schema(example = "Phuong Ben Nghe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ward;

    @NotBlank(message = "District is required")
    @Size(max = 100, message = "District must not exceed 100 characters")
    @Schema(example = "Quan 1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String district;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @Schema(example = "TP. Ho Chi Minh", requiredMode = Schema.RequiredMode.REQUIRED)
    private String city;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    @Schema(example = "700000")
    private String postalCode;

    @NotNull(message = "Address type is required")
    @Schema(description = "Address usage type", example = "SHIPPING", requiredMode = Schema.RequiredMode.REQUIRED)
    private AddressType addressType;

    @Schema(description = "Set as default address", example = "false")
    private Boolean isDefault;

    @Size(max = 50, message = "Label must not exceed 50 characters")
    @Schema(description = "Address label (e.g., Home, Office)", example = "Home")
    private String label;
}
