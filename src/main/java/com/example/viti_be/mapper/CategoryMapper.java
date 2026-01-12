package com.example.viti_be.mapper;

import com.example.viti_be.dto.response.CategoryResponse;
import com.example.viti_be.dto.response.CategorySpecResponse;
import com.example.viti_be.model.Category;
import com.example.viti_be.model.CategorySpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class CategoryMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    // --- MAPPING CATEGORY ---

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    @Mapping(target = "relatedCategoryIds", source = "relatedCategoryIds", qualifiedByName = "jsonIdsToList")
    public abstract CategoryResponse toCategoryResponse(Category category);

    public abstract List<CategoryResponse> toCategoryResponseList(List<Category> categories);

    // --- MAPPING CATEGORY SPEC ---

    public abstract CategorySpecResponse toSpecResponse(CategorySpec spec);

    public abstract List<CategorySpecResponse> toSpecResponseList(List<CategorySpec> specs);

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