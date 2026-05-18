package com.tappy.pos.service.invoice;

import com.tappy.pos.model.dto.invoice.InvoiceBuyerRequest;
import com.tappy.pos.model.dto.invoice.InvoiceItemRequest;
import com.tappy.pos.model.dto.invoice.InvoiceRequest;
import com.tappy.pos.model.dto.invoice.SInvoiceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SInvoiceMapper Unit Tests")
class SInvoiceMapperTest {

    private SInvoiceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SInvoiceMapper();
    }

    // ── fromGeneralInvoiceInfo ─────────────────────────────────────────────────

    @Test
    @DisplayName("fromGeneralInvoiceInfo returns null for null request")
    void fromGeneralInvoiceInfo_nullInput() {
        assertThat(mapper.fromGeneralInvoiceInfo(null)).isNull();
    }

    @Test
    @DisplayName("fromGeneralInvoiceInfo maps all fields")
    void fromGeneralInvoiceInfo_mapsFields() {
        InvoiceRequest request = InvoiceRequest.builder()
                .invoiceIssuedDate(LocalDateTime.of(2024, 1, 15, 10, 0, 0))
                .invoiceType("01GTKT")
                .currencyCode("VND")
                .invoiceSeries("C24T")
                .transactionUuid("uuid-1234")
                .build();

        SInvoiceRequest.GeneralInvoiceInfo info = mapper.fromGeneralInvoiceInfo(request);

        assertThat(info).isNotNull();
        assertThat(info.getInvoiceType()).isEqualTo("01GTKT");
        assertThat(info.getCurrencyCode()).isEqualTo("VND");
        assertThat(info.getInvoiceSeries()).isEqualTo("C24T");
        assertThat(info.getTransactionUuid()).isEqualTo("uuid-1234");
        assertThat(info.getInvoiceIssuedDate()).isNotNull();
    }

    @Test
    @DisplayName("fromGeneralInvoiceInfo handles null issuedDate")
    void fromGeneralInvoiceInfo_nullIssuedDate() {
        InvoiceRequest request = InvoiceRequest.builder()
                .invoiceIssuedDate(null)
                .invoiceType("01GTKT")
                .build();

        SInvoiceRequest.GeneralInvoiceInfo info = mapper.fromGeneralInvoiceInfo(request);

        assertThat(info).isNotNull();
        assertThat(info.getInvoiceIssuedDate()).isNull();
    }

    // ── fromInvoiceItemRequest ─────────────────────────────────────────────────

    @Test
    @DisplayName("fromInvoiceItemRequest returns null for null input")
    void fromInvoiceItemRequest_nullInput() {
        assertThat(mapper.fromInvoiceItemRequest(null)).isNull();
    }

    @Test
    @DisplayName("fromInvoiceItemRequest maps all item fields")
    void fromInvoiceItemRequest_mapsFields() {
        InvoiceItemRequest itemRequest = InvoiceItemRequest.builder()
                .lineNumber(1L)
                .itemName("Nhẫn vàng 18K")
                .itemCode("NV18K")
                .unit("Cái")
                .unitPrice(new BigDecimal("2000000"))
                .quantity(new BigDecimal("1"))
                .itemTotalAmountWithTax(new BigDecimal("2200000"))
                .build();

        SInvoiceRequest.ItemInfo item = mapper.fromInvoiceItemRequest(itemRequest);

        assertThat(item).isNotNull();
        assertThat(item.getLineNumber()).isEqualTo(1L);
        assertThat(item.getItemName()).isEqualTo("Nhẫn vàng 18K");
        assertThat(item.getItemCode()).isEqualTo("NV18K");
        assertThat(item.getUnitName()).isEqualTo("Cái");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("2000000");
        assertThat(item.getQuantity()).isEqualByComparingTo("1");
        assertThat(item.getItemTotalAmountWithTax()).isEqualByComparingTo("2200000");
    }

    // ── fromInvoiceBuyerRequest ────────────────────────────────────────────────

    @Test
    @DisplayName("fromInvoiceBuyerRequest returns null for null input")
    void fromInvoiceBuyerRequest_nullInput() {
        assertThat(mapper.fromInvoiceBuyerRequest(null)).isNull();
    }

    @Test
    @DisplayName("fromInvoiceBuyerRequest maps all buyer fields")
    void fromInvoiceBuyerRequest_mapsFields() {
        InvoiceBuyerRequest buyer = InvoiceBuyerRequest.builder()
                .customerId(10L)
                .buyerName("Công ty XYZ")
                .buyerLegalName("CÔNG TY TNHH XYZ")
                .buyerTaxCode("0123456789")
                .buyerAddressLine("123 Đường ABC, Quận 1, TP.HCM")
                .buyerPhoneNumber("0901234567")
                .buyerFaxNumber("0281234567")
                .buyerEmail("xyz@company.vn")
                .buyerBankName("Vietcombank")
                .buyerBankAccount("1234567890")
                .buyerIdNo("012345678901")
                .build();

        SInvoiceRequest.BuyerRequest mapped = mapper.fromInvoiceBuyerRequest(buyer);

        assertThat(mapped).isNotNull();
        assertThat(mapped.getCustomerId()).isEqualTo(10L);
        assertThat(mapped.getBuyerName()).isEqualTo("Công ty XYZ");
        assertThat(mapped.getBuyerLegalName()).isEqualTo("CÔNG TY TNHH XYZ");
        assertThat(mapped.getBuyerTaxCode()).isEqualTo("0123456789");
        assertThat(mapped.getBuyerAddressLine()).isEqualTo("123 Đường ABC, Quận 1, TP.HCM");
        assertThat(mapped.getBuyerPhoneNumber()).isEqualTo("0901234567");
        assertThat(mapped.getBuyerFaxNumber()).isEqualTo("0281234567");
        assertThat(mapped.getBuyerEmail()).isEqualTo("xyz@company.vn");
        assertThat(mapped.getBuyerBankName()).isEqualTo("Vietcombank");
        assertThat(mapped.getBuyerBankAccount()).isEqualTo("1234567890");
        assertThat(mapped.getBuyerIdNo()).isEqualTo("012345678901");
    }

    @Test
    @DisplayName("fromInvoiceBuyerRequest handles minimal buyer (all nulls)")
    void fromInvoiceBuyerRequest_minimalBuyer() {
        InvoiceBuyerRequest buyer = InvoiceBuyerRequest.builder().build();

        SInvoiceRequest.BuyerRequest mapped = mapper.fromInvoiceBuyerRequest(buyer);

        assertThat(mapped).isNotNull();
        assertThat(mapped.getBuyerName()).isNull();
        assertThat(mapped.getBuyerTaxCode()).isNull();
    }
}
