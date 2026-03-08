# Pattern Detection Guide — How to Identify Design Patterns in LLD Problems

When facing an LLD interview problem, the challenge isn't knowing the patterns — it's recognizing *which* pattern fits the situation. This guide teaches you to read problem requirements and map them to the right design pattern.

---

## The 3-Step Detection Process

```
Step 1: Read the problem → extract TRIGGER PHRASES
Step 2: Match triggers → CANDIDATE PATTERNS (usually 2-3)
Step 3: Apply ELIMINATION QUESTIONS → pick the one that fits
```

---

## Step 1 — Trigger Phrase Dictionary

These are phrases you'll hear in problem statements or interviews that point directly to specific patterns. Train yourself to spot them.

### Creational Triggers

| Trigger Phrase | Points To | Why |
|---|---|---|
| "only one instance", "global access", "shared resource" | **Singleton** | Single instance = Singleton by definition |
| "create objects without specifying exact class" | **Factory Method** | Delegates creation to subclasses |
| "family of related objects", "theme", "platform" | **Abstract Factory** | Multiple related products created together |
| "many optional parameters", "fluent API", "immutable object" | **Builder** | Telescoping constructor → Builder |
| "clone", "template", "copy with modifications" | **Prototype** | Creating from existing instance |

### Structural Triggers

| Trigger Phrase | Points To | Why |
|---|---|---|
| "integrate legacy system", "incompatible interface", "third-party library" | **Adapter** | Interface translation |
| "simplify", "one entry point", "hide complexity" | **Facade** | Subsystem simplification |
| "add features at runtime", "stackable", "toppings/extras" | **Decorator** | Dynamic behavior composition |
| "lazy load", "access control", "cache", "log every call" | **Proxy** | Controlled access |
| "tree structure", "part-whole", "recursive", "files and folders" | **Composite** | Uniform treatment of leaf + composite |
| "two independent dimensions", "varies in X and Y" | **Bridge** | Avoid M x N class explosion |
| "thousands of objects", "memory optimization", "shared state" | **Flyweight** | Intrinsic/extrinsic state split |

### Behavioral Triggers

| Trigger Phrase | Points To | Why |
|---|---|---|
| "swap algorithm at runtime", "multiple strategies", "policy" | **Strategy** | Pluggable behavior |
| "notify subscribers", "event", "when X changes, Y updates" | **Observer** | One-to-many notification |
| "undo/redo", "queue operations", "macro", "log commands" | **Command** | Request as object |
| "same steps, different details", "skeleton", "hook" | **Template Method** | Fixed algorithm, variable steps |
| "traverse without exposing internals", "custom iteration" | **Iterator** | Sequential access abstraction |
| "escalate", "chain of handlers", "pass to next" | **Chain of Responsibility** | Dynamic handler selection |
| "state machine", "behavior changes with state", "lifecycle" | **State** | State-dependent behavior |
| "chat room", "centralized coordination", "decouple many-to-many" | **Mediator** | Hub-and-spoke communication |

---

## Step 2 — Problem Scenarios and Pattern Mapping

Below are common LLD interview problems and the patterns they typically use. Study these to build pattern-recognition intuition.

### Parking Lot System

| Requirement | Pattern | Reasoning |
|---|---|---|
| Different vehicle types (car, bike, truck) with different space needs | **Strategy** | Parking assignment algorithm varies by vehicle type |
| Multiple floors, each with spots — treat floor as container | **Composite** | Floor contains spots; ParkingLot contains floors |
| Display board shows real-time availability | **Observer** | Spots notify display board on state change |
| Payment calculation (hourly, flat, subscription) | **Strategy** | Different pricing algorithms |
| Entry/exit gate controls | **State** | Gate has states: open, closed, processing |

### Library Management System

| Requirement | Pattern | Reasoning |
|---|---|---|
| Search by title, author, subject | **Strategy** | Different search algorithms behind one interface |
| Book lifecycle: available → reserved → checked out → returned | **State** | Book behavior depends on current state |
| Notify members when reserved book is available | **Observer** | Library notifies waiting members |
| Fine calculation rules (per day, per week, maximum cap) | **Strategy** | Different fine calculation policies |
| Browse catalog without exposing internal structure | **Iterator** | Custom traversal over catalog |

### Ride Sharing / Cab Booking (Uber/Ola)

| Requirement | Pattern | Reasoning |
|---|---|---|
| Multiple ride types (economy, premium, pool) | **Factory Method** | Create Ride subtype based on request |
| Fare calculation (distance-based, surge, subscription) | **Strategy** | Swap pricing algorithm |
| Notify driver and rider of trip updates | **Observer** | Trip events broadcast to participants |
| Trip lifecycle: requested → matched → in-progress → completed | **State** | Trip behavior changes with state |
| Payment via multiple gateways (Stripe, PayPal, wallet) | **Adapter** | Unify incompatible payment APIs |

