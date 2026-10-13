package com.ramkumar.lld.javareadiness.javaidioms.practice;

import java.util.*;
import java.util.stream.*;

/**
 * Practice Exercise — Phase 4 Java Readiness | 4.3 Java 8–21 Idioms (size S, lite: 6 TODOs)
 * Scenario B: Insurance Claims Desk
 *
 * ═══════════════════════════════════════════════════════════════════════
 * PROBLEM STATEMENT
 * ═══════════════════════════════════════════════════════════════════════
 *
 * An insurer handles three kinds of claims. Model them as a closed hierarchy of
 * immutable values and process them with modern Java (records, sealed types,
 * switch expressions, Optional, Comparator, streams). Java 21.
 *
 * ── SEALED INTERFACE: Claim  (permits AutoClaim, HealthClaim, TravelClaim) ──
 *   Members every claim exposes:
 *     1. id()     → String
 *     2. amount() → double      (the amount the customer claimed)
 *
 * ── RECORDS (each implements Claim; the record accessors satisfy the interface) ──
 *   AutoClaim   components, in order: String id, double amount, boolean atFault
 *   HealthClaim components, in order: String id, double amount, boolean hospitalised
 *   TravelClaim components, in order: String id, double amount, int tripDays
 *   Validation in a compact constructor, in THIS order:
 *     id null or blank     → IllegalArgumentException("id must not be blank")
 *     amount <= 0          → IllegalArgumentException("amount must be > 0")
 *     (TravelClaim only) tripDays < 1 → IllegalArgumentException("tripDays must be >= 1")
 *
 * ── CLASS: ClaimProcessor  (static methods only) ───────────────────────
 *   1. payout(Claim c) → double
 *        null claim → IllegalArgumentException("claim must not be null")
 *        AutoClaim:   atFault → 0.0, otherwise 80% of amount
 *        HealthClaim: hospitalised → 90% of amount, otherwise 70% of amount
 *        TravelClaim: the smaller of amount and (500.0 * tripDays)
 *   2. label(Claim c) → String      "AUTO", "HEALTH" or "TRAVEL"
 *   3. describe(Claim c) → String   (formats use %.2f for the amount)
 *        AutoClaim:   "AUTO <id>: <amount> [at fault]"  or  "... [not at fault]"
 *        HealthClaim: "HEALTH <id>: <amount> [hospitalised]"  or  "... [outpatient]"
 *        TravelClaim: "TRAVEL <id>: <amount> [<tripDays> days]"
 *        e.g.  AUTO A-1: 1000.00 [not at fault]
 *   4. sortedForReview(List<Claim> claims) → List<Claim>
 *        amount DESCENDING, then id ASCENDING; returns a NEW unmodifiable list;
 *        the input list must not be modified
 *   5. largest(List<Claim> claims) → Optional<Claim>
 *        the claim that comes FIRST in the review order (largest amount; ties →
 *        lowest id); Optional.empty() for an empty list
 *   6. totalPayoutByType(List<Claim> claims) → Map<String, Double>
 *        key = label(claim); value = sum of payout(claim) per key; keys sorted
 *        alphabetically; only types that occur appear; empty input → empty map
 *
 * ── Design constraints ─────────────────────────────────────────────────
 *   - payout, label and describe must be switch EXPRESSIONS over the sealed type
 *     with NO default branch and NO instanceof / casts.
 *   - describe must use record deconstruction patterns (bind the components in
 *     the case label; do not call the accessors).
 *   - The review ordering must be defined once and reused by sortedForReview and largest.
 *   - No hand-written sort or max loops; no isPresent()/get() on Optionals.
 *   - Do not modify main().
 */
public class ClaimsDeskPractice {

    // ── TODO 1: The Claim sealed interface body
    //    Add the two members every claim exposes: id() → String and amount() → double
    //    (the sealed interface shell below already lists the permitted records)
    sealed interface Claim permits AutoClaim, HealthClaim, TravelClaim {
        String id();
        double amount();
    }

