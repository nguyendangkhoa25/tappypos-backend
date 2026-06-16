package com.tappy.pos.service.stocktake;

import com.tappy.pos.model.dto.stocktake.*;
import com.tappy.pos.model.enums.StocktakeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StocktakeService {

    /** Start a session; if one is already IN_PROGRESS, returns that one (resume). */
    StocktakeSessionDTO createSession(CreateStocktakeSessionRequest request);

    Page<StocktakeSessionDTO> listSessions(StocktakeStatus status, Pageable pageable);

    /** Session with its counted lines. */
    StocktakeSessionDTO getSession(Long sessionId);

    /** The current IN_PROGRESS session, or null if none. */
    StocktakeSessionDTO getActiveSession();

    /** Resolve a barcode to a countable product line (expected qty + already counted). */
    StocktakeProductLineDTO lookup(Long sessionId, String barcode);

    /** Add or update the counted quantity for one product. */
    StocktakeCountDTO upsertCount(Long sessionId, UpsertCountRequest request);

    void deleteCount(Long sessionId, Long countId);

    /** Counted lines whose physical count differs from the system. */
    List<StocktakeCountDTO> getDiscrepancies(Long sessionId);

    /** Products with stock that have not yet been counted in this session. */
    List<StocktakeProductLineDTO> getUncounted(Long sessionId);

    /** Apply every counted line to inventory and complete the session. Uncounted left untouched. */
    StocktakeSessionDTO apply(Long sessionId);

    /** Cancel the session — no stock change. */
    StocktakeSessionDTO cancel(Long sessionId);
}
