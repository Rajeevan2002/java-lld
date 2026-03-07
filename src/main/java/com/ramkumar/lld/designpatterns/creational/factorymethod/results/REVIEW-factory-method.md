# Review — Factory Method Pattern (Creational)
Phase 3, Topic 3.2 | Scenario B: Payment Gateway

---

## What You Got Right

- **`PaymentProcessor` interface** — all 4 method signatures declared correctly. Clean, minimal contract.
- **`StripeProcessor.processPayment()` currency validation** — `Set.of("USD", "EUR", "GBP")` + `contains(currency)` is the right approach. No verbose if-chains.
- **`transactionCounter` as instance field** in all three processors — `private int transactionCounter` (not `static`), so each processor instance has its own counter. Core constraint met.
- **`PaymentService.charge()` is clean Factory Method usage** — validates, calls `createProcessor()` (no type check), calls `processPayment()`, logs, returns. This is textbook.
- **`PaymentService.refundTransaction()` validates both `txId` AND `amount`** — both validations present before the factory method call.
- **No `if`/`switch` on gateway type in `PaymentService`** — the business methods reference only `PaymentProcessor`, never any concrete class.
- **All three concrete creators** override `createProcessor()` with a single `return new X()`. Minimal and correct.

---

## Issues Found

### Issue 1 — Design: `processPayment()` missing charging print in all three processors
- **Severity**: Design (spec violation — affects observability)
- **What**: The spec requires `"[<GatewayName>] Charging $<amount> <currency> → tx: <txId>"` but none of the three processors print anything in `processPayment()`.
- **Your code** (all three):
  ```java
  return String.format("STRIPE-%05d", ++transactionCounter);  // no print before this
  ```
- **Fix**:
  ```java
  String txId = String.format("STRIPE-%05d", ++transactionCounter);
  System.out.printf("[Stripe] Charging $%.2f %s → tx: %s%n", amount, currency, txId);
  return txId;
  ```
- **Why it matters**: In a real payment system, every charge attempt must be logged before returning. If a charge is made but the response is lost (network timeout), the only reconciliation record is the log.

### Issue 2 — Bug: `BankTransferProcessor.processPayment()` uses `amount <= 10` instead of `amount < 10`
- **Severity**: Bug
- **What**: The spec says minimum is $10.00 — exactly $10.00 is valid, but `amount <= 10` rejects it.
- **Your code**:
  ```java
  if(amount <= 10){
      throw new IllegalArgumentException("Amount has to > 10");
  }
  ```
- **Fix**:
  ```java
  if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
  if (amount < 10.0) throw new IllegalArgumentException("Bank Transfer minimum is $10.00");
  ```
- **Why it matters**: Off-by-one on a payment boundary rejects valid transactions and creates support escalations.

### Issue 3 — Bug: `BankTransferProcessor.getTransactionFee()` wrong validation order and message
- **Severity**: Bug
- **What**: Checks `amount < 10` first with message "Amount has to be > 0" — missing the `> 0` guard entirely, and using the wrong message for the minimum check.
- **Your code**:
  ```java
  if(amount < 10){
      throw new IllegalArgumentException("Amount has to be > 0");
  }
  ```
- **Fix**:
  ```java
  if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
  if (amount < 10.0) throw new IllegalArgumentException("Bank Transfer minimum is $10.00");
  ```
- **Why it matters**: A caller sending -5.0 sees a technically correct but misleading message. A caller sending 7.0 sees "Amount has to be > 0" when the real problem is the $10 minimum — wasted debugging time.

### Issue 4 — Design: `calculateFee()` calls `getTransactionFee()` twice
- **Severity**: Design
- **What**: The result of `getTransactionFee()` is computed once for printing and a second time for the return — two calls to the same method.
- **Your code**:
  ```java
  System.out.printf("...", amount, paymentProcessor.getTransactionFee(amount));
  return paymentProcessor.getTransactionFee(amount);   // called again
  ```
- **Fix**:
  ```java
  double fee = paymentProcessor.getTransactionFee(amount);
  System.out.printf("[PaymentService] Fee for $%.2f: $%.2f%n", amount, fee);
  return fee;
  ```
