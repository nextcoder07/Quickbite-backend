package com.quickbite.storage;

import com.quickbite.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTest {

    private SupabaseStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SupabaseStorageAdapter(
                "https://wwkepshdsnfqhsleifop.supabase.co",
                "sample-service-key",
                "QuickBite"
        );
    }

    @Test
    @DisplayName("Reject empty or null image uploads")
    void shouldRejectEmptyImageUpload() {
        assertThrows(BadRequestException.class, () ->
                adapter.uploadImage("products", "test.png", new byte[0], "image/png"));

        assertThrows(BadRequestException.class, () ->
                adapter.uploadImage("products", "test.png", null, "image/png"));
    }

    @Test
    @DisplayName("Reject non-image content type uploads")
    void shouldRejectInvalidContentType() {
        byte[] dummyData = new byte[]{1, 2, 3, 4};
        assertThrows(BadRequestException.class, () ->
                adapter.uploadImage("products", "malicious.exe", dummyData, "application/octet-stream"));
    }

    @Test
    @DisplayName("Format public URL correctly for Supabase QuickBite bucket")
    void shouldFormatPublicUrlForQuickBiteBucket() {
        String path = "products/item-123.jpg";
        String publicUrl = adapter.getPublicUrl(path);

        assertEquals("https://wwkepshdsnfqhsleifop.supabase.co/storage/v1/object/public/QuickBite/products/item-123.jpg", publicUrl);
    }

    @Test
    @DisplayName("Pass through existing HTTP/HTTPS or Data URLs without modification")
    void shouldPassThroughDirectUrls() {
        String webUrl = "https://images.unsplash.com/photo-123";
        assertEquals(webUrl, adapter.getPublicUrl(webUrl));

        String dataUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA";
        assertEquals(dataUrl, adapter.getPublicUrl(dataUrl));
    }
}
