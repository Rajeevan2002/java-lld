# 3.2.7 Flyweight Pattern

## What Problem Does It Solve?

You need to create a very large number of fine-grained objects that share most of their
state. Storing every bit of data in each individual object blows out the heap:

```
Without Flyweight — 1,000,000 Particle objects:
  each stores: color (16 B) + texture (32 B) + x (8 B) + y (8 B) + vx (8 B) + vy (8 B)
  total: 1,000,000 × 80 B = ~80 MB

With Flyweight — 5 shared ParticleType + 1,000,000 lightweight position structs:
  5 × 48 B (color + texture) + 1,000,000 × 32 B (x, y, vx, vy) = ~32 MB
```

The key insight: split an object's state into two parts.
- **Intrinsic state** — shared, immutable, stored inside the flyweight.
- **Extrinsic state** — unique per context, passed in as method parameters; NOT stored.

---

## Intrinsic vs Extrinsic State

| | Intrinsic | Extrinsic |
|---|---|---|
| **Definition** | Shared, context-independent data | Context-dependent data |
| **Storage** | Inside the Flyweight object | Outside — passed as method args |
| **Mutability** | Immutable (`private final`) | Varies per use |
| **Example (trees)** | Species, color, texture | Position (x, y), age |
| **Example (chess)** | Piece type ("Pawn"), color ("White") | Board position (row, col) |
| **Example (text)** | Font, size, style | Character x/y position on page |

---

## Core Structure

```
Client ──► Flyweight (interface / class)
                ▲
                │
         ConcreteFlyweight
         - intrinsicA (final)   ← stored here
         - intrinsicB (final)   ← stored here
         + operation(extrinsic) ← extrinsic passed in, NOT stored

FlyweightFactory
- cache: Map<Key, Flyweight>
+ getFlyweight(key): Flyweight  ← returns cached; creates only on first request
```

---

## Code Skeleton

```java
// ── [Flyweight] — stores intrinsic state; receives extrinsic state as parameters ──
class TreeType {
    private final String species;   // [Intrinsic] same for all Oak trees
    private final String color;     // [Intrinsic] same for all Oak trees
    private final String texture;   // [Intrinsic] same for all Oak trees

    TreeType(String species, String color, String texture) {
        this.species = species;
        this.color   = color;
        this.texture = texture;
    }

    // [Extrinsic] x and y are NOT stored — they are passed in on every call.
    // This is the defining characteristic: the method uses them but does NOT keep them.
    void plant(int x, int y) {
        System.out.printf("[%s/%s] planted at (%d, %d)%n", species, color, x, y);
    }
}

// ── [FlyweightFactory] — caches and reuses flyweight instances ────────────────
class TreeTypeRegistry {
    private final Map<String, TreeType> cache = new HashMap<>();

    TreeType getTreeType(String species, String color, String texture) {
        String key = species + "-" + color;
        if (!cache.containsKey(key)) {
            cache.put(key, new TreeType(species, color, texture));
            System.out.println("[Factory] Creating new TreeType: " + key);
        }
        return cache.get(key);
    }

    int getCacheSize() { return cache.size(); }
}
```

Usage:

```java
TreeTypeRegistry registry = new TreeTypeRegistry();

// 1000 trees — but only 3 TreeType objects ever created
for (int i = 0; i < 1000; i++) {
    TreeType t = registry.getTreeType("Oak", "Green", "rough");  // same object every time
    t.plant(random.nextInt(500), random.nextInt(500));            // extrinsic position differs
}
System.out.println(registry.getCacheSize());  // → 3 (Oak, Pine, Birch)
```

---

## Flyweight vs Object Pool

Both reduce object creation, but they are different:

| Dimension | Flyweight | Object Pool |
|---|---|---|
| **State** | Immutable intrinsic state; shared by many callers simultaneously | Mutable; one borrower at a time |
| **Return** | Never returned — client holds reference as long as needed | Borrowed and returned to pool |
| **Purpose** | Reduce memory for shared immutable data | Reduce creation cost for mutable, expensive objects |
| **Examples** | Character glyphs, tree types, chess piece types | DB connections, thread pool workers |

---

## When to Use (and When Not To)

**Use Flyweight when:**
- You create a very large number of objects (tens of thousands or more).
- Most of the object state can be made extrinsic (passed in, not stored).
- Objects can be identified by a small set of shared keys (species, color, type).
- Memory usage is a measurable problem.

**Do NOT use Flyweight when:**
- The number of objects is small — the factory overhead adds complexity for no gain.
- Objects have little or no shared state — every instance is truly unique.
- The extrinsic state is difficult to separate from the object (tightly coupled).

---

## Interview Q&A

**Q1. What is the Flyweight pattern? State the intrinsic/extrinsic split.**
Flyweight reduces memory by sharing common (intrinsic) state across many fine-grained
objects. Intrinsic state is immutable and stored inside the flyweight. Extrinsic state
varies per use and is passed in as method parameters — it is never stored.

**Q2. What is the FlyweightFactory's job?**
The factory maintains a `Map<Key, Flyweight>` cache. When asked for a flyweight, it
checks the cache first. If the key exists, it returns the cached instance. If not, it
creates a new flyweight, stores it, and returns it. This guarantees that each unique set
of intrinsic data has exactly one flyweight object.

**Q3. What happens if you store extrinsic state inside the flyweight?**
The flyweight can no longer be shared. Two trees at different positions would need
different objects — the sharing collapses and you are back to one object per use. In the
worst case, mutable extrinsic state causes one caller to corrupt another's view (because
they share the same object).

**Q4. Why must intrinsic state be immutable (`final` fields)?**
Multiple callers share the same flyweight instance simultaneously. If one caller could
modify the intrinsic state (e.g., change the color), all other callers holding the same
flyweight would see the change — a data corruption bug that is extremely hard to trace.

**Q5. How does Flyweight differ from Singleton?**
Singleton ensures exactly one instance of a class exists globally. Flyweight manages a
family of objects — one instance per unique key. There can be thousands of flyweights (one
per species-color combination), not just one. The factory is the manager; a Singleton
would be one specific flyweight.

**Q6. Name real-world Java examples of the Flyweight pattern.**
- `java.lang.Integer.valueOf(int)` (−128 to 127 range): returns cached `Integer`
  instances rather than creating new ones.
- `String` interning (`String.intern()`): returns a canonical, shared `String` from
  the pool.
- Glyph rendering in text editors: one glyph object per character+font combination,
  shared across all occurrences of that glyph in the document.

---

## Common Mistakes

1. **Storing extrinsic state (position, velocity, index) in the flyweight.**
   This is the most critical mistake — it makes sharing impossible because each context
   needs different values. Extrinsic state must always be passed as a parameter.

2. **Making intrinsic state mutable.**
   `private String color` (no `final`) lets callers modify the shared state, causing
   data corruption for all other callers sharing the same flyweight.

3. **Creating flyweights directly instead of through the factory.**
   `new TreeType("Oak", "Green", texture)` bypasses the cache — you lose the sharing
   benefit and may create duplicate flyweights with identical intrinsic state.

4. **Using a complex or unstable cache key.**
   The key must uniquely identify the intrinsic state and be deterministic. A key like
   `species + "-" + color` is stable; a key that includes a timestamp or random value
   defeats caching.

5. **Confusing Flyweight with Object Pool.**
   Object Pool recycles *mutable* objects that are borrowed and returned (like DB
   connections). Flyweight shares *immutable* objects that are never "returned" — callers
   hold them as long as needed without exclusive ownership.
