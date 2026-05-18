package com.tappy.pos.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeatureContext Unit Tests")
class FeatureContextTest {

    private final FeatureContext ctx = new FeatureContext();

    @AfterEach
    void tearDown() {
        ctx.clear();
    }

    @Test
    @DisplayName("hasFeature: returns true when feature is in the set")
    void hasFeature_found() {
        ctx.set(List.of("ORDER", "PRODUCT"), false);
        assertThat(ctx.hasFeature("ORDER")).isTrue();
        assertThat(ctx.hasFeature("PRODUCT")).isTrue();
    }

    @Test
    @DisplayName("hasFeature: returns false when feature is not in the set")
    void hasFeature_notFound() {
        ctx.set(List.of("ORDER"), false);
        assertThat(ctx.hasFeature("INVOICE")).isFalse();
    }

    @Test
    @DisplayName("hasFeature: returns false when no features are set")
    void hasFeature_notSet() {
        assertThat(ctx.hasFeature("ORDER")).isFalse();
    }

    @Test
    @DisplayName("isMasterUser: returns true when set as master")
    void isMasterUser_true() {
        ctx.set(List.of("TENANT_MGMT"), true);
        assertThat(ctx.isMasterUser()).isTrue();
    }

    @Test
    @DisplayName("isMasterUser: returns false when set as non-master")
    void isMasterUser_false() {
        ctx.set(List.of("ORDER"), false);
        assertThat(ctx.isMasterUser()).isFalse();
    }

    @Test
    @DisplayName("isMasterUser: returns false when context is not set")
    void isMasterUser_notSet() {
        assertThat(ctx.isMasterUser()).isFalse();
    }

    @Test
    @DisplayName("set: handles null features list as empty set")
    void set_nullFeatures() {
        ctx.set(null, false);
        assertThat(ctx.hasFeature("ORDER")).isFalse();
    }

    @Test
    @DisplayName("clear: removes all features and master flag")
    void clear_removesContext() {
        ctx.set(List.of("ORDER"), true);
        ctx.clear();
        assertThat(ctx.hasFeature("ORDER")).isFalse();
        assertThat(ctx.isMasterUser()).isFalse();
    }
}
