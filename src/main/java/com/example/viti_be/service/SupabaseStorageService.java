package com.example.viti_be.service;

public interface SupabaseStorageService {

    /**
     * Upload file to Supabase Storage
     * @return Public URL of uploaded file
     */
    String uploadFile(String bucketName, String fileName, byte[] fileBytes, String contentType);

    /**
     * Delete file from Supabase Storage
     */
    void deleteFile(String bucketName, String fileName);
}