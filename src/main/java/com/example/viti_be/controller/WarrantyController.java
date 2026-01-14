package com.example.viti_be.controller;

import com.example.viti_be.dto.request.*;
import com.example.viti_be.dto.response.*;
import com.example.viti_be.service.WarrantyService;
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
 * REST Controller cho Warranty Module
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Warranty Management", description = "APIs for managing warranty/repair tickets")
public class WarrantyController {

    private final WarrantyService warrantyService;

    // ========================================
    // ADMIN/TECHNICIAN APIs
    // ========================================

    @PostMapping("/admin/warranty-tickets")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @Operation(summary = "Create warranty ticket")
    public ResponseEntity<Map<String, Object>> createTicket(
            @Valid @RequestBody CreateWarrantyTicketRequest request) {

        UUID actorId = SecurityUtils.getCurrentUserId();
        WarrantyTicketResponse response = warrantyService.createTicket(request, actorId);

        return buildSuccessResponse(response, "Ticket created successfully", HttpStatus.CREATED);
    }

    @PutMapping("/admin/warranty-tickets/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @Operation(summary = "Update warranty ticket")
    public ResponseEntity<Map<String, Object>> updateTicket(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWarrantyTicketRequest request) {

        UUID actorId = SecurityUtils.getCurrentUserId();
        WarrantyTicketResponse response = warrantyService.updateTicket(id, request, actorId);

        return buildSuccessResponse(response, "Ticket updated successfully");
    }

    @DeleteMapping("/admin/warranty-tickets/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete warranty ticket")
    public ResponseEntity<Map<String, Object>> deleteTicket(@PathVariable UUID id) {
        UUID actorId = SecurityUtils.getCurrentUserId();
        warrantyService.deleteTicket(id, actorId);

        return buildSuccessResponse(null, "Ticket deleted successfully");
    }

    @GetMapping("/admin/warranty-tickets/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @Operation(summary = "Get ticket detail")
    public ResponseEntity<Map<String, Object>> getTicketById(@PathVariable UUID id) {
        WarrantyTicketResponse response = warrantyService.getTicketById(id);
        return buildSuccessResponse(response, "Success");
    }

    @GetMapping("/admin/warranty-tickets")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @Operation(summary = "Get all tickets")
    public ResponseEntity<Map<String, Object>> getAllTickets() {
        List<WarrantyTicketSummaryResponse> tickets = warrantyService.getAllTickets();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", tickets);
        result.put("total", tickets.size());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/admin/warranty-tickets/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @Operation(summary = "Search tickets by keyword")
    public ResponseEntity<Map<String, Object>> searchTickets(@RequestParam String keyword) {
        List<WarrantyTicketSummaryResponse> tickets = warrantyService.searchTickets(keyword);
        return buildSuccessResponse(tickets, "Search completed");
    }

    @PatchMapping("/admin/warranty-tickets/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @Operation(summary = "Change ticket status")
    public ResponseEntity<Map<String, Object>> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeTicketStatusRequest request) {

        UUID actorId = SecurityUtils.getCurrentUserId();
        WarrantyTicketResponse response = warrantyService.changeTicketStatus(id, request, actorId);

        return buildSuccessResponse(response, "Status changed successfully");
    }

    @PostMapping("/admin/warranty-tickets/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @Operation(summary = "Start repair")
    public ResponseEntity<Map<String, Object>> startRepair(@PathVariable UUID id) {
        UUID actorId = SecurityUtils.getCurrentUserId();
        WarrantyTicketResponse response = warrantyService.startRepair(id, actorId);

        return buildSuccessResponse(response, "Repair started");
    }

    @PostMapping("/admin/warranty-tickets/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @Operation(summary = "Complete repair")
    public ResponseEntity<Map<String, Object>> completeRepair(@PathVariable UUID id) {
        UUID actorId = SecurityUtils.getCurrentUserId();
        WarrantyTicketResponse response = warrantyService.completeRepair(id, actorId);

        return buildSuccessResponse(response, "Repair completed");
    }

    @PostMapping("/admin/warranty-tickets/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN', 'CASHIER')")
    @Operation(summary = "Return to customer")
    public ResponseEntity<Map<String, Object>> returnToCustomer(@PathVariable UUID id) {
        UUID actorId = SecurityUtils.getCurrentUserId();
        WarrantyTicketResponse response = warrantyService.returnToCustomer(id, actorId);

        return buildSuccessResponse(response, "Returned to customer");
    }

    @PostMapping("/admin/warranty-tickets/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel ticket")
    public ResponseEntity<Map<String, Object>> cancelTicket(
            @PathVariable UUID id,
            @RequestParam String reason) {

        UUID actorId = SecurityUtils.getCurrentUserId();
        WarrantyTicketResponse response = warrantyService.cancelTicket(id, reason, actorId);

        return buildSuccessResponse(response, "Ticket cancelled");
    }

    @PostMapping("/admin/warranty-tickets/{id}/services")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @Operation(summary = "Add services to ticket")
    public ResponseEntity<Map<String, Object>> addServices(
            @PathVariable UUID id,
            @Valid @RequestBody AddServicesRequest request) {

        UUID actorId = SecurityUtils.getCurrentUserId();
        WarrantyTicketResponse response = warrantyService.addServices(id, request, actorId);

        return buildSuccessResponse(response, "Services added");
    }

    @DeleteMapping("/admin/warranty-tickets/{id}/services/{serviceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @Operation(summary = "Remove service from ticket")
    public ResponseEntity<Map<String, Object>> removeService(
            @PathVariable UUID id,
            @PathVariable UUID serviceId) {

        UUID actorId = SecurityUtils.getCurrentUserId();
        WarrantyTicketResponse response = warrantyService.removeService(id, serviceId, actorId);

        return buildSuccessResponse(response, "Service removed");
    }

    @PostMapping("/admin/warranty-tickets/{id}/parts")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @Operation(summary = "Add parts to ticket")
    public ResponseEntity<Map<String, Object>> addParts(
            @PathVariable UUID id,
            @Valid @RequestBody AddPartsRequest request) {

        UUID actorId = SecurityUtils.getCurrentUserId();
        WarrantyTicketResponse response = warrantyService.addParts(id, request, actorId);

        return buildSuccessResponse(response, "Parts added");
    }

    @DeleteMapping("/admin/warranty-tickets/{id}/parts/{partId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @Operation(summary = "Remove part from ticket")
    public ResponseEntity<Map<String, Object>> removePart(
            @PathVariable UUID id,
            @PathVariable UUID partId) {

        UUID actorId = SecurityUtils.getCurrentUserId();
        WarrantyTicketResponse response = warrantyService.removePart(id, partId, actorId);

        return buildSuccessResponse(response, "Part removed");
    }

    @GetMapping("/admin/warranty-tickets/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get warranty dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        WarrantyDashboardResponse dashboard = warrantyService.getDashboard();
        return buildSuccessResponse(dashboard, "Success");
    }

    // ========================================
    // PUBLIC APIs (Customer)
    // ========================================

    @GetMapping("/warranty-tickets/search")
    @Operation(summary = "Search ticket by serial (Public)")
    public ResponseEntity<Map<String, Object>> searchBySerial(@RequestParam String serial) {
        List<WarrantyTicketSummaryResponse> tickets = warrantyService.findTicketsBySerial(serial);
        return buildSuccessResponse(tickets, "Success");
    }

    @GetMapping("/warranty-tickets/my-tickets")
    @Operation(summary = "Get my tickets (Customer)")
    public ResponseEntity<Map<String, Object>> getMyTickets() {
        UUID customerId = SecurityUtils.getCurrentUserId(); // Assuming customer is also a user
        List<WarrantyTicketSummaryResponse> tickets = warrantyService.getTicketsByCustomer(customerId);
        return buildSuccessResponse(tickets, "Success");
    }

    @GetMapping("/warranty-tickets/{id}/status")
    @Operation(summary = "Check ticket status (Public)")
    public ResponseEntity<Map<String, Object>> checkStatus(@PathVariable UUID id) {
        WarrantyTicketResponse ticket = warrantyService.getTicketById(id);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("ticketNumber", ticket.getTicketNumber());
        result.put("status", ticket.getStatus());
        result.put("expectedReturnDate", ticket.getExpectedReturnDate());
        result.put("notes", ticket.getNotes());

        return ResponseEntity.ok(result);
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(Object data, String message) {
        return buildSuccessResponse(data, message, HttpStatus.OK);
    }

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(Object data, String message, HttpStatus status) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", message);
        if (data != null) {
            result.put("data", data);
        }
        return ResponseEntity.status(status).body(result);
    }
}