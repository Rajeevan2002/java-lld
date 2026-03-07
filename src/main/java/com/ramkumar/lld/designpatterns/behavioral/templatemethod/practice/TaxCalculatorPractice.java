package com.ramkumar.lld.designpatterns.behavioral.templatemethod.practice;

/**
 * Practice Exercise — Template Method Pattern: Tax Calculation Pipeline
 *
 * <p><b>Scenario B — Fixed algorithm skeleton with varying steps</b>
 *
 * <p>A tax platform computes the tax due for different filing types. The overall
 * algorithm is always the same four-step sequence, but each filing type varies in
 * how taxable income is computed, what rate is applied, and whether credits apply.
 *
 * <p><b>Class hierarchy (do not change the names):</b>
 * <pre>
 *   TaxCalculator             [AbstractClass]     ← TODO 1
 *   IndividualTaxCalculator   [ConcreteClass 1]   ← TODO 2
 *   CorporateTaxCalculator    [ConcreteClass 2]   ← TODO 3
 * </pre>
 *
 * <p><b>TaxCalculator (abstract class)</b> (TODO 1):
 * <ul>
 *   <li>No fields.</li>
 *   <li><b>Template method</b> {@code final double calculate(double grossIncome)}:
 *     <ol>
 *       <li>{@code double taxable  = computeTaxableIncome(grossIncome)}</li>
 *       <li>{@code double tax      = applyTaxRate(taxable)}</li>
 *       <li>{@code double finalTax = applyCredits(tax)}</li>
 *       <li>{@code printBreakdown(grossIncome, taxable, tax, finalTax)}</li>
 *       <li>return {@code finalTax}</li>
 *     </ol>
 *     Must be declared {@code final} — subclasses must NOT override it.
 *   </li>
 *   <li>{@code abstract double computeTaxableIncome(double grossIncome)}.</li>
 *   <li>{@code abstract double applyTaxRate(double taxableIncome)}.</li>
 *   <li>{@code double applyCredits(double tax)} — <b>hook</b>; default returns
 *       {@code tax} unchanged (no credits). Subclasses may override.</li>
 *   <li>{@code void printBreakdown(double gross, double taxable, double tax, double finalTax)}
 *       — <b>concrete</b>; prints:
 *       {@code System.out.printf("[Tax] Gross: $%.2f | Taxable: $%.2f | Pre-credit: $%.2f | Due: $%.2f%n",
 *       gross, taxable, tax, finalTax)}</li>
 * </ul>
 *
 * <p><b>IndividualTaxCalculator (ConcreteClass 1)</b> (TODO 2):
 * <ul>
 *   <li>No fields.</li>
 *   <li>No-arg constructor.</li>
 *   <li>{@code computeTaxableIncome}: {@code grossIncome - 12_000.0} (standard deduction).</li>
 *   <li>{@code applyTaxRate}: {@code taxableIncome * 0.22} (22% flat rate).</li>
 *   <li>Does NOT override {@code applyCredits} — uses the hook default (no credits).</li>
 * </ul>
 *
 * <p><b>CorporateTaxCalculator (ConcreteClass 2)</b> (TODO 3):
 * <ul>
 *   <li>No fields.</li>
 *   <li>No-arg constructor.</li>
 *   <li>{@code computeTaxableIncome}: {@code grossIncome * 0.80}
 *       (20% business expense deduction).</li>
 *   <li>{@code applyTaxRate}: {@code taxableIncome * 0.21} (21% flat rate).</li>
 *   <li>Overrides {@code applyCredits}: returns {@code tax - tax * 0.05}
 *       (5% R&amp;D credit reduces the pre-credit tax by 5%).</li>
 * </ul>
 *
 * <p><b>Design constraints:</b>
 * <ul>
 *   <li>{@code calculate()} must be {@code final} — subclasses cannot reorder or skip steps.</li>
 *   <li>{@code printBreakdown()} must be concrete in the base class and not overridden.</li>
 *   <li>{@code applyCredits()} must have a working default so {@code IndividualTaxCalculator}
 *       is not required to implement it.</li>
 *   <li>No {@code instanceof} or type-checking anywhere.</li>
 * </ul>
 */
public class TaxCalculatorPractice {

    // ── AbstractClass ──────────────────────────────────────────────────────────

    static abstract class TaxCalculator {
        final double calculate(double grossIncome){
            double taxable = computeTaxableIncome(grossIncome);
            double tax = applyTaxRate(taxable);
            double finalTax =  applyCredits(tax);
            printBreakdown(grossIncome, taxable, tax, finalTax);
            return finalTax;
        }


        abstract double computeTaxableIncome(double grossIncome);
        abstract double applyTaxRate(double taxableIncome);
        double applyCredits(double tax) {
            return tax;
        }

        void printBreakdown(double gross, double taxable, double tax, double finalTax){
            System.out.printf("[Tax] Gross: $%.2f | Taxable: $%.2f | Pre-credit: $%.2f | Due: $%.2f%n"
                    , gross, taxable, tax, finalTax);
        }
    }

    // ── ConcreteClass 1 ────────────────────────────────────────────────────────

    static class IndividualTaxCalculator extends TaxCalculator {
        @Override
        double computeTaxableIncome(double grossIncome){
            return grossIncome - 12_000.0;
        }

        @Override
        double applyTaxRate(double taxableIncome){
            return taxableIncome * 0.22;
        }
    }

    // ── ConcreteClass 2 ────────────────────────────────────────────────────────

    static class CorporateTaxCalculator extends TaxCalculator {
        @Override
        double computeTaxableIncome(double grossIncome){
            return grossIncome * 0.80;
        }

        @Override
        double applyTaxRate(double taxableIncome){
            return taxableIncome * 0.21;
        }

