package com.mobiledoc.mobiledocbackend.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    @Qualifier("gmailMailSender")
    public JavaMailSender gmailMailSender(
            @Value("${app.mail.gmail.host}") String host,
            @Value("${app.mail.gmail.port}") int port,
            @Value("${app.mail.gmail.username:}") String username,
            @Value("${app.mail.gmail.password:}") String password
    ) {
        return build(host, port, username, password);
    }

    @Bean
    @Qualifier("naverMailSender")
    public JavaMailSender naverMailSender(
            @Value("${app.mail.naver.host}") String host,
            @Value("${app.mail.naver.port}") int port,
            @Value("${app.mail.naver.username:}") String username,
            @Value("${app.mail.naver.password:}") String password
    ) {
        return build(host, port, username, password);
    }

    private JavaMailSender build(String host, int port, String username, String password) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);

        // 비어있으면 “설정 안 된 상태”로 두고, MailService에서 스킵/실패 처리
        if (username != null && !username.isBlank()) sender.setUsername(username);
        if (password != null && !password.isBlank()) sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");

        return sender;
    }
}
