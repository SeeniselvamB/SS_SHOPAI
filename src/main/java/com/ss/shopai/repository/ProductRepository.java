package com.ss.shopai.repository;

import com.ss.shopai.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);

    Page<Product> findByActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND (" +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.category.id = :categoryId " +
            "AND p.id != :excludeId ORDER BY p.averageRating DESC")
    List<Product> findRecommendedByCategory(@Param("categoryId") Long categoryId,
                                             @Param("excludeId") Long excludeId,
                                             org.springframework.data.domain.Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.totalSold DESC")
    List<Product> findTopSelling(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.averageRating DESC")
    List<Product> findTopRated(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stockQuantity <= :threshold")
    List<Product> findLowStock(@Param("threshold") Integer threshold);

    long countByActiveTrue();

    @Query("SELECT COALESCE(SUM(p.stockQuantity), 0) FROM Product p WHERE p.active = true")
    long sumStockQuantity();
}