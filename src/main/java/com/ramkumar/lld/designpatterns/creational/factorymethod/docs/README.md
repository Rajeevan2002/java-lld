# 3.2 — Factory Method Pattern (Creational)

> "Define an interface for creating an object, but let subclasses decide which class to instantiate.
>  Factory Method lets a class defer instantiation to subclasses."
> — Gang of Four (GoF)

---

## 1. The Problem Factory Method Solves

Imagine a logistics company that started with road transport only. Their `planDelivery()` method
is full of `new Truck()`. Six months later they add sea freight. The `planDelivery()` code must
change every time a new transport type is added — violating OCP.

**Without Factory Method:**
```java
class Logistics {
    void planDelivery(String type) {
        if ("road".equals(type)) {
            new Truck().deliver();                // tightly coupled to Truck
        } else if ("sea".equals(type)) {
            new Ship().deliver();                 // grows every new type
        }
    }
}
```

**With Factory Method:**
```java
abstract class Logistics {
    abstract Transport createTransport();          // factory method — subclass decides

    final void planDelivery() {                   // business method — doesn't know the type
        Transport t = createTransport();          // just calls the factory method
        t.deliver();
    }
}
class RoadLogistics extends Logistics {
    Transport createTransport() { return new Truck(); }  // subclass decides
}
class SeaLogistics extends Logistics {
    Transport createTransport() { return new Ship(); }
}
```

Adding `AirLogistics` never touches `Logistics.planDelivery()`.

---

## 2. Structure

```
«interface» / «abstract»          «interface»
     Creator                          Product
─────────────────────────         ────────────────
+ createProduct()  ◄─── calls ──►  + operation()
  (factory method)                        △
+ someOperation()                         │
        △                                 │
        │ overrides                 ┌─────┴────────┐
        │                           │              │
ConcreteCreatorA         ConcreteProductA  ConcreteProductB
─────────────────
+ createProduct() {
    return new ConcreteProductA();
  }
```

**The four participants:**

| Role | Responsibility |
|---|---|
| **Product** | Interface or abstract class for the objects the factory method creates |
| **ConcreteProduct** | Specific implementation of Product |
| **Creator** | Abstract class declaring the factory method; contains business logic that uses Product |
| **ConcreteCreator** | Overrides the factory method to return a specific ConcreteProduct |

---

## 3. Key Rule: Creator Contains Business Logic, Not Just the Factory

The factory method alone isn't the pattern. The power is that **Creator has business methods
that call the factory method** — they work with whatever Product the subclass provides,
without knowing the concrete type.

```java
abstract class NotificationSender {
    // ── Factory Method (abstract — subclass decides what to create) ──────────
    abstract Notification createNotification();

    // ── Business Method (concrete — works with any Notification) ────────────
    final void sendAlert(String recipient, String message) {
        Notification n = createNotification();   // doesn't know: Email? SMS? Slack?
        System.out.println("Sending via " + n.getChannel());
        n.send(recipient, message);              // polymorphic call
    }
}
```

---

## 4. Factory Method vs Simple Factory vs Abstract Factory

| | Simple Factory | Factory Method | Abstract Factory |
|---|---|---|---|
| GoF pattern? | No (idiom) | Yes | Yes |
| How | Static method, switch/if | Abstract method, overridden | Interface with multiple factory methods |
| Who decides? | The factory class | The subclass (creator) | The concrete factory implementation |
| Extensible? | Must modify factory | Add new creator subclass | Add new factory implementation |
| Use when | Need one creation point | Let subclasses choose what to create | Create *families* of related objects |

```java
// Simple Factory — NOT a GoF pattern
class NotificationFactory {
    static Notification create(String type) {   // switch statement hidden here
        return switch (type) {
            case "email" -> new EmailNotification();
            case "sms"   -> new SmsNotification();
            default      -> throw new IllegalArgumentException(type);
        };
    }
}

// Factory Method — GoF pattern
abstract class NotificationSender {
    abstract Notification createNotification();  // no switch — subclass overrides
}
class EmailSender extends NotificationSender {
    Notification createNotification() { return new EmailNotification(); }
}
```

