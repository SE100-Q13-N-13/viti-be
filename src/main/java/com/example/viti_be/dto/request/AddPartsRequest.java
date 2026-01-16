package com.example.viti_be.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddPartsRequest {

    @NotNull(message = "Parts list cannot be null")
    @Size(min = 1, message = "At least one part is required")
    @Valid
    private List<PartItemRequest> parts;
}
