package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.AddToCartRequest;
import com.example.viti_be.dto.request.UpdateCartItemRequest;
import com.example.viti_be.dto.response.CartResponse;
import com.example.viti_be.mapper.CartMapper;
import com.example.viti_be.model.*;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.CartService;
import com.example.viti_be.service.CartTokenService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;
    private final CartTokenService cartTokenService;

    // ==================== ORIGINAL METHODS (backward compatibility) ====================

    @Override
    public CartResponse getCart(UUID userId) {
        Customer customer = getCustomerByUserId(userId);
        Cart cart = getOrCreateCart(customer);
        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(UUID userId, AddToCartRequest request) {
        Customer customer = getCustomerByUserId(userId);
        Cart cart = getOrCreateCart(customer);
        return addItemToCart(cart, request);
    }

    @Override
    @Transactional
    public CartResponse updateCartItemQuantity(UUID userId, UpdateCartItemRequest request) {
        Customer customer = getCustomerByUserId(userId);
        Cart cart = getOrCreateCart(customer);
        return updateItemQuantity(cart, request);
    }

    @Override
    @Transactional
    public CartResponse incrementCartItem(UUID userId, UUID productVariantId) {
        Customer customer = getCustomerByUserId(userId);
        Cart cart = getOrCreateCart(customer);
        return incrementItem(cart, productVariantId);
    }

    @Override
    @Transactional
    public CartResponse decrementCartItem(UUID userId, UUID productVariantId) {
        Customer customer = getCustomerByUserId(userId);
        Cart cart = getOrCreateCart(customer);
        return decrementItem(cart, productVariantId);
    }

    @Override
    @Transactional
    public CartResponse removeFromCart(UUID userId, UUID productVariantId) {
        Customer customer = getCustomerByUserId(userId);
        Cart cart = getOrCreateCart(customer);
        return removeItem(cart, productVariantId);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        Customer customer = getCustomerByUserId(userId);
        Optional<Cart> cartOpt = cartRepository.findByCustomerIdAndIsDeletedFalse(customer.getId());
        cartOpt.ifPresent(this::clearCartItems);
    }

    @Override
    public Integer getCartItemCount(UUID userId) {
        Customer customer = getCustomerByUserId(userId);
        Optional<Cart> cartOpt = cartRepository.findByCustomerIdAndIsDeletedFalse(customer.getId());
        return cartOpt.map(cart -> cartItemRepository.countTotalItemsByCartId(cart.getId())).orElse(0);
    }

    // ==================== NEW METHODS (guest user support) ====================

    @Override
    public CartResponse getCart(UUID userId, String cartToken, HttpServletResponse response) {
        Cart cart = resolveCart(userId, cartToken, response);
        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(UUID userId, String cartToken, AddToCartRequest request, HttpServletResponse response) {
        Cart cart = resolveCart(userId, cartToken, response);
        return addItemToCart(cart, request);
    }

    @Override
    @Transactional
    public CartResponse updateCartItemQuantity(UUID userId, String cartToken, UpdateCartItemRequest request, HttpServletResponse response) {
        Cart cart = resolveCart(userId, cartToken, response);
        return updateItemQuantity(cart, request);
    }

    @Override
    @Transactional
    public CartResponse incrementCartItem(UUID userId, String cartToken, UUID productVariantId, HttpServletResponse response) {
        Cart cart = resolveCart(userId, cartToken, response);
        return incrementItem(cart, productVariantId);
    }

    @Override
    @Transactional
    public CartResponse decrementCartItem(UUID userId, String cartToken, UUID productVariantId, HttpServletResponse response) {
        Cart cart = resolveCart(userId, cartToken, response);
        return decrementItem(cart, productVariantId);
    }

    @Override
    @Transactional
    public CartResponse removeFromCart(UUID userId, String cartToken, UUID productVariantId, HttpServletResponse response) {
        Cart cart = resolveCart(userId, cartToken, response);
        return removeItem(cart, productVariantId);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId, String cartToken, HttpServletResponse response) {
        Cart cart = resolveCart(userId, cartToken, response);
        clearCartItems(cart);
    }

    @Override
    public Integer getCartItemCount(UUID userId, String cartToken) {
        Cart cart = findCartByUserIdOrToken(userId, cartToken);
        if (cart == null) {
            return 0;
        }
        return cartItemRepository.countTotalItemsByCartId(cart.getId());
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Resolves cart based on userId (priority) or cartToken
     */
    private Cart resolveCart(UUID userId, String cartToken, HttpServletResponse response) {
        return cartTokenService.resolveCart(userId, cartToken, response);
    }

    /**
     * Finds cart by userId or token without creating a new one
     */
    private Cart findCartByUserIdOrToken(UUID userId, String cartToken) {
        if (userId != null) {
            Optional<Customer> customerOpt = customerRepository.findByUserIdAndIsDeletedFalse(userId);
            if (customerOpt.isPresent()) {
                return cartRepository.findByCustomerIdAndIsDeletedFalse(customerOpt.get().getId()).orElse(null);
            }
        }
        if (cartToken != null && !cartToken.isEmpty()) {
            return cartRepository.findByCartTokenAndIsDeletedFalse(cartToken).orElse(null);
        }
        return null;
    }

    /**
     * Lấy Customer theo User ID
     */
    private Customer getCustomerByUserId(UUID userId) {
        return customerRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new RuntimeException("Customer không tồn tại cho user: " + userId));
    }

    /**
     * Lấy Cart của customer, nếu chưa có thì tạo mới
     */
    private Cart getOrCreateCart(Customer customer) {
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
     * Tìm CartItem trong giỏ theo variant ID
     */
    private CartItem findCartItem(Cart cart, UUID productVariantId) {
        return cartItemRepository.findByCartIdAndProductVariantIdAndIsDeletedFalse(cart.getId(), productVariantId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không có trong giỏ hàng: " + productVariantId));
    }

    /**
     * Add item to cart (shared logic)
     */
    private CartResponse addItemToCart(Cart cart, AddToCartRequest request) {
        // Tìm product variant
        ProductVariant variant = productVariantRepository.findByIdAndIsDeletedFalse(request.getProductVariantId())
                .orElseThrow(() -> new RuntimeException("Product variant không tồn tại: " + request.getProductVariantId()));

        // Kiểm tra product còn hoạt động không
        Product product = variant.getProduct();
        if (product.getIsDeleted() || !"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new RuntimeException("Sản phẩm không khả dụng");
        }

        // Tìm xem variant đã có trong giỏ chưa
        CartItem existingItem = cart.findItemByVariantId(variant.getId());

        if (existingItem != null && !existingItem.getIsDeleted()) {
            // Đã có trong giỏ -> tăng quantity lên 1
            existingItem.setQuantity(existingItem.getQuantity() + 1);
            cartItemRepository.save(existingItem);
            log.info("Increased quantity for variant {} in cart. New quantity: {}", 
                    variant.getId(), existingItem.getQuantity());
        } else {
            // Chưa có -> thêm mới với quantity = 1
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productVariant(variant)
                    .quantity(1)
                    .unitPrice(variant.getSellingPrice())
                    .build();
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
            log.info("Added new variant {} to cart with quantity 1", variant.getId());
        }

        return buildCartResponse(cart);
    }

    /**
     * Update item quantity (shared logic)
     */
    private CartResponse updateItemQuantity(Cart cart, UpdateCartItemRequest request) {
        CartItem cartItem = findCartItem(cart, request.getProductVariantId());

        if (request.getQuantity() <= 0) {
            // Quantity <= 0 -> xóa item
            cart.removeItem(cartItem);
            cartItemRepository.delete(cartItem);
            log.info("Removed variant {} from cart (quantity set to 0)", request.getProductVariantId());
        } else {
            cartItem.setQuantity(request.getQuantity());
            cartItemRepository.save(cartItem);
            log.info("Updated quantity for variant {} to {}", 
                    request.getProductVariantId(), request.getQuantity());
        }

        return buildCartResponse(cart);
    }

    /**
     * Increment item (shared logic)
     */
    private CartResponse incrementItem(Cart cart, UUID productVariantId) {
        CartItem cartItem = findCartItem(cart, productVariantId);
        cartItem.setQuantity(cartItem.getQuantity() + 1);
        cartItemRepository.save(cartItem);

        log.info("Incremented quantity for variant {} to {}", productVariantId, cartItem.getQuantity());
        return buildCartResponse(cart);
    }

    /**
     * Decrement item (shared logic)
     */
    private CartResponse decrementItem(Cart cart, UUID productVariantId) {
        CartItem cartItem = findCartItem(cart, productVariantId);

        if (cartItem.getQuantity() <= 1) {
            // Quantity = 1 -> xóa item
            cart.removeItem(cartItem);
            cartItemRepository.delete(cartItem);
            log.info("Removed variant {} from cart (quantity reached 0)", productVariantId);
        } else {
            cartItem.setQuantity(cartItem.getQuantity() - 1);
            cartItemRepository.save(cartItem);
            log.info("Decremented quantity for variant {} to {}", productVariantId, cartItem.getQuantity());
        }

        return buildCartResponse(cart);
    }

    /**
     * Remove item (shared logic)
     */
    private CartResponse removeItem(Cart cart, UUID productVariantId) {
        CartItem cartItem = findCartItem(cart, productVariantId);
        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        log.info("Removed variant {} from cart", productVariantId);
        return buildCartResponse(cart);
    }

    /**
     * Clear all items from cart
     */
    private void clearCartItems(Cart cart) {
        cart.clearItems();
        cartItemRepository.deleteAllByCartId(cart.getId());
        log.info("Cleared all items from cart {}", cart.getId());
    }

    /**
     * Build CartResponse với thông tin inventory
     */
    private CartResponse buildCartResponse(Cart cart) {
        // Lấy danh sách variant IDs trong giỏ
        List<UUID> variantIds = cart.getItems().stream()
                .filter(item -> !item.getIsDeleted())
                .map(item -> item.getProductVariant().getId())
                .collect(Collectors.toList());

        // Lấy thông tin inventory cho các variants
        Map<UUID, Inventory> inventoryMap = new HashMap<>();
        for (UUID variantId : variantIds) {
            inventoryRepository.findByProductVariantId(variantId)
                    .ifPresent(inv -> inventoryMap.put(variantId, inv));
        }

        return CartMapper.mapToCartResponse(cart, inventoryMap);
    }
}
