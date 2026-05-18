package com.tappy.pos.util;

import com.tappy.pos.model.dto.tenant.ReceiptPreviewRequest;
import com.tappy.pos.model.dto.tenant.ReceiptTemplateConfig;
import com.tappy.pos.model.entity.finance.BankAccount;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.entity.order.OrderItem;
import com.tappy.pos.model.entity.tenant.ShopInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generates an 80mm thermal receipt as a self-contained HTML string.
 * The returned HTML auto-prints on load and closes the window after printing.
 */
public class ReceiptHtmlBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ReceiptHtmlBuilder() {}

    // ── public entry points ────────────────────────────────────────────────

    /** Build receipt HTML from a completed Order entity + shop info using default config. */
    public static String build(Order order, ShopInfo shopInfo) {
        return build(order, shopInfo, ReceiptTemplateConfig.defaults(), null);
    }

    /** Build receipt HTML from a completed Order entity + shop info with custom config. */
    public static String build(Order order, ShopInfo shopInfo, ReceiptTemplateConfig cfg) {
        return build(order, shopInfo, cfg, null);
    }

    /** Build receipt HTML from a completed Order entity + shop info with custom config and optional VietQR bank account. */
    public static String build(Order order, ShopInfo shopInfo, ReceiptTemplateConfig cfg, BankAccount bankAccount) {
        List<ReceiptItem> items = order.getOrderItems().stream()
                .map(oi -> new ReceiptItem(
                        oi.getProductName(),
                        null,
                        oi.getQuantity(),
                        oi.getUnitPrice(),
                        oi.getAmount(),
                        oi.getTaxPercentage() != null ? oi.getTaxPercentage() : BigDecimal.ZERO
                ))
                .toList();

        LocalDateTime displayDate = order.getCompletedAt() != null
                ? order.getCompletedAt()
                : order.getCreatedAt();

        String customerName = order.getCustomer() != null ? order.getCustomer().getName() : null;
        String vietQrBlock = (cfg.isShowVietQr() && bankAccount != null)
                ? buildVietQrBlock(bankAccount, order.getTotalAmount(), order.getOrderNumber())
                : null;

        return buildHtml(
                shopInfo != null ? shopInfo.getShopName() : null,
                cfg.isShowAddress() && shopInfo != null ? shopInfo.getAddress() : null,
                cfg.isShowTaxId() && shopInfo != null ? shopInfo.getSupplierTaxCode() : null,
                cfg.isShowOrderNumber() ? order.getOrderNumber() : null,
                cfg.isShowDateTime() ? displayDate : null,
                cfg.isShowCustomer() ? customerName : null,
                cfg.isShowTable() ? order.getTableLabel() : null,
                items,
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getPaymentMethod(),
                cfg.isShowCashDetails() ? order.getAmountPaid() : null,
                cfg.isShowCashDetails() ? order.getChangeAmount() : null,
                cfg.isShowTaxBreakdown(),
                cfg.getHeaderText(),
                cfg.getFooterText(),
                cfg.getPaperWidth(),
                cfg.isAutoClose(),
                vietQrBlock
        );
    }

    /** Build receipt HTML from a pre-checkout preview request + shop info. */
    public static String buildPreview(ReceiptPreviewRequest req, ShopInfo shopInfo) {
        return buildPreview(req, shopInfo, ReceiptTemplateConfig.defaults(), null);
    }

    /** Build receipt HTML from a pre-checkout preview request + shop info with custom config. */
    public static String buildPreview(ReceiptPreviewRequest req, ShopInfo shopInfo, ReceiptTemplateConfig cfg) {
        return buildPreview(req, shopInfo, cfg, null);
    }

    /** Build receipt HTML from a pre-checkout preview request + shop info with optional VietQR bank account. */
    public static String buildPreview(ReceiptPreviewRequest req, ShopInfo shopInfo, ReceiptTemplateConfig cfg, BankAccount bankAccount) {
        List<ReceiptItem> items = req.getItems().stream()
                .map(i -> new ReceiptItem(
                        i.getProductName(),
                        i.getSku(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getLineTotal(),
                        i.getTaxRate() != null ? i.getTaxRate() : BigDecimal.TEN
                ))
                .toList();

        String vietQrBlock = (cfg.isShowVietQr() && bankAccount != null)
                ? buildVietQrBlock(bankAccount, req.getTotal(), "Thanh toan")
                : null;

        return buildHtml(
                shopInfo != null ? shopInfo.getShopName() : null,
                cfg.isShowAddress() && shopInfo != null ? shopInfo.getAddress() : null,
                cfg.isShowTaxId() && shopInfo != null ? shopInfo.getSupplierTaxCode() : null,
                cfg.isShowOrderNumber() ? "(chưa hoàn tất)" : null,
                cfg.isShowDateTime() ? LocalDateTime.now() : null,
                cfg.isShowCustomer() ? req.getCustomerName() : null,
                cfg.isShowTable() ? req.getTableLabel() : null,
                items,
                req.getTotalDiscount(),
                req.getTotal(),
                req.getPaymentMethod(),
                cfg.isShowCashDetails() ? req.getAmountPaid() : null,
                cfg.isShowCashDetails() ? req.getChangeAmount() : null,
                cfg.isShowTaxBreakdown(),
                cfg.getHeaderText(),
                cfg.getFooterText(),
                cfg.getPaperWidth(),
                cfg.isAutoClose(),
                vietQrBlock
        );
    }

    // ── core HTML builder ──────────────────────────────────────────────────

    private static String buildHtml(
            String shopName,
            String shopAddress,
            String shopTaxId,
            String orderNumber,
            LocalDateTime date,
            String customerName,
            String tableLabel,
            List<ReceiptItem> items,
            BigDecimal totalDiscount,
            BigDecimal total,
            String paymentMethod,
            BigDecimal amountPaid,
            BigDecimal changeAmount,
            boolean showTaxBreakdown,
            String headerText,
            String footerText,
            String paperWidth,
            boolean autoClose,
            String vietQrBlock
    ) {
        String width = (paperWidth != null && !paperWidth.isBlank()) ? paperWidth : "80mm";

        StringBuilder rows = new StringBuilder();
        Map<BigDecimal, BigDecimal> taxByRate = new LinkedHashMap<>();

        for (ReceiptItem item : items) {
            BigDecimal rate = item.taxRate().setScale(0, RoundingMode.HALF_UP);
            BigDecimal taxAmt = item.lineTotal()
                    .multiply(rate)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

            taxByRate.merge(rate, taxAmt, BigDecimal::add);

            String skuHtml = (item.sku() != null && !item.sku().isBlank())
                    ? "<br/><small style=\"color:#666\">" + escHtml(item.sku()) + "</small>"
                    : "";

            if (showTaxBreakdown) {
                rows.append("<tr>")
                        .append("<td>").append(escHtml(item.productName())).append(skuHtml).append("</td>")
                        .append("<td style=\"text-align:center\">").append(item.quantity()).append("</td>")
                        .append("<td style=\"text-align:right\">").append(fmt(item.unitPrice())).append("</td>")
                        .append("<td style=\"text-align:center\">").append(rate).append("%</td>")
                        .append("<td style=\"text-align:right\">").append(fmt(taxAmt)).append("</td>")
                        .append("<td style=\"text-align:right\">").append(fmt(item.lineTotal())).append("</td>")
                        .append("</tr>\n");
            } else {
                rows.append("<tr>")
                        .append("<td>").append(escHtml(item.productName())).append(skuHtml).append("</td>")
                        .append("<td style=\"text-align:center\">").append(item.quantity()).append("</td>")
                        .append("<td style=\"text-align:right\">").append(fmt(item.unitPrice())).append("</td>")
                        .append("<td style=\"text-align:right\">").append(fmt(item.lineTotal())).append("</td>")
                        .append("</tr>\n");
            }
        }

        StringBuilder discountRow = new StringBuilder();
        if (totalDiscount != null && totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
            int colspan = showTaxBreakdown ? 5 : 3;
            discountRow.append("<tr>")
                    .append("<td colspan=\"").append(colspan).append("\">Giảm giá</td>")
                    .append("<td style=\"text-align:right;color:green\">-").append(fmt(totalDiscount)).append("</td>")
                    .append("</tr>\n");
        }

        StringBuilder taxRows = new StringBuilder();
        if (showTaxBreakdown) {
            List<Map.Entry<BigDecimal, BigDecimal>> sortedTax = new ArrayList<>(taxByRate.entrySet());
            sortedTax.sort(Map.Entry.comparingByKey());
            for (Map.Entry<BigDecimal, BigDecimal> entry : sortedTax) {
                taxRows.append("<tr>")
                        .append("<td colspan=\"5\">Thuế ").append(entry.getKey()).append("%</td>")
                        .append("<td style=\"text-align:right\">").append(fmt(entry.getValue())).append("</td>")
                        .append("</tr>\n");
            }
        }

        StringBuilder cashRows = new StringBuilder();
        if ("CASH".equalsIgnoreCase(paymentMethod) && amountPaid != null) {
            int colspan = showTaxBreakdown ? 5 : 3;
            BigDecimal change = changeAmount != null ? changeAmount : BigDecimal.ZERO;
            cashRows.append("<tr>")
                    .append("<td colspan=\"").append(colspan).append("\">Tiền khách trả</td>")
                    .append("<td style=\"text-align:right\">").append(fmt(amountPaid)).append("</td>")
                    .append("</tr>\n")
                    .append("<tr>")
                    .append("<td colspan=\"").append(colspan).append("\">Tiền thừa</td>")
                    .append("<td style=\"text-align:right\">").append(fmt(change)).append("</td>")
                    .append("</tr>\n");
        }

        String displayDate = date != null ? date.format(DATE_FMT) : "";
        int totalColspan = showTaxBreakdown ? 5 : 3;

        // Build table header based on showTaxBreakdown
        String tableHeader = showTaxBreakdown
                ? "        <th style=\"text-align:left\">Sản phẩm</th>\n" +
                  "        <th style=\"text-align:center\">SL</th>\n" +
                  "        <th style=\"text-align:right\">Đơn giá</th>\n" +
                  "        <th style=\"text-align:center\">Thuế</th>\n" +
                  "        <th style=\"text-align:right\">T.Thuế</th>\n" +
                  "        <th style=\"text-align:right\">T.Tiền</th>\n"
                : "        <th style=\"text-align:left\">Sản phẩm</th>\n" +
                  "        <th style=\"text-align:center\">SL</th>\n" +
                  "        <th style=\"text-align:right\">Đơn giá</th>\n" +
                  "        <th style=\"text-align:right\">T.Tiền</th>\n";

        // Build footer lines from footerText (supports \n)
        StringBuilder footerLines = new StringBuilder();
        if (footerText != null && !footerText.isBlank()) {
            for (String line : footerText.split("\\\\n|\n")) {
                if (!line.isBlank()) {
                    footerLines.append("  <p class=\"footer\">").append(escHtml(line)).append("</p>\n");
                }
            }
        }

        String autoCloseScript = autoClose
                ? "  <script>window.onload = () => { window.print(); window.onafterprint = () => window.close(); }</script>\n"
                : "";

        return "<!DOCTYPE html>\n" +
                "<html lang=\"vi\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\"/>\n" +
                "  <title>Hóa Đơn " + escHtml(orderNumber != null ? orderNumber : "") + "</title>\n" +
                "  <style>\n" +
                "    * { box-sizing: border-box; margin: 0; padding: 0; }\n" +
                "    body { font-family: 'Courier New', monospace; font-size: 11px; width: " + width + "; padding: 8px; }\n" +
                "    h2 { font-size: 14px; text-align: center; margin-bottom: 2px; }\n" +
                "    .center { text-align: center; }\n" +
                "    .meta { font-size: 10px; color: #444; margin-bottom: 6px; }\n" +
                "    .header-text { font-size: 10px; text-align: center; margin-bottom: 4px; }\n" +
                "    table { width: 100%; border-collapse: collapse; margin: 6px 0; }\n" +
                "    th { border-bottom: 1px dashed #000; padding: 2px 0; font-size: 10px; }\n" +
                "    td { padding: 2px 0; vertical-align: top; }\n" +
                "    .total-row td { font-weight: bold; font-size: 12px; border-top: 1px dashed #000; padding-top: 3px; }\n" +
                "    .footer { text-align: center; margin-top: 8px; font-size: 10px; }\n" +
                "    @media print {\n" +
                "      @page { margin: 0; size: " + width + " auto; }\n" +
                "      body { padding: 0; }\n" +
                "    }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <h2>" + escHtml(shopName != null ? shopName : "CỬA HÀNG") + "</h2>\n" +
                (headerText != null && !headerText.isBlank() ? "  <p class=\"header-text\">" + escHtml(headerText) + "</p>\n" : "") +
                (shopAddress != null ? "  <p class=\"center meta\">" + escHtml(shopAddress) + "</p>\n" : "") +
                (shopTaxId != null ? "  <p class=\"center meta\">MST: " + escHtml(shopTaxId) + "</p>\n" : "") +
                "  <p class=\"center meta\">HÓA ĐƠN BÁN HÀNG</p>\n" +
                (orderNumber != null ? "  <p class=\"center meta\">Số: " + escHtml(orderNumber) + "</p>\n" : "") +
                (!displayDate.isBlank() ? "  <p class=\"center meta\">" + displayDate + "</p>\n" : "") +
                (customerName != null ? "  <p class=\"center meta\">Khách: " + escHtml(customerName) + "</p>\n" : "") +
                (tableLabel != null && !tableLabel.isBlank() ? "  <p class=\"center meta\">Bàn: " + escHtml(tableLabel) + "</p>\n" : "") +
                "  <table>\n" +
                "    <thead>\n" +
                "      <tr>\n" +
                tableHeader +
                "      </tr>\n" +
                "    </thead>\n" +
                "    <tbody>\n" +
                rows +
                discountRow +
                taxRows +
                "      <tr class=\"total-row\">\n" +
                "        <td colspan=\"" + totalColspan + "\">TỔNG CỘNG</td>\n" +
                "        <td style=\"text-align:right\">" + fmt(total) + "</td>\n" +
                "      </tr>\n" +
                cashRows +
                "    </tbody>\n" +
                "  </table>\n" +
                (vietQrBlock != null ? vietQrBlock : "") +
                footerLines +
                autoCloseScript +
                "</body>\n" +
                "</html>";
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private static String fmt(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(amount) + " ₫";
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String buildVietQrBlock(BankAccount account, BigDecimal amount, String addInfo) {
        String url = buildVietQrImageUrl(account, amount, addInfo);
        String shortName = account.getBankShortName() != null && !account.getBankShortName().isBlank()
                ? account.getBankShortName() : account.getBankName();
        return "  <div style=\"text-align:center;border-top:1px dashed #000;margin-top:8px;padding-top:6px\">\n" +
               "    <p style=\"font-size:9px;color:#555;margin-bottom:3px\">QUÉT MÃ ĐỂ THANH TOÁN</p>\n" +
               "    <img src=\"" + escHtml(url) + "\" style=\"width:130px;height:auto;display:block;margin:2px auto\"" +
               "         onerror=\"this.style.display='none'\"/>\n" +
               "    <p style=\"font-size:9px;margin-top:3px\">" + escHtml(shortName) + ": " + escHtml(account.getAccountNumber()) + "</p>\n" +
               "    <p style=\"font-size:9px\">" + escHtml(account.getAccountName()) + "</p>\n" +
               "  </div>\n";
    }

    private static String buildVietQrImageUrl(BankAccount account, BigDecimal amount, String addInfo) {
        String base = "https://img.vietqr.io/image/" + account.getBankBin() + "-" + account.getAccountNumber() + "-compact2.png";
        StringBuilder params = new StringBuilder();
        if (account.getAccountName() != null && !account.getAccountName().isBlank()) {
            params.append("accountName=").append(URLEncoder.encode(account.getAccountName(), StandardCharsets.UTF_8));
        }
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            if (!params.isEmpty()) params.append("&");
            params.append("amount=").append(amount.setScale(0, RoundingMode.HALF_UP).toPlainString());
        }
        if (addInfo != null && !addInfo.isBlank()) {
            if (!params.isEmpty()) params.append("&");
            params.append("addInfo=").append(URLEncoder.encode(addInfo, StandardCharsets.UTF_8));
        }
        return params.isEmpty() ? base : base + "?" + params;
    }

    private record ReceiptItem(
            String productName,
            String sku,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            BigDecimal taxRate
    ) {}
}
