package com.tappy.pos.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns every request a {@code requestId} and exposes it three ways so a failure can be traced end
 * to end (unified Tappy tracing — matches Tappy Land / Build):
 *
 * <ul>
 *   <li>MDC key {@code requestId} — picked up by the {@code %X{requestId}} log pattern.
 *   <li>{@code X-Request-Id} response header — visible to the client / proxy.
 *   <li>{@code ApiResponse.errorDetail.traceId} — derived from the same MDC key at serialization.
 * </ul>
 *
 * <p>Honors an inbound {@code X-Request-Id} header (set by Cloudflare / the edge Caddy) so the id is
 * consistent across hops; otherwise generates a fresh UUID. Runs at highest precedence (before the
 * tenant + security filters) and always clears the key in {@code finally} to avoid leaking across
 * pooled threads.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {

  /** MDC key — kept in sync with the {@code %X{requestId}} token in the logging pattern. */
  public static final String REQUEST_ID = "requestId";

  private static final String HEADER = "X-Request-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String requestId = request.getHeader(HEADER);
    if (!StringUtils.hasText(requestId)) {
      requestId = UUID.randomUUID().toString();
    }
    MDC.put(REQUEST_ID, requestId);
    response.setHeader(HEADER, requestId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(REQUEST_ID);
    }
  }
}
