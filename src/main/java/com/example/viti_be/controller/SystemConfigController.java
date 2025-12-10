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
        ApiResponse<SystemConfigResponse> response = ApiResponse.<SystemConfigResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("System config created successfully")
                .result(config)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> updateConfig(
            @PathVariable UUID id,
            @Valid @RequestBody SystemConfigRequest request) {
        SystemConfigResponse config = systemConfigService.updateConfig(id, request);
        ApiResponse<SystemConfigResponse> response = ApiResponse.<SystemConfigResponse>builder()
                .code(HttpStatus.OK.value())
                .message("System config updated successfully")
                .result(config)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> getConfigById(@PathVariable UUID id) {
        SystemConfigResponse config = systemConfigService.getConfigById(id);
        ApiResponse<SystemConfigResponse> response = ApiResponse.<SystemConfigResponse>builder()
                .code(HttpStatus.OK.value())
                .message("System config retrieved successfully")
                .result(config)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/key/{configKey}")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> getConfigByKey(@PathVariable String configKey) {
        SystemConfigResponse config = systemConfigService.getConfigByKey(configKey);
        ApiResponse<SystemConfigResponse> response = ApiResponse.<SystemConfigResponse>builder()
                .code(HttpStatus.OK.value())
                .message("System config retrieved successfully")
                .result(config)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemConfigResponse>>> getAllConfigs() {
        List<SystemConfigResponse> configs = systemConfigService.getAllConfigs();
        ApiResponse<List<SystemConfigResponse>> response = ApiResponse.<List<SystemConfigResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("System configs retrieved successfully")
                .result(configs)
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable UUID id) {
        systemConfigService.deleteConfig(id);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("System config deleted successfully")
                .build();
        return ResponseEntity.ok(response);
    }
}
