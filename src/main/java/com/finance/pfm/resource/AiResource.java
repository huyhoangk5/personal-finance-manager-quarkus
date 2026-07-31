package com.finance.pfm.resource;

import com.finance.pfm.dto.AiTransactionParseResult;
import com.finance.pfm.dto.request.AiChatRequest;
import com.finance.pfm.dto.request.AiConfirmTransactionRequest;
import com.finance.pfm.dto.request.AiParseRequest;
import com.finance.pfm.service.AiChatService;
import com.finance.pfm.service.AiTransactionService;
import io.smallrye.mutiny.Multi;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * REST Resource cho tất cả các tính năng AI của WalletZen.
 * - POST /api/ai/parse-transaction  → Nhập liệu thông minh: phân tích ngôn ngữ tự nhiên
 * - POST /api/ai/confirm-transaction → Xác nhận và lưu giao dịch
 * - POST /api/ai/chat               → Chatbot hỏi đáp (SSE streaming)
 */
@Path("/api/ai")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AI Assistant", description = "Các tính năng trợ lý ảo AI của WalletZen")
public class AiResource {

    private static final Logger LOG = Logger.getLogger(AiResource.class);

    @Inject
    AiTransactionService aiTransactionService;

    @Inject
    AiChatService aiChatService;

    /**
     * Phân tích câu lệnh ngôn ngữ tự nhiên thành thông tin giao dịch có cấu trúc.
     * Trả về bản "xem trước" để người dùng xác nhận trước khi lưu.
     */
    @POST
    @Path("/parse-transaction")
    @Operation(summary = "Nhập liệu thông minh: Phân tích câu lệnh tự nhiên thành dữ liệu giao dịch")
    public Response parseTransaction(AiParseRequest request, @Context SecurityContext ctx) {
        if (request == null || request.userMessage == null || request.userMessage.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Vui lòng nhập câu lệnh giao dịch."))
                    .build();
        }

        try {
            Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
            AiTransactionParseResult result = aiTransactionService.parseTransaction(request, userId);
            return Response.ok(result).build();
        } catch (Exception e) {
            LOG.error("Lỗi parse transaction: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Trợ lý AI tạm thời không khả dụng. Vui lòng thử lại sau."))
                    .build();
        }
    }

    /**
     * Xác nhận và lưu giao dịch vào CSDL sau khi người dùng đã xem trước.
     * Nếu danh mục chưa tồn tại, tự động tạo mới theo yêu cầu người dùng.
     */
    @POST
    @Path("/confirm-transaction")
    @Operation(summary = "Nhập liệu thông minh: Lưu giao dịch sau khi người dùng xác nhận")
    public Response confirmTransaction(AiConfirmTransactionRequest request, @Context SecurityContext ctx) {
        if (request == null || request.amount == null || request.type == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Dữ liệu giao dịch không đầy đủ."))
                    .build();
        }
        if (request.categoryId == null && (request.newCategoryName == null || request.newCategoryName.isBlank())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Vui lòng xác nhận danh mục cho giao dịch."))
                    .build();
        }

        try {
            Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
            String result = aiTransactionService.confirmAndSaveTransaction(request, userId);
            return Response.ok(Map.of("message", result)).build();
        } catch (Exception e) {
            LOG.error("Lỗi confirm transaction: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Lỗi hệ thống khi lưu giao dịch. Vui lòng thử lại."))
                    .build();
        }
    }

    /**
     * Chatbot hỏi đáp dữ liệu tài chính với phản hồi dạng streaming (SSE).
     * Sử dụng @Blocking để chạy trên worker thread (Hibernate cần worker thread).
     * Client kết nối qua fetch + ReadableStream để nhận từng token từ AI theo thời gian thực.
     */
    @POST
    @Path("/chat")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @io.smallrye.common.annotation.Blocking
    @Operation(summary = "Chatbot hỏi đáp tài chính với phản hồi streaming (SSE)")
    public Multi<String> chat(AiChatRequest request, @Context SecurityContext ctx) {
        if (request == null || request.message == null || request.message.isBlank()) {
            return Multi.createFrom().item("data: {\"error\": \"Vui lòng nhập câu hỏi.\"}\n\n");
        }

        Long userId;
        try {
            userId = Long.parseLong(ctx.getUserPrincipal().getName());
        } catch (Exception e) {
            return Multi.createFrom().item("data: {\"error\": \"Phiên đăng nhập không hợp lệ.\"}\n\n");
        }

        final Long finalUserId = userId;

        return Multi.createFrom().<String>emitter(emitter -> {
            try {
                String sessionId = aiChatService.chatBlocking(
                        request,
                        finalUserId,
                        // onToken: mỗi token từ AI → bọc JSON để bảo toàn khoảng trắng qua SSE
                        token -> {
                            try {
                                String escaped = token
                                        .replace("\\", "\\\\")
                                        .replace("\"", "\\\"")
                                        .replace("\n", "\\n")
                                        .replace("\r", "\\r")
                                        .replace("\t", "\\t");
                                emitter.emit("{\"t\":\"" + escaped + "\"}");
                            } catch (Exception ignored) {
                            }
                        },
                        // onComplete
                        fullResponse -> {
                            emitter.emit("[DONE]");
                            emitter.complete();
                        },
                        // onError
                        error -> {
                            LOG.error("Lỗi streaming AI: " + error.getMessage(), error);
                            try {
                                String errEscaped = "❌ Trợ lý ảo tạm thời không thể kết nối. Vui lòng thử lại sau!"
                                        .replace("\"", "\\\"");
                                emitter.emit("{\"t\":\"" + errEscaped + "\"}");
                            } catch (Exception ignored) {}
                            emitter.emit("[DONE]");
                            emitter.complete();
                        }
                );

                emitter.emit("[SESSION_ID] " + sessionId);

            } catch (Exception e) {
                LOG.error("Lỗi hệ thống trong chat endpoint: " + e.getMessage(), e);
                try {
                    String errEscaped = "❌ Đã xảy ra lỗi hệ thống. Vui lòng thử lại!"
                            .replace("\"", "\\\"");
                    emitter.emit("{\"t\":\"" + errEscaped + "\"}");
                } catch (Exception ignored) {}
                emitter.emit("[DONE]");
                emitter.complete();
            }
        }).runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
    }
}

