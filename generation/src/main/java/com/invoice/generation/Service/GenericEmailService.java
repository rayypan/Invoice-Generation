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
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
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
    private String smtpPassword; // never log

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

        log.info("========== EMAIL FLOW START ==========");

        /* STEP 1: INPUT LOG */
        log.info("STEP 1 → Input received");
        log.info("to={}, customerName={}, invoiceStatus={}, date={}",
                to, customerName, invoiceStatus, date);
        log.info("attachment={}", attachment != null ? attachment.getAbsolutePath() : "null");

        /* STEP 2: VALIDATION */
        log.info("STEP 2 → Validating inputs");
        if (to == null || to.isBlank()) {
            log.error("Recipient email missing");
            throw new IllegalArgumentException("Recipient email (to) is required");
        }

        /* STEP 3: BUILD SUBJECT & BODY */
        log.info("STEP 3 → Building subject and body");

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

        /* STEP 4: BUILD FORM DATA */
        log.info("STEP 4 → Building multipart form-data");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("to", to);
        body.add("from", "The Tinkori Tales <" + from + ">");
        body.add("subject", subject);
        body.add("text", textBody);

        body.add("emailHost", smtpHost);
        body.add("emailPort", smtpPort);
        body.add("emailUser", smtpUser);
        body.add("emailPassword", smtpPassword);

        if (cc != null && !cc.isBlank()) {
            for (String c : cc.split(",")) {
                body.add("cc", c.trim());
            }
        }

        if (bcc != null && !bcc.isBlank()) {
            for (String b : bcc.split(",")) {
                body.add("bcc", b.trim());
            }
        }

        if (attachment != null && attachment.exists() && attachment.length() > 0) {
            body.add("file", new FileSystemResource(attachment));
        }

        /* STEP 5: DUMP FORM DATA (THIS IS WHAT YOU ASKED FOR) */
        log.info("STEP 5 → FINAL FORM DATA BEFORE SENDING");

        body.forEach((key, values) -> {
            for (Object value : values) {
                if (value instanceof FileSystemResource file) {
                    try {
                        log.info("FORM → {} = FILE[name={}, size={} bytes]",
                                key, file.getFilename(), file.contentLength());
                    } catch (Exception e) {
                        log.info("FORM → {} = FILE[name={}]", key, file.getFilename());
                    }
                } else if ("emailPassword".equals(key)) {
                    log.info("FORM → {} = ****** (hidden)", key);
                } else {
                    log.info("FORM → {} = {}", key, value);
                }
            }
        });

        /* STEP 6: PREPARE REQUEST */
        log.info("STEP 6 → Preparing HTTP request");

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<MultiValueMap<String, Object>> request =
                new HttpEntity<>(body, headers);

        /* STEP 7: CONFIGURE REST TEMPLATE */
        log.info("STEP 7 → Configuring RestTemplate");

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().clear();
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        restTemplate.getMessageConverters().add(new ResourceHttpMessageConverter());

        /* STEP 8: SEND REQUEST */
        log.info("STEP 8 → Sending request to external email service");
        log.info("External URL → {}", apiUrl);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(apiUrl, request, String.class);

            /* STEP 9: RESPONSE */
            log.info("STEP 9 → Response received");
            log.info("Status={}, Body={}",
                    response.getStatusCode(),
                    response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Email failed: " + response.getBody());
            }

            log.info("STEP 10 → Email sent successfully");

        } catch (Exception e) {
            log.error("STEP 10 → Email sending FAILED", e);
            throw new RuntimeException("Email sending failed", e);
        }

        log.info("========== EMAIL FLOW END ==========");
    }
}
