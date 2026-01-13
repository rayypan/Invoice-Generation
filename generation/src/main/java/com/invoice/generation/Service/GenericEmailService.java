package com.invoice.generation.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GenericEmailService {

    private static final Logger log
            = LoggerFactory.getLogger(GenericEmailService.class);

    @Value("${email.from.display}")
    private String fromDisplay;

    @Value("${email.api.url}")
    private String apiUrl;

    @Value("${email.smtp.user}")
    private String smtpUser;

    @Value("${email.smtp.password}")
    private String smtpPassword;

    @Value("${email.from.address}")
    private String from;

    @Value("${email.secret.key}") // same 16-char key as Vercel
    private String secretKey;

    @Value("${email.cc}")
    private String cc;

    @Value("${email.bcc}")
    private String bcc;

    @Value("${email.replyto}")
    private String replyTo;

    public void sendEmail(
            String to,
            String customerName,
            String invoiceStatus,
            String date,
            File attachment
    ) {

        if (secretKey == null || secretKey.length() != 16) {
            throw new IllegalStateException("email.secret.key must be exactly 16 characters");
        }

        log.info("========== EMAIL FLOW START ==========");

        try {
            String subject = "Thank You | "
                    + customerName + " | "
                    + invoiceStatus + " | "
                    + date;

            String textBody
                    = "Dear " + customerName + ",\n\n"
                    + "Thank you for choosing The Tinkori Tales. We truly appreciate your trust in us.\n"
                    + "Your invoice has been attached to this email.\n"
                    + "Invoice Status: " + invoiceStatus + "\n\n"
                    + "We hope this brings a small smile to your day.\n\n"
                    + "Warm regards,\n"
                    + "The Tinkori Tales\n"
                    + "Diptimoy Hazra\n"
                    + "Finance & Accounts\n"
                    + "For support, please email us at diptimoy2003@gmail.com";

            /* ========== BUILD PAYLOAD ========== */
            Map<String, Object> payload = new HashMap<>();
            payload.put("to", to);
            payload.put("from", fromDisplay + "<" + from + ">");
            payload.put("subject", subject);
            payload.put("text", textBody);
            payload.put("smtpUser", smtpUser);
            payload.put("smtpPassword", smtpPassword);

            if (attachment != null && attachment.exists()) {
                payload.put("attachments", new Object[]{
                    Map.of(
                    "fileName", attachment.getName(),
                    "fileBase64", Base64.getEncoder()
                    .encodeToString(
                    Files.readAllBytes(
                    attachment.toPath()
                    )
                    )
                    )
                });
            }

            if (cc != null && !cc.isBlank()) {
                payload.put("cc", cc);
            }
            if (cc != null && !cc.isBlank()) {
                payload.put("cc", cc);
            }
            if (replyTo != null && !replyTo.isBlank()) {
                payload.put("replyTo", replyTo);
            }
            /* ========== LOG BEFORE ENCRYPTION ========== */
            ObjectMapper mapper = new ObjectMapper();
            String jsonPayload = mapper.writeValueAsString(payload);

            log.info("EMAIL PAYLOAD BEFORE ENCRYPTION:");
            System.out.println(payload);
            log.info(jsonPayload);

            /* ========== ENCRYPT ========== */
            String encryptedPayload = encrypt(jsonPayload);

            log.info("Encrypted payload generated successfully");

            /* ========== SEND TO NODE SERVICE ========== */
            WebClient.create()
                    .post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(
                            Map.of("encryptedPayload", encryptedPayload)
                    )
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnNext(resp
                            -> log.info("Email API response: {}", resp)
                    )
                    .doOnError(err
                            -> log.error("Email API call failed", err)
                    )
                    .block();

        } catch (Exception e) {
            log.error("EMAIL FLOW FAILED", e);
            throw new RuntimeException("Email sending failed", e);
        }

        log.info("========== EMAIL FLOW END ==========");
    }

    /* ========== AES-128-ECB ENCRYPTION ========== */
    private String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        SecretKeySpec key
                = new SecretKeySpec(secretKey.getBytes(), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.getEncoder()
                .encodeToString(cipher.doFinal(data.getBytes()));
    }
}
