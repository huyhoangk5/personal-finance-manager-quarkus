package com.finance.pfm.dto;

import com.finance.pfm.entity.Budget;

public class BudgetDTO {
    public Long budgetId;
    public String month;
    public Double totalLimit;
    public Double categoryLimit;
    public Long userId;
    public CategoryDTO category;

    public static BudgetDTO from(Budget budget) {
        BudgetDTO dto = new BudgetDTO();
        dto.budgetId = budget.budgetId;
        dto.month = budget.month;
        dto.totalLimit = budget.totalLimit;
        dto.categoryLimit = budget.categoryLimit;
        dto.userId = budget.user != null ? budget.user.userId : null;
        dto.category = budget.category != null ? CategoryDTO.from(budget.category) : null;
        return dto;
    }
}
