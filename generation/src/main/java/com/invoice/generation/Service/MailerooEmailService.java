package com.invoice.generation.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    public void sendInvoice(String to, String pdfPath, String customerName,
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

            // ✅ STEP 5: Build JSON request body using Jackson
            System.out.println("🔵 STEP 5: Building Maileroo JSON request...");
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode body = mapper.createObjectNode();

            // Add recipients
            body.put("to", to);
            System.out.println("   Added 'to': " + to);

            // Add CC if present
            if (cc != null && !cc.isBlank()) {
                body.put("cc", cc);
                System.out.println("   Added 'cc': " + cc);
            }

            // Add BCC if present
            if (bcc != null && !bcc.isBlank()) {
                body.put("bcc", bcc);
                System.out.println("   Added 'bcc': " + bcc);
            }

            body.put("from", from);
            System.out.println("   Added 'from': " + from);
            
            body.put("subject", "Invoice - " + invoiceStatus);
            System.out.println("   Added 'subject': Invoice - " + invoiceStatus);
            
            String htmlBody = String.format(
                "<p>Hi %s,<br/>Your invoice (%s) dated %s is attached.</p>",
                customerName, invoiceStatus, date
            );
            body.put("html", htmlBody);
            System.out.println("   Added 'html' body");

            // Add attachment
            System.out.println("🔵 STEP 6: Adding PDF attachment...");
            ArrayNode attachments = mapper.createArrayNode();
            ObjectNode attachment = mapper.createObjectNode();
            attachment.put("filename", pdfFile.getName());
            attachment.put("content", base64Content);
            attachment.put("type", "application/pdf");
            attachments.add(attachment);
            body.set("attachments", attachments);
            System.out.println("✅ PDF attachment added to JSON body");

            // ✅ STEP 7: Set headers with API key
            System.out.println("🔵 STEP 7: Setting request headers...");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);
            System.out.println("✅ Headers set with Maileroo API Key");

            // ✅ STEP 8: Create request entity
            System.out.println("🔵 STEP 8: Creating HTTP request entity...");
            String jsonBody = mapper.writeValueAsString(body);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            System.out.println("✅ Request entity created");

            // ✅ STEP 9: Send request
            System.out.println("🔵 STEP 9: Sending HTTP POST request to Maileroo API...");
            System.out.println("   URL: " + mailerooApiUrl);
            
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(
                mailerooApiUrl,
                request,
                String.class
            );

            System.out.println("🔵 STEP 10: Received response from Maileroo API");
            System.out.println("   Status Code: " + response.getStatusCode());
            System.out.println("   Response Body: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK || 
                response.getStatusCode() == HttpStatus.ACCEPTED) {
                System.out.println("✅✅✅ EMAIL SENT SUCCESSFULLY VIA MAILEROO ✅✅✅");
                
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