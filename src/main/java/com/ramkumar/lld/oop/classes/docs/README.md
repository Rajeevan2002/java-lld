# Classes, Objects, and Constructors

## 1. What is a Class?

A **class** is a blueprint or template that defines the structure and behavior of objects.
It groups together:
- **Fields** (state / attributes)
- **Methods** (behavior)
- **Constructors** (object initialization)

```java
public class Person {        // class declaration
    private String name;     // field (state)
    private int age;

    public void greet() {    // method (behavior)
        System.out.println("Hi, I am " + name);
    }
}
```

---

## 2. What is an Object?

An **object** is a concrete instance of a class, created at runtime and stored on the **heap**.

```java
Person p = new Person("Alice", 30);
//     ^         ^       ^
//  reference  keyword  constructor args
```

```
Stack                   Heap
┌─────────┐            ┌──────────────────┐
│  p ──────────────►   │  Person object   │
└─────────┘            │  name = "Alice"  │
                       │  age  = 30       │
                       └──────────────────┘
```

> `p` on the stack holds a **reference** (memory address), not the object itself.

---

## 3. Class Anatomy — ASCII UML

```
┌──────────────────────────┐
│         Person           │   ← Class name
├──────────────────────────┤
│ - name : String          │   ← Fields (- = private)
│ - age  : int             │
│ - id   : int             │
├──────────────────────────┤
│ + Person()               │   ← Constructors (+ = public)
│ + Person(name, age)      │
│ + Person(Person other)   │
├──────────────────────────┤
│ + getName() : String     │   ← Methods
│ + getAge()  : int        │
│ + toString(): String     │
└──────────────────────────┘
```

---

## 4. Constructors

A **constructor** is a special method that:
- Has the **same name** as the class
- Has **no return type** (not even `void`)
- Is called automatically when you use `new`

### 4.1 Types of Constructors

| Type | Description |
|------|-------------|
| Default (compiler-generated) | Added by compiler if you write none. No-arg, empty body. |
| No-arg | You explicitly write a no-argument constructor |
| Parameterized | Accepts arguments to initialize fields |
| Copy | Takes an object of the same type and copies its state |
| Private | Prevents external instantiation (used in Singleton, utility classes) |

### 4.2 Constructor Overloading

Multiple constructors with **different parameter lists** in the same class.

```java
public class Person {
    Person() { }                          // no-arg
    Person(String name) { }               // 1 param
    Person(String name, int age) { }      // 2 params
}
```

### 4.3 Constructor Chaining — `this()`

Calling one constructor from another within the same class.
**Must be the first statement.**

```java
public Person(String name) {
    this(name, 0);   // delegates to Person(String, int)
}

public Person(String name, int age) {
    this.name = name;
    this.age  = age;
}
```

```
new Person("Alice")
       │
       ▼
  Person(String)
       │  this(name, 0)
       ▼
  Person(String, int)   ← actual initialization happens here
```

### 4.4 The `this` Keyword

| Use | Meaning |
|-----|---------|
| `this.field` | Refers to the current object's field (disambiguates from param) |
| `this()` | Calls another constructor in the same class |
| `return this` | Returns the current object (used in builder/fluent patterns) |

---

## 5. Static vs Instance Members

```
                ┌──────────────────────────────────┐
                │         Person (class)            │
                │  static int count = 0;  ◄────────── Shared across ALL instances
                └──────────────┬───────────────────┘
                               │
           ┌───────────────────┼───────────────────┐
           ▼                   ▼                   ▼
      Person p1           Person p2           Person p3
   name="Alice"        name="Bob"          name="Carol"
   age=30              age=25              age=28
   (own copy)          (own copy)          (own copy)
```

- **Instance fields**: Each object gets its own copy
- **Static fields**: One copy shared across the entire class

---

## 6. Execution Order

When `new Person("Alice", 30)` is called:

```
1. Static initializer block    (only once, when class loads)
        ↓
2. Parent class constructor    (if extends something)
        ↓
3. Instance initializer block  (each time before constructor body)
        ↓
4. Constructor body            (the one that matched the args)
```

---

## 7. Interview Questions

**Q1: What is the difference between a constructor and a method?**
> A constructor has no return type and must match the class name. It is called automatically with `new`. A method has an explicit return type and must be called explicitly.

**Q2: What happens if you don't define any constructor?**
> The compiler auto-generates a **default no-arg constructor** with an empty body. But the moment you define *any* constructor, the compiler stops generating the default one.

**Q3: Can a constructor have a `return` statement?**
> Yes, but only a bare `return;` (no value). It exits the constructor early.

**Q4: Can a constructor be `private`?**
> Yes. It prevents external code from calling `new`. Used in:
> - **Singleton** pattern (only one instance allowed)
> - **Utility classes** (all-static, like `Math`)
> - **Static factory methods** (you control construction)

**Q5: What is the difference between `this()` and `super()`?**
> `this()` calls another constructor in the **same class**. `super()` calls the constructor of the **parent class**. Both must be the first statement — you cannot use both in one constructor.

**Q6: Is it possible to call a constructor explicitly (without `new`)?**
> No. Constructors can only be invoked via `new` or via `this()` / `super()` chaining inside another constructor.

**Q7: What is the order of execution: static block, instance block, constructor?**
> Static block → (class loaded) → instance block → constructor body. Static runs only once per class load; instance block runs every time an object is created.

---

## 8. Common Mistakes

```java
// MISTAKE 1: Constructor with return type
public void Person() { }   // ← This is a METHOD named Person, not a constructor!

// MISTAKE 2: this() not as first statement
public Person(String name) {
    this.name = name;
    this("default");   // ← compile error: this() must be first
}

// MISTAKE 3: Forgetting that adding a parameterized constructor removes the default
public class Car {
    public Car(String model) { }  // now new Car() won't compile!
}

// MISTAKE 4: Circular constructor chaining
public Person() { this("Alice"); }
public Person(String name) { this(); }  // ← StackOverflowError at runtime
```
