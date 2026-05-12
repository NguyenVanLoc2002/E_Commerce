package com.locnguyen.ecommerce.domains.cart.service;

import com.locnguyen.ecommerce.domains.cart.dto.AddCartItemRequest;
import com.locnguyen.ecommerce.domains.cart.dto.CartResponse;
import com.locnguyen.ecommerce.domains.cart.dto.UpdateCartItemRequest;
import com.locnguyen.ecommerce.domains.cart.entity.Cart;
import com.locnguyen.ecommerce.domains.customer.entity.Customer;

import java.util.UUID;

public interface CartService {

    Cart getOrCreateCart(Customer customer);

    CartResponse getMyCart(Customer customer);

    CartResponse addItem(Customer customer, AddCartItemRequest request);

    CartResponse updateItemQuantity(Customer customer, UUID itemId, UpdateCartItemRequest request);

    CartResponse removeItem(Customer customer, UUID itemId);

    void clearCart(Customer customer);
}
