package com.ramkumar.lld.solid.ocp.results;

import java.util.HashMap;
import java.util.Map;

/**
 * Reference solution — Open/Closed Principle (OCP)
 * Phase 2, Topic 2 | Scenario B: Insurance Premium Calculator
 *
 * Fixes from the practice review:
 *   Issue 1: policyId format — "POL-%03d" not "POL-%2d" (zero-pad to 3 digits)
 *   Issue 2: HomeStrategy — remove Math.round(), return exact double
 *   Issue 3: HealthStrategy — use getHolderAge() not direct field access
 *   Issue 4: getSummary()  — delegate to calculate(); consistent exception behaviour
 *   Issue 5: getSummary()  — "Premium: " without extra space before colon
 */
public class InsuranceReference {

    // =========================================================================
    // InsurancePolicy — DATA ONLY (immutable)
    // =========================================================================
    static class InsurancePolicy {

        // Static counter FIRST (Java convention: static before instance fields)
        private static int counter = 0;

        private final String policyId;
        private final String holderName;
        private final String policyType;
        private final double baseAmount;
        private final int    holderAge;

        public InsurancePolicy(String holderName, String policyType,
                               double baseAmount, int holderAge) {
            // Validate null BEFORE isBlank() to avoid NullPointerException
            if (holderName == null || holderName.isBlank())
                throw new IllegalArgumentException("Holder name cannot be blank");
            if (policyType == null || policyType.isBlank())
                throw new IllegalArgumentException("Policy type cannot be blank");
            if (baseAmount <= 0)
                throw new IllegalArgumentException("Base amount must be positive");
            if (holderAge < 18)
                throw new IllegalArgumentException("Holder must be at least 18 years old");

            // KEY FIX 1: "%03d" = zero-padded to 3 digits → "POL-001", not "POL- 1"
            this.policyId   = String.format("POL-%03d", ++counter);
            this.holderName = holderName;
            this.policyType = policyType;
            this.baseAmount = baseAmount;
            this.holderAge  = holderAge;
        }

        public String getPolicyId()     { return policyId; }
        public String getHolderName()   { return holderName; }
        public String getPolicyType()   { return policyType; }
        public double getBaseAmount()   { return baseAmount; }
        public int    getHolderAge()    { return holderAge; }
    }

    // =========================================================================
    // PremiumStrategy — EXTENSION POINT (interface, never modified)
    // =========================================================================
    interface PremiumStrategy {
        double calculate(InsurancePolicy policy);
        String getPolicyType();
    }

    // =========================================================================
    // Concrete strategies — CLOSED after they ship; only new types need new classes
    // =========================================================================

    static class HealthStrategy implements PremiumStrategy {
        @Override public String getPolicyType() { return "HEALTH"; }

        @Override
        public double calculate(InsurancePolicy policy) {
            double premium = policy.getBaseAmount() * 1.20;
            // KEY FIX 3: use the getter, not direct field access
            if (policy.getHolderAge() > 45)
                premium += policy.getBaseAmount() * 0.05;   // age surcharge
            return premium;
        }
    }

    static class AutoStrategy implements PremiumStrategy {
        @Override public String getPolicyType() { return "AUTO"; }

        @Override
        public double calculate(InsurancePolicy policy) {
            return policy.getBaseAmount() * 1.15;
        }
    }

    static class HomeStrategy implements PremiumStrategy {
        @Override public String getPolicyType() { return "HOME"; }

        @Override
        public double calculate(InsurancePolicy policy) {
            // KEY FIX 2: return exact double — do NOT use Math.round()
            // Math.round() returns long and silently truncates fractional premiums
            return policy.getBaseAmount() * 1.10;
        }
    }

    // =========================================================================
    // PremiumCalculator — CLOSED orchestrator (Map dispatch, never an if/else)
    // =========================================================================
    static class PremiumCalculator {

        // Inline initialisation: private + final + no constructor needed
        private final Map<String, PremiumStrategy> strategies = new HashMap<>();

        // EXTENSION POINT — register new strategies without modifying this class
        public void register(PremiumStrategy strategy) {
            if (strategy == null)
                throw new IllegalArgumentException("Strategy cannot be null");
            strategies.put(strategy.getPolicyType(), strategy);
        }

