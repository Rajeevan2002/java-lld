package com.ramkumar.lld.designpatterns.behavioral.templatemethod.results;

/**
 * Reference solution — Template Method Pattern: Tax Calculation Pipeline
 *
 * <p>Key decisions vs common mistakes:
 * <ul>
 *   <li>{@code calculate()} is {@code public final} — public so clients can call it;
 *       final so no subclass can reorder or skip the steps.</li>
 *   <li>Step methods are {@code protected} — visible to subclasses in any package;
 *       package-private methods silently break cross-package inheritance.</li>
 *   <li>{@code applyCredits()} is a hook with a working default — subclasses that
 *       need credits override it; others inherit the no-op default automatically.</li>
 *   <li>{@code printBreakdown()} is {@code protected} (not abstract, not private) —
 *       concrete shared step; it could also be package-private since it is not part
 *       of the override contract, but {@code protected} is consistent with the others.</li>
 * </ul>
 */
public class TemplateMethodReference {

    // ── [AbstractClass] — owns the algorithm skeleton ─────────────────────────
    static abstract class TaxCalculator {

        // [TemplateMethod] public = callable by clients. final = CANNOT be overridden.
        // These two modifiers together are the defining feature of the pattern.
        public final double calculate(double grossIncome) {
            double taxable  = computeTaxableIncome(grossIncome);  // [AbstractStep]
            double tax      = applyTaxRate(taxable);               // [AbstractStep]
            double finalTax = applyCredits(tax);                   // [Hook]
            printBreakdown(grossIncome, taxable, tax, finalTax);   // [ConcreteStep]
            return finalTax;
        }

        // [AbstractStep] No default makes sense — every filing type must define these.
        // protected = subclasses in any package can override; package-private would break
        // cross-package inheritance.
        protected abstract double computeTaxableIncome(double grossIncome);
        protected abstract double applyTaxRate(double taxableIncome);

        // [Hook] Concrete with a sensible default. Subclasses that need credits override;
        // others inherit this and never need to mention credits at all.
        protected double applyCredits(double tax) {
            return tax;   // default: no credits applied
        }

        // [ConcreteStep] Identical for every filing type — lives here, not in subclasses.
        // Not abstract (there is one correct implementation), not private (protected so
        // subclasses can see it, though overriding it would be unusual).
        protected void printBreakdown(double gross, double taxable,
                                      double tax, double finalTax) {
            System.out.printf(
                "[Tax] Gross: $%.2f | Taxable: $%.2f | Pre-credit: $%.2f | Due: $%.2f%n",
                gross, taxable, tax, finalTax);
        }
    }

    // ── [ConcreteClass 1] — overrides only the two required abstract steps ─────
    static class IndividualTaxCalculator extends TaxCalculator {

        @Override
        protected double computeTaxableIncome(double grossIncome) {
            return grossIncome - 12_000.0;   // standard deduction
        }

        @Override
        protected double applyTaxRate(double taxableIncome) {
            return taxableIncome * 0.22;
        }
        // [HookNotOverridden] applyCredits() uses base default — no credits for individuals.
        // Not declaring it here is intentional: it forces the reader to look at the base class.
    }

    // ── [ConcreteClass 2] — overrides both abstract steps AND the hook ─────────
    static class CorporateTaxCalculator extends TaxCalculator {

        @Override
        protected double computeTaxableIncome(double grossIncome) {
            return grossIncome * 0.80;   // 20% business expense deduction
        }

        @Override
        protected double applyTaxRate(double taxableIncome) {
            return taxableIncome * 0.21;
        }

        // [HookOverride] Corporations get a 5% R&D credit. The hook lets us add this
        // without touching the base class or IndividualTaxCalculator.
        @Override
        protected double applyCredits(double tax) {
            return tax - tax * 0.05;   // 5% credit reduces tax due by 5%
        }
    }

