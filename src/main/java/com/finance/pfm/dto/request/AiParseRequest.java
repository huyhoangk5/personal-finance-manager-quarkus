package com.finance.pfm.dto.request;

/**
 * Request body cho API phân tích câu lệnh nhập liệu thông minh.
 */
public class AiParseRequest {
    /** Câu lệnh ngôn ngữ tự nhiên từ người dùng. VD: "Sáng nay ăn phở 50k" */
    public String userMessage;

    /**
     * Context JSON từ lần phân tích trước (dùng khi người dùng yêu cầu chỉnh sửa).
     * null nếu là câu lệnh mới.
     */
    public String previousContext;
}
