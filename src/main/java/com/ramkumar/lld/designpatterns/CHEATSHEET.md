# Design Patterns Cheatsheet

A quick-reference guide for all 20 GoF patterns practiced in this project.

---

## How to Read This Cheatsheet

Each pattern entry has:
- **Intent** — what problem it solves (one sentence)
- **Key Participants** — the roles in the pattern
- **Structure** — ASCII diagram of the relationships
- **Code Skeleton** — minimal Java snippet showing the pattern's shape
- **When to Use** — bullet list of trigger conditions
- **Watch Out** — common mistakes

---

## Creational Patterns

> **Theme:** Control *how* objects are created — hide construction logic from the client.

### 1. Singleton

**Intent:** Ensure exactly one instance exists for the JVM lifetime; provide a global access point.

**Participants:** `Singleton`

**Structure:**
```
Singleton
├── - instance : Singleton       (static, private)
├── - Singleton()                (private constructor)
└── + getInstance() : Singleton  (static, synchronized/holder)
```

**Code Skeleton:**
```java
public class AppConfig {
    private static final AppConfig INSTANCE = new AppConfig();  // eager
    private AppConfig() {}
    public static AppConfig getInstance() { return INSTANCE; }
}
```

**When to Use:**
- Shared resource that must be unique (config, connection pool, registry)
- Expensive-to-create object reused everywhere

**Watch Out:**
- Eager init is simplest and thread-safe; double-checked locking is error-prone
- Makes unit testing harder (global state) — consider dependency injection instead
- `enum` singleton is the safest form (prevents reflection/serialization attacks)

---

### 2. Factory Method

**Intent:** Define an interface for creating objects, but let subclasses decide which concrete class to instantiate.

**Participants:** `Creator` (abstract), `ConcreteCreator`, `Product` (interface), `ConcreteProduct`

**Structure:**
```
        Creator                     Product
  ┌──────────────────┐       ┌──────────────────┐
  │ + createProduct() │──────▶│ + operation()    │
  └────────▲─────────┘       └────────▲─────────┘
           │                          │
  ConcreteCreatorA           ConcreteProductA
  ConcreteCreatorB           ConcreteProductB
```

**Code Skeleton:**
```java
interface Notification { void send(String msg); }

abstract class NotificationFactory {
    abstract Notification createNotification();       // factory method
    void notify(String msg) { createNotification().send(msg); }
}

class EmailFactory extends NotificationFactory {
    Notification createNotification() { return new EmailNotification(); }
}
```

**When to Use:**
- Class doesn't know in advance which concrete object it needs
- Subclasses should control the creation decision
- "new ConcreteClass()" scattered across client code — centralize it

**Watch Out:**
- Factory method is about *inheritance* (subclass overrides); don't confuse with Simple Factory (static method)
- Each new product type requires a new creator subclass

---

### 3. Abstract Factory

**Intent:** Create *families* of related objects without specifying their concrete classes.

**Participants:** `AbstractFactory`, `ConcreteFactory`, `AbstractProduct` (one per product type), `ConcreteProduct`

**Structure:**
```
        AbstractFactory
  ┌──────────────────────────┐
  │ + createButton() : Button │
  │ + createDialog() : Dialog │
  └──────────▲───────────────┘
             │
  ┌──────────┴──────────┐
  LightThemeFactory    DarkThemeFactory
  (creates LightBtn,   (creates DarkBtn,
   LightDialog)         DarkDialog)
```

**Code Skeleton:**
```java
interface UIFactory {
    Button createButton();
    Dialog createDialog();
}

class DarkThemeFactory implements UIFactory {
    public Button createButton() { return new DarkButton(); }
    public Dialog createDialog() { return new DarkDialog(); }
}
```

**When to Use:**
- System needs to work with multiple families of related products
- You want to enforce that products from the same family are used together
- Swapping the entire family at once (e.g., theme, OS platform, database vendor)

**Watch Out:**
- Adding a new product type (e.g., `createCheckbox()`) forces changes to every factory
- Abstract Factory creates *families*; Factory Method creates *one product*

---

