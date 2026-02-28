package com.ramkumar.lld.solid.ocp.practice;

import java.util.HashMap;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * PRACTICE — Open/Closed Principle (OCP)
 * Phase 2, Topic 2 | Scenario B: Insurance Premium Calculator
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * PROBLEM STATEMENT
 * ─────────────────
 * An insurance company built their premium calculator as a single class that
 * switches on policy type. Every time a new policy type is introduced, an
 * engineer must open and modify the calculator — causing regressions in
 * existing policy calculations and merge conflicts across teams.
 *
 * Your job: refactor it so that adding a new policy type requires only
 * writing a new class — no modification to any existing class.
 *
 * ─── CURRENT VIOLATION (shown below as PremiumCalculatorGod) ─────────────
 * Problems:
 *   • if/else chain grows with every new policy type
 *   • adding LifeInsurance forces reopening a tested, deployed class
 *   • unit-testing health logic requires also loading auto and home logic
 *   • different actuarial teams cannot work independently
 *
 * ─── YOUR TASK ──────────────────────────────────────────────────────────────
 * Create these four SRP+OCP-compliant classes:
 *
 *  1. InsurancePolicy   — immutable data holder (policyId, holderName,
 *                          policyType, baseAmount, holderAge)
 *  2. PremiumStrategy   — interface with two methods (calculate + getPolicyType)
 *  3. Three concrete strategies:
 *       HealthStrategy  — implements PremiumStrategy for "HEALTH"
 *       AutoStrategy    — implements PremiumStrategy for "AUTO"
 *       HomeStrategy    — implements PremiumStrategy for "HOME"
 *  4. PremiumCalculator — orchestrator with a Map-based registry (no if/else)
 *
 * ─── FIELD REQUIREMENTS ─────────────────────────────────────────────────────
 * InsurancePolicy (all fields private and final — immutable after construction):
 *   • policyId    : String — auto-generated "POL-001" format (private static counter)
 *   • holderName  : String — cannot be blank
 *   • policyType  : String — cannot be blank (e.g., "HEALTH", "AUTO", "HOME")
 *   • baseAmount  : double — must be > 0
 *   • holderAge   : int    — must be >= 18
 *
 * PremiumStrategy (interface):
 *   • calculate(InsurancePolicy policy) : double
 *       Returns the total premium amount for the given policy.
 *   • getPolicyType() : String
 *       Returns the policy type this strategy handles (e.g., "HEALTH").
 *
 * HealthStrategy implements PremiumStrategy:
 *   • getPolicyType()        → "HEALTH"
 *   • calculate(policy)      → base * 1.20  (20% loading)
 *                               + (holderAge > 45 ? base * 0.05 : 0.0)  (age surcharge)
 *   example: base=10000, age=50 → 10000*1.20 + 10000*0.05 = 12500.0
 *   example: base=10000, age=40 → 10000*1.20               = 12000.0
 *
 * AutoStrategy implements PremiumStrategy:
 *   • getPolicyType()        → "AUTO"
 *   • calculate(policy)      → base * 1.15  (15% loading — flat, no age factor)
 *   example: base=8000 → 8000*1.15 = 9200.0
 *
 * HomeStrategy implements PremiumStrategy:
 *   • getPolicyType()        → "HOME"
 *   • calculate(policy)      → base * 1.10  (10% loading — flat)
 *   example: base=12000 → 12000*1.10 = 13200.0
 *
 * PremiumCalculator:
 *   • Internal registry    : Map<String, PremiumStrategy> (private final)
 *   • register(PremiumStrategy) : void
 *       Adds a strategy to the registry keyed by getPolicyType().
 *       Throws IllegalArgumentException if strategy is null.
 *   • calculate(InsurancePolicy policy) : double
 *       Looks up the strategy for policy.getPolicyType().
 *       Throws IllegalArgumentException if no strategy is registered for that type.
 *       Returns the result of strategy.calculate(policy).
 *   • getSummary(InsurancePolicy policy) : String
 *       Format: "POL-001 | HEALTH | Alice | Base: ₹10000.00 | Premium: ₹12500.00"
 *   • isSupported(String policyType) : boolean
 *       Returns true if a strategy is registered for that type.
 *
 * ─── DESIGN CONSTRAINTS ─────────────────────────────────────────────────────
 *  • PremiumCalculator must contain NO if/else or switch on policy type.
 *  • No instanceof checks anywhere.
 *  • InsurancePolicy constructor validates all fields; throws IllegalArgumentException.
 *  • The static counter in InsurancePolicy must be private.
 *  • PremiumCalculator.calculate() must delegate to a strategy — no inlined math.
 *  • OCP proof: adding LifeStrategy (base * 1.30) must require ZERO changes to
 *    InsurancePolicy, HealthStrategy, AutoStrategy, HomeStrategy, or PremiumCalculator.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 */
