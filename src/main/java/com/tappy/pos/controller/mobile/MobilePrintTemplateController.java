package com.tappy.pos.controller.mobile;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/shop-config/print-templates")
@RequiresFeature("PRINT_TEMPLATE")
public class MobilePrintTemplateController {

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPrintTemplates() {
        return ResponseEntity.ok(ApiResponse.success(List.of(), "OK"));
    }
}
