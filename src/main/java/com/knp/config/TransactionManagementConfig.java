package com.knp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Gives the Spring @Transactional advisor a fixed order (100) so that
 * TenantRlsAspect (@Order 101) can run *inside* each transaction — i.e.
 * after the EntityManager + JDBC connection are bound but before any
 * business SQL executes.
 *
 * Spring Boot's default EnableTransactionManagement order is LOWEST_PRECEDENCE
 * (Integer.MAX_VALUE), which makes it impossible to insert another aspect
 * inside it. By declaring our own @EnableTransactionManagement here, the
 * Spring Boot auto-configuration backs off (it's @ConditionalOnMissingBean).
 */
@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement(proxyTargetClass = false, order = 100)
public class TransactionManagementConfig {
}
