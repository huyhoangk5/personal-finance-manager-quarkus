package com.finance.pfm.dto;

/**
 * DTO trả về sau khi tạo/cập nhật giao dịch.
 * Dùng TransactionDTO thay vì Entity trực tiếp.
 */
public record TransactionResponse(TransactionDTO transaction, String budgetMessage) {
}
