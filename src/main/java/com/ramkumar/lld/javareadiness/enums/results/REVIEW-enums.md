# Review — 4.4 Enums as Design Tools (Streaming subscription desk)

## What You Got Right
- **Constant-specific `quote`:** an `abstract double quote(int)` with a body inside each constant. The pricing rule lives in the enum, and a 4th plan won't compile until it has a rule.
- **`fromCode`:** the lookup map is built once in a static block that loops over `values()`. You trim, lowercase and handle `null`, and the message matches the spec.
- **`SubState` transitions:** an `EnumMap<SubState, EnumSet<SubState>>` wired in a static block, with `canMoveTo` and `isTerminal` reading from it. There is no `ordinal()`, no if-chain and no `default`.
- **`Subscription.moveTo`:** it asks the enum whether the move is allowed instead of re-implementing the rules. Constructor validation order and messages are exactly as specified, and fields are assigned only after every check passes.
- **`open()`:** it validates before touching the counter, and it computes the id from `counter + 1` and only increments after success, so no id is burned.
- **`find`:** `Optional.ofNullable(map.get(id))`.
- **`move`:** the unknown-id message is exact.
- **`quote`:** months are checked before the plan code.

## Issues Found

### 1. Bug — `countByState()` is missing the zero entries
- **Your code:** `stream().collect(groupingBy(Subscription::getState, () -> new EnumMap<>(SubState.class), summingInt(e -> 1)))`
- **Output:** `{TRIAL=1, ACTIVE=1, CANCELLED=1}`. The spec and Test 7 expect `{TRIAL=1, ACTIVE=1, PAST_DUE=0, CANCELLED=1, EXPIRED=0}`.
- **Fix:** zero-fill first, then count:
  ```java
  Map<SubState, Integer> counts = new EnumMap<>(SubState.class);
  for (SubState st : SubState.values()) counts.put(st, 0);
  for (Subscription s : byId.values()) counts.merge(s.getState(), 1, Integer::sum);
  return counts;
  ```
- **Why it matters:** `groupingBy` only creates keys it sees. A report that silently drops empty states breaks dashboards and any code that does `counts.get(PAST_DUE)` and expects a number.

### 2. Bug — message typo in `quote`
- **Your code:** `"months must be >=1"`
- **Fix:** `"months must be >= 1"`
- **Why it matters:** exact messages are compared literally. This is the same class of slip as the missing `P` in 4.1 and `Travel` vs `TRAVEL` in 4.3.

### 3. Design — `plansInUse()` returns a `HashSet`
- **Your code:** `...map(Subscription::getPlan).collect(Collectors.toSet())`
- **Fix:** `Collectors.toCollection(() -> EnumSet.noneOf(Plan.class))`, or a loop adding to `EnumSet.noneOf(Plan.class)`.
- **Why it matters:** the spec said "not HashSet". Enum hash codes are identity-based, so the order can differ between JVM runs. Your output happened to print `[FREE, BASIC]`, but the order is not guaranteed.

### 4. Design — `HashMap` for the subscription store
- **Your code:** `new HashMap<>()` for `subscriptionStore`
- **Fix:** `new LinkedHashMap<>()`
- **Why it matters:** the spec asked for insertion order, and any listing or reporting built on the store would come out shuffled.

### 5. Minor — cross-topic import and leftover code style
- **Your code:** `import com.ramkumar.lld.javareadiness.cheatsheet.code.TripDispatchDemo;` (unused)
- **Fix:** delete it, and replace the `java.util.*` and `Collectors` wildcard with specific imports once the streams are gone.
- **Why it matters:** a stray import from another topic's `code/` ties this file to it. This is the same slip as Issue 3 in the 4.1 review.

### 6. Minor — `toString` uses `String.format` on already-concatenated text
- **Your code:** `String.format(id + "[" + customer + ", " + ...)`
- **Fix:** return the concatenation directly.
- **Why it matters:** the concatenated text becomes the format string, so a customer named `50% off` throws `UnknownFormatConversionException`.

### 7. Minor — field access and unneeded `public`
- **Your code:** `subscription.state.isTerminal()`, and `public` on methods of package-private nested types.
- **Fix:** use `getState()` and drop `public`.
- **Why it matters:** it keeps encapsulation consistent and doesn't suggest an API that isn't there.

### 8. Minor — `canMoveTo` throws on `null`
- **Your code:** `throw new IllegalArgumentException("Target should not be null")` inside `canMoveTo`
- **Fix:** a predicate should return `false` (`contains(null)` is false on an `EnumSet`), and `moveTo` already has the spec'd null check.
- **Why it matters:** a boolean query that throws is a surprise for callers. The message also doesn't match the one in the spec.

## Score Card
| Requirement | Result |
|---|---|
| `Plan` constants, final fields, constructor, getters | ✅ |
| `Plan.quote` constant-specific (FREE / BASIC / PREMIUM discount) | ✅ |
| `Plan.fromCode` built once, trim, case-insensitive, exact message | ✅ |
| `SubState` transition table inside the enum, `isTerminal` | ✅ |
| `Subscription` fields, validation order, messages | ✅ |
| `Subscription.moveTo` delegates to the enum, exact messages | ✅ |
| `Subscription.toString` format | ⚠️ (works, but `String.format` misuse) |
| `open` id format and no burned id | ✅ |
| `find` returns `Optional` | ✅ |
| `move` unknown-id message | ✅ |
| Store keeps insertion order | ❌ (`HashMap`) |
| `countByState` zero-filled EnumMap | ❌ |
| `plansInUse` EnumSet, declaration order | ❌ (`HashSet`) |
| `quote` months check first, exact message | ❌ (`>=1`) |
| No `ordinal()`, no `default`, no String/int stand-ins | ✅ |
| Naming and cleanliness | ⚠️ |

## Key Takeaways — Do Not Miss These
1. **TK-1: `groupingBy` only makes keys it sees, so zero-fill an `EnumMap` from `values()` first.** *Interview value: "every state must appear in the report" is a common requirement, and the stream one-liner quietly fails it.*
2. **TK-2: If the spec says `EnumSet`/`EnumMap`, a `HashSet` or a `Collectors.toSet()` result is a failure even when the output looks right.** *Interview value: it shows you know enum hash order is not stable and that deterministic output matters.*
3. **TK-3: Diff your output against every `expected:` line.** *Interview value: a missing space in a message costs a test you otherwise passed (3rd review in a row with this).*
4. **TK-4: Put the rules in the enum and ask the enum.** You did this well for `quote` and `canMoveTo`. *Interview value: it is the answer to "how do you remove a switch(type) from this code".*
5. **TK-5: A boolean query should not throw; leave validation to the method that owns the spec.** *Interview value: predictable APIs read as senior code.*
6. **TK-6: Never pass concatenated user text as a format string.** *Interview value: `%` in a name is a classic hidden crash.*
7. **TK-7: Read the spec's data-structure words literally: "keeps insertion order" means `LinkedHashMap`.** *Interview value: structure choices are scored, not just outputs.*

## Reference Solution
`StreamingPlanReference.java` in this folder. It runs the 9 test blocks plus an extra block covering the typical mistakes (empty-desk zero fill, `plansInUse` order independent of insertion order, exact months message, `%` in a customer name). Compiled with `-Xlint:all` and run, and its output matches every expected value.