### 4. Builder

**Intent:** Separate construction of a complex object from its representation; same process, different configurations.

**Participants:** `Builder` (static inner class), `Product` (outer class with private constructor)

**Structure:**
```
Product                          Builder
├── - field1 (private final)     ├── - field1
├── - field2 (private final)     ├── - field2
├── - Product(Builder b)         ├── + field1(val) : Builder  ← returns this
│    (private constructor)       ├── + field2(val) : Builder
└── + getField1()                └── + build() : Product
```

**Code Skeleton:**
```java
class HttpRequest {
    private final String url;
    private final int timeout;

    private HttpRequest(Builder b) { this.url = b.url; this.timeout = b.timeout; }

    static class Builder {
        private String url;           // required
        private int timeout = 30_000; // optional with default

        Builder(String url) { this.url = url; }
        Builder timeout(int t) { this.timeout = t; return this; }
        HttpRequest build() {
            if (url == null) throw new IllegalStateException("url required");
            return new HttpRequest(this);
        }
    }
}
// Usage: new HttpRequest.Builder("https://...").timeout(5000).build();
```

**When to Use:**
- Object has many optional parameters (telescoping constructor problem)
- Object must be immutable after construction
- Construction requires validation before the object is usable

**Watch Out:**
- Product constructor must be `private` — only `Builder.build()` calls it
- Builder fields mirror Product fields but are mutable; Product fields are `final`
- `build()` is the validation gate — check required fields here

---

### 5. Prototype

**Intent:** Create new objects by cloning an existing prototype instead of constructing from scratch.

**Participants:** `Prototype` (interface with `clone()`), `ConcretePrototype`, `Client`

**Structure:**
```
  Prototype
  ┌──────────────────┐
  │ + clone() : self  │
  └────────▲─────────┘
           │
  ConcretePrototype
  ┌──────────────────┐
  │ + clone()         │ ← deep-copies all mutable fields
  └──────────────────┘
```

**Code Skeleton:**
```java
abstract class Document {
    abstract Document clone();
}

class Report extends Document {
    private List<String> sections;

    Report clone() {
        Report copy = new Report();
        copy.sections = new ArrayList<>(this.sections);  // deep copy
        return copy;
    }
}
```

**When to Use:**
- Creating an object is expensive (DB query, network call, heavy computation)
- You need copies that start from a known good configuration (templates)
- Runtime registration of new types (prototype registry)

**Watch Out:**
- **Deep copy vs shallow copy** — mutable fields (lists, maps, nested objects) must be deep-copied
- Avoid `Object.clone()` — define your own `clone()` method on your interface
- Circular references make deep copy tricky

---

## Structural Patterns

> **Theme:** Control *how* objects are composed — assemble objects into larger structures.

### 6. Adapter

**Intent:** Convert an incompatible interface into the interface the client expects.

**Participants:** `Target` (interface), `Adaptee` (legacy class), `Adapter` (implements Target, wraps Adaptee)

**Structure:**
```
Client ──▶ Target (interface)
                ▲
                │
           Adapter ──composition──▶ Adaptee
           │ + targetMethod()  │    │ + legacyMethod() │
           │   { adaptee       │    └──────────────────┘
           │     .legacyMethod()│
           │   }               │
           └───────────────────┘
```

**Code Skeleton:**
```java
interface TemperatureSensor { double getCelsius(); }

class LegacyFahrenheitSensor { double readTempF() { return 98.6; } }

class FahrenheitAdapter implements TemperatureSensor {
    private final LegacyFahrenheitSensor sensor;  // composition
    FahrenheitAdapter(LegacyFahrenheitSensor s) { this.sensor = s; }
    public double getCelsius() { return (sensor.readTempF() - 32) * 5.0 / 9.0; }
}
```

**When to Use:**
- Integrate a third-party or legacy class whose interface doesn't match yours
- You cannot modify the adaptee (closed source, library, legacy)
- You need multiple adapters for different adaptees behind one target interface

**Watch Out:**
- Prefer **object adapter** (composition) over class adapter (inheritance) — Java has single inheritance
- Adapter only translates; it should NOT contain business logic
- Don't modify the adaptee — if you can modify it, you don't need an adapter

