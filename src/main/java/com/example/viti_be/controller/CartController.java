package com.example.viti_be.controller;

import com.example.viti_be.dto.request.AddToCartRequest;
import com.example.viti_be.dto.request.UpdateCartItemRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.CartResponse;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "APIs quản lý giỏ hàng")
public class CartController {

    private final CartService cartService;

    /**
     * Lấy giỏ hàng của user hiện tại
     * GET /api/cart
     */
    @GetMapping
    @Operation(summary = "Lấy giỏ hàng", description = "Lấy thông tin giỏ hàng của user hiện tại, bao gồm danh sách sản phẩm và thông tin chi tiết")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        CartResponse cart = cartService.getCart(userImpl.getId());

        return ResponseEntity.ok(ApiResponse.success(cart, "Lấy giỏ hàng thành công"));
    }

    /**
     * Thêm sản phẩm vào giỏ hàng
     * POST /api/cart/add
     */
    @PostMapping("/add")
    @Operation(summary = "Thêm sản phẩm vào giỏ hàng", 
            description = "Thêm product variant vào giỏ hàng. Nếu đã có thì tăng quantity lên 1, nếu chưa có thì thêm mới với quantity = 1")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        CartResponse cart = cartService.addToCart(userImpl.getId(), request);

        return ResponseEntity.ok(ApiResponse.success(cart, "Thêm sản phẩm vào giỏ hàng thành công"));
    }

    /**
     * Cập nhật số lượng sản phẩm trong giỏ hàng
     * PUT /api/cart/update
     */
    @PutMapping("/update")
    @Operation(summary = "Cập nhật số lượng sản phẩm", 
            description = "Cập nhật số lượng cụ thể cho một sản phẩm trong giỏ hàng")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        CartResponse cart = cartService.updateCartItemQuantity(userImpl.getId(), request);

        return ResponseEntity.ok(ApiResponse.success(cart, "Cập nhật số lượng thành công"));
    }

    /**
     * Tăng số lượng sản phẩm lên 1
     * PUT /api/cart/increment/{productVariantId}
     */
    @PutMapping("/increment/{productVariantId}")
    @Operation(summary = "Tăng số lượng +1", 
            description = "Tăng số lượng sản phẩm trong giỏ hàng lên 1")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> incrementCartItem(
            @PathVariable UUID productVariantId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        CartResponse cart = cartService.incrementCartItem(userImpl.getId(), productVariantId);

        return ResponseEntity.ok(ApiResponse.success(cart, "Tăng số lượng thành công"));
    }

    /**
     * Giảm số lượng sản phẩm đi 1
     * PUT /api/cart/decrement/{productVariantId}
     */
    @PutMapping("/decrement/{productVariantId}")
    @Operation(summary = "Giảm số lượng -1", 
            description = "Giảm số lượng sản phẩm trong giỏ hàng đi 1. Nếu quantity = 1 thì sẽ xóa sản phẩm khỏi giỏ")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> decrementCartItem(
            @PathVariable UUID productVariantId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        CartResponse cart = cartService.decrementCartItem(userImpl.getId(), productVariantId);

        return ResponseEntity.ok(ApiResponse.success(cart, "Giảm số lượng thành công"));
    }

    /**
     * Xóa sản phẩm khỏi giỏ hàng
     * DELETE /api/cart/remove/{productVariantId}
     */
    @DeleteMapping("/remove/{productVariantId}")
    @Operation(summary = "Xóa sản phẩm khỏi giỏ hàng", 
            description = "Xóa một sản phẩm (product variant) khỏi giỏ hàng")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @PathVariable UUID productVariantId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        CartResponse cart = cartService.removeFromCart(userImpl.getId(), productVariantId);

        return ResponseEntity.ok(ApiResponse.success(cart, "Xóa sản phẩm khỏi giỏ hàng thành công"));
    }

    /**
     * Xóa toàn bộ giỏ hàng
     * DELETE /api/cart/clear
     */
    @DeleteMapping("/clear")
    @Operation(summary = "Xóa toàn bộ giỏ hàng", 
            description = "Xóa tất cả sản phẩm trong giỏ hàng")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        cartService.clearCart(userImpl.getId());

        return ResponseEntity.ok(ApiResponse.success(null, "Xóa toàn bộ giỏ hàng thành công"));
    }

    /**
     * Đếm số lượng sản phẩm trong giỏ hàng
     * GET /api/cart/count
     */
    @GetMapping("/count")
    @Operation(summary = "Đếm số lượng sản phẩm", 
            description = "Trả về tổng số lượng sản phẩm trong giỏ hàng")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> getCartItemCount(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        Integer count = cartService.getCartItemCount(userImpl.getId());

        return ResponseEntity.ok(ApiResponse.success(count, "Lấy số lượng sản phẩm thành công"));
    }
}
