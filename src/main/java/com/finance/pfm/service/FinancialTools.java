package com.finance.pfm.service;

import com.finance.pfm.entity.Budget;
import com.finance.pfm.entity.Category;
import com.finance.pfm.entity.Transaction;
import com.finance.pfm.entity.User;
import com.finance.pfm.repository.BudgetRepository;
import com.finance.pfm.repository.CategoryRepository;
import com.finance.pfm.repository.TransactionRepository;
import com.finance.pfm.repository.UserRepository;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Lớp công cụ (Tools) cung cấp cho AI khả năng tự chủ động truy vấn và ghi dữ liệu tài chính.
 * Mỗi instance được khởi tạo với userId cụ thể tại thời điểm request để đảm bảo an toàn phân quyền.
 *
 * AI sẽ đọc mô tả @Tool và tự quyết định gọi hàm nào dựa trên câu hỏi của người dùng.
 */
public class FinancialTools {

    private static final double DEFAULT_CHI_BUDGET_LIMIT = 1_800_000.0;

    private final DashboardService dashboardService;
    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final Long userId;

    public FinancialTools(DashboardService dashboardService,
                          CategoryService categoryService,
                          CategoryRepository categoryRepository,
                          TransactionRepository transactionRepository,
                          BudgetRepository budgetRepository,
                          UserRepository userRepository,
                          Long userId) {
        this.dashboardService = dashboardService;
        this.categoryService = categoryService;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
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

    @Tool("Lấy danh sách tất cả danh mục hiện có của người dùng (cả thu và chi). Dùng trước khi lưu giao dịch để kiểm tra danh mục đã tồn tại chưa.")
    public String getUserCategories() {
        List<Category> categories = categoryRepository.findByUser_UserId(userId);
        if (categories.isEmpty()) {
            return "Người dùng chưa có danh mục nào.";
        }
        String list = categories.stream()
                .map(c -> String.format("  - id=%d | \"%s\" | loại=%s", c.categoryId, c.categoryName, c.type))
                .collect(Collectors.joining("\n"));
        return "Danh mục hiện có:\n" + list;
    }

    @Tool("""
            Lưu một giao dịch tài chính mới vào hệ thống.
            - amount: số tiền dương (ví dụ: 8000000 cho 8 triệu, 50000 cho 50k)
            - type: "THU" (thu nhập) hoặc "CHI" (chi tiêu)
            - categoryName: tên danh mục tiếng Việt (ví dụ: "Mua sắm", "Ăn uống", "Lương")
            - date: ngày giao dịch định dạng yyyy-MM-dd (ví dụ: "2026-08-04"), hoặc null để dùng ngày hôm nay
            - note: ghi chú mô tả giao dịch, hoặc null nếu không có

            Nếu danh mục chưa tồn tại → tự động tạo mới và thông báo cho người dùng.
            Với danh mục CHI mới: tạo hạn mức ngân sách mặc định 1.800.000đ/tháng (người dùng có thể chỉnh sau).
            Dùng hàm này khi người dùng muốn thêm/ghi lại/lưu một giao dịch qua chat.
            """)
    public String saveTransaction(
            @P("Số tiền giao dịch, luôn dương") double amount,
            @P("Loại giao dịch: THU hoặc CHI") String type,
            @P("Tên danh mục tiếng Việt") String categoryName,
            @P("Ngày giao dịch yyyy-MM-dd, hoặc null") String date,
            @P("Ghi chú mô tả, hoặc null") String note) {
        try {
            User user = userRepository.findById(userId);
            if (user == null) return "Lỗi: Không tìm thấy người dùng.";

            Category.TransactionType txType;
            try {
                txType = Category.TransactionType.valueOf(type.toUpperCase().trim());
            } catch (Exception e) {
                return "Lỗi: Loại giao dịch không hợp lệ. Chỉ chấp nhận 'THU' hoặc 'CHI'.";
            }

            // Tìm danh mục khớp tên (case-insensitive) và loại
            final Category.TransactionType finalTxType = txType;
            Optional<Category> existingCat = categoryRepository.findByUser_UserId(userId).stream()
                    .filter(c -> c.categoryName.trim().equalsIgnoreCase(categoryName.trim())
                            && c.type.equals(finalTxType))
                    .findFirst();

            Category category;
            boolean newCategoryCreated = false;

            if (existingCat.isPresent()) {
                category = existingCat.get();
            } else {
                // Tạo danh mục mới qua CategoryService (đảm bảo validation + cache invalidation)
                category = new Category();
                category.categoryName = categoryName.trim();
                category.type = txType;
                category.user = user;
                String createResult = categoryService.createCategory(category);
                if (createResult.startsWith("Lỗi")) {
                    return "Không thể tạo danh mục: " + createResult;
                }
                newCategoryCreated = true;

                // Tạo budget mặc định 1.800.000đ cho danh mục CHI mới
                if (txType == Category.TransactionType.CHI) {
                    String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                    Budget defaultBudget = new Budget();
                    defaultBudget.user = user;
                    defaultBudget.category = category;
                    defaultBudget.categoryLimit = DEFAULT_CHI_BUDGET_LIMIT;
                    defaultBudget.totalLimit = DEFAULT_CHI_BUDGET_LIMIT;
                    defaultBudget.month = currentMonth;
                    budgetRepository.persist(defaultBudget);
                }
            }

            // Xác nhận type khớp danh mục
            if (!category.type.equals(txType)) {
                return String.format("Lỗi: Danh mục \"%s\" là loại %s, không khớp với loại giao dịch %s.",
                        categoryName, category.type, type);
            }

            // Tạo và lưu giao dịch
            Transaction tx = new Transaction();
            tx.amount = amount;
            tx.type = txType;
            tx.user = user;
            tx.category = category;
            tx.note = (note == null || note.isBlank() || "null".equalsIgnoreCase(note)) ? null : note.trim();
            try {
                tx.date = (date == null || date.isBlank() || "null".equalsIgnoreCase(date))
                        ? LocalDate.now()
                        : LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                tx.date = LocalDate.now();
            }
            transactionRepository.persist(tx);

            // Xây dựng thông báo kết quả
            String sign = txType == Category.TransactionType.CHI ? "-" : "+";
            String newCatMsg = newCategoryCreated
                    ? String.format(" (đã tạo danh mục mới \"%s\"%s)",
                        categoryName,
                        txType == Category.TransactionType.CHI ? " với hạn mức mặc định 1.800.000đ/tháng" : "")
                    : "";
            return String.format("✅ Đã lưu giao dịch: %s%,.0f VNĐ | Danh mục: %s | Ngày: %s%s",
                    sign, amount, category.categoryName, tx.date, newCatMsg);

        } catch (Exception e) {
            return "Lỗi hệ thống khi lưu giao dịch: " + e.getMessage();
        }
    }
}
