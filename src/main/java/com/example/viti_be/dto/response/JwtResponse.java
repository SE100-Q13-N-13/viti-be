package com.example.viti_be.dto.response;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter @Setter @AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private UUID id;
    private String username;
    private String email;
    private List<String> roles;
    private Boolean isFirstLogin;

    public JwtResponse(String accessToken, UUID id, String username, String email, List<String> roles, Boolean isFirstLogin) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.isFirstLogin = isFirstLogin;
    }
}