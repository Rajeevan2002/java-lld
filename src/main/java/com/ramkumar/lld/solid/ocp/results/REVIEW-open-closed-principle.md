# Review — Open/Closed Principle (OCP)
Phase 2, Topic 2 | Scenario B: Insurance Premium Calculator

---

## What You Got Right

- **OCP core satisfied**: `PremiumCalculator` contains zero if/else or switch on policy type — pure `strategies.get()` dispatch. This is the central lesson and it was executed perfectly.
- **Plugin registration pattern**: `register(PremiumStrategy)` keyed on `getPolicyType()` — callers extend the system by registering new strategies. `PremiumCalculator` never changes.
- **LifeStrategy (OCP proof)**: Added as a new class with zero modifications to `InsurancePolicy`, `HealthStrategy`, `AutoStrategy`, `HomeStrategy`, or `PremiumCalculator`. Exactly what OCP demands.
- **PremiumStrategy interface**: Clean two-method contract — `calculate()` returns a value; `getPolicyType()` identifies the strategy. Both follow the right abstraction.
- **`register()` null guard**: `if(strategy == null) throw new IAE(...)` — good defensive boundary.
- **`isSupported()`**: Clean one-liner `strategies.containsKey(policyType)` — correct and minimal.
- **All validations in `InsurancePolicy`**: null check before `isBlank()`, `baseAmount <= 0`, `holderAge < 18` — all in the right class, all in the right order.
- **`calculate()` delegates completely**: No inlined math, just `strategy.calculate(policy)`. The single line of delegation is the payoff of the whole pattern.

---

## Issues Found

### Issue 1 — Bug: `"POL-%2d"` produces space-padded IDs, not zero-padded

**Severity**: Bug

`%2d` pads with spaces to width 2, producing `"POL- 1"`. The spec requires `"POL-001"` — zero-padded to 3 digits.

**Your code:**
```java
this.policyId = String.format("POL-%2d", counter);
// counter=1  → "POL- 1"   (space + 1, width 2)
```

**Fix:**
```java
this.policyId = String.format("POL-%03d", counter);
// counter=1  → "POL-001"
```

**Why it matters**: Test 1 prints `"POL- 1"` but the comment says `"// POL-001"` — the test appears to pass because it never asserts the format. In production, policy IDs used in contracts, emails, and databases are malformed — breaking sorting, display, and any downstream system that parses the ID.

---

### Issue 2 — Bug: `HomeStrategy` uses `Math.round()`, changing precision

**Severity**: Bug

`Math.round()` returns a `long`, rounding the premium. Any non-integer base gives a truncated result — losing cents.

**Your code:**
```java
return Math.round(policy.getBaseAmount() * 1.10);
// base=12000.0 → Math.round(13200.0) = 13200L → 13200.0  (happens to be exact in test)
// base=10001.0 → Math.round(11001.1) = 11001L → 11001.0  (0.1 silently lost)
```

**Fix:**
```java
return policy.getBaseAmount() * 1.10;
```

**Why it matters**: Test 6 passes only because `12000 * 1.10` is exactly representable as a double. Change the base and the test fails. Insurance premiums must not be silently truncated — even ₹0.10 per policy across millions is a regulatory compliance issue.

---

### Issue 3 — Design: `HealthStrategy` accesses `policy.holderAge` directly

**Severity**: Design

`policy.holderAge` reaches into `InsurancePolicy`'s private field. Even though sibling nested classes can access each other's private members in Java, this bypasses the getter.

**Your code:**
```java
if(policy.holderAge > 45) premium += policy.getBaseAmount() * 0.05;
//         ↑ private field — bypasses getHolderAge()
```

**Fix:**
```java
if (policy.getHolderAge() > 45) premium += policy.getBaseAmount() * 0.05;
```

**Why it matters**: If `InsurancePolicy` later derives `holderAge` from a date of birth, the getter handles the computation. Direct field access silently bypasses the updated logic.

---

### Issue 4 — Design: `getSummary()` duplicates lookup and has inconsistent error handling

**Severity**: Design

`getSummary()` calls `containsKey()` + `get()` (two lookups) and returns a fallback string when unregistered — inconsistent with `calculate()`, which throws. Callers cannot detect the failure without string parsing.

**Your code:**
```java
if(strategies.containsKey(policy.getPolicyType())) {
    PremiumStrategy strategy = strategies.get(policy.getPolicyType()); // two lookups
    return String.format("...");
}
return "Strategy is not yet registered So Summary cannot be provided"; // silent failure
```

**Fix** — delegate to `calculate()`:
```java
public String getSummary(InsurancePolicy policy) {
    double premium = calculate(policy);  // throws if not registered — consistent
    return String.format("%s | %s | %s | Base: ₹%.2f | Premium: ₹%.2f",
            policy.getPolicyId(), policy.getPolicyType(), policy.getHolderName(),
            policy.getBaseAmount(), premium);
}
```

