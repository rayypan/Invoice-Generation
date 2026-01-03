package com.invoice.generation.Controller;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.invoice.generation.DTOs.InvoiceDTO;
import com.invoice.generation.Service.EmailApiClient;
import com.invoice.generation.Service.EmailService;
import com.invoice.generation.Service.GoogleSheetsService;
import com.invoice.generation.Service.InvoiceService;
import com.invoice.generation.Service.PdfService;

@CrossOrigin(
        origins = {"http://localhost:3000", "https://invoicegeneration-pi.vercel.app"},
        methods = {RequestMethod.POST, RequestMethod.OPTIONS}
)
@RestController
@RequestMapping("/invoice")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private GoogleSheetsService googleSheetsService;

    @Autowired
    private EmailApiClient eac;

    @PostMapping("/generate")
    public String generateInvoice(@RequestBody InvoiceDTO invoice) {

        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));

        double amount = invoiceService.calculatePayable(invoice);

        String pdfPath = pdfService.generatePdf(invoice, amount);
        emailService.sendInvoice(invoice.customerEmail, pdfPath, invoice.customerName, invoice.invoiceStatus, date);

        String itemsSummary = invoice.items.stream()
                .map(item
                        -> item.name
                + " x" + item.quantity
                + " @ " + item.price
                )
                .collect(Collectors.joining("\n"));

        if (invoice.enableLogging && !"Customer".equalsIgnoreCase(invoice.issuedBy)
                && !invoice.invoiceStatus.equalsIgnoreCase("Order_Placed")) {
            String sheetName = "";
            if (invoice.invoiceStatus.equalsIgnoreCase("Placed")) {
                sheetName = "SP_LOG";
            } else if (invoice.invoiceStatus.equalsIgnoreCase("Quotation")) {
                sheetName = "QUOTATION_LOG";
            }
            googleSheetsService.writeRow(
                    sheetName,
                    "A:P",
                    List.of(
                            date,
                            itemsSummary,
                            Objects.toString(invoice.customerName, ""),
                            Objects.toString(invoice.customerPhone, ""),
                            Objects.toString(invoice.customerEmail, ""),
                            Objects.toString(invoice.customerAddress, ""),
                            Objects.toString(invoice.invoiceStatus, ""),
                            Objects.toString(invoice.ownerMessage, ""),
                            Objects.toString(invoice.paymentMethod, ""),
                            Objects.toString(invoice.paymentDetails, ""),
                            Objects.toString(invoice.issuedBy, ""),
                            Objects.toString(invoice.applyOverallDiscount, "false"),
                            Objects.toString(invoice.overallDiscount, "0"),
                            Objects.toString(invoice.overallDiscountType, ""),
                            Objects.toString(invoice.adjustmentAmount, "0"),
                            Objects.toString(invoice.adjustmentAmountType, "")
                    )
            );
        }

        if (!"Customer".equalsIgnoreCase(invoice.issuedBy) && !invoice.enableLogging) {

            googleSheetsService.writeRow(
                    "CP_LOG",
                    "A:P",
                    List.of(
                            date,
                            itemsSummary,
                            Objects.toString(invoice.customerName, ""),
                            Objects.toString(invoice.customerPhone, ""),
                            Objects.toString(invoice.customerEmail, ""),
                            Objects.toString(invoice.customerAddress, ""),
                            Objects.toString(invoice.invoiceStatus, ""),
                            Objects.toString(invoice.ownerMessage, ""),
                            Objects.toString(invoice.paymentMethod, ""),
                            Objects.toString(invoice.paymentDetails, ""),
                            Objects.toString(invoice.issuedBy, ""),
                            Objects.toString(invoice.applyOverallDiscount, "false"),
                            Objects.toString(invoice.overallDiscount, "0"),
                            Objects.toString(invoice.overallDiscountType, ""),
                            Objects.toString(invoice.adjustmentAmount, "0"),
                            Objects.toString(invoice.adjustmentAmountType, "")
                    )
            );
        }

        if ("Customer".equalsIgnoreCase(invoice.issuedBy)) {

            googleSheetsService.writeRow(
                    "ORDER_LOG",
                    "A:H",
                    List.of(
                            date,
                            itemsSummary,
                            Objects.toString(invoice.invoiceStatus, ""),
                            Objects.toString(invoice.customerName, ""),
                            Objects.toString(invoice.customerPhone, ""),
                            Objects.toString(invoice.customerEmail, ""),
                            Objects.toString(invoice.customerAddress, ""),
                            Objects.toString(invoice.issuedBy, ""),
                            Objects.toString(invoice.ownerMessage, "")
                    )
            );
        }

        return "Invoice generated & emailed successfully";
    }

    @GetMapping("/test-pdf")
    public String testPdf() {
        InvoiceDTO testInvoice = new InvoiceDTO();
        testInvoice.customerName = "Test Customer";
        testInvoice.customerPhone = "1234567890";
        testInvoice.customerAddress = "Test Address";
        testInvoice.invoiceStatus = "PAID";
        testInvoice.items = new ArrayList<>();

        String pdfPath = pdfService.generatePdf(testInvoice, 100.0);
        File pdfFile;
        pdfFile = new File(pdfPath);

        return "PDF Generated: " + pdfPath + ", Size: " + pdfFile.length() + " bytes";
    }

    @PostMapping("/emailapiclient")
    public String testemail() {
        EmailApiClient.sendEmail(
                "/path/to/invoice.pdf",
                "alice@example.com,bob@example.com",
                "John Doe <johndoe@example.com>",
                "Invoice Generated",
                "<h1>Hello</h1>",
                "smtp.gmail.com",
                "587",
                "your-email@gmail.com",
                "your-app-password"
        );

        return "Successful";

    }
}
