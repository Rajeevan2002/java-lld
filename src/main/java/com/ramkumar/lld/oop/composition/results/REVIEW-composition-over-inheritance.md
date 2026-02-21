# Review: Composition over Inheritance
**Topic:** Phase 1 — OOP Fundamentals / Topic 4
**Reference solution:** `OrderReference.java` (same directory)

---

## What You Got Right

1. **`getFinalPrice()` delegates entirely — no math in `Order`** — The most important requirement of this exercise. `Order` calls `discountStrategy.apply(basePrice) + shippingStrategy.calculateCost(weightKg)` and does nothing else. This is textbook delegation.

2. **`checkout()` delegates to `paymentBehavior.processPayment(getFinalPrice())`** — Clean single-line delegation. Order doesn't know how payment works.

3. **Runtime swap methods `upgradeShipping()` and `applyNewDiscount()` exist** — Both correctly replace the mutable behavior field. This is the key feature that inheritance cannot offer on a live object.

4. **`Order` has no superclass** — Correctly modelled as a composed class, not a subclass of anything. `Order` IS-A `Order`, not IS-A `DiscountedOrder` or `ShippingOrder`.

5. **All three interfaces are pure contracts** — `DiscountStrategy`, `ShippingStrategy`, `PaymentBehavior` have no fields, no constructors. Correct use of interface as capability contract.

6. **`PercentageDiscount` validation is correct** — `percent <= 0 || percent > 100` correctly covers the full invalid range. Formula `price * (1.0 - percent / 100.0)` is right.

7. **`FlatDiscount.apply()` uses `Math.max(0, ...)`** — Price correctly floors at 0. This is the Null Object–adjacent pattern that prevents negative prices.

8. **`StandardShipping`, `ExpressShipping`, `FreeShipping` are stateless** — No instance fields, no constructors. Pure input → output. Behavior classes should be stateless wherever possible — these are correct.

9. **`final` on data fields in `Order`** — `orderId`, `itemName`, `basePrice`, `weightKg` are immutable after construction. The three behavior fields are correctly left mutable to support runtime swapping.

10. **`CreditCardPayment` validates both length AND numeric content** — Two separate guards cover both failure modes.

---

## Issues Found

### Issue 1 — Bug: `getSummary()` hardcodes `"Express"` instead of using `shippingStrategy.getName()`
**Severity: Bug**

This is the most significant error because it directly contradicts the core principle of the exercise: the summary must reflect the *actual* composed behavior, not a hardcoded string.

```java
// Your code — "Express" is hardcoded regardless of which shipping strategy is used
return String.format("[%s] %s | Base ₹%.0f | Discount: %s | Shipping: Express (%dd) ...",
        orderId, itemName, basePrice,
        discountStrategy.getDescription(),
        shippingStrategy.getEstimatedDays(),   // days are dynamic...
        paymentBehavior.getPaymentMethod(),
        getFinalPrice());
// For order1 (StandardShipping): prints "Shipping: Express (5d)" — wrong!
```

```java
// Fix — use shippingStrategy.getName() to delegate the name too
return String.format("[%s] %s | Base ₹%.0f | Discount: %s | Shipping: %s (%dd) ...",
        orderId, itemName, basePrice,
        discountStrategy.getDescription(),
        shippingStrategy.getName(),           // dynamic — delegates to composed object
        shippingStrategy.getEstimatedDays(),
        paymentBehavior.getPaymentMethod(),
        getFinalPrice());
```

**Why it matters:** If you upgrade to `FreeShipping` at runtime and `getSummary()` still says "Express", you have a display inconsistency. In a real order confirmation email, the customer sees "Express Shipping" while paying a Free Shipping price — either a legal issue or a customer service nightmare.

---

### Issue 2 — Missing Validation: `FlatDiscount` allows `amount = 0`
**Severity: Missing Validation**

Spec says `validate amount > 0`. You wrote `amount < 0`, which lets `new FlatDiscount(0)` succeed. A `FlatDiscount` of zero is identical to `NoDiscount` — it's a nonsensical no-op that should be rejected.

