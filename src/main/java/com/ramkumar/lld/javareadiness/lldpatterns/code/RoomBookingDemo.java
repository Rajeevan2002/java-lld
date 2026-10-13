package com.ramkumar.lld.javareadiness.lldpatterns.code;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * Worked Example — Phase 4 Java Readiness | 4.2 Recurring LLD Patterns
 * Scenario A: Meeting Room Booking
 *
 * Patterns demonstrated (labelled inline):
 *   [VALIDATE]   constructor validation, cheap checks first, exact exception types
 *   [ID-GEN]     per-instance counter + String.format zero padding, no burned ids
 *   [REPOSITORY] LinkedHashMap store, Optional findById, putIfAbsent, defensive copy
 *   [EXCEPTIONS] IAE vs NPE vs ISE vs NoSuchElementException
 *   [LOGGING]    "[Service] action: detail" via printf
 */
public class RoomBookingDemo {

    // ── Entity ─────────────────────────────────────────────────────
    static class Booking {
        private final String id;            // immutable identity
        private final String organiser;
        private final int minutes;
        private boolean cancelled;          // the only mutable state

        Booking(String id, String organiser, int minutes) {
            // [VALIDATE] order = order of the spec; assign only after all checks pass
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("id must not be blank");
            }
            // [EXCEPTIONS] required reference missing -> NullPointerException with a message
            Objects.requireNonNull(organiser, "organiser must not be null");
            if (organiser.isBlank()) {
                throw new IllegalArgumentException("organiser must not be blank");
            }
            if (minutes <= 0) {
                throw new IllegalArgumentException("minutes must be > 0");
            }
            this.id = id;
            this.organiser = organiser;
            this.minutes = minutes;
        }

        String getId() { return id; }
        String getOrganiser() { return organiser; }
        boolean isCancelled() { return cancelled; }
        void cancel() { cancelled = true; }

        @Override
        public String toString() {
            // [LOGGING] literal "min" and brackets are typed by us, not added by format
            return String.format("%s[%s, %dmin, %s]", id, organiser, minutes, cancelled ? "CANCELLED" : "ACTIVE");
        }
    }

    // ── Repository: storage only, no business rules ────────────────
    static class BookingRepository {
        private final Map<String, Booking> store = new LinkedHashMap<>();   // [REPOSITORY] keeps insertion order

        void save(Booking b) {
            Objects.requireNonNull(b, "booking must not be null");
            // putIfAbsent: one lookup instead of containsKey + put
            if (store.putIfAbsent(b.getId(), b) != null) {
                throw new IllegalArgumentException("duplicate id: " + b.getId());
            }
        }

        // [REPOSITORY] "missing" is a normal outcome here, so Optional, not null / exception
        Optional<Booking> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        // [REPOSITORY] snapshot copy: caller can neither mutate nor corrupt our map
        List<Booking> findAll() {
            return List.copyOf(store.values());
        }
    }

    // ── Service: rules, ids, logging ───────────────────────────────
    static class BookingService {
        private final BookingRepository repo = new BookingRepository();
        private int counter;                                  // [ID-GEN] per instance, NOT static

        Booking book(String organiser, int minutes) {
            // [ID-GEN] candidate id first; Booking's constructor may throw, and then
            // the counter must NOT advance (no burned ids)
            String id = String.format("BKG-%03d", counter + 1);
            Booking b = new Booking(id, organiser, minutes);
            counter++;
            repo.save(b);
            System.out.printf("[BookingService] booked %s for %s%n", id, organiser);   // [LOGGING]
            return b;
        }

        Booking cancel(String id) {
            // [EXCEPTIONS] service decides "missing" is an error -> NoSuchElementException
            Booking b = repo.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("booking not found: " + id));
            // [EXCEPTIONS] argument is fine, object state forbids it -> IllegalStateException
            if (b.isCancelled()) {
                throw new IllegalStateException("booking already cancelled: " + id);
            }
            b.cancel();
            System.out.printf("[BookingService] cancelled %s%n", id);
            return b;
        }

        List<Booking> active() {
            return repo.findAll().stream().filter(b -> !b.isCancelled()).toList();   // unmodifiable
        }
    }

    public static void main(String[] args) {
        BookingService svc = new BookingService();

        System.out.println("=== [ID-GEN] ===");
        System.out.println(svc.book("Asha", 30));                  // BKG-001[Asha, 30min, ACTIVE]
        System.out.println(svc.book("Ben", 60));                   // BKG-002[Ben, 60min, ACTIVE]

        System.out.println("\n=== [VALIDATE] + burned id check ===");
        try { svc.book("Cy", 0); } catch (IllegalArgumentException e) { System.out.println("IAE: " + e.getMessage()); }
        try { svc.book(null, 10); } catch (NullPointerException e) { System.out.println("NPE: " + e.getMessage()); }
        System.out.println(svc.book("Dee", 15).getId());           // BKG-003 (failed calls did not burn ids)

        System.out.println("\n=== [EXCEPTIONS] ===");
        svc.cancel("BKG-001");
        try { svc.cancel("BKG-001"); } catch (IllegalStateException e) { System.out.println("ISE: " + e.getMessage()); }
        try { svc.cancel("BKG-099"); } catch (NoSuchElementException e) { System.out.println("NSEE: " + e.getMessage()); }

        System.out.println("\n=== [REPOSITORY] defensive copy ===");
        List<Booking> active = svc.active();
        System.out.println(active);                                // [BKG-002..., BKG-003...]
        try { active.add(null); } catch (UnsupportedOperationException e) { System.out.println("unmodifiable"); }
    }
}
