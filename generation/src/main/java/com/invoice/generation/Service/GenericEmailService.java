package com.invoice.generation.Service;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class GenericEmailService {

    private static final Logger log =
            LoggerFactory.getLogger(GenericEmailService.class);

    @Value("${email.api.url}")
    private String apiUrl;

    @Value("${email.smtp.host}")
    private String smtpHost;

    @Value("${email.smtp.port}")
    private String smtpPort;

    @Value("${email.smtp.user}")
    private String smtpUser;

    @Value("${email.smtp.password}")
    private String smtpPassword; // ❌ never log

    @Value("${email.from.address}")
    private String from;

    @Value("${email.cc:}")
    private String cc;

    @Value("${email.bcc:}")
    private String bcc;

    public void sendEmail(
            String to,
            String customerName,
            String invoiceStatus,
            String date,
            File attachment
    ) {

        String subject = "Thank You | "
                + customerName + " | "
                + invoiceStatus + " | "
                + date;

        String textBody =
                "Dear " + customerName + ",\n\n"
                + "Thank you for choosing The Tinkori Tales.\n"
                + "Invoice Status: " + invoiceStatus + "\n\n"
                + "Best regards,\n"
                + "The Tinkori Tales\n\n"
                + "Diptimoy Hazra\n"
                + "Finance & Accounts\n"
                + "For support email us at thetinkoritales@gmail.com";

        log.info("📧 Email send request started");

        // ===== VALIDATION =====
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient email (to) is required");
        }
        if (subject.isBlank()) {
            throw new IllegalArgumentException("Subject is required");
        }
        if (textBody.isBlank()) {
            throw new IllegalArgumentException("Text body is required");
        }

        // ===== FORM DATA =====
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("to", to);
        body.add("from", "The Tinkori Tales <" + from + ">");
        body.add("subject", subject);
        body.add("text", textBody);

        body.add("emailHost", smtpHost);
        body.add("emailPort", smtpPort);
        body.add("emailUser", smtpUser);
        body.add("emailPassword", smtpPassword);

        // OPTIONAL CC
        if (cc != null && !cc.isBlank()) {
            for (String c : cc.split(",")) {
                body.add("cc", c.trim());
            }
        }

        // OPTIONAL BCC
        if (bcc != null && !bcc.isBlank()) {
            for (String b : bcc.split(",")) {
                body.add("bcc", b.trim());
            }
        }

        // ATTACHMENT
        if (attachment != null && attachment.exists() && attachment.length() > 0) {
            body.add("file", new FileSystemResource(attachment));
            log.info("Attachment added: {} ({} bytes)",
                    attachment.getName(),
                    attachment.length());
        }

        // ===== REQUEST =====
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<MultiValueMap<String, Object>> request =
                new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(0, new FormHttpMessageConverter());

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(apiUrl, request, String.class);

            log.info("Email service responded with status: {}", response.getStatusCode());

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Email failed: " + response.getBody());
            }

            log.info("✅ Email sent successfully to {}", to);

        } catch (Exception e) {
            log.error("❌ Email sending error", e);
            throw new RuntimeException("Email sending failed", e);
        }
    }
}
