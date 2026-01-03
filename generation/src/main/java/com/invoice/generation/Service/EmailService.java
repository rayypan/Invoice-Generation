package com.invoice.generation.Service;

import java.io.File;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class EmailService {

    @Value("${email.api.url}")
    private String emailApiUrl;

    @Value("${email.smtp.host}")
    private String smtpHost;

    @Value("${email.smtp.port}")
    private String smtpPort;

    @Value("${email.smtp.user}")
    private String smtpUser;

    @Value("${email.smtp.password}")
    private String smtpPassword;

    @Value("${email.from}")
    private String from;

    @Value("${email.cc:}")
    private String cc;

    @Value("${email.bcc:}")
    private String bcc;

    public void sendInvoice(String to, String pdfPath, String customerName,
            String invoiceStatus, String date) {

        try {
            RestTemplate restTemplate = new RestTemplate();
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // Recipient
            body.add("to", to);

            // CC and BCC (if provided)
            addMultipleEmails(body, "cc", cc);
            addMultipleEmails(body, "bcc", bcc);

            // Email details
            body.add("from", from);
            body.add("subject", "Invoice - " + invoiceStatus);
            
            String htmlBody = String.format(
                "<p>Hi %s,<br/>Your invoice (%s) dated %s is attached.</p>",
                customerName,
                invoiceStatus,
                date
            );
            body.add("body", htmlBody);
            body.add("isHtml", "true");

            // SMTP credentials
            body.add("emailHost", smtpHost);
            body.add("emailPort", smtpPort);
            body.add("emailUser", smtpUser);
            body.add("emailPassword", smtpPassword);

            // PDF attachment
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                body.add("file", new FileSystemResource(pdfFile));
            }

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Create request
            HttpEntity<MultiValueMap<String, Object>> request = 
                new HttpEntity<>(body, headers);

            // Send email
            ResponseEntity<String> response = restTemplate.postForEntity(
                emailApiUrl,
                request,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Email sent successfully: " + response.getBody());
            } else {
                throw new RuntimeException("Email failed: " + response.getBody());
            }

        } catch (Exception e) {
            throw new RuntimeException("Email sending failed: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to add multiple email addresses for CC/BCC
     */
    private void addMultipleEmails(MultiValueMap<String, Object> body, 
                                   String fieldName, String emails) {
        if (emails == null || emails.isBlank()) {
            return;
        }

        Arrays.stream(emails.split(","))
            .map(String::trim)
            .filter(email -> !email.isEmpty())
            .forEach(email -> body.add(fieldName, email));
    }
}