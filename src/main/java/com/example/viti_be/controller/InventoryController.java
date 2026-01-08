package com.example.viti_be.controller;

import com.example.viti_be.dto.request.UpdateSerialStatusRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.InventoryResponse;
import com.example.viti_be.dto.response.ProductSerialResponse;
import com.example.viti_be.model.ProductSerial;
import com.example.viti_be.model.model_enum.ProductSerialStatus;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "APIs for Inventory Management")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    /**
     * Get all inventory items
     * GET /api/inventory
     */
    @Operation(summary = "Get all inventory items")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAllInventory() {
        List<InventoryResponse> response = inventoryService.getAllInventory();
        return ResponseEntity.ok(ApiResponse.success(response, "Inventory fetched successfully"));
    }

    /**
     * Get inventory by Product Variant ID
     * GET /api/inventory/variant/{productVariantId}
     */
    @Operation(summary = "Get inventory by Product Variant ID")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @GetMapping("/variant/{productVariantId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryByProductVariantId(
            @PathVariable UUID productVariantId) {
        InventoryResponse response = inventoryService.getInventoryByProductVariantId(productVariantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Inventory fetched successfully"));
    }

    // TODO: Temporarily disabled
    // /**
    //  * Get low stock items (below threshold)
    //  * GET /api/inventory/low-stock
    //  */
    // @Operation(summary = "Get low stock items")
    // @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    // @GetMapping("/low-stock")
    // public ResponseEntity<ApiResponse<List<InventoryResponse>>> getLowStockItems() {
    //     List<InventoryResponse> response = inventoryService.getLowStockItems();
    //     return ResponseEntity.ok(ApiResponse.success(response, "Low stock items fetched successfully"));
    // }

    /**
     * Get serials by Product Variant ID
     * GET /api/inventory/variant/{productVariantId}/serials
     */
    @Operation(summary = "Get serial numbers by Product Variant ID")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @GetMapping("/variant/{productVariantId}/serials")
    public ResponseEntity<ApiResponse<List<ProductSerialResponse>>> getSerialsByProductVariantId(
            @PathVariable UUID productVariantId,
            @RequestParam(required = false) ProductSerialStatus status) {
        List<ProductSerialResponse> response = inventoryService.getSerialsByProductVariantId(productVariantId, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Serials fetched successfully"));
    }

    /**
     * Get serials by status
     * GET /api/inventory/serials/status/{status}
     */
    @Operation(summary = "Get serials by status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @GetMapping("/serials/status/{status}")
    public ResponseEntity<ApiResponse<List<ProductSerialResponse>>> getSerialsByStatus(
            @PathVariable ProductSerialStatus status) {
        List<ProductSerialResponse> response = inventoryService.getSerialsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(response, "Serials fetched successfully"));
    }

    /**
     * Update serial status
     * PUT /api/inventory/serials/{serialNumber}/status
     */
    @Operation(summary = "Update serial status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PutMapping("/serials/{serialNumber}/status")
    public ResponseEntity<ApiResponse<ProductSerialResponse>> updateSerialStatus(
            @PathVariable String serialNumber,
            @Valid @RequestBody UpdateSerialStatusRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        UUID updatedBy = currentUser != null ? currentUser.getId() : null;
        ProductSerial updated = inventoryService.updateSerialStatus(serialNumber, request.getStatus(), updatedBy);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Serial status updated successfully"));
    }

    // TODO: Temporarily disabled
    // /**
    //  * Check if serial is available for sale (Helper endpoint for Order module)
    //  * GET /api/inventory/serials/{serialNumber}/available
    //  */
    // @Operation(summary = "Check if serial is available for sale")
    // @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    // @GetMapping("/serials/{serialNumber}/available")
    // public ResponseEntity<ApiResponse<Boolean>> checkSerialAvailability(@PathVariable String serialNumber) {
    //     boolean isAvailable = inventoryService.isSerialAvailableForSale(serialNumber);
    //     return ResponseEntity.ok(ApiResponse.success(isAvailable, 
    //             isAvailable ? "Serial is available" : "Serial is not available"));
    // }
}
