package com.tappy.pos.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata for paginated / listing API responses — the unified Tappy shape (matches Tappy Land /
 * Build). Wrapped inside {@link ApiResponse} via the {@code meta} field.
 *
 * <p><b>Phase 1:</b> opt-in for <i>new</i> endpoints only. Existing POS list endpoints still embed
 * the Spring {@code Page} directly in {@code data} ({@code data.content}/{@code data.last}); migrating
 * those to {@code meta} is a separate, larger track. See {@code docs/API_RESPONSE_ENVELOPE_PLAN.md}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiMeta {

  /** Current page (0-indexed). */
  private Integer page;

  /** Items per page. */
  private Integer size;

  /** Total items across all pages. */
  private Long totalElements;

  /** Total number of pages. */
  private Integer totalPages;

  /** Whether there is a next page. */
  private Boolean hasNext;

  /** Whether there is a previous page. */
  private Boolean hasPrevious;

  /** Optional request ID for distributed tracing. */
  private String requestId;

  /** Server processing time in milliseconds (debug aid in dev). */
  private Long processingTimeMs;

  /** Convenience factory for pagination metadata. */
  public static ApiMeta fromPage(int page, int size, long totalElements) {
    int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
    return ApiMeta.builder()
        .page(page)
        .size(size)
        .totalElements(totalElements)
        .totalPages(totalPages)
        .hasNext(page + 1 < totalPages)
        .hasPrevious(page > 0)
        .build();
  }
}
