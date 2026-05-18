package com.tappy.pos.service.invoice;

import com.tappy.pos.model.dto.invoice.InvoiceBuyerRequest;
import com.tappy.pos.model.dto.invoice.InvoiceItemRequest;
import com.tappy.pos.model.dto.invoice.InvoiceRequest;
import com.tappy.pos.model.dto.invoice.SInvoiceRequest;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class SInvoiceMapper {

    public SInvoiceRequest.GeneralInvoiceInfo fromGeneralInvoiceInfo(InvoiceRequest request) {
        if (request == null) return null;
        SInvoiceRequest.GeneralInvoiceInfo info = new SInvoiceRequest.GeneralInvoiceInfo();
        info.setInvoiceIssuedDate(fromLocalDateTime(request.getInvoiceIssuedDate()));
        info.setInvoiceType(request.getInvoiceType());
        info.setCurrencyCode(request.getCurrencyCode());
        info.setInvoiceSeries(request.getInvoiceSeries());
        info.setTransactionUuid(request.getTransactionUuid());
        return info;
    }

    public SInvoiceRequest.ItemInfo fromInvoiceItemRequest(InvoiceItemRequest itemRequest) {
        if (itemRequest == null) return null;
        SInvoiceRequest.ItemInfo item = new SInvoiceRequest.ItemInfo();
        item.setItemId(itemRequest.getItemId());
        item.setLineNumber(itemRequest.getLineNumber());
        item.setItemName(itemRequest.getItemName());
        item.setItemCode(itemRequest.getItemCode());
        item.setUnitPrice(itemRequest.getUnitPrice());
        item.setQuantity(itemRequest.getQuantity());
        item.setItemTotalAmountWithTax(itemRequest.getItemTotalAmountWithTax());
        item.setUnitName(itemRequest.getUnit());
        return item;
    }

    public SInvoiceRequest.BuyerRequest fromInvoiceBuyerRequest(InvoiceBuyerRequest buyer) {
        if (buyer == null) return null;
        SInvoiceRequest.BuyerRequest b = new SInvoiceRequest.BuyerRequest();
        b.setBuyerId(buyer.getBuyerId());
        b.setInvoiceId(buyer.getInvoiceId());
        b.setCustomerId(buyer.getCustomerId());
        b.setBuyerName(buyer.getBuyerName());
        b.setBuyerLegalName(buyer.getBuyerLegalName());
        b.setBuyerTaxCode(buyer.getBuyerTaxCode());
        b.setBuyerAddressLine(buyer.getBuyerAddressLine());
        b.setBuyerPhoneNumber(buyer.getBuyerPhoneNumber());
        b.setBuyerFaxNumber(buyer.getBuyerFaxNumber());
        b.setBuyerEmail(buyer.getBuyerEmail());
        b.setBuyerBankName(buyer.getBuyerBankName());
        b.setBuyerBankAccount(buyer.getBuyerBankAccount());
        b.setBuyerIdNo(buyer.getBuyerIdNo());
        return b;
    }

    private Long fromLocalDateTime(java.time.LocalDateTime localDateTime) {
        return localDateTime == null ? null
                : ZonedDateTime.of(localDateTime, ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
