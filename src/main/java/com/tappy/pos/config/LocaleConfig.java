package com.tappy.pos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Configuration for internationalization (i18n)
 * Supports English (en) and Vietnamese (vi)
 * Default language: English
 */
@Configuration
public class LocaleConfig {

    /**
     * Configure locale resolver to get language from Accept-Language header
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setDefaultLocale(Locale.ENGLISH); // Default to English
        List<Locale> supportedLocales = Arrays.asList(
                Locale.ENGLISH,
                new Locale("vi") // Vietnamese
        );
        localeResolver.setSupportedLocales(supportedLocales);
        return localeResolver;
    }

    /**
     * Configure message source for i18n messages
     * Uses UTF-8 encoding to support Vietnamese characters directly
     */
    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages"); // Location of message files
        messageSource.setDefaultEncoding("UTF-8"); // Support UTF-8 for Vietnamese
        messageSource.setUseCodeAsDefaultMessage(true); // Return code if message not found
        messageSource.setCacheSeconds(3600); // Cache for 1 hour
        messageSource.setFallbackToSystemLocale(false); // Don't fall back to system locale
        return messageSource;
    }
}

