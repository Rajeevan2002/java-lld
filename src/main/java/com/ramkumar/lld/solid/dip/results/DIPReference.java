package com.ramkumar.lld.solid.dip.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reference solution — Dependency Inversion Principle (DIP)
 * Phase 2, Topic 5 | Scenario B: Ride-Hailing Trip Service
 *
 * Fixes from the practice review:
 *   Issue 1: nextTripId() returned "[TRIP-001]" (brackets) → fixed to "TRIP-001"
 *   Issue 2: tripCounter was static (shared across instances) → fixed to instance field
 *   Issue 3: completeTrip/cancelTrip skipped tripRepository.save() after status change → added
 *   Issue 4: cancelTrip called notify() before setStatus() → order fixed to: set → save → notify
 *   Issue 5: fareAmount <= 0 rejected 0 (spec says >= 0 valid) → fixed to fareAmount < 0
 *   Issue 6: nextTripId() was public → made private
 *   Bonus:   bookTrip() was missing riderNotifier.notify() call → added
 */
public class DIPReference {

    // =========================================================================
    // STEP 1 — DIP VIOLATION: concrete classes (low-level details)
    // =========================================================================

    /** Concrete SMS client — low-level detail */
    static class InternalSmsClient {
        public void notify(String riderId, String message) {
            System.out.println("[InternalSMS] To: " + riderId + " | " + message);
        }
    }

    /** Concrete MySQL store — low-level detail */
    static class InternalMySqlTripStore {
        public void save(String tripId, String riderId, String pickup, String drop) {
            System.out.println("[MySQLStore] Saving trip [" + tripId + "]: rider=" + riderId
                    + " | " + pickup + " → " + drop);
        }
    }

    /**
     * ❌ DIP VIOLATED: ViolatingTripService creates its own concrete dependencies.
     * You cannot swap MySQL for InMemory or SMS for Email without editing this class.
     * You cannot test this class without a real MySQL server and SMS gateway.
     */
    static class ViolatingTripService {

        // ← DIP VIOLATION: concrete types as fields (not interfaces)
        private final InternalSmsClient      smsClient;   // KEY: these should be final
        private final InternalMySqlTripStore tripStore;

        public ViolatingTripService() {
            // ← DIP VIOLATION: high-level module creates its own low-level dependencies
            this.smsClient = new InternalSmsClient();
            this.tripStore = new InternalMySqlTripStore();
        }

        public void bookTrip(String riderId, String pickup, String drop) {
            System.out.println("[ViolatingTripService] Booking trip for " + riderId);
            tripStore.save("TRIP-V01", riderId, pickup, drop);
            smsClient.notify(riderId, "Your trip has been booked");
        }
    }

    // =========================================================================
    // Abstractions — owned by the high-level (domain) layer
    // Low-level modules implement these; they do NOT define them
    // =========================================================================

    /** The "inversion": low-level repositories implement THIS interface — not vice versa */
    interface TripRepository {
        void             save(Trip trip);
        Optional<Trip>   findById(String tripId);
        List<Trip>       findByRider(String riderId);
    }

    /** Any notification mechanism: SMS, email, push — high-level module doesn't care */
    interface RiderNotifier {
        void notify(String riderId, String message);
    }

    /** Any fare computation strategy: standard, surge, fixed — injected, not hardcoded */
    interface FareCalculator {
        double calculate(String pickupLocation, String dropLocation);
    }

    // =========================================================================
    // Value types
    // =========================================================================

    enum TripStatus { PENDING, COMPLETED, CANCELLED }

    static class Trip {
        private final String     tripId;
        private final String     riderId;
        private final String     pickupLocation;
        private final String     dropLocation;
        private final double     fareAmount;    // immutable — set once at booking
        private       TripStatus status;        // mutable — transitions: PENDING → COMPLETED/CANCELLED

        public Trip(String tripId, String riderId,
                    String pickupLocation, String dropLocation, double fareAmount) {
            if (tripId         == null || tripId.isBlank())         throw new IllegalArgumentException("tripId cannot be blank");
            if (riderId        == null || riderId.isBlank())        throw new IllegalArgumentException("riderId cannot be blank");
            if (pickupLocation == null || pickupLocation.isBlank()) throw new IllegalArgumentException("pickupLocation cannot be blank");
            if (dropLocation   == null || dropLocation.isBlank())   throw new IllegalArgumentException("dropLocation cannot be blank");
            // KEY FIX 5: spec says fareAmount >= 0; 0 is valid (promotional free ride)
            if (fareAmount < 0) throw new IllegalArgumentException("fareAmount cannot be negative");
            this.tripId         = tripId;
            this.riderId        = riderId;
            this.pickupLocation = pickupLocation;
            this.dropLocation   = dropLocation;
            this.fareAmount     = fareAmount;
            this.status         = TripStatus.PENDING;
        }

