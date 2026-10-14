# 4.4 Enums as Design Tools — Fixed Sets, Behaviour, State Machines

> Goal: replace `String`/`int` constants and `if/else` chains with enums that carry data, carry behaviour, and encode legal state transitions.

---

## 1. What an enum really is

An enum is a class with a **fixed, closed set of singleton instances**, created once at class-load time.

```
 enum Size { SMALL, MEDIUM, LARGE }

 compiles to roughly:
   final class Size extends java.lang.Enum<Size> {
       public static final Size SMALL  = new Size("SMALL", 0);
       public static final Size MEDIUM = new Size("MEDIUM", 1);
       public static final Size LARGE  = new Size("LARGE", 2);
       private static final Size[] $VALUES = {SMALL, MEDIUM, LARGE};
   }
```

Consequences:
- `==` is correct and safe for comparison (one instance per constant). `equals` is the same as `==`.
- The constructor is implicitly `private`; nobody else can create instances.
- Enums are `Comparable` (by declaration order), `Serializable`, and safe in `switch`.
- `values()` returns a **new array every call** (defensive copy) — cache it if used in a hot loop.

| Replace this | With this | Gain |
|---|---|---|
| `static final String STATUS_PAID = "PAID"` | `enum Status { PAID }` | compile-time typo safety |
| `int type = 2` | `enum Type` | no magic numbers, no out-of-range |
| `if (type == A) … else if (type == B) …` | method on the enum | Open/Closed: new constant = new body |
| `Map<String, X>` keyed by a fixed set | `EnumMap<E, X>` | array-backed, ordered, faster |

---

## 2. Fields, constructor, methods

```java
enum Size {
    SMALL(250, 0.0),            // each constant calls the constructor
    MEDIUM(350, 0.5),
    LARGE(500, 1.0);            // <-- semicolon required once there is a body

    private final int ml;       // fields are final: constants are shared singletons
    private final double surcharge;

    Size(int ml, double surcharge) {   // implicitly private
        this.ml = ml;
        this.surcharge = surcharge;
    }
    int ml() { return ml; }
    double surcharge() { return surcharge; }
}
```

Never use a **mutable** field in an enum — it is global shared state.

---

## 3. Constant-specific behaviour (strategy without a strategy class)

```
            abstract double apply(double subtotal)
                         │
     ┌───────────────────┼────────────────────┐
   NONE               FLAT50              PERCENT10
 {return sub;}   {return max(0, sub-50);}  {return sub*0.9;}
```

```java
enum Coupon {
    NONE    { double apply(double s) { return s; } },
    FLAT50  { double apply(double s) { return Math.max(0, s - 50); } },
    PERCENT10 { double apply(double s) { return s * 0.9; } };

    abstract double apply(double subtotal);   // compiler forces every constant to implement it
}
```

Forget a body for a new constant → **compile error**. This is the enum answer to "add a new type without touching an if-chain".

| Approach | New constant forces you to… | Compile-checked? |
|---|---|---|
| `switch` outside the enum, with `default` | remember to edit the switch | ❌ silently falls to `default` |
| `switch` expression outside, no `default` | edit the switch | ✅ non-exhaustive = error |
| Abstract method inside the enum | write a body | ✅ |

Rule of thumb: behaviour that belongs to the concept → inside the enum. Behaviour specific to one caller → switch expression at the caller.

---

## 4. Enums as state machines

Constructor arguments cannot reference constants declared *later* (illegal forward reference). So transitions are wired in a static block, or with a `switch` method.

```
  PLACED ──▶ PREPARING ──▶ OUT_FOR_DELIVERY ──▶ DELIVERED
     │            │
     └────────────┴──────▶ CANCELLED
```

```java
enum Status {
    PLACED, PREPARING, OUT_FOR_DELIVERY, DELIVERED, CANCELLED;

    private static final Map<Status, Set<Status>> NEXT = new EnumMap<>(Status.class);
    static {
        NEXT.put(PLACED,           EnumSet.of(PREPARING, CANCELLED));
        NEXT.put(PREPARING,        EnumSet.of(OUT_FOR_DELIVERY, CANCELLED));
        NEXT.put(OUT_FOR_DELIVERY, EnumSet.of(DELIVERED));
        NEXT.put(DELIVERED,        EnumSet.noneOf(Status.class));
        NEXT.put(CANCELLED,        EnumSet.noneOf(Status.class));
    }
    boolean canMoveTo(Status t) { return NEXT.get(this).contains(t); }
    boolean isTerminal()        { return NEXT.get(this).isEmpty(); }
}
```

Alternative with an exhaustive switch expression (no `default`):

