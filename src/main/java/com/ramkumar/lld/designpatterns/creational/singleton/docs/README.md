# 3.1 — Singleton Pattern (Creational)

> "Ensure a class has only one instance and provide a global point of access to it."
> — Gang of Four (GoF)

---

## 1. Intent

A Singleton guarantees that a class has **exactly one instance** for the lifetime of the JVM, and provides a **global access point** to that instance.

```
Client A ──────────┐
                   ▼
Client B ──────► Singleton.getInstance() ──► [ single instance ]
                   ▲
Client C ──────────┘

No matter how many callers ask, they all get the same object back.
```

**When to USE Singleton:**
- Shared resource managers: application config, logging, thread pool
- Global registries: metrics tracker, feature flags, in-memory cache
- Expensive-to-create objects that must be shared: DB connection pool, scheduler
- Single source of truth: game leaderboard, audit trail, session manager

**When NOT to USE Singleton:**
- When the class needs to be unit-tested (singletons resist mocking — use DIP + constructor injection instead)
- When the "single instance" concept doesn't truly apply (you're just avoiding passing an argument)
- When you might need multiple environments in one JVM (multi-tenant applications)
- In multi-JVM clustered systems — Singleton scope is limited to one JVM process

---

## 2. The Five Implementation Variants

### Variant 1 — Eager Initialization

```java
class EagerSingleton {
    // Created immediately when the class is loaded — guaranteed thread-safe by JVM
    private static final EagerSingleton INSTANCE = new EagerSingleton();

    private EagerSingleton() {}   // private constructor

    public static EagerSingleton getInstance() { return INSTANCE; }
}
```

| Pro | Con |
|---|---|
| Thread-safe with zero synchronization overhead | Created even if never used (wasteful if expensive to construct) |
| Simplest implementation | Cannot propagate constructor exceptions gracefully |

---

### Variant 2 — Lazy Initialization (NOT thread-safe)

```java
class LazySingleton {
    private static LazySingleton instance;   // null until first call

    private LazySingleton() {}

    // ❌ NOT thread-safe — two threads can enter the null check simultaneously
    // and both create an instance
    public static LazySingleton getInstance() {
        if (instance == null) {
            instance = new LazySingleton();   // ← race condition here
        }
        return instance;
    }
}
```

**Never use in production** — shown here to explain why the other variants exist.

---

### Variant 3 — Synchronized Method (thread-safe, but slow)

```java
class SynchronizedSingleton {
    private static SynchronizedSingleton instance;

    private SynchronizedSingleton() {}

    // ✅ Thread-safe — but synchronizes on EVERY call, even after instance is created
    // Performance bottleneck: every getInstance() acquires a lock
    public static synchronized SynchronizedSingleton getInstance() {
        if (instance == null) {
            instance = new SynchronizedSingleton();
        }
        return instance;
    }
}
```

Acceptable for low-traffic code. Avoid when `getInstance()` is called frequently.

---

### Variant 4 — Double-Checked Locking (DCL) with `volatile`

```java
class DCLSingleton {
    // volatile is MANDATORY — without it the JVM can reorder writes,
    // making the reference visible before the object is fully constructed
    private static volatile DCLSingleton instance;

    private DCLSingleton() {}

    public static DCLSingleton getInstance() {
        if (instance == null) {                     // First check — no lock (fast path)
            synchronized (DCLSingleton.class) {
                if (instance == null) {             // Second check — with lock (slow path, once only)
                    instance = new DCLSingleton();
                }
            }
        }
        return instance;
    }
}
```

**Why two null checks?**
- First check: avoids acquiring the lock on every call after initialization (fast path).
- Second check: if two threads both pass the first check simultaneously, only one creates the instance.

**Why `volatile`?**
```
Without volatile, the JVM can reorder the steps of `new DCLSingleton()`:
  Step A: allocate memory for the object
  Step B: assign the reference to `instance`     ← JVM may do B before C
  Step C: run the constructor, initialise fields

Thread 1 executes A, then B (reference assigned but constructor not done yet).
Thread 2 sees non-null `instance`, skips both checks, uses a half-constructed object.
→ Silent data corruption.

`volatile` forces a happens-before relationship:
all writes before the assignment are visible to any thread that reads the reference.
```

---

### Variant 5 — Static Inner Class (Bill Pugh / Holder Pattern)

```java
class HolderSingleton {
    private HolderSingleton() {}

    // Inner class is NOT loaded until getInstance() is first called.
    // JVM class loading is intrinsically thread-safe — no synchronized needed.
    private static class Holder {
        private static final HolderSingleton INSTANCE = new HolderSingleton();
    }

    public static HolderSingleton getInstance() {
        return Holder.INSTANCE;   // lazy + thread-safe + no synchronization overhead
    }
}
```

**The cleanest Singleton for production code when the class can be instantiated without checked exceptions.**

---

### Variant 6 — Enum Singleton (Joshua Bloch's recommendation)

```java
enum EnumSingleton {
    INSTANCE;   // only one enum constant — only one instance

    private final Map<String, String> config = new HashMap<>();

    EnumSingleton() {
        config.put("version", "1.0");
    }

    public String get(String key) { return config.get(key); }
}

// Usage:
EnumSingleton.INSTANCE.get("version");
```

**Enum Singleton is immune to:**
- Reflection attacks (`Constructor.newInstance()` throws on enums)
- Serialization attacks (enum deserialization is handled by JVM; same instance returned)

**Con:** Cannot extend another class (enums implicitly extend `Enum<T>`).

---

## 3. Comparison Table

