# Inheritance, Polymorphism, Encapsulation, Abstraction

> The four pillars of OOP. Together they let you model the real world cleanly,
> swap implementations without breaking callers, and keep internals hidden.

---

## 1. Encapsulation

**Hide the internals, expose a controlled surface.**

```java
public class BankAccount {
    private double balance;          // hidden — no direct access

    public void deposit(double amt) {
        if (amt <= 0) throw new IllegalArgumentException("...");
        balance += amt;              // logic lives here, not in the caller
    }

    public double getBalance() { return balance; }   // read-only access
}
```

### Access Modifier Ladder

```
Most restrictive ──────────────────────────── Least restrictive
     private  │  package-private  │  protected  │  public
              │   (no keyword)    │             │
  own class   │  own + package    │ + subclasses│  + everyone
```

### Law of Demeter (one-dot rule)
> Talk only to your immediate friends. Don't chain calls into objects you received.

```java
// Bad — reaches into a sub-object's internals
order.getCustomer().getAddress().getCity();

// Better — expose what the caller needs
order.getCustomerCity();
```

---

## 2. Inheritance

**A subclass inherits state and behavior from a superclass, then extends or overrides it.**

```
        Employee
           │
    ┌──────┼──────┐
    │      │      │
FullTime  PartTime  Contract
```

### Key rules

```java
public class Animal {
    protected String name;           // protected: accessible in subclasses

    public Animal(String name) {
        this.name = name;
    }

    public void breathe() { System.out.println(name + " breathes"); }
}

public class Dog extends Animal {
    private String breed;

    public Dog(String name, String breed) {
        super(name);                 // MUST be first — calls parent constructor
        this.breed = breed;
    }
}
```

### `super` keyword

| Use | Meaning |
|-----|---------|
| `super(args)` | Call parent constructor — must be first statement |
| `super.method()` | Call the parent's version of an overridden method |
| `super.field` | Access parent field (rare; prefer protected getter) |

### `final` in inheritance context

| Applied to | Effect |
|------------|--------|
| `final class` | Cannot be extended |
| `final method` | Cannot be overridden by subclasses |
| `final field` | Cannot be reassigned after construction |

### Method Hiding vs Overriding

```java
// Static method → HIDING (resolved at compile time, based on reference type)
class Parent { static void greet() { System.out.println("Parent"); } }
class Child extends Parent { static void greet() { System.out.println("Child"); } }

Parent p = new Child();
p.greet();   // prints "Parent" — compile-time binding!

// Instance method → OVERRIDING (resolved at runtime, based on actual object type)
class Parent { void speak() { System.out.println("Parent"); } }
class Child extends Parent { void speak() { System.out.println("Child"); } }

Parent p = new Child();
p.speak();   // prints "Child" — runtime binding!
```

---

## 3. Polymorphism

**One interface, many implementations. The caller doesn't know which one it's talking to.**

### Runtime Polymorphism (Method Overriding)

```
           List<Employee>
          ┌──────────────┐
          │ FullTime  e1 │──► e1.calculateMonthlySalary() → annualSalary / 12
          │ PartTime  e2 │──► e2.calculateMonthlySalary() → hourlyRate * hours
          │ Contract  e3 │──► e3.calculateMonthlySalary() → amount / duration
          └──────────────┘

     for (Employee e : employees)
         e.calculateMonthlySalary();    ← same call, different behaviour
```

### Rules for valid `@Override`

```java
@Override                              // tells compiler to verify the override
public double calculateMonthlySalary() { ... }
```

| Rule | Detail |
|------|--------|
| Same method name | Must match exactly |
| Same or narrower access | `protected` → can be made `public`, not `private` |
| Same or covariant return type | `Animal` can become `Dog` |
| Cannot throw broader checked exceptions | Only same or narrower |

### Upcasting vs Downcasting

```java
Employee e = new FullTimeEmployee("Alice", 1200000);  // upcasting — implicit, safe
FullTimeEmployee fte = (FullTimeEmployee) e;           // downcasting — explicit, risky

// Safe pattern with instanceof
if (e instanceof FullTimeEmployee fte) {               // Java 16+ pattern matching
    System.out.println(fte.getAnnualSalary());
}
```

---

## 4. Abstraction

**Show what an object does, hide how it does it.**

### `abstract class`

```java
public abstract class Employee {                       // cannot instantiate directly
    private final String id;
    private String name;

    public Employee(String id, String name) {          // can have constructors
        this.id = id;
        this.name = name;
    }

    public abstract double calculateMonthlySalary();   // subclasses MUST implement

    public String getDetails() {                       // concrete — shared logic
        return id + " | " + name + " | ₹" + calculateMonthlySalary();
    }
}
```

### Abstract class vs Interface (preview — deep dive in Topic 3)

