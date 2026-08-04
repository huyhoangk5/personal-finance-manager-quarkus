package com.finance.pfm.service;

import com.finance.pfm.dto.request.AiChatRequest;
import com.finance.pfm.entity.AiChatMessage;
import com.finance.pfm.repository.AiChatMessageRepository;
import com.finance.pfm.repository.BudgetRepository;
import com.finance.pfm.repository.CategoryRepository;
import com.finance.pfm.repository.TransactionRepository;
import com.finance.pfm.repository.UserRepository;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service xử lý tính năng Chatbot hỏi đáp tài chính.
 * Tích hợp Function Calling (@Tool) để AI tự động truy vấn dữ liệu thực tế.
 * Sử dụng LangChain4j AiServices để quản lý Tool Calling tự động.
 */
@ApplicationScoped
public class AiChatService {

    private static final Logger LOG = Logger.getLogger(AiChatService.class);

    @Inject
    ChatLanguageModel chatModel;

    @Inject
    AiChatMessageRepository chatMessageRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    DashboardService dashboardService;

    @Inject
    CategoryService categoryService;

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    TransactionRepository transactionRepository;

    @Inject
    BudgetRepository budgetRepository;

    // ─── Interface cho AiServices ────────────────────────────────────────────
    interface FinancialAssistant {
        @SystemMessage("""
                Bạn là trợ lý tài chính AI của ứng dụng WalletZen. Trả lời bằng tiếng Việt, thân thiện và chuyên nghiệp.

                QUY TẮC:
                1. LUÔN sử dụng các công cụ (tools) được cung cấp để lấy dữ liệu tài chính thực tế trước khi trả lời. KHÔNG BAO GIỜ tự bịa số liệu.
                2. Khi người dùng hỏi về "tháng này" hoặc "tháng trước", hãy gọi hàm getCurrentDate() trước để biết tháng hiện tại, rồi gọi tiếp các hàm phù hợp.
                3. Khi cần so sánh giữa các tháng, hãy gọi hàm lấy dữ liệu cho từng tháng riêng biệt rồi phân tích.
                4. Sử dụng đơn vị VNĐ, định dạng số có dấu chấm phân cách hàng nghìn.
                5. Từ chối câu hỏi ngoài lĩnh vực tài chính cá nhân một cách lịch sự.
                6. Đưa ra nhận xét, phân tích xu hướng và lời khuyên hữu ích khi có đủ dữ liệu.

                KHẢ NĂNG GHI GIAO DỊCH:
                7. Khi người dùng đề cập đến một khoản thu/chi cụ thể (mua đồ, nhận lương, trả tiền, v.v.), hãy dùng hàm saveTransaction() để lưu ngay.
                   - Trước tiên gọi getUserCategories() để kiểm tra danh mục phù hợp đã tồn tại chưa.
                   - Nếu danh mục chưa tồn tại: vẫn gọi saveTransaction() bình thường — hệ thống sẽ tự tạo danh mục mới và thông báo lại.
                   - KHÔNG hỏi lại người dùng xem có muốn lưu không nếu thông tin đã đủ (có số tiền, loại thu/chi, danh mục gợi ý).
                   - Sau khi lưu thành công, thông báo kết quả rõ ràng cho người dùng.
                8. Quy tắc chuyển đổi số tiền: "k"/"nghìn" = ×1.000; "triệu"/"tr" = ×1.000.000. VD: "8 triệu" → 8000000, "50k" → 50000.
                9. Quy tắc xác định loại: mua/chi/trả/tiêu → CHI; nhận/lương/thu/bán/hoàn → THU.
                10. Ngày: "hôm nay" → ngày hiện tại (gọi getCurrentDate()); nếu người dùng nêu ngày cụ thể → dùng ngày đó.
                """)
        String chat(String userMessage);
    }

    /**
     * Xử lý câu hỏi từ chatbot bằng cơ chế Function Calling.
     * BLOCKING: Phương thức này chạy đồng bộ, gọi AI → nhận kết quả → stream tokens.
     * Caller (AiResource) phải đảm bảo chạy trên worker thread.
     */
    public String chatBlocking(AiChatRequest request, Long userId,
                               Consumer<String> onToken,
                               Consumer<String> onComplete,
                               Consumer<Exception> onError) {

        String sessionId = (request.sessionId != null && !request.sessionId.isBlank())
                ? request.sessionId
                : "session-" + userId + "-" + System.currentTimeMillis();

        try {
            // Lưu câu hỏi của người dùng vào DB
            saveMessage(sessionId, userId, "USER", request.message);

            // Tạo FinancialTools instance riêng cho userId này
            FinancialTools tools = new FinancialTools(
                    dashboardService,
                    categoryService,
                    categoryRepository,
                    transactionRepository,
                    budgetRepository,
                    userRepository,
                    userId);

            // Tải lịch sử chat gần đây vào ChatMemory
            ChatMemory memory = MessageWindowChatMemory.withMaxMessages(6);
            loadChatHistoryIntoMemory(sessionId, memory);

            // Xây dựng AiServices với Tool Calling
            FinancialAssistant assistant = AiServices.builder(FinancialAssistant.class)
                    .chatLanguageModel(chatModel)
                    .tools(tools)
                    .chatMemory(memory)
                    .build();

            // Gọi AI (AiServices sẽ tự xử lý multi-turn Tool Calling)
            String fullResponse = assistant.chat(request.message);

            // Stream từng từ về client (mô phỏng streaming effect)
            streamResponse(fullResponse, onToken);

            // Lưu response vào DB
            saveMessage(sessionId, userId, "AI", fullResponse);
            onComplete.accept(fullResponse);

        } catch (Exception e) {
            LOG.error("Lỗi chatbot Function Calling: " + e.getMessage(), e);
            onError.accept(e);
        }

        return sessionId;
    }

    /**
     * Stream từng đoạn nhỏ của response về client để tạo hiệu ứng typing.
     */
    private void streamResponse(String fullResponse, Consumer<String> onToken) {
        // Chia theo ranh giới từ: giữ cả khoảng trắng
        String[] words = fullResponse.split("(?<=\\s)|(?=\\s)");
        for (String word : words) {
            onToken.accept(word);
            try {
                Thread.sleep(15);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // ─── Chat History ─────────────────────────────────────────────────────────

    private void loadChatHistoryIntoMemory(String sessionId, ChatMemory memory) {
        try {
            List<AiChatMessage> history = chatMessageRepository.findRecentBySessionId(sessionId, 3);
            for (AiChatMessage msg : history) {
                if ("USER".equals(msg.role)) {
                    memory.add(UserMessage.from(msg.content));
                } else if ("AI".equals(msg.role)) {
                    memory.add(AiMessage.from(msg.content));
                }
            }
        } catch (Exception e) {
            LOG.warn("Không thể tải lịch sử chat: " + e.getMessage());
        }
    }

    @Transactional
    public void saveMessage(String sessionId, Long userId, String role, String content) {
        try {
            AiChatMessage msg = new AiChatMessage();
            msg.sessionId = sessionId;
            msg.role = role;
            msg.content = content;
            msg.createdAt = LocalDateTime.now();
            msg.user = userRepository.findById(userId);
            chatMessageRepository.persist(msg);
        } catch (Exception e) {
            LOG.error("Lỗi khi lưu lịch sử chat: " + e.getMessage());
        }
    }
}