---

### 7. Facade

**Intent:** Provide a simplified, unified interface to a complex subsystem.

**Participants:** `Facade`, `SubsystemClass1`, `SubsystemClass2`, ...

**Structure:**
```
Client ──▶ Facade
           ├── subsystem1
           ├── subsystem2
           └── subsystem3
           │
           │ + simpleOperation()
           │   { sub1.step1();
           │     sub2.step2();
           │     sub3.step3(); }
```

**Code Skeleton:**
```java
class HomeTheaterFacade {
    private final Projector projector;
    private final SoundSystem sound;
    private final Screen screen;

    void watchMovie(String title) {
        screen.down();
        projector.on();
        sound.setVolume(8);
        projector.play(title);
    }
}
```

**When to Use:**
- Client needs to call multiple subsystem classes in a specific order
- You want to hide subsystem complexity behind a single entry point
- Subsystem classes should still be accessible directly for advanced use

**Watch Out:**
- Facade should NOT become a "god class" — it orchestrates, not implements
- Facade doesn't add new functionality — it simplifies existing functionality
- Don't confuse with Adapter (translates interface) or Mediator (coordinates peers)

---

### 8. Decorator

**Intent:** Attach additional responsibilities to an object dynamically; stackable wrappers.

**Participants:** `Component` (interface), `ConcreteComponent`, `Decorator` (abstract, implements Component, wraps Component), `ConcreteDecorator`

**Structure:**
```
Component (interface)
├── ConcreteComponent
└── Decorator (abstract)
    ├── - wrapped : Component       ← composition
    ├── + operation() { wrapped.operation(); + extra }
    ├── BoldDecorator
    ├── ItalicDecorator
    └── UnderlineDecorator
```

**Code Skeleton:**
```java
interface TextComponent { String format(String text); }

class PlainText implements TextComponent {
    public String format(String text) { return text; }
}

abstract class TextDecorator implements TextComponent {
    protected final TextComponent wrapped;
    TextDecorator(TextComponent c) { this.wrapped = c; }
}

class BoldDecorator extends TextDecorator {
    BoldDecorator(TextComponent c) { super(c); }
    public String format(String text) { return "<b>" + wrapped.format(text) + "</b>"; }
}

// Usage: new BoldDecorator(new ItalicDecorator(new PlainText())).format("hi")
// Result: "<b><i>hi</i></b>"
```

**When to Use:**
- Add behavior to individual objects without affecting others of the same class
- Behaviors can be combined in any order (stackable)
- Subclassing would cause a combinatorial explosion (bold+italic+underline = 7 subclasses)

**Watch Out:**
- Decorator and Component share the same interface — decorator IS-A component
- Order of wrapping matters: `Bold(Italic(text))` differs from `Italic(Bold(text))`
- Don't confuse with Proxy (controls access, same interface) or Adapter (converts interface)

---

### 9. Proxy

**Intent:** Provide a surrogate or placeholder that controls access to another object.

**Participants:** `Subject` (interface), `RealSubject`, `Proxy` (implements Subject, wraps RealSubject)

**Structure:**
```
Client ──▶ Subject (interface)
           ├── RealSubject        (expensive/restricted)
           └── Proxy              (controls access)
               ├── - real : RealSubject
               └── + operation()
                     { /* lazy-load, auth check, cache, log */
                       real.operation(); }
```

**Code Skeleton:**
```java
interface Image { void display(); }

class RealImage implements Image {
    RealImage(String file) { loadFromDisk(file); }  // expensive
    public void display() { System.out.println("Displaying"); }
}

class ProxyImage implements Image {
    private RealImage real;
    private final String file;
    ProxyImage(String file) { this.file = file; }  // cheap
    public void display() {
        if (real == null) real = new RealImage(file);  // lazy
        real.display();
    }
}
```

**When to Use:**
- Lazy initialization (virtual proxy) — defer expensive creation until first use
- Access control (protection proxy) — check permissions before delegating
- Caching (caching proxy) — return cached results for repeated calls
- Logging/monitoring — wrap calls with metrics

