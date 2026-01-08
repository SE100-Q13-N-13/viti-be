package com.example.viti_be.service;

import com.example.viti_be.dto.response.InventoryResponse;
import com.example.viti_be.dto.response.ProductSerialResponse;
import com.example.viti_be.model.Inventory;
import com.example.viti_be.model.ProductSerial;
import com.example.viti_be.model.model_enum.ProductSerialStatus;

import java.util.List;
import java.util.UUID;

public interface InventoryService {
    
    /**
     * Get or create inventory for a product variant
     * @param productVariantId UUID of the product variant
     * @param createdBy UUID of the user creating the inventory
     * @return Inventory entity
     */
    Inventory getOrCreateInventory(UUID productVariantId, UUID createdBy);
    
    /**
     * Add stock to inventory
     * @param productVariantId UUID of the product variant
     * @param quantity Quantity to add
     * @param createdBy UUID of the user
     * @return Updated Inventory
     */
    Inventory addStock(UUID productVariantId, int quantity, UUID createdBy);
    
    /**
     * Get inventory by product variant ID
     * @param productVariantId UUID of the product variant
     * @return InventoryResponse
     */
    InventoryResponse getInventoryByProductVariantId(UUID productVariantId);
    
    /**
     * Get all inventory items
     * @return List of InventoryResponse
     */
    List<InventoryResponse> getAllInventory();
    
    /**
     * Get low stock items (below threshold)
     * @return List of InventoryResponse
     */
    List<InventoryResponse> getLowStockItems();
    
    /**
     * Get serials by product variant ID
     * @param productVariantId UUID of the product variant
     * @param status Optional status filter
     * @return List of ProductSerialResponse
     */
    List<ProductSerialResponse> getSerialsByProductVariantId(UUID productVariantId, ProductSerialStatus status);
    
    /**
     * Get serial by serial number
     * @param serialNumber Serial number to search
     * @return ProductSerialResponse
     */
    ProductSerialResponse getSerialByNumber(String serialNumber);
    
    /**
     * Check if serial is available for sale (Helper for Order module)
     * @param serialNumber Serial number to check
     * @return true if serial exists and status is AVAILABLE
     */
    boolean isSerialAvailableForSale(String serialNumber);
    
    /**
     * Update serial status
     * @param serialNumber Serial number
     * @param newStatus New status
     * @param updatedBy User updating the status
     * @return Updated ProductSerial
     */
    ProductSerial updateSerialStatus(String serialNumber, ProductSerialStatus newStatus, UUID updatedBy);
    
    /**
     * Get all serials by status
     * @param status ProductSerialStatus
     * @return List of ProductSerialResponse
     */
    List<ProductSerialResponse> getSerialsByStatus(ProductSerialStatus status);
    
    // ==================== Methods for Order Module ====================
    
    /**
     * Mark serial as SOLD when order is completed
     * @param serialNumber Serial number
     * @param orderId Order ID
     * @param updatedBy User completing the order
     * @return Updated ProductSerial
     */
    ProductSerial markSerialAsSold(String serialNumber, UUID orderId, UUID updatedBy);
    
    /**
     * Release serial (set to AVAILABLE) when order is cancelled
     * @param serialNumber Serial number
     * @param updatedBy User cancelling the order
     * @return Updated ProductSerial
     */
    ProductSerial releaseSerial(String serialNumber, UUID updatedBy);
    
    /**
     * Mark multiple serials as SOLD in batch (for orders with multiple items)
     * @param serialNumbers List of serial numbers
     * @param orderId Order ID
     * @param updatedBy User completing the order
     */
    void markSerialsAsSold(List<String> serialNumbers, UUID orderId, UUID updatedBy);
    
    /**
     * Release multiple serials in batch (when order is cancelled)
     * @param serialNumbers List of serial numbers
     * @param updatedBy User cancelling the order
     */
    void releaseSerials(List<String> serialNumbers, UUID updatedBy);
    
    /**
     * Mark serial as WARRANTY
     * @param serialNumber Serial number
     * @param warrantyExpireDate Warranty expiration date
     * @param updatedBy User
     * @return Updated ProductSerial
     */
    ProductSerial markSerialAsWarranty(String serialNumber, java.time.LocalDateTime warrantyExpireDate, UUID updatedBy);
    
    /**
     * Mark serial as DEFECTIVE
     * @param serialNumber Serial number
     * @param updatedBy User
     * @return Updated ProductSerial
     */
    ProductSerial markSerialAsDefective(String serialNumber, UUID updatedBy);
}
