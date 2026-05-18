package com.tappy.pos.controller.integration;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.integration.EntityImageDTO;
import com.tappy.pos.service.integration.DriveUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/drive")
@RequiresFeature("GOOGLE_DRIVE")
@RequiredArgsConstructor
public class DriveUploadController {

    private final DriveUploadService driveUploadService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EntityImageDTO>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") Long entityId,
            @RequestParam(value = "label", required = false) String label) throws IOException {

        EntityImageDTO result = driveUploadService.upload(file, entityType, entityId, label);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/images/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<List<EntityImageDTO>>> getImages(
            @PathVariable String entityType,
            @PathVariable Long entityId) {

        List<EntityImageDTO> images = driveUploadService.getImages(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success(images));
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable Long imageId) {
        driveUploadService.deleteImage(imageId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
