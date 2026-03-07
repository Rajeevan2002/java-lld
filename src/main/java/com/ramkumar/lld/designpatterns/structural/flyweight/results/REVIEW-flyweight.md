# Review — Flyweight Pattern: Chess Piece System

---

## What You Got Right

- **Correct intrinsic/extrinsic state split**: `name` and `color` are stored as `private final` fields in `PieceType`. `row` and `col` are received as parameters in `render()` and never assigned to fields — this is the defining rule of Flyweight.
- **`private final` on all intrinsic fields**: Guarantees the flyweight is immutable. Shared callers cannot corrupt each other's view.
- **Cache key format**: `color + "-" + name` (e.g., `"White-Pawn"`) correctly distinguishes `"White-Pawn"` from `"Black-Pawn"` and from `"Pawn-White"`. The separator prevents collisions.
- **`containsKey` + `put` pattern**: Clear and correct. `computeIfAbsent` is equally valid; you chose the readable option.
- **Identity equality in Test 2**: The factory guarantees `p1 == p2` (same reference), not just `p1.equals(p2)`. Your cache returns the exact cached object — this is correct.
- **No state corruption in Test 8**: The same `PieceType` (White Queen) renders correctly at `(7,3)`, then `(4,4)`, then back to `(7,3)` — proof that extrinsic state is never stored.
- **Cache size math**: 12 unique flyweights (6 piece names × 2 colors) for 32 physical chess pieces — Test 7 passes cleanly.
- **`render()` return value**: Returns the formatted string AND prints it — satisfying both the test assertion (`equals`) and the visual output requirement.

---

## Issues Found

**1.**
- **Severity**: Minor
- **What**: `render()` is declared `public` on an inner static class inside a top-level class. Inner class members default to package-private, which is the right scope here — `public` is unnecessary noise.
- **Your code**:
  ```java
  public String render(int row, int col) {
  ```
- **Fix**:
  ```java
  String render(int row, int col) {
  ```
- **Why it matters**: Overly broad visibility on inner-class methods signals unfamiliarity with Java access scoping; interviewers may ask why `public` was chosen.

**2.**
- **Severity**: Minor
- **What**: Missing space before `{` in the constructor declaration.
- **Your code**:
  ```java
  PieceType(String name, String color){
  ```
- **Fix**:
  ```java
  PieceType(String name, String color) {
  ```
- **Why it matters**: Style consistency — every other method in the file uses the space; a mismatch suggests a rushed edit.

**3.**
- **Severity**: Minor
- **What**: `getPieceType` calls `cache.containsKey(cacheKey)` and then `cache.get(cacheKey)` — two lookups for the "cache hit" path. A single `computeIfAbsent` or storing the result of `put` removes the redundancy. This is micro-level, but worth knowing.
- **Your code**:
  ```java
  if (!cache.containsKey(cacheKey)) {
      cache.put(cacheKey, new PieceType(name, color));
  }
  return cache.get(cacheKey);
  ```
- **Fix (one lookup on hit)**:
  ```java
  return cache.computeIfAbsent(cacheKey, k -> new PieceType(name, color));
  ```
- **Why it matters**: In a hot path with millions of pieces, the double lookup doubles map overhead. `computeIfAbsent` is the idiomatic Java 8+ solution.

---

## Score Card

| Requirement | Result |
|---|---|
| `PieceType` has `private final String name` and `private final String color` | ✅ |
| Constructor `PieceType(String name, String color)` | ✅ |
| `render(int row, int col)` prints correct format | ✅ |
| `render(int row, int col)` returns correct String | ✅ |
| `row` and `col` NOT stored as fields | ✅ |
| `PieceTypeFactory` has `private final Map<String, PieceType> cache` | ✅ |
| Cache initialized as `new HashMap<>()` | ✅ |
| Cache key format: `color + "-" + name` | ✅ |
| `getPieceType` returns cached instance on repeat calls | ✅ |
| `getPieceType` creates only one instance per unique key | ✅ |
| `getCacheSize()` returns `cache.size()` | ✅ |
| 8 White Pawns share 1 flyweight (Test 6) | ✅ |
| 32 pieces use 12 unique flyweights (Test 7) | ✅ |
| No state corruption across render calls (Test 8) | ✅ |
| All 11 assertions pass | ✅ |

---

## Key Takeaways — Do Not Miss These

**TK-1: Extrinsic state is always a parameter, never a field.**
If `row` and `col` were stored as fields, the same flyweight at two positions would corrupt each other — Test 8 is specifically designed to catch this. In interviews: "extrinsic state is passed in, not stored."

**TK-2: The factory cache key must include ALL intrinsic dimensions.**
`color + "-" + name` covers both axes. A key of just `name` would incorrectly share `"White-Pawn"` and `"Black-Pawn"` — 6 flyweights instead of 12.

**TK-3: Verify sharing with `==`, not `.equals()`.**
The flyweight guarantee is reference identity (`p1 == p2`). `.equals()` would also pass if you accidentally implemented `equals()` based on name/color — hiding the fact that two separate objects were created.

**TK-4: Flyweight ≠ Object Pool.**
Flyweight shares immutable objects simultaneously (no "return" step). Object Pool lends mutable objects one at a time (DB connections, thread workers). Mixing them up in interviews is a common mistake.

**TK-5: `computeIfAbsent` is the idiomatic single-lookup factory cache.**
`cache.computeIfAbsent(key, k -> new PieceType(name, color))` is one map lookup on the hit path vs two with `containsKey` + `get`. Prefer it in production code.

**TK-6: Intrinsic fields must be `final` — not just immutable by convention.**
`private String color` (without `final`) lets a subclass or reflection change the shared state. `private final` makes the immutability a compile-time guarantee. Interviewers will ask why.
