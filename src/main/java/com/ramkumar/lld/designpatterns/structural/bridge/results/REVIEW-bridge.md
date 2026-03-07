# Review — Bridge Pattern (Scenario B: Alert System)

## What You Got Right

1. **`protected final Channel channel` in `Alert`** — both modifiers correct and intentional: `protected` allows subclasses to call `channel.deliver()` directly; `final` ensures the bridge reference is set once and never accidentally swapped.

2. **`super(channel)` in both RefinedAbstraction constructors** — the bridge reference lives in `Alert`, not in the subclasses. Chaining via `super(channel)` stores it there. If either subclass stored its own separate field instead, the pattern would break down.

3. **`SimpleAlert.send()` delegates exactly as-is** — no modification, no wrapping, just `return channel.deliver(recipient, message)`. The formatting responsibility is zero here; all behaviour comes from the injected Channel.

4. **`PriorityAlert.send()` modifies before delegating** — `"[PRIORITY] " + message` before the delegate call is exactly the RefinedAbstraction's job: add its own layer of logic, then hand off to the Implementor.

5. **No `instanceof` anywhere** — `channel.deliver()` is called uniformly regardless of whether the channel is Email or SMS.

6. **Composition, not inheritance** — `Alert` holds a `Channel` reference; it does not extend `EmailChannel` or `SMSChannel`. This is what keeps the two hierarchies independent.

7. **All 10 test assertions passed** — including Tests 6 (channel-swap) and 7 (alert-type-swap), which directly demonstrate the Bridge independence property.

---

## Issues Found

**Issue 1 — Minor: `abstract String send(...)` should be `public abstract`**

- **Severity**: Minor
- **What**: The abstract `send()` method in `Alert` is declared package-private (no access modifier). The overrides in the subclasses are `public` — widening access on override is legal in Java, but the base declaration should match the intended visibility.
- **Your code** (`:104`):
  ```java
  abstract String send(String recipient, String message);
  ```
- **Fix**:
  ```java
  public abstract String send(String recipient, String message);
  ```
- **Why it matters**: If a caller outside the package holds an `Alert` reference and tries to call `send()`, the package-private abstract method blocks it — the public override doesn't help when dispatching through the base type.

**Issue 2 — Minor: Missing space before `{` in 4 declarations**

- **Severity**: Minor
- **What**: Java convention is `methodName() {` with a space before the opening brace; four declarations are missing it.
- **Your code** (`:100`, `:110`, `:121`, `:126`):
  ```java
  Alert(Channel channel){
  SimpleAlert(Channel channel){
  PriorityAlert(Channel channel){
  public String send(String recipient, String message){
  ```
- **Fix**:
  ```java
  Alert(Channel channel) {
  SimpleAlert(Channel channel) {
  PriorityAlert(Channel channel) {
  public String send(String recipient, String message) {
  ```
- **Why it matters**: Consistent formatting is a code-review gate item and signals attention to detail.

---

## Score Card

| Requirement | Result |
|---|---|
| `Channel` interface with `String deliver(String recipient, String message)` | ✅ |
| `protected final Channel channel` in `Alert` | ✅ |
| `Alert(Channel channel)` constructor assigns field | ✅ |
| `abstract String send(...)` declared with correct signature | ✅ |
| `send()` declared `public abstract` | ❌ (package-private) |
| `SimpleAlert(Channel c) { super(c); }` — correct chaining | ✅ |
| `SimpleAlert.send()` delegates as-is | ✅ |
| `PriorityAlert(Channel c) { super(c); }` — correct chaining | ✅ |
| `PriorityAlert.send()` prepends `"[PRIORITY] "` | ✅ |
| Composition — `Alert` holds `Channel`, does not extend a ConcreteImplementor | ✅ |
| No `instanceof` in `send()` | ✅ |
| All 10 test assertions passed | ✅ |

---

## Key Takeaways — Do Not Miss These

1. **TK-1: The bridge reference lives in the Abstraction base, not in the subclasses**
   `protected final Channel channel` is in `Alert`; both `SimpleAlert` and `PriorityAlert` call `super(channel)` to put it there. Storing a separate `channel` field in each subclass duplicates the bridge and defeats the purpose of the base class.
   *In interviews: "where is the bridge?" — point to the `protected final Implementor` field in the abstract base class.*

2. **TK-2: RefinedAbstractions call `super(impl)` — they do not store the bridge themselves**
   The bridge reference exists exactly once, in `Alert`. If a subclass stored its own copy, the base class field would be null — the bridge is broken and any base-class method that calls `channel.deliver()` would NPE.
   *In interviews: forgetting `super(impl)` is the most common Bridge implementation error — the NPE on the first delegate call is the symptom.*

3. **TK-3: Both hierarchies must grow independently — that's the whole point**
   Add a `PushChannel` (Test 9 in the reference): zero `Alert` code changes. Add an `EscalatingAlert`: zero `Channel` code changes. If adding one side requires touching the other, the pattern is not properly decoupled.
   *In interviews: always demonstrate independent extension with a 3rd implementor that plugs in instantly.*

4. **TK-4: Abstract methods intended for external callers must be `public abstract`**
   A package-private `abstract` method can be overridden with `public`, but callers holding the base type cannot invoke it — the package-private declaration wins at the call site.
   *In interviews: access modifier mismatches on abstract methods are a common Java API pitfall — know the widening rule and why the base visibility matters.*

5. **TK-5: Bridge vs Adapter — intent distinguishes them despite structural similarity**
   Bridge is designed upfront to prevent class explosion across two independent dimensions. Adapter is retrofitted to reconcile two incompatible existing interfaces. If you're joining things designed to be separate, it's Bridge; if you're wrapping something that already exists, it's Adapter.
   *In interviews: "how is Bridge different from Adapter?" is a near-universal follow-up in structural pattern discussions.*

---

## Reference Solution

See `BridgeReference.java` in this directory.

Extra Test 9 catches the most common Bridge mistake: the `PushChannel` (a third `ConcreteImplementor`) is added without modifying `Alert`, `SimpleAlert`, or `PriorityAlert`. If the student stored the `channel` reference in each subclass rather than in the base, adding a new channel would require modifying every `Alert` subclass — proving the bridge is not properly decoupled.
