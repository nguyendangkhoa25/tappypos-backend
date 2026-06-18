package com.tappy.pos.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Self-healing Flyway strategy: run {@code repair()} before {@code migrate()} on
 * every startup (i.e. every deploy, since migrations run as the Spring Boot
 * container boots).
 *
 * <p>{@code repair()} does <b>not</b> touch the schema. It:
 * <ul>
 *   <li>removes failed-migration rows left in {@code flyway_schema_history} by a
 *       previously-aborted deploy, so the next deploy can re-apply cleanly
 *       instead of refusing to start; and</li>
 *   <li>realigns stored checksums when an already-applied migration file is
 *       later corrected (e.g. the V004/V006 hardcoded-id collision fix).</li>
 * </ul>
 *
 * <p>Paired with {@code spring.flyway.validate-on-migrate=false}, this keeps
 * deploys robust to migration edits/fixes. See {@code production/PRODUCTION.md}
 * → "Database migrations".
 */
@Slf4j
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            log.info("Flyway: repair() before migrate() (self-healing deploy strategy)");
            flyway.repair();
            flyway.migrate();
        };
    }
}
