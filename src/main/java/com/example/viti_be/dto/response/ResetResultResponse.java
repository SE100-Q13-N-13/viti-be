package com.example.viti_be.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetResultResponse {
    private Integer totalCustomersReset;
    private Integer totalPointsReset;
    private LocalDateTime resetDate;
    private String reason;
}
