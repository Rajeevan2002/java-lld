package com.ramkumar.lld.javareadiness.enums.code;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Worked Example — Phase 4 Java Readiness | 4.4 Enums as Design Tools
 * Scenario A: Food-delivery order (size, coupon, lifecycle)
 *
 * Concepts demonstrated (each is labelled inline):
 *   [FIELDS]      enum with final fields + private constructor
 *   [LOOKUP]      static map + fromCode(...) with a clear error
 *   [CONST-BODY]  constant-specific behaviour via an abstract method
 *   [STATE]       transitions owned by the enum (EnumMap<State, EnumSet<State>>)
 *   [SWITCH]      exhaustive switch expression, no default
 *   [ENUMMAP]     zero-filled EnumMap report, deterministic order
 *   [ENUMSET]     EnumSet.complementOf for "still open" states
 */
public class FoodOrderDemo {

    // ── [FIELDS] ────────────────────────────────────────────────────
    enum Size {
        SMALL(250, 0.0), MEDIUM(350, 30.0), LARGE(500, 60.0);

        private final int ml;                 // final: constants are shared singletons
        private final double surcharge;

        Size(int ml, double surcharge) {      // implicitly private
            this.ml = ml;
            this.surcharge = surcharge;
        }

        int ml() { return ml; }
        double surcharge() { return surcharge; }

        // [LOOKUP] Static fields are initialised AFTER the constants, so this block may call values().
        private static final Map<String, Size> BY_CODE = new HashMap<>();
        static {
            for (Size s : values()) {
                BY_CODE.put(s.name().toLowerCase(), s);
            }
        }

        static Size fromCode(String code) {
            Size s = (code == null) ? null : BY_CODE.get(code.trim().toLowerCase());
            if (s == null) {
                throw new IllegalArgumentException("unknown size: " + code);
            }
            return s;
        }
    }

    // ── [CONST-BODY] each constant supplies its own discount rule ──────
    enum Coupon {
        NONE {
            @Override double apply(double subtotal) { return subtotal; }
        },
        FLAT50 {
            @Override double apply(double subtotal) { return Math.max(0.0, subtotal - 50.0); }
        },
        PERCENT10 {
            @Override double apply(double subtotal) { return subtotal * 0.90; }
        };

        // The compiler rejects any constant that forgets to implement this.
        abstract double apply(double subtotal);
    }

    // ── [STATE] transitions live in the enum, not in the Order class ───
    enum Status {
        PLACED, PREPARING, OUT_FOR_DELIVERY, DELIVERED, CANCELLED;

        // Constructor args cannot name later constants (illegal forward reference),
        // so the graph is wired in a static block after all constants exist.
        private static final Map<Status, Set<Status>> NEXT = new EnumMap<>(Status.class);
        static {
            NEXT.put(PLACED,           EnumSet.of(PREPARING, CANCELLED));
            NEXT.put(PREPARING,        EnumSet.of(OUT_FOR_DELIVERY, CANCELLED));
            NEXT.put(OUT_FOR_DELIVERY, EnumSet.of(DELIVERED));
            NEXT.put(DELIVERED,        EnumSet.noneOf(Status.class));
            NEXT.put(CANCELLED,        EnumSet.noneOf(Status.class));
        }

        boolean canMoveTo(Status target) { return NEXT.get(this).contains(target); }
        boolean isTerminal()             { return NEXT.get(this).isEmpty(); }

        // [SWITCH] no default: adding a constant breaks compilation here, which is the point.
        String label() {
            return switch (this) {
                case PLACED -> "Waiting";
                case PREPARING -> "Cooking";
                case OUT_FOR_DELIVERY -> "On the way";
                case DELIVERED, CANCELLED -> "Closed";
            };
        }
    }

    static class Order {
        private final String id;
        private final Size size;
        private final double basePrice;
        private Status status = Status.PLACED;

        Order(String id, Size size, double basePrice) {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
            if (size == null) throw new IllegalArgumentException("size must not be null");
            if (basePrice <= 0) throw new IllegalArgumentException("basePrice must be > 0");
            this.id = id;
            this.size = size;
            this.basePrice = basePrice;
        }

        String id() { return id; }
        Status status() { return status; }

        double total(Coupon coupon) {
            return coupon.apply(basePrice + size.surcharge());
        }

        void moveTo(Status target) {
            if (!status.canMoveTo(target)) {
                throw new IllegalStateException("illegal transition: " + status + " -> " + target);
            }
            status = target;
        }

        @Override
        public String toString() {
            return id + "[" + size + ", " + status + "]";
        }
    }

    // ── [ENUMMAP] ───────────────────────────────────────────────────
    static Map<Status, Integer> countByStatus(Iterable<Order> orders) {
        Map<Status, Integer> counts = new EnumMap<>(Status.class);
        for (Status s : Status.values()) counts.put(s, 0);       // zero-fill
        for (Order o : orders) counts.merge(o.status(), 1, Integer::sum);
        return counts;
    }

    public static void main(String[] args) {
        System.out.println("=== [FIELDS][LOOKUP] size ===");
        System.out.println("LARGE ml          : " + Size.LARGE.ml());               // 500
        System.out.println("fromCode(' Medium'): " + Size.fromCode(" Medium"));     // MEDIUM
        try {
            Size.fromCode("huge");
        } catch (IllegalArgumentException e) {
            System.out.println("fromCode(huge)    : " + e.getMessage());            // unknown size: huge
        }

        System.out.println("\n=== [CONST-BODY] coupons on 200 + LARGE(60) = 260 ===");
        Order o1 = new Order("O-1", Size.LARGE, 200.0);
        for (Coupon c : Coupon.values()) {
            System.out.printf("%-10s %.2f%n", c, o1.total(c));                      // 260.00 / 210.00 / 234.00
        }

        System.out.println("\n=== [STATE] transitions ===");
        System.out.println("PLACED->PREPARING : " + Status.PLACED.canMoveTo(Status.PREPARING));   // true
        System.out.println("PLACED->DELIVERED : " + Status.PLACED.canMoveTo(Status.DELIVERED));   // false
        o1.moveTo(Status.PREPARING);
        try {
            o1.moveTo(Status.PLACED);
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());          // illegal transition: PREPARING -> PLACED
        }

        System.out.println("\n=== [SWITCH] labels ===");
        Map<String, String> labels = new LinkedHashMap<>();
        for (Status s : Status.values()) labels.put(s.name(), s.label());
        System.out.println(labels);

        System.out.println("\n=== [ENUMMAP][ENUMSET] report ===");
        Order o2 = new Order("O-2", Size.SMALL, 100.0);
        Order o3 = new Order("O-3", Size.MEDIUM, 150.0);
        o3.moveTo(Status.CANCELLED);
        System.out.println(countByStatus(java.util.List.of(o1, o2, o3)));
        // {PLACED=1, PREPARING=1, OUT_FOR_DELIVERY=0, DELIVERED=0, CANCELLED=1}
        System.out.println("open states       : " + EnumSet.complementOf(EnumSet.of(Status.DELIVERED, Status.CANCELLED)));
        // [PLACED, PREPARING, OUT_FOR_DELIVERY]
    }
}
