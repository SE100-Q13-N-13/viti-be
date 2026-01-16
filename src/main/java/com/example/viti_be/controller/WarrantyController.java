package com.example.viti_be.controller;

import com.example.viti_be.dto.request.*;
import com.example.viti_be.dto.response.*;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.WarrantyService;
import com.example.viti_be.util.SecurityUtils;
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
 * REST Controller cho Warranty Module
 * CHUẨN HÓA với ApiResponse như OrderController
 */
@RestController
@RequestMapping("/api/warranty-tickets")
@RequiredArgsConstructor
@Tag(name = "Warranty Management", description = "APIs for managing warranty/repair tickets")
public class WarrantyController {

    private final WarrantyService warrantyService;

    // ========================================
    // ADMIN/TECHNICIAN APIs - WARRANTY TICKETS
    // ========================================

    @PostMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Tạo phiếu bảo hành mới")
    public ResponseEntity<ApiResponse<WarrantyTicketResponse>> createTicket(
            @Valid @RequestBody CreateWarrantyTicketRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        WarrantyTicketResponse response = warrantyService.createTicket(request, actorId);

        return ResponseEntity.ok(ApiResponse.success(response, "Tạo phiếu bảo hành thành công"));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Cập nhật phiếu bảo hành")
    public ResponseEntity<ApiResponse<WarrantyTicketResponse>> updateTicket(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWarrantyTicketRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        WarrantyTicketResponse response = warrantyService.updateTicket(id, request, actorId);

        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật phiếu thành công"));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa phiếu bảo hành")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(@PathVariable UUID id,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        warrantyService.deleteTicket(id, actorId);

        return ResponseEntity.ok(ApiResponse.success(null, "Xóa phiếu thành công"));
    }

    @GetMapping("/admin/warranty-tickets/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Lấy chi tiết phiếu")
    public ResponseEntity<ApiResponse<WarrantyTicketResponse>> getTicketById(@PathVariable UUID id) {
        WarrantyTicketResponse response = warrantyService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin phiếu thành công"));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Lấy danh sách tất cả phiếu")
    public ResponseEntity<ApiResponse<List<WarrantyTicketSummaryResponse>>> getAllTickets() {
        List<WarrantyTicketSummaryResponse> tickets = warrantyService.getAllTickets();
        return ResponseEntity.ok(ApiResponse.success(tickets, "Lấy danh sách phiếu thành công"));
    }

    @GetMapping("/admin/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Tìm kiếm phiếu theo keyword")
    public ResponseEntity<ApiResponse<List<WarrantyTicketSummaryResponse>>> searchTickets(
            @RequestParam String keyword) {
        List<WarrantyTicketSummaryResponse> tickets = warrantyService.searchTickets(keyword);
        return ResponseEntity.ok(ApiResponse.success(tickets, "Tìm kiếm hoàn tất"));
    }

    // ========================================
    // STATUS MANAGEMENT APIs
    // ========================================

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Đổi trạng thái phiếu")
    public ResponseEntity<ApiResponse<WarrantyTicketResponse>> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeTicketStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        WarrantyTicketResponse response = warrantyService.changeTicketStatus(id, request, actorId);

        return ResponseEntity.ok(ApiResponse.success(response, "Đổi trạng thái thành công"));
    }

    @PostMapping("/admin/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Bắt đầu sửa chữa")
    public ResponseEntity<ApiResponse<WarrantyTicketResponse>> startRepair(@PathVariable UUID id,
                                                                           @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        WarrantyTicketResponse response = warrantyService.startRepair(id, actorId);

        return ResponseEntity.ok(ApiResponse.success(response, "Đã bắt đầu sửa chữa"));
    }

    @PostMapping("/admin/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Hoàn thành sửa chữa")
    public ResponseEntity<ApiResponse<WarrantyTicketResponse>> completeRepair(@PathVariable UUID id,
                                                                              @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        WarrantyTicketResponse response = warrantyService.completeRepair(id, actorId);

        return ResponseEntity.ok(ApiResponse.success(response, "Hoàn thành sửa chữa"));
    }

    @PostMapping("/admin/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Trả máy cho khách")
    public ResponseEntity<ApiResponse<WarrantyTicketResponse>> returnToCustomer(@PathVariable UUID id,
                                                                                @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        WarrantyTicketResponse response = warrantyService.returnToCustomer(id, actorId);

        return ResponseEntity.ok(ApiResponse.success(response, "Đã trả máy cho khách"));
    }

    @PostMapping("/admin/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Hủy phiếu")
    public ResponseEntity<ApiResponse<WarrantyTicketResponse>> cancelTicket(
            @PathVariable UUID id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        WarrantyTicketResponse response = warrantyService.cancelTicket(id, reason, actorId);

        return ResponseEntity.ok(ApiResponse.success(response, "Đã hủy phiếu"));
    }

