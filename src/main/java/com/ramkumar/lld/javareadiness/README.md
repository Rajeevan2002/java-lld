# Phase 4 — Java Readiness for LLD

> Bridges the gap between knowing Java syntax and writing clean, interview-grade LLD code.
> Every topic has: `docs/` (theory + ASCII diagrams), `code/` (worked example), `practice/` (exercise skeleton), `results/` (your solution).
> A consolidated `cheatsheet/` covers everything in one revision file.

---

## 4.0 Cheat Sheet (Consolidated Revision Reference)

- [ ] Collections decision table — which collection for which LLD scenario
- [ ] Time complexity quick-reference — `get`, `put`, `contains`, `iterate` for all major collections
- [ ] Exception type decision tree — IAE vs ISE vs UOE vs NPE, when to use each
- [ ] `String.format` precision guide — `%d`, `%03d`, `%s`, `%.2f`, `%-10s`
- [ ] LLD boilerplate templates — ID generation, repository skeleton, constructor validation
- [ ] Output/logging patterns for LLD demos and machine coding
- [ ] Java naming conventions — `*Service`, `*Repository`, `*Factory`, `*Handler`, interface naming

---

## 4.1 Collections Deep Dive

> Goal: know every collection's internal mechanism, time complexity, and the right LLD use case.

### List

- [ ] `ArrayList` — dynamic array, O(1) random access, O(n) insert/delete mid-list; when to use
- [ ] `LinkedList` — doubly-linked list, O(1) insert at ends, O(n) random access; `Deque` usage
- [ ] `Vector` and `Stack` — legacy synchronized wrappers; why to avoid; `ArrayDeque` as replacement
- [ ] `List.of()` / `List.copyOf()` — immutable lists; difference from `Collections.unmodifiableList()`

### Set

- [ ] `HashSet` — O(1) add/contains/remove, no order; backed by `HashMap`; null handling
- [ ] `LinkedHashSet` — insertion-order guaranteed; O(1) operations; LRU / ordered-unique use cases
- [ ] `TreeSet` — sorted (natural or `Comparator`), O(log n) operations; `NavigableSet` API — `floor`, `ceiling`, `headSet`, `tailSet`
- [ ] `EnumSet` — ultra-fast bitset-backed set for enum values; when to prefer over `HashSet<MyEnum>`

### Map

- [ ] `HashMap` — hash table, O(1) average get/put, unordered; load factor, rehashing, collision handling
- [ ] `LinkedHashMap` — insertion-order or access-order (LRU cache); `removeEldestEntry` hook
- [ ] `TreeMap` — Red-Black tree, O(log n), sorted keys; `NavigableMap` API — `floorKey`, `ceilingKey`, `subMap`, `headMap`, `tailMap`
- [ ] `EnumMap` — array-backed, O(1), for enum keys; always prefer over `HashMap<MyEnum, V>`
- [ ] `Map` utility methods — `getOrDefault`, `putIfAbsent`, `computeIfAbsent`, `computeIfPresent`, `compute`, `merge`, `replaceAll`, `forEach`
- [ ] `Map.of()` / `Map.entry()` / `Map.copyOf()` — immutable maps
- [ ] `Hashtable` — legacy synchronized map; why to avoid; `ConcurrentHashMap` as replacement

### Queue and Deque

- [ ] `ArrayDeque` — resizable array deque; use as Stack (`push`/`pop`) and Queue (`offer`/`poll`); O(1) for all end operations; prefer over `LinkedList` for queue/stack
- [ ] `PriorityQueue` — min-heap by default; `Comparator` for custom order; O(log n) offer/poll, O(1) peek; LLD uses: task scheduler, ride dispatch, order priority
- [ ] `LinkedList` as `Queue` / `Deque` — functional but slower than `ArrayDeque` in practice
- [ ] `Deque` interface — `addFirst`/`addLast`, `peekFirst`/`peekLast`, `pollFirst`/`pollLast`

### Algorithms and Utilities

- [ ] `Collections` utility class — `sort`, `reverse`, `shuffle`, `min`, `max`, `frequency`, `disjoint`, `nCopies`, `singletonList`, `emptyList`, `unmodifiableList`, `synchronizedList`
- [ ] `Arrays` utility class — `sort`, `binarySearch`, `fill`, `copyOf`, `copyOfRange`, `asList`, `stream`
- [ ] `Comparator` chaining — `comparing`, `thenComparing`, `reversed`, `nullsFirst`, `nullsLast`
- [ ] `Comparable` vs `Comparator` — natural ordering vs external ordering; when each is appropriate

### Time Complexity Reference

| Collection | get | add/put | contains/remove | iterate | Order |
|---|---|---|---|---|---|
| `ArrayList` | O(1) | O(1) amortized | O(n) | O(n) | insertion |
| `LinkedList` | O(n) | O(1) at ends | O(n) | O(n) | insertion |
| `HashSet` | — | O(1) avg | O(1) avg | O(n) | none |
| `LinkedHashSet` | — | O(1) avg | O(1) avg | O(n) | insertion |
| `TreeSet` | — | O(log n) | O(log n) | O(n) | sorted |
| `HashMap` | O(1) avg | O(1) avg | O(1) avg | O(n) | none |
| `LinkedHashMap` | O(1) avg | O(1) avg | O(1) avg | O(n) | insertion/access |
| `TreeMap` | O(log n) | O(log n) | O(log n) | O(n) | sorted |
| `ArrayDeque` | O(1) ends | O(1) ends | O(n) | O(n) | insertion |
| `PriorityQueue` | O(1) peek | O(log n) | O(n) | O(n) | priority |
| `EnumMap` | O(1) | O(1) | O(1) | O(n) | enum ordinal |
| `EnumSet` | — | O(1) | O(1) | O(n) | enum ordinal |