- **Why it matters**: If `getTransactionFee()` ever acquires a side effect (metering, logging), you'd trigger it twice per `calculateFee()` call.

### Issue 5 — Minor: Inconsistent print tags in `refund()`
- **Severity**: Minor
- **What**: `PayPalProcessor` prints `[Paypal]` (lowercase 'p'), `BankTransferProcessor` prints `[Bank]`. Spec requires `[PayPal]` and `[Bank Transfer]`.
- **Fix**: Match the exact string returned by `getGatewayName()` in all print statements.
- **Why it matters**: Monitoring tools parse log tags by exact string match — `[Paypal]` and `[PayPal]` are different entries, breaking dashboards and alerts silently.

---

## Score Card

| Requirement | Result |
|---|---|
| `PaymentProcessor` interface — 4 methods | ✅ |
| `StripeProcessor` — validates amount > 0 | ✅ |
| `StripeProcessor` — validates currency not null/blank | ✅ |
| `StripeProcessor` — rejects unsupported currencies | ✅ |
| `StripeProcessor` — prints charging message | ❌ |
| `StripeProcessor` — returns `STRIPE-%05d` tx ID | ✅ |
| `StripeProcessor` — fee formula `amount * 0.029 + 0.30` | ✅ |
| `PayPalProcessor` — accepts any non-blank currency | ✅ |
| `PayPalProcessor` — prints charging message | ❌ |
| `PayPalProcessor` — returns `PAYPAL-%05d` tx ID | ✅ |
| `PayPalProcessor` — `refund` prints `[PayPal]` | ⚠️ (`[Paypal]`) |
| `BankTransferProcessor` — minimum `< $10`, not `<= $10` | ❌ (`<= 10`) |
| `BankTransferProcessor` — correct error message "Bank Transfer minimum is $10.00" | ❌ |
| `BankTransferProcessor` — `refund` prints `[Bank Transfer]` | ❌ (`[Bank]`) |
| `BankTransferProcessor` — prints charging message | ❌ |
| `BankTransferProcessor` — flat $1.50 fee | ✅ |
| `PaymentService.createProcessor()` is abstract | ✅ |
| `PaymentService.charge()` — validates and delegates via factory | ✅ |
| `PaymentService.refundTransaction()` — validates txId and amount | ✅ |
| `PaymentService.calculateFee()` — stores result before returning | ❌ (double call) |
| No `if`/`switch` on gateway type in `PaymentService` | ✅ |
| Business methods reference only `PaymentProcessor` | ✅ |
| `transactionCounter` is instance field (not `static`) | ✅ |
| All three concrete creators override `createProcessor()` | ✅ |

---

## Key Takeaways — Do Not Miss These

**TK-1: The Factory Method isn't just the factory — it's the business methods that call it.**
`createProcessor()` alone is not the pattern. The pattern is `charge()`, `refundTransaction()`, and `calculateFee()` calling `createProcessor()` without knowing which concrete type they get. Interviewers check whether your Creator has meaningful business methods or is just a thin wrapper.

**TK-2: Boundary conditions are `<` not `<=` — always read the spec word for word.**
"Minimum is $10.00" means amounts `< 10.0` are rejected; `10.0` is valid. `amount <= 10` rejects a valid transaction. In payment systems, off-by-one on a boundary produces refunds, chargebacks, and support escalations.

**TK-3: Validate in the right order — generic checks before domain-specific checks.**
`amount > 0` first, then `amount >= 10`. A caller sending `-5.0` to Bank Transfer should see "amount must be > 0", not "Bank Transfer minimum is $10.00". Validation order is part of your API's communication contract with callers.

**TK-4: Store every computed result in a variable before using it twice.**
`getTransactionFee()` called twice in `calculateFee()`. Even if the method is pure today, this habit prevents double-billing, double-logging, and unexpected failures when the method later acquires a side effect.

**TK-5: Print statements are part of the spec — they're your audit trail.**
Missing the charging print in `processPayment()` means no log of the attempt. In production, if a charge is made but the response is lost (network timeout), the only way to reconcile is the log. Every charge must be logged before the return.
