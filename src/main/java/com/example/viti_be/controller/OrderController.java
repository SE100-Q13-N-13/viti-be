package com.example.viti_be.controller;

import com.example.viti_be.dto.request.CreateOrderRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.OrderResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.model.model_enum.OrderStatus;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @Operation(summary = "Create new order", description = "If order type OFFLINE: employeeId = current user id, require customer id or customer info\n" +
            "If order type ONLINE_COD or ONLINE_TRANSFER: customerId = current user Id, or create new customer with info"
    )
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID actorId = null;

        if (userDetails != null) {
            UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
            actorId = userImpl.getId();
        }

        OrderResponse newOrder = orderService.createOrder(request, actorId);

        return ResponseEntity.ok(ApiResponse.success(newOrder, "Tạo đơn hàng thành công"));
    }

    /**
     * Lấy chi tiết đơn hàng
     * GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get order by id")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable("id") UUID id) {
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order, "Lấy thông tin đơn hàng thành công"));
    }

    /**
     * Lấy danh sách đơn hàng
     * GET /api/orders
     */
    @GetMapping
    @Operation(summary = "Get all orders (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
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
    @Operation(summary = "Update order status")
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
    @Operation(summary = "Cancel order (Shortcut for Update status)")
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
    @Operation(summary = "Confirm order (Shortcut for Update status)")
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
    @Operation(summary = "Delete order")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable("id") UUID id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa đơn hàng"));
    }

    /**
     * Lấy danh sách đơn hàng (user)
     * GET /api/orders/user
     */
    @GetMapping("/user")
    @Operation(summary = "Get order list (USER)",
            description = "Current user employee or current user customer"
    )
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrdersByUser(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        UUID actorId = userImpl.getId();

        PageResponse<OrderResponse> orders = orderService.getOrdersByUserId(actorId, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Lấy danh sách đơn hàng thành công"));
    }

}