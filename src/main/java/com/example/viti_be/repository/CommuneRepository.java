package com.example.viti_be.repository;

import com.example.viti_be.model.Commune;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommuneRepository extends JpaRepository<Commune, String> {
    List<Commune> findByProvinceCodeOrderByNameAsc(String provinceCode);
}
