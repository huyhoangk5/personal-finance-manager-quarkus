package com.finance.pfm.dto;

import java.time.LocalDate;

/**
 * DTO phản hồi sau khi AI bóc tách ngôn ngữ tự nhiên thành thông tin giao dịch.
 * Được trả về cho Frontend để hiển thị bản "xem trước" trước khi lưu.
 */
public class AiTransactionParseResult {

    /** Số tiền bóc tách được (null nếu không xác định). */
    public Double amount;

    /** Loại giao dịch: "THU" hoặc "CHI" (null nếu không xác định). */
    public String type;

    /** Tên danh mục AI đề xuất (ví dụ: "Ăn uống", "Di chuyển"). */
    public String categoryName;

    /** ID danh mục khớp trong hệ thống (null nếu không tìm thấy danh mục phù hợp). */
    public Long categoryId;

    /** Ngày giao dịch (mặc định là hôm nay nếu người dùng không nói). */
    public LocalDate date;

    /** Ghi chú về giao dịch. */
    public String note;

    /**
     * true nếu danh mục đề xuất CHƯA tồn tại trong hệ thống
     * → Frontend sẽ hiển thị cảnh báo yêu cầu xác nhận tạo mới.
     */
    public boolean newCategoryRequired;

    /** Thông báo trả về cho người dùng (ví dụ: "Tôi thiếu thông tin số tiền..."). */
    public String message;

    /** true nếu AI đã bóc tách đủ thông tin bắt buộc (amount, type, categoryName). */
    public boolean isComplete;
}
