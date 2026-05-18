package com.tappy.pos.controller.mobile;

import com.tappy.pos.model.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/legal")
public class LegalController {

    @GetMapping("/tnc")
    public ResponseEntity<ApiResponse<Map<String, String>>> getTnC() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("version", "1.0",
                        "content", "Điều khoản sử dụng TappyPOS.",
                        "updatedAt", "2024-01-01T00:00:00"),
                "OK"));
    }
}
