package com.finance.pfm.dto;

import com.finance.pfm.entity.Transaction;
import java.time.LocalDate;

public class TransactionDTO {
    public Long transactionId;
    public Double amount;
    public LocalDate date;
    public String note;
    public String type;
    public Long userId;
    public CategoryDTO category;

    public static TransactionDTO from(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.transactionId = transaction.transactionId;
        dto.amount = transaction.amount;
        dto.date = transaction.date;
        dto.note = transaction.note;
        dto.type = transaction.type != null ? transaction.type.name() : null;
        dto.userId = transaction.user != null ? transaction.user.userId : null;
        dto.category = transaction.category != null ? CategoryDTO.from(transaction.category) : null;
        return dto;
    }
}
