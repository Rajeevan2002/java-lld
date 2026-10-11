package com.ramkumar.lld.javareadiness.cheatsheet.code;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * Worked Example — Phase 4 Java Readiness | Scenario A: Ride-Sharing Trip Dispatcher
 *
 * Concepts demonstrated (each is labelled inline):
 *   [ENUM-STATE]   enum as a state machine (canTransitionTo, switch expression)
 *   [ENUM-DATA]    enum constants carrying data (RideType fares)
 *   [ENUMMAP]      EnumMap for per-status counts, zero-filled
 *   [RECORD]       record with compact-constructor validation
 *   [ID-GEN]       per-instance counter + String.format zero padding
 *   [REPOSITORY]   LinkedHashMap-backed store, Optional findById, defensive copy
 *   [VALIDATION]   IAE vs ISE vs NoSuchElementException
 *   [COMPARATOR]   Comparator chaining + streams
 *   [PQ]           PriorityQueue for dispatch order
 */
public class TripDispatchDemo {

    // [ENUM-STATE] Transition rules live ON the enum — one source of truth.
    enum TripStatus {
        REQUESTED, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED;

        boolean canTransitionTo(TripStatus next) {
            // [ENUM-STATE] switch expression: no fall-through, compiler enforces exhaustiveness
            return switch (this) {
                case REQUESTED -> next == ASSIGNED || next == CANCELLED;
                case ASSIGNED -> next == IN_PROGRESS || next == CANCELLED;
                case IN_PROGRESS -> next == COMPLETED;
                case COMPLETED, CANCELLED -> false;   // terminal states
            };
        }
    }

    // [ENUM-DATA] Declaration order = dispatch priority (PREMIUM first) — used by the PriorityQueue below.
    enum RideType {
        PREMIUM(5.00, 2.20),
        STANDARD(2.50, 1.20);

        private final double baseFare;
        private final double perKm;

        RideType(double baseFare, double perKm) {
            this.baseFare = baseFare;
            this.perKm = perKm;
        }

        double fare(double km) {
            return baseFare + perKm * km;
        }
    }

    // [RECORD] Immutable value object; the compact constructor runs before fields are assigned.
    record Rider(String id, String name) {
        Rider {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("rider id must not be blank");
            if (name == null || name.isBlank()) throw new IllegalArgumentException("rider name must not be blank");
        }
    }

    static final class Trip {
        private final String id;            // immutable identity
        private final Rider rider;
        private final RideType rideType;
        private final double distanceKm;
        private TripStatus status;          // the ONLY mutable field

        Trip(String id, Rider rider, RideType rideType, double distanceKm) {
            this.id = id;
            this.rider = rider;
            this.rideType = rideType;
            this.distanceKm = distanceKm;
            this.status = TripStatus.REQUESTED;
        }

        String getId() { return id; }
        Rider getRider() { return rider; }
        RideType getRideType() { return rideType; }
        TripStatus getStatus() { return status; }
        double fare() { return rideType.fare(distanceKm); }

        // package-private: only the dispatcher (same outer class) may change status
        void setStatus(TripStatus s) { this.status = s; }

        @Override
        public String toString() {
            return String.format("%s[%s, %s, %.1fkm, %s]", id, rider.name(), rideType, distanceKm, status);
        }
    }

    static final class TripDispatcher {
        // [REPOSITORY] LinkedHashMap keeps creation order => deterministic demo output
        private final Map<String, Trip> store = new LinkedHashMap<>();
        // [ID-GEN] instance counter (NOT static): each dispatcher numbers its own trips from 1
        private int counter = 0;

        Trip request(Rider rider, RideType type, double km) {
            // [VALIDATION] cheap null checks first, then range checks
            if (rider == null) throw new IllegalArgumentException("rider must not be null");
            if (type == null) throw new IllegalArgumentException("rideType must not be null");
            if (km <= 0) throw new IllegalArgumentException("distanceKm must be > 0");

            String id = String.format("TRP-%03d", ++counter);          // [ID-GEN] zero-padded
            Trip trip = new Trip(id, rider, type, km);
            store.put(id, trip);
            System.out.printf("[Dispatcher] requested %s%n", trip);
            return trip;
        }

        // [REPOSITORY] Absence is a normal outcome of a query -> Optional, not null, not exception
        Optional<Trip> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        // [REPOSITORY] List.copyOf = immutable snapshot; caller cannot touch our state
        List<Trip> findAll() {
            return List.copyOf(store.values());
        }

