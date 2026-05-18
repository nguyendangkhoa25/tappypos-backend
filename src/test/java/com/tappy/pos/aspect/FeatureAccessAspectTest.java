package com.tappy.pos.aspect;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.exception.ForbiddenException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureAccessAspect Unit Tests")
class FeatureAccessAspectTest {

    @Mock private FeatureContext featureContext;
    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private MethodSignature signature;

    @InjectMocks
    private FeatureAccessAspect aspect;

    @BeforeEach
    void setUp() {
        when(joinPoint.getSignature()).thenReturn(signature);
    }

    @RequiresFeature("ORDER")
    static class SampleController {
        public void orderAction() {}

        @RequiresFeature("PRODUCT")
        public void productAction() {}

        @RequiresFeature({"POS", "ORDER"})
        public void multiFeatureAction() {}
    }

    @Test
    @DisplayName("checkFeatureAccess: proceeds when user has required feature")
    void checkFeatureAccess_allowed() throws Throwable {
        Method method = SampleController.class.getMethod("orderAction");
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(new SampleController());
        when(featureContext.hasFeature("ORDER")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.checkFeatureAccess(joinPoint);

        assertThat(result).isEqualTo("result");
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("checkFeatureAccess: throws ForbiddenException when user lacks required feature")
    void checkFeatureAccess_denied() throws Throwable {
        Method method = SampleController.class.getMethod("orderAction");
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(new SampleController());
        when(featureContext.hasFeature("ORDER")).thenReturn(false);

        assertThatThrownBy(() -> aspect.checkFeatureAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class);
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("checkFeatureAccess: method-level annotation takes precedence — user lacks PRODUCT → denied")
    void checkFeatureAccess_methodLevelOverridesClass() throws Throwable {
        // productAction has @RequiresFeature("PRODUCT") — method-level takes precedence
        Method method = SampleController.class.getMethod("productAction");
        when(signature.getMethod()).thenReturn(method);
        // No getTarget() stub needed: code returns after finding method annotation

        assertThatThrownBy(() -> aspect.checkFeatureAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("checkFeatureAccess: OR logic — access denied when neither POS nor ORDER present")
    void checkFeatureAccess_orLogic_denied() throws Throwable {
        // multiFeatureAction has @RequiresFeature({"POS", "ORDER"}) — method-level
        Method method = SampleController.class.getMethod("multiFeatureAction");
        when(signature.getMethod()).thenReturn(method);
        // default hasFeature returns false for all features

        assertThatThrownBy(() -> aspect.checkFeatureAccess(joinPoint))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("checkFeatureAccess: falls back to class-level annotation when no method-level")
    void checkFeatureAccess_classLevelAnnotation() throws Throwable {
        Method method = SampleController.class.getMethod("orderAction");
        // Simulate no method-level annotation by returning method that doesn't have one
        Method noAnnotationMethod = Object.class.getMethod("toString");
        when(signature.getMethod()).thenReturn(noAnnotationMethod);
        when(joinPoint.getTarget()).thenReturn(new SampleController());
        when(featureContext.hasFeature("ORDER")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.checkFeatureAccess(joinPoint);

        assertThat(result).isEqualTo("ok");
    }
}
