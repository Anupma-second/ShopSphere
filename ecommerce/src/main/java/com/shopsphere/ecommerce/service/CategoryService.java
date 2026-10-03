package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.entity.Category;
import com.shopsphere.ecommerce.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import com.shopsphere.ecommerce.exception.CategoryNotFoundException;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category updateCategory(Long id, Category updatedCategory) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));

        category.setName(updatedCategory.getName());
        category.setDescription(updatedCategory.getDescription());

        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