    // ── Reference main() ──────────────────────────────────────────────────────
    public static void main(String[] args) {

        // ── Test 1: Individual $100k ──────────────────────────────────────────
        IndividualTaxCalculator ind = new IndividualTaxCalculator();
        double t1 = ind.calculate(100_000.0);
        System.out.println("Test 1 — Individual $100k: "
            + (Math.abs(t1 - 19_360.0) < 0.001 ? "PASSED" : "FAILED (got: " + t1 + ")"));

        // ── Test 2: Individual $150k ──────────────────────────────────────────
        double t2 = ind.calculate(150_000.0);
        System.out.println("Test 2 — Individual $150k: "
            + (Math.abs(t2 - 30_360.0) < 0.001 ? "PASSED" : "FAILED (got: " + t2 + ")"));

        // ── Test 3: Corporate $500k ───────────────────────────────────────────
        CorporateTaxCalculator corp = new CorporateTaxCalculator();
        double t3 = corp.calculate(500_000.0);
        System.out.println("Test 3 — Corporate $500k: "
            + (Math.abs(t3 - 79_800.0) < 0.001 ? "PASSED" : "FAILED (got: " + t3 + ")"));

        // ── Test 4: Corporate $1M ─────────────────────────────────────────────
        double t4 = corp.calculate(1_000_000.0);
        System.out.println("Test 4 — Corporate $1M: "
            + (Math.abs(t4 - 159_600.0) < 0.001 ? "PASSED" : "FAILED (got: " + t4 + ")"));

        // ── Test 5: Hook default — Individual pre-credit == final ─────────────
        double t5 = ind.calculate(112_000.0);   // taxable=100000, tax=22000, final=22000
        System.out.println("Test 5 — Individual no credits: "
            + (Math.abs(t5 - 22_000.0) < 0.001 ? "PASSED" : "FAILED (got: " + t5 + ")"));

        // ── Test 6: Hook override — Corporate final < pre-credit ──────────────
        double preCreditCorp = 500_000.0 * 0.80 * 0.21;
        double t6 = corp.calculate(500_000.0);
        System.out.println("Test 6 — Corporate credits applied: "
            + (t6 < preCreditCorp ? "PASSED" : "FAILED (final=" + t6 + " pre=" + preCreditCorp + ")"));

        // ── Test 7: Polymorphic via base class reference ───────────────────────
        TaxCalculator[] filers = { new IndividualTaxCalculator(), new CorporateTaxCalculator() };
        double[] expected = { 19_360.0, 79_800.0 };
        double[] incomes  = { 100_000.0, 500_000.0 };
        boolean poly = true;
        for (int i = 0; i < filers.length; i++) {
            double result = filers[i].calculate(incomes[i]);
            if (Math.abs(result - expected[i]) >= 0.001) {
                poly = false;
                System.out.println("  FAILED filer " + i + " got " + result);
            }
        }
        System.out.println("Test 7 — polymorphic calculate: " + (poly ? "PASSED" : "FAILED"));

        // ── Test 8: Anonymous subclass — calculate() cannot be overridden ──────
        TaxCalculator custom = new TaxCalculator() {
            @Override protected double computeTaxableIncome(double g) { return g * 0.90; }
            @Override protected double applyTaxRate(double t)          { return t * 0.15; }
        };
        double t8 = custom.calculate(200_000.0);   // taxable=180000, tax=27000, final=27000
        System.out.println("Test 8 — anonymous subclass respects skeleton: "
            + (Math.abs(t8 - 27_000.0) < 0.001 ? "PASSED" : "FAILED (got: " + t8 + ")"));

        // ── Test 9 (OCP — adding a new filing type needs ZERO changes to the base) ──
        // SelfEmployedTaxCalculator: 25% expense deduction, 25% rate, 10% home-office credit.
        // gross=80_000 → taxable=60_000 → pre-credit=15_000 → final=13_500
        // This test catches the common mistake of adding new-type logic INTO TaxCalculator
        // instead of creating a new subclass — which would require editing the base class.
        System.out.println("\n── Test 9: OCP — new filing type via subclass, no base change ──");
        TaxCalculator selfEmployed = new TaxCalculator() {
            @Override
            protected double computeTaxableIncome(double g) {
                return g * 0.75;   // 25% business expense deduction
            }
            @Override
            protected double applyTaxRate(double t) {
                return t * 0.25;   // 25% self-employment rate
            }
            @Override
            protected double applyCredits(double tax) {
                return tax - tax * 0.10;   // 10% home-office credit
            }
        };
        double t9 = selfEmployed.calculate(80_000.0);
        // taxable = 80_000 * 0.75 = 60_000
        // tax     = 60_000 * 0.25 = 15_000
        // final   = 15_000 * 0.90 = 13_500
        System.out.println("Test 9 — Self-employed $80k: "
            + (Math.abs(t9 - 13_500.0) < 0.001 ? "PASSED" : "FAILED (got: " + t9 + ")"));
        System.out.println("(TaxCalculator base class was NOT modified to support this new type)");
    }
}
