package com.tappy.pos.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransactionManagementConfig Tests")
class TransactionManagementConfigTest {

    @Test
    @DisplayName("TransactionManagementConfig can be instantiated")
    void transactionManagementConfig_instantiation() {
        TransactionManagementConfig config = new TransactionManagementConfig();
        assertThat(config).isNotNull();
    }

    @Test
    @DisplayName("TransactionManagementConfig has @EnableTransactionManagement with order 100")
    void transactionManagementConfig_hasCorrectOrder() {
        EnableTransactionManagement annotation = TransactionManagementConfig.class
                .getAnnotation(EnableTransactionManagement.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.order()).isEqualTo(100);
    }
}
