# 3.2.3 Decorator Pattern

## What Problem Does It Solve?

You want to add optional behaviors to objects at runtime. The naive solution is subclassing — but for N independent features you would need up to 2ᴺ subclasses to cover every combination.

The **Decorator** wraps an object in another object that implements the **same interface**, adding behavior before or after delegating to the wrapped object. Because decorators and the original object share the same interface, they can be stacked in any order to any depth.

```
Without Decorator (N=3 features → 7 subclasses)       With Decorator (3 Decorator classes, ∞ combinations)

Beverage                                               Beverage (interface)
├── MilkCoffee                                             ↑ implements + has-a
├── SugarCoffee                                        ┌────────────────────┐
├── WhipCoffee                                         │   BaseDecorator    │ wraps Beverage
├── MilkSugarCoffee                                    └────────────────────┘
├── MilkWhipCoffee                                          ↑        ↑
├── SugarWhipCoffee                                 MilkDecorator  SugarDecorator
└── MilkSugarWhipCoffee
```

---

## ASCII Structure Diagram

```
Client ──► Component (interface)
                ▲                   ▲
                │                   │
    ConcreteComponent          BaseDecorator          ← implements Component
                                                        AND has-a Component
                               - wrapped: Component   ← [the key relationship]
                               + operation()          ← delegates to wrapped
                                    ▲         ▲
                                    │         │
                            DecoratorA   DecoratorB
                            + operation()             ← calls super, adds behavior
```

**The key:** `BaseDecorator` both **implements** Component (is-a) **and** holds a Component (has-a).
The is-a lets it be stacked. The has-a lets it delegate.

---

## Code Skeleton

```java
// ── [Component] — interface all objects must implement ─────────────────────
interface TextTransformer {
    String apply(String text);
}

// ── [ConcreteComponent] — base object, no decoration ─────────────────────
class PassThroughTransformer implements TextTransformer {
    @Override
    public String apply(String text) { return text; }
}

// ── [BaseDecorator] — implements Component AND holds a Component ───────────
abstract class TextDecorator implements TextTransformer {       // [implements Component]
    private final TextTransformer wrapped;                      // [has-a Component]

    TextDecorator(TextTransformer wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public String apply(String text) {
        return wrapped.apply(text);                             // [Delegation — pure pass-through]
    }
}

// ── [ConcreteDecorator] — adds one behavior, delegates the rest ───────────
class UpperCaseDecorator extends TextDecorator {
    UpperCaseDecorator(TextTransformer wrapped) { super(wrapped); }

    @Override
    public String apply(String text) {
        return super.apply(text).toUpperCase();                 // [AddBehavior after delegation]
    }
}

// ── [ConcreteDecoratorWithState] — decorator can carry its own fields ─────
class PrefixDecorator extends TextDecorator {
    private final String prefix;                                // [OwnState]

    PrefixDecorator(TextTransformer wrapped, String prefix) {
        super(wrapped);
        this.prefix = prefix;
    }

    @Override
    public String apply(String text) {
        return prefix + super.apply(text);                      // [PrependBehavior]
    }
}
```

---

## Decorator vs Subclassing

| Dimension | Subclassing | Decorator |
|---|---|---|
| New behavior | New subclass per combination | New Decorator class per feature |
| N independent features | Up to 2ᴺ subclasses | N decorator classes |
| Timing | Fixed at compile time | Dynamic at runtime |
| SRP | Mixed concerns in subclasses | Each decorator has one responsibility |
| Own state | Yes (fields in subclass) | Yes (fields in decorator) |

---

## Comparison: Decorator vs Other Structural Patterns

| Pattern | Intent | Effect on interface |
|---|---|---|
| **Decorator** | Add behavior to an object, stack-able at runtime | Same interface as component |
| **Adapter** | Make an incompatible interface compatible | Changes the interface |
| **Proxy** | Control access to an object | Same interface as subject |
| **Facade** | Simplify a complex subsystem | New, simplified interface |

