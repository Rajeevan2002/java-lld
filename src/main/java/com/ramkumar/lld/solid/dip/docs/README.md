# D — Dependency Inversion Principle (DIP)

> "High-level modules should not depend on low-level modules. Both should depend on abstractions.
> Abstractions should not depend on details. Details should depend on abstractions."
> — Robert C. Martin

Simplified: **Program to interfaces, not to implementations. Inject your dependencies; don't create them.**

---

## 1. The Two DIP Rules

| Rule | Plain English |
|---|---|
| **Rule 1** | High-level modules (business logic) must NOT `new` their low-level dependencies (database, email, payment). Both must depend on an abstraction (interface). |
| **Rule 2** | The interface is defined by the high-level module (or a shared layer). Low-level modules implement it — the dependency arrow is inverted. |

---

## 2. Before → After ASCII Diagram

```
BEFORE — DIP Violation
══════════════════════
                    High-level module
                  ┌───────────────────────────┐
                  │   OrderProcessor           │
                  │   ─────────────────────── │
                  │   new MySQLOrderRepo()    ─┼──► MySQLOrderRepository  (low-level)
                  │   new GmailEmailSender()  ─┼──► GmailEmailSender      (low-level)
                  │   new StripeGateway()     ─┼──► StripePaymentGateway  (low-level)
                  └───────────────────────────┘

  Problem: OrderProcessor is hardwired to MySQL, Gmail, and Stripe.
           You cannot swap them without modifying OrderProcessor.
           You cannot unit-test OrderProcessor without a real database and SMTP server.

AFTER — DIP Compliant
═════════════════════
      High-level module          Abstractions (interfaces — owned by high-level layer)
  ┌────────────────────┐         ┌──────────────────┐  ┌───────────────┐  ┌────────────────┐
  │  OrderProcessor    │──uses──►│  OrderRepository │  │  MessageSender│  │ PaymentGateway │
  └────────────────────┘         └──────────────────┘  └───────────────┘  └────────────────┘
                                          ▲                    ▲                  ▲
                                          │ implements         │ implements       │ implements
                                  MySQLOrderRepo         GmailSender        StripeGateway
                                  InMemoryOrderRepo      SmsSender          MockGateway
                                  (low-level details)    (low-level details)(low-level details)

  Result: OrderProcessor never changes when you swap MySQL → Postgres,
          Gmail → SNS, Stripe → PayPal. Low-level details depend on abstractions — not vice versa.
```

---

## 3. Core Concepts

### 3.1 The Violation: Creating Dependencies Internally

```java
// ❌ DIP VIOLATED — OrderProcessor "new"s its own dependencies
class OrderProcessor {
    private final MySQLOrderRepository repo;    // ← concrete low-level class
    private final GmailEmailSender     sender;  // ← concrete low-level class
    private final StripePaymentGateway gateway; // ← concrete low-level class

    public OrderProcessor() {
        this.repo    = new MySQLOrderRepository();   // ← tightly coupled
        this.sender  = new GmailEmailSender();       // ← tightly coupled
        this.gateway = new StripePaymentGateway();   // ← tightly coupled
    }
    // To test this class you need a real MySQL DB, Gmail SMTP, and Stripe API key.
}
```

### 3.2 The Fix: Constructor Injection

```java
// ✅ DIP COMPLIANT — dependencies are abstractions, injected from outside
interface OrderRepository  { Order save(Order o); Optional<Order> findById(String id); }
interface MessageSender    { void send(String to, String subject, String body); }
interface PaymentGateway   { PaymentResult charge(String customerId, double amount); }

class OrderProcessor {
    private final OrderRepository  repo;      // ← abstraction (interface)
    private final MessageSender    sender;    // ← abstraction (interface)
    private final PaymentGateway   gateway;   // ← abstraction (interface)

    // CONSTRUCTOR INJECTION: the caller decides which implementation to provide
    public OrderProcessor(OrderRepository repo, MessageSender sender, PaymentGateway gateway) {
        this.repo    = Objects.requireNonNull(repo,    "repo cannot be null");
        this.sender  = Objects.requireNonNull(sender,  "sender cannot be null");
        this.gateway = Objects.requireNonNull(gateway, "gateway cannot be null");
    }
    // Now you can test with InMemoryOrderRepo + MockSender + MockGateway — no real infra needed.
}
```

### 3.3 The Three Injection Types

| Type | Code | When to use |
|---|---|---|
| **Constructor injection** | `public MyService(Repo r, Sender s)` | Always prefer — dependencies clear at instantiation, enables `final` fields |
| **Setter injection** | `public void setRepo(Repo r)` | Optional dependencies; rarely used in Java |
| **Field injection** | `@Autowired private Repo r;` | Framework-managed (Spring DI); avoid in vanilla Java — hidden dependency |

**Prefer constructor injection**: fields can be `final`, dependencies are explicit, no "partially constructed" state.

### 3.4 DIP ≠ Dependency Injection Framework

DIP is a **principle** — you can apply it manually in pure Java.
Dependency Injection (Spring, Guice, CDI) is a **framework** that automates the wiring.

