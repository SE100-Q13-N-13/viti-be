package com.example.viti_be.controller;

import com.example.viti_be.dto.request.CreateOrderRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.OrderResponse;
import com.example.viti_be.model.model_enum.OrderStatus;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.OrderService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * Tạo đơn hàng mới
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        UUID actorId = userImpl.getId();
        OrderResponse newOrder = orderService.createOrder(request, actorId);

        return ResponseEntity.ok(ApiResponse.success(newOrder, "Tạo đơn hàng thành công"));
    }

    /**
     * Lấy chi tiết đơn hàng
     * GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable("id") UUID id) {
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order, "Lấy thông tin đơn hàng thành công"));
    }

    /**
     * Lấy danh sách đơn hàng
     * GET /api/orders
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Page<OrderResponse> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Lấy danh sách đơn hàng thành công"));
    }

    /**
     * Cập nhật trạng thái đơn hàng (CONFIRM, COMPLETE, CANCEL)
     * PUT /api/orders/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable("id") UUID id,
            @RequestParam("status") OrderStatus status,
            @RequestParam(value = "reason", required = false) String reason,
            @AuthenticationPrincipal UserDetails userDetails) {

        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        UUID actorId = userImpl.getId();
        OrderResponse updatedOrder = orderService.updateOrderStatus(id, status, reason, actorId);

        return ResponseEntity.ok(ApiResponse.success(updatedOrder, "Cập nhật trạng thái đơn hàng thành công: " + status));
    }

    /**
     * API chuyên biệt để hủy đơn (Shortcut cho Update Status)
     * PUT /api/orders/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable("id") UUID id,
            @RequestParam(value = "reason", required = true) String reason,
            @AuthenticationPrincipal UserDetails userDetails) {

        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        UUID actorId = userImpl.getId();
        // Gọi service update status -> CANCELLED
        OrderResponse cancelledOrder = orderService.updateOrderStatus(id, OrderStatus.CANCELLED, reason, actorId);

        return ResponseEntity.ok(ApiResponse.success(cancelledOrder, "Đã hủy đơn hàng"));
    }

    /**
     * API chuyên biệt để xác nhận đơn (Shortcut)
     * PUT /api/orders/{id}/confirm
     */
    @PutMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        UUID actorId = userImpl.getId();
        // Gọi service update status -> CONFIRMED
        OrderResponse confirmedOrder = orderService.updateOrderStatus(id, OrderStatus.CONFIRMED, null, actorId);

        return ResponseEntity.ok(ApiResponse.success(confirmedOrder, "Đã xác nhận đơn hàng"));
    }

    /**
     * Xóa đơn hàng (Soft delete hoặc Hard delete tùy policy)
     * DELETE /api/orders/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable("id") UUID id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa đơn hàng"));
    }

    /**
     * Lấy danh sách đơn hàng (user)
     * GET /api/orders/user
     */
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersByUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        UUID actorId = userImpl.getId();
        Sort.Direction direction = order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<OrderResponse> orders = orderService.getOrdersByUserId(actorId, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Lấy danh sách đơn hàng thành công"));
    }

}