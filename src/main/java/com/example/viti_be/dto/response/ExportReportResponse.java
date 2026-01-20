package com.example.viti_be.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportReportResponse {
    private String fileUrl; // Supabase Storage URL
    private String fileName;
    private String message;
}