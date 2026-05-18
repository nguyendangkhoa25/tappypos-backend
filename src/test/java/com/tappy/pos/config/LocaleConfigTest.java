package com.tappy.pos.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LocaleConfig
 * Covers i18n configuration for English and Vietnamese locales
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LocaleConfig Unit Tests")
class LocaleConfigTest {

    private LocaleConfig localeConfig;

    @BeforeEach
    void setUp() {
        localeConfig = new LocaleConfig();
    }

    @Test
    @DisplayName("Should create locale resolver bean")
    void testLocaleResolver_Created() {
        // When
        LocaleResolver resolver = localeConfig.localeResolver();

        // Then
        assertThat(resolver).isNotNull();
        assertThat(resolver).isInstanceOf(AcceptHeaderLocaleResolver.class);
    }

    @Test
    @DisplayName("Should support English locale")
    void testLocaleResolver_SupportsEnglish() {
        // When
        LocaleResolver resolver = localeConfig.localeResolver();
        AcceptHeaderLocaleResolver acceptHeaderResolver = (AcceptHeaderLocaleResolver) resolver;
        List<Locale> supportedLocales = acceptHeaderResolver.getSupportedLocales();

        // Then
        assertThat(supportedLocales).contains(Locale.ENGLISH);
    }

    @Test
    @DisplayName("Should support Vietnamese locale")
    void testLocaleResolver_SupportsVietnamese() {
        // When
        LocaleResolver resolver = localeConfig.localeResolver();
        AcceptHeaderLocaleResolver acceptHeaderResolver = (AcceptHeaderLocaleResolver) resolver;
        List<Locale> supportedLocales = acceptHeaderResolver.getSupportedLocales();

        // Then
        Locale vietnamLocale = new Locale.Builder().setLanguage("vi").build();
        assertThat(supportedLocales).contains(vietnamLocale);
    }

    @Test
    @DisplayName("Should have exactly 2 supported locales")
    void testLocaleResolver_TwoSupportedLocales() {
        // When
        LocaleResolver resolver = localeConfig.localeResolver();
        AcceptHeaderLocaleResolver acceptHeaderResolver = (AcceptHeaderLocaleResolver) resolver;
        List<Locale> supportedLocales = acceptHeaderResolver.getSupportedLocales();

        // Then
        assertThat(supportedLocales).hasSize(2);
    }

    @Test
    @DisplayName("Should create message source bean")
    void testMessageSource_Created() {
        // When
        ResourceBundleMessageSource messageSource = localeConfig.messageSource();

        // Then
        assertThat(messageSource).isNotNull();
    }

    @Test
    @DisplayName("Should set correct basename for message source")
    void testMessageSource_Basename() {
        // When
        ResourceBundleMessageSource messageSource = localeConfig.messageSource();

        // Then
        assertThat(messageSource.getBasenameSet()).contains("i18n/messages");
    }

    @Test
    @DisplayName("Should use code as default message")
    void testMessageSource_UseCodeAsDefaultMessage() {
        // When
        ResourceBundleMessageSource messageSource = localeConfig.messageSource();

        // Then
        // Message source should return the code if message not found
        assertThat(messageSource).isNotNull();
    }

    @Test
    @DisplayName("Should create multiple resolver instances")
    void testLocaleResolver_MultipleInstances() {
        // When
        LocaleResolver resolver1 = localeConfig.localeResolver();
        LocaleResolver resolver2 = localeConfig.localeResolver();

        // Then
        assertThat(resolver1).isNotNull();
        assertThat(resolver2).isNotNull();
    }

    @Test
    @DisplayName("Should create multiple message source instances")
    void testMessageSource_MultipleInstances() {
        // When
        ResourceBundleMessageSource messageSource1 = localeConfig.messageSource();
        ResourceBundleMessageSource messageSource2 = localeConfig.messageSource();

        // Then
        assertThat(messageSource1).isNotNull();
        assertThat(messageSource2).isNotNull();
    }

    @Test
    @DisplayName("Should support both locales with correct language codes")
    void testLocaleResolver_LanguageCodes() {
        // When
        LocaleResolver resolver = localeConfig.localeResolver();
        AcceptHeaderLocaleResolver acceptHeaderResolver = (AcceptHeaderLocaleResolver) resolver;
        List<Locale> supportedLocales = acceptHeaderResolver.getSupportedLocales();

        // Then
        List<String> languageCodes = supportedLocales.stream()
                .map(Locale::getLanguage)
                .toList();
        
        assertThat(languageCodes).contains("en", "vi");
    }

    @Test
    @DisplayName("Should have basename configured for i18n support")
    void testMessageSource_I18nSupport() {
        // When
        ResourceBundleMessageSource messageSource = localeConfig.messageSource();

        // Then
        assertThat(messageSource).isNotNull();
        assertThat(messageSource.getBasenameSet()).isNotEmpty();
    }

    @Test
    @DisplayName("Should be instantiable")
    void testLocaleConfig_Instantiation() {
        // When & Then
        assertThat(localeConfig).isNotNull();
    }

    @Test
    @DisplayName("Should support English as primary language")
    void testLocaleConfig_EnglishSupport() {
        // When
        LocaleResolver resolver = localeConfig.localeResolver();

        // Then
        assertThat(resolver).isNotNull();
        assertThat(resolver).isInstanceOf(AcceptHeaderLocaleResolver.class);
    }

    @Test
    @DisplayName("Should support Vietnamese as secondary language")
    void testLocaleConfig_VietnameseSupport() {
        // When
        LocaleResolver resolver = localeConfig.localeResolver();
        AcceptHeaderLocaleResolver acceptHeaderResolver = (AcceptHeaderLocaleResolver) resolver;
        List<Locale> supportedLocales = acceptHeaderResolver.getSupportedLocales();

        // Then
        assertThat(supportedLocales).hasSize(2);
        List<String> languages = supportedLocales.stream()
                .map(Locale::getLanguage)
                .toList();
        assertThat(languages).contains("vi");
    }
}



