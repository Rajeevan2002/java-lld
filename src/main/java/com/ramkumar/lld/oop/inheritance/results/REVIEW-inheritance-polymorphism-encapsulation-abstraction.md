# Review: Inheritance, Polymorphism, Encapsulation, Abstraction
**Topic:** Phase 1 — OOP Fundamentals / Topic 2
**Reference solution:** `PayrollReference.java` (same directory)

---

## What You Got Right

1. **`Employee` correctly declared `abstract`** — cannot be instantiated, forces subclasses to provide `calculateMonthlySalary()`. This is exactly the right use of abstraction.

2. **`@Override` on every overriding method** — all three subclasses annotate their `calculateMonthlySalary()`. This lets the compiler catch signature mismatches early. Never skip this.

3. **`super()` as first statement in every subclass constructor** — `FullTimeEmployee`, `PartTimeEmployee`, `ContractEmployee` all call `super(id, name, dept)` correctly. The compiler would have caught it if you missed it, but the habit matters.

4. **`employeeId` is `final`** — immutability enforced by the compiler. Once set, it cannot be accidentally reassigned.

5. **All salary fields are `private`** — `annualSalary`, `hourlyRate`, `contractAmount` are not accessible outside their respective classes. Clean encapsulation.

6. **`getEmployees()` returns `Collections.unmodifiableList()`** — the internal list is protected. Callers cannot mutate it from outside.

7. **`getHighestPaid()` uses stream + lambda without instanceof** — pure polymorphism. `PayrollProcessor` has no idea what concrete type it's comparing.

8. **`getTotalPayroll()` loops without instanceof** — same strength. One loop, any mix of types.

9. **`setName()` has blank validation** — controlled write access. This is the difference between a getter-setter pair and a proper encapsulated class.

---

## Issues Found

### Issue 1 — Bug: `FullTimeEmployee` rejects 0 salary — crashes Test 6
**Severity: Bug**

The spec says `annualSalary >= 0` (zero is valid). You wrote `<= 0.0`, which rejects zero. Test 6 passes `0` as the salary to trigger `UnsupportedOperationException` from the unmodifiable list — but your validation fires first with `IllegalArgumentException`, crashing the program before the list is even touched. **This is why your run crashes at Test 6.**

```java
// Your code — wrong
if(annualSalary <= 0.0){
    throw new IllegalArgumentException("Annual Salary cannot be negative or 0");
}

// Fix — allow 0 (spec says >= 0)
if (annualSalary < 0) {
    throw new IllegalArgumentException("Annual salary cannot be negative");
}
```

**Why it matters:** A validation that's stricter than the contract is a hidden bug. It passes your own tests but breaks callers who had every right to pass 0 (unpaid intern, zero-salary founder, etc.).

---

### Issue 2 — Bug: `processSalaries()` computes details but never prints them
**Severity: Bug**

`getDetails()` returns a `String`. You call it but discard the return value. Test 2 shows the header line but zero employee lines.

```java
// Your code — return value silently discarded
public void processSalaries(){
    for(Employee e : employees){
        e.getDetails();    // ← result thrown away
    }
}

// Fix
public void processSalaries(){
    for(Employee e : employees){
        System.out.println(e.getDetails());
    }
}
```

**Why it matters:** This is the most common silent bug in Java — calling a method for its side effects when it has none, and ignoring its return value. IDEs warn about this. In a code review, this would be caught immediately.

---

### Issue 3 — Design: `updateHoursWorked` accumulates instead of setting
**Severity: Design**

The method name and the spec both imply "set the hours for this month". You use `+=` (accumulate), so each call adds on top. In Test 5, Bob's 80 hours becomes 240 (80 + 160), not 160.

```java
// Your code — accumulates
public void updateHoursWorked(int hours){
    hoursWorkedThisMonth += hours;   // 80 + 160 = 240
}

// Fix — replace, don't add
public void updateHoursWorked(int hours) {
    if (hours < 0) throw new IllegalArgumentException("Hours cannot be negative");
    this.hoursWorkedThisMonth = hours;  // set to 160
}
```

