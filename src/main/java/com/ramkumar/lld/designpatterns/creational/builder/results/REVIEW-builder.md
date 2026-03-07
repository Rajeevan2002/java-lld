# Review — Builder Pattern (Creational)
Phase 3, Topic 3.4 | Scenario B: Server Configuration Builder

---

## What You Got Right

- **All 9 product fields declared `private final`** — the foundation of immutability. No field can be reassigned after construction.
- **Private constructor `ServerConfig(Builder b)`** — correctly forces all callers through the Builder; `new ServerConfig()` is a compile error everywhere else.
- **All 9 fields copied in the constructor** — every field from `b.host` through `b.keyPath` is transferred, nothing missed.
- **All 9 getters, zero setters** — the product is truly immutable after `build()` returns.
- **`toString()` includes exactly the right fields** — host, port, maxConnections, connectionTimeoutMs, readTimeoutMs, keepAlive, sslEnabled. certPath and keyPath correctly excluded for security.
- **Required fields `private final` in Builder** — `host` and `port` can never be reassigned after the constructor call.
- **Optional fields have the exact correct defaults** — `maxConnections = 100`, `connectionTimeoutMs = 5_000L`, `readTimeoutMs = 30_000L`, `keepAlive = false`, `sslEnabled = false`.
- **All 5 fluent setters return `this`** — method chaining works correctly; `builder12 == after1 == after2` will be true.
- **None of the fluent setters validate** — deferred validation is intact; setters only store.
- **All 7 validation rules in `validate()`** — correct order, exact messages.
- **IAE for single-field rules, ISE for cross-field SSL rules** — correct exception semantics throughout.
- **`build()` calls `validate()` then `return new ServerConfig(this)`** — the only correct pattern.
- **`Builder` is a static nested class** — access to `ServerConfig`'s private constructor is correct.

---

## Issues Found

### Issue 1 — Minor: Stale skeleton comments left in method bodies

- **Severity**: Minor
- **What**: All 5 fluent setter bodies still contain `// your code here`, and `build()` still has the skeleton hint comment.
- **Your code**:
  ```java
  public Builder maxConnections(int val) {
      // your code here      ← leftover scaffold
      this.maxConnections = val;
      return this;
  }

  public ServerConfig build() {
      // your code here
      validate();
      return new ServerConfig(this); // replace with: validate(); return new ServerConfig(this);
  }
  ```
- **Fix**:
  ```java
  public Builder maxConnections(int val) {
      this.maxConnections = val;
      return this;
  }

  public ServerConfig build() {
      validate();
      return new ServerConfig(this);
  }
  ```
- **Why it matters**: Dead comments are noise that make reviewers wonder if the method body is incomplete. Remove scaffold before submitting.

### Issue 2 — Minor: `certPath` and `keyPath` not explicitly initialized to `null`

- **Severity**: Minor
- **What**: The spec says `default = null`, but the declarations omit the explicit initializer.
- **Your code**:
  ```java
  private String certPath;
  private String keyPath;
  ```
- **Fix**:
  ```java
  private String certPath = null;
  private String keyPath  = null;
  ```
- **Why it matters**: Java defaults uninitialized reference fields to `null`, so this is functionally correct — but explicit `= null` signals intent and keeps the defaults table consistent with the other six fields. It also documents that `null` is the valid "not set" state, not an oversight.

### Issue 3 — Minor: Missing `@Override` on `toString()`

- **Severity**: Minor
- **What**: `toString()` overrides `Object.toString()` but lacks the annotation.
- **Your code**:
  ```java
  public String toString() { ... }
  ```
- **Fix**:
  ```java
  @Override
  public String toString() { ... }
  ```
- **Why it matters**: `@Override` makes the compiler verify you're actually overriding — if you typo the name as `tostring()`, without `@Override` it silently compiles as a new method and `println(obj)` uses `Object.toString()` instead.

---

## Score Card

| Requirement | Result |
|---|---|
| 9 product fields all `private final` | ✅ |
| Private constructor copies all 9 fields from Builder | ✅ |
| All 9 getters, no setters | ✅ |
| `toString()` — exact format | ✅ |
| `toString()` — excludes certPath and keyPath | ✅ |
| Builder required fields `private final` | ✅ |
| Builder optional fields with correct defaults | ⚠️ certPath/keyPath not explicit null |
| Builder constructor takes required fields only | ✅ |
| All 5 fluent setters return `this`, no validation | ✅ |
| `validate()` — all 7 rules in correct order | ✅ |
| `validate()` — IAE for single-field rules | ✅ |
| `validate()` — ISE for cross-field SSL rules | ✅ |
| `build()` calls `validate()` then returns product | ✅ |
| Builder is `static` nested class | ✅ |
| No stale scaffold comments | ❌ (7 leftover `// your code here` + 1 hint comment) |

---

## Key Takeaways — Do Not Miss These

**TK-1: Required fields go in the Builder constructor; optional fields get defaults.**
This is the structural contract of the Builder — the constructor signature tells the caller exactly what is mandatory without any runtime surprises.
*Why it matters in interviews*: Interviewers check whether you separate required from optional correctly; putting everything in the constructor defeats the pattern.

**TK-2: Validate in `build()`, never in individual setters.**
Deferred validation is what makes cross-field rules (ssl requires certPath) expressible — at setter time, the other field isn't set yet.
*Why it matters in interviews*: Candidates who validate in setters fail cross-field tests; it also makes the builder order-dependent, which breaks the fluent API contract.

**TK-3: `IllegalStateException` for cross-field constraints, `IllegalArgumentException` for single bad arguments.**
When ssl=true and certPath=null, neither argument alone is wrong — the *combination* is invalid, which is a state problem, not an argument problem.
*Why it matters in interviews*: Exception semantics are tested explicitly in code review rounds; using IAE everywhere signals you're guessing.

**TK-4: The product constructor must be `private`.**
Without `private`, a caller can write `new ServerConfig()` and bypass `validate()` entirely, getting an object with null host and port 0.
*Why it matters in interviews*: Interviewers specifically ask "how do you prevent direct construction?" — `private` constructor is the answer.

**TK-5: Remove scaffold before submitting — `// your code here` and hint comments are noise.**
Dead comments signal incomplete work to a reviewer even when the implementation is correct.
*Why it matters in interviews*: In a live coding session, leftover scaffold comments create a false impression of incompleteness and distract from the logic.
