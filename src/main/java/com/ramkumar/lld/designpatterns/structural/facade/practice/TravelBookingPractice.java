package com.ramkumar.lld.designpatterns.structural.facade.practice;

/**
 * Facade Pattern — Scenario B: Travel Booking System
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * PROBLEM STATEMENT
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * You are building the trip-booking module of a travel app. The module must
 * coordinate five independent backend services — each built by a different
 * team with its own incompatible API:
 *
 *   FlightService    — search and book flights, cancel bookings
 *   HotelService     — find hotels, reserve rooms, cancel reservations
 *   CarRentalService — find cars, rent, and return them
 *   PaymentService   — charge and refund payments
 *   EmailService     — send confirmation and cancellation emails
 *
 * The client code (main) must be able to book or cancel an entire trip with
 * a SINGLE method call and must NEVER call any service class directly.
 *
 * ── Pre-written classes (do NOT modify) ────────────────────────────────────
 *
 *   FlightService
 *     searchFlight(String from, String to, String date) → String flightId
 *       e.g. "FL-LON-PAR-01"
 *     bookFlight(String flightId) → String bookingRef
 *       e.g. "BK-FL-LON-PAR-01"
 *     cancelFlight(String bookingRef) → void
 *
 *   HotelService
 *     findHotel(String city, int nights) → String hotelId
 *       e.g. "HTL-PAR-01"
 *     reserveRoom(String hotelId) → String reservationId
 *       e.g. "RSV-HTL-PAR-01"
 *     cancelReservation(String reservationId) → void
 *
 *   CarRentalService
 *     findCar(String city) → String carId
 *       e.g. "CAR-PAR-01"
 *     rentCar(String carId) → String rentalId
 *       e.g. "RNT-CAR-PAR-01"
 *     returnCar(String rentalId) → void
 *
 *   PaymentService
 *     charge(double amount) → String receiptId
 *       e.g. "RCP-50000"  (amount $500.00 → 50000 cents in the ID)
 *     refund(String receiptId) → boolean
 *
 *   EmailService
 *     sendConfirmation(String to, String details) → void
 *     sendCancellation(String to, String details) → void
 *
 *   TripBooking  (value object — pre-written)
 *     final String flightRef
 *     final String hotelRef
 *     final String carRef
 *     final String receiptId
 *     final String customerEmail
 *
 * ── Your task: implement TravelFacade (4 TODOs) ────────────────────────────
 *
 * TODO 1 — Declare 5 private final subsystem fields (one per service).
 *
 * TODO 2 — Constructor: TravelFacade(FlightService, HotelService,
 *           CarRentalService, PaymentService, EmailService)
 *           Store each parameter in the corresponding field.
 *
 * TODO 3 — bookTrip(String from, String to, String date,
 *                   String city, int nights, String customerEmail)
 *           → TripBooking
 *           Orchestrate in this exact 11-step sequence — see TODO 3 below.
 *
 * TODO 4 — cancelTrip(TripBooking booking) → void
 *           Orchestrate in this exact 7-step sequence — see TODO 4 below.
 *
 * ── Design constraints ──────────────────────────────────────────────────────
 *   • main() may only call TravelFacade methods — never service classes directly.
 *   • All subsystem fields must be private and final.
 *   • No static state anywhere in TravelFacade.
 * ═══════════════════════════════════════════════════════════════════════════
 */
public class TravelBookingPractice {

    // =========================================================================
    // PRE-WRITTEN SUBSYSTEMS — Do NOT modify any class below this line until
    // the TravelFacade section.
    // =========================================================================

    static class FlightService {
        private int counter = 0;

        public String searchFlight(String from, String to, String date) {
            String id = String.format("FL-%s-%s-%02d",
                    from.substring(0, 3).toUpperCase(),
                    to.substring(0, 3).toUpperCase(),
                    ++counter);
            System.out.printf("  [FlightService] Found %s (%s→%s on %s)%n", id, from, to, date);
            return id;
        }

        public String bookFlight(String flightId) {
            String ref = "BK-" + flightId;
            System.out.printf("  [FlightService] Booked %s → ref %s%n", flightId, ref);
            return ref;
        }

        public void cancelFlight(String bookingRef) {
            System.out.printf("  [FlightService] Cancelled %s%n", bookingRef);
        }
    }

    static class HotelService {
        private int counter = 0;

        public String findHotel(String city, int nights) {
            String id = String.format("HTL-%s-%02d",
                    city.substring(0, 3).toUpperCase(), ++counter);
            System.out.printf("  [HotelService] Found %s (%s, %d nights)%n", id, city, nights);
            return id;
        }

        public String reserveRoom(String hotelId) {
            String ref = "RSV-" + hotelId;
            System.out.printf("  [HotelService] Reserved %s → ref %s%n", hotelId, ref);
            return ref;
        }

        public void cancelReservation(String reservationId) {
            System.out.printf("  [HotelService] Cancelled reservation %s%n", reservationId);
        }
    }

    static class CarRentalService {
        private int counter = 0;

