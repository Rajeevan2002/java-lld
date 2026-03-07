package com.ramkumar.lld.designpatterns.behavioral.strategy.results;

/**
 * Reference solution — Strategy Pattern: Shipping Cost Calculator
 *
 * <p>Key decisions vs common mistakes:
 * <ul>
 *   <li>{@code Order.strategy} is typed as {@code ShippingStrategy} (interface) — never a concrete class.
 *       Typing it as {@code StandardShipping} breaks polymorphism: {@code setStrategy()} could
 *       only accept {@code StandardShipping}, making runtime swap impossible.</li>
 *   <li>Rate constants ({@code ratePerKg}, {@code baseFee}, {@code insuranceRate}) live inside
 *       each strategy, not in {@code Order}. The context has no business knowing the rates.</li>
 *   <li>{@code checkout()} contains zero branching — one delegation call covers all current and
 *       future strategies. Any {@code instanceof} inside {@code checkout()} is a design failure.</li>
 *   <li>Note: Test 8 expected values in the practice skeleton had a copy-paste error
 *       (values from earlier tests with different inputs). Correct values are recalculated here.</li>
 * </ul>
 */
public class StrategyReference {

    // ── [Strategy interface] — the algorithm contract ────────────────────────
    interface ShippingStrategy {
        // [Contract] Context passes its data; strategy returns the cost.
        // Context never knows which formula is being applied.
        double calculate(double weightKg, double declaredValue);
    }

    // ── [ConcreteStrategy 1] ──────────────────────────────────────────────────
    static class StandardShipping implements ShippingStrategy {

        // [StrategyState] Algorithm config lives HERE, not in Order.
        // private final = immutable; not a constructor parameter = caller can't override.
        private final double ratePerKg = 3.50;

        @Override
        public double calculate(double weightKg, double declaredValue) {
            // [MinimumCharge] Math.max is cleaner than an if-branch for floor values.
            double cost = Math.max(weightKg * ratePerKg, 5.00);
            System.out.printf("[Standard] %.2f kg → $%.2f%n", weightKg, cost);
            return cost;
        }
    }

    // ── [ConcreteStrategy 2] ──────────────────────────────────────────────────
    static class ExpressShipping implements ShippingStrategy {

        private final double ratePerKg = 7.00;
        private final double baseFee   = 10.00;

        @Override
        public double calculate(double weightKg, double declaredValue) {
            double cost = baseFee + weightKg * ratePerKg;
            System.out.printf("[Express] %.2f kg → $%.2f%n", weightKg, cost);
            return cost;
        }
    }

    // ── [ConcreteStrategy 3] ──────────────────────────────────────────────────
    static class OvernightShipping implements ShippingStrategy {

        private final double ratePerKg     = 15.00;
        private final double insuranceRate = 0.02;

        @Override
        public double calculate(double weightKg, double declaredValue) {
            // [MultiFactorFormula] Strategy uses BOTH context parameters.
            // This is why checkout() must pass declaredValue even though Standard/Express ignore it.
            double cost = weightKg * ratePerKg + declaredValue * insuranceRate;
            System.out.printf("[Overnight] %.2f kg, $%.2f declared → $%.2f%n",
                weightKg, declaredValue, cost);
            return cost;
        }
    }

    // ── [Context] ─────────────────────────────────────────────────────────────
    static class Order {

        private final String orderId;
        private final double weightKg;
        private final double declaredValue;
        // [CriticalLine] Typed as the interface — NOT StandardShipping or any concrete class.
        // This single decision is what makes the whole pattern work.
        private ShippingStrategy strategy;

        Order(String orderId, double weightKg, double declaredValue, ShippingStrategy strategy) {
            if (weightKg <= 0) {
                throw new IllegalArgumentException("weightKg must be > 0");
            }
            this.orderId       = orderId;
            this.weightKg      = weightKg;
            this.declaredValue = declaredValue;
            this.strategy      = strategy;
        }

        // [RuntimeSwap] Accepts any ShippingStrategy — including ones not yet written.
        // This works ONLY because the field is typed as the interface.
        void setStrategy(ShippingStrategy strategy) {
            this.strategy = strategy;
        }

        // [ZeroBranching] No if/else, no instanceof, no switch.
        // One line handles Standard, Express, Overnight, and any future tier.
        double checkout() {
            double cost = strategy.calculate(weightKg, declaredValue);
            System.out.printf("[Order %s] shipping cost: $%.2f%n", orderId, cost);
            return cost;
        }
    }

