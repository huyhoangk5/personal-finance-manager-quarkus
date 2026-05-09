package com.finance.pfm.repository;

import com.finance.pfm.entity.Category;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class CategoryRepository implements PanacheRepository<Category> {
    public List<Category> findByType(Category.TransactionType type) {
        return list("type", type);
    }

    public List<Category> findByUser_UserId(Long userId) {
        return list("user.userId = ?1", userId);
    }

    public List<Category> findByTypeAndUser_UserId(Category.TransactionType type, Long userId) {
        return list("type = ?1 and user.userId = ?2", type, userId);
    }
    
    public List<Category> findByUser_UserIdAndType(Long userId, Category.TransactionType type) {
        return list("user.userId = ?1 and type = ?2", userId, type);
    }

    public List<Category> findByCategoryNameContainingIgnoreCaseAndUser_UserId(String name, Long userId) {
        return list("lower(categoryName) like ?1 and user.userId = ?2", "%" + name.toLowerCase() + "%", userId);
    }
}
