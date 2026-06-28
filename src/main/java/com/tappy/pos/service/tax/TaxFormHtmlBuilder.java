package com.tappy.pos.service.tax;

import com.tappy.pos.model.entity.finance.TaxDeclaration;
import com.tappy.pos.model.entity.finance.TaxDeclarationLine;
import com.tappy.pos.model.entity.tenant.ShopInfo;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Dựng bản in HTML đơn giản của tờ khai (mẫu 01/CNKD) để chủ shop "In → Lưu PDF".
 * Đây là bản rút gọn cho MVP — bản render đúng schema XML cơ quan thuế là bước nâng cấp sau.
 */
final class TaxFormHtmlBuilder {

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    private TaxFormHtmlBuilder() {}

    static String build(TaxDeclaration d, ShopInfo shop) {
        String shopName = shop != null && shop.getShopName() != null ? shop.getShopName() : "";
        String mst = shop != null && shop.getSupplierTaxCode() != null ? shop.getSupplierTaxCode() : "";
        String address = shop != null && shop.getAddress() != null ? shop.getAddress() : "";
        String period = label(d);

        StringBuilder rows = new StringBuilder();
        for (TaxDeclarationLine l : d.getLines()) {
            rows.append("<tr>")
                .append(td(l.getIndustryName(), false))
                .append(td(money(l.getRevenue()), true))
                .append(td(pct(l.getVatRate()), true))
                .append(td(money(l.getVatAmount()), true))
                .append(td(pct(l.getPitRate()), true))
                .append(td(money(l.getPitAmount()), true))
                .append("</tr>");
        }

        return "<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\">"
                + "<title>Tờ khai thuế " + esc(period) + "</title>"
                + "<style>body{font-family:Arial,sans-serif;padding:24px;color:#111}"
                + "h1{font-size:18px;text-align:center;margin-bottom:4px}"
                + ".sub{text-align:center;font-size:13px;margin-bottom:16px}"
                + "table{width:100%;border-collapse:collapse;margin-top:12px}"
                + "th,td{border:1px solid #999;padding:6px 8px;font-size:13px}"
                + "th{background:#f0f0f0}.r{text-align:right}.tot{font-weight:bold}"
                + ".info{font-size:13px;line-height:1.6}.note{margin-top:18px;font-size:12px;color:#666}"
                + "</style></head><body>"
                + "<h1>TỜ KHAI THUẾ ĐỐI VỚI HỘ, CÁ NHÂN KINH DOANH</h1>"
                + "<div class=\"sub\">(Mẫu 01/CNKD) — Kỳ tính thuế: " + esc(period) + "</div>"
                + "<div class=\"info\"><b>Hộ kinh doanh:</b> " + esc(shopName) + "<br>"
                + "<b>Mã số thuế:</b> " + esc(mst) + "<br>"
                + "<b>Địa chỉ:</b> " + esc(address) + "</div>"
                + "<table><thead><tr>"
                + "<th>Nhóm ngành</th><th>Doanh thu</th><th>Tỷ lệ GTGT</th><th>Thuế GTGT</th>"
                + "<th>Tỷ lệ TNCN</th><th>Thuế TNCN</th></tr></thead><tbody>"
                + rows
                + "<tr class=\"tot\"><td>Tổng cộng</td>"
                + td(money(d.getDeclaredRevenue()), true)
                + "<td></td>" + td(money(d.getTotalVat()), true)
                + "<td></td>" + td(money(d.getTotalPit()), true)
                + "</tr></tbody></table>"
                + "<p class=\"r tot\">Tổng tiền thuế phải nộp: " + money(d.getTotalTax()) + " ₫</p>"
                + "<p class=\"note\">Số liệu mang tính tham khảo, do hộ kinh doanh tự kê khai và chịu trách nhiệm. "
                + "Vui lòng đối chiếu quy định hiện hành trước khi nộp lên cơ quan thuế.</p>"
                + "</body></html>";
    }

    private static String label(TaxDeclaration d) {
        return switch (d.getPeriodType()) {
            case QUARTER -> "Quý " + d.getPeriodNumber() + "/" + d.getPeriodYear();
            case MONTH -> "Tháng " + d.getPeriodNumber() + "/" + d.getPeriodYear();
            case YEAR -> "Năm " + d.getPeriodYear();
        };
    }

    private static String td(String content, boolean right) {
        return "<td class=\"" + (right ? "r" : "") + "\">" + esc(content) + "</td>";
    }

    private static String money(BigDecimal v) {
        return v == null ? "0" : VND.format(v.setScale(0, java.math.RoundingMode.HALF_UP));
    }

    private static String pct(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString() + "%";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