**Watch Out:**
- Proxy and RealSubject share the same interface — client doesn't know which it's using
- Don't confuse with Decorator (adds behavior) — Proxy controls access
- Proxy manages the lifecycle of the real object; Decorator doesn't

---

### 10. Composite

**Intent:** Compose objects into tree structures; clients treat individual objects and compositions uniformly.

**Participants:** `Component` (interface), `Leaf`, `Composite` (holds `List<Component>`)

**Structure:**
```
Component (interface)
├── + operation()
├── Leaf                     (no children)
│   └── + operation()
└── Composite                (has children)
    ├── - children : List<Component>
    ├── + add(Component)
    ├── + remove(Component)
    └── + operation()        { for child : children → child.operation() }
```

**Code Skeleton:**
```java
interface FileSystemEntry { long getSize(); }

class File implements FileSystemEntry {
    private final long size;
    public long getSize() { return size; }
}

class Directory implements FileSystemEntry {
    private final List<FileSystemEntry> children = new ArrayList<>();
    void add(FileSystemEntry e) { children.add(e); }
    public long getSize() {
        return children.stream().mapToLong(FileSystemEntry::getSize).sum();
    }
}
```

**When to Use:**
- Part-whole hierarchies (file systems, org charts, UI components, menus)
- Client code should treat leaves and composites identically
- Recursive structures where the operation "rolls up" from leaves to root

**Watch Out:**
- Leaf and Composite both implement the same interface — that's the whole point
- `add()`/`remove()` on Leaf can throw `UnsupportedOperationException` or just not exist on the interface
- Don't force `add()`/`remove()` onto Component if only Composite needs them

---

### 11. Bridge

**Intent:** Decouple an abstraction from its implementation so both can vary independently.

**Participants:** `Abstraction`, `RefinedAbstraction`, `Implementor` (interface), `ConcreteImplementor`

**Structure:**
```
Abstraction ───has-a──▶ Implementor (interface)
├── RefinedAbstraction1    ├── ConcreteImplementorA
└── RefinedAbstraction2    └── ConcreteImplementorB

    2 abstractions x 2 implementors = 4 combinations
    (without Bridge: 4 separate classes)
```

**Code Skeleton:**
```java
interface Color { String fill(); }
class Red implements Color { public String fill() { return "Red"; } }

abstract class Shape {
    protected final Color color;           // ← the bridge
    Shape(Color color) { this.color = color; }
    abstract String draw();
}

class Circle extends Shape {
    Circle(Color c) { super(c); }
    String draw() { return "Circle in " + color.fill(); }
}
// Circle+Red, Circle+Blue, Square+Red, Square+Blue — no class explosion
```

**When to Use:**
- Two independent dimensions of variation (shape+color, device+platform, message+channel)
- Subclass explosion: M abstractions x N implementations = M*N classes without Bridge
- Want to switch implementations at runtime

**Watch Out:**
- Bridge separates *what* (abstraction) from *how* (implementation)
- Don't confuse with Adapter (makes incompatible interfaces work) — Bridge is designed upfront
- Don't confuse with Strategy (swaps algorithm) — Bridge separates a permanent structural dimension

---

### 12. Flyweight

**Intent:** Share intrinsic state across many objects to reduce memory usage.

**Participants:** `FlyweightFactory`, `Flyweight` (shared intrinsic state), `Client` (supplies extrinsic state)

**Structure:**
```
FlyweightFactory
├── - cache : Map<Key, Flyweight>
└── + getFlyweight(key) : Flyweight
    { return cache.computeIfAbsent(key, Flyweight::new); }

Flyweight
├── intrinsic state (shared, immutable)   ← stored inside
└── + operation(extrinsicState)           ← passed as parameter

Client
└── holds extrinsic state (position, context) per instance
```