    // ========================================
    // SERVICE & PART MANAGEMENT APIs
    // ========================================

    @PostMapping("/admin/{id}/services")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Thêm dịch vụ vào phiếu")
    public ResponseEntity<ApiResponse<WarrantyTicketResponse>> addServices(
            @PathVariable UUID id,
            @Valid @RequestBody AddServicesRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        WarrantyTicketResponse response = warrantyService.addServices(id, request, actorId);

        return ResponseEntity.ok(ApiResponse.success(response, "Đã thêm dịch vụ"));
    }

    @DeleteMapping("/admin/{id}/services/{serviceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Xóa dịch vụ khỏi phiếu")
    public ResponseEntity<ApiResponse<WarrantyTicketResponse>> removeService(
            @PathVariable UUID id,
            @PathVariable UUID serviceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        WarrantyTicketResponse response = warrantyService.removeService(id, serviceId, actorId);

        return ResponseEntity.ok(ApiResponse.success(response, "Đã xóa dịch vụ"));
    }

    @PostMapping("/admin/{id}/parts")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Thêm linh kiện vào phiếu")
    public ResponseEntity<ApiResponse<WarrantyTicketResponse>> addParts(
            @PathVariable UUID id,
            @Valid @RequestBody AddPartsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        WarrantyTicketResponse response = warrantyService.addParts(id, request, actorId);

        return ResponseEntity.ok(ApiResponse.success(response, "Đã thêm linh kiện"));
    }

    @DeleteMapping("/admin/{id}/parts/{partId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Xóa linh kiện khỏi phiếu")
    public ResponseEntity<ApiResponse<WarrantyTicketResponse>> removePart(
            @PathVariable UUID id,
            @PathVariable UUID partId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        WarrantyTicketResponse response = warrantyService.removePart(id, partId, actorId);

        return ResponseEntity.ok(ApiResponse.success(response, "Đã xóa linh kiện"));
    }

    @PutMapping("/admin/{id}/technician")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reassign kỹ thuật viên")
    public ResponseEntity<ApiResponse<WarrantyTicketResponse>> reassignTechnician(
            @PathVariable UUID id,
            @Valid @RequestBody ReassignTechnicianRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID actorId = ((UserDetailsImpl) userDetails).getId();
        WarrantyTicketResponse response = warrantyService.reassignTechnician(id, request, actorId);

        return ResponseEntity.ok(ApiResponse.success(response, "Đã chuyển kỹ thuật viên"));
    }

    // ========================================
    // REPORTING APIs
    // ========================================

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy dashboard thống kê")
    public ResponseEntity<ApiResponse<WarrantyDashboardResponse>> getDashboard() {
        WarrantyDashboardResponse dashboard = warrantyService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Lấy thống kê thành công"));
    }

    @GetMapping("/admin/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Lấy danh sách phiếu quá hạn")
    public ResponseEntity<ApiResponse<List<WarrantyTicketSummaryResponse>>> getOverdueTickets() {
        List<WarrantyTicketSummaryResponse> tickets = warrantyService.getOverdueTickets();
        return ResponseEntity.ok(ApiResponse.success(tickets, "Lấy danh sách phiếu quá hạn thành công"));
    }

    @GetMapping("/admin/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Lấy lịch sử đổi trạng thái")
    public ResponseEntity<ApiResponse<List<TicketStatusHistoryResponse>>> getStatusHistory(
            @PathVariable UUID id) {
        List<TicketStatusHistoryResponse> history = warrantyService.getTicketStatusHistory(id);
        return ResponseEntity.ok(ApiResponse.success(history, "Lấy lịch sử thành công"));
    }

    // ========================================
    // PUBLIC APIs (Customer)
    // ========================================

    @GetMapping("/search")
    @Operation(summary = "Tra cứu phiếu theo serial (Public)")
    public ResponseEntity<ApiResponse<List<WarrantyTicketSummaryResponse>>> searchBySerial(
            @RequestParam String serial) {
        List<WarrantyTicketSummaryResponse> tickets = warrantyService.findTicketsBySerial(serial);
        return ResponseEntity.ok(ApiResponse.success(tickets, "Tra cứu thành công"));
    }

    @GetMapping("/my-tickets")
    @Operation(summary = "Lấy phiếu của tôi (Customer)")
    public ResponseEntity<ApiResponse<List<WarrantyTicketSummaryResponse>>> getMyTickets(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID customerId = ((UserDetailsImpl) userDetails).getId();
        List<WarrantyTicketSummaryResponse> tickets = warrantyService.getTicketsByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(tickets, "Lấy danh sách phiếu thành công"));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Kiểm tra trạng thái phiếu (Public)")
    public ResponseEntity<ApiResponse<WarrantyTicketResponse>> checkStatus(@PathVariable UUID id) {
        WarrantyTicketResponse ticket = warrantyService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.success(ticket, "Lấy trạng thái thành công"));
    }

}