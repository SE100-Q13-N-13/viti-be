package com.example.viti_be.controller;

import com.example.viti_be.dto.request.SystemConfigRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.SystemConfigResponse;
import com.example.viti_be.service.SystemConfigService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/system-config")
@PreAuthorize("hasRole('ADMIN')")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    @PostMapping
    public ResponseEntity<ApiResponse<SystemConfigResponse>> createConfig(@Valid @RequestBody SystemConfigRequest request) {
        SystemConfigResponse config = systemConfigService.createConfig(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(config, "System config created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> updateConfig(
            @PathVariable UUID id,
            @Valid @RequestBody SystemConfigRequest request) {
        SystemConfigResponse config = systemConfigService.updateConfig(id, request);
        return ResponseEntity.ok(ApiResponse.success(config, "System config updated successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> getConfigById(@PathVariable UUID id) {
        SystemConfigResponse config = systemConfigService.getConfigById(id);
        return ResponseEntity.ok(ApiResponse.success(config, "System config retrieved successfully"));
    }

    @GetMapping("/key/{configKey}")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> getConfigByKey(@PathVariable String configKey) {
        SystemConfigResponse config = systemConfigService.getConfigByKey(configKey);
        return ResponseEntity.ok(ApiResponse.success(config, "System config retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemConfigResponse>>> getAllConfigs() {
        List<SystemConfigResponse> configs = systemConfigService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.success(configs, "System configs retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable UUID id) {
        systemConfigService.deleteConfig(id);
        return ResponseEntity.ok(ApiResponse.success(null, "System config deleted successfully"));
    }
}
