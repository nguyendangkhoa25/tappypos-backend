package com.knp.service;

import com.knp.model.dto.ReceiptPreviewRequest;
import com.knp.model.dto.order.CancelOrderRequest;
import com.knp.model.dto.order.MyWorkStatsDTO;
import com.knp.model.dto.order.OrderDTO;
import com.knp.model.dto.order.VoidOrderRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    Page<OrderDTO> getAllOrders(String status, Pageable pageable);

    Page<OrderDTO> searchOrders(String keyword, Pageable pageable);

    OrderDTO getOrderById(Long id);

    OrderDTO startOrder(Long id);

    OrderDTO completeOrder(Long id);

    OrderDTO cancelOrder(Long id, CancelOrderRequest request);

    OrderDTO voidOrder(Long id, VoidOrderRequest request);

    String generateReceipt(Long id);

    String generatePreviewReceipt(ReceiptPreviewRequest request);

    Page<OrderDTO> getOrdersByCustomerId(Long customerId, Pageable pageable);

    Page<OrderDTO> getMyPendingOrders(Pageable pageable);

    Page<OrderDTO> getMyCompletedOrders(String filterType, Integer day, Integer month, Integer year, Pageable pageable);

    MyWorkStatsDTO getMyWorkStats(String filterType, Integer day, Integer month, Integer year);
}
