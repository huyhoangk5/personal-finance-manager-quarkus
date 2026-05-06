package com.finance.pfm.repository;

import com.finance.pfm.entity.Category;
import com.finance.pfm.entity.Transaction;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class TransactionRepository implements PanacheRepository<Transaction> {
    public List<Transaction> findByUser_UserId(Long userId) {
        return list("user.userId", userId);
    }

    public List<Transaction> findByNoteContainingIgnoreCaseAndUser_UserId(String keyword, Long userId) {
        return list("lower(note) like ?1 and user.userId = ?2", "%" + keyword.toLowerCase() + "%", userId);
    }

    public List<Transaction> findTop5ByUser_UserIdOrderByTransactionIdDesc(Long userId) {
        return find("user.userId = ?1 order by transactionId desc", userId).page(0, 5).list();
    }

    public List<Object[]> sumAmountByCategoryAndUser(Long userId) {
        return find("SELECT t.category.categoryName, SUM(t.amount) " +
                    "FROM Transaction t " +
                    "WHERE t.user.userId = ?1 AND t.type = 'CHI' " +
                    "GROUP BY t.category.categoryName", userId).project(Object[].class).list();
    }

    public Object sumAmountByTypeAndUser(Category.TransactionType type, Long userId) {
        return find("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = ?1 AND t.user.userId = ?2", type, userId).project(Object.class).firstResult();
    }

    public Object sumCurrentMonthSpending(Long userId, Long categoryId) {
        return find("SELECT SUM(t.amount) FROM Transaction t " +
                    "WHERE t.user.userId = ?1 " +
                    "AND t.category.categoryId = ?2 " +
                    "AND t.type = 'CHI' " +
                    "AND EXTRACT(MONTH FROM t.date) = EXTRACT(MONTH FROM CURRENT_DATE) " +
                    "AND EXTRACT(YEAR FROM t.date) = EXTRACT(YEAR FROM CURRENT_DATE)", userId, categoryId).project(Object.class).firstResult();
    }

    public List<Object[]> sumAmountByCategoryAndUserAndMonth(Long userId, String month) {
        return find("SELECT c.categoryName, SUM(t.amount) FROM Transaction t JOIN t.category c " +
                    "WHERE t.user.userId = ?1 AND t.type = 'CHI' " +
                    "AND TO_CHAR(t.date, 'YYYY-MM') = ?2 " +
                    "GROUP BY c.categoryName", userId, month).project(Object[].class).list();
    }

    public void deleteByCategory(Category category) {
        delete("category", category);
    }

    public Object sumAmountByTypeAndUserAndMonth(Category.TransactionType type, Long userId, String month) {
        return find("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = ?1 AND t.user.userId = ?2 AND TO_CHAR(t.date, 'YYYY-MM') = ?3", type, userId, month).project(Object.class).firstResult();
    }

    public List<Object[]> sumIncomeByCategoryAndUserAndMonth(Long userId, String month) {
        return find("SELECT c.categoryName, SUM(t.amount) FROM Transaction t JOIN t.category c " +
                    "WHERE t.user.userId = ?1 AND t.type = 'THU' " +
                    "AND TO_CHAR(t.date, 'YYYY-MM') = ?2 " +
                    "GROUP BY c.categoryName", userId, month).project(Object[].class).list();
    }

    public List<Object[]> sumAmountByDayAndType(Long userId, Category.TransactionType type, String month) {
        return find("SELECT t.date, SUM(t.amount) FROM Transaction t " +
                    "WHERE t.user.userId = ?1 AND t.type = ?2 " +
                    "AND TO_CHAR(t.date, 'YYYY-MM') = ?3 " +
                    "GROUP BY t.date", userId, type, month).project(Object[].class).list();
    }
}