        public String     getTripId()         { return tripId; }
        public String     getRiderId()        { return riderId; }
        public String     getPickupLocation() { return pickupLocation; }
        public String     getDropLocation()   { return dropLocation; }
        public double     getFareAmount()     { return fareAmount; }
        public TripStatus getStatus()         { return status; }
        public void       setStatus(TripStatus s) {
            if (s == null) throw new IllegalArgumentException("status cannot be null");
            this.status = s;
        }

        public String describe() {
            return String.format("Trip[%s, rider=%s, %s→%s, fare=₹%.1f, %s]",
                    tripId, riderId, pickupLocation, dropLocation, fareAmount, status);
        }
    }

    // =========================================================================
    // TripService — HIGH-LEVEL MODULE (the heart of DIP)
    // Depends ONLY on abstractions. Never calls `new` for its dependencies.
    // =========================================================================

    static class TripService {

        // DIP: interface fields — not concrete types
        private final TripRepository tripRepository;
        private final RiderNotifier  riderNotifier;
        private final FareCalculator fareCalculator;

        // KEY FIX 2: instance field — each TripService has its own independent counter
        // Not static: static would share state across all TripService instances
        private int tripCounter = 0;

        public TripService(TripRepository tripRepository,
                           RiderNotifier  riderNotifier,
                           FareCalculator fareCalculator) {
            // Null-check at the DIP boundary: if any dependency is null, fail immediately
            if (tripRepository == null) throw new IllegalArgumentException("tripRepository cannot be null");
            if (riderNotifier  == null) throw new IllegalArgumentException("riderNotifier cannot be null");
            if (fareCalculator == null) throw new IllegalArgumentException("fareCalculator cannot be null");
            // No `new` anywhere in this constructor — DIP satisfied
            this.tripRepository = tripRepository;
            this.riderNotifier  = riderNotifier;
            this.fareCalculator = fareCalculator;
        }

        // KEY FIX 1: private (not public), no brackets: "TRIP-001" not "[TRIP-001]"
        // KEY FIX 2: uses instance field tripCounter (not static)
        private String nextTripId() {
            return String.format("TRIP-%03d", ++tripCounter);
        }

        public Trip bookTrip(String riderId, String pickup, String drop) {
            if (riderId == null || riderId.isBlank()) throw new IllegalArgumentException("riderId cannot be blank");
            if (pickup  == null || pickup.isBlank())  throw new IllegalArgumentException("pickup cannot be blank");
            if (drop    == null || drop.isBlank())    throw new IllegalArgumentException("drop cannot be blank");

            // DIP: calls fareCalculator interface — doesn't know if it's Standard or Fixed
            double fare   = fareCalculator.calculate(pickup, drop);
            String tripId = nextTripId();
            Trip   trip   = new Trip(tripId, riderId, pickup, drop, fare);

            // DIP: calls repository interface — doesn't know if it's InMemory or MySQL
            tripRepository.save(trip);

            // Bonus fix: notify rider on booking (was missing in practice solution)
            riderNotifier.notify(riderId,
                String.format("Trip booked: %s | Fare: \u20b9%.1f", tripId, fare));

            return trip;
        }

        public Trip completeTrip(String tripId) {
            if (tripId == null || tripId.isBlank())
                throw new IllegalArgumentException("tripId cannot be blank");

            Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

            // KEY FIX 4: order is set → save → notify (not notify then set)
            trip.setStatus(TripStatus.COMPLETED);
            tripRepository.save(trip);   // KEY FIX 3: explicit save after status change
            riderNotifier.notify(trip.getRiderId(), "Trip completed: " + tripId);
            return trip;
        }

        public Trip cancelTrip(String tripId) {
            if (tripId == null || tripId.isBlank())
                throw new IllegalArgumentException("tripId cannot be blank");

            Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

            // KEY FIX 4: setStatus BEFORE notify — state committed before observable side effect
            trip.setStatus(TripStatus.CANCELLED);
            tripRepository.save(trip);   // KEY FIX 3: persist the status change
            riderNotifier.notify(trip.getRiderId(), "Trip cancelled: " + tripId);
            return trip;
        }