**Why it matters:** In payroll, `updateHoursWorked` is called once per month with the total. Accumulating means the second call doubles the salary. This is a financial calculation error.

---

### Issue 4 — Missing Validation: `updateHoursWorked` rejects 0 hours
**Severity: Missing Validation**

Spec says `validate hours >= 0` — zero hours is valid (leave without pay, first day of month, etc.). You wrote `hours <= 0` which rejects it.

```java
// Your code
if(hours <= 0){ throw ... }   // rejects 0 — wrong

// Fix
if(hours < 0){ throw ... }    // rejects negatives only
```

---

### Issue 5 — Type: `hoursWorkedThisMonth` declared as `double`, spec says `int`
**Severity: Design**

Hours worked is a whole number. Using `double` allows fractional hours which don't make sense for a monthly count. The spec and `updateHoursWorked(int hours)` both signal `int`.

```java
// Your code
private double hoursWorkedThisMonth;   // allows 80.5 hours?

// Fix
private int hoursWorkedThisMonth;
```

---

### Issue 6 — Missing Validation: No null check before `isBlank()`
**Severity: Missing Validation**

`String.isBlank()` throws `NullPointerException` if called on `null`. Callers who pass `null` get a confusing NPE instead of a clear `IllegalArgumentException`.

```java
// Your code
if(employeedId.isBlank()){ ... }   // NPE if null is passed

// Fix
if(employeedId == null || employeedId.isBlank()){ ... }
```

---

### Issue 7 — Minor: `employeedId` typo throughout (extra 'd')
**Severity: Minor**

`employeedId` should be `employeeId`. Consistent within your code, so no runtime impact, but it would fail a code review and looks unprofessional.

---

## Score Card

| Requirement | Result | Note |
|-------------|--------|------|
| `Employee` is abstract | ✅ | |
| `employeeId` is final | ✅ | |
| Employee constructor validates blank ID | ⚠️ | No null check before `isBlank()` |
| `calculateMonthlySalary()` is abstract | ✅ | |
| `getDetails()` calls `calculateMonthlySalary()` internally | ✅ | Template method pattern |
| All subclasses call `super()` first | ✅ | |
| `@Override` on all overrides | ✅ | |
| `FullTimeEmployee` validates `annualSalary >= 0` | ❌ | Rejects 0, crashes Test 6 |
| `PartTimeEmployee.hoursWorkedThisMonth` is `int` | ❌ | Declared as `double` |
| `updateHoursWorked` sets (not accumulates) | ❌ | Uses `+=` instead of `=` |
| `updateHoursWorked` allows 0 | ❌ | Uses `<= 0` instead of `< 0` |
| `processSalaries()` prints each employee | ❌ | Return value discarded |
| `getEmployees()` is unmodifiable | ✅ | |
| `getTotalPayroll()` uses polymorphism, no instanceof | ✅ | |
| `getHighestPaid()` uses polymorphism, no instanceof | ✅ | |
| Tests 1–7 all pass | ❌ | Crash at Test 6 |

---

## Key Takeaways — Do Not Miss These

**TK-1: Always print return values — don't call methods for non-existent side effects**
> `e.getDetails()` does nothing unless you `System.out.println(e.getDetails())`. Java will silently discard return values.
> *Interview note:* This mistake is invisible during a whiteboard session but immediately obvious when you run the code. Always run before declaring done.

**TK-2: Validate to the exact contract — not stricter, not looser**
> `annualSalary >= 0` means 0 IS valid. `< 0` is the correct guard. Writing `<= 0` is a hidden contract violation that breaks callers who followed the spec correctly.
> *Interview note:* Over-validation is just as wrong as under-validation. Say the constraint out loud before writing the `if` condition.

