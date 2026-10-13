# Review — 4.3 Java 8–21 Idioms (Insurance Claims Desk)

## What You Got Right
- **Sealed interface plus three records** — the record accessors `id()` and `amount()` satisfy the interface, so you wrote nothing extra. Compact constructors validate only and never assign fields.
- **Validation order and messages** match the spec on all three records, including `tripDays` last on `TravelClaim`.
- **Exhaustive switch expressions with no `default`** for `payout` and `label`, and a guard-free design with `Math.min` for the travel cap.
- **Record deconstruction patterns** in `describe`: components bound in the case label, no accessor calls.
- **Comparator** built with `comparingDouble(...).reversed().thenComparing(Claim::id)`, defined once and reused (Test 5 output is correct, and the input list is untouched).
- **`totalPayoutByType`** with `groupingBy(label, TreeMap::new, summingDouble(payout))` returned through a typed return, so inference works and keys are sorted.
- Tests 1–5 and 7 pass, apart from the `describe` casing (Issue 2).

## Issues Found

### 1. Bug — `largest` uses `max(...)` with a descending comparator
- **Your code:** `return claims.stream().max(claimComparator);`
- **Output:** Test 6 prints `H-2` and `A-3` (the smallest claim, and the tie loser). It expects `H-1` and `A-1`.
- **Fix:** `return claims.stream().min(REVIEW_ORDER);` because `min` returns the element that sorts first, and the review order puts the largest first.
- **Why it matters:** "largest" silently returns the smallest claim, so a reviewer sees the wrong claim first.

### 2. Bug — `describe` prints `Travel` instead of `TRAVEL`
- **Your code:** `String.format("Travel %s: %.2f [%d days]", ...)`
- **Output:** `Travel T-2: 1200.00 [3 days]`, but the spec says `TRAVEL T-2: 1200.00 [3 days]`.
- **Fix:** change the literal to `"TRAVEL %s: %.2f [%d days]"`.
- **Why it matters:** exact output text is checked literally in machine-coding rounds, and `label` already returns `TRAVEL`.

### 3. Minor — direct record field access in `payout`
- **Your code:** `autoClaim.atFault ? ...` and `healthClaim.hospitalised ? ...`
- **Fix:** `a.atFault()` and `h.hospitalised()`. It compiles only because the records are nested in the same top-level class.
- **Why it matters:** the accessor is the public API, and field access breaks the moment the record moves to its own file.

### 4. Minor — naming and tidy-ups
- **Your code:** `claimComparator` as a constant; `public` on methods of a package-private class; stray blank lines before closing braces.
- **Fix:** `REVIEW_ORDER`; drop `public`; remove blank lines.
- **Why it matters:** constants are `UPPER_SNAKE`, and consistent style reads as deliberate.

## Score Card
| Requirement | Result |
|---|---|
| Sealed `Claim` with `id()` and `amount()` | ✅ |
| Three records, component order | ✅ |
| Compact-constructor validation, order and messages | ✅ |
| `payout`: null check, three rules | ✅ |
| `payout`/`label` as switch expressions, no default | ✅ |
| `label` | ✅ |
| `describe` via record patterns | ✅ |
| `describe` output format | ❌ (`Travel` vs `TRAVEL`) |
| Review comparator defined once, amount desc then id asc | ✅ |
| `sortedForReview` new unmodifiable list, input untouched | ✅ |
| `largest` returns first in review order | ❌ |
| `largest` empty → `Optional.empty()` | ✅ |
| `totalPayoutByType` grouping, sorted keys, empty map | ✅ |
| Naming / style | ⚠️ |

## Key Takeaways — Do Not Miss These
1. **TK-1: `max(cmp)` returns what sorts LAST.** With a descending comparator the first element is `min`. *Interview value: this "wrong end of the comparator" slip is a classic and quick for an interviewer to spot.*
2. **TK-2: Compare every output line with the `expected:` comment.** Both bugs here showed up as a mismatch in the printed output. *Interview value: running the examples is how you find a bug before the interviewer does.*
3. **TK-3: Use accessors, not fields, on records.** `a.atFault()` is the API. *Interview value: it shows you treat records as value types, not as classes with public fields.*
4. **TK-4: A switch over a sealed type needs no `default`.** You got this right: a `default` would hide a missing case later. *Interview value: it is the main payoff of `sealed`.*
5. **TK-5: Define one comparator constant and reuse it for `sorted`, `min` and `max`.** *Interview value: one definition means one place to get the ordering wrong.*
