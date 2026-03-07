# 3.3.2 Observer Pattern

## What Problem Does It Solve?

You have an object (the **subject**) whose state changes matter to many other objects
(the **observers**). Without Observer, the subject must know every dependent type and
call them directly — tight coupling that grows with every new consumer.

```
Without Observer — WeatherStation coupled to every display:
  recordReading() {
      phoneDisplay.update(temp, humidity);     // knows PhoneDisplay
      webDashboard.refresh(temp, humidity);    // knows WebDashboard
      alertService.check(temp, humidity);      // knows AlertService
      // add a new display → modify WeatherStation every time
  }

With Observer — WeatherStation coupled only to the interface:
  recordReading() {
      for (WeatherObserver o : observers) {
          o.onReadingUpdate(temp, humidity, pressure);   // one line; any number of observers
      }
  }
```

The key insight: the subject maintains a list of observer interface references.
Observers register themselves; the subject notifies all of them through the interface.

---

## Core Structure

```
          «interface»
         WeatherObserver
      + onReadingUpdate(t, h, p)
               ▲
    ┌──────────┼──────────┐
    │          │          │
PhoneDisplay WebDashboard AlertService
(concrete)   (concrete)  (concrete)

WeatherStation ───────────────► WeatherObserver
  (Subject)      has-many        subscribe(o)
  - observers: List<WeatherObserver>    unsubscribe(o)
  - tempC, humidity, pressureHpa        notify all on recordReading()
```

Relationships:
- **Subject** knows only the `Observer` interface — never concrete types.
- **Observers** register/unregister themselves at runtime.
- **Notification** is driven by the subject; observers are purely reactive.

---

## Push vs Pull Model

| Model | How data flows | Trade-offs |
|---|---|---|
| **Push** | Subject passes all data as method parameters: `update(temp, humidity, pressure)` | Simple; observer gets exactly what the subject decides to send |
| **Pull** | Subject passes only a reference to itself: `update(WeatherStation station)` then observer calls `station.getTemp()` | Observer picks what it needs; subject may expose more state than intended |

Most implementations start with **Push**. Use **Pull** when observers need different
subsets of subject state and you don't want to change the interface signature every time.

---

## Snapshot Copy — Avoiding ConcurrentModificationException

If an observer unsubscribes during a notification loop, iterating the live list throws
`ConcurrentModificationException`. Always iterate a copy:

```java
// ❌ Dangerous — observer could call unsubscribe() on this station while iterating
for (WeatherObserver o : observers) {
    o.onReadingUpdate(tempC, humidity, pressureHpa);
}

// ✅ Safe — snapshot taken before notification; list changes don't affect iteration
List<WeatherObserver> snapshot = new ArrayList<>(observers);
for (WeatherObserver o : snapshot) {
    o.onReadingUpdate(tempC, humidity, pressureHpa);
}
```

---

## Code Skeleton

```java
// ── Observer interface ────────────────────────────────────────────────────────
interface WeatherObserver {
    void onReadingUpdate(double tempC, double humidity, double pressureHpa);
}

// ── Subject ───────────────────────────────────────────────────────────────────
class WeatherStation {
    private final List<WeatherObserver> observers = new ArrayList<>();
    private double tempC, humidity, pressureHpa;

    void subscribe(WeatherObserver o) {
        if (o == null) throw new IllegalArgumentException("observer must not be null");
        observers.add(o);
    }

    void unsubscribe(WeatherObserver o) {
        observers.remove(o);   // no-op if not present
    }

    void recordReading(double tempC, double humidity, double pressureHpa) {
        this.tempC = tempC; this.humidity = humidity; this.pressureHpa = pressureHpa;
        List<WeatherObserver> snapshot = new ArrayList<>(observers);   // safe copy
        for (WeatherObserver o : snapshot) {
            o.onReadingUpdate(tempC, humidity, pressureHpa);
        }
    }
}

// ── Concrete Observer ─────────────────────────────────────────────────────────
class PhoneDisplay implements WeatherObserver {
    private final String owner;
    PhoneDisplay(String owner) { this.owner = owner; }

    @Override
    public void onReadingUpdate(double tempC, double humidity, double pressureHpa) {
        System.out.printf("[Phone:%s] %.1f°C  %.0f%% humidity%n", owner, tempC, humidity);
    }
}
```

