package com.example.viti_be.dto.request;

import com.example.viti_be.model.model_enum.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserStatusRequest {

    @NotNull(message = "Status is required")
    private UserStatus newStatus;

    private String reason; // Lý do thay đổi status (optional)
}