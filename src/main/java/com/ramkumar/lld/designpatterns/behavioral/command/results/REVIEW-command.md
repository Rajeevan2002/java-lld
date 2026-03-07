# Review — Command Pattern: Smart Home Lighting

---

## What You Got Right

- **`Command` interface with `execute()` and `undo()`** — both void; correct contract. This two-method interface is the foundation that makes undo possible without the invoker knowing anything about receivers.
- **`previousLevel` captured in `execute()`, not the constructor** — `previousLevel = light.getBrightness()` appears as the first line of `execute()`, before `setBrightness()` is called. Test 5 verifies this: brightness was changed to 60 between construction and execution, and undo correctly restored 60 (not the 30 that existed at construction time). This is the most important design point in the exercise.
- **`previousLevel` is NOT `final`** — correctly allows assignment inside `execute()`.
- **LIFO history via `Deque<Command>`** — `history.push(c)` after `c.execute()` gives correct LIFO undo order. Test 6 confirms three mixed commands are undone in reverse order.
- **`SmartController.history` typed as `Deque<Command>`** — the invoker never references `TurnOnCommand` or any concrete type; the whole `undo()` path works through the interface alone.
- **Empty history guard** — `history.isEmpty()` check with exact print `"[Controller] Nothing to undo"` before any pop. Test 7 confirms both the first empty-history call and the second after the last command is undone.
- **Reverse-operation undo for `TurnOnCommand`/`TurnOffCommand`** — `TurnOnCommand.undo()` calls `light.turnOff()` and vice versa — clean symmetry, no state to save.
- **Batch execution (Test 8)** — four commands scheduled in an array and executed in a loop; LIFO undo gives correct reversal of the last two.

---

## Issues Found

**1.**
- **Severity**: Bug
- **What**: The exception message uses a plain hyphen (`-`) instead of an en dash (`–`), so the exact-match test assertion fails.
- **Your code**:
  ```java
  throw new IllegalArgumentException("brightness must be 0-100");
  ```
- **Fix**:
  ```java
  throw new IllegalArgumentException("brightness must be 0–100");
  ```
- **Why it matters**: Exception messages are API contracts — any caller catching the exception and matching the message string (logging, test assertions, monitoring) will silently diverge from the spec.

**2.**
- **Severity**: Minor
- **What**: Two unused imports from `java.util.concurrent` — `DelayQueue` and `TimeUnit` — are not used anywhere in the file.
- **Your code**:
  ```java
  import java.util.concurrent.DelayQueue;
  import java.util.concurrent.TimeUnit;
  ```
- **Fix**: Delete both lines.
- **Why it matters**: Unused imports are flagged by every linter and signal copy-paste from an unrelated source; in code review they raise questions about what the original intent was.

**3.**
- **Severity**: Minor
- **What**: Unnecessary `public` on inner static class methods — `public void turnOn()`, `public void execute()`, `public void undo()`, `public void execute(Command c)`, `public void undo()`, `public int historySize()` etc. Inner static class members are package-private by default.
- **Fix**: Drop `public` from all inner-class method declarations.
- **Why it matters**: Overly broad visibility on inner-class members signals unfamiliarity with Java access scoping.

**4.**
- **Severity**: Minor
- **What**: Missing spaces after `if` and before `{` in two places.
- **Your code**:
  ```java
  if(level < 0 || level > 100){
  if(history.isEmpty()){
  ```
- **Fix**:
  ```java
  if (level < 0 || level > 100) {
  if (history.isEmpty()) {
  ```
- **Why it matters**: Style consistency — every other conditional in the file uses the space; inconsistency suggests a rushed edit.

**5.**
- **Severity**: Minor
- **What**: `TODO 6` scaffolding comment was left in the file above `SmartController`, which is fully implemented below it.
- **Fix**: Delete the TODO comment block once the implementation is complete.
- **Why it matters**: Stale TODO comments in submitted code signal that the cleanup step was skipped.

---

## Score Card

| Requirement | Result |
|---|---|
| `Command` interface with `execute()` and `undo()` (both void) | ✅ |
| `Light` — `private final String name`, `boolean on`, `int brightness = 50` | ✅ |
| `Light.turnOn()` — sets `on = true`, correct print format | ✅ |
| `Light.turnOff()` — sets `on = false`, correct print format | ✅ |
| `Light.setBrightness()` — validates 0–100, correct print format | ✅ |
| `Light.setBrightness()` — exact exception message `"brightness must be 0–100"` | ❌ (used hyphen) |
| `TurnOnCommand` — holds receiver, correct execute/undo | ✅ |
| `TurnOffCommand` — holds receiver, correct execute/undo | ✅ |
| `SetBrightnessCommand` — `previousLevel` NOT final | ✅ |
| `SetBrightnessCommand` — `previousLevel` captured in `execute()`, not constructor | ✅ |
| `SmartController.history` typed as `Deque<Command>` | ✅ |
| `execute()` calls `c.execute()` then `history.push(c)` | ✅ |
| `undo()` — empty guard with exact print, then pop + `c.undo()` | ✅ |
| LIFO undo order | ✅ |
| No unused imports | ❌ |
| No unnecessary `public` on inner-class members | ❌ |

---

## Key Takeaways — Do Not Miss These

**TK-1: Capture previous state in `execute()`, never in the constructor.**
`previousLevel = light.getBrightness()` must be the first line of `execute()`. Capturing it in the constructor records the state at object-creation time — but the receiver may change between construction and execution (another command could run first). The "before" snapshot must be taken at the actual moment of execution.
*In interviews: "How does undo know what to restore?" — the answer is always "the command saves state just before it changes it."*

**TK-2: `previousLevel` must NOT be `final`.**
A `final` field can only be assigned in a constructor. Capturing state in `execute()` requires a non-final field. This is the only legitimate case in the Command pattern where a concrete-command field should not be `final`.
*Interviewers notice `final` on all fields and ask why — explaining this distinction shows deep understanding.*

**TK-3: The Invoker holds `Deque<Command>` — never a concrete command type.**
`SmartController` knows only `Command.execute()` and `Command.undo()`. It has zero knowledge of `TurnOnCommand` or `Light`. Adding a `DimCommand` tomorrow requires zero changes to `SmartController` — that is the Open/Closed principle in practice.
*In interviews: "What changes if you add a new command?" — the answer should always be "only a new class".*

**TK-4: `history.push(c)` must come AFTER `c.execute()`, not before.**
If you push before executing, a mid-execute exception leaves a command in the history that was never actually applied — `undo()` would then "reverse" an action that never happened.
*This ordering is a real production bug that causes phantom state corruption.*

**TK-5: Exception messages are exact-match API contracts.**
`"brightness must be 0-100"` vs `"brightness must be 0–100"` is the only difference that caused a test failure here. In production, monitoring systems, callers, and integration tests all match exception messages as strings — a one-character drift silently breaks them.
*Always copy-paste the exact message from the spec; do not retype it.*