        @Override
        double applyCredits(double tax) {
            return tax - tax * 0.05;
        }
    }

    // ── DO NOT MODIFY — test harness ──────────────────────────────────────────
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: Individual — taxable income = gross - 12000 (uncomment after TODO 2) ──
         IndividualTaxCalculator ind = new IndividualTaxCalculator();
         double t1 = ind.calculate(100_000.0);
         // [Tax] Gross: $100000.00 | Taxable: $88000.00 | Pre-credit: $19360.00 | Due: $19360.00
         System.out.println("Test 1 — Individual $100k: "
             + (Math.abs(t1 - 19_360.0) < 0.001 ? "PASSED" : "FAILED (got: " + t1 + ")"));

        // ── Test 2: Individual — different income (uncomment after TODO 2) ──────────────
         double t2 = ind.calculate(150_000.0);
         // taxable = 138000, tax = 30360, final = 30360
         System.out.println("Test 2 — Individual $150k: "
             + (Math.abs(t2 - 30_360.0) < 0.001 ? "PASSED" : "FAILED (got: " + t2 + ")"));

        // ── Test 3: Corporate — taxable = gross * 0.80, rate 21% (uncomment after TODO 3) ─
         CorporateTaxCalculator corp = new CorporateTaxCalculator();
         double t3 = corp.calculate(500_000.0);
         // taxable = 400000, pre-credit = 84000, final = 79800 (5% credit = 4200)
         System.out.println("Test 3 — Corporate $500k: "
             + (Math.abs(t3 - 79_800.0) < 0.001 ? "PASSED" : "FAILED (got: " + t3 + ")"));

        // ── Test 4: Corporate — different income (uncomment after TODO 3) ────────────────
         double t4 = corp.calculate(1_000_000.0);
         // taxable = 800000, pre-credit = 168000, final = 159600
         System.out.println("Test 4 — Corporate $1M: "
             + (Math.abs(t4 - 159_600.0) < 0.001 ? "PASSED" : "FAILED (got: " + t4 + ")"));

        // ── Test 5: Hook default — Individual has NO credits (uncomment after TODOs 1–2) ─
        // // applyCredits default returns tax unchanged — pre-credit == final for Individual
         double t5 = ind.calculate(112_000.0);   // taxable=100000, pre-credit=22000, final=22000
         System.out.println("Test 5 — Individual no credits (pre-credit == final): "
             + (Math.abs(t5 - 22_000.0) < 0.001 ? "PASSED" : "FAILED (got: " + t5 + ")"));

        // ── Test 6: Hook override — Corporate credits reduce final tax (uncomment after TODOs 1–3) ─
        // // Corporate: pre-credit != final (credits applied)
         double t6pre = 500_000.0 * 0.80 * 0.21;   // = 84000
         double t6    = corp.calculate(500_000.0);  // = 79800 (84000 * 0.95)
         System.out.println("Test 6 — Corporate credits applied (final < pre-credit): "
             + (t6 < t6pre ? "PASSED" : "FAILED (final=" + t6 + " pre=" + t6pre + ")"));

        // ── Test 7: Polymorphic call via base class reference (uncomment after all TODOs) ─
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

        // ── Test 8: calculate() is final — cannot be overridden (compile-time check) ──────
        // // The fact that this anonymous class only overrides the two abstract methods
        // // (and NOT calculate) proves calculate() is final.
         TaxCalculator custom = new TaxCalculator() {
             @Override protected double computeTaxableIncome(double g) { return g * 0.90; }
             @Override protected double applyTaxRate(double t)          { return t * 0.15; }
             // cannot override calculate() — it is final
         };
         double t8 = custom.calculate(200_000.0);   // taxable=180000, tax=27000, final=27000
         System.out.println("Test 8 — anonymous subclass respects skeleton: "
             + (Math.abs(t8 - 27_000.0) < 0.001 ? "PASSED" : "FAILED (got: " + t8 + ")"));
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HINTS — read only if stuck
    // ════════════════════════════════════════════════════════════════════════════

    // HINT 1 (Gentle):
    //   Both IndividualTaxCalculator and CorporateTaxCalculator follow the same four
    //   steps in the same order, but the numbers come out differently. Instead of
    //   duplicating the four-step sequence in each class, define it once in a base
    //   class method that calls smaller, overridable methods. The base class should
    //   prevent subclasses from changing the order of the four steps.

    // HINT 2 (Direct):
    //   Use the Template Method pattern.
    //   TaxCalculator is an abstract class. Its template method calculate() is final
    //   and calls four methods in a fixed sequence: computeTaxableIncome (abstract),
    //   applyTaxRate (abstract), applyCredits (hook with default return tax),
    //   printBreakdown (concrete). Subclasses override only the methods they need to.

    // HINT 3 (Near-solution skeleton — class outlines only, no method bodies):
    //
    //   static abstract class TaxCalculator {
    //       public final double calculate(double grossIncome) { ... }
    //       protected abstract double computeTaxableIncome(double grossIncome);
    //       protected abstract double applyTaxRate(double taxableIncome);
    //       protected double applyCredits(double tax) { ... }   // hook: default returns tax
    //       protected void printBreakdown(double gross, double taxable,
    //                                     double tax, double finalTax) { ... }
    //   }
    //
    //   static class IndividualTaxCalculator extends TaxCalculator {
    //       @Override protected double computeTaxableIncome(double g) { ... }
    //       @Override protected double applyTaxRate(double t)          { ... }
    //       // applyCredits NOT overridden — uses base default
    //   }
    //
    //   static class CorporateTaxCalculator extends TaxCalculator {
    //       @Override protected double computeTaxableIncome(double g) { ... }
    //       @Override protected double applyTaxRate(double t)          { ... }
    //       @Override protected double applyCredits(double tax)        { ... }
    //   }
}
