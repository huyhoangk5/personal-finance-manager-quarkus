package com.finance.pfm.dto;

import com.finance.pfm.entity.Category;

public class CategoryDTO {
    public Long categoryId;
    public String categoryName;
    public String type;
    public Long userId;

    public static CategoryDTO from(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.categoryId = category.categoryId;
        dto.categoryName = category.categoryName;
        dto.type = category.type != null ? category.type.name() : null;
        dto.userId = category.user != null ? category.user.userId : null;
        return dto;
    }
}
