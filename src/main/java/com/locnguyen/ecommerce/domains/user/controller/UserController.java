package com.locnguyen.ecommerce.domains.user.controller;

import com.locnguyen.ecommerce.common.constants.AppConstants;
import com.locnguyen.ecommerce.common.response.ApiResponse;
import com.locnguyen.ecommerce.domains.user.dto.UpdateProfileRequest;
import com.locnguyen.ecommerce.domains.user.dto.UserProfileResponse;
import com.locnguyen.ecommerce.domains.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Profile", description = "Current user profile management")
@RestController
@RequestMapping(AppConstants.API_V1 + "/me")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Get current user profile",
            description = "Returns combined auth identity and customer profile data " +
                    "for the currently authenticated user."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Profile retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = com.locnguyen.ecommerce.common.response.ErrorResponse.class)))
    })
    @GetMapping
    public ApiResponse<UserProfileResponse> getMyProfile() {
        return ApiResponse.success(userService.getMyProfile());
    }

    @Operation(
            summary = "Update current user profile",
            description = "Partially updates the user's profile. Only provided fields are applied; " +
                    "null fields are ignored. Updates both User (name, phone) and Customer (gender, birthDate) data."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Profile updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = com.locnguyen.ecommerce.common.response.ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Phone number already registered",
                    content = @Content(schema = @Schema(implementation = com.locnguyen.ecommerce.common.response.ErrorResponse.class)))
    })
    @PatchMapping
    public ApiResponse<UserProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateMyProfile(request));
    }
}
