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
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId);
        if (category == null) {
            throw new RuntimeException("Category not found");
        }
        transactionRepository.deleteByCategory(category);
        budgetRepository.deleteByCategory(category);
        categoryRepository.delete(category);
    }
}
