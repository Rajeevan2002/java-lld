# Review — Singleton Pattern (Creational)
Phase 3, Topic 3.1 | Scenario B: Game Score Board

---

## What You Got Right

- **`private static volatile GameScoreBoard instance`** — correct field declaration. `volatile` is mandatory for DCL; without it the JVM may reorder the write to `instance` and let another thread see a partially-constructed object.
- **Double-Checked Locking is textbook correct** — two null checks, `synchronized (GameScoreBoard.class)`, correct structure. This is the hardest part of the exercise and you got it right.
- **`boardCreatedAt` is `private final`** — correctly immutable; `reset()` does not reassign it.
- **`recordScore` validation** — null/blank player and `score < 0` both validated before any state change.
- **`Math.max` logic in `recordScore`** — the "keeps higher score" rule is functionally correct.
- **`getTopPlayers`** — validates `n <= 0`, uses `Math.min(n, getPlayerCount())` to cap the result, and returns `Collections.unmodifiableList`. All three requirements met.
- **`reset()`** — creates a new `HashMap`, does not touch `boardCreatedAt`, prints the correct message.
- **`getUptimeMillis()`** — correct subtraction, always non-negative.
- **Stream-based sort in `getTopNKeys`** — `Map.Entry.comparingByValue().reversed()` is the right comparator.

---

## Issues Found

### Issue 1 — Bug: Wrong exception type in constructor (reflection defence)
- **Severity**: Bug
- **What**: Constructor throws `IllegalArgumentException` instead of `IllegalStateException`, and the message is backwards.
- **Your code**:
  ```java
  if(instance != null){
      throw new IllegalArgumentException("Instance cannot be null");
  }
  ```
- **Fix**:
  ```java
  if (instance != null) {
      throw new IllegalStateException("Use getInstance()");
  }
  ```
- **Why it matters**: `IllegalArgumentException` means "you passed a bad argument" — there are no arguments here. `IllegalStateException` means "the object is in a state that makes this operation illegal." An interviewer scanning this will flag it immediately. The message "Instance cannot be null" is also backwards: `instance` **is** non-null — that's exactly why you're throwing. The message should tell the caller what to do.

### Issue 2 — Design: `getTopNKeys` is `public static`
- **Severity**: Design
- **What**: An internal helper method is exposed as a `public` API.
- **Your code**:
  ```java
  public static List<String> getTopNKeys(Map<String, Integer> map, int n) { ... }
  ```
- **Fix**:
  ```java
  private static List<String> getTopNKeys(Map<String, Integer> map, int n) { ... }
  ```
- **Why it matters**: Exposing implementation details as `public` couples any caller to internals you might want to change — and it breaks encapsulation inside a Singleton where information hiding is paramount.

### Issue 3 — Design: `recordScore` uses verbose `containsKey`/`put` instead of `Map.merge`
- **Severity**: Design
- **What**: Four lines of `if/else` do exactly what `Map.merge` was designed for in one.
- **Your code**:
  ```java
  if(scores.containsKey(player)){
      scores.put(player, Math.max(scores.get(player), score));
  } else {
      scores.put(player, score);
  }
  ```
- **Fix**:
  ```java
  scores.merge(player, score, Math::max);
  ```
- **Why it matters**: `Map.merge(key, value, remappingFn)` — if the key is absent, stores `value`; if present, stores `remappingFn(existing, value)`. This is the canonical one-liner for "insert or update with a function." Interviewers expect you to know it.

### Issue 4 — Minor: Inconsistent and informal error messages
- **Severity**: Minor
- **What**: `"Player cannot be null or blank!!"` vs `"Player cannot be null or Blank!!"` (inconsistent capitalisation), and `"Players count has to be atleast > 0 !!"` has "atleast" (two words) and double `!!`.
- **Fix**: Use uniform, professional messages without exclamation marks: `"player must not be null or blank"`, `"n must be >= 1"`.
- **Why it matters**: Exception messages become log entries and error responses in production. Double `!!` in a log at 3am signals "written under pressure" not "production-ready code."

---

## Score Card

| Requirement | Result |
|---|---|
| `static volatile` instance field declared | ✅ |
| `boardCreatedAt` is `private final` | ✅ |
| Constructor is `private` | ✅ |
| Reflection defence throws `IllegalStateException` | ❌ (throws `IllegalArgumentException`) |
| Reflection defence message: `"Use getInstance()"` | ❌ (`"Instance cannot be null"`) |
| Double-Checked Locking — outer null check | ✅ |
| Double-Checked Locking — `synchronized` block | ✅ |
| Double-Checked Locking — inner null check | ✅ |
| `recordScore` validates null/blank player | ✅ |
| `recordScore` validates `score >= 0` | ✅ |
| `recordScore` keeps the HIGHER score | ✅ |
| `recordScore` print format correct | ✅ |
| `getScore` throws for unknown player | ✅ |
| `getTopPlayers` validates `n >= 1` | ✅ |
| `getTopPlayers` returns sorted descending | ✅ |
| `getTopPlayers` caps at `Math.min(n, size)` | ✅ |
| `getTopPlayers` returns unmodifiable list | ✅ |
| `hasPlayer` validates input | ✅ |
| `reset` clears scores | ✅ |
| `reset` preserves `boardCreatedAt` | ✅ |
| `getTopNKeys` is `private` | ❌ (`public static`) |
| `Map.merge` used in `recordScore` | ❌ (verbose `if/else`) |

---

## Key Takeaways — Do Not Miss These

**TK-1: `IllegalStateException` vs `IllegalArgumentException` — choose semantically, not randomly.**
The exception type is part of your API contract. `IAE` = bad argument passed. `ISE` = object is in wrong state for this call. In a constructor with no parameters throwing because the singleton already exists, the state is wrong — `ISE` is the only correct choice. Interviewers notice mismatched exception types.

**TK-2: Reflection-defence message = the call to action.**
When an attacker (or careless developer) bypasses `getInstance()` via reflection, they see your exception message. "Instance cannot be null" tells them nothing. "Use getInstance()" tells them exactly what to do. Exception messages are user-facing documentation.

**TK-3: `Map.merge(key, value, remappingFn)` is the one-liner for "insert-or-update with a function."**
`merge` covers three cases atomically: absent (stores value), present (applies function), function returns null (removes key). It replaces the `containsKey` + `get` + `put` trio and is what Java interviewers expect when "keep the higher score" appears in a spec.

**TK-4: Minimum visibility always — every helper method starts `private` until proven otherwise.**
Making `getTopNKeys` `public static` exposes an internal implementation detail. The rule is: default to `private`, loosen only when you have a concrete caller outside the class. This prevents accidental coupling.

**TK-5: Test 13 passes with `IllegalArgumentException` — never trust a catch-all `Exception` test.**
Your code passes Test 13 even with the wrong exception type, because the test catches `Exception`. A good test would check `e.getCause() instanceof IllegalStateException`. Always prefer specific assertions over broad catches in test code. Test 14 in the reference solution demonstrates this.
