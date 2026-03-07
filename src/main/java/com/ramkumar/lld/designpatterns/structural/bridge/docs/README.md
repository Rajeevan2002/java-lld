# 3.2.6 Bridge Pattern

## What Problem Does It Solve?

You have two orthogonal dimensions of variation. Without Bridge, combining them via
inheritance produces a combinatorial explosion:

```
Shape × Color (N shapes, M colors → N×M subclasses)

RedCircle    BlueCircle    GreenCircle
RedSquare    BlueSquare    GreenSquare
RedTriangle  BlueTriangle  GreenTriangle
```

Every time you add a color you must add N subclasses. Every time you add a shape you
must add M subclasses.

**Bridge** separates the two dimensions into two independent class hierarchies connected
by a reference (the "bridge"). N + M classes cover all combinations.

```
  Abstraction                Implementor
  ───────────                ───────────
  Shape                      Color (interface)
  ├── Circle        ─────►   ├── RedColor
  ├── Square                 ├── BlueColor
  └── Triangle               └── GreenColor
```

---

## Core Structure

```
Client ──► Abstraction (abstract class)
               - impl: Implementor          ← [the bridge]
               + operation()
                    │
           ┌────────┴────────┐
    RefinedAbstractionA   RefinedAbstractionB
                                    Implementor (interface)
                                    + operationImpl()
                                         │
                               ┌─────────┴──────────┐
                         ConcreteImplX         ConcreteImplY
```

**Two independent hierarchies:**
- Abstraction side: defines *what* the high-level operation does
- Implementor side: defines *how* the platform-specific work is done

The `impl` reference in Abstraction is the bridge between them.

---

## Code Skeleton

```java
// ── [Implementor] — the platform-specific interface ──────────────────────────
interface Renderer {
    void render(String shape, String color);
}

// ── [ConcreteImplementors] — different platforms/backends ────────────────────
class VectorRenderer implements Renderer {
    @Override public void render(String shape, String color) {
        System.out.println("[Vector] Drawing " + shape + " in " + color);
    }
}

class RasterRenderer implements Renderer {
    @Override public void render(String shape, String color) {
        System.out.println("[Raster] Drawing " + shape + " filled with " + color);
    }
}

// ── [Abstraction] — the high-level abstraction; holds the bridge reference ───
abstract class Shape {
    protected final Renderer renderer;   // [the bridge — Implementor reference]

    Shape(Renderer renderer) {
        this.renderer = renderer;        // injected at construction — swappable
    }

    abstract void draw();                // each RefinedAbstraction implements this
}

// ── [RefinedAbstractions] — extend Abstraction; add their own behaviour ───────
class Circle extends Shape {
    private final String color;

    Circle(Renderer renderer, String color) {
        super(renderer);                 // [ChainToAbstraction] passes bridge upward
        this.color = color;
    }

    @Override
    public void draw() {
        renderer.render("Circle", color);  // [DelegatesToImpl] calls the bridge
    }
}

class Square extends Shape {
    private final String color;

    Square(Renderer renderer, String color) {
        super(renderer);
        this.color = color;
    }

    @Override
    public void draw() {
        renderer.render("Square", color);
    }
}
```

Usage — all 4 combinations, 0 new classes:

```java
Shape c1 = new Circle(new VectorRenderer(), "Red");    // Vector + Circle
Shape c2 = new Circle(new RasterRenderer(), "Blue");   // Raster + Circle
Shape s1 = new Square(new VectorRenderer(), "Green");  // Vector + Square
Shape s2 = new Square(new RasterRenderer(), "Red");    // Raster + Square
```

---

## Bridge vs Related Patterns

