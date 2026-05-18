package com.tappy.pos.service.table;

import com.tappy.pos.model.dto.table.CreateTableRequest;
import com.tappy.pos.model.dto.table.TableDTO;
import com.tappy.pos.model.dto.table.UpdateTableRequest;

import java.util.List;

public interface TableService {
    List<TableDTO> getTables();
    TableDTO createTable(CreateTableRequest request);
    TableDTO updateTable(Long id, UpdateTableRequest request);
    void deleteTable(Long id);
    void occupyTable(Long tableId, Long orderId);
    void releaseTable(Long tableId);
}
