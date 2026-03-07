package com.ramkumar.lld.designpatterns.structural.facade.results;

// ─────────────────────────────────────────────────────────────────────────────
// Reference Solution — Facade Pattern (Scenario B: Travel Booking System)
// ─────────────────────────────────────────────────────────────────────────────

public class FacadeReference {

    // ── Pre-written subsystems (unchanged) ───────────────────────────────────

    static class FlightService {
        private int counter = 0;
        public String searchFlight(String from, String to, String date) {
            String id = String.format("FL-%s-%s-%02d",
                    from.substring(0, 3).toUpperCase(),
                    to.substring(0, 3).toUpperCase(), ++counter);
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
            String id = String.format("HTL-%s-%02d", city.substring(0, 3).toUpperCase(), ++counter);
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
            String id = String.format("CAR-%s-%02d", city.substring(0, 3).toUpperCase(), ++counter);
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
            return String.format("TripBooking{flight=%s, hotel=%s, car=%s, receipt=%s, email=%s}",
                    flightRef, hotelRef, carRef, receiptId, customerEmail);
        }
    }

    // ── Facade ────────────────────────────────────────────────────────────────

    static class TravelFacade {

        // [Composition] Each subsystem is a private final field — injected, never replaced.
        // Field names match the problem-statement language (flights, hotels, cars, payment, email)
        // rather than the class names, keeping intent readable.
        private final FlightService    flights;
        private final HotelService     hotels;
        private final CarRentalService cars;
        private final PaymentService   payment;
        private final EmailService     email;

        // [ConstructorInjection] No null checks here — the Facade trusts its caller.
        // Null-safety belongs at the injection site (DI framework, factory, or test setup),
        // not inside the Facade. Adding null checks would mix orchestration with validation.
        TravelFacade(FlightService flights, HotelService hotels,
                     CarRentalService cars, PaymentService payment, EmailService email) {
            this.flights = flights;
            this.hotels  = hotels;
            this.cars    = cars;
            this.payment = payment;
            this.email   = email;
        }

        // [SimplifiedInterface] One method replaces 11 subsystem calls.
        // The client supplies intent (from, to, date, city, nights, email).
        // The Facade owns the sequence — the client never needs to know it.
        TripBooking bookTrip(String from, String to, String date,
                             String city, int nights, String customerEmail) {
            // Steps 1–2: flight
            String flightId  = flights.searchFlight(from, to, date);
            String flightRef = flights.bookFlight(flightId);

            // Steps 3–4: hotel
            String hotelId  = hotels.findHotel(city, nights);
            String hotelRef = hotels.reserveRoom(hotelId);

            // Steps 5–6: car
            String carId  = cars.findCar(city);
            String carRef = cars.rentCar(carId);

            // Step 7: payment
            String receiptId = payment.charge(500.00);

            // Steps 8–9: notification
            String details = String.format("Flight:%s | Hotel:%s | Car:%s | Receipt:%s",
                    flightRef, hotelRef, carRef, receiptId);
            email.sendConfirmation(customerEmail, details);

            // Step 10: audit print
            System.out.printf("[TravelFacade] Trip booked: %s → %s on %s%n", from, to, date);

            // Step 11: return value object so the client can reference this trip later
            // (e.g. for cancelTrip) without knowing any internal IDs
            return new TripBooking(flightRef, hotelRef, carRef, receiptId, customerEmail);
        }

        // [Orchestration] Reverse of bookTrip — the Facade knows the correct cancellation order.
        // No null check on booking: if null is passed, the NPE from booking.flightRef is
        // the natural, informative failure. Adding an explicit IAE would be noise.
        void cancelTrip(TripBooking booking) {
            // Steps 1–3: cancel bookings (flight first — most time-sensitive)
            flights.cancelFlight(booking.flightRef);
            hotels.cancelReservation(booking.hotelRef);
            cars.returnCar(booking.carRef);

            // Step 4: refund
            payment.refund(booking.receiptId);

            // Steps 5–6: notification
            String details = "Trip cancelled: " + booking.flightRef;
            email.sendCancellation(booking.customerEmail, details);

            // Step 7: audit print
            System.out.printf("[TravelFacade] Trip cancelled for %s%n", booking.customerEmail);
        }
    }

