package com.example.viti_be.service;

import com.example.viti_be.dto.request.SystemConfigRequest;
import com.example.viti_be.dto.response.SystemConfigResponse;

import java.util.List;
import java.util.UUID;

public interface SystemConfigService {
    SystemConfigResponse createConfig(SystemConfigRequest request);
    SystemConfigResponse updateConfig(UUID id, SystemConfigRequest request);
    SystemConfigResponse getConfigById(UUID id);
    SystemConfigResponse getConfigByKey(String configKey);
    List<SystemConfigResponse> getAllConfigs();
    void deleteConfig(UUID id);
}

