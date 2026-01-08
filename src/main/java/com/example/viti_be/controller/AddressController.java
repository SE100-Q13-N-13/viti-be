package com.example.viti_be.controller;

import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.CommuneResponse;
import com.example.viti_be.dto.response.ProvinceResponse;
import com.example.viti_be.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<ProvinceResponse>>> getAllProvinces() {
        List<ProvinceResponse> provinces = addressService.getAllProvinces();
        return ResponseEntity.ok(ApiResponse.success(provinces, "Provinces retrieved successfully"));
    }

    @GetMapping("/communes")
    public ResponseEntity<ApiResponse<List<CommuneResponse>>> getCommunesByProvince(
            @RequestParam String provinceCode) {
        List<CommuneResponse> communes = addressService.getCommunesByProvince(provinceCode);
        return ResponseEntity.ok(ApiResponse.success(communes, "Communes retrieved successfully"));
    }
}