```java
// Your code — accepts 0
if(amount < 0){
    throw new IllegalArgumentException("Amount Cannot be Less than Zero");
}
```

```java
// Fix — reject 0 too
if(amount <= 0){
    throw new IllegalArgumentException("Flat discount amount must be greater than 0");
}
```

**Why it matters:** A developer mistakenly passes `0` expecting an error that signals their bug, but instead silently gets a no-op discount. Silent no-ops are harder to debug than early exceptions.

---

### Issue 3 — Missing Validation: `upgradeShipping()` and `applyNewDiscount()` don't null-check
**Severity: Missing Validation**

The spec says "validate not null". Both methods accept a `null` strategy silently, which will cause `NullPointerException` on the next `getFinalPrice()` or `getSummary()` call — far from where the bad value was introduced.

```java
// Your code — null passes through silently
public void upgradeShipping(ShippingStrategy shippingStrategy){
    this.shippingStrategy = shippingStrategy;
}
```

```java
// Fix — fail at the point of bad input
public void upgradeShipping(ShippingStrategy s){
    if (s == null) throw new IllegalArgumentException("Shipping strategy cannot be null");
    this.shippingStrategy = s;
}
```

**Why it matters:** Null strategies cause NPE inside `getFinalPrice()` — the stack trace points to the wrong line. Failing fast at the setter makes the bug location obvious.

---

### Issue 4 — Design: Exposing behavior strategy getters violates Law of Demeter
**Severity: Design**

You expose `getDiscountStrategy()`, `getShippingStrategy()`, `getPaymentBehavior()`. Callers can then bypass `Order`'s delegation:

```java
// Your code enables this Law of Demeter violation
order.getShippingStrategy().calculateCost(2.5);  // caller reaches into Order's internals
order.getDiscountStrategy().apply(50_000);        // bypasses Order's controlled surface
```

```java
// Fix — expose what callers need, not the internal objects
// Instead of getShippingStrategy(), expose:
public String getShippingName()     { return shippingStrategy.getName(); }
public int    getEstimatedDays()    { return shippingStrategy.getEstimatedDays(); }
public double calculateShipping()   { return shippingStrategy.calculateCost(weightKg); }
```

**Why it matters:** If `ShippingStrategy` is refactored to remove `calculateCost()`, all callers who reached through `getShippingStrategy()` break. `Order`'s job is to be the single point of access — don't let callers tunnel through it.

---

### Issue 5 — Minor: `CreditCardPayment` uses `"\\d+"` instead of `"\\d{4}"`
**Severity: Minor**

`"\\d+"` validates "one or more digits" — the separate `length() != 4` check makes it work correctly, but `"\\d{4}"` does both in one and is the idiomatic pattern.

```java
// Your code — two checks
lastFourDigits.length() != 4 || !lastFourDigits.matches("\\d+")

// Cleaner — one check that encodes the exact constraint
!lastFourDigits.matches("\\d{4}")
```

---

### Issue 6 — Minor: Error message says "Cannot be Null" for a primitive `double`
**Severity: Minor**

```java
throw new IllegalArgumentException("Base Price Cannot be Null or < 0");
// double is a primitive — it literally cannot be null
```

```java
// Fix — accurate message
throw new IllegalArgumentException("basePrice and weightKg must be > 0");
```

---

## Score Card

