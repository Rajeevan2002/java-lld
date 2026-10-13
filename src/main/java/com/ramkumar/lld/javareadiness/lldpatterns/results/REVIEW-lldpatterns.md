# Review — 4.2 Recurring LLD Patterns (Parcel Locker Service)

## What You Got Right
- **`Objects.requireNonNull(recipient, "...")` then `isBlank()`** — the NPE/IAE split matches the spec, and the checks run in the listed order with all fields assigned only after every check passes.
- **`counter + 1` as the candidate id, then `counter += 1` after the `Parcel` is built** — a failed `register` does not burn an id (Test 8 passes).
- **`LinkedHashMap` + `putIfAbsent(...) != null`** — one lookup, insertion order, no `containsKey` + `put`.
- **`Optional.ofNullable(store.get(id))`** and `List.copyOf(store.values())` — no null returns, and a real snapshot that doesn't leak the internal map.
- **`orElseThrow(() -> new NoSuchElementException(...))`** then `IllegalStateException` for the already-picked-up state, matching the exception tree (missing vs wrong state).
- **Per-instance counter and logging format** exactly as specified; `toString` uses a ternary and the exact literal text.
- Tests 1–6 and 8 print the expected output.

## Issues Found

### 1. Bug — `pendingFor` ignores the `recipient` argument
- **Your code:** `parcelRepository.findAll().stream().filter(parcel -> !parcel.isPickedUp()).toList();`
- **Output:** `pendingFor("Asha")` returns `[PCL-0002, PCL-0003]`, which includes Ben's parcel. Test 7 expects `[PCL-0003]`, `[PCL-0002]` and `[]`.
- **Fix:**
  ```java
  return parcelRepository.findAll().stream()
          .filter(p -> p.getRecipient().equals(recipient))
          .filter(p -> !p.isPickedUp())
          .toList();
  ```
- **Why it matters:** in a real locker, Asha would be shown, and could collect, Ben's parcels.

### 2. Minor — `Print:` is a Java label, not a comment
- **Your code:** `Print: System.out.printf("[LockerService] picked up %s%n", id);`
- **Fix:** delete the `Print:` text. A statement label compiles, so the compiler never warned you.
- **Why it matters:** it was copied from the spec text, and in a real codebase a stray label is confusing.

### 3. Minor — inconsistent access modifiers and naming
- **Your code:** `public` methods on package-private nested classes, `parcelRepository` as the field name, `@Override` missing on `toString`
- **Fix:** keep package-private (or consistently public), name the field `repo`, add `@Override` to `toString()`.
- **Why it matters:** `@Override` catches a misspelled `toString` at compile time.

## Score Card
| Requirement | Result |
|---|---|
| `Parcel` fields (3 final, `pickedUp` mutable) | ✅ |
| `Parcel` validation order, types and messages | ✅ |
| Getters, `markPickedUp`, `toString` format | ✅ |
| Repo: insertion-ordered map, `save` null/duplicate | ✅ |
| `findById` → `Optional` | ✅ |
| `findAll` unmodifiable snapshot | ✅ |
| Service: per-instance counter, `PCL-%04d` | ✅ |
| Failed `register` doesn't burn an id | ✅ |
| `pickUp` exceptions, mark, print, return | ✅ |
| `pendingFor` filters by recipient | ❌ |
| `pendingFor` excludes picked-up, unmodifiable | ✅ |
| `pendingFor` output matches Test 7 | ❌ |
| Code hygiene (`Print:` label, `@Override`) | ⚠️ |

## Key Takeaways — Do Not Miss These
1. **TK-1: Tick every parameter against the body.** A method that never reads one of its arguments is almost certainly wrong. *Interview value: an unused parameter is the quickest bug for an interviewer to spot.*
2. **TK-2: Re-read each `expected:` line against your actual output.** Your Test 7 output differed from the comment. *Interview value: running the examples is how you catch a bug before the interviewer does.*
3. **TK-3: Build the object with the candidate id, then commit the counter.** *Interview value: it shows you think about failure paths.*
4. **TK-4: Argument problem → IAE or NPE; object-state problem → ISE; missing lookup → NoSuchElementException or `Optional`.** *Interview value: the exception tree is a common follow-up.*
5. **TK-5: Return `List.copyOf(...)` or `.toList()`, never the internal collection.** *Interview value: it shows you protect encapsulation.*
6. **TK-6: Don't paste spec text into code.** `Print:` compiled only because Java allows labels. *Interview value: stray tokens that compile are worse than errors.*
