package com.quickbite.storage;

import com.quickbite.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.UUID;

@Service
public class SupabaseStorageAdapter implements StorageService {
    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageAdapter.class);

    private final String supabaseUrl;
    private final String serviceKey;
    private final String bucket;
    private final RestTemplate restTemplate;

    public SupabaseStorageAdapter(
            @Value("${quickbite.storage.supabase-url:}") String supabaseUrl,
            @Value("${quickbite.storage.service-key:}") String serviceKey,
            @Value("${quickbite.storage.bucket:QuickBite}") String bucket) {
        this.supabaseUrl = normalizeUrl(supabaseUrl);
        this.serviceKey = serviceKey != null ? serviceKey.trim() : "";
        this.bucket = (bucket != null && !bucket.isBlank()) ? bucket.trim() : "QuickBite";
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(20_000);
        this.restTemplate = new RestTemplate(requestFactory);
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
        requireConfigured();

        String safeFolder = sanitizeFolder(folder);
        String extension = getFileExtension(originalFilename);
        String uniqueName = UUID.randomUUID() + (extension.isEmpty() ? ".jpg" : "." + extension);
        String objectKey = safeFolder + "/" + uniqueName;

        String uploadUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucket, objectKey);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceKey);
        headers.set("apikey", serviceKey);
        headers.set("x-upsert", "true");
        headers.setContentType(MediaType.parseMediaType(contentType));
        HttpEntity<byte[]> requestEntity = new HttpEntity<>(data, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(uploadUrl),
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException(
                        "Supabase Storage upload failed with HTTP status " + response.getStatusCode().value());
            }

            log.info("Uploaded image to Supabase Storage: {}", objectKey);
            return objectKey;
        } catch (RestClientResponseException ex) {
            String details = ex.getResponseBodyAsString();
            String msg = (details != null && !details.isBlank()) ? details : ex.getStatusText();
            throw new IllegalStateException(
                    "Supabase Storage upload failed (" + ex.getStatusCode().value() + "): " + msg, ex);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Supabase Storage upload failed: " + ex.getMessage(), ex);
        }
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
        if (path == null || path.isBlank() || path.startsWith("data:")) {
            return;
        }
        String objectKey = extractObjectKey(path);
        if (objectKey == null) {
            return;
        }
        requireConfigured();

        String deleteUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucket, objectKey);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceKey);
        headers.set("apikey", serviceKey);

        try {
            restTemplate.exchange(URI.create(deleteUrl), HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (RestClientResponseException ex) {
            String details = ex.getResponseBodyAsString();
            String msg = (details != null && !details.isBlank()) ? details : ex.getStatusText();
            throw new IllegalStateException(
                    "Failed to delete object from Supabase Storage (" + ex.getStatusCode().value() + "): " + msg, ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to delete object from Supabase Storage: " + ex.getMessage(), ex);
        }
    }

    private void requireConfigured() {
        if (supabaseUrl.isBlank() || supabaseUrl.contains("example.supabase.co")
                || serviceKey.isBlank() || serviceKey.equals("placeholder")) {
            throw new IllegalStateException(
                    "Supabase Storage is not configured. Set SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY.");
        }
    }

    private String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "general";
        }
        String trimmed = folder.trim();
        if (!trimmed.matches("^[a-zA-Z0-9_-]+$")) {
            throw new BadRequestException("Invalid storage folder name");
        }
        return trimmed;
    }

    private String extractObjectKey(String path) {
        if (!path.startsWith("http://") && !path.startsWith("https://")) {
            return path.startsWith("/") ? path.substring(1) : path;
        }
        String publicPrefix = String.format("%s/storage/v1/object/public/%s/", supabaseUrl, bucket);
        if (path.startsWith(publicPrefix)) {
            return path.substring(publicPrefix.length());
        }
        return null;
    }

    private static String normalizeUrl(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return ext.matches("^[a-z0-9]{1,8}$") ? ext : "";
    }
}
