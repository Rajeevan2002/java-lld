package com.ramkumar.lld.designpatterns.behavioral.strategy.practice;


/**
 * Practice Exercise — Strategy Pattern: Shipping Cost Calculator
 *
 * <p><b>Scenario B — Interchangeable algorithms via composition</b>
 *
 * <p>A shipping platform calculates delivery costs at checkout. The formula varies by
 * tier (Standard, Express, Overnight), but the order details are always the same inputs.
 * The {@code Order} context must delegate cost calculation to a swappable
 * {@code ShippingStrategy} — with no {@code instanceof} or type-checking in {@code Order}.
 *
 * <p><b>Class hierarchy (do not change the names):</b>
 * <pre>
 *   ShippingStrategy    [Strategy interface]    ← TODO 1
 *   StandardShipping    [ConcreteStrategy]      ← TODO 2
 *   ExpressShipping     [ConcreteStrategy]      ← TODO 3
 *   OvernightShipping   [ConcreteStrategy]      ← TODO 4
 *   Order               [Context]               ← TODOs 5–8
 * </pre>
 *
 * <p><b>ShippingStrategy (interface)</b> (TODO 1):
 * <ul>
 *   <li>{@code calculate(double weightKg, double declaredValue) → double}
 *       — returns total shipping cost in USD.</li>
 * </ul>
 *
 * <p><b>StandardShipping</b> (TODO 2):
 * <ul>
 *   <li>Field: {@code ratePerKg = 3.50} (private final; NOT a constructor parameter).</li>
 *   <li>{@code calculate}: {@code weightKg * ratePerKg}, minimum charge {@code 5.00}.</li>
 *   <li>Print: {@code System.out.printf("[Standard] %.2f kg → $%.2f%n", weightKg, cost)}</li>
 * </ul>
 *
 * <p><b>ExpressShipping</b> (TODO 3):
 * <ul>
 *   <li>Fields: {@code ratePerKg = 7.00}, {@code baseFee = 10.00}
 *       (private final; NOT constructor parameters).</li>
 *   <li>{@code calculate}: {@code baseFee + weightKg * ratePerKg}.</li>
 *   <li>Print: {@code System.out.printf("[Express] %.2f kg → $%.2f%n", weightKg, cost)}</li>
 * </ul>
 *
 * <p><b>OvernightShipping</b> (TODO 4):
 * <ul>
 *   <li>Fields: {@code ratePerKg = 15.00}, {@code insuranceRate = 0.02}
 *       (private final; NOT constructor parameters).</li>
 *   <li>{@code calculate}: {@code weightKg * ratePerKg + declaredValue * insuranceRate}.</li>
 *   <li>Print: {@code System.out.printf("[Overnight] %.2f kg, $%.2f declared → $%.2f%n",
 *       weightKg, declaredValue, cost)}</li>
 * </ul>
 *
 * <p><b>Order (Context)</b> (TODOs 5–8):
 * <ul>
 *   <li>Fields: {@code private final String orderId},
 *       {@code private final double weightKg},
 *       {@code private final double declaredValue},
 *       {@code private ShippingStrategy strategy} — mutable; can be swapped.</li>
 *   <li>Constructor: {@code Order(String orderId, double weightKg, double declaredValue,
 *       ShippingStrategy strategy)}.
 *       Validate: {@code weightKg <= 0} →
 *       throw {@code new IllegalArgumentException("weightKg must be > 0")}.</li>
 *   <li>{@code setStrategy(ShippingStrategy strategy)} — replaces current strategy.</li>
 *   <li>{@code checkout() → double}:
 *       delegates to {@code strategy.calculate(weightKg, declaredValue)};
 *       prints {@code System.out.printf("[Order %s] shipping cost: $%.2f%n", orderId, cost)};
 *       returns the cost.</li>
 * </ul>
 *
 * <p><b>Design constraints:</b>
 * <ul>
 *   <li>{@code Order} must reference only the {@code ShippingStrategy} interface —
 *       never a concrete strategy class.</li>
 *   <li>No {@code instanceof} or type-checking anywhere in {@code Order}.</li>
 *   <li>{@code ratePerKg}, {@code baseFee}, {@code insuranceRate} are private implementation
 *       details of each strategy — NOT constructor parameters.</li>
 * </ul>
 */
public class ShippingCalculatorPractice {

    // ── Strategy interface ─────────────────────────────────────────────────────
    interface ShippingStrategy {
        double calculate(double weightKg, double declaredValue);
    }