```java
// DIP without a framework — manual wiring in main()
OrderRepository  repo    = new InMemoryOrderRepository();
MessageSender    sender  = new ConsoleMessageSender();
PaymentGateway   gateway = new MockPaymentGateway();
OrderProcessor   proc    = new OrderProcessor(repo, sender, gateway);   // ← manual DI
```

### 3.5 The Inversion: Who Owns the Interface?

This is the key insight of DIP vs simple "use interfaces":

```
Simple "use interfaces" (not DIP):
  Low-level module defines its own interface:
  MySQLOrderRepository → defines IOrderRepository
  OrderProcessor depends on IOrderRepository (defined by MySQL module)
  The control still flows from high to low.

DIP — true inversion:
  OrderRepository interface is defined IN the high-level (OrderProcessor's) package.
  MySQLOrderRepository (low-level) DEPENDS ON and implements the interface.
  The low-level module now depends on the high-level module's abstraction.
  The dependency arrow from low-level TO high-level is the "inversion".
```

---

## 4. DIP Enables OCP and Testability

```java
// ✅ DIP + OCP: Swap payment gateway without changing OrderProcessor
OrderProcessor forStripe = new OrderProcessor(repo, sender, new StripeGateway());
OrderProcessor forPayPal = new OrderProcessor(repo, sender, new PayPalGateway());
// OrderProcessor.java has ZERO changes — OCP satisfied

// ✅ DIP + Testability: Test OrderProcessor with mocks (no real infra)
class FixedAmountGateway implements PaymentGateway {
    @Override public PaymentResult charge(String customerId, double amount) {
        return new PaymentResult(true, "MOCK-TXN-001");   // always succeeds
    }
}
OrderProcessor testProc = new OrderProcessor(inMemoryRepo, captureSender, new FixedAmountGateway());
// No database, no SMTP, no Stripe API key needed.
```

---

## 5. Common Violations

```java
// VIOLATION 1: new inside the constructor (most common)
class ReportService {
    private final DatabaseLogger logger = new DatabaseLogger();   // ← hardcoded
    // Fix: inject Logger interface
}

// VIOLATION 2: new inside a method body
class UserService {
    public void register(User u) {
        new SmtpEmailSender().send(u.email, "Welcome!");   // ← new inside method body
        // Fix: inject MessageSender, call sender.send(...)
    }
}

// VIOLATION 3: static factory call (hidden dependency)
class OrderService {
    public void process(Order o) {
        PaymentGateway gw = StripeFactory.getInstance();   // ← static factory = hidden coupling
        // Fix: inject PaymentGateway
    }
}

// VIOLATION 4: depending on a concrete class from another package
class BillingService {
    private MySQLInvoiceRepository repo;   // ← concrete class — DIP violated even without "new"
    // Fix: use InvoiceRepository interface
}

// VIOLATION 5: exposing concrete type in return value
interface OrderService {
    MySQLOrderRepository getRepository();   // ← leaks concrete type through interface
    // Fix: OrderRepository getRepository();
}
```

---

## 6. Interview Questions

**Q1: What does DIP say in plain English?**
> High-level business logic should not be wired to specific implementations (MySQL, Gmail, Stripe). Both the business logic and the implementation should depend on a shared abstraction (interface). When you need to change the database, only the concrete class changes — the business logic stays untouched.

**Q2: What is the difference between Dependency Inversion Principle and Dependency Injection?**
> DIP is a design principle — it says to depend on abstractions. Dependency Injection (DI) is a pattern/technique — it says to supply dependencies from outside (via constructor, setter, or framework). DI is the most common way to achieve DIP, but DIP doesn't require a framework. You can do manual constructor injection in plain Java.

**Q3: Why is `new` inside a constructor a DIP violation?**
> When class A does `new B()` in its constructor, A is tightly coupled to the concrete class B. You cannot substitute a different implementation of B without modifying A. The fix is constructor injection: A declares it needs a `BInterface`, and the caller provides the actual `B` (or a test mock). A never creates its own dependencies.

**Q4: Why is constructor injection preferred over field injection?**
> Constructor injection makes dependencies explicit (you cannot forget them), allows fields to be `final` (immutability), and enables easy unit testing (pass mocks in the constructor). Field injection (e.g., `@Autowired`) hides dependencies, prevents `final`, and makes testing harder without a DI framework.

**Q5: How does DIP relate to OCP?**
> DIP enables OCP: when your high-level module depends on an interface, you can add new implementations (new payment gateways, new notifiers) without modifying the high-level module. The interface is the stable "closed" abstraction; new implementations are "open extensions." Without DIP, every new provider would require changes to the high-level class.

**Q6: What does "the abstraction belongs to the high-level module" mean?**
> In true DIP, the interface is defined in (or alongside) the high-level module, not the low-level module. For example, `OrderRepository` interface lives in the business/domain layer. `MySQLOrderRepository` lives in the infrastructure layer and depends on (implements) the domain interface. The dependency arrow goes from infrastructure → domain — which is the "inversion" compared to typical layered architecture where business logic depends on infrastructure.

**Q7: How would you test a class that uses DIP without a database?**
> Because DIP uses constructor injection with interfaces, you create a lightweight in-memory implementation (e.g., `InMemoryOrderRepository` backed by a `HashMap`). You pass this mock into the high-level class's constructor. No database is needed. This is the DIP testability payoff — the concrete implementation is swapped without touching any business logic code.
