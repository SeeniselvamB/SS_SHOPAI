package com.ss.shopai.controller.admin;

import com.ss.shopai.dto.request.*;
import com.ss.shopai.dto.response.ApiResponse;
import com.ss.shopai.dto.response.PagedResponse;
import com.ss.shopai.dto.response.ProductResponse;
import com.ss.shopai.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getAllProducts(
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<ProductResponse> response = productService.getAllProductsForAdmin(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Product created", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(@PathVariable Long id,
                                                                        @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deactivated", null));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(@PathVariable Long id,
                                                                      @Valid @RequestBody UpdateStockRequest request) {
        ProductResponse response = productService.updateStock(id, request);
        return ResponseEntity.ok(ApiResponse.success("Stock updated", response));
    }

    @PatchMapping("/{id}/price")
    public ResponseEntity<ApiResponse<ProductResponse>> updatePrice(@PathVariable Long id,
                                                                      @Valid @RequestBody UpdatePriceRequest request) {
        ProductResponse response = productService.updatePrice(id, request);
        return ResponseEntity.ok(ApiResponse.success("Price updated", response));
    }

    @PatchMapping("/{id}/discount")
    public ResponseEntity<ApiResponse<ProductResponse>> updateDiscount(@PathVariable Long id,
                                                                         @Valid @RequestBody UpdateDiscountRequest request) {
        ProductResponse response = productService.updateDiscount(id, request);
        return ResponseEntity.ok(ApiResponse.success("Discount updated", response));
    }
}