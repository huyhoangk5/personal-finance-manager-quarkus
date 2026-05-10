package com.finance.pfm.service;

import com.finance.pfm.dto.TransactionResponse;
import com.finance.pfm.entity.Category;
import com.finance.pfm.entity.Transaction;
import com.finance.pfm.repository.CategoryRepository;
import com.finance.pfm.repository.TransactionRepository;
import com.finance.pfm.repository.UserRepository;
import com.finance.pfm.util.ValidationUtil;
import io.quarkus.cache.CacheInvalidateAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class TransactionService {

    @Inject
    TransactionRepository transactionRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    BudgetService budgetService;

    public List<Transaction> getAllTransactionsByUser(Long userId) {
        return transactionRepository.findByUser_UserId(userId);
    }

    @Transactional
    @CacheInvalidateAll(cacheName = "dashboard-balance")
    @CacheInvalidateAll(cacheName = "spending-by-category")
    public TransactionResponse saveTransaction(Transaction transaction) {
        // Validate transaction data
        String validationError = validateTransaction(transaction);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }
        
        if (transaction.user != null && transaction.user.userId != null) {
            transaction.user = userRepository.findById(transaction.user.userId);
        }
        if (transaction.category != null && transaction.category.categoryId != null) {
            transaction.category = categoryRepository.findById(transaction.category.categoryId);
        }

        // Validate category belongs to user and type matches
        if (transaction.category != null && transaction.user != null) {
            if (!transaction.category.user.userId.equals(transaction.user.userId)) {
                throw new IllegalArgumentException("Danh mục không thuộc về người dùng này");
            }
            if (!transaction.category.type.equals(transaction.type)) {
                throw new IllegalArgumentException("Loại giao dịch không khớp với loại danh mục");
            }
        }

        // Normalize note
        transaction.note = ValidationUtil.normalizeString(transaction.note);

        String message = "Giao dịch thu nhập hoặc chưa đặt hạn mức.";
        if (transaction.type == Category.TransactionType.CHI && transaction.user != null && transaction.category != null) {
            message = budgetService.checkBudgetExceeded(
                    transaction.user.userId,
                    transaction.category.categoryId,
                    transaction.amount
            );
        }

        transactionRepository.persist(transaction);
        return new TransactionResponse(transaction, message);
    }
    
    @Transactional
    @CacheInvalidateAll(cacheName = "dashboard-balance")
    @CacheInvalidateAll(cacheName = "spending-by-category")
    public TransactionResponse updateTransaction(Long transactionId, Transaction updatedTransaction, Long userId) {
        // Validate transaction data
        String validationError = validateTransaction(updatedTransaction);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }
        
        Transaction existing = transactionRepository.findById(transactionId);
        if (existing == null) {
            throw new IllegalArgumentException("Không tìm thấy giao dịch");
        }
        
        if (!existing.user.userId.equals(userId)) {
            throw new IllegalArgumentException("Bạn không có quyền sửa giao dịch này");
        }
        
        // Validate category belongs to user and type matches
        if (updatedTransaction.category != null && updatedTransaction.category.categoryId != null) {
            Category category = categoryRepository.findById(updatedTransaction.category.categoryId);
            if (category == null) {
                throw new IllegalArgumentException("Danh mục không tồn tại");
            }
            if (!category.user.userId.equals(userId)) {
                throw new IllegalArgumentException("Danh mục không thuộc về người dùng này");
            }
            if (!category.type.equals(updatedTransaction.type)) {
                throw new IllegalArgumentException("Loại giao dịch không khớp với loại danh mục");
            }
            existing.category = category;
        }
        
        // Update fields
        existing.amount = updatedTransaction.amount;
        existing.date = updatedTransaction.date;
        existing.note = ValidationUtil.normalizeString(updatedTransaction.note);
        existing.type = updatedTransaction.type;
        
        String message = "Giao dịch thu nhập hoặc chưa đặt hạn mức.";
        if (existing.type == Category.TransactionType.CHI) {
            message = budgetService.checkBudgetExceeded(
                    existing.user.userId,
                    existing.category.categoryId,
                    existing.amount
            );
        }
        
        return new TransactionResponse(existing, message);
    }
    
    private String validateTransaction(Transaction transaction) {
        // Validate transaction type
        if (transaction.type == null) {
            return "Loại giao dịch không được để trống";
        }
        
        ValidationUtil.ValidationResult typeValidation = ValidationUtil.validateTransactionType(transaction.type.toString());
        if (!typeValidation.isValid()) {
            return typeValidation.getFirstError();
        }
        
        // Validate amount
        ValidationUtil.ValidationResult amountValidation = ValidationUtil.validateAmount(transaction.amount);
        if (!amountValidation.isValid()) {
            return amountValidation.getFirstError();
        }
        
        // Validate date
        ValidationUtil.ValidationResult dateValidation = ValidationUtil.validateTransactionDate(transaction.date);
        if (!dateValidation.isValid()) {
            return dateValidation.getFirstError();
        }
        
        // Validate note
        ValidationUtil.ValidationResult noteValidation = ValidationUtil.validateNote(transaction.note);
        if (!noteValidation.isValid()) {
            return noteValidation.getFirstError();
        }
        
        // Validate category is provided
        if (transaction.category == null || transaction.category.categoryId == null) {
            return "Danh mục không được để trống";
        }
        
        // Validate user is provided
        if (transaction.user == null || transaction.user.userId == null) {
            return "Người dùng không được để trống";
        }
        
        return null; // No validation errors
    }

    public List<Transaction> searchByNote(String keyword, Long userId) {
        // Validate search keyword
        ValidationUtil.ValidationResult keywordValidation = ValidationUtil.validateSearchKeyword(keyword);
        if (!keywordValidation.isValid()) {
            throw new IllegalArgumentException(keywordValidation.getFirstError());
        }
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllTransactionsByUser(userId);
        }
        
        return transactionRepository.findByNoteContainingIgnoreCaseAndUser_UserId(keyword.trim(), userId);
    }
    
    public List<Transaction> filterTransactions(Long userId, String type, LocalDate fromDate, LocalDate toDate, String noteKeyword) {
        // Validate transaction type filter
        if (type != null && !type.trim().isEmpty() && !"all".equals(type)) {
            ValidationUtil.ValidationResult typeValidation = ValidationUtil.validateTransactionType(type);
            if (!typeValidation.isValid()) {
                throw new IllegalArgumentException(typeValidation.getFirstError());
            }
        }
        
        // Validate date range
        ValidationUtil.ValidationResult dateRangeValidation = ValidationUtil.validateDateRange(fromDate, toDate);
        if (!dateRangeValidation.isValid()) {
            throw new IllegalArgumentException(dateRangeValidation.getFirstError());
        }
        
        // Validate search keyword
        if (noteKeyword != null && !noteKeyword.trim().isEmpty()) {
            ValidationUtil.ValidationResult keywordValidation = ValidationUtil.validateSearchKeyword(noteKeyword);
            if (!keywordValidation.isValid()) {
                throw new IllegalArgumentException(keywordValidation.getFirstError());
            }
        }
        
        // Apply filters (this would need to be implemented in repository)
        // For now, return basic filtering
        List<Transaction> transactions = getAllTransactionsByUser(userId);
        
        return transactions.stream()
            .filter(t -> type == null || "all".equals(type) || t.type.toString().equals(type))
            .filter(t -> fromDate == null || !t.date.isBefore(fromDate))
            .filter(t -> toDate == null || !t.date.isAfter(toDate))
            .filter(t -> noteKeyword == null || noteKeyword.trim().isEmpty() || 
                    (t.note != null && t.note.toLowerCase().contains(noteKeyword.trim().toLowerCase())))
            .toList();
    }

    public List<Transaction> getRecentTransactions(Long userId) {
        return transactionRepository.findTop5ByUser_UserIdOrderByTransactionIdDesc(userId);
    }

    @Transactional
    @CacheInvalidateAll(cacheName = "dashboard-balance")
    @CacheInvalidateAll(cacheName = "spending-by-category")
    public boolean deleteTransaction(Long id, Long userId) {
        Transaction t = transactionRepository.findById(id);
        if (t != null && t.user.userId.equals(userId)) {
            transactionRepository.delete(t);
            return true;
        }
        return false;
    }

    public Optional<Transaction> getTransactionById(Long id) {
        return Optional.ofNullable(transactionRepository.findById(id));
    }

    public Map<String, Double> getSpendingByCategoryAndMonth(Long userId, String month) {
        List<Object[]> results = transactionRepository.sumAmountByCategoryAndUserAndMonth(userId, month);
        Map<String, Double> map = new HashMap<>();
        for (Object[] row : results) {
            map.put((String) row[0], (Double) row[1]);
        }
        return map;
    }
}
