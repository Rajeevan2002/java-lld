# Review — Phase 4 Java Readiness | Library Loan Desk (`LoanDeskPractice.java`)

Run result of your file as submitted: Tests 1–5 pass. Test 6 crashes with
`NoSuchElementException: Loan not found: LN-001` and nothing after it runs.

---

## What You Got Right

- **`LoanStatus.canTransitionTo` as a switch expression** over `this`, with `case RETURNED, LOST -> false`. The rules live in the enum, the switch is exhaustive, and the compiler will force you to revisit it when a state is added.
- **`MembershipTier` as data-carrying constants**: three `private final` fields, a constructor, getters, and `fineFor` handling `daysLate <= 0`. No tier numbers leak into `LoanDesk`.
- **`Book` record with a compact constructor**, checks in the required order with exact messages. Test 3 passes, including value-based `equals`.
- **`Loan` is correctly shaped**: five `final` fields, only `status` mutable, constructor sets `ACTIVE`, and `daysLate` clamps with `Math.max(0, ...)` (the first draft threw here; you fixed it).
- **`checkout` argument validation**: all four checks, right order, right exception type and text.
- **Id generation**: `String.format("LN-%03d", ++counter)` with an instance counter, placed after the checks it does reach, so the one rejected case (duplicate ACTIVE book) consumes no id.
- **`findById` / `findAll`**: `Optional.ofNullable(store.get(id))` and `List.copyOf(store.values())` are exactly the idioms intended.
- **`markOverdue` mutates status, not map structure** inside the loop, which is safe, and it reuses `transition`.
- **`nextDue` returns `Optional.ofNullable(...)`**, never `null`.

## Issues Found

### 1. Bug — `transition` has an inverted condition
- **Severity**: Bug (breaks Tests 6–10; most of the desk is unusable)
- **What**: it throws "not found" when the id **exists**, and NPEs when it does not.
- **Your code**
  ```java
  if(store.containsKey(id)) throw new NoSuchElementException("Loan not found: " + id);
  Loan loan = store.get(id);
  ```
- **Fix**
  ```java
  Loan loan = findById(id).orElseThrow(() -> new NoSuchElementException("Loan not found: " + id));
  ```
- **Why it matters**: `containsKey` + `get` is two lookups and an easy `!` to forget; `orElseThrow` states the intent once and cannot be inverted.

### 2. Bug — `nextDue` comparison is backwards, so it always returns empty
- **Severity**: Bug
- **Your code**
  ```java
  int dueDay = Integer.MAX_VALUE;
  if(loan.getStatus() == LoanStatus.ACTIVE && dueDay < loan.getDueDay()) { ... }
  ```
  `MAX_VALUE < anything` is never true, so `smallestLoan` stays `null`. Even if flipped to `>`, a tie on `dueDay` is not broken by id.
- **Fix**
  ```java
  return store.values().stream()
          .filter(l -> l.getStatus() == LoanStatus.ACTIVE)
          .min(Comparator.comparingInt(Loan::getDueDay).thenComparing(Loan::getId));
  ```
- **Why it matters**: hand-rolled "track the best so far" loops are where off-by-direction bugs live; `min(Comparator)` can't have that bug and handles the empty case via `Optional`.

### 3. Bug — `checkout` never enforces the member's loan limit
- **Severity**: Bug / Missing validation (the 4th BASIC book is silently accepted)
- **Your code**: no limit check at all.
- **Fix**
  ```java
  long openForMember = store.values().stream()
          .filter(l -> l.getMemberId().equals(memberId))
          .filter(l -> !l.getStatus().isTerminal())
          .count();
  if (openForMember >= tier.getMaxLoans())
      throw new IllegalStateException("Loan limit reached for member: " + memberId);
  ```
- **Why it matters**: the limit is the core business rule of a lending desk; without it `MembershipTier.maxLoans` is dead data.

