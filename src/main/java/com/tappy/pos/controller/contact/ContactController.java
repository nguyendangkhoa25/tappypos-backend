package com.tappy.pos.controller.contact;

import com.tappy.pos.annotation.MasterDatabaseOnly;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.contact.ContactLeadDTO;
import com.tappy.pos.model.dto.contact.ContactLeadRequest;
import com.tappy.pos.model.dto.contact.UpdateLeadStatusRequest;
import com.tappy.pos.model.enums.LeadStatus;
import com.tappy.pos.service.contact.ContactLeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactLeadService contactLeadService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submit(@Valid @RequestBody ContactLeadRequest request) {
        log.info("Contact lead received: name={}, phone={}, shopType={}", request.getName(), request.getPhone(), request.getShopType());
        contactLeadService.submit(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Yêu cầu của bạn đã được gửi thành công."));
    }

    @MasterDatabaseOnly
    @GetMapping
    public ResponseEntity<Page<ContactLeadDTO>> getAll(
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /contact (admin) status={}", status);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(contactLeadService.getAll(status, pageable));
    }

    @MasterDatabaseOnly
    @PatchMapping("/{id}")
    public ResponseEntity<ContactLeadDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLeadStatusRequest request) {
        log.info("PATCH /contact/{} status={}", id, request.getStatus());
        return ResponseEntity.ok(contactLeadService.updateStatus(id, request));
    }
}