| Requirement | Result | Note |
|-------------|--------|------|
| `DiscountStrategy` interface with `apply()` + `getDescription()` | ✅ | |
| `NoDiscount` returns price unchanged | ✅ | |
| `PercentageDiscount` validates `0 < percent <= 100` | ✅ | |
| `PercentageDiscount.apply()` formula correct | ✅ | |
| `FlatDiscount` validates `amount > 0` | ❌ | Allows 0 (`< 0` instead of `<= 0`) |
| `FlatDiscount.apply()` floors at 0 | ✅ | `Math.max(0, ...)` |
| `ShippingStrategy` interface with 3 methods | ✅ | |
| `StandardShipping` / `ExpressShipping` / `FreeShipping` stateless | ✅ | No fields |
| All shipping formulas correct | ✅ | |
| `PaymentBehavior` interface | ✅ | |
| `CreditCardPayment` validates 4 numeric digits | ✅ | Works, minor style note |
| `UPIPayment` validates `@` | ✅ | |
| `CashPayment` — no fields, always true | ✅ | |
| `Order` has no superclass | ✅ | Composed, not inherited |
| `Order` data fields are `final` | ✅ | |
| `Order` behavior fields are mutable | ✅ | Supports runtime swap |
| `getFinalPrice()` delegates — no math in Order | ✅ | |
| `checkout()` delegates to paymentBehavior | ✅ | |
| `getSummary()` uses `shippingStrategy.getName()` | ❌ | Hardcoded "Express" |
| `upgradeShipping()` null-checks | ❌ | No null guard |
| `applyNewDiscount()` null-checks | ❌ | No null guard |
| Strategy getters follow Law of Demeter | ❌ | Exposes internal objects |
| All 9 tests pass | ⚠️ | Tests 1–6 pass; Test 7 shows wrong shipping name |

---

## Key Takeaways — Do Not Miss These

**TK-1: `getSummary()` must delegate every field — none can be hardcoded**
> If any display string is hardcoded (e.g., `"Express"`), the object's description diverges from its actual state. Every piece of the summary that comes from a composed behavior must be obtained from that behavior.
> *Interview note:* "Does your summary update after a runtime swap?" is a natural follow-up question. If anything is hardcoded, the answer is no — and that exposes a failure to understand delegation.

**TK-2: Runtime swap is the proof — state it explicitly when asked about composition**
> The single clearest way to demonstrate composition over inheritance in an interview is: "I can call `order.upgradeShipping(new ExpressShipping())` on a live object and `getFinalPrice()` immediately reflects the new cost. With inheritance I'd have to create a new subclass object." Practice saying this.
> *Interview note:* Interviewers love the runtime swap because it's concrete, demonstrable, and impossible to replicate cleanly with pure inheritance.

**TK-3: Behavior classes should be stateless — pure input → output**
> `StandardShipping`, `ExpressShipping`, `FreeShipping` have no fields. They take weight in, return cost out. Stateless behavior objects are thread-safe, reusable, and trivially testable. Add state only when the behavior genuinely needs to remember something between calls.
> *Interview note:* "Are your strategy objects thread-safe?" — if they're stateless, the answer is automatically yes.

**TK-4: Never expose composed objects directly — delegate, don't reveal**
> `getShippingStrategy()` lets callers bypass `Order` and call `calculateCost()` directly. The Law of Demeter says: talk to your immediate collaborators, not to their internals. Expose what callers need (`getShippingName()`, `getEstimatedDays()`), not the composed object itself.
> *Interview note:* This is the difference between `order.getShippingStrategy().getName()` (bad) and `order.getShippingName()` (good). The first creates a coupling to `ShippingStrategy`'s interface in the caller; the second doesn't.

**TK-5: Validate at runtime swap points, not just at construction**
> Construction validation protects the initial state. `upgradeShipping(null)` bypasses the constructor — it needs its own guard. Every public mutation point is a new trust boundary.
> *Interview note:* "What happens if I pass null to upgradeShipping?" is a common probe. The answer should be "IllegalArgumentException immediately at the call site, not NPE three lines later."

**TK-6: The class count equation is your interview soundbite**
> With inheritance: 3 discounts × 3 shipping × 3 payments = 27 subclasses. With composition: 3 + 3 + 3 = 9 behavior classes + 1 `Order`. Add a 4th payment type: inheritance needs 12 new subclasses; composition needs 1 new class.
> *Interview note:* State this equation when asked "why composition over inheritance?" It's concrete, quantifiable, and immediately convincing.
