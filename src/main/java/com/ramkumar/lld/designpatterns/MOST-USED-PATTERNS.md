# Most Frequently Used Design Patterns in LLD Interviews

This guide ranks design patterns by how often they appear in LLD interview problems and machine coding rounds. Focus your revision on the top tier first.

---

## Tier 1 — Appears in Almost Every LLD Problem

These 5 patterns show up in 80%+ of LLD interview problems. You must be able to apply them without thinking.

---

### 1. Strategy

**Frequency:** Extremely high — appears in nearly every LLD problem.

**Why it's everywhere:** Almost every system has "multiple ways to do the same thing" — pricing, sorting, searching, matching, routing, scoring. Whenever a problem says "support multiple algorithms/policies," Strategy is the answer.

**Where you'll use it:**
| LLD Problem | Strategy For |
|---|---|
| Parking Lot | Parking fee calculation (hourly, flat, subscription) |
| Ride Sharing | Fare calculation (distance, surge, subscription) |
| Hotel Booking | Room pricing (seasonal, weekend, loyalty) |
| Food Delivery | Delivery partner matching (nearest, rating, load-balanced) |
| E-commerce | Discount calculation (percentage, flat, BOGO) |
| Payment System | Fraud detection rules (rule-based, ML-based, threshold) |
| Game Engine | AI behavior (aggressive, defensive, random) |
| Notification System | Channel selection (email, SMS, push) |

**The pattern in 4 lines:**
```java
interface PricingStrategy { double calculate(Order order); }
class FlatRate implements PricingStrategy { /* ... */ }
class DistanceBased implements PricingStrategy { /* ... */ }
// Context holds: private PricingStrategy strategy;
```

**Interview signal:** "Different rules for...", "Multiple ways to...", "Policy can change..."

---

### 2. Observer

**Frequency:** Very high — any system with notifications, events, or real-time updates.

**Why it's everywhere:** Modern systems are event-driven. Whenever one thing happens and multiple other things need to react, that's Observer. Most LLD problems have at least one notification requirement.

**Where you'll use it:**
| LLD Problem | Observer For |
|---|---|
| Ride Sharing | Notify rider + driver of trip status changes |
| Food Delivery | Notify customer + restaurant + driver on order state change |
| Stock Trading | Price change → notify all watching clients |
| E-commerce | Order placed → notify inventory, billing, shipping |
| Social Media | New post → notify all followers |
| Parking Lot | Spot freed → notify waiting vehicles / display board |
| Auction System | New bid → notify all other bidders |
| Chat Application | New message → notify all room members |

**The pattern in 5 lines:**
```java
interface OrderObserver { void onOrderUpdate(Order order, String event); }
class OrderService {
    private List<OrderObserver> observers = new ArrayList<>();
    void subscribe(OrderObserver o) { observers.add(o); }
    void notifyAll(Order o, String e) { observers.forEach(obs -> obs.onOrderUpdate(o, e)); }
}
```

**Interview signal:** "Notify...", "When X happens, update Y...", "Real-time updates...", "Subscribe..."

---

### 3. State

**Frequency:** Very high — any entity with a lifecycle.

**Why it's everywhere:** Most LLD entities have lifecycles: orders go from placed → confirmed → shipped → delivered; bookings go from pending → confirmed → checked-in → checked-out. Whenever behavior depends on the current phase, that's State.

**Where you'll use it:**
| LLD Problem | State For |
|---|---|
| Food Delivery | Order: placed → accepted → preparing → out-for-delivery → delivered |
| Ride Sharing | Trip: requested → matched → in-progress → completed → rated |
| Hotel Booking | Booking: pending → confirmed → checked-in → checked-out |
| Elevator | Elevator: idle → moving-up → moving-down → stopped |
| ATM | Transaction: idle → card-inserted → pin-verified → dispensing |
| Document System | Document: draft → in-review → approved / rejected |
| Ticket Booking | Ticket: available → reserved → booked → cancelled |
| Vending Machine | idle → coin-inserted → item-selected → dispensing |

**The pattern in 6 lines:**
```java
interface OrderState {
    void confirm(Order ctx);
    void ship(Order ctx);
    void cancel(Order ctx);
}
class PlacedState implements OrderState {
    public void confirm(Order ctx) { ctx.setState(new ConfirmedState()); }
    public void ship(Order ctx) { System.out.println("Cannot ship — not confirmed yet"); }
    public void cancel(Order ctx) { ctx.setState(new CancelledState()); }
}
```

**Interview signal:** "Lifecycle...", "Status changes...", "Can only do X when in state Y...", "Transitions..."

---

### 4. Factory Method

**Frequency:** High — any system that creates different types of objects based on input.

**Why it's everywhere:** Most LLD problems have type hierarchies: vehicle types, room types, notification types, payment types. Whenever the problem says "create the right type based on input," that's Factory Method.

