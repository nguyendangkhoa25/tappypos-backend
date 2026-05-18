package com.tappy.pos.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Exposes the Spring ApplicationContext as a static reference so that
 * JPA AttributeConverters (instantiated by Hibernate, not Spring) can
 * resolve Spring-managed beans.
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        ctx = applicationContext;
    }

    public static <T> T getBean(Class<T> type) {
        if (ctx == null) throw new IllegalStateException("Spring context not yet initialised");
        return ctx.getBean(type);
    }
}
