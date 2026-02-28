# S — Single Responsibility Principle (SRP)

> "A class should have only one reason to change."
> — Robert C. Martin (Uncle Bob)

More precisely: **a class should be responsible to one, and only one, actor** —
one person, team, or stakeholder who can demand a change to it.

---

## 1. What "Reason to Change" Really Means

The word "reason" refers to *who* can cause a change, not *how many* methods a class has.

```
Class: InvoiceProcessor
┌────────────────────────────────────────────────────────────┐
│  - stores invoice data              ← Reason 1: Data team  │
│  - calculates tax                   ← Reason 2: Finance     │
│  - formats as HTML / PDF            ← Reason 3: UI team     │
│  - saves to database                ← Reason 4: DBA team    │
│  - sends email                      ← Reason 5: Ops team    │
└────────────────────────────────────────────────────────────┘
         ↑ Five different actors who can demand changes.
           Changing tax rules forces you to retest formatting.
           Changing DB schema forces you to retest email code.
```

A class with one responsibility:

```
┌──────────────────┐   ┌────────────────┐   ┌──────────────────┐
│  Invoice (data)  │   │  TaxCalculator │   │ InvoiceFormatter │
│  one actor:      │   │  one actor:    │   │  one actor:      │
│  Data modeller   │   │  Finance dept  │   │  UI/print team   │
└──────────────────┘   └────────────────┘   └──────────────────┘
```

---

## 2. The God Class Smell

A **god class** is one that knows too much and does too much. Signs:

| Smell | Example |
|-------|---------|
| Class name ends in `Manager`, `Processor`, `Handler`, `Util` | `OrderManager`, `DataHandler` |
| Long list of unrelated imports | `java.sql.*`, `java.mail.*`, `org.html.*` all in one file |
| Methods fall into 3+ unrelated groups | data getters, formatters, DB methods, calculators |
| "And" appears in the responsibility statement | "manages users **and** formats reports **and** sends emails" |
| Changing one feature requires touching this class | Every sprint this file changes |

**The test:** Complete this sentence about your class.
> "This class is responsible for ___."

If you need the word **"and"**, split it.

---

## 3. Before and After SRP

```
BEFORE — God class (one file, five reasons to change)
═══════════════════════════════════════════════════════
InvoiceManager.java
  ├── data: invoiceId, customer, items
  ├── business: calculateTotal(), applyTax()
  ├── formatting: toHtml(), toCsv(), toPdf()
  ├── persistence: save(), findById(), findAll()
  └── notification: sendEmail(), sendSms()

AFTER — SRP-compliant (five files, one reason each)
════════════════════════════════════════════════════
Invoice.java          ← Data only. Reason: data schema changes.
TaxCalculator.java    ← Business only. Reason: tax rule changes.
InvoiceFormatter.java ← Formatting only. Reason: output format changes.
InvoiceRepository.java← Persistence only. Reason: DB/storage changes.
InvoiceNotifier.java  ← Notification only. Reason: comms channel changes.
InvoiceService.java   ← Orchestrates the above. Reason: workflow changes.
```

---

## 4. Refactoring Strategy: Extract Class

The primary refactoring move for SRP violations is **Extract Class**:

```
Step 1: Identify groups of methods that change for the same reason
Step 2: Create a new class for each group
Step 3: Move fields and methods to the new class
Step 4: Replace the original code with delegation to the new class
Step 5: Verify each new class has a single, clear responsibility statement
```

```java
// BEFORE — all in one god class
class OrderProcessor {
    private double price;
    public double applyDiscount()  { ... }  // business logic
    public String toJson()         { ... }  // formatting
    public void   saveToDb()       { ... }  // persistence
    public void   sendReceipt()    { ... }  // notification
}

// AFTER — extracted classes
class Order              { private double price; /* getters */ }
class DiscountCalculator { double apply(Order o) { ... } }
class OrderFormatter     { String toJson(Order o) { ... } }
class OrderRepository    { void save(Order o) { ... } }
class OrderNotifier      { void sendReceipt(Order o) { ... } }
class OrderService       { /* orchestrates all of the above */ }
```

---

## 5. Cohesion vs Coupling

SRP maximises **cohesion** and minimises **coupling**:

```
HIGH COHESION (good)                LOW COHESION (bad)
Everything in the class              Class is a miscellaneous
is closely related                   grab-bag of features

class TaxCalculator {                class Utils {
  double applyGST(double price) {}     double applyGST() {}
  double applyVAT(double price) {}     String formatDate() {}
  double getRateForCategory() {}       void saveToDb() {}
}                                      void sendEmail() {}
                                   }

LOW COUPLING (good)                 HIGH COUPLING (bad)
Classes know little about           Classes reach deep into
each other's internals              each other's internals

OrderService uses Order             OrderService accesses
through its public API              Order's private fields
```

---

## 6. SRP and the Other SOLID Principles

SRP is the foundation. Violating it often forces violations of the others:

- **OCP**: A god class gets modified for every new feature instead of being extended.
- **LSP**: Subclasses of a god class inherit responsibilities they shouldn't have.
- **ISP**: Callers of a god class must depend on methods they don't use.
- **DIP**: A god class creates concrete dependencies everywhere; nothing is injected.

---

## 7. Interview Questions

**Q1: What does "one reason to change" mean?**
> It means one stakeholder or actor can demand a change. A class that handles both tax calculation and PDF formatting has two different departments who can force a change. Separating them means a change in tax rules only touches `TaxCalculator`, not formatting code.

**Q2: How do you identify an SRP violation?**
> Three signals: (1) you need "and" to describe the class's responsibility, (2) the class has method groups that change for unrelated reasons, (3) a change to one feature requires you to touch and retest unrelated code.

**Q3: SRP says one responsibility — does that mean one method?**
> No. `TaxCalculator` can have `applyGST()`, `applyVAT()`, `getRateForCategory()` — many methods, one responsibility (tax calculation). The number of methods is irrelevant; what matters is that they all change for the same reason.

**Q4: What is the difference between SRP and the separation of concerns?**
> They're related but not the same. Separation of concerns is the broader design principle (UI, business logic, data are separate concerns). SRP is the class-level rule that enforces it: each class owns exactly one concern.

**Q5: Can SRP lead to too many small classes?**
> Yes — over-application creates "class explosion" with tiny classes that are hard to navigate. The balance: extract when you have multiple *actors* who would demand changes independently. Don't extract just to make methods shorter.

**Q6: What is a god class and why is it dangerous?**
> A god class handles everything in one place. It's dangerous because: (1) it must be retested entirely when any one of its many responsibilities changes, (2) it becomes a merge conflict hotspot as multiple teams touch it, (3) it's impossible to test in isolation because it has too many dependencies.

**Q7: How does SRP relate to testability?**
> A class with one responsibility has few dependencies and a narrow interface — it's easy to unit test in isolation. A god class requires mocking the DB, the email server, the formatter, and the calculator just to test one behaviour.

---

## 8. Common Mistakes

```java
// MISTAKE 1: Thinking SRP = one method per class
class TaxCalculator {
    double applyGST(double price) { return price * 1.18; }
}
class VATCalculator {           // ← over-split! GST and VAT are the same responsibility
    double applyVAT(double price) { return price * 1.20; }
}
// Fix: one TaxCalculator with multiple tax methods is fine

// MISTAKE 2: "Utility" classes as god-class replacements
class InvoiceUtils {
    static double   tax(double price)  { ... }   // ← multiple unrelated reasons
    static String   format(Invoice i)  { ... }
    static void     save(Invoice i)    { ... }
    static boolean  validate(Invoice i){ ... }
}
// Utility classes are god classes in disguise. Extract proper types.

// MISTAKE 3: Putting validation inside the data class
class BlogPost {
    public boolean validate() {            // ← wrong: BlogPost shouldn't know validation rules
        return title.length() >= 5;       //   validation rules change independently of data shape
    }
}
// Fix: PostValidator.validate(BlogPost post)

// MISTAKE 4: Formatting inside the data class
class Invoice {
    public String toHtml() { ... }        // ← wrong: Invoice shouldn't know about HTML
    public String toCsv()  { ... }        // ← wrong: Invoice shouldn't know about CSV
}
// Fix: InvoiceFormatter.toHtml(Invoice i), InvoiceFormatter.toCsv(Invoice i)

// MISTAKE 5: Service class that also has state
class InvoiceService {
    private List<Invoice> invoices = new ArrayList<>();  // ← state belongs in Repository
    public void save(Invoice i) { invoices.add(i); }    // ← InvoiceService is now also a repo
}
// Fix: InvoiceService orchestrates; InvoiceRepository holds state
```
