package com.example.viti_be.config;

import com.example.viti_be.model.Commune;
import com.example.viti_be.model.Province;
import com.example.viti_be.repository.CommuneRepository;
import com.example.viti_be.repository.ProvinceRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private CommuneRepository communeRepository;

    @Override
    public void run(String... args) throws Exception {
        // Only load data if tables are empty
        if (provinceRepository.count() == 0) {
            loadProvinces();
        }
        
        if (communeRepository.count() == 0) {
            loadCommunes();
        }
    }

    private void loadProvinces() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = new ClassPathResource("data/provinces.json").getInputStream();
        
        List<Map<String, String>> provincesData = mapper.readValue(
            inputStream, 
            new TypeReference<List<Map<String, String>>>() {}
        );

        for (Map<String, String> data : provincesData) {
            Province province = new Province();
            province.setCode(data.get("code"));
            province.setName(data.get("name"));
            provinceRepository.save(province);
        }
        
        System.out.println("Loaded " + provincesData.size() + " provinces");
    }

    private void loadCommunes() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = new ClassPathResource("data/communes.json").getInputStream();
        
        List<Map<String, String>> communesData = mapper.readValue(
            inputStream, 
            new TypeReference<List<Map<String, String>>>() {}
        );

        // Cache provinces to avoid repeated database queries
        Map<String, Province> provinceCache = new HashMap<>();
        provinceRepository.findAll().forEach(p -> provinceCache.put(p.getCode(), p));

        for (Map<String, String> data : communesData) {
            String provinceCode = data.get("provinceCode");
            Province province = provinceCache.get(provinceCode);
            
            if (province != null) {
                Commune commune = new Commune();
                commune.setCode(data.get("code"));
                commune.setName(data.get("name"));
                commune.setProvince(province);
                communeRepository.save(commune);
            }
        }
        
        System.out.println("Loaded " + communesData.size() + " communes");
    }
}
