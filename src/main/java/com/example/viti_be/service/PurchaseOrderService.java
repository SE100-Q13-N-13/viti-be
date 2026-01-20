package com.example.viti_be.service;

import com.example.viti_be.dto.request.PurchaseOrderRequest;
import com.example.viti_be.dto.request.ReceiveGoodsRequest;
import com.example.viti_be.dto.response.PurchaseOrderResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PurchaseOrderService {
    
    /**
     * Create a new Purchase Order in DRAFT status
     * @param request PurchaseOrderRequest containing supplier, items, and expected delivery date
     * @param createdBy UUID of the user creating the PO
     * @return PurchaseOrderResponse with created PO details
     */
    PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request, UUID createdBy);
    
    /**
     * Confirm goods receipt - Stock In operation
     * Updates inventory, calculates Moving Average Cost, and creates stock transactions
     * @param purchaseOrderId UUID of the purchase order
     * @param request ReceiveGoodsRequest containing actual delivery date and received quantities
     * @param updatedBy UUID of the user confirming the receipt
     * @return PurchaseOrderResponse with updated PO details
     */
    PurchaseOrderResponse receiveGoods(UUID purchaseOrderId, ReceiveGoodsRequest request, UUID updatedBy);
    
    /**
     * Get Purchase Order by ID
     * @param id UUID of the purchase order
     * @return PurchaseOrderResponse
     */
    PurchaseOrderResponse getPurchaseOrderById(UUID id);
    
    /**
     * Get all Purchase Orders
     * @return List of PurchaseOrderResponse
     */
    PageResponse<PurchaseOrderResponse> getAllPurchaseOrders(Pageable pageable);
    
    /**
     * Get Purchase Orders by Supplier ID
     * @param supplierId UUID of the supplier
     * @return List of PurchaseOrderResponse
     */
    List<PurchaseOrderResponse> getPurchaseOrdersBySupplierId(UUID supplierId);
    
    /**
     * Close a Purchase Order (RECEIVED -> CLOSED)
     * @param purchaseOrderId UUID of the purchase order
     * @param updatedBy UUID of the user closing the PO
     * @return PurchaseOrderResponse with updated status
     */
    PurchaseOrderResponse closePurchaseOrder(UUID purchaseOrderId, UUID updatedBy);
    
    /**
     * Delete (soft delete) a Purchase Order
     * Only DRAFT status can be deleted
     * @param purchaseOrderId UUID of the purchase order
     * @param deletedBy UUID of the user deleting the PO
     */
    void deletePurchaseOrder(UUID purchaseOrderId, UUID deletedBy);
}
