package com.example.viti_be.controller;

import com.example.viti_be.dto.request.ApplyPromotionCodeRequest;
import com.example.viti_be.dto.request.PromotionRequest;
import com.example.viti_be.dto.response.CartDiscountCalculationResponse;
import com.example.viti_be.dto.response.PromotionResponse;
import com.example.viti_be.dto.response.PromotionUsageReportResponse;
import com.example.viti_be.service.PromotionService;
import com.example.viti_be.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller cho Promotion Management
 * Endpoints:
 * - Admin: CRUD promotions, reports
 * - Public: View active promotions, calculate cart discount
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Promotion Management", description = "APIs for managing promotions and discounts")
public class PromotionController {

    private final PromotionService promotionService;

    // ========================================
    // ADMIN ENDPOINTS
    // ========================================

    @PostMapping("/admin/promotions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new promotion (Admin)")
    public ResponseEntity<Map<String, Object>> createPromotion(
            @Valid @RequestBody PromotionRequest request) {

        UUID adminId = SecurityUtils.getCurrentUserId();
        PromotionResponse response = promotionService.createPromotion(request, adminId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Promotion created successfully");
        result.put("data", response);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/admin/promotions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update promotion (Admin)")
    public ResponseEntity<Map<String, Object>> updatePromotion(
            @PathVariable UUID id,
            @Valid @RequestBody PromotionRequest request) {

        UUID adminId = SecurityUtils.getCurrentUserId();
        PromotionResponse response = promotionService.updatePromotion(id, request, adminId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Promotion updated successfully");
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/admin/promotions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete promotion (Admin)")
    public ResponseEntity<Map<String, Object>> deletePromotion(@PathVariable UUID id) {
        UUID adminId = SecurityUtils.getCurrentUserId();
        promotionService.deletePromotion(id, adminId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Promotion deleted successfully");

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/admin/promotions/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle promotion status (Admin)")
    public ResponseEntity<Map<String, Object>> togglePromotionStatus(@PathVariable UUID id) {
        UUID adminId = SecurityUtils.getCurrentUserId();
        PromotionResponse response = promotionService.togglePromotionStatus(id, adminId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Promotion status toggled successfully");
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/admin/promotions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all promotions (Admin)")
    public ResponseEntity<Map<String, Object>> getAllPromotions() {
        List<PromotionResponse> promotions = promotionService.getAllPromotions();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", promotions);
        result.put("total", promotions.size());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/admin/promotions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get promotion by ID (Admin)")
    public ResponseEntity<Map<String, Object>> getPromotionById(@PathVariable UUID id) {
        PromotionResponse response = promotionService.getPromotionById(id);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/admin/promotions/{id}/report")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get promotion usage report (Admin)")
    public ResponseEntity<Map<String, Object>> getPromotionUsageReport(@PathVariable UUID id) {
        PromotionUsageReportResponse report = promotionService.getPromotionUsageReport(id);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", report);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/admin/promotions/top")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get top promotions by usage (Admin)")
    public ResponseEntity<Map<String, Object>> getTopPromotions(
            @RequestParam(defaultValue = "10") int limit) {

        List<PromotionUsageReportResponse> topPromotions = promotionService.getTopPromotions(limit);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", topPromotions);

        return ResponseEntity.ok(result);
    }

    // ========================================
    // PUBLIC ENDPOINTS
    // ========================================

    @GetMapping("/promotions")
    @Operation(summary = "Get all active promotions (Public)")
    public ResponseEntity<Map<String, Object>> getActivePromotions() {
        List<PromotionResponse> promotions = promotionService.getActivePromotions();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", promotions);
        result.put("total", promotions.size());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/promotions/{id}")
    @Operation(summary = "Get promotion detail (Public)")
    public ResponseEntity<Map<String, Object>> getPromotionDetail(@PathVariable UUID id) {
        PromotionResponse response = promotionService.getPromotionById(id);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/promotions/calculate-discount")
    @Operation(summary = "Calculate cart discount (auto-apply promotions)")
    public ResponseEntity<Map<String, Object>> calculateCartDiscount(
            @Valid @RequestBody ApplyPromotionCodeRequest request) {

        CartDiscountCalculationResponse calculation = promotionService.calculateCartDiscount(request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", calculation);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/promotions/apply-code")
    @Operation(summary = "Apply promotion code to cart")
    public ResponseEntity<Map<String, Object>> applyPromotionCode(
            @Valid @RequestBody ApplyPromotionCodeRequest request) {

        CartDiscountCalculationResponse calculation = promotionService.applyPromotionCode(request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Promotion applied successfully");
        result.put("data", calculation);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/promotions/remove-code")
    @Operation(summary = "Remove promotion code from cart")
    public ResponseEntity<Map<String, Object>> removePromotionCode(
            @RequestParam String code,
            @Valid @RequestBody ApplyPromotionCodeRequest request) {

        CartDiscountCalculationResponse calculation =
                promotionService.removePromotionFromCart(code, request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Promotion removed successfully");
        result.put("data", calculation);

        return ResponseEntity.ok(result);
    }

    // ========================================
    // EMPLOYEE ENDPOINTS (Optional)
    // ========================================

    @GetMapping("/employee/promotions")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get active promotions for employees")
    public ResponseEntity<Map<String, Object>> getPromotionsForEmployee() {
        List<PromotionResponse> promotions = promotionService.getActivePromotions();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", promotions);
        result.put("total", promotions.size());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/employee/promotions/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Validate promotion code (for POS)")
    public ResponseEntity<Map<String, Object>> validatePromotionCode(
            @RequestParam String code,
            @Valid @RequestBody ApplyPromotionCodeRequest request) {

        try {
            CartDiscountCalculationResponse calculation =
                    promotionService.applyPromotionCode(request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("valid", true);
            result.put("data", calculation);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("valid", false);
            result.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(result);
        }
    }
}