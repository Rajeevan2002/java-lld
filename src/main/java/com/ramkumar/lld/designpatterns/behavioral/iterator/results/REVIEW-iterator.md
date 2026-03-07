# Review — Iterator Pattern: Order History

---

## What You Got Right

- **`OrderHistoryIterator` declared `private`** — concrete iterator types are implementation details; making them private ensures clients can only hold `Iterator<Order>` references, which is the correct encapsulation boundary.
- **`StatusIterator` declared `private`** — same reasoning; the filtered iterator's internal pre-fetch strategy is invisible to callers.
- **Pre-fetch (`peek`) strategy in `StatusIterator`** — `advance()` is called in the constructor and at the end of every `next()`. `hasNext()` only reads `peek != null` without advancing — this is the correct design and the hardest part of the filtered iterator to get right.
- **`cursor < count` in `hasNext()`** — reads `count` from the enclosing `OrderHistory` instance (via inner class access); no getter needed and no stale copy.
- **`NoSuchElementException` with the correct messages** — `"No more orders"` and `"No more " + targetStatus + " orders"` match the spec exactly (modulo a double-space noted below).
- **`orders[cursor++]` in `next()`** — post-increment: return current element then advance. Correct order.
- **`implements Iterable<Order>` on `OrderHistory`** — enables the for-each loop in Test 6; `iterator()` satisfies the contract.
- **All 11 tests pass** — including Test 5 (independent iterators), Test 6 (for-each), and Test 8 (no-match filtered iterator).

---

## Issues Found

**1.**
- **Severity**: Design
- **What**: `advance()` in `StatusIterator` is declared `public` instead of `private`.
- **Your code**:
  ```java
  public void advance(){
      peek = null;
      while(cursor < count) { ... }
  }
  ```
- **Fix**:
  ```java
  private void advance(){
      peek = null;
      while(cursor < count) { ... }
  }
  ```
- **Why it matters**: `advance()` mutates the iterator's internal cursor — calling it externally would silently skip elements and corrupt traversal. Internal helpers that no caller should invoke must be `private`. In an interview, exposing internal state-mutation methods signals unfamiliarity with encapsulation.

**2.**
- **Severity**: Minor
- **What**: Double space before `targetStatus` in the `StatusIterator.next()` exception message.
- **Your code**:
  ```java
  throw new NoSuchElementException("No more " +  targetStatus + " orders");
  ```
- **Fix**:
  ```java
  throw new NoSuchElementException("No more " + targetStatus + " orders");
  ```
- **Why it matters**: Style noise — signals a rushed edit; most IDEs flag this.

**3.**
- **Severity**: Minor
- **What**: `addOrder()`, `size()`, `OrderHistory(int)`, and `getOrderId()`/`getAmountUsd()`/`getStatus()` are all declared `public`. These are inner classes of a top-level class — package-private visibility (no modifier) is conventional for methods that are not part of a published API and are only used within the same file/package.
- **Your code**:
  ```java
  public OrderHistory(int capacity){ ... }
  public void addOrder(Order order){ ... }
  public int size() { ... }
  public String getOrderId() { return orderId; }
  ```
- **Fix**:
  ```java
  OrderHistory(int capacity){ ... }
  void addOrder(Order order){ ... }
  int size() { ... }
  String getOrderId() { return orderId; }
  ```
- **Why it matters**: Unnecessary `public` on internal classes declares a broader API contract than intended. If this class were extracted to its own file later, every `public` method would be part of the published API — including things that should stay internal.

---

## Score Card

| Requirement | Result |
|---|---|
| `Order` — 3 `private final` fields (`orderId`, `amountUsd`, `status`) | ✅ |
| `Order` constructor, getters, `toString()` format | ✅ |
| `OrderHistory` — `private final Order[] orders`, `private int count = 0` | ✅ |
| `OrderHistory(int capacity)` — allocates array | ✅ |
| `addOrder()` — throws `IllegalStateException("Order history is full")` | ✅ |
| `size()` — returns `count` | ✅ |
| `implements Iterable<Order>` + `iterator()` returns new cursor | ✅ |
| `statusIterator(String status)` — returns new `StatusIterator` | ✅ |
| `OrderHistoryIterator` — `private` inner class | ✅ |
| `hasNext()` — reads only, does NOT advance cursor | ✅ |
| `next()` — throws `NoSuchElementException("No more orders")` when empty | ✅ |
| `StatusIterator` — `private` inner class | ✅ |
| `StatusIterator` fields: `targetStatus`, `cursor`, `peek` | ✅ |
| Constructor calls `advance()` to pre-fetch first match | ✅ |
| `advance()` declared `private` | ❌ |
| `hasNext()` — returns `peek != null`, no cursor movement | ✅ |
| `next()` — captures peek, advances, returns captured value | ✅ |
| All 11 tests pass | ✅ |

---

## Key Takeaways — Do Not Miss These

**TK-1: `hasNext()` must NEVER advance the cursor.**
It is a pure read — safe to call zero, one, or ten times without side effects. Any code that does `if (peek == null) advance()` inside `hasNext()` will cause elements to be skipped when `hasNext()` is called twice before `next()`.
*In interviews: callers routinely call `hasNext()` multiple times inside a while-loop condition; your iterator must tolerate this.*

**TK-2: Internal helpers must be `private` — especially state-mutating ones.**
`advance()` moves the cursor and writes `peek`; these are irreversible side effects. A `public advance()` lets any external caller corrupt the iterator's position silently. The rule: if a method is an implementation detail and no caller outside the class should invoke it, it must be `private`.
*In interviews: exposing internal mutation methods is a red flag on encapsulation understanding.*

**TK-3: The pre-fetch (`peek`) pattern is the correct way to implement a filtered iterator.**
Pre-fetch in the constructor and at the end of every `next()` call. `hasNext()` then reduces to a null-check — fast and side-effect-free. The common mistake is scanning inside `hasNext()`, which requires state flags to avoid double-advancing and is fragile.
*In interviews: "how would you add a filtered view to your iterator?" is a direct follow-up; pre-fetch is the answer.*

**TK-4: Each `iterator()` call must return a NEW instance — independent cursor.**
Two calls to `iterator()` on the same collection must produce two unrelated cursors. If you return `this` or a singleton, the second caller corrupts the first. This is why the concrete iterator is an inner class with its own `cursor` field, not a field on the collection.
*In interviews: Test 5 (two independent iterators) directly probes this; getting it wrong is a disqualifying bug.*

**TK-5: `next()` throws `NoSuchElementException`, never returns `null`.**
`null` is ambiguous — a collection may legitimately contain `null` elements. `NoSuchElementException` is the contract of `java.util.Iterator` and fails fast with a clear diagnostic. A `null` return hides bugs and pushes the crash to an unrelated site (NullPointerException somewhere downstream).
*In interviews: returning `null` here signals unfamiliarity with `java.util.Iterator`'s contract.*

**TK-6: Inner class vs static nested class — know the difference.**
`OrderHistoryIterator` is a non-static inner class: it has an implicit reference to the enclosing `OrderHistory` instance, so it can access `orders[]` and `count` directly. A `static` nested class would have no such reference and would need them passed explicitly. For iterators tied to a specific collection instance, non-static inner class is correct.
*In interviews: "why is your iterator an inner class, not a static nested class?" — the answer is the implicit outer-instance reference.*
