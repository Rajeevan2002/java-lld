# Java Low Level Design

A structured learning path for LLD concepts and interview problems in Java 21.

> **Goal:** go from "I know OOP concepts" to "I can design and code a clean LLD solution in 35–40 minutes under interview pressure."
> This roadmap merges the original phase-by-phase plan with the 8-week field guide (Java refresher → patterns → approach framework → problem set → mocks).

---

## Project Structure

```
src/main/java/com/ramkumar/lld/
├── oop/                        # Phase 1 — OOP Fundamentals                       (done)
├── solid/                      # Phase 2 — SOLID Principles                       (done)
├── designpatterns/             # Phase 3 — Design Patterns                        (done)
│   ├── creational/             #   Singleton, Factory, Abstract Factory, Builder, Prototype
│   ├── structural/             #   Adapter, Bridge, Composite, Decorator, Facade, Flyweight, Proxy
│   └── behavioral/             #   Strategy, Observer, Command, Template Method, Iterator,
│                               #   Chain of Responsibility, State, Mediator, ...
├── javareadiness/              # Phase 4 — Java Readiness (guide Week 1 gaps)
│   ├── cheatsheet/             #   4.0 Consolidated reference + lambdas/streams from zero   (done)
│   ├── collections/            #   4.1 Collections Deep Dive (+ LRU trick, PriorityQueue)
│   ├── lldpatterns/            #   4.2 Recurring LLD Patterns in Java
│   ├── javaidioms/             #   4.3 Java 8–21 Idioms
│   ├── enums/                  #   4.4 Enums as Design Tools
│   ├── packagestructure/       #   4.5 Package Structure and Naming Conventions
│   ├── generics/               #   4.6 Generics (NEW from guide)
│   └── exceptions/             #   4.7 Exceptions and fail-fast validation (NEW from guide)
├── concurrency/                # Phase 5 — Concurrency (core first, deep-dive later)
├── lldthinking/                # Phase 6 — Approach Framework, Class Blueprint, Skeleton Drill
└── problems/                   # Phase 7 — Machine Coding Problems
    ├── core/                   #   7A — 6 core problems
    └── advanced/               #   7B — 5 advanced problems (+ bonus)
                                # Phase 8 — Mocks and speed (tracked in MISTAKES-LOG.md at the repo root)
```

---

## How to Study (read this first — exercises are now smaller)

The earlier exercises (for example the 18-TODO Loan Desk) were too large for a single sitting. From here every topic uses one of three sizes. Pick the size the phase asks for; **never do more than the size says.**

| Size | Time | What it looks like | Used in |
|---|---|---|---|
| **S — Snippet** | 10–20 min | Write ~20 lines **from memory** in a scratch file. No skeleton, no hints. Then compare with the doc. | Phase 4, Phase 5 |
| **M — Mini** | 30–45 min | One skeleton file, **≤ 6 TODOs, ≤ 5 test blocks**, one concept per file. | Phase 4 (1 per topic, optional), Phase 5, Phase 6 |
| **L — Timed problem** | 40 min | A problem statement only. Run the 8-step framework (Phase 6). Deliverables: class skeleton → core flow coded → 2 extensibility notes. | Phase 7 |

