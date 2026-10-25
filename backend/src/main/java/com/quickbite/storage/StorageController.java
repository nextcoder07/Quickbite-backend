package com.quickbite.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/storage")
@Tag(name = "Storage", description = "File and image storage endpoints backed by Supabase Storage")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload image", description = "Uploads an image to Supabase Storage and returns the public asset URL")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder) throws IOException {

        String pathOrDataUrl = storageService.uploadImage(
                folder,
                file.getOriginalFilename(),
                file.getBytes(),
                file.getContentType()
        );

        String publicUrl = storageService.getPublicUrl(pathOrDataUrl);

        return ResponseEntity.ok(Map.of(
                "key", pathOrDataUrl,
                "url", publicUrl
        ));
    }
}
