package com.invoice.generation.Service;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

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
    private String smtpPassword;

    @Value("${email.from.address}")
    private String from;

    public void sendEmail(
            String to,
            String customerName,
            String invoiceStatus,
            String date,
            File attachment
    ) {

        log.info("========== EMAIL FLOW START ==========");

        String subject = "Thank You | "
                + customerName + " | "
                + invoiceStatus + " | "
                + date;

        String textBody =
                "Dear " + customerName + ",\n\n"
                + "Thank you for choosing The Tinkori Tales.\n"
                + "Invoice Status: " + invoiceStatus + "\n\n"
                + "Best regards,\n"
                + "The Tinkori Tales";

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();

        form.add("to", to);
        form.add("from", "The Tinkori Tales <" + from + ">");
        form.add("subject", subject);
        form.add("text", textBody);

        form.add("emailHost", smtpHost);
        form.add("emailPort", smtpPort);
        form.add("emailUser", smtpUser);
        form.add("emailPassword", smtpPassword);

        if (attachment != null && attachment.exists()) {
            form.add("file", new FileSystemResource(attachment));
        }

        // 🔍 LOG EXACT FORM DATA
        log.info("FINAL FORM DATA:");
        form.forEach((k, v) -> log.info("{} = {}", k, v));

        WebClient webClient = WebClient.builder().build();

        webClient.post()
                .uri(apiUrl)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(form))
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(resp -> log.info("Email API response: {}", resp))
                .doOnError(err -> log.error("Email sending failed", err))
                .block(); // synchronous on purpose

        log.info("========== EMAIL FLOW END ==========");
    }
}
