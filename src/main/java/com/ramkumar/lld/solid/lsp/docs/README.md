# L — Liskov Substitution Principle (LSP)

> "If S is a subtype of T, then objects of type T may be replaced with objects of type S
> without altering any of the desirable properties of the program."
> — Barbara Liskov (1987)

Simplified: **Every subclass must be substitutable for its parent class — without breaking the caller.**

---

## 1. The Substitutability Test

Ask this question about every subclass you write:

> "If I hand a `Square` to code that expects a `Rectangle`, will it still work correctly?"

If the answer is no, LSP is violated.

```
Code that uses a supertype                 Subtype passed in at runtime
┌─────────────────────────────┐            ┌──────────────────────────────┐
│ void resize(Rectangle r) {  │            │ Square  (claims to be a      │
│   r.setWidth(5);            │◄───────────│  Rectangle)                  │
│   r.setHeight(3);           │            │                              │
│   assert r.area() == 15;    │            │ setWidth(5) → also sets h=5  │
│ }                           │            │ setHeight(3) → also sets w=3 │
└─────────────────────────────┘            │ area() = 9, NOT 15 ← BREAKS │
                                           └──────────────────────────────┘
                                           LSP VIOLATED — Square is not
                                           substitutable for Rectangle
```

---

## 2. The Three LSP Rules

### Rule 1: Precondition Rule (Subtypes cannot strengthen preconditions)

The subtype must accept every input the supertype accepts.

```java
// Supertype contract: process(double amount) — accepts any positive amount
abstract class Payment {
    abstract Receipt process(double amount);   // works for amount=0.50 upwards
}

// ❌ VIOLATION — strengthened precondition: only accepts amounts >= 100
class BulkPayment extends Payment {
    @Override
    Receipt process(double amount) {
        if (amount < 100)
            throw new IllegalArgumentException("BulkPayment requires >= 100");
        // ...
    }
}
// Code that calls process(50.0) on a Payment reference now unexpectedly throws.
```

### Rule 2: Postcondition Rule (Subtypes cannot weaken postconditions)

The subtype must deliver at least what the supertype promises.

```java
// Supertype contract: getDiscount() returns a value in [0.0, 1.0]
abstract class Discount {
    abstract double getDiscount();   // guarantee: 0.0 ≤ result ≤ 1.0
}

// ❌ VIOLATION — weakened postcondition: can return > 1.0 (more than 100% off)
class MegaDiscount extends Discount {
    @Override
    double getDiscount() { return 1.50; }  // breaks the 0..1 contract
}
```

### Rule 3: Invariant Rule (Subtypes must preserve parent invariants)

Invariants are properties that always hold for objects of that type.

```java
// Rectangle invariant: width and height are independent
class Rectangle {
    protected int width, height;
    void setWidth(int w)  { this.width  = w; }          // ONLY changes width
    void setHeight(int h) { this.height = h; }          // ONLY changes height
    int  area()           { return width * height; }
}

// ❌ VIOLATION — Square breaks the "width and height are independent" invariant
class Square extends Rectangle {
    @Override
    void setWidth(int w)  { width = w; height = w; }   // ← changes BOTH
    @Override
    void setHeight(int h) { height = h; width = h; }   // ← changes BOTH
}
```

---

## 3. Before → After ASCII Diagram (Rectangle / Square)

```
BEFORE — LSP Violation
══════════════════════
        Rectangle
       (mutable w, h)
            │
            └── Square (overrides setWidth/setHeight to keep w==h)

Problem: void resize(Rectangle r) expects w and h to be independent.
         When r is actually a Square, the assertion fails.
         Square IS-A Rectangle in Java — but NOT substitutable for Rectangle.

AFTER — LSP Compliant
═════════════════════
         Shape (interface / abstract)
        /          \
  Rectangle       Square
  (w, h final)   (side final)
  implements      implements

Both are Shapes. Neither IS-A the other. No mutation issue.
resize() takes a Shape and calls area() — both work identically.
```

---

## 4. IS-A vs IS-SUBSTITUTABLE-FOR

The critical distinction LSP introduces:

| | IS-A (Java `extends`) | IS-SUBSTITUTABLE-FOR (LSP) |
|---|---|---|
| **What it checks** | Syntax: "can I pass a Square where Rectangle is expected?" | Behaviour: "will the code still work correctly?" |
| **Rectangle / Square** | Square IS-A Rectangle ✅ (in Java) | Square IS NOT SUBSTITUTABLE for mutable Rectangle ❌ |
| **Penguin / Bird** | Penguin IS-A Bird ✅ | Penguin IS NOT SUBSTITUTABLE for Bird if Bird has `fly()` ❌ |

**The key insight**: In Java, `extends` gives you IS-A for free. LSP requires you to manually verify IS-SUBSTITUTABLE-FOR.

> "A Square IS-A Rectangle (in Java) but a Square is NOT a Rectangle (in maths/LSP)."

---

## 5. Classic LSP Violations

| Violation | Symptom | Fix |
|---|---|---|
| `Square extends Rectangle` | `setWidth()` also sets height → invariant broken | Make both independent `Shape` implementations |
| `Penguin extends Bird` where `Bird.fly()` exists | `Penguin.fly()` throws `UnsupportedOperationException` | Segregate: `FlyingBird` / `NonFlyingBird` |
| `Stack extends Vector` (Java SDK mistake) | `Vector.remove(index)` breaks LIFO invariant | Prefer composition over inheritance |
| `ReadOnlyList extends ArrayList` | `add()` throws `UnsupportedOperationException` | Expose only `List<T>` (read-only interface) |
| `FreeSubscription extends Subscription` where `getMaxDevices()>0` | returns 0, breaking positive-count contract | Enforce in abstract class constructor |

---

