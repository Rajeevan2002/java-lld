package com.ramkumar.lld.designpatterns.structural.decorator.practice;

/**
 * Decorator Pattern — Scenario B: Subscription Pricing
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * PROBLEM STATEMENT
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * You are building the pricing engine for a streaming service. Customers
 * start with a BasicPlan and can optionally add any combination of upgrades:
 * HD video, Family Sharing, and Offline Downloads. Each add-on is independent —
 * customers mix and match them in any order.
 *
 * Design constraint: you MUST NOT create a subclass per combination.
 * Use the Decorator pattern so that N add-ons require exactly N Decorator
 * classes and any combination can be assembled at runtime.
 *
 * ── Pricing rules ──────────────────────────────────────────────────────────
 *
 *   BasicPlan              label: "Basic Plan"                cost: $9.99/mo
 *   HDUpgradeDecorator     appends " + HD"                   adds: $3.00/mo
 *   FamilySharingDecorator appends " + Family Sharing"       adds: $5.00/mo
 *   DownloadsDecorator     appends " + Offline Downloads"    adds: $2.00/mo
 *
 * ── Interface contract ─────────────────────────────────────────────────────
 *
 *   Subscription
 *     getLabel()        → String   returns the plan's full display label
 *     getMonthlyCost()  → double   returns the total monthly cost in USD
 *
 * ── Design constraints ─────────────────────────────────────────────────────
 *   • No subclass per combination — use composition / Decorator.
 *   • SubscriptionDecorator's wrapped field must be private and final.
 *   • Each concrete decorator must call super(wrapped) — no direct field access.
 *   • getLabel() in each decorator must delegate to super.getLabel(), not
 *     hard-code the full label string.
 *   • getMonthlyCost() in each decorator must delegate to super.getMonthlyCost(),
 *     not hard-code the total cost.
 *
 * ── Class hierarchy (shells below — you fill in the members) ───────────────
 *
 *   interface Subscription                  ← TODO 1
 *   class BasicPlan implements Subscription ← TODO 2  (ConcreteComponent)
 *   abstract class SubscriptionDecorator    ← TODO 3  (BaseDecorator)
 *   class HDUpgradeDecorator                ← TODO 4  (ConcreteDecorator)
 *   class FamilySharingDecorator            ← TODO 5  (ConcreteDecorator)
 *   class DownloadsDecorator                ← TODO 6  (ConcreteDecorator)
 * ═══════════════════════════════════════════════════════════════════════════
 */
public class SubscriptionPricingPractice {

    interface Subscription {
        String getLabel();
        double getMonthlyCost();
    }

    static class BasicPlan implements Subscription {

        @Override
        public String getLabel() {
            return "Basic Plan";
        }

        @Override
        public double getMonthlyCost(){
            return 9.99;
        }
    }

    static abstract class SubscriptionDecorator implements Subscription {
        private final Subscription wrapped;

        public SubscriptionDecorator(Subscription wrapped){
            this.wrapped = wrapped;
        }

        @Override
        public String getLabel() {
            return wrapped.getLabel();
        }

        @Override
        public double getMonthlyCost() {
            return wrapped.getMonthlyCost();
        }
    }

    static class HDUpgradeDecorator extends SubscriptionDecorator {

        public HDUpgradeDecorator(Subscription wrappped){
            super(wrappped);
        }

        @Override
        public String getLabel() {
            return super.getLabel() + " + HD";
        }

        @Override
        public double getMonthlyCost() {
            return super.getMonthlyCost() + 3.00;
        }
    }

    static class FamilySharingDecorator extends SubscriptionDecorator {
        public FamilySharingDecorator(Subscription wrapped){
            super(wrapped);
        }

        @Override
        public String getLabel() {
            return super.getLabel() + " + Family Sharing";
        }

        @Override
        public double getMonthlyCost(){
            return super.getMonthlyCost() + 5.00;
        }
    }

    static class DownloadsDecorator extends SubscriptionDecorator {
        public DownloadsDecorator(Subscription wrapped){
            super(wrapped);
        }

        @Override
        public String getLabel() {
            return super.getLabel() + " + Offline Downloads";
        }

