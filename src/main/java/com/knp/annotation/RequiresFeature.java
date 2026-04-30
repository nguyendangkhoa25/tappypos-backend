package com.knp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts access to the annotated controller or method to users whose JWT
 * contains the specified feature. Works for both master-tenant and shop users —
 * access is granted only when the feature appears in the token's "features" claim.
 * <p>
 * Master-tenant users cannot access shop-only features (e.g. PRODUCT, INVENTORY)
 * because those features are never assigned to master roles.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresFeature {
    String value();
}
