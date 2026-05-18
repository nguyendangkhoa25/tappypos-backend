package com.tappy.pos.controller.feedback;

import com.tappy.pos.annotation.MasterDatabaseOnly;
import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.feedback.CreateFeedbackRequest;
import com.tappy.pos.model.dto.feedback.FeedbackDTO;
import com.tappy.pos.model.dto.feedback.UpdateFeedbackRequest;
import com.tappy.pos.model.enums.FeedbackStatus;
import com.tappy.pos.model.enums.FeedbackType;
import com.tappy.pos.service.feedback.FeedbackService;
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
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @RequiresFeature("FEEDBACK")
    public ResponseEntity<FeedbackDTO> create(@Valid @RequestBody CreateFeedbackRequest request) {
        log.info("POST /feedback");
        return ResponseEntity.ok(feedbackService.create(request));
    }

    @GetMapping("/my")
    @RequiresFeature("FEEDBACK")
    public ResponseEntity<Page<FeedbackDTO>> getMy(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /feedback/my");
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(feedbackService.getMyFeedback(pageable));
    }

    @MasterDatabaseOnly
    @GetMapping
    public ResponseEntity<Page<FeedbackDTO>> getAll(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(required = false) FeedbackType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /feedback (admin)");
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(feedbackService.getAllFeedback(tenantId, status, type, pageable));
    }

    @MasterDatabaseOnly
    @PatchMapping("/{id}")
    public ResponseEntity<FeedbackDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFeedbackRequest request) {
        log.info("PATCH /feedback/{}", id);
        return ResponseEntity.ok(feedbackService.updateStatus(id, request));
    }
}
