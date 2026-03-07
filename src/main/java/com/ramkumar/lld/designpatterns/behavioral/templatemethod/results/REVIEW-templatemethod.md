# Review — Template Method Pattern: Tax Calculation Pipeline

---

## What You Got Right

- **`calculate()` declared `final`** — the single most important modifier in the exercise. Without `final`, a subclass could override the skeleton, reorder or skip steps, and break the pattern's guarantee. You got it right.
- **Correct 4-step sequence in the template method** — `computeTaxableIncome → applyTaxRate → applyCredits → printBreakdown → return finalTax`. The sequence is exactly right and each call maps to the correct step type.
- **`applyCredits()` is a concrete hook with a sensible default** (`return tax`) — `IndividualTaxCalculator` is therefore NOT required to implement it, which is precisely the "optional extension point" behaviour a hook must provide.
- **`CorporateTaxCalculator` correctly overrides only the hook** — it inherits the template method and the two abstract implementations, and adds only the credit reduction. This is the minimum override set for a class that adds optional behaviour.
- **`IndividualTaxCalculator` correctly does NOT override `applyCredits()`** — uses the base default, proving the hook's default is functional and that subclasses are not forced to implement it.
- **`printBreakdown()` is concrete and not overridden** — shared formatting belongs in the base class; neither subclass touches it.
- **All 8 tests pass** — including the anonymous subclass in Test 8 (which proves `calculate()` is truly `final`).

---

## Issues Found

**1.**
- **Severity**: Minor
- **What**: `calculate()`, step methods, and `printBreakdown()` are all package-private (no visibility modifier). Abstract methods and hooks designed for subclass override should be `protected` to make the design intent explicit and to support subclasses in other packages.
- **Your code**:
  ```java
  final double calculate(double grossIncome) { ... }
  abstract double computeTaxableIncome(double grossIncome);
  abstract double applyTaxRate(double taxableIncome);
  double applyCredits(double tax) { ... }
  void printBreakdown(...) { ... }
  ```
- **Fix**:
  ```java
  public final double calculate(double grossIncome) { ... }
  protected abstract double computeTaxableIncome(double grossIncome);
  protected abstract double applyTaxRate(double taxableIncome);
  protected double applyCredits(double tax) { ... }
  protected void printBreakdown(...) { ... }
  ```
- **Why it matters**: A subclass in a different package cannot override package-private methods — the pattern silently breaks the moment the class hierarchy crosses package boundaries.

**2.**
- **Severity**: Minor
- **What**: Unusual formatting in `printBreakdown` — the comma before the argument list is on the next line with leading spaces.
- **Your code**:
  ```java
  System.out.printf("[Tax] Gross: $%.2f | Taxable: $%.2f | Pre-credit: $%.2f | Due: $%.2f%n"
          , gross, taxable, tax, finalTax);
  ```
- **Fix**:
  ```java
  System.out.printf("[Tax] Gross: $%.2f | Taxable: $%.2f | Pre-credit: $%.2f | Due: $%.2f%n",
      gross, taxable, tax, finalTax);
  ```
- **Why it matters**: Style consistency — the trailing-comma style is unconventional in Java and is flagged by most formatters.

**3.**
- **Severity**: Minor
- **What**: Double space before `applyCredits(tax)` in the `calculate()` body.
- **Your code**:
  ```java
  double finalTax =  applyCredits(tax);
  ```
- **Fix**:
  ```java
  double finalTax = applyCredits(tax);
  ```
- **Why it matters**: Style noise — signals a rushed edit.

---

## Score Card

| Requirement | Result |
|---|---|
| `TaxCalculator` declared as `abstract class` | ✅ |
| `calculate()` declared `final` | ✅ |
| Correct 4-step sequence in `calculate()` | ✅ |
| `computeTaxableIncome` and `applyTaxRate` declared `abstract` | ✅ |
| `applyCredits` hook with default `return tax` | ✅ |
| `printBreakdown` concrete with correct format string | ✅ |
| `IndividualTaxCalculator` — correct deduction and rate | ✅ |
| `IndividualTaxCalculator` — does NOT override `applyCredits` | ✅ |
| `CorporateTaxCalculator` — correct deduction and rate | ✅ |
| `CorporateTaxCalculator` — correct hook override (`tax * 0.95`) | ✅ |
| All 8 tests pass | ✅ |
| Step methods declared `protected` (not package-private) | ❌ |
| `calculate()` declared `public` | ❌ |
| No double spaces | ❌ |
| No unusual comma placement in method calls | ❌ |

---

## Key Takeaways — Do Not Miss These

**TK-1: The template method must be `final` — this is non-negotiable.**
Without `final`, a subclass can override `calculate()` and bypass the skeleton entirely — reordering steps, skipping `printBreakdown`, or calling `applyTaxRate` before `computeTaxableIncome`. `final` is what makes the pattern's guarantee concrete.
*Interviewers specifically ask "why is the template method final?" — the answer is "to prevent subclasses from reordering or skipping steps."*

**TK-2: Abstract steps vs hooks — understand the difference and when to use each.**
An `abstract` step forces every subclass to provide an implementation — use it when there is no sensible default. A hook has a concrete default — use it for optional extension points where most subclasses won't need to do anything special. Misclassifying these (making a hook abstract or an abstract step a hook) forces unnecessary implementation or removes required customisation.
*In interviews: "What is a hook method?" is a direct follow-up to "explain Template Method."*

**TK-3: Step methods should be `protected`, not package-private.**
`protected` signals to readers and the compiler that the method is part of the extension contract for subclasses. Package-private works in the same package but silently breaks the moment a subclass lives in a different package. The Hint 3 skeleton in the exercise uses `protected` for a reason.
*In interviews: visibility on abstract methods reveals whether a candidate understands Java's inheritance model.*

**TK-4: Template Method uses inheritance; Strategy uses composition — know when to pick each.**
Template Method is the right choice when the skeleton is stable and you control both the base class and all subclasses. Strategy is better when you need runtime swap, or when "strategies" come from outside your codebase. Mixing them up is a common interview design question.
*"Why not just use a strategy here?" — answer: the skeleton is fixed and known at compile time; no runtime swap is needed.*

**TK-5: The Hollywood Principle — "don't call us, we'll call you."**
The base class calls the subclass's overridden methods at the right moments; the subclass never directly invokes the template method itself. This inversion of control is what the pattern achieves — the framework (base class) drives execution; the plugin (subclass) fills in the details.
*Interviewers use this principle to test understanding of why the pattern is useful beyond "it avoids code duplication."*
