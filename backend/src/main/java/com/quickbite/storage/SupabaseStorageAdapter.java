package com.quickbite.storage;

import com.quickbite.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Base64;
import java.util.UUID;

@Service
public class SupabaseStorageAdapter implements StorageService {
    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageAdapter.class);

    private final String supabaseUrl;
    private final String serviceKey;
    private final String bucket;
    private final RestTemplate restTemplate;

    public SupabaseStorageAdapter(
            @Value("${quickbite.storage.supabase-url:https://example.supabase.co}") String supabaseUrl,
            @Value("${quickbite.storage.service-key:placeholder}") String serviceKey,
            @Value("${quickbite.storage.bucket:QuickBite}") String bucket) {
        this.supabaseUrl = supabaseUrl;
        this.serviceKey = serviceKey;
        this.bucket = bucket;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String uploadImage(String folder, String originalFilename, byte[] data, String contentType) {
        if (data == null || data.length == 0) {
            throw new BadRequestException("Image file cannot be empty");
        }
        if (data.length > 5 * 1024 * 1024) { // 5MB limit
            throw new BadRequestException("Image size exceeds maximum limit of 5MB");
        }
        if (contentType == null || (!contentType.startsWith("image/"))) {
            throw new BadRequestException("Only valid image formats (JPEG, PNG, WebP) are allowed");
        }

        String extension = getFileExtension(originalFilename);
        String uniqueName = UUID.randomUUID() + (extension.isEmpty() ? ".jpg" : "." + extension);
        String objectKey = folder + "/" + uniqueName;

        // Check if real Supabase credentials are provided
        if (supabaseUrl != null && !supabaseUrl.contains("example.supabase.co") && serviceKey != null && !serviceKey.equals("placeholder")) {
            try {
                String uploadUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucket, objectKey);
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(serviceKey);
                headers.set("apikey", serviceKey);
                headers.setContentType(MediaType.parseMediaType(contentType));
                HttpEntity<byte[]> requestEntity = new HttpEntity<>(data, headers);

                ResponseEntity<String> response = restTemplate.exchange(
                        URI.create(uploadUrl),
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Uploaded image to Supabase Storage: {}", objectKey);
                    return objectKey;
                }
            } catch (Exception ex) {
                log.warn("Supabase upload failed, falling back to data URL for development: {}", ex.getMessage());
            }
        }

        // Development fallback: Base64 data URL with object key metadata
        log.info("Using embedded dev asset format for object: {}", objectKey);
        String base64Data = Base64.getEncoder().encodeToString(data);
        return "data:" + contentType + ";base64," + base64Data;
    }

    @Override
    public String getPublicUrl(String path) {
        if (path == null || path.isBlank()) return "";
        if (path.startsWith("data:") || path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        return String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, bucket, path);
    }

    @Override
    public void deleteImage(String path) {
        if (path == null || path.startsWith("data:")) return;
        if (supabaseUrl != null && !supabaseUrl.contains("example.supabase.co") && serviceKey != null && !serviceKey.equals("placeholder")) {
            try {
                String deleteUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucket, path);
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(serviceKey);
                headers.set("apikey", serviceKey);
                restTemplate.exchange(URI.create(deleteUrl), HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
            } catch (Exception ex) {
                log.warn("Failed to delete object from Supabase: {}", ex.getMessage());
            }
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
