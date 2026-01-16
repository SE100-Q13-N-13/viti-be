package com.example.viti_be.controller;

import com.example.viti_be.dto.request.RepairServiceRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.RepairServiceResponse;
import com.example.viti_be.model.RepairService;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.RepairServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller cho Repair Service Module
 */
@RestController
@RequestMapping("/api/repair-services")
@RequiredArgsConstructor
@Tag(name = "Repair Service Management", description = "APIs for managing repair service")
public class RepairServiceController {

    private final RepairServiceService repairService;
    // ========================================
    // MASTER DATA APIs - REPAIR SERVICES
    // ========================================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo dịch vụ sửa chữa mới (Master Data)")
    public ResponseEntity<ApiResponse<RepairServiceResponse>> createRepairService(
            @Valid @RequestBody RepairServiceRequest request, // Lưu ý: Service đang dùng Response làm Request
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        RepairServiceResponse response = repairService.createRepairService(request, actorId);
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo dịch vụ thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật dịch vụ sửa chữa")
    public ResponseEntity<ApiResponse<RepairServiceResponse>> updateRepairService(
            @PathVariable UUID id,
            @Valid @RequestBody RepairServiceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        RepairServiceResponse response = repairService.updateRepairService(id, request, actorId);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật dịch vụ thành công"));
    }

    @DeleteMapping("/repair-services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa dịch vụ sửa chữa (Soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteRepairService(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        repairService.deleteRepairService(id, actorId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa dịch vụ thành công"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Lấy tất cả dịch vụ sửa chữa")
    public ResponseEntity<ApiResponse<List<RepairServiceResponse>>> getAllRepairServices() {
        List<RepairServiceResponse> services = repairService.getAllRepairServices();
        return ResponseEntity.ok(ApiResponse.success(services, "Lấy danh sách dịch vụ thành công"));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Lấy dịch vụ đang hoạt động (Cho dropdown)")
    public ResponseEntity<ApiResponse<List<RepairServiceResponse>>> getActiveRepairServices() {
        List<RepairServiceResponse> services = repairService.getActiveRepairServices();
        return ResponseEntity.ok(ApiResponse.success(services, "Lấy danh sách active thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Lấy chi tiết dịch vụ")
    public ResponseEntity<ApiResponse<RepairServiceResponse>> getRepairServiceById(@PathVariable UUID id) {
        RepairServiceResponse service = repairService.getRepairServiceById(id);
        return ResponseEntity.ok(ApiResponse.success(service, "Lấy chi tiết thành công"));
    }
}
