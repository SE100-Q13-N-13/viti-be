package com.example.viti_be.service;

import com.example.viti_be.dto.request.AddToCartRequest;
import com.example.viti_be.dto.request.UpdateCartItemRequest;
import com.example.viti_be.dto.response.CartItemResponse;
import com.example.viti_be.dto.response.CartResponse;
import com.example.viti_be.model.Cart;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

/**
 * Service interface cho chức năng giỏ hàng
 * Supports both logged-in users (via userId) and guest users (via cartToken)
 */
public interface CartService {

    /**
     * Lấy giỏ hàng của customer hiện tại (dựa vào user đang đăng nhập)
     * @param userId ID của user đang đăng nhập (nullable for guest)
     * @return CartResponse
     */
    CartResponse getCart(UUID userId);

    /**
     * Lấy giỏ hàng cho guest hoặc logged-in user
     * @param userId ID của user đang đăng nhập (nullable for guest)
     * @param cartToken cart token từ cookie (nullable if logged in)
     * @param response HTTP response để set cookie nếu cần
     * @return CartResponse
     */
    CartResponse getCart(UUID userId, String cartToken, HttpServletResponse response);

    /**
     * Thêm sản phẩm vào giỏ hàng
     * Nếu sản phẩm đã có trong giỏ, tăng quantity lên 1
     * Nếu chưa có, thêm mới với quantity = 1
     * @param userId ID của user đang đăng nhập
     * @param request AddToCartRequest chứa productVariantId
     * @return CartResponse sau khi thêm
     */
    CartResponse addToCart(UUID userId, AddToCartRequest request);

    /**
     * Thêm sản phẩm vào giỏ hàng (hỗ trợ guest)
     * @param userId ID của user đang đăng nhập (nullable for guest)
     * @param cartToken cart token từ cookie (nullable if logged in)
     * @param request AddToCartRequest chứa productVariantId
     * @param response HTTP response để set cookie nếu cần
     * @return CartResponse sau khi thêm
     */
    CartResponse addToCart(UUID userId, String cartToken, AddToCartRequest request, HttpServletResponse response);

    /**
     * Cập nhật số lượng sản phẩm trong giỏ hàng (set quantity cụ thể)
     * @param userId ID của user đang đăng nhập
     * @param request UpdateCartItemRequest chứa productVariantId và quantity mới
     * @return CartResponse sau khi cập nhật
     */
    CartResponse updateCartItemQuantity(UUID userId, UpdateCartItemRequest request);

    /**
     * Cập nhật số lượng sản phẩm trong giỏ hàng (hỗ trợ guest)
     * @param userId ID của user đang đăng nhập (nullable for guest)
     * @param cartToken cart token từ cookie (nullable if logged in)
     * @param request UpdateCartItemRequest chứa productVariantId và quantity mới
     * @param response HTTP response để set cookie nếu cần
     * @return CartResponse sau khi cập nhật
     */
    CartResponse updateCartItemQuantity(UUID userId, String cartToken, UpdateCartItemRequest request, HttpServletResponse response);

    /**
     * Tăng số lượng sản phẩm trong giỏ hàng lên 1
     * @param userId ID của user đang đăng nhập
     * @param productVariantId ID của product variant
     * @return CartResponse sau khi tăng
     */
    CartResponse incrementCartItem(UUID userId, UUID productVariantId);

    /**
     * Tăng số lượng sản phẩm trong giỏ hàng lên 1 (hỗ trợ guest)
     * @param userId ID của user đang đăng nhập (nullable for guest)
     * @param cartToken cart token từ cookie (nullable if logged in)
     * @param productVariantId ID của product variant
     * @param response HTTP response để set cookie nếu cần
     * @return CartResponse sau khi tăng
     */
    CartResponse incrementCartItem(UUID userId, String cartToken, UUID productVariantId, HttpServletResponse response);

    /**
     * Giảm số lượng sản phẩm trong giỏ hàng đi 1
     * Nếu quantity = 1 thì xóa item khỏi giỏ
     * @param userId ID của user đang đăng nhập
     * @param productVariantId ID của product variant
     * @return CartResponse sau khi giảm
     */
    CartResponse decrementCartItem(UUID userId, UUID productVariantId);

    /**
     * Giảm số lượng sản phẩm trong giỏ hàng đi 1 (hỗ trợ guest)
     * @param userId ID của user đang đăng nhập (nullable for guest)
     * @param cartToken cart token từ cookie (nullable if logged in)
     * @param productVariantId ID của product variant
     * @param response HTTP response để set cookie nếu cần
     * @return CartResponse sau khi giảm
     */
    CartResponse decrementCartItem(UUID userId, String cartToken, UUID productVariantId, HttpServletResponse response);

    /**
     * Xóa một sản phẩm khỏi giỏ hàng
     * @param userId ID của user đang đăng nhập
     * @param productVariantId ID của product variant cần xóa
     * @return CartResponse sau khi xóa
     */
    CartResponse removeFromCart(UUID userId, UUID productVariantId);

    /**
     * Xóa một sản phẩm khỏi giỏ hàng (hỗ trợ guest)
     * @param userId ID của user đang đăng nhập (nullable for guest)
     * @param cartToken cart token từ cookie (nullable if logged in)
     * @param productVariantId ID của product variant cần xóa
     * @param response HTTP response để set cookie nếu cần
     * @return CartResponse sau khi xóa
     */
    CartResponse removeFromCart(UUID userId, String cartToken, UUID productVariantId, HttpServletResponse response);

    /**
     * Xóa toàn bộ giỏ hàng
     * @param userId ID của user đang đăng nhập
     */
    void clearCart(UUID userId);

    /**
     * Xóa toàn bộ giỏ hàng (hỗ trợ guest)
     * @param userId ID của user đang đăng nhập (nullable for guest)
     * @param cartToken cart token từ cookie (nullable if logged in)
     * @param response HTTP response để set cookie nếu cần
     */
    void clearCart(UUID userId, String cartToken, HttpServletResponse response);

    /**
     * Đếm số lượng items trong giỏ hàng
     * @param userId ID của user đang đăng nhập
     * @return Tổng số lượng sản phẩm
     */
    Integer getCartItemCount(UUID userId);

    /**
     * Đếm số lượng items trong giỏ hàng (hỗ trợ guest)
     * @param userId ID của user đang đăng nhập (nullable for guest)
     * @param cartToken cart token từ cookie (nullable if logged in)
     * @return Tổng số lượng sản phẩm
     */
    Integer getCartItemCount(UUID userId, String cartToken);
}
