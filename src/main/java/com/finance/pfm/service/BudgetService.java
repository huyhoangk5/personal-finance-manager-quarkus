package com.finance.pfm.service;

import com.finance.pfm.entity.Budget;
import com.finance.pfm.repository.BudgetRepository;
import com.finance.pfm.repository.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class BudgetService {

    @Inject
    BudgetRepository budgetRepository;

    @Inject
    TransactionRepository transactionRepository;

    public String checkBudgetExceeded(Long userId, Long categoryId, Double newAmount) {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        Double limit = budgetRepository.findFirstByUser_UserIdAndCategory_CategoryIdAndMonth(userId, categoryId, currentMonth)
                .map(b -> b.categoryLimit)
                .orElse(0.0);

        if (limit <= 0) {
            return "CHƯA THIẾT LẬP: Danh mục này chưa có ngân sách cho tháng " + currentMonth;
        }

        Double actualSpending = transactionRepository.sumCurrentMonthSpending(userId, categoryId);
        if (actualSpending == null) actualSpending = 0.0;

        double totalForecast = actualSpending + newAmount;
        double percentage = (totalForecast / limit) * 100;

        if (totalForecast > limit) {
            return "CẢNH BÁO ĐỎ: Vượt hạn mức tháng " + currentMonth + "! Dự kiến: " + String.format("%.0f", percentage) + "%";
        }

        if (percentage >= 80) {
            return "CẢNH BÁO VÀNG: Sắp hết ngân sách tháng " + currentMonth + " (" + String.format("%.0f", percentage) + "%)";
        }

        return "AN TOÀN: Ngân sách tháng " + currentMonth + " hiện tại là " + String.format("%.0f", percentage) + "%.";
    }

    @Transactional
    public String copyLastMonthBudget(Long userId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        LocalDate now = LocalDate.now();

        String currentMonth = now.format(formatter);
        String lastMonth = now.minusMonths(1).format(formatter);

        List<Budget> currentBudgets = budgetRepository.findByUser_UserIdAndMonth(userId, currentMonth);
        if (!currentBudgets.isEmpty()) {
            return "THẤT BẠI: Tháng " + currentMonth + " đã được thiết lập ngân sách trước đó.";
        }

        List<Budget> lastMonthBudgets = budgetRepository.findByUser_UserIdAndMonth(userId, lastMonth);
        if (lastMonthBudgets.isEmpty()) {
            return "THÔNG BÁO: Không tìm thấy ngân sách tháng " + lastMonth + " để sao chép.";
        }

        List<Budget> newBudgets = new ArrayList<>();
        for (Budget oldBudget : lastMonthBudgets) {
            Budget newBudget = new Budget();
            newBudget.user = oldBudget.user;
            newBudget.category = oldBudget.category;
            newBudget.categoryLimit = oldBudget.categoryLimit;
            newBudget.totalLimit = oldBudget.totalLimit;
            newBudget.month = currentMonth;
            newBudgets.add(newBudget);
        }

        budgetRepository.persist(newBudgets);
        return "THÀNH CÔNG: Đã sao chép " + newBudgets.size() + " danh mục ngân sách từ tháng " + lastMonth + " sang tháng " + currentMonth;
    }
}
