package com.finance.pfm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.pfm.dto.AiTransactionParseResult;
import com.finance.pfm.dto.request.AiConfirmTransactionRequest;
import com.finance.pfm.dto.request.AiParseRequest;
import com.finance.pfm.entity.*;
import com.finance.pfm.repository.*;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.quarkus.cache.CacheInvalidateAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service xử lý tính năng Nhập liệu thông minh (Smart Data Entry).
 * Sử dụng Groq Llama 3.3 để phân tích câu lệnh ngôn ngữ tự nhiên.
 */
@ApplicationScoped
public class AiTransactionService {

    private static final Logger LOG = Logger.getLogger(AiTransactionService.class);

    @Inject
    ChatLanguageModel chatModel;

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    TransactionRepository transactionRepository;

    @Inject
    BudgetService budgetService;

    @Inject
    CategoryService categoryService;

    @Inject
    BudgetRepository budgetRepository;

    /** Hạn mức mặc định cho danh mục CHI do AI tạo tự động: 1.800.000đ */
    private static final double DEFAULT_CHI_BUDGET_LIMIT = 1_800_000.0;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // ─── System Prompt cho Nhập liệu thông minh ───────────────────────────────
    private static final String PARSE_SYSTEM_PROMPT = """
            Bạn là trợ lý AI tài chính của ứng dụng WalletZen. Nhiệm vụ DUY NHẤT của bạn là phân tích câu lệnh ngôn ngữ tự nhiên của người dùng và bóc tách thành dữ liệu giao dịch tài chính theo định dạng JSON.
            
            QUY TẮC BẮT BUỘC:
            1. Chỉ trả về DUY NHẤT một đối tượng JSON, không có bất kỳ văn bản nào khác.
            2. Cấu trúc JSON bắt buộc:
            {
              "amount": <số thực dương, null nếu không xác định>,
              "type": <"THU" hoặc "CHI", null nếu không xác định>,
              "categoryName": <tên danh mục tiếng Việt, null nếu không xác định>,
              "date": <"YYYY-MM-DD", null nếu không đề cập — KHÔNG giả định ngày>,
              "note": <ghi chú ngắn mô tả giao dịch, có thể null>,
              "isComplete": <true nếu đủ amount, type, categoryName; false nếu thiếu>,
              "message": <null nếu isComplete=true; hoặc chuỗi thân thiện hỏi thêm thông tin nếu thiếu>
            }
            3. Quy tắc bóc tách:
               - "k" hoặc "nghìn" = nhân 1000. VD: "50k" → 50000, "1.5 triệu" → 1500000
               - "hôm nay" → dùng ngày hiện tại. "hôm qua" → hôm nay - 1 ngày. "tháng này" → đầu tháng.
               - Nếu không đề cập ngày → date = null (không được tự đặt ngày)
               - Phân loại type: mua/chi/trả → "CHI"; nhận/lương/thu/bán → "THU"
               - categoryName phải là tên tiếng Việt, ngắn gọn (tối đa 30 ký tự)
            4. Nếu có context từ lần phân tích trước (trong message người dùng), hãy cập nhật context đó thay vì tạo mới.
            5. TUYỆT ĐỐI không thêm markdown, code block, hay giải thích. Chỉ JSON thuần.
            """;

