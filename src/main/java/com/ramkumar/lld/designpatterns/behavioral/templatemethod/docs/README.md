# 3.3.4 Template Method Pattern

## What Problem Does It Solve?

You have several classes that perform the same overall algorithm, but differ in specific
steps. Without Template Method you either duplicate the skeleton in each class or use a
messy chain of conditionals:

```
Without Template Method — duplicated skeleton:
  class CsvExporter {
      void export(String[] rows) {
          openFile();    // same
          writeHeader(); // differs
          writeRows();   // differs
          closeFile();   // same
      }
  }
  class HtmlExporter {
      void export(String[] rows) {
          openFile();    // same — duplicated!
          writeHeader(); // differs
          writeRows();   // differs
          closeFile();   // same — duplicated!
      }
  }

With Template Method — skeleton in base class, steps in subclasses:
  abstract class DataExporter {
      final void export(String[] rows) {   // ← template method; FINAL
          openFile();
          writeHeader();   // abstract — subclass fills in
          writeRows(rows); // abstract — subclass fills in
          closeFile();     // concrete — shared by all
      }
  }
```

The key insight: define the algorithm skeleton once in a `final` method of an abstract
class, and let subclasses fill in the varying steps by overriding abstract methods.

---

## Core Structure

```
        TaxCalculator          ← abstract class
        ─────────────────────
        + final calculate()    ← template method — sequence is fixed
                 │
          ┌──────┼───────────────────────┐
          │      │                       │
          ▼      ▼                       ▼
   computeTaxable()  applyTaxRate()  applyCredits()
      «abstract»      «abstract»       «hook»
                                   (default: no-op)

              ▲                    ▲
    ┌─────────┤          ┌─────────┤
    │                    │
 Individual           Corporate
 TaxCalculator        TaxCalculator
 (overrides 2)        (overrides 3)
```

Three method types in a Template Method:

| Type | Declared as | Subclass must? | Purpose |
|---|---|---|---|
| **Template method** | `final` (concrete) | Cannot override | Fixes the algorithm sequence |
| **Abstract step** | `abstract` | Must override | Varying parts that differ per subclass |
| **Hook** | Concrete with default | May override | Optional extension point |

---

## Code Skeleton

```java
// ── Abstract class — owns the algorithm skeleton ──────────────────────────────
abstract class TaxCalculator {

    // [TemplateMethod] final = subclasses CANNOT reorder or skip steps.
    // This is the defining constraint of the pattern.
    public final double calculate(double grossIncome) {
        double taxable  = computeTaxableIncome(grossIncome);  // abstract
        double tax      = applyTaxRate(taxable);               // abstract
        double finalTax = applyCredits(tax);                   // hook
        printBreakdown(grossIncome, taxable, tax, finalTax);   // concrete
        return finalTax;
    }

    // [AbstractStep] Subclass must implement — no default makes sense here.
    abstract double computeTaxableIncome(double grossIncome);
    abstract double applyTaxRate(double taxableIncome);

    // [Hook] Concrete method with a sensible default.
    // Subclasses can override to add credits; most won't need to.
    double applyCredits(double tax) { return tax; }   // default: no credits

    // [ConcreteStep] Shared logic — identical for every filing type.
    void printBreakdown(double gross, double taxable, double tax, double finalTax) {
        System.out.printf("[Tax] Gross: $%.2f | Taxable: $%.2f | Pre-credit: $%.2f | Due: $%.2f%n",
            gross, taxable, tax, finalTax);
    }
}

// ── Concrete subclass — fills in the varying steps ───────────────────────────
class IndividualTaxCalculator extends TaxCalculator {

    @Override
    double computeTaxableIncome(double grossIncome) {
        return grossIncome - 12_000.0;   // standard deduction
    }

    @Override
    double applyTaxRate(double taxableIncome) {
        return taxableIncome * 0.22;
    }
    // applyCredits not overridden — uses hook default (no credits)
}
```

---

## Template Method vs Strategy

Both solve the "varying algorithm" problem, but in opposite ways:

| Dimension | Template Method | Strategy |
|---|---|---|
| **Mechanism** | Inheritance (abstract class) | Composition (interface field) |
| **Skeleton fixed?** | Yes — `final` in base class | No — each strategy is the whole algorithm |
| **Runtime swap** | No — subclass is chosen at construction | Yes — `setStrategy()` at any time |
| **Granularity** | Varies steps of one algorithm | Varies the whole algorithm |
| **OCP** | New variant = new subclass | New variant = new class |
| **Coupling** | Subclass and base class are tightly coupled | Context and strategy are loosely coupled |

**Rule of thumb**: Use Template Method when the skeleton is stable and you control both
the base and the subclasses. Use Strategy when you need runtime swap or the "strategies"
come from outside your codebase.

---

## Interview Q&A

**Q1. What is the Template Method pattern? State the intent in one sentence.**
Template Method defines the skeleton of an algorithm in a `final` method of an abstract
class, deferring some steps to subclasses — so subclasses can redefine certain steps
without changing the algorithm's structure.

**Q2. Why must the template method be `final`?**
If a subclass could override `calculate()`, it could reorder or skip steps — defeating the
entire purpose of fixing the skeleton in the base class. `final` enforces that the
algorithm's structure belongs to the base class alone.

**Q3. What is a hook method?**
A hook is a concrete method in the base class with a default (often empty or identity)
implementation. Subclasses may override it to extend behaviour at a specific point in the
algorithm. Unlike abstract methods, hooks are optional — a subclass that doesn't need the
extension point simply inherits the default.

**Q4. What is the Hollywood Principle and how does Template Method apply it?**
"Don't call us, we'll call you" — the base class (Hollywood) calls the subclass's overridden
methods at the right moments; the subclass never directly calls the template method itself.
The flow of control is inverted relative to normal library use.

**Q5. How does Template Method differ from Strategy?**
Template Method varies steps of an algorithm via inheritance; the skeleton is fixed in the
base class and cannot be swapped at runtime. Strategy varies the whole algorithm via
composition; the context holds a strategy reference that can be replaced at runtime.

**Q6. Can a hook method call an abstract method?**
Yes. A concrete method in the base class can call abstract methods defined in the same
class — the JVM dispatches to the overridden version at runtime. This is a common pattern:
the hook provides a default that may delegate to an abstract step.

**Q7. What happens if you forget `final` on the template method?**
Subclasses can override the template method and bypass the skeleton entirely. The pattern
breaks — a subclass could call `applyTaxRate` before `computeTaxableIncome`, or skip
`printBreakdown` altogether. `final` is not optional.

---

## Common Mistakes

1. **Forgetting `final` on the template method.**
   Without `final`, subclasses can override the skeleton — the pattern's core guarantee
   is lost. Every template method must be `final`.

2. **Declaring the template method `abstract`.**
   An `abstract` template method has no body — it cannot define the sequence. The template
   method must be `final` AND concrete (have a body that calls the steps).

3. **Using `private` on abstract step methods.**
   `private abstract` is illegal in Java (private methods can't be overridden). Step methods
   should be `protected` or package-private so subclasses can override them.

4. **Not providing a default for hook methods.**
   If a hook has no body, it is `abstract` and all subclasses are forced to implement it —
   defeating the "optional extension" purpose of a hook. Always provide a sensible default.

5. **Putting algorithm logic in the constructor.**
   The template method should be called explicitly by the client, not auto-run in the
   constructor. Calling overridable methods from a constructor is dangerous because the
   subclass fields may not yet be initialised when the base constructor runs.
