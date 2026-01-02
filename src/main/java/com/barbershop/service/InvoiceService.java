package com.barbershop.service;

import com.barbershop.model.dto.invoice.*;
import com.barbershop.model.entity.Invoice;
import com.barbershop.model.entity.InvoiceItem;
import com.barbershop.model.entity.InvoiceBuyer;
import com.barbershop.model.entity.Order;
import com.barbershop.model.entity.ShopInfo;
import com.barbershop.repository.InvoiceRepository;
import com.barbershop.repository.InvoiceItemRepository;
import com.barbershop.repository.InvoiceBuyerRepository;
import com.barbershop.repository.OrderRepository;
import com.barbershop.repository.ShopInfoRepository;
import com.barbershop.service.invoice.ExternalInvoiceService;
import com.barbershop.service.invoice.InvoiceServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceBuyerRepository invoiceBuyerRepository;
    private final OrderRepository orderRepository;
    private final InvoiceServiceFactory invoiceServiceFactory;
    private final ShopInfoRepository shopInfoRepository;

    public InvoiceDTO createInvoice(CreateInvoiceRequest request) {
        Order order = orderRepository.findByIdActive(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Check if invoice already exists for this order
        if (order.getInvoiceId() != null) {
            throw new RuntimeException("Invoice already exists for this order (Invoice ID: " + order.getInvoiceId() + ")");
        }

        String invoiceNumber = generateInvoiceNumber();
        String invoiceSeries = request.getInvoiceSeries();
        BigDecimal taxPercentage = BigDecimal.ZERO;
        String currencyCode = request.getCurrencyCode() != null ? request.getCurrencyCode() : "VND";

        // Calculate totals from order
        BigDecimal totalAmountWithoutTax = order.getTotalAmount();
        BigDecimal taxAmount = order.getTaxAmount() != null ? order.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = totalAmountWithoutTax;

        // Create buyer info from customer fields or buyerInfo object
        InvoiceBuyer buyer = InvoiceBuyer.builder()
                .customerId(order.getCustomer().getId())
                .buyerName(request.getBuyerInfo().getBuyerName())
                .buyerLegalName(request.getBuyerInfo().getBuyerLegalName())
                .buyerTaxCode(request.getBuyerInfo().getBuyerTaxCode())
                .buyerAddressLine(request.getBuyerInfo().getBuyerAddressLine())
                .buyerPhoneNumber(request.getBuyerInfo().getBuyerPhoneNumber())
                .buyerEmail(request.getBuyerInfo().getBuyerEmail())
                .buyerBankName(request.getBuyerInfo().getBuyerBankName())
                .buyerBankAccount(request.getBuyerInfo().getBuyerBankAccount())
                .buyerIdNumber(request.getBuyerInfo().getBuyerIdNumber())
                .visitingGuest(request.getBuyerInfo().isVisitingGuest())
                .build();

        // Create invoice
        Invoice invoice = Invoice.builder()
                .order(order)
                .invoiceNumber(invoiceNumber)
                .invoiceSeries(invoiceSeries)
                .issuedDate(LocalDateTime.now())
                .totalAmountWithoutTax(totalAmountWithoutTax)
                .totalAmount(totalAmount)
                .taxAmount(taxAmount)
                .taxPercentage(taxPercentage)
                .status(Invoice.InvoiceStatus.DRAFT)
                .paymentType(request.getPaymentType() != null ? Invoice.PaymentType.valueOf(request.getPaymentType()) : Invoice.PaymentType.CASH)
                .invoiceType(request.getInvoiceType() != null ? Invoice.InvoiceType.valueOf(request.getInvoiceType()) : Invoice.InvoiceType.RETAIL)
                .currencyCode(currencyCode)
                .notes(request.getNotes())
                .buyer(buyer)
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Invoice saved with ID: {}", savedInvoice.getId());

        // Create invoice items from order items
        if (request.getOrderItems() != null && !request.getOrderItems().isEmpty()) {
            List<InvoiceItem> items = request.getOrderItems().stream()
                    .map((orderItem) -> createInvoiceItemFromOrderItem(savedInvoice, orderItem))
                    .collect(Collectors.toList());
            invoiceItemRepository.saveAll(items);
            log.info("Invoice items saved: {} items", items.size());
            savedInvoice.setItems(items);
        } else if (request.getItems() != null && !request.getItems().isEmpty()) {
            // Fallback to items array if orderItems not provided
            List<InvoiceItem> items = request.getItems().stream()
                    .map((itemInput) -> createInvoiceItem(savedInvoice, itemInput))
                    .collect(Collectors.toList());
            invoiceItemRepository.saveAll(items);
            log.info("Invoice items saved: {} items", items.size());
            savedInvoice.setItems(items);
        } else if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            // If no items provided in request, create from order items
            List<InvoiceItem> items = order.getOrderItems().stream()
                    .map((orderItem) -> createInvoiceItemFromEntity(savedInvoice, orderItem))
                    .collect(Collectors.toList());
            invoiceItemRepository.saveAll(items);
            log.info("Invoice items created from order items: {} items", items.size());
            savedInvoice.setItems(items);
        }

        // Update order with invoice ID
        order.setInvoiceId(savedInvoice.getId());
        orderRepository.save(order);
        log.info("Order {} updated with invoice ID: {}", order.getId(), savedInvoice.getId());

        log.info("Invoice created successfully: {}", invoiceNumber);
        return mapToDTO(savedInvoice);
    }

    private InvoiceItem createInvoiceItem(Invoice invoice, CreateInvoiceRequest.InvoiceItemInput itemInput) {
        BigDecimal discount = itemInput.getDiscount() != null ? itemInput.getDiscount() : BigDecimal.ZERO;
        BigDecimal taxPercentage = itemInput.getTaxPercentage() != null ? itemInput.getTaxPercentage() : BigDecimal.ZERO;

        BigDecimal subtotal = itemInput.getUnitPrice().multiply(itemInput.getQuantity());
        BigDecimal afterDiscount = subtotal.subtract(discount);
        BigDecimal taxAmount = afterDiscount.multiply(taxPercentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalWithTax = afterDiscount.add(taxAmount);

        return InvoiceItem.builder()
                .invoice(invoice)
                .lineNumber((invoice.getItems() != null ? invoice.getItems().size() : 0) + 1)
                .orderItemId(itemInput.getOrderItemId())
                .serviceName(itemInput.getServiceName())
                .serviceCode(itemInput.getServiceCode())
                .unit(itemInput.getUnit())
                .unitPrice(itemInput.getUnitPrice())
                .quantity(itemInput.getQuantity())
                .discount(discount)
                .totalAmountWithoutTax(afterDiscount)
                .taxPercentage(taxPercentage)
                .taxAmount(taxAmount)
                .totalAmountWithTax(totalWithTax)
                .build();
    }

    private InvoiceItem createInvoiceItemFromOrderItem(Invoice invoice, CreateInvoiceRequest.OrderItemInput orderItem) {
        int lineNumber = orderItem.getOrdinalNumber() != null ? orderItem.getOrdinalNumber() :
                        (invoice.getItems() != null ? invoice.getItems().size() : 0) + 1;

        BigDecimal unitPrice = orderItem.getPrice() != null ? orderItem.getPrice() : BigDecimal.ZERO;
        BigDecimal quantity = orderItem.getQuantity() != null ? orderItem.getQuantity() : BigDecimal.ONE;
        BigDecimal totalPrice = orderItem.getTotalPrice() != null ? orderItem.getTotalPrice() :
                               unitPrice.multiply(quantity);

        return InvoiceItem.builder()
                .invoice(invoice)
                .lineNumber(lineNumber)
                .orderItemId(orderItem.getId())
                .serviceName(orderItem.getServiceName() != null ? orderItem.getServiceName() : orderItem.getProductName())
                .serviceCode(null)
                .unit("Lần")
                .unitPrice(unitPrice)
                .quantity(quantity)
                .discount(BigDecimal.ZERO)
                .totalAmountWithoutTax(totalPrice)
                .taxPercentage(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmountWithTax(totalPrice)
                .build();
    }

    private InvoiceItem createInvoiceItemFromEntity(Invoice invoice, com.barbershop.model.entity.OrderItem orderItem) {
        BigDecimal unitPrice = orderItem.getUnitPrice() != null ? orderItem.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal quantity = BigDecimal.valueOf(orderItem.getQuantity() != null ? orderItem.getQuantity() : 1);
        BigDecimal totalPrice = orderItem.getAmount() != null ? orderItem.getAmount() :
                               unitPrice.multiply(quantity);

        return InvoiceItem.builder()
                .invoice(invoice)
                .lineNumber(invoice.getItems() != null ? invoice.getItems().size() + 1 : 1)
                .orderItemId(orderItem.getId())
                .serviceName(orderItem.getProductName())
                .serviceCode(null)
                .unit("Lần")
                .unitPrice(unitPrice)
                .quantity(quantity)
                .discount(BigDecimal.ZERO)
                .totalAmountWithoutTax(totalPrice)
                .taxPercentage(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmountWithTax(totalPrice)
                .build();
    }

    public Page<InvoiceDTO> getAllInvoices(Pageable pageable) {
        Page<Invoice> invoices = invoiceRepository.findAllActive(pageable);
        return invoices.map(this::mapToDTO);
    }

    public InvoiceDTO getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
        return mapToDTO(invoice);
    }

    public InvoiceDTO getInvoiceByOrderId(Long orderId) {
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Invoice not found for order: " + orderId));
        return mapToDTO(invoice);
    }

    public InvoiceDTO updateInvoice(Long id, UpdateInvoiceRequest request) {
        Invoice invoice = invoiceRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        if (request.getStatus() != null) {
            invoice.setStatus(Invoice.InvoiceStatus.valueOf(request.getStatus()));
        }
        if (request.getPaymentType() != null) {
            invoice.setPaymentType(Invoice.PaymentType.valueOf(request.getPaymentType()));
        }
        if (request.getNotes() != null) {
            invoice.setNotes(request.getNotes());
        }

        Invoice updated = invoiceRepository.save(invoice);
        return mapToDTO(updated);
    }

    public InvoiceDTO issueInvoice(Long id) {
        Invoice invoice = invoiceRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        invoice.setStatus(Invoice.InvoiceStatus.COMPLETED);
        invoice.setIssuedDate(LocalDateTime.now());

        Invoice updated = invoiceRepository.save(invoice);
        log.info("Invoice completed: {}", invoice.getInvoiceNumber());
        return mapToDTO(updated);
    }

    public InvoiceDTO syncWithExternalSystem(Long id, SyncInvoiceRequest request) {
        Invoice invoice = invoiceRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        try {
            String externalId = callExternalInvoiceSystem();
            invoice.syncWithExternal(externalId);
            Invoice updated = invoiceRepository.save(invoice);
            log.info("Invoice synced with external system: {}", invoice.getInvoiceNumber());
            return mapToDTO(updated);
        } catch (Exception e) {
            invoice.setErrorMessage(e.getMessage());
            invoiceRepository.save(invoice);
            throw new RuntimeException("Failed to sync invoice with external system", e);
        }
    }

    public void deleteInvoice(Long id) {
        Invoice invoice = invoiceRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        // Clear invoice ID from order
        Order order = invoice.getOrder();
        if (order != null) {
            order.setInvoiceId(null);
            orderRepository.save(order);
        }

        invoice.setDeletedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
        log.info("Invoice deleted: {}", invoice.getInvoiceNumber());
    }

    public Page<InvoiceDTO> searchInvoicesByStatus(String status, Pageable pageable) {
        Page<Invoice> invoices = invoiceRepository.findByStatus(status, pageable);
        return invoices.map(this::mapToDTO);
    }

    public Page<InvoiceDTO> searchInvoices(String query, Pageable pageable) {
        Page<Invoice> invoices = invoiceRepository.searchByInvoiceNumberOrCustomerName(query, pageable);
        return invoices.map(this::mapToDTO);
    }

    private String generateInvoiceNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = LocalDateTime.now().format(formatter);
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "INV-" + datePart + "-" + randomPart;
    }

    private String callExternalInvoiceSystem() {
        // Placeholder for external API integration
        // In production, you would call the actual external system API here
        return "EXT-" + UUID.randomUUID();
    }

    private InvoiceDTO mapToDTO(Invoice invoice) {
        List<InvoiceItemDTO> itemDTOs = invoice.getItems() != null ?
                invoice.getItems().stream()
                        .map(this::mapItemToDTO)
                        .collect(Collectors.toList())
                : null;

        InvoiceDTO.BuyerInfo buyerInfo = null;
        if (invoice.getBuyer() != null) {
            buyerInfo = InvoiceDTO.BuyerInfo.builder()
                    .id(invoice.getBuyer().getId())
                    .customerId(invoice.getBuyer().getCustomerId())
                    .buyerName(invoice.getBuyer().getBuyerName())
                    .buyerLegalName(invoice.getBuyer().getBuyerLegalName())
                    .buyerTaxCode(invoice.getBuyer().getBuyerTaxCode())
                    .buyerAddressLine(invoice.getBuyer().getBuyerAddressLine())
                    .buyerPhoneNumber(invoice.getBuyer().getBuyerPhoneNumber())
                    .buyerEmail(invoice.getBuyer().getBuyerEmail())
                    .buyerBankName(invoice.getBuyer().getBuyerBankName())
                    .buyerBankAccount(invoice.getBuyer().getBuyerBankAccount())
                    .buyerIdNumber(invoice.getBuyer().getBuyerIdNumber())
                    .visitingGuest(invoice.getBuyer().isVisitingGuest())
                    .build();
        }

        return InvoiceDTO.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceSeries(invoice.getInvoiceSeries())
                .issuedDate(invoice.getIssuedDate())
                .totalAmountWithoutTax(invoice.getTotalAmountWithoutTax())
                .totalAmount(invoice.getTotalAmount())
                .taxAmount(invoice.getTaxAmount())
                .taxPercentage(invoice.getTaxPercentage())
                .status(invoice.getStatus().name())
                .paymentType(invoice.getPaymentType() != null ? invoice.getPaymentType().name() : null)
                .invoiceType(invoice.getInvoiceType() != null ? invoice.getInvoiceType().name() : null)
                .currencyCode(invoice.getCurrencyCode())
                .externalInvoiceId(invoice.getExternalInvoiceId())
                .externalSyncAt(invoice.getExternalSyncAt())
                .errorMessage(invoice.getErrorMessage())
                .notes(invoice.getNotes())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .order(mapOrderToDTO(invoice.getOrder()))
                .buyer(buyerInfo)
                .items(itemDTOs)
                .build();
    }

    private InvoiceItemDTO mapItemToDTO(InvoiceItem item) {
        return InvoiceItemDTO.builder()
                .id(item.getId())
                .lineNumber(item.getLineNumber())
                .orderItemId(item.getOrderItemId())
                .serviceName(item.getServiceName())
                .serviceCode(item.getServiceCode())
                .unit(item.getUnit())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .discount(item.getDiscount())
                .totalAmountWithoutTax(item.getTotalAmountWithoutTax())
                .taxPercentage(item.getTaxPercentage())
                .taxAmount(item.getTaxAmount())
                .totalAmountWithTax(item.getTotalAmountWithTax())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private InvoiceDTO.OrderInfo mapOrderToDTO(Order order) {
        if (order == null) return null;

        return InvoiceDTO.OrderInfo.builder()
                .id(order.getId())
                .invoiceNumber(null)
                .customer(order.getCustomer() != null ?
                        InvoiceDTO.CustomerInfo.builder()
                                .id(order.getCustomer().getId())
                                .name(order.getCustomer().getName())
                                .phone(order.getCustomer().getPhone())
                                .build()
                        : null)
                .build();
    }

    /**
     * Get the configured invoice service from shop info
     * @return The configured ExternalInvoiceService implementation
     */
    private ExternalInvoiceService getConfiguredInvoiceService() {
        ShopInfo shopInfo = shopInfoRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Shop info not found"));

        String invoiceSystem = shopInfo.getInvoiceSystem();
        if (invoiceSystem == null || invoiceSystem.trim().isEmpty()) {
            log.warn("Invoice system not configured in shop info, defaulting to S-INVOICE");
            invoiceSystem = "S-INVOICE";
        }

        log.info("Using invoice system from shop config: {}", invoiceSystem);
        return invoiceServiceFactory.getInvoiceService(invoiceSystem);
    }

    /**
     * Sync invoice with external system (e.g., tax authority)
     * Changes status from DRAFT/FAILED to COMPLETED if successful
     */
    public InvoiceDTO syncInvoiceWithExternalSystem(Long id) {
        log.info("Syncing invoice {} with external system", id);
        Invoice invoice = invoiceRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        // Only allow sync for DRAFT or FAILED invoices
        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT &&
            invoice.getStatus() != Invoice.InvoiceStatus.FAILED) {
            throw new RuntimeException("Invoice status must be DRAFT or FAILED to sync. Current status: " + invoice.getStatus());
        }

        try {
            // Get the configured invoice service from shop info
            ExternalInvoiceService externalInvoiceService = getConfiguredInvoiceService();

            // Call external invoice service
            InvoiceResponse response = externalInvoiceService.createInvoice(invoice);

            if (response.isSuccess()) {
                // Update invoice with external system data
                invoice.setStatus(Invoice.InvoiceStatus.COMPLETED);
                invoice.setExternalInvoiceId(response.getInvoiceNo());
                invoice.setExternalSyncAt(LocalDateTime.now());
                invoice.setErrorMessage(null);

                log.info("Invoice {} synced successfully. External Invoice No: {}", id, response.getInvoiceNo());
            } else {
                // Sync failed
                invoice.setStatus(Invoice.InvoiceStatus.FAILED);
                invoice.setErrorMessage(response.getMessage());

                log.error("Failed to sync invoice {}: {}", id, response.getMessage());
            }

            Invoice savedInvoice = invoiceRepository.save(invoice);
            return mapToDTO(savedInvoice);

        } catch (Exception e) {
            log.error("Error syncing invoice {}", id, e);
            invoice.setStatus(Invoice.InvoiceStatus.FAILED);
            invoice.setErrorMessage("Error: " + e.getMessage());
            throw new RuntimeException("Failed to sync invoice: " + e.getMessage(), e);
        }
    }

    /**
     * Download invoice PDF from external system
     * Only available for COMPLETED invoices
     */
    public byte[] downloadInvoicePdf(Long id) throws IOException {
        log.info("Downloading PDF for invoice {}", id);
        Invoice invoice = invoiceRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        // Only allow download for COMPLETED invoices
        if (invoice.getStatus() != Invoice.InvoiceStatus.COMPLETED) {
            throw new RuntimeException("Invoice must be COMPLETED to download. Current status: " + invoice.getStatus());
        }

        if (StringUtils.isEmpty(invoice.getExternalInvoiceId())) {
            throw new RuntimeException("External invoice ID not found. Invoice may not be synced properly.");
        }

        try {
            ExternalInvoiceService externalInvoiceService = getConfiguredInvoiceService();
            return externalInvoiceService.downloadInvoicePdf(invoice);
        } catch (IOException e) {
            log.error("Error downloading PDF for invoice {}", id, e);
            throw new IOException("Failed to download invoice PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Send invoice via email
     * Only available for COMPLETED invoices
     */
    public InvoiceDTO sendInvoiceEmail(Long id) {
        log.info("Sending email for invoice {}", id);
        Invoice invoice = invoiceRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        // Only allow email for COMPLETED invoices
        if (invoice.getStatus() != Invoice.InvoiceStatus.COMPLETED) {
            throw new RuntimeException("Invoice must be COMPLETED to send email. Current status: " + invoice.getStatus());
        }

        // Check if buyer has email
        if (invoice.getBuyer() == null || StringUtils.isEmpty(invoice.getBuyer().getBuyerEmail())) {
            throw new RuntimeException("Customer email not found. Cannot send email.");
        }

        try {
            ExternalInvoiceService externalInvoiceService = getConfiguredInvoiceService();
            InvoiceResponse response = externalInvoiceService.sendEmailInvoice(invoice);

            if (!response.isSuccess()) {
                throw new RuntimeException("Failed to send email: " + response.getMessage());
            }

            log.info("Email sent successfully for invoice {} to {}", id, invoice.getBuyer().getBuyerEmail());
            return mapToDTO(invoice);

        } catch (Exception e) {
            log.error("Error sending email for invoice {}", id, e);
            throw new RuntimeException("Failed to send invoice email: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel invoice
     * Only available for DRAFT or FAILED invoices
     */
    public InvoiceDTO cancelInvoice(Long id, String reason) {
        log.info("Cancelling invoice {} with reason: {}", id, reason);
        Invoice invoice = invoiceRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        // Only allow cancel for DRAFT or FAILED invoices
        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT &&
            invoice.getStatus() != Invoice.InvoiceStatus.FAILED) {
            throw new RuntimeException("Only DRAFT or FAILED invoices can be cancelled. Current status: " + invoice.getStatus());
        }

        invoice.setStatus(Invoice.InvoiceStatus.CANCELLED);
        invoice.setNotes(invoice.getNotes() != null ?
                        invoice.getNotes() + "\nCancellation reason: " + reason :
                        "Cancellation reason: " + reason);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Invoice {} cancelled successfully", id);

        return mapToDTO(savedInvoice);
    }
}

