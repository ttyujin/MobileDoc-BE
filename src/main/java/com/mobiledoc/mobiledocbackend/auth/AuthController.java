package com.mobiledoc.mobiledocbackend.auth;

import com.mobiledoc.mobiledocbackend.auth.dto.*;
import com.mobiledoc.mobiledocbackend.user.User;
import com.mobiledoc.mobiledocbackend.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    public AuthController(
            UserRepository userRepository,
            EmailVerificationRepository emailVerificationRepository,
            PasswordEncoder passwordEncoder,
            MailService mailService
    ) {
        this.userRepository = userRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    // =========================
    // 아이디(이메일) 찾기 - 이름 기반 MVP
    // POST /auth/find-email
    // =========================
    @PostMapping("/find-email")
    public ResponseEntity<?> findEmail(@Valid @RequestBody FindEmailRequest req) {
        String name = req.name.trim();
        List<User> users = userRepository.findByNameIgnoreCase(name);

        if (users.isEmpty()) {
            return ResponseEntity.ok(new FindEmailResponse(null, "해당 정보로 가입된 계정이 있습니다."));
        }
        if (users.size() > 1) {
            return ResponseEntity.ok(new FindEmailResponse(null, "같은 이름의 계정이 여러 개 있습니다. 고객지원으로 확인 부탁드립니다."));
        }
        return ResponseEntity.ok(new FindEmailResponse(maskEmail(users.get(0).getEmail()), "확인했습니다."));
    }

    // =========================
    // 이메일 인증번호 발송 (회원가입/재설정 공용)
    // POST /auth/email/send-code
    // =========================
    @Transactional
    @PostMapping("/email/send-code")
    public ResponseEntity<?> sendEmailCode(@Valid @RequestBody SendEmailCodeRequest req) {

        String email = req.email.trim().toLowerCase();
        VerificationPurpose purpose = VerificationPurpose.valueOf(req.purpose.trim());

        // 존재 여부 노출 방지:
        // SIGNUP: 이미 가입된 이메일이면 "보냈다"만 응답하고 실제 발송은 스킵
        if (purpose == VerificationPurpose.SIGNUP && userRepository.existsByEmail(email)) {
            return ResponseEntity.ok(new SimpleOkResponse(true, "인증번호를 보냈습니다. 메일함을 확인 부탁드립니다."));
        }
        // RESET: 가입 안 된 이메일이면 "보냈다"만 응답하고 실제 발송은 스킵
        if (purpose == VerificationPurpose.RESET_PASSWORD && !userRepository.existsByEmail(email)) {
            return ResponseEntity.ok(new SimpleOkResponse(true, "인증번호를 보냈습니다. 메일함을 확인 부탁드립니다."));
        }

        // 재전송 제한(60초): 너무 자주 누르면 그냥 ok 응답
        var prevOpt = emailVerificationRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose);
        if (prevOpt.isPresent()) {
            EmailVerification prev = prevOpt.get();
            if (prev.getLastSentAt().isAfter(Instant.now().minusSeconds(60))) {
                return ResponseEntity.ok(new SimpleOkResponse(true, "인증번호를 보냈습니다. 메일함을 확인부탁드립니다."));
            }
        }

        // 최신 1개만 유지
        emailVerificationRepository.deleteByEmailAndPurpose(email, purpose);

        String code = OtpUtil.new6Digit();
        String codeHash = passwordEncoder.encode(code);
        Instant expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES);

        EmailVerification ev = new EmailVerification(email, purpose, codeHash, expiresAt);
        ev.markSentNow();
        emailVerificationRepository.save(ev);

        String subject = (purpose == VerificationPurpose.SIGNUP)
                ? "[MobileDoc] 회원가입 이메일 인증번호"
                : "[MobileDoc] 비밀번호 재설정 인증번호";

        String text = ""
                + "인증번호: " + code + "\n"
                + "유효시간: 10분\n\n"
                + "본인이 요청하지 않았다면 고객센터로 연락 부탁드립니다.";

        boolean sent = mailService.trySendText(email, subject, text);

        // 메일 전송 실패해도 서버/프론트 흐름이 “터지지 않게” 응답은 ok로 돌려줌.
        // 대신 서버 콘솔에 코드는 남겨서 개발 중 테스트 가능.
        if (!sent) {
            System.out.println("[DEV] Email code (mail failed) to " + email + " / " + purpose + ": " + code);
            return ResponseEntity.ok(new SimpleOkResponse(true, "인증번호를 보냈습니다."));
        }

        return ResponseEntity.ok(new SimpleOkResponse(true, "인증번호를 보냈습니다. 메일함을 확인 부탁드립니다."));
    }

    // =========================
    // 이메일 인증번호 확인 (공용)
    // POST /auth/email/verify-code
    // =========================
    @Transactional
    @PostMapping("/email/verify-code")
    public ResponseEntity<?> verifyEmailCode(@Valid @RequestBody VerifyEmailCodeRequest req) {

        String email = req.email.trim().toLowerCase();
        VerificationPurpose purpose = VerificationPurpose.valueOf(req.purpose.trim());
        String code = req.code.trim();

        var evOpt = emailVerificationRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose);
        if (evOpt.isEmpty()) return ResponseEntity.status(400).body(new Msg("인증번호가 올바르지 않거나 만료됐습니다."));

        EmailVerification ev = evOpt.get();

        if (ev.getExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(400).body(new Msg("인증번호가 올바르지 않거나 만료됐습니다."));
        }
        if (ev.getAttempts() >= 5) {
            return ResponseEntity.status(429).body(new Msg("시도 횟수가 너무 많습니다. 잠시 후 다시 시도하세요."));
        }

        ev.incAttempts();

        if (!passwordEncoder.matches(code, ev.getCodeHash())) {
            return ResponseEntity.status(400).body(new Msg("인증번호가 올바르지 않거나 만료됐습니다."));
        }

        ev.markVerified();
        return ResponseEntity.ok(new SimpleOkResponse(true, "인증 완료"));
    }

    // =========================
    // 회원가입: SIGNUP 인증 완료된 이메일만 허용
    // POST /auth/signup
    // =========================
    @Transactional
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {

        String email = req.email.trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(409).body(new Msg("이미 사용 중인 이메일입니다."));
        }

        var evOpt = emailVerificationRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, VerificationPurpose.SIGNUP);
        if (evOpt.isEmpty()) return ResponseEntity.status(400).body(new Msg("이메일 인증을 먼저 완료 부탁드립니다."));

        EmailVerification ev = evOpt.get();
        if (ev.getVerifiedAt() == null || ev.getExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(400).body(new Msg("이메일 인증을 먼저 완료 부탁드립니다."));
        }

        String hash = passwordEncoder.encode(req.password);
        userRepository.save(new User(email, hash, req.name.trim()));

        // 1회성 인증 기록 폐기
        emailVerificationRepository.deleteByEmailAndPurpose(email, VerificationPurpose.SIGNUP);

        return ResponseEntity.ok(new Msg("ok"));
    }

    // =========================
    // 로그인(기존)
    // POST /auth/login
    // =========================
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {

        String email = req.email.trim().toLowerCase();

        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(new Msg("이메일 또는 비밀번호가 올바르지 않습니다."));
        }

        var user = userOpt.get();
        if (!passwordEncoder.matches(req.password, user.getPasswordHash())) {
            return ResponseEntity.status(401).body(new Msg("이메일 또는 비밀번호가 올바르지 않습니다."));
        }

        return ResponseEntity.ok(new LoginResponse(user.getId(), user.getEmail(), user.getName()));
    }

    // =========================
    // 비밀번호 재설정: RESET_PASSWORD 코드 + 새 비번
    // POST /auth/password/reset-with-code
    // =========================
    @Transactional
    @PostMapping("/password/reset-with-code")
    public ResponseEntity<?> resetWithCode(@Valid @RequestBody ResetWithCodeRequest req) {

        String email = req.email.trim().toLowerCase();
        String code = req.code.trim();

        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(400).body(new Msg("인증번호가 올바르지 않거나 만료됐습니다."));
        }

        var evOpt = emailVerificationRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, VerificationPurpose.RESET_PASSWORD);
        if (evOpt.isEmpty()) {
            return ResponseEntity.status(400).body(new Msg("인증번호가 올바르지 않거나 만료됐습니다."));
        }

        EmailVerification ev = evOpt.get();
        if (ev.getExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(400).body(new Msg("인증번호가 올바르지 않거나 만료됐습니다."));
        }
        if (ev.getAttempts() >= 5) {
            return ResponseEntity.status(429).body(new Msg("시도 횟수가 너무 많습니다. 잠시 후 다시 시도 부탁드립니다."));
        }

        ev.incAttempts();

        if (!passwordEncoder.matches(code, ev.getCodeHash())) {
            return ResponseEntity.status(400).body(new Msg("인증번호가 올바르지 않거나 만료됐습니다."));
        }

        User user = userOpt.get();
        user.changePasswordHash(passwordEncoder.encode(req.newPassword));

        // 1회성 폐기
        emailVerificationRepository.deleteByEmailAndPurpose(email, VerificationPurpose.RESET_PASSWORD);

        return ResponseEntity.ok(new Msg("ok"));
    }

    // ===== responses =====

    static class Msg {
        public String message;
        public Msg(String message) { this.message = message; }
    }

    static class LoginResponse {
        public UUID id;
        public String email;
        public String name;

        public LoginResponse(UUID id, String email, String name) {
            this.id = id;
            this.email = email;
            this.name = name;
        }
    }

    static class FindEmailResponse {
        public String maskedEmail;
        public String message;

        public FindEmailResponse(String maskedEmail, String message) {
            this.maskedEmail = maskedEmail;
            this.message = message;
        }
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];

        String maskedLocal = local.length() <= 1 ? "*" : (local.charAt(0) + "***");
        int dot = domain.lastIndexOf('.');
        String tail = dot >= 0 ? domain.substring(dot) : "";
        String maskedDomain = domain.length() <= 2 ? "***" : (domain.charAt(0) + "***" + tail);

        return maskedLocal + "@" + maskedDomain;
    }
}
