package com.finance.pfm.service;

import dev.langchain4j.agent.tool.Tool;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lớp công cụ (Tools) cung cấp cho AI khả năng tự chủ động truy vấn dữ liệu tài chính.
 * Mỗi instance được khởi tạo với userId cụ thể tại thời điểm request để đảm bảo an toàn phân quyền.
 *
 * AI sẽ đọc mô tả @Tool và tự quyết định gọi hàm nào dựa trên câu hỏi của người dùng.
 */
public class FinancialTools {

    private final DashboardService dashboardService;
    private final Long userId;

    public FinancialTools(DashboardService dashboardService, Long userId) {
        this.dashboardService = dashboardService;
        this.userId = userId;
    }

    @Tool("Lấy tổng thu nhập, tổng chi tiêu, và số dư còn lại của toàn bộ tất cả các tháng (all-time). Không cần tham số.")
    public String getTotalBalance() {
        Map<String, Double> stats = dashboardService.getBalanceStats(userId);
        return String.format("Tổng thu: %,.0f VNĐ, Tổng chi: %,.0f VNĐ, Số dư: %,.0f VNĐ",
                stats.getOrDefault("totalIncomes", 0.0),
                stats.getOrDefault("totalExpenses", 0.0),
                stats.getOrDefault("balance", 0.0));
    }

    @Tool("Lấy tổng thu nhập, tổng chi tiêu, và số dư trong 1 tháng cụ thể. Tham số month có định dạng yyyy-MM, ví dụ: 2026-07")
    public String getBalanceByMonth(String month) {
        Map<String, Double> stats = dashboardService.getBalanceStatsByMonth(userId, month);
        return String.format("Tháng %s — Thu: %,.0f VNĐ, Chi: %,.0f VNĐ, Dư: %,.0f VNĐ",
                month,
                stats.getOrDefault("totalIncomes", 0.0),
                stats.getOrDefault("totalExpenses", 0.0),
                stats.getOrDefault("balance", 0.0));
    }

    @Tool("Lấy chi tiết chi tiêu theo từng danh mục trong 1 tháng. Tham số month có định dạng yyyy-MM, ví dụ: 2026-07. Dùng hàm này khi người dùng hỏi về chi tiêu theo danh mục hoặc so sánh danh mục giữa các tháng.")
    public String getSpendingByCategory(String month) {
        Map<String, Double> cats = dashboardService.getSpendingByCategoryAndMonth(userId, month);
        if (cats.isEmpty()) {
            return "Không có dữ liệu chi tiêu trong tháng " + month;
        }
        String details = cats.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(e -> String.format("  - %s: %,.0f VNĐ", e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n"));
        if (details.isEmpty()) {
            return "Tháng " + month + ": không có khoản chi tiêu nào.";
        }
        return "Chi tiêu theo danh mục tháng " + month + ":\n" + details;
    }

    @Tool("Lấy chi tiết thu nhập theo từng danh mục trong 1 tháng. Tham số month có định dạng yyyy-MM, ví dụ: 2026-07. Dùng hàm này khi người dùng hỏi về thu nhập theo danh mục.")
    public String getIncomeByCategory(String month) {
        Map<String, Double> cats = dashboardService.getIncomeByCategoryAndMonth(userId, month);
        if (cats.isEmpty()) {
            return "Không có dữ liệu thu nhập trong tháng " + month;
        }
        String details = cats.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(e -> String.format("  - %s: %,.0f VNĐ", e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n"));
        if (details.isEmpty()) {
            return "Tháng " + month + ": không có khoản thu nhập nào.";
        }
        return "Thu nhập theo danh mục tháng " + month + ":\n" + details;
    }

    @Tool("Lấy xu hướng thu chi theo từng tháng trong N tháng gần nhất. Tham số months là số tháng cần xem, ví dụ: 3 hoặc 6.")
    public String getTrend(int months) {
        var trend = dashboardService.getTrend(userId, months);
        StringBuilder sb = new StringBuilder("Xu hướng " + months + " tháng gần nhất:\n");
        for (var point : trend) {
            sb.append(String.format("  %s — Thu: %,.0f, Chi: %,.0f VNĐ\n",
                    point.get("month"),
                    ((Number) point.get("income")).doubleValue(),
                    ((Number) point.get("expense")).doubleValue()));
        }
        return sb.toString();
    }

    @Tool("Lấy ngày hiện tại và tháng hiện tại. Dùng khi cần xác định 'tháng này', 'tháng trước' để truyền tham số cho các hàm khác.")
    public String getCurrentDate() {
        LocalDate now = LocalDate.now();
        return String.format("Hôm nay: %s. Tháng hiện tại: %s. Tháng trước: %s.",
                now.format(DateTimeFormatter.ISO_LOCAL_DATE),
                now.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                now.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM")));
    }

    @Tool("Lấy danh sách các giao dịch cụ thể trong N ngày gần nhất. Tham số days là số ngày muốn xem, ví dụ: 3 (3 ngày), 7 (1 tuần), 30 (1 tháng). Dùng hàm này khi người dùng hỏi về 'giao dịch gần đây', 'giao dịch hôm nay', 'giao dịch tuần này', hoặc muốn xem chi tiết từng giao dịch.")
    public String getRecentTransactions(int days) {
        var txList = dashboardService.getRecentTransactions(userId, days);
        if (txList.isEmpty()) {
            return String.format("Không có giao dịch nào trong %d ngày gần đây.", days);
        }
        StringBuilder sb = new StringBuilder(String.format("Danh sách %d giao dịch trong %d ngày gần đây:\n", txList.size(), days));
        for (var tx : txList) {
            String type = "CHI".equals(tx.get("type")) ? "Chi" : "Thu";
            sb.append(String.format("  - [%s] %s | %s | %,.0f VNĐ | %s\n",
                    tx.get("date"), type, tx.get("category"),
                    ((Number) tx.get("amount")).doubleValue(),
                    tx.get("note").toString().isBlank() ? "(không có ghi chú)" : tx.get("note")));
        }
        return sb.toString();
    }
}
