# Java Low Level Design — Interview Preparation

A structured learning path for LLD concepts and interview problems in Java 21.

---

## Project Structure

```
src/main/java/com/ramkumar/lld/
├── designpatterns/
│   ├── creational/       # Singleton, Factory, Abstract Factory, Builder, Prototype
│   ├── structural/       # Adapter, Bridge, Composite, Decorator, Facade, Flyweight, Proxy
│   └── behavioral/       # Strategy, Observer, Command, Template Method, Iterator,
│                         # Chain of Responsibility, State, Mediator, Memento, Visitor
├── solid/                # SOLID principle examples
└── problems/             # Real-world LLD machine coding problems
```

---

## Learning Roadmap

### Phase 1 — OOP Fundamentals
- [ ] Classes, Objects, Constructors
- [ ] Inheritance, Polymorphism, Encapsulation, Abstraction
- [ ] Interfaces vs Abstract Classes
- [ ] Composition over Inheritance

### Phase 2 — SOLID Principles
- [ ] S — Single Responsibility Principle
- [ ] O — Open/Closed Principle
- [ ] L — Liskov Substitution Principle
- [ ] I — Interface Segregation Principle
- [ ] D — Dependency Inversion Principle

### Phase 3 — Design Patterns

#### Creational
- [ ] Singleton
- [ ] Factory Method
- [ ] Abstract Factory
- [ ] Builder
- [ ] Prototype

#### Structural
- [ ] Adapter
- [ ] Facade
- [ ] Decorator
- [ ] Proxy
- [ ] Composite
- [ ] Bridge
- [ ] Flyweight

#### Behavioral
- [ ] Strategy
- [ ] Observer
- [ ] Command
- [ ] Template Method
- [ ] Iterator
- [ ] Chain of Responsibility
- [ ] State
- [ ] Mediator

### Phase 4 — Machine Coding Problems
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
