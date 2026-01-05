package com.invoice.generation.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Primary  // This makes it the default EmailService implementation
public class MailerooEmailService {

    @Value("${maileroo.api.url:https://smtp.maileroo.com/send}")
    private String mailerooApiUrl;

    @Value("${maileroo.api.key}")
    private String apiKey;

    @Value("${email.from}")
    private String from;

    @Value("${email.cc:}")
    private String cc;

    @Value("${email.bcc:}")
    private String bcc;

    public void sendEmailWithInvoice(String to, String pdfPath, String customerName,
            String invoiceStatus, String date) {

        System.out.println("\n========================================");
        System.out.println("📧 STARTING MAILEROO EMAIL SEND PROCESS");
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
            System.out.println("   File size: " + fileSize + " bytes");
            
            if (fileSize == 0) {
                System.err.println("❌ FILE IS EMPTY (0 bytes)");
                throw new IllegalArgumentException("PDF file is empty: " + pdfPath);
            }
            System.out.println("✅ File has content: " + fileSize + " bytes");

            // ✅ STEP 4: Read and encode file to Base64
            System.out.println("🔵 STEP 4: Reading and encoding PDF to Base64...");
            byte[] fileBytes = Files.readAllBytes(pdfFile.toPath());
            String base64Content = Base64.getEncoder().encodeToString(fileBytes);
            System.out.println("✅ PDF encoded to Base64. Length: " + base64Content.length());

            // ✅ STEP 5: Build JSON request body using Map (Maileroo format)
            System.out.println("🔵 STEP 5: Building Maileroo JSON request...");
            Map<String, Object> body = new HashMap<>();

            // Required fields
            body.put("to", to);
            System.out.println("   Added 'to': " + to);

            body.put("from", from);
            System.out.println("   Added 'from': " + from);
            
            body.put("subject", "Invoice - " + invoiceStatus);
            System.out.println("   Added 'subject': Invoice - " + invoiceStatus);
            
            // Email body
            String htmlBody = String.format(
                "<html><body>" +
                "<p>Hi %s,</p>" +
                "<p>Your invoice (%s) dated %s is attached.</p>" +
                "<p>Thank you for using The Tinkori Tales!</p>" +
                "<p>Best regards,<br/>The Tinkori Tales Team</p>" +
                "</body></html>",
                customerName, invoiceStatus, date
            );
            body.put("html", htmlBody);
            
            // Also add plain text version for better compatibility
            String textBody = String.format(
                "Hi %s,\n\nYour invoice (%s) dated %s is attached.\n\n" +
                "Thank you for using The Tinkori Tales!\n\n" +
                "Best regards,\nThe Tinkori Tales Team",
                customerName, invoiceStatus, date
            );
            body.put("text", textBody);
            System.out.println("   Added 'html' and 'text' body");

            // Optional: Add CC if present
            if (cc != null && !cc.isBlank()) {
                body.put("cc", cc);
                System.out.println("   Added 'cc': " + cc);
            }

            // Optional: Add BCC if present
            if (bcc != null && !bcc.isBlank()) {
                body.put("bcc", bcc);
                System.out.println("   Added 'bcc': " + bcc);
            }

            // Add attachment
            System.out.println("🔵 STEP 6: Adding PDF attachment...");
            List<Map<String, String>> attachments = new ArrayList<>();
            Map<String, String> attachment = new HashMap<>();
            attachment.put("filename", "invoice_" + invoiceStatus + ".pdf");
            attachment.put("content", base64Content);
            attachment.put("type", "application/pdf");
            attachments.add(attachment);
            body.put("attachments", attachments);
            System.out.println("✅ PDF attachment added to JSON body");
            System.out.println("   Attachment filename: invoice_" + invoiceStatus + ".pdf");

            // Debug: Print the JSON structure
            System.out.println("🔵 STEP 7: JSON Request Structure:");
            System.out.println("   Keys in body: " + body.keySet());
            System.out.println("   Subject value: " + body.get("subject"));
            System.out.println("   From value: " + body.get("from"));
            System.out.println("   To value: " + body.get("to"));

            // ✅ STEP 8: Set headers with API key
            System.out.println("🔵 STEP 8: Setting request headers...");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);
            headers.set("Accept", "application/json");
            System.out.println("✅ Headers set:");
            System.out.println("   Content-Type: application/json");
            System.out.println("   X-API-Key: " + (apiKey != null && apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : "NOT SET"));
            System.out.println("   Accept: application/json");

            // ✅ STEP 9: Create request entity
            System.out.println("🔵 STEP 9: Creating HTTP request entity...");
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            System.out.println("✅ Request entity created");

            // ✅ STEP 10: Send request
            System.out.println("🔵 STEP 10: Sending HTTP POST request to Maileroo API...");
            System.out.println("   URL: " + mailerooApiUrl);
            
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(
                mailerooApiUrl,
                request,
                String.class
            );

            System.out.println("🔵 STEP 11: Received response from Maileroo API");
            System.out.println("   Status Code: " + response.getStatusCode());
            System.out.println("   Response Body: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK || 
                response.getStatusCode() == HttpStatus.ACCEPTED ||
                response.getStatusCode() == HttpStatus.CREATED) {
                System.out.println("✅✅✅ EMAIL SENT SUCCESSFULLY VIA MAILEROO ✅✅✅");
                System.out.println("📧 Email sent to: " + to);
                System.out.println("📎 Attachment: invoice_" + invoiceStatus + ".pdf");
                
                // Clean up temp file
                if (pdfFile.delete()) {
                    System.out.println("🗑️ Temp PDF file deleted: " + pdfPath);
                } else {
                    System.out.println("⚠️ Could not delete temp PDF: " + pdfPath);
                }
            } else {
                System.err.println("❌ Maileroo API returned non-success status");
                throw new RuntimeException("Email failed with status: " + response.getStatusCode());
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("\n❌❌❌ HTTP CLIENT ERROR ❌❌❌");
            System.err.println("Response Status: " + e.getStatusCode());
            System.err.println("Response Body: " + e.getResponseBodyAsString());
            System.err.println("This means Maileroo API rejected our request");
            System.err.println("\n🔍 Common issues:");
            System.err.println("   1. API Key not set or invalid (check MAILEROO_API_KEY env var)");
            System.err.println("   2. Sender email not verified in Maileroo dashboard");
            System.err.println("   3. Invalid email format");
            System.err.println("   4. Attachment too large (max 10MB)");
            System.err.println("========================================\n");
            throw new RuntimeException("Maileroo API error: " + e.getResponseBodyAsString(), e);
            
        } catch (IOException e) {
            System.err.println("\n❌❌❌ FILE READ ERROR ❌❌❌");
            System.err.println("Error reading PDF file: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================================\n");
            throw new RuntimeException("Failed to read PDF file: " + e.getMessage(), e);
            
        } catch (Exception e) {
            System.err.println("\n❌❌❌ EMAIL SEND FAILED ❌❌❌");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================================\n");
            throw new RuntimeException("Email sending failed: " + e.getMessage(), e);
        }

        System.out.println("========================================\n");
    }
}