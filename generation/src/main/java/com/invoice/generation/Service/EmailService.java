package com.invoice.generation.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

        System.out.println("\n========================================");
        System.out.println("📧 STARTING EMAIL SEND PROCESS");
        System.out.println("========================================");

        try {
            // ✅ STEP 1: Validate PDF file
            System.out.println("🔵 STEP 1: Validating PDF file");
            System.out.println("   Path received: " + pdfPath);
            
            File pdfFile = new File(pdfPath);
            
            System.out.println("🔵 STEP 2: Checking if file exists...");
            if (!pdfFile.exists()) {
                System.err.println("❌ FILE DOES NOT EXIST: " + pdfPath);
                throw new IllegalArgumentException("PDF file not found at: " + pdfPath);
            }
            System.out.println("✅ File exists");

            System.out.println("🔵 STEP 3: Checking file size...");
            long fileSize = pdfFile.length();
            System.out.println("   File size from File.length(): " + fileSize + " bytes");
            
            if (fileSize == 0) {
                System.err.println("❌ FILE IS EMPTY (0 bytes)");
                throw new IllegalArgumentException("PDF file is empty: " + pdfPath);
            }
            System.out.println("✅ File has content: " + fileSize + " bytes");

            // ✅ STEP 4: Read file to verify it's readable
            System.out.println("🔵 STEP 4: Testing file readability...");
            try {
                byte[] fileBytes = Files.readAllBytes(pdfFile.toPath());
                System.out.println("✅ File is readable. Actual bytes read: " + fileBytes.length);
                
                if (fileBytes.length == 0) {
                    System.err.println("❌ Files.readAllBytes returned 0 bytes!");
                    throw new IllegalArgumentException("File appears empty when read");
                }
            } catch (IOException e) {
                System.err.println("❌ Cannot read file: " + e.getMessage());
                throw new RuntimeException("File is not readable", e);
            }

            // ✅ STEP 5: Create FileSystemResource
            System.out.println("🔵 STEP 5: Creating FileSystemResource...");
            FileSystemResource fileResource = new FileSystemResource(pdfFile);
            System.out.println("   Resource exists: " + fileResource.exists());
            System.out.println("   Resource filename: " + fileResource.getFilename());
            System.out.println("   Resource is readable: " + fileResource.isReadable());
            
            try {
                long resourceSize = fileResource.contentLength();
                System.out.println("   Resource content length: " + resourceSize + " bytes");
                
                if (resourceSize == 0) {
                    System.err.println("❌ FileSystemResource reports 0 bytes!");
                    throw new IllegalArgumentException("FileSystemResource content is empty");
                }
            } catch (IOException e) {
                System.err.println("❌ Cannot get resource content length: " + e.getMessage());
            }

            // ✅ STEP 6: Build email request
            System.out.println("🔵 STEP 6: Building email request body...");
            RestTemplate restTemplate = new RestTemplate();
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            body.add("to", to);
            System.out.println("   Added 'to': " + to);

            addMultipleEmails(body, "cc", cc);
            addMultipleEmails(body, "bcc", bcc);

            body.add("from", from);
            System.out.println("   Added 'from': " + from);
            
            body.add("subject", "Invoice - " + invoiceStatus);
            System.out.println("   Added 'subject': Invoice - " + invoiceStatus);
            
            String htmlBody = String.format(
                "<p>Hi %s,<br/>Your invoice (%s) dated %s is attached.</p>",
                customerName, invoiceStatus, date
            );
            body.add("body", htmlBody);
            body.add("isHtml", "true");
            System.out.println("   Added 'body' (HTML)");

            body.add("emailHost", smtpHost);
            body.add("emailPort", smtpPort);
            body.add("emailUser", smtpUser);
            body.add("emailPassword", smtpPassword);
            System.out.println("   Added SMTP config:");
            System.out.println("     Host: " + smtpHost);
            System.out.println("     Port: " + smtpPort);
            System.out.println("     User: " + smtpUser);

            // ✅ CRITICAL: Add file AFTER validation
            System.out.println("🔵 STEP 7: Adding PDF attachment to request...");
            body.add("file", fileResource);
            System.out.println("✅ PDF attachment added to multipart body");

            // ✅ STEP 8: Set headers
            System.out.println("🔵 STEP 8: Setting request headers...");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            System.out.println("✅ Headers set: Content-Type = multipart/form-data");

            // ✅ STEP 9: Create request entity
            System.out.println("🔵 STEP 9: Creating HTTP request entity...");
            HttpEntity<MultiValueMap<String, Object>> request = 
                new HttpEntity<>(body, headers);
            System.out.println("✅ Request entity created");

            // ✅ STEP 10: Send request
            System.out.println("🔵 STEP 10: Sending HTTP POST request to email API...");
            System.out.println("   URL: " + emailApiUrl);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                emailApiUrl,
                request,
                String.class
            );

            System.out.println("🔵 STEP 11: Received response from email API");
            System.out.println("   Status Code: " + response.getStatusCode());
            System.out.println("   Response Body: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("✅✅✅ EMAIL SENT SUCCESSFULLY ✅✅✅");
                
                // Clean up temp file
                if (pdfFile.delete()) {
                    System.out.println("🗑️ Temp PDF file deleted: " + pdfPath);
                } else {
                    System.out.println("⚠️ Could not delete temp PDF: " + pdfPath);
                }
            } else {
                System.err.println("❌ Email API returned non-200 status");
                throw new RuntimeException("Email failed with status: " + response.getStatusCode());
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("\n❌❌❌ HTTP CLIENT ERROR (400) ❌❌❌");
            System.err.println("Response Status: " + e.getStatusCode());
            System.err.println("Response Body: " + e.getResponseBodyAsString());
            System.err.println("This means the email API rejected our request");
            System.err.println("========================================\n");
            throw new RuntimeException("Email API error (400): " + e.getResponseBodyAsString(), e);
            
        } catch (Exception e) {
            System.err.println("\n❌❌❌ EMAIL SEND FAILED ❌❌❌");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================================\n");
            throw new RuntimeException("Email sending failed: " + e.getMessage(), e);
        }

        System.out.println("========================================\n");
    }

    private void addMultipleEmails(MultiValueMap<String, Object> body, 
                                   String fieldName, String emails) {
        if (emails == null || emails.isBlank()) {
            return;
        }

        Arrays.stream(emails.split(","))
            .map(String::trim)
            .filter(email -> !email.isEmpty())
            .forEach(email -> {
                body.add(fieldName, email);
                System.out.println("   Added '" + fieldName + "': " + email);
            });
    }
}


