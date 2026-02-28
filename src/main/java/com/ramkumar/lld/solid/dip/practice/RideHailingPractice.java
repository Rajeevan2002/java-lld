package com.ramkumar.lld.solid.dip.practice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Practice Exercise — Dependency Inversion Principle (DIP)
 * Phase 2, Topic 5 | Scenario B: Ride-Hailing Trip Service
 *
 * ═══════════════════════════════════════════════════════════════════════
 * PROBLEM STATEMENT
 * ═══════════════════════════════════════════════════════════════════════
 *
 * You are building a ride-hailing backend (think Uber/Ola). The core of
 * the system is TripService — the HIGH-LEVEL module that handles business
 * logic for booking, completing, and cancelling trips.
 *
 * Your job is to design TripService so it NEVER creates its own
 * dependencies. All dependencies are abstractions (interfaces) supplied
 * via constructor injection.
 *
 * ── Part 1: Show the DIP Violation ─────────────────────────────────────
 *
 * Create a ViolatingTripService that:
 *   - Creates its own InternalSmsClient (a concrete class you define)
 *     and InternalMySqlTripStore (a concrete class you define) in its constructor
 *   - Has a bookTrip(String riderId, String pickup, String drop) method
 *     that calls both internally to save + notify
 *   This is the violation: ViolatingTripService is hardwired to specific
 *   implementations it cannot change or test.
 *
 * ── Part 2: Fix with Abstractions + Constructor Injection ───────────────
 *
 * Define three abstractions (interfaces — these belong to the high-level layer):
 *
 *   TripRepository
 *     - void save(Trip trip)
 *     - Optional<Trip> findById(String tripId)
 *     - List<Trip> findByRider(String riderId)
 *
 *   RiderNotifier
 *     - void notify(String riderId, String message)
 *
 *   FareCalculator
 *     - double calculate(String pickupLocation, String dropLocation)
 *     Returns a non-negative fare in rupees for the given route.
 *
 * Define TripStatus enum:
 *   PENDING, COMPLETED, CANCELLED
 *
 * Define Trip value object:
 *   Fields (all private):
 *     - String tripId        (immutable — final)
 *     - String riderId       (immutable — final)
 *     - String pickupLocation (immutable — final)
 *     - String dropLocation   (immutable — final)
 *     - double fareAmount     (immutable — final, calculated at booking)
 *     - TripStatus status    (mutable — starts as PENDING)
 *   Constructor: Trip(String tripId, String riderId, String pickupLocation,
 *                     String dropLocation, double fareAmount)
 *     Validates: tripId, riderId, pickupLocation, dropLocation not null/blank
 *                fareAmount >= 0
 *   Methods:
 *     - Getters for all fields
 *     - void setStatus(TripStatus status) — allows TripService to update status
 *     - String describe() — returns:
 *       "Trip[<tripId>, rider=<riderId>, <pickup>→<drop>, fare=₹<amount>, <status>]"
 *       (use String.format with %.1f for fare)
 *
 * Define TripService (HIGH-LEVEL MODULE — the most important class):
 *   Fields (all private final):
 *     - TripRepository tripRepository   (interface — injected)
 *     - RiderNotifier  riderNotifier    (interface — injected)
 *     - FareCalculator fareCalculator   (interface — injected)
 *     - int            tripCounter      (for generating trip IDs, start at 0)
 *   Constructor: TripService(TripRepository, RiderNotifier, FareCalculator)
 *     - Validates all three are not null (throws IllegalArgumentException)
 *     - NEVER creates its own implementations
 *   Private helper: String nextTripId() — returns "TRIP-%03d" formatted with ++tripCounter
 *   Methods:
 *     bookTrip(String riderId, String pickup, String drop) : Trip
 *       - Validates riderId, pickup, drop are not null/blank
 *       - Calculates fare via fareCalculator.calculate(pickup, drop)
 *       - Creates Trip with nextTripId(), riderId, pickup, drop, fare
 *       - Saves via tripRepository.save(trip)
 *       - Notifies via riderNotifier.notify(riderId, "Trip booked: <tripId> | Fare: ₹<fare>")
 *         (fare formatted with %.1f)
 *       - Returns the saved trip
 *     completeTrip(String tripId) : Trip
 *       - Finds trip via tripRepository.findById(tripId)
 *       - Throws IllegalArgumentException("Trip not found: " + tripId) if absent
 *       - Sets status to COMPLETED, saves, notifies rider ("Trip completed: <tripId>")
 *       - Returns the updated trip
 *     cancelTrip(String tripId) : Trip
 *       - Same pattern as completeTrip but sets status to CANCELLED
 *       - Notifies rider: "Trip cancelled: <tripId>"
 *     getRiderHistory(String riderId) : List<Trip>
 *       - Validates riderId not null/blank
 *       - Returns tripRepository.findByRider(riderId)
 *
 * Create three low-level implementations:
 *
 *   InMemoryTripRepository implements TripRepository
 *     - Use a Map<String, Trip> for storage
 *     - findByRider: iterate map values, filter by riderId
 *     - findByRider returns unmodifiable list
 *     - save: print "[InMemoryRepo] Saved: <trip.describe()>"
 *
 *   SmsRiderNotifier implements RiderNotifier
 *     - notify: prints "[SMS → <riderId>]: <message>"
 *
 *   StandardFareCalculator implements FareCalculator
 *     - BASE_FARE = 50.0, PER_KM_RATE = 12.0
 *     - Simulated distance: Math.abs(drop.length() - pickup.length()) + 5
 *     - fare = BASE_FARE + (distance * PER_KM_RATE)
 *     - Returns the computed fare (never negative)
 *
 * DIP PROOF — also create:
 *   FixedFareCalculator implements FareCalculator
 *     - Constructor: FixedFareCalculator(double fixedFare)
 *     - calculate() always returns fixedFare regardless of locations
 *     - Used in tests to verify TripService can work with any FareCalculator
 *
 *   EmailRiderNotifier implements RiderNotifier
 *     - notify: prints "[EMAIL → <riderId>]: <message>"
 *     - Used in tests to prove TripService works with any RiderNotifier
 *
 * DESIGN CONSTRAINTS:
 *   1. TripService must NEVER use `new` for TripRepository, RiderNotifier, or FareCalculator
 *   2. TripService fields for dependencies must be final interfaces (not concrete types)
 *   3. Trip.tripId must be formatted as "TRIP-%03d" (zero-padded 3 digits)
 *   4. All TripService constructor arguments validated as non-null
 *   5. completeTrip/cancelTrip throw IllegalArgumentException for unknown tripId
 *   6. InMemoryTripRepository.findByRider() returns unmodifiable list
 *
 * ═══════════════════════════════════════════════════════════════════════
 * DO NOT MODIFY the main() method — fill in the TODOs to make tests pass
 * ═══════════════════════════════════════════════════════════════════════
 */
