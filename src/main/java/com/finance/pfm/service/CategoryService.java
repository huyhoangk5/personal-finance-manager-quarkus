package com.finance.pfm.service;

import com.finance.pfm.entity.Category;
import com.finance.pfm.repository.BudgetRepository;
import com.finance.pfm.repository.CategoryRepository;
import com.finance.pfm.repository.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CategoryService {
    @Inject
    CategoryRepository categoryRepository;

    @Inject
    BudgetRepository budgetRepository;

    @Inject
    TransactionRepository transactionRepository;

    @Transactional
    public void deleteCategory(Long categoryId, Long userId) {
        Category category = categoryRepository.findById(categoryId);
        if (category == null || (category.user != null && !category.user.userId.equals(userId))) {
            throw new RuntimeException("Category not found or access denied");
        }
        transactionRepository.deleteByUserIdAndCategory(userId, category);
        budgetRepository.deleteByUserIdAndCategory(userId, category);
        categoryRepository.delete(category);
    }
}