    // ── Reference main() ──────────────────────────────────────────────────────
    public static void main(String[] args) {

        // ── Test 1: StandardShipping — basic rate ─────────────────────────────
        StandardShipping std = new StandardShipping();
        double c1 = std.calculate(4.0, 0);   // 4.0 * 3.50 = 14.00
        System.out.println("Test 1 — Standard 4kg: "
            + (Math.abs(c1 - 14.00) < 0.001 ? "PASSED" : "FAILED (got: " + c1 + ")"));

        // ── Test 2: StandardShipping — minimum charge ─────────────────────────
        double c2 = std.calculate(1.0, 0);   // 1.0 * 3.50 = 3.50 → minimum 5.00
        System.out.println("Test 2 — Standard minimum $5.00: "
            + (Math.abs(c2 - 5.00) < 0.001 ? "PASSED" : "FAILED (got: " + c2 + ")"));

        // ── Test 3: ExpressShipping — baseFee + rate ──────────────────────────
        ExpressShipping exp = new ExpressShipping();
        double c3 = exp.calculate(3.0, 0);   // 10.00 + 3.0 * 7.00 = 31.00
        System.out.println("Test 3 — Express 3kg: "
            + (Math.abs(c3 - 31.00) < 0.001 ? "PASSED" : "FAILED (got: " + c3 + ")"));

        // ── Test 4: OvernightShipping — rate + insurance ──────────────────────
        OvernightShipping ovn = new OvernightShipping();
        double c4 = ovn.calculate(2.0, 500.0);   // 2.0*15.00 + 500.0*0.02 = 40.00
        System.out.println("Test 4 — Overnight 2kg $500: "
            + (Math.abs(c4 - 40.00) < 0.001 ? "PASSED" : "FAILED (got: " + c4 + ")"));

        // ── Test 5: Order.checkout() delegates to strategy ────────────────────
        Order o5 = new Order("ORD-001", 4.0, 200.0, new StandardShipping());
        double c5 = o5.checkout();
        System.out.println("Test 5 — Order checkout Standard: "
            + (Math.abs(c5 - 14.00) < 0.001 ? "PASSED" : "FAILED (got: " + c5 + ")"));

        // ── Test 6: Runtime strategy swap ─────────────────────────────────────
        Order o6 = new Order("ORD-002", 3.0, 150.0, new StandardShipping());
        o6.checkout();                           // Standard: 3.0 * 3.50 = 10.50
        o6.setStrategy(new ExpressShipping());
        double c6b = o6.checkout();              // Express: 10.00 + 3.0 * 7.00 = 31.00
        o6.setStrategy(new OvernightShipping());
        double c6c = o6.checkout();              // Overnight: 3.0*15.00 + 150.0*0.02 = 48.00
        System.out.println("Test 6a — swap to Express: "
            + (Math.abs(c6b - 31.00) < 0.001 ? "PASSED" : "FAILED (got: " + c6b + ")"));
        System.out.println("Test 6b — swap to Overnight: "
            + (Math.abs(c6c - 48.00) < 0.001 ? "PASSED" : "FAILED (got: " + c6c + ")"));

        // ── Test 7: Validation — weightKg <= 0 throws ─────────────────────────
        try {
            new Order("ORD-BAD", 0.0, 100.0, new StandardShipping());
            System.out.println("Test 7 — zero weight: FAILED (no exception)");
        } catch (IllegalArgumentException e) {
            System.out.println("Test 7 — zero weight throws IAE: "
                + ("weightKg must be > 0".equals(e.getMessage()) ? "PASSED" : "FAILED (msg: " + e.getMessage() + ")"));
        }

        // ── Test 8: Polymorphic use — CORRECTED expected values ──────────────
        // Exercise skeleton had wrong expected values (copied from 3kg/2kg tests).
        // Correct values for 4kg, $200 declared:
        //   Standard:  4.0 * 3.50           = 14.00
        //   Express:   10.00 + 4.0 * 7.00   = 38.00  (was incorrectly 31.00 in skeleton)
        //   Overnight: 4.0*15.00 + 200*0.02 = 64.00  (was incorrectly 40.00 in skeleton)
        ShippingStrategy[] tiers    = { new StandardShipping(), new ExpressShipping(), new OvernightShipping() };
        double[]           expected = { 14.00, 38.00, 64.00 };
        boolean poly = true;
        for (int i = 0; i < tiers.length; i++) {
            Order o = new Order("ORD-POLY-" + i, 4.0, 200.0, tiers[i]);
            double cost = o.checkout();
            if (Math.abs(cost - expected[i]) >= 0.001) {
                poly = false;
                System.out.println("  FAILED tier " + i + " got " + cost);
            }
        }
        System.out.println("Test 8 — polymorphic strategies: " + (poly ? "PASSED" : "FAILED"));

        // ── Test 9 (catches the most common mistake: concrete field type) ─────
        // If Order.strategy were typed as StandardShipping, setStrategy() could only
        // accept StandardShipping — passing ExpressShipping would fail to compile.
        // This test proves the field is typed as the interface by passing an anonymous
        // strategy (a type that doesn't exist at compile time of Order).
        ShippingStrategy flatRate = (w, v) -> {
            // Lambda = anonymous ShippingStrategy. Works ONLY if the field is typed as the interface.
            double cost = 9.99;
            System.out.printf("[FlatRate] %.2f kg → $%.2f%n", w, cost);
            return cost;
        };
        Order o9 = new Order("ORD-LAMBDA", 2.0, 0.0, new StandardShipping());
        o9.setStrategy(flatRate);   // would fail to compile if field were StandardShipping
        double c9 = o9.checkout();
        System.out.println("Test 9 — lambda strategy (proves interface field): "
            + (Math.abs(c9 - 9.99) < 0.001 ? "PASSED" : "FAILED (got: " + c9 + ")"));
    }
}
