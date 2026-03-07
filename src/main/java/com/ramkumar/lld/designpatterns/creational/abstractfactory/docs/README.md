# 3.3 — Abstract Factory Pattern (Creational)

> "Provide an interface for creating families of related or dependent objects
>  without specifying their concrete classes."
> — Gang of Four (GoF)

---

## 1. The Problem Abstract Factory Solves

Your app must support two UI themes: Light and Dark. Each theme has its own Button, TextBox,
and Dialog. Without Abstract Factory, mixing components from different themes is easy:

```java
// Danger: mixing theme components — inconsistent UI
Button btn      = new LightButton("OK");      // Light
TextBox input   = new DarkTextBox("email");   // Dark ← mismatch!
Dialog confirm  = new LightDialog("Action");  // Light
```

**With Abstract Factory**, the factory guarantees all components belong to the same family:

```java
UIThemeFactory factory = new DarkThemeFactory();
Button btn    = factory.createButton("OK");      // Dark — guaranteed
TextBox input = factory.createTextBox("email");  // Dark — guaranteed
Dialog dialog = factory.createDialog("Action");  // Dark — guaranteed
```

Switching themes is one line: `new DarkThemeFactory()` → `new LightThemeFactory()`.

---

## 2. Structure — The Grid View

The key insight: Abstract Factory is a **2D grid** — families (rows) × product types (columns).

```
                  «interface»      «interface»      «interface»
                  ProductA         ProductB         ProductC
                 ─────────────    ─────────────    ─────────────
                  + operation()    + operation()    + operation()
                       △                △                △
                       │                │                │
«interface»            │                │                │
AbstractFactory  ConcreteA1       ConcreteB1       ConcreteC1    ← Family 1
─────────────
+ createA()      ConcreteA2       ConcreteB2       ConcreteC2    ← Family 2
+ createB()
+ createC()      ConcreteA3       ConcreteB3       ConcreteC3    ← Family 3
     △
     │
ConcreteFactory1 → creates ConcreteA1, ConcreteB1, ConcreteC1  (Family 1)
ConcreteFactory2 → creates ConcreteA2, ConcreteB2, ConcreteC2  (Family 2)
ConcreteFactory3 → creates ConcreteA3, ConcreteB3, ConcreteC3  (Family 3)
```

**The five participants:**

| Role | Responsibility |
|---|---|
| **AbstractProduct** (×N) | One interface per product type (Button, TextBox, Dialog) |
| **ConcreteProduct** (N×M) | Specific implementation for one family (LightButton, DarkButton) |
| **AbstractFactory** | Interface declaring a factory method per product type |
| **ConcreteFactory** | Implements AbstractFactory; creates one consistent product family |
| **Client** | Uses only AbstractFactory and AbstractProduct — never concrete types |

---

## 3. Abstract Factory vs Factory Method

| | Factory Method | Abstract Factory |
|---|---|---|
| Creates | **One** product type | **Family** of related products |
| Mechanism | Subclass overrides one factory method | Separate factory object injected into client |
| Uses | Inheritance | Composition |
| Add new family | Add new ConcreteCreator | Add new ConcreteFactory |
| Add new product type | Easy — add to one class | Hard — must change AbstractFactory interface |
| Classic example | Logistics: `createTransport()` | UI kit: `createButton()` + `createDialog()` + `createTextBox()` |

**Abstract Factory is often built from Factory Methods**: each method in the ConcreteFactory
is a Factory Method that creates one product type.

---

## 4. Code Skeleton

```java
// ── Abstract Products ─────────────────────────────────────────────────────
interface Button  { void render(); String getStyle(); }
interface TextBox { void render(); String getStyle(); }

// ── Abstract Factory ──────────────────────────────────────────────────────
interface UIFactory {
    Button  createButton(String label);
    TextBox createTextBox(String placeholder);
}

// ── Concrete Products — Family 1 (Light) ──────────────────────────────────
class LightButton  implements Button  { /* white bg */ }
class LightTextBox implements TextBox { /* white bg */ }

// ── Concrete Products — Family 2 (Dark) ───────────────────────────────────
class DarkButton  implements Button  { /* dark bg */ }
class DarkTextBox implements TextBox { /* dark bg */ }

// ── Concrete Factories ────────────────────────────────────────────────────
class LightFactory implements UIFactory {
    public Button  createButton(String l)  { return new LightButton(l); }
    public TextBox createTextBox(String p) { return new LightTextBox(p); }
}
class DarkFactory implements UIFactory {
    public Button  createButton(String l)  { return new DarkButton(l); }
    public TextBox createTextBox(String p) { return new DarkTextBox(p); }
}

// ── Client ────────────────────────────────────────────────────────────────
class App {
    private final Button  btn;
    private final TextBox input;

    App(UIFactory factory) {              // factory injected — client is decoupled
        btn   = factory.createButton("OK");
        input = factory.createTextBox("email");
    }

    void render() { btn.render(); input.render(); }
}

// ── Usage ─────────────────────────────────────────────────────────────────
App lightApp = new App(new LightFactory());  // all Light
App darkApp  = new App(new DarkFactory());   // all Dark
```

