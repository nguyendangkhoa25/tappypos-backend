package com.tappy.pos.service.integration;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.integration.google.GoogleDriveCredentials;
import com.tappy.pos.integration.google.GoogleDriveIntegrationProvider;
import com.tappy.pos.integration.google.GoogleDriveService;
import com.tappy.pos.model.dto.integration.EntityImageDTO;
import com.tappy.pos.model.entity.integration.EntityImage;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.integration.EntityImageRepository;
import com.tappy.pos.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriveUploadService {

    private static final String THUMBNAIL_BASE = "https://drive.google.com/thumbnail?id=%s&sz=w400";
    private static final int    MAX_FILE_BYTES  = 10 * 1024 * 1024; // 10 MB
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final GoogleDriveIntegrationProvider driveProvider;
    private final GoogleDriveService             driveService;
    private final EntityImageRepository          entityImageRepo;
    private final TenantContext                  tenantContext;
    private final MessageService                 messageService;

    /**
     * Uploads a file to the shop's Google Drive folder for the given entity type,
     * then saves a reference row in entity_images.
     *
     * Shop isolation is guaranteed at two levels:
     *  1. {@code tenantContext.getCurrentTenantId()} is set by TenantInterceptor from the
     *     X-Tenant-ID header on every request — so this method always operates on the
     *     calling shop's data.
     *  2. {@code getCredentialsWithFreshToken(tenantId)} loads credentials from
     *     shop_integrations which is RLS-protected, so it is impossible to read
     *     another shop's Drive credentials even if tenantId were somehow spoofed.
     *  3. The folder IDs inside those credentials were created for THIS shop at
     *     connection time — uploads always land in the correct shop's folder tree.
     */
    @Transactional
    public EntityImageDTO upload(MultipartFile file, String entityType,
                                 Long entityId, String label) throws IOException {

        String tenantId = tenantContext.getCurrentTenantId();
        validate(file, entityType);

        // ── Load THIS shop's Drive credentials ────────────────────────────────
        GoogleDriveCredentials creds = driveProvider.getCredentialsWithFreshToken(tenantId);

        // ── Resolve correct upload folder ─────────────────────────────────────
        String folderId = resolveFolderId(creds, entityType.toUpperCase());

        // ── Upload ────────────────────────────────────────────────────────────
        String fileName  = buildFileName(entityType, entityId, file.getOriginalFilename());
        String mimeType  = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        byte[] fileBytes = file.getBytes();

        log.info("Uploading {} ({} bytes) for tenant={} entity={}/{} to folder={}",
                fileName, fileBytes.length, tenantId, entityType, entityId, folderId);

        GoogleDriveService.UploadResult result =
                driveService.uploadFile(creds.getAccessToken(), folderId, fileName, fileBytes, mimeType);

        // Persist updated access token if it was refreshed
        updateStoredToken(tenantId, creds);

        // ── Make file publicly accessible for thumbnail display ───────────────
        driveService.makePublic(creds.getAccessToken(), result.fileId());

        // ── Save reference in entity_images ───────────────────────────────────
        EntityImage image = EntityImage.builder()
                .tenantId(tenantId)
                .entityType(entityType.toUpperCase())
                .entityId(entityId)
                .driveFileId(result.fileId())
                .driveUrl(result.webViewLink())
                .thumbnailUrl(String.format(THUMBNAIL_BASE, result.fileId()))
                .label(label)
                .uploadedAt(LocalDateTime.now())
                .build();

        EntityImage saved = entityImageRepo.save(image);
        log.info("Saved entity_image id={} for tenant={} entity={}/{}", saved.getId(), tenantId, entityType, entityId);

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<EntityImageDTO> getImages(String entityType, Long entityId) {
        return entityImageRepo
                .findByEntityTypeAndEntityIdAndDeletedFalseOrderByUploadedAtAsc(
                        entityType.toUpperCase(), entityId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteImage(Long imageId) {
        EntityImage image = entityImageRepo.findByIdAndDeletedFalse(imageId)
                .orElseThrow(() -> new BadRequestException(
                        messageService.getMessage("error.not.found", imageId)));
        image.softDelete();
        entityImageRepo.save(image);
        log.info("Soft-deleted entity_image id={}", imageId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validate(MultipartFile file, String entityType) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(messageService.getMessage("error.drive.file.empty"));
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BadRequestException(messageService.getMessage("error.drive.file.too.large"));
        }
        if (!EntityType.isValid(entityType)) {
            throw new BadRequestException(
                    messageService.getMessage("error.drive.invalid.entity.type", entityType));
        }
    }

    /**
     * Returns the Drive folder ID for the entity type.
     * PAWN uploads go into a year/month sub-folder (e.g. 2026/05) to keep
     * the Pawns folder manageable as volume grows.
     * All other types go directly into their entity folder.
     */
    private String resolveFolderId(GoogleDriveCredentials creds, String entityType) {
        Map<String, String> folderIds = creds.getFolderIds();
        if (folderIds == null || !folderIds.containsKey(entityType)) {
            throw new BadRequestException(
                    messageService.getMessage("error.drive.folder.not.configured", entityType));
        }

        String baseFolderId = folderIds.get(entityType);

        if ("PAWN".equals(entityType)) {
            LocalDateTime now = LocalDateTime.now();
            return driveService.getOrCreateMonthFolder(
                    creds.getAccessToken(), baseFolderId, now.getYear(), now.getMonthValue());
        }

        return baseFolderId;
    }

    private String buildFileName(String entityType, Long entityId, String originalName) {
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.'));
        }
        return entityType.toUpperCase() + "_" + entityId + "_" + LocalDateTime.now().format(STAMP) + ext;
    }

    private void updateStoredToken(String tenantId, GoogleDriveCredentials creds) {
        // getCredentialsWithFreshToken already persists updated tokens; no extra save needed.
    }

    private EntityImageDTO mapToDTO(EntityImage img) {
        return EntityImageDTO.builder()
                .id(img.getId())
                .entityType(img.getEntityType())
                .entityId(img.getEntityId())
                .driveFileId(img.getDriveFileId())
                .driveUrl(img.getDriveUrl())
                .thumbnailUrl(img.getThumbnailUrl())
                .label(img.getLabel())
                .uploadedAt(img.getUploadedAt())
                .build();
    }

    // ── Allowed entity types ──────────────────────────────────────────────────

    public enum EntityType {
        PAWN, EMPLOYEE, CUSTOMER, PRODUCT;

        public static boolean isValid(String value) {
            if (value == null) return false;
            try {
                EntityType.valueOf(value.toUpperCase());
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }
}
