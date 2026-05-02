package com.knp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class RetailPlatformApplication {

    public static void main(String[] args) {
        // Pin JVM timezone before Spring context initialises so @CreationTimestamp,
        // @UpdateTimestamp, and any LocalDateTime.now() calls all use UTC+7.
        // The TZ env-var in Docker does the same thing, but this is a safety net
        // for local development runs outside the container.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(RetailPlatformApplication.class, args);
    }
}

