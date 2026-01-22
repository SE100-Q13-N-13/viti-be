package com.example.viti_be.mapper;

import com.example.viti_be.dto.response.CategoryResponse;
import com.example.viti_be.dto.response.CategorySpecResponse;
import com.example.viti_be.model.Category;
import com.example.viti_be.model.CategorySpec;
import com.example.viti_be.repository.CategorySpecRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class CategoryMapper {

    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected CategorySpecRepository categorySpecRepository;

    // --- MAPPING CATEGORY ---

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    @Mapping(target = "relatedCategoryIds", source = "relatedCategoryIds", qualifiedByName = "jsonIdsToList")
    @Mapping(target = "specs", ignore = true)
    public abstract CategoryResponse toCategoryResponse(Category category);

    public abstract List<CategoryResponse> toCategoryResponseList(List<Category> categories);
    
    @AfterMapping
    protected void enrichWithInheritedSpecs(@MappingTarget CategoryResponse response, Category category) {
        List<CategorySpecResponse> allSpecs = new ArrayList<>();
        
        // Add parent specs first (inherited) - fetch from repository to avoid lazy loading
        if (category.getParent() != null) {
            Category parent = category.getParent();
            List<CategorySpec> parentSpecs = categorySpecRepository.findByCategoryIdAndIsDeletedFalse(parent.getId());
            
            List<CategorySpecResponse> parentSpecResponses = parentSpecs.stream()
                .map(spec -> {
                    CategorySpecResponse specResponse = toSpecResponse(spec);
                    specResponse.setIsInherited(true);
                    specResponse.setInheritedFromCategoryId(parent.getId());
                    specResponse.setInheritedFromCategoryName(parent.getName());
                    return specResponse;
                })
                .collect(Collectors.toList());
            allSpecs.addAll(parentSpecResponses);
        }
        
        // Add own specs
        if (category.getSpecs() != null) {
            List<CategorySpecResponse> ownSpecs = category.getSpecs().stream()
                .filter(spec -> spec.getIsDeleted() == null || !spec.getIsDeleted())
                .map(spec -> {
                    CategorySpecResponse specResponse = toSpecResponse(spec);
                    specResponse.setIsInherited(false);
                    return specResponse;
                })
                .collect(Collectors.toList());
            allSpecs.addAll(ownSpecs);
        }
        
        response.setSpecs(allSpecs);
    }

    // --- MAPPING CATEGORY SPEC ---

    public abstract CategorySpecResponse toSpecResponse(CategorySpec spec);

    public abstract List<CategorySpecResponse> toSpecResponseList(List<CategorySpec> specs);
    
    /**
     * Map specs with inheritance information
     */
    public List<CategorySpecResponse> toSpecResponseListWithInheritance(List<CategorySpec> specs, UUID currentCategoryId) {
        return specs.stream().map(spec -> {
            CategorySpecResponse response = toSpecResponse(spec);
            
            // Check if spec belongs to current category or inherited from parent
            boolean isInherited = !spec.getCategory().getId().equals(currentCategoryId);
            response.setIsInherited(isInherited);
            
            if (isInherited) {
                response.setInheritedFromCategoryId(spec.getCategory().getId());
                response.setInheritedFromCategoryName(spec.getCategory().getName());
            }
            
            return response;
        }).toList();
    }

    @Named("jsonIdsToList")
    List<UUID> mapJsonIds(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<UUID>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}