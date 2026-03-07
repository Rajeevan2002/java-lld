# Java Low Level Design

A structured learning path for LLD concepts and interview problems in Java 21.

---

## Project Structure

```
src/main/java/com/ramkumar/lld/
├── oop/                        # Phase 1 — OOP Fundamentals
├── solid/                      # Phase 2 — SOLID Principles
├── designpatterns/             # Phase 3 — Design Patterns
│   ├── creational/             #   Singleton, Factory, Abstract Factory, Builder, Prototype
│   ├── structural/             #   Adapter, Bridge, Composite, Decorator, Facade, Flyweight, Proxy
│   └── behavioral/             #   Strategy, Observer, Command, Template Method, Iterator,
│                               #   Chain of Responsibility, State, Mediator, Memento, Visitor
├── javareadiness/              # Phase 4 — Java Readiness for LLD
│   ├── cheatsheet/             #   Consolidated revision reference (collections + patterns + idioms)
│   ├── collections/            #   4.1 Collections Deep Dive
│   ├── lldpatterns/            #   4.2 Recurring LLD Patterns in Java
│   ├── javaidioms/             #   4.3 Java 8–21 Idioms
│   ├── enums/                  #   4.4 Enums as Design Tools
│   └── packagestructure/       #   4.5 Package Structure and Naming Conventions
├── concurrency/                # Phase 5 — Concurrency
├── lldthinking/                # Phase 6 — LLD Thinking: Problem to Design
└── problems/                   # Phase 7 — Machine Coding Problems
```

---

## Learning Roadmap

### Phase 1 — OOP Fundamentals
- [x] Classes, Objects, Constructors
- [x] Inheritance, Polymorphism, Encapsulation, Abstraction
- [x] Interfaces vs Abstract Classes
- [x] Composition over Inheritance

### Phase 2 — SOLID Principles
- [x] S — Single Responsibility Principle
- [x] O — Open/Closed Principle
- [x] L — Liskov Substitution Principle
- [x] I — Interface Segregation Principle
- [x] D — Dependency Inversion Principle

### Phase 3 — Design Patterns

#### Creational
- [x] Singleton
- [x] Factory Method
- [x] Abstract Factory
- [x] Builder
- [x] Prototype

#### Structural
- [x] Adapter
- [x] Facade
- [x] Decorator
- [x] Proxy
- [x] Composite
- [x] Bridge
- [x] Flyweight

#### Behavioral
- [x] Strategy
- [x] Observer
- [x] Command
- [x] Template Method
- [x] Iterator
- [x] Chain of Responsibility
- [x] State
- [x] Mediator

---

### Phase 4 — Java Readiness for LLD

> Full topic documentation: [`javareadiness/README.md`](src/main/java/com/ramkumar/lld/javareadiness/README.md)

- [ ] 4.0 Cheat Sheet — consolidated revision reference (collections + patterns + idioms)
- [ ] 4.1 Collections Deep Dive — List, Set, Map, Queue/Deque, Algorithms, time complexity
- [ ] 4.2 Recurring LLD Patterns — ID generation, Repository skeleton, validation, String.format
- [ ] 4.3 Java 8–21 Idioms — Streams, Optional, Comparator, Records, Sealed classes, Switch expressions
- [ ] 4.4 Enums as Design Tools — state machine, EnumMap, EnumSet, Singleton enum
- [ ] 4.5 Package Structure and Naming Conventions

---

### Phase 5 — Concurrency

#### Foundations
- [ ] Thread lifecycle — creation via `Thread`, `Runnable`, `Callable`
- [ ] `synchronized` keyword — method and block level
- [ ] `volatile` and the Java Memory Model (visibility, ordering, happens-before)

#### java.util.concurrent
- [ ] `ExecutorService` and thread pools (`FixedThreadPool`, `CachedThreadPool`, `ScheduledThreadPool`)
- [ ] `Future` and `Callable` — retrieving results from threads
- [ ] `CompletableFuture` — async pipelines, chaining, exception handling
- [ ] `ReentrantLock` and `ReadWriteLock` — explicit locking
- [ ] Atomic variables — `AtomicInteger`, `AtomicReference`, compare-and-swap

#### Concurrent Collections
- [ ] `ConcurrentHashMap`, `CopyOnWriteArrayList`
- [ ] `BlockingQueue` — `ArrayBlockingQueue`, `LinkedBlockingQueue`

#### Coordination Utilities
- [ ] `CountDownLatch` — wait for N events
- [ ] `CyclicBarrier` — synchronize N threads at a checkpoint
- [ ] `Semaphore` — control access to N permits

#### Java 21 — Modern Concurrency
- [ ] Virtual Threads (Project Loom) — lightweight threads, structured concurrency
- [ ] `StructuredTaskScope` — scoped, tree-shaped concurrency

#### Classic Concurrency Problems
- [ ] Producer-Consumer (BlockingQueue-based)
- [ ] Reader-Writer with `ReadWriteLock`
- [ ] Deadlock — detection, avoidance, `tryLock`

---

### Phase 6 — LLD Thinking: Problem to Design

> Language-agnostic. The goal is a clean **class diagram + responsibility table** before a single line of code.

#### The Framework
- [ ] Step 1 — Extract entities: scan for nouns → candidate classes
- [ ] Step 2 — Extract behaviors: verbs on those nouns → methods
- [ ] Step 3 — Map relationships: is-a / has-a / uses-a → inheritance / composition / dependency
- [ ] Step 4 — Define contracts first: interfaces before implementations
- [ ] Step 5 — Assign responsibilities: one reason to change per class (SRP in practice)
- [ ] Step 6 — Iterate: refine the model before writing code

#### Decomposition Practice (design only — no full implementation)
- [ ] Parking Lot — entities, states, relationships
- [ ] Library System — actors, flows, edge cases
- [ ] Ride Sharing App — real-time state, multiple actors
- [ ] Hotel Booking — availability, concurrency concerns
- [ ] Chess Game — board, pieces, rules separation

---

### Phase 7 — Machine Coding Problems
- [ ] Parking Lot System
- [ ] Library Management System
- [ ] Elevator System
- [ ] Food Delivery App (Swiggy/Zomato)
- [ ] Ride Sharing App (Uber/Ola)
- [ ] Hotel Booking System
- [ ] Chess Game
- [ ] ATM System
- [ ] Snake and Ladder Game
- [ ] Movie Ticket Booking (BookMyShow)

---

## Build & Run

```bash
# Compile
mvn compile

# Run tests
mvn test

# Package
mvn package
```

## Java Version Management (jenv)

```bash
jenv global 21          # use Java 21 globally
jenv local 17           # use Java 17 for this project only
jenv versions           # list available versions
```
