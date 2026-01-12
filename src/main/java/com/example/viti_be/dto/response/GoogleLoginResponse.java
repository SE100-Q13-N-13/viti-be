package com.example.viti_be.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoogleLoginResponse {
    private String accessToken;
    private String refreshToken;
    private UUID id;
    private String username;
    private String email;
    private List<String> roles;
    private Boolean isFirstLogin;

    private Boolean hasEmailProvider;  // Đã có email/password chưa
    private Boolean shouldPromptLinking; // Có nên hiện modal hỏi tạo acc không
    private String linkingStatus; // "NOT_LINKED", "LINKED", "SKIPPED"
}