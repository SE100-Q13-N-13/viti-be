package com.example.viti_be.service;

import com.example.viti_be.dto.request.SystemConfigRequest;
import com.example.viti_be.dto.response.SystemConfigResponse;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.model.SystemConfig;
import com.example.viti_be.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SystemConfigService {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Transactional
    public SystemConfigResponse createConfig(SystemConfigRequest request) {
        if (systemConfigRepository.existsByConfigKey(request.getConfigKey())) {
            throw new BadRequestException("Config key already exists");
        }

        SystemConfig config = new SystemConfig();
        config.setConfigKey(request.getConfigKey());
        config.setConfigValue(request.getConfigValue());
        config.setDataType(request.getDataType());
        config.setDescription(request.getDescription());
        config.setIsEncrypted(request.getIsEncrypted());

        SystemConfig savedConfig = systemConfigRepository.save(config);
        return mapToResponse(savedConfig);
    }

    @Transactional
    public SystemConfigResponse updateConfig(UUID id, SystemConfigRequest request) {
        SystemConfig config = systemConfigRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("System config not found"));

        // Don't allow changing config key
        if (!config.getConfigKey().equals(request.getConfigKey())) {
            throw new BadRequestException("Cannot change config key");
        }

        config.setConfigValue(request.getConfigValue());
        config.setDataType(request.getDataType());
        config.setDescription(request.getDescription());
        config.setIsEncrypted(request.getIsEncrypted());

        SystemConfig updatedConfig = systemConfigRepository.save(config);
        return mapToResponse(updatedConfig);
    }

    public SystemConfigResponse getConfigById(UUID id) {
        SystemConfig config = systemConfigRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("System config not found"));
        return mapToResponse(config);
    }

    public SystemConfigResponse getConfigByKey(String configKey) {
        SystemConfig config = systemConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new ResourceNotFoundException("System config not found with key: " + configKey));
        
        if (Boolean.TRUE.equals(config.getIsDeleted())) {
            throw new ResourceNotFoundException("System config not found");
        }
        
        return mapToResponse(config);
    }

    public List<SystemConfigResponse> getAllConfigs() {
        return systemConfigRepository.findByIsDeletedFalse().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteConfig(UUID id) {
        SystemConfig config = systemConfigRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("System config not found"));
        config.setIsDeleted(true);
        systemConfigRepository.save(config);
    }

    private SystemConfigResponse mapToResponse(SystemConfig config) {
        return SystemConfigResponse.builder()
                .id(config.getId())
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .dataType(config.getDataType())
                .description(config.getDescription())
                .isEncrypted(config.getIsEncrypted())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
