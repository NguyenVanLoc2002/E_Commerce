package com.locnguyen.ecommerce.domains.address.controller;

import com.locnguyen.ecommerce.common.constants.AppConstants;
import com.locnguyen.ecommerce.common.response.ApiResponse;
import com.locnguyen.ecommerce.domains.address.dto.AddressResponse;
import com.locnguyen.ecommerce.domains.address.dto.CreateAddressRequest;
import com.locnguyen.ecommerce.domains.address.dto.UpdateAddressRequest;
import com.locnguyen.ecommerce.domains.address.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.UUID;
@Tag(name = "Address", description = "Customer address management")
@RestController
@RequestMapping(AppConstants.API_V1 + "/addresses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final AddressService addressService;

    @Operation(
            summary = "List current user's addresses",
            description = "Returns all addresses for the authenticated user, " +
                    "sorted by default first then by newest."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Addresses retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = com.locnguyen.ecommerce.common.response.ErrorResponse.class)))
    })
    @GetMapping
    public ApiResponse<List<AddressResponse>> getMyAddresses() {
        return ApiResponse.success(addressService.getMyAddresses());
    }

    @Operation(
            summary = "Get address by ID",
            description = "Returns a specific address. Fails with 404 if the address " +
                    "doesn't exist or doesn't belong to the current user."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Address retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Address not found",
                    content = @Content(schema = @Schema(implementation = com.locnguyen.ecommerce.common.response.ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ApiResponse<AddressResponse> getAddress(@PathVariable UUID id) {
        return ApiResponse.success(addressService.getAddressById(id));
    }

    @Operation(
            summary = "Create a new address",
            description = "Creates an address for the authenticated user. " +
                    "If isDefault is true, any existing default is cleared."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Address created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = com.locnguyen.ecommerce.common.response.ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = com.locnguyen.ecommerce.common.response.ErrorResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AddressResponse> createAddress(
            @Valid @RequestBody CreateAddressRequest request) {
        return ApiResponse.created(addressService.createAddress(request));
    }

    @Operation(
            summary = "Update an address",
            description = "Partially updates an address. Only provided fields are applied. " +
                    "If isDefault is set to true, clears any existing default first."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Address updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Address not found",
                    content = @Content(schema = @Schema(implementation = com.locnguyen.ecommerce.common.response.ErrorResponse.class)))
    })
    @PatchMapping("/{id}")
    public ApiResponse<AddressResponse> updateAddress(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAddressRequest request) {
        return ApiResponse.success(addressService.updateAddress(id, request));
    }

    @Operation(
            summary = "Delete an address",
            description = "Soft-deletes an address. The address is marked as deleted but " +
                    "remains in the database for order history reference."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Address deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Address not found",
                    content = @Content(schema = @Schema(implementation = com.locnguyen.ecommerce.common.response.ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAddress(@PathVariable UUID id) {
        addressService.deleteAddress(id);
        return ApiResponse.noContent();
    }
}
