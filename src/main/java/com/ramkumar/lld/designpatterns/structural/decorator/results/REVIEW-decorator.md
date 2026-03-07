# Review — Decorator Pattern (Scenario B: Subscription Pricing)

## What You Got Right

1. **`interface Subscription` with correct method signatures** (`getLabel() → String`, `getMonthlyCost() → double`) — clean, minimal contract with no implementation details leaking in.

2. **`SubscriptionDecorator` has BOTH relationships** — it `implements Subscription` (is-a) AND holds `private final Subscription wrapped` (has-a). This dual relationship is the structural core of the pattern and you got it exactly right.

3. **`wrapped` is `private final`** — `private` forces concrete decorators to use `super.method()` rather than accessing `wrapped` directly; `final` prevents the reference from being swapped after construction.

4. **Base delegation is correct** — `SubscriptionDecorator.getLabel()` calls `wrapped.getLabel()` and `getMonthlyCost()` calls `wrapped.getMonthlyCost()`. Pure pass-through in the base is the right default.

5. **All concrete decorators call `super(wrapped)` in their constructors** — the wrapped reference is stored in the base's `private final` field; this is the only correct way to set it.

6. **`getLabel()` delegates, not hard-codes** — every concrete decorator calls `super.getLabel() + " + ..."` instead of returning a full literal like `"Basic Plan + HD"`. This is what enables stacking to arbitrary depth.

7. **`getMonthlyCost()` delegates** — all three decorators call `super.getMonthlyCost() + N.00`, not a hard-coded total.

8. **No validation in any class** — you added zero null checks, range checks, or defensive guards. Structural wrappers are transparent; you've clearly internalized this lesson from earlier reviews.

9. **All 8 test blocks passed** — Tests 1 through 8 covering BasicPlan alone, single decorators, stacked decorators, different stacking orders, and the polymorphic catalog all passed.

---

## Issues Found

**Issue 1 — Minor: Typo in constructor parameter name**

- **Severity**: Minor
- **What**: `HDUpgradeDecorator`'s constructor parameter is named `wrappped` (three p's).
- **Your code** (`practice/SubscriptionPricingPractice.java:91`):
  ```java
  public HDUpgradeDecorator(Subscription wrappped) {
      super(wrappped);
  }
  ```
- **Fix**:
  ```java
  HDUpgradeDecorator(Subscription wrapped) {
      super(wrapped);
  }
  ```
- **Why it matters**: Typos in parameter names are caught in code review and signal carelessness; in an interview setting, a reviewer notices immediately.

**Issue 2 — Minor: `public` constructor on an abstract class**

- **Severity**: Minor
- **What**: `SubscriptionDecorator`'s constructor is `public` but should be `protected`.
- **Your code** (`practice/SubscriptionPricingPractice.java:74`):
  ```java
  public SubscriptionDecorator(Subscription wrapped) {
      this.wrapped = wrapped;
  }
  ```
- **Fix**:
  ```java
  protected SubscriptionDecorator(Subscription wrapped) {
      this.wrapped = wrapped;
  }
  ```
- **Why it matters**: `public` on an abstract class constructor misleads readers — it implies the class can be constructed externally, which it cannot. `protected` correctly communicates "only subclass constructors may call `super(wrapped)`."

---

## Score Card

| Requirement | Result |
|---|---|
| `interface Subscription` with `getLabel()` and `getMonthlyCost()` | ✅ |
| `BasicPlan` implements `Subscription` | ✅ |
| `BasicPlan` returns `"Basic Plan"` and `9.99` | ✅ |
| `SubscriptionDecorator` is `abstract` | ✅ |
| `wrapped` field is `private final` | ✅ |
| `SubscriptionDecorator` constructor is `protected` | ❌ (was `public`) |
| Each concrete decorator calls `super(wrapped)` | ✅ |
| `getLabel()` delegates via `super.getLabel()` (not hard-coded) | ✅ |
| `getMonthlyCost()` delegates via `super.getMonthlyCost()` | ✅ |
| `HDUpgradeDecorator`: appends `" + HD"`, adds `$3.00` | ✅ |
| `FamilySharingDecorator`: appends `" + Family Sharing"`, adds `$5.00` | ✅ |
| `DownloadsDecorator`: appends `" + Offline Downloads"`, adds `$2.00` | ✅ |
| No subclass per combination | ✅ |
| No validation in decorators | ✅ |
| Tests 1–8 all passed | ✅ |

---

## Key Takeaways — Do Not Miss These

1. **TK-1: BaseDecorator must both implement AND hold the Component interface**
   The is-a relationship (implements) lets the decorator be used anywhere the component is expected; the has-a relationship (private final field) lets it delegate. Without both, stacking is impossible.
   *In interviews: draw this dual arrow explicitly on the class diagram — interviewers check whether you know why both are required.*

2. **TK-2: Always delegate with `super.method()`, never hard-code the full result**
   `super.getLabel() + " + HD"` works when wrapped inside another decorator; `"Basic Plan + HD"` breaks because the outer decorator sees only the inner hard-coded string, not the full chain.
   *In interviews: this is the single most common Decorator mistake — expect a follow-up question asking you to stack the same decorator twice.*

3. **TK-3: `wrapped` must be `private` (not `protected`) in the base**
   `private` forces concrete decorators to use `super.method()` for delegation — they cannot accidentally access or reassign `wrapped`. `protected` would be a temptation to bypass the base logic.
   *In interviews: if asked "why private?", the answer is "encapsulation — subclasses interact through the interface, not the field."*

4. **TK-4: Abstract class constructors should be `protected`**
   `protected` signals "for subclass use only"; `public` on an abstract class is misleading because the class cannot be instantiated directly regardless.
   *In interviews: access modifiers on abstract class constructors are a common Java detail check.*

5. **TK-5: Structural wrappers (Decorator, Adapter, Proxy) do not validate their inputs**
   Decorators are transparent — they add behavior, not defensiveness. Validation belongs in the layer that constructs the chain (e.g., a factory or service).
   *In interviews: adding null checks to a decorator's constructor is a red flag that the candidate is confusing responsibilities.*

---

## Reference Solution

See `DecoratorReference.java` in this directory.

Extra Test 9 catches the most common mistake: applying the same decorator twice (`HDUpgradeDecorator(HDUpgradeDecorator(BasicPlan))`) must produce `"Basic Plan + HD + HD"` and `$15.99`. A hard-coded label like `"Basic Plan + HD"` in the decorator body would produce the wrong result.
