# Review — Observer Pattern: Weather Station

---

## What You Got Right

- **`WeatherObserver` interface declared correctly** — `onReadingUpdate(double, double, double)` with `void` return type. This is the notification contract that makes the whole pattern work.
- **Snapshot copy in `recordReading()`** — `List<WeatherObserver> snapshot = new ArrayList<>(observers)` before the loop is the single most important implementation detail of this exercise. Test 8 confirms it protects against `ConcurrentModificationException` when an observer calls `unsubscribe()` mid-iteration.
- **`WeatherStation.observers` typed as `List<WeatherObserver>`** — interface type, not `ArrayList`. This is the right declaration: the field contract is the interface; the implementation detail (`ArrayList`) is only visible at construction time.
- **`subscribe()` null guard** — `o == null` check with exact message `"observer must not be null"` fires before the list is modified. Fail-fast at the boundary, not as a delayed NPE.
- **`unsubscribe()` no-op behaviour** — `List.remove()` returns `false` silently when the element is absent; no extra guard needed.
- **`AlertService` threshold check uses `>=`** — correctly fires at exactly the threshold (Test 4 uses `35.0°C` at threshold `35.0`).
- **All three observers use only method parameters, not fields, for `tempC`/`humidity`/`pressureHpa`** — the station's state is stored in the station; observers receive it as a push, not by querying back.
- **All 8 tests pass** — including the self-unsubscribing `oneShot` observer in Test 8 (no CME).

---

## Issues Found

**1.**
- **Severity**: Minor
- **What**: Unnecessary `public` on inner static class constructors and methods — `public PhoneDisplay(...)`, `public WeatherStation()`, `public void subscribe(...)`, etc. Inner static class members are package-private by default; `public` adds no value here.
- **Your code**:
  ```java
  public PhoneDisplay(String owner) { ... }
  public WeatherStation() { ... }
  public void subscribe(WeatherObserver o) { ... }
  ```
- **Fix**: Drop `public` from all inner-class member declarations.
- **Why it matters**: Signals unfamiliarity with Java access scoping; interviewers notice scope on every declaration.

**2.**
- **Severity**: Minor
- **What**: Explicit `this.tempC = 0.0; this.humidity = 0.0; this.pressureHpa = 0.0;` in the constructor — `double` fields default to `0.0` automatically; these three lines are redundant.
- **Your code**:
  ```java
  this.tempC       = 0.0;
  this.humidity    = 0.0;
  this.pressureHpa = 0.0;
  ```
- **Fix**: Remove the three lines; `double` fields are zero-initialised by the JVM.
- **Why it matters**: Redundant initialisers are noise that grows with every field added; trusting the language defaults is idiomatic Java.

**3.**
- **Severity**: Minor
- **What**: Double space before `heatThresholdC` in the `AlertService` condition — `tempC >=  heatThresholdC`.
- **Your code**:
  ```java
  if(tempC >=  heatThresholdC){
  ```
- **Fix**:
  ```java
  if (tempC >= heatThresholdC) {
  ```
- **Why it matters**: Style consistency — also missing the space after `if` and before `{`.

**4.**
- **Severity**: Minor
- **What**: TODO 4 comment block is still in the file above `AlertService` even though the class is fully implemented below it — leftover scaffolding.
- **Fix**: Delete the TODO comment block once the implementation is complete.
- **Why it matters**: Stale TODO comments in submitted code signal that the cleanup step was skipped; in a real PR review this would be flagged.

---

## Score Card

| Requirement | Result |
|---|---|
| `WeatherObserver` interface with correct `onReadingUpdate` signature | ✅ |
| `PhoneDisplay` — `private final String owner` field | ✅ |
| `PhoneDisplay` — correct print format | ✅ |
| `WebDashboard` — no extra fields, correct print format | ✅ |
| `AlertService` — `private final double heatThresholdC` field | ✅ |
| `AlertService` — fires at `>=` threshold, silent otherwise | ✅ |
| `WeatherStation.observers` typed as `List<WeatherObserver>` | ✅ |
| `WeatherStation` constructor initialises `ArrayList` | ✅ |
| `subscribe()` null guard with exact message | ✅ |
| `unsubscribe()` no-op if absent | ✅ |
| `recordReading()` uses snapshot copy | ✅ |
| `observerCount()` returns `observers.size()` | ✅ |
| No `instanceof` in `WeatherStation` | ✅ |
| No direct exposure of observer list | ✅ |
| All 8 tests pass | ✅ |
| No redundant field initialisers | ⚠️ |
| No unnecessary `public` on inner-class members | ❌ |
| No leftover TODO comments | ❌ |

---

## Key Takeaways — Do Not Miss These

**TK-1: The snapshot copy is the most important line in any Observer subject.**
`List<WeatherObserver> snapshot = new ArrayList<>(observers)` before the notification loop prevents `ConcurrentModificationException` when an observer calls `unsubscribe()` mid-iteration. Omitting it is a silent time-bomb that only surfaces under concurrent use.
*Interviewers specifically ask "what happens if an observer unsubscribes during notification?" — the snapshot answer is expected.*

**TK-2: The subject list must be typed as the interface, not the concrete collection.**
`private final List<WeatherObserver> observers` — not `ArrayList<WeatherObserver>`. The interface type is the public contract; the `ArrayList` is an implementation detail visible only at construction. This makes it easy to swap the backing collection (e.g., to `LinkedList`) without touching any code that reads the field.

**TK-3: Fail fast at `subscribe()`, not at notification time.**
Validating `o == null` in `subscribe()` gives a clear `IllegalArgumentException` at the source. Without it, the `null` enters the list silently and causes a `NullPointerException` during `recordReading()` — inside the notification loop, far from where the bug was introduced.

**TK-4: Observer vs Strategy — broadcasting vs swapping.**
Strategy replaces the whole algorithm (one strategy active at a time). Observer broadcasts to many observers simultaneously (all active at once). The confusion is common because both use interfaces — the difference is cardinality: 1 vs N.
*In interviews: "Why not just use Strategy?" is a common challenge — answer with cardinality.*

**TK-5: `double` primitives, `int`, `boolean`, and object references all have defined JVM default values.**
`double` → `0.0`, `int` → `0`, `boolean` → `false`, object reference → `null`. Explicit `= 0.0` in a constructor is redundant. Trust the language spec; removing the noise makes the constructor's true work (initialising non-default values) easier to spot.