**Where you'll use it:**
| LLD Problem | Factory For |
|---|---|
| Parking Lot | Create ParkingSpot by vehicle type (bike, car, truck) |
| Notification System | Create Notification by channel (email, SMS, push) |
| Ride Sharing | Create Ride by type (economy, premium, pool) |
| Hotel Booking | Create Room by category (single, double, suite) |
| Food Delivery | Create Order by type (delivery, pickup, dine-in) |
| Document System | Create Document by format (PDF, Word, Spreadsheet) |
| Game | Create Character by class (warrior, mage, archer) |

**The pattern in 4 lines:**
```java
interface Notification { void send(String msg); }
class NotificationFactory {
    static Notification create(String type) {
        return switch (type) {
            case "email" -> new EmailNotification();
            case "sms"   -> new SmsNotification();
            case "push"  -> new PushNotification();
            default -> throw new IllegalArgumentException("Unknown: " + type);
        };
    }
}
```

**Interview signal:** "Different types of...", "Based on the type, create...", "Pluggable creation..."

---

### 5. Decorator

**Frequency:** High — any system with stackable add-ons or optional features.

**Why it's everywhere:** Many LLD problems have optional extras that can be combined: pizza toppings, booking add-ons, notification formatting, message encryption. Whenever features can be layered onto a base, that's Decorator.

**Where you'll use it:**
| LLD Problem | Decorator For |
|---|---|
| Food Delivery | Pizza/burger with stackable toppings |
| Hotel Booking | Base room + breakfast + spa + airport transfer |
| Coffee Shop | Base coffee + milk + sugar + whipped cream |
| Ticket Booking | Base ticket + insurance + meal + seat upgrade |
| Notification System | Base message + encryption + compression + logging |
| E-commerce | Base product + gift wrap + express shipping + warranty |
| Text Editor | Base text + bold + italic + underline (stackable) |

**The pattern in 5 lines:**
```java
interface Coffee { double cost(); String description(); }
class BasicCoffee implements Coffee { /* base: $2.00 */ }
abstract class CoffeeDecorator implements Coffee {
    protected final Coffee wrapped;
    CoffeeDecorator(Coffee c) { this.wrapped = c; }
}
class MilkDecorator extends CoffeeDecorator {
    public double cost() { return wrapped.cost() + 0.50; }
}
// new MilkDecorator(new SugarDecorator(new BasicCoffee()))
```

**Interview signal:** "Add-ons...", "Extras...", "Toppings...", "Optional features that stack..."

---

## Tier 2 — Appears in Many LLD Problems

These patterns appear in 40-60% of problems. Know them well.

---

### 6. Adapter

**When it appears:** Integrating external systems — payment gateways, notification providers, third-party APIs.

**Common LLD scenarios:**
- Payment processing: Stripe SDK + PayPal SDK → unified `PaymentProcessor` interface
- Notification delivery: Twilio + SendGrid + Firebase → unified `NotificationSender`
- Map services: Google Maps + MapBox → unified `MapProvider`

**Interview signal:** "Integrate with...", "Third-party...", "Legacy system...", "Unified interface for external..."

---

### 7. Builder

**When it appears:** Complex object construction with many optional fields.

**Common LLD scenarios:**
- Search query with optional filters (location, price range, rating, cuisine, date)
- Configuration objects (server config, database config)
- Report generation with optional sections
- HTTP request construction

**Interview signal:** "Many optional parameters...", "Configure with...", "Immutable object with..."

---

### 8. Command

**When it appears:** Undo/redo, operation queuing, or transaction logging.

**Common LLD scenarios:**
- Text editor: type, delete, format — all undoable
- Game: player moves — replayable
- Smart home: schedule device operations
- Stock trading: queue buy/sell orders

**Interview signal:** "Undo...", "Redo...", "Queue operations...", "Transaction log...", "Replay..."

---

### 9. Composite

**When it appears:** Tree/hierarchical structures.

**Common LLD scenarios:**
- File system: files and directories
- Organization chart: employees and departments
- Menu system: items and sub-menus
- UI components: panels containing panels containing buttons

**Interview signal:** "Tree...", "Hierarchy...", "Recursive structure...", "Contains other..."

---

### 10. Singleton

**When it appears:** Shared resources, configuration, caches.

**Common LLD scenarios:**
- Database connection pool
- Application configuration
- Logger
- Cache manager

**Interview signal:** "Single instance...", "Shared across...", "Global access..."

**Caution:** Overusing Singleton is a red flag in interviews. Use it only for truly global resources. Prefer dependency injection.

---

## Tier 3 — Situational but Important

These patterns appear less often but are the right answer when they fit.

---

### 11. Chain of Responsibility

**When it appears:** Request processing with escalation or filtering.

**Common scenarios:** Support ticket escalation, approval workflows (amount-based), middleware/filter chains, input validation pipelines.