| Feature | Abstract Class | Interface |
|---------|---------------|-----------|
| Instantiable | No | No |
| Constructor | Yes | No |
| Fields | Any | `public static final` only |
| Methods | Abstract + concrete | Abstract (default/static allowed since Java 8) |
| Inheritance | Single (`extends`) | Multiple (`implements`) |
| Use when | Shared state + partial implementation | Pure contract / capability |

### Template Method Pattern (natural result of abstract classes)

```java
abstract class DataProcessor {
    // Template — defines the algorithm skeleton
    public final void process() {
        readData();           // step 1
        processData();        // step 2 — abstract, subclass fills in
        writeResults();       // step 3
    }

    protected abstract void processData();   // subclass-specific logic
    private void readData()    { /* common */ }
    private void writeResults() { /* common */ }
}
```

---

## 5. How the Four Pillars Work Together

```
          Abstraction                    Encapsulation
    (abstract Employee class)         (private salary fields)
           │                                  │
           ▼                                  ▼
    ┌─────────────────────────────────────────────────────┐
    │               abstract class Employee               │
    │   - id, name (private)                              │
    │   + calculateMonthlySalary() [abstract]             │
    │   + getDetails() [concrete]                         │
    └──────────────────────┬──────────────────────────────┘
                           │     Inheritance
              ┌────────────┼────────────┐
              ▼            ▼            ▼
         FullTime      PartTime     Contract
         Employee      Employee     Employee
              │            │            │
              └────────────┴────────────┘
                           │     Polymorphism
                           ▼
              payrollProcessor.process(List<Employee>)
              → calls calculateMonthlySalary() on each
              → different result, same call
```

---

## 6. Interview Questions

**Q1: Can an abstract class have a constructor? Why?**
> Yes. Abstract classes can have constructors, called via `super()` from subclasses. They cannot be instantiated directly, but the constructor runs when a concrete subclass is instantiated. Use it to initialize fields shared by all subclasses.

**Q2: What is the difference between method overloading and overriding?**
> Overloading is compile-time polymorphism — same name, different parameter list, resolved by the compiler. Overriding is runtime polymorphism — same signature in a subclass, resolved at runtime based on the actual object type.

**Q3: Can you override a `private` or `static` method?**
> No. `private` methods are not visible to subclasses, so they cannot be overridden — only hidden. `static` methods belong to the class, not the instance; a subclass can declare the same static method but it's method hiding, not overriding (resolved at compile time).

**Q4: What is the Liskov Substitution Principle (preview of Phase 2)?**
> Any code that works with a `Employee` reference should work correctly when given a `FullTimeEmployee`, `PartTimeEmployee`, or `ContractEmployee`. Subclasses must honour the contract of the parent.

**Q5: What is the difference between abstraction and encapsulation?**
> Encapsulation is about *hiding data* (using `private` fields and controlled access). Abstraction is about *hiding complexity* — showing only what an object does, not how. A car's encapsulation is the locked hood; its abstraction is the steering wheel and pedals.

**Q6: When would you choose an abstract class over an interface?**
> Choose abstract class when: subclasses share common state (fields), you want to provide partial implementation, or constructor logic is needed. Choose interface when: you want multiple inheritance of type, or you're defining a pure capability (Flyable, Serializable).

**Q7: What is covariant return type?**
> A subclass can override a method and return a more specific type. E.g., parent returns `Animal`, child can return `Dog`. The child's return type must be a subtype of the parent's return type.

---

## 7. Common Mistakes

```java
// MISTAKE 1: Forgetting super() in subclass constructor
public class Dog extends Animal {
    public Dog(String name) {
        // compiler inserts super() here — but only if Animal has a no-arg constructor!
        // If Animal only has Animal(String name), this is a compile error
    }
}

// MISTAKE 2: Confusing method hiding (static) with overriding (instance)
class Parent { static void greet() { ... } }
class Child extends Parent { static void greet() { ... } }
Parent p = new Child();
p.greet();  // calls Parent.greet(), NOT Child.greet() — this surprises people!

// MISTAKE 3: Widening access is OK, narrowing is not
class Parent { protected void act() { } }
class Child extends Parent {
    private void act() { }  // compile error — can't reduce from protected to private
}

// MISTAKE 4: Breaking encapsulation by returning mutable internal objects
public List<Employee> getEmployees() {
    return employees;   // caller can modify your internal list!
}
// Fix:
public List<Employee> getEmployees() {
    return Collections.unmodifiableList(employees);
}

// MISTAKE 5: instanceof chains instead of polymorphism
if (e instanceof FullTimeEmployee) { ... }
else if (e instanceof PartTimeEmployee) { ... }
// ← This is the smell polymorphism eliminates. If you see this, use @Override.
```
