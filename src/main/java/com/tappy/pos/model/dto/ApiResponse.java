package com.tappy.pos.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tappy.pos.config.RequestTraceFilter;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.MDC;

/**
 * Uniform response envelope — the unified Tappy shape (matches Tappy Land / Build):
 * {@code { success, data, message?, meta?, error: ApiError{code,message,details,traceId}, timestamp }}.
 *
 * <p>Immutable; serialized with {@link JsonInclude} {@code NON_NULL}, so {@code null} members are
 * omitted. The {@code message} is a human-readable note the client may surface directly (e.g. a
 * snackbar) — populated on both success and error responses; on errors the same text is also carried
 * inside {@link ApiError} (with the machine-readable {@code code} + {@code traceId}). Errors are
 * built centrally by {@code GlobalExceptionHandler} via the {@link #error} factories, which stamp the
 * request {@code traceId} from MDC.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;
    private final ApiError error;
    private final ApiMeta meta;

    @Builder.Default private final Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operation successful");
    }

    public static <T> ApiResponse<T> success(T data, String message, ApiMeta meta) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .meta(meta)
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return error(code, message, null);
    }

    /** Build an error envelope; {@code details} carries per-field info (e.g. validation errors). */
    public static <T> ApiResponse<T> error(String code, String message, Map<String, Object> details) {
        return ApiResponse.<T>builder()
                .success(false)
                // Mirror the human-readable text at the top level too, not only inside ApiError.
                // The unified clients read it via apiErrMsg()/getApiErrorMessage(), but a large number
                // of older web call sites still read `err.response.data.message` directly — keeping
                // both populated avoids those sites silently degrading to a generic fallback while the
                // Phase-2 envelope flip is still gated on the mobile min-version rollout.
                .message(message)
                .error(buildError(code, message, details))
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return error("ERROR", message, null);
    }

    /** Error envelope that also carries a data payload (e.g. device-conflict session info). */
    public static <T> ApiResponse<T> errorWithData(String code, String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .error(buildError(code, message, null))
                .build();
    }

    private static ApiError buildError(String code, String message, Map<String, Object> details) {
        return ApiError.builder()
                .code(code)
                .message(message)
                .details(details)
                .traceId(MDC.get(RequestTraceFilter.REQUEST_ID))
                .build();
    }
}
