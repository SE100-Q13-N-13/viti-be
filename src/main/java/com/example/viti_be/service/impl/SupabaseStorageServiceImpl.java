package com.example.viti_be.service.impl;

import com.example.viti_be.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseStorageServiceImpl implements SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String uploadFile(String bucketName, String fileName, byte[] fileBytes, String contentType) {
        try {
            String uploadUrl = String.format("%s/storage/v1/object/%s/%s",
                    supabaseUrl, bucketName, fileName);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.setContentType(MediaType.parseMediaType(contentType));

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileBytes, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                // Return public URL
                return String.format("%s/storage/v1/object/public/%s/%s",
                        supabaseUrl, bucketName, fileName);
            } else {
                throw new RuntimeException("Failed to upload file to Supabase Storage");
            }

        } catch (Exception e) {
            log.error("Error uploading file to Supabase: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Override
    public void deleteFile(String bucketName, String fileName) {
        try {
            String deleteUrl = String.format("%s/storage/v1/object/%s/%s",
                    supabaseUrl, bucketName, fileName);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            restTemplate.exchange(
                    deleteUrl,
                    HttpMethod.DELETE,
                    requestEntity,
                    String.class
            );

        } catch (Exception e) {
            log.error("Error deleting file from Supabase: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }
}