### Hotel Booking System

| Requirement | Pattern | Reasoning |
|---|---|---|
| Room types (single, double, suite) with different amenities | **Factory Method** | Create room by type |
| Add-on services (breakfast, spa, airport transfer) | **Decorator** | Stack optional extras onto base booking |
| Booking lifecycle: pending → confirmed → checked-in → checked-out | **State** | Booking behavior depends on state |
| Notify housekeeping, billing, concierge on check-in | **Observer** | Check-in event triggers multiple systems |
| Price calculation with seasonal rates, discounts | **Strategy** | Different pricing strategies |

### Snake and Ladder Game

| Requirement | Pattern | Reasoning |
|---|---|---|
| Players take turns in order | **Iterator** | Cycle through players |
| Board cells have effects (snake, ladder, normal) | **Strategy or State** | Cell behavior varies |
| Game lifecycle: waiting → in-progress → finished | **State** | Game behavior depends on phase |
| Dice roll strategy (single die, two dice, loaded) | **Strategy** | Swap dice behavior |
| Undo last move | **Command** | Encapsulate move as undoable command |

### Elevator System

| Requirement | Pattern | Reasoning |
|---|---|---|
| Elevator states: idle, moving up, moving down, stopped | **State** | Behavior depends on current state |
| Scheduling algorithm (FCFS, SCAN, SSTF) | **Strategy** | Swap scheduling policy |
| Multiple elevators coordinated by controller | **Mediator** | Central controller dispatches requests |
| Button press triggers an action | **Command** | Encapsulate floor request as command |

### Food Delivery (Swiggy/Zomato)

| Requirement | Pattern | Reasoning |
|---|---|---|
| Order lifecycle: placed → accepted → preparing → out-for-delivery → delivered | **State** | Order behavior changes per phase |
| Notify customer, restaurant, delivery partner on state change | **Observer** | State change triggers multiple notifications |
| Multiple payment methods | **Adapter** | Unify incompatible payment SDKs |
| Discount/promo code application | **Decorator** | Stack discounts on base order |
| Delivery partner assignment algorithm | **Strategy** | Different matching algorithms |
| Restaurant → Customer communication via platform | **Mediator** | Platform mediates, parties never talk directly |

### Movie Ticket Booking (BookMyShow)

| Requirement | Pattern | Reasoning |
|---|---|---|
| Seat selection with different categories | **Factory Method** | Create seat by category |
| Booking with add-ons (insurance, food combo, premium seat) | **Decorator** | Stack extras on base ticket |
| Show notifications for seat availability | **Observer** | Notify waitlisted users |
| Payment processing | **Adapter** | Unify payment gateway interfaces |
| Booking lifecycle | **State** | Pending → confirmed → cancelled |

---

## Step 3 — Elimination Questions

When multiple patterns seem to fit, ask these questions to narrow down:

### "Is it about creating objects or using them?"

```
Creating → Creational pattern
    How many instances? → One: Singleton
    Complex construction? → Builder
    Decide type at runtime? → Factory Method
    Family of types? → Abstract Factory
    Copy existing? → Prototype

Using → Structural or Behavioral
```

### "Is it about structure or behavior?"

```
Structure (how objects are composed) → Structural pattern
    Incompatible interface? → Adapter
    Simplify subsystem? → Facade
    Add behavior dynamically? → Decorator
    Control access? → Proxy
    Tree/hierarchy? → Composite
    Two dimensions of variation? → Bridge
    Memory optimization? → Flyweight

Behavior (how objects interact) → Behavioral pattern
```

### Behavioral Pattern Decision Tree

```
Does behavior change based on internal state?
├── YES → Does the object transition between states automatically?
│         ├── YES → STATE
│         └── NO (client sets it) → STRATEGY
└── NO → Continue...

Is this about notifying multiple objects of a change?
├── YES → Is it one-to-many (one source, many listeners)?
│         ├── YES → OBSERVER
│         └── NO (many-to-many coordination) → MEDIATOR
└── NO → Continue...

Does the request need to be passed to the next handler?
├── YES → CHAIN OF RESPONSIBILITY
└── NO → Continue...

Do you need undo/redo or operation queuing?
├── YES → COMMAND
└── NO → Continue...

Is there a fixed algorithm with variable steps?
├── YES → TEMPLATE METHOD
└── NO → Continue...

Do you need to traverse a collection without exposing internals?
├── YES → ITERATOR
└── NO → Reconsider the above or combine patterns.
```