    // ── ConcreteStrategy 1 ─────────────────────────────────────────────────────
    static class StandardShipping implements ShippingStrategy {
        private final double ratePerKg = 3.50;
        @Override
        public double calculate(double weightKg, double declaredValue){
            double cost = weightKg * ratePerKg;
            if(cost < 5.0 ){
                cost = 5.0;
            }
            System.out.printf("[Standard] %.2f kg  → $%.2f%n", weightKg, cost);
            return cost;
        }
    }

    static class ExpressShipping implements ShippingStrategy {
        private final double ratePerKg = 7.00;
        private final double baseFee = 10.00;

        @Override
        public double calculate(double weightKg, double declaredValue){
            double cost = baseFee + weightKg * ratePerKg;
            System.out.printf("[Express] %.2f kg → $%.2f%n", weightKg, cost);
            return cost;
        }
    }

    static class OvernightShipping implements ShippingStrategy {
        private final double ratePerKg = 15.00;
        private final double insuranceRate = 0.02;

        @Override
        public double calculate(double weightKg, double declaredValue) {
            double cost = weightKg * ratePerKg +  declaredValue * insuranceRate;
            System.out.printf("[Overnight] %.2f kg, $%.2f declared → $%.2f%n", weightKg, declaredValue, cost);
            return cost;
        }
    }

    static class Order {
        private final String orderId;
        private final double weightKg;
        private final double declaredValue;
        private ShippingStrategy strategy;

        public Order(String orderId, double weightKg, double declaredValue, ShippingStrategy strategy){
            if(weightKg <= 0){
                throw new IllegalArgumentException("weightKg must be > 0");
            }
            this.orderId = orderId;
            this.weightKg = weightKg;
            this.declaredValue = declaredValue;
            this.strategy = strategy;
        }

        public void setStrategy(ShippingStrategy strategy){
            this.strategy = strategy;
        }

        public double checkout(){
            double cost = strategy.calculate(weightKg, declaredValue);
            System.out.printf("[Order %s] shipping cost: $%.2f%n", orderId, cost);
            return cost;
        }
    }

    // ── DO NOT MODIFY — test harness ──────────────────────────────────────────
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: StandardShipping — basic rate (uncomment after TODO 2) ────────────────
         StandardShipping std = new StandardShipping();
         double c1 = std.calculate(4.0, 0);   // 4.0 * 3.50 = 14.00
         System.out.println("Test 1 — Standard 4kg: " + (Math.abs(c1 - 14.00) < 0.001 ? "PASSED" : "FAILED (got: " + c1 + ")"));

        // ── Test 2: StandardShipping — minimum charge (uncomment after TODO 2) ───────────
         double c2 = std.calculate(1.0, 0);   // 1.0 * 3.50 = 3.50 → minimum 5.00
         System.out.println("Test 2 — Standard minimum $5.00: " + (Math.abs(c2 - 5.00) < 0.001 ? "PASSED" : "FAILED (got: " + c2 + ")"));

        // ── Test 3: ExpressShipping — baseFee + rate (uncomment after TODO 3) ───────────
         ExpressShipping exp = new ExpressShipping();
         double c3 = exp.calculate(3.0, 0);   // 10.00 + 3.0 * 7.00 = 31.00
         System.out.println("Test 3 — Express 3kg: " + (Math.abs(c3 - 31.00) < 0.001 ? "PASSED" : "FAILED (got: " + c3 + ")"));

        // ── Test 4: OvernightShipping — rate + insurance (uncomment after TODO 4) ────────
         OvernightShipping ovn = new OvernightShipping();
         double c4 = ovn.calculate(2.0, 500.0);   // 2.0*15.00 + 500.0*0.02 = 30.00 + 10.00 = 40.00
         System.out.println("Test 4 — Overnight 2kg $500: " + (Math.abs(c4 - 40.00) < 0.001 ? "PASSED" : "FAILED (got: " + c4 + ")"));

        // ── Test 5: Order.checkout() delegates to strategy (uncomment after TODO 8) ──────
         Order o5 = new Order("ORD-001", 4.0, 200.0, new StandardShipping());
         double c5 = o5.checkout();   // [Standard] 4.0 * 3.50 = 14.00
         System.out.println("Test 5 — Order checkout Standard: " + (Math.abs(c5 - 14.00) < 0.001 ? "PASSED" : "FAILED (got: " + c5 + ")"));