**Why it matters**: Mixed error models (throw vs return-string) force every caller to write defensive `if (s.contains("not registered"))` checks — a maintenance burden and a source of silent bugs.

---

### Issue 5 — Minor: `getSummary()` format has extra space before the colon

**Severity**: Minor

Spec format: `"Premium: ₹12500.00"`. Your format: `"Premium : ₹12500.00"` — extra space before the colon.

**Your code:**
```java
String.format("... | Premium : ₹%.2f", ...)
```

**Fix:**
```java
String.format("... | Premium: ₹%.2f", ...)
```

**Why it matters**: Test 7 only checks `summary.contains("5750.00")`, so it passes despite the extra space. But any code that parses the summary string would see `"Premium : ₹"` and fail.

---

## Score Card

| Requirement | Result | Notes |
|---|---|---|
| InsurancePolicy: 5 fields, all `private final` | ✅ | Correct |
| InsurancePolicy: auto-ID `POL-001` format (zero-padded) | ❌ | `%2d` gives `"POL- 1"` not `"POL-001"` |
| InsurancePolicy: null/blank validation, throws IAE | ✅ | Correct |
| InsurancePolicy: `baseAmount <= 0` rejects zero and negative | ✅ | Correct |
| InsurancePolicy: `holderAge < 18` rejects minors | ✅ | Correct |
| `PremiumStrategy` interface: two correct methods | ✅ | Correct |
| `HealthStrategy`: 20% base + 5% surcharge for age > 45 | ✅ | Logic correct |
| `HealthStrategy`: uses getter (not direct field access) | ❌ | `policy.holderAge` bypasses getter |
| `AutoStrategy`: 15% flat | ✅ | Correct |
| `HomeStrategy`: 10% flat, no rounding | ❌ | `Math.round()` changes precision |
| `PremiumCalculator`: Map-based registry, no if/else | ✅ | Correct |
| `PremiumCalculator.register()`: null check | ✅ | Correct |
| `PremiumCalculator.calculate()`: throws for unknown type | ✅ | Correct |
| `PremiumCalculator.getSummary()`: consistent error handling | ❌ | Returns string instead of throwing |
| `PremiumCalculator.getSummary()`: correct format (no extra space) | ⚠️ | Extra space before colon |
| `PremiumCalculator.isSupported()`: correct | ✅ | Correct |
| OCP proof: `LifeStrategy` added with zero modification | ✅ | Correct |
| No `instanceof` checks | ✅ | Correct |

---

## Key Takeaways — Do Not Miss These

**TK-1: `%03d` means zero-padded to 3 digits; `%2d` means space-padded to width 2**
In `String.format`, the `0` flag before the width specifier switches from space-padding to zero-padding. `"POL-%03d"` → `"POL-001"`. `"POL-%2d"` → `"POL- 1"`. Memorise: if IDs must look like `001`, always use `0` before the width.
*Interview relevance*: ID generation is in every machine-coding problem — wrong format breaks sorting, display, and downstream parsing.

**TK-2: Never use `Math.round()` where you need exact floating-point arithmetic**
`Math.round(x)` returns a `long` — it silently truncates `11001.1` to `11001`. For money/premiums, return the raw `double` multiplication; format-rounding only happens at the display layer (`%.2f` in `String.format`).
*Interview relevance*: Financial calculation bugs introduced by silent rounding are a classic code-review catch — every interviewer who has worked in fintech looks for this.

**TK-3: Always use the getter, even from sibling classes**
Java allows sibling nested classes to access each other's private fields — that doesn't mean you should. The getter is the contract; it can evolve (add logging, lazy computation, type conversion) without touching callers. Direct field access hard-couples you to the implementation detail.
*Interview relevance*: In code reviews, `someObject.privateField` from a different class is flagged immediately as an encapsulation violation.

**TK-4: Delegate, don't duplicate — `getSummary()` should call `calculate()`**
Every time you see `containsKey()` immediately followed by `get()`, consolidate to a single `get()` + null check — or better, delegate to the method that already handles it. `getSummary()` should start with `double premium = calculate(policy)`.
*Interview relevance*: Double-lookup on a map is a recognised code smell — interviewers expect you to write `get()` once and branch on null.

**TK-5: Consistent error contracts — always throw, or always return; never mix**
`calculate()` throws when the type is unregistered. `getSummary()` returns a string. Callers cannot tell success from failure without string parsing. Pick one policy per class: throw for programmer errors (wrong type), return `Optional` for expected-empty cases.
*Interview relevance*: "How do you distinguish a missing record from an error?" is a standard design-review question — mixed return-value + exception signalling is the wrong answer.

**TK-6: The OCP proof test is the most important test in the exercise**
If adding `LifeStrategy` requires only writing one new class and calling `register()` — OCP is satisfied. If you needed an `else-if` anywhere, OCP is violated. Always run this thought experiment as the final check on any OCP implementation.
*Interview relevance*: Interviewers will ask "how would you add a new type?" — the OCP-correct answer is always "I write a new class and register it; nothing else changes."
