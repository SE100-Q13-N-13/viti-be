package com.example.viti_be.service.impl;

import com.example.viti_be.dto.response.CommuneResponse;
import com.example.viti_be.dto.response.ProvinceResponse;
import com.example.viti_be.repository.CommuneRepository;
import com.example.viti_be.repository.ProvinceRepository;
import com.example.viti_be.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private CommuneRepository communeRepository;

    @Override
    public List<ProvinceResponse> getAllProvinces() {
        return provinceRepository.findAllByOrderByNameAsc().stream()
                .map(province -> ProvinceResponse.builder()
                        .code(province.getCode())
                        .name(province.getName())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<CommuneResponse> getCommunesByProvince(String provinceCode) {
        return communeRepository.findByProvince_CodeOrderByNameAsc(provinceCode).stream()
                .map(commune -> CommuneResponse.builder()
                        .code(commune.getCode())
                        .name(commune.getName())
                        .provinceCode(commune.getProvince().getCode())
                        .build())
                .collect(Collectors.toList());
    }
}

