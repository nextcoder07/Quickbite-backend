package com.quickbite.shop;

import com.quickbite.audit.AuditService;
import com.quickbite.common.enums.ShopStatus;
import com.quickbite.common.exception.BadRequestException;
import com.quickbite.common.exception.ConflictException;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.security.CurrentAccountPrincipal;
import com.quickbite.shop.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class MarketplaceService {

    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final ShopOwnerRepository shopOwnerRepository;
    private final AuditService auditService;

    public MarketplaceService(
            ShopRepository shopRepository,
            ProductRepository productRepository,
            ShopOwnerRepository shopOwnerRepository,
            AuditService auditService) {
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
        this.shopOwnerRepository = shopOwnerRepository;
        this.auditService = auditService;
    }

    // --- Public / Buyer Marketplace ---

    @Transactional(readOnly = true)
    public List<ShopResponse> getAvailableShops() {
        return shopRepository.findByApprovalStatusAndDeactivatedFalseAndOpenTrue(ShopStatus.APPROVED)
                .stream()
                .map(this::toShopResponseWithProducts)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShopResponse> getAllApprovedShops() {
        return shopRepository.findByApprovalStatus(ShopStatus.APPROVED)
                .stream()
                .filter(s -> !s.isDeactivated())
                .map(this::toShopResponseWithProducts)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShopResponse> searchShops(String query, String location, boolean includeClosed) {
        List<ShopResponse> base = includeClosed ? getAllApprovedShops() : getAvailableShops();
        String q = query != null ? query.trim().toLowerCase() : "";
        String loc = location != null ? location.trim().toLowerCase() : "";

        return base.stream()
                .filter(s -> {
                    boolean matchesQ = q.isEmpty()
                            || (s.name() != null && s.name().toLowerCase().contains(q))
                            || (s.description() != null && s.description().toLowerCase().contains(q))
                            || (s.productNames() != null && s.productNames().stream().anyMatch(p -> p.toLowerCase().contains(q)));
                    boolean matchesLoc = loc.isEmpty() || "all".equals(loc)
                            || (s.location() != null && s.location().toLowerCase().contains(loc));
                    return matchesQ && matchesLoc;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductSearchResponse> searchProducts(String query, String location) {
        String q = query != null ? query.trim().toLowerCase() : "";
        String loc = location != null ? location.trim().toLowerCase() : "";

        return shopRepository.findByApprovalStatus(ShopStatus.APPROVED).stream()
                .filter(s -> !s.isDeactivated())
                .filter(s -> loc.isEmpty() || "all".equals(loc) || s.getLocation().toLowerCase().contains(loc))
                .flatMap(shop -> productRepository.findByShopIdAndAvailableTrue(shop.getId()).stream())
                .filter(p -> q.isEmpty()
                        || p.getName().toLowerCase().contains(q)
                        || (p.getDescription() != null && p.getDescription().toLowerCase().contains(q))
                        || p.getShop().getName().toLowerCase().contains(q))
                .map(ProductSearchResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShopResponse getShopById(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));
        return toShopResponseWithProducts(shop);
    }

    private ShopResponse toShopResponseWithProducts(Shop shop) {
        List<String> productNames = productRepository.findByShopIdAndAvailableTrue(shop.getId())
                .stream()
                .map(Product::getName)
                .toList();
        return ShopResponse.from(shop, productNames);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAvailableProductsByShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));
        return productRepository.findByShopIdAndAvailableTrue(shop.getId())
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    // --- Shop Owner Operations ---

    @Transactional(readOnly = true)
    public ShopResponse getMyShop(CurrentAccountPrincipal principal) {
        ShopOwner owner = getShopOwner(principal);
        Shop shop = shopRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No registered shop found for this owner"));
        return ShopResponse.from(shop);
    }

    @Transactional
    public ShopResponse createShop(CurrentAccountPrincipal principal, CreateShopRequest request) {
        ShopOwner owner = getShopOwner(principal);
        if (!owner.isActive()) {
            throw new ForbiddenException("Your shop owner account is not active");
        }
        if (shopRepository.findByOwnerId(owner.getId()).isPresent()) {
            throw new ConflictException("Shop Owner may own at most one shop");
        }
        String resolvedLocation = request.resolvedLocation();
        if (resolvedLocation == null || resolvedLocation.isBlank()) {
            throw new BadRequestException("Shop location or address is required");
        }

        Shop shop = new Shop(
                owner,
                request.name(),
                request.description(),
                resolvedLocation,
                request.imagePath(),
                request.latitude(),
                request.longitude()
        );
        Shop saved = shopRepository.save(shop);

        auditService.record(owner.getIdentity(), "SHOP_APPLICATION_SUBMITTED", "SHOP", String.valueOf(saved.getId()),
                Map.of("shopName", saved.getName(), "location", saved.getLocation()));

        return ShopResponse.from(saved);
    }

    @Transactional
    public ShopResponse updateShop(CurrentAccountPrincipal principal, UpdateShopRequest request) {
        ShopOwner owner = getShopOwner(principal);
        if (!owner.isActive()) {
            throw new ForbiddenException("Your shop owner account is not active");
        }
        Shop shop = shopRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No registered shop found for this owner"));

        shop.updateDetails(
                request.name(),
                request.description(),
                request.resolvedLocation(),
                request.imagePath(),
                request.latitude(),
                request.longitude()
        );

        if (request.open() != null) {
            if (request.open() && shop.getApprovalStatus() != ShopStatus.APPROVED) {
                throw new BadRequestException("Shop must be APPROVED by admin before it can be opened");
            }
            if (request.open() && shop.isDeactivated()) {
                throw new BadRequestException("Deactivated shop cannot be opened");
            }
            shop.setOpen(request.open());
        }

        Shop updated = shopRepository.save(shop);
        return ShopResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getMyProducts(CurrentAccountPrincipal principal) {
        ShopOwner owner = getShopOwner(principal);
        Shop shop = getVerifiedOwnedShop(owner);
        return productRepository.findByShopId(shop.getId())
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(CurrentAccountPrincipal principal, CreateProductRequest request) {
        ShopOwner owner = getShopOwner(principal);
        Shop shop = getVerifiedOwnedShop(owner);

        Product product = new Product(shop, request.name(), request.description(),
                request.price(), request.stockQuantity(), request.imagePath());
        Product saved = productRepository.save(product);

        auditService.record(owner.getIdentity(), "PRODUCT_CREATED", "PRODUCT", String.valueOf(saved.getId()),
                Map.of("productName", saved.getName(), "price", saved.getPrice().toString()));

        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse updateProduct(CurrentAccountPrincipal principal, Long productId, UpdateProductRequest request) {
        ShopOwner owner = getShopOwner(principal);
        Shop shop = getVerifiedOwnedShop(owner);

        Product product = productRepository.findByIdAndShopId(productId, shop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found or does not belong to your shop"));

        product.updateDetails(request.name(), request.description(), request.price(),
                request.stockQuantity(), request.imagePath(), request.available());
        Product updated = productRepository.save(product);

        return ProductResponse.from(updated);
    }

    @Transactional
    public void deleteProduct(CurrentAccountPrincipal principal, Long productId) {
        ShopOwner owner = getShopOwner(principal);
        Shop shop = getVerifiedOwnedShop(owner);

        Product product = productRepository.findByIdAndShopId(productId, shop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        productRepository.delete(product);
        auditService.record(owner.getIdentity(), "PRODUCT_DELETED", "PRODUCT", String.valueOf(productId),
                Map.of("productName", product.getName()));
    }

    private Shop getVerifiedOwnedShop(ShopOwner owner) {
        if (!owner.isActive()) {
            throw new ForbiddenException("Your shop owner account is deactivated or suspended");
        }
        Shop shop = shopRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No registered shop found for this owner"));
        if (shop.getApprovalStatus() != ShopStatus.APPROVED || shop.isDeactivated()) {
            throw new ForbiddenException("Your shop must be verified and approved by an admin before accessing shop tools");
        }
        return shop;
    }

    private ShopOwner getShopOwner(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        return shopOwnerRepository.findByIdentityId(principal.getIdentityId())
                .orElseThrow(() -> new ForbiddenException("Only authorized Shop Owners can perform this action"));
    }
}
