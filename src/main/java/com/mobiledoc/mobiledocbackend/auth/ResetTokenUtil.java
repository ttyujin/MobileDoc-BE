package com.mobiledoc.mobiledocbackend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class ResetTokenUtil {

    private static final SecureRandom RND = new SecureRandom();

    private ResetTokenUtil() {}

    public static String newToken() {
        byte[] buf = new byte[32]; // 256-bit
        RND.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    public static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];

        String maskedLocal = local.length() <= 1
                ? "*"
                : local.charAt(0) + "***";

        String maskedDomain = domain.length() <= 2
                ? "***"
                : domain.charAt(0) + "***" + domain.substring(domain.lastIndexOf('.'));

        return maskedLocal + "@" + maskedDomain;
    }
}
