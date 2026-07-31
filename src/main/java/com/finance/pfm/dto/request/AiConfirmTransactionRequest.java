package com.finance.pfm.dto.request;

import java.time.LocalDate;

/**
 * Request body cho API xác nhận lưu giao dịch từ AI (sau khi đã xem trước).
 */
public class AiConfirmTransactionRequest {
    public Double amount;
    public String type;
    public Long categoryId;
    /** Tên danh mục mới cần tạo (nếu categoryId == null và newCategoryRequired == true). */
    public String newCategoryName;
    public LocalDate date;
    public String note;
}