        @Override
        public double getMonthlyCost(){
            return super.getMonthlyCost() + 2.00;
        }

    }


    // ─────────────────────────────────────────────────────────────────────────
    // DO NOT MODIFY main() — uncomment each block after finishing the TODO it names.
    // Blocks build on each other: uncomment in order, starting with block 1.
    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: BasicPlan alone — label and cost (uncomment after TODO 2) ─────────────
         Subscription basic = new BasicPlan();
         System.out.println("Test 1a — label: "
             + ("Basic Plan".equals(basic.getLabel()) ? "PASSED" : "FAILED (got: " + basic.getLabel() + ")"));
         System.out.println("Test 1b — cost:  "
             + (String.format("%.2f", basic.getMonthlyCost()).equals("9.99") ? "PASSED" : "FAILED (got: " + basic.getMonthlyCost() + ")"));

        // ── Test 2: HD upgrade wraps BasicPlan (uncomment after TODO 4) ───────────────────
         Subscription hd = new HDUpgradeDecorator(new BasicPlan());
         System.out.println("Test 2a — label: "
             + ("Basic Plan + HD".equals(hd.getLabel()) ? "PASSED" : "FAILED (got: " + hd.getLabel() + ")"));
         System.out.println("Test 2b — cost:  "
             + (String.format("%.2f", hd.getMonthlyCost()).equals("12.99") ? "PASSED" : "FAILED (got: " + hd.getMonthlyCost() + ")"));

        // ── Test 3: Family Sharing wraps BasicPlan (uncomment after TODO 5) ──────────────
         Subscription family = new FamilySharingDecorator(new BasicPlan());
         System.out.println("Test 3a — label: "
             + ("Basic Plan + Family Sharing".equals(family.getLabel()) ? "PASSED" : "FAILED (got: " + family.getLabel() + ")"));
         System.out.println("Test 3b — cost:  "
             + (String.format("%.2f", family.getMonthlyCost()).equals("14.99") ? "PASSED" : "FAILED (got: " + family.getMonthlyCost() + ")"));

        // ── Test 4: Downloads wraps BasicPlan (uncomment after TODO 6) ────────────────────
         Subscription dl = new DownloadsDecorator(new BasicPlan());
         System.out.println("Test 4a — label: "
             + ("Basic Plan + Offline Downloads".equals(dl.getLabel()) ? "PASSED" : "FAILED (got: " + dl.getLabel() + ")"));
         System.out.println("Test 4b — cost:  "
             + (String.format("%.2f", dl.getMonthlyCost()).equals("11.99") ? "PASSED" : "FAILED (got: " + dl.getMonthlyCost() + ")"));

        // ── Test 5: Two decorators stacked — HD then Family Sharing (uncomment after TODOs 4,5) ─
         Subscription hdFamily = new FamilySharingDecorator(new HDUpgradeDecorator(new BasicPlan()));
         System.out.println("Test 5a — label: "
             + ("Basic Plan + HD + Family Sharing".equals(hdFamily.getLabel()) ? "PASSED" : "FAILED (got: " + hdFamily.getLabel() + ")"));
         System.out.println("Test 5b — cost:  "
             + (String.format("%.2f", hdFamily.getMonthlyCost()).equals("17.99") ? "PASSED" : "FAILED (got: " + hdFamily.getMonthlyCost() + ")"));

        // ── Test 6: All three decorators stacked (uncomment after TODOs 4,5,6) ────────────
         Subscription premium = new DownloadsDecorator(
             new FamilySharingDecorator(new HDUpgradeDecorator(new BasicPlan())));
         System.out.println("Test 6a — label: "
             + ("Basic Plan + HD + Family Sharing + Offline Downloads".equals(premium.getLabel())
                ? "PASSED" : "FAILED (got: " + premium.getLabel() + ")"));
         System.out.println("Test 6b — cost:  "
             + (String.format("%.2f", premium.getMonthlyCost()).equals("19.99") ? "PASSED" : "FAILED (got: " + premium.getMonthlyCost() + ")"));