### 4. Bug — duplicate-book check only looks at `ACTIVE`
- **Severity**: Bug
- **Your code**
  ```java
  if(loan.getStatus() == LoanStatus.ACTIVE) { if(loan.getBook().isbn().equals(book.isbn())) throw ... }
  ```
- **Fix**: use the spec's definition of *open*: `.filter(l -> !l.getStatus().isTerminal()).anyMatch(l -> l.getBook().isbn().equals(book.isbn()))`.
- **Why it matters**: an `OVERDUE` book is the one most obviously still out; your version would lend it to a second member. This is the extra Test 11 in the reference.

### 5. Bug — `checkout` does not print the required line
- **Severity**: Minor/Bug against spec
- **Fix**: `System.out.printf("[LoanDesk] checkout %s%n", loan);` before `return loan;`.
- **Why it matters**: output format is part of the contract in machine-coding rounds.

### 6. Bug — `countByStatus` is not zero-filled, not ordered, and uses the wrong map
- **Severity**: Bug
- **Your code**
  ```java
  Map<LoanStatus, Long> statusCounts = new HashMap<>();
  ... containsKey / compute / put ...
  return Map.copyOf(statusCounts);
  ```
  Statuses with no loans are missing, and both `HashMap` and `Map.copyOf` have **unspecified iteration order**, so the printed form will not be `{ACTIVE=2, OVERDUE=2, RETURNED=0, LOST=0}`.
- **Fix**
  ```java
  Map<LoanStatus, Long> counts = new EnumMap<>(LoanStatus.class);
  for (LoanStatus s : LoanStatus.values()) counts.put(s, 0L);
  store.values().forEach(l -> counts.merge(l.getStatus(), 1L, Long::sum));
  return counts;
  ```
- **Why it matters**: `EnumMap` gives declaration order for free; `merge(k, 1L, Long::sum)` replaces the whole `containsKey/compute/put` branch.

### 7. Bug — `overdueReport` sorts the wrong way and has no tie-break
- **Severity**: Bug
- **Your code**
  ```java
  .sorted(Comparator.comparing(loan -> loan.daysLate(today)))
  ```
  This is ascending (spec: most days late first), has no `thenComparing(id)`, and boxes each int.
- **Fix**
  ```java
  .sorted(Comparator.comparingInt((Loan l) -> l.daysLate(today)).reversed()
          .thenComparing(Loan::getId))
  ```
  The explicit `(Loan l)` is required: with `loan -> ...` followed by `.reversed()` the compiler infers `Object`.
- **Why it matters**: with your code Test 8 would print `LN-003, LN-001`.

### 8. Design — `returnBook` duplicates the existence check with a different message
- **Severity**: Design
- **Your code**
  ```java
  if(!store.containsKey(id)) throw new NoSuchElementException(String.format("Loan Id %s doesn't exist%n", id));
  ```
  Different text from `transition`, and a stray `%n` in an exception message. The spec says unknown ids "fail exactly as in transition".
- **Fix**: delete that line; call `transition(id, RETURNED)` first, then `store.get(id)`.
- **Why it matters**: one place owns the error contract; duplicates drift apart.

### 9. Design — `isTerminal()` repeats the transition table
- **Severity**: Design (the spec asked to derive it)
- **Your code**: `return (this == RETURNED || this == LOST);`
- **Fix**: `return Arrays.stream(values()).noneMatch(this::canTransitionTo);`
- **Why it matters**: add a `CLAIMED` state and the hand-written list is silently wrong.

### 10. Minor — visibility and API leftovers
- `Loan.setStatus` is `public`; spec says package-private. Same for `public` on `Loan` getters (harmless but inconsistent with the rest of the file).
- `store.sequencedValues()` is Java 21-only and gives nothing over `store.values()`.
- Field type `LinkedHashMap<String, Loan>` should be declared `Map<String, Loan>` (program to the interface; the spec said so).
- Tests 1–10 were uncommented before the TODOs were done; uncomment each block only after its TODO.

## Score Card

