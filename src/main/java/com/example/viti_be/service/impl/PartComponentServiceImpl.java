package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.PartComponentRequest;
import com.example.viti_be.dto.response.PartComponentResponse;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.mapper.WarrantyMapper;
import com.example.viti_be.model.Inventory;
import com.example.viti_be.model.PartComponent;
import com.example.viti_be.model.Supplier;
import com.example.viti_be.model.model_enum.AuditAction;
import com.example.viti_be.model.model_enum.AuditModule;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.AuditLogService;
import com.example.viti_be.service.InventoryService;
import com.example.viti_be.service.PartComponentService;
import com.example.viti_be.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PartComponentServiceImpl implements PartComponentService {

    @Autowired
    private final SupplierRepository supplierRepository;
    @Autowired
    private final PartComponentRepository partComponentRepository;
    @Autowired
    private final InventoryRepository inventoryRepository;
    @Autowired
    private final InventoryService inventoryService;
    @Autowired
    private final AuditLogService auditLogService;
    @Autowired
    private SystemConfigService systemConfigService;

    // Mapper
    private final WarrantyMapper mapper;

    public PartComponentServiceImpl(SupplierRepository supplierRepository, PartComponentRepository partComponentRepository, InventoryRepository inventoryRepository, InventoryService inventoryService, AuditLogService auditLogService, WarrantyMapper mapper) {
        this.supplierRepository = supplierRepository;
        this.partComponentRepository = partComponentRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
        this.auditLogService = auditLogService;
        this.mapper = mapper;
    }


    // ========================================
    // PART COMPONENT CRUD
    // ========================================

    @Override
    @Transactional
    public PartComponentResponse createPartComponent(PartComponentRequest request, UUID actorId) {
        log.info("Creating part component: {}", request.getName());

        Supplier supplier = null;
        if (request.getSupplierId() != null) {
            supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        }

        PartComponent partComponent = new PartComponent();
        partComponent.setName(request.getName());
        partComponent.setPartType(request.getPartType());
        partComponent.setSupplier(supplier);
        partComponent.setUnit(request.getUnit());
        partComponent.setMinStock(request.getMinStock());
        partComponent.setCreatedBy(actorId);

        BigDecimal initialCost = request.getUnitPrice();
        partComponent.setPurchasePriceAvg(initialCost);

        if (initialCost != null && initialCost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal markupPercent = systemConfigService.getPartMarkupPercent();

            BigDecimal multiplier = BigDecimal.ONE.add(
                    markupPercent.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            );

            BigDecimal sellingPrice = initialCost.multiply(multiplier);
            partComponent.setSellingPrice(sellingPrice);
        } else {
            partComponent.setSellingPrice(BigDecimal.ZERO);
        }

        partComponent.setCreatedBy(actorId);
        PartComponent saved = partComponentRepository.save(partComponent);
        inventoryService.getOrCreatePartInventory(saved.getId(), actorId);

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
    public PartComponentResponse updatePartComponent(UUID id, PartComponentRequest request, UUID actorId) {
        PartComponent partComponent = partComponentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part component not found: " + id));

        String oldName = partComponent.getName();

        // Update supplier if changed
        if (request.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
            partComponent.setSupplier(supplier);
        }

        // Update fields
        if (request.getName() != null) partComponent.setName(request.getName());
        if (request.getPartType() != null) partComponent.setPartType(request.getPartType());
        if (request.getUnit() != null) partComponent.setUnit(request.getUnit());
        if (request.getUnitPrice() != null) partComponent.setPurchasePriceAvg(request.getUnitPrice());
        if (request.getMinStock() != null) partComponent.setMinStock(request.getMinStock());

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

        Inventory inventory = inventoryRepository.findByPartComponentId(id).orElse(null);
        if (inventory != null && inventory.getQuantityPhysical() > 0) {
            throw new BadRequestException("Không thể xóa linh kiện đang còn tồn kho.");
        }

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
        List<PartComponent> components = partComponentRepository.findByIsDeletedFalseOrderByName();
        return enrichPartComponentsWithStock(components);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartComponentResponse> getLowStockParts() {
        List<PartComponent> lowStockParts = partComponentRepository.findLowStockParts();
        return enrichPartComponentsWithStock(lowStockParts);
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
