package com.tappy.pos.controller.mobile;

import com.tappy.pos.model.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/app")
public class AppVersionController {

    @GetMapping("/version")
    public ResponseEntity<ApiResponse<Map<String, String>>> getVersion() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("minVersion", "1.0.0", "latestVersion", "1.0.0"), "OK"));
    }
}