| Requirement | Result |
|---|---|
| `LoanStatus` constants in order | ✅ |
| `canTransitionTo` rules in enum | ✅ |
| `isTerminal` derived, not duplicated | ⚠️ |
| `MembershipTier` data, getters, `fineFor` | ✅ |
| `Book` record + validation order/messages | ✅ |
| `Loan` fields (5 final + mutable status) | ✅ |
| `Loan` constructor sets `ACTIVE` | ✅ |
| `daysLate` never negative | ✅ |
| `setStatus` package-private | ⚠️ |
| `toString` format | ✅ |
| `checkout` argument checks | ✅ |
| `checkout` duplicate-book check (open = not terminal) | ❌ |
| `checkout` member limit | ❌ |
| `checkout` id only after checks pass | ✅ |
| `checkout` prints line | ❌ |
| `findById` Optional / `findAll` immutable | ✅ |
| `transition` errors + print | ❌ |
| `markOverdue` | ⚠️ (logic right, depends on broken `transition`) |
| `returnBook` via `transition`, fine, print | ⚠️ |
| `countByStatus` EnumMap, zero-filled | ❌ |
| `overdueReport` order + tie-break + immutable | ❌ |
| `nextDue` min + tie-break + Optional | ❌ |
| Counter per-instance | ✅ |
| No `isPresent()+get()` | ✅ |
| Comparator chaining, not hand-written compare | ⚠️ |

## Key Takeaways — Do Not Miss These

1. **TK-1: Never write `containsKey` then `get`.** Use `Optional.ofNullable(map.get(id)).orElseThrow(...)` (or `findById(...).orElseThrow(...)`). *Interview note: one inverted `!` here broke five tests; interviewers watch for this exact slip.*
2. **TK-2: Define "open" once and reuse it.** `!status.isTerminal()` decides both the duplicate-book rule and the member limit. *Interview note: checking only `ACTIVE` is the classic state-machine edge case they probe.*
3. **TK-3: Replace "track the best so far" loops with `min`/`max` + a `Comparator`.** `min(comparingInt(...).thenComparing(...))` returns an `Optional` and handles ties. *Interview note: shows you know the Comparator toolkit and avoid direction bugs.*
4. **TK-4: Descending + tie-break = `comparingInt(...).reversed().thenComparing(...)`, and annotate the lambda parameter.** *Interview note: the type-inference trap with `.reversed()` is a favorite "why doesn't this compile" question.*
5. **TK-5: Enum-keyed counts are `EnumMap`, seeded with zeros, filled with `merge(k, 1L, Long::sum)`.** *Interview note: it gives deterministic output order with no sorting code.*
6. **TK-6: One method owns each error contract.** `returnBook` calls `transition`; it doesn't re-implement the "not found" check. *Interview note: duplicated validation is how two messages for the same failure appear.*
7. **TK-7: Run every test block as you go.** Uncomment a block only after its TODO is done, and stop at the first failure. *Interview note: incremental verification is how you avoid debugging five bugs at once.*

## Lambda tip (your request)

Every loop you wrote has a stream/lambda equivalent that removes the bug it contained:

| Your loop | One-liner |
|---|---|
| duplicate-book `for` + nested `if` | `stream().filter(l -> !l.getStatus().isTerminal()).anyMatch(l -> l.getBook().isbn().equals(i))` |
| `nextDue` best-so-far loop | `stream().filter(ACTIVE).min(Comparator.comparingInt(Loan::getDueDay).thenComparing(Loan::getId))` |
| `containsKey / compute / put` | `counts.merge(status, 1L, Long::sum)` |

Recipe: the `if` condition becomes the `filter` lambda; the value you compute becomes the `map` lambda; if the lambda only forwards its argument to one method, write `Class::method`. The full step-by-step guide (five lambda shapes, 4-level shrinking, Comparator recipes, when not to use a lambda, and a practice ladder) is now in section **4b** of `../docs/README.md`.
