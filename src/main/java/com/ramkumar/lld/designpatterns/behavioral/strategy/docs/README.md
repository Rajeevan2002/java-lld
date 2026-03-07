# 3.3.1 Strategy Pattern

## What Problem Does It Solve?

You have a class that needs to perform some behaviour that can vary independently — and you
want to swap that behaviour at runtime without modifying the class itself.

Without Strategy you end up with large `if/else` or `switch` chains inside the context
class. Every new variant requires editing the class — violating OCP.

```
Without Strategy — Order with switch:
  checkout() {
      if (tier == STANDARD)   cost = weight * 3.5;
      else if (tier == EXPRESS) cost = 10 + weight * 7;
      else if (tier == OVERNIGHT) ...   // grows forever
  }

With Strategy — Order delegates:
  checkout() {
      cost = strategy.calculate(weight, declaredValue);  // one line, forever
  }
```

---

## Core Structure

```
          «interface»
         ShippingStrategy
        + calculate(w, v) → double
               ▲
    ┌──────────┼──────────┐
    │          │          │
 Standard   Express   Overnight
 Shipping   Shipping   Shipping
 (concrete) (concrete) (concrete)

          Order  ──────────────► ShippingStrategy
         (Context)   has-a
         - strategy              setStrategy(s)
         + checkout()            strategy.calculate(...)
```

The key relationships:
- **Context** (`Order`) holds a reference to the **Strategy interface** — NOT a concrete class.
- **ConcreteStrategies** implement the interface independently; they never know about `Order`.
- The context delegates the varying behaviour to its strategy; it handles everything else.

---

## Intrinsic vs Injected State

| | Strategy | Context |
|---|---|---|
| **Owns** | Algorithm-specific config (rate, fee, multiplier) | The data the algorithm operates on (weight, value) |
| **Mutability** | Usually immutable after construction | Context fields are final; strategy field is mutable |
| **Knows about** | Only the method parameters | Only the Strategy interface |

---

## Code Skeleton

```java
// ── Strategy interface ────────────────────────────────────────────────────────
interface ShippingStrategy {
    double calculate(double weightKg, double declaredValue);
}

// ── Concrete Strategy ─────────────────────────────────────────────────────────
class StandardShipping implements ShippingStrategy {
    private final double ratePerKg = 3.50;

    @Override
    public double calculate(double weightKg, double declaredValue) {
        double cost = weightKg * ratePerKg;
        return Math.max(cost, 5.00);   // minimum charge
    }
}

// ── Context ───────────────────────────────────────────────────────────────────
class Order {
    private final String orderId;
    private final double weightKg;
    private ShippingStrategy strategy;   // mutable — can be swapped

    Order(String orderId, double weightKg, ShippingStrategy strategy) {
        this.orderId  = orderId;
        this.weightKg = weightKg;
        this.strategy = strategy;
    }

    void setStrategy(ShippingStrategy strategy) {
        this.strategy = strategy;   // swap at runtime
    }

    double checkout() {
        return strategy.calculate(weightKg, 0);   // delegates entirely
    }
}
```

Usage:

```java
Order o = new Order("ORD-1", 2.5, new StandardShipping());
o.checkout();           // → Standard rate

o.setStrategy(new ExpressShipping());
o.checkout();           // → Express rate, same order, zero changes to Order class
```

---

## Strategy vs Other Patterns

| Dimension | Strategy | Template Method | State |
|---|---|---|---|
| **Varies** | Whole algorithm | Steps of an algorithm | Object behaviour per state |
| **Mechanism** | Composition (interface) | Inheritance (abstract class) | Composition (interface) |
| **Runtime swap** | Yes — `setStrategy()` | No — subclass is fixed at construction | Yes — triggered by transitions |
| **Knows context** | No | Yes (it IS the context) | Often yes |
| **OCP** | New strategy = new class | New variant = new subclass | New state = new class |

---

## Interview Q&A

**Q1. What is the Strategy pattern? State the intent in one sentence.**
Strategy defines a family of algorithms, encapsulates each one, and makes them
interchangeable — allowing the algorithm to vary independently from the clients that use it.

**Q2. How does Strategy differ from a plain `if/else` chain?**
An `if/else` chain puts all variant logic inside the context class. Adding a new variant
means editing that class (violates OCP). With Strategy, a new variant is a new class that
implements the interface — the context never changes.

**Q3. Why does the context hold a reference to the interface, not a concrete class?**
Holding the interface reference decouples the context from every concrete strategy.
The context can work with any strategy — including ones written after the context was
compiled — without recompilation. This is the Open/Closed Principle in practice.

**Q4. When should you use Strategy vs Template Method?**
Use Strategy when you want to swap the whole algorithm at runtime via composition.
Use Template Method when the algorithm skeleton is fixed but specific steps vary — the
variation is achieved by subclassing, not by injection.

**Q5. Can a Strategy hold its own state?**
Yes. A strategy may have fields (e.g., `ratePerKg`, `baseFee`) that configure the
algorithm. These are set at construction time and are typically immutable. What the
strategy does NOT hold is the context's data (weight, order ID) — those are passed in
as method parameters.

**Q6. What is the difference between Strategy and Decorator?**
Both wrap behaviour behind an interface. Strategy *replaces* the whole algorithm —
only one strategy is active at a time. Decorator *layers* behaviour on top of an existing
implementation — multiple decorators can be stacked simultaneously.

**Q7. Name a real-world Java example of Strategy.**
`java.util.Comparator<T>` — a `Comparator` is a strategy for comparing two objects.
`Collections.sort(list, comparator)` is the context; you inject whichever comparison
algorithm you need. `Comparator.comparing(...)` creates a strategy inline.

---

## Common Mistakes

1. **Context holds a concrete type, not the interface.**
   `private StandardShipping strategy` — this breaks polymorphism. `setStrategy()` can
   only accept `StandardShipping`; you can never inject `ExpressShipping`. Always type the
   field as the interface.

2. **Strategy reaches back into the context.**
   A strategy that calls `order.getOrderId()` creates bidirectional coupling. The strategy
   should work purely on what is passed to its method — no back-references.

3. **Putting business logic in the context instead of the strategy.**
   `checkout() { cost = strategy.calculate(weight, value); if (cost > 100) applyDiscount(); }`
   — the discount rule belongs in a strategy variant, not hardcoded in the context.

4. **Using `instanceof` or `getClass()` inside the context.**
   `if (strategy instanceof ExpressShipping) { ... }` defeats the purpose of the interface.
   The context must remain oblivious to which concrete strategy it holds.

5. **Making strategy state mutable.**
   If a strategy modifies its own fields during `calculate()`, it becomes stateful and
   non-reusable. Strategies should be side-effect-free with respect to their own fields.
