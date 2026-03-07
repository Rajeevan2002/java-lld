# Review — Prototype Pattern Practice (GameCharacterPractice.java)

Phase 3, Topic 3.5 | Scenario B: Game Character Loadout Cloning

---

## What You Got Right

**1. Protected copy constructor with deep copy (GameCharacter)**
`this.inventory = new ArrayList<>(source.inventory)` — the most critical line in the entire exercise.
Creating a `new ArrayList` from the source list ensures clone and original have independent lists.
Getting this right means Tests 2 and 11 (the deep-copy isolation tests) pass.

**2. Copy constructor chaining pattern (Warrior, Mage, Archer)**
Every private copy constructor correctly calls `super(source)` first, then copies its own field.
This is the canonical Java Prototype pattern: base handles base fields (including the deep copy),
subclass handles only its own extra field. No duplication.

**3. Covariant return types on all three clone() methods**
`Warrior.clone()` returns `Warrior`, `Mage.clone()` returns `Mage`, `Archer.clone()` returns `Archer` —
not `GameCharacter`. This means the caller avoids a cast, which is what Test 5 implicitly checks
(`Archer aClone = a.clone()` without any `(Archer)` cast).

**4. `getInventory()` returns `Collections.unmodifiableList(inventory)`**
The raw list is never exposed. Test 6 proves this: `w.getInventory().add("Hack Attempt")` throws
`UnsupportedOperationException`. Any code path that returns `this.inventory` directly would fail here.

**5. `removeItem()` throws `NoSuchElementException` with exact message**
Message `"item not found: " + item` matches what Test 8 asserts character-for-character.
The implementation correctly uses `inventory.contains()` to detect the missing item before throwing.

**6. `abstract GameCharacter clone()` declared on the base class**
Test 10 (polymorphic clone via base reference) iterates a `GameCharacter[]` and calls `clone()` on each.
Without the abstract declaration on the base, this wouldn't compile.

**7. `addItem()` exception message exact match**
`"item must not be blank"` is exactly what Test 7 asserts via `e.getMessage()`.

**8. Getters and setters for all base and subclass fields**
`getName()`, `getLevel()`, `setName()`, `setLevel()`, `getArmor()`, `getManaPool()`,
`getArrowCount()` — all present and correct.

---

## Issues Found

### Issue 1 — Bug: `setArmor()` validates but never assigns

**Severity**: Bug

**What**: `Warrior.setArmor(int v)` validates the argument but the body is missing the actual assignment,
so calling it has no effect on the field.

**Your code**:
```java
public void setArmor(int v){
    if(v<=0) {
        throw new IllegalArgumentException("v has to > 0 ");
    }
    // ← this.armor = v; is missing
}
```

**Fix**:
```java
public void setArmor(int v) {
    this.armor = v;
}
```

**Why it matters**: Test 3 calls `wClone.setArmor(800)` — the clone's armor remains 350 (unchanged).
Any downstream check on `wClone.getArmor()` would silently return the wrong value. In an interview
setting, this would be caught immediately by a test case that checks the setter's effect.

---

### Issue 2 — Minor: Stale `// your code here` scaffold comments left in

**Severity**: Minor

**What**: Five places still contain the scaffold placeholder comment even though real code follows it.

**Your code** (Warrior copy constructor, representative):
```java
private Warrior(Warrior source) {
    super(source);
    // your code here      ← stale
    this.armor = source.armor;
}
```

**Fix**: Delete the `// your code here` line in each case.

**Why it matters**: Scaffold comments in submitted code signal that a checklist step was skipped;
reviewers notice immediately.

---

### Issue 3 — Minor: Extra `System.out.println` in `addItem()` not in spec

**Severity**: Minor

**What**: Every `addItem()` call prints `"Item : <item> added to the Inventory"`, polluting all test output.

**Your code**:
```java
inventory.add(item);
System.out.println("Item : " +  item + " added to the Inventory");
```

**Fix**:
```java
inventory.add(item);
```

**Why it matters**: Side effects not in the contract are a maintenance risk — callers that redirect stdout
to a log file get unexpected noise. Spec-adherence is a core interview signal.

---

### Issue 4 — Minor: `%s` instead of `%d` for int field in `Archer.toString()`

**Severity**: Minor

**What**: `Archer.toString()` uses `%s` for `arrowCount` (an `int`), not `%d`.

**Your code**:
```java
return String.format("Archer{name='%s', level=%d, arrowCount=%s, inventory=%s}",
        name, level, arrowCount, inventory);
```

**Fix**:
```java
return String.format("Archer{name='%s', level=%d, arrowCount=%d, inventory=%s}",
        name, level, arrowCount, inventory);
```

**Why it matters**: `%s` autoboxes the int and calls `toString()`, so the output is the same here —
but it's semantically wrong and will fail code review. `%d` is the correct specifier for integers.

---

### Issue 5 — Minor: Extra validation beyond spec in constructors

