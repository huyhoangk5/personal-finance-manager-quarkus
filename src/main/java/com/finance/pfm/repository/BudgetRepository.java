package com.finance.pfm.repository;

import com.finance.pfm.entity.Budget;
import com.finance.pfm.entity.Category;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class BudgetRepository implements PanacheRepository<Budget> {
    public Optional<Budget> findFirstByUser_UserIdAndCategory_CategoryId(Long userId, Long categoryId) {
        return find("user.userId = ?1 and category.categoryId = ?2", userId, categoryId).firstResultOptional();
    }

    public Optional<Budget> findFirstByUser_UserIdAndCategory_CategoryIdAndMonth(Long userId, Long categoryId, String month) {
        return find("user.userId = ?1 and category.categoryId = ?2 and month = ?3", userId, categoryId, month).firstResultOptional();
    }

    public List<Budget> findByUser_UserIdAndMonth(Long userId, String month) {
        return list("user.userId = ?1 and month = ?2", userId, month);
    }

    public List<Budget> findByUser_UserId(Long userId) {
        return list("user.userId = ?1", userId);
    }

    public void deleteByUserIdAndCategory(Long userId, Category category) {
        delete("user.userId = ?1 and category = ?2", userId, category);
    }
}
