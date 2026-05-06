package com.finance.pfm.resource;

import com.finance.pfm.service.DashboardService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Path("/api/dashboard")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DashboardResource {

    @Inject
    DashboardService dashboardService;

    @GET
    @Path("/spending-by-category")
    public Map<String, Double> getSpendingByCategory(@QueryParam("userId") Long userId) {
        return dashboardService.getSpendingStats(userId);
    }

    @GET
    @Path("/balance")
    public Map<String, Double> getBalance(@QueryParam("userId") Long userId) {
        return dashboardService.getBalanceStats(userId);
    }

    @GET
    @Path("/spending-by-category-month")
    public Map<String, Double> getSpendingByCategoryAndMonth(
            @QueryParam("userId") Long userId,
            @QueryParam("month") String month) {
        return dashboardService.getSpendingByCategoryAndMonth(userId, month);
    }

    @GET
    @Path("/top-spending-categories")
    public List<Map<String, Object>> getTopSpendingCategories(@QueryParam("userId") Long userId, @QueryParam("limit") int limit) {
        return dashboardService.getTopSpendingCategories(userId, limit);
    }

    @GET
    @Path("/trend")
    public List<Map<String, Object>> getTrend(@QueryParam("userId") Long userId, @QueryParam("months") int months) {
        return dashboardService.getTrend(userId, months);
    }

    @GET
    @Path("/balance-month")
    public Map<String, Double> getBalanceByMonth(@QueryParam("userId") Long userId, @QueryParam("month") String month) {
        return dashboardService.getBalanceStatsByMonth(userId, month);
    }

    @GET
    @Path("/income-by-category-month")
    public Map<String, Double> getIncomeByCategoryAndMonth(
            @QueryParam("userId") Long userId,
            @QueryParam("month") String month) {
        return dashboardService.getIncomeByCategoryAndMonth(userId, month);
    }

    @GET
    @Path("/daily-summary")
    public Map<String, Map<String, Double>> getDailySummary(@QueryParam("userId") Long userId, @QueryParam("month") String month) {
        return dashboardService.getDailySummary(userId, month);
    }
}