// ## **What This Will Show You**

// After deployment, when you make a request, you'll see detailed output like:
// ```
// ========================================
// 🔵 STEP 1: Starting PDF Generation
// ========================================
// 🔵 STEP 2: Creating Thymeleaf context
// 🔵 STEP 3: Processing Thymeleaf template
// ✅ HTML generated: 5432 characters
// 🔵 STEP 4: PDF path: /tmp/invoice_1704300000000.pdf
// ✅ FileOutputStream created
// 🔵 STEP 5: Base URL for resources: file:/app/
// 🔵 STEP 6: Running PDF builder...
// ✅ PDF builder completed
// 🔵 STEP 7: Flushing stream...
// ✅ Stream flushed
// ✅ Stream closed
// 🔵 STEP 8: Validating PDF file...
// ✅ PDF file exists
// 🔵 STEP 9: PDF file size: 45678 bytes
// ✅✅✅ PDF GENERATION COMPLETE ✅✅✅
// 📄 Path: /tmp/invoice_1704300000000.pdf
// 📊 Size: 45678 bytes
// ========================================

// ========================================
// 📧 STARTING EMAIL SEND PROCESS
// ========================================
// 🔵 STEP 1: Validating PDF file
//    Path received: /tmp/invoice_1704300000000.pdf
// 🔵 STEP 2: Checking if file exists...
// ✅ File exists
// 🔵 STEP 3: Checking file size...
//    File size from File.length(): 45678 bytes
// ✅ File has content: 45678 bytes
// 🔵 STEP 4: Testing file readability...
// ✅ File is readable. Actual bytes read: 45678
// 🔵 STEP 5: Creating FileSystemResource...
//    Resource exists: true
//    Resource filename: invoice_1704300000000.pdf
//    Resource is readable: true
//    Resource content length: 45678 bytes
// 🔵 STEP 6: Building email request body...
//    Added 'to': customer@example.com
//    Added 'from': Your Name <your@email.com>
//    Added 'subject': Invoice - PAID
//    Added 'body' (HTML)
//    Added SMTP config:
//      Host: smtp.gmail.com
//      Port: 587
//      User: your@email.com
// 🔵 STEP 7: Adding PDF attachment to request...
// ✅ PDF attachment added to multipart body
// 🔵 STEP 8: Setting request headers...
// ✅ Headers set: Content-Type = multipart/form-data
// 🔵 STEP 9: Creating HTTP request entity...
// ✅ Request entity created
// 🔵 STEP 10: Sending HTTP POST request to email API...
//    URL: https://generic-email-service.vercel.app/api/v1/email
// 🔵 STEP 11: Received response from email API
//    Status Code: 200 OK
//    Response Body: {"message":"Email sent successfully"}
// ✅✅✅ EMAIL SENT SUCCESSFULLY ✅✅✅
// 🗑️ Temp PDF file deleted: /tmp/invoice_1704300000000.pdf
// ========================================