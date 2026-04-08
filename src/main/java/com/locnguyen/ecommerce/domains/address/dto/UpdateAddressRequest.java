package com.locnguyen.ecommerce.domains.address.dto;

import com.locnguyen.ecommerce.common.validation.PhoneNumber;
import com.locnguyen.ecommerce.domains.address.enums.AddressType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Update address request — all fields are optional.
 * Only provided fields will be updated; null fields are ignored.
 */
@Data
@Schema(description = "Update address request — only provided fields are updated")
public class UpdateAddressRequest {

    @Size(max = 100, message = "Receiver name must not exceed 100 characters")
    @Schema(example = "Nguyen Van Loc")
    private String receiverName;

    @PhoneNumber
    @Schema(example = "0912345678")
    private String phoneNumber;

    @Size(max = 255, message = "Street address must not exceed 255 characters")
    @Schema(example = "456 Le Loi, Ben Thanh")
    private String streetAddress;

    @Size(max = 100, message = "Ward must not exceed 100 characters")
    @Schema(example = "Phuong Ben Thanh")
    private String ward;

    @Size(max = 100, message = "District must not exceed 100 characters")
    @Schema(example = "Quan 1")
    private String district;

    @Size(max = 100, message = "City must not exceed 100 characters")
    @Schema(example = "TP. Ho Chi Minh")
    private String city;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    @Schema(example = "700000")
    private String postalCode;

    @Schema(description = "Address usage type", example = "SHIPPING")
    private AddressType addressType;

    @Schema(description = "Set as default address", example = "true")
    private Boolean isDefault;

    @Size(max = 50, message = "Label must not exceed 50 characters")
    @Schema(description = "Address label", example = "Office")
    private String label;
}
