# Review — Facade Pattern (Scenario B: Travel Booking System)

## What You Got Right

1. **All 5 fields declared `private final`** — the subsystem references are immutable after construction, which is the correct Object Facade structure and makes the class thread-safe for reads.

2. **Constructor injection** — all five services are injected via the constructor rather than created inside the class. This keeps subsystem dependencies visible and makes the Facade testable with test doubles.

3. **Correct `bookTrip` orchestration sequence** — all 11 steps are present and in the exact order specified: search → book → find hotel → reserve → find car → rent → charge → build details → send email → print → return `TripBooking`. This is the core of what a Facade does.

4. **Correct `cancelTrip` orchestration sequence** — all 7 steps are present and in the right order: cancel flight → cancel reservation → return car → refund → build details → send cancellation → print.

5. **`TripBooking` constructed correctly** — all five fields (`flightRef`, `hotelRef`, `carRef`, `receiptId`, `customerEmail`) are passed to the constructor in the correct order, and `customerEmail` is threaded through so `cancelTrip` can use it.

6. **No static state** — all fields are instance-level, so multiple `TravelFacade` instances work independently.

7. **`main()` never calls a subsystem directly** — the design constraint is respected; all calls go through `facade`.

---

## Issues Found

**1.**
- **Severity**: Design
- **What**: Null-checking all five service constructor parameters is validation that doesn't belong in the Facade. The Facade's job is orchestration; checking preconditions is the caller's responsibility.
- **Your code**:
```java
if (flightService == null) {
    throw new IllegalArgumentException("Flight Service cannot be null!!");
}
// ... (repeated 4 more times)
```
- **Fix**: Remove all null checks. Trust the caller:
```java
TravelFacade(FlightService flights, HotelService hotels,
             CarRentalService cars, PaymentService payment, EmailService email) {
    this.flights = flights;
    this.hotels  = hotels;
    this.cars    = cars;
    this.payment = payment;
    this.email   = email;
}
```
- **Why it matters**: This is the same error flagged in the Adapter review — business/defensive logic in a structural pattern class. If null-safety is needed, it belongs at the injection site (DI framework or factory), not inside the Facade itself.

---

**2.**
- **Severity**: Design
- **What**: Null-checking the `booking` parameter in `cancelTrip` is input validation, not orchestration.
- **Your code**:
```java
if (booking == null) {
    throw new IllegalArgumentException("Booking cannot be null !!!");
}
```
- **Fix**: Remove it. `cancelTrip` should just delegate.
- **Why it matters**: The spec defines `cancelTrip(TripBooking booking)` with no null-handling requirement. A `NullPointerException` from the first `booking.flightRef` access is the natural failure mode and gives the caller a clear stacktrace. A manually thrown `IAE` adds noise and couples the Facade to a validation concern.

---

**3.**
- **Severity**: Minor
- **What**: Orphaned TODO 4 step comments (lines 211–219 in the file) were left between the `YOUR WORK STARTS HERE` banner and the `TravelFacade` class — outside the class body they belong to.
- **Why it matters**: Leftover scaffolding comments in production code create confusion about whether the feature is truly complete.

---

## Score Card

| Requirement | Result |
|---|---|
| 5 `private final` subsystem fields | ✅ |
| Constructor accepts all 5 subsystems, stores them | ✅ |
| `bookTrip` — 11-step orchestration in correct order | ✅ |
| `bookTrip` — builds details string with correct format | ✅ |
| `bookTrip` — sends confirmation email | ✅ |
| `bookTrip` — prints `[TravelFacade] Trip booked: ...` | ✅ |
| `bookTrip` — returns `new TripBooking(...)` with all 5 fields | ✅ |
| `cancelTrip` — 7-step orchestration in correct order | ✅ |
| `cancelTrip` — sends cancellation email with booking details | ✅ |
| `cancelTrip` — prints `[TravelFacade] Trip cancelled for ...` | ✅ |
| No static state in `TravelFacade` | ✅ |
| `main()` never calls subsystem classes directly | ✅ |
| No input validation inside the Facade | ❌ |
| No orphaned scaffolding comments | ⚠️ |

---

## Key Takeaways — Do Not Miss These

1. **TK-1: Facade orchestrates, it does not validate.** The Facade's single responsibility is knowing the correct sequence of subsystem calls. Null checks, range validation, and business rules belong in the layer that *calls* the Facade, not inside it. This is now the second exercise in a row where this boundary was crossed — make this a reflex.

2. **TK-2: Constructor injection is the correct way to supply subsystems.** Injecting via the constructor (rather than `new`-ing subsystems inside the Facade) is what makes the Facade testable. In an interview, always say: "I'd inject the subsystems so they can be replaced with test doubles."

3. **TK-3: The Facade owns the sequence; the client owns the intent.** `bookTrip("London", "Paris", ...)` is intent. Steps 1–11 are sequence. These two concerns must live in different places. If the sequence changes (e.g. charge before email), only the Facade changes — no client is affected.

4. **TK-4: `TripBooking` as a return type makes the Facade's API complete.** Returning a value object that captures all generated IDs allows `cancelTrip` to work without the client ever needing to remember individual IDs. This is a common interview ask: "How does the client know what to cancel?" — the Facade's `bookTrip` returns everything needed.

5. **TK-5: Facade does not prevent direct subsystem access — Proxy does.** A Facade is opt-in simplification. If an interviewer asks "how do you stop clients from bypassing the Facade?", the correct answer is package-private access modifiers or Java modules — not the Facade pattern itself.

---

## Reference Solution

See `FacadeReference.java` in this directory.

Test 10 in the reference verifies that a second `TravelFacade` instance with its own fresh services works correctly — specifically confirming that no static state leaks between instances, which is the second most common mistake after adding validation inside the Facade.
