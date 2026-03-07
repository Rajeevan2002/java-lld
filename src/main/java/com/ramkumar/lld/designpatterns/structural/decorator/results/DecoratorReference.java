package com.ramkumar.lld.designpatterns.structural.decorator.results;

// ─────────────────────────────────────────────────────────────────────────────
// Reference Solution — Decorator Pattern (Scenario B: Subscription Pricing)
// ─────────────────────────────────────────────────────────────────────────────

public class DecoratorReference {

    // ── [Component] — interface all subscriptions (base and decorated) must implement ──
    interface Subscription {
        String getLabel();
        double getMonthlyCost();
    }

    // ── [ConcreteComponent] — the base plan; no decoration applied ───────────
    static class BasicPlan implements Subscription {
        @Override
        public String getLabel()       { return "Basic Plan"; }

        @Override
        public double getMonthlyCost() { return 9.99; }
    }

    // ── [BaseDecorator] — the structural centre of the pattern ───────────────
    // Two relationships are essential and must coexist:
    //   implements Subscription  → is-a Subscription; can be stacked/passed as one
    //   has-a    Subscription    → delegates to the next in the chain
    //
    // Constructor is protected: only subclass constructors call super(wrapped).
    // Public would expose it unnecessarily — abstract classes can't be instantiated
    // directly, but access modifiers still communicate intent.
    static abstract class SubscriptionDecorator implements Subscription {

        private final Subscription wrapped;    // [private] — subclasses use super.method(), never wrapped directly
                                               // [final]   — reference never replaced after construction

        protected SubscriptionDecorator(Subscription wrapped) {
            this.wrapped = wrapped;
        }

        // [Delegation] — pure pass-through in the base; concrete decorators override to add their piece.
        // Calling wrapped.getLabel() here (not super.getLabel()) because there is no
        // higher-level getLabel() to call — this IS the base of the delegation chain.
        @Override
        public String getLabel()       { return wrapped.getLabel(); }

        @Override
        public double getMonthlyCost() { return wrapped.getMonthlyCost(); }
    }

    // ── [ConcreteDecorator 1] — adds HD video ────────────────────────────────
    static class HDUpgradeDecorator extends SubscriptionDecorator {

        HDUpgradeDecorator(Subscription wrapped) {
            super(wrapped);   // [MustChain] — stores wrapped in the private final field
        }

        @Override
        public String getLabel() {
            // [Delegation + Addition] Call super, then append own piece.
            // NEVER return "Basic Plan + HD" — hard-coding breaks 2+ deep stacking.
            return super.getLabel() + " + HD";
        }

        @Override
        public double getMonthlyCost() {
            return super.getMonthlyCost() + 3.00;
        }
    }

    // ── [ConcreteDecorator 2] — adds family sharing ──────────────────────────
    static class FamilySharingDecorator extends SubscriptionDecorator {

        FamilySharingDecorator(Subscription wrapped) { super(wrapped); }

        @Override
        public String getLabel()       { return super.getLabel() + " + Family Sharing"; }

        @Override
        public double getMonthlyCost() { return super.getMonthlyCost() + 5.00; }
    }

    // ── [ConcreteDecorator 3] — adds offline downloads ───────────────────────
    static class DownloadsDecorator extends SubscriptionDecorator {

        DownloadsDecorator(Subscription wrapped) { super(wrapped); }

        @Override
        public String getLabel()       { return super.getLabel() + " + Offline Downloads"; }

        @Override
        public double getMonthlyCost() { return super.getMonthlyCost() + 2.00; }
    }

    // ── main() ────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        int passed = 0, total = 16;  // 2 per test 1-6, 1 for test 7, 1 for test 8, 2 for test 9

        // Test 1: BasicPlan alone
        Subscription basic = new BasicPlan();
        boolean t1a = "Basic Plan".equals(basic.getLabel());
        boolean t1b = fmt(basic.getMonthlyCost()).equals("9.99");
        System.out.println("Test 1a — BasicPlan label:    " + (t1a ? "PASSED" : "FAILED (got: " + basic.getLabel() + ")"));
        System.out.println("Test 1b — BasicPlan cost:     " + (t1b ? "PASSED" : "FAILED (got: " + basic.getMonthlyCost() + ")"));
        if (t1a) passed++; if (t1b) passed++;

        // Test 2: HD upgrade
        Subscription hd = new HDUpgradeDecorator(new BasicPlan());
        boolean t2a = "Basic Plan + HD".equals(hd.getLabel());
        boolean t2b = fmt(hd.getMonthlyCost()).equals("12.99");
        System.out.println("Test 2a — HD label:           " + (t2a ? "PASSED" : "FAILED (got: " + hd.getLabel() + ")"));
        System.out.println("Test 2b — HD cost:            " + (t2b ? "PASSED" : "FAILED (got: " + hd.getMonthlyCost() + ")"));
        if (t2a) passed++; if (t2b) passed++;