    // ── TODO 2: The three records
    //    Each shell below needs its components (in the order given in the problem statement)
    //    and a compact constructor that validates in this order:
    //      id null or blank  → IllegalArgumentException("id must not be blank")
    //      amount <= 0       → IllegalArgumentException("amount must be > 0")
    //      TravelClaim only: tripDays < 1 → IllegalArgumentException("tripDays must be >= 1")
    //    AutoClaim:   String id, double amount, boolean atFault
    //    HealthClaim: String id, double amount, boolean hospitalised
    //    TravelClaim: String id, double amount, int tripDays
    //    Do NOT assign fields in the compact constructor; do NOT write accessors yourself
    record AutoClaim(String id, double amount, boolean atFault) implements Claim {
        AutoClaim {
            if(id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
            if(amount <= 0 ) throw new IllegalArgumentException("amount must be > 0");

        }
    }
    record HealthClaim(String id, double amount, boolean hospitalised) implements Claim {
        HealthClaim {
            if(id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
            if(amount <= 0 ) throw new IllegalArgumentException("amount must be > 0");

        }
    }
    record TravelClaim(String id, double amount, int tripDays) implements Claim {
        TravelClaim {
            if(id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
            if(amount <= 0 ) throw new IllegalArgumentException("amount must be > 0");
            if(tripDays < 1) throw new IllegalArgumentException("tripDays must be >= 1");
        }
    }

    static final class ClaimProcessor {

        private ClaimProcessor() { }

        // ── TODO 3: payout(Claim c) → double  AND  label(Claim c) → String  (static methods)
        //    payout:
        //      c == null → throw new IllegalArgumentException("claim must not be null")
        //      AutoClaim:   atFault → 0.0; not at fault → 80% of amount
        //      HealthClaim: hospitalised → 90% of amount; otherwise 70% of amount
        //      TravelClaim: the smaller of amount and 500.0 * tripDays
        //    label: AutoClaim → "AUTO", HealthClaim → "HEALTH", TravelClaim → "TRAVEL"
        //    Both must be switch EXPRESSIONS over the sealed type: no default branch,
        //    no instanceof, no casts, no if/else chain
        public static  double payout(Claim c){
            if(c == null) throw new IllegalArgumentException("claim must not be null");

            return switch (c) {
                case AutoClaim autoClaim -> autoClaim.atFault ? 0.0 : 0.8 * autoClaim.amount();
                case HealthClaim healthClaim -> healthClaim.hospitalised
                        ? 0.9 * healthClaim.amount() : 0.7 * healthClaim.amount();
                case TravelClaim travelClaim -> Math.min(travelClaim.amount(), 500.0 * travelClaim.tripDays());
            };
        }

        public static String label(Claim c){
            return switch (c){
                case AutoClaim a -> "AUTO";
                case HealthClaim h -> "HEALTH";
                case TravelClaim t -> "TRAVEL";
            };
        }


        // ── TODO 4: describe(Claim c) → String  (static)
        //    AutoClaim:   "AUTO <id>: <amount %.2f> [at fault]" or "[not at fault]"
        //    HealthClaim: "HEALTH <id>: <amount %.2f> [hospitalised]" or "[outpatient]"
        //    TravelClaim: "TRAVEL <id>: <amount %.2f> [<tripDays> days]"
        //    e.g. "AUTO A-1: 1000.00 [not at fault]"
        //    Use a switch expression with RECORD DECONSTRUCTION patterns: bind the
        //    components in the case label instead of calling accessors; no default
        public static String describe(Claim c) {
            return switch (c) {
                case AutoClaim(String id, double amount, boolean atFault) -> String.format("AUTO %s: %.2f [%s]", id, amount, atFault ? "at fault" : "not at fault");
                case HealthClaim(String id, double amount, boolean hospitalised) -> String.format("HEALTH %s: %.2f [%s]", id, amount, hospitalised ? "hospitalised" : "outpatient");
                case TravelClaim(String id, double amount, int tripDays) -> String.format("Travel %s: %.2f [%d days]", id, amount, tripDays);
            };
        }

