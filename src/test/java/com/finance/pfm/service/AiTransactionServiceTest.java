package com.finance.pfm.service;

import com.finance.pfm.dto.AiTransactionParseResult;
import com.finance.pfm.dto.request.AiParseRequest;
import com.finance.pfm.entity.Category;
import com.finance.pfm.repository.CategoryRepository;
import com.finance.pfm.repository.TransactionRepository;
import com.finance.pfm.repository.UserRepository;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.data.message.AiMessage;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho AiTransactionService.
 * Sử dụng Mock để giả lập AI và Repository, không cần kết nối thực tế.
 */
@QuarkusTest
class AiTransactionServiceTest {

    @Inject
    AiTransactionService aiTransactionService;

    @InjectMock
    ChatLanguageModel chatModel;

    @InjectMock
    CategoryRepository categoryRepository;

    @InjectMock
    UserRepository userRepository;

    @InjectMock
    TransactionRepository transactionRepository;

    @InjectMock
    BudgetService budgetService;

    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        when(categoryRepository.findByUser_UserId(TEST_USER_ID)).thenReturn(
                List.of(createCategory(1L, "Ăn uống", "CHI"),
                        createCategory(2L, "Di chuyển", "CHI"),
                        createCategory(3L, "Lương", "THU"))
        );
    }

    @Test
    @DisplayName("TC-AI-01: Phân tích câu lệnh hợp lệ - 'Ăn phở 50k'")
    void testParseValidTransaction_FoodExpense() {
        // Giả lập AI trả về JSON chuẩn
        String mockJson = """
                {
                  "amount": 50000,
                  "type": "CHI",
                  "categoryName": "Ăn uống",
                  "date": null,
                  "note": "ăn phở",
                  "isComplete": true,
                  "message": null
                }
                """;
        when(chatModel.chat(ArgumentMatchers.<List<ChatMessage>>any()))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from(mockJson)).build());

        AiParseRequest request = new AiParseRequest();
        request.userMessage = "Ăn phở 50k";

        AiTransactionParseResult result = aiTransactionService.parseTransaction(request, TEST_USER_ID);

        assertNotNull(result, "Kết quả không được null");
        assertTrue(result.isComplete, "Phải đủ thông tin");
        assertEquals(50000.0, result.amount, "Số tiền phải là 50000");
        assertEquals("CHI", result.type, "Loại phải là CHI");
        assertEquals("Ăn uống", result.categoryName, "Danh mục phải là Ăn uống");
        assertEquals(1L, result.categoryId, "Phải tìm thấy categoryId = 1");
        assertFalse(result.newCategoryRequired, "Không cần tạo danh mục mới");
    }

    @Test
    @DisplayName("TC-AI-02: Phân tích câu lệnh thiếu số tiền - AI yêu cầu bổ sung")
    void testParseIncompleteTransaction_MissingAmount() {
        String mockJson = """
                {
                  "amount": null,
                  "type": "CHI",
                  "categoryName": "Ăn uống",
                  "date": null,
                  "note": "ăn phở",
                  "isComplete": false,
                  "message": "Bạn ơi, tôi chưa thấy số tiền cho giao dịch này. Bạn đã chi bao nhiêu vậy?"
                }
                """;
        when(chatModel.chat(ArgumentMatchers.<List<ChatMessage>>any()))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from(mockJson)).build());

        AiParseRequest request = new AiParseRequest();
        request.userMessage = "Hôm nay ăn phở";

        AiTransactionParseResult result = aiTransactionService.parseTransaction(request, TEST_USER_ID);

        assertFalse(result.isComplete, "Phải là thiếu thông tin");
        assertNull(result.amount, "Số tiền phải là null");
        assertNotNull(result.message, "Phải có thông báo yêu cầu bổ sung");
    }

    @Test
    @DisplayName("TC-AI-03: Phân tích câu lệnh với danh mục mới chưa tồn tại")
    void testParseTransaction_NewCategoryRequired() {
        String mockJson = """
                {
                  "amount": 200000,
                  "type": "CHI",
                  "categoryName": "Mua sắm quần áo",
                  "date": null,
                  "note": "mua áo",
                  "isComplete": true,
                  "message": null
                }
                """;
        when(chatModel.chat(ArgumentMatchers.<List<ChatMessage>>any()))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from(mockJson)).build());

        AiParseRequest request = new AiParseRequest();
        request.userMessage = "Mua áo 200k";

        AiTransactionParseResult result = aiTransactionService.parseTransaction(request, TEST_USER_ID);

        assertTrue(result.isComplete, "Phải đủ thông tin");
        assertNull(result.categoryId, "Không có categoryId vì danh mục chưa tồn tại");
        assertTrue(result.newCategoryRequired, "Phải yêu cầu tạo danh mục mới");
        assertEquals("Mua sắm quần áo", result.categoryName);
    }

    @Test
    @DisplayName("TC-AI-04: Xử lý khi AI gọi thất bại (kết nối mạng lỗi)")
    void testParseTransaction_AiConnectionFailed() {
        when(chatModel.chat(ArgumentMatchers.<List<ChatMessage>>any()))
                .thenThrow(new RuntimeException("Connection refused"));

        AiParseRequest request = new AiParseRequest();
        request.userMessage = "Ăn phở 50k";

        AiTransactionParseResult result = aiTransactionService.parseTransaction(request, TEST_USER_ID);

        assertFalse(result.isComplete, "Khi lỗi phải trả về false");
        assertNotNull(result.message, "Phải có thông báo lỗi");
        assertTrue(result.message.contains("kết nối"), "Thông báo phải đề cập lỗi kết nối");
    }

    @Test
    @DisplayName("TC-AI-05: AI trả về JSON có code-block markdown (phải được làm sạch)")
    void testParseTransaction_CleanMarkdownCodeBlock() {
        String mockJsonWithMarkdown = "```json\n{\"amount\":75000,\"type\":\"CHI\",\"categoryName\":\"Di chuyển\",\"date\":null,\"note\":\"đi xe ôm\",\"isComplete\":true,\"message\":null}\n```";
        when(chatModel.chat(ArgumentMatchers.<List<ChatMessage>>any()))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from(mockJsonWithMarkdown)).build());

        AiParseRequest request = new AiParseRequest();
        request.userMessage = "Đi xe ôm 75k";

        AiTransactionParseResult result = aiTransactionService.parseTransaction(request, TEST_USER_ID);

        assertNotNull(result);
        assertTrue(result.isComplete, "Phải parse được dù có code-block markdown");
        assertEquals(75000.0, result.amount);
        assertEquals("Di chuyển", result.categoryName);
    }

    // ─── Helper methods ────────────────────────────────────────────────────────

    private Category createCategory(Long id, String name, String type) {
        Category c = new Category();
        c.categoryId = id;
        c.categoryName = name;
        c.type = Category.TransactionType.valueOf(type);
        return c;
    }
}