public class RideHailingPractice {

    // =========================================================================
    // ── TODO 1: Define InternalSmsClient (concrete class — DIP violation)
    //            notify(String riderId, String message) — prints "[InternalSMS] ..."
    // =========================================================================
    static class InternalSmsClient {
        public void notify(String riderId, String message){
            System.out.println("[InternalSMS] from: " +  riderId + " " + message);
        }
    }


    // =========================================================================
    // ── TODO 2: Define InternalMySqlTripStore (concrete class — DIP violation)
    //            save(String tripId, String riderId, String pickup, String drop)
    //            — prints "[MySQLStore] Saving trip: ..."
    // =========================================================================
    static class InternalMySqlTripStore {
        public void save(String tripId, String riderId,
                         String pickup, String drop){
            System.out.println("[MySQLStore] Saving trip [" + tripId
                                + "]: for rider[" + riderId + "]  From Pickup: " + pickup
                                + " To Drop :" + drop);
        }
    }

    // =========================================================================
    // ── TODO 3: Implement ViolatingTripService (shows DIP violation)
    //            Fields: private InternalSmsClient smsClient
    //                    private InternalMySqlTripStore tripStore
    //            Constructor (no args): creates both with `new` (the violation)
    //            Method bookTrip(String riderId, String pickup, String drop):
    //              prints "[ViolatingTripService] Booking trip for " + riderId
    //              calls tripStore.save(...)
    //              calls smsClient.notify(...)
    // =========================================================================
    static class ViolatingTripService {
        private static int counter = 0;
        private InternalSmsClient smsClient;
        private InternalMySqlTripStore tripStore;
        private final String tripId;

