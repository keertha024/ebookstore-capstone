package com.ebookstore.backend.service;

import com.ebookstore.backend.dto.CartItemRequest;
import com.ebookstore.backend.dto.CartItemResponse;
import com.ebookstore.backend.dto.CartResponse;
import com.ebookstore.backend.entity.Cart;
import com.ebookstore.backend.entity.CartItem;
import com.ebookstore.backend.entity.Product;
import com.ebookstore.backend.entity.User;
import com.ebookstore.backend.repository.CartItemRepository;
import com.ebookstore.backend.repository.CartRepository;
import com.ebookstore.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public CartResponse getCart() {
        Cart cart = getOrCreateCart();
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(CartItemRequest request) {
        Cart cart = getOrCreateCart();

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.getProductId()));

        // If the product is already in the cart, just bump the quantity instead of duplicating a row
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(request.getQuantity());
            cart.getItems().add(newItem);
        }

        Cart saved = cartRepository.save(cart);
        return toResponse(saved);
    }

    @Transactional
    public CartResponse updateItemQuantity(Long itemId, Integer quantity) {
        Cart cart = getOrCreateCart();

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + itemId));

        item.setQuantity(quantity);
        Cart saved = cartRepository.save(cart);
        return toResponse(saved);
    }

    @Transactional
    public CartResponse removeItem(Long itemId) {
        Cart cart = getOrCreateCart();

        boolean removed = cart.getItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new IllegalArgumentException("Cart item not found: " + itemId);
        }

        Cart saved = cartRepository.save(cart);
        return toResponse(saved);
    }

    // ----- helpers -----

    private Cart getOrCreateCart() {
        User user = currentUserService.getCurrentUser();

        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> new CartItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getPrice(),
                        item.getQuantity()
                ))
                .toList();

        double total = itemResponses.stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();

        return new CartResponse(cart.getId(), cart.getUser().getId(), itemResponses, total);
    }
}