# Review: Interfaces vs Abstract Classes
**Topic:** Phase 1 — OOP Fundamentals / Topic 3
**Reference solution:** `NotificationReference.java` (same directory)

---

## What You Got Right

1. **`Sendable` and `Loggable` as pure interfaces** — no fields, no constructors, just method contracts. Exactly the right choice because they represent capabilities, not identity.

2. **`default getLastLog()` correctly implemented in the interface** — `getLastLog()` lives in `Loggable` with a body, so every implementor gets it for free without overriding. You used `getLast()` (Java 21 List API) — modern and clean.

3. **`getChannel()` declared abstract in `Notification`** — this is the sharp decision of the exercise. `getSummary()` needs to call `getChannel()`, but `Notification` doesn't implement `Sendable`. Making `getChannel()` abstract in `Notification` means the template method can call it without coupling the abstract class to the interface. Well reasoned.

4. **`SMSNotification` correctly excludes `Loggable`** — interface segregation in practice. SMS has no audit trail, so it shouldn't be forced to fake one. This is the structural point of the exercise and you got it.

5. **`EmailNotification.getLogHistory()` returns `Collections.unmodifiableList()`** — correct encapsulation. Callers can read, not mutate.

6. **`NotificationService` stores `List<Sendable>`** — coded entirely to the interface. `sendAll()`, `getByChannel()`, `getHighPriority()` all work without ever knowing a concrete type.

7. **`getByChannel()` and `getHighPriority()` return unmodifiable filtered views** — wrapped correctly with `Collections.unmodifiableList()`.

8. **`getLoggable()` uses `instanceof` check** — appropriate here: you're checking for a *different* interface than the stored type. This is one of the spec-permitted uses of `instanceof`.

9. **All subclass constructors chain via `super()`** — every concrete class correctly delegates base field initialization to `Notification(recipient, message)`.

10. **`Instant.now().toEpochMilli()` for `createdAt`** — more idiomatic than `System.currentTimeMillis()`. Good Java 21 instinct.

---

## Issues Found

### Issue 1 — Bug: `SMSNotification.validateContent()` has two errors in one line
**Severity: Bug**

`"!"` is used instead of `"+"`, and `||` is used instead of `&&`. The intent was: phone starts with `"+"` AND message ≤ 160 chars. Both conditions must hold.

```java
// Your code — wrong operator AND wrong string literal
return (phoneNumber.startsWith("!") || getMessage().length() <= 160);
```

```java
// Fix — correct literal AND correct operator
return phoneNumber.startsWith("+") && getMessage().length() <= 160;
```

**Why it matters:** With `||`, a valid long message (> 160 chars) still sends if the check on `"!"` is ever true. With `"!"` instead of `"+"`, any phone without a `"!"` prefix only passes based on message length — meaning a phone number like `"9876543210"` (no country code) would still succeed. In a real SMS gateway, this would cause delivery failures that are silent from the application's perspective.

The tests happen to pass because the specific test data doesn't expose this — but try `SMSNotification("X", "Hi", "9876543210")` and `send()` returns `true` when it should return `false`.

---

### Issue 2 — Bug: `getSummary()` throws instead of returning a description
**Severity: Bug**

The spec says `getSummary()` returns a formatted string. You throw `IllegalArgumentException` when `validateContent()` is false. This means calling `getSummary()` on any notification with bad content (e.g., a test with invalid data) crashes the program instead of describing the problem.

```java
// Your code — throwing from a getter-like method
public String getSummary(){
    if(validateContent()){
        return notificationId + getChannel() + " -> " + recipient + ": " + message;
    }
    throw new IllegalArgumentException("Notification Content is invalid!!!");
}
```

```java
// Fix — always return; include validity in the output
public String getSummary(){
    return notificationId + " " + getChannel() + " → " + recipient
         + ": " + message + " (valid=" + validateContent() + ")";
}
```

**Why it matters:** `getSummary()` is an informational method. Throwing from it violates the principle of least surprise — callers expect a description, not a crash. Throwing from getters/descriptive methods makes them unsafe to use in logging, debugging, or UI rendering contexts.

---

### Issue 3 — Design: `PushNotification.getLogHistory()` exposes the raw mutable list
**Severity: Design**

`EmailNotification` correctly wraps with `Collections.unmodifiableList()`. `PushNotification` returns the raw `ArrayList` directly.

```java
// Your PushNotification — mutable list returned
@Override public List<String> getLogHistory() { return logs; }
```

```java
// Fix — consistent with EmailNotification
@Override public List<String> getLogHistory() { return Collections.unmodifiableList(logs); }
```

**Why it matters:** A caller can do `push.getLogHistory().clear()` and silently wipe the audit trail. Inconsistency between two classes implementing the same interface is a design smell — the contract says "read the history", not "mutate it".

---

### Issue 4 — Design: `NotificationService` stores the caller's list reference directly
**Severity: Design**

```java
// Your code — stores reference; caller can mutate from outside
public NotificationService(List<Sendable> notifications){
    this.notifications = notifications;
}
```

```java
// Fix — defensive copy; service owns its own list
public NotificationService(List<Sendable> notifications){
    this.notifications = new ArrayList<>(notifications);
}
```

**Why it matters:** After construction, the caller can do `myList.clear()` and the service's internal state changes without calling any service method. This breaks encapsulation from the constructor.

---

### Issue 5 — Design: `getLoggable()` returns a mutable list
**Severity: Design**

```java
// Your code — returns raw ArrayList
public List<Loggable> getLoggable(){
    List<Loggable> loggables = new ArrayList<>();
    for(Sendable s : notifications){ if(s instanceof Loggable) loggables.add((Loggable) s); }
    return loggables;   // caller can add/remove from this
}
```

