package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.SystemConfigRequest;
import com.example.viti_be.dto.response.SystemConfigResponse;
import com.example.viti_be.exception.BadRequestException;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.model.SystemConfig;
import com.example.viti_be.repository.SystemConfigRepository;
import com.example.viti_be.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SystemConfigServiceImpl implements SystemConfigService {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Override
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

    @Override
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

    @Override
    public SystemConfigResponse getConfigById(UUID id) {
        SystemConfig config = systemConfigRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("System config not found"));
        return mapToResponse(config);
    }

    @Override
    public SystemConfigResponse getConfigByKey(String configKey) {
        SystemConfig config = systemConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new ResourceNotFoundException("System config not found with key: " + configKey));

        if (Boolean.TRUE.equals(config.getIsDeleted())) {
            throw new ResourceNotFoundException("System config not found");
        }

        return mapToResponse(config);
    }

    @Override
    public List<SystemConfigResponse> getAllConfigs() {
        return systemConfigRepository.findByIsDeletedFalse().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteConfig(UUID id) {
        SystemConfig config = systemConfigRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("System config not found"));
        config.setIsDeleted(true);
        systemConfigRepository.save(config);
    }

    @Override
    public String getStringValue(String key, String defaultValue) {
        try {
            SystemConfig config = systemConfigRepository.findByConfigKey(key)
                    .orElse(null);

            if (config == null || Boolean.TRUE.equals(config.getIsDeleted())) {
                log.warn("Config key not found, using default: {} = {}", key, defaultValue);
                return defaultValue;
            }

            return config.getConfigValue();
        } catch (Exception e) {
            log.error("Error getting config {}, using default: {}", key, defaultValue, e);
            return defaultValue;
        }
    }

    @Override
    public Integer getIntegerValue(String key, Integer defaultValue) {
        try {
            String value = getStringValue(key, null);
            if (value == null) {
                return defaultValue;
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.error("Error parsing integer config {}, using default: {}", key, defaultValue, e);
            return defaultValue;
        }
    }

    @Override
    public BigDecimal getDecimalValue(String key, BigDecimal defaultValue) {
        try {
            String value = getStringValue(key, null);
            if (value == null) {
                return defaultValue;
            }
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.error("Error parsing decimal config {}, using default: {}", key, defaultValue, e);
            return defaultValue;
        }
    }

    @Override
    public Boolean getBooleanValue(String key, Boolean defaultValue) {
        try {
            String value = getStringValue(key, null);
            if (value == null) {
                return defaultValue;
            }
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            log.error("Error parsing boolean config {}, using default: {}", key, defaultValue, e);
            return defaultValue;
        }
    }

    // ========================================
    // BUSINESS-SPECIFIC HELPERS
    // ========================================

    @Override
    public BigDecimal getPartMarkupPercent() {
        return getDecimalValue("PART_MARKUP_PERCENT", new BigDecimal("30.00"));
    }

    @Override
    public Integer getWarrantyPeriodMonths() {
        return getIntegerValue("WARRANTY_PERIOD_MONTHS", 12);
    }

    @Override
    public Integer getWarrantyReturnDays() {
        return getIntegerValue("WARRANTY_RETURN_DAYS", 7);
    }

    @Override
    public BigDecimal getVatPercent() {
        return getDecimalValue("VAT_PERCENT", new BigDecimal("10.00"));
    }

    @Override
    public BigDecimal getMaxEmployeeDiscountPercent() {
        return getDecimalValue("MAX_EMPLOYEE_DISCOUNT_PERCENT", new BigDecimal("15.00"));
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

