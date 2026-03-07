# 3.2.1 Adapter Pattern

## What Problem Does It Solve?

You have an existing class (the **Adaptee**) with a useful interface, but its interface is incompatible with what the rest of your system expects (the **Target** interface). You cannot modify the Adaptee — it may be a third-party library, legacy code, or code owned by another team.

The **Adapter** wraps the Adaptee and translates calls so the client sees only the Target interface.

```
Without Adapter                     With Adapter

Client ──X──► Adaptee               Client ──► Target (interface)
(incompatible interface)                           ▲
                                         ┌─────────┤
                                         │  Adapter │  wraps Adaptee
                                         └─────────┘
                                                │ (composition)
                                                ▼
                                           Adaptee
```

---

## ASCII Structure Diagram

```
┌──────────────┐        uses        ┌───────────────────────┐
│    Client    │ ──────────────────► │  Target  (interface)  │
└──────────────┘                    │  + operation()        │
                                    └───────────────────────┘
                                               ▲
                                               │ implements
                                    ┌──────────┴──────────┐
                                    │      Adapter        │
                                    │  - adaptee: Adaptee │  ← composition
                                    │  + operation()      │
                                    └─────────────────────┘
                                               │ delegates to
                                               ▼
                                    ┌──────────────────────┐
                                    │      Adaptee         │
                                    │  + specificMethod()  │
                                    └──────────────────────┘
```

---

## Object Adapter vs Class Adapter

| Dimension | Object Adapter | Class Adapter |
|---|---|---|
| Mechanism | Composition — stores adaptee as a field | Inheritance — extends the adaptee |
| Java support | Yes (preferred) | Limited — Java has single inheritance, so only works if Target is an interface |
| Flexibility | Can adapt multiple adaptees or subclasses | Locked to one concrete adaptee class |
| Encapsulation | Adaptee is a black box | Adapter exposes adaptee's protected members |
| Override behaviour | Cannot override adaptee methods | Can override adaptee methods |
| **When to use** | **Almost always** | Rarely — only when you need to override adaptee behaviour |

**Rule of thumb**: always start with Object Adapter (composition). Use Class Adapter only if you need to override adaptee methods and inheritance is truly necessary.

---

## Code Skeleton

```java
// Target — the interface the client expects
interface TemperatureSensor {
    double getTemperatureCelsius();
    String getSensorId();
}

// Adaptee — third-party / legacy; cannot modify
class LegacyFahrenheitSensor {
    public double readTempF()       { return 98.6; }
    public String getSerialNo()     { return "SN-001"; }
}

// Object Adapter — bridges the gap via composition
class FahrenheitSensorAdapter implements TemperatureSensor {   // [implements Target]
    private final LegacyFahrenheitSensor sensor;               // [Composition — not inheritance]

    FahrenheitSensorAdapter(LegacyFahrenheitSensor sensor) {
        this.sensor = sensor;
    }

    @Override
    public double getTemperatureCelsius() {
        return (sensor.readTempF() - 32) * 5.0 / 9.0;         // [Translation logic]
    }

    @Override
    public String getSensorId() {
        return sensor.getSerialNo();                           // [Delegation]
    }
}
```

---

## Comparison: Adapter vs Facade vs Bridge

| Pattern | Intent | Relationship to existing code |
|---|---|---|
| **Adapter** | Make an incompatible interface compatible | Wraps a single (or few) existing class(es) |
| **Facade** | Simplify a complex subsystem into one entry point | Wraps multiple classes, hides complexity |
| **Bridge** | Decouple an abstraction from its implementation so both can vary independently | Both sides are *designed* to be extensible; not about legacy code |

Key distinction: Adapter is **retrofit** (applied after the fact to incompatible code). Facade is **simplification**. Bridge is **design-time extensibility**.

---

## Interview Q&A

**Q1. What is the intent of the Adapter pattern?**
To convert the interface of a class into another interface that clients expect. It lets classes work together that otherwise couldn't because of incompatible interfaces — without modifying either the client or the adaptee.

**Q2. What is the difference between object adapter and class adapter?**
Object adapter uses composition: the adapter holds the adaptee as a field and delegates calls. Class adapter uses inheritance: the adapter extends the adaptee. Java's single-inheritance constraint means class adapter only works when the Target is an interface; even then, composition is preferred because it's more flexible and doesn't expose adaptee internals.

**Q3. How does Adapter support the Open/Closed Principle?**
The adaptee is closed for modification (you don't touch it). The adapter is a new class that's open for extension — you can create new adapters for new adaptees without changing existing code. The client (closed) never changes.

**Q4. Can one adapter wrap multiple adaptees?**
Yes. An adapter can hold references to several adaptees and route calls to the right one. However, this is unusual — the more common pattern is one adapter per adaptee to keep responsibilities clear.

**Q5. What is the difference between Adapter and Bridge?**
Adapter is applied *after the fact* to make two incompatible classes work together. Bridge is designed *upfront* to separate an abstraction from its implementation so both can vary independently. Bridge is proactive; Adapter is reactive.

**Q6. Name real-world Java examples of the Adapter pattern.**
- `java.io.InputStreamReader` adapts `InputStream` (byte stream) to `Reader` (character stream).
- `java.io.OutputStreamWriter` adapts `OutputStream` to `Writer`.
- `Arrays.asList()` adapts an array to the `List` interface.
- JDBC `ResultSet` adapts vendor-specific data to a common SQL interface.
- Spring's `HandlerAdapter` in MVC adapts different handler types to a uniform processing interface.

---

## Common Mistakes

1. **Inheriting from the adaptee** instead of wrapping it. This couples you to the adaptee's implementation, breaks if the adaptee is final, and violates composition-over-inheritance.

2. **Putting business logic in the adapter**. The adapter's only job is translation. Validation, calculations, and decisions belong in the service or domain layer — not here.

3. **Modifying the adaptee** to fit the target interface. This defeats the purpose of the pattern and breaks the Open/Closed Principle.

4. **Creating a God adapter** that wraps many unrelated adaptees. Keep each adapter focused on one adaptee.

5. **Forgetting to delegate all methods**. If the Target has five methods and you only implement three, the remaining two will silently return defaults or throw, causing subtle bugs.
