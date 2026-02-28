# I — Interface Segregation Principle (ISP)

> "Clients should not be forced to depend on interfaces they do not use."
> — Robert C. Martin

Simplified: **Split fat interfaces into small, focused role interfaces. Each class implements only what it actually needs.**

---

## 1. The ISP Test

Ask this question about every interface you write:

> "Is there any class that implements this interface but leaves some methods throwing `UnsupportedOperationException` or empty?"

If yes, your interface is too fat. Split it.

```
Fat Interface                          Role Interfaces (ISP compliant)
┌────────────────────────────┐         ┌──────────────┐   ┌──────────────────┐
│ <<interface>> Worker       │         │ <<interface>>│   │ <<interface>>    │
│  + cook()                  │  ──►    │  Cookable    │   │  Servable        │
│  + serve()                 │         │  + cook()    │   │  + serve()       │
│  + checkInventory()        │         └──────────────┘   └──────────────────┘
│  + processPayment()        │
└────────────────────────────┘         ┌──────────────────────────────────────┐
                                       │ <<interface>> InventoryManageable    │
Chef forced to implement               │  + checkInventory()                  │
serve() and processPayment()           │  + reorderItem()                     │
with UnsupportedOperationException     └──────────────────────────────────────┘
— ISP VIOLATED.
```

---

## 2. Before → After: Fat Interface vs Role Interfaces

```
BEFORE — ISP Violation
══════════════════════
         <<interface>>
         RestaurantWorker
         cook() | serve() | clean()
         checkInventory() | reorderItem()
         processPayment() | openCashRegister()
              │
       ┌──────┴──────┐
      Chef          Waiter
      cook() ✅     serve() ✅
      serve() ❌     cook() ❌ ← throws UnsupportedOperationException
      processPayment() ❌      processPayment() ❌ ← throws
      ...                     ...

AFTER — ISP Compliant
═════════════════════
  <<Cookable>>    <<Servable>>   <<Cleanable>>   <<InventoryManageable>>  <<CashierOperable>>
  cook()          serve()        clean()         checkInventory()          processPayment()
                                                  reorderItem()            openCashRegister()
      │                │               │
    Chef           Waiter           Manager
  Cookable ✅     Servable ✅     Servable ✅
  Cleanable ✅    Cleanable ✅    Cleanable ✅
                               InventoryManageable ✅
                               CashierOperable ✅

Chef and Waiter implement ONLY what they do.
Manager implements all relevant roles.
No UnsupportedOperationException anywhere.
```

---

## 3. Core Concepts

### 3.1 Role Interfaces

Each interface represents one capability (role). Classes implement only the roles they fill.

```java
interface Cookable         { void cook(String dish); }
interface Servable         { void serve(String order, String table); }
interface Cleanable        { void clean(String area); }
interface InventoryManageable {
    int checkInventory(String item);
    void reorderItem(String item, int qty);
}
interface CashierOperable  {
    Receipt processPayment(double amount);
    void openCashRegister();
}
```

### 3.2 Fat Interface (Anti-Pattern)

One big interface forces every implementor to carry all methods — even irrelevant ones.

```java
// ❌ Fat interface — every worker must implement everything
interface RestaurantWorker {
    void cook(String dish);          // Chef only
    void serve(String order, String table); // Waiter only
    void clean(String area);         // Everyone
    int  checkInventory(String item); // Manager only
    void reorderItem(String item, int qty); // Manager only
    Receipt processPayment(double amount);  // Cashier only
    void openCashRegister();                // Cashier only
}

// ❌ Chef is forced to implement serve() and processPayment()
class Chef implements RestaurantWorker {
    @Override
    public void serve(String order, String table) {
        throw new UnsupportedOperationException("Chef cannot serve!"); // ← ISP VIOLATED
    }
    @Override
    public Receipt processPayment(double amount) {
        throw new UnsupportedOperationException("Chef cannot process payment!"); // ← ISP VIOLATED
    }
}
```

### 3.3 Client-Specific Interfaces

The service class depends only on the interface it actually uses — not the full concrete type.

```java
// ✅ ISP: RestaurantService only needs Cookable for kitchen orders
class RestaurantService {
    private final List<Cookable> kitchenStaff = new ArrayList<>();
    private final List<Servable> floorStaff   = new ArrayList<>();

    void addKitchenStaff(Cookable c) { kitchenStaff.add(c); }
    void addFloorStaff(Servable s)   { floorStaff.add(s); }

    // Sends order to kitchen — doesn't care if staff member also serves or manages
    void placeOrder(String dish) {
        for (Cookable c : kitchenStaff) c.cook(dish);
    }
}
```

### 3.4 Multiple Interface Implementation

A class can implement multiple role interfaces to cover all the roles it actually fills.

```java
// Manager does everything
class Manager implements Servable, Cleanable, InventoryManageable, CashierOperable {
    @Override public void serve(String order, String table) { ... }
    @Override public void clean(String area) { ... }
    @Override public int  checkInventory(String item) { ... }
    @Override public void reorderItem(String item, int qty) { ... }
    @Override public Receipt processPayment(double amount) { ... }
    @Override public void openCashRegister() { ... }
}
```

