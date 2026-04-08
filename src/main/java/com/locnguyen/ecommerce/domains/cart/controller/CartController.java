package com.locnguyen.ecommerce.domains.cart.controller;

import com.locnguyen.ecommerce.common.constants.AppConstants;
import com.locnguyen.ecommerce.common.response.ApiResponse;
import com.locnguyen.ecommerce.domains.cart.dto.AddCartItemRequest;
import com.locnguyen.ecommerce.domains.cart.dto.CartResponse;
import com.locnguyen.ecommerce.domains.cart.dto.UpdateCartItemRequest;
import com.locnguyen.ecommerce.domains.cart.service.CartService;
import com.locnguyen.ecommerce.domains.customer.entity.Customer;
import com.locnguyen.ecommerce.domains.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Cart", description = "Shopping cart management")
@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_V1 + "/cart")
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    @Operation(summary = "Get my cart")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ApiResponse<CartResponse> getMyCart() {
        return ApiResponse.success(cartService.getMyCart(userService.getCurrentCustomer()));
    }

    @Operation(summary = "Add item to cart")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.success(cartService.addItem(userService.getCurrentCustomer(), request));
    }

    @Operation(summary = "Update cart item quantity")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateItemQuantity(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success(cartService.updateItemQuantity(
                userService.getCurrentCustomer(), itemId, request));
    }

    @Operation(summary = "Remove item from cart")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(@PathVariable Long itemId) {
        return ApiResponse.success(cartService.removeItem(userService.getCurrentCustomer(), itemId));
    }

    @Operation(summary = "Clear all items from cart")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping
    public ApiResponse<Void> clearCart() {
        cartService.clearCart(userService.getCurrentCustomer());
        return ApiResponse.noContent();
    }
}
