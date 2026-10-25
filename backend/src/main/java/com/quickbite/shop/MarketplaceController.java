package com.quickbite.shop;

import com.quickbite.security.CurrentAccountPrincipal;
import com.quickbite.shop.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/marketplace")
@Tag(name = "Marketplace", description = "Shop and Product browsing, shop owner catalog management")
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    public MarketplaceController(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    // --- Public / Buyer Endpoints ---

    @GetMapping("/shops")
    @Operation(summary = "Browse campus shops", description = "Retrieves active approved shops for buyers")
    public ResponseEntity<List<ShopResponse>> getShops(@RequestParam(name = "includeClosed", defaultValue = "false") boolean includeClosed) {
        List<ShopResponse> shops = includeClosed
                ? marketplaceService.getAllApprovedShops()
                : marketplaceService.getAvailableShops();
        return ResponseEntity.ok(shops);
    }

    @GetMapping("/shops/{shopId}")
    @Operation(summary = "Get shop details", description = "Retrieves details of a specific shop")
    public ResponseEntity<ShopResponse> getShopById(@PathVariable Long shopId) {
        return ResponseEntity.ok(marketplaceService.getShopById(shopId));
    }

    @GetMapping("/shops/{shopId}/products")
    @Operation(summary = "Get available products for a shop", description = "Returns available menu items for buyers")
    public ResponseEntity<List<ProductResponse>> getProductsByShop(@PathVariable Long shopId) {
        return ResponseEntity.ok(marketplaceService.getAvailableProductsByShop(shopId));
    }

    // --- Shop Owner Endpoints ---

    @GetMapping("/owner/shop")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Get owned shop", description = "Retrieves shop owned by the authenticated shop owner")
    public ResponseEntity<ShopResponse> getMyShop(@AuthenticationPrincipal CurrentAccountPrincipal principal) {
        return ResponseEntity.ok(marketplaceService.getMyShop(principal));
    }

    @PostMapping("/owner/shop")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Register shop", description = "Submits a new shop registration for admin approval")
    public ResponseEntity<ShopResponse> createShop(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @Valid @RequestBody CreateShopRequest request) {
        return new ResponseEntity<>(marketplaceService.createShop(principal, request), HttpStatus.CREATED);
    }

    @PutMapping("/owner/shop")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Update shop", description = "Updates shop details or opens/closes the shop")
    public ResponseEntity<ShopResponse> updateShop(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @Valid @RequestBody UpdateShopRequest request) {
        return ResponseEntity.ok(marketplaceService.updateShop(principal, request));
    }

    @GetMapping("/owner/products")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "List shop owner products", description = "Returns all products belonging to the shop owner's shop")
    public ResponseEntity<List<ProductResponse>> getMyProducts(@AuthenticationPrincipal CurrentAccountPrincipal principal) {
        return ResponseEntity.ok(marketplaceService.getMyProducts(principal));
    }

    @PostMapping("/owner/products")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Create product", description = "Adds a new product to the shop")
    public ResponseEntity<ProductResponse> createProduct(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @Valid @RequestBody CreateProductRequest request) {
        return new ResponseEntity<>(marketplaceService.createProduct(principal, request), HttpStatus.CREATED);
    }

    @PutMapping("/owner/products/{productId}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Update product", description = "Updates details, price, stock, or availability of a product")
    public ResponseEntity<ProductResponse> updateProduct(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(marketplaceService.updateProduct(principal, productId, request));
    }

    @DeleteMapping("/owner/products/{productId}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Delete product", description = "Deletes a product from the shop")
    public ResponseEntity<Map<String, String>> deleteProduct(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @PathVariable Long productId) {
        marketplaceService.deleteProduct(principal, productId);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }
}
