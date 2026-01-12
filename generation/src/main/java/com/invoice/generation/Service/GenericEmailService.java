package com.invoice.generation.Service;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class GenericEmailService {

    private static final Logger log
            = LoggerFactory.getLogger(GenericEmailService.class);

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

        String textBody
                = "Dear " + customerName + ",\n\n"
                + "Thank you for choosing The Tinkori Tales.\n"
                + "Invoice Status: " + invoiceStatus + "\n\n"
                + "Best regards,\n"
                + "The Tinkori Tales\n\n"
                + "Diptimoy Hazra\n"
                + "Finance & Accounts\n"
                + "For support email us at thetinkoritales@gmail.com";

        log.info("📧 Email send request started");

        // ===== VALIDATION =====
        log.debug("Validating required fields");

        if (to == null || to.isBlank()) {
            log.error("Recipient email missing");
            throw new IllegalArgumentException("Recipient email (to) is required");
        }

        if (subject == null || subject.isBlank()) {
            log.error("Subject missing");
            throw new IllegalArgumentException("Subject is required");
        }

        if (textBody == null || textBody.isBlank()) {
            log.error("Text body missing");
            throw new IllegalArgumentException("Text body is required");
        }

        log.info("Validation successful | To: {}", to);

        // ===== FORM DATA =====
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        log.debug("Adding required form fields");
        body.add("to", to);
        body.add("from", from);
        body.add("subject", subject);
        body.add("text", textBody);

        body.add("emailHost", smtpHost);
        body.add("emailPort", smtpPort);
        body.add("emailUser", smtpUser);
        body.add("emailPassword", smtpPassword);

        // ===== OPTIONAL CC =====
        if (cc != null && !cc.isBlank()) {
            log.debug("Adding CC recipients");
            for (String c : cc.split(",")) {
                body.add("cc", c.trim());
                log.debug("CC added: {}", c.trim());
            }
        } else {
            log.debug("No CC configured");
        }

        // ===== OPTIONAL BCC =====
        if (bcc != null && !bcc.isBlank()) {
            log.debug("Adding BCC recipients");
            for (String b : bcc.split(",")) {
                body.add("bcc", b.trim());
                log.debug("BCC added: {}", b.trim());
            }
        } else {
            log.debug("No BCC configured");
        }

        // ===== ATTACHMENT =====
        if (attachment != null) {
            log.debug("Attachment detected");
            if (attachment.exists()) {
                body.add("file", new FileSystemResource(attachment));
                log.info("Attachment added: {} ({} bytes)",
                        attachment.getName(),
                        attachment.length());
            } else {
                log.warn("Attachment file not found: {}", attachment.getAbsolutePath());
            }
        } else {
            log.debug("No attachment provided");
        }

        // ===== REQUEST =====
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request
                = new HttpEntity<>(body, headers);

        log.info("Sending email request to external email service");
        log.debug("Email API URL: {}", apiUrl);
        log.debug("SMTP Host: {}, Port: {}", smtpHost, smtpPort);
        log.debug("From: {}", from);

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response
                    = restTemplate.postForEntity(apiUrl, request, String.class);

            log.info("Email service responded with status: {}",
                    response.getStatusCode());

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Email sending failed | Response: {}", response.getBody());
                throw new RuntimeException("Email failed: " + response.getBody());
            }

            log.info("✅ Email sent successfully to {}", to);

        } catch (Exception e) {
            log.error("❌ Email sending error", e);
            throw new RuntimeException("Email sending failed", e);
        }
    }
}
