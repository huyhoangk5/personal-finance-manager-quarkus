package com.finance.pfm.service;

import com.finance.pfm.entity.PasswordResetToken;
import com.finance.pfm.entity.User;
import com.finance.pfm.entity.UserQrCode;
import com.finance.pfm.repository.PasswordResetTokenRepository;
import com.finance.pfm.repository.UserQrCodeRepository;
import com.finance.pfm.repository.UserRepository;
import com.finance.pfm.util.ValidationUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    @Inject
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Inject
    UserQrCodeRepository userQrCodeRepository;

    @Inject
    Mailer mailer;

    private final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();
    private final Map<String, QrSession> qrSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> qrLoginSessions = new ConcurrentHashMap<>();
    private final Map<String, String> qrTokenToSessionToken = new ConcurrentHashMap<>();

    private static class OtpData {
        String otp;
        LocalDateTime expiry;
        OtpData(String otp) {
            this.otp = otp;
            this.expiry = LocalDateTime.now().plusMinutes(5);
        }
        boolean isValid() {
            return LocalDateTime.now().isBefore(expiry);
        }
    }

    private static class QrSession {
        String token;
        LocalDateTime expiry;
        boolean used;
        User user;
        QrSession(String token) {
            this.token = token;
            this.expiry = LocalDateTime.now().plusMinutes(5);
            this.used = false;
            this.user = null;
        }
        boolean isValid() { return LocalDateTime.now().isBefore(expiry); }
    }

    @PostConstruct
    public void init() {
        // Initialization if needed
    }

    @Transactional
    public String registerUser(User user) {
        // Validate username
        ValidationUtil.ValidationResult usernameValidation = ValidationUtil.validateUsername(user.username);
        if (!usernameValidation.isValid()) {
            return "Lỗi: " + usernameValidation.getFirstError();
        }
        
        // Validate password
        ValidationUtil.ValidationResult passwordValidation = ValidationUtil.validatePassword(user.password);
        if (!passwordValidation.isValid()) {
            return "Lỗi: " + passwordValidation.getFirstError();
        }
        
        // Validate email if provided
        if (user.email != null && !user.email.trim().isEmpty()) {
            ValidationUtil.ValidationResult emailValidation = ValidationUtil.validateEmail(user.email);
            if (!emailValidation.isValid()) {
                return "Lỗi: " + emailValidation.getFirstError();
            }
            
            // Check email uniqueness
            Optional<User> existingEmail = userRepository.findByEmail(user.email.trim().toLowerCase());
            if (existingEmail.isPresent()) {
                return "Lỗi: Email đã tồn tại!";
            }
        }
        
        // Validate full name if provided
        if (user.fullName != null && !user.fullName.trim().isEmpty()) {
            ValidationUtil.ValidationResult fullNameValidation = ValidationUtil.validateFullName(user.fullName);
            if (!fullNameValidation.isValid()) {
                return "Lỗi: " + fullNameValidation.getFirstError();
            }
        }
        
        // Check username uniqueness
        if (userRepository.existsByUsername(user.username.trim())) {
            return "Lỗi: Tên đăng nhập đã tồn tại!";
        }
        
        // Normalize data
        user.username = ValidationUtil.normalizeString(user.username);
        user.email = user.email != null ? ValidationUtil.normalizeString(user.email).toLowerCase() : null;
        user.fullName = ValidationUtil.normalizeString(user.fullName);
        
        user.password = BcryptUtil.bcryptHash(user.password);
        userRepository.persist(user);
        return "Đăng ký thành công!";
    }

    public Optional<User> login(String loginInput, String password) {
        Optional<User> userOpt = userRepository.findByEmail(loginInput);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(loginInput);
        }
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (BcryptUtil.matches(password, user.password)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public Optional<User> findById(Long userId) {
        return Optional.ofNullable(userRepository.findById(userId));
    }

    @Transactional
    public User updateUser(User user) {
        return userRepository.getEntityManager().merge(user);
    }

    @Transactional
    public Optional<User> authenticateGoogle(String idTokenString) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList("923508787768-tirtvocpu20jrba6khna61ppbqjv3idj.apps.googleusercontent.com"))
                .build();
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                User user = userRepository.findByUsername(email).orElse(null);
                if (user == null) {
                    user = new User();
                    user.username = email;
                    user.password = BcryptUtil.bcryptHash(UUID.randomUUID().toString());
                    user.email = email;
                    user.fullName = name;
                    userRepository.persist(user);
                }
                return Optional.of(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<User> authenticateFacebook(String accessToken) {
        String email = "fb_" + UUID.randomUUID().toString() + "@example.com";
        String username = email;
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            user = new User();
            user.username = username;
            user.password = BcryptUtil.bcryptHash(UUID.randomUUID().toString());
            user.email = email;
            user.fullName = "Facebook User";
            userRepository.persist(user);
        }
        return Optional.of(user);
    }

    @Transactional
    public String changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId);
        if (user == null) {
            return "Lỗi: Không tìm thấy người dùng";
        }
        
        // Validate old password
        if (oldPassword == null || oldPassword.isEmpty()) {
            return "Lỗi: Mật khẩu cũ không được để trống";
        }
        
        if (!BcryptUtil.matches(oldPassword, user.password)) {
            return "Lỗi: Mật khẩu cũ không đúng";
        }
        
        // Validate new password
        ValidationUtil.ValidationResult passwordValidation = ValidationUtil.validatePassword(newPassword);
        if (!passwordValidation.isValid()) {
            return "Lỗi: " + passwordValidation.getFirstError();
        }
        
        // Check if new password is different from old password
        if (BcryptUtil.matches(newPassword, user.password)) {
            return "Lỗi: Mật khẩu mới phải khác mật khẩu cũ";
        }
        
        user.password = BcryptUtil.bcryptHash(newPassword);
        return "Đổi mật khẩu thành công";
    }

    public String generateAndSendOtp(String phoneNumber) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpStore.put(phoneNumber, new OtpData(otp));
        System.out.println("OTP for " + phoneNumber + " is " + otp + " (Twilio disabled)");
        return otp;
    }

    @Transactional
    public Optional<User> verifyOtpAndCreateUser(String phoneNumber, String otp) {
        OtpData otpData = otpStore.get(phoneNumber);
        if (otpData != null && otpData.isValid() && otpData.otp.equals(otp)) {
            User user = userRepository.findByUsername(phoneNumber).orElse(null);
            if (user == null) {
                user = new User();
                user.username = phoneNumber;
                user.password = BcryptUtil.bcryptHash(UUID.randomUUID().toString());
                user.fullName = "User " + phoneNumber;
                userRepository.persist(user);
            }
            otpStore.remove(phoneNumber);
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public String generateQrToken() {
        String token = UUID.randomUUID().toString();
        qrSessions.put(token, new QrSession(token));
        return token;
    }

    @Transactional
    public Optional<User> verifyQrToken(String token) {
        QrSession session = qrSessions.get(token);
        if (session != null && session.isValid() && !session.used) {
            User user = new User();
            user.username = "qr_" + UUID.randomUUID().toString().substring(0, 8);
            user.password = BcryptUtil.bcryptHash(UUID.randomUUID().toString());
            user.fullName = "User from QR";
            userRepository.persist(user);
            session.user = user;
            session.used = true;
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public Optional<User> getQrTokenStatus(String token) {
        QrSession session = qrSessions.get(token);
        if (session != null && session.isValid() && session.used) {
            return Optional.of(session.user);
        }
        return Optional.empty();
    }

    @Transactional
    public String generateUserQrCode(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) throw new RuntimeException("User not found");
        Optional<UserQrCode> existing = userQrCodeRepository.findByUser(user);
        if (existing.isPresent()) {
            return existing.get().qrToken;
        }
        String token = UUID.randomUUID().toString();
        UserQrCode qrCode = new UserQrCode();
        qrCode.user = user;
        qrCode.qrToken = token;
        qrCode.createdAt = LocalDateTime.now();
        userQrCodeRepository.persist(qrCode);
        return token;
    }

    public boolean confirmQrLogin(String qrToken, Long userId) {
        UserQrCode qrCode = userQrCodeRepository.findByQrToken(qrToken).orElse(null);
        if (qrCode == null || !qrCode.user.userId.equals(userId)) {
            return false;
        }
        String sessionToken = UUID.randomUUID().toString();
        qrLoginSessions.put(sessionToken, userId);
        qrTokenToSessionToken.put(qrToken, sessionToken);
        return true;
    }

    public Optional<User> getQrLoginUser(String qrToken) {
        String sessionToken = qrTokenToSessionToken.remove(qrToken);
        if (sessionToken == null) return Optional.empty();
        Long userId = qrLoginSessions.remove(sessionToken);
        if (userId != null) {
            return Optional.ofNullable(userRepository.findById(userId));
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public String createPasswordResetToken(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return null;
        User user = userOpt.get();
        passwordResetTokenRepository.deleteByUser(user);
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.token = token;
        resetToken.user = user;
        resetToken.expiry = LocalDateTime.now().plusHours(1);
        passwordResetTokenRepository.persist(resetToken);
        return token;
    }

    public void sendPasswordResetEmail(String email, String token) {
        String resetLink = "http://localhost:5173/reset-password?token=" + token;
        System.out.println("Link đặt lại mật khẩu: " + resetLink);
        try {
            mailer.send(Mail.withText(email, 
                "Đặt lại mật khẩu Finance Manager", 
                "Click vào link để đặt lại mật khẩu: " + resetLink));
        } catch (Exception e) {
            System.err.println("Gửi email thất bại: " + e.getMessage());
        }
    }

    @Transactional
    public String resetPassword(String token, String newPassword) {
        if (token == null || token.trim().isEmpty()) {
            return "Lỗi: Token không hợp lệ";
        }
        
        // Validate new password
        ValidationUtil.ValidationResult passwordValidation = ValidationUtil.validatePassword(newPassword);
        if (!passwordValidation.isValid()) {
            return "Lỗi: " + passwordValidation.getFirstError();
        }
        
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty() || tokenOpt.get().expiry.isBefore(LocalDateTime.now())) {
            return "Lỗi: Token không hợp lệ hoặc đã hết hạn";
        }
        
        User user = tokenOpt.get().user;
        user.password = BcryptUtil.bcryptHash(newPassword);
        passwordResetTokenRepository.delete(tokenOpt.get());
        return "Đặt lại mật khẩu thành công";
    }

    @Transactional
    public Optional<User> registerWithQrToken(String token, String email, String password) {
        QrSession session = qrSessions.get(token);
        if (session == null || !session.isValid() || session.used) {
            return Optional.empty();
        }
        User user = new User();
        user.username = email;
        user.email = email;
        user.password = BcryptUtil.bcryptHash(password);
        user.fullName = "";
        userRepository.persist(user);
        session.user = user;
        session.used = true;
        return Optional.of(user);
    }
}
