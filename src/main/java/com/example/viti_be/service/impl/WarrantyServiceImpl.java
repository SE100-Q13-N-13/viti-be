package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.*;
import com.example.viti_be.dto.response.*;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.mapper.WarrantyMapper;
import com.example.viti_be.model.*;
import com.example.viti_be.model.model_enum.*;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.AuditLogService;
import com.example.viti_be.service.InventoryService;
import com.example.viti_be.service.SystemConfigService;
import com.example.viti_be.service.WarrantyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * WARRANTY SERVICE IMPLEMENTATION
 * Business Rules:
 * - Serial bắt buộc, phải tồn tại trong hệ thống
 * - Check warranty validity: còn BH = free service, hết BH = charge full
 * - Parts: Reserve khi add, deduct khi COMPLETED
 * - Status flow: RECEIVED → PROCESSING → [WAITING_FOR_PARTS] → COMPLETED → RETURNED
 * - Payment trước khi nhận máy (COMPLETED → RETURNED)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WarrantyServiceImpl implements WarrantyService {

    // Repositories
    private final WarrantyTicketRepository ticketRepository;
    private final WarrantyTicketServiceRepository ticketServiceRepository;
    private final WarrantyTicketPartRepository ticketPartRepository;
    private final RepairServiceRepository repairServiceRepository;
    private final PartComponentRepository partComponentRepository;
    private final ProductSerialRepository productSerialRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final SupplierRepository supplierRepository;

    // Services
    private final InventoryService inventoryService;
    private final SystemConfigService systemConfigService;
    private final AuditLogService auditLogService;

    // Mapper
    private final WarrantyMapper mapper;

    // ========================================
    // TICKET CRUD OPERATIONS
    // ========================================

    @Override
    @Transactional
    public WarrantyTicketResponse createTicket(CreateWarrantyTicketRequest request, UUID actorId) {
        log.info("Creating warranty ticket for serial: {}", request.getSerialNumber());

        // 1. Validate & Get Serial
        ProductSerial serial = productSerialRepository.findBySerialNumber(request.getSerialNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Serial not found: " + request.getSerialNumber()));

        // Validate serial must be SOLD
        if (serial.getStatus() != ProductSerialStatus.SOLD) {
            throw new BadRequestException(
                    String.format("Serial %s cannot create warranty ticket. Current status: %s",
                            serial.getSerialNumber(), serial.getStatus())
            );
        }

        // 2. Get Product Variant
        ProductVariant variant = serial.getProductVariant();
        if (variant == null) {
            throw new ResourceNotFoundException("Product variant not found for serial: " + serial.getSerialNumber());
        }

        // 3. Get or Validate Customer
        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.getCustomerId()));
        } else {
            // Guest: Validate name & phone
            if (request.getCustomerName() == null || request.getCustomerPhone() == null) {
                throw new BadRequestException("Customer name and phone are required for guest");
            }
        }

        // 4. Get Technician (optional)
        User technician = null;
        if (request.getTechnicianId() != null) {
            technician = userRepository.findById(request.getTechnicianId())
                    .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + request.getTechnicianId()));
        }

        // 5. Check Warranty Validity
        LocalDateTime warrantyExpireDate = serial.getWarrantyExpireDate();
        boolean isUnderWarranty = warrantyExpireDate != null &&
                warrantyExpireDate.isAfter(LocalDateTime.now());

        // 6. Create Ticket
        WarrantyTicket ticket = WarrantyTicket.builder()
                .ticketNumber(generateTicketNumber())
                .productVariant(variant)
                .productSerial(serial)
                .customer(customer)
                .customerName(customer != null ? customer.getFullName() : request.getCustomerName())
                .customerPhone(customer != null ? customer.getPhone() : request.getCustomerPhone())
                .technician(technician)
                .problemDescription(request.getProblemDescription())
                .accessories(request.getAccessories())
                .isUnderWarranty(isUnderWarranty)
                .warrantyExpireDate(warrantyExpireDate)
                .status(WarrantyTicketStatus.RECEIVED)
                .receivedDate(LocalDateTime.now())
                .expectedReturnDate(request.getExpectedReturnDate())
                .totalServiceCost(BigDecimal.ZERO)
                .totalPartCost(BigDecimal.ZERO)
                .totalCost(BigDecimal.ZERO)
                .notes(request.getNotes())
                .createdBy(actorId)
                .build();

        // 7. Add Initial Services (if any)
        if (request.getServices() != null && !request.getServices().isEmpty()) {
            for (ServiceItemRequest serviceReq : request.getServices()) {
                addServiceToTicket(ticket, serviceReq, isUnderWarranty);
            }
        }

        // 8. Calculate Total Cost
        ticket.calculateTotalCost();

        // 9. Update Serial Status
        serial.setStatus(ProductSerialStatus.WARRANTY);
        productSerialRepository.save(serial);

        // 10. Save Ticket
        WarrantyTicket savedTicket = ticketRepository.save(ticket);

        // 11. Audit Log
        auditLogService.log(actorId, AuditModule.WARRANTY, AuditAction.CREATE,
                savedTicket.getId().toString(), "warranty_ticket", null,
                savedTicket.getTicketNumber(), "Created warranty ticket");

        log.info("Created warranty ticket: {}", savedTicket.getTicketNumber());
        return mapper.toTicketResponse(savedTicket);
    }

    @Override
    @Transactional
    public WarrantyTicketResponse updateTicket(UUID ticketId, UpdateWarrantyTicketRequest request, UUID actorId) {
        WarrantyTicket ticket = getTicketEntity(ticketId);

        // Update fields
        if (request.getProblemDescription() != null) {
            ticket.setProblemDescription(request.getProblemDescription());
        }
        if (request.getAccessories() != null) {
            ticket.setAccessories(request.getAccessories());
        }
        if (request.getExpectedReturnDate() != null) {
            ticket.setExpectedReturnDate(request.getExpectedReturnDate());
        }
        if (request.getNotes() != null) {
            ticket.setNotes(request.getNotes());
        }

        // Reassign technician
        if (request.getTechnicianId() != null) {
            User newTechnician = userRepository.findById(request.getTechnicianId())
                    .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + request.getTechnicianId()));

            User oldTechnician = ticket.getTechnician();
            if (oldTechnician == null || !oldTechnician.getId().equals(newTechnician.getId())) {
                ticket.setTechnician(newTechnician);

                // Log reassignment
                auditLogService.log(actorId, AuditModule.WARRANTY, AuditAction.UPDATE,
                        ticketId.toString(), "technician",
                        oldTechnician != null ? oldTechnician.getFullName() : "None",
                        newTechnician.getFullName(), "Reassigned technician");
            }
        }

        String oldAccessories = ticket.getAccessories();
        String newAccessories = request.getAccessories();
        if (newAccessories != null && !newAccessories.equals(oldAccessories)) {
            auditLogService.log(
                    actorId,
                    AuditModule.WARRANTY,
                    AuditAction.UPDATE,
                    ticket.getId().toString(),
                    "warranty_ticket_accessories",
                    oldAccessories,
                    newAccessories,
                    "SUCCESS"
            );
        }

        ticket.setUpdatedBy(actorId);
        WarrantyTicket savedTicket = ticketRepository.save(ticket);

        auditLogService.log(actorId, AuditModule.WARRANTY, AuditAction.UPDATE,
                ticketId.toString(), "warranty_ticket", null, null, "Updated ticket info");

        return mapper.toTicketResponse(savedTicket);
    }

    @Override
    @Transactional
    public void deleteTicket(UUID ticketId, UUID actorId) {
        WarrantyTicket ticket = getTicketEntity(ticketId);

        // Only allow delete for RECEIVED status
        if (ticket.getStatus() != WarrantyTicketStatus.RECEIVED) {
            throw new BadRequestException("Only RECEIVED tickets can be deleted");
        }

        // Restore serial status
        ProductSerial serial = ticket.getProductSerial();
        serial.setStatus(ProductSerialStatus.SOLD);
        productSerialRepository.save(serial);

        // Soft delete
        ticket.setIsDeleted(true);
        ticket.setUpdatedBy(actorId);
        ticketRepository.save(ticket);

        auditLogService.log(actorId, AuditModule.WARRANTY, AuditAction.DELETE,
                ticketId.toString(), "warranty_ticket", ticket.getTicketNumber(),
                null, "Deleted ticket");

        log.info("Deleted warranty ticket: {}", ticket.getTicketNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public WarrantyTicketResponse getTicketById(UUID ticketId) {
        WarrantyTicket ticket = getTicketEntity(ticketId);
        return mapper.toTicketResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyTicketSummaryResponse> getAllTickets() {
        List<WarrantyTicket> tickets = ticketRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc();
        return mapper.toTicketSummaryResponseList(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyTicketSummaryResponse> findTicketsBySerial(String serialNumber) {
        List<WarrantyTicket> tickets = ticketRepository.findBySerialNumber(serialNumber);
        return mapper.toTicketSummaryResponseList(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyTicketSummaryResponse> searchTickets(String keyword) {
        List<WarrantyTicket> tickets = ticketRepository.searchTickets(keyword);
        return mapper.toTicketSummaryResponseList(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyTicketSummaryResponse> getTicketsByCustomer(UUID customerId) {
        List<WarrantyTicket> tickets = ticketRepository.findByCustomerId(customerId);
        return mapper.toTicketSummaryResponseList(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyTicketSummaryResponse> getTicketsByTechnician(UUID technicianId) {
        List<WarrantyTicket> tickets = ticketRepository.findByTechnicianId(technicianId);
        return mapper.toTicketSummaryResponseList(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyTicketSummaryResponse> getTicketsByStatus(String statusStr) {
        WarrantyTicketStatus status = WarrantyTicketStatus.valueOf(statusStr.toUpperCase());
        List<WarrantyTicket> tickets = ticketRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(status);
        return mapper.toTicketSummaryResponseList(tickets);
    }

    // ========================================
    // STATUS MANAGEMENT
    // ========================================

    @Override
    @Transactional
    public WarrantyTicketResponse changeTicketStatus(UUID ticketId, ChangeTicketStatusRequest request, UUID actorId) {
        WarrantyTicket ticket = getTicketEntity(ticketId);
        WarrantyTicketStatus oldStatus = ticket.getStatus();
        WarrantyTicketStatus newStatus = request.getNewStatus();

        // Validate transition
        if (!ticket.canTransitionTo(newStatus)) {
            throw new BadRequestException("Cannot change status from " + oldStatus + " to " + newStatus);
        }

        // Perform status-specific logic
        switch (newStatus) {
            case PROCESSING:
                handleStartProcessing(ticket, actorId);
                break;
            case WAITING_FOR_PARTS:
                handleWaitingForParts(ticket, actorId);
                break;
            case COMPLETED:
                handleCompleteRepair(ticket, actorId);
                break;
            case RETURNED:
                handleReturnToCustomer(ticket, actorId);
                break;
            case CANCELLED:
                handleCancelTicket(ticket, request.getReason(), actorId);
                break;
            default:
                break;
        }

        // Update status
        ticket.setStatus(newStatus);
        ticket.setUpdatedBy(actorId);
        WarrantyTicket savedTicket = ticketRepository.save(ticket);

        // Audit log
        auditLogService.log(actorId, AuditModule.WARRANTY, AuditAction.UPDATE,
                ticketId.toString(), "status", oldStatus.toString(), newStatus.toString(),
                request.getReason());

        log.info("Changed ticket {} status: {} -> {}", ticket.getTicketNumber(), oldStatus, newStatus);
        return mapper.toTicketResponse(savedTicket);
    }

    @Override
    @Transactional
    public WarrantyTicketResponse startRepair(UUID ticketId, UUID actorId) {
        ChangeTicketStatusRequest request = ChangeTicketStatusRequest.builder()
                .newStatus(WarrantyTicketStatus.PROCESSING)
                .reason("Started repair")
                .build();
        return changeTicketStatus(ticketId, request, actorId);
    }

    @Override
    @Transactional
    public WarrantyTicketResponse completeRepair(UUID ticketId, UUID actorId) {
        ChangeTicketStatusRequest request = ChangeTicketStatusRequest.builder()
                .newStatus(WarrantyTicketStatus.COMPLETED)
                .reason("Repair completed")
                .build();
        return changeTicketStatus(ticketId, request, actorId);
    }

    @Override
    @Transactional
    public WarrantyTicketResponse returnToCustomer(UUID ticketId, UUID actorId) {
        ChangeTicketStatusRequest request = ChangeTicketStatusRequest.builder()
                .newStatus(WarrantyTicketStatus.RETURNED)
                .reason("Returned to customer")
                .build();
        return changeTicketStatus(ticketId, request, actorId);
    }

    @Override
    @Transactional
    public WarrantyTicketResponse cancelTicket(UUID ticketId, String reason, UUID actorId) {
        WarrantyTicket ticket = getTicketEntity(ticketId);
        ticket.setCancellationReason(reason);

        ChangeTicketStatusRequest request = ChangeTicketStatusRequest.builder()
                .newStatus(WarrantyTicketStatus.CANCELLED)
                .reason(reason)
                .build();
        return changeTicketStatus(ticketId, request, actorId);
    }

    // ========================================
    // HELPER METHODS - STATUS HANDLERS
    // ========================================

    private void handleStartProcessing(WarrantyTicket ticket, UUID actorId) {
        // No special logic needed
    }

    private void handleWaitingForParts(WarrantyTicket ticket, UUID actorId) {
        // Logic: Check if really missing parts
        // In real scenario, should validate that parts were added but not available
    }

    private void handleCompleteRepair(WarrantyTicket ticket, UUID actorId) {
        // 1. Deduct parts from inventory (if reserved)
        for (WarrantyTicketPart part : ticket.getParts()) {
            inventoryService.confirmPartStockOut(
                    part.getPartComponent().getId(),
                    part.getQuantity(),
                    "TICKET-" + ticket.getTicketNumber(),
                    actorId
            );

            part.setUsedAt(LocalDateTime.now());
        }

        // 2. Mark all services as completed
        for (WarrantyTicketService service : ticket.getServices()) {
            if (service.getStatus() != WarrantyServiceStatus.COMPLETED) {
                service.setStatus(WarrantyServiceStatus.COMPLETED);
                service.setCompletedAt(LocalDateTime.now());
            }
        }
    }

    private void handleReturnToCustomer(WarrantyTicket ticket, UUID actorId) {
        // Update actual return date
        ticket.setActualReturnDate(LocalDateTime.now());

        // Restore serial status
        ProductSerial serial = ticket.getProductSerial();
        serial.setStatus(ProductSerialStatus.SOLD); // Back to customer
        productSerialRepository.save(serial);
    }

    private void handleCancelTicket(WarrantyTicket ticket, String reason, UUID actorId) {
        // Restore serial status
        ProductSerial serial = ticket.getProductSerial();
        serial.setStatus(ProductSerialStatus.SOLD);
        productSerialRepository.save(serial);

        // Restore parts inventory (unreserve)
        for (WarrantyTicketPart part : ticket.getParts()) {
            // Logic tương tự OrderService.cancelOrder
            inventoryService.unreservePartStock(
                    part.getPartComponent().getId(),
                    part.getQuantity(),
                    "CANCEL-TICKET-" + ticket.getTicketNumber(),
                    actorId
            );
        }
    }

    @Override
    @Transactional
    public WarrantyTicketResponse addServices(UUID ticketId, AddServicesRequest request, UUID actorId) {
        WarrantyTicket ticket = getTicketEntity(ticketId);

        for (ServiceItemRequest serviceReq : request.getServices()) {
            addServiceToTicket(ticket, serviceReq, ticket.getIsUnderWarranty());
        }

        ticket.calculateTotalCost();
        ticket.setUpdatedBy(actorId);
        WarrantyTicket savedTicket = ticketRepository.save(ticket);

        auditLogService.log(actorId, AuditModule.WARRANTY, AuditAction.UPDATE,
                ticketId.toString(), "services", null,
                String.valueOf(request.getServices().size()), "Added services");

        return mapper.toTicketResponse(savedTicket);
    }

    @Override
    @Transactional
    public WarrantyTicketResponse removeService(UUID ticketId, UUID serviceId, UUID actorId) {
        WarrantyTicket ticket = getTicketEntity(ticketId);

        WarrantyTicketService service = ticket.getServices().stream()
                .filter(s -> s.getId().equals(serviceId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Service not found in ticket"));

        ticket.getServices().remove(service);
        ticketServiceRepository.delete(service);

        ticket.calculateTotalCost();
        ticket.setUpdatedBy(actorId);
        WarrantyTicket savedTicket = ticketRepository.save(ticket);

        return mapper.toTicketResponse(savedTicket);
    }

    @Override
    @Transactional
    public WarrantyTicketResponse addParts(UUID ticketId, AddPartsRequest request, UUID actorId) {
        WarrantyTicket ticket = getTicketEntity(ticketId);

        for (PartItemRequest partReq : request.getParts()) {
            addPartToTicket(ticket, partReq, actorId);
        }

        ticket.calculateTotalCost();
        ticket.setUpdatedBy(actorId);
        WarrantyTicket savedTicket = ticketRepository.save(ticket);

        String details = "Added parts: " + request.getParts().stream()
                .map(p -> p.getPartComponentId() + " (x" + p.getQuantity() + ")")
                .collect(Collectors.joining(", "));

        auditLogService.log(
                actorId,
                AuditModule.WARRANTY,
                AuditAction.UPDATE,
                ticket.getId().toString(),
                "warranty_ticket",
                null,
                details,
                "SUCCESS"
        );

        return mapper.toTicketResponse(savedTicket);
    }

    @Override
    @Transactional
    public WarrantyTicketResponse removePart(UUID ticketId, UUID partId, UUID actorId) {
        WarrantyTicket ticket = getTicketEntity(ticketId);

        WarrantyTicketPart part = ticket.getParts().stream()
                .filter(p -> p.getId().equals(partId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Part not found in ticket"));

        // Unreserve inventory
        inventoryService.unreservePartStock(
                part.getPartComponent().getId(), part.getQuantity(),
                "REMOVE-FROM-" + ticket.getTicketNumber(), actorId);

        ticket.getParts().remove(part);
        ticketPartRepository.delete(part);

        ticket.calculateTotalCost();
        ticket.setUpdatedBy(actorId);
        WarrantyTicket savedTicket = ticketRepository.save(ticket);

        String details = "Removed part: " + part.getId();

        auditLogService.log(
                actorId,
                AuditModule.WARRANTY,
                AuditAction.UPDATE,
                ticket.getId().toString(),
                "warranty_ticket",
                null,
                details,
                "SUCCESS"
        );

        return mapper.toTicketResponse(savedTicket);
    }

    @Override
    @Transactional
    public WarrantyTicketResponse reassignTechnician(UUID ticketId, ReassignTechnicianRequest request, UUID actorId) {
        // 1. Tìm phiếu bảo hành
        WarrantyTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty ticket not found"));

        // 2. Tìm kỹ thuật viên mới
        User newTechnician = userRepository.findById(request.getNewTechnicianId())
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found"));

        // Kiểm tra role của user mới (đảm bảo là kỹ thuật viên)
        if (!newTechnician.getRoleNames().contains("ROLE_TECHNICIAN")) {
            throw new BadRequestException("User is not a technician");
        }

        String oldTechName = ticket.getTechnician() != null ? ticket.getTechnician().getFullName() : "Unassigned";
        auditLogService.log(
                actorId,
                AuditModule.WARRANTY,
                AuditAction.UPDATE,
                ticket.getId().toString(),
                "warranty_ticket",
                "Technician: " + oldTechName,
                "Technician: " + newTechnician.getFullName(),
                "SUCCESS"
        );

        // 3. Cập nhật
        ticket.setTechnician(newTechnician);

        // 4. Lưu log thay đổi (tùy vào logic audit của bạn)
        log.info("Ticket {} reassigned from {} to {} by {}",
                ticket.getTicketNumber(), oldTechName, newTechnician.getFullName(), actorId);

        WarrantyTicket savedTicket = ticketRepository.save(ticket);

        // 5. Trả về response (sử dụng mapper của bạn)
        return mapper.toTicketResponse(savedTicket);
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private void addServiceToTicket(WarrantyTicket ticket, ServiceItemRequest serviceReq, boolean isUnderWarranty) {
        RepairService repairService = repairServiceRepository.findById(serviceReq.getRepairServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Repair service not found"));

        BigDecimal unitPrice = isUnderWarranty ? BigDecimal.ZERO : repairService.getStandardPrice();
        BigDecimal additionalCost = serviceReq.getAdditionalCost() != null ?
                serviceReq.getAdditionalCost() : BigDecimal.ZERO;

        WarrantyTicketService ticketService = WarrantyTicketService.builder()
                .ticket(ticket)
                .repairService(repairService)
                .status(WarrantyServiceStatus.PENDING)
                .unitPrice(unitPrice)
                .additionalCost(additionalCost)
                .notes(serviceReq.getNotes())
                .build();

        ticketService.calculateTotalCost();
        ticket.addService(ticketService);
    }

    private void addPartToTicket(WarrantyTicket ticket, PartItemRequest partReq, UUID actorId) {
        PartComponent partComponent = partComponentRepository.findById(partReq.getPartComponentId())
                .orElseThrow(() -> new ResourceNotFoundException("Part component not found"));

        // Get markup from system config
        BigDecimal markup = systemConfigService.getPartMarkupPercent();

        // Calculate selling price
        BigDecimal unitPrice = partReq.getUnitPrice();
        if (unitPrice == null) {
            BigDecimal costPrice = partComponent.getUnitPrice();
            unitPrice = costPrice.add(costPrice.multiply(markup)
                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP));
        }

        // Check inventory availability
        Inventory inventory = inventoryRepository.findByPartComponentId(partComponent.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Part inventory not found"));

        if (inventory.getQuantityAvailable() < partReq.getQuantity()) {
            // Auto-switch ticket to WAITING_FOR_PARTS
            if (ticket.getStatus() == WarrantyTicketStatus.PROCESSING) {
                ticket.setStatus(WarrantyTicketStatus.WAITING_FOR_PARTS);
            }
            throw new BadRequestException(
                    String.format("Part %s: Insufficient stock. Available: %d, Required: %d",
                            partComponent.getName(), inventory.getQuantityAvailable(), partReq.getQuantity()));
        }

        // Reserve inventory
        inventory.reserveStock(partReq.getQuantity());
        inventoryRepository.save(inventory);

        // Create ticket part
        WarrantyTicketPart ticketPart = WarrantyTicketPart.builder()
                .ticket(ticket)
                .partComponent(partComponent)
                .quantity(partReq.getQuantity())
                .unitPrice(unitPrice)
                .notes(partReq.getNotes())
                .build();

        ticketPart.calculateTotalCost();
        ticket.addPart(ticketPart);
    }

    private WarrantyTicket getTicketEntity(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty ticket not found: " + ticketId));
    }

    private String generateTicketNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = new Random().nextInt(90000) + 10000;
        return "WRT-" + datePart + "-" + random;
    }


    // ========================================
    // REPORTING IMPLEMENTATION
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public WarrantyDashboardResponse getDashboard() {
        log.info("Generating warranty dashboard");

        // 1. Count by status
        long receivedCount = ticketRepository.countByStatus(WarrantyTicketStatus.RECEIVED);
        long processingCount = ticketRepository.countByStatus(WarrantyTicketStatus.PROCESSING);
        long waitingForPartsCount = ticketRepository.countByStatus(WarrantyTicketStatus.WAITING_FOR_PARTS);
        long completedCount = ticketRepository.countByStatus(WarrantyTicketStatus.COMPLETED);
        long returnedCount = ticketRepository.countByStatus(WarrantyTicketStatus.RETURNED);
        long cancelledCount = ticketRepository.countByStatus(WarrantyTicketStatus.CANCELLED);
        long totalTickets = receivedCount + processingCount + waitingForPartsCount +
                completedCount + returnedCount + cancelledCount;

        // 2. Overdue tickets
        long overdueCount = ticketRepository.findOverdueTickets(LocalDateTime.now()).size();

        // 3. Financial stats
        List<WarrantyTicket> allTickets = ticketRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc();

        BigDecimal totalServiceRevenue = allTickets.stream()
                .map(WarrantyTicket::getTotalServiceCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPartRevenue = allTickets.stream()
                .map(WarrantyTicket::getTotalPartCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = totalServiceRevenue.add(totalPartRevenue);

        // 4. Time-based stats
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime startOfLastMonth = startOfMonth.minusMonths(1);
        LocalDateTime endOfLastMonth = startOfMonth.minusDays(1).toLocalDate().atTime(23, 59, 59);

        List<WarrantyTicket> thisMonthTickets = ticketRepository.findByDateRange(startOfMonth, now);
        List<WarrantyTicket> lastMonthTickets = ticketRepository.findByDateRange(startOfLastMonth, endOfLastMonth);

        long ticketsThisMonth = thisMonthTickets.size();
        long ticketsLastMonth = lastMonthTickets.size();

        // 5. Average repair days (RECEIVED -> COMPLETED)
        Double avgRepairDays = calculateAverageRepairDays(allTickets);

        // 6. Tickets by technician
        Map<String, Long> ticketsByTechnician = allTickets.stream()
                .filter(ticket -> ticket.getTechnician() != null)
                .collect(Collectors.groupingBy(
                        ticket -> ticket.getTechnician().getFullName(),
                        Collectors.counting()
                ));

        // 7. Tickets by status (for chart)
        Map<String, Long> ticketsByStatus = new HashMap<>();
        ticketsByStatus.put("RECEIVED", receivedCount);
        ticketsByStatus.put("PROCESSING", processingCount);
        ticketsByStatus.put("WAITING_FOR_PARTS", waitingForPartsCount);
        ticketsByStatus.put("COMPLETED", completedCount);
        ticketsByStatus.put("RETURNED", returnedCount);
        ticketsByStatus.put("CANCELLED", cancelledCount);

        // 8. Low stock parts count
        long lowStockPartsCount = partComponentRepository.findLowStockParts().size();

        // Build response
        return WarrantyDashboardResponse.builder()
                .totalTickets(totalTickets)
                .receivedCount(receivedCount)
                .processingCount(processingCount)
                .waitingForPartsCount(waitingForPartsCount)
                .completedCount(completedCount)
                .returnedCount(returnedCount)
                .cancelledCount(cancelledCount)
                .overdueCount(overdueCount)
                .totalRevenue(totalRevenue)
                .totalServiceRevenue(totalServiceRevenue)
                .totalPartRevenue(totalPartRevenue)
                .ticketsThisMonth(ticketsThisMonth)
                .ticketsLastMonth(ticketsLastMonth)
                .avgRepairDays(avgRepairDays)
                .ticketsByTechnician(ticketsByTechnician)
                .ticketsByStatus(ticketsByStatus)
                .lowStockPartsCount(lowStockPartsCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyTicketSummaryResponse> getOverdueTickets() {
        List<WarrantyTicket> overdueTickets = ticketRepository.findOverdueTickets(LocalDateTime.now());
        return mapper.toTicketSummaryResponseList(overdueTickets);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketStatusHistoryResponse> getTicketStatusHistory(UUID ticketId) {
        // 1. Kiểm tra ticket tồn tại
        WarrantyTicket ticket = getTicketEntity(ticketId);

        // 2. Lấy danh sách audit logs cho ticket này
        List<AuditLog> logs = auditLogRepository.findAllByResourceId(ticketId.toString());

        // 3. Lọc logs liên quan đến status và thu thập danh sách Actor ID để tránh truy vấn nhiều lần
        List<AuditLog> statusLogs = logs.stream()
                .filter(log -> "status".equals(log.getResourceType()))
                .toList();

        Set<UUID> actorIds = statusLogs.stream()
                .map(AuditLog::getActorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 4. Lấy Map User ID -> Full Name
        Map<UUID, String> actorNames = userRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));

        // 5. Map sang Response DTO
        return statusLogs.stream()
                .map(log -> TicketStatusHistoryResponse.builder()
                        .id(log.getId())
                        .oldStatus(parseStatus(log.getOldValue()))
                        .newStatus(parseStatus(log.getNewValue()))
                        .reason(log.getStatus())
                        .actorName(log.getActorId() != null ? actorNames.getOrDefault(log.getActorId(), "Unknown User") : "System")
                        .changedAt(log.getCreatedAt())
                        .notes(null)
                        .build())
                .collect(Collectors.toList());
    }

    // ========================================
    // REPAIR SERVICE CRUD
    // ========================================

    @Override
    @Transactional
    public RepairServiceResponse createRepairService(RepairServiceResponse request, UUID actorId) {
        log.info("Creating repair service: {}", request.getName());

        RepairService repairService = RepairService.builder()
                .name(request.getName())
                .description(request.getDescription())
                .standardPrice(request.getStandardPrice())
                .estimatedDuration(request.getEstimatedDuration())
                .category(request.getCategory())
                .isActive(true)
                .build();

        repairService.setCreatedBy(actorId);
        RepairService saved = repairServiceRepository.save(repairService);

        auditLogService.log(actorId, AuditModule.WARRANTY, AuditAction.CREATE,
                saved.getId().toString(), "repair_service", null,
                saved.getName(), "SUCCESS");

        return mapper.toRepairServiceResponse(saved);
    }

    @Override
    @Transactional
    public RepairServiceResponse updateRepairService(UUID id, RepairServiceResponse request, UUID actorId) {
        RepairService repairService = repairServiceRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repair service not found: " + id));

        String oldName = repairService.getName();

        // Update fields
        if (request.getName() != null) repairService.setName(request.getName());
        if (request.getDescription() != null) repairService.setDescription(request.getDescription());
        if (request.getStandardPrice() != null) repairService.setStandardPrice(request.getStandardPrice());
        if (request.getEstimatedDuration() != null) repairService.setEstimatedDuration(request.getEstimatedDuration());
        if (request.getCategory() != null) repairService.setCategory(request.getCategory());
        if (request.getIsActive() != null) repairService.setIsActive(request.getIsActive());

        repairService.setUpdatedBy(actorId);
        RepairService saved = repairServiceRepository.save(repairService);

        auditLogService.log(actorId, AuditModule.WARRANTY, AuditAction.UPDATE,
                id.toString(), "repair_service", oldName,
                saved.getName(), "SUCCESS");

        return mapper.toRepairServiceResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRepairService(UUID id, UUID actorId) {
        RepairService repairService = repairServiceRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repair service not found: " + id));

        repairService.setIsDeleted(true);
        repairService.setUpdatedBy(actorId);
        repairServiceRepository.save(repairService);

        auditLogService.log(actorId, AuditModule.WARRANTY, AuditAction.DELETE,
                id.toString(), "repair_service", repairService.getName(),
                null, "SUCCESS");

        log.info("Deleted repair service: {}", repairService.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public RepairServiceResponse getRepairServiceById(UUID id) {
        RepairService repairService = repairServiceRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repair service not found: " + id));
        return mapper.toRepairServiceResponse(repairService);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepairServiceResponse> getAllRepairServices() {
        List<RepairService> services = repairServiceRepository.findAllByIsDeletedFalseOrderByName();
        return mapper.toRepairServiceResponseList(services);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepairServiceResponse> getActiveRepairServices() {
        List<RepairService> services = repairServiceRepository.findByIsActiveTrueAndIsDeletedFalseOrderByName();
        return mapper.toRepairServiceResponseList(services);
    }

    // ========================================
    // PART COMPONENT CRUD
    // ========================================

    @Override
    @Transactional
    public PartComponentResponse createPartComponent(PartComponentResponse request, UUID actorId) {
        log.info("Creating part component: {}", request.getName());

        // Get supplier if provided
        Supplier supplier = null;
        if (request.getSupplier() != null && request.getSupplier().getSupplierId() != null) {
            supplier = supplierRepository.findById(request.getSupplier().getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        }

        PartComponent partComponent = PartComponent.builder()
                .name(request.getName())
                .partType(request.getPartType())
                .supplier(supplier)
                .unit(request.getUnit())
                .unitPrice(request.getUnitPrice())
                .minStock(request.getMinStock())
                .isActive(true)
                .build();

        partComponent.setCreatedBy(actorId);
        PartComponent saved = partComponentRepository.save(partComponent);

        // Create inventory record
        Inventory inventory = Inventory.builder()
                .partComponentId(saved.getId())
                .quantityPhysical(0)
                .quantityReserved(0)
                .quantityAvailable(0)
                .minThreshold(saved.getMinStock())
                .lastCountedAt(LocalDateTime.now())
                .build();
        inventory.setCreatedBy(actorId);
        inventoryRepository.save(inventory);

        auditLogService.log(actorId, AuditModule.INVENTORY, AuditAction.CREATE,
                saved.getId().toString(), "part_component", null,
                saved.getName(), "SUCCESS");

        // Map response
        PartComponentResponse response = mapper.toPartComponentResponse(saved);
        response.setCurrentStock(0);
        return response;
    }

    @Override
    @Transactional
    public PartComponentResponse updatePartComponent(UUID id, PartComponentResponse request, UUID actorId) {
        PartComponent partComponent = partComponentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part component not found: " + id));

        String oldName = partComponent.getName();

        // Update supplier if changed
        if (request.getSupplier() != null && request.getSupplier().getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(request.getSupplier().getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
            partComponent.setSupplier(supplier);
        }

        // Update fields
        if (request.getName() != null) partComponent.setName(request.getName());
        if (request.getPartType() != null) partComponent.setPartType(request.getPartType());
        if (request.getUnit() != null) partComponent.setUnit(request.getUnit());
        if (request.getUnitPrice() != null) partComponent.setUnitPrice(request.getUnitPrice());
        if (request.getMinStock() != null) partComponent.setMinStock(request.getMinStock());
        if (request.getIsActive() != null) partComponent.setIsActive(request.getIsActive());

        partComponent.setUpdatedBy(actorId);
        PartComponent saved = partComponentRepository.save(partComponent);

        auditLogService.log(actorId, AuditModule.INVENTORY, AuditAction.UPDATE,
                id.toString(), "part_component", oldName,
                saved.getName(), "SUCCESS");

        // Get current stock
        Integer currentStock = inventoryRepository.findByPartComponentId(id)
                .map(Inventory::getQuantityAvailable)
                .orElse(0);

        PartComponentResponse response = mapper.toPartComponentResponse(saved);
        response.setCurrentStock(currentStock);
        return response;
    }

    @Override
    @Transactional
    public void deletePartComponent(UUID id, UUID actorId) {
        PartComponent partComponent = partComponentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part component not found: " + id));

        partComponent.setIsDeleted(true);
        partComponent.setUpdatedBy(actorId);
        partComponentRepository.save(partComponent);

        auditLogService.log(actorId, AuditModule.INVENTORY, AuditAction.DELETE,
                id.toString(), "part_component", partComponent.getName(),
                null, "SUCCESS");

        log.info("Deleted part component: {}", partComponent.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public PartComponentResponse getPartComponentById(UUID id) {
        PartComponent partComponent = partComponentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part component not found: " + id));

        // Get current stock
        Integer currentStock = inventoryRepository.findByPartComponentId(id)
                .map(Inventory::getQuantityAvailable)
                .orElse(0);

        PartComponentResponse response = mapper.toPartComponentResponse(partComponent);
        response.setCurrentStock(currentStock);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartComponentResponse> getAllPartComponents() {
        List<PartComponent> components = partComponentRepository.findAllByIsDeletedFalseOrderByName();
        return enrichPartComponentsWithStock(components);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartComponentResponse> getActivePartComponents() {
        List<PartComponent> components = partComponentRepository.findByIsActiveTrueAndIsDeletedFalseOrderByName();
        return enrichPartComponentsWithStock(components);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartComponentResponse> getLowStockParts() {
        List<PartComponent> lowStockParts = partComponentRepository.findLowStockParts();
        return enrichPartComponentsWithStock(lowStockParts);
    }

    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================

    /**
     * Calculate average repair days (from RECEIVED to COMPLETED)
     */
    private Double calculateAverageRepairDays(List<WarrantyTicket> tickets) {
        List<Long> repairDaysList = tickets.stream()
                .filter(ticket -> ticket.getStatus() == WarrantyTicketStatus.COMPLETED ||
                        ticket.getStatus() == WarrantyTicketStatus.RETURNED)
                .filter(ticket -> ticket.getReceivedDate() != null && ticket.getActualReturnDate() != null)
                .map(ticket -> {
                    return java.time.Duration.between(
                            ticket.getReceivedDate(),
                            ticket.getActualReturnDate()
                    ).toDays();
                })
                .toList();

        if (repairDaysList.isEmpty()) {
            return 0.0;
        }

        double sum = repairDaysList.stream().mapToLong(Long::longValue).sum();
        return sum / repairDaysList.size();
    }

    /**
     * Parse status string to enum
     */
    private WarrantyTicketStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isEmpty()) {
            return null;
        }
        try {
            return WarrantyTicketStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status string: {}", statusStr);
            return null;
        }
    }

    /**
     * Enrich part components with current stock from inventory
     */
    private List<PartComponentResponse> enrichPartComponentsWithStock(List<PartComponent> components) {
        return components.stream()
                .map(component -> {
                    PartComponentResponse response = mapper.toPartComponentResponse(component);

                    // Get current stock
                    Integer currentStock = inventoryRepository.findByPartComponentId(component.getId())
                            .map(Inventory::getQuantityAvailable)
                            .orElse(0);

                    response.setCurrentStock(currentStock);
                    return response;
                })
                .collect(Collectors.toList());
    }
}