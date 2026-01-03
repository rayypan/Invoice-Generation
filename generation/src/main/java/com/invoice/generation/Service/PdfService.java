package com.invoice.generation.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.invoice.generation.DTOs.InvoiceDTO;
import com.invoice.generation.DTOs.ItemDTO;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

@Service
public class PdfService {

    private final TemplateEngine templateEngine;

    public PdfService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String generatePdf(InvoiceDTO invoice, double amount) {

        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));

        // ✅ Calculate subtotal
        double subtotal = 0;
        for (ItemDTO item : invoice.items) {
            double itemTotal = item.price * item.quantity;

            if ("PERCENT".equals(item.discountType)) {
                itemTotal -= (itemTotal * item.discount / 100);
            } else {
                itemTotal -= item.discount;
            }
            subtotal += itemTotal;
        }

        Context context = new Context();
        context.setVariable("subtotal", subtotal);
        context.setVariable("applyOverallDiscount", invoice.applyOverallDiscount);
        context.setVariable("overallDiscount", invoice.overallDiscount);
        context.setVariable("overallDiscountType", invoice.overallDiscountType);
        context.setVariable("adjustmentAmount", invoice.adjustmentAmount);
        context.setVariable("adjustmentAmountType", invoice.adjustmentAmountType);
        context.setVariable("paymentMethod", invoice.paymentMethod);
        context.setVariable("paymentDetails", invoice.paymentDetails);
        context.setVariable("issuedBy", invoice.issuedBy);
        context.setVariable("items", invoice.items);
        context.setVariable("amount", amount);
        context.setVariable("name", invoice.customerName);
        context.setVariable("phone", invoice.customerPhone);
        context.setVariable("address", invoice.customerAddress);
        context.setVariable("date", date);
        context.setVariable("invoiceStatus", invoice.invoiceStatus);
        context.setVariable("ownerMessage", invoice.ownerMessage);

        String html = templateEngine.process("invoice", context);

        // ✅ Use system temp directory instead of current directory
        String tempDir = System.getProperty("java.io.tmpdir");
        String fileName = "invoice_" + System.currentTimeMillis() + ".pdf";
        String path = tempDir + File.separator + fileName;

        System.out.println("🔍 Generating PDF at: " + path);

        try (FileOutputStream os = new FileOutputStream(path)) {

            PdfRendererBuilder builder = new PdfRendererBuilder();
            
            // ✅ Get base URL for resources (for images in template)
            String baseUrl = getClass().getResource("/").toExternalForm();
            builder.withHtmlContent(html, baseUrl);

            builder.toStream(os);
            builder.run();

            // ✅ Force flush and close
            os.flush();

        } catch (Exception e) {
            System.err.println("❌ PDF generation failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate PDF", e);
        }

        // ✅ VALIDATE: Check if PDF was actually created and has content
        File pdfFile = new File(path);
        if (!pdfFile.exists()) {
            throw new RuntimeException("PDF file was not created at: " + path);
        }
        
        long fileSize = pdfFile.length();
        System.out.println("✅ PDF generated successfully. Size: " + fileSize + " bytes");
        
        if (fileSize == 0) {
            throw new RuntimeException("PDF file is empty (0 bytes). Check template and resources.");
        }

        return path;
    }
}