public class InsurancePractice {

    // =========================================================================
    // GOD CLASS — read to understand what you are refactoring AWAY from.
    // DO NOT ADD CODE HERE. DO NOT CALL THIS FROM MAIN.
    // =========================================================================

    @SuppressWarnings("unused")
    static class PremiumCalculatorGod {

        // ← Every new type forces you to open this method and add an else-if
        public double calculate(String policyType, double baseAmount, int holderAge) {
            if (policyType.equals("HEALTH")) {
                double premium = baseAmount * 1.20;
                if (holderAge > 45) premium += baseAmount * 0.05;
                return premium;

            } else if (policyType.equals("AUTO")) {
                return baseAmount * 1.15;

            } else if (policyType.equals("HOME")) {
                return baseAmount * 1.10;

            } else {
                // Adding LIFE requires opening this class and adding another branch
                throw new IllegalArgumentException("Unknown policy type: " + policyType);
            }
        }
    }

    // =========================================================================
    // ── TODO 1: InsurancePolicy ──────────────────────────────────────────────
    // Immutable data holder. All fields private and final.
    // Auto-generates policyId: "POL-001" format using a private static counter.
    // Constructor: InsurancePolicy(String holderName, String policyType,
    //                              double baseAmount, int holderAge)
    //   Validate:
    //     • holderName  null/blank  → throw IAE("Holder name cannot be blank")
    //     • policyType  null/blank  → throw IAE("Policy type cannot be blank")
    //     • baseAmount  <= 0        → throw IAE("Base amount must be positive")
    //     • holderAge   < 18        → throw IAE("Holder must be at least 18 years old")
    // Expose only getters; no business logic.
    // =========================================================================

    // TODO 1a: private static int counter = 0;
    // TODO 1b: declare five private final fields
    // TODO 1c: constructor with validation and auto-generated policyId
    // TODO 1d: getters for all five fields
    static class InsurancePolicy {
        private static int counter = 0;

        private final String policyId;
        private final String holderName;
        private final String policyType;
        private final double baseAmount;
        private final int holderAge;

        public InsurancePolicy(String holderName, String policyType,
                               double baseAmount, int holderAge){
            if(holderName == null || holderName.isBlank()) {
                throw new IllegalArgumentException("Holder Name cannot be null or Blank");
            }
            if(policyType == null || policyType.isBlank()) {
                throw new IllegalArgumentException("Policy Type cannot be null or Blank");
            }
            if(baseAmount <= 0){
                throw new IllegalArgumentException("Base Amount cannot be less than zero and must be positive");
            }
            if(holderAge < 18){
                throw new IllegalArgumentException("Age must be atleast 18 years old");
            }

            ++counter;
            this.policyId = String.format("POL-%2d", counter);
            this.holderName = holderName;
            this.policyType = policyType;
            this.baseAmount = baseAmount;
            this.holderAge = holderAge;
        }

        public String getPolicyId() { return policyId; }
        public String getHolderName() { return holderName; }
        public String getPolicyType() { return policyType; }
        public double getBaseAmount() { return baseAmount; }
        public int getHolderAge() { return holderAge; }
    }

    // =========================================================================
    // ── TODO 2: PremiumStrategy (interface) ──────────────────────────────────
    // Two methods:
    //   double calculate(InsurancePolicy policy)
    //   String getPolicyType()
    // =========================================================================