```java
boolean isTerminal() {
    return switch (this) {
        case DELIVERED, CANCELLED -> true;
        case PLACED, PREPARING, OUT_FOR_DELIVERY -> false;
    };
}
```

The transition rule lives **in the enum**, so the order class is just `if (!status.canMoveTo(t)) throw …`.

---

## 5. `EnumMap` and `EnumSet`

| | `EnumMap<K, V>` | `EnumSet<E>` |
|---|---|---|
| Backing | array indexed by `ordinal()` | bit vector (`long`) |
| Iteration order | declaration order | declaration order |
| `null` keys | not allowed | n/a |
| vs `HashMap`/`HashSet` | faster, smaller, deterministic output | same |

```java
Map<Status, Integer> counts = new EnumMap<>(Status.class);
for (Status s : Status.values()) counts.put(s, 0);        // zero-fill so every key is present
counts.merge(Status.PLACED, 1, Integer::sum);

EnumSet<Status> open  = EnumSet.complementOf(EnumSet.of(Status.DELIVERED, Status.CANCELLED));
EnumSet<Status> range = EnumSet.range(Status.PLACED, Status.OUT_FOR_DELIVERY);
EnumSet<Status> none  = EnumSet.noneOf(Status.class);
```

Printing an `EnumMap` gives `{PLACED=1, PREPARING=0, …}` — always declaration order, so output is deterministic.

---

## 6. Lookup by code (`valueOf` is not enough)

`Enum.valueOf("x")` is case-sensitive and throws `IllegalArgumentException: No enum constant …`. For friendly input, build a map once:

```java
private static final Map<String, Size> BY_CODE = new HashMap<>();
static {
    for (Size s : values()) BY_CODE.put(s.name().toLowerCase(), s);
}
static Size fromCode(String code) {
    Size s = (code == null) ? null : BY_CODE.get(code.toLowerCase());
    if (s == null) throw new IllegalArgumentException("unknown size: " + code);
    return s;
}
```

Static fields of an enum are initialised **after** the constants, which is why the static block can call `values()`. (An enum *constructor* cannot touch a static field — it runs before they exist.)

---

## 7. Enums + switch expressions + interfaces

```java
interface Priced { double price(); }
enum Item implements Priced { ... }          // enums may implement interfaces, never extend a class
```

```java
String label = switch (status) {             // exhaustive, no default -> new constant = compile error
    case PLACED -> "Waiting";
    case PREPARING -> "Cooking";
    case OUT_FOR_DELIVERY -> "On the way";
    case DELIVERED, CANCELLED -> "Closed";
};
```

---

## 8. Interview Q&A

**Q1. Why is `==` fine for enums but not for `String`?**
Each constant is a singleton, so reference equality equals logical equality. It is also null-safe on the left side and checked at compile time for type mismatch.

**Q2. Can an enum extend a class? implement an interface?**
No to extends (it already extends `java.lang.Enum`). Yes to implements.

**Q3. How would you remove a big `switch(type)` from a pricing method?**
Put an abstract method on the enum and give each constant its own body. Adding a constant then forces an implementation.

**Q4. `EnumMap` vs `HashMap`?**
`EnumMap` is array-backed, iterates in declaration order, no hashing; use it whenever the key is an enum.

**Q5. Why can't an enum constructor read a static field?**
Constants are initialised first, then static fields. Use a static block (runs after the constants) or a nested holder.

**Q6. Why avoid `ordinal()`?**
It changes when someone reorders or inserts a constant, and it breaks anything persisted. Use an explicit field (`code`, `rank`) instead.

**Q7. How do you model a state machine with enums?**
Constants are the states; the enum owns `canMoveTo` / `isTerminal` (from an `EnumMap<State, EnumSet<State>>` built in a static block, or a switch). The entity calls `state.canMoveTo(next)` and throws `IllegalStateException` otherwise.

---

## 9. Common Mistakes

| Mistake | Fix |
|---|---|
| Storing the status as a `String`/`int` | enum field |
| `switch` on an enum with `default ->` | list every constant, no `default`; compiler flags new ones |
| Using `ordinal()` as data or for persistence | explicit field |
| Mutable instance fields in an enum | `final` fields only |
| `HashMap<MyEnum, …>` for a fixed key set | `EnumMap` |
| `valueOf(input)` on user input | custom `fromCode` with a clear message |
| Constructor referencing a later constant or a static field | static block / `switch` |
| Returning `values()` repeatedly in a loop | cache once (`private static final X[] ALL = values()`) |
| Printing an `EnumMap` after only inserting seen keys (missing zeros) | zero-fill with `values()` first |
| Transition rules scattered in the order class | put them in the enum |
