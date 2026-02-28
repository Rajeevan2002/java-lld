# Review — Dependency Inversion Principle (DIP)
Phase 2, Topic 5 | Scenario B: Ride-Hailing Trip Service

---

## What You Got Right

- **`ViolatingTripService` — textbook DIP violation**: `new InternalSmsClient()` and `new InternalMySqlTripStore()` hardcoded in the constructor. This is exactly the violation to demonstrate — the class creates its own dependencies and can never swap them.
- **All three abstractions correctly defined**: `TripRepository`, `RiderNotifier`, `FareCalculator` — all are interfaces, methods have correct signatures, and they are the right level of abstraction for the high-level module.
- **`TripService` fields are `final` interfaces** (not concrete types): `private final TripRepository`, `private final RiderNotifier`, `private final FareCalculator` — this is the core DIP requirement satisfied correctly.
- **`TripService` constructor null-checks**: All three dependencies validated at construction time — fail-fast boundary guard. `TripService` never reaches an inconsistent state where a field is null.
- **`TripService` never uses `new`** for its dependencies: The constructor only assigns. The caller (main) is responsible for creating implementations — this is the DIP contract fulfilled.
- **`bookTrip()` validation + save + return**: Input validation, `fareCalculator.calculate()` call, `new Trip(...)`, `tripRepository.save()`, and return — all correct.
- **`completeTrip`/`cancelTrip` — Unknown ID throws `IllegalArgumentException`**: Test 10 passes — `trip.isEmpty()` check correctly throws with the right message format.
- **`InMemoryTripRepository`**: `HashMap` storage, `save`/`findById`/`findByRider` all work correctly for the happy path.
- **`findByRider` returns `Collections.unmodifiableList()`**: Test 7 passes.
- **`FixedFareCalculator`** — returns fixed fare regardless of route. Clean DIP proof: `TripService` gets a different `FareCalculator` and produces different fares without any code change.
- **`EmailRiderNotifier`** — swappable notifier implementation. Test 9 proves DIP: same `TripService` logic, different notification mechanism.
- **`Trip` value object**: `final` fields for immutable data, mutable `status`, all five validations present, `setStatus()` with null guard, `describe()` method.

---

## Issues Found

### Issue 1 — Bug (Tests 2 & 3 fail): `nextTripId()` wraps ID in square brackets

**Severity**: Bug (Tests 2 and 3 fail at runtime)

`nextTripId()` returns `"[TRIP-001]"` (with brackets). Test 2 asserts `t1.getTripId().startsWith("TRIP-")` — returns `false` because the string starts with `[`. Test 3 asserts `t1.getTripId().matches("TRIP-\\d{3}")` — also `false`.

**Your code:**
```java
public String nextTripId() {
    return String.format("[TRIP-%03d]", ++tripCounter);
    //                    ↑          ↑  brackets make all format checks fail
}
```

**Fix:**
```java
private String nextTripId() {
    return String.format("TRIP-%03d", ++tripCounter);
}
```

**Why it matters**: The ID format is a contract. Every downstream system (notifications, lookup, logs) that receives `"TRIP-001"` will fail if it actually gets `"[TRIP-001]"`. Format strings that "look right" but include extra characters are a common real-world bug.

---

### Issue 2 — Design: `tripCounter` is `static` — shared across all `TripService` instances

**Severity**: Design

`private static int tripCounter = 0` means ALL `TripService` instances share the same counter. In main(), `service`, `fixedService`, and `emailService` are three separate instances — they all increment the same counter. Tests 8 and 9 create `fixedTrip` and `emailTrip` with IDs continuing from where `service` left off (`TRIP-004`, `TRIP-005`). This is unexpected behaviour: creating a second `TripService` should not affect the ID sequence of the first.

**Your code:**
```java
static class TripService {
    private static int tripCounter = 0;   // ← static: shared across all instances
```

**Fix:**
```java
static class TripService {
    private int tripCounter = 0;   // instance field — each TripService has its own counter
```

**Why it matters**: Static mutable state is one of the most common sources of flaky tests and subtle concurrency bugs. If two `TripService` instances ever run concurrently (e.g., in tests), the counter becomes a race condition.

---