**Interview signal:** "Escalate to...", "If can't handle, pass to...", "Approval levels..."

---

### 12. Mediator

**When it appears:** Many-to-many communication that needs a central coordinator.

**Common scenarios:** Chat rooms, auction houses, air traffic control, multiplayer game lobby.

**Interview signal:** "Participants communicate through...", "Central coordinator...", "No direct references between..."

---

### 13. Template Method

**When it appears:** Same algorithm structure, different step implementations.

**Common scenarios:** Data export (CSV/JSON/XML share same flow), payment processing (validate → charge → receipt), report generation.

**Interview signal:** "Same steps but different...", "Framework calls your code...", "Override specific steps..."

---

### 14. Proxy

**When it appears:** Lazy loading, caching, or access control around expensive objects.

**Common scenarios:** Image lazy loading, database query caching, API rate limiting, permission checking.

**Interview signal:** "Lazy load...", "Cache...", "Check permission before...", "Rate limit..."

---

### 15. Iterator

**When it appears:** Custom collection traversal.

**Common scenarios:** Playlist navigation, paginated results, filtered browsing, custom data structure traversal.

**Interview signal:** "Traverse...", "Browse through...", "Next/previous...", "Filter while iterating..."

---

### 16. Bridge

**When it appears:** Two independent dimensions of variation.

**Common scenarios:** Shape + Color, Message + Channel, Device + Platform, Payment + Currency.

**Interview signal:** "Varies in two ways...", "M types x N types...", "Independent dimensions..."

---

### 17. Flyweight

**When it appears:** Memory optimization for many similar objects.

**Common scenarios:** Game particles, character glyphs in a text editor, map tiles, bullet types in a game.

**Interview signal:** "Thousands of...", "Memory optimization...", "Shared properties..."

---

## Quick Reference — Pattern Frequency by LLD Problem

| Problem | Tier 1 Patterns Used | Tier 2+ Patterns Used |
|---|---|---|
| **Parking Lot** | Strategy, Observer, State, Factory | Composite, Singleton |
| **Food Delivery** | Strategy, Observer, State, Factory, Decorator | Adapter, Mediator |
| **Ride Sharing** | Strategy, Observer, State, Factory | Adapter, Builder |
| **Hotel Booking** | Strategy, Observer, State, Decorator | Builder, Factory |
| **E-commerce** | Strategy, Observer, State, Factory, Decorator | Adapter, Command, Builder |
| **Elevator** | State, Strategy | Command, Mediator |
| **Snake & Ladder** | Strategy, State | Iterator, Command |
| **Movie Booking** | State, Observer, Decorator | Factory, Adapter |
| **ATM** | State | Chain of Resp., Singleton |
| **Chess** | Strategy, State | Command, Composite, Iterator |
| **Library** | Strategy, Observer, State | Iterator, Factory |
| **Chat App** | Observer / Mediator | Adapter, Factory |
| **Vending Machine** | State | Factory, Singleton |
| **Text Editor** | Command, Decorator | Iterator, Observer |
| **Notification System** | Strategy, Observer, Factory | Adapter, Decorator |

---

## The Interview Priority Stack

If you're short on revision time, study in this exact order:

```
MUST KNOW (Tier 1 — use in every problem):
  1. Strategy      — swap algorithms
  2. Observer       — event notification
  3. State          — lifecycle management
  4. Factory Method — type-based creation
  5. Decorator      — stackable add-ons

SHOULD KNOW (Tier 2 — use in many problems):
  6. Adapter        — external integration
  7. Builder        — complex construction
  8. Command        — undo/redo
  9. Composite      — tree structures
 10. Singleton      — global resources

GOOD TO KNOW (Tier 3 — situational):
 11. Chain of Resp. — escalation
 12. Mediator       — many-to-many hub
 13. Template Method— algorithm skeleton
 14. Proxy          — access control
 15. Iterator       — custom traversal
 16. Bridge         — two dimensions
 17. Flyweight      — memory saving
 18. Abstract Factory— product families
 19. Prototype      — cloning
```

---

## Final Advice

1. **Every LLD problem uses 3-5 patterns.** Don't force all of them — pick the ones that genuinely solve a requirement.

2. **Strategy + Observer + State cover 60% of behavioral needs.** Master these three and you'll handle most interview problems.

3. **Name the pattern explicitly in interviews.** Say "I'll use the State pattern for the order lifecycle" — interviewers want to hear that you know what you're applying and why.

4. **Start with the entity lifecycle.** In every LLD problem, find the main entity (Order, Ride, Booking) and map its states first. State pattern gives you the backbone; then layer Strategy, Observer, and Decorator on top.

5. **Don't apply a pattern unless the problem needs it.** A two-branch `if/else` doesn't need Strategy. An object with no lifecycle doesn't need State. The best engineers know when NOT to use a pattern.
