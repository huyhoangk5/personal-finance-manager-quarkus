package com.finance.pfm.service;

import com.finance.pfm.dto.TransactionResponse;
import com.finance.pfm.entity.Category;
import com.finance.pfm.entity.Transaction;
import com.finance.pfm.repository.CategoryRepository;
import com.finance.pfm.repository.TransactionRepository;
import com.finance.pfm.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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
    public TransactionResponse saveTransaction(Transaction transaction) {
        if (transaction.user != null && transaction.user.userId != null) {
            transaction.user = userRepository.findById(transaction.user.userId);
        }
        if (transaction.category != null && transaction.category.categoryId != null) {
            transaction.category = categoryRepository.findById(transaction.category.categoryId);
        }

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

    public List<Transaction> searchByNote(String keyword, Long userId) {
        return transactionRepository.findByNoteContainingIgnoreCaseAndUser_UserId(keyword, userId);
    }

    public List<Transaction> getRecentTransactions(Long userId) {
        return transactionRepository.findTop5ByUser_UserIdOrderByTransactionIdDesc(userId);
    }

    @Transactional
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

    @Transactional
    public Transaction updateTransaction(Transaction transaction) {
        return transactionRepository.getEntityManager().merge(transaction);
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
