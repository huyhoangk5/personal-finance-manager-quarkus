package com.finance.pfm.resource;

import com.finance.pfm.dto.TransactionResponse;
import com.finance.pfm.entity.Category;
import com.finance.pfm.entity.Transaction;
import com.finance.pfm.service.BudgetService;
import com.finance.pfm.service.TransactionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/api/transactions")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionResource {

    @Inject
    TransactionService transactionService;

    @Inject
    BudgetService budgetService;

    @GET
    public List<Transaction> getAll(@QueryParam("userId") Long userId) {
        return transactionService.getAllTransactionsByUser(userId);
    }

    @POST
    public Response createTransaction(Transaction transaction) {
        try {
            TransactionResponse response = transactionService.saveTransaction(transaction);
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/search")
    public Response search(@QueryParam("note") String note, @QueryParam("userId") Long userId) {
        try {
            List<Transaction> transactions = transactionService.searchByNote(note, userId);
            return Response.ok(transactions).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/recent")
    public List<Transaction> getRecent(@QueryParam("userId") Long userId) {
        return transactionService.getRecentTransactions(userId);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id, @QueryParam("userId") Long userId) {
        boolean deleted = transactionService.deleteTransaction(id, userId);
        if (deleted) {
            return Response.ok("Xóa giao dịch thành công!").build();
        } else {
            return Response.status(Response.Status.FORBIDDEN).entity("Không tìm thấy giao dịch hoặc bạn không có quyền xóa!").build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateTransaction(
            @PathParam("id") Long id,
            Transaction transaction,
            @QueryParam("userId") Long userId) {

        try {
            TransactionResponse response = transactionService.updateTransaction(id, transaction, userId);
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
        existing.date = transaction.date;
        existing.note = transaction.note;
        existing.type = transaction.type;
        if (transaction.category != null) {
            existing.category = transaction.category;
        }

        Transaction updated = transactionService.updateTransaction(existing);

        String message = "Cập nhật thành công";
        if (updated.type == Category.TransactionType.CHI) {
            message = budgetService.checkBudgetExceeded(
                    userId,
                    updated.category.categoryId,
                    updated.amount
            );
        }

        return Response.ok(new TransactionResponse(updated, message)).build();
    }

    @GET
    @Path("/spending-by-category-month")
    public Map<String, Double> getSpendingByCategoryAndMonth(@QueryParam("userId") Long userId, @QueryParam("month") String month) {
        return transactionService.getSpendingByCategoryAndMonth(userId, month);
    }
}
