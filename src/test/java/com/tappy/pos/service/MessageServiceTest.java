package com.tappy.pos.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService Unit Tests")
class MessageServiceTest {

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private MessageService messageService;

    private Locale defaultLocale;
    private Locale vietnameseLocale;

    @BeforeEach
    void setUp() {
        defaultLocale = Locale.getDefault();
        vietnameseLocale = new Locale.Builder().setLanguage("vi").setRegion("VN").build();
    }

    // ============= getMessage(String code) Tests =============

    @Test
    @DisplayName("Should get message by code using current locale")
    void testGetMessage_ByCodeOnly_Success() {
        // Given
        String messageCode = "error.user.not.found";
        String expectedMessage = "User not found";
        LocaleContextHolder.setLocale(defaultLocale);

        when(messageSource.getMessage(messageCode, null, defaultLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode);

        // Then
        assertThat(result).isEqualTo(expectedMessage);
        verify(messageSource).getMessage(messageCode, null, defaultLocale);
    }

    @Test
    @DisplayName("Should get message by code with Vietnamese locale")
    void testGetMessage_ByCodeOnly_VietnameseLocale() {
        // Given
        String messageCode = "error.user.inactive";
        String expectedMessage = "Người dùng không hoạt động";
        LocaleContextHolder.setLocale(vietnameseLocale);

        when(messageSource.getMessage(messageCode, null, vietnameseLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode);

        // Then
        assertThat(result).isEqualTo(expectedMessage);
        verify(messageSource).getMessage(messageCode, null, vietnameseLocale);
    }

    @Test
    @DisplayName("Should return message code as default when message not found")
    void testGetMessage_MessageNotFound() {
        // Given
        String messageCode = "error.unknown";
        LocaleContextHolder.setLocale(defaultLocale);

        when(messageSource.getMessage(messageCode, null, defaultLocale))
                .thenReturn(messageCode); // Returns code itself as fallback

        // When
        String result = messageService.getMessage(messageCode);

        // Then
        assertThat(result).isEqualTo(messageCode);
    }

    // ============= getMessage(String code, Object... args) Tests =============

    @Test
    @DisplayName("Should get message by code with arguments using current locale")
    void testGetMessage_WithArguments_Success() {
        // Given
        String messageCode = "error.user.not.found";
        Object[] args = {"john_doe"};
        String expectedMessage = "User john_doe not found";
        LocaleContextHolder.setLocale(defaultLocale);

        when(messageSource.getMessage(messageCode, args, defaultLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode, args);

        // Then
        assertThat(result).isEqualTo(expectedMessage);
        verify(messageSource).getMessage(messageCode, args, defaultLocale);
    }

    @Test
    @DisplayName("Should get message with multiple arguments")
    void testGetMessage_WithMultipleArguments() {
        // Given
        String messageCode = "error.validation.failed";
        Object[] args = {"age", "18", "100"};
        String expectedMessage = "Field age must be between 18 and 100";
        LocaleContextHolder.setLocale(defaultLocale);

        when(messageSource.getMessage(messageCode, args, defaultLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode, args);

        // Then
        assertThat(result).isEqualTo(expectedMessage);
        verify(messageSource).getMessage(messageCode, args, defaultLocale);
    }

    @Test
    @DisplayName("Should handle message with numeric arguments")
    void testGetMessage_WithNumericArguments() {
        // Given
        String messageCode = "success.items.created";
        Object[] args = {5};
        String expectedMessage = "Successfully created 5 items";
        LocaleContextHolder.setLocale(defaultLocale);

        when(messageSource.getMessage(messageCode, args, defaultLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode, args);

        // Then
        assertThat(result).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Should get message with empty arguments array")
    void testGetMessage_WithEmptyArguments() {
        // Given
        String messageCode = "error.general";
        Object[] args = {};
        String expectedMessage = "An error occurred";
        LocaleContextHolder.setLocale(defaultLocale);

        when(messageSource.getMessage(messageCode, args, defaultLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode, args);

        // Then
        assertThat(result).isEqualTo(expectedMessage);
    }

    // ============= getMessage(String code, Locale locale) Tests =============

    @Test
    @DisplayName("Should get message by code and specific locale")
    void testGetMessage_ByCodeAndLocale_Success() {
        // Given
        String messageCode = "error.user.not.found";
        String expectedMessage = "Người dùng không tìm thấy";

        when(messageSource.getMessage(messageCode, null, vietnameseLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode, vietnameseLocale);

        // Then
        assertThat(result).isEqualTo(expectedMessage);
        verify(messageSource).getMessage(messageCode, null, vietnameseLocale);
    }

    @Test
    @DisplayName("Should override current locale with specific locale")
    void testGetMessage_SpecificLocaleOverridesCurrent() {
        // Given
        String messageCode = "success.login";
        LocaleContextHolder.setLocale(defaultLocale);
        String expectedVietnamese = "Đăng nhập thành công";

        when(messageSource.getMessage(messageCode, null, vietnameseLocale))
                .thenReturn(expectedVietnamese);

        // When
        String result = messageService.getMessage(messageCode, vietnameseLocale);

        // Then
        assertThat(result).isEqualTo(expectedVietnamese);
        verify(messageSource).getMessage(messageCode, null, vietnameseLocale);
    }

    @Test
    @DisplayName("Should get message with English locale")
    void testGetMessage_WithEnglishLocale() {
        // Given
        String messageCode = "error.invalid.credentials";
        Locale englishLocale = Locale.ENGLISH;
        String expectedMessage = "Invalid credentials";

        when(messageSource.getMessage(messageCode, null, englishLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode, englishLocale);

        // Then
        assertThat(result).isEqualTo(expectedMessage);
    }

    // ============= getMessage(String code, Locale locale, Object... args) Tests =============

    @Test
    @DisplayName("Should get message with specific locale and arguments")
    void testGetMessage_WithLocaleAndArguments_Success() {
        // Given
        String messageCode = "error.user.not.found";
        Object[] args = {"john_doe"};
        String expectedMessage = "Người dùng john_doe không tìm thấy";

        when(messageSource.getMessage(messageCode, args, vietnameseLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode, vietnameseLocale, args);

        // Then
        assertThat(result).isEqualTo(expectedMessage);
        verify(messageSource).getMessage(messageCode, args, vietnameseLocale);
    }

    @Test
    @DisplayName("Should get message with multiple arguments and specific locale")
    void testGetMessage_WithLocaleAndMultipleArguments() {
        // Given
        String messageCode = "error.validation.failed";
        Object[] args = {"email", "valid email format"};
        String expectedMessage = "Trường email phải có định dạng valid email format";

        when(messageSource.getMessage(messageCode, args, vietnameseLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode, vietnameseLocale, args);

        // Then
        assertThat(result).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Should handle message with numeric arguments and locale")
    void testGetMessage_WithLocaleAndNumericArguments() {
        // Given
        String messageCode = "success.items.processed";
        Object[] args = {42};
        String expectedMessage = "Đã xử lý thành công 42 mục";

        when(messageSource.getMessage(messageCode, args, vietnameseLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode, vietnameseLocale, args);

        // Then
        assertThat(result).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Should get message with empty arguments array and specific locale")
    void testGetMessage_WithLocaleAndEmptyArguments() {
        // Given
        String messageCode = "error.general";
        Object[] args = {};
        String expectedMessage = "Lỗi chung chung";

        when(messageSource.getMessage(messageCode, args, vietnameseLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode, vietnameseLocale, args);

        // Then
        assertThat(result).isEqualTo(expectedMessage);
    }

    // ============= Edge Case Tests =============

    @Test
    @DisplayName("Should handle null locale gracefully")
    void testGetMessage_WithNullLocale() {
        // Given
        String messageCode = "error.unknown";
        when(messageSource.getMessage(eq(messageCode), isNull(), isNull(Locale.class)))
                .thenReturn(messageCode);

        // When
        String result = messageService.getMessage(messageCode, (Locale) null);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should handle special characters in message arguments")
    void testGetMessage_WithSpecialCharacters() {
        // Given
        String messageCode = "error.invalid.input";
        Object[] args = {"<script>alert('xss')</script>"};
        String expectedMessage = "Invalid input: <script>alert('xss')</script>";
        LocaleContextHolder.setLocale(defaultLocale);

        when(messageSource.getMessage(messageCode, args, defaultLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode, args);

        // Then
        assertThat(result).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Should handle messages with long text")
    void testGetMessage_WithLongText() {
        // Given
        String messageCode = "error.description";
        String longText = "A".repeat(1000);
        Object[] args = {longText};
        String expectedMessage = "Error description: " + longText;
        LocaleContextHolder.setLocale(defaultLocale);

        when(messageSource.getMessage(messageCode, args, defaultLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode, args);

        // Then
        assertThat(result).contains("Error description:");
    }

    @Test
    @DisplayName("Should handle unicode characters in messages")
    void testGetMessage_WithUnicodeCharacters() {
        // Given
        String messageCode = "success.greeting";
        Object[] args = {"Nguyễn Đăng Khoa"};
        String expectedMessage = "Xin chào Nguyễn Đăng Khoa";

        when(messageSource.getMessage(messageCode, args, vietnameseLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode, vietnameseLocale, args);

        // Then
        assertThat(result).isEqualTo(expectedMessage);
        assertThat(result).contains("Nguyễn Đăng Khoa");
    }

    @Test
    @DisplayName("Should get common error messages")
    void testGetMessage_CommonErrorMessages() {
        // Given
        LocaleContextHolder.setLocale(defaultLocale);
        String[] errorCodes = {
                "error.unauthorized",
                "error.forbidden",
                "error.not.found",
                "error.conflict",
                "error.internal.server"
        };
        String[] expectedMessages = {
                "Unauthorized",
                "Forbidden",
                "Not Found",
                "Conflict",
                "Internal Server Error"
        };

        for (int i = 0; i < errorCodes.length; i++) {
            when(messageSource.getMessage(errorCodes[i], null, defaultLocale))
                    .thenReturn(expectedMessages[i]);
        }

        // When & Then
        for (int i = 0; i < errorCodes.length; i++) {
            String result = messageService.getMessage(errorCodes[i]);
            assertThat(result).isEqualTo(expectedMessages[i]);
        }
    }

    @Test
    @DisplayName("Should get common success messages")
    void testGetMessage_CommonSuccessMessages() {
        // Given
        LocaleContextHolder.setLocale(defaultLocale);
        String[] successCodes = {
                "success.created",
                "success.updated",
                "success.deleted",
                "success.saved"
        };
        String[] expectedMessages = {
                "Successfully created",
                "Successfully updated",
                "Successfully deleted",
                "Successfully saved"
        };

        for (int i = 0; i < successCodes.length; i++) {
            when(messageSource.getMessage(successCodes[i], null, defaultLocale))
                    .thenReturn(expectedMessages[i]);
        }

        // When & Then
        for (int i = 0; i < successCodes.length; i++) {
            String result = messageService.getMessage(successCodes[i]);
            assertThat(result).isEqualTo(expectedMessages[i]);
        }
    }

    // ============= Additional Edge Case Tests =============

    @Test
    @DisplayName("Should handle message code with no parameters in Vietnamese locale")
    void testGetMessage_VietnameseLocale_NoParams() {
        // Given
        LocaleContextHolder.setLocale(vietnameseLocale);
        String messageCode = "error.user.not.found";
        String expectedVietnamese = "Không tìm thấy người dùng";

        when(messageSource.getMessage(messageCode, null, vietnameseLocale))
                .thenReturn(expectedVietnamese);

        // When
        String result = messageService.getMessage(messageCode);

        // Then
        assertThat(result).isEqualTo(expectedVietnamese);
        verify(messageSource).getMessage(messageCode, null, vietnameseLocale);
    }

    @Test
    @DisplayName("Should get message with multiple parameters")
    void testGetMessage_MultipleParameters() {
        // Given
        LocaleContextHolder.setLocale(defaultLocale);
        String messageCode = "error.validation.field.required";
        Object[] args = {"username", "email", "password"};
        String expectedMessage = "Fields username, email, password are required";

        when(messageSource.getMessage(messageCode, args, defaultLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode, args);

        // Then
        assertThat(result).isEqualTo(expectedMessage);
        verify(messageSource).getMessage(messageCode, args, defaultLocale);
    }

    @Test
    @DisplayName("Should handle message with numeric parameter")
    void testGetMessage_NumericParameter() {
        // Given
        LocaleContextHolder.setLocale(defaultLocale);
        String messageCode = "error.resource.not.found.by.id";
        Object[] args = {123L};
        String expectedMessage = "Resource with ID 123 not found";

        when(messageSource.getMessage(messageCode, args, defaultLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode, args);

        // Then
        assertThat(result).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Should handle message with null parameter")
    void testGetMessage_NullParameter() {
        // Given
        LocaleContextHolder.setLocale(defaultLocale);
        String messageCode = "error.invalid.value";
        Object[] args = {null};
        String expectedMessage = "Invalid value: null";

        when(messageSource.getMessage(messageCode, args, defaultLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode, args);

        // Then
        assertThat(result).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Should get message using specific locale parameter")
    void testGetMessage_WithSpecificLocale() {
        // Given
        Locale frenchLocale = new Locale("fr", "FR");
        String messageCode = "success.created";
        String expectedFrench = "Créé avec succès";

        when(messageSource.getMessage(messageCode, null, frenchLocale))
                .thenReturn(expectedFrench);

        // When
        String result = messageService.getMessage(messageCode, frenchLocale);

        // Then
        assertThat(result).isEqualTo(expectedFrench);
        verify(messageSource).getMessage(messageCode, null, frenchLocale);
    }

    @Test
    @DisplayName("Should get message with parameters using specific locale")
    void testGetMessage_WithParametersAndLocale() {
        // Given
        Locale spanishLocale = new Locale("es", "ES");
        String messageCode = "error.duplicate.entity";
        Object[] args = {"User"};
        String expectedSpanish = "El usuario ya existe";

        when(messageSource.getMessage(messageCode, args, spanishLocale))
                .thenReturn(expectedSpanish);

        // When
        String result = messageService.getMessage(messageCode, spanishLocale, args);

        // Then
        assertThat(result).isEqualTo(expectedSpanish);
        verify(messageSource).getMessage(messageCode, args, spanishLocale);
    }

    @Test
    @DisplayName("Should handle message code with empty string")
    void testGetMessage_EmptyMessageCode() {
        // Given
        LocaleContextHolder.setLocale(defaultLocale);
        when(messageSource.getMessage("", null, defaultLocale))
                .thenReturn("");

        // When
        String result = messageService.getMessage("");

        // Then
        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("Should handle locale change during message retrieval")
    void testGetMessage_LocaleChange() {
        // Given
        LocaleContextHolder.setLocale(defaultLocale);
        String messageCode = "success.operation";
        when(messageSource.getMessage(messageCode, null, defaultLocale))
                .thenReturn("Operation successful");

        // When
        String resultEnglish = messageService.getMessage(messageCode);

        // Change locale
        LocaleContextHolder.setLocale(vietnameseLocale);
        when(messageSource.getMessage(messageCode, null, vietnameseLocale))
                .thenReturn("Hoạt động thành công");
        String resultVietnamese = messageService.getMessage(messageCode);

        // Then
        assertThat(resultEnglish).isEqualTo("Operation successful");
        assertThat(resultVietnamese).isEqualTo("Hoạt động thành công");
    }

    @Test
    @DisplayName("Should handle message with special characters")
    void testGetMessage_SpecialCharacters() {
        // Given
        LocaleContextHolder.setLocale(defaultLocale);
        String messageCode = "error.special.chars";
        String expectedMessage = "Error with special chars: !@#$%^&*()";

        when(messageSource.getMessage(messageCode, null, defaultLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode);

        // Then
        assertThat(result).contains("!@#$%^&*()");
    }

    @Test
    @DisplayName("Should handle message with newlines and tabs")
    void testGetMessage_FormattedMessage() {
        // Given
        LocaleContextHolder.setLocale(defaultLocale);
        String messageCode = "error.detailed.message";
        String expectedMessage = "Error occurred:\nDetails:\n\t- Field: invalid\n\t- Reason: format";

        when(messageSource.getMessage(messageCode, null, defaultLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode);

        // Then
        assertThat(result).contains("\n");
        assertThat(result).contains("\t");
    }

    @Test
    @DisplayName("Should get message with very long parameter value")
    void testGetMessage_LongParameterValue() {
        // Given
        LocaleContextHolder.setLocale(defaultLocale);
        String messageCode = "error.value.too.long";
        String longValue = "A".repeat(1000);
        Object[] args = {longValue};
        String expectedMessage = "Value is too long: " + longValue;

        when(messageSource.getMessage(messageCode, args, defaultLocale))
                .thenReturn(expectedMessage);

        // When
        String result = messageService.getMessage(messageCode, args);

        // Then
        assertThat(result).contains("A".repeat(100)); // Check substring
    }

    @Test
    @DisplayName("Should handle message retrieval multiple times for same code")
    void testGetMessage_MultipleCalls() {
        // Given
        LocaleContextHolder.setLocale(defaultLocale);
        String messageCode = "success.operation";
        String expectedMessage = "Operation successful";

        when(messageSource.getMessage(messageCode, null, defaultLocale))
                .thenReturn(expectedMessage);

        // When
        String result1 = messageService.getMessage(messageCode);
        String result2 = messageService.getMessage(messageCode);
        String result3 = messageService.getMessage(messageCode);

        // Then
        assertThat(result1).isEqualTo(expectedMessage);
        assertThat(result2).isEqualTo(expectedMessage);
        assertThat(result3).isEqualTo(expectedMessage);
        verify(messageSource, times(3)).getMessage(messageCode, null, defaultLocale);
    }
}
