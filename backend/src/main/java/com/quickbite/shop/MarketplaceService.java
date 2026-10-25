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
                .map(ShopResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShopResponse> getAllApprovedShops() {
        return shopRepository.findByApprovalStatus(ShopStatus.APPROVED)
                .stream()
                .filter(s -> !s.isDeactivated())
                .map(ShopResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShopResponse getShopById(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));
        return ShopResponse.from(shop);
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
        if (shopRepository.findByOwnerId(owner.getId()).isPresent()) {
            throw new ConflictException("Shop Owner may own at most one shop");
        }

        Shop shop = new Shop(owner, request.name(), request.description(), request.campusLocation(), request.imagePath());
        Shop saved = shopRepository.save(shop);

        auditService.record(owner.getIdentity(), "SHOP_APPLICATION_SUBMITTED", "SHOP", String.valueOf(saved.getId()),
                Map.of("shopName", saved.getName()));

        return ShopResponse.from(saved);
    }

    @Transactional
    public ShopResponse updateShop(CurrentAccountPrincipal principal, UpdateShopRequest request) {
        ShopOwner owner = getShopOwner(principal);
        Shop shop = shopRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No registered shop found for this owner"));

        shop.updateDetails(request.name(), request.description(), request.campusLocation(), request.imagePath());

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
        Shop shop = shopRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        return productRepository.findByShopId(shop.getId())
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(CurrentAccountPrincipal principal, CreateProductRequest request) {
        ShopOwner owner = getShopOwner(principal);
        Shop shop = shopRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

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
        Shop shop = shopRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

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
        Shop shop = shopRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        Product product = productRepository.findByIdAndShopId(productId, shop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        productRepository.delete(product);
        auditService.record(owner.getIdentity(), "PRODUCT_DELETED", "PRODUCT", String.valueOf(productId),
                Map.of("productName", product.getName()));
    }

    private ShopOwner getShopOwner(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        return shopOwnerRepository.findByIdentityId(principal.getIdentityId())
                .orElseThrow(() -> new ForbiddenException("Only authorized Shop Owners can perform this action"));
    }
}
