package com.tappy.pos.controller.table;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.table.CreateTableRequest;
import com.tappy.pos.model.dto.table.TableDTO;
import com.tappy.pos.model.dto.table.UpdateTableRequest;
import com.tappy.pos.service.table.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tables")
@RequiredArgsConstructor
@RequiresFeature("TABLE_SERVICE")
public class TableController {

    private final TableService tableService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TableDTO>>> getTables() {
        return ResponseEntity.ok(ApiResponse.success(tableService.getTables(), "OK"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TableDTO>> createTable(@Valid @RequestBody CreateTableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tableService.createTable(request), "Tạo bàn thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TableDTO>> updateTable(
            @PathVariable Long id, @RequestBody UpdateTableRequest request) {
        return ResponseEntity.ok(ApiResponse.success(tableService.updateTable(id, request), "Cập nhật bàn thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTable(@PathVariable Long id) {
        tableService.deleteTable(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa bàn thành công"));
    }
}
