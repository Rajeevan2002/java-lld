# Review — Interface Segregation Principle (ISP)
Phase 2, Topic 4 | Scenario B: Smart Home Device System

---

## Your IdentifiableDevice Approach — Two Approaches Compared

You invented `IdentifiableDevice` to solve the `getOnlineDevices()` problem. This is the right instinct. Here are the two clean approaches:

### Approach A (Your approach): `Switchable extends IdentifiableDevice`

```java
interface IdentifiableDevice { String getDeviceId(); }

interface Switchable extends IdentifiableDevice {
    void turnOn();
    void turnOff();
    boolean isOn();
}
// SmartHub can call s.getDeviceId() on any Switchable — no cast, no instanceof
```

**Pro**: Clean — `getDeviceId()` is available directly on every `Switchable`. No casting in `getOnlineDevices()`.
**Con**: Every `Switchable` is now *contractually* required to have a device ID, even if some future device doesn't have a meaningful ID (e.g., a virtual/unnamed toggle). The `Switchable` interface now carries an identity concern — which is technically a mild ISP issue on the interface itself.

### Approach B: Keep `Switchable` clean; use a separate `NotifiableDevice` / `IdentifiableDevice` list in SmartHub

```java
interface Switchable { void turnOn(); void turnOff(); boolean isOn(); }
// SmartHub keeps a parallel list:
private final List<Switchable>         switchables = new ArrayList<>();
private final List<IdentifiableDevice> identifiables = new ArrayList<>();

public void addSwitchable(Switchable s) {
    switchables.add(s);
    if (s instanceof IdentifiableDevice) {              // only one acceptable instanceof
        identifiables.add((IdentifiableDevice) s);
    }
}

public List<String> getOnlineDevices() {
    // Requires parallel index tracking — fragile when lists can diverge
    // Better: use a single Map<Switchable, String> instead
}
```

**Better variant of B**: Use a `Map<Switchable, String>` keyed on the device, with the ID as value:

```java
private final Map<Switchable, String> switchableIds = new LinkedHashMap<>();

public void addSwitchable(Switchable s, String deviceId) { switchableIds.put(s, deviceId); }

public List<String> getOnlineDevices() {
    List<String> ids = new ArrayList<>();
    for (Map.Entry<Switchable, String> e : switchableIds.entrySet()) {
        if (e.getKey().isOn()) ids.add(e.getValue());
    }
    return Collections.unmodifiableList(ids);
}
```

**Pro of B**: `Switchable` stays a pure switch-behavior interface — no identity contamination.
**Con of B**: `addSwitchable` now needs a `deviceId` argument (API change), or the `instanceof` workaround.

**Verdict**: Your Approach A is pragmatic and clean. In interview settings, both are acceptable — the key is that you can articulate the trade-off. Approach A is simpler; Approach B is purer from a strict ISP standpoint.

---

## What You Got Right

- **`FatSmartDevice` interface**: All 13 methods correctly declared. This is the ISP violation proof — well done.
- **`ViolatingSmartBulb`**: Correctly implements only `turnOn`/`turnOff`/`isOn` and throws `UnsupportedOperationException("SmartBulb: not supported")` for all other 10 methods — demonstrates the ISP violation exactly as required.
- **All 6 role interfaces**: `Switchable`, `TemperatureControllable`, `MusicPlayable`, `Lockable`, `NotifiableDevice`, `EmergencyCallable` — all correctly defined with the right method signatures.
- **`SmartLock` — clean implementation**: `turnOn`/`turnOff`/`isOn` are simple (no throws), `lock`/`unlock`/`isLocked` correct, `callEmergency()` works. Best implementation among the four devices.
- **`SmartThermostat.setTemperature()` validation**: `celsius < 10 || celsius > 35` correctly catches both bounds.
- **`SmartSpeaker.showNotification()` validation**: Validates `message == null || message.isBlank()` — good defensive check (the spec doesn't require it, but it's a sensible guard).
- **`SmartHub` — no `instanceof` in `allOn`, `allOff`, `setAllTemp`, `triggerEmergency`**: Pure polymorphic loops — exactly what ISP enables.
- **`SmartHub.getOnlineDevices()` returns `Collections.unmodifiableList()`**: Test 11b passes.
- **`IdentifiableDevice` interface + `Switchable extends IdentifiableDevice`**: A clean, compiler-enforced way to access `getDeviceId()` from `SmartHub.getOnlineDevices()` — the right instinct for avoiding casting.
- **`SmartHub` registration null-checks**: Each `addXxx()` throws `IllegalArgumentException` for null — correct boundary validation.
- **`SmartSpeaker.playMusic()` validation**: Correctly throws `IllegalArgumentException` for null/blank track.