## 6. How Java Enforces (Some) LSP

```java
// Java gives you two LSP enforcement tools:

// 1. final methods — subclasses CANNOT override invariant-enforcing methods
abstract class Notification {
    private final int maxMessageLength;
    // final → subclass can't override to return 0 (broken invariant)
    public final int getMaxMessageLength() { return maxMessageLength; }
}

// 2. final fields — subclasses CANNOT mutate the invariant
class Rectangle {
    private final int width;   // ← final: can't be changed after construction
    private final int height;  // ← final: independent from width
    // Now setWidth() can't exist → LSP violation is impossible
    public int getWidth()  { return width; }
    public int getHeight() { return height; }
}
```

The lesson: **prefer immutability and `final` methods to enforce LSP structurally** rather than relying on documentation.

---

## 7. LSP and Other SOLID Principles

| Principle | Relationship to LSP |
|---|---|
| **SRP** | God classes have many reasons to change — making robust hierarchies harder. SRP helps isolate substitutable behaviours. |
| **OCP** | OCP says "extend without modification." LSP says "extensions must be substitutable." They work together: new subtypes satisfy both when contracts are honoured. |
| **ISP** | Many LSP violations (`Penguin.fly()` throws) are better fixed with ISP — split the fat interface into `FlyingBird` / `Bird`. |
| **DIP** | Depending on abstractions means you're relying on subtype substitutability — if LSP is violated, DIP breaks too. |

---

## 8. Interview Questions

**Q1: What is the Liskov Substitution Principle in plain terms?**
> If you have code that works with a supertype, it should also work correctly when you substitute any of its subtypes — without the caller needing to know which subtype it got. The behaviour must be compatible, not just the Java types.

**Q2: Why does `Square extends Rectangle` violate LSP?**
> Rectangle has an invariant: width and height are independent — setting one doesn't change the other. Square breaks this: `setWidth(5)` also sets height to 5. Code that calls `setWidth(5); setHeight(3)` and expects `area() == 15` fails when given a Square. The behavioural contract is broken even though the Java types are compatible.

**Q3: How do you fix the Rectangle/Square violation?**
> Remove the inheritance relationship. Create a `Shape` interface/abstract class with `area()` and `perimeter()`. Both `Rectangle` and `Square` implement `Shape` independently. If they need immutable construction, use `final` fields. Neither `extends` the other — they're sibling implementations of the same abstraction.

**Q4: What is the difference between strengthening a precondition and weakening a postcondition?**
> A precondition is what the method requires of its caller (input constraints). Strengthening it means rejecting inputs the parent accepted — breaking callers who passed valid inputs to the parent. A postcondition is what the method promises to deliver (output guarantees). Weakening it means delivering less than the parent promised — breaking callers who relied on those guarantees.

**Q5: How can `final` methods help enforce LSP?**
> If a method enforces an invariant (e.g., `getMaxMessageLength()` must return > 0), declaring it `final` prevents subclasses from overriding it to return invalid values. The invariant is then structurally enforced by the compiler — no subclass can break it regardless of the programmer's intent.

**Q6: When is it acceptable to throw `UnsupportedOperationException` in a subclass?**
> Almost never in an LSP-compliant hierarchy. If `Collections.unmodifiableList()` returns a list that throws on `add()`, it does so via the `List` interface (which doesn't guarantee `add()` succeeds — the javadoc says it's optional). The key is the interface contract: if the supertype's contract guarantees a method works, the subtype must honour that guarantee. `UnsupportedOperationException` is only acceptable when the parent's contract explicitly calls the method "optional."

**Q7: How do you test for LSP violations in code review?**
> Look for: (1) `UnsupportedOperationException` or `NotImplementedException` in override methods, (2) `instanceof` checks before calling subtype methods, (3) overrides that throw for inputs the parent accepted, (4) overrides that return values outside the parent's promised range, (5) mutable state in a subclass that changes inherited field semantics.

---

## 9. Common Mistakes

```java
// MISTAKE 1: Using UnsupportedOperationException to "skip" a method
class ReadOnlyStack extends Stack<Integer> {
    @Override
    public Integer remove(int index) {
        throw new UnsupportedOperationException("ReadOnlyStack is immutable");
        // ← Any code holding a Stack<Integer> reference now unexpectedly throws
    }
}
// Fix: Don't extend Stack. Use composition or implement a read-only interface.

// MISTAKE 2: Checking instanceof to decide what to call
void processPayment(Payment p) {
    if (p instanceof BulkPayment) {                       // ← LSP violation signal
        ((BulkPayment) p).processWithMinimum(100.0);
    } else {
        p.process(amount);
    }
}
// If you need instanceof, your hierarchy is not substitutable. Fix the hierarchy.

// MISTAKE 3: Strengthening precondition silently (no exception, just different behaviour)
class PositiveOnlyCounter extends Counter {
    @Override
    public void increment(int by) {
        if (by < 0) return;   // ← silently ignores negative increments
        super.increment(by);  //   parent accepted and processed negatives; subclass doesn't
    }
}
// Callers who pass -1 to "undo" an increment now see no effect — silent misbehaviour.

// MISTAKE 4: Inheriting from a concrete class with mutable state
class SpecialList extends ArrayList<String> {
    // Inheriting ArrayList's mutable state makes it very hard to guarantee
    // that SpecialList's invariants hold — ArrayList can add/remove anything.
    // Prefer composition: have a List<String> field instead.
}

// MISTAKE 5: Confusing "IS-A" (Java) with "IS-SUBSTITUTABLE-FOR" (LSP)
// Square IS-A Rectangle in Java — you can write: Rectangle r = new Square();
// But Square IS NOT SUBSTITUTABLE for a mutable Rectangle.
// Always verify behavioural compatibility, not just type compatibility.
```
