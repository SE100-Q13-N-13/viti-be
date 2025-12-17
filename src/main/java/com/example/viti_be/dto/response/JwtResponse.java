package com.example.viti_be.dto.response;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter @Setter @AllArgsConstructor
public class JwtResponse {
    private String accessToken;
    private String type = "Bearer";
    private String refreshToken;
    private UUID id;
    private String username;
    private String email;
    private List<String> roles;
    private Boolean isFirstLogin;

    public JwtResponse(String accessToken, String refreshToken, UUID id, String username, String email, List<String> roles, Boolean isFirstLogin) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.isFirstLogin = isFirstLogin;
    }

    public JwtResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}