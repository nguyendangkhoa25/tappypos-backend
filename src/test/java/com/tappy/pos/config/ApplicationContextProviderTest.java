package com.tappy.pos.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ApplicationContextProvider Unit Tests")
class ApplicationContextProviderTest {

    @Test
    @DisplayName("setApplicationContext stores context and getBean retrieves bean")
    void setApplicationContext_andGetBean() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBean(String.class)).thenReturn("hello");

        ApplicationContextProvider provider = new ApplicationContextProvider();
        provider.setApplicationContext(ctx);

        String bean = ApplicationContextProvider.getBean(String.class);
        assertThat(bean).isEqualTo("hello");
    }

    @Test
    @DisplayName("getBean throws IllegalStateException before context is set")
    void getBean_beforeContextSet_throwsIllegalState() {
        // Reset static field via reflection to simulate uninitialized state
        try {
            var field = ApplicationContextProvider.class.getDeclaredField("ctx");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertThatThrownBy(() -> ApplicationContextProvider.getBean(String.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not yet initialised");
    }

    @Test
    @DisplayName("setApplicationContext overwrites previous context")
    void setApplicationContext_overwritesPrevious() {
        ApplicationContext ctx1 = mock(ApplicationContext.class);
        when(ctx1.getBean(Integer.class)).thenReturn(1);

        ApplicationContext ctx2 = mock(ApplicationContext.class);
        when(ctx2.getBean(Integer.class)).thenReturn(2);

        ApplicationContextProvider provider = new ApplicationContextProvider();
        provider.setApplicationContext(ctx1);
        provider.setApplicationContext(ctx2);

        assertThat(ApplicationContextProvider.getBean(Integer.class)).isEqualTo(2);
    }
}
