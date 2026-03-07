# Review — Proxy Pattern (Scenario B: Report Portal Access Control)

## What You Got Right

1. **Correct interface declaration** — `ReportService` declares all three methods with the right signatures and return types. Once you add methods to the interface, `RealReportService` satisfies them automatically — you correctly understood the progressive compilation story.

2. **Validation order in `purgeOldReports` is correct** — `daysOld < 1` check fires first, then the role check. This means even ADMIN gets `IllegalArgumentException` for bad input, and you never have a role-check side-effect when the input is invalid.

3. **Exact exception messages** — Both `SecurityException` messages (`"Access denied: VIEWER cannot export data"`, `"Access denied: ANALYST cannot purge reports"`) match the spec exactly, and `"daysOld must be >= 1"` is exact. Tests 4, 6, and 7 all verified the message strings.

4. **Log before delegate** — the `[Proxy] ROLE → method(arg)` print fires before the real service call, and only fires when the call is authorised. Denied calls throw without printing.

5. **No validation inside `RealReportService`** — the real service is trusted; all guards live in the proxy.

6. **All 8 tests passed** — including the polymorphic Test 8.

7. **`daysOld < 1` before role check** — correctly ensures even ADMIN gets `IllegalArgumentException` for `daysOld = 0`.

---

## Issues Found

**Issue 1 — Design: Field and constructor typed as `RealReportService` instead of `ReportService`**

- **Severity**: Design
- **What**: The proxy's field is `private final RealReportService realReportService` and the constructor takes `RealReportService` — the concrete class, not the interface. This breaks the Proxy pattern's core principle.
- **Your code** (`:97–103`):
  ```java
  private final RealReportService realReportService;

  public AccessControlProxy(RealReportService reportService, Role role) {
      this.realReportService = reportService;
      ...
  }
  ```
- **Fix**:
  ```java
  private final ReportService service;

  AccessControlProxy(ReportService service, Role role) {
      this.service = service;
      ...
  }
  ```
- **Why it matters**: With the concrete type, you cannot pass a mock, a caching proxy, or another `AccessControlProxy` into this proxy — it only accepts `RealReportService`. The whole point of the pattern is that the proxy is blind to what it wraps as long as it satisfies the interface. Test 9 in the reference solution catches this: it passes one `AccessControlProxy` into another — which won't compile if the field is `RealReportService`.

**Issue 2 — Minor: `.equals()` used for enum comparison — should use `==`**

- **Severity**: Minor
- **What**: `role.equals(Role.VIEWER)` and `role.equals(Role.ADMIN)` — enum constants are JVM singletons; reference equality `==` is always correct and preferred.
- **Your code** (`:113`, `:126`):
  ```java
  if(role.equals(Role.VIEWER)) { ... }
  if(!role.equals(Role.ADMIN)) { ... }
  ```
- **Fix**:
  ```java
  if (role == Role.VIEWER) { ... }
  if (role != Role.ADMIN) { ... }
  ```
- **Why it matters**: Using `.equals()` on enums signals uncertainty about the Java enum contract. Reviewers flag this immediately.

**Issue 3 — Minor: Double space in `SecurityException` message**

- **Severity**: Minor
- **What**: Stray extra space before `role` in the purge error message.
- **Your code** (`:127`):
  ```java
  throw new SecurityException("Access denied: " +  role + " cannot purge reports");
  ```
- **Fix**:
  ```java
  throw new SecurityException("Access denied: " + role + " cannot purge reports");
  ```
- **Why it matters**: Cosmetic, but caught in code review.

---

## Score Card

| Requirement | Result |
|---|---|
| `ReportService` interface with 3 correct method signatures | ✅ |
| `private final ReportService service` field (interface type) | ❌ (typed as `RealReportService`) |
| `private final Role role` field | ✅ |
| Constructor accepts `ReportService` (interface type) | ❌ (accepts `RealReportService`) |
| `generateReport`: all roles allowed, log then delegate | ✅ |
| `exportData`: VIEWER → SecurityException with exact message | ✅ |
| `exportData`: ANALYST/ADMIN log then delegate | ✅ |
| `purgeOldReports`: `daysOld < 1` validation fires first | ✅ |
| `purgeOldReports`: non-ADMIN → SecurityException with exact message | ✅ |
| `purgeOldReports`: ADMIN logs then delegates | ✅ |
| Composition (not extending `RealReportService`) | ✅ |
| No `instanceof` checks | ✅ |
| All 8 tests passed | ✅ |

---

## Key Takeaways — Do Not Miss These

1. **TK-1: The proxy's field must be typed as the Subject interface, not the concrete class**
   `private final ReportService service` — not `private final RealReportService` — is the difference between a reusable Proxy and a class that happens to wrap one specific thing.
   *In interviews: if your proxy's constructor signature accepts a concrete class, the interviewer will ask "can you wrap a mock? can you wrap another proxy?" — the answer must be yes.*

2. **TK-2: Use `==` for enum comparisons, never `.equals()`**
   Enum constants are JVM singletons; `role == Role.ADMIN` is always correct, compile-checked, and reads exactly like the English logic.
   *In interviews: `.equals()` on enums is flagged in every Java code review — it signals a gap in enum fundamentals.*

3. **TK-3: Validate input before checking authorization**
   In `purgeOldReports`, the input guard (`daysOld < 1`) fires before the role guard. Bad data must never reach the authorization layer — this is a security principle.
   *In interviews: the order of guard clauses reveals how the candidate thinks about contract layering.*

4. **TK-4: The proxy intercepts every call transparently — the client never changes**
   Test 8 proves this: `ReportService ref = new AccessControlProxy(...)` — the client calls `ref.generateReport()` with no knowledge of the proxy. Substituting a proxy for the real subject without touching client code is the pattern's defining value.
   *In interviews: draw the arrow from Client → ReportService (interface) ← proxy and real subject both. The client never points at concrete types.*

5. **TK-5: Proxy vs Decorator — same structure, different intent**
   Both wrap an object behind the same interface. Decorator adds stackable behaviour; Proxy controls access/lifecycle. When you find yourself writing a single wrapper that intercepts calls for a cross-cutting concern (auth, caching, logging) rather than adding composable features, it's a Proxy.
   *In interviews: expect "how is this different from Decorator?" — the answer is intent and stacking model.*

---

## Reference Solution

See `ProxyReference.java` in this directory.

Extra Test 9 catches the most common mistake: wrapping one `AccessControlProxy` inside another. If the field/constructor is typed as `RealReportService` instead of `ReportService`, the line `new AccessControlProxy(innerProxy, Role.ADMIN)` will not compile — because `innerProxy` is an `AccessControlProxy`, not a `RealReportService`.