**Decorator vs Proxy:** Structurally identical — both wrap an object and implement the same interface. The difference is *intent*: Proxy controls access (lazy init, auth, remote), usually set up once. Decorator adds behavior and is designed to be stacked many times in different combinations.

---

## When to Use (and When Not To)

**Use Decorator when:**
- You need optional, combinable features on objects (logging, auth, caching, pricing tiers)
- Subclassing would cause a combinatorial explosion
- You want to compose behavior at runtime rather than compile time

**Do NOT use Decorator when:**
- The Component interface has many methods — every decorator must implement all of them
- You only need one fixed combination — a simple subclass is clearer
- The decorators are not truly independent — if feature A requires feature B, the dependency makes the open-ended stacking misleading

---

## Interview Q&A

**Q1. What is the intent of the Decorator pattern?**
To attach additional responsibilities to an object *dynamically*, as a flexible alternative to subclassing. Decorators provide a way to extend functionality by wrapping objects that implement the same interface, enabling arbitrary stacking at runtime without a combinatorial explosion of subclasses.

**Q2. Why does BaseDecorator both implement the Component interface AND hold a Component reference?**
`implements Component` makes the decorator substitutable everywhere a Component is expected (Liskov). `has-a Component` enables delegation — the decorator calls the wrapped object's method. Without both, you cannot stack decorators on top of each other: the inner object wouldn't be the right type to pass into the outer decorator's constructor.

**Q3. Does the order of decoration matter?**
Yes, when the decorators interact or transform. `TrimDecorator(UpperCaseDecorator(text))` uppercases first, then trims; reversing the order changes the result if the original string has leading/trailing content. For purely additive behavior (like cost: `+$3`, `+$5`), the order doesn't change the total but *does* change the label string.

**Q4. Can a ConcreteDecorator have its own fields?**
Yes. A `PrefixDecorator` can hold a `String prefix`; a `RetryDecorator` can hold an `int maxRetries`. This is one of Decorator's strengths over subclassing — the extra state is encapsulated in the decorator and independent of the wrapped component.

**Q5. Name real-world Java examples of the Decorator pattern.**
- `java.io` streams: `new BufferedReader(new InputStreamReader(new FileInputStream("f.txt")))` — each class wraps the previous, all implement `Reader` or `InputStream`.
- Spring Security's servlet filter chain — each `Filter` decorates the `FilterChain`.
- `javax.servlet.http.HttpServletRequestWrapper` — wraps an `HttpServletRequest` to add or override behavior.
- Logging wrappers around service classes in Spring AOP (conceptually).

**Q6. How is Decorator different from Proxy?**
Structurally identical — both wrap an object and implement the same interface. Proxy *controls access*: the proxy often manages the lifecycle of the real subject (lazy initialization, access control, remote delegation) and is usually a one-to-one relationship. Decorator *adds behavior* and is explicitly designed to be stacked multiple times in varying orders. If you find yourself creating multiple wrappers of the same type with the intent of composing features, it's a Decorator. If you're controlling who or when the subject is accessed, it's a Proxy.

---

## Common Mistakes

1. **Skipping the BaseDecorator.** Without an abstract base, every concrete decorator must re-implement every Component method just to pass through — code duplication in proportion to the interface size.

2. **Forgetting `super(wrapped)` in concrete decorator constructors.** The `wrapped` field lives in the base class and is stored only in `super(wrapped)`. Forgetting it leaves `wrapped = null`, causing NPE on the first delegation call.

3. **Making `wrapped` protected instead of `private`.** Subclasses should call `super.apply()` to delegate — they should not access `wrapped` directly. `private` enforces this and prevents subclasses from bypassing the base's logic.

4. **Returning a hard-coded value instead of delegating.** A decorator that returns `"Basic Plan + HD"` without calling `super.getLabel()` breaks stacking — when two decorators are stacked, only the outer one's label appears.

5. **Using `instanceof` to check what is wrapped.** This couples decorators to specific implementations and defeats the purpose. Decorators should be blind to what they wrap — they only know it's a `Component`.