        // ── TODO 5: Review order, sortedForReview() and largest()  (static)
        //    One private static final Comparator<Claim>: amount DESCENDING, then id ASCENDING
        //      (primitive-specialised factory method, not a boxing one; `reversed` applies
        //       only to the amount key; define it once)
        //    sortedForReview(List<Claim> claims) → List<Claim>
        //      A new unmodifiable list in review order; the argument is NOT modified
        //    largest(List<Claim> claims) → Optional<Claim>
        //      The claim that comes first in the review order; Optional.empty() when the
        //      list is empty. Reuse the same comparator; no loops; no isPresent()/get()
        private static final Comparator<Claim> claimComparator = Comparator.comparingDouble(Claim::amount).reversed().thenComparing(Claim::id);

        public static List<Claim> sortedForReview(List<Claim> claims){
            return claims.stream().sorted(claimComparator).toList();
        }

        public static Optional<Claim> largest(List<Claim> claims){
            return claims.stream().max(claimComparator);
        }

        // ── TODO 6: totalPayoutByType(List<Claim> claims) → Map<String, Double>  (static)
        //    Key = label(claim); value = sum of payout(claim) over claims with that label
        //    Keys in alphabetical order (sorted map); only labels that occur appear;
        //    empty input → empty map
        //    Use a stream with a grouping collector (no manual loop / merge)
        //    Hint on typing: return the collector result directly from the method
        public static Map<String, Double> totalPayoutByType(List<Claim> claims) {
            return claims.stream().collect(Collectors.groupingBy(ClaimProcessor::label, TreeMap::new, Collectors.summingDouble(ClaimProcessor::payout)));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // DO NOT MODIFY main()
    // ══════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: records — toString, accessors, value equality (uncomment after TODO 1 and 2) ──
         AutoClaim a = new AutoClaim("A-1", 1000, false);
         System.out.println(a);                               // expected: AutoClaim[id=A-1, amount=1000.0, atFault=false]
         System.out.println(a.id() + " " + a.amount());       // expected: A-1 1000.0
         System.out.println(a.equals(new AutoClaim("A-1", 1000, false)));   // expected: true
         Claim viaInterface = a;
         System.out.println(viaInterface.id());               // expected: A-1

        // ── Test 2: compact-constructor validation (uncomment after TODO 2) ──
         try { new AutoClaim(" ", 5, false); }  catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }   // expected: id must not be blank
         try { new HealthClaim("H", 0, true); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }   // expected: amount must be > 0
         try { new TravelClaim("T", 50, 0); }   catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }   // expected: tripDays must be >= 1

        // ── Test 3: payout and label via switch expressions (uncomment after TODO 3) ──
         List<Claim> claims = List.of(
                 new AutoClaim("A-1", 1000, false), new AutoClaim("A-2", 400, true),
                 new AutoClaim("A-3", 1000, false), new HealthClaim("H-1", 2000, true),
                 new HealthClaim("H-2", 300, false), new TravelClaim("T-1", 900, 1),
                 new TravelClaim("T-2", 1200, 3));
         StringBuilder sb = new StringBuilder();
         for (Claim c : claims) sb.append(ClaimProcessor.label(c)).append('=').append(ClaimProcessor.payout(c)).append(' ');
         System.out.println(sb.toString().trim());
         // expected: AUTO=800.0 AUTO=0.0 AUTO=800.0 HEALTH=1800.0 HEALTH=210.0 TRAVEL=500.0 TRAVEL=1200.0
         try { ClaimProcessor.payout(null); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }     // expected: claim must not be null

        // ── Test 4: describe with record patterns (uncomment after TODO 4) ──
         System.out.println(ClaimProcessor.describe(claims.get(0)));   // expected: AUTO A-1: 1000.00 [not at fault]
         System.out.println(ClaimProcessor.describe(claims.get(1)));   // expected: AUTO A-2: 400.00 [at fault]
         System.out.println(ClaimProcessor.describe(claims.get(3)));   // expected: HEALTH H-1: 2000.00 [hospitalised]
         System.out.println(ClaimProcessor.describe(claims.get(4)));   // expected: HEALTH H-2: 300.00 [outpatient]
         System.out.println(ClaimProcessor.describe(claims.get(6)));   // expected: TRAVEL T-2: 1200.00 [3 days]