        public ViolatingTripService(){
            this.smsClient = new InternalSmsClient();
            this.tripStore = new InternalMySqlTripStore();
            counter ++;
            this.tripId = getTripId();
        }

        private String getTripId(){
            return String.format("[TRIP-%03d]", counter);
        }

        public void bookTrip(String riderId, String pickup,
                             String drop){
            System.out.println("[ViolatingTripService] Booking Trip[" + tripId + "]for " + riderId  + " from " +
                    pickup  + " to " + drop);
            smsClient.notify(riderId, "Your Ride got allotted");
            tripStore.save(tripId, riderId, pickup, drop);
        }
    }

    // =========================================================================
    // ── TODO 4: Define TripRepository interface
    //            Methods: void save(Trip trip)
    //                     Optional<Trip> findById(String tripId)
    //                     List<Trip> findByRider(String riderId)
    // =========================================================================
    interface TripRepository {
        void save(Trip trip);
        Optional<Trip> findById(String tripId);
        List<Trip> findByRider(String riderId);
    }

    // =========================================================================
    // ── TODO 5: Define RiderNotifier interface
    //            Method: void notify(String riderId, String message)
    // =========================================================================
    interface RiderNotifier {
        void notify(String riderId, String message);
    }

    // =========================================================================
    // ── TODO 6: Define FareCalculator interface
    //            Method: double calculate(String pickupLocation, String dropLocation)
    // =========================================================================
    interface FareCalculator {
        double calculate(String pickupLocation, String dropLocation);
    }


    // =========================================================================
    // ── TODO 7: Define TripStatus enum
    //            Values: PENDING, COMPLETED, CANCELLED
    // =========================================================================
    enum TripStatus {
        PENDING, COMPLETED, CANCELLED
    }

    // =========================================================================
    // ── TODO 8: Implement Trip value object
    //            Fields: private final String tripId, riderId, pickupLocation, dropLocation
    //                    private final double fareAmount
    //                    private TripStatus status
    //            Constructor: validates tripId/riderId/pickup/drop not null/blank,
    //                         fareAmount >= 0; sets status = PENDING
    //            Methods: getters for all fields, setStatus(TripStatus),
    //                     String describe()
    // =========================================================================
    static class Trip {
        private final String tripId;
        private final String riderId;
        private final String pickupLocation;
        private final String dropLocation;
        private final double fareAmount;
        private TripStatus status;

        public Trip(String tripId,
                    String riderId,
                    String pickupLocation,
                    String dropLocation,
                    double fareAmount){
            if(tripId == null || tripId.isBlank()) {
                throw new IllegalArgumentException("Trip Id cannot be blank !!!");
            }
            if(riderId == null || riderId.isBlank()){
                throw new IllegalArgumentException("Rider Id cannot be null or blank");
            }
            if(pickupLocation == null || pickupLocation.isBlank()){
                throw new IllegalArgumentException("Pickup Location cannot be null or blank");
            }
            if(dropLocation == null || dropLocation.isBlank()){
                throw new IllegalArgumentException("Drop Location cannot be null or blank");
            }
            if(fareAmount<=0){
                throw new IllegalArgumentException("Fare Amount has to atleast > 0");
            }
            this.tripId = tripId;
            this.riderId = riderId;
            this.pickupLocation = pickupLocation;
            this.dropLocation = dropLocation;
            this.fareAmount = fareAmount;
            this.status = TripStatus.PENDING;
        }

        public double getFareAmount() { return fareAmount; }

        public String getDropLocation() { return dropLocation; }

        public String getTripId() { return tripId; }

        public String getRiderId() { return riderId; }

        public String getPickupLocation() { return pickupLocation; }

        public TripStatus getStatus() { return status; }

        public String describe(){
            return String.format("Trip[tripId=%s,riderId=%s,pickupLocation=%s," +
                    "dropLocation=%s,fareAmount=%.2f,status=%s]", tripId, riderId, pickupLocation, dropLocation,
                    fareAmount, status);
        }

