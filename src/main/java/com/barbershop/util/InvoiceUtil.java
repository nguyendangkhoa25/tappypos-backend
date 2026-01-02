package com.barbershop.util;

import com.barbershop.model.dto.SInvoiceRequest;
import com.barbershop.model.entity.Invoice;
import com.barbershop.model.entity.InvoiceBuyer;
import com.barbershop.model.entity.InvoiceItem;

import java.math.BigDecimal;

/**
 * Utility class for mapping Invoice entities to S-Invoice request objects
 */
public class InvoiceUtil {

    // S-Invoice API endpoints
    public static final String S_INVOICE_EP_CREATE_INVOICE = "/InvoiceAPI/InvoiceWS/createInvoice/";
    public static final String S_INVOICE_EP_SEARCH_BY_TRANSACTION_INVOICE = "/InvoiceAPI/InvoiceWS/searchInvoiceByTransactionUuid";
    public static final String S_INVOICE_EP_DOWNLOAD_INVOICE = "/InvoiceAPI/InvoiceWS/getInvoiceRepresentationFile";
    public static final String S_INVOICE_EP_SEND_EMAIL_INVOICE = "/InvoiceAPI/InvoiceWS/sendMailInvoice";

    /**
     * Map InvoiceItem to S-Invoice ItemInfo
     */
    public static SInvoiceRequest.ItemInfo fromInvoiceItemRequest(InvoiceItem item) {
        return SInvoiceRequest.ItemInfo.builder()
                .lineNumber(Long.valueOf(item.getLineNumber()))
                .itemCode(String.valueOf(item.getOrderItemId()))
                .itemName(item.getServiceName())
                .unitName(item.getUnit())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .itemTotalAmountWithoutTax(item.getTotalAmountWithoutTax())
                .taxPercentage(item.getTaxPercentage())
                .taxAmount(item.getTaxAmount())
                .itemTotalAmountWithTax(item.getTotalAmountWithTax())
                .discount(item.getDiscount() != null ? item.getDiscount() : BigDecimal.ZERO)
                .itemTotalAmountAfterDiscount(item.getTotalAmountWithTax())
                .build();
    }

    /**
     * Map Invoice to S-Invoice GeneralInvoiceInfo
     */
    public static SInvoiceRequest.GeneralInvoiceInfo fromGeneralInvoiceInfo(Invoice invoice) {
        return SInvoiceRequest.GeneralInvoiceInfo.builder()
                .invoiceType(invoice.getInvoiceType() != null ? invoice.getInvoiceType().code : "01")
                .invoiceSeries(invoice.getInvoiceSeries())
                .currencyCode(invoice.getCurrencyCode() != null ? invoice.getCurrencyCode() : "VND")
                .adjustmentType(0)
                .paymentStatus(true)
                .cusGetInvoiceRight(true)
                .build();
    }

    /**
     * Map InvoiceBuyer to S-Invoice BuyerRequest
     */
    public static SInvoiceRequest.BuyerRequest fromInvoiceBuyerRequest(InvoiceBuyer buyer) {
        if (buyer == null) {
            return SInvoiceRequest.BuyerRequest.builder()
                    .buyerName("Khách lẻ")
                    .buyerNotGetInvoice(1)
                    .build();
        }

        return SInvoiceRequest.BuyerRequest.builder()
                .buyerName(buyer.getBuyerName())
                .buyerLegalName(buyer.getBuyerLegalName())
                .buyerTaxCode(buyer.getBuyerTaxCode())
                .buyerAddressLine(buyer.getBuyerAddressLine())
                .buyerPhoneNumber(buyer.getBuyerPhoneNumber())
                .buyerEmail(buyer.getBuyerEmail())
                .buyerIdNo(buyer.getBuyerIdNumber())
                .buyerIdType(buyer.getBuyerIdNumber() != null && !buyer.getBuyerIdNumber().isEmpty() ? "1" : null)
                .buyerNotGetInvoice(buyer.isVisitingGuest() ? 1 : 0)
                .build();
    }

    private InvoiceUtil() {
        // Utility class - prevent instantiation
    }
}