    /**
     * Phân tích câu lệnh ngôn ngữ tự nhiên thành thông tin giao dịch có cấu trúc.
     */
    public AiTransactionParseResult parseTransaction(AiParseRequest request, Long userId) {
        try {
            // Lấy danh sách danh mục của người dùng để cung cấp ngữ cảnh cho AI
            List<Category> userCategories = categoryRepository.findByUser_UserId(userId);
            String categoryContext = buildCategoryContext(userCategories);

            // Xây dựng nội dung prompt người dùng
            String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, 'ngày' yyyy-MM-dd", new Locale("vi", "VN")));
            String userContent = "Hôm nay là " + todayStr + "\n";
            userContent += "Danh sách danh mục hiện có của người dùng: " + categoryContext + "\n";
            if (request.previousContext != null && !request.previousContext.isBlank()) {
                userContent += "Context giao dịch từ lần trước (hãy cập nhật theo yêu cầu mới): " + request.previousContext + "\n";
            }
            userContent += "Câu lệnh của người dùng: \"" + request.userMessage + "\"";

            // Gọi AI
            String rawJson = chatModel.chat(List.of(
                    SystemMessage.from(PARSE_SYSTEM_PROMPT),
                    UserMessage.from(userContent)
            )).aiMessage().text();

            // Làm sạch JSON (xóa code-block nếu model lỡ thêm)
            rawJson = cleanJson(rawJson);

            // Parse JSON thành DTO trung gian
            AiRawParseDto rawDto = objectMapper.readValue(rawJson, AiRawParseDto.class);

            // Map sang kết quả cuối cùng + kiểm tra danh mục
            return mapToResult(rawDto, userCategories);

        } catch (Exception e) {
            LOG.error("Lỗi khi gọi AI parse transaction: " + e.getMessage(), e);
            AiTransactionParseResult error = new AiTransactionParseResult();
            error.isComplete = false;
            error.message = "Trợ lý ảo hiện không thể kết nối máy chủ. Bạn vui lòng thử lại sau!";
            return error;
        }
    }

    /**
     * Lưu giao dịch sau khi người dùng xác nhận.
     * Nếu danh mục chưa tồn tại (newCategoryName), tự động tạo trước khi lưu.
     */
    @Transactional
    @CacheInvalidateAll(cacheName = "dashboard-balance")
    @CacheInvalidateAll(cacheName = "spending-by-category")
    public String confirmAndSaveTransaction(AiConfirmTransactionRequest req, Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            return "Người dùng không hợp lệ.";
        }

        Category category;

        if (req.categoryId != null) {
            // Danh mục đã tồn tại
            category = categoryRepository.findById(req.categoryId);
            if (category == null || !category.user.userId.equals(userId)) {
                return "Danh mục không tồn tại hoặc không thuộc về bạn.";
            }
        } else if (req.newCategoryName != null && !req.newCategoryName.isBlank()) {
            // Kiểm tra xem danh mục có tên này đã tồn tại chưa (AI có thể đặt tên trùng)
            Category.TransactionType newCatType = Category.TransactionType.valueOf(req.type.toUpperCase());
            String normalizedReqName = req.newCategoryName.trim().toLowerCase();
            java.util.Optional<Category> existingByName = categoryRepository
                    .findByUser_UserId(userId).stream()
                    .filter(c -> c.categoryName.trim().equalsIgnoreCase(normalizedReqName)
                            && c.type.equals(newCatType))
                    .findFirst();

            if (existingByName.isPresent()) {
                // Danh mục đã tồn tại (tên trùng khớp) → dùng lại, không tạo mới
                category = existingByName.get();
                LOG.infof("AI tìm thấy danh mục trùng tên '%s', dùng lại id=%d (userId=%d)",
                        category.categoryName, category.categoryId, userId);
            } else {
                // Tạo danh mục mới qua CategoryService để đảm bảo validation + cache invalidation
                category = new Category();
                category.categoryName = req.newCategoryName.trim();
                category.type = newCatType;
                category.user = user;
                String createResult = categoryService.createCategory(category);
                if (createResult.startsWith("Lỗi")) {
                    return "Không thể tạo danh mục: " + createResult;
                }
                LOG.infof("AI tự động tạo danh mục mới: '%s' loại=%s (userId=%d)",
                        category.categoryName, category.type, userId);

                // Với danh mục CHI: tạo budget mặc định 1.800.000đ cho tháng hiện tại
                if (newCatType == Category.TransactionType.CHI) {
                    String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                    Budget defaultBudget = new Budget();
                    defaultBudget.user = user;
                    defaultBudget.category = category;
                    defaultBudget.categoryLimit = DEFAULT_CHI_BUDGET_LIMIT;
                    defaultBudget.totalLimit = DEFAULT_CHI_BUDGET_LIMIT;
                    defaultBudget.month = currentMonth;
                    budgetRepository.persist(defaultBudget);
                    LOG.infof("Tạo budget mặc định %.0fđ cho danh mục '%s' tháng %s (userId=%d)",
                            DEFAULT_CHI_BUDGET_LIMIT, category.categoryName, currentMonth, userId);
                }
            }
        } else {
            return "Vui lòng chọn hoặc xác nhận danh mục cho giao dịch.";
        }

