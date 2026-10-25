package com.quickbite.storage;

public interface StorageService {
    String uploadImage(String folder, String originalFilename, byte[] data, String contentType);
    String getPublicUrl(String path);
    void deleteImage(String path);
}
