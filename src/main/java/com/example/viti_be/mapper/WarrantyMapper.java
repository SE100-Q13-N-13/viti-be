package com.example.viti_be.mapper;

import com.example.viti_be.dto.response.*;
import com.example.viti_be.model.*;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct Mapper cho Warranty Module
 */
@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface WarrantyMapper {

    // ========== TICKET MAPPINGS ==========

    /**
     * Map WarrantyTicket -> WarrantyTicketResponse
     */
    @Mapping(target = "product", source = "ticket", qualifiedByName = "mapProductInfo")
    @Mapping(target = "serialNumber", source = "productSerial.serialNumber")
    @Mapping(target = "customer", source = "ticket", qualifiedByName = "mapCustomerInfo")
    @Mapping(target = "technician", source = "ticket", qualifiedByName = "mapTechnicianInfo")
    @Mapping(target = "services", source = "services")
    @Mapping(target = "parts", source = "parts")
    WarrantyTicketResponse toTicketResponse(WarrantyTicket ticket);

    /**
     * Map list tickets
     */
    List<WarrantyTicketResponse> toTicketResponseList(List<WarrantyTicket> tickets);

    /**
     * Map WarrantyTicket -> WarrantyTicketSummaryResponse (cho list/dashboard)
     */
    @Mapping(target = "serialNumber", source = "productSerial.serialNumber")
    @Mapping(target = "productName", source = "productVariant.product.name")
    @Mapping(target = "technicianName", source = "technician.fullName")
    WarrantyTicketSummaryResponse toTicketSummaryResponse(WarrantyTicket ticket);

    /**
     * Map list tickets to summary
     */
    List<WarrantyTicketSummaryResponse> toTicketSummaryResponseList(List<WarrantyTicket> tickets);

    // ========== SERVICE MAPPINGS ==========

    /**
     * Map WarrantyTicketService -> ServiceItemResponse
     */
    @Mapping(target = "repairServiceId", source = "repairService.id")
    @Mapping(target = "serviceName", source = "repairService.name")
    @Mapping(target = "serviceDescription", source = "repairService.description")
    ServiceItemResponse toServiceItemResponse(WarrantyTicketService service);

    /**
     * Map list services
     */
    List<ServiceItemResponse> toServiceItemResponseList(List<WarrantyTicketService> services);

    // ========== PART MAPPINGS ==========

    /**
     * Map WarrantyTicketPart -> PartItemResponse
     */
    @Mapping(target = "partComponentId", source = "partComponent.id")
    @Mapping(target = "partName", source = "partComponent.name")
    @Mapping(target = "partType", source = "partComponent.partType")
    PartItemResponse toPartItemResponse(WarrantyTicketPart part);

    /**
     * Map list parts
     */
    List<PartItemResponse> toPartItemResponseList(List<WarrantyTicketPart> parts);

    // ========== REPAIR SERVICE MAPPINGS ==========

    /**
     * Map RepairService -> RepairServiceResponse
     */
    RepairServiceResponse toRepairServiceResponse(RepairService repairService);

    /**
     * Map list repair services
     */
    List<RepairServiceResponse> toRepairServiceResponseList(List<RepairService> repairServices);

    // ========== PART COMPONENT MAPPINGS ==========

    /**
     * Map PartComponent -> PartComponentResponse
     */
    @Mapping(target = "supplier", source = "partComponent", qualifiedByName = "mapSupplierInfo")
    @Mapping(target = "currentStock", ignore = true) // Set manually in service
    PartComponentResponse toPartComponentResponse(PartComponent partComponent);

    /**
     * Map list part components
     */
    List<PartComponentResponse> toPartComponentResponseList(List<PartComponent> partComponents);

    // ========== NAMED MAPPING METHODS ==========

    /**
     * Map Product info
     */
    @Named("mapProductInfo")
    default WarrantyTicketResponse.ProductInfo mapProductInfo(WarrantyTicket ticket) {
        if (ticket.getProductVariant() == null) {
            return null;
        }

        ProductVariant variant = ticket.getProductVariant();
        Product product = variant.getProduct();

        return WarrantyTicketResponse.ProductInfo.builder()
                .variantId(variant.getId())
                .sku(variant.getSku())
                .productName(product != null ? product.getName() : null)
                .variantName(variant.getVariantName())
                .build();
    }

    /**
     * Map Customer info
     */
    @Named("mapCustomerInfo")
    default WarrantyTicketResponse.CustomerInfo mapCustomerInfo(WarrantyTicket ticket) {
        if (ticket.getCustomer() != null) {
            Customer customer = ticket.getCustomer();
            return WarrantyTicketResponse.CustomerInfo.builder()
                    .customerId(customer.getId())
                    .name(customer.getFullName())
                    .phone(customer.getPhone())
                    .email(customer.getEmail())
                    .build();
        } else {
            // Guest customer
            return WarrantyTicketResponse.CustomerInfo.builder()
                    .customerId(null)
                    .name(ticket.getCustomerName())
                    .phone(ticket.getCustomerPhone())
                    .email(null)
                    .build();
        }
    }

    /**
     * Map Technician info
     */
    @Named("mapTechnicianInfo")
    default WarrantyTicketResponse.TechnicianInfo mapTechnicianInfo(WarrantyTicket ticket) {
        if (ticket.getTechnician() == null) {
            return null;
        }

        User technician = ticket.getTechnician();
        return WarrantyTicketResponse.TechnicianInfo.builder()
                .userId(technician.getId())
                .fullName(technician.getFullName())
                .email(technician.getEmail())
                .build();
    }

    /**
     * Map Supplier info
     */
    @Named("mapSupplierInfo")
    default PartComponentResponse.SupplierInfo mapSupplierInfo(PartComponent partComponent) {
        if (partComponent.getSupplier() == null) {
            return null;
        }

        Supplier supplier = partComponent.getSupplier();
        return PartComponentResponse.SupplierInfo.builder()
                .supplierId(supplier.getId())
                .name(supplier.getName())
                .build();
    }
}