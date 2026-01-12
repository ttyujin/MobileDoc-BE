package com.mobiledoc.mobiledocbackend.auth;

import java.security.SecureRandom;

public final class OtpUtil {
    private static final SecureRandom RND = new SecureRandom();
    private OtpUtil() {}

    public static String new6Digit() {
        int n = RND.nextInt(1_000_000);
        return String.format("%06d", n);
    }
}