**Code Skeleton:**
```java
class TreeType {  // flyweight — shared
    final String name, color, texture;     // intrinsic (immutable)
}

class Tree {      // client — per instance
    int x, y;                              // extrinsic (unique per tree)
    TreeType type;                         // reference to shared flyweight
}

class TreeFactory {
    static Map<String, TreeType> cache = new HashMap<>();
    static TreeType get(String name, String color, String texture) {
        return cache.computeIfAbsent(name, k -> new TreeType(name, color, texture));
    }
}
```

**When to Use:**
- Thousands/millions of similar objects consuming too much memory
- Objects share significant common state (sprite images, font glyphs, particle types)
- Shared state is immutable

**Watch Out:**
- Intrinsic state must be **immutable** — if shared objects mutate, all users see the change
- Extrinsic state is passed as a parameter, never stored in the flyweight
- Trade-off: saves memory but adds complexity (factory, parameter passing)

---

## Behavioral Patterns

> **Theme:** Control *how* objects communicate — define interaction and responsibility patterns.

### 13. Strategy

**Intent:** Define a family of algorithms, encapsulate each, and make them interchangeable at runtime.

**Participants:** `Context`, `Strategy` (interface), `ConcreteStrategy`

**Structure:**
```
Context ───has-a──▶ Strategy (interface)
                    ├── ConcreteStrategyA
                    ├── ConcreteStrategyB
                    └── ConcreteStrategyC

Context.execute() → strategy.algorithm()
```

**Code Skeleton:**
```java
interface SortStrategy { void sort(int[] data); }

class BubbleSort implements SortStrategy {
    public void sort(int[] data) { /* bubble sort */ }
}

class Sorter {
    private SortStrategy strategy;
    void setStrategy(SortStrategy s) { this.strategy = s; }
    void sort(int[] data) { strategy.sort(data); }
}
// Usage: sorter.setStrategy(new QuickSort()); sorter.sort(data);
```

**When to Use:**
- Multiple algorithms for the same task, selected at runtime
- Eliminate `if/else` or `switch` chains that select behavior
- Algorithm needs to be swapped without changing client code

**Watch Out:**
- Strategy is set by the client — context doesn't choose
- All strategies must share the same interface
- Don't confuse with State (transitions are internal) — Strategy is client-driven

---

### 14. Observer

**Intent:** One-to-many notification — when a subject changes state, all observers are notified automatically.

**Participants:** `Subject`, `Observer` (interface), `ConcreteObserver`

**Structure:**
```
Subject
├── - observers : List<Observer>
├── + subscribe(Observer)
├── + unsubscribe(Observer)
└── + notifyObservers()
    { for o : observers → o.update(data) }

Observer (interface)
├── + update(data)
├── PhoneDisplay
├── WebDashboard
└── AlertService
```

**Code Skeleton:**
```java
interface Observer { void update(String stock, double price); }

class StockTicker {
    private List<Observer> observers = new ArrayList<>();
    void subscribe(Observer o) { observers.add(o); }
    void setPrice(String stock, double price) {
        observers.forEach(o -> o.update(stock, price));
    }
}
```

**When to Use:**
- One object's state change should trigger updates in multiple other objects
- The set of dependent objects can change at runtime (subscribe/unsubscribe)
- Publisher shouldn't know the concrete types of its subscribers

**Watch Out:**
- Subject holds `List<Observer>` — never `List<ConcreteObserver>`
- Observer receives data via `update()` — it never polls the subject
- Don't confuse with Mediator (many-to-many) — Observer is one-to-many

---

### 15. Command

**Intent:** Encapsulate a request as an object, enabling undo/redo, queuing, and logging.

**Participants:** `Command` (interface), `ConcreteCommand`, `Invoker`, `Receiver`

**Structure:**
```
Invoker ──has-a──▶ Command (interface)
                   ├── + execute()
                   ├── + undo()
                   │
                   ConcreteCommand
                   ├── - receiver : Receiver
                   ├── - previousState        ← for undo
                   ├── + execute() { save state; receiver.action(); }
                   └── + undo()   { receiver.restore(previousState); }
```