---

## 5. When to Use / Not Use

**Use Factory Method when:**
- A class can't anticipate which type of objects it must create
- You want subclasses to specify the objects they create
- You're building a framework and want users to extend creation logic
- You need the Creator's business methods to work with any Product variant

**Do NOT use when:**
- You only ever have one concrete product — just use `new`
- The factory logic is trivial (Simple Factory is sufficient)
- You need to create *families* of objects that must be used together → use Abstract Factory

---

## 6. Common Pattern Combination: Factory Method + Template Method

The Creator's business method is often a **Template Method** — it defines the algorithm
skeleton and calls `createProduct()` as one of its steps:

```java
abstract class ReportService {
    // Template Method: defines the steps; calls the factory method in step 2
    final void generateAndSend(String recipient) {
        // Step 1: validate (concrete)
        System.out.println("Validating data...");
        // Step 2: create report — factory method (abstract, subclass provides)
        Report report = createReport();
        // Step 3: render (concrete, delegates to product)
        report.render();
        // Step 4: send (concrete)
        System.out.println("Sending to " + recipient);
    }

    abstract Report createReport();   // factory method
}
```

---

## 7. Interview Q&A

**Q1: What is the Factory Method pattern?**
A: A creational pattern where an abstract Creator class declares an abstract factory method
   for creating a Product. Subclasses (ConcreteCreators) override the factory method to return
   specific ConcreteProduct instances. The Creator's business methods call the factory method
   and work with the Product interface — they never know the concrete type.

**Q2: How is Factory Method different from Simple Factory?**
A: Simple Factory is a static method (or class) with a switch/if chain that creates objects —
   it's not a GoF pattern and violates OCP (must modify to add types). Factory Method has no
   switch: you add a new type by creating a new ConcreteCreator subclass without touching
   existing code. It uses inheritance to defer the decision.

**Q3: What's the difference between Factory Method and Abstract Factory?**
A: Factory Method creates one product type; the subclass decides which concrete product.
   Abstract Factory creates *families* of related objects (e.g., Button + Checkbox +
   TextInput all styled for Windows or Mac). Abstract Factory uses composition (you inject
   a factory object); Factory Method uses inheritance (you subclass the Creator).

**Q4: Should the factory method always be abstract?**
A: It can be abstract (subclass *must* override) or concrete with a default implementation
   (subclass *may* override). Making it abstract is safer when every subclass must provide
   a specific product. Providing a default is useful when a sensible fallback exists.

**Q5: Can the factory method be static?**
A: No. A static method cannot be overridden — it's hidden, not polymorphically dispatched.
   The factory method must be an instance method so that subclass implementations are
   called correctly through polymorphism.

**Q6: What real-world Java APIs use Factory Method?**
A: `java.util.Iterator` — `Collection.iterator()` is a factory method: each collection
   (ArrayList, LinkedList, HashSet) returns a different concrete Iterator. Also
   `java.sql.Connection.createStatement()`, `java.nio.charset.Charset.newDecoder()`,
   and the `DocumentBuilderFactory` family in Java XML.

**Q7: How does Factory Method support OCP?**
A: New product types are added by creating new ConcreteCreator + ConcreteProduct pairs —
   existing Creator and ConcreteCreator classes are not modified. The business logic in
   the Creator is open for extension (via new subclasses) but closed for modification.

---

## 8. Common Mistakes

| Mistake | Why it's wrong | Fix |
|---|---|---|
| Making the factory method `static` | Cannot be overridden — kills polymorphism | Make it an instance method |
| Putting business logic in the factory method | Factory method should only create and return | Move logic to a separate business method |
| Returning `null` from the factory method | Callers get NullPointerException | Always return a non-null Product |
| Calling `createProduct()` in the Creator constructor | Calls the factory before the subclass is ready | Call it lazily in the business method |
| Confusing with Simple Factory | Simple Factory is not Factory Method | Simple Factory = static + switch; Factory Method = abstract + inheritance |
| Making ConcreteCreator unrelated to Creator | Breaks the substitutability | ConcreteCreator always extends Creator |