    // TODO 2: declare the interface with the two methods above
    interface PremiumStrategy {
        double calculate(InsurancePolicy policy);
        String getPolicyType();
    }

    // =========================================================================
    // ── TODO 3: HealthStrategy ───────────────────────────────────────────────
    // getPolicyType() → "HEALTH"
    // calculate(policy):
    //   premium = policy.getBaseAmount() * 1.20
    //   if policy.getHolderAge() > 45: premium += policy.getBaseAmount() * 0.05
    //   return premium
    // =========================================================================

    // TODO 3: implement HealthStrategy
    static class HealthStrategy implements PremiumStrategy {
        @Override public String getPolicyType() { return "HEALTH"; }

        @Override
        public double calculate(InsurancePolicy policy) {
            double premium = policy.getBaseAmount() * 1.20;
            if(policy.holderAge > 45) premium += policy.getBaseAmount() * 0.05;
            return premium;
        }
    }

    // =========================================================================
    // ── TODO 4: AutoStrategy ─────────────────────────────────────────────────
    // getPolicyType() → "AUTO"
    // calculate(policy) → policy.getBaseAmount() * 1.15
    // =========================================================================

    // TODO 4: implement AutoStrategy
    static class AutoStrategy implements PremiumStrategy {
        @Override public String getPolicyType() { return "AUTO"; }

        @Override
        public double calculate(InsurancePolicy policy){
            return policy.getBaseAmount() * 1.15;
        }
    }

    // =========================================================================
    // ── TODO 5: HomeStrategy ─────────────────────────────────────────────────
    // getPolicyType() → "HOME"
    // calculate(policy) → policy.getBaseAmount() * 1.10
    // =========================================================================

    // TODO 5: implement HomeStrategy
    static class HomeStrategy implements PremiumStrategy {
        @Override public String getPolicyType() { return "HOME"; }
        @Override
        public double calculate(InsurancePolicy policy){
            return Math.round(policy.getBaseAmount() * 1.10);
        }
    }

    // =========================================================================
    // ── TODO 6: PremiumCalculator ────────────────────────────────────────────
    // Fields:
    //   private final Map<String, PremiumStrategy> strategies (use HashMap)
    //
    // Methods:
    //   register(PremiumStrategy strategy) : void
    //     • null strategy → throw IAE("Strategy cannot be null")
    //     • store: strategies.put(strategy.getPolicyType(), strategy)
    //
    //   calculate(InsurancePolicy policy) : double
    //     • look up strategies.get(policy.getPolicyType())
    //     • if null → throw IAE("No strategy registered for: " + policy.getPolicyType())
    //     • return strategy.calculate(policy)
    //     • NO if/else or switch inside this method
    //
    //   getSummary(InsurancePolicy policy) : String
    //     Format exactly:
    //     "POL-001 | HEALTH | Alice | Base: ₹10000.00 | Premium: ₹12500.00"
    //
    //   isSupported(String policyType) : boolean
    //     return strategies.containsKey(policyType)
    // =========================================================================

    // TODO 6a: private final Map<String, PremiumStrategy> strategies = new HashMap<>()
    // TODO 6b: register(PremiumStrategy)
    // TODO 6c: calculate(InsurancePolicy)  — NO if/else allowed
    // TODO 6d: getSummary(InsurancePolicy)
    // TODO 6e: isSupported(String)
    static class PremiumCalculator {
        private final Map<String, PremiumStrategy> strategies;

        public PremiumCalculator() {
            strategies = new HashMap<>();
        }

        public void register(PremiumStrategy strategy) {
            if(strategy == null) {
                throw new IllegalArgumentException("Strategy cannot be null");
            }
            strategies.put(strategy.getPolicyType(), strategy);
        }

        public double calculate(InsurancePolicy policy) {
            PremiumStrategy strategy = strategies.get(policy.getPolicyType());
            if(strategy == null) throw new IllegalArgumentException("No strategy register for : "
                    + policy.getPolicyType());
            return strategy.calculate(policy);
        }

