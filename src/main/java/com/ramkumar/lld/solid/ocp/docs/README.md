# O — Open/Closed Principle (OCP)

> "Software entities (classes, modules, functions) should be **open for extension**
> but **closed for modification**."
> — Bertrand Meyer (1988), popularised by Robert C. Martin

In plain terms: **add new behaviour by writing new code, not by editing old code.**

---

## 1. What the Principle Really Means

| Term | What it means in practice |
|------|--------------------------|
| **Open for extension** | You can add new behaviour — new types, new algorithms, new output formats |
| **Closed for modification** | Existing, tested classes are not touched to support the new behaviour |

The goal: when requirements change, the blast radius is **zero** on existing code.

```
CLOSED class (never touched again)     OPEN point (new code added here)
┌──────────────────────────────┐       ┌──────────────────────────────┐
│  ReportService               │       │  <<interface>> ReportExporter│
│  + generate(exporter, data)  │───────│  + export(report): String    │
│  [no if/else; never changes] │       │  + getFormat(): String       │
└──────────────────────────────┘       └──────────────────────────────┘
                                                    ▲
                                       ┌────────────┼────────────┐
                                  PdfExporter  CsvExporter  HtmlExporter
                                  [existing]   [existing]   [NEW — added
                                                             without touching
                                                             any class above]
```

---

## 2. The OCP Violation Pattern — if/else / switch Chains

The most common OCP violation is a method that dispatches on a type string:

```java
// ❌ VIOLATION — every new type forces you to open and modify this method
public String generateReport(String type, Report report) {
    if (type.equals("PDF")) {
        return "--- PDF ---\n" + report.getTitle() + "\n" + report.getContent();
    } else if (type.equals("CSV")) {
        return "title,content\n" + report.getTitle() + "," + report.getContent();
    } else if (type.equals("HTML")) {
        return "<html><h1>" + report.getTitle() + "</h1><p>" + report.getContent() + "</p></html>";
    } else {
        throw new IllegalArgumentException("Unknown type: " + type);
    }
    // Adding JSON requires:  ← open this file, add else-if, retest the whole method
}
```

**What breaks when you add JSON?**
- You open `ReportGenerator` (closed for modification → violated)
- You retest PDF, CSV, HTML logic even though nothing changed there
- The class grows indefinitely with every new format
- You cannot add a format without access to the source of `ReportGenerator`

---

## 3. Before → After ASCII Diagram

```
BEFORE — OCP Violation
══════════════════════
ReportGenerator
  └── generateReport(type: String, report: Report): String
        ├── if "PDF"   → format as PDF
        ├── if "CSV"   → format as CSV
        ├── if "HTML"  → format as HTML
        └── else       → throw

        ↑ Adding JSON touches this class.
          Adding XML touches this class.
          Adding YAML touches this class.

AFTER — OCP Compliant
═════════════════════
<<interface>> ReportExporter
  + export(Report): String
  + getFormat(): String
         ▲
┌────────┼────────┬──────────┬─────────────────┐
PdfExport CsvExp HtmlExport JsonExporter  [NEW] XmlExporter [NEW]
         ↑ New formats are new classes.
           ReportService never changes.
           Existing exporters never change.
```

---

## 4. Enabling OCP — The Three Tools

### Tool 1: Interface as the extension point

```java
// The ABSTRACTION that is never modified
interface ReportExporter {
    String export(Report report);   // what to do
    String getFormat();             // identity (PDF, CSV, HTML…)
}

// EXTENSION — add without modifying anything above
class JsonExporter implements ReportExporter {
    @Override public String getFormat() { return "JSON"; }
    @Override public String export(Report report) {
        return "{ \"title\": \"" + report.getTitle() + "\", "
             + "\"content\": \"" + report.getContent() + "\" }";
    }
}
```

### Tool 2: Abstract class as partial implementation

Use when several exporters share boilerplate (e.g., logging, header writing):

```java
abstract class BaseExporter implements ReportExporter {
    @Override
    public final String export(Report report) {
        String header = writeHeader(report);   // shared
        String body   = writeBody(report);     // extension point (abstract)
        String footer = writeFooter();         // shared
        return header + body + footer;
    }
    // Template Method — subclasses only override what differs
    protected abstract String writeBody(Report report);
    protected String writeHeader(Report r) { return "=== " + r.getTitle() + " ===\n"; }
    protected String writeFooter()         { return "\n--- end ---"; }
}
```

### Tool 3: Registration / plugin pattern (no switch needed)

```java
class ReportService {
    private final Map<String, ReportExporter> exporters = new HashMap<>();

    // EXTENSION POINT — caller registers new exporters; service never changes
    public void register(ReportExporter exporter) {
        exporters.put(exporter.getFormat(), exporter);
    }

    public String generate(String format, Report report) {
        ReportExporter exporter = exporters.get(format);
        if (exporter == null)
            throw new IllegalArgumentException("No exporter registered for: " + format);
        return exporter.export(report);   // polymorphic dispatch — no if/else
    }
}
```

---