        // ── Test 7: Order matters — same add-ons, different wrapping order → different label
        //           (uncomment after TODOs 4,5)  ──────────────────────────────────────────
         Subscription familyThenHD = new HDUpgradeDecorator(new FamilySharingDecorator(new BasicPlan()));
         System.out.println("Test 7 — order matters (same cost, different label): "
             + (familyThenHD.getMonthlyCost() == hdFamily.getMonthlyCost()
                && !familyThenHD.getLabel().equals(hdFamily.getLabel()) ? "PASSED" : "FAILED"));
         System.out.println("  HD-first label:     " + hdFamily.getLabel());      // expected: Basic Plan + HD + Family Sharing
         System.out.println("  Family-first label: " + familyThenHD.getLabel()); // expected: Basic Plan + Family Sharing + HD

        // ── Test 8: Polymorphic catalog — all accessed via Subscription interface
        //           (uncomment after all TODOs) ──────────────────────────────────────────
         Subscription[] catalog = {
             new BasicPlan(),
             new HDUpgradeDecorator(new BasicPlan()),
             new FamilySharingDecorator(new HDUpgradeDecorator(new BasicPlan())),
             new DownloadsDecorator(new FamilySharingDecorator(new HDUpgradeDecorator(new BasicPlan())))
         };
         System.out.println("Test 8 — polymorphic catalog:");
         for (Subscription s : catalog) {
             System.out.printf("  $%.2f/mo  —  %s%n", s.getMonthlyCost(), s.getLabel());
         }
         System.out.println("Test 8 — PASSED if all 4 plans printed correctly above");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HINTS — read only if stuck
    // ─────────────────────────────────────────────────────────────────────────

    /*
     * HINT 1 (Gentle)
     * ───────────────
     * A BasicPlan, an HD-upgraded plan, and an HD+Family plan are all "subscriptions"
     * — they all have a label and a cost. Instead of writing a new class for every
     * combination, think about what would happen if you wrapped one subscription
     * inside another that added its own piece to the label and cost, while
     * still looking like a subscription to the outside world.
     */

    /*
     * HINT 2 (Direct)
     * ───────────────
     * This is the Decorator pattern. The four participants are:
     *
     *   Subscription            — Component interface (TODO 1)
     *   BasicPlan               — ConcreteComponent (TODO 2)
     *   SubscriptionDecorator   — abstract BaseDecorator: implements Subscription
     *                             AND holds a Subscription (TODO 3)
     *   HD/Family/Downloads     — ConcreteDecorators: each extends BaseDecorator,
     *                             overrides getLabel() and getMonthlyCost() by
     *                             calling super.X() and adding its own piece (TODOs 4–6)
     *
     * Critical: SubscriptionDecorator must both implement Subscription (so it can
     * be passed anywhere a Subscription is expected) AND hold a Subscription field
     * (so it can delegate to whatever it wraps).
     */

    /*
     * HINT 3 (Near-solution — class skeleton only, no method bodies)
     * ───────────────────────────────────────────────────────────────
     *
     * interface Subscription {
     *     String getLabel();
     *     double getMonthlyCost();
     * }
     *
     * static class BasicPlan implements Subscription {
     *     @Override public String getLabel()       { ... }  // "Basic Plan"
     *     @Override public double getMonthlyCost() { ... }  // 9.99
     * }
     *
     * static abstract class SubscriptionDecorator implements Subscription {
     *     private final Subscription wrapped;
     *     SubscriptionDecorator(Subscription wrapped) { ... }
     *     @Override public String getLabel()       { ... }  // delegate to wrapped
     *     @Override public double getMonthlyCost() { ... }  // delegate to wrapped
     * }
     *
     * static class HDUpgradeDecorator extends SubscriptionDecorator {
     *     HDUpgradeDecorator(Subscription wrapped) { super(wrapped); }
     *     @Override public String getLabel()       { ... }  // super.getLabel() + " + HD"
     *     @Override public double getMonthlyCost() { ... }  // super.getMonthlyCost() + 3.00
     * }
     *
     * // FamilySharingDecorator and DownloadsDecorator follow the same shape.
     */
}
