package com.example.viti_be.controller;

import com.example.viti_be.dto.request.CreateEmployeeRequest;
import com.example.viti_be.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    AuthService authService;

    @PostMapping("/create-employee")
    public ResponseEntity<?> createEmployee(@RequestBody CreateEmployeeRequest request) {
        authService.createEmployee(request);
        return ResponseEntity.ok("Employee created successfully!");
    }
}