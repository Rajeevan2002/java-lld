# 4.3 Java 8–21 Idioms

> The APIs that replace loops, `null` checks and `instanceof` chains in LLD code. Project level: Java 21.
> Lambda / stream fundamentals from zero: [`cheatsheet/docs/README.md`](../../cheatsheet/docs/README.md) §4a. This page is the *design-level* view.

```
 loop + if + null            Stream / Optional / Comparator
 class + getters + equals    record
 abstract class + instanceof sealed interface + switch expression
```

---

## 1. Records (Java 16)

An immutable data carrier: the compiler writes private final fields, a canonical constructor, accessors (`x()`, not `getX()`), `equals`, `hashCode`, `toString`.

```java
record Point(int x, int y) {
    Point {                                   // COMPACT constructor: validate, then fields auto-assigned
        if (x < 0) throw new IllegalArgumentException("x must be >= 0");
    }
}
new Point(1, 2);            // Point[x=1, y=2]
new Point(1, 2).equals(new Point(1, 2));   // true — value equality
```

| | `class` | `record` |
|---|---|---|
| Mutable fields | yes | never |
| `equals`/`hashCode`/`toString` | write them | generated, by components |
| Extends another class | yes | no (can implement interfaces) |
| Use for | entities with identity/state (`Booking`, `Account`) | value objects, DTOs, events, results |

Rules: no extra instance fields; the compact constructor must **not** assign `this.x`. A record holding a `List` is only shallowly immutable — copy it with `List.copyOf` in the compact constructor.

---

## 2. Sealed Interfaces (Java 17)

A closed set of subtypes the compiler knows completely.

```java
sealed interface Shape permits Circle, Rect {}
record Circle(double r) implements Shape {}
record Rect(double w, double h) implements Shape {}
```

```
        Shape  (sealed)
        ├── Circle   (record → implicitly final)
        └── Rect
 permitted subtypes must be final, sealed or non-sealed
```

Why: `switch` over the type is **exhaustive** — add a new subtype and every switch without a case stops compiling. That is the safe version of the Visitor pattern for closed hierarchies.

---

## 3. Switch Expressions and Pattern Matching (Java 14 / 21)

```java
double area(Shape s) {
    return switch (s) {                            // expression: produces a value
        case Circle c -> Math.PI * c.r() * c.r();  // type pattern: c is already a Circle
        case Rect r   -> r.w() * r.h();
    };                                             // no default → compiler checks exhaustiveness
}
```

Variants you need:

```java
case Circle c when c.r() == 0 -> 0.0;                 // guard (put before the general case)
case Rect(double w, double h) -> w * h;               // record deconstruction (Java 21)
case MONDAY, TUESDAY -> 1;                            // several labels, no fall-through
```

| Old | New |
|---|---|
| `if (s instanceof Circle) { Circle c = (Circle) s; ... }` | `case Circle c ->` / `if (s instanceof Circle c)` |
| `switch` + `break` + variable assigned in every branch | `return switch ... ->` |
| `default -> throw` on a sealed type | **no `default`** — it hides missing cases |

`switch` on a `null` selector throws `NullPointerException` unless you add `case null`.

---

## 4. Optional

```
 find…()   ──▶ Optional<T>   ──▶ map / filter / orElse / orElseThrow
```

| Task | Use |
|---|---|
| wrap a maybe-null | `Optional.ofNullable(x)` (`of(null)` throws) |
| default | `orElse(v)` (always evaluates v) / `orElseGet(() -> v)` (lazy) |
| missing is an error | `orElseThrow(() -> new NoSuchElementException(...))` |
| transform | `opt.map(Parcel::getId)` |
| side effect | `ifPresent`, `ifPresentOrElse` |

Anti-patterns: `if (opt.isPresent()) opt.get()` (a null check in disguise), `Optional` as a field or parameter, `Optional<List<T>>` (return an empty list).

---

## 5. Comparator

```java
Comparator<Claim> byReview = Comparator.comparingDouble(Claim::amount).reversed()   // biggest first
                                       .thenComparing(Claim::id);                    // stable tie-break
```
- `comparingInt/Long/Double` avoid boxing. `reversed()` flips everything built **so far**.
- Define one comparator constant and reuse it for `sorted(...)`, `max/min(...)`, `PriorityQueue`.
- `max(cmp)` = the element that sorts **last**; for "largest, ties by lowest id" use `min` of the descending comparator.

---

## 6. Streams in LLD

| Need | Pipeline |
|---|---|
| filter + map to list | `list.stream().filter(...).map(...).toList()` (unmodifiable) |
| sum | `mapToDouble(...).sum()` |
| group + total | `groupingBy(key, TreeMap::new, summingDouble(f))` |
| count per key | `groupingBy(key, counting())` |
| sorted copy | `sorted(cmp).toList()` — original untouched |
| max | `stream().max(cmp)` → `Optional` |

```java
Map<String, Double> totals = claims.stream()
        .collect(Collectors.groupingBy(Main::label, TreeMap::new, Collectors.summingDouble(Main::payout)));
```
Assign collectors with `TreeMap::new` to a **typed variable or return**; inside `System.out.println(...)` inference can fail.

When not to stream: you mutate state, need an index, or the loop body has several side effects — use a `for`.

---

## 7. Interview Q&A

**Q1. Why records instead of classes with getters?** Less boilerplate, guaranteed value semantics, immutability by default; use classes when identity or mutation matters.
**Q2. What does `sealed` buy over an interface?** A closed hierarchy, so `switch` is exhaustive and new subtypes force you to update every switch.
**Q3. Why no `default` in a switch over a sealed type?** It would silence the compiler when a subtype is added.
**Q4. `orElse` vs `orElseGet`?** `orElse` evaluates its argument always; `orElseGet` only when empty — matters for expensive or side-effecting defaults.
**Q5. `Stream.toList()` vs `Collectors.toList()`?** `toList()` is unmodifiable; `Collectors.toList()` makes no promise (currently `ArrayList`).
**Q6. Why not `Optional` as a field?** It's not serializable, adds an object per field, and doesn't express anything a nullable field plus validation can't.
**Q7. `sorted()` on a list — does it modify it?** No, it returns a new stream; `Collections.sort`/`list.sort` mutates in place.

---

## 8. Common Mistakes

| Mistake | Fix |
|---|---|
| `getX()` on a record | accessor is `x()` |
| Assigning `this.x = x` in a compact constructor | omit; fields are assigned after the body |
| `default ->` on a sealed switch | remove it |
| Specific `case` after a general one | order: guarded/specific first |
| `opt.get()` without a check | `orElseThrow` |
| `comparingInt(t -> ...).reversed()` fails to compile | method ref or `(T t) ->` |
| `max(cmp)` when you meant "first by tie-break" | check which end of the order you want |
| Sorting the input list in place | `stream().sorted(cmp).toList()` |
| Collector with `TreeMap::new` inside `println` fails inference | assign to a typed variable |
| Streaming where a loop is clearer | use a loop |
