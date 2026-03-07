# 3.5 — Prototype Pattern (Creational)

> "Specify the kinds of objects to create using a prototypical instance,
>  and create new objects by copying this prototype."
> — Gang of Four (GoF)

---

## 1. The Problem Prototype Solves

Sometimes creating an object from scratch is expensive or complex — it may involve
database lookups, API calls, heavy computation, or deeply nested sub-objects.
The Prototype pattern lets you **clone an existing object** as a starting point
instead of constructing a new one.

```
Without Prototype                     With Prototype
─────────────────────────────────     ────────────────────────────────────
new Warrior("template", 50, 300)      Warrior clone = template.clone()
  .addItem("Sword")                   clone.setName("Boss Warrior")
  .addItem("Shield")                  clone.setLevel(99)
  .addItem("Potion x3")              // inventory already copied — no rebuild
  .setArmor(300)
// ← re-does all setup for every variant
```

---

## 2. Structure

```
«AbstractPrototype»
──────────────────────────────────────────────────────────────────
GameCharacter  (abstract)
──────────────────────────────────────────────────────────────────
# name      : String
# level     : int
# inventory : List<String>
──────────────────────────────────────────────────────────────────
+ clone()           : GameCharacter   ← abstract; each subclass deep-copies itself
+ addItem(item)     : void
+ removeItem(item)  : void
+ getInventory()    : List<String>    ← unmodifiable view
+ toString()        : String


«ConcretePrototypes»
──────────────────────────
Warrior   Mage    Archer
──────────────────────────
armor     manaPool  arrowCount
──────────────────────────
clone()   clone()   clone()   ← each creates a new instance with deep-copied state
```

---

## 3. Shallow Copy vs Deep Copy — The Critical Distinction

```
Original Warrior
  name      → "Warrior"          (String — immutable, safe to share)
  level     → 50                 (int    — primitive, always copied by value)
  armor     → 300                (int    — primitive, always copied by value)
  inventory → ArrayList@A        (reference — MUST deep-copy)
                └─ ["Sword", "Shield"]

─────────────────────────────────────────────────────────
Shallow copy (WRONG)              Deep copy (CORRECT)
─────────────────────────────────────────────────────────
clone.inventory → ArrayList@A     clone.inventory → ArrayList@B
                  ↑ SAME object                     ↑ NEW object
                  │                                  │
                  └─ ["Sword",     └─ ["Sword", "Shield"]
                      "Shield"]
If clone adds "Axe":              If clone adds "Axe":
  original.inventory now has       original.inventory unchanged
  "Axe" too — BUG!                 — correct
```

**Rule**: any mutable field (collection, array, mutable object) needs a defensive copy.
Primitives and Strings are safe to copy by value/reference.

---

## 4. Implementation Options

### Option A — Copy constructor (preferred in Java, no Cloneable needed)

```java
// In Warrior:
public Warrior(Warrior source) {                      // copy constructor
    super(source);                                    // copies base fields
    this.armor = source.armor;
}

@Override
public Warrior clone() {
    return new Warrior(this);                         // delegates to copy constructor
}

// In GameCharacter:
protected GameCharacter(GameCharacter source) {       // protected copy constructor
    this.name      = source.name;
    this.level     = source.level;
    this.inventory = new ArrayList<>(source.inventory); // deep copy of list
}
```

### Option B — Manual inline deep copy (acceptable, slightly more verbose)

```java
@Override
public Warrior clone() {
    Warrior w = new Warrior(this.name, this.level, this.armor);
    for (String item : this.inventory) {
        w.addItem(item);
    }
    return w;
}
```

### Why NOT `Cloneable`?

| | `Cloneable` / `Object.clone()` | Copy constructor |
|---|---|---|
| Access modifier | `protected` by default — must override | `public` naturally |
| Exception | throws `CloneNotSupportedException` | no checked exception |
| Deep copy | NOT automatic — still must override | explicit in constructor |
| Readability | `super.clone()` magic, fragile | explicit field-by-field |
| Java guidance | Effective Java §13: "Avoid Cloneable" | recommended |

---

## 5. Prototype vs Factory Method vs Builder

| Question | Prototype | Factory Method | Builder |
|---|---|---|---|
| Starting point | An existing instance | Nothing | Nothing |
| Construction cost | Amortized — template built once | Full cost every time | Full cost every time |
| Variation | Mutate clone after copy | Override creator method | Chain setter calls |
| Use when | Object creation is expensive; variants differ slightly | Subclass decides exact type | Many optional parameters |

---

## 6. Prototype Registry (optional extension)

A registry stores named prototypes so callers don't need a reference to the original:

```java
class CharacterRegistry {
    private final Map<String, GameCharacter> prototypes = new HashMap<>();

    void register(String key, GameCharacter proto) {
        prototypes.put(key, proto);
    }

    GameCharacter get(String key) {
        GameCharacter proto = prototypes.get(key);
        if (proto == null) throw new NoSuchElementException("No prototype: " + key);
        return proto.clone();   // always returns a clone, never the original
    }
}
```

---

## 7. Interview Q&A

**Q1: What problem does Prototype solve?**
A: It avoids expensive re-construction of complex objects. Instead of building from scratch each time, you clone a ready-made prototype and only change what differs.

**Q2: What is the difference between shallow and deep copy?**
A: A shallow copy copies primitive fields and copies the references to objects (both original and clone share the same sub-objects). A deep copy creates new instances of all mutable sub-objects. For the Prototype pattern, deep copy is almost always required for collections.

**Q3: Why avoid Java's `Cloneable`?**
A: `Object.clone()` is protected, requires casting, throws a checked exception, does a shallow copy by default, and its contract is poorly specified. Josh Bloch in Effective Java calls it "a highly problematic interface." The copy constructor approach is clearer and safer.

**Q4: How does Prototype differ from copy constructor?**
A: They're complementary. A copy constructor is a common Java implementation of Prototype. Prototype is the design pattern (intent: clone-based creation). Copy constructor is the mechanism.

**Q5: When is Prototype better than Factory Method?**
A: When the cost of constructing the initial object is high (DB query, complex init), and you need many slightly-different variants. Factory Method recreates from scratch each time; Prototype pays the init cost once.

**Q6: What is a Prototype Registry?**
A: A map from name → prototype instance. Callers ask the registry for a clone by name, without needing a direct reference to the prototype. Decouples clone users from concrete prototype types.

**Q7: Can primitives and Strings be shallow-copied safely?**
A: Yes. Primitives are copied by value always. Strings are immutable in Java — sharing the same String reference between original and clone is safe because neither can mutate it.

---

## 8. Common Mistakes

| Mistake | Why it's wrong | Fix |
|---|---|---|
| Shallow-copying a mutable collection | Original and clone share the list — mutations bleed | `new ArrayList<>(source.list)` |
| Using `Cloneable` / `Object.clone()` | Fragile, shallow by default, checked exception | Use copy constructor |
| Making `clone()` return `Object` | Forces callers to cast — breaks covariant return | Declare return type as the concrete class |
| Forgetting to copy subclass fields in `clone()` | Clone has wrong subclass state (armor=0 for Warrior) | Each subclass copies its own extra fields |
| Exposing internal list via `getInventory()` | Caller can mutate internal state | Return `Collections.unmodifiableList(inventory)` |
| Mutating the prototype itself instead of a clone | All future clones start from a dirty template | Always mutate the clone, never the prototype |
