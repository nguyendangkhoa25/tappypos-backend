package com.tappy.pos.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Spring Boot 4's server MVC JSON converter is Jackson 3, which (unlike the Jackson 2 this app was
     * built on) defaults {@code FAIL_ON_NULL_FOR_PRIMITIVES} to {@code true} — so a request that omits a
     * primitive {@code boolean}/{@code int} field 500s instead of defaulting it. Turn it back off so the
     * API keeps its pre-Boot-4 leniency. (The {@code spring.jackson.deserialization.*} property is not
     * honored here because the user-defined Jackson 2 {@link ObjectMapper} bean above changes how the
     * Jackson 3 mapper is built, so we set the feature directly on the Jackson 3 mapper builder.)
     */
    @Bean
    public JsonMapperBuilderCustomizer failOnNullForPrimitivesDisabled() {
        return builder -> builder.disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
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