        public void setStatus(TripStatus tripStatus){
            if(tripStatus == null){
                throw new IllegalArgumentException("Trip Status should not be null!!!");
            }
            System.out.println("TRIP[" + tripId + "] set to : " + tripStatus.name());
            this.status = tripStatus;
        }
    }

    // =========================================================================
    // ── TODO 9: Implement TripService (HIGH-LEVEL MODULE — core of this exercise)
    //            Fields (all private final):
    //              TripRepository tripRepository
    //              RiderNotifier  riderNotifier
    //              FareCalculator fareCalculator
    //              int            tripCounter (start at 0, NOT final)
    //            Constructor(TripRepository, RiderNotifier, FareCalculator):
    //              validate all not null; NEVER use `new` here
    //            Methods: nextTripId(), bookTrip(), completeTrip(), cancelTrip(),
    //                     getRiderHistory()
    // =========================================================================
    static class TripService {
        private static int tripCounter = 0;
        private final TripRepository tripRepository;
        private final RiderNotifier riderNotifier;
        private final FareCalculator fareCalculator;

        public TripService(TripRepository tripRepository,
                           RiderNotifier riderNotifier,
                           FareCalculator fareCalculator){
            if(tripRepository == null){
                throw new IllegalArgumentException("Trip Repository cannot be null");
            }
            if(riderNotifier == null){
                throw new IllegalArgumentException("Rider Notifier cannot be null");
            }
            if(fareCalculator == null){
                throw new IllegalArgumentException("Fare Calculator cannot be null");
            }
            this.tripRepository = tripRepository;
            this.riderNotifier = riderNotifier;
            this.fareCalculator = fareCalculator;
        }

        public String nextTripId() {
            return String.format("[TRIP-%03d]", ++tripCounter);
        }

        public Trip bookTrip(String riderId, String pickup, String drop){
            if(riderId == null || riderId.isBlank()) { throw new IllegalArgumentException("Rider Id cannot be blank"); }
            if(pickup == null || pickup.isBlank()) { throw new IllegalArgumentException("Pick Up cannot be blank");}
            if(drop == null || drop.isBlank()){ throw new IllegalArgumentException("Drop cannot be blank or null");}
            double fare = fareCalculator.calculate(pickup, drop);
            Trip trip = new Trip(nextTripId(), riderId, pickup, drop, fare);
            tripRepository.save(trip);
            return trip;
        }

        public Trip completeTrip(String tripId){
            if(tripId == null || tripId.isBlank()){
                throw new IllegalArgumentException("Trip Id cannot be null or blank !!!");
            }
            Optional<Trip> trip = tripRepository.findById(tripId);
            if(trip.isEmpty()) {
                throw new IllegalArgumentException("Trip not found: " +  tripId);
            }
            trip.get().setStatus(TripStatus.COMPLETED);
            riderNotifier.notify(trip.get().getTripId(), "Trip Completed: " +  trip.get().getTripId());
            return trip.get();
        }

        public Trip cancelTrip(String tripId) {
            if(tripId == null || tripId.isBlank()){
                throw new IllegalArgumentException("Trip Id cannot be null or blank !!!");
            }
            Optional<Trip> trip = tripRepository.findById(tripId);
            if(trip.isEmpty()) {
                throw new IllegalArgumentException("Trip not found: " +  tripId);
            }
            riderNotifier.notify(trip.get().getTripId(), "Trip cancelled: " +  trip.get().getTripId());
            trip.get().setStatus(TripStatus.CANCELLED);
            return trip.get();
        }

        public List<Trip> getRiderHistory(String riderId){
            if(riderId == null || riderId.isBlank()) {
                throw new IllegalArgumentException("Rider Id cannot be null or blank !!!");
            }
            return tripRepository.findByRider(riderId);
        }
    }

    // =========================================================================
    // ── TODO 10: Implement InMemoryTripRepository implements TripRepository
    //             Use Map<String, Trip> for storage
    //             findByRider returns unmodifiable list
    //             save: print "[InMemoryRepo] Saved: <trip.describe()>"
    // =========================================================================
    static class InMemoryTripRepository implements TripRepository {
        private final Map<String, Trip> storage;