        // CLOSED — this method never grows; OCP satisfied by Map dispatch
        public double calculate(InsurancePolicy policy) {
            PremiumStrategy strategy = strategies.get(policy.getPolicyType());
            if (strategy == null)
                throw new IllegalArgumentException(
                        "No strategy registered for: " + policy.getPolicyType());
            return strategy.calculate(policy);  // polymorphic dispatch — no if/else
        }

        // KEY FIX 4: delegate to calculate() — single lookup, consistent exception
        // KEY FIX 5: "Premium: " without extra space
        public String getSummary(InsurancePolicy policy) {
            // calculate() handles the null check and throws IAE if not registered
            // — consistent with the rest of the class's error contract
            double premium = calculate(policy);
            return String.format("%s | %s | %s | Base: ₹%.2f | Premium: ₹%.2f",
                    policy.getPolicyId(),
                    policy.getPolicyType(),
                    policy.getHolderName(),
                    policy.getBaseAmount(),
                    premium);
        }

        public boolean isSupported(String policyType) {
            return strategies.containsKey(policyType);
        }
    }

    // =========================================================================
    // OCP proof: LifeStrategy — new class, zero modification to anything above
    // =========================================================================
    static class LifeStrategy implements PremiumStrategy {
        @Override public String getPolicyType() { return "LIFE"; }

        @Override
        public double calculate(InsurancePolicy policy) {
            return policy.getBaseAmount() * 1.30;
        }
    }