**Severity**: Minor

**What**: The normal constructor validates `name` is not blank and `level >= 0`; each subclass
constructor validates its numeric field is `> 0`. None of this was in the spec.

**Your code** (representative):
```java
if(name == null || name.isBlank()) {
    throw new IllegalArgumentException("Name cannot be null or Blank!!");
}
if(armor <= 0) {
    throw new IllegalArgumentException("Armor has to positive");
}
```

**Fix**: Remove the extra guards, or keep them only if the spec explicitly requires them.

**Why it matters**: Extra validation means Test 9 creates `new Warrior("Test", 1, 10)` — fine here,
but if a test case passed `level = 0` or `armor = 0` (valid in some domains), your extra guards
would throw while the reference passes. Validate exactly what the spec says — no more, no less.

---

## Score Card

| Requirement | Result |
|---|---|
| `GameCharacter` fields declared (`name`, `level`, `inventory`) | ✅ |
| Normal constructor: initialises name, level, empty ArrayList | ✅ |
| Protected copy constructor: copies name, level, deep-copies inventory | ✅ |
| `abstract clone()` declared on base | ✅ |
| `addItem()`: appends item, throws IAE("item must not be blank") | ✅ |
| `addItem()`: no extra side effects (no println) | ❌ |
| `removeItem()`: removes item, throws NSE("item not found: " + item) | ✅ |
| `getInventory()`: returns unmodifiable view | ✅ |
| Base getters/setters (`getName`, `getLevel`, `setName`, `setLevel`) | ✅ |
| `Warrior`: `int armor` field | ✅ |
| `Warrior`: normal constructor calls super, sets armor | ✅ |
| `Warrior`: private copy constructor calls super(source), copies armor | ✅ |
| `Warrior.clone()` → Warrior (covariant), returns new Warrior(this) | ✅ |
| `Warrior`: `getArmor()` / `setArmor()` — setter actually assigns field | ❌ |
| `Warrior.toString()` format exact | ✅ |
| `Mage`: `int manaPool` field | ✅ |
| `Mage`: normal constructor, copy constructor, clone() | ✅ |
| `Mage`: `getManaPool()` / `setManaPool()` | ✅ |
| `Mage.toString()` format exact | ✅ |
| `Archer`: `int arrowCount` field | ✅ |
| `Archer`: normal constructor, copy constructor, clone() | ✅ |
| `Archer`: `getArrowCount()` / `setArrowCount()` | ✅ |
| `Archer.toString()` format exact (uses `%d` for int) | ⚠️ |
| No `Cloneable` / `Object.clone()` used | ✅ |
| Deep copy: `new ArrayList<>(source.inventory)` | ✅ |
| Covariant return on all three `clone()` methods | ✅ |
| No instanceof chains | ✅ |
| Stale scaffold comments removed | ❌ |

**Score: 24 / 27**

---

## Key Takeaways — Do Not Miss These

**TK-1: A setter must assign — validation alone is not enough**
Every setter's final act must be `this.field = value;`; a setter that only validates but never assigns
is silently broken and extremely hard to debug because no exception is thrown.
*Interview note*: Reviewers check setters first — a missing assignment fails any mutation test case.

**TK-2: Covariant return is the correct Java idiom for Prototype**
Declaring `public Warrior clone()` instead of `public GameCharacter clone()` is not an accident —
it lets callers hold the result as `Warrior` without a cast, keeping the API clean.
*Interview note*: An interviewer who asks "why return Warrior and not GameCharacter?" wants to hear
exactly this: covariant return, caller avoids cast, compile-time type safety.

**TK-3: The protected copy constructor is the engine of the pattern**
Every clone() method is one line — `return new SubClass(this)`. All the work (deep copy, field
assignment) lives in the copy constructors. Keeping clone() trivial means there is only one place
to audit for correctness.
*Interview note*: If asked to explain the Prototype pattern in Java, lead with the copy constructor
chain, not the clone() method.

**TK-4: Validate exactly what the spec says — no more, no less**
Adding guards for `level < 0` or `armor <= 0` that the spec doesn't require means your code
rejects inputs the spec deems valid. Over-validation fails test cases that probe boundary values
the spec explicitly allows.
*Interview note*: Interviewers often probe the boundary with `level = 0` or `armor = 0`. If your
extra guard throws, you fail the test case even though the core pattern is correct.

**TK-5: Remove all scaffold markers before submitting**
`// your code here` inside implemented methods is a signal that the file was not cleaned up.
It costs a deduction even when the surrounding code is correct.
*Interview note*: Interviewers read every line. Scaffold noise makes it harder to assess your work
and signals lack of attention to detail.

**TK-6: Use `%d` for integers in `String.format`**
`%s` happens to work for int (via autoboxing to Integer then toString()), but `%d` is correct
and communicates intent. Mixing format specifiers misleads future readers.
*Interview note*: A format string is part of the output contract — use the specifier that matches
the data type.
