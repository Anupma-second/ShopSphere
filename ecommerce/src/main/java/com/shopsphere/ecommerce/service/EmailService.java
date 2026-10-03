package com.shopsphere.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends plain-text emails. If no SMTP server is configured
 * (spring.mail.host not set) the email is written to the log instead,
 * so local development works without any mail account.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.from}")
    private String from;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void send(String to, String subject, String body) {

        JavaMailSender sender = mailSenderProvider.getIfAvailable();

        if (sender == null) {
            log.warn("[MAIL NOT CONFIGURED - printing instead]\nTo: {}\nSubject: {}\n{}",
                    to, subject, body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
        } catch (Exception e) {
            // never break registration / reset because the mail server is down
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
