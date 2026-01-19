package com.example.viti_be.controller;

import com.example.viti_be.dto.request.PurchaseOrderRequest;
import com.example.viti_be.dto.request.ReceiveGoodsRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.PurchaseOrderResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/purchase-orders")
@Tag(name = "Purchase Order", description = "APIs for Purchase Order & Stock In Management")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    /**
     * API 1: Create Purchase Order (Draft)
     * POST /api/purchase-orders
     */
    @Operation(summary = "Create a new Purchase Order in DRAFT status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        UUID createdBy = currentUser != null ? currentUser.getId() : null;
        PurchaseOrderResponse response = purchaseOrderService.createPurchaseOrder(request, createdBy);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase Order created successfully"));
    }

    /**
     * API 2: Confirm Goods Receipt (Stock In)
     * POST /api/purchase-orders/{id}/receive
     */
    @Operation(summary = "Confirm goods receipt and process Stock In")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping("/{id}/receive")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> receiveGoods(
            @PathVariable UUID id,
            @Valid @RequestBody ReceiveGoodsRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        UUID updatedBy = currentUser != null ? currentUser.getId() : null;
        PurchaseOrderResponse response = purchaseOrderService.receiveGoods(id, request, updatedBy);
        return ResponseEntity.ok(ApiResponse.success(response, "Goods received successfully"));
    }

    /**
     * Get Purchase Order by ID
     * GET /api/purchase-orders/{id}
     */
    @Operation(summary = "Get Purchase Order by ID")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getPurchaseOrderById(@PathVariable UUID id) {
        PurchaseOrderResponse response = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase Order fetched successfully"));
    }

    /**
     * Get all Purchase Orders
     * GET /api/purchase-orders
     */
    @Operation(summary = "Get all Purchase Orders")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PurchaseOrderResponse>>> getAllPurchaseOrders(
            @ParameterObject Pageable pageable
            ) {
        PageResponse<PurchaseOrderResponse> response = purchaseOrderService.getAllPurchaseOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase Orders fetched successfully"));
    }

    /**
     * Get Purchase Orders by Supplier ID
     * GET /api/purchase-orders/supplier/{supplierId}
     */
    @Operation(summary = "Get Purchase Orders by Supplier ID")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> getPurchaseOrdersBySupplierId(
            @PathVariable UUID supplierId) {
        List<PurchaseOrderResponse> response = purchaseOrderService.getPurchaseOrdersBySupplierId(supplierId);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase Orders fetched successfully"));
    }

    /**
     * Close Purchase Order (RECEIVED -> CLOSED)
     * PUT /api/purchase-orders/{id}/close
     */
    @Operation(summary = "Close a Purchase Order")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> closePurchaseOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        UUID updatedBy = currentUser != null ? currentUser.getId() : null;
        PurchaseOrderResponse response = purchaseOrderService.closePurchaseOrder(id, updatedBy);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase Order closed successfully"));
    }

    /**
     * Delete Purchase Order (Soft delete, only DRAFT status)
     * DELETE /api/purchase-orders/{id}
     */
    @Operation(summary = "Delete a Purchase Order (only DRAFT status)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePurchaseOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        UUID deletedBy = currentUser != null ? currentUser.getId() : null;
        purchaseOrderService.deletePurchaseOrder(id, deletedBy);
        return ResponseEntity.ok(ApiResponse.success(null, "Purchase Order deleted successfully"));
    }
}
