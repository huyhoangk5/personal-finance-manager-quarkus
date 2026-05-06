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

    public List<Category> findByCategoryNameContainingIgnoreCase(String name) {
        return list("lower(categoryName) like ?1", "%" + name.toLowerCase() + "%");
    }
}