**Code Skeleton:**
```java
interface Command { void execute(); void undo(); }

class TypeCommand implements Command {
    private final TextEditor editor;      // receiver
    private final String text;
    private int prevCursorPos;            // for undo

    public void execute() {
        prevCursorPos = editor.getCursor();
        editor.insert(text);
    }
    public void undo() { editor.delete(text.length()); }
}

class EditorInvoker {
    private Deque<Command> history = new ArrayDeque<>();
    void execute(Command cmd) { cmd.execute(); history.push(cmd); }
    void undo() { if (!history.isEmpty()) history.pop().undo(); }
}
```

**When to Use:**
- Need undo/redo functionality
- Need to queue, schedule, or log operations
- Need to parameterize objects with operations (callback objects)
- Decouple "what is requested" from "who executes it"

**Watch Out:**
- Command stores the state needed for undo — capture it in `execute()`, before the action
- Invoker manages the history stack — it doesn't know what the command does
- Don't confuse with Strategy (swaps algorithms) — Command encapsulates a request with undo

---

### 16. Template Method

**Intent:** Define the skeleton of an algorithm in a base class; let subclasses override specific steps.

**Participants:** `AbstractClass` (template method + abstract steps), `ConcreteClass` (implements steps)

**Structure:**
```
AbstractClass
├── + templateMethod()         ← final — defines the algorithm skeleton
│   { step1(); step2(); step3(); }
├── # step1()                  ← abstract — subclass provides
├── # step2()                  ← abstract — subclass provides
└── # step3()                  ← hook — optional override, has default

ConcreteClassA               ConcreteClassB
├── # step1() { ... }        ├── # step1() { ... }
├── # step2() { ... }        ├── # step2() { ... }
└── # step3() { ... }        └── (inherits default step3)
```

**Code Skeleton:**
```java
abstract class DataExporter {
    final void export(List<String> data) {   // template method
        openFile();
        writeHeader();
        for (String row : data) writeRow(row);
        closeFile();
    }
    abstract void openFile();
    abstract void writeHeader();
    abstract void writeRow(String row);
    void closeFile() { /* default hook */ }
}

class CsvExporter extends DataExporter {
    void openFile() { /* open .csv */ }
    void writeHeader() { /* CSV header */ }
    void writeRow(String row) { /* CSV row */ }
}
```

**When to Use:**
- Multiple classes share the same algorithm structure but differ in specific steps
- You want to enforce an algorithm's step order (steps can't be skipped or reordered)
- "Don't call us, we'll call you" — framework calls subclass methods, not the other way around

**Watch Out:**
- Template method should be `final` — subclasses override steps, not the skeleton
- Abstract steps = mandatory; hook methods = optional (provide default implementation)
- Don't confuse with Strategy (composition, runtime swap) — Template Method uses inheritance

---

### 17. Iterator

**Intent:** Access elements of a collection sequentially without exposing its internal structure.

**Participants:** `Iterable<E>` (factory), `Iterator<E>` (cursor), `ConcreteIterator` (inner class)

**Structure:**
```
Iterable<E>                    Iterator<E>
├── + iterator() : Iterator<E>  ├── + hasNext() : boolean
│                                ├── + next() : E
│                                └── (inner class — accesses collection's private fields)
│
ConcreteCollection implements Iterable<E>
└── ConcreteIterator implements Iterator<E>
```

**Code Skeleton:**
```java
class BookShelf implements Iterable<Book> {
    private Book[] books;
    private int count;

    public Iterator<Book> iterator() { return new BookIterator(); }

    private class BookIterator implements Iterator<Book> {
        private int cursor = 0;
        public boolean hasNext() { return cursor < count; }
        public Book next() {
            if (!hasNext()) throw new NoSuchElementException();
            return books[cursor++];
        }
    }
}
// Usage: for (Book b : shelf) { ... }  // works because of Iterable
```

**When to Use:**
- Hide internal data structure (array, linked list, tree) from clients
- Support multiple traversal strategies (all items, filtered, reverse)
- Enable `for-each` loop support by implementing `Iterable<E>`

**Watch Out:**
- `hasNext()` must **never** advance the cursor — it's a pure query
- `next()` must throw `NoSuchElementException`, not return `null`
- Iterator is typically a private inner class with access to the collection's fields
- Each `iterator()` call returns a **new** instance — independent traversal

