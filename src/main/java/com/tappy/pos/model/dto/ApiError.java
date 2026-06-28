package com.tappy.pos.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * Structured error envelope — the unified Tappy shape (matches Tappy Land / Build): stable {@code
 * code}, localized {@code message}, optional field {@code details}, and a {@code traceId} that
 * correlates the failure with the {@code requestId} in the server logs.
 *
 * <p><b>Phase 1 (non-breaking):</b> this is surfaced on the {@code errorDetail} field of {@link
 * ApiResponse} <i>alongside</i> the legacy string {@code error} field, so existing web/mobile
 * clients keep reading {@code error} while new code can migrate to {@code errorDetail}. See {@code
 * docs/API_RESPONSE_ENVELOPE_PLAN.md}.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
  private final String code;
  private final String message;
  private final Map<String, Object> details;
  private final String traceId;
}
