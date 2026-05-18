package com.finance.pfm.resource;

import com.finance.pfm.entity.Budget;
import com.finance.pfm.repository.BudgetRepository;
import com.finance.pfm.service.BudgetService;
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
import java.util.Optional;

@Path("/api/budgets")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Budgets", description = "Quản lý ngân sách hàng tháng")
public class BudgetResource {

    @Inject
    BudgetService budgetService;

    @Inject
    BudgetRepository budgetRepository;

    @GET
    @Operation(summary = "Lấy danh sách ngân sách của user đang đăng nhập")
    public List<Budget> getBudgets(@Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        return budgetRepository.findByUser_UserId(userId);
    }

    @POST
    @Path("/copy-last-month")
    @Operation(summary = "Sao chép ngân sách từ tháng trước")
    public Response copyBudget(@Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        String result = budgetService.copyLastMonthBudget(userId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/set-limit")
    @Operation(summary = "Thiết lập hạn mức ngân sách cho danh mục")
    public Response setBudgetLimit(Budget budget, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        // Gán userId từ token
        if (budget.user == null) {
            budget.user = new com.finance.pfm.entity.User();
        }
        budget.user.userId = userId;

        Optional<Budget> existing = budgetRepository.findFirstByUser_UserIdAndCategory_CategoryIdAndMonth(
                userId,
                budget.category.categoryId,
                budget.month);

        if (existing.isPresent()) {
            Budget b = existing.get();
            b.categoryLimit = budget.categoryLimit;
            return Response.ok(updateAndSave(b)).build();
        }
        return Response.ok(updateAndSave(budget)).build();
    }

    @jakarta.transaction.Transactional
    protected Budget updateAndSave(Budget budget) {
        if (budget.budgetId == null) {
            budgetRepository.persist(budget);
            return budget;
        } else {
            return budgetRepository.getEntityManager().merge(budget);
        }
    }
}