```java
// Fix
return Collections.unmodifiableList(loggables);
```

**Why it matters:** Consistent with your own `getByChannel()` and `getHighPriority()` — all filter methods should return unmodifiable views.

---

### Issue 6 — Minor: `Notification.counter` is not private
**Severity: Minor**

```java
static int counter = 0;   // package-private — accessible from anywhere in the package
```

```java
private static int counter = 0;  // only accessible within Notification
```

**Why it matters:** Another class in the same package could reset `counter = 0` and cause duplicate notification IDs. Internal counters are implementation details and must be `private`.

---

### Issue 7 — Minor: Missing `@Override` on `EmailNotification.send()`
**Severity: Minor**

```java
public boolean send(){   // missing @Override
```

```java
@Override
public boolean send(){
```

**Why it matters:** `@Override` lets the compiler verify the signature matches the interface. Without it, a signature typo (e.g., `send(String reason)`) becomes a new method silently, not an override.

---

### Issue 8 — Minor: `getSummary()` output missing space between ID and channel
**Severity: Minor**

```java
return notificationId + getChannel() + ...
// Output: [NOTIF-001]EMAIL → ...  ← ID and channel run together
```

```java
return notificationId + " " + getChannel() + ...
// Output: [NOTIF-001] EMAIL → ...  ← readable
```

---

## Score Card

| Requirement | Result | Note |
|-------------|--------|------|
| `Sendable` is an interface with 3 methods | ✅ | |
| `Loggable` is an interface with `log()`, `getLogHistory()` | ✅ | |
| `Loggable.getLastLog()` is a `default` method | ✅ | Uses Java 21 `getLast()` |
| `Notification` is abstract with all 4 final fields | ✅ | |
| `Notification.counter` is `private` | ❌ | Package-private |
| `Notification` validates null + blank for recipient and message | ✅ | |
| `getChannel()` abstract in `Notification` | ✅ | Smart design |
| `getSummary()` returns string (not throws) | ❌ | Throws on invalid content |
| `EmailNotification` implements `Sendable` + `Loggable` | ✅ | |
| `EmailNotification.send()` has `@Override` | ❌ | Missing annotation |
| `EmailNotification.getLogHistory()` unmodifiable | ✅ | |
| `SMSNotification` implements `Sendable` only | ✅ | Interface segregation |
| `SMSNotification.validateContent()` correct logic | ❌ | `"!"` typo + `\|\|` vs `&&` |
| `PushNotification` implements `Sendable` + `Loggable` | ✅ | |
| `PushNotification.getLogHistory()` unmodifiable | ❌ | Returns raw list |
| `NotificationService` stores `List<Sendable>` | ✅ | |
| `NotificationService` defensive copy in constructor | ❌ | Stores reference directly |
| `getByChannel()` returns unmodifiable list | ✅ | |
| `getHighPriority()` returns unmodifiable list | ✅ | |
| `getLoggable()` returns unmodifiable list | ❌ | Returns mutable list |
| All tests pass | ⚠️ | Tests pass with current test data; `validateContent` bug hidden |

---

## Key Takeaways — Do Not Miss These

**TK-1: `default` methods are the interface's way of evolving without breaking implementors**
> A `default` method in an interface has a body and is inherited by all implementors — they can override it, but don't have to.
> *Interview note:* The classic question is "what happens when two interfaces both have a `default` method with the same signature?" — the implementing class must `@Override` and resolve explicitly using `A.super.method()`.

**TK-2: Declare abstract methods in the abstract class when template methods need them**
> `getSummary()` in `Notification` calls `getChannel()`. Since `getChannel()` is defined in `Sendable` (which `Notification` doesn't implement), the solution is to also declare `getChannel()` abstract in `Notification`. This keeps the template method clean without coupling the abstract class to the interface.
> *Interview note:* This is a subtle but common design point. If an interviewer asks "how does `getSummary()` call `getChannel()`?", this is the answer.

**TK-3: Interface segregation means some classes deliberately NOT implementing certain interfaces**
> `SMSNotification` not implementing `Loggable` is not an oversight — it's intentional design. Forcing SMS to implement `log()` when it has no audit trail would be a fake implementation.
> *Interview note:* This is a direct preview of the Interface Segregation Principle (ISP) in Phase 2 — "don't force classes to implement methods they don't use."

**TK-4: All filtering methods in a service should return unmodifiable views**
> If `getByChannel()` and `getHighPriority()` return unmodifiable lists, `getLoggable()` must too. Inconsistency within the same class is a code smell and a maintenance trap.
> *Interview note:* An interviewer will notice if one method protects the list and another doesn't. Consistency is part of the design quality signal.

**TK-5: Validate logic before writing the `if` condition — say it out loud first**
> `validateContent()` for SMS should have been: "is it true that the phone starts with + AND the message is at most 160 chars?" — `&&`, not `||`. The typo `"!"` vs `"+"` is a keyboard slip; the `||` vs `&&` is a logic error. Both are caught by saying the condition aloud before typing.
> *Interview note:* In a live coding session, narrate your validation logic before typing it. This gives the interviewer a chance to correct you before you write the bug.

**TK-6: `getSummary()` and similar descriptive methods must never throw**
> Methods whose job is to describe, format, or log should always return, even if the data is invalid. Reserve exceptions for true failure states, not informational methods.
> *Interview note:* Throwing from a `getSummary()`-style method is an immediate red flag in code review. The caller expects a string, not a stack trace.