---

## Pattern Combination Rules

Real systems use multiple patterns together. Here are the most common combinations:

### State + Observer
**When:** Object has a lifecycle AND state changes must notify others.
**Example:** Order lifecycle (State) + notify customer/restaurant/driver (Observer).
```
Order.setState(new PreparingState())  →  state transition
    └── notifyObservers("preparing")  →  observer notification
```

### Strategy + Factory Method
**When:** Algorithm selection AND object creation both vary by type.
**Example:** Pricing strategy (Strategy) created via factory based on ride type (Factory Method).

### Decorator + Strategy
**When:** Stackable add-ons AND a pluggable core algorithm.
**Example:** Base pizza with toppings (Decorator) + pricing strategy (Strategy).

### Command + Observer
**When:** Operations need undo AND others need to be notified of changes.
**Example:** Text editor commands (Command) + UI panels update on edit (Observer).

### Composite + Iterator
**When:** Tree structure AND need to traverse it.
**Example:** File system tree (Composite) + depth-first traversal (Iterator).

### Adapter + Strategy
**When:** Multiple external APIs AND they need to be swappable.
**Example:** Payment gateways adapted to common interface (Adapter) + selected at runtime (Strategy).

### State + Command
**When:** State machine AND operations need to be undoable.
**Example:** Document workflow states (State) + approval/rejection commands with undo (Command).

---

## The 5-Minute Interview Pattern Detection Checklist

When you get an LLD problem in an interview, run through this checklist mentally:

```
1. LIFECYCLE?
   Does any entity go through distinct phases?
   YES → State pattern (+ Observer if transitions notify others)

2. ALGORITHM VARIATION?
   Are there multiple ways to do the same thing?
   YES → Strategy pattern

3. NOTIFICATIONS?
   When X happens, should Y and Z be told?
   YES → Observer (one-to-many) or Mediator (many-to-many)

4. OBJECT CREATION?
   Is there complex construction or type selection?
   YES → Builder (complex) / Factory (type selection) / Abstract Factory (families)

5. INTERFACE MISMATCH?
   Integrating external systems or legacy code?
   YES → Adapter

6. ADD-ONS / EXTRAS?
   Can features be stacked at runtime?
   YES → Decorator

7. TREE STRUCTURE?
   Part-whole hierarchy?
   YES → Composite

8. UNDO / HISTORY?
   Need to reverse or replay operations?
   YES → Command

9. PASS-ALONG HANDLING?
   Request handled by one of many possible handlers?
   YES → Chain of Responsibility

10. ACCESS CONTROL / LAZY LOAD?
    Need to guard or defer expensive operations?
    YES → Proxy
```

---

## Common Mistakes in Pattern Selection

### 1. Using Strategy when you need State
**Symptom:** You're calling `setStrategy()` inside the strategy itself.
**Rule:** If the object transitions between behaviors *on its own*, it's State. If the *client* picks the behavior, it's Strategy.

### 2. Using Observer when you need Mediator
**Symptom:** Observers are sending messages to each other through the subject.
**Rule:** Observer is one-directional (subject → observers). If participants need to communicate with each other, use Mediator.

### 3. Using Decorator when you need Proxy
**Symptom:** You're wrapping an object to control access, not to add behavior.
**Rule:** Decorator adds new behavior. Proxy controls access to existing behavior. If you're checking permissions, lazy-loading, or caching — that's Proxy.

### 4. Using Adapter when you should just refactor
**Symptom:** You own both interfaces and they're slightly different.
**Rule:** Adapter is for code you *cannot modify* (third-party, legacy). If you own both sides, just make the interfaces match.

### 5. Using Factory Method when you need Abstract Factory
**Symptom:** Your factory creates multiple related objects that must be consistent.
**Rule:** Factory Method creates *one product*. Abstract Factory creates a *family* of products that belong together.

### 6. Forcing a pattern where none is needed
**Symptom:** The code is simpler without the pattern.
**Rule:** Patterns solve specific problems. If the problem doesn't exist (e.g., only one algorithm, no state changes, no notifications), don't add the pattern. A direct `if/else` with two branches is fine — it doesn't need Strategy.

---

## Practice Exercise

Take any LLD problem (Parking Lot, Library, Elevator) and:
1. List all requirements as bullet points
2. For each requirement, identify the trigger phrase
3. Map each trigger to a candidate pattern
4. Apply the elimination questions
5. Draw the final class diagram with pattern roles labeled

The goal: go from problem statement to pattern selection in under 5 minutes.