**TK-3: "Update" means set, not accumulate — unless the name says otherwise**
> `updateHoursWorked(160)` means "this month 160 hours were worked", not "add 160 to whatever was there". Accumulation methods are usually named `addHours()` or `logHours()`.
> *Interview note:* A wrong accumulation in a financial system means every payroll run pays more than the last. Naming clarity prevents this.

**TK-4: Null-check before calling instance methods on parameters**
> `param.isBlank()` throws `NullPointerException` if `param` is `null`. Always guard: `param == null || param.isBlank()`. This pattern is so common it's muscle memory.
> *Interview note:* In interviews, if you write a constructor, always add null + blank checks on String parameters. It signals defensive programming discipline.

**TK-5: Match field types to the domain — hours are `int`, amounts are `double`**
> Hours worked in a month is a whole number — `int`. Using `double` introduces unnecessary precision and allows semantically invalid values like `80.5 hours`. Let the type system encode domain constraints.
> *Interview note:* Choosing the right primitive type is a small signal but a real one. It shows you think about what values are actually valid.

**TK-6: `processSalaries()` with no instanceof is the point of this whole exercise**
> The fact that `PayrollProcessor` iterates `List<Employee>` and calls `calculateMonthlySalary()` without a single `instanceof` check is runtime polymorphism working exactly as designed. If you find yourself adding an instanceof chain to a polymorphic loop, you've broken the design.
> *Interview note:* Interviewers deliberately ask "how would you add a fourth employee type?" — the correct answer is "add a new class and override the method; no existing code changes". That's the Open/Closed Principle preview.

---

## Appendix: Why Are All Classes `static` in the Demo File?

> This section answers a question that came up during the review session.

### The Java one-public-class-per-file constraint

Java enforces a hard rule: **one `public` class per `.java` file, and its name must match the filename**. So `PayrollPractice.java` can only have `public class PayrollPractice` at the top level. To keep the entire demo self-contained in one file, all supporting classes (`Employee`, `FullTimeEmployee`, etc.) live *inside* `PayrollPractice` as nested classes.

### Why `static` nested and not just inner?

In Java there are two kinds of nested classes:

```
               Nested Classes
              ┌──────────────────────────────────┐
              │                                  │
        Static Nested Class             Non-Static Inner Class
    (no link to outer instance)     (holds implicit ref to outer instance)
              │                                  │
    new Employee(...)              new PayrollPractice().new Employee(...)
    ← works from static main()     ← CANNOT use from static main() without
                                     first creating a PayrollPractice instance
```

A **non-static inner class** holds an implicit hidden reference to the outer class instance. To create one, you need an outer instance first — `new Outer().new Inner()`. Since `main()` is `static`, it cannot create a non-static inner class without first instantiating `PayrollPractice`, which is pointless and ugly.

A **static nested class** has no reference to the outer instance. It behaves exactly like a standalone top-level class, just namespaced under the outer class. `new Employee(...)` works directly from `main()`.

### In production code — separate files

```
Learning (one file)                   Production (separate files)
──────────────────────────────────    ──────────────────────────────
PayrollPractice.java                  Employee.java
  └─ static class Employee            FullTimeEmployee.java
  └─ static class FullTimeEmployee    PartTimeEmployee.java
  └─ static class PartTimeEmployee    ContractEmployee.java
  └─ static class ContractEmployee    PayrollProcessor.java
  └─ static class PayrollProcessor    PayrollApp.java (just main)
```

### When are static nested classes used in real production code?

Only when the nested class is tightly and *exclusively* coupled to the outer class:

| Example | Why nested |
|---------|-----------|
| `Map.Entry<K,V>` inside `java.util.Map` | An entry only makes sense as part of a map |
| `Builder` inside `Person` | The builder exists solely to construct `Person` |
| `Node` inside `LinkedList` | A node has no meaning outside the list |

**TL;DR:** `static` inner classes in the practice/demo files are a convenience to keep learning material in one compilable file. The `static` keyword is what makes them instantiable from `static main()`. In production, every class gets its own file.
