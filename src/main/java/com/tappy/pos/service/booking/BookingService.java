package com.tappy.pos.service.booking;

import com.tappy.pos.model.dto.booking.BookingDTO;
import com.tappy.pos.model.dto.booking.BookingResourceDTO;
import com.tappy.pos.model.dto.booking.BookingResourceRequest;
import com.tappy.pos.model.dto.booking.CreateBookingRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {

    // ── Resources (tables / courts) ──────────────────────────────────────────
    /** All resources, each with its live activeBooking (running session) attached. */
    List<BookingResourceDTO> getResources();

    BookingResourceDTO createResource(BookingResourceRequest request);

    BookingResourceDTO updateResource(Long id, BookingResourceRequest request);

    void deleteResource(Long id);

    // ── Bookings ─────────────────────────────────────────────────────────────
    Page<BookingDTO> getByDate(LocalDate date, String status, Pageable pageable);

    BookingDTO getById(Long id);

    /** Create a reservation (future slot) or start a walk-in timer immediately. */
    BookingDTO create(CreateBookingRequest request);

    /** Reservation RESERVED → IN_PROGRESS (start the timer now). */
    BookingDTO checkIn(Long id);

    /** IN_PROGRESS → COMPLETED: stop timer, bill elapsed time, create the linked POS order. */
    BookingDTO checkout(Long id);

    BookingDTO cancel(Long id);

    BookingDTO noShow(Long id);

    void delete(Long id);
}
