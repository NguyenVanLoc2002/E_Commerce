package com.locnguyen.ecommerce.domains.address.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.locnguyen.ecommerce.domains.address.enums.AddressType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Address response DTO — never exposes the customer foreign key.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Address detail")
public class AddressResponse {

    @Schema(description = "Address ID")
    private final Long id;

    @Schema(description = "Receiver name")
    private final String receiverName;

    @Schema(description = "Phone number")
    private final String phoneNumber;

    @Schema(description = "Street address")
    private final String streetAddress;

    @Schema(description = "Ward")
    private final String ward;

    @Schema(description = "District")
    private final String district;

    @Schema(description = "City / Province")
    private final String city;

    @Schema(description = "Postal code")
    private final String postalCode;

    @Schema(description = "Address type", example = "SHIPPING")
    private final AddressType addressType;

    @Schema(description = "Is this the default address?")
    private final boolean isDefault;

    @Schema(description = "Address label", example = "Home")
    private final String label;

    @Schema(description = "Full address string (computed)", example = "123 Nguyen Hue, Ben Nghe, Quan 1, TP. Ho Chi Minh")
    private final String fullAddress;

    @Schema(description = "Created timestamp")
    private final LocalDateTime createdAt;
}