        // ── Test 6: Runtime strategy swap (uncomment after TODOs 7–8) ────────────────────
         Order o6 = new Order("ORD-002", 3.0, 150.0, new StandardShipping());
         double c6a = o6.checkout();                  // Standard: 3.0 * 3.50 = 10.50
         o6.setStrategy(new ExpressShipping());
         double c6b = o6.checkout();                  // Express: 10.00 + 3.0 * 7.00 = 31.00
         o6.setStrategy(new OvernightShipping());
         double c6c = o6.checkout();                  // Overnight: 3.0*15.00 + 150.0*0.02 = 48.00
         System.out.println("Test 6a — swap to Express: " + (Math.abs(c6b - 31.00) < 0.001 ? "PASSED" : "FAILED (got: " + c6b + ")"));
         System.out.println("Test 6b — swap to Overnight: " + (Math.abs(c6c - 48.00) < 0.001 ? "PASSED" : "FAILED (got: " + c6c + ")"));

        // ── Test 7: Validation — weightKg <= 0 throws (uncomment after TODO 6) ──────────
         try {
             new Order("ORD-BAD", 0.0, 100.0, new StandardShipping());
             System.out.println("Test 7 — zero weight: FAILED (no exception)");
         } catch (IllegalArgumentException e) {
             System.out.println("Test 7 — zero weight throws IAE: "
                 + ("weightKg must be > 0".equals(e.getMessage()) ? "PASSED" : "FAILED (msg: " + e.getMessage() + ")"));
         }

        // ── Test 8: Polymorphic use — all strategies via interface reference ──────────────
         ShippingStrategy[] tiers = { new StandardShipping(), new ExpressShipping(), new OvernightShipping() };
         double[] expected = { 14.00, 31.00, 40.00 };  // for 4kg, $200 declared
         boolean poly = true;
         for (int i = 0; i < tiers.length; i++) {
             Order o = new Order("ORD-POLY-" + i, 4.0, 200.0, tiers[i]);
             double cost = o.checkout();
             if (Math.abs(cost - expected[i]) >= 0.001) { poly = false; System.out.println("  FAILED tier " + i + " got " + cost); }
         }
         System.out.println("Test 8 — polymorphic strategies: " + (poly ? "PASSED" : "FAILED"));
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HINTS — read only if stuck
    // ════════════════════════════════════════════════════════════════════════════

    // HINT 1 (Gentle):
    //   Order needs to perform different calculations depending on a setting — but
    //   it should not contain any if/else for the different tiers. Think about
    //   extracting the calculation behind a common "contract" that each tier fulfils,
    //   and let Order hold a reference to that contract rather than to a specific tier.

    // HINT 2 (Direct):
    //   Use the Strategy pattern.
    //   ShippingStrategy is an interface with one method: calculate(weightKg, declaredValue).
    //   StandardShipping, ExpressShipping, and OvernightShipping each implement it.
    //   Order stores a private ShippingStrategy field (NOT typed as a concrete class)
    //   and calls strategy.calculate(...) inside checkout().

    // HINT 3 (Near-solution skeleton — class outlines only, no method bodies):
    //
    //   interface ShippingStrategy {
    //       double calculate(double weightKg, double declaredValue);
    //   }
    //
    //   static class StandardShipping implements ShippingStrategy {
    //       private final double ratePerKg = 3.50;
    //       @Override public double calculate(double weightKg, double declaredValue) { ... }
    //   }
    //
    //   static class ExpressShipping implements ShippingStrategy {
    //       private final double ratePerKg = 7.00;
    //       private final double baseFee   = 10.00;
    //       @Override public double calculate(double weightKg, double declaredValue) { ... }
    //   }
    //
    //   static class OvernightShipping implements ShippingStrategy {
    //       private final double ratePerKg     = 15.00;
    //       private final double insuranceRate = 0.02;
    //       @Override public double calculate(double weightKg, double declaredValue) { ... }
    //   }
    //
    //   static class Order {
    //       private final String           orderId;
    //       private final double           weightKg;
    //       private final double           declaredValue;
    //       private       ShippingStrategy strategy;   // interface type, NOT concrete
    //       Order(String orderId, double weightKg, double declaredValue, ShippingStrategy strategy) { ... }
    //       void   setStrategy(ShippingStrategy strategy) { ... }
    //       double checkout() { ... }
    //   }
}