---

## 5. Adding a New Family vs Adding a New Product Type

**Adding a new family (easy — OCP-compliant):**
```java
// Add HighContrastFactory + HighContrastButton + HighContrastTextBox
// ZERO changes to existing classes
class HighContrastFactory implements UIFactory {
    public Button  createButton(String l)  { return new HighContrastButton(l); }
    public TextBox createTextBox(String p) { return new HighContrastTextBox(p); }
}
```

**Adding a new product type (hard — violates OCP):**
```java
// Must add createDialog() to UIFactory interface
// EVERY existing ConcreteFactory must also implement createDialog()
// LightFactory, DarkFactory, HighContrastFactory — all change
interface UIFactory {
    Button  createButton(String label);
    TextBox createTextBox(String placeholder);
    Dialog  createDialog(String title);    // ← new method — all factories must implement
}
```

This is the main trade-off: Abstract Factory is extensible along the family axis but rigid along
the product-type axis.

---

## 6. When to Use / Not Use

**Use when:**
- Products must work together in consistent families (UI themes, cloud providers, DB drivers)
- You want to swap entire product families at configuration time
- You have N families × M product types and want to eliminate N×M switch statements

**Do NOT use when:**
- You only have one product type → Factory Method is sufficient
- Product types change frequently → Abstract Factory interface changes are expensive
- Families have only one variant → just use the concrete class directly

---

## 7. Interview Q&A

**Q1: What is Abstract Factory and how does it differ from Factory Method?**
A: Abstract Factory creates *families* of related objects through a single interface with
   multiple factory methods (one per product type). Factory Method creates one product type
   and defers which concrete type to a subclass. Abstract Factory uses composition (inject
   a factory object); Factory Method uses inheritance (subclass the creator). Use Abstract
   Factory when products must be used together and must belong to the same family.

**Q2: What is the "family consistency guarantee"?**
A: Since all products are created by the same ConcreteFactory, they are guaranteed to be
   compatible. A `LightThemeFactory` can only produce Light-family components — the compiler
   prevents mixing themes at the factory level. This is the core value proposition: the
   factory is the contract that all products in one group are designed to work together.

**Q3: When does adding to Abstract Factory violate OCP?**
A: When you add a new product type. Adding `createSpinner()` to `UIFactory` forces every
   existing ConcreteFactory to implement `createSpinner()`. Adding a new *family* (new
   ConcreteFactory) is OCP-compliant — no existing code changes.

**Q4: How is Abstract Factory implemented in the Java standard library?**
A: `javax.xml.parsers.DocumentBuilderFactory` — `newDocumentBuilder()` creates parser
   components. `java.sql.Connection` — `createStatement()`, `prepareStatement()`, and
   `prepareCall()` together are an abstract factory for SQL command objects.
   `java.awt.Toolkit` creates platform-specific (Windows/Mac/Linux) AWT component families.

**Q5: Should the AbstractFactory interface be an interface or abstract class?**
A: Prefer an interface — it allows ConcreteFactories to extend other classes if needed,
   and keeps the contract minimal. Use an abstract class only if you need shared
   implementation across factories (e.g., logging every factory call centrally).

**Q6: How do you select which ConcreteFactory to use at runtime?**
A: Common approaches: (1) Read from a config file (`aws`, `azure`, `gcp` → select factory),
   (2) Environment variable (`APP_THEME=dark`), (3) Dependency Injection framework (Spring
   @Profile or @Qualifier). The key: the selection happens *once* at startup/wiring time;
   the client code never sees the switch.

**Q7: Is Abstract Factory the same as a Registry of Factories?**
A: No. A Registry of Factories is a map from string keys to factory instances — it is
   a way to *configure* which factory to use. Abstract Factory is the pattern that defines
   the interface for creating product families. They are complementary — you can use a
   Registry to look up which AbstractFactory to inject.

---

## 8. Common Mistakes

| Mistake | Why it's wrong | Fix |
|---|---|---|
| Referencing ConcreteProducts in the Client | Client couples to implementation, defeating the pattern | Client uses only AbstractProduct interfaces |
| Making factory methods `static` | Prevents polymorphic dispatch — factories can't be swapped | Instance methods only |
| Putting business logic in the factory methods | Factories should only create, not process | Delegate logic to the created product or a service |
| Using Abstract Factory when only one product type exists | Over-engineering | Use Factory Method or Simple Factory |
| Exposing factory internals (e.g., `getConcreteFactoryType()`) | Re-couples the client to the family decision | Client only needs AbstractFactory methods |
| Sharing mutable state between products of different families | Breaks the consistency guarantee | Each ConcreteProduct is independent |
