package com.ss.shopai.service;

import com.ss.shopai.dto.request.*;
import com.ss.shopai.dto.response.PagedResponse;
import com.ss.shopai.dto.response.ProductResponse;
import com.ss.shopai.entity.Category;
import com.ss.shopai.entity.Product;
import com.ss.shopai.entity.ProductImage;
import com.ss.shopai.exception.BadRequestException;
import com.ss.shopai.exception.DuplicateResourceException;
import com.ss.shopai.exception.ResourceNotFoundException;
import com.ss.shopai.repository.CategoryRepository;
import com.ss.shopai.repository.ProductImageRepository;
import com.ss.shopai.repository.ProductRepository;
import com.ss.shopai.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("A product with this SKU already exists");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .description(request.getDescription())
                .brand(request.getBrand())
                .price(request.getPrice())
                .discountPercentage(request.getDiscountPercentage() == null ? BigDecimal.ZERO : request.getDiscountPercentage())
                .stockQuantity(request.getStockQuantity())
                .thumbnailUrl(request.getThumbnailUrl())
                .category(category)
                .active(request.getActive() == null || request.getActive())
                .averageRating(0.0)
                .totalReviews(0)
                .totalSold(0)
                .build();

        Product saved = productRepository.save(product);

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            saveProductImages(saved, request.getImageUrls());
        }

        return mapToResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProductById(id);

        if (!product.getSku().equalsIgnoreCase(request.getSku())
                && productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("A product with this SKU already exists");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setPrice(request.getPrice());
        if (request.getDiscountPercentage() != null) {
            product.setDiscountPercentage(request.getDiscountPercentage());
        }
        product.setStockQuantity(request.getStockQuantity());
        if (request.getThumbnailUrl() != null) {
            product.setThumbnailUrl(request.getThumbnailUrl());
        }
        product.setCategory(category);
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        Product saved = productRepository.save(product);

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            productImageRepository.deleteByProductId(saved.getId());
            saveProductImages(saved, request.getImageUrls());
        }

        return mapToResponse(saved);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProductById(id);
        // Soft delete to preserve order history integrity
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional
    public ProductResponse updateStock(Long id, UpdateStockRequest request) {
        Product product = findProductById(id);
        product.setStockQuantity(request.getStockQuantity());
        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse adjustStock(Long id, int delta) {
        Product product = findProductById(id);
        int newQuantity = product.getStockQuantity() + delta;
        if (newQuantity < 0) {
            throw new BadRequestException("Insufficient stock for product: " + product.getName());
        }
        product.setStockQuantity(newQuantity);
        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updatePrice(Long id, UpdatePriceRequest request) {
        Product product = findProductById(id);
        product.setPrice(request.getPrice());
        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateDiscount(Long id, UpdateDiscountRequest request) {
        Product product = findProductById(id);
        product.setDiscountPercentage(request.getDiscountPercentage());
        return mapToResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return mapToResponse(findProductById(id));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> searchProducts(ProductSearchRequest request) {
        Sort sort = buildSort(request.getSortBy(), request.getSortDirection());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<Product> page = productRepository.findAll(ProductSpecification.build(request), pageable);
        Page<ProductResponse> mapped = page.map(this::mapToResponse);
        return PagedResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getAllProductsForAdmin(Pageable pageable) {
        Page<Product> page = productRepository.findAll(pageable);
        return PagedResponse.from(page.map(this::mapToResponse));
    }

    Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private void saveProductImages(Product product, List<String> imageUrls) {
        List<ProductImage> images = new ArrayList<>();
        int order = 0;
        for (String url : imageUrls) {
            images.add(ProductImage.builder()
                    .product(product)
                    .imageUrl(url)
                    .displayOrder(order++)
                    .build());
        }
        productImageRepository.saveAll(images);
    }

    private Sort buildSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String property = switch (sortBy == null ? "newest" : sortBy) {
            case "price" -> "price";
            case "rating" -> "averageRating";
            case "popularity" -> "totalSold";
            case "name" -> "name";
            default -> "createdAt";
        };
        return Sort.by(direction, property);
    }

    public ProductResponse mapToResponse(Product product) {
        List<String> imageUrls = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId())
                .stream().map(ProductImage::getImageUrl).collect(Collectors.toList());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .description(product.getDescription())
                .brand(product.getBrand())
                .price(product.getPrice())
                .discountPercentage(product.getDiscountPercentage())
                .discountedPrice(product.getDiscountedPrice())
                .stockQuantity(product.getStockQuantity())
                .inStock(product.getStockQuantity() != null && product.getStockQuantity() > 0)
                .thumbnailUrl(product.getThumbnailUrl())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .active(product.isActive())
                .averageRating(product.getAverageRating())
                .totalReviews(product.getTotalReviews())
                .totalSold(product.getTotalSold())
                .imageUrls(imageUrls)
                .createdAt(product.getCreatedAt())
                .build();
    }
}