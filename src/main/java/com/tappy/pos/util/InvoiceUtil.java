package com.tappy.pos.util;

public class InvoiceUtil {
    private InvoiceUtil() {}

    public static final String S_INVOICE_EP_CREATE_INVOICE = "/InvoiceWS/createInvoice/";
    public static final String S_INVOICE_EP_DOWNLOAD_INVOICE = "/InvoiceUtilsWS/getInvoiceRepresentationFile";
    public static final String S_INVOICE_EP_SENDEMAIL_INVOICE = "/InvoiceUtilsWS/sendHtmlMailProcess";
    public static final String S_INVOICE_EP_SEARCH_BYTRANSACTION_INVOICE = "/InvoiceWS/searchInvoiceByTransactionUuid";
}
