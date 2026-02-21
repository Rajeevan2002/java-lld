# Interfaces vs Abstract Classes

> The most common design decision in Java OOP:
> "Should this be an interface or an abstract class?"
> The answer almost always comes down to one question: **does it have state?**

---

## 1. Interface — A Pure Contract

An interface defines **what** an object can do, not **what it is** or **what it holds**.

```java
public interface Payable {
    boolean pay(double amount);      // abstract by default (public)
    String  getPaymentMethod();      // abstract by default

    // Java 8+ — default method: provides a body, subclasses can override
    default String getReceipt() {
        return "Payment via " + getPaymentMethod();
    }

    // Java 8+ — static method: belongs to the interface, not the instance
    static boolean isValidAmount(double amount) {
        return amount > 0;
    }
}
```

### Interface rules

| Rule | Detail |
|------|--------|
| Fields | `public static final` only — constants, not instance state |
| Methods | `public abstract` by default; `default` and `static` since Java 8 |
| Constructor | **None** — cannot be instantiated |
| Inheritance | A class can `implements` **multiple** interfaces |
| Extends | An interface can `extends` multiple other interfaces |

---

## 2. Abstract Class — Partial Implementation

An abstract class defines **what something is** and shares **common state and behaviour**
with subclasses, while deferring specific behaviour to them.

```java
public abstract class Payment {
    private final String transactionId;    // instance state — impossible in an interface
    private final double amount;
    private final long   timestamp;

    protected Payment(String transactionId, double amount) {
        this.transactionId = transactionId;
        this.amount        = amount;
        this.timestamp     = System.currentTimeMillis();
    }

    // Subclasses MUST implement this
    public abstract boolean execute();

    // Shared logic — calls the abstract method (Template Method pattern)
    public String getSummary() {
        return transactionId + " | ₹" + amount + " | " + (execute() ? "OK" : "FAIL");
    }

    public String getTransactionId() { return transactionId; }
    public double getAmount()        { return amount; }
}
```

### Abstract class rules

| Rule | Detail |
|------|--------|
| Fields | Any — instance fields, static fields, constants |
| Methods | Any mix of abstract and concrete |
| Constructor | Yes — called via `super()` from subclasses |
| Inheritance | A class can `extends` only **one** abstract class |

---

## 3. The Decision Diagram

```
You need to share behaviour/structure across related classes?
                │
         ┌──────┴──────┐
         │             │
        YES             NO
         │             │
   Does it have         Use INTERFACE
   shared STATE         (pure contract,
   (instance fields)?   multiple impl)
         │
   ┌─────┴─────┐
   │           │
  YES          NO
   │           │
Abstract    Could work either way —
 Class      prefer INTERFACE for
            flexibility (testability,
            multiple inheritance of type)
```

**One-line rule:**
> If it has **instance fields** → abstract class.
> If it's a **capability** (can-do, not is-a) → interface.

---

## 4. Multiple Interface Implementation

A class can implement **multiple** interfaces — this is Java's answer to multiple inheritance.

```
                ┌──────────────┐     ┌──────────────┐
                │   Playable   │     │  Recordable  │
                │  + play()    │     │  + record()  │
                │  + pause()   │     │  + stop()    │
                └──────┬───────┘     └──────┬───────┘
                       │                    │
                       └────────┬───────────┘
                                │  implements both
                         ┌──────┴──────┐
                         │  Smartphone │
                         │  extends    │
                         │  MediaDevice│
                         └─────────────┘
```

```java
class Smartphone extends MediaDevice implements Playable, Recordable {
    @Override public void play()   { ... }
    @Override public void pause()  { ... }
    @Override public void record() { ... }
    @Override public void stop()   { ... }
}
```

---

## 5. Coding to Interface (Dependency Inversion preview)

Always declare variables using the **interface type**, not the concrete class.

```java
// Bad — locked to one concrete type
CreditCardPayment payment = new CreditCardPayment(...);

// Good — works with any Payable
Payable payment = new CreditCardPayment(...);

// Excellent — a method that accepts any Payable
public void process(List<Payable> payments) {
    payments.forEach(p -> p.pay(p.getAmount()));
}
```

This means you can swap `CreditCardPayment` for `UPIPayment` without changing the calling code.

---

## 6. Default Methods (Java 8+)

Default methods let interfaces evolve without breaking existing implementations.

```java
public interface Loggable {
    void log(String event);
    List<String> getLogHistory();

    // Default — implementors get this for free; can override if needed
    default String getLastLog() {
        List<String> history = getLogHistory();
        return history.isEmpty() ? "No logs yet" : history.get(history.size() - 1);
    }
}
```