---

### 18. Chain of Responsibility

**Intent:** Pass a request along a chain of handlers; each handler decides to process or forward.

**Participants:** `Handler` (abstract), `ConcreteHandler`, `Client`

**Structure:**
```
Client ──▶ Handler (abstract)
           ├── - next : Handler
           ├── + setNext(Handler) : Handler   ← returns next (for chaining)
           ├── # handle(request)              ← abstract
           └── # passToNext(request)          ← null-guard for next
               { if (next != null) next.handle(request); }

           ConcreteHandlerA ──▶ ConcreteHandlerB ──▶ ConcreteHandlerC
```

**Code Skeleton:**
```java
abstract class Approver {
    private Approver next;
    Approver setNext(Approver next) { this.next = next; return next; }

    abstract void handle(PurchaseRequest req);

    protected void passToNext(PurchaseRequest req) {
        if (next != null) next.handle(req);
        else System.out.println("No one can approve: " + req);
    }
}

class Manager extends Approver {
    void handle(PurchaseRequest req) {
        if (req.amount() <= 10_000) approve(req);
        else passToNext(req);
    }
}
// Chain: teamLead.setNext(manager).setNext(director).setNext(ceo);
```

**When to Use:**
- Multiple handlers can process a request, but only one should
- The handler is determined at runtime based on the request's properties
- You want to decouple sender from receiver

**Watch Out:**
- `setNext()` returns `next` (not `this`) — enables `a.setNext(b).setNext(c)` fluent chaining
- Every handler has exactly two branches: handle OR forward — never both
- Always handle the end-of-chain case (request falls through with no handler)

---

### 19. State

**Intent:** Allow an object to change its behavior when its internal state changes — appears to change its class.

**Participants:** `Context`, `State` (interface), `ConcreteState`

**Structure:**
```
Context ───has-a──▶ State (interface)
├── - state : State             ├── + handle(Context)
├── + setState(State)           │
├── + request()                 ConcreteStateA
│   { state.handle(this); }     ├── + handle(Context ctx)
│                               │   { /* do A stuff */
│                               │     ctx.setState(new StateB()); }
│                               │
│                               ConcreteStateB (terminal)
│                               └── + handle(Context ctx)
│                                   { /* do B stuff — no transition */ }
```

**Code Skeleton:**
```java
interface DocState { void submit(Document doc); void approve(Document doc); }

class DraftState implements DocState {
    public void submit(Document doc) {
        System.out.println("Submitting for review");
        doc.setState(new InReviewState());
    }
    public void approve(Document doc) {
        System.out.println("Cannot approve a draft");
    }
}

class Document {
    private DocState state = new DraftState();
    void setState(DocState s) { this.state = s; }
    void submit() { state.submit(this); }
    void approve() { state.approve(this); }
}
```

**When to Use:**
- Object's behavior depends on its state, and it must change at runtime
- Complex state-dependent conditionals (`if/switch` on state) — replace with polymorphism
- State machine with well-defined transitions (e.g., order lifecycle, document workflow)

**Watch Out:**
- State objects decide transitions — context just calls `setState()`
- Every state implements every method (even if some are no-ops in certain states)
- Terminal states make no `setState()` calls
- Don't confuse with Strategy (client sets it, no transitions) — State transitions are internal

---

### 20. Mediator

**Intent:** Centralize complex communication between objects; reduce many-to-many coupling to many-to-one.

**Participants:** `Mediator` (interface), `ConcreteMediator`, `Colleague` (participant)

**Structure:**
```
Colleague ───knows──▶ Mediator (interface)
├── + send(message)              ├── + register(Colleague)
│   { mediator.relay(this, msg) }├── + relay(Colleague sender, message)
│                                │   { for each colleague != sender:
│                                │       colleague.receive(msg); }
│
ConcreteColleagueA               ConcreteMediator
ConcreteColleagueB               ├── - colleagues : List<Colleague>
```

