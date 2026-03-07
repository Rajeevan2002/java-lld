# Review — Strategy Pattern: Shipping Cost Calculator

---

## Exercise Errata — Test 8

**Test 8 in the skeleton has incorrect expected values — this is a bug in the exercise, not in your code.**

The `expected` array `{ 14.00, 31.00, 40.00 }` was copied from earlier tests that used different inputs
(3 kg and 2 kg/$500). Test 8 uses 4 kg and $200 declared, so the correct expected values are:
- StandardShipping(4 kg): `4.0 × 3.50 = 14.00` ✅ (happened to match)
- ExpressShipping(4 kg): `10.00 + 4.0 × 7.00 = 38.00` — exercise said 31.00 ❌ (exercise bug)
- OvernightShipping(4 kg, $200): `4.0 × 15.00 + 200.0 × 0.02 = 64.00` — exercise said 40.00 ❌ (exercise bug)

Your formulas are correct. Tests 1–7 validate this independently with the right inputs.

---

## What You Got Right

- **`ShippingStrategy` interface declared correctly** — single `calculate(double, double)` method; return type `double`. This is the contract that decouples `Order` from every concrete tier.
- **Strategy constants as private final fields, not constructor parameters** — `ratePerKg`, `baseFee`, and `insuranceRate` are hardcoded inside each class. This is the right encapsulation: the calling code has no business knowing or controlling the rate.
- **All three formulas correct** — Standard minimum charge (`Math.max` equivalent), Express base fee, Overnight rate + insurance all produce the right numbers (confirmed by Tests 1–4).
- **`Order.strategy` typed as `ShippingStrategy`, not a concrete class** — `private ShippingStrategy strategy` is the most critical line in the entire exercise. It enables polymorphic dispatch and makes `setStrategy()` work with any strategy, present or future.
- **No `instanceof` anywhere in `Order`** — `checkout()` is one delegation call. Order has zero knowledge of which tier is active.
- **`weightKg <= 0` validation with exact message** — `"weightKg must be > 0"` matches the spec; Test 7 confirms it.
- **Runtime swap works correctly** — Test 6 confirms Standard → Express → Overnight on the same `Order` object without reconstructing it; the swap is clean and behavioural change is immediate.
- **`declaredValue` correctly threaded through** — passed from `Order` fields into `strategy.calculate(weightKg, declaredValue)`, so `OvernightShipping` can access it without `Order` knowing about insurance.

---

## Issues Found

**1.**
- **Severity**: Minor
- **What**: Spurious import `jdk.jfr.Percentage` — this is a JDK-internal Flight Recorder annotation not used anywhere in the file.
- **Your code**:
  ```java
  import jdk.jfr.Percentage;
  ```
- **Fix**: Delete the line.
- **Why it matters**: JDK-internal imports (`jdk.*`) are not part of the public API and may not be available on all JVMs; an unused import also flags as a warning in any linter.

**2.**
- **Severity**: Minor
- **What**: Double space before `→` in the Standard print format — `"[Standard] %.2f kg  → $%.2f%n"` — produces `"[Standard] 4.00 kg  → $14.00"` instead of `"[Standard] 4.00 kg → $14.00"`.
- **Your code**:
  ```java
  System.out.printf("[Standard] %.2f kg  → $%.2f%n", weightKg, cost);
  ```
- **Fix**:
  ```java
  System.out.printf("[Standard] %.2f kg → $%.2f%n", weightKg, cost);
  ```
- **Why it matters**: Format strings are an exact contract — in a real system, parsers or log aggregators expecting a fixed format would misparse every Standard line.

**3.**
- **Severity**: Minor
- **What**: `public` visibility on `Order`'s constructor and methods is unnecessary for inner static classes in a top-level class — package-private is the right scope.
- **Your code**:
  ```java
  public Order(String orderId, ...) { ... }
  public void setStrategy(...) { ... }
  public double checkout() { ... }
  ```
- **Fix**:
  ```java
  Order(String orderId, ...) { ... }
  void setStrategy(...) { ... }
  double checkout() { ... }
  ```
- **Why it matters**: Overly broad visibility on inner-class members signals unfamiliarity with Java access scoping; interviewers notice.

---

## Score Card

| Requirement | Result |
|---|---|
| `ShippingStrategy` interface with `calculate(double, double) → double` | ✅ |
| `StandardShipping` — `ratePerKg = 3.50` as private final field | ✅ |
| `StandardShipping` — minimum charge 5.00 | ✅ |
| `StandardShipping` — correct print format | ⚠️ (double space) |
| `ExpressShipping` — `ratePerKg = 7.00`, `baseFee = 10.00` as private final fields | ✅ |
| `ExpressShipping` — correct formula | ✅ |
| `OvernightShipping` — `ratePerKg = 15.00`, `insuranceRate = 0.02` as private final fields | ✅ |
| `OvernightShipping` — correct formula | ✅ |
| `Order` — 4 fields with correct types/mutability | ✅ |
| `Order.strategy` typed as interface, NOT concrete class | ✅ |
| `Order` constructor validates `weightKg <= 0` with exact message | ✅ |
| `Order.setStrategy()` replaces strategy | ✅ |
| `Order.checkout()` delegates to strategy with no instanceof | ✅ |
| `Order.checkout()` prints correct format and returns cost | ✅ |
| No spurious imports | ❌ (jdk.jfr.Percentage) |
| Tests 1–7 pass | ✅ |
| Test 8 expected values | ❌ (exercise bug — student's formulas correct) |

---

## Key Takeaways — Do Not Miss These

**TK-1: The context field must be typed as the interface, always.**
`private ShippingStrategy strategy` — if this were `private StandardShipping strategy`, `setStrategy()` could only accept `StandardShipping` objects; polymorphism collapses entirely.
*In interviews: this is the first line an interviewer checks when reviewing a Strategy implementation.*

**TK-2: Strategies own their algorithm configuration; the context owns the data.**
`ratePerKg` belongs in `ExpressShipping`, not in `Order`. `weightKg` belongs in `Order`, not in any strategy. Mixing these up breaks encapsulation and forces the caller to know details it shouldn't.
*In interviews: "Where does X live?" is a standard design question — the answer is always the class whose responsibility it is.*

**TK-3: `checkout()` should have zero branching — one delegation call.**
`strategy.calculate(weightKg, declaredValue)` is the entire algorithm. If you find yourself writing `if (strategy instanceof ...)` inside `checkout()`, you are re-implementing the switch that Strategy was supposed to eliminate.
*In interviews: any `instanceof` in the context is an automatic design flag.*

**TK-4: Runtime strategy swap is the pattern's primary value proposition.**
The same `Order` object transitions from Standard → Express → Overnight without reconstruction. This is impossible if the strategy is baked into the constructor as a concrete type or an enum.
*In interviews: demonstrating `setStrategy()` and re-running the context is what distinguishes "knows Strategy" from "memorised the diagram".*

**TK-5: Strategy vs Template Method — composition vs inheritance.**
Strategy extracts the whole algorithm behind an interface (composition). Template Method fixes the skeleton in an abstract class and varies only the steps (inheritance). Strategy wins when you need runtime swap; Template Method wins when the skeleton must be enforced.
*In interviews: "Why not just use an abstract class?" is almost always the follow-up.*