| Dimension | Bridge | Adapter | Decorator |
|---|---|---|---|
| **Intent** | Decouple abstraction from implementation; both vary independently | Make an incompatible interface compatible | Add stackable behaviour at runtime |
| **Designed upfront?** | Yes — designed to allow two hierarchies to grow independently | No — retrofitted to connect existing code | Yes — designed for stacking |
| **Reference type** | Abstraction holds Implementor | Adapter holds Adaptee | Decorator holds Component (same interface) |
| **Interface match** | Abstraction ≠ Implementor interface | Target interface ≠ Adaptee interface | Decorator interface = Component interface |
| **Typical use** | Shape × Renderer, Device × Remote, Alert × Channel | Legacy API wrapping | Logging, auth, caching wrappers |

---

## When to Use (and When Not To)

**Use Bridge when:**
- You want to vary both abstraction and implementation independently.
- Subclassing would produce an N×M matrix of classes.
- You want to switch the implementation at runtime (inject a different Implementor).
- You want to hide implementation details from clients entirely.

**Do NOT use Bridge when:**
- Only one dimension varies — a single abstract class or interface is enough.
- The abstraction and implementation are tightly coupled and will never vary independently.
- The added complexity of two hierarchies outweighs the benefit for a small fixed set.

---

## Interview Q&A

**Q1. What is the Bridge pattern? State the four participants.**
Bridge decouples an abstraction from its implementation so the two can vary
independently. Participants: Abstraction (holds the bridge reference), RefinedAbstraction
(extends Abstraction), Implementor (the platform interface), ConcreteImplementor
(implements the platform interface).

**Q2. How does Bridge differ from Adapter? Both use composition.**
Intent and timing. Adapter is a retrofit — it wraps an *existing incompatible* interface
to make it work with new code. Bridge is designed *upfront* — it intentionally separates
two dimensions into independent hierarchies so both can grow without a class explosion.
Adapter resolves incompatibility; Bridge prevents subclass combinatorics.

**Q3. What is "the bridge" in the Bridge pattern?**
The `protected final Implementor impl` field inside Abstraction. It is the composition
link — the reference that connects the high-level abstraction hierarchy to the
platform-specific implementation hierarchy. Calling `impl.operationImpl()` inside
Abstraction's methods is the act of "crossing the bridge."

**Q4. Why is the Implementor interface often different from the Abstraction interface?**
The Implementor defines primitive, platform-level operations (e.g., `drawLine()`);
the Abstraction defines higher-level operations (e.g., `drawCircle()`). The Abstraction
composes its operations from one or more Implementor calls. They are different levels of
abstraction, so they naturally have different method signatures.

**Q5. How does Bridge allow runtime switching of implementations?**
Because the Implementor reference inside Abstraction is accessed through an interface
(not a concrete type), you can pass a different ConcreteImplementor at construction
time — or expose a setter `setImpl(Implementor i)` if you need to swap at runtime.
The Abstraction logic is identical either way.

**Q6. Name a real-world Java example of Bridge.**
`java.sql.DriverManager` + JDBC drivers: `Connection` is the Abstraction; JDBC `Driver`
(MySQL, PostgreSQL, H2) is the Implementor. Application code uses `Connection` methods;
the concrete SQL is generated by the injected driver. Swap the driver without changing
any application code.

---

## Common Mistakes

1. **Confusing Bridge with Adapter.** If you are retrofitting an existing class, it's
   an Adapter. If you are designing two independent hierarchies from scratch, it's Bridge.

2. **Using inheritance instead of composition for the Implementor.**
   `class SmartRemote extends TV` is not Bridge — it couples the remote permanently to
   the TV. The bridge reference `Device device` is what allows runtime substitution.

3. **Putting the Implementor's interface on Abstraction.**
   Abstraction and Implementor should have *different* interfaces. If Abstraction mirrors
   Implementor method-for-method, there is no value-add from the abstraction layer.

4. **Forgetting `super(impl)` in RefinedAbstraction constructors.**
   The bridge reference lives in Abstraction; it must be set by chaining to
   `super(impl)`. Skipping this leaves the `impl` field null.

5. **Making the `impl` field `private` in Abstraction when subclasses need direct access.**
   Use `protected final` so RefinedAbstractions can call `impl.operationImpl()` directly.
   If private, add a `protected Implementor getImpl()` accessor.