---

## 4. ISP vs Related Principles

| Principle | Relationship to ISP |
|---|---|
| **SRP** | SRP: one class, one reason to change. ISP: one interface, one client need. Both reduce coupling. |
| **LSP** | Many LSP violations (`Penguin.fly()` throws) are ISP violations too — the interface was too fat. Split it: `FlyingBird` / `NonFlyingBird`. |
| **OCP** | New device types (SmartBulb, SmartThermostat) can implement only the interfaces they need — open for extension, no modification. |
| **DIP** | Depend on small, focused interfaces (abstractions) rather than fat interfaces. ISP makes DIP more effective. |

---

## 5. ISP in the Java SDK (Examples)

| Java SDK Pattern | ISP Applied |
|---|---|
| `Runnable` | Single method `run()` — you don't get forced to implement `start()` or `stop()` |
| `Comparable<T>` | Just `compareTo()` — nothing else |
| `Iterable<T>` | Just `iterator()` |
| `Closeable` | Just `close()` |
| `List` (optional ops) | `add()` marked "optional operation" in javadoc — `Collections.unmodifiableList` throws on it; this is Java's compromise (not ideal ISP, but documented) |

---

## 6. Common Mistakes

```java
// MISTAKE 1: One monolithic interface for all operations
interface Device {
    void turnOn();
    void turnOff();
    void setTemperature(int celsius);  // only thermostats
    void playMusic(String track);      // only speakers
    void lock();                       // only locks
    void callEmergency();              // only security devices
}
// SmartBulb implements Device → forced to throw for temperature, music, lock, emergency

// MISTAKE 2: instanceof to avoid UnsupportedOperationException
void controlDevice(Device d) {
    if (d instanceof Thermostat) {           // ← ISP violation signal
        ((Thermostat) d).setTemperature(22);
    } else if (d instanceof Speaker) {       // ← ISP violation signal
        ((Speaker) d).playMusic("Jazz");
    }
}
// Fix: use role interfaces; no instanceof needed

// MISTAKE 3: Default methods to "solve" fat interface
interface Worker {
    void cook();
    default void serve()         { throw new UnsupportedOperationException(); }
    default void processPayment(){ throw new UnsupportedOperationException(); }
}
// Still fat — just hides the problem with defaults. Split the interface.

// MISTAKE 4: Too many single-method interfaces (over-splitting)
interface NameProvider    { String getName(); }
interface AgeProvider     { int getAge(); }
interface AddressProvider { String getAddress(); }
// For a simple Person, this is over-engineering. Group logically related operations.

// MISTAKE 5: Marker interface confusion
// ISP is about methods, not markers.
// Marker interfaces (like Serializable) are fine — they carry no method contracts.
```

---

## 7. Interview Questions

**Q1: What does the Interface Segregation Principle say in plain terms?**
> Don't force a class to implement methods it doesn't use. Split fat interfaces into small, role-specific ones. A class should depend only on the interface methods it actually calls.

**Q2: How do you recognize an ISP violation in a code review?**
> Look for: (1) `UnsupportedOperationException` or `NotImplementedException` in interface implementations, (2) `instanceof` checks before calling type-specific methods, (3) an interface with 7+ methods where different implementors use different subsets, (4) empty method bodies that do nothing in a subclass.

**Q3: What is the difference between ISP and SRP?**
> SRP applies to **classes** — one class has one reason to change. ISP applies to **interfaces** — one interface serves one client need. A god class violates SRP; a fat interface violates ISP. They often appear together: a god class implements a fat interface.

**Q4: Can a class implement multiple interfaces? Doesn't that violate ISP?**
> No — multiple interface implementation is the ISP solution, not the problem. A `Manager` can implement `Servable`, `Cleanable`, and `CashierOperable` because a manager genuinely fills all those roles. ISP says each interface should be narrow; it says nothing about how many interfaces a class can implement.

**Q5: How does ISP fix LSP violations like `Penguin.fly()`?**
> `Bird` with `fly()` forces `Penguin` to throw — an LSP violation. Split the interface: `FlyingBird extends Bird { fly(); }` and `Bird { eat(); sleep(); }`. `Penguin implements Bird` (not `FlyingBird`). No throws, no LSP violation, ISP applied.

**Q6: What does "client" mean in ISP?**
> "Client" means the calling code — the class or method that uses the interface. ISP says the interface should be shaped around what the client calls, not what the implementor provides. A `KitchenService` that only calls `cook()` is a client that should depend on `Cookable`, not on the full `RestaurantWorker` interface.

**Q7: Is it ever acceptable to have a fat interface?**
> Yes — when all implementors genuinely need all the methods. A `Shape` interface with `area()`, `perimeter()`, and `describe()` is not fat because every shape implementation uses all three. ISP only requires splitting when some implementors are forced to leave methods unimplemented.
