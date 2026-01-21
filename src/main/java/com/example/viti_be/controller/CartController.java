package com.example.viti_be.controller;

import com.example.viti_be.dto.request.AddToCartRequest;
import com.example.viti_be.dto.request.UpdateCartItemRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.CartResponse;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.CartService;
import com.example.viti_be.service.CartTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "APIs quản lý giỏ hàng - hỗ trợ cả guest và logged-in users")
public class CartController {

    private final CartService cartService;
    private final CartTokenService cartTokenService;

    /**
     * Helper method to extract userId from authenticated user (nullable for guests)
     */
    private UUID getUserId(UserDetails userDetails) {
        if (userDetails instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) userDetails).getId();
        }
        return null;
    }

    /**
     * Helper method to extract cart token from request
     */
    private String getCartToken(HttpServletRequest request) {
        return cartTokenService.getCartTokenFromCookie(request).orElse(null);
    }

    /**
     * Lấy giỏ hàng của user hiện tại hoặc guest
     * GET /api/cart
     */
    @GetMapping
    @Operation(summary = "Lấy giỏ hàng", description = "Lấy thông tin giỏ hàng. Hỗ trợ cả guest (via cookie) và logged-in users")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response) {

        UUID userId = getUserId(userDetails);
        String cartToken = getCartToken(request);
        
        CartResponse cart = cartService.getCart(userId, cartToken, response);

        return ResponseEntity.ok(ApiResponse.success(cart, "Lấy giỏ hàng thành công"));
    }

    /**
     * Thêm sản phẩm vào giỏ hàng
     * POST /api/cart/add
     */
    @PostMapping("/add")
    @Operation(summary = "Thêm sản phẩm vào giỏ hàng", 
            description = "Thêm product variant vào giỏ hàng. Hỗ trợ cả guest và logged-in users")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest addRequest,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response) {

        UUID userId = getUserId(userDetails);
        String cartToken = getCartToken(request);
        
        CartResponse cart = cartService.addToCart(userId, cartToken, addRequest, response);

        return ResponseEntity.ok(ApiResponse.success(cart, "Thêm sản phẩm vào giỏ hàng thành công"));
    }

    /**
     * Cập nhật số lượng sản phẩm trong giỏ hàng
     * PUT /api/cart/update
     */
    @PutMapping("/update")
    @Operation(summary = "Cập nhật số lượng sản phẩm", 
            description = "Cập nhật số lượng cụ thể cho một sản phẩm trong giỏ hàng")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @Valid @RequestBody UpdateCartItemRequest updateRequest,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response) {

        UUID userId = getUserId(userDetails);
        String cartToken = getCartToken(request);
        
        CartResponse cart = cartService.updateCartItemQuantity(userId, cartToken, updateRequest, response);

        return ResponseEntity.ok(ApiResponse.success(cart, "Cập nhật số lượng thành công"));
    }

    /**
     * Tăng số lượng sản phẩm lên 1
     * PUT /api/cart/increment/{productVariantId}
     */
    @PutMapping("/increment/{productVariantId}")
    @Operation(summary = "Tăng số lượng +1", 
            description = "Tăng số lượng sản phẩm trong giỏ hàng lên 1")
    public ResponseEntity<ApiResponse<CartResponse>> incrementCartItem(
            @PathVariable UUID productVariantId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response) {

        UUID userId = getUserId(userDetails);
        String cartToken = getCartToken(request);
        
        CartResponse cart = cartService.incrementCartItem(userId, cartToken, productVariantId, response);

        return ResponseEntity.ok(ApiResponse.success(cart, "Tăng số lượng thành công"));
    }

    /**
     * Giảm số lượng sản phẩm đi 1
     * PUT /api/cart/decrement/{productVariantId}
     */
    @PutMapping("/decrement/{productVariantId}")
    @Operation(summary = "Giảm số lượng -1", 
            description = "Giảm số lượng sản phẩm trong giỏ hàng đi 1. Nếu quantity = 1 thì sẽ xóa sản phẩm khỏi giỏ")
    public ResponseEntity<ApiResponse<CartResponse>> decrementCartItem(
            @PathVariable UUID productVariantId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response) {

        UUID userId = getUserId(userDetails);
        String cartToken = getCartToken(request);
        
        CartResponse cart = cartService.decrementCartItem(userId, cartToken, productVariantId, response);

        return ResponseEntity.ok(ApiResponse.success(cart, "Giảm số lượng thành công"));
    }

    /**
     * Xóa sản phẩm khỏi giỏ hàng
     * DELETE /api/cart/remove/{productVariantId}
     */
    @DeleteMapping("/remove/{productVariantId}")
    @Operation(summary = "Xóa sản phẩm khỏi giỏ hàng", 
            description = "Xóa một sản phẩm (product variant) khỏi giỏ hàng")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @PathVariable UUID productVariantId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response) {

        UUID userId = getUserId(userDetails);
        String cartToken = getCartToken(request);
        
        CartResponse cart = cartService.removeFromCart(userId, cartToken, productVariantId, response);

        return ResponseEntity.ok(ApiResponse.success(cart, "Xóa sản phẩm khỏi giỏ hàng thành công"));
    }

    /**
     * Xóa toàn bộ giỏ hàng
     * DELETE /api/cart/clear
     */
    @DeleteMapping("/clear")
    @Operation(summary = "Xóa toàn bộ giỏ hàng", 
            description = "Xóa tất cả sản phẩm trong giỏ hàng")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response) {

        UUID userId = getUserId(userDetails);
        String cartToken = getCartToken(request);
        
        cartService.clearCart(userId, cartToken, response);

        return ResponseEntity.ok(ApiResponse.success(null, "Xóa toàn bộ giỏ hàng thành công"));
    }

    /**
     * Đếm số lượng sản phẩm trong giỏ hàng
     * GET /api/cart/count
     */
    @GetMapping("/count")
    @Operation(summary = "Đếm số lượng sản phẩm", 
            description = "Trả về tổng số lượng sản phẩm trong giỏ hàng")
    public ResponseEntity<ApiResponse<Integer>> getCartItemCount(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {

        UUID userId = getUserId(userDetails);
        String cartToken = getCartToken(request);
        
        Integer count = cartService.getCartItemCount(userId, cartToken);

        return ResponseEntity.ok(ApiResponse.success(count, "Lấy số lượng sản phẩm thành công"));
    }
}
