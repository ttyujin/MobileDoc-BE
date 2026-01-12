package com.mobiledoc.mobiledocbackend.auth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender gmailSender;
    private final JavaMailSender naverSender;

    @Value("${app.mail.gmail.username:}")
    private String gmailUsername;

    @Value("${app.mail.naver.username:}")
    private String naverUsername;

    @Value("${app.mail.gmail.from:MobileDoc <no-reply@mobiledoc.local>}")
    private String gmailFrom;

    @Value("${app.mail.naver.from:MobileDoc <no-reply@mobiledoc.local>}")
    private String naverFrom;

    public MailService(
            @Qualifier("gmailMailSender") JavaMailSender gmailSender,
            @Qualifier("naverMailSender") JavaMailSender naverSender
    ) {
        this.gmailSender = gmailSender;
        this.naverSender = naverSender;
    }

    public boolean trySendText(String to, String subject, String text) {
        // 1) Gmail 먼저
        if (isConfigured(gmailUsername)) {
            if (trySend(gmailSender, gmailFrom, to, subject, text)) return true;
        }

        // 2) 실패하거나 미설정이면 Naver
        if (isConfigured(naverUsername)) {
            if (trySend(naverSender, naverFrom, to, subject, text)) return true;
        }

        System.out.println("[WARN] No mail sender configured (gmail/naver).");
        return false;
    }

    private boolean isConfigured(String username) {
        return username != null && !username.isBlank();
    }

    private boolean trySend(JavaMailSender sender, String from, String to, String subject, String text) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            sender.send(msg);
            return true;
        } catch (Exception e) {
            System.out.println("[WARN] Mail send failed: " + e.getMessage());
            return false;
        }
    }
}
