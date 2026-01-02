package com.invoice.generation.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import okhttp3.*;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String apiKey;

    @Value("${email.from}")
    private String from;

    @Value("${email.cc:}")
    private String cc;

    @Value("${email.bcc:}")
    private String bcc;

    private static final String RESEND_URL = "https://api.resend.com/emails";

    public void sendInvoice(String to, String pdfPath, String customerName,
            String invoiceStatus, String date) {

        try {
            byte[] pdfBytes = Files.readAllBytes(new File(pdfPath).toPath());
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

            String json = """
            {
              "from": "%s",
              "to": ["%s"],
              "cc": [%s],
              "bcc": [%s],
              "subject": "Invoice - %s",
              "html": "<p>Hi %s,<br/>Your invoice (%s) dated %s is attached.</p>",
              "attachments": [
                {
                  "filename": "invoice.pdf",
                  "content": "%s"
                }
              ]
            }
            """.formatted(
                    from,
                    to,
                    wrap(cc),
                    wrap(bcc),
                    invoiceStatus,
                    customerName,
                    invoiceStatus,
                    date,
                    base64Pdf
            );

            Request request = new Request.Builder()
                    .url(RESEND_URL)
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();

            OkHttpClient client = new OkHttpClient();
            client.newCall(request).execute();

        } catch (Exception e) {
            throw new RuntimeException("Email sending failed", e);
        }
    }

    private String wrap(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(email -> "\"" + email + "\"")
                .collect(java.util.stream.Collectors.joining(","));
    }

}