        // Test 3: Family Sharing
        Subscription family = new FamilySharingDecorator(new BasicPlan());
        boolean t3a = "Basic Plan + Family Sharing".equals(family.getLabel());
        boolean t3b = fmt(family.getMonthlyCost()).equals("14.99");
        System.out.println("Test 3a — Family label:       " + (t3a ? "PASSED" : "FAILED (got: " + family.getLabel() + ")"));
        System.out.println("Test 3b — Family cost:        " + (t3b ? "PASSED" : "FAILED (got: " + family.getMonthlyCost() + ")"));
        if (t3a) passed++; if (t3b) passed++;

        // Test 4: Downloads
        Subscription dl = new DownloadsDecorator(new BasicPlan());
        boolean t4a = "Basic Plan + Offline Downloads".equals(dl.getLabel());
        boolean t4b = fmt(dl.getMonthlyCost()).equals("11.99");
        System.out.println("Test 4a — Downloads label:    " + (t4a ? "PASSED" : "FAILED (got: " + dl.getLabel() + ")"));
        System.out.println("Test 4b — Downloads cost:     " + (t4b ? "PASSED" : "FAILED (got: " + dl.getMonthlyCost() + ")"));
        if (t4a) passed++; if (t4b) passed++;

        // Test 5: Two decorators stacked — HD then Family Sharing
        Subscription hdFamily = new FamilySharingDecorator(new HDUpgradeDecorator(new BasicPlan()));
        boolean t5a = "Basic Plan + HD + Family Sharing".equals(hdFamily.getLabel());
        boolean t5b = fmt(hdFamily.getMonthlyCost()).equals("17.99");
        System.out.println("Test 5a — HD+Family label:    " + (t5a ? "PASSED" : "FAILED (got: " + hdFamily.getLabel() + ")"));
        System.out.println("Test 5b — HD+Family cost:     " + (t5b ? "PASSED" : "FAILED (got: " + hdFamily.getMonthlyCost() + ")"));
        if (t5a) passed++; if (t5b) passed++;

        // Test 6: All three stacked
        Subscription premium = new DownloadsDecorator(
                new FamilySharingDecorator(new HDUpgradeDecorator(new BasicPlan())));
        boolean t6a = "Basic Plan + HD + Family Sharing + Offline Downloads".equals(premium.getLabel());
        boolean t6b = fmt(premium.getMonthlyCost()).equals("19.99");
        System.out.println("Test 6a — Premium label:      " + (t6a ? "PASSED" : "FAILED (got: " + premium.getLabel() + ")"));
        System.out.println("Test 6b — Premium cost:       " + (t6b ? "PASSED" : "FAILED (got: " + premium.getMonthlyCost() + ")"));
        if (t6a) passed++; if (t6b) passed++;

        // Test 7: Order matters — same add-ons, different wrapping order → different label
        Subscription familyThenHD = new HDUpgradeDecorator(new FamilySharingDecorator(new BasicPlan()));
        boolean t7 = hdFamily.getMonthlyCost() == familyThenHD.getMonthlyCost()
                  && !hdFamily.getLabel().equals(familyThenHD.getLabel());
        System.out.println("Test 7  — order matters:      " + (t7 ? "PASSED" : "FAILED"));
        System.out.println("  HD-first:     " + hdFamily.getLabel());
        System.out.println("  Family-first: " + familyThenHD.getLabel());
        if (t7) passed++;

        // Test 8: Polymorphic catalog
        Subscription[] catalog = {
            new BasicPlan(),
            new HDUpgradeDecorator(new BasicPlan()),
            new FamilySharingDecorator(new HDUpgradeDecorator(new BasicPlan())),
            new DownloadsDecorator(new FamilySharingDecorator(new HDUpgradeDecorator(new BasicPlan())))
        };
        System.out.println("Test 8  — polymorphic catalog:");
        for (Subscription s : catalog) {
            System.out.printf("  $%.2f/mo  —  %s%n", s.getMonthlyCost(), s.getLabel());
        }
        System.out.println("Test 8  — PASSED if all 4 plans printed correctly above");
        passed++;   // manual verification — count it once

        // Test 9 — catches the most common mistake: hard-coding the full label string.
        // If HDUpgradeDecorator returns "Basic Plan + HD" (hard-coded), then
        // wrapping it in itself would still return "Basic Plan + HD" — wrong.
        // The correct delegation must produce "Basic Plan + HD + HD".
        System.out.println("\nTest 9  — same decorator applied twice (catches hard-coded labels)");
        Subscription doubleHD = new HDUpgradeDecorator(new HDUpgradeDecorator(new BasicPlan()));
        boolean t9a = "Basic Plan + HD + HD".equals(doubleHD.getLabel());
        boolean t9b = fmt(doubleHD.getMonthlyCost()).equals("15.99");
        System.out.println("Test 9a — double-HD label:    "
                + (t9a ? "PASSED" : "FAILED — hard-coded label detected (got: " + doubleHD.getLabel() + ")"));
        System.out.println("Test 9b — double-HD cost:     "
                + (t9b ? "PASSED" : "FAILED (got: " + doubleHD.getMonthlyCost() + ")"));
        if (t9a) passed++; if (t9b) passed++;

        System.out.printf("%n══════════════════════════════%n");
        System.out.printf("Results: %d / %d PASSED%n", passed, total);
        System.out.printf("══════════════════════════════%n");
    }

    private static String fmt(double cost) { return String.format("%.2f", cost); }
}
