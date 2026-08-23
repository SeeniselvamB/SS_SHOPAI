package com.ss.shopai.service;

import com.ss.shopai.dto.request.CategoryRequest;
import com.ss.shopai.dto.response.CategoryResponse;
import com.ss.shopai.entity.Category;
import com.ss.shopai.exception.BadRequestException;
import com.ss.shopai.exception.DuplicateResourceException;
import com.ss.shopai.exception.ResourceNotFoundException;
import com.ss.shopai.repository.CategoryRepository;
import com.ss.shopai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("A category with this name already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .active(request.getActive() == null || request.getActive())
                .build();

        Category saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategoryById(id);

        if (!category.getName().equalsIgnoreCase(request.getName())
                && categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("A category with this name already exists");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        if (request.getImageUrl() != null) {
            category.setImageUrl(request.getImageUrl());
        }
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        Category saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategoryById(id);
        long productCount = productRepository.findByCategoryIdAndActiveTrue(id).size();
        if (productCount > 0) {
            throw new BadRequestException(
                    "Cannot delete category with active products. Deactivate it instead or reassign products first.");
        }
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return mapToResponse(findCategoryById(id));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(boolean activeOnly) {
        List<Category> categories = activeOnly
                ? categoryRepository.findByActiveTrue()
                : categoryRepository.findAll();
        return categories.stream().map(this::mapToResponse).toList();
    }

    private Category findCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private CategoryResponse mapToResponse(Category category) {
        long productCount = productRepository.findByCategoryIdAndActiveTrue(category.getId()).size();
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .active(category.isActive())
                .productCount(productCount)
                .build();
    }
}