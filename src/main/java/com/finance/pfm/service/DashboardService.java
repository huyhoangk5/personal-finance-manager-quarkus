package com.finance.pfm.service;

import com.finance.pfm.entity.Category;
import com.finance.pfm.repository.CategoryRepository;
import com.finance.pfm.repository.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class DashboardService {

    @Inject
    TransactionRepository transactionRepository;

    @Inject
    CategoryRepository categoryRepository;

    public Map<String, Double> getSpendingStats(Long userId) {
        Map<String, Double> stats = new HashMap<>();

        categoryRepository.listAll().stream()
                .filter(cat -> cat.type == Category.TransactionType.CHI)
                .forEach(cat -> stats.put(cat.categoryName, 0.0));

        List<Object[]> results = transactionRepository.sumAmountByCategoryAndUser(userId);
        for (Object[] result : results) {
            stats.put((String) result[0], convertToDouble(result[1]));
        }

        return stats;
    }

    public Map<String, Double> getBalanceStats(Long userId) {
        Double totalIncomes = convertToDouble(transactionRepository.sumAmountByTypeAndUser(Category.TransactionType.THU, userId));
        Double totalExpenses = convertToDouble(transactionRepository.sumAmountByTypeAndUser(Category.TransactionType.CHI, userId));

        Map<String, Double> stats = new HashMap<>();
        stats.put("totalIncomes", totalIncomes);
        stats.put("totalExpenses", totalExpenses);
        stats.put("balance", totalIncomes - totalExpenses);

        return stats;
    }

    public Map<String, Double> getSpendingByCategoryAndMonth(Long userId, String month) {
        Map<String, Double> stats = new HashMap<>();
        List<Category> categories = categoryRepository.findByType(Category.TransactionType.CHI);
        for (Category cat : categories) {
            stats.put(cat.categoryName, 0.0);
        }
        for (Object[] result : results) {
            stats.put((String) result[0], convertToDouble(result[1]));
        }
        return stats;
    }

    public List<Map<String, Object>> getTopSpendingCategories(Long userId, int limit) {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<Object[]> results = transactionRepository.sumAmountByCategoryAndUserAndMonth(userId, currentMonth);
        List<Map<String, Object>> top = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("category", row[0]);
            item.put("amount", convertToDouble(row[1]));
            top.add(item);
        }
        top.sort((a, b) -> Double.compare((Double) b.get("amount"), (Double) a.get("amount")));
        return top.stream().limit(limit).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getTrend(Long userId, int months) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            String monthStr = monthDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            Double income = transactionRepository.sumAmountByTypeAndUserAndMonth(Category.TransactionType.THU, userId, monthStr);
            Double expense = transactionRepository.sumAmountByTypeAndUserAndMonth(Category.TransactionType.CHI, userId, monthStr);
            income = income != null ? income : 0.0;
            expense = expense != null ? expense : 0.0;
            Map<String, Object> point = new HashMap<>();
            point.put("month", monthStr);
            point.put("income", income);
            point.put("expense", expense);
            trend.add(point);
        }
        return trend;
    }

    public Map<String, Double> getBalanceStatsByMonth(Long userId, String month) {
        Double totalIncomes = convertToDouble(transactionRepository.sumAmountByTypeAndUserAndMonth(Category.TransactionType.THU, userId, month));
        Double totalExpenses = convertToDouble(transactionRepository.sumAmountByTypeAndUserAndMonth(Category.TransactionType.CHI, userId, month));
        Map<String, Double> stats = new HashMap<>();
        stats.put("totalIncomes", totalIncomes);
        stats.put("totalExpenses", totalExpenses);
        stats.put("balance", totalIncomes - totalExpenses);
        return stats;
    }

    public Map<String, Double> getIncomeByCategoryAndMonth(Long userId, String month) {
        Map<String, Double> stats = new HashMap<>();
        List<Category> categories = categoryRepository.findByType(Category.TransactionType.THU);
        for (Category cat : categories) {
            stats.put(cat.categoryName, 0.0);
        }
        List<Object[]> results = transactionRepository.sumIncomeByCategoryAndUserAndMonth(userId, month);
        for (Object[] result : results) {
            stats.put((String) result[0], convertToDouble(result[1]));
        }
        return stats;
    }

    public Map<String, Map<String, Double>> getDailySummary(Long userId, String month) {
        List<Object[]> incomes = transactionRepository.sumAmountByDayAndType(userId, Category.TransactionType.THU, month);
        List<Object[]> expenses = transactionRepository.sumAmountByDayAndType(userId, Category.TransactionType.CHI, month);

        Map<String, Map<String, Double>> dailyMap = new HashMap<>();
        YearMonth yearMonth = YearMonth.parse(month);
        int lastDay = yearMonth.lengthOfMonth();
        for (int day = 1; day <= lastDay; day++) {
            String dateStr = month + "-" + String.format("%02d", day);
            Map<String, Double> dayData = new HashMap<>();
            dayData.put("income", 0.0);
            dayData.put("expense", 0.0);
            dailyMap.put(dateStr, dayData);
        }
        for (Object[] row : incomes) {
            String dateStr = row[0].toString();
            Double amount = convertToDouble(row[1]);
            if (dailyMap.containsKey(dateStr)) {
                dailyMap.get(dateStr).put("income", amount);
            }
        }
        for (Object[] row : expenses) {
            String dateStr = row[0].toString();
            Double amount = convertToDouble(row[1]);
            if (dailyMap.containsKey(dateStr)) {
                dailyMap.get(dateStr).put("expense", amount);
            }
        }
        return dailyMap;
    }

    private Double convertToDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
}
