package com.tappy.pos.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * FeatureContext — thread-local holder for the authenticated user's feature set.
 * Populated by JwtAuthenticationFilter from the JWT "features" claim.
 * Cleared at the end of each request by JwtAuthenticationFilter's finally block.
 */
@Component
@Slf4j
public class FeatureContext {

    private static final ThreadLocal<Set<String>> currentFeatures = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> masterUserFlag = new ThreadLocal<>();
    private static final ThreadLocal<Integer> tokenFeaturesVersion = new ThreadLocal<>();

    public void set(List<String> features, boolean isMasterUser) {
        set(features, isMasterUser, null);
    }

    public void set(List<String> features, boolean isMasterUser, Integer featuresVersion) {
        currentFeatures.set(features != null ? new HashSet<>(features) : Collections.emptySet());
        masterUserFlag.set(isMasterUser);
        tokenFeaturesVersion.set(featuresVersion);
        log.debug("FeatureContext set: isMasterUser={}, fv={}, features={}", isMasterUser, featuresVersion, features);
    }

    /** The `fv` claim from the current request's JWT, or null for older tokens / no token. */
    public Integer getTokenFeaturesVersion() {
        return tokenFeaturesVersion.get();
    }

    public boolean hasFeature(String feature) {
        Set<String> features = currentFeatures.get();
        return features != null && features.contains(feature);
    }

    public boolean isMasterUser() {
        return Boolean.TRUE.equals(masterUserFlag.get());
    }

    public void clear() {
        currentFeatures.remove();
        masterUserFlag.remove();
        tokenFeaturesVersion.remove();
    }
}