### Issue 3 — Design: `completeTrip` and `cancelTrip` skip `tripRepository.save()` after status update

**Severity**: Design

The spec says: "Sets status to COMPLETED, **saves**, notifies rider." The user skips the `tripRepository.save(trip)` call after `setStatus()`. It works in the current tests only because `InMemoryTripRepository` stores a reference — the mutable `Trip` object in the map reflects the status change without re-saving. Switch to any real database (or a future copy-on-write repo), and completed/cancelled trips will never be persisted with the new status.

**Your code:**
```java
public Trip completeTrip(String tripId) {
    ...
    trip.get().setStatus(TripStatus.COMPLETED);
    riderNotifier.notify(...);       // ← notify but never save the updated state
    return trip.get();
}
```

**Fix:**
```java
public Trip completeTrip(String tripId) {
    ...
    trip.get().setStatus(TripStatus.COMPLETED);
    tripRepository.save(trip.get());   // ← explicitly persist the status change
    riderNotifier.notify(trip.get().getRiderId(), "Trip completed: " + tripId);
    return trip.get();
}
```

**Why it matters**: DIP means your high-level module must explicitly drive all side effects through abstractions. Relying on the in-memory reference being reflected is an implicit coupling to the InMemory implementation — which defeats the purpose of the repository abstraction.

---

### Issue 4 — Design: `cancelTrip` calls `notify` before `setStatus`

**Severity**: Design

In `cancelTrip`, the notification is sent before the status is changed to `CANCELLED`. The correct order is: change state → persist → notify. If the notification fails (exception thrown by notifier), the trip status would never have been set — but the rider was already told it's cancelled. State change and persistence must happen before notification.

**Your code:**
```java
riderNotifier.notify(trip.get().getTripId(), "Trip cancelled: " + ...);  // ← notify first
trip.get().setStatus(TripStatus.CANCELLED);                               // ← state change after
```

**Fix:**
```java
trip.get().setStatus(TripStatus.CANCELLED);                               // ← state first
tripRepository.save(trip.get());                                          // ← persist
riderNotifier.notify(trip.get().getRiderId(), "Trip cancelled: " + tripId); // ← then notify
```

**Why it matters**: Notification is an observable side effect. It should only fire after the state is successfully committed. "Notify then change state" is a classic event ordering bug in distributed systems.

---

### Issue 5 — Minor: `fareAmount <= 0` rejects 0 — spec says `>= 0`

**Severity**: Minor (design contract violation)

The spec says `fareAmount >= 0` — meaning 0 is a valid fare (e.g., a promotional free ride). `fareAmount <= 0` rejects 0, which would cause `new FixedFareCalculator(0.0)` to throw inside `Trip`.

**Your code:**
```java
if (fareAmount <= 0) throw new IllegalArgumentException("Fare Amount has to atleast > 0");
```

**Fix:**
```java
if (fareAmount < 0) throw new IllegalArgumentException("fareAmount cannot be negative");
```

---

### Issue 6 — Minor: `nextTripId()` should be `private`

**Severity**: Minor

The spec describes `nextTripId()` as an internal helper — no external caller should generate IDs directly. Making it `public` allows callers to increment the counter without creating a trip, breaking the ID sequence integrity.

**Fix:** `private String nextTripId() { ... }`

---

### Issue 7 — Minor: `findById` has redundant double-lookup (`containsKey` + `get`)

**Severity**: Minor

```java
if (storage.containsKey(tripId)) {
    return Optional.of(storage.get(tripId));   // two map lookups for the same key
}
return Optional.ofNullable(storage.get(tripId));
```

`Optional.ofNullable(storage.get(tripId))` handles both cases in one lookup:
- If key exists → `Optional.of(value)`
- If key absent → `Optional.empty()`

**Fix:** `return Optional.ofNullable(storage.get(tripId));`

---

## Score Card