        public InMemoryTripRepository(){
            this.storage = new HashMap<>();
        }

        public Optional<Trip> findById(String tripId) {
            if(tripId == null || tripId.isBlank()){
                throw new IllegalArgumentException("Trip ID cannot be null or Blank!!");
            }
            if(storage.containsKey(tripId)){
                return Optional.of(storage.get(tripId));
            }
            return Optional.ofNullable(storage.get(tripId));
        }

        public void save(Trip trip){
            if(trip == null){
                throw new IllegalArgumentException("Trip cannot be null");
            }
            storage.put(trip.getTripId(), trip);
            System.out.println("[InMemoryRepo] Saved: "  + trip.describe());
        }

        public List<Trip> findByRider(String riderId){
            if(riderId == null || riderId.isBlank()){
                throw new IllegalArgumentException("Rider ID cannot be null or Blank!!");
            }
            List<Trip> tripsList = new ArrayList<>();
            for(Trip trip : storage.values()){
                if(trip.getRiderId().equals(riderId)) {
                    tripsList.add(trip);
                }
            }
            return Collections.unmodifiableList(tripsList);
        }

    }

    // =========================================================================
    // ── TODO 11: Implement SmsRiderNotifier implements RiderNotifier
    //             notify: prints "[SMS → <riderId>]: <message>"
    // =========================================================================
    static class SmsRiderNotifier implements RiderNotifier {
        @Override
        public void notify(String riderId, String message) {
            if(riderId == null || riderId.isBlank()) {
                throw new IllegalArgumentException("Rider Id cannot be null or blank!!!");
            }
            if(message == null || message.isBlank()){
                throw new IllegalArgumentException("Message cannot be null or blank!!!");
            }
            System.out.println("[SMS -> " + riderId + " ]: " + message);
        }
    }

    // =========================================================================
    // ── TODO 12: Implement StandardFareCalculator implements FareCalculator
    //             BASE_FARE = 50.0, PER_KM_RATE = 12.0
    //             distance = Math.abs(drop.length() - pickup.length()) + 5
    //             fare = BASE_FARE + (distance * PER_KM_RATE)
    // =========================================================================
    static class StandardFareCalculator implements FareCalculator {

        private final double BASE_FARE = 50.0;
        private final double PER_KM_RATE = 12.0;

        @Override
        public double calculate(String pickupLocation, String dropLocation){
            if(dropLocation == null || dropLocation.isBlank()){
                throw new IllegalArgumentException("Drop Location cannot be null or blank");
            }
            if(pickupLocation == null || pickupLocation.isBlank()){
                throw new IllegalArgumentException("Pickup Location cannot be bull or blank");
            }
            int distance = Math.abs(dropLocation.length() - pickupLocation.length()) + 5;
            return BASE_FARE + (distance * PER_KM_RATE);
        }
    }

    // =========================================================================
    // ── TODO 13: Implement FixedFareCalculator implements FareCalculator
    //             Constructor: FixedFareCalculator(double fixedFare)
    //             calculate(): always returns fixedFare (ignore locations)
    // =========================================================================
    static class FixedFareCalculator implements FareCalculator {
        private final double fixedFare;

        public FixedFareCalculator(double fixedFare){
            this.fixedFare = fixedFare;
        }

        @Override
        public double calculate(String pickupLocation, String dropLocation){
            return fixedFare;
        }
    }

    // =========================================================================
    // ── TODO 14: Implement EmailRiderNotifier implements RiderNotifier
    //             notify: prints "[EMAIL → <riderId>]: <message>"
    // =========================================================================
    static class EmailRiderNotifier implements RiderNotifier {
        @Override
        public void notify(String riderId, String message) {
            if(riderId == null || riderId.isBlank()) {
                throw new IllegalArgumentException("Rider Id cannot be null or blank!!!");
            }
            if(message == null || message.isBlank()){
                throw new IllegalArgumentException("Message cannot be null or blank!!!");
            }
            System.out.println("[EMAIL -> " + riderId + " ]: " + message);
        }
    }

