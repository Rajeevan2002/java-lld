# Review — Liskov Substitution Principle (LSP)
Phase 2, Topic 3 | Scenario B: Notification Channel System

---

## Understanding `final` Methods (Your Question)

`final` on a method **prevents any subclass from overriding that method**. It is a hard compiler guarantee — not a convention or a comment.

```java
abstract class Notification {
    //  ┌── final means: NO subclass can override this
    //  ↓
    public final int getMaxMessageLength() { return maxMessageLength; }

    // Without final, a bad subclass could do:
    // class BadPush extends Notification {
    //     @Override
    //     public int getMaxMessageLength() { return 0; }  // ← breaks invariant
    // }
    // With final, the compiler rejects this override entirely.
}
```

**Three reasons `final` on methods matters specifically for LSP:**

| Why final? | Effect |
|---|---|
| **Invariant enforcement** | `getMaxMessageLength()` is `final` → no subclass can return 0 or negative. The invariant (>= 10) is structurally guaranteed. |
| **Template Method safety** | `prepare()` is `final` → all subtypes get the same truncation logic. No SMS subclass can override `prepare()` to throw instead of truncate. |
| **Preventing accidental LSP violations** | `getChannelName()` is `final` → no subclass can return `null` or a different name. The postcondition ("returns non-null channel name") is compiler-enforced. |

**The rule to remember**: If a method's return value is part of the class's invariant or contract, make it `final`. Otherwise, a well-meaning subclass might override it and silently break the LSP contract.

---

## What You Got Right