| Requirement | Result | Notes |
|---|---|---|
| `ViolatingTripService` uses `new` for both concrete dependencies | ✅ | Correct violation |
| `TripRepository` interface: save/findById/findByRider | ✅ | Correct |
| `RiderNotifier` interface: notify | ✅ | Correct |
| `FareCalculator` interface: calculate | ✅ | Correct |
| `TripStatus` enum: PENDING, COMPLETED, CANCELLED | ✅ | Correct |
| `Trip` fields: final where immutable | ✅ | Correct |
| `Trip.fareAmount` validation: `>= 0` (0 is valid) | ❌ | Uses `<= 0` — rejects 0 |
| `Trip.describe()` — returns correct format | ⚠️ | Different format from spec but not tested |
| `TripService` dependency fields: `final` interfaces | ✅ | Correct |
| `TripService.tripCounter`: instance field (not static) | ❌ | Is `static` — shared across instances |
| `TripService.nextTripId()`: private, no brackets | ❌ | Public + brackets cause Tests 2 & 3 to fail |
| `TripService.bookTrip()`: validate + fare + save + notify | ⚠️ | Missing `riderNotifier.notify()` in bookTrip |
| `TripService.completeTrip()`: setStatus + save + notify | ❌ | Missing `tripRepository.save()` after setStatus |
| `TripService.cancelTrip()`: setStatus + save + notify (in order) | ❌ | Notify before setStatus; missing save |
| `TripService` constructor: null-checks all three | ✅ | Correct |
| `InMemoryTripRepository`: save / findById / findByRider | ✅ | Correct |
| `findByRider` returns unmodifiable list | ✅ | Correct |
| `FixedFareCalculator`: returns fixed fare | ✅ | Correct |
| `EmailRiderNotifier`: prints email format | ✅ | Correct |
| Test 8: DIP proof — swap FareCalculator | ✅ | Passes |
| Test 9: DIP proof — swap RiderNotifier | ✅ | Passes |
| Test 10: unknown tripId throws IAE | ✅ | Passes |
| Test 11: null dependency throws IAE | ✅ | Passes |

---

## Key Takeaways — Do Not Miss These

**TK-1: The ID format is a contract — every character counts**
`"TRIP-%03d"` and `"[TRIP-%03d]"` look almost identical but one silently breaks every downstream check that relies on the format. Before writing any format string, ask: "what does the consumer expect to receive?"
*Interview relevance*: Format-string bugs (extra characters, wrong padding, wrong separator) are one of the most common "it compiles and looks right but tests fail" bugs. Print the actual value and compare it to the spec.

**TK-2: `static` on a mutable counter = shared state across all instances**
`private static int counter` belongs to the class, not to the object. Every `TripService` you create shares the same counter. Use an instance field (`private int tripCounter = 0`) so each service has its own independent ID sequence. Static mutable state is the enemy of testability and concurrency.
*Interview relevance*: "Why is global/static mutable state bad?" is a common Java interview question. The answer: shared state causes unpredictable behavior in multi-instance and multi-threaded contexts, and makes unit testing unreliable.

**TK-3: Every state change must be explicitly persisted through the abstraction**
`trip.setStatus(COMPLETED)` changes the in-memory object. But `tripRepository.save(trip)` is the explicit command that tells the repository "this is the new truth." In a database-backed repo, without `save()`, the status update is lost on restart. DIP means driving ALL side effects through the injected abstractions — not relying on reference semantics of the in-memory implementation.
*Interview relevance*: "Why did your status update not persist?" — nine times out of ten it's because `save()` was never called after a mutation. Interviewers look for explicit persistence calls in every state-changing method.

**TK-4: State → Persist → Notify is the correct order, always**
Change the state first. Persist it. Only then send the notification. If you notify before persisting, a persistence failure leaves the system in a state where the user was told "trip cancelled" but the database still shows PENDING. The notification is observable and can't be taken back; the state must be committed before sending it.
*Interview relevance*: Event ordering is tested in distributed systems and event-driven design questions. "What happens if the notifier throws before you set the status?" is a follow-up you should be ready for.

**TK-5: `bookTrip` should notify the rider — don't forget the notification in the booking flow**
The spec says bookTrip should notify: `"Trip booked: <tripId> | Fare: ₹<fare>"`. Your implementation saves the trip and returns it but never calls `riderNotifier.notify()`. A rider who books a trip and gets no confirmation is a broken user experience.
*Interview relevance*: Reviewing requirements line-by-line before coding prevents silent omissions. "Does the user get a notification on booking?" is a product-level question that maps directly to one method call.
