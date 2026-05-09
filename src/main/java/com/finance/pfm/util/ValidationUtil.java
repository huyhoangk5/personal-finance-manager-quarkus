package com.finance.pfm.util;

import java.util.regex.Pattern;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ValidationUtil {
    
    // Regex patterns
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9.@_-]{3,50}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,16}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern FULLNAME_PATTERN = Pattern.compile("^[a-zA-ZÀ-ỹ\\s\\.]{1,100}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9,10}$");
    private static final Pattern OTP_PATTERN = Pattern.compile("^\\d{6}$");
    
    // Constants
    private static final double MAX_AMOUNT = 1_000_000_000_000.0; // 10^12
    private static final int MAX_NOTE_LENGTH = 255;
    private static final int MAX_CATEGORY_NAME_LENGTH = 50;
    private static final int MAX_SEARCH_KEYWORD_LENGTH = 100;
    
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
        
        public ValidationResult() {
            this.valid = true;
            this.errors = new ArrayList<>();
        }
        
        public void addError(String error) {
            this.valid = false;
            this.errors.add(error);
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public String getFirstError() { return errors.isEmpty() ? null : errors.get(0); }
    }
    
    // Username validation (STT 1, 4)
    public static ValidationResult validateUsername(String username) {
        ValidationResult result = new ValidationResult();
        
        if (username == null || username.trim().isEmpty()) {
            result.addError("Tên đăng nhập không được để trống");
            return result;
        }
        
        username = username.trim();
        
        if (username.length() < 3 || username.length() > 50) {
            result.addError("Tên đăng nhập phải từ 3-50 ký tự");
        }
        
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            result.addError("Tên đăng nhập không hợp lệ");
        }
        
        return result;
    }
    
    // Password validation (STT 2, 19, 24)
    public static ValidationResult validatePassword(String password) {
        ValidationResult result = new ValidationResult();
        
        if (password == null || password.isEmpty()) {
            result.addError("Mật khẩu không được để trống");
            return result;
        }
        
        if (password.length() < 6 || password.length() > 16) {
            result.addError("Mật khẩu phải từ 6-16 ký tự");
        }
        
        if (password.contains(" ")) {
            result.addError("Mật khẩu không được chứa khoảng trắng");
        }
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            result.addError("Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt");
        }
        
        return result;
    }
    
    // Password confirmation validation (STT 3, 20, 25)
    public static ValidationResult validatePasswordConfirmation(String password, String confirmPassword) {
        ValidationResult result = new ValidationResult();
        
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            result.addError("Xác nhận mật khẩu không được để trống");
            return result;
        }
        
        if (!password.equals(confirmPassword)) {
            result.addError("Mật khẩu xác nhận không khớp");
        }
        
        return result;
    }
    
    // Email validation (STT 18, 22, 30, 33)
    public static ValidationResult validateEmail(String email) {
        ValidationResult result = new ValidationResult();
        
        if (email == null || email.trim().isEmpty()) {
            result.addError("Email không được để trống");
            return result;
        }
        
        email = email.trim().toLowerCase();
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            result.addError("Email không đúng định dạng");
        }
        
        return result;
    }
    
    // Full name validation (STT 21)
    public static ValidationResult validateFullName(String fullName) {
        ValidationResult result = new ValidationResult();
        
        if (fullName != null && !fullName.trim().isEmpty()) {
            fullName = fullName.trim();
            
            if (fullName.length() > 100) {
                result.addError("Họ và tên không được vượt quá 100 ký tự");
            }
            
            if (!FULLNAME_PATTERN.matcher(fullName).matches()) {
                result.addError("Họ và tên chỉ được chứa chữ cái, khoảng trắng và dấu chấm");
            }
        }
        
        return result;
    }
    
    // Transaction type validation (STT 6, 12, 15, 26)
    public static ValidationResult validateTransactionType(String type) {
        ValidationResult result = new ValidationResult();
        
        if (type == null || type.trim().isEmpty()) {
            result.addError("Loại giao dịch không được để trống");
            return result;
        }
        
        if (!"THU".equals(type) && !"CHI".equals(type) && !"all".equals(type)) {
            result.addError("Loại giao dịch chỉ được nhận THU, CHI hoặc all");
        }
        
        return result;
    }
    
    // Amount validation (STT 8, 13, 16, 17)
    public static ValidationResult validateAmount(Double amount) {
        ValidationResult result = new ValidationResult();
        
        if (amount == null) {
            result.addError("Số tiền không được để trống");
            return result;
        }
        
        if (amount <= 0) {
            result.addError("Số tiền phải là số dương lớn hơn 0");
        }
        
        if (amount > MAX_AMOUNT) {
            result.addError("Số tiền không được vượt quá " + String.format("%.0f", MAX_AMOUNT));
        }
        
        return result;
    }
    
    // Optional amount validation for budget limit
    public static ValidationResult validateOptionalAmount(Double amount) {
        ValidationResult result = new ValidationResult();
        
        if (amount != null) {
            if (amount <= 0) {
                result.addError("Hạn mức phải là số dương lớn hơn 0");
            }
            
            if (amount > MAX_AMOUNT) {
                result.addError("Hạn mức không được vượt quá " + String.format("%.0f", MAX_AMOUNT));
            }
        }
        
        return result;
    }
    
    // Date validation (STT 9, 27, 28)
    public static ValidationResult validateTransactionDate(LocalDate date) {
        ValidationResult result = new ValidationResult();
        
        if (date == null) {
            result.addError("Ngày giao dịch không được để trống");
            return result;
        }
        
        if (date.isAfter(LocalDate.now())) {
            result.addError("Ngày giao dịch không được lớn hơn ngày hiện tại");
        }
        
        return result;
    }
    
    // Date range validation (STT 27, 28)
    public static ValidationResult validateDateRange(LocalDate fromDate, LocalDate toDate) {
        ValidationResult result = new ValidationResult();
        
        if (fromDate != null && fromDate.isAfter(LocalDate.now())) {
            result.addError("Từ ngày không được lớn hơn ngày hiện tại");
        }
        
        if (toDate != null && toDate.isAfter(LocalDate.now())) {
            result.addError("Đến ngày không được lớn hơn ngày hiện tại");
        }
        
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            result.addError("Từ ngày không được lớn hơn đến ngày");
        }
        
        return result;
    }
    
    // Note validation (STT 10)
    public static ValidationResult validateNote(String note) {
        ValidationResult result = new ValidationResult();
        
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            result.addError("Ghi chú không được vượt quá " + MAX_NOTE_LENGTH + " ký tự");
        }
        
        return result;
    }
    
    // Category name validation (STT 11, 14)
    public static ValidationResult validateCategoryName(String categoryName) {
        ValidationResult result = new ValidationResult();
        
        if (categoryName == null || categoryName.trim().isEmpty()) {
            result.addError("Tên danh mục không được để trống");
            return result;
        }
        
        categoryName = categoryName.trim();
        
        if (categoryName.length() < 1 || categoryName.length() > MAX_CATEGORY_NAME_LENGTH) {
            result.addError("Tên danh mục phải từ 1-" + MAX_CATEGORY_NAME_LENGTH + " ký tự");
        }
        
        return result;
    }
    
    // Search keyword validation (STT 29)
    public static ValidationResult validateSearchKeyword(String keyword) {
        ValidationResult result = new ValidationResult();
        
        if (keyword != null && keyword.length() > MAX_SEARCH_KEYWORD_LENGTH) {
            result.addError("Từ khóa tìm kiếm không được vượt quá " + MAX_SEARCH_KEYWORD_LENGTH + " ký tự");
        }
        
        return result;
    }
    
    // Phone number validation (STT 31)
    public static ValidationResult validatePhoneNumber(String phoneNumber) {
        ValidationResult result = new ValidationResult();
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            result.addError("Số điện thoại không được để trống");
            return result;
        }
        
        phoneNumber = phoneNumber.trim();
        
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            result.addError("Số điện thoại phải có 10-11 số và bắt đầu bằng số 0");
        }
        
        return result;
    }
    
    // OTP validation (STT 32)
    public static ValidationResult validateOtp(String otp) {
        ValidationResult result = new ValidationResult();
        
        if (otp == null || otp.trim().isEmpty()) {
            result.addError("Mã OTP không được để trống");
            return result;
        }
        
        otp = otp.trim();
        
        if (!OTP_PATTERN.matcher(otp).matches()) {
            result.addError("Mã OTP phải có đúng 6 chữ số");
        }
        
        return result;
    }
    
    // Utility method to trim and normalize string
    public static String normalizeString(String input) {
        return input == null ? null : input.trim();
    }
    
    // Utility method to normalize category name for duplicate checking
    public static String normalizeCategoryName(String categoryName) {
        return categoryName == null ? null : categoryName.trim().toLowerCase();
    }
}