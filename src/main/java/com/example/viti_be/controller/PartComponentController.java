package com.example.viti_be.controller;

import com.example.viti_be.dto.request.PartComponentRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.PartComponentResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.PartComponentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller cho Part Componenent Module
 */
@RestController
@RequestMapping("/api/part-components")
@RequiredArgsConstructor
@Tag(name = "Part Component Management", description = "APIs for managing part components")
public class PartComponentController {

    private final PartComponentService partComponentService;

    // ========================================
    // MASTER DATA APIs - PART COMPONENTS
    // ========================================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo linh kiện mới (Master Data)")
    public ResponseEntity<ApiResponse<PartComponentResponse>> createPartComponent(
            @Valid @RequestBody PartComponentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        PartComponentResponse response = partComponentService.createPartComponent(request, actorId);
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo linh kiện thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật linh kiện")
    public ResponseEntity<ApiResponse<PartComponentResponse>> updatePartComponent(
            @PathVariable UUID id,
            @Valid @RequestBody PartComponentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        PartComponentResponse response = partComponentService.updatePartComponent(id, request, actorId);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật linh kiện thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa linh kiện (Soft delete)")
    public ResponseEntity<ApiResponse<Void>> deletePartComponent(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        partComponentService.deletePartComponent(id, actorId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa linh kiện thành công"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Lấy tất cả linh kiện")
    public ResponseEntity<ApiResponse<PageResponse<PartComponentResponse>>> getAllPartComponents(
            @ParameterObject Pageable pageable
    ) {
        PageResponse<PartComponentResponse> parts = partComponentService.getAllPartComponents(pageable);
        return ResponseEntity.ok(ApiResponse.success(parts, "Lấy danh sách linh kiện thành công"));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Lấy linh kiện đang hoạt động (Cho dropdown)")
    public ResponseEntity<ApiResponse<List<PartComponentResponse>>> getActivePartComponents() {
        List<PartComponentResponse> parts = partComponentService.getActivePartComponents();
        return ResponseEntity.ok(ApiResponse.success(parts, "Lấy danh sách active thành công"));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Cảnh báo linh kiện sắp hết hàng")
    public ResponseEntity<ApiResponse<List<PartComponentResponse>>> getLowStockParts() {
        List<PartComponentResponse> parts = partComponentService.getLowStockParts();
        return ResponseEntity.ok(ApiResponse.success(parts, "Lấy danh sách sắp hết hàng thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Lấy chi tiết linh kiện")
    public ResponseEntity<ApiResponse<PartComponentResponse>> getPartComponentById(@PathVariable UUID id) {
        PartComponentResponse part = partComponentService.getPartComponentById(id);
        return ResponseEntity.ok(ApiResponse.success(part, "Lấy chi tiết thành công"));
    }
}