        public List<Trip> getRiderHistory(String riderId) {
            if (riderId == null || riderId.isBlank())
                throw new IllegalArgumentException("riderId cannot be blank");
            return tripRepository.findByRider(riderId);
        }
    }

    // =========================================================================
    // Low-level implementations — depend on (implement) abstractions
    // This is the "inversion": low-level modules now depend on high-level interfaces
    // =========================================================================

    static class InMemoryTripRepository implements TripRepository {

        private final Map<String, Trip> store = new HashMap<>();

        @Override
        public void save(Trip trip) {
            store.put(trip.getTripId(), trip);
            System.out.println("[InMemoryRepo] Saved: " + trip.describe());
        }

        @Override
        public Optional<Trip> findById(String tripId) {
            // KEY FIX 7: single lookup — Optional.ofNullable handles both found and not-found
            return Optional.ofNullable(store.get(tripId));
        }

        @Override
        public List<Trip> findByRider(String riderId) {
            List<Trip> result = new ArrayList<>();
            for (Trip t : store.values()) {
                if (t.getRiderId().equals(riderId)) result.add(t);
            }
            return Collections.unmodifiableList(result);
        }
    }

    static class SmsRiderNotifier implements RiderNotifier {
        @Override
        public void notify(String riderId, String message) {
            System.out.println("[SMS \u2192 " + riderId + "]: " + message);
        }
    }

    static class EmailRiderNotifier implements RiderNotifier {
        @Override
        public void notify(String riderId, String message) {
            System.out.println("[EMAIL \u2192 " + riderId + "]: " + message);
        }
    }

    static class StandardFareCalculator implements FareCalculator {
        private static final double BASE_FARE    = 50.0;
        private static final double PER_KM_RATE  = 12.0;

        @Override
        public double calculate(String pickup, String drop) {
            int distance = Math.abs(drop.length() - pickup.length()) + 5;
            return BASE_FARE + (distance * PER_KM_RATE);
        }
    }

    static class FixedFareCalculator implements FareCalculator {
        private final double fixedFare;

        public FixedFareCalculator(double fixedFare) { this.fixedFare = fixedFare; }

        @Override
        public double calculate(String pickup, String drop) {
            return fixedFare;   // ignores route — always returns the fixed amount
        }
    }

