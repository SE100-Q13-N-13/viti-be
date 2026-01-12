package com.example.viti_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LinkGoogleAccountRequest {
    @NotBlank(message = "Google ID token is required")
    private String idToken;
}