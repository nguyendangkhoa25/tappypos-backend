package com.tappy.pos.util;

import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.order.OrderItem;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class PdfBillGenerator {

    public static byte[] generateOrderBill(Order order) throws DocumentException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);

        document.open();

        // Title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("BARBER SHOP BILL", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("\n"));

        // Shop info
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        document.add(new Paragraph("Invoice Date: " + order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), headerFont));
        document.add(new Paragraph("\n"));

        // Customer info
        document.add(new Paragraph("CUSTOMER INFORMATION", headerFont));
        document.add(new Paragraph("Name: " + order.getCustomer().getName()));
        document.add(new Paragraph("Phone: " + order.getCustomer().getPhone()));
        if (order.getCustomer().getEmail() != null) {
            document.add(new Paragraph("Email: " + order.getCustomer().getEmail()));
        }
        document.add(new Paragraph("\n"));

        // Items table
        document.add(new Paragraph("ORDER DETAILS", headerFont));
        PdfTable table = createOrderItemsTable(order);
        document.add(table.getTable());
        document.add(new Paragraph("\n"));

        // Total amount
        Font totalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Paragraph totalPara = new Paragraph("TOTAL AMOUNT: " + order.getTotalAmount() + " VND", totalFont);
        totalPara.setAlignment(Element.ALIGN_RIGHT);
        document.add(totalPara);

        if (order.getNotes() != null && !order.getNotes().isEmpty()) {
            document.add(new Paragraph("\nNotes: " + order.getNotes()));
        }

        document.add(new Paragraph("\n\nThank you for your visit!"));

        document.close();
        return baos.toByteArray();
    }

    private static PdfTable createOrderItemsTable(Order order) throws DocumentException {
        com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(4);
        table.setWidthPercentage(100);

        // Header
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        table.addCell(new Phrase("Service/Product", headerFont));
        table.addCell(new Phrase("Quantity", headerFont));
        table.addCell(new Phrase("Unit Price", headerFont));
        table.addCell(new Phrase("Total", headerFont));

        // Items
        for (OrderItem item : order.getOrderItems()) {
            table.addCell(item.getProductName());
            table.addCell(String.valueOf(item.getQuantity()));
            table.addCell(item.getUnitPrice() + " VND");
            table.addCell(item.getAmount() + " VND");
        }

        return new PdfTable(table);
    }

    private static class PdfTable {
        private final com.itextpdf.text.pdf.PdfPTable table;

        public PdfTable(com.itextpdf.text.pdf.PdfPTable table) {
            this.table = table;
        }

        public com.itextpdf.text.pdf.PdfPTable getTable() {
            return table;
        }
    }
}