**Code Skeleton:**
```java
interface AuctionMediator {
    void register(Bidder b);
    void placeBid(Bidder bidder, double amount);
    void closeAuction();
}

class AuctionHouse implements AuctionMediator {
    private List<Bidder> bidders = new ArrayList<>();
    public void placeBid(Bidder bidder, double amount) {
        // update state, then notify all EXCEPT sender
        for (Bidder b : bidders) {
            if (b != bidder) b.onNewBid(bidder.getName(), amount);
        }
    }
}
```

**When to Use:**
- Multiple objects communicate in complex ways — too many direct references
- You want to centralize control logic (chat room, auction, air traffic control)
- Adding/removing participants should not require updating all other participants

**Watch Out:**
- Participants hold only the mediator reference — **never** each other
- Sender must not receive their own notification (`b != sender` filter)
- Different operations may have different notification policies (e.g., `placeBid` excludes sender; `closeAuction` includes everyone)
- Don't confuse with Observer (one-to-many broadcast) — Mediator is many-to-many coordination

---

## Quick Comparison Tables

### Creational Patterns at a Glance

| Pattern | Creates | Key Mechanism | Use When |
|---|---|---|---|
| Singleton | One instance | Private constructor + static accessor | Global unique resource |
| Factory Method | One product | Subclass overrides creation method | Subclass decides which class |
| Abstract Factory | Product family | Factory interface with multiple create methods | Swap entire product families |
| Builder | Complex object | Fluent setters + `build()` | Many optional params, immutable result |
| Prototype | Clone of existing | `clone()` method, deep copy | Expensive creation, template-based |

### Structural Patterns at a Glance

| Pattern | Purpose | Key Mechanism | Use When |
|---|---|---|---|
| Adapter | Interface translation | Wraps adaptee, implements target | Legacy/third-party integration |
| Facade | Simplification | Wraps subsystem behind one class | Complex subsystem, simple API needed |
| Decorator | Add behavior | Wraps same interface, stackable | Runtime behavior composition |
| Proxy | Control access | Wraps same interface, guards access | Lazy load, auth, cache, logging |
| Composite | Tree structure | Leaf + Composite share interface | Part-whole hierarchies |
| Bridge | Decouple dimensions | Abstraction has-a Implementor | Two independent variation axes |
| Flyweight | Memory optimization | Shared intrinsic + external extrinsic | Millions of similar objects |

### Behavioral Patterns at a Glance

| Pattern | Purpose | Key Mechanism | Use When |
|---|---|---|---|
| Strategy | Swap algorithms | Context has-a Strategy interface | Runtime algorithm selection |
| Observer | Event notification | Subject notifies List\<Observer\> | One-to-many state broadcast |
| Command | Encapsulate request | Command object with execute/undo | Undo/redo, queuing, logging |
| Template Method | Algorithm skeleton | Abstract base + override steps | Shared structure, different steps |
| Iterator | Sequential access | Inner class cursor over collection | Hide internal data structure |
| Chain of Resp. | Pass-along handling | Linked handler chain | Dynamic handler selection |
| State | State-driven behavior | Context delegates to State object | State machine, behavior changes |
| Mediator | Centralized comms | Hub coordinates participants | Many-to-many interaction |

### Commonly Confused Pairs

| Pattern A | Pattern B | Key Difference |
|---|---|---|
| Strategy | State | Strategy: client sets it, no transitions. State: internal transitions. |
| Decorator | Proxy | Decorator: adds behavior. Proxy: controls access. |
| Adapter | Bridge | Adapter: retrofit incompatible. Bridge: designed upfront for 2 dimensions. |
| Adapter | Facade | Adapter: 1:1 interface translation. Facade: simplifies many classes. |
| Observer | Mediator | Observer: one-to-many broadcast. Mediator: many-to-many coordination. |
| Factory Method | Abstract Factory | FM: one product via subclass. AF: product family via factory object. |
| Template Method | Strategy | TM: inheritance, compile-time. Strategy: composition, runtime. |
| Command | Strategy | Command: encapsulates request + undo. Strategy: encapsulates algorithm. |
| Composite | Decorator | Composite: tree structure. Decorator: single-chain wrapping. |
