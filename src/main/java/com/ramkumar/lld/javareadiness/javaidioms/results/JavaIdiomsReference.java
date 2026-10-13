package com.ramkumar.lld.javareadiness.javaidioms.results;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Reference solution — Phase 4 | 4.3 Java 8–21 Idioms
 * Scenario B: Insurance Claims Desk.
 */
public class JavaIdiomsReference {

    // Sealed: the compiler knows all three subtypes, so switches need no default.
    sealed interface Claim permits AutoClaim, HealthClaim, TravelClaim {
        String id();          // satisfied by each record's generated accessor
        double amount();
    }

    // Compact constructors: validate only; fields are assigned automatically afterwards.
    record AutoClaim(String id, double amount, boolean atFault) implements Claim {
        AutoClaim {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
            if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        }
    }

    record HealthClaim(String id, double amount, boolean hospitalised) implements Claim {
        HealthClaim {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
            if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        }
    }

    record TravelClaim(String id, double amount, int tripDays) implements Claim {
        TravelClaim {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
            if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
            if (tripDays < 1) throw new IllegalArgumentException("tripDays must be >= 1");
        }
    }

    static final class ClaimProcessor {
        private ClaimProcessor() { }

        // Review order defined ONCE: amount DESC (reversed applies to the amount key only), then id ASC.
        private static final Comparator<Claim> REVIEW_ORDER =
                Comparator.comparingDouble(Claim::amount).reversed()
                          .thenComparing(Claim::id);

        static double payout(Claim c) {
            if (c == null) throw new IllegalArgumentException("claim must not be null");
            // Switch expression, no default: adding a 4th subtype breaks compilation here.
            return switch (c) {
                case AutoClaim a   -> a.atFault() ? 0.0 : a.amount() * 0.80;
                case HealthClaim h -> h.amount() * (h.hospitalised() ? 0.90 : 0.70);
                case TravelClaim t -> Math.min(t.amount(), 500.0 * t.tripDays());
            };
        }

        static String label(Claim c) {
            return switch (c) {
                case AutoClaim a   -> "AUTO";
                case HealthClaim h -> "HEALTH";
                case TravelClaim t -> "TRAVEL";
            };
        }

        // Record deconstruction: components bound in the label, no accessor calls.
        static String describe(Claim c) {
            return switch (c) {
                case AutoClaim(String id, double amount, boolean atFault) ->
                        String.format("AUTO %s: %.2f [%s]", id, amount, atFault ? "at fault" : "not at fault");
                case HealthClaim(String id, double amount, boolean hospitalised) ->
                        String.format("HEALTH %s: %.2f [%s]", id, amount, hospitalised ? "hospitalised" : "outpatient");
                case TravelClaim(String id, double amount, int tripDays) ->
                        String.format("TRAVEL %s: %.2f [%d days]", id, amount, tripDays);   // upper-case TRAVEL
            };
        }

        // sorted() returns a new stream: the input is never mutated; toList() is unmodifiable.
        static List<Claim> sortedForReview(List<Claim> claims) {
            return claims.stream().sorted(REVIEW_ORDER).toList();
        }

        // The review order puts the LARGEST claim FIRST, so we want min(), not max():
        // max(cmp) returns the element that sorts LAST (here: the smallest claim).
        static Optional<Claim> largest(List<Claim> claims) {
            return claims.stream().min(REVIEW_ORDER);
        }

        // Typed return keeps generic inference happy; TreeMap::new sorts the keys.
        static Map<String, Double> totalPayoutByType(List<Claim> claims) {
            return claims.stream().collect(Collectors.groupingBy(
                    ClaimProcessor::label, TreeMap::new, Collectors.summingDouble(ClaimProcessor::payout)));
        }
    }

    public static void main(String[] args) {
        // Test 1
        AutoClaim a = new AutoClaim("A-1", 1000, false);
        System.out.println(a);                                              // AutoClaim[id=A-1, amount=1000.0, atFault=false]
        System.out.println(a.id() + " " + a.amount());                      // A-1 1000.0
        System.out.println(a.equals(new AutoClaim("A-1", 1000, false)));    // true
        Claim viaInterface = a;
        System.out.println(viaInterface.id());                              // A-1

        // Test 2
        try { new AutoClaim(" ", 5, false); }  catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        try { new HealthClaim("H", 0, true); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        try { new TravelClaim("T", 50, 0); }   catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }

        // Test 3
        List<Claim> claims = List.of(
                new AutoClaim("A-1", 1000, false), new AutoClaim("A-2", 400, true),
                new AutoClaim("A-3", 1000, false), new HealthClaim("H-1", 2000, true),
                new HealthClaim("H-2", 300, false), new TravelClaim("T-1", 900, 1),
                new TravelClaim("T-2", 1200, 3));
        StringBuilder sb = new StringBuilder();
        for (Claim c : claims) sb.append(ClaimProcessor.label(c)).append('=').append(ClaimProcessor.payout(c)).append(' ');
        System.out.println(sb.toString().trim());
        try { ClaimProcessor.payout(null); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }

        // Test 4
        System.out.println(ClaimProcessor.describe(claims.get(0)));
        System.out.println(ClaimProcessor.describe(claims.get(1)));
        System.out.println(ClaimProcessor.describe(claims.get(3)));
        System.out.println(ClaimProcessor.describe(claims.get(4)));
        System.out.println(ClaimProcessor.describe(claims.get(6)));

        // Test 5
        List<Claim> input = new ArrayList<>(claims);
        List<Claim> sorted = ClaimProcessor.sortedForReview(input);
        System.out.println(sorted.stream().map(Claim::id).toList());       // [H-1, T-2, A-1, A-3, T-1, A-2, H-2]
        System.out.println(input.stream().map(Claim::id).toList());        // [A-1, A-2, A-3, H-1, H-2, T-1, T-2]
        try { sorted.add(a); } catch (UnsupportedOperationException e) { System.out.println("unmodifiable"); }

        // Test 6
        System.out.println(ClaimProcessor.largest(claims).map(Claim::id).orElse("none"));                              // H-1
        System.out.println(ClaimProcessor.largest(List.of(claims.get(2), claims.get(0))).map(Claim::id).orElse("none")); // A-1
        System.out.println(ClaimProcessor.largest(List.of()).isPresent());                                             // false

        // Test 7
        System.out.println(ClaimProcessor.totalPayoutByType(claims));      // {AUTO=1600.0, HEALTH=2010.0, TRAVEL=1700.0}
        System.out.println(ClaimProcessor.totalPayoutByType(List.of(claims.get(5))));   // {TRAVEL=500.0}
        System.out.println(ClaimProcessor.totalPayoutByType(List.of()));                // {}

        // Extra: catches the most common mistake — max() with a "descending" comparator returns the SMALLEST claim,
        // and the describe label must be upper-case for every type
        System.out.println(ClaimProcessor.largest(claims).get().id().equals(sorted.get(0).id()));   // true: largest == first in review order
        System.out.println(ClaimProcessor.describe(claims.get(5)).startsWith("TRAVEL "));           // true
    }
}
