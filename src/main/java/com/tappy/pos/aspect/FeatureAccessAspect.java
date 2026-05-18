package com.tappy.pos.aspect;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * Enforces @RequiresFeature on controllers and methods.
 * The user's feature set comes from FeatureContext, which is populated by
 * JwtAuthenticationFilter from the JWT "features" claim.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class FeatureAccessAspect {

    private final FeatureContext featureContext;

    @Around("@within(com.tappy.pos.annotation.RequiresFeature) || @annotation(com.tappy.pos.annotation.RequiresFeature)")
    public Object checkFeatureAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String[] required = resolveRequiredFeatures(joinPoint);

        boolean hasAccess = java.util.Arrays.stream(required).anyMatch(featureContext::hasFeature);
        if (!hasAccess) {
            log.warn("Access denied: none of features {} present. isMasterUser={}", java.util.Arrays.toString(required), featureContext.isMasterUser());
            throw new ForbiddenException("error.access.feature_required");
        }

        return joinPoint.proceed();
    }

    private String[] resolveRequiredFeatures(ProceedingJoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();

        // Method-level annotation takes precedence over class-level
        RequiresFeature method = sig.getMethod().getAnnotation(RequiresFeature.class);
        if (method != null) return method.value();

        RequiresFeature type = joinPoint.getTarget().getClass().getAnnotation(RequiresFeature.class);
        if (type != null) return type.value();

        throw new IllegalStateException("@RequiresFeature not found on: " + sig);
    }
}
