package com.example.viti_be.service;

import com.example.viti_be.dto.response.CommuneResponse;
import com.example.viti_be.dto.response.ProvinceResponse;
import com.example.viti_be.repository.CommuneRepository;
import com.example.viti_be.repository.ProvinceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressService {

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private CommuneRepository communeRepository;

    public List<ProvinceResponse> getAllProvinces() {
        return provinceRepository.findAllByOrderByNameAsc().stream()
                .map(province -> ProvinceResponse.builder()
                        .code(province.getCode())
                        .name(province.getName())
                        .build())
                .collect(Collectors.toList());
    }

    public List<CommuneResponse> getCommunesByProvince(String provinceCode) {
        return communeRepository.findByProvinceCodeOrderByNameAsc(provinceCode).stream()
                .map(commune -> CommuneResponse.builder()
                        .code(commune.getCode())
                        .name(commune.getName())
                        .provinceCode(commune.getProvince().getCode())
                        .build())
                .collect(Collectors.toList());
    }
}
