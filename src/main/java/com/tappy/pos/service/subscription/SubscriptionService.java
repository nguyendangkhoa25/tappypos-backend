package com.tappy.pos.service.subscription;

import java.util.Map;

public interface SubscriptionService {
    Map<String, Object> getForCurrentTenant();
    void checkOrderLimit();
}
