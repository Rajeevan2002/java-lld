# Review — Adapter Pattern (Scenario B: Payment Gateway)

## What You Got Right

1. **Object Adapter via composition** — both `StripeAdapter` and `PayPalAdapter` hold the adaptee as `private final` and never extend it. This is exactly correct and the most important design decision in this pattern.

2. **Constructor injection** — both adapters receive the adaptee through the constructor and assign it to a `final` field, making them immutable and testable.

3. **Cents conversion** — `(int) Math.round(amountUsd * 100)` is the correct approach. Using `Math.round` avoids floating-point truncation bugs (e.g. `15.50 * 100` can produce `1549.9999...` in some runtimes; `round` handles it correctly).

4. **Customer string prefix** — `"cus-" + customerId` correctly follows the spec.

5. **PayPal approval check** — correctly calls `sdk.submitPayment()`, reads `response.isApproved()`, and throws `IllegalStateException` when declined.

6. **Delegation in `refund()`** — both adapters delegate to the underlying SDK and return the right value (`true` for Stripe, the SDK boolean for PayPal).

7. **Polymorphic use** — `PaymentProcessor[] gateways` in Test 8 works correctly because both adapters implement the same interface. You understood why the pattern exists.

---

## Issues Found

**1.**
- **Severity**: Bug
- **What**: ISE message case mismatch — `"Paypal payment declined"` vs required `"PayPal payment declined"`. Test 5 fails because `String.equals()` is case-sensitive.
- **Your code**:
```java
throw new IllegalStateException("Paypal payment declined");
```
- **Fix**:
```java
throw new IllegalStateException("PayPal payment declined");
```
- **Why it matters**: In real systems, exception messages are parsed by callers, monitoring tools, and error logs. A case typo silently breaks downstream error handling.

---

**2.**
- **Severity**: Design
- **What**: Input validation (`amountUsd <= 0`, `customerId null/blank`, `transactionId null/blank`) does not belong in an Adapter. The adapter's sole job is interface translation, not domain validation. The `docs/README.md` calls this out explicitly as a common mistake: *"Putting business logic in the adapter."*
- **Your code** (same pattern in both adapters):
```java
if (amountUsd <= 0) {
    throw new IllegalArgumentException("Amount in USD should be > 0");
}
if (customerId == null || customerId.isBlank()) {
    throw new IllegalArgumentException("CustomerId should not be Blank or NULL");
}
```
- **Fix**: Remove all three validation blocks. The adapter trusts its caller. If validation is needed, it belongs in a service layer above the adapter.
- **Why it matters**: Adapters that validate create two problems — they duplicate logic that should live once in a service, and they make the adapter harder to test in isolation (test data must pass validation before you can even test translation).

---

**3.**
- **Severity**: Minor
- **What**: `StripeAdapter.charge` uses ASCII `->` instead of the Unicode `→` specified in the print format.
- **Your code**:
```java
System.out.printf("[Stripe] Charged $%.2f -> %s%n", amountUsd, chargeId);
```
- **Fix**:
```java
System.out.printf("[Stripe] Charged $%.2f → %s%n", amountUsd, chargeId);
```
- **Why it matters**: In an interview coding exercise, exact output format is part of the contract.

---

**4.**
- **Severity**: Minor
- **What**: `PayPalAdapter.refund` has a stray space before the colon in the format string (`%s : %s` vs `%s: %s`).
- **Your code**:
```java
System.out.printf("[PayPal] Refund %s : %s%n", transactionId, success ? "OK" : "FAILED");
```
- **Fix**:
```java
System.out.printf("[PayPal] Refund %s: %s%n", transactionId, success ? "OK" : "FAILED");
```
- **Why it matters**: Same as above — exact output format is part of the spec contract.

---

## Score Card

| Requirement | Result |
|---|---|
| `PaymentProcessor` interface with `charge` and `refund` declared | ✅ |
| `StripeAdapter` holds `private final StripeClient client` | ✅ |
| `StripeAdapter(StripeClient client)` constructor | ✅ |
| `charge` converts USD → cents with `Math.round` | ✅ |
| `charge` builds `"cus-" + customerId` | ✅ |
| `charge` delegates to `client.createCharge` and returns ID | ✅ |
| `charge` prints `[Stripe] Charged $X.XX → id` (exact arrow) | ⚠️ ASCII `->` used |
| `refund` delegates to `client.reverseCharge` (void) | ✅ |
| `refund` prints `[Stripe] Refunded <id>` | ✅ |
| `refund` returns `true` | ✅ |
| `PayPalAdapter` holds `private final PayPalSDK sdk` | ✅ |
| `PayPalAdapter(PayPalSDK sdk)` constructor | ✅ |
| `charge` calls `sdk.submitPayment` and reads `PayPalResponse` | ✅ |
| `charge` throws `IllegalStateException("PayPal payment declined")` on decline | ❌ case typo |
| `charge` prints `[PayPal] Charged $X.XX → code` | ✅ |
| `charge` returns confirmation code | ✅ |
| `refund` calls `sdk.cancelTransaction` and returns result | ✅ |
| `refund` prints `[PayPal] Refund <id>: OK/FAILED` | ⚠️ extra space before `:` |
| Polymorphic use via `PaymentProcessor[]` | ✅ |
| No business validation inside adapters | ❌ |

---

## Key Takeaways — Do Not Miss These

1. **TK-1: Exception messages are part of the contract.** String literals in `throw` statements are observable API. A case difference between `"Paypal"` and `"PayPal"` silently breaks every caller that does `"PayPal payment declined".equals(e.getMessage())`. In interviews, always copy the exact message string from the spec.

2. **TK-2: Adapters translate, they do not validate.** An adapter's responsibility is exactly one thing: map one interface to another. Validation (null checks, range checks, business rules) belongs in the layer above — a service, controller, or use-case class. Putting it in the adapter mixes responsibilities and duplicates logic.

3. **TK-3: `private final` field + constructor injection is the object adapter template.** Every object adapter you ever write will have this shape. Memorise it: `private final Adaptee adaptee;` set in the constructor, never changed after that.

4. **TK-4: Read the `PayPalResponse` before deciding.** A real-world SDK often returns a response object rather than throwing on failure. The adapter's job is to convert that response-object failure into the exception style the target interface uses. This translation from "return value failure" → "exception failure" is a very common interview scenario.

5. **TK-5: Print format strings are part of the spec.** Treat format strings like test assertions — character-exact. In production, log formats are parsed by tools like Splunk, Datadog, and CloudWatch. A stray space or wrong arrow character breaks regex parsers silently.

---

## Reference Solution

See `AdapterReference.java` in this directory.

Test 9 in the reference solution specifically catches the most common mistake from this exercise: it verifies that the adapter does **not** add its own input validation by passing a negative amount and confirming no `IllegalArgumentException` is thrown by the adapter.