---

## 4.2 Recurring LLD Patterns in Java

> These appear in every LLD exercise. Codify them once, apply everywhere.

- [ ] **ID generation** — `static AtomicInteger` vs instance `int counter` vs `UUID.randomUUID()`; when each is appropriate; `String.format("TRIP-%03d", id)` zero-padding
- [ ] **Repository skeleton** — `HashMap`-backed `InMemoryRepository<T>`; `Optional<T>` for `findById`; `Collections.unmodifiableList()` for query returns; update-in-place vs copy-on-write
- [ ] **Constructor validation template** — null/blank guards; `IllegalArgumentException` vs `NullPointerException`; `Objects.requireNonNull`; validation order (cheap checks first)
- [ ] **Exception type decision tree** — `IllegalArgumentException` (bad input), `IllegalStateException` (wrong object state), `UnsupportedOperationException` (not implemented), `NoSuchElementException` (not found), when to use checked vs unchecked
- [ ] **`String.format` precision guide** — `%d` vs `%03d`, `%s`, `%n`, `%.2f` vs `%.1f`, `%-10s` (left-aligned), `String.format` vs `String.formatted` vs `StringBuilder`
- [ ] **Defensive copy patterns** — `new ArrayList<>(original)` vs `List.copyOf(original)` vs `Collections.unmodifiableList(original)`; which mutates, which throws, which is safe
- [ ] **Output and logging patterns** — `[ServiceName] action: detail` format; `System.out.printf` vs `String.format`; structured demo output for machine coding rounds

---

## 4.3 Java 8–21 Idioms

> The APIs that replace verbose for-loops in real LLD code.

### Stream API

- [ ] `filter`, `map`, `flatMap`, `collect` — the four pillars
- [ ] Collectors — `toList()`, `toSet()`, `toMap()`, `groupingBy()`, `counting()`, `joining()`
- [ ] Terminal operations — `findFirst()`, `findAny()`, `anyMatch()`, `allMatch()`, `noneMatch()`, `count()`, `min()`, `max()`, `reduce()`
- [ ] `Stream` vs `for` loop in LLD — when streams hurt readability; when they help

### Optional

- [ ] `of` vs `ofNullable` vs `empty` — construction
- [ ] `get` vs `orElse` vs `orElseGet` vs `orElseThrow` — retrieval
- [ ] `map`, `flatMap`, `filter` — chaining
- [ ] `ifPresent`, `ifPresentOrElse` — side effects
- [ ] Anti-patterns — `isPresent()` + `get()` is just null-check in disguise

### Comparator and Comparable

- [ ] `Comparator.comparing(KeyExtractor)` — type-safe sorting
- [ ] `thenComparing` — multi-field sort (sort by fare, then by tripId)
- [ ] `reversed()`, `naturalOrder()`, `reverseOrder()`, `nullsFirst()`, `nullsLast()`
- [ ] `Comparable<T>` — natural ordering; when to implement vs when to use `Comparator`

### Modern Java (16–21)

- [ ] **Records** (Java 16) — `record Point(int x, int y) {}` — immutable value objects; compact constructor; when to use over a class
- [ ] **Sealed classes** (Java 17) — `sealed interface Shape permits Circle, Rectangle` — finite type hierarchies; exhaustive switch
- [ ] **Pattern matching** `instanceof` (Java 16) — `if (obj instanceof String s)` — eliminates cast
- [ ] **Switch expressions** (Java 14) — `switch` that returns a value; arrow syntax; exhaustiveness
- [ ] **Text blocks** (Java 15) — multi-line strings with `"""`; useful for test data and JSON in demos
- [ ] **`var`** (Java 10) — local type inference; when it helps and when it harms readability

---

## 4.4 Enums as Design Tools

> Enums are not just constants — they are first-class objects with fields, constructors, and methods.

- [ ] Enum basics — fields, constructors, getters; `name()`, `ordinal()`, `values()`, `valueOf()`
- [ ] Enum with abstract methods — each constant overrides behaviour; state machine in an enum
- [ ] Enum as state machine — `TripStatus` with `canTransitionTo(TripStatus next)` validation
- [ ] `EnumMap` — `Map<TripStatus, List<Trip>>` — O(1) operations, ordered by `ordinal()`
- [ ] `EnumSet` — `EnumSet.of(...)`, `EnumSet.allOf(...)`, `EnumSet.noneOf(...)` — bitset performance
- [ ] Enum in `switch` — traditional `switch`, `switch` expressions, exhaustiveness checking
- [ ] Singleton enum — thread-safe singleton via single-element enum (Effective Java Item 3)

---

## 4.5 Package Structure and Naming Conventions

> The decisions that separate "it works" from "it's production-ready."

- [ ] **Layer architecture** — `domain/` (entities, enums, value objects), `service/` (orchestration), `repository/` (data access interfaces + implementations), `notification/`, `calculator/` — when to split, when to stay flat
- [ ] **Java naming conventions** — `*Service`, `*Repository`/`*Store`/`*Dao`, `*Factory`, `*Builder`, `*Handler`, `*Processor`, `*Strategy`, `*Calculator`; no `I`-prefix for interfaces; `Abstract*` for abstract classes
- [ ] **Interface location** — interface belongs in the high-level (domain/service) package; implementations belong in infrastructure packages (DIP in practice)
- [ ] **Visibility modifiers** — `public` vs package-private vs `protected` vs `private`; minimise surface area; prefer package-private for helpers
- [ ] **Static nested classes vs top-level classes** — when to nest (cohesion), when to extract (reuse)
- [ ] **Comment conventions** — Javadoc for public API (`/** */`), inline for non-obvious logic (`//`), no redundant comments; when comments signal a design smell
