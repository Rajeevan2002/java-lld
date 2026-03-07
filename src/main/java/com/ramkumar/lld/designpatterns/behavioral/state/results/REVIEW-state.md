# Review — State Pattern: Document Approval Workflow

---

## What You Got Right

- **`Document.state` is `private`** — external callers cannot inspect or switch on the state field. All state-dependent behaviour is reached only through the context's action methods.
- **`setState()` is package-private** — only state classes in the same file can call it; clients cannot force arbitrary transitions.
- **Pure delegation in the context** — `submit()`, `approve()`, `reject()`, `getStatus()` each delegate to the current state without any `instanceof`, type-check, or equality test on the state field. This is the central requirement of the pattern.
- **All four methods implemented in every state** — including all three error-message methods in `ApprovedState` (terminal) and the two error-message methods in `DraftState`. Every state class fulfils the complete interface contract.
- **`ApprovedState` makes no `setState()` call** — correctly modelled as a terminal state. All three actions print rejection messages and leave the document unchanged.
- **`RejectedState.submit()` transitions back to `ReviewState`** — the re-entry path (REJECTED → IN_REVIEW) is correctly implemented, and the test proves it (Test 7: `getStatus()` returns `"IN_REVIEW"` after re-submission).
- **Transition-before-print order in DraftState and ReviewState** — `doc.setState()` is called before `System.out.printf()`. This matters: if `getTitle()` or `getStatus()` were called inside the print statement after a failed transition, the state would already be correct.
- **All 9 tests pass**, including the full reject → re-submit → approve lifecycle in Test 9.

---

## Issues Found

**1.**
- **Severity**: Minor
- **What**: Unused import `import javax.print.Doc;` at line 3.
- **Your code**:
  ```java
  import javax.print.Doc;
  ```
- **Fix**: Delete the import entirely. `javax.print.Doc` is an unrelated JDK interface; the exercise uses the inner class `Document`.
- **Why it matters**: Unused imports signal an incomplete cleanup pass; some CI pipelines treat them as warnings or errors.

**2.**
- **Severity**: Minor
- **What**: `Document` constructor and action methods are declared `public`; the spec requires `setState()` to be package-private, which you got right — but the others don't need `public` on inner static classes.
- **Your code**:
  ```java
  public Document(String title){ ... }
  public String getTitle()  { ... }
  public String getStatus() { ... }
  public void submit()      { ... }
  ```
- **Fix**:
  ```java
  Document(String title){ ... }
  String getTitle()  { ... }
  String getStatus() { ... }
  void submit()      { ... }
  ```
- **Why it matters**: Unnecessary `public` on inner class methods widens the published API; if these classes are extracted to their own files later, every `public` becomes a contract commitment.

**3.**
- **Severity**: Minor
- **What**: Stale TODO comment blocks for TODO 4 (lines 123–131) and TODO 7 (lines 205–210) were left in the file after implementing those classes.
- **Fix**: Delete TODO blocks once the corresponding TODO is complete.
- **Why it matters**: Stale `// TODO` comments mislead reviewers into thinking the class is unfinished.

---

## Score Card

| Requirement | Result |
|---|---|
| `DocumentState` interface — 4 method signatures | ✅ |
| `Document.state` is `private` | ✅ |
| `Document` constructor initialises `state = new DraftState()` | ✅ |
| `submit()`, `approve()`, `reject()` delegate to state with no inspection | ✅ |
| `getStatus()` delegates to `state.getStatus()` | ✅ |
| `setState()` is package-private | ✅ |
| No `instanceof` / no type-checking anywhere | ✅ |
| `DraftState` — `submit()` → ReviewState | ✅ |
| `DraftState` — `approve()`/`reject()` print errors, no transition | ✅ |
| `ReviewState` — `approve()` → ApprovedState | ✅ |
| `ReviewState` — `reject()` → RejectedState | ✅ |
| `ReviewState` — `submit()` prints "already under review" | ✅ |
| `ApprovedState` — terminal: all actions print errors, NO `setState()` | ✅ |
| `RejectedState` — `submit()` → ReviewState (re-entry) | ✅ |
| `RejectedState` — `approve()`/`reject()` print errors | ✅ |
| All 9 tests pass | ✅ |
| No unused imports | ❌ |

---

## Key Takeaways — Do Not Miss These

**TK-1: The context never inspects its own state — it only delegates.**
`Document.submit()` calls `state.submit(this)` and nothing else. No `if (state instanceof DraftState)`, no equality check, no switch. The moment the context inspects its state, the pattern breaks and the if/else problem returns.
*Interviewers ask: "how does the context know which behaviour to invoke?" — the answer is: it doesn't, and that's the point.*

**TK-2: Every state class must implement every interface method — even no-ops.**
A terminal state like `ApprovedState` must implement `submit()`, `approve()`, and `reject()` even though all three just print rejection messages. Leaving any method unimplemented causes a compile error; using a default `throw new UnsupportedOperationException()` causes a surprise runtime crash.
*In interviews: terminal state handling is a common gap — be explicit about it.*

**TK-3: `setState()` must be package-private — never public.**
If `setState()` is public, any external caller can force the document into any state at any time, bypassing all transition rules. Only state classes (in the same package/file) should call it; the lifecycle is the state machine's responsibility, not the client's.
*In interviews: "how do you prevent invalid state transitions?" — package-private `setState()` plus the state machine encoding is the answer.*

**TK-4: State instances should not be shared across context instances.**
Creating `new DraftState()` per transition (not a singleton) ensures that one document's state cannot bleed into another's. If state objects were `static` or cached, two documents could corrupt each other's lifecycle.
*Test 10 in the reference catches this: two independent Document instances that start in DRAFT and transition separately.*

**TK-5: State vs if/else — OCP and SRP are the reasons.**
Adding a new state to an if/else approach requires modifying every action method in the context. Adding a new state to the State pattern requires one new class — zero changes to the context or existing states. Each state class has exactly one reason to change: the rules of that state.
*In interviews: "why not just use a String field and switch statements?" — OCP is the answer.*