| Variant | Thread-safe | Lazy | Reflection-proof | Serialization-proof | Complexity |
|---|---|---|---|---|---|
| Eager | ✅ | ❌ | ❌ | ❌ | Low |
| Synchronized method | ✅ | ✅ | ❌ | ❌ | Low |
| Double-Checked Locking | ✅ | ✅ | ❌ | ❌ | Medium |
| Static inner class (Holder) | ✅ | ✅ | ❌ | ❌ | Low |
| Enum | ✅ | ❌ | ✅ | ✅ | Low |

**Practical guide:**
- Enum → default choice when no constructor exceptions, no inheritance needed
- Holder → best when you want lazy + no reflection/serialization concern
- DCL → needed when you must initialize with checked exceptions or complex setup

---

## 4. Breaking a Singleton (and How to Defend)

### Attack 1: Reflection

```java
// Without defence — an attacker can create a second instance
Constructor<MySingleton> c = MySingleton.class.getDeclaredConstructor();
c.setAccessible(true);
MySingleton second = c.newInstance();   // ← bypasses private constructor

// Defence: throw in private constructor if already instantiated
private MySingleton() {
    if (instance != null) {
        throw new IllegalStateException("Use getInstance()");
    }
}
```

### Attack 2: Serialization / Deserialization

```java
// Without defence — deserializing creates a new object, breaking the singleton
ObjectInputStream ois = new ObjectInputStream(...);
MySingleton second = (MySingleton) ois.readObject();   // different object!

// Defence: implement readResolve()
protected Object readResolve() {
    return instance;   // return the existing singleton; garbage-collect the deserialized copy
}
```

---

## 5. Singleton vs Static Class

| | Singleton | Static Class (all-static methods) |
|---|---|---|
| Can implement interface | ✅ | ❌ |
| Can be passed as a parameter | ✅ | ❌ |
| Can be lazy-initialized | ✅ | ❌ (class loads eagerly) |
| Can be subclassed | ✅ | ❌ (static methods don't override) |
| Mockable in tests | Possible | Hard |
| When to use | Stateful shared resource | Pure utility functions |

Use Singleton when the object needs state and/or implements an interface. Use static classes for pure utility (like `Math`, `Collections`).

---

## 6. Interview Questions

**Q1: What is the Singleton pattern and when should you use it?**
> Singleton restricts a class to one instance and provides a global access point. Use it when exactly one object must coordinate actions across a system — a config manager, connection pool, or global registry. Avoid it when testability matters (it resists mocking) or when multiple instances might genuinely be needed later.

**Q2: Why is `volatile` required in Double-Checked Locking?**
> Without `volatile`, the JVM can reorder the three steps of object creation: allocate memory → run constructor → assign reference. If the reference assignment happens before the constructor finishes, another thread can read a non-null but incompletely initialized object. `volatile` enforces a happens-before guarantee: all writes before the reference assignment are visible to any thread that reads the reference.

**Q3: What is the best Singleton implementation in modern Java?**
> Enum Singleton is the safest — immune to reflection attacks and serialization bypasses. The Static Inner Class (Holder) pattern is the best choice when enum is inappropriate (e.g., the class must extend another class or has a complex constructor).

**Q4: How can a Singleton be broken and how do you prevent it?**
> Two attack vectors: (1) Reflection — call `getDeclaredConstructor().setAccessible(true).newInstance()`. Defend by throwing `IllegalStateException` in the private constructor if the instance already exists. (2) Serialization — `readObject()` creates a new instance. Defend by implementing `readResolve()` to return the existing singleton. Enum Singleton is immune to both without any additional code.

**Q5: Is a Singleton the same as a static class?**
> No. A Singleton is an object — it can implement interfaces, be passed as a parameter, be lazy-initialized, and be subclassed. A static class has none of these properties. If your "singleton" needs to implement an interface (e.g., for DIP), it must be a Singleton, not a static class.

**Q6: What is the problem with Singletons and unit testing?**
> Singletons carry global state that persists across tests. If one test modifies singleton state (e.g., adds config entries), subsequent tests see the modified state — making tests order-dependent and flaky. The fix: either add a `reset()` method for testing, or better, redesign to use dependency injection with an interface — the Singleton becomes one implementation, injectable and replaceable with a mock.

**Q7: What is the Holder (Bill Pugh) pattern and why is it preferred over DCL?**
> The Holder pattern uses a private static inner class that is only loaded when `getInstance()` is first called. JVM class loading is guaranteed thread-safe, so no `synchronized` or `volatile` is needed. It is lazy, thread-safe, and has no synchronization overhead on reads — combining the benefits of eager initialization and DCL without the complexity.

---

## 7. Common Mistakes

```java
// MISTAKE 1: Missing volatile in DCL — the most dangerous Singleton bug
class BrokenDCL {
    private static BrokenDCL instance;   // ← missing volatile
    public static BrokenDCL getInstance() {
        if (instance == null) {
            synchronized (BrokenDCL.class) {
                if (instance == null) {
                    instance = new BrokenDCL();  // ← may be visible before constructor finishes
                }
            }
        }
        return instance;
    }
}

// MISTAKE 2: Synchronized on the instance (doesn't protect the static method)
public static BrokenSingleton getInstance() {
    synchronized (instance) { ... }   // ← instance is null initially — NullPointerException
    // Must synchronize on the CLASS: synchronized (BrokenSingleton.class)
}

// MISTAKE 3: Public constructor accidentally left in
class LeakySingleton {
    private static LeakySingleton instance;
    public LeakySingleton() {}   // ← should be private — anyone can call new LeakySingleton()
}

// MISTAKE 4: Returning a copy in getInstance()
public static Config getInstance() {
    return new Config(instance);   // ← defeats the entire purpose
}

// MISTAKE 5: Using Singleton where DIP + injection is cleaner
class OrderService {
    public void process(Order o) {
        Config.getInstance().get("tax.rate");  // ← hidden dependency, untestable
        // Better: inject Config as a constructor argument
    }
}
```