        // Kiểm tra type khớp với danh mục
        Category.TransactionType requestedType = Category.TransactionType.valueOf(req.type.toUpperCase());
        if (!category.type.equals(requestedType)) {
            return "Loại giao dịch (" + req.type + ") không khớp với loại danh mục (" + category.type + ").";
        }

        // Tạo và lưu giao dịch
        Transaction tx = new Transaction();
        tx.amount = req.amount;
        tx.type = requestedType;
        tx.date = req.date != null ? req.date : LocalDate.now();
        tx.note = req.note;
        tx.user = user;
        tx.category = category;
        transactionRepository.persist(tx);

        // Kiểm tra ngân sách nếu là giao dịch CHI
        String budgetMessage = "Giao dịch đã được lưu thành công!";
        if (requestedType == Category.TransactionType.CHI) {
            String budgetCheck = budgetService.checkBudgetExceeded(userId, category.categoryId, req.amount);
            if (budgetCheck != null && budgetCheck.contains("vượt")) {
                budgetMessage = "Giao dịch đã lưu! ⚠️ " + budgetCheck;
            }
        }

        return budgetMessage;
    }

    // ─── Helper Methods ────────────────────────────────────────────────────────

    private String buildCategoryContext(List<Category> categories) {
        if (categories.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (Category c : categories) {
            sb.append("{id:").append(c.categoryId)
              .append(",name:\"").append(c.categoryName)
              .append("\",type:\"").append(c.type).append("\"},");
        }
        if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

    private AiTransactionParseResult mapToResult(AiRawParseDto raw, List<Category> categories) {
        AiTransactionParseResult result = new AiTransactionParseResult();
        result.amount = raw.amount;
        result.type = raw.type;
        result.categoryName = raw.categoryName;
        result.date = raw.date;
        result.note = raw.note;
        result.isComplete = raw.isComplete != null && raw.isComplete;
        result.message = raw.message;

        // Tìm danh mục phù hợp trong danh sách của user
        if (raw.categoryName != null && raw.type != null) {
            Category.TransactionType txType;
            try {
                txType = Category.TransactionType.valueOf(raw.type.toUpperCase());
            } catch (Exception e) {
                txType = null;
            }
            final Category.TransactionType finalType = txType;
            Optional<Category> matched = categories.stream()
                    .filter(c -> c.categoryName.equalsIgnoreCase(raw.categoryName)
                            && (finalType == null || c.type.equals(finalType)))
                    .findFirst();

            if (matched.isPresent()) {
                result.categoryId = matched.get().categoryId;
                result.newCategoryRequired = false;
            } else {
                result.categoryId = null;
                result.newCategoryRequired = result.isComplete; // Chỉ báo cần tạo mới nếu đủ thông tin
            }
        }

        return result;
    }

    private String cleanJson(String raw) {
        if (raw == null) return "{}";
        // Loại bỏ code-block markdown nếu model lỡ thêm
        raw = raw.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        // Trích xuất JSON đầu tiên tìm thấy
        Pattern pattern = Pattern.compile("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(raw);
        if (matcher.find()) {
            return matcher.group();
        }
        return raw;
    }

    /** DTO nội bộ để nhận kết quả raw từ AI (trước khi xử lý thêm). */
    public static class AiRawParseDto {
        public Double amount;
        public String type;
        public String categoryName;
        public LocalDate date;
        public String note;
        public Boolean isComplete;
        public String message;
    }
}
