package com.finance.pfm.dto;

import com.finance.pfm.entity.Transaction;

public record TransactionResponse(Transaction transaction, String budgetMessage) {
}
