package com.finance.pfm.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Optional;

/**
 * Cấu hình khởi tạo các model AI thông qua Groq (sử dụng chuẩn OpenAI).
 * Đọc API Key qua Quarkus Config (application.properties / .env / biến môi trường).
 */
@ApplicationScoped
public class AiConfig {

    private static final Logger LOG = Logger.getLogger(AiConfig.class);

    private static final String MODEL_ID = "llama-3.3-70b-versatile";
    private static final String BASE_URL = "https://api.groq.com/openai/v1";

    @ConfigProperty(name = "groq.api.key")
    Optional<String> groqApiKey;

    private String getApiKey() {
        // Ưu tiên Quarkus config property, fallback System.getenv
        String key = groqApiKey.filter(k -> !k.isBlank()).orElseGet(() -> System.getenv("GROQ_API_KEY"));
        if (key == null || key.isBlank()) {
            LOG.warn("[AiConfig] GROQ_API_KEY chưa được cấu hình. Tính năng AI sẽ không hoạt động.");
            return null;
        }
        return key;
    }

    @Produces
    @ApplicationScoped
    public ChatLanguageModel chatLanguageModel() {
        String apiKey = getApiKey();
        if (apiKey == null) {
            // Trả về model noop để app vẫn khởi động được, tránh NPE
            return (messages) -> { throw new RuntimeException("GROQ_API_KEY chưa được cấu hình. Tính năng AI không khả dụng."); };
        }
        LOG.info("[AiConfig] Khởi tạo ChatLanguageModel (Groq Llama 3).");
        return OpenAiChatModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(apiKey)
                .modelName(MODEL_ID)
                .temperature(0.2)
                .maxRetries(0)
                .timeout(Duration.ofSeconds(15))
                .build();
    }

    @Produces
    @ApplicationScoped
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        String apiKey = getApiKey();
        if (apiKey == null) {
            return (messages, handler) -> handler.onError(
                new RuntimeException("GROQ_API_KEY chưa được cấu hình. Tính năng AI không khả dụng."));
        }
        LOG.info("[AiConfig] Khởi tạo StreamingChatLanguageModel (Groq Llama 3).");
        return OpenAiStreamingChatModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(apiKey)
                .modelName(MODEL_ID)
                .temperature(0.7)
                .build();
    }
}
