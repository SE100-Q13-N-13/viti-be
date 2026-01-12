package com.example.viti_be.mapper;

import com.example.viti_be.dto.response.ProductResponse;
import com.example.viti_be.dto.response.ProductVariantResponse;
import com.example.viti_be.model.Product;
import com.example.viti_be.model.ProductVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    // --- MAPPING PRODUCT ---

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "supplierName", source = "supplier.name") // Giả sử Supplier có field name
    @Mapping(target = "variants", source = "variants") // Map list variants con
    ProductResponse toProductResponse(Product product);

    List<ProductResponse> toProductResponseList(List<Product> products);

    // --- MAPPING VARIANT ---

    ProductVariantResponse toVariantResponse(ProductVariant variant);

    List<ProductVariantResponse> toVariantResponseList(List<ProductVariant> variants);
}