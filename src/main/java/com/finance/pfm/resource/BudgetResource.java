package com.finance.pfm.resource;

import com.finance.pfm.entity.Budget;
import com.finance.pfm.repository.BudgetRepository;
import com.finance.pfm.service.BudgetService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Path("/api/budgets")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BudgetResource {

    @Inject
    BudgetService budgetService;

    @Inject
    BudgetRepository budgetRepository;

    @GET
    public List<Budget> getBudgets(@QueryParam("userId") Long userId) {
        return budgetRepository.findByUser_UserId(userId);
    }

    @POST
    @Path("/copy-last-month")
    public Response copyBudget(@QueryParam("userId") Long userId) {
        String result = budgetService.copyLastMonthBudget(userId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/set-limit")
    public Response setBudgetLimit(Budget budget) {
        Optional<Budget> existing = budgetRepository.findFirstByUser_UserIdAndCategory_CategoryIdAndMonth(
                budget.user.userId,
                budget.category.categoryId,
                budget.month);

        if (existing.isPresent()) {
            Budget b = existing.get();
            b.categoryLimit = budget.categoryLimit;
            // No need for save() in Panache if inside transaction, but this isn't @Transactional yet.
            // Let's use service or wrap in transactional.
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