        public String findCar(String city) {
            String id = String.format("CAR-%s-%02d",
                    city.substring(0, 3).toUpperCase(), ++counter);
            System.out.printf("  [CarRental] Found %s in %s%n", id, city);
            return id;
        }

        public String rentCar(String carId) {
            String ref = "RNT-" + carId;
            System.out.printf("  [CarRental] Rented %s → ref %s%n", carId, ref);
            return ref;
        }

        public void returnCar(String rentalId) {
            System.out.printf("  [CarRental] Returned %s%n", rentalId);
        }
    }

    static class PaymentService {
        public String charge(double amount) {
            String id = "RCP-" + (int) (amount * 100);
            System.out.printf("  [Payment] Charged $%.2f → receipt %s%n", amount, id);
            return id;
        }

        public boolean refund(String receiptId) {
            System.out.printf("  [Payment] Refunded %s%n", receiptId);
            return true;
        }
    }

    static class EmailService {
        public void sendConfirmation(String to, String details) {
            System.out.printf("  [Email] Confirmation → %s: %s%n", to, details);
        }

        public void sendCancellation(String to, String details) {
            System.out.printf("  [Email] Cancellation → %s: %s%n", to, details);
        }
    }

    // ── PRE-WRITTEN value object — do NOT modify ──────────────────────────────
    static class TripBooking {
        final String flightRef;
        final String hotelRef;
        final String carRef;
        final String receiptId;
        final String customerEmail;

        TripBooking(String flightRef, String hotelRef, String carRef,
                    String receiptId, String customerEmail) {
            this.flightRef     = flightRef;
            this.hotelRef      = hotelRef;
            this.carRef        = carRef;
            this.receiptId     = receiptId;
            this.customerEmail = customerEmail;
        }

        @Override
        public String toString() {
            return String.format(
                    "TripBooking{flight=%s, hotel=%s, car=%s, receipt=%s, email=%s}",
                    flightRef, hotelRef, carRef, receiptId, customerEmail);
        }
    }

    // =========================================================================
    // YOUR WORK STARTS HERE
    // Complete each TODO in order. Uncomment the matching test block in main()
    // after finishing each TODO. Blocks build on each other — uncomment in order.
    // =========================================================================


    //    Step 1: flights.cancelFlight(booking.flightRef)
    //    Step 2: hotels.cancelReservation(booking.hotelRef)
    //    Step 3: cars.returnCar(booking.carRef)
    //    Step 4: payment.refund(booking.receiptId)
    //    Step 5: Build the cancellation details string:
    //            "Trip cancelled: " + booking.flightRef
    //    Step 6: email.sendCancellation(booking.customerEmail, details)
    //    Step 7: Print: System.out.printf("[TravelFacade] Trip cancelled for %s%n",
    //                                    booking.customerEmail)

    static class TravelFacade {
        private final FlightService flightService;
        private final HotelService hotelService;
        private final CarRentalService carRentalService;
        private final PaymentService paymentService;
        private final EmailService emailService;

        public TravelFacade(FlightService flightService, HotelService hotelService,
                            CarRentalService carRentalService, PaymentService paymentService,
                            EmailService emailService) {
            if(flightService == null) {
                throw new IllegalArgumentException("Flight Service cannot be null!!");
            }
            if(hotelService == null){
                throw new IllegalArgumentException("Hotel Service cannot be null!!");
            }
            if(carRentalService == null){
                throw new IllegalArgumentException("Car Rental Service cannot be null!!");
            }
            if(paymentService == null){
                throw new IllegalArgumentException("Payment Service cannot be null!!");
            }
            if(emailService == null){
                throw new IllegalArgumentException("Email Service cannot be null!!");
            }
            this.flightService = flightService;
            this.hotelService = hotelService;
            this.carRentalService = carRentalService;
            this.paymentService = paymentService;
            this.emailService = emailService;
        }

        public TripBooking bookTrip(String from, String to, String date, String city, int nights, String customerEmail){
            String flightId  = flightService.searchFlight(from, to, date);
            String flightRef = flightService.bookFlight(flightId);
            String hotelId   = hotelService.findHotel(city, nights);
            String hotelRef  = hotelService.reserveRoom(hotelId);
            String carId     = carRentalService.findCar(city);
            String carRef    = carRentalService.rentCar(carId);
            String receiptId = paymentService.charge(500.00);
            String details = String.format("Flight:%s | Hotel:%s | Car:%s | Receipt:%s",
                               flightRef, hotelRef, carRef, receiptId);
            emailService.sendConfirmation(customerEmail, details);
            System.out.printf("[TravelFacade] Trip booked: %s → %s on %s%n",
                                         from, to, date);
            return new TripBooking(flightRef, hotelRef, carRef, receiptId, customerEmail);
        }

        public void cancelTrip(TripBooking booking){
            if(booking == null) {
                throw new IllegalArgumentException("Booking cannot be null !!!");
            }
            flightService.cancelFlight(booking.flightRef);
            hotelService.cancelReservation(booking.hotelRef);
            carRentalService.returnCar(booking.carRef);
            paymentService.refund(booking.receiptId);
            String details =  "Trip cancelled: " + booking.flightRef;
            emailService.sendCancellation(booking.customerEmail, details);
            System.out.printf("[TravelFacade] Trip cancelled for %s%n", booking.customerEmail);
        }

    }   // shell — student writes all members inside


