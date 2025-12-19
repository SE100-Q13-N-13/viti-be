package com.example.viti_be.dto.request;

import com.example.viti_be.model.model_enum.ProductSerialStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateSerialStatusRequest {
    
    @NotNull(message = "Status is required")
    private ProductSerialStatus status;
    
    private String reason; // Lý do thay đổi trạng thái (optional)
}