        void transition(String id, TripStatus next) {
            // [VALIDATION] a command on a missing target is a caller bug -> fail fast
            Trip trip = findById(id).orElseThrow(() -> new NoSuchElementException("Trip not found: " + id));
            if (!trip.getStatus().canTransitionTo(next)) {
                // [VALIDATION] argument is fine, object state forbids it -> ISE
                throw new IllegalStateException("Cannot move from " + trip.getStatus() + " to " + next);
            }
            trip.setStatus(next);
            System.out.printf("[Dispatcher] %s -> %s%n", id, next);
        }

        // [ENUMMAP] zero-filled so the report always lists every status
        Map<TripStatus, Long> countByStatus() {
            Map<TripStatus, Long> counts = new EnumMap<>(TripStatus.class);
            for (TripStatus s : TripStatus.values()) counts.put(s, 0L);
            for (Trip t : store.values()) counts.merge(t.getStatus(), 1L, Long::sum);
            return counts;
        }

        // [COMPARATOR] + [STREAM] highest fare first, ties broken by id
        List<Trip> completedByFareDesc() {
            return store.values().stream()
                    .filter(t -> t.getStatus() == TripStatus.COMPLETED)
                    .sorted(Comparator.comparingDouble(Trip::fare).reversed().thenComparing(Trip::getId))
                    .toList();
        }

        // [PQ] enum natural order (declaration order) then id: PREMIUM trips are dispatched first
        PriorityQueue<Trip> dispatchQueue() {
            PriorityQueue<Trip> pq = new PriorityQueue<>(
                    Comparator.comparing(Trip::getRideType).thenComparing(Trip::getId));
            store.values().stream().filter(t -> t.getStatus() == TripStatus.REQUESTED).forEach(pq::offer);
            return pq;
        }

        // [STREAM] groupingBy with an EnumMap factory
        Map<RideType, Long> tripsPerRideType() {
            return store.values().stream().collect(Collectors.groupingBy(
                    Trip::getRideType, () -> new EnumMap<>(RideType.class), Collectors.counting()));
        }
    }

    public static void main(String[] args) {
        TripDispatcher d = new TripDispatcher();

        System.out.println("── 1. Requests + ID generation ──");
        Rider asha = new Rider("R1", "Asha");
        Rider ben = new Rider("R2", "Ben");
        d.request(asha, RideType.STANDARD, 10);   // TRP-001
        d.request(ben, RideType.PREMIUM, 4);      // TRP-002
        d.request(asha, RideType.STANDARD, 25);   // TRP-003

        System.out.println("── 2. State machine ──");
        d.transition("TRP-001", TripStatus.ASSIGNED);
        d.transition("TRP-001", TripStatus.IN_PROGRESS);
        d.transition("TRP-001", TripStatus.COMPLETED);
        d.transition("TRP-003", TripStatus.CANCELLED);
        try {
            d.transition("TRP-001", TripStatus.CANCELLED);
        } catch (IllegalStateException e) {
            System.out.println("Rejected: " + e.getMessage());     // Cannot move from COMPLETED to CANCELLED
        }

        System.out.println("── 3. Optional lookup ──");
        System.out.println(d.findById("TRP-002").map(Trip::toString).orElse("none"));
        System.out.println(d.findById("TRP-999").map(Trip::toString).orElse("none"));
        try {
            d.transition("TRP-999", TripStatus.ASSIGNED);
        } catch (NoSuchElementException e) {
            System.out.println("Rejected: " + e.getMessage());
        }

        System.out.println("── 4. EnumMap counts ──");
        System.out.println(d.countByStatus());     // {REQUESTED=1, ASSIGNED=0, IN_PROGRESS=0, COMPLETED=1, CANCELLED=1}

        System.out.println("── 5. Comparator + stream ──");
        d.completedByFareDesc().forEach(t -> System.out.printf("%s fare=%.2f%n", t.getId(), t.fare()));

        System.out.println("── 6. PriorityQueue dispatch order ──");
        d.request(asha, RideType.STANDARD, 3);    // TRP-004
        PriorityQueue<Trip> q = d.dispatchQueue();
        while (!q.isEmpty()) System.out.println("next: " + q.poll().getId());   // TRP-002 then TRP-004

        System.out.println("── 7. Defensive copy ──");
        try {
            d.findAll().add(null);
        } catch (UnsupportedOperationException e) {
            System.out.println("findAll() is immutable");
        }
        System.out.println(d.tripsPerRideType());  // {PREMIUM=1, STANDARD=3}
    }
}
