package com.tappy.pos.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class AppConfig {

    /** Zalo's OAuth + ZNS endpoints reply with Content-Type {@code text/json}, which the default
     *  Jackson converter does not bind. Without this, every Zalo token refresh / send fails with
     *  "no suitable HttpMessageConverter for response type Map and content type text/json". */
    static final MediaType TEXT_JSON = MediaType.valueOf("text/json");

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        for (HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
            if (converter instanceof MappingJackson2HttpMessageConverter jackson) {
                List<MediaType> supported = new ArrayList<>(jackson.getSupportedMediaTypes());
                if (!supported.contains(TEXT_JSON)) {
                    supported.add(TEXT_JSON);
                    jackson.setSupportedMediaTypes(supported);
                }
            }
        }
        return restTemplate;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Frontend often echoes back read-only fields (id, createdAt, sku on update, etc.)
        // that are not present on request DTOs — ignore them instead of throwing.
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // Treat empty string as null for enums (frontend sends "" when no option is selected)
        mapper.coercionConfigFor(LogicalType.Enum)
              .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
        return mapper;
    }
}

