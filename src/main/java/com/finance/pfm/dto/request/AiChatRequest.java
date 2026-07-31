package com.finance.pfm.dto.request;

/**
 * Request body cho API chatbot hỏi đáp.
 */
public class AiChatRequest {
    /** Câu hỏi của người dùng. */
    public String message;

    /**
     * Session ID để duy trì ngữ cảnh hội thoại.
     * Nếu null, backend sẽ tạo session mới.
     */
    public String sessionId;
}