    // =========================================================================
    // Main — same 11 tests as practice + Test 12 (catches the bracket/static bugs)
    // =========================================================================
    public static void main(String[] args) {

        System.out.println("═══ Test 1: DIP Violation — ViolatingTripService ════════");
        ViolatingTripService violator = new ViolatingTripService();
        violator.bookTrip("RIDER-V1", "Airport", "Hotel");
        System.out.println("Test 1 PASSED — ViolatingTripService shows DIP violation");

        System.out.println("\n═══ Test 2: bookTrip — trip created and saved ═══════════");
        TripRepository repo     = new InMemoryTripRepository();
        RiderNotifier  notifier = new SmsRiderNotifier();
        FareCalculator calc     = new StandardFareCalculator();
        TripService    service  = new TripService(repo, notifier, calc);

        Trip t1 = service.bookTrip("RIDER-001", "Airport", "Hotel");
        System.out.println("Trip: " + t1.describe());
        System.out.println("tripId starts with TRIP-: " + t1.getTripId().startsWith("TRIP-"));
        System.out.println("Status PENDING: " + (t1.getStatus() == TripStatus.PENDING));
        System.out.println("fare > 0: " + (t1.getFareAmount() > 0));
        System.out.println("Test 2 PASSED: " + (t1.getTripId().startsWith("TRIP-")
                && t1.getStatus() == TripStatus.PENDING
                && t1.getFareAmount() > 0));

        System.out.println("\n═══ Test 3: bookTrip — tripId zero-padded (TRIP-001) ════");
        System.out.println("tripId: " + t1.getTripId());
        boolean t3 = t1.getTripId().matches("TRIP-\\d{3}");
        System.out.println("Test 3 PASSED: " + t3);

        System.out.println("\n═══ Test 4: completeTrip — status changes to COMPLETED ══");
        service.completeTrip(t1.getTripId());
        System.out.println("Status after complete: " + t1.getStatus());
        System.out.println("Test 4 PASSED: " + (t1.getStatus() == TripStatus.COMPLETED));

        System.out.println("\n═══ Test 5: cancelTrip — status changes to CANCELLED ════");
        Trip t2 = service.bookTrip("RIDER-001", "Mall", "Home");
        service.cancelTrip(t2.getTripId());
        System.out.println("Status after cancel: " + t2.getStatus());
        System.out.println("Test 5 PASSED: " + (t2.getStatus() == TripStatus.CANCELLED));

        System.out.println("\n═══ Test 6: getRiderHistory — returns all trips ══════════");
        Trip t3trip = service.bookTrip("RIDER-001", "Office", "Station");
        List<Trip> history = service.getRiderHistory("RIDER-001");
        System.out.println("RIDER-001 trip count: " + history.size());
        System.out.println("Test 6 PASSED: " + (history.size() == 3));

        System.out.println("\n═══ Test 7: unmodifiable history list ═══════════════════");
        try {
            history.add(null);
            System.out.println("Test 7 FAILED — list should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            System.out.println("Test 7 PASSED — getRiderHistory() returns unmodifiable list");
        }

        System.out.println("\n═══ Test 8: DIP proof — swap FareCalculator (FixedFare) ══");
        FareCalculator fixedCalc    = new FixedFareCalculator(100.0);
        TripService    fixedService = new TripService(repo, notifier, fixedCalc);
        Trip           fixedTrip   = fixedService.bookTrip("RIDER-002", "Anywhere", "Somewhere");
        System.out.println("Fixed fare trip: " + fixedTrip.describe());
        System.out.println("fare == 100.0: " + (fixedTrip.getFareAmount() == 100.0));
        System.out.println("Test 8 PASSED: " + (fixedTrip.getFareAmount() == 100.0));

        System.out.println("\n═══ Test 9: DIP proof — swap RiderNotifier (Email) ══════");
        RiderNotifier emailNotifier = new EmailRiderNotifier();
        TripService   emailService  = new TripService(repo, emailNotifier, calc);
        Trip          emailTrip     = emailService.bookTrip("RIDER-003", "Station", "Airport");
        System.out.println("Trip via email notifier: " + emailTrip.getTripId());
        System.out.println("Test 9 PASSED — TripService unchanged, notifier swapped");

        System.out.println("\n═══ Test 10: completeTrip on unknown ID — throws ═════════");
        try {
            service.completeTrip("TRIP-999");
            System.out.println("Test 10 FAILED — should have thrown");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
            System.out.println("Test 10 PASSED — unknown tripId throws IllegalArgumentException");
        }

        System.out.println("\n═══ Test 11: null dependency in TripService — throws ═════");
        try {
            new TripService(null, notifier, calc);
            System.out.println("Test 11 FAILED — should have thrown for null repo");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
            System.out.println("Test 11 PASSED — null dependency rejected at construction");
        }

        // ── Test 12 (Extra) — catches Issues 1 & 2: bracket in ID + static counter ─────────
        // If nextTripId() returns "[TRIP-001]" (with brackets):
        //   startsWith("TRIP-") → false
        //   matches("TRIP-\\d{3}") → false
        // If tripCounter is static (shared), a second TripService's first ID will NOT be "TRIP-001".
        System.out.println("\n═══ Test 12 (Extra): ID format is TRIP-NNN, no brackets; counter resets ═");
        TripService freshService = new TripService(
                new InMemoryTripRepository(), new SmsRiderNotifier(), new StandardFareCalculator());
        Trip freshTrip = freshService.bookTrip("RIDER-NEW", "North", "South");
        System.out.println("Fresh service first tripId: " + freshTrip.getTripId());

        // KEY FIX 1: no brackets — must be exactly "TRIP-001"
        boolean noSquareBrackets = !freshTrip.getTripId().contains("[")
                                && !freshTrip.getTripId().contains("]");
        System.out.println("No brackets in ID: " + noSquareBrackets);

        // KEY FIX 2: instance counter (not static) — fresh service starts at TRIP-001
        boolean counterReset = "TRIP-001".equals(freshTrip.getTripId());
        System.out.println("Fresh service starts at TRIP-001: " + counterReset);

        System.out.println("Test 12 PASSED: " + (noSquareBrackets && counterReset));
        if (!counterReset) {
            System.out.println("Test 12 FAILED — tripCounter is static (shared across instances)");
            System.out.println("  Fix: change `private static int tripCounter` to `private int tripCounter`");
        }
        if (!noSquareBrackets) {
            System.out.println("Test 12 FAILED — tripId contains square brackets");
            System.out.println("  Fix: change `[TRIP-%03d]` to `TRIP-%03d` in nextTripId()");
        }
    }
}
