package com.example.viti_be.dto.request;
import lombok.Data;

@Data
public class CreateEmployeeRequest {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String phone;
    // Admin có thể chọn role cụ thể cho nhân viên (vd: ROLE_SALES, ROLE_WAREHOUSE)
    private String roleName;
}