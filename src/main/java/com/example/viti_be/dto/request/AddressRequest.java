package com.example.viti_be.dto.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressRequest {

    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "Commune code is required")
    private String communeCode;

    @NotBlank(message = "Province code is required")
    private String provinceCode;

    private String type;

    private Boolean isPrimary = false;

    private String postalCode;

    private String contactName;

    private String phoneNumber;
}
