package com.knp.service.invoice;

import com.knp.exception.BadRequestException;
import com.knp.service.MessageService;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.invoice.*;
import java.time.Instant;
import java.time.ZoneId;
import com.knp.model.entity.finance.*;
import com.knp.model.entity.order.*;
import com.knp.model.entity.customer.*;
import com.knp.model.entity.tenant.*;
import com.knp.repository.finance.InvoiceRepository;
import com.knp.repository.order.OrderRepository;
import com.knp.model.enums.ShopConfigKey;
import com.knp.service.invoice.ExternalInvoiceService;
import com.knp.service.invoice.InvoiceServiceFactory;
import com.knp.service.tenant.ShopConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final ShopConfigService shopConfigService;
    private final InvoiceServiceFactory invoiceServiceFactory;
    private final MessageService messageService;

    @Override
    public Page<InvoiceDTO> getAllInvoices(Pageable pageable) {
        log.info("Request: Get all invoices");
        return invoiceRepository.findAllActive(pageable).map(this::mapToDTO);
    }

    @Override
    public Page<InvoiceDTO> getInvoicesByStatus(String status, Pageable pageable) {
        log.info("Request: Get invoices by status: {}", status);
        Invoice.InvoiceStatus invoiceStatus = Invoice.InvoiceStatus.valueOf(status.toUpperCase());
        return invoiceRepository.findAllActiveByStatus(invoiceStatus, pageable).map(this::mapToDTO);
    }

    @Override
    public Page<InvoiceDTO> searchInvoices(String keyword, Pageable pageable) {
        log.info("Request: Search invoices - keyword: {}", keyword);
        return invoiceRepository.searchByKeyword(keyword, pageable).map(this::mapToDTO);
    }

    @Override
    public InvoiceDTO getById(Long id) {
        log.info("Request: Get invoice by id: {}", id);
        Invoice invoice = findActiveInvoice(id);
        return mapToDTO(invoice);
    }

    @Override
    public InvoiceDTO getByOrderId(Long orderId) {
        log.info("Request: Get invoice by order id: {}", orderId);
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.invoice.not.found.for.order", orderId)));
        return mapToDTO(invoice);
    }

    @Override
    @Transactional
    public InvoiceDTO create(CreateInvoiceRequest request) {
        log.info("Request: Create invoice for {} order(s)", request.getOrderIds() != null ? request.getOrderIds().size() : 0);

        if (request.getOrderIds() == null || request.getOrderIds().isEmpty()) {
            throw new BadRequestException(messageService.getMessage("error.invoice.orders.required"));
        }

        List<Order> orders = new ArrayList<>();
        for (Long orderId : request.getOrderIds()) {
            Order order = orderRepository.findById(orderId)
                    .filter(o -> !Boolean.TRUE.equals(o.getDeleted()))
                    .orElseThrow(() -> new ResourceNotFoundException(
                            messageService.getMessage("error.order.not.found", orderId)));

            if (order.getInvoice() != null) {
                throw new BadRequestException(
                        messageService.getMessage("error.invoice.order.already.invoiced", order.getOrderNumber()));
            }

            orders.add(order);
        }

        String createdBy = SecurityContextHolder.getContext().getAuthentication().getName();

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .invoiceSeries(request.getInvoiceSeries())
                .status(Invoice.InvoiceStatus.DRAFT)
                .taxPercentage(request.getTaxPercentage() != null ? request.getTaxPercentage() : BigDecimal.ZERO)
                .paymentType(request.getPaymentType())
                .invoiceType(request.getInvoiceType())
                .currencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "VND")
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        if (request.getBuyerInfo() != null) {
            CreateInvoiceRequest.BuyerInfo b = request.getBuyerInfo();
            invoice.setBuyerInfo(InvoiceBuyerInfo.builder()
                    .buyerName(b.getBuyerName())
                    .buyerLegalName(b.getBuyerLegalName())
                    .buyerTaxCode(b.getBuyerTaxCode())
                    .buyerAddressLine(b.getBuyerAddressLine())
                    .buyerPhoneNumber(b.getBuyerPhoneNumber())
                    .buyerEmail(b.getBuyerEmail())
                    .buyerBankName(b.getBuyerBankName())
                    .buyerBankAccount(b.getBuyerBankAccount())
                    .buyerIdNumber(b.getBuyerIdNumber())
                    .visitingGuest(b.isVisitingGuest())
                    .build());
        } else {
            // Auto-populate buyer info from first order's customer
            Order firstOrder = orders.get(0);
            if (firstOrder.getCustomer() != null) {
                Customer c = firstOrder.getCustomer();
                invoice.setBuyerInfo(InvoiceBuyerInfo.builder()
                        .customerId(c.getId())
                        .buyerName(c.getName())
                        .buyerPhoneNumber(c.getPhone())
                        .buyerEmail(c.getEmail())
                        .visitingGuest(false)
                        .build());
            } else {
                invoice.setBuyerInfo(InvoiceBuyerInfo.builder().visitingGuest(true).build());
            }
        }

        // Build invoice items from order items
        List<InvoiceItem> invoiceItems = buildInvoiceItems(invoice, orders, request);
        invoice.setItems(invoiceItems);

        // Calculate totals
        BigDecimal totalWithoutTax = invoiceItems.stream()
                .map(InvoiceItem::getTotalAmountWithoutTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxAmount = invoiceItems.stream()
                .map(InvoiceItem::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        invoice.setTotalAmountWithoutTax(totalWithoutTax);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(totalWithoutTax.add(taxAmount));

        Invoice saved = invoiceRepository.save(invoice);

        // Link orders to this invoice
        for (Order order : orders) {
            order.setInvoice(saved);
            orderRepository.save(order);
        }

        log.info("Invoice {} created successfully", saved.getInvoiceNumber());
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public InvoiceDTO update(Long id, UpdateInvoiceRequest request) {
        log.info("Request: Update invoice id: {}", id);
        Invoice invoice = findActiveInvoice(id);

        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new BadRequestException(messageService.getMessage("error.invoice.cannot.modify.non.draft"));
        }

        if (request.getNotes() != null) invoice.setNotes(request.getNotes());
        if (request.getPaymentType() != null) invoice.setPaymentType(request.getPaymentType());
        if (request.getInvoiceType() != null) invoice.setInvoiceType(request.getInvoiceType());
        if (request.getInvoiceSeries() != null) invoice.setInvoiceSeries(request.getInvoiceSeries());

        if (request.getBuyerInfo() != null) {
            UpdateInvoiceRequest.BuyerInfoRequest b = request.getBuyerInfo();
            InvoiceBuyerInfo existing = invoice.getBuyerInfo() != null ? invoice.getBuyerInfo() : new InvoiceBuyerInfo();
            if (b.getVisitingGuest() != null) existing.setVisitingGuest(b.getVisitingGuest());
            if (b.getBuyerName() != null) existing.setBuyerName(b.getBuyerName());
            if (b.getBuyerLegalName() != null) existing.setBuyerLegalName(b.getBuyerLegalName());
            if (b.getBuyerTaxCode() != null) existing.setBuyerTaxCode(b.getBuyerTaxCode());
            if (b.getBuyerAddressLine() != null) existing.setBuyerAddressLine(b.getBuyerAddressLine());
            if (b.getBuyerPhoneNumber() != null) existing.setBuyerPhoneNumber(b.getBuyerPhoneNumber());
            if (b.getBuyerEmail() != null) existing.setBuyerEmail(b.getBuyerEmail());
            invoice.setBuyerInfo(existing);
        }

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice {} updated", saved.getInvoiceNumber());
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public InvoiceDTO issue(Long id) {
        log.info("Request: Issue invoice id: {}", id);
        Invoice invoice = findActiveInvoice(id);

        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new BadRequestException(messageService.getMessage("error.invoice.cannot.issue.non.draft"));
        }

        invoice.issue();
        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice {} issued", saved.getInvoiceNumber());
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public InvoiceDTO cancel(Long id) {
        log.info("Request: Cancel invoice id: {}", id);
        Invoice invoice = findActiveInvoice(id);

        if (invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED) {
            throw new BadRequestException(messageService.getMessage("error.invoice.already.cancelled"));
        }

        // Unlink orders from this invoice
        for (Order order : invoice.getOrders()) {
            order.setInvoice(null);
            orderRepository.save(order);
        }

        invoice.cancel();
        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice {} cancelled", saved.getInvoiceNumber());
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Request: Delete invoice id: {}", id);
        Invoice invoice = findActiveInvoice(id);

        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new BadRequestException(messageService.getMessage("error.invoice.cannot.delete.non.draft"));
        }

        // Unlink orders
        for (Order order : invoice.getOrders()) {
            order.setInvoice(null);
            orderRepository.save(order);
        }

        invoice.softDelete();
        invoiceRepository.save(invoice);
        log.info("Invoice {} deleted", invoice.getInvoiceNumber());
    }

    @Override
    @Transactional
    public InvoiceDTO syncExternal(Long id) {
        log.info("Request: Sync invoice id: {} with external e-invoice system", id);
        Invoice invoice = findActiveInvoice(id);

        InvoiceRequest invoiceRequest = buildInvoiceRequest(invoice);
        String system = shopConfigService.getString(ShopConfigKey.INVOICE_SYSTEM);
        String vendor = system != null ? system : shopConfigService.getString(ShopConfigKey.INVOICE_VENDOR);
        ExternalInvoiceService externalService = invoiceServiceFactory.getInvoiceService(vendor);
        InvoiceResponse response = externalService.createInvoice(invoiceRequest);

        if (response.isSuccess()) {
            invoice.setExternalInvoiceId(response.getInvoiceNo());
            invoice.setCodeOfTax(response.getCodeOfTax());
            invoice.setExternalSyncAt(LocalDateTime.now());
        }
        if (response.getTransactionId() != null) {
            invoice.setTransactionUuid(response.getTransactionId());
        }
        if (!response.isSuccess() && response.getMessage() != null) {
            invoice.setErrorMessage(response.getMessage());
        }

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice {} synced with external: success={}", saved.getInvoiceNumber(), response.isSuccess());
        return mapToDTO(saved);
    }

    @Override
    public byte[] downloadPdf(Long id) throws IOException {
        log.info("Request: Download PDF for invoice id: {}", id);
        Invoice invoice = findActiveInvoice(id);

        if (invoice.getExternalInvoiceId() == null && invoice.getTransactionUuid() == null) {
            throw new BadRequestException(messageService.getMessage("error.invoice.not.synced"));
        }

        String system = shopConfigService.getString(ShopConfigKey.INVOICE_SYSTEM);
        String vendor = system != null ? system : shopConfigService.getString(ShopConfigKey.INVOICE_VENDOR);
        ExternalInvoiceService externalService = invoiceServiceFactory.getInvoiceService(vendor);
        return externalService.downloadInvoicePdf(invoice);
    }

    @Override
    public InvoiceResponse sendEmail(Long id) {
        log.info("Request: Send email for invoice id: {}", id);
        Invoice invoice = findActiveInvoice(id);

        if (invoice.getTransactionUuid() == null) {
            throw new BadRequestException(messageService.getMessage("error.invoice.not.synced"));
        }

        String system = shopConfigService.getString(ShopConfigKey.INVOICE_SYSTEM);
        String vendor = system != null ? system : shopConfigService.getString(ShopConfigKey.INVOICE_VENDOR);
        ExternalInvoiceService externalService = invoiceServiceFactory.getInvoiceService(vendor);
        return externalService.sendEmailInvoice(invoice);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Invoice findActiveInvoice(Long id) {
        return invoiceRepository.findById(id)
                .filter(i -> !Boolean.TRUE.equals(i.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.invoice.not.found", id)));
    }

    @Override
    public InvoiceKpiResponse getKpiSection(Long fromDateMs, Long toDateMs) {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDateTime fromDate = fromDateMs != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(fromDateMs), zone)
                : LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime toDate = toDateMs != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(toDateMs), zone)
                : LocalDateTime.now();
        long count = invoiceRepository.countInvoicesInRange(fromDate, toDate);
        BigDecimal sum = invoiceRepository.sumInvoicesInRange(fromDate, toDate);
        return InvoiceKpiResponse.builder()
                .totalInvoiceCount(count)
                .totalInvoiceAmount(sum != null ? sum : BigDecimal.ZERO)
                .build();
    }

    private InvoiceRequest buildInvoiceRequest(Invoice invoice) {
        List<InvoiceItemRequest> itemRequests = invoice.getItems().stream()
                .map(item -> InvoiceItemRequest.builder()
                        .lineNumber(item.getLineNumber() != null ? item.getLineNumber().longValue() : null)
                        .itemName(item.getServiceName())
                        .itemCode(item.getServiceCode())
                        .unit(item.getUnit())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .discount(item.getDiscount())
                        .taxPercentage(item.getTaxPercentage())
                        .itemTotalAmountWithoutTax(item.getTotalAmountWithoutTax())
                        .itemTotalAmountWithTax(item.getTotalAmountWithTax())
                        .build())
                .collect(Collectors.toList());

        InvoiceBuyerRequest buyerRequest = null;
        if (invoice.getBuyerInfo() != null) {
            InvoiceBuyerInfo b = invoice.getBuyerInfo();
            buyerRequest = InvoiceBuyerRequest.builder()
                    .customerId(b.getCustomerId())
                    .buyerName(b.getBuyerName())
                    .buyerLegalName(b.getBuyerLegalName())
                    .buyerTaxCode(b.getBuyerTaxCode())
                    .buyerAddressLine(b.getBuyerAddressLine())
                    .buyerPhoneNumber(b.getBuyerPhoneNumber())
                    .buyerEmail(b.getBuyerEmail())
                    .buyerBankName(b.getBuyerBankName())
                    .buyerBankAccount(b.getBuyerBankAccount())
                    .visitingGuest(b.isVisitingGuest())
                    .build();
        }

        String transactionUuid = invoice.getTransactionUuid() != null
                ? invoice.getTransactionUuid()
                : UUID.randomUUID().toString();

        return InvoiceRequest.builder()
                .invoiceIssuedDate(invoice.getIssuedDate() != null ? invoice.getIssuedDate() : LocalDateTime.now())
                .paymentType(invoice.getPaymentType())
                .invoiceType(invoice.getInvoiceType())
                .currencyCode(invoice.getCurrencyCode())
                .invoiceSeries(invoice.getInvoiceSeries() != null ? invoice.getInvoiceSeries() : shopConfigService.getString(ShopConfigKey.EINVOICE_SERIES))
                .transactionUuid(transactionUuid)
                .itemInfo(itemRequests)
                .buyerInfo(buyerRequest)
                .build();
    }

    private String generateInvoiceNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = invoiceRepository.countByDeletedFalse() + 1;
        String candidate = String.format("INV-%s-%04d", datePrefix, count);
        // Ensure uniqueness by incrementing if collisions exist
        while (invoiceRepository.existsByInvoiceNumber(candidate)) {
            count++;
            candidate = String.format("INV-%s-%04d", datePrefix, count);
        }
        return candidate;
    }

    private List<InvoiceItem> buildInvoiceItems(Invoice invoice, List<Order> orders,
                                                 CreateInvoiceRequest request) {
        List<InvoiceItem> items = new ArrayList<>();
        AtomicInteger lineNumber = new AtomicInteger(1);

        for (Order order : orders) {
            for (OrderItem orderItem : order.getOrderItems()) {
                BigDecimal qty = BigDecimal.ONE;
                if (orderItem.getQuantity() != null) {
                    qty = BigDecimal.valueOf(orderItem.getQuantity());
                }
                BigDecimal unitPrice = orderItem.getUnitPrice() != null
                        ? orderItem.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal discount = BigDecimal.ZERO;
                BigDecimal taxPct = orderItem.getTaxPercentage() != null
                        ? orderItem.getTaxPercentage()
                        : (request.getTaxPercentage() != null ? request.getTaxPercentage() : BigDecimal.ZERO);

                BigDecimal lineAmount = unitPrice.multiply(qty).subtract(discount);
                BigDecimal taxAmount = lineAmount.multiply(taxPct)
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

                InvoiceItem item = InvoiceItem.builder()
                        .invoice(invoice)
                        .orderItemId(orderItem.getId())
                        .orderId(order.getId())
                        .lineNumber(lineNumber.getAndIncrement())
                        .serviceName(orderItem.getProductName())
                        .unit("Cái")
                        .unitPrice(unitPrice)
                        .quantity(qty)
                        .discount(discount)
                        .totalAmountWithoutTax(lineAmount)
                        .taxPercentage(taxPct)
                        .taxAmount(taxAmount)
                        .totalAmountWithTax(lineAmount.add(taxAmount))
                        .build();

                items.add(item);
            }
        }
        return items;
    }

    private InvoiceDTO mapToDTO(Invoice invoice) {
        List<InvoiceDTO.OrderInfo> orderInfos = invoice.getOrders().stream()
                .map(o -> {
                    InvoiceDTO.CustomerInfo customerInfo = null;
                    if (o.getCustomer() != null) {
                        Customer c = o.getCustomer();
                        customerInfo = InvoiceDTO.CustomerInfo.builder()
                                .id(c.getId())
                                .name(c.getName())
                                .phone(c.getPhone())
                                .email(c.getEmail())
                                .build();
                    }

                    List<InvoiceDTO.OrderItemInfo> itemInfos = o.getOrderItems().stream()
                            .map(oi -> InvoiceDTO.OrderItemInfo.builder()
                                    .id(oi.getId())
                                    .orderId(o.getId())
                                    .productId(oi.getProductId())
                                    .productName(oi.getProductName())
                                    .quantity(oi.getQuantity())
                                    .unitPrice(oi.getUnitPrice())
                                    .totalPrice(oi.getAmount())
                                    .taxPercentage(oi.getTaxPercentage())
                                    .taxAmount(oi.getTaxAmount())
                                    .status(oi.getStatus() != null ? oi.getStatus().name() : null)
                                    .build())
                            .collect(Collectors.toList());

                    return InvoiceDTO.OrderInfo.builder()
                            .id(o.getId())
                            .invoiceNumber(o.getOrderNumber())
                            .customer(customerInfo)
                            .customerName(o.getCustomer() != null ? o.getCustomer().getName() : null)
                            .totalAmount(o.getTotalAmount())
                            .discountAmount(o.getDiscountAmount())
                            .taxAmount(o.getTaxAmount())
                            .status(o.getStatus().name())
                            .orderItems(itemInfos)
                            .build();
                })
                .collect(Collectors.toList());

        InvoiceDTO.BuyerInfo buyerInfo = null;
        if (invoice.getBuyerInfo() != null) {
            InvoiceBuyerInfo b = invoice.getBuyerInfo();
            buyerInfo = InvoiceDTO.BuyerInfo.builder()
                    .customerId(b.getCustomerId())
                    .buyerName(b.getBuyerName())
                    .buyerLegalName(b.getBuyerLegalName())
                    .buyerTaxCode(b.getBuyerTaxCode())
                    .buyerAddressLine(b.getBuyerAddressLine())
                    .buyerPhoneNumber(b.getBuyerPhoneNumber())
                    .buyerEmail(b.getBuyerEmail())
                    .buyerBankName(b.getBuyerBankName())
                    .buyerBankAccount(b.getBuyerBankAccount())
                    .buyerIdNumber(b.getBuyerIdNumber())
                    .visitingGuest(b.isVisitingGuest())
                    .build();
        }

        List<InvoiceItemDTO> itemDTOs = invoice.getItems().stream()
                .map(item -> InvoiceItemDTO.builder()
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
                        .build())
                .collect(Collectors.toList());

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
                .paymentType(invoice.getPaymentType())
                .invoiceType(invoice.getInvoiceType())
                .currencyCode(invoice.getCurrencyCode())
                .externalInvoiceId(invoice.getExternalInvoiceId())
                .transactionUuid(invoice.getTransactionUuid())
                .codeOfTax(invoice.getCodeOfTax())
                .externalSyncAt(invoice.getExternalSyncAt())
                .errorMessage(invoice.getErrorMessage())
                .notes(invoice.getNotes())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .orders(orderInfos)
                .buyer(buyerInfo)
                .items(itemDTOs)
                .build();
    }
}
