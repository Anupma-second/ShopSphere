package com.shopsphere.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Sends plain-text emails, trying in this order:
 * 1. Brevo HTTP API (BREVO_API_KEY set) - works on hosts that block SMTP ports, like Render free
 * 2. SMTP (spring.mail.host set) - handy locally
 * 3. Nothing configured - the email is written to the log instead
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    private final RestClient brevo = RestClient.builder()
            .baseUrl("https://api.brevo.com/v3")
            .build();

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.from-name:ShopSphere}")
    private String fromName;

    @Value("${app.mail.brevo-api-key:}")
    private String brevoApiKey;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void send(String to, String subject, String body) {

        // never break registration / reset because the mail provider is down
        try {
            if (!brevoApiKey.isBlank()) {
                sendWithBrevo(to, subject, body);
                return;
            }

            JavaMailSender sender = mailSenderProvider.getIfAvailable();

            if (sender == null) {
                log.warn("[MAIL NOT CONFIGURED - printing instead]\nTo: {}\nSubject: {}\n{}",
                        to, subject, body);
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private void sendWithBrevo(String to, String subject, String body) {

        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", fromName, "email", from),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "textContent", body);

        brevo.post()
                .uri("/smtp/email")
                .header("api-key", brevoApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        log.info("Email sent via Brevo to {}: {}", to, subject);
    }
}