Rules that keep it light:
1. **Read → write from memory → check.** Never type while reading. (This is the guide's "throwaway scripts, not tutorials" rule.)
2. **Stop at the time box.** An unfinished attempt reviewed is worth more than a finished one dragged out.
3. **One mistakes log.** After every exercise add one line to `MISTAKES-LOG.md`: what you got wrong and the rule that fixes it.
4. **Skip, don't pile up.** If an optional item takes more than 2 attempts, move on and revisit in Phase 8.
5. The completed Loan Desk (`javareadiness/cheatsheet/`) counts as the Phase 4 capstone; the remaining Phase 4 topics use S/M sizes only.

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

> Guide Week 2 ("patterns by the sentence that triggers them") is revisited in **Phase 6** with the selection cheat sheet.

---

### Phase 4 — Java Readiness for LLD  *(guide Week 1 — ~6 days, 1–1.5 h/day)*

> Full topic documentation: [`javareadiness/README.md`](src/main/java/com/ramkumar/lld/javareadiness/README.md)
> Lambdas and streams from zero: [`javareadiness/cheatsheet/docs/README.md`](src/main/java/com/ramkumar/lld/javareadiness/cheatsheet/docs/README.md) §4a

| Day | Topic | Size | Do this (and only this) |
|---|---|---|---|
| 1 | 4.0 Cheat Sheet ✅ | — | Done (Loan Desk + lambdas/streams guide). Redo `nextDue` and `countByStatus` with streams as an **S**. |
| 1 | 4.3 Java 8–21 Idioms | **S** | Part 12 practice ladder (10 one-liners) in the cheat-sheet doc. |
| 2 | 4.1 Collections | **S** | Write the `LRUCache` (`LinkedHashMap` access-order + `removeEldestEntry`) from memory; then a `PriorityQueue<Task>` with `comparingInt(...).thenComparing(...)`. |
| 3 | 4.4 Enums as Design Tools | **S** | Write `OrderStatus` with per-constant `next()` and a `canTransitionTo` version; use an `EnumMap` count. |
| 4 | 4.6 Generics | **S** | `Repository<T extends Identifiable>` and `static <T extends Comparable<T>> T max(List<T>)`. |
| 5 | 4.7 Exceptions | **S** | `SpotNotAvailableException` (unchecked) used in an `orElseThrow` chain; fail-fast `assignVehicle`. |
| 5 | 4.2 + 4.5 | **S** | ID generation (`TRP-%03d`), constructor validation template, package layout sketch (domain / service / repository). |
| 6 | Self-check | — | Answer the 7-question quiz in `javareadiness/README.md` cold; log misses. |
| 7 | Optional capstone | **M** | One 6-TODO mini on a fresh scenario (ask for `/lld-exercise` in **lite mode**: ≤ 6 TODOs). |

- [x] 4.0 Cheat Sheet — consolidated revision reference (collections + patterns + idioms), Loan Desk capstone, lambdas/streams guide
- [x] 4.1 Collections Deep Dive — List, Set, Map, Queue/Deque, Algorithms, time complexity
- [ ] 4.2 Recurring LLD Patterns — ID generation, Repository skeleton, validation, String.format
- [ ] 4.3 Java 8–21 Idioms — Streams, Optional, Comparator, Records, Sealed classes, Switch expressions
- [ ] 4.4 Enums as Design Tools — state machine, EnumMap, EnumSet, Singleton enum
- [ ] 4.5 Package Structure and Naming Conventions
- [ ] 4.6 Generics — generic class, bounded types, `<T extends Comparable<T>>`, wildcards (PECS) *(new)*
- [ ] 4.7 Exceptions — checked vs unchecked, custom exceptions, fail-fast validation, never swallow *(new)*

**Ready for Phase 5 when:** you can write the LRU cache, a generic repository, and a behaviour enum from memory, and answer the quiz without hesitation.

---

### Phase 5 — Concurrency  *(guide Week 1 §6 first, deep dive later)*

> Reordered: the **Core** block is what Parking Lot, Booking and Rate Limiter need. The **Deep dive** block is kept but moved after Phase 7A so it does not delay problem-solving.

#### 5A — Core (do now, ~3 days, sizes S/M)
- [ ] `synchronized` keyword — method and block level (**S**: thread-safe `SeatBooking.bookSeat`)
- [ ] Race conditions — spot / seat / counter; say the race out loud (the guide's step 4 of the concurrency checklist)
- [ ] `ReentrantLock` — `lock()` / `try` / `finally unlock()`; when it beats `synchronized` (**S**)
- [ ] Thread-safe Singleton — eager init (default), double-checked locking with `volatile` (**S**: both from memory)
- [ ] `ConcurrentHashMap` — atomic `computeIfAbsent`, per-key state (**S**: tiny `RateLimiter` skeleton)
- [ ] Atomic variables — `AtomicInteger` for counters (**S**)
- [ ] Thread lifecycle — `Thread`, `Runnable`, `Callable` (**S**: start 2 threads that book the same seat)
- [ ] `volatile` — visibility basics, why it is required in double-checked locking

#### 5B — Deep dive (after Phase 7A)
- [ ] `ExecutorService` and thread pools (`FixedThreadPool`, `CachedThreadPool`, `ScheduledThreadPool`)
- [ ] `Future` and `Callable` — retrieving results from threads
- [ ] `CompletableFuture` — async pipelines, chaining, exception handling
- [ ] `ReadWriteLock` and `CopyOnWriteArrayList`
- [ ] `BlockingQueue` — `ArrayBlockingQueue`, `LinkedBlockingQueue` + Producer-Consumer
- [ ] Coordination — `CountDownLatch`, `CyclicBarrier`, `Semaphore`
- [ ] Deadlock — detection, avoidance, `tryLock`
- [ ] Java 21 — Virtual Threads, `StructuredTaskScope`

**Ready for Phase 6 when:** you can name the race in a seat-booking class, fix it two ways, and write the eager singleton without notes.

---

### Phase 6 — LLD Thinking: Approach Framework  *(guide Week 2 + "Approach Framework", ~5 days)*

> Language-agnostic. The goal is a clean **class diagram + responsibility table** before a single line of code.
> Patterns are tools: learn each by the **sentence in the requirements that triggers it**, not by the UML.

#### 6.1 The 8-step framework — run the same sequence every time
- [ ] 1. **Clarify scope (2–3 min)** — functional + non-functional (concurrency? persistence? scale?), said as a numbered list
- [ ] 2. **Identify core entities** — nouns → candidate classes
- [ ] 3. **Define relationships** — is-a / has-a / uses-a
- [ ] 4. **List actions → methods** — verbs become interface methods (`park()`, `unpark()`, `findSpot()`)
- [ ] 5. **Apply a pattern only where it earns its keep** — only where a requirement literally implies varying behaviour
- [ ] 6. **Write class skeletons first** — names + signatures, no bodies
- [ ] 7. **Implement the core flow fully** — the 1–2 methods that matter most; stub the rest
- [ ] 8. **Call out extensibility and edge cases** — "if we added EV spots, I'd …"

#### 6.2 Universal Class Blueprint
- [ ] The ten building blocks (entity, value object, enum, repository, service, strategy, factory, listener, exception, facade/main)
- [ ] Dependency direction — facade → service → repository/strategy → entity (never upward)
- [ ] "Where does this logic go?" table (entity vs service vs repository)
- [ ] Aggregate-root rule — one repository per aggregate, not per entity
- [ ] Naming conventions interviewers parse instantly (links to 4.5)

#### 6.3 Pattern selection and judgment
- [ ] Requirement phrase → pattern cheat sheet (Strategy, Factory, Observer, Singleton, Builder, Decorator, State)
- [ ] Strategy vs State, Strategy vs Decorator — who changes it, replace vs wrap
- [ ] Anti-patterns and over-engineering traps (including `instanceof` chains in client code)
- [ ] Interview scripts — what to say while designing

#### 6.4 The 20-Minute Skeleton Drill (skeleton only, signatures only)
Fill the template in `lldthinking/` (entities → relationships → methods → layers → patterns → checks), timed.
- [ ] Library Management
- [ ] Food Delivery
- [ ] Hotel Booking
- [ ] Chat Application
- [ ] Parking Lot, Ride Sharing, Chess (design only — no implementation)

**Ready for Phase 7 when:** you produce a layered skeleton for an unseen problem in **≤ 20 min**, and can name the trigger phrase for each core pattern without notes.

---

### Phase 7 — Machine Coding Problems  *(Size L — 40 min each, follow the 8-step framework)*

For every problem: timebox to 40 min → skeleton first → core flow → extensibility notes → add a line to the mistakes log → review against a reference.

#### 7A — Core set (guide Weeks 3–4, do in this order)
- [ ] 1. **Parking Lot** — entities, has-a, enums with behaviour; thread-safe spot assignment
- [ ] 2. **Vending Machine** — State pattern
- [ ] 3. **Library Management System** — repository-style entity design (reuses the Loan Desk work)
- [ ] 4. **Tic-Tac-Toe / simplified Chess** — board representation, move validation, `Piece` polymorphism
- [ ] 5. **LRU Cache** — `LinkedHashMap` trick, then the doubly-linked-list version
- [ ] 6. **Rate Limiter** — Strategy (token bucket vs sliding window) + `ConcurrentHashMap`

> Now do **Phase 5B (deep dive)** before 7B if you want, or interleave one topic per problem.

#### 7B — Advanced set (guide Weeks 5–6)
- [ ] 7. **Splitwise / Expense Sharing** — settlement logic, Strategy for split types
- [ ] 8. **Elevator System** — State + concurrency, scheduling algorithm
- [ ] 9. **Movie Ticket Booking (BookMyShow)** — seat locking, Factory, Observer
- [ ] 10. **Notification System** — Observer + Strategy (email / SMS / push)
- [ ] 11. **Logging Framework** — Singleton, Builder, Decorator

#### 7C — Bonus (original list, only if time allows)
- [ ] Food Delivery App (Swiggy/Zomato)
- [ ] Ride Sharing App (Uber/Ola)
- [ ] Hotel Booking System
- [ ] ATM System
- [ ] Snake and Ladder Game

---

### Phase 8 — Mocks and Speed  *(guide Weeks 7–8)*
- [ ] 3–4 timed mocks per week, strict 40-min cap (self-timed or with a peer)
- [ ] Re-solve 2–3 earlier problems from scratch, unaided, and compare time
- [ ] Review `MISTAKES-LOG.md` before every mock (typical entries: forgot thread-safety, over-applied a pattern, skipped edge cases)
- [ ] Write your own one-page "LLD approach cheat sheet" and refine it weekly

---

## Suggested Cadence (1–1.5 h/day)

| Block | Focus | Output |
|---|---|---|
| Week A | Phase 4 (7 days, S-sized) | 6 snippets + quiz, mistakes log started |
| Week B | Phase 5A (3 days) + Phase 6 (4 days start) | thread-safe snippets, 8-step framework memorised |
| Week C | Phase 6 finish + 7A #1–#2 | 4 skeleton drills, Parking Lot, Vending Machine |
| Week D | 7A #3–#6 | Library, Tic-Tac-Toe, LRU, Rate Limiter |
| Weeks E–F | 7B #7–#11 (+ 5B as needed) | 5 advanced problems |
| Weeks G–H | Phase 8 | 3–4 mocks/week, re-solves, cheat sheet |

Adjust freely; the order matters more than the dates.

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