    // =========================================================================
    // DO NOT MODIFY — fill in TODOs above to make all tests pass
    // =========================================================================
    public static void main(String[] args) {

        System.out.println("═══ Test 1: DIP Violation — ViolatingTripService ════════");
        ViolatingTripService violator = new ViolatingTripService();
        violator.bookTrip("RIDER-V1", "Airport", "Hotel");
        System.out.println("Test 1 PASSED — ViolatingTripService shows DIP violation");

        System.out.println("\n═══ Test 2: bookTrip — trip created and saved ═══════════");
        TripRepository      repo      = new InMemoryTripRepository();
        RiderNotifier       notifier  = new SmsRiderNotifier();
        FareCalculator      calc      = new StandardFareCalculator();
        TripService         service   = new TripService(repo, notifier, calc);

        Trip t1 = service.bookTrip("RIDER-001", "Airport", "Hotel");
        System.out.println("Trip: " + t1.describe());
        System.out.println("tripId starts with TRIP-: " + t1.getTripId().startsWith("TRIP-"));
        System.out.println("Status PENDING: " + (t1.getStatus() == TripStatus.PENDING));
        System.out.println("fare > 0: " + (t1.getFareAmount() > 0));
        System.out.println("Test 2 PASSED: " + (t1.getTripId().startsWith("TRIP-")
                && t1.getStatus() == TripStatus.PENDING
                && t1.getFareAmount() > 0));

        System.out.println("\n═══ Test 3: bookTrip — tripId zero-padded (TRIP-001) ════");
        System.out.println("tripId: " + t1.getTripId());   // TRIP-001
        boolean t34 = t1.getTripId().matches("TRIP-\\d{3}");
        System.out.println("Test 3 PASSED: " + t34);

        System.out.println("\n═══ Test 4: completeTrip — status changes to COMPLETED ══");
        service.completeTrip(t1.getTripId());
        System.out.println("Status after complete: " + t1.getStatus());   // COMPLETED
        System.out.println("Test 4 PASSED: " + (t1.getStatus() == TripStatus.COMPLETED));

        System.out.println("\n═══ Test 5: cancelTrip — status changes to CANCELLED ════");
        Trip t2 = service.bookTrip("RIDER-001", "Mall", "Home");
        service.cancelTrip(t2.getTripId());
        System.out.println("Status after cancel: " + t2.getStatus());   // CANCELLED
        System.out.println("Test 5 PASSED: " + (t2.getStatus() == TripStatus.CANCELLED));

        System.out.println("\n═══ Test 6: getRiderHistory — returns all trips ══════════");
        Trip t3 = service.bookTrip("RIDER-001", "Office", "Station");
        List<Trip> history = service.getRiderHistory("RIDER-001");
        System.out.println("RIDER-001 trip count: " + history.size());   // 3
        System.out.println("Test 6 PASSED: " + (history.size() == 3));

        System.out.println("\n═══ Test 7: unmodifiable history list ═══════════════════");
        try {
            history.add(null);
            System.out.println("Test 7 FAILED — list should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            System.out.println("Test 7 PASSED — getRiderHistory() returns unmodifiable list");
        }

        System.out.println("\n═══ Test 8: DIP proof — swap FareCalculator (FixedFare) ══");
        // TripService unchanged — just inject a different FareCalculator
        FareCalculator fixedCalc      = new FixedFareCalculator(100.0);
        TripService    fixedService   = new TripService(repo, notifier, fixedCalc);
        Trip           fixedTrip      = fixedService.bookTrip("RIDER-002", "Anywhere", "Somewhere");
        System.out.println("Fixed fare trip: " + fixedTrip.describe());
        System.out.println("fare == 100.0: " + (fixedTrip.getFareAmount() == 100.0));
        System.out.println("Test 8 PASSED: " + (fixedTrip.getFareAmount() == 100.0));

        System.out.println("\n═══ Test 9: DIP proof — swap RiderNotifier (Email) ══════");
        // TripService unchanged — just inject EmailRiderNotifier instead of SMS
        RiderNotifier  emailNotifier = new EmailRiderNotifier();
        TripService    emailService  = new TripService(repo, emailNotifier, calc);
        Trip           emailTrip     = emailService.bookTrip("RIDER-003", "Station", "Airport");
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
    }

    // =========================================================================
    // HINTS (read only if stuck)
    // =========================================================================

    /*
     * ── HINT 1 (Gentle) ────────────────────────────────────────────────────
     * The key rule: TripService should never write `new InMemoryTripRepository()`,
     * `new SmsRiderNotifier()`, or `new StandardFareCalculator()` anywhere inside it.
     * All three dependencies arrive from outside via the constructor.
     * Think of TripService as someone who "orders" a repository and a notifier,
     * but doesn't "build" them.
     *
     * For the violation: ViolatingTripService does the opposite — it creates
     * InternalSmsClient and InternalMySqlTripStore with `new` inside its constructor.
     * That's the entire violation.
     *
     * ── HINT 2 (Direct) ────────────────────────────────────────────────────
     * Three interfaces + constructor injection pattern:
     *
     *   interface TripRepository { void save(Trip); Optional<Trip> findById(String); ... }
     *   interface RiderNotifier  { void notify(String riderId, String message); }
     *   interface FareCalculator { double calculate(String pickup, String drop); }
     *
     *   class TripService {
     *       private final TripRepository  tripRepository;
     *       private final RiderNotifier   riderNotifier;
     *       private final FareCalculator  fareCalculator;
     *       private int tripCounter = 0;
     *
     *       public TripService(TripRepository r, RiderNotifier n, FareCalculator c) {
     *           if (r == null || n == null || c == null) throw new IllegalArgumentException(...);
     *           this.tripRepository = r;
     *           this.riderNotifier  = n;
     *           this.fareCalculator = c;
     *       }
     *
     *       private String nextTripId() { return String.format("TRIP-%03d", ++tripCounter); }
     *   }
     *
     * Use Objects.requireNonNull() or explicit null checks.
     * Use Optional.ofNullable(store.get(tripId)) for findById.
     * Use Collections.unmodifiableList() for findByRider.
     *
     * ── HINT 3 (Near-Solution) ─────────────────────────────────────────────
     * bookTrip skeleton:
     *
     *   public Trip bookTrip(String riderId, String pickup, String drop) {
     *       if (riderId == null || riderId.isBlank()) throw new IllegalArgumentException(...);
     *       if (pickup  == null || pickup.isBlank())  throw new IllegalArgumentException(...);
     *       if (drop    == null || drop.isBlank())    throw new IllegalArgumentException(...);
     *
     *       double fare   = fareCalculator.calculate(pickup, drop);
     *       String tripId = nextTripId();
     *       Trip   trip   = new Trip(tripId, riderId, pickup, drop, fare);
     *
     *       tripRepository.save(trip);
     *       riderNotifier.notify(riderId,
     *           String.format("Trip booked: %s | Fare: ₹%.1f", tripId, fare));
     *       return trip;
     *   }
     *
     * completeTrip skeleton:
     *
     *   public Trip completeTrip(String tripId) {
     *       Trip trip = tripRepository.findById(tripId)
     *           .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
     *       trip.setStatus(TripStatus.COMPLETED);
     *       tripRepository.save(trip);
     *       riderNotifier.notify(trip.getRiderId(), "Trip completed: " + tripId);
     *       return trip;
     *   }
     *
     * InMemoryTripRepository skeleton:
     *
     *   static class InMemoryTripRepository implements TripRepository {
     *       private final Map<String, Trip> store = new HashMap<>();
     *
     *       @Override public void save(Trip trip) {
     *           store.put(trip.getTripId(), trip);
     *           System.out.println("[InMemoryRepo] Saved: " + trip.describe());
     *       }
     *       @Override public Optional<Trip> findById(String id) {
     *           return Optional.ofNullable(store.get(id));
     *       }
     *       @Override public List<Trip> findByRider(String riderId) {
     *           List<Trip> result = new ArrayList<>();
     *           for (Trip t : store.values())
     *               if (t.getRiderId().equals(riderId)) result.add(t);
     *           return Collections.unmodifiableList(result);
     *       }
     *   }
     */
}