**Key point:** If two interfaces both provide a `default` method with the same signature,
the implementing class **must** override it to resolve the conflict.

```java
interface A { default void greet() { System.out.println("A"); } }
interface B { default void greet() { System.out.println("B"); } }

class C implements A, B {
    @Override public void greet() { A.super.greet(); }  // must resolve explicitly
}
```

---

## 7. Full Comparison Table

| Feature | Interface | Abstract Class |
|---------|-----------|----------------|
| Instance fields | ❌ (only `public static final`) | ✅ |
| Constructor | ❌ | ✅ |
| Multiple inheritance | ✅ (implements many) | ❌ (extends one) |
| Default methods | ✅ (Java 8+) | ✅ (always) |
| Static methods | ✅ (Java 8+) | ✅ |
| Access modifiers on methods | `public` only | Any |
| Use when | Defining a capability/role | Sharing state + partial impl |
| IS-A vs CAN-DO | CAN-DO (`Flyable`, `Serializable`) | IS-A (`Animal`, `Vehicle`) |

---

## 8. ASCII — How They Relate in a Real Design

```
          «interface»          «interface»
           Sendable              Loggable
         + send()             + log(event)
         + getChannel()       + getLogHistory()
         + getPriority()      + getLastLog() ← default
              │                    │
              │    abstract class  │
              └──────┐    ┌────────┘
                     ▼    ▼
              abstract Notification
              - notificationId (final)
              - recipient      (final)
              - message        (final)
              + validateContent() ← abstract
              + getSummary()      ← concrete, calls validateContent()
                     │
         ┌───────────┼───────────┐
         ▼           ▼           ▼
      Email         SMS         Push
   Notification  Notification Notification
  (Sendable +   (Sendable     (Sendable +
   Loggable)     only)         Loggable)
```

---

## 9. Interview Questions

**Q1: Can an interface have a constructor?**
> No. Interfaces cannot be instantiated and have no constructors. Object construction belongs to classes.

**Q2: Why would you choose abstract class over interface when both could work?**
> When subclasses need to share **instance state** (fields). Interfaces only allow `public static final` constants. If five subclasses all need a `private String id` set in construction, an abstract class is the only clean option.

**Q3: Can an abstract class implement an interface without providing all method implementations?**
> Yes. An abstract class can choose which interface methods to implement and leave the rest `abstract` for its subclasses. Only concrete (non-abstract) classes must implement all interface methods.

**Q4: What is the diamond problem and how does Java handle it?**
> If two interfaces both define a `default` method with the same signature, and a class implements both, the compiler forces the class to `@Override` the method and explicitly choose which one to call (`A.super.method()` or `B.super.method()`).

**Q5: What is a marker interface?**
> An interface with no methods at all — used purely to tag a class for some JVM or framework behaviour. `Serializable` and `Cloneable` are classic examples. Modern Java prefers annotations for this purpose.

**Q6: Since Java 8 added default methods, is there any reason to use abstract classes?**
> Yes — **instance fields**. Default methods let interfaces provide behaviour, but they still cannot hold instance state. If your design requires shared mutable or immutable state across subclasses (e.g., a shared `id`, `createdAt`, `name`), you need an abstract class.

**Q7: What does "programming to an interface" mean?**
> Declare variables and parameters using the interface type (`List<Sendable>`) instead of the concrete type (`List<EmailNotification>`). This decouples callers from implementations, making the code easier to test, extend, and swap.

---

## 10. Common Mistakes

```java
// MISTAKE 1: Putting state in an interface (compile error)
public interface Notification {
    private String id;   // ← compile error — interfaces cannot have instance fields
}

// MISTAKE 2: Forgetting that interface fields are implicitly public static final
public interface Config {
    int MAX_RETRIES = 3;   // actually: public static final int MAX_RETRIES = 3
}
// Trying to do: new Config() — compile error, interface can't be instantiated

// MISTAKE 3: Narrowing access when implementing interface methods
public interface Sendable {
    boolean send();   // implicitly public
}
class Email implements Sendable {
    protected boolean send() { }   // compile error — can't narrow from public to protected
}

// MISTAKE 4: Using abstract class when there's no shared state
abstract class Flyable {    // no fields, all abstract methods
    abstract void fly();
    abstract void land();
}
// ← This should be an interface. The abstract class adds no value here.

// MISTAKE 5: Forgetting to resolve default method conflict
interface A { default void greet() { System.out.println("A"); } }
interface B { default void greet() { System.out.println("B"); } }
class C implements A, B { }   // compile error — must override greet()
```
