package com.finance.pfm.resource;

import com.finance.pfm.dto.TransactionResponse;
import com.finance.pfm.entity.Transaction;
import com.finance.pfm.service.TransactionService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Path("/api/transactions")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Transactions", description = "Quản lý giao dịch thu chi")
public class TransactionResource {

    @Inject
    TransactionService transactionService;

    @GET
    @Operation(summary = "Lấy tất cả giao dịch của user đang đăng nhập")
    public List<Transaction> getAll(@Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        return transactionService.getAllTransactionsByUser(userId);
    }

    @POST
    @Operation(summary = "Tạo giao dịch mới")
    public Response createTransaction(Transaction transaction, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        // Gán userId từ token, không tin vào body
        if (transaction.user == null) {
            transaction.user = new com.finance.pfm.entity.User();
        }
        transaction.user.userId = userId;
        try {
            TransactionResponse response = transactionService.saveTransaction(transaction);
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/search")
    @Operation(summary = "Tìm kiếm giao dịch theo ghi chú")
    public Response search(@QueryParam("note") String note, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        try {
            List<Transaction> transactions = transactionService.searchByNote(note, userId);
            return Response.ok(transactions).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/recent")
    @Operation(summary = "Lấy 5 giao dịch gần nhất")
    public List<Transaction> getRecent(@Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        return transactionService.getRecentTransactions(userId);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Xóa giao dịch")
    public Response delete(@PathParam("id") Long id, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        boolean deleted = transactionService.deleteTransaction(id, userId);
        if (deleted) {
            return Response.ok("Xóa giao dịch thành công!").build();
        } else {
            return Response.status(Response.Status.FORBIDDEN).entity("Không tìm thấy giao dịch hoặc bạn không có quyền xóa!").build();
        }
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Cập nhật giao dịch")
    public Response updateTransaction(@PathParam("id") Long id, Transaction transaction, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        try {
            TransactionResponse response = transactionService.updateTransaction(id, transaction, userId);
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/spending-by-category-month")
    @Operation(summary = "Thống kê chi tiêu theo danh mục trong tháng")
    public Map<String, Double> getSpendingByCategoryAndMonth(@QueryParam("month") String month, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        return transactionService.getSpendingByCategoryAndMonth(userId, month);
    }
}
