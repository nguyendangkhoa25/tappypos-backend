package com.tappy.pos.service.table;

import com.tappy.pos.model.dto.table.CreateTableRequest;
import com.tappy.pos.model.dto.table.CreateTableReservationRequest;
import com.tappy.pos.model.dto.table.SetTableStatusRequest;
import com.tappy.pos.model.dto.table.TableDTO;
import com.tappy.pos.model.dto.table.TableReservationDTO;
import com.tappy.pos.model.dto.table.UpdateTableRequest;

import java.time.LocalDate;
import java.util.List;

public interface TableService {
    List<TableDTO> getTables();
    TableDTO createTable(CreateTableRequest request);
    TableDTO updateTable(Long id, UpdateTableRequest request);
    void deleteTable(Long id);
    void occupyTable(Long tableId, Long orderId);
    void releaseTable(Long tableId);
    /** Staff-facing: set a table to RESERVED (with optional name+time), CLEANING, or AVAILABLE. */
    TableDTO setStatus(Long tableId, SetTableStatusRequest request);

    // ── Advance reservation calendar (đặt bàn trước) ───────────────────────────
    TableReservationDTO createReservation(CreateTableReservationRequest request);
    List<TableReservationDTO> listReservations(LocalDate from, LocalDate to);
    /** Guest arrived: mark the reservation SEATED and flag the table RESERVED if it's free. */
    TableReservationDTO seatReservation(Long reservationId);
    TableReservationDTO cancelReservation(Long reservationId);
    TableReservationDTO markReservationNoShow(Long reservationId);
}
