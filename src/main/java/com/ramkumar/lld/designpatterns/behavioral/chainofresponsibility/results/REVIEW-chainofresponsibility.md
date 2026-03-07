# Review — Chain of Responsibility Pattern: Purchase Approval

---

## What You Got Right

- **`next` declared `private`** — subclasses are forced to use `passToNext()` and cannot bypass or corrupt the chain link. This is the correct encapsulation boundary for the pattern.
- **`setNext()` returns `next` (not `this`)** — enables fluent chain assembly: `tl.setNext(mgr).setNext(dir).setNext(ceo)`. If it returned `this`, all three `.setNext()` calls would operate on `tl`, silently overwriting the previous link each time and leaving a chain of only `tl → ceo`.
- **`passToNext()` declared `protected`** — visible to subclasses that need to forward; invisible to external callers who should only call `approve()`.
- **`passToNext()` handles the null case** — prints `[Chain] No approver for ...` instead of silently swallowing the request or throwing NPE. This is the correct fallthrough behaviour.
- **Every concrete handler has exactly two branches** — approve (stop) OR forward (escalate). No handler both approves and forwards, and no handler swallows silently.
- **Boundary conditions correct** — `<= 1_000.0`, `<= 10_000.0`, `<= 50_000.0` with `>` handled by `passToNext()`. Test 7 boundary values all pass.
- **CEO does not call `passToNext()`** — correctly implemented as the terminal handler.
- **All 8 tests pass.**

---

## Issues Found

**1.**
- **Severity**: Minor (spec violation — does not affect passing tests)
- **What**: `toString()` format string is missing the closing `]`.
- **Your code**:
  ```java
  return String.format("PurchaseRequest[%s \"%s\" $%.2f", id, description, amount);
  ```
- **Fix**:
  ```java
  return String.format("PurchaseRequest[%s \"%s\" $%.2f]", id, description, amount);
  ```
- **Why it matters**: Any code that parses or logs the `toString()` output would get malformed strings. In a real system this would produce silent data corruption in structured logs.

**2.**
- **Severity**: Minor
- **What**: `PurchaseRequest` constructor and getters are declared `public`; `setNext()` is also `public`. These are inner classes of a package-private outer class — package-private (no modifier) is conventional.
- **Your code**:
  ```java
  public PurchaseRequest(String id, String description, double amount){ ... }
  public String getId() { return id; }
  public Approver setNext(Approver next) { ... }
  ```
- **Fix**:
  ```java
  PurchaseRequest(String id, String description, double amount){ ... }
  String getId() { return id; }
  Approver setNext(Approver next) { ... }
  ```
- **Why it matters**: Unnecessary `public` widens the published API contract — if these classes are ever extracted to their own files, every `public` method becomes a commitment that must be maintained.

**3.**
- **Severity**: Minor
- **What**: Skeleton TODO comment blocks for Director (lines 155–161) and CEO (lines 174–179) were left in the file after implementing those classes.
- **Fix**: Delete the TODO comment blocks once a TODO is implemented.
- **Why it matters**: Stale TODO comments create confusion — a reader cannot tell whether the class is complete or still in progress.

---

## Score Card

| Requirement | Result |
|---|---|
| `PurchaseRequest` — 3 `private final` fields | ✅ |
| Constructor + getters | ✅ |
| `toString()` format includes closing `]` | ❌ |
| `Approver.next` is `private` | ✅ |
| `setNext()` returns `next` (not `this`, not `void`) | ✅ |
| `abstract void approve(PurchaseRequest)` | ✅ |
| `passToNext()` is `protected` | ✅ |
| `passToNext()` null-guard + `[Chain] No approver for...` fallback | ✅ |
| `TeamLead` — approves ≤ $1,000; else `passToNext()` | ✅ |
| `Manager` — approves ≤ $10,000; else `passToNext()` | ✅ |
| `Director` — approves ≤ $50,000; else `passToNext()` | ✅ |
| `CEO` — always approves; does NOT call `passToNext()` | ✅ |
| All 8 tests pass | ✅ |

---

## Key Takeaways — Do Not Miss These

**TK-1: `setNext()` must return `next`, not `this`.**
Returning `this` silently breaks fluent chaining: every `.setNext()` in `a.setNext(b).setNext(c)` operates on `a`, overwriting the previous link so the final chain is `a → c` (b is lost). Returning `next` means each call extends from the handler just added.
*Interviewers check this directly — Test 4 and Test 9 both probe it.*

**TK-2: `next` must be `private`; subclasses call `passToNext()`, never `next` directly.**
`passToNext()` is the single point that centralises the null-guard. If `next` were `protected`, a subclass could call `next.approve()` directly — bypassing the null check and throwing NPE on the last handler. The invariant "null means end of chain" belongs in one place.
*In interviews: "why protected passToNext instead of protected next?" — this is the answer.*

**TK-3: Every concrete handler has exactly two branches — handle OR forward.**
A handler that approves AND then calls `passToNext()` fires two approvals for the same request. A handler that neither approves nor forwards silently swallows the request. Both are bugs. The structure is always: `if (can handle) { approve; } else { passToNext(); }`.
*In interviews: draw the two-branch structure; explain why both branches are mandatory.*

**TK-4: The fallthrough case must be handled explicitly.**
When `next == null` and the request is not handled, silently doing nothing is a bug — the request disappears without trace. `passToNext()` should log, throw, or otherwise signal "no handler found". Test 5 directly probes this.
*In production systems, silent request loss is harder to debug than an explicit error.*

**TK-5: CoR vs if/else — why the pattern is worth the extra code.**
An if/else chain in a single method requires modifying that method to add a new tier (violates OCP). CoR adds a new handler class and updates the chain-assembly call — existing handlers are untouched. The cost is one new class; the benefit is zero modification of existing code.
*Interviewers ask: "couldn't you just use if/else?" — OCP is the answer.*