    // ─────────────────────────────────────────────────────────────────────────
    // DO NOT MODIFY main() — uncomment each block after finishing the TODO it names.
    // Blocks build on each other: uncomment block 1 first, then 2, 3, 4 in order.
    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: Facade construction (uncomment after TODO 2) ──────────────────
         FlightService    flights = new FlightService();
         HotelService     hotels  = new HotelService();
         CarRentalService cars    = new CarRentalService();
         PaymentService   payment = new PaymentService();
         EmailService     email   = new EmailService();
         TravelFacade facade = new TravelFacade(flights, hotels, cars, payment, email);
         System.out.println("Test 1 — Facade constructed: PASSED");

        // ── Test 2: bookTrip returns a non-null TripBooking (uncomment after TODO 3) ─
         TripBooking trip = facade.bookTrip("London", "Paris", "2026-06-01", "Paris", 3, "alice@example.com");
         System.out.println("Test 2 — trip not null: " + (trip != null ? "PASSED" : "FAILED"));

        // ── Test 3: flightRef has correct format (uncomment after TODO 3) ────────────
         System.out.println("Test 3 — flightRef starts BK-FL-: "
             + (trip.flightRef.startsWith("BK-FL-") ? "PASSED" : "FAILED (got: " + trip.flightRef + ")"));

        // ── Test 4: hotelRef has correct format (uncomment after TODO 3) ─────────────
         System.out.println("Test 4 — hotelRef starts RSV-HTL-: "
             + (trip.hotelRef.startsWith("RSV-HTL-") ? "PASSED" : "FAILED (got: " + trip.hotelRef + ")"));

        // ── Test 5: carRef has correct format (uncomment after TODO 3) ───────────────
         System.out.println("Test 5 — carRef starts RNT-CAR-: "
             + (trip.carRef.startsWith("RNT-CAR-") ? "PASSED" : "FAILED (got: " + trip.carRef + ")"));

        // ── Test 6: receiptId is RCP-50000 for $500.00 charge (uncomment after TODO 3) ─
         System.out.println("Test 6 — receiptId is RCP-50000: "
             + (trip.receiptId.equals("RCP-50000") ? "PASSED" : "FAILED (got: " + trip.receiptId + ")"));

        // ── Test 7: customerEmail is stored in TripBooking (uncomment after TODO 3) ──
         System.out.println("Test 7 — customerEmail stored: "
             + ("alice@example.com".equals(trip.customerEmail) ? "PASSED" : "FAILED"));

        // ── Test 8: second booking produces different refs (uncomment after TODO 3) ──
         TripBooking trip2 = facade.bookTrip("NYC", "Tokyo", "2026-07-15", "Tokyo", 5, "bob@example.com");
         System.out.println("Test 8 — unique flight refs: "
             + (!trip.flightRef.equals(trip2.flightRef) ? "PASSED" : "FAILED"));

        // ── Test 9: cancelTrip runs all 5 services without exception (uncomment after TODO 4) ─
         facade.cancelTrip(trip);
         System.out.println("Test 9 — cancelTrip completed: PASSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HINTS — read only if stuck
    // ─────────────────────────────────────────────────────────────────────────

    /*
     * HINT 1 (Gentle)
     * ───────────────
     * The client in main() builds five service objects and passes them somewhere.
     * That "somewhere" must remember each service and know the exact sequence of
     * calls needed to complete a booking or a cancellation. The client should
     * never need to know the order — it just says "book this trip" and waits for
     * a result. Think about what class owns that sequencing knowledge.
     */

    /*
     * HINT 2 (Direct)
     * ───────────────
     * This is the Facade pattern. TravelFacade is the Facade class.
     * The five service classes are the subsystem.
     *
     * Structure:
     *   - Each service is a private final field, set once in the constructor.
     *   - bookTrip() calls the services in the 11-step sequence specified in TODO 3.
     *   - cancelTrip() calls the services in the 7-step sequence specified in TODO 4.
     *   - The client (main) only ever calls TravelFacade methods — never service methods.
     *
     * The key insight: the Facade owns the "what order?" knowledge. The client
     * owns the "what trip?" knowledge. They are cleanly separated.
     */

    /*
     * HINT 3 (Near-solution — class skeleton only, no method bodies)
     * ───────────────────────────────────────────────────────────────
     *
     * static class TravelFacade {
     *
     *     private final FlightService    flights;
     *     private final HotelService     hotels;
     *     private final CarRentalService cars;
     *     private final PaymentService   payment;
     *     private final EmailService     email;
     *
     *     TravelFacade(FlightService flights, HotelService hotels,
     *                  CarRentalService cars, PaymentService payment,
     *                  EmailService email) { ... }
     *
     *     TripBooking bookTrip(String from, String to, String date,
     *                          String city, int nights, String customerEmail) { ... }
     *
     *     void cancelTrip(TripBooking booking) { ... }
     * }
     */
}
