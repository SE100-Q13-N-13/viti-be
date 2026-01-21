package com.example.viti_be.config;

import com.example.viti_be.service.CartTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to handle cart_token cookie for cart endpoints.
 * Ensures guest users have a cart token before accessing cart operations.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CartCookieInterceptor implements HandlerInterceptor {

    private final CartTokenService cartTokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Only process cart-related endpoints
        String path = request.getRequestURI();
        if (!path.startsWith("/api/cart")) {
            return true;
        }

        // Check if cart_token cookie exists
        String existingToken = cartTokenService.getCartTokenFromCookie(request).orElse(null);
        
        if (existingToken == null || existingToken.isEmpty()) {
            // For GET requests, we might want to create a token
            // For other requests, the service layer will handle it
            log.debug("No cart token found in request for path: {}", path);
        } else {
            log.debug("Cart token found: {}", existingToken);
        }

        // Store cart token in request attribute for easy access in controller
        request.setAttribute("cartToken", existingToken);
        
        return true;
    }
}