---

## Issues Found

### Issue 1 — Bug (Runtime): `SmartSpeaker.playMusic()` does not set `currentTrack`

**Severity**: Bug (Test 5 fails at runtime)

`playMusic()` prints the track name but never assigns `this.currentTrack = track`. So `getCurrentTrack()` always returns `null` even after playing a song.

**Your code:**
```java
@Override public void playMusic(String track) {
    if (track == null || track.isBlank()) {
        throw new IllegalArgumentException("Track cannot be null");
    }
    System.out.println("Currently Playing Track : " + track);
    // ← MISSING: this.currentTrack = track
}
```

**Fix:**
```java
@Override public void playMusic(String track) {
    if (track == null || track.isBlank()) {
        throw new IllegalArgumentException("Track cannot be null");
    }
    this.currentTrack = track;   // ← assign the state
    System.out.println("[SmartSpeaker:" + deviceId + "] Playing: " + track);
}
```

**Why it matters**: Test 5 calls `speaker.playMusic("Bohemian Rhapsody")` then asserts `getCurrentTrack().equals("Bohemian Rhapsody")` — returns `null` instead. A speaker that "plays" a song but doesn't remember what it's playing is a broken state machine.

---

### Issue 2 — Bug: `SmartThermostat` constructor throws wrong exception type

**Severity**: Bug (wrong exception semantics)

`SmartThermostat(null)` throws `UnsupportedOperationException` — which means "this operation is not supported". The correct exception for invalid input is `IllegalArgumentException`.

**Your code:**
```java
public SmartThermostat(String deviceId) {
    if (deviceId == null || deviceId.isBlank()) {
        throw new UnsupportedOperationException("Device Id cannot be null / Blank");
        //        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^ wrong type
    }
}
```

**Fix:**
```java
public SmartThermostat(String deviceId) {
    if (deviceId == null || deviceId.isBlank()) {
        throw new IllegalArgumentException("deviceId cannot be blank");
    }
}
```

**Why it matters**: Callers catching `IllegalArgumentException` for input validation will miss this — it propagates as an unexpected `UnsupportedOperationException`. In code reviews, the wrong exception type is flagged as a correctness bug.

---

### Issue 3 — Design: `SmartBulb.turnOn()` and `SmartThermostat.turnOn()` throw for already-on state

**Severity**: Design

The spec says `turnOn()` sets `on = true` and prints a message — it should be **idempotent** (calling it twice should be harmless). Throwing `UnsupportedOperationException` for "already on" is semantically wrong — `UnsupportedOperationException` means "this method is not supported", not "invalid state". If you must guard, throw `IllegalStateException`. But for power switches the correct contract is: calling `turnOn()` on an already-on device is a no-op or a log statement, not an exception.

**Your code:**
```java
@Override
public void turnOn() {
    if (on) {
        throw new UnsupportedOperationException("Bulb is already on!!!");
        //        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^ wrong type, wrong design
    }
    on = true;
}
```

**Fix:**
```java
@Override
public void turnOn() {
    on = true;   // idempotent — calling twice is safe
    System.out.println("[SmartBulb:" + deviceId + "] Turned ON");
}
```

**Why it matters**: `hub.allOn()` calls `turnOn()` on every Switchable. If any device is already on, the whole operation throws — breaking the bulk-operation contract. Real smart home systems handle redundant on/off commands gracefully.

---

### Issue 4 — Design: `SmartHub` lists are not `final`

**Severity**: Design

The four lists are declared without `final`, which means they could theoretically be reassigned. The spec requires them to be private; declaring them `final` structurally enforces that the list reference is never replaced.

**Your code:**
```java
private List<Switchable> switchables;
private List<TemperatureControllable> temperatureControllables;
```

**Fix:**
```java
private final List<Switchable>              switchables             = new ArrayList<>();
private final List<TemperatureControllable> temperatureControllables = new ArrayList<>();
```

**Why it matters**: `final` on a field is a structural guarantee — no code path (including a future subclass) can replace the list with null. This is the Java equivalent of "set once in constructor, never changed."

---

### Issue 5 — Minor: Unexposed getter methods on `SmartHub` not required by spec

**Severity**: Minor

You added `getSwitchables()`, `getTemperatureControllables()`, `getMusicPlayables()`, `getEmergencyCallables()` to `SmartHub`. The spec says "SmartHub lists are private (not accessible directly)". These weren't required and technically expose the list structure (even if unmodifiable). They don't cause test failures but add surface area not called by any test.

