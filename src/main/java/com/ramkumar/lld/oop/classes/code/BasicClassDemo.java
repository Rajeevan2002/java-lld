package com.ramkumar.lld.oop.classes.code;

/**
 * Demonstrates the anatomy of a well-structured Java class:
 *  - Private fields (encapsulation)
 *  - Default, parameterized, and copy constructors
 *  - Getters and setters
 *  - toString / equals override
 *  - Static counter field (shared across instances)
 */
public class BasicClassDemo {

    // ─── Fields ───────────────────────────────────────────────────────────────

    private static int instanceCount = 0;   // shared across ALL Person objects

    private final int id;       // immutable after construction
    private String name;
    private int age;

    // ─── Constructors ─────────────────────────────────────────────────────────

    /**
     * No-arg constructor — explicitly defined so callers can create an "empty" Person.
     * (Without this, once we add parameterized constructors, the compiler default is gone.)
     */
    public BasicClassDemo() {
        this("Unknown", 0);     // delegate to the 2-arg constructor
    }

    /**
     * Parameterized constructor — primary initialization path.
     */
    public BasicClassDemo(String name, int age) {
        this.id   = ++instanceCount;    // auto-assign a unique ID
        this.name = name;
        this.age  = age;
    }

    /**
     * Copy constructor — creates a new object with the same state as `other`.
     * Note: the ID is NOT copied; the copy gets its own unique ID.
     */
    public BasicClassDemo(BasicClassDemo other) {
        this(other.name, other.age);
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public int getId()      { return id; }
    public String getName() { return name; }
    public int getAge()     { return age; }

    public void setName(String name) { this.name = name; }
    public void setAge(int age) {
        if (age < 0) throw new IllegalArgumentException("Age cannot be negative");
        this.age = age;
    }

    public static int getInstanceCount() { return instanceCount; }

    // ─── Overrides ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Person{id=" + id + ", name='" + name + "', age=" + age + "}";
    }

    // ─── Main — demonstration ─────────────────────────────────────────────────

    public static void main(String[] args) {
        // 1. Create using no-arg constructor
        BasicClassDemo p1 = new BasicClassDemo();
        System.out.println("No-arg:       " + p1);

        // 2. Create using parameterized constructor
        BasicClassDemo p2 = new BasicClassDemo("Alice", 30);
        System.out.println("Parameterized:" + p2);

        // 3. Create a copy
        BasicClassDemo p3 = new BasicClassDemo(p2);
        System.out.println("Copy of p2:   " + p3);

        // p3 is a DIFFERENT object even though values look the same
        System.out.println("p2 == p3 ?    " + (p2 == p3));          // false (different refs)
        System.out.println("p2.id == p3.id?" + (p2.getId() == p3.getId())); // false (own ID)

        // 4. Static field is shared
        System.out.println("Total persons created: " + BasicClassDemo.getInstanceCount()); // 3

        // 5. Stack vs Heap — reference assignment (shallow copy, NOT a copy constructor)
        BasicClassDemo ref = p2;    // ref and p2 point to the SAME object
        ref.setName("Bob");
        System.out.println("p2 after ref.setName: " + p2);  // name changed to Bob too!
    }
}