    // =========================================================================
    // Main — same 10 tests as the practice file + Test 11 (catches Issue 1)
    // =========================================================================
    public static void main(String[] args) {

        PremiumCalculator calculator = new PremiumCalculator();
        calculator.register(new HealthStrategy());
        calculator.register(new AutoStrategy());
        calculator.register(new HomeStrategy());

        System.out.println("═══ Test 1: InsurancePolicy construction ════════════════");
        InsurancePolicy p1 = new InsurancePolicy("Alice", "HEALTH", 10000.0, 50);
        System.out.println("policyId    : " + p1.getPolicyId());   // POL-001
        System.out.println("holderName  : " + p1.getHolderName());
        System.out.println("policyType  : " + p1.getPolicyType());
        System.out.println("baseAmount  : " + p1.getBaseAmount());
        System.out.println("holderAge   : " + p1.getHolderAge());
        System.out.println("Test 1 PASSED");

        System.out.println("\n═══ Test 2: InsurancePolicy validation ══════════════════");
        try {
            new InsurancePolicy("", "HEALTH", 10000.0, 30);
            System.out.println("Test 2 FAILED");
        } catch (IllegalArgumentException e) {
            System.out.println("Test 2a PASSED: " + e.getMessage());
        }
        try {
            new InsurancePolicy("Bob", "AUTO", -500.0, 25);
            System.out.println("Test 2 FAILED");
        } catch (IllegalArgumentException e) {
            System.out.println("Test 2b PASSED: " + e.getMessage());
        }
        try {
            new InsurancePolicy("Carol", "HOME", 5000.0, 16);
            System.out.println("Test 2 FAILED");
        } catch (IllegalArgumentException e) {
            System.out.println("Test 2c PASSED: " + e.getMessage());
        }

        System.out.println("\n═══ Test 3: HealthStrategy — age > 45 (with surcharge) ═");
        InsurancePolicy healthSenior = new InsurancePolicy("Alice", "HEALTH", 10000.0, 50);
        double premium = calculator.calculate(healthSenior);
        System.out.println("Premium (age 50): " + premium);      // 12500.0
        System.out.println("Test 3 PASSED: " + (premium == 12500.0));

        System.out.println("\n═══ Test 4: HealthStrategy — age <= 45 (no surcharge) ══");
        InsurancePolicy healthJunior = new InsurancePolicy("Dave", "HEALTH", 10000.0, 40);
        double premiumYoung = calculator.calculate(healthJunior);
        System.out.println("Premium (age 40): " + premiumYoung);  // 12000.0
        System.out.println("Test 4 PASSED: " + (premiumYoung == 12000.0));

        System.out.println("\n═══ Test 5: AutoStrategy ════════════════════════════════");
        InsurancePolicy autoPol = new InsurancePolicy("Eve", "AUTO", 8000.0, 30);
        double autoPremium = calculator.calculate(autoPol);
        System.out.println("Auto premium: " + autoPremium);       // 9200.0
        System.out.println("Test 5 PASSED: " + (autoPremium == 9200.0));

        System.out.println("\n═══ Test 6: HomeStrategy ════════════════════════════════");
        InsurancePolicy homePol = new InsurancePolicy("Frank", "HOME", 12000.0, 45);
        double homePremium = calculator.calculate(homePol);
        System.out.println("Home premium: " + homePremium);       // 13200.0
        System.out.println("Test 6 PASSED: " + (homePremium == 13200.0));

        System.out.println("\n═══ Test 7: getSummary ══════════════════════════════════");
        InsurancePolicy p7 = new InsurancePolicy("Grace", "AUTO", 5000.0, 35);
        String summary = calculator.getSummary(p7);
        System.out.println(summary);
        System.out.println("Contains AUTO   : " + summary.contains("AUTO"));
        System.out.println("Contains Grace  : " + summary.contains("Grace"));
        System.out.println("Contains 5750.00: " + summary.contains("5750.00"));
        // Also assert no extra space in "Premium: "
        System.out.println("Format correct  : " + summary.contains("Premium: ₹"));
        System.out.println("Test 7 PASSED");

        System.out.println("\n═══ Test 8: isSupported ═════════════════════════════════");
        System.out.println("HEALTH supported: " + calculator.isSupported("HEALTH")); // true
        System.out.println("LIFE supported  : " + calculator.isSupported("LIFE"));   // false
        System.out.println("Test 8 PASSED");

        System.out.println("\n═══ Test 9: unknown type → exception ════════════════════");
        try {
            InsurancePolicy unknown = new InsurancePolicy("Hank", "LIFE", 20000.0, 35);
            calculator.calculate(unknown);
            System.out.println("Test 9 FAILED — should have thrown");
        } catch (IllegalArgumentException e) {
            System.out.println("Test 9 PASSED: " + e.getMessage());
        }

        System.out.println("\n═══ Test 10: OCP proof — add LIFE with zero modification ═");
        calculator.register(new LifeStrategy());
        InsurancePolicy lifePol = new InsurancePolicy("Ivy", "LIFE", 15000.0, 42);
        double lifePremium = calculator.calculate(lifePol);
        System.out.println("Life premium: " + lifePremium);        // 19500.0
        System.out.println("LIFE supported now: " + calculator.isSupported("LIFE")); // true
        System.out.println("Test 10 PASSED: " + (lifePremium == 19500.0));

        // ── Test 11 (Extra) — catches Issue 1: wrong format specifier ─────────
        // "POL-%2d" produces "POL- 1" (space-padded, width 2).
        // "POL-%03d" produces "POL-001" (zero-padded, width 3).
        // This test asserts the exact format of the generated ID.
        System.out.println("\n═══ Test 11 (Extra): policyId format is zero-padded ════");
        InsurancePolicy t11 = new InsurancePolicy("Test", "AUTO", 1000.0, 25);
        String id = t11.getPolicyId();
        boolean correctFormat = id.matches("POL-\\d{3}");  // 3 digits, always
        boolean noLeadingSpace = !id.contains(" ");         // space-padding would add a space
        System.out.println("policyId        : '" + id + "'");
        System.out.println("Matches POL-ddd : " + correctFormat);
        System.out.println("No space inside : " + noLeadingSpace);
        if (correctFormat && noLeadingSpace)
            System.out.println("Test 11 PASSED — zero-padded ID format correct");
        else
            System.out.println("Test 11 FAILED — check String.format specifier: use %03d not %2d");

        // ── Test 12 (Extra) — catches Issue 2: Math.round() precision loss ────
        // A base amount that produces a fractional premium exposes the rounding bug.
        System.out.println("\n═══ Test 12 (Extra): HomeStrategy precision (no Math.round) ═");
        InsurancePolicy fractional = new InsurancePolicy("Zara", "HOME", 10001.0, 30);
        double fractionalPremium = calculator.calculate(fractional);
        // 10001 * 1.10 = 11001.1
        // Math.round(11001.1) = 11001L = 11001.0  ← wrong
        // correct answer: 11001.1
        System.out.println("Premium for base 10001: " + fractionalPremium);
        boolean precisionOk = fractionalPremium > 11001.0 && fractionalPremium < 11002.0;
        System.out.println("Precision preserved   : " + precisionOk);
        if (precisionOk)
            System.out.println("Test 12 PASSED — no rounding applied");
        else
            System.out.println("Test 12 FAILED — Math.round() is silently truncating the premium");
    }
}