---

## Observer vs Other Patterns

| Dimension | Observer | Mediator | Event Bus |
|---|---|---|---|
| **Who knows whom** | Subject knows observers (via interface) | All parties know only the mediator | Nobody knows anybody — event bus decouples completely |
| **Directionality** | Subject → Observers (one-to-many) | Many-to-many via mediator | Publisher → Subscribers (topic-based) |
| **Runtime registration** | Yes — subscribe/unsubscribe | Fixed at construction | Yes — subscribe by topic |
| **Use when** | One object's changes must notify many | Many objects interact in complex ways | You want full decoupling across modules |

---

## Interview Q&A

**Q1. What is the Observer pattern? State the intent in one sentence.**
Observer defines a one-to-many dependency: when one object (subject) changes state,
all registered dependents (observers) are notified and updated automatically.

**Q2. Why does the subject hold a `List<Observer>` instead of concrete types?**
The subject must remain open for new observer types without modification (OCP). If it
held `PhoneDisplay` and `WebDashboard` as separate fields, adding a third display would
require editing the subject class.

**Q3. What is the ConcurrentModificationException risk and how do you avoid it?**
If an observer calls `unsubscribe()` on the subject during a notification loop, the list
is modified mid-iteration and throws `ConcurrentModificationException`. The fix is to
iterate over a snapshot copy of the list taken before the loop starts.

**Q4. Push vs Pull — when do you use each?**
Push: subject passes data as parameters. Simple, works when all observers need the same
data. Pull: subject passes `this`; observers query what they need. Flexible when observers
need different subsets, but exposes more subject state. Start with Push; move to Pull if
the interface signature becomes a maintenance burden.

**Q5. How does Observer differ from Strategy?**
Strategy varies the *algorithm inside* one object (single strategy active at a time).
Observer broadcasts *state changes from* one object to *many* others simultaneously.
Strategy is about "how to do X"; Observer is about "who cares when X happens."

**Q6. What happens if you don't validate `null` observers in `subscribe()`?**
A null in the observer list will cause a `NullPointerException` when the subject
iterates and calls `o.onReadingUpdate(...)`. Validation at the boundary (subscribe)
gives a clear error at the source rather than a cryptic NPE during notification.

**Q7. Name real-world Java examples of Observer.**
- `java.util.EventListener` hierarchy — all Swing/AWT listeners are observers.
- `java.util.Observer` / `java.util.Observable` (deprecated in Java 9 but historically the
  canonical example).
- `PropertyChangeListener` in JavaBeans — observe field changes on a bean.
- Reactive streams (`Flow.Subscriber`) in Java 9 — push-based observer with back-pressure.

---

## Common Mistakes

1. **Subject holds concrete observer types.**
   `private PhoneDisplay display; private WebDashboard dashboard;` — this is not Observer,
   it is just direct coupling. Adding a third type requires modifying the subject. Always
   hold `List<Observer>`.

2. **Iterating the live observer list during notification.**
   If `onReadingUpdate()` calls `unsubscribe()`, iterating the original list throws
   `ConcurrentModificationException`. Use a snapshot copy before the loop.

3. **Not validating null in `subscribe()`.**
   A null observer in the list produces a delayed, hard-to-trace NPE at notification time.
   Fail fast at `subscribe()` with a clear `IllegalArgumentException`.

4. **Exposing the observer list directly.**
   `List<Observer> getObservers()` lets callers modify the list outside the subject's
   control. Keep the list private; expose only `subscribe()` and `unsubscribe()`.

5. **Observer calling heavy/blocking operations synchronously.**
   The subject's notification loop is synchronous — if one observer blocks (network call,
   disk write), all subsequent observers wait. In production, push to a queue and process
   asynchronously, or accept that the observer must be fast.
