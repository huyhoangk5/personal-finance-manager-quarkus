package com.finance.pfm.service;

import com.finance.pfm.entity.Category;
import com.finance.pfm.repository.BudgetRepository;
import com.finance.pfm.repository.CategoryRepository;
import com.finance.pfm.repository.TransactionRepository;
import com.finance.pfm.repository.UserRepository;
import com.finance.pfm.util.ValidationUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CategoryService {
    @Inject
    CategoryRepository categoryRepository;

    @Inject
    BudgetRepository budgetRepository;

    @Inject
    TransactionRepository transactionRepository;
    
    @Inject
    UserRepository userRepository;

    @Transactional
    public String createCategory(Category category) {
        // Validate category name
        ValidationUtil.ValidationResult nameValidation = ValidationUtil.validateCategoryName(category.categoryName);
        if (!nameValidation.isValid()) {
            return "Lỗi: " + nameValidation.getFirstError();
        }
        
        // Validate transaction type
        ValidationUtil.ValidationResult typeValidation = ValidationUtil.validateTransactionType(category.type.toString());
        if (!typeValidation.isValid()) {
            return "Lỗi: " + typeValidation.getFirstError();
        }
        
        // Normalize category name for duplicate checking
        String normalizedName = ValidationUtil.normalizeCategoryName(category.categoryName);
        
        // Check for duplicate category name (case-insensitive) for the same user and type
        List<Category> existingCategories = categoryRepository.findByUser_UserIdAndType(category.user.userId, category.type);
        boolean isDuplicate = existingCategories.stream()
            .anyMatch(c -> ValidationUtil.normalizeCategoryName(c.categoryName).equals(normalizedName));
        
        if (isDuplicate) {
            return "Lỗi: Tên danh mục đã tồn tại cho loại này";
        }
        
        // Normalize and set data
        category.categoryName = ValidationUtil.normalizeString(category.categoryName);
        
        // Ensure user exists
        if (category.user != null && category.user.userId != null) {
            category.user = userRepository.findById(category.user.userId);
            if (category.user == null) {
                return "Lỗi: Người dùng không tồn tại";
            }
        }
        
        categoryRepository.persist(category);
        return "Tạo danh mục thành công";
    }
    
    @Transactional
    public String updateCategory(Long categoryId, Category updatedCategory, Long userId) {
        Category existing = categoryRepository.findById(categoryId);
        if (existing == null) {
            return "Lỗi: Không tìm thấy danh mục";
        }
        
        if (!existing.user.userId.equals(userId)) {
            return "Lỗi: Bạn không có quyền sửa danh mục này";
        }
        
        // Validate category name
        ValidationUtil.ValidationResult nameValidation = ValidationUtil.validateCategoryName(updatedCategory.categoryName);
        if (!nameValidation.isValid()) {
            return "Lỗi: " + nameValidation.getFirstError();
        }
        
        // Validate transaction type
        ValidationUtil.ValidationResult typeValidation = ValidationUtil.validateTransactionType(updatedCategory.type.toString());
        if (!typeValidation.isValid()) {
            return "Lỗi: " + typeValidation.getFirstError();
        }
        
        // Normalize category name for duplicate checking
        String normalizedName = ValidationUtil.normalizeCategoryName(updatedCategory.categoryName);
        
        // Check for duplicate category name (case-insensitive) for the same user and type, excluding current category
        List<Category> existingCategories = categoryRepository.findByUser_UserIdAndType(userId, updatedCategory.type);
        boolean isDuplicate = existingCategories.stream()
            .anyMatch(c -> !c.categoryId.equals(categoryId) && 
                          ValidationUtil.normalizeCategoryName(c.categoryName).equals(normalizedName));
        
        if (isDuplicate) {
            return "Lỗi: Tên danh mục đã tồn tại cho loại này";
        }
        
        // Update fields
        existing.categoryName = ValidationUtil.normalizeString(updatedCategory.categoryName);
        existing.type = updatedCategory.type;
        
        return "Cập nhật danh mục thành công";
    }
    
    public List<Category> getCategoriesByUserAndType(Long userId, Category.TransactionType type) {
        return categoryRepository.findByUser_UserIdAndType(userId, type);
    }
    
    public List<Category> getCategoriesByUser(Long userId) {
        return categoryRepository.findByUser_UserId(userId);
    }
    
    public Optional<Category> getCategoryById(Long categoryId) {
        return Optional.ofNullable(categoryRepository.findById(categoryId));
    }

    @Transactional
    public String deleteCategory(Long categoryId, Long userId) {
        Category category = categoryRepository.findById(categoryId);
        if (category == null) {
            return "Lỗi: Không tìm thấy danh mục";
        }
        
        if (category.user != null && !category.user.userId.equals(userId)) {
            return "Lỗi: Bạn không có quyền xóa danh mục này";
        }
        
        // Check if category is being used in transactions
        long transactionCount = transactionRepository.countByCategory_CategoryIdAndUser_UserId(categoryId, userId);
        if (transactionCount > 0) {
            return "Lỗi: Không thể xóa danh mục đang được sử dụng trong giao dịch";
        }
        
        // Delete related budgets first
        budgetRepository.deleteByUserIdAndCategory(userId, category);
        
        // Delete category
        categoryRepository.delete(category);
        
        return "Xóa danh mục thành công";
    }
}