- **`Notification` abstract class design**: `private final` fields, `protected` constructor, correct IAE validation for both `channelName` and `maxMessageLength < 10`. The invariants are structurally enforced at construction time.
- **`final` on key methods**: `getChannelName()`, `getMaxMessageLength()`, and `prepare()` are all correctly declared `final` — subclasses cannot override them to return invalid values or throw. This is LSP enforcement by the compiler.
- **`prepare()` truncation logic**: The substring logic `message.substring(0, maxMessageLength)` is correct for the length-guard path.
- **`send()` blank-recipient guard in all three channels**: Returns `false` gracefully instead of throwing — correct contract behaviour.
- **`NotificationService.broadcast()` — no `instanceof`**: Pure loop over channels, `channel.send()` polymorphically dispatched — exactly what LSP enables.
- **`NotificationService.getRegisteredChannels()` returns unmodifiable list**: `Collections.unmodifiableList()` applied correctly.
- **`NotificationService.register()` null check**: Throws IAE for null — good defensive boundary.
- **`assertLSP()` body correctly uncommented**: The six contracts are verified (channelName non-blank, maxLen >= 10, send doesn't throw for long/null messages, returns false for blank recipient).

---

## Issues Found

### Issue 1 — Bug (Compilation): All three concrete classes have wrong constructor signatures

**Severity**: Bug (Compilation — prevents the program from running)

Each subclass needs a **no-arg constructor** that calls `super("EMAIL", 500)` etc. Instead, you gave them 2-arg constructors that expect the caller to supply the channel name and limit. The test cases call `new EmailNotification()` (no args), which fails to compile.

**Your code:**
```java
static class EmailNotification extends Notification {
    public EmailNotification(String message, int messageLength) {
        super(message, messageLength);   // ← caller decides channelName and maxLength?
    }
}
// Test cases call: new EmailNotification()  ← compilation error
```

**Fix:**
```java
static class EmailNotification extends Notification {
    public EmailNotification() {
        super("EMAIL", 500);   // EmailNotification owns its own identity
    }
}
// Same pattern:
// SMSNotification()  { super("SMS", 160); }
// PushNotification() { super("PUSH", 100); }
```

**Why it matters**: Each channel type IS the authority on its own channel name and limits. Letting the caller pass the channelName breaks encapsulation — anyone could do `new SMSNotification("EMAIL", 999)`.

---

### Issue 2 — Bug: `assertLSP(null)` causes NullPointerException at runtime

**Severity**: Bug (Runtime crash)

After uncommenting the `assertLSP()` body, the placeholder `assertLSP(null)` call remains in main(). Casting `null` to `Notification` succeeds in Java, but `n.getChannelName()` on null immediately throws NPE.

**Your code:**
```java
assertLSP(email);
assertLSP(sms);
assertLSP(push);
System.out.println("Test 7 PASSED ...");
assertLSP(null);   // ← NPE: null.getChannelName() crashes here
```

**Fix:** Delete the `assertLSP(null)` line — it was a skeleton placeholder with no purpose once the body is implemented.

---

### Issue 3 — Design: `prepare()` converts blank/whitespace messages to `""` incorrectly

**Severity**: Design

You added `|| message.isBlank()` to the null check, so `prepare("   ")` returns `""`. The spec says only `null → ""`. A whitespace-only string is a valid non-null message that should pass through unchanged (or be truncated if too long) — not silently discarded.

**Your code:**
```java
public final String prepare(String message) {
    if (message == null || message.isBlank()) {  // ← isBlank() check is wrong here
        return "";
    }
    ...
}
// prepare("   ") → ""    (wrong — 3 spaces is a valid message)
```

**Fix:**
```java
public final String prepare(String message) {
    if (message == null) return "";   // only null → empty
    return message.length() > maxMessageLength
           ? message.substring(0, maxMessageLength)
           : message;
}
// prepare("   ") → "   "   (correct — passes through)
// prepare(null)  → ""      (correct — guarded)
```

**Why it matters**: An OTP SMS like `"  123456  "` is a valid whitespace-padded message. Silently stripping it to `""` and returning `true` is a data-loss bug invisible to the caller.

---

## Score Card

| Requirement | Result | Notes |
|---|---|---|
| `Notification`: `private final` fields | ✅ | Correct |
| `Notification`: validates `channelName` null/blank | ✅ | Correct |
| `Notification`: validates `maxMessageLength < 10` | ✅ | Correct |
| `Notification`: `getChannelName()` declared `final` | ✅ | Correct |
| `Notification`: `getMaxMessageLength()` declared `final` | ✅ | Correct |
| `Notification`: `prepare()` declared `final` | ✅ | Correct |
| `prepare()`: `null → ""` | ✅ | Correct |
| `prepare()`: blank/whitespace → pass through (not "") | ❌ | `isBlank()` wrongly discards valid messages |
| `prepare()`: truncates at `maxMessageLength` | ✅ | Correct |
| `EmailNotification`: no-arg constructor `super("EMAIL", 500)` | ❌ | 2-arg constructor breaks all test cases |
| `SMSNotification`: no-arg constructor `super("SMS", 160)` | ❌ | Same — 2-arg constructor |
| `PushNotification`: no-arg constructor `super("PUSH", 100)` | ❌ | Same — 2-arg constructor |
| `SMSNotification.send()`: calls `prepare()`, never throws | ✅ | Logic correct (only constructor is wrong) |
| `NotificationService`: no `instanceof` checks | ✅ | Correct |
| `NotificationService.broadcast()`: unmodifiable map | ✅ | Correct |
| `NotificationService.register()`: null guard | ✅ | Correct |
| `assertLSP(null)` placeholder removed from main | ❌ | Causes NPE at runtime |

---

## Key Takeaways — Do Not Miss These

**TK-1: Each channel class owns its own channel name and limit — no-arg constructor**
When a subclass's identity is fixed (SMSNotification is always SMS with 160 chars), the no-arg constructor calls `super("SMS", 160)` and the caller never supplies these values. Letting the caller pass the channel name breaks encapsulation.
*Interview relevance*: "Who should know the SMS 160-char limit — the caller or the SMS class?" Always the class. This is encapsulation at the hierarchy level.

**TK-2: `final` on a method = compiler-enforced LSP invariant**
Without `final`, any subclass can override `getMaxMessageLength()` to return 0, breaking the invariant silently. With `final`, the compiler makes this impossible. Use `final` on any method whose return value is part of the type's contract.
*Interview relevance*: "How do you enforce LSP in Java?" The two answers are: (1) `final` on invariant methods, (2) `final` fields (immutability). Both are structural — they don't rely on documentation or trust.

**TK-3: `isBlank()` ≠ `== null` — use only the check you actually mean**
`null` means "no value provided". `" "` means "the caller provided a whitespace string" — a valid, non-null value. For `prepare()`, only `null` warrants the `""` fallback. `isBlank()` silently discards valid messages and creates data-loss bugs that return `true` (success) while sending nothing.
*Interview relevance*: Confusing null-safety with blank-safety is one of the most common validation mistakes caught in Java code reviews.

**TK-4: The `assertLSP` method IS the LSP test — run it on every concrete subtype**
`assertLSP(Notification n)` represents all the code in the world that uses `Notification` as a type. If any subtype makes this method throw or produce wrong results, LSP is violated. Call it on each concrete type — all should pass without changing the method.
*Interview relevance*: "Can you show me how you'd test LSP?" — pointing to `assertLSP()` and running it on each subtype is the correct answer.

**TK-5: Remove scaffolding placeholders after uncommenting — they cause real bugs**
The `assertLSP(null)` call was a skeleton placeholder. Once you uncomment the method body, the placeholder causes a real NPE. Every TODO comment and placeholder you resolve must also be cleaned up.
*Interview relevance*: Leaving placeholder code in production is a code review red flag — it signals incomplete implementation.