## 5. OCP and the Strategy Pattern

OCP is the **principle**; Strategy is the most common **pattern** that implements it.

```
Strategy Pattern Structure        Maps to OCP like this
─────────────────────────         ─────────────────────
Context                    →      The class that is CLOSED
Strategy (interface)       →      The extension point abstraction
ConcreteStrategyA          →      Existing behaviour (never modified)
ConcreteStrategyB          →      Existing behaviour (never modified)
ConcreteStrategyC  [NEW]   →      New behaviour (new class, zero modification)
```

When you hear "add a new algorithm / format / rule without touching existing code" — you are implementing OCP via the Strategy pattern.

---

## 6. OCP and Other SOLID Principles

OCP works in partnership with the others:

| Principle | How it supports OCP |
|-----------|---------------------|
| **SRP** | Splitting classes by responsibility creates natural extension points. A god class cannot be closed. |
| **LSP** | New subclasses that violate LSP break the closed code that depends on the abstraction. |
| **ISP** | Thin interfaces make extension easier — new implementations only need to cover relevant methods. |
| **DIP** | Depending on abstractions (not concretions) is what makes the context class closed. |

---

## 7. When NOT to Apply OCP

OCP costs: more classes, more files, more indirection. Apply it only at **stable variation points** — places where new types are genuinely expected to arrive.

```
APPLY OCP when:                         SKIP OCP when:
• adding new types is a known pattern   • the set of types is fixed (e.g., 2 booleans)
• you cannot anticipate which types     • premature: you have 2 cases today, may never add more
• the dispatch logic already exists     • the logic is trivial (1-line if/else is fine)
  and has been modified twice before
```

The rule of thumb: **first time → write the if/else. Second new type → refactor to OCP.**

---

## 8. Interview Questions

**Q1: What does "closed for modification" mean in practice?**
> It means once a class is tested and deployed, you should not have to open its file to accommodate new features. New behaviour is added via new classes that implement an existing abstraction (interface or abstract class).

**Q2: How do you identify an OCP violation?**
> Three signals: (1) a method has an if/else or switch on a type field, (2) every time a new type is introduced you must edit existing code, (3) the class name ends in "Processor" or "Manager" and it keeps growing.

**Q3: What is the relationship between OCP and the Strategy pattern?**
> Strategy is the primary design pattern that makes OCP concrete: the Context class is closed, the Strategy interface is the extension point, and each ConcreteStrategy adds behaviour without modifying the Context or any existing strategy.

**Q4: Does OCP mean you can never modify a class?**
> No — OCP applies to behaviour that varies. Bug fixes, performance improvements, or adding a method to an interface are legitimate modifications. OCP specifically targets the pattern of "add a new type → open an existing class to add an if/else branch."

**Q5: How does DIP enable OCP?**
> DIP says high-level modules should depend on abstractions, not concretions. If `ReportService` depended on `PdfExporter` directly, it could not be closed — adding `CsvExporter` would require modifying `ReportService`. By depending on the `ReportExporter` interface, `ReportService` is closed regardless of how many concrete exporters exist.

**Q6: Can an abstract class be used instead of an interface for OCP?**
> Yes — and it is often better when extensions share significant boilerplate. The Template Method pattern (abstract class + abstract hook methods) is OCP implemented via abstract classes. Use an interface when extensions are fully independent; use an abstract class when they share common steps.

**Q7: What is the "plugin pattern" in the context of OCP?**
> The plugin pattern is when the closed class holds a registry (e.g., `Map<String, Strategy>`) and callers register new strategies at runtime. The class never needs a recompile to support a new type — a new implementation is written, instantiated, and registered. Many frameworks (Spring, Java SPI) are built on this pattern.

---

## 9. Common Mistakes

```java
// MISTAKE 1: Protecting the wrong thing — making the extension point closed
final class PdfExporter implements ReportExporter {  // ← final is fine here
    @Override public String export(Report r) { ... } // you can't extend a concrete impl anyway
}

// MISTAKE 2: Using instanceof instead of polymorphism
public String export(ReportExporter exporter, Report report) {
    if (exporter instanceof PdfExporter) { ... }   // ← OCP violation hiding behind a method
    else if (exporter instanceof CsvExporter) { ... }
}
// Fix: call exporter.export(report) — that's what polymorphism is for

// MISTAKE 3: Over-engineering from day one
// If you have ONE format today, an interface is premature.
// Write the simple version first; add the interface when the second type arrives.

// MISTAKE 4: Putting the if/else in a different class (still violates OCP)
class ReportController {
    public void handle(String type) {
        if (type.equals("PDF"))  new PdfExporter().export(report);   // ← same smell, different file
        else if ...
    }
}
// Fix: the dispatch must be polymorphic, not conditional, anywhere in the call chain.

// MISTAKE 5: Making the interface too fat
interface ReportExporter {
    String export(Report report);
    void   printPreview();          // ← not every exporter supports preview
    byte[] toBytes();               // ← not every format needs bytes
}
// OCP + ISP together: keep the interface narrow so every implementation is natural
```