        // ── Test 5: sortedForReview — order, tie-break, input untouched, unmodifiable (uncomment after TODO 5) ──
         List<Claim> input = new ArrayList<>(claims);
         List<Claim> sorted = ClaimProcessor.sortedForReview(input);
         System.out.println(sorted.stream().map(Claim::id).toList());   // expected: [H-1, T-2, A-1, A-3, T-1, A-2, H-2]
         System.out.println(input.stream().map(Claim::id).toList());    // expected: [A-1, A-2, A-3, H-1, H-2, T-1, T-2]
         try { sorted.add(a); } catch (UnsupportedOperationException e) { System.out.println("unmodifiable"); }   // expected: unmodifiable

        // ── Test 6: largest — Optional, tie-break, empty (uncomment after TODO 5) ──
         System.out.println(ClaimProcessor.largest(claims).map(Claim::id).orElse("none"));                    // expected: H-1
         System.out.println(ClaimProcessor.largest(List.of(claims.get(2), claims.get(0))).map(Claim::id).orElse("none"));   // expected: A-1
         System.out.println(ClaimProcessor.largest(List.of()).isPresent());                                   // expected: false

        // ── Test 7: totalPayoutByType — grouping, sorted keys, empty (uncomment after TODO 3 and 6) ──
         System.out.println(ClaimProcessor.totalPayoutByType(claims));   // expected: {AUTO=1600.0, HEALTH=2010.0, TRAVEL=1700.0}
         System.out.println(ClaimProcessor.totalPayoutByType(List.of(claims.get(5))));   // expected: {TRAVEL=500.0}
         System.out.println(ClaimProcessor.totalPayoutByType(List.of()));                // expected: {}
    }
}

/*
 * ═══════════════════════════════════════════════════════════════════════
 * HINTS — read one level at a time, only when stuck
 * ═══════════════════════════════════════════════════════════════════════
 *
 * ── HINT 1 (Gentle) ────────────────────────────────────────────────────
 *   - A record gives you the accessor methods for free. If the interface asks
 *     for methods with the SAME names as the components, you write nothing extra.
 *   - Validation in a record belongs in a special constructor form that has no
 *     parameter list; the fields are assigned for you after it finishes.
 *   - If the compiler knows every possible subtype, a switch can be checked for
 *     completeness. What does that say about the default branch?
 *   - "Largest, ties by lowest id" — which end of the review order is that?
 *   - Collectors can take a "what kind of map?" supplier. Which Map keeps keys sorted?
 *
 * ── HINT 2 (Direct) ────────────────────────────────────────────────────
 *   - interface body: String id(); double amount();
 *   - record AutoClaim(String id, double amount, boolean atFault) implements Claim {
 *         AutoClaim { ...two checks... }
 *     }
 *   - return switch (c) { case AutoClaim a -> ...; case HealthClaim h -> ...; case TravelClaim t -> ...; };
 *   - ternaries or a `when` guard for the atFault / hospitalised branches; Math.min for the travel cap
 *   - case AutoClaim(String id, double amount, boolean atFault) -> ...
 *   - Comparator.comparingDouble(Claim::amount).reversed().thenComparing(Claim::id)
 *   - claims.stream().sorted(ORDER).toList();   claims.stream().min(ORDER)
 *   - Collectors.groupingBy(ClaimProcessor::label, TreeMap::new, Collectors.summingDouble(ClaimProcessor::payout))
 *
 * ── HINT 3 (Near-solution) ─────────────────────────────────────────────
 *   sealed interface Claim permits AutoClaim, HealthClaim, TravelClaim {
 *       String id();
 *       double amount();
 *   }
 *   record AutoClaim(String id, double amount, boolean atFault) implements Claim {
 *       AutoClaim { ...two checks... }
 *   }
 *   // HealthClaim: same two checks; TravelClaim: two checks + tripDays
 *
 *   static final class ClaimProcessor {
 *       private static final Comparator<Claim> REVIEW_ORDER = ...;
 *       static double payout(Claim c)            { ...null check; return switch (c) { ... }; }
 *       static String label(Claim c)             { return switch (c) { ... }; }
 *       static String describe(Claim c)          { return switch (c) { case AutoClaim(...) -> ...; ... }; }
 *       static List<Claim> sortedForReview(List<Claim> claims) { ... }
 *       static Optional<Claim> largest(List<Claim> claims)     { ... }
 *       static Map<String, Double> totalPayoutByType(List<Claim> claims) { ... }
 *   }
 */