        public String getSummary(InsurancePolicy policy) {
            if(strategies.containsKey(policy.getPolicyType()))
            {
                PremiumStrategy strategy = strategies.get(policy.getPolicyType());
                return String.format("%s | %s | %s | Base: ₹%.2f | Premium : ₹%.2f",
                        policy.getPolicyId(), policy.getPolicyType(), policy.getHolderName(), policy.getBaseAmount(),
                        strategy.calculate(policy));
            }
            return "Strategy is not yet registered So Summary cannot be provided";
        }

        public boolean isSupported(String policyType) {
            return strategies.containsKey(policyType);
        }
    }
    // =========================================================================
    // DO NOT MODIFY — pre-written tests; fill in the TODOs above to make them pass
    // =========================================================================

    public static void main(String[] args) {

        // Uncomment after implementing your classes:
        //
         PremiumCalculator calculator = new PremiumCalculator();
         calculator.register(new HealthStrategy());
         calculator.register(new AutoStrategy());
         calculator.register(new HomeStrategy());

        System.out.println("═══ Test 1: InsurancePolicy construction ════════════════");
         InsurancePolicy p1 = new InsurancePolicy("Alice", "HEALTH", 10000.0, 50);
         System.out.println("policyId    : " + p1.getPolicyId());     // POL-001
         System.out.println("holderName  : " + p1.getHolderName());   // Alice
         System.out.println("policyType  : " + p1.getPolicyType());   // HEALTH
         System.out.println("baseAmount  : " + p1.getBaseAmount());   // 10000.0
         System.out.println("holderAge   : " + p1.getHolderAge());    // 50
         System.out.println("Test 1 PASSED");

        System.out.println("\n═══ Test 2: InsurancePolicy validation ══════════════════");
         try {
             new InsurancePolicy("", "HEALTH", 10000.0, 30);
             System.out.println("Test 2 FAILED — should have thrown for blank name");
         } catch (IllegalArgumentException e) {
             System.out.println("Test 2a PASSED: " + e.getMessage());
         }
         try {
             new InsurancePolicy("Bob", "AUTO", -500.0, 25);
             System.out.println("Test 2 FAILED — should have thrown for negative base");
         } catch (IllegalArgumentException e) {
             System.out.println("Test 2b PASSED: " + e.getMessage());
         }
         try {
             new InsurancePolicy("Carol", "HOME", 5000.0, 16);
             System.out.println("Test 2 FAILED — should have thrown for age < 18");
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
         // expected: "POL-xxx | AUTO | Grace | Base: ₹5000.00 | Premium: ₹5750.00"
         System.out.println("Contains AUTO   : " + summary.contains("AUTO"));
         System.out.println("Contains Grace  : " + summary.contains("Grace"));
         System.out.println("Contains 5750.00: " + summary.contains("5750.00"));
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
        // Create LifeStrategy as a NEW class (written below in this file or as a new class).
        // Register it. Verify it works. Zero changes to any existing class.
        //
        // LifeStrategy: getPolicyType() → "LIFE", calculate() → base * 1.30
        //
         calculator.register(new LifeStrategy());
         InsurancePolicy lifePol = new InsurancePolicy("Ivy", "LIFE", 15000.0, 42);
         double lifePremium = calculator.calculate(lifePol);
         System.out.println("Life premium: " + lifePremium);        // 19500.0
         System.out.println("LIFE supported now: " + calculator.isSupported("LIFE")); // true
         System.out.println("Test 10 PASSED: " + (lifePremium == 19500.0));

        System.out.println("\n[Uncomment the test code above after implementing your classes]");
        System.out.println("[For Test 10, add LifeStrategy as a new static inner class at the");
        System.out.println(" bottom of this file — do NOT modify any existing class to make it work]");
    }

    // =========================================================================
    // ── TODO 7 (OCP proof): LifeStrategy ─────────────────────────────────────
    // Add this class at the bottom, AFTER completing all other TODOs.
    // getPolicyType() → "LIFE"
    // calculate(policy) → policy.getBaseAmount() * 1.30
    //
    // OCP check: Did you need to modify InsurancePolicy?       NO ✅
    //            Did you need to modify HealthStrategy?         NO ✅
    //            Did you need to modify AutoStrategy?           NO ✅
    //            Did you need to modify HomeStrategy?           NO ✅
    //            Did you need to modify PremiumCalculator?      NO ✅
    //            Only new code was written (one new class).    YES ✅ → OCP satisfied
    // =========================================================================

    // TODO 7: implement LifeStrategy here
    static class LifeStrategy implements PremiumStrategy {
        @Override public String getPolicyType() { return "LIFE"; }
        @Override public double calculate(InsurancePolicy policy) {
            return policy.getBaseAmount() * 1.30;
        }
    }
}

/*
 * ═══════════════════════════════════════════════════════════════════════════
 * HINTS
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * HINT 1 (Gentle) — Think about what changes when a new policy type arrives.
 *   The calculation logic changes, but the dispatcher (PremiumCalculator) should not.
 *   Ask yourself: "If I add LIFE insurance, which classes do I have to open?"
 *   With OCP, the answer must be: "Only the new LifeStrategy class."
 *   For PremiumCalculator, think about a data structure that maps a String key
 *   (the policyType) to the strategy object that knows how to calculate for it.
 *   When you look up by key, no if/else is needed — the map does the dispatch.
 *
 * HINT 2 (Direct) — Implementation pointers:
 *   • PremiumStrategy: interface with `double calculate(InsurancePolicy)` and `String getPolicyType()`
 *   • HealthStrategy, AutoStrategy, HomeStrategy: each implements PremiumStrategy
 *   • PremiumCalculator: `private final Map<String, PremiumStrategy> strategies = new HashMap<>()`
 *     - register() → strategies.put(strategy.getPolicyType(), strategy)
 *     - calculate() → strategies.get(policy.getPolicyType())  [no if/else!]
 *   • InsurancePolicy.policyId: String.format("POL-%03d", ++counter)
 *   • getSummary(): String.format("%s | %s | %s | Base: ₹%.2f | Premium: ₹%.2f", ...)
 *   • HealthStrategy age check: policy.getHolderAge() > 45
 *
 * HINT 3 (Near-solution) — Class skeletons without method bodies:
 *
 *   static class InsurancePolicy {
 *       private static int counter = 0;
 *       private final String policyId, holderName, policyType;
 *       private final double baseAmount;
 *       private final int    holderAge;
 *       InsurancePolicy(String holderName, String policyType, double baseAmount, int holderAge) {
 *           // validate, then assign
 *           this.policyId = String.format("POL-%03d", ++counter);
 *       }
 *       // getters only
 *   }
 *
 *   interface PremiumStrategy {
 *       double calculate(InsurancePolicy policy);
 *       String getPolicyType();
 *   }
 *
 *   static class HealthStrategy implements PremiumStrategy {
 *       public String getPolicyType() { return "HEALTH"; }
 *       public double calculate(InsurancePolicy policy) {
 *           double premium = policy.getBaseAmount() * 1.20;
 *           if (policy.getHolderAge() > 45) premium += policy.getBaseAmount() * 0.05;
 *           return premium;
 *       }
 *   }
 *
 *   static class AutoStrategy implements PremiumStrategy { ... }  // base * 1.15
 *   static class HomeStrategy implements PremiumStrategy { ... }  // base * 1.10
 *
 *   static class PremiumCalculator {
 *       private final Map<String, PremiumStrategy> strategies = new HashMap<>();
 *       void   register(PremiumStrategy s) { ... }
 *       double calculate(InsurancePolicy p) { ... }  // strategies.get(p.getPolicyType())
 *       String getSummary(InsurancePolicy p) { ... }
 *       boolean isSupported(String type) { ... }
 *   }
 *
 *   // OCP proof — new class, no modification to any class above:
 *   static class LifeStrategy implements PremiumStrategy {
 *       public String getPolicyType() { return "LIFE"; }
 *       public double calculate(InsurancePolicy policy) { return policy.getBaseAmount() * 1.30; }
 *   }
 */
