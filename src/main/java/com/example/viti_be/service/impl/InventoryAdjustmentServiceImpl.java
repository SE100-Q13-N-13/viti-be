package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.CreateInventoryAdjustmentRequest;
import com.example.viti_be.dto.request.StockTransactionItemRequest;
import com.example.viti_be.dto.response.InventoryAdjustmentResponse;
import com.example.viti_be.dto.response.StockTransactionResponse;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.model.Inventory;
import com.example.viti_be.model.InventoryAdjustment;
import com.example.viti_be.model.ProductVariant;
import com.example.viti_be.model.StockTransaction;
import com.example.viti_be.model.User;
import com.example.viti_be.model.model_enum.InventoryAdjustmentStatus;
import com.example.viti_be.model.model_enum.StockTransactionType;
import com.example.viti_be.repository.InventoryAdjustmentRepository;
import com.example.viti_be.repository.ProductVariantRepository;
import com.example.viti_be.repository.StockTransactionRepository;
import com.example.viti_be.repository.UserRepository;
import com.example.viti_be.service.InventoryAdjustmentService;
import com.example.viti_be.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryAdjustmentServiceImpl implements InventoryAdjustmentService {

    private final InventoryAdjustmentRepository adjustmentRepository;
    private final StockTransactionRepository transactionRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    public InventoryAdjustmentResponse createAdjustment(CreateInventoryAdjustmentRequest request, UUID actorId) {
        
        // 1. Create InventoryAdjustment entity
        InventoryAdjustment adjustment = InventoryAdjustment.builder()
                .referenceCode(UUID.randomUUID())
                .reason(request.getReason())
                .status(InventoryAdjustmentStatus.PENDING)
                .createdBy(actorId)
                .stockTransactions(new ArrayList<>())
                .build();
        
        // 2. Save adjustment first to get ID
        InventoryAdjustment savedAdjustment = adjustmentRepository.save(adjustment);
        
        // 3. Create StockTransaction entities (không apply vào inventory)
        List<StockTransaction> transactions = new ArrayList<>();
        
        for (StockTransactionItemRequest item : request.getItems()) {
            // Validate product variant exists
            ProductVariant variant = variantRepository.findByIdAndIsDeletedFalse(item.getProductVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));
            
            // Get current inventory
            Inventory inventory = inventoryService.getOrCreateInventory(item.getProductVariantId(), actorId);
            
            // Determine quantity (+ for increase, - for decrease)
            int quantity;
            if (isIncreaseType(item.getType())) {
                quantity = item.getQuantity();
            } else if (isDecreaseType(item.getType())) {
                quantity = -item.getQuantity();
                
                // Validate sufficient stock for decrease
                if (inventory.getQuantityAvailable() < item.getQuantity()) {
                    throw new BadRequestException(
                        String.format("Insufficient stock for %s. Available: %d, Requested: %d",
                            variant.getSku(), inventory.getQuantityAvailable(), item.getQuantity())
                    );
                }
            } else {
                throw new BadRequestException("Invalid transaction type: " + item.getType());
            }
            
            // Create transaction (pending, not applied yet)
            StockTransaction transaction = StockTransaction.builder()
                    .inventory(inventory)
                    .inventoryAdjustment(savedAdjustment)
                    .type(item.getType())
                    .quantity(quantity)
                    .quantityBefore(inventory.getQuantityPhysical()) // Snapshot current
                    .quantityAfter(inventory.getQuantityPhysical() + quantity) // Projected
                    .reason(item.getReason())
                    .referenceId(savedAdjustment.getReferenceCode().toString())
                    .createdBy(actorId)
                    .build();
            
            transactions.add(transaction);
        }
        
        // 4. Save all transactions
        List<StockTransaction> savedTransactions = transactionRepository.saveAll(transactions);
        savedAdjustment.setStockTransactions(savedTransactions);
        
        return mapToResponse(savedAdjustment);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryAdjustmentResponse getAdjustmentById(UUID id) {
        InventoryAdjustment adjustment = adjustmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory adjustment not found"));
        
        return mapToResponse(adjustment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryAdjustmentResponse> getAllAdjustments() {
        return adjustmentRepository.findAllByIsDeletedFalse()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryAdjustmentResponse> getAdjustmentsByStatus(InventoryAdjustmentStatus status) {
        return adjustmentRepository.findAllByStatusAndIsDeletedFalse(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InventoryAdjustmentResponse approveAdjustment(UUID adjustmentId, UUID actorId) {
        
        // 1. Get adjustment
        InventoryAdjustment adjustment = adjustmentRepository.findByIdAndIsDeletedFalse(adjustmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory adjustment not found"));
        
        // 2. Validate status
        if (adjustment.getStatus() != InventoryAdjustmentStatus.PENDING) {
            throw new BadRequestException("Only PENDING adjustments can be approved");
        }
        
        // 3. Apply inventory changes
        for (StockTransaction transaction : adjustment.getStockTransactions()) {
            UUID variantId = transaction.getInventory().getProductVariant().getId();
            int absQuantity = Math.abs(transaction.getQuantity());
            
            if (transaction.getQuantity() > 0) {
                // Increase stock
                inventoryService.addStock(variantId, absQuantity, actorId);
            } else {
                // Decrease stock
                inventoryService.reduceStock(variantId, absQuantity, transaction.getReason(), actorId);
            }
        }
        
        // 4. Update adjustment status
        adjustment.setStatus(InventoryAdjustmentStatus.APPROVED);
        adjustment.setUpdatedBy(actorId);
        
        InventoryAdjustment updated = adjustmentRepository.save(adjustment);
        
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public InventoryAdjustmentResponse rejectAdjustment(UUID adjustmentId, String reason, UUID actorId) {
        
        // 1. Get adjustment
        InventoryAdjustment adjustment = adjustmentRepository.findByIdAndIsDeletedFalse(adjustmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory adjustment not found"));
        
        // 2. Validate status
        if (adjustment.getStatus() != InventoryAdjustmentStatus.PENDING) {
            throw new BadRequestException("Only PENDING adjustments can be rejected");
        }
        
        // 3. Update status (no inventory changes)
        adjustment.setStatus(InventoryAdjustmentStatus.REJECTED);
        adjustment.setReason(adjustment.getReason() + " | Rejection reason: " + reason);
        adjustment.setUpdatedBy(actorId);
        
        InventoryAdjustment updated = adjustmentRepository.save(adjustment);
        
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteAdjustment(UUID adjustmentId, UUID actorId) {
        
        // 1. Get adjustment
        InventoryAdjustment adjustment = adjustmentRepository.findByIdAndIsDeletedFalse(adjustmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory adjustment not found"));
        
        // 2. Only allow delete PENDING or REJECTED
        if (adjustment.getStatus() == InventoryAdjustmentStatus.APPROVED) {
            throw new BadRequestException("Cannot delete APPROVED adjustment");
        }
        
        // 3. Soft delete
        adjustment.setIsDeleted(true);
        adjustment.setUpdatedBy(actorId);
        adjustmentRepository.save(adjustment);
    }

    // ==================== HELPER METHODS ====================
    
    private boolean isIncreaseType(StockTransactionType type) {
        return type == StockTransactionType.STOCK_IN ||
               type == StockTransactionType.ADJUSTMENT ||
               type == StockTransactionType.RETURN;
    }
    
    private boolean isDecreaseType(StockTransactionType type) {
        return type == StockTransactionType.STOCK_OUT ||
               type == StockTransactionType.TRANSFER ||
               type == StockTransactionType.ADJUSTMENT;
    }
    
    private InventoryAdjustmentResponse mapToResponse(InventoryAdjustment adjustment) {
        
        // Map transactions
        List<StockTransactionResponse> transactionResponses = adjustment.getStockTransactions()
                .stream()
                .map(this::mapTransactionToResponse)
                .collect(Collectors.toList());
        
        // Get usernames
        String createdByName = getUserName(adjustment.getCreatedBy());
        String updatedByName = adjustment.getUpdatedBy() != null ? getUserName(adjustment.getUpdatedBy()) : null;
        
        return InventoryAdjustmentResponse.builder()
                .id(adjustment.getId())
                .referenceCode(adjustment.getReferenceCode())
                .reason(adjustment.getReason())
                .status(adjustment.getStatus().name())
                .transactions(transactionResponses)
                .createdBy(adjustment.getCreatedBy())
                .createdByName(createdByName)
                .updatedBy(adjustment.getUpdatedBy())
                .updatedByName(updatedByName)
                .createdAt(adjustment.getCreatedAt())
                .updatedAt(adjustment.getUpdatedAt())
                .build();
    }
    
    private StockTransactionResponse mapTransactionToResponse(StockTransaction transaction) {
        
        ProductVariant variant = transaction.getInventory().getProductVariant();
        String createdByName = getUserName(transaction.getCreatedBy());
        
        return StockTransactionResponse.builder()
                .id(transaction.getId())
                .productVariantId(variant.getId())
                .productVariantName(variant.getVariantName())
                .sku(variant.getSku())
                .type(transaction.getType())
                .quantity(transaction.getQuantity())
                .quantityBefore(transaction.getQuantityBefore())
                .quantityAfter(transaction.getQuantityAfter())
                .reason(transaction.getReason())
                .referenceId(transaction.getReferenceId())
                .createdBy(transaction.getCreatedBy())
                .createdByName(createdByName)
                .createdAt(transaction.getCreatedAt())
                .build();
    }
    
    private String getUserName(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getFullName)
                .orElse("Unknown");
    }
}