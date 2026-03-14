package com.knp.service;

import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.inventory.CreateInventoryRequest;
import com.knp.model.dto.inventory.InventoryDTO;
import com.knp.model.dto.inventory.UpdateInventoryRequest;
import com.knp.model.entity.Inventory;
import com.knp.model.entity.Product;
import com.knp.repository.InventoryRepository;
import com.knp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final MessageService messageService;

    @Override
    public InventoryDTO createInventory(CreateInventoryRequest request) {
        log.info("Request: Create new inventory - productId: {}, quantity: {}, location: {}, batch: {}",
                request.getProductId(), request.getQuantityInStock(), request.getWarehouseLocation(), request.getBatchNumber());

        // Fetch the product first
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> {
                    log.error("Product not found - productId: {}", request.getProductId());
                    String errorMessage = messageService.getMessage("error.product.not.found", request.getProductId());
                    return new ResourceNotFoundException(errorMessage);
                });

        // Note: Multiple inventory records allowed per product for different batches
        // (e.g., same drug from different suppliers or received on different dates)

        Inventory inventory = Inventory.builder()
                .product(product)
                .quantityInStock(request.getQuantityInStock())
                .reorderLevel(request.getReorderLevel())
                .reorderQuantity(request.getReorderQuantity())
                .unitCost(request.getUnitCost())
                .warehouseLocation(request.getWarehouseLocation())
                .zone(request.getZone())
                .aisle(request.getAisle())
                .shelf(request.getShelf())
                .bin(request.getBin())
                .expiryDate(request.getExpiryDate())
                .batchNumber(request.getBatchNumber())
                .notes(request.getNotes())
                .status(Inventory.InventoryStatus.valueOf(request.getStatus()))
                .inventoryType(Inventory.InventoryType.valueOf(request.getInventoryType()))
                .lastRestockDate(LocalDateTime.now())
                .deleted(false)
                .build();

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Inventory created successfully - id: {}, productId: {}", saved.getId(), saved.getProduct().getId());
        return InventoryDTO.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryDTO getInventoryById(Long id) {
        log.info("Request: Get inventory - id: {}", id);
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Inventory not found - id: {}", id);
                    String errorMessage = messageService.getMessage("error.inventory.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });

        if (inventory.isDeleted()) {
            String errorMessage = messageService.getMessage("error.inventory.not.found", id);
            log.error("Inventory is deleted - id: {}", id);
            throw new ResourceNotFoundException(errorMessage);
        }

        log.info("Retrieved inventory - id: {}, productId: {}", inventory.getId(), inventory.getProduct().getId());
        return InventoryDTO.fromEntity(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryDTO> getInventoryByProductId(Long productId, Pageable pageable) {
        log.info("Request: Get inventory by productId - productId: {}, page: {}", productId, pageable.getPageNumber());
        Page<Inventory> inventories = inventoryRepository.findByProductIdAndDeletedFalseOrderByCreatedAtDesc(productId, pageable);
        
        if (inventories.isEmpty()) {
            log.warn("No inventory found for product - productId: {}", productId);
        }
        
        log.info("Retrieved {} inventory records for productId: {}", inventories.getTotalElements(), productId);
        return inventories.map(InventoryDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryDTO> getAllInventory(Pageable pageable) {
        log.info("Request: Get all inventory - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Inventory> inventories = inventoryRepository.findAllActive(pageable);
        log.info("Retrieved {} inventories from page {}", inventories.getContent().size(), pageable.getPageNumber());
        return inventories.map(InventoryDTO::fromEntity);
    }

    @Override
    public InventoryDTO updateInventory(Long id, UpdateInventoryRequest request) {
        log.info("Request: Update inventory - id: {}, quantity: {}", id, request.getQuantityInStock());
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Inventory not found - id: {}", id);
                    String errorMessage = messageService.getMessage("error.inventory.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });

        if (inventory.isDeleted()) {
            String errorMessage = messageService.getMessage("error.inventory.not.found", id);
            log.error("Inventory is deleted - id: {}", id);
            throw new ResourceNotFoundException(errorMessage);
        }

        inventory.setQuantityInStock(request.getQuantityInStock());
        inventory.setReorderLevel(request.getReorderLevel());
        inventory.setReorderQuantity(request.getReorderQuantity());
        inventory.setUnitCost(request.getUnitCost());
        inventory.setWarehouseLocation(request.getWarehouseLocation());
        inventory.setZone(request.getZone());
        inventory.setAisle(request.getAisle());
        inventory.setShelf(request.getShelf());
        inventory.setBin(request.getBin());
        inventory.setExpiryDate(request.getExpiryDate());
        inventory.setBatchNumber(request.getBatchNumber());
        inventory.setNotes(request.getNotes());
        inventory.setStatus(Inventory.InventoryStatus.valueOf(request.getStatus()));
        inventory.setInventoryType(Inventory.InventoryType.valueOf(request.getInventoryType()));

        Inventory updated = inventoryRepository.save(inventory);
        log.info("Inventory updated successfully - id: {}, productId: {}", updated.getId(), updated.getProduct().getId());
        return InventoryDTO.fromEntity(updated);
    }

    @Override
    public void deleteInventory(Long id) {
        log.info("Request: Delete inventory - id: {}", id);
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Inventory not found - id: {}", id);
                    String errorMessage = messageService.getMessage("error.inventory.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });

        inventory.softDelete();
        inventoryRepository.save(inventory);
        log.info("Inventory deleted successfully - id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryDTO> getLowStockItems() {
        log.info("Request: Get low stock items");
        List<Inventory> items = inventoryRepository.findLowStockItems();
        log.info("Found {} low stock items", items.size());
        return items.stream().map(InventoryDTO::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryDTO> getExpiredItems() {
        log.info("Request: Get expired items");
        List<Inventory> items = inventoryRepository.findExpiredItems();
        log.info("Found {} expired items", items.size());
        return items.stream().map(InventoryDTO::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryDTO> getExpiringSoon() {
        log.info("Request: Get items expiring soon");
        java.time.LocalDate expiryThreshold = java.time.LocalDate.now().plusDays(30);
        List<Inventory> items = inventoryRepository.findExpiringSoon(expiryThreshold);
        log.info("Found {} items expiring soon", items.size());
        return items.stream().map(InventoryDTO::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryDTO> searchInventory(String keyword, Pageable pageable) {
        log.info("Request: Search inventory - keyword: {}, page: {}", keyword, pageable.getPageNumber());
        try {
            Long productId = Long.parseLong(keyword);
            Page<Inventory> inventories = inventoryRepository.searchByKeyword(productId, keyword, pageable);
            log.info("Found {} inventories matching keyword", inventories.getTotalElements());
            return inventories.map(InventoryDTO::fromEntity);
        } catch (NumberFormatException e) {
            // Search by batch number or warehouse location instead
            Page<Inventory> inventories = inventoryRepository.searchByKeyword(0L, keyword, pageable);
            log.info("Found {} inventories matching keyword", inventories.getTotalElements());
            return inventories.map(InventoryDTO::fromEntity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryDTO> getInventoryByWarehouse(String location, Pageable pageable) {
        log.info("Request: Get inventory by warehouse - location: {}, page: {}", location, pageable.getPageNumber());
        Page<Inventory> inventories = inventoryRepository.findByWarehouseLocation(location, pageable);
        log.info("Found {} inventories in warehouse: {}", inventories.getTotalElements(), location);
        return inventories.map(InventoryDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryDTO> getInventoryByType(String type, Pageable pageable) {
        log.info("Request: Get inventory by type - type: {}, page: {}", type, pageable.getPageNumber());
        Inventory.InventoryType inventoryType = Inventory.InventoryType.valueOf(type.toUpperCase());
        Page<Inventory> inventories = inventoryRepository.findByInventoryType(inventoryType, pageable);
        log.info("Found {} inventories of type: {}", inventories.getTotalElements(), type);
        return inventories.map(InventoryDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateTotalInventoryValue() {
        log.info("Request: Calculate total inventory value");
        Double totalValue = inventoryRepository.calculateTotalInventoryValue();
        log.info("Total inventory value: {}", totalValue);
        return totalValue != null ? totalValue : 0.0;
    }

    @Override
    public InventoryDTO updateInventoryQuantity(Long id, Long newQuantity) {
        log.info("Request: Update inventory quantity - id: {}, newQuantity: {}", id, newQuantity);
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Inventory not found - id: {}", id);
                    String errorMessage = messageService.getMessage("error.inventory.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });

        if (inventory.isDeleted()) {
            String errorMessage = messageService.getMessage("error.inventory.not.found", id);
            log.error("Inventory is deleted - id: {}", id);
            throw new ResourceNotFoundException(errorMessage);
        }

        inventory.setQuantityInStock(newQuantity);
        Inventory updated = inventoryRepository.save(inventory);
        log.info("Inventory quantity updated - id: {}, newQuantity: {}", id, newQuantity);
        return InventoryDTO.fromEntity(updated);
    }

    @Override
    public InventoryDTO addStock(Long id, Long quantity) {
        log.info("Request: Add stock - id: {}, quantity: {}", id, quantity);
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Inventory not found - id: {}", id);
                    String errorMessage = messageService.getMessage("error.inventory.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });

        if (inventory.isDeleted()) {
            String errorMessage = messageService.getMessage("error.inventory.not.found", id);
            log.error("Inventory is deleted - id: {}", id);
            throw new ResourceNotFoundException(errorMessage);
        }

        inventory.setQuantityInStock(inventory.getQuantityInStock() + quantity);
        inventory.setLastRestockDate(LocalDateTime.now());
        Inventory updated = inventoryRepository.save(inventory);
        log.info("Stock added successfully - id: {}, quantity: {}, newTotal: {}", id, quantity, updated.getQuantityInStock());
        return InventoryDTO.fromEntity(updated);
    }

    @Override
    public InventoryDTO removeStock(Long id, Long quantity) {
        log.info("Request: Remove stock - id: {}, quantity: {}", id, quantity);
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Inventory not found - id: {}", id);
                    String errorMessage = messageService.getMessage("error.inventory.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });

        if (inventory.isDeleted()) {
            String errorMessage = messageService.getMessage("error.inventory.not.found", id);
            log.error("Inventory is deleted - id: {}", id);
            throw new ResourceNotFoundException(errorMessage);
        }

        if (inventory.getQuantityInStock() < quantity) {
            String errorMessage = messageService.getMessage("error.inventory.insufficient.stock", id);
            log.error("Insufficient stock - id: {}, available: {}, requested: {}", id, inventory.getQuantityInStock(), quantity);
            throw new BadRequestException(errorMessage);
        }

        inventory.setQuantityInStock(inventory.getQuantityInStock() - quantity);
        Inventory updated = inventoryRepository.save(inventory);
        log.info("Stock removed successfully - id: {}, quantity: {}, newTotal: {}", id, quantity, updated.getQuantityInStock());
        return InventoryDTO.fromEntity(updated);
    }

    @Override
    public List<InventoryDTO> getExpiringItems() {
        log.info("Request: Get expiring items");
        java.time.LocalDate expiryThreshold = java.time.LocalDate.now().plusDays(30);
        return inventoryRepository.findExpiringSoon(expiryThreshold).stream()
                .map(InventoryDTO::fromEntity)
                .toList();
    }

    @Override
    public Page<InventoryDTO> getByWarehouseLocation(String location, Pageable pageable) {
        log.info("Request: Get inventory by warehouse location - location: {}", location);
        return inventoryRepository.findByWarehouseLocation(location, pageable)
                .map(InventoryDTO::fromEntity);
    }

    @Override
    public Page<InventoryDTO> getByType(String type, Pageable pageable) {
        log.info("Request: Get inventory by type - type: {}", type);
        Inventory.InventoryType inventoryType = Inventory.InventoryType.valueOf(type.toUpperCase());
        return inventoryRepository.findByInventoryType(inventoryType, pageable)
                .map(InventoryDTO::fromEntity);
    }

    @Override
    public Double calculateTotalValue() {
        log.info("Request: Calculate total inventory value");
        return calculateTotalInventoryValue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryDTO> locateProduct(String keyword) {
        log.info("Request: Locate product - keyword: {}", keyword);
        List<Inventory> results = inventoryRepository.locateByKeyword(keyword.trim());
        log.info("Found {} location results for keyword: {}", results.size(), keyword);
        return results.stream().map(InventoryDTO::fromEntity).collect(Collectors.toList());
    }
}

