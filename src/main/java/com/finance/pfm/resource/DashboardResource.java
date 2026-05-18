package com.finance.pfm.resource;

import com.finance.pfm.service.DashboardService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Path("/api/dashboard")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboard", description = "Thống kê và phân tích tài chính")
public class DashboardResource {

    @Inject
    DashboardService dashboardService;

    @GET
    @Path("/spending-by-category")
    @Operation(summary = "Thống kê chi tiêu theo danh mục (toàn thời gian)")
    public Map<String, Double> getSpendingByCategory(@Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        return dashboardService.getSpendingStats(userId);
    }

    @GET
    @Path("/balance")
    @Operation(summary = "Số dư tổng (toàn thời gian)")
    public Map<String, Double> getBalance(@Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        return dashboardService.getBalanceStats(userId);
    }

    @GET
    @Path("/spending-by-category-month")
    @Operation(summary = "Thống kê chi tiêu theo danh mục trong tháng")
    public Map<String, Double> getSpendingByCategoryAndMonth(@QueryParam("month") String month, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        return dashboardService.getSpendingByCategoryAndMonth(userId, month);
    }

    @GET
    @Path("/top-spending-categories")
    @Operation(summary = "Top danh mục chi tiêu nhiều nhất")
    public List<Map<String, Object>> getTopSpendingCategories(@QueryParam("limit") int limit, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        return dashboardService.getTopSpendingCategories(userId, limit);
    }

    @GET
    @Path("/trend")
    @Operation(summary = "Xu hướng thu chi theo tháng")
    public List<Map<String, Object>> getTrend(@QueryParam("months") int months, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        return dashboardService.getTrend(userId, months);
    }

    @GET
    @Path("/balance-month")
    @Operation(summary = "Số dư trong tháng cụ thể")
    public Map<String, Double> getBalanceByMonth(@QueryParam("month") String month, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        return dashboardService.getBalanceStatsByMonth(userId, month);
    }

    @GET
    @Path("/income-by-category-month")
    @Operation(summary = "Thống kê thu nhập theo danh mục trong tháng")
    public Map<String, Double> getIncomeByCategoryAndMonth(@QueryParam("month") String month, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        return dashboardService.getIncomeByCategoryAndMonth(userId, month);
    }

    @GET
    @Path("/daily-summary")
    @Operation(summary = "Tổng hợp thu chi theo ngày trong tháng")
    public Map<String, Map<String, Double>> getDailySummary(@QueryParam("month") String month, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        return dashboardService.getDailySummary(userId, month);
    }
}
