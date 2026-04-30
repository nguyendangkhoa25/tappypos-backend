package com.knp.controller.inventory;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.inventory.CreateInventoryRequest;
import com.knp.model.dto.inventory.InventoryDTO;
import com.knp.model.dto.inventory.UpdateInventoryRequest;
import com.knp.service.inventory.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.knp.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@RequiresFeature("INVENTORY")
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Create a new inventory record
     * POST /api/v1/inventory
     */
    @PostMapping
    public ResponseEntity<ApiResponse<InventoryDTO>> createInventory(
            @RequestBody @Valid CreateInventoryRequest request) {
        log.info("POST /api/v1/inventory - Create inventory for product: {}", request.getProductId());
        InventoryDTO inventory = inventoryService.createInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventory, "Inventory created successfully"));
    }

    /**
     * Get inventory by ID
     * GET /api/v1/inventory/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryDTO>> getInventoryById(@PathVariable Long id) {
        log.info("GET /api/v1/inventory/{} - Get inventory by id", id);
        InventoryDTO inventory = inventoryService.getInventoryById(id);
        return ResponseEntity.ok(ApiResponse.success(inventory, "Inventory retrieved successfully"));
    }

    /**
     * Get inventory by product ID (returns all batches)
     * GET /api/v1/inventory/product/{productId}
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<Page<InventoryDTO>>> getInventoryByProductId(
            @PathVariable Long productId,
            Pageable pageable) {
        log.info("GET /api/v1/inventory/product/{} - Get inventory by product id, page: {}", productId, pageable.getPageNumber());
        Page<InventoryDTO> inventories = inventoryService.getInventoryByProductId(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(inventories, "Inventory retrieved successfully"));
    }

    /**
     * Locate products by name, SKU, batch, zone, aisle, shelf, or bin — for cashier "where is it?" lookup
     * GET /api/inventory/locate?q=panadol
     */
    @GetMapping("/locate")
    public ResponseEntity<ApiResponse<List<InventoryDTO>>> locateProduct(
            @RequestParam String q) {
        log.info("GET /api/inventory/locate - keyword: {}", q);
        List<InventoryDTO> results = inventoryService.locateProduct(q);
        return ResponseEntity.ok(ApiResponse.success(results, "Location results retrieved successfully"));
    }

    /**
     * Get all inventory items with pagination
     * GET /api/v1/inventory
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<InventoryDTO>>> getAllInventory(Pageable pageable) {
        log.info("GET /api/v1/inventory - Get all inventory, page: {}, size: {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        Page<InventoryDTO> inventories = inventoryService.getAllInventory(pageable);
        return ResponseEntity.ok(ApiResponse.success(inventories, "Inventories retrieved successfully"));
    }

    /**
     * Update inventory record
     * PUT /api/v1/inventory/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryDTO>> updateInventory(
            @PathVariable Long id,
            @RequestBody @Valid UpdateInventoryRequest request) {
        log.info("PUT /api/v1/inventory/{} - Update inventory", id);
        InventoryDTO inventory = inventoryService.updateInventory(id, request);
        return ResponseEntity.ok(ApiResponse.success(inventory, "Inventory updated successfully"));
    }

    /**
     * Delete inventory (soft delete)
     * DELETE /api/v1/inventory/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInventory(@PathVariable Long id) {
        log.info("DELETE /api/v1/inventory/{} - Delete inventory", id);
        inventoryService.deleteInventory(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Inventory deleted successfully"));
    }

    /**
     * Get low stock items
     * GET /api/v1/inventory/alerts/low-stock
     */
    @GetMapping("/alerts/low-stock")
    public ResponseEntity<ApiResponse<List<InventoryDTO>>> getLowStockItems() {
        log.info("GET /api/v1/inventory/alerts/low-stock - Get low stock items");
        List<InventoryDTO> items = inventoryService.getLowStockItems();
        return ResponseEntity.ok(ApiResponse.success(items, "Low stock items retrieved successfully"));
    }

    /**
     * Get expired items
     * GET /api/v1/inventory/alerts/expired
     */
    @GetMapping("/alerts/expired")
    public ResponseEntity<ApiResponse<List<InventoryDTO>>> getExpiredItems() {
        log.info("GET /api/v1/inventory/alerts/expired - Get expired items");
        List<InventoryDTO> items = inventoryService.getExpiredItems();
        return ResponseEntity.ok(ApiResponse.success(items, "Expired items retrieved successfully"));
    }

    /**
     * Get items expiring soon (within 30 days)
     * GET /api/v1/inventory/alerts/expiring-soon
     */
    @GetMapping("/alerts/expiring-soon")
    public ResponseEntity<ApiResponse<List<InventoryDTO>>> getExpiringSoon() {
        log.info("GET /api/v1/inventory/alerts/expiring-soon - Get items expiring soon");
        List<InventoryDTO> items = inventoryService.getExpiringSoon();
        return ResponseEntity.ok(ApiResponse.success(items, "Items expiring soon retrieved successfully"));
    }

    /**
     * Search inventory by keyword
     * GET /api/v1/inventory/search?keyword=value
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<InventoryDTO>>> searchInventory(
            @RequestParam String keyword,
            Pageable pageable) {
        log.info("GET /api/v1/inventory/search - Search inventory by keyword: {}", keyword);
        Page<InventoryDTO> inventories = inventoryService.searchInventory(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(inventories, "Inventory search results retrieved successfully"));
    }

    /**
     * Get inventory by warehouse location
     * GET /api/v1/inventory/warehouse?location=value
     */
    @GetMapping("/warehouse")
    public ResponseEntity<ApiResponse<Page<InventoryDTO>>> getInventoryByWarehouse(
            @RequestParam String location,
            Pageable pageable) {
        log.info("GET /api/v1/inventory/warehouse - Get inventory by warehouse: {}", location);
        Page<InventoryDTO> inventories = inventoryService.getInventoryByWarehouse(location, pageable);
        return ResponseEntity.ok(ApiResponse.success(inventories, "Warehouse inventory retrieved successfully"));
    }

    /**
     * Get inventory by type
     * GET /api/v1/inventory/type?type=value
     */
    @GetMapping("/type")
    public ResponseEntity<ApiResponse<Page<InventoryDTO>>> getInventoryByType(
            @RequestParam String type,
            Pageable pageable) {
        log.info("GET /api/v1/inventory/type - Get inventory by type: {}", type);
        Page<InventoryDTO> inventories = inventoryService.getInventoryByType(type, pageable);
        return ResponseEntity.ok(ApiResponse.success(inventories, "Inventory by type retrieved successfully"));
    }

    /**
     * Calculate total inventory value
     * GET /api/v1/inventory/value/total
     */
    @GetMapping("/value/total")
    public ResponseEntity<ApiResponse<Double>> calculateTotalInventoryValue() {
        log.info("GET /api/v1/inventory/value/total - Calculate total inventory value");
        Double totalValue = inventoryService.calculateTotalInventoryValue();
        return ResponseEntity.ok(ApiResponse.success(totalValue, "Total inventory value calculated successfully"));
    }

    /**
     * Update inventory quantity
     * PATCH /api/v1/inventory/{id}/quantity?newQuantity=value
     */
    @PatchMapping("/{id}/quantity")
    public ResponseEntity<ApiResponse<InventoryDTO>> updateInventoryQuantity(
            @PathVariable Long id,
            @RequestParam Long newQuantity) {
        log.info("PATCH /api/v1/inventory/{}/quantity - Update quantity to: {}", id, newQuantity);
        InventoryDTO inventory = inventoryService.updateInventoryQuantity(id, newQuantity);
        return ResponseEntity.ok(ApiResponse.success(inventory, "Inventory quantity updated successfully"));
    }

    /**
     * Add stock to inventory
     * PATCH /api/v1/inventory/{id}/add-stock?quantity=value
     */
    @PatchMapping("/{id}/add-stock")
    public ResponseEntity<ApiResponse<InventoryDTO>> addStock(
            @PathVariable Long id,
            @RequestParam Long quantity) {
        log.info("PATCH /api/v1/inventory/{}/add-stock - Add stock: {}", id, quantity);
        InventoryDTO inventory = inventoryService.addStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success(inventory, "Stock added successfully"));
    }

    /**
     * Remove stock from inventory
     * PATCH /api/v1/inventory/{id}/remove-stock?quantity=value
     */
    @PatchMapping("/{id}/remove-stock")
    public ResponseEntity<ApiResponse<InventoryDTO>> removeStock(
            @PathVariable Long id,
            @RequestParam Long quantity) {
        log.info("PATCH /api/v1/inventory/{}/remove-stock - Remove stock: {}", id, quantity);
        InventoryDTO inventory = inventoryService.removeStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success(inventory, "Stock removed successfully"));
    }
}