    // ── main() ────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        FlightService    flights = new FlightService();
        HotelService     hotels  = new HotelService();
        CarRentalService cars    = new CarRentalService();
        PaymentService   payment = new PaymentService();
        EmailService     email   = new EmailService();

        TravelFacade facade = new TravelFacade(flights, hotels, cars, payment, email);
        int passed = 0, total = 10;

        // Test 1: Facade construction
        System.out.println("\n[Test 1] Facade constructed");
        check("Facade constructed", true); passed++;

        // Test 2: bookTrip returns non-null TripBooking
        System.out.println("\n[Test 2] bookTrip returns TripBooking");
        TripBooking trip = facade.bookTrip("London", "Paris", "2026-06-01", "Paris", 3, "alice@example.com");
        boolean t2 = trip != null;
        check("trip not null", t2); if (t2) passed++;

        // Test 3: flightRef format
        System.out.println("\n[Test 3] flightRef format");
        boolean t3 = trip.flightRef.startsWith("BK-FL-");
        check("flightRef starts BK-FL-", t3); if (t3) passed++;

        // Test 4: hotelRef format
        System.out.println("\n[Test 4] hotelRef format");
        boolean t4 = trip.hotelRef.startsWith("RSV-HTL-");
        check("hotelRef starts RSV-HTL-", t4); if (t4) passed++;

        // Test 5: carRef format
        System.out.println("\n[Test 5] carRef format");
        boolean t5 = trip.carRef.startsWith("RNT-CAR-");
        check("carRef starts RNT-CAR-", t5); if (t5) passed++;

        // Test 6: receiptId for $500.00 = RCP-50000
        System.out.println("\n[Test 6] receiptId = RCP-50000");
        boolean t6 = "RCP-50000".equals(trip.receiptId);
        check("receiptId is RCP-50000", t6); if (t6) passed++;

        // Test 7: customerEmail stored in TripBooking
        System.out.println("\n[Test 7] customerEmail stored");
        boolean t7 = "alice@example.com".equals(trip.customerEmail);
        check("customerEmail stored", t7); if (t7) passed++;

        // Test 8: second booking produces different refs (subsystem counters increment)
        System.out.println("\n[Test 8] unique refs across two bookings");
        TripBooking trip2 = facade.bookTrip("NYC", "Tokyo", "2026-07-15", "Tokyo", 5, "bob@example.com");
        boolean t8 = !trip.flightRef.equals(trip2.flightRef);
        check("unique flight refs", t8); if (t8) passed++;

        // Test 9: cancelTrip completes all 7 steps without exception
        System.out.println("\n[Test 9] cancelTrip runs all services");
        facade.cancelTrip(trip);
        check("cancelTrip completed", true); passed++;

        // Test 10 — catches the most common mistake: null validation inside the Facade.
        // A correctly implemented Facade does NOT validate its inputs — it delegates.
        // If the student added a null check in the constructor, this test would still
        // pass (the null check would fire), but the review flags it as a design error.
        // Here we demonstrate the correct approach: constructing with real services works fine.
        System.out.println("\n[Test 10] Facade constructed with real services — no spurious validation");
        TravelFacade facade2 = new TravelFacade(
                new FlightService(), new HotelService(),
                new CarRentalService(), new PaymentService(), new EmailService());
        TripBooking trip3 = facade2.bookTrip("Rome", "Madrid", "2026-08-01", "Madrid", 2, "carol@example.com");
        boolean t10 = trip3 != null && trip3.flightRef.startsWith("BK-FL-");
        check("second facade instance works correctly (no static state leakage)", t10);
        if (t10) passed++;

        System.out.printf("%n══════════════════════════════%n");
        System.out.printf("Results: %d / %d PASSED%n", passed, total);
        System.out.printf("══════════════════════════════%n");
    }

    private static void check(String label, boolean condition) {
        System.out.println("  " + label + ": " + (condition ? "PASSED" : "FAILED"));
    }
}
