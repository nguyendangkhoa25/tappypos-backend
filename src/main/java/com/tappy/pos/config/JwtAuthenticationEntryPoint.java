package com.tappy.pos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Custom authentication entry point to return 401 for unauthorized requests
 * This handles cases where JWT is invalid, expired, or missing
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException {

        log.warn("Unauthorized access attempt: {} {} - {}",
                request.getMethod(), request.getRequestURI(), authException.getMessage());

        // Get language from request header (default to Vietnamese)
        String language = request.getHeader("Accept-Language");
        Locale locale = (language != null && language.startsWith("en")) ? Locale.ENGLISH : new Locale("vi");

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String message = messageSource.getMessage("error.authentication.token.invalid", null, locale);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("error", authException.getMessage());
        errorResponse.put("data", null);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}

