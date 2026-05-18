package com.tappy.pos.controller.mobile;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.service.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/subscriptions")
@RequiresFeature("SHOP_INFO")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrent() {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getForCurrentTenant(), "OK"));
    }
}
