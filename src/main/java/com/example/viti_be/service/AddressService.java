package com.example.viti_be.service;

import com.example.viti_be.dto.response.CommuneResponse;
import com.example.viti_be.dto.response.ProvinceResponse;

import java.util.List;

public interface AddressService {
    List<ProvinceResponse> getAllProvinces();
    List<CommuneResponse> getCommunesByProvince(String provinceCode);
}

