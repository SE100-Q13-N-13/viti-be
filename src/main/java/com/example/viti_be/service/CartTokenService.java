package com.example.viti_be.service;

import com.example.viti_be.model.Cart;
import com.example.viti_be.model.Customer;
import com.example.viti_be.repository.CartRepository;
import com.example.viti_be.repository.CustomerRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/**
 * Service to handle cart token operations for guest users.
 * Manages cart identification via persistent cookies.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CartTokenService {

    public static final String CART_TOKEN_COOKIE_NAME = "cart_token";
    public static final int CART_TOKEN_MAX_AGE_SECONDS = 30 * 24 * 60 * 60; // 30 days

    private final CartRepository cartRepository;
    private final CustomerRepository customerRepository;

    /**
     * Reads cart_token from request cookies
     */
    public Optional<String> getCartTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (CART_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return Optional.ofNullable(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    /**
     * Creates a new cart_token cookie and adds it to the response
     */
    public String createCartTokenCookie(HttpServletResponse response) {
        String cartToken = UUID.randomUUID().toString();
        addCartTokenCookie(response, cartToken);
        log.info("Created new cart token: {}", cartToken);
        return cartToken;
    }

    /**
     * Adds cart_token cookie to response
     */
    public void addCartTokenCookie(HttpServletResponse response, String cartToken) {
        Cookie cookie = new Cookie(CART_TOKEN_COOKIE_NAME, cartToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(CART_TOKEN_MAX_AGE_SECONDS);
        // SameSite=Lax is set via response header since Cookie class doesn't support it directly
        response.addCookie(cookie);
        // Add SameSite attribute via header
        response.addHeader("Set-Cookie", 
            String.format("%s=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=Lax",
                CART_TOKEN_COOKIE_NAME, cartToken, CART_TOKEN_MAX_AGE_SECONDS));
    }

    /**
     * Clears the cart_token cookie
     */
    public void clearCartTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(CART_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * Resolves cart based on:
     * 1. If logged in (userId provided) -> get cart by customerId
     * 2. If not logged in -> get cart by cartToken
     * 
     * Creates a new cart if none exists.
     */
    @Transactional
    public Cart resolveCart(UUID userId, String cartToken, HttpServletResponse response) {
        // Priority 1: If user is logged in, use customer-based cart
        if (userId != null) {
            Optional<Customer> customerOpt = customerRepository.findByUserIdAndIsDeletedFalse(userId);
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                return getOrCreateCartForCustomer(customer);
            }
        }

        // Priority 2: Use cart token for guest users
        if (cartToken != null && !cartToken.isEmpty()) {
            Optional<Cart> cartOpt = cartRepository.findByCartTokenAndIsDeletedFalse(cartToken);
            if (cartOpt.isPresent()) {
                return cartOpt.get();
            }
        }

        // Priority 3: Create new guest cart with new token
        String newToken = createCartTokenCookie(response);
        return createGuestCart(newToken);
    }

    /**
     * Get or create cart for a logged-in customer
     */
    @Transactional
    public Cart getOrCreateCartForCustomer(Customer customer) {
        return cartRepository.findByCustomerIdAndIsDeletedFalse(customer.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .customer(customer)
                            .items(new ArrayList<>())
                            .build();
                    Cart savedCart = cartRepository.save(newCart);
                    log.info("Created new cart for customer {}", customer.getId());
                    return savedCart;
                });
    }

    /**
     * Creates a new guest cart with the given token
     */
    @Transactional
    public Cart createGuestCart(String cartToken) {
        Cart cart = Cart.builder()
                .cartToken(cartToken)
                .items(new ArrayList<>())
                .build();
        Cart savedCart = cartRepository.save(cart);
        log.info("Created new guest cart with token: {}", cartToken);
        return savedCart;
    }

    /**
     * Gets cart by token only (for guest operations)
     */
    @Transactional
    public Cart getOrCreateCartByToken(String cartToken, HttpServletResponse response) {
        if (cartToken != null && !cartToken.isEmpty()) {
            Optional<Cart> cartOpt = cartRepository.findByCartTokenAndIsDeletedFalse(cartToken);
            if (cartOpt.isPresent()) {
                return cartOpt.get();
            }
        }
        
        // Create new token and cart
        String newToken = (cartToken != null && !cartToken.isEmpty()) ? cartToken : createCartTokenCookie(response);
        return createGuestCart(newToken);
    }

    /**
     * Merge guest cart into customer cart after login.
     * Transfers all items from guest cart to customer cart.
     */
    @Transactional
    public Cart mergeGuestCartToCustomer(String cartToken, Customer customer) {
        if (cartToken == null || cartToken.isEmpty()) {
            return getOrCreateCartForCustomer(customer);
        }

        Optional<Cart> guestCartOpt = cartRepository.findByCartTokenAndIsDeletedFalse(cartToken);
        Cart customerCart = getOrCreateCartForCustomer(customer);

        if (guestCartOpt.isPresent()) {
            Cart guestCart = guestCartOpt.get();
            
            // Merge items from guest cart to customer cart
            guestCart.getItems().forEach(guestItem -> {
                if (!guestItem.getIsDeleted()) {
                    var existingItem = customerCart.findItemByVariantId(guestItem.getProductVariant().getId());
                    if (existingItem != null && !existingItem.getIsDeleted()) {
                        // Item exists, add quantities
                        existingItem.setQuantity(existingItem.getQuantity() + guestItem.getQuantity());
                    } else {
                        // Item doesn't exist, move it
                        guestItem.setCart(customerCart);
                        customerCart.getItems().add(guestItem);
                    }
                }
            });

            // Mark guest cart as deleted
            guestCart.setIsDeleted(true);
            guestCart.clearItems();
            cartRepository.save(guestCart);
            
            log.info("Merged guest cart {} into customer cart {}", cartToken, customer.getId());
        }

        return cartRepository.save(customerCart);
    }
}