---

## Score Card

| Requirement | Result | Notes |
|---|---|---|
| `FatSmartDevice` with all 13 methods | ✅ | Correct |
| `ViolatingSmartBulb` throws for non-switch methods | ✅ | Correct message |
| `Switchable` interface: turnOn/turnOff/isOn | ✅ | Extends IdentifiableDevice — valid approach |
| `TemperatureControllable` interface | ✅ | Correct |
| `MusicPlayable` interface | ✅ | Correct |
| `Lockable` interface | ✅ | Correct |
| `NotifiableDevice` interface | ✅ | Correct |
| `EmergencyCallable` interface | ✅ | Correct |
| `SmartBulb`: deviceId final, validates constructor | ✅ | Correct |
| `SmartBulb`: turnOn/turnOff idempotent (no throws) | ❌ | Throws UnsupportedOperationException for already-on/off |
| `SmartThermostat`: constructor uses IllegalArgumentException | ❌ | Throws UnsupportedOperationException instead |
| `SmartThermostat`: setTemperature validates 10–35 | ✅ | Correct |
| `SmartSpeaker`: playMusic sets currentTrack | ❌ | State not updated — Test 5 fails |
| `SmartSpeaker`: playMusic validates blank track | ✅ | Correct |
| `SmartSpeaker`: showNotification prints message | ✅ | Correct |
| `SmartLock`: default locked = true | ✅ | Correct |
| `SmartLock`: callEmergency prints message | ✅ | Correct |
| `SmartHub`: 4 private typed lists | ⚠️ | Private ✅ but not `final` |
| `SmartHub`: allOn/allOff — no instanceof | ✅ | Correct |
| `SmartHub`: setAllTemp — no instanceof | ✅ | Correct |
| `SmartHub`: triggerEmergency — no instanceof | ✅ | Correct |
| `SmartHub`: getOnlineDevices() returns unmodifiable | ✅ | Correct |
| `SmartHub`: registration null-checks | ✅ | Correct |

---

## Key Takeaways — Do Not Miss These

**TK-1: State assignments must follow validation — never skip the assignment**
When a method validates input and then does work, the assignment of state is the "work" — don't forget it. `playMusic()` validates, prints, but skips `this.currentTrack = track`. The method signature promises to start playing; the state must reflect that.
*Interview relevance*: Missing state assignments are a classic "it looks right but doesn't work" bug. Reviewers look for whether all paths (validation pass → state update) are complete.

**TK-2: `UnsupportedOperationException` means "not supported" — not "invalid state" or "invalid input"**
`UnsupportedOperationException` is for methods that are not implemented (like the fat interface violation you demonstrated). For invalid input, use `IllegalArgumentException`. For invalid object state, use `IllegalStateException`. Using the wrong exception confuses callers and is flagged in every Java code review.
*Interview relevance*: Exception semantics are tested directly in Java code reviews and in interview design questions about exception hierarchies.

**TK-3: Power switches should be idempotent — turning on an already-on device is a no-op, not an error**
Real-world hardware controls (network switches, smart home APIs, relay controllers) are idempotent by design. Calling `turnOn()` twice doesn't break the device — it just stays on. Throwing for repeated calls breaks bulk operations like `allOn()`.
*Interview relevance*: Idempotency is a fundamental concept in distributed systems and API design. Being able to name and apply it to method design shows systems-level thinking.

**TK-4: Always declare collection fields `final` in Java**
`private final List<X> things = new ArrayList<>()` ensures the list reference is fixed. The list's contents can still change (add/remove), but the field itself can never be reassigned to null or a different list. This is structural safety that doesn't cost anything.
*Interview relevance*: `final` fields are a standard Java defensive-programming practice. Interviewers look for it as a signal that the candidate understands immutability at the field level.

**TK-5: `IdentifiableDevice` as a separate interface is a clean ISP approach — understand the trade-off**
Your `Switchable extends IdentifiableDevice` is pragmatic and works. The strict ISP alternative keeps `Switchable` as a pure behavior interface and has `SmartHub` manage identity through a parallel map. The right choice depends on whether "being identifiable" is truly a universal property of all Switchables in your domain. Being able to articulate both approaches and choose deliberately is what makes a design discussion strong.
*Interview relevance*: "Why did you design it this way?" is the most common follow-up in a design interview. Having a clear trade-off answer ("Approach A is simpler; Approach B is purer") shows mature design thinking.
