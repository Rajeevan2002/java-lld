# Review — Composite Pattern (Scenario B: Organisation Chart)

## What You Got Right

1. **`private final List<OrgUnit> members`** — the single most important decision in this exercise. Typing the list as the Component interface (not `List<Individual>` or `List<Team>`) is what allows composites to contain other composites at any depth. You got it right.

2. **`add(OrgUnit unit)` accepts the interface** — not `Individual` or `Team`. Combined with the interface-typed list, this is what makes Test 5 work: `company.add(engineering)` passes a `Team` into another `Team` with no cast, no instanceof, no special case.

3. **`getSalary()` uses no `instanceof`** — the loop calls `member.getSalary()` on every `OrgUnit`. Because `Team` also implements `OrgUnit`, the recursion just works for any depth without any type-checking logic.

4. **Recursive `display()` with growing indent** — `member.display(indent + "  ")` is exactly right. Each level adds two spaces; no global counter, no depth parameter, no manual bookkeeping. Test 6's 3-level output confirms the indentation is correct.

5. **`members` initialised in constructor** — `this.members = new ArrayList<>()` is correct. `final` ensures the reference never gets accidentally reassigned; mutating via `add()` is fine.

6. **`add()` not on `OrgUnit` interface** — kept correctly on `Team` only. Putting it on the interface would force `Individual` to implement it with either a no-op or an `UnsupportedOperationException` — both mislead callers.

7. **Empty team returns 0.0** — the loop simply doesn't iterate; no special case needed. Test 7 confirmed this.

8. **All 8 tests passed** — including Test 5 (nested getSalary) and Test 8 (polymorphic `OrgUnit[]`).

---

## Issues Found

**Issue 1 — Minor: `public` constructors on static inner classes**

- **Severity**: Minor
- **What**: Both `Individual` and `Team` constructors are marked `public`. For static inner classes used only within the same file, the conventional access is package-private (no modifier).
- **Your code** (`:86`, `:112`):
  ```java
  public Individual(String name, String role, double salary) { ... }
  public Team(String name) { ... }
  ```
- **Fix**:
  ```java
  Individual(String name, String role, double salary) { ... }
  Team(String name) { ... }
  ```
- **Why it matters**: `public` on an inner class constructor signals "this is part of the external API"; package-private signals "this is an implementation detail of the enclosing class." The intent distinction matters in code review.

**Issue 2 — Minor: Double space in variable initialisation**

- **Severity**: Minor
- **What**: Extra space before `=` in `teamSalary` declaration (`:129`).
  ```java
  double teamSalary  = 0.0;
  ```
- **Fix**:
  ```java
  double teamSalary = 0.0;
  ```
- **Why it matters**: Cosmetic, caught in code review.

**Issue 3 — Minor: Double blank line between constructor and `add()` method**

- **Severity**: Minor
- **What**: Two consecutive blank lines between the `Team` constructor and the `add()` method (lines 116–117).
- **Fix**: One blank line between methods is the Java convention.
- **Why it matters**: Inconsistent spacing signals inattention in a professional setting.

---

## Score Card

| Requirement | Result |
|---|---|
| `OrgUnit` interface with 3 correct method signatures | ✅ |
| `Individual` fields: `private final` name, role, salary | ✅ |
| `Individual` constructor assigns all 3 fields | ✅ |
| `Individual.getName()` returns name | ✅ |
| `Individual.getSalary()` returns field directly (no delegation) | ✅ |
| `Individual.display()` exact format string | ✅ |
| `Team` field `private final String name` | ✅ |
| `Team` field `private final List<OrgUnit> members` (interface type) | ✅ |
| `Team` constructor initialises members to `ArrayList` | ✅ |
| `Team.add()` accepts `OrgUnit` (not concrete type) | ✅ |
| `Team.getName()` returns name | ✅ |
| `Team.getSalary()` iterates via interface, no `instanceof` | ✅ |
| `Team.display()` header then `member.display(indent + "  ")` | ✅ |
| `add()` NOT on `OrgUnit` interface | ✅ |
| Constructors package-private (not `public`) | ❌ (both marked `public`) |
| All 8 tests passed | ✅ |

---

## Key Takeaways — Do Not Miss These

1. **TK-1: `children` must be `List<Component>` — the interface, never the concrete type**
   `private final List<OrgUnit> members` is what allows arbitrary nesting; `List<Individual>` collapses the tree to a flat list of leaves.
   *In interviews: the interviewer will ask "can a Team contain another Team?" — the answer is yes, and you prove it by showing the field type.*

2. **TK-2: Never use `instanceof` in the Composite's delegating methods**
   `member.getSalary()` dispatches correctly to both `Individual` and `Team` because they share the interface — no type check needed. Adding `instanceof` means every new node type breaks the method.
   *In interviews: if you write `instanceof` in getSalary(), the interviewer will add a third node type and ask you to extend — you'll have to change every composite method.*

3. **TK-3: The recursion terminates naturally at Leaf — no depth tracking needed**
   `member.display(indent + "  ")` works for any depth because `Individual.display()` just prints and returns. The tree structure itself is the termination condition.
   *In interviews: never add a depth counter or base-case check in the Composite's loop — the Leaf handles its own termination.*

4. **TK-4: `add()` belongs on Composite only, not on the Component interface**
   Putting `add()` on `OrgUnit` forces `Individual` (Leaf) to implement it, which produces a misleading no-op or `UnsupportedOperationException`. Leaves cannot have children — the interface should not suggest otherwise.
   *In interviews: this design trade-off (transparency vs safety) is a common follow-up — know that the "safe" approach keeps `add()` on Composite only.*

5. **TK-5: Polymorphic substitution is the payoff — callers never know leaf from composite**
   Test 8's `OrgUnit[] units = { alice, engineering, company }` iterates with `u.getSalary()` regardless of depth. This is why the pattern exists — the client never needs to unwrap the tree.
   *In interviews: always demonstrate this by showing an array or list of the Component interface type, not concrete types.*

---

## Reference Solution

See `CompositeReference.java` in this directory.

Extra Test 9 catches the two most common mistakes:
1. **`members` typed as `List<Individual>`** — `megaCorp.add(company)` throws `ClassCastException` at runtime because `company` is a `Team`, not an `Individual`.
2. **`getSalary()` using `instanceof` that skips Team children** — total would be wrong (returns 0 for Team sub-totals); expected `$485,000.00` would not match.
