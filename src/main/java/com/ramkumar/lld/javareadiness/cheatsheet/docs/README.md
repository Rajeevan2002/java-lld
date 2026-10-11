# Phase 4 — Java Readiness for LLD (Consolidated Cheat Sheet)

> Covers 4.0–4.4: collections, recurring LLD patterns, Java 8–21 idioms, enums as design tools.
> **New to lambdas/streams? Jump to section 4a.**
> Worked example: `../code/TripDispatchDemo.java` · Exercise: `../practice/LoanDeskPractice.java`

---

## 1. Pick the right collection

```
Need key → value lookup?
 ├─ keys are an enum ............................ EnumMap          O(1), ordinal order
 ├─ need sorted keys / floor / ceiling .......... TreeMap          O(log n)
 ├─ need insertion order (reports, LRU) ......... LinkedHashMap    O(1)
 └─ otherwise ................................... HashMap          O(1) avg
Need unique elements?
 ├─ enum values ................................. EnumSet          bitset, O(1)
 ├─ sorted ...................................... TreeSet          O(log n)
 ├─ insertion order ............................. LinkedHashSet
 └─ otherwise ................................... HashSet
Need ordered sequence?
 ├─ random access, mostly appends ............... ArrayList
 ├─ queue / stack ............................... ArrayDeque  (NOT LinkedList / Stack)
 └─ always take the smallest/highest first ...... PriorityQueue   O(log n) offer/poll
```

| Collection | get/contains | add | Order | LLD use |
|---|---|---|---|---|
| `ArrayList` | O(1) / O(n) | O(1) amortized | insertion | query results |
| `HashMap` | O(1) | O(1) | none | repository by id |
| `LinkedHashMap` | O(1) | O(1) | insertion | repository that prints in creation order |
| `TreeMap` | O(log n) | O(log n) | sorted | time-slot / range queries |
| `EnumMap` | O(1) | O(1) | ordinal | counts per status |
| `PriorityQueue` | peek O(1) | O(log n) | priority | dispatch / triage |

---

## 2. Enums as design tools

An enum constant is an object: it can have fields, a constructor and behaviour.

```
        ┌──────────┐  assign   ┌──────────┐  start   ┌─────────────┐  finish  ┌───────────┐
        │REQUESTED │──────────▶│ ASSIGNED │─────────▶│ IN_PROGRESS │─────────▶│ COMPLETED │
        └────┬─────┘           └────┬─────┘          └─────────────┘          └───────────┘
             │ cancel               │ cancel                                   (terminal)
             ▼                      ▼
        ┌──────────────────────────────┐
        │          CANCELLED           │ (terminal)
        └──────────────────────────────┘
```

```java
enum TripStatus {
    REQUESTED, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED;

    boolean canTransitionTo(TripStatus next) {
        return switch (this) {                       // switch expression: exhaustive, no fall-through
            case REQUESTED   -> next == ASSIGNED || next == CANCELLED;
            case ASSIGNED    -> next == IN_PROGRESS || next == CANCELLED;
            case IN_PROGRESS -> next == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}

enum RideType {                                       // data lives on the constant
    PREMIUM(5.00, 2.20), STANDARD(2.50, 1.20);
    private final double baseFare, perKm;
    RideType(double baseFare, double perKm) { this.baseFare = baseFare; this.perKm = perKm; }
}
```

`EnumMap` / `EnumSet`:

```java
Map<TripStatus, Long> counts = new EnumMap<>(TripStatus.class);
for (TripStatus s : TripStatus.values()) counts.put(s, 0L);          // zero-fill so reports are complete
Set<TripStatus> terminal = EnumSet.of(COMPLETED, CANCELLED);
```

---

## 3. Recurring LLD patterns

**ID generation**

| Approach | Use when | Gotcha |
|---|---|---|
| instance `int counter` | one service owns ids; deterministic tests | not thread-safe |
| `static AtomicInteger` | ids unique across all instances / threads | shared across tests |
| `UUID.randomUUID()` | distributed, no ordering needed | unreadable output |

```java
String id = String.format("TRP-%03d", ++counter);      // TRP-001, TRP-002 ...
```

**Constructor validation** — cheap checks first, exact exception types:

```java
if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
Objects.requireNonNull(rider, "rider must not be null");   // NullPointerException with message
```

| Situation | Exception |
|---|---|
| bad argument value | `IllegalArgumentException` |
| object is in the wrong state for this call | `IllegalStateException` |
| lookup failed | `NoSuchElementException` (or `Optional.empty()` from `find*`) |
| mutation of an immutable view | `UnsupportedOperationException` (thrown by the JDK) |

**Repository skeleton**

```java
private final Map<String, Trip> store = new LinkedHashMap<>();
Optional<Trip> findById(String id) { return Optional.ofNullable(store.get(id)); }
List<Trip> findAll()               { return List.copyOf(store.values()); }   // defensive copy
```

**Defensive copies**

| Call | Result | Reflects later changes to source? | Mutable? |
|---|---|---|---|
| `new ArrayList<>(src)` | independent copy | no | yes |
| `List.copyOf(src)` | immutable copy | no | no (throws UOE) |
| `Collections.unmodifiableList(src)` | read-only **view** | **yes** | no (via the view) |

**`String.format` quick guide**: `%d` int · `%03d` zero-pad · `%s` any · `%.2f` two decimals · `%-10s` left-align · `%n` newline · `%b` boolean.

**Logging convention** for demos: `System.out.printf("[TripService] assigned %s%n", id);`

---

## 4. Java 8–21 idioms

```java
// Stream: filter → sort → collect
List<Trip> late = trips.stream()
        .filter(t -> t.getStatus() == IN_PROGRESS)
        .sorted(Comparator.comparingDouble(Trip::fare).reversed().thenComparing(Trip::getId))
        .toList();                                                   // Java 16, immutable

// Optional: never isPresent()+get()
String name = repo.findById(id).map(Trip::getRider).orElseThrow(() -> new NoSuchElementException(id));

// Record: immutable value object, compact constructor validates
record Rider(String id, String name) {
    Rider { if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank"); }
}

// groupingBy into an EnumMap
Map<TripStatus, Long> byStatus = trips.stream()
        .collect(Collectors.groupingBy(Trip::getStatus, () -> new EnumMap<>(TripStatus.class), Collectors.counting()));
```

| Idiom | Replaces |
|---|---|
| `record` | getter/equals/hashCode/toString boilerplate for value objects |
| switch expression | `switch` with `break` + temp variable |
| `Optional` return | returning `null` from `find*` |
| `Comparator.comparing().thenComparing()` | hand-written `compare` with `if` chains |
| `var` | long generic types on the left of `=` (only when the right side is obvious) |

---

## 4a. Lambdas and Streams from zero (start here if these are new to you)

> Every example in this section was compiled and run on Java 21; the outputs shown are the real outputs.
> All examples use this data (a `record` is a tiny immutable class: `b.title()` reads the field):

```java
record Book(String title, String genre, int year, int pages) {}

List<Book> books = List.of(
    new Book("Dune",        "SCIFI",   1965, 412),
    new Book("Emma",        "ROMANCE", 1815, 474),
    new Book("Hamlet",      "DRAMA",   1603, 150),
    new Book("Neuromancer", "SCIFI",   1984, 271),
    new Book("Ulysses",     "NOVEL",   1922, 730),
    new Book("Foundation",  "SCIFI",   1951, 255));
```

Imports used throughout: `java.util.*`, `java.util.function.*`, `java.util.stream.*`.

### Part 1 — What is a lambda, really?

Some methods need *behaviour* as input, not data. "Sort these strings — but **by length**." You must hand over the *how*. Before Java 8 that took a lot of typing:

```java
// 1. Old way: an anonymous class (5 lines for 1 idea)
Comparator<String> byLen = new Comparator<String>() {
    public int compare(String x, String y) { return Integer.compare(x.length(), y.length()); }
};

// 2. Lambda: keep only the parts that matter  →  (inputs) -> result
Comparator<String> byLen = (x, y) -> Integer.compare(x.length(), y.length());
```

Both do the same thing. A lambda is **an unnamed method written inline**:

```
   (x, y)   ->   Integer.compare(x.length(), y.length())
   └input┘  arrow └──────────── what to compute ─────────┘
```

**Where can a lambda go?** Only where Java expects a *functional interface*: an interface with **exactly one abstract method**. The compiler looks at the place you wrote the lambda and figures out which interface it is, so you never type the interface name. You do not have to create these; Java ships the common ones:

| Name | Single method | "In → Out" | Lambda example | Result |
|---|---|---|---|---|
| `Predicate<T>` | `test(t)` | T → **boolean** | `Predicate<Integer> isEven = n -> n % 2 == 0;` | `isEven.test(4)` → `true` |
| `Function<T,R>` | `apply(t)` | T → R | `Function<String,Integer> len = s -> s.length();` | `len.apply("Dune")` → `4` |
| `Consumer<T>` | `accept(t)` | T → nothing | `Consumer<String> shout = s -> System.out.println(s.toUpperCase());` | `shout.accept("dune")` prints `DUNE` |
| `Supplier<T>` | `get()` | nothing → T | `Supplier<String> hello = () -> "hi";` | `hello.get()` → `hi` |
| `BinaryOperator<T>` | `apply(a,b)` | (T,T) → T | `BinaryOperator<Integer> add = (x, y) -> x + y;` | `add.apply(2,3)` → `5` |
| `Comparator<T>` | `compare(a,b)` | (T,T) → int | `(x, y) -> x.length() - y.length()` | negative / 0 / positive |

**Syntax rules (the whole grammar):**

```java
n -> n * 2                       // one parameter: parentheses optional
(a, b) -> a + b                  // two parameters: parentheses required
() -> "hi"                       // no parameters: empty ()
(int a, int b) -> a + b          // types are optional, usually left out
x -> x * 2                       // single expression: its value IS the result (no `return`)
x -> { int y = x * 2; return y; }// block body: braces, and you must write `return`
```

**One capture rule:** a lambda can *read* local variables from around it only if they are never changed afterwards ("effectively final"):

```java
int threshold = 300;
books.stream().filter(b -> b.pages() > threshold).count();   // OK → 3
// threshold = 400;   ← adding this line anywhere makes the lambda above a compile error
```

### Part 2 — Method references (a shorter lambda)

When a lambda **only calls one existing method and passes its input straight to it**, write `Class::method`.

| You would write | Shorter | Meaning |
|---|---|---|
| `b -> b.title()` | `Book::title` | call this method on each element |
| `s -> s.toUpperCase()` | `String::toUpperCase` | same idea |
| `(x, y) -> x + y` (Integers) | `Integer::sum` | static method taking both inputs |
| `x -> System.out.println(x)` | `System.out::println` | method on a specific object |
| `() -> new ArrayList<>()` | `ArrayList::new` | constructor |
| `b -> b.daysLate(today)` | *(cannot)* | it passes an extra variable, so keep the lambda |

### Part 3 — What is a stream?

A **stream** is a *conveyor belt of elements* that you describe a pipeline for. It is **not** a collection: it stores nothing, it just carries elements from a source through steps.

```
 SOURCE            INTERMEDIATE steps (0 or more)             TERMINAL step (exactly 1)
 ┌────────┐        ┌────────┐   ┌────────┐   ┌────────┐       ┌───────────────┐
 │ books  │──────▶ │ filter │──▶│  map   │──▶│ sorted │─────▶ │ toList/count/ │──▶ result
 │ .stream│        └────────┘   └────────┘   └────────┘       │ collect/...   │
 └────────┘        each returns another Stream               └───────────────┘
                                                              returns a real value (or nothing)
```

Four facts that explain almost every stream surprise:
1. **Lazy.** Intermediate steps do *nothing* until a terminal step runs. `books.stream().filter(b -> {System.out.println("hi"); return true;});` prints nothing — there is no terminal step.
2. **Doesn't change the source.** `books` is the same list afterwards; you get new results.
3. **One use only.** Using the same `Stream` object twice throws `IllegalStateException: stream has already been operated upon or closed`. Make a new one with `books.stream()` each time.
4. **Elements flow one at a time** through the whole pipeline (not step-by-step over the whole list).

### Part 4 — Creating a stream (the SOURCE)

```java
books.stream()                               // from any List / Set / Queue  ← 95% of the time
map.values().stream()                        // from a Map's values
map.entrySet().stream()                      // from a Map's entries (e.getKey(), e.getValue())
Stream.of("a", "b", "c")                     // from literal values        → [a, b, c]
Arrays.stream(new int[]{3, 1, 2})            // from an array              → .sum() = 6
IntStream.rangeClosed(1, 5)                  // numbers 1..5               → 1,2,3,4,5
IntStream.range(0, 5)                        // numbers 0..4               → .sum() = 10
"hello".chars()                              // characters as ints         → .filter(c -> c == 'l').count() = 2
```

### Part 5 — INTERMEDIATE operations (they return another Stream)

| Operation | Lambda it takes | What it does | Example → result |
|---|---|---|---|
| `filter(p)` | `Predicate` (→ boolean) | **keep** elements where `p` is true | `.filter(b -> b.genre().equals("SCIFI")).map(Book::title)` → `[Dune, Neuromancer, Foundation]` |
| `map(f)` | `Function` | **transform** each element into something else | `.map(Book::title)` turns Books into Strings |
| `mapToInt(f)` | `ToIntFunction` | map to `int`, gives an `IntStream` with `sum()`, `average()`, `max()` | `.mapToInt(Book::pages).sum()` → `2292` |
| `distinct()` | — | drop duplicates (uses `equals`) | `.map(Book::genre).distinct()` → `[SCIFI, ROMANCE, DRAMA, NOVEL]` |
| `sorted()` / `sorted(cmp)` | `Comparator` | order the elements | `.sorted(Comparator.comparingInt(Book::year)).map(Book::title)` → `[Hamlet, Emma, Ulysses, Foundation, Dune, Neuromancer]` |
| `skip(n)` / `limit(n)` | — | drop the first n / keep only the first n | `.map(Book::title).skip(1).limit(2)` → `[Emma, Hamlet]` |
| `flatMap(f)` | `Function` returning a Stream | flatten "list of lists" into one stream | `List.of(List.of(1,2), List.of(3), List.of()).stream().flatMap(List::stream).toList()` → `[1, 2, 3]` |
| `peek(c)` | `Consumer` | look at elements while debugging (don't use for real work) | `.peek(System.out::println)` |

How to choose between `filter` and `map`: **filter keeps or drops whole elements and never changes them; map changes every element and never drops any.**

### Part 6 — TERMINAL operations (they run the pipeline and give a result)

| Operation | Returns | Example → result |
|---|---|---|
| `toList()` | immutable `List` | `.map(Book::title).toList()` |
| `count()` | `long` | `books.stream().count()` → `6` |
| `forEach(c)` | nothing | `.forEach(b -> System.out.println(b.title()))` |
| `anyMatch(p)` / `allMatch(p)` / `noneMatch(p)` | `boolean` | `anyMatch(b -> b.pages() > 700)` → `true` ; `allMatch(b -> b.year() > 1600)` → `true` ; `noneMatch(b -> b.year() > 2000)` → `true` |
| `findFirst()` | `Optional<T>` | `.filter(b -> b.year() > 1950).findFirst().map(Book::title).orElse("none")` → `Dune` |
| `min(cmp)` / `max(cmp)` | `Optional<T>` | `.max(Comparator.comparingInt(Book::pages)).map(Book::title).get()` → `Ulysses` |
| `reduce(start, op)` | one combined value | `.map(Book::pages).reduce(0, Integer::sum)` → `2292` |
| `sum()` `average()` `max()` (on `IntStream`) | number / `OptionalDouble` / `OptionalInt` | `.mapToInt(Book::pages).average().orElse(0)` → `382.0` ; `.mapToInt(Book::pages).max().getAsInt()` → `730` |
| `collect(collector)` | anything (see Part 7) | `.collect(Collectors.toSet())` |

Why `Optional` for `findFirst` / `min` / `max`? The stream might be empty, so there may be no answer. `Optional` forces you to decide what happens then (`orElse`, `orElseThrow`, `ifPresent`, `map`).

### Part 7 — `collect` and `Collectors` (turning a stream into a List / Set / Map / String)

`collect(...)` is the terminal operation that **gathers** the stream into a result. You tell it *how* by passing a `Collector`, and `Collectors.something(...)` builds the common ones.

```
stream of Books ──▶ collect( Collectors.groupingBy(Book::genre, Collectors.counting()) ) ──▶ Map<String, Long>
                             └──────────────────── "how to gather" ───────────────────┘
```

**Simple gathers**

```java
.collect(Collectors.toList())      // mutable List (older style).  Prefer .toList() (Java 16) if you don't need to modify it.
.collect(Collectors.toSet())       // Set (removes duplicates); books.stream().map(Book::genre).collect(toSet()).size() → 4
.collect(Collectors.joining(", ", "[", "]"))   // one String: separator, prefix, suffix  (stream must contain Strings)
    // books.stream().map(Book::title).collect(Collectors.joining(", ", "[", "]"))
    //   → [Dune, Emma, Hamlet, Neuromancer, Ulysses, Foundation]
```

**`toMap(keyFn, valueFn)` — one entry per element**

```java
Map<String,Integer> pagesByTitle = books.stream().collect(Collectors.toMap(Book::title, Book::pages));
pagesByTitle.get("Dune");   // 412
```
Trap: if two elements produce the **same key** it throws `IllegalStateException: Duplicate key SCIFI (attempted merging values Dune and Neuromancer)`. Add a third argument to say how to combine them (a lambda over the two clashing values), and a fourth for the map type:

```java
Map<String,Integer> pagesByGenre =
    books.stream().collect(Collectors.toMap(Book::genre, Book::pages, Integer::sum, TreeMap::new));
// {DRAMA=150, NOVEL=730, ROMANCE=474, SCIFI=938}
```

**`groupingBy(keyFn)` — bucket elements by a key (the most useful one)**

```java
// A. group → Map<genre, List<Book>>
Map<String, List<Book>> byGenre = books.stream().collect(Collectors.groupingBy(Book::genre));

// B. group and COUNT       (second argument = "what to do with each bucket" = downstream collector)
Map<String, Long> counts = books.stream().collect(Collectors.groupingBy(Book::genre, Collectors.counting()));
//   {DRAMA=1, NOVEL=1, ROMANCE=1, SCIFI=3}

// C. group and keep only titles
books.stream().collect(Collectors.groupingBy(Book::genre, Collectors.mapping(Book::title, Collectors.toList())));
//   {DRAMA=[Hamlet], NOVEL=[Ulysses], ROMANCE=[Emma], SCIFI=[Dune, Neuromancer, Foundation]}

// D. group and add up a number
books.stream().collect(Collectors.groupingBy(Book::genre, Collectors.summingInt(Book::pages)));
//   {DRAMA=150, NOVEL=730, ROMANCE=474, SCIFI=938}

// E. group and average
books.stream().collect(Collectors.groupingBy(Book::genre, Collectors.averagingInt(Book::pages)));
//   {DRAMA=150.0, NOVEL=730.0, ROMANCE=474.0, SCIFI=312.6666666666667}

// F. choose the map type (3-argument form: key, map factory, downstream)  → sorted keys
Map<String, Long> sortedCounts =
    books.stream().collect(Collectors.groupingBy(Book::genre, TreeMap::new, Collectors.counting()));
//   For an enum key use:  () -> new EnumMap<>(LoanStatus.class)   (declaration order)
```
Important: the plain `groupingBy` returns a `HashMap` whose **printing order is not guaranteed** — don't depend on it. Use form F (or sort the keys) when order matters.

**`partitioningBy(predicate)` — exactly two buckets: `true` and `false`**

```java
books.stream().collect(Collectors.partitioningBy(b -> b.pages() > 400,
                                                 Collectors.mapping(Book::title, Collectors.toList())));
// {false=[Hamlet, Neuromancer, Foundation], true=[Dune, Emma, Ulysses]}
```

**Statistics in one go**

```java
books.stream().mapToInt(Book::pages).summaryStatistics();
// IntSummaryStatistics{count=6, sum=2292, min=150, average=382.000000, max=730}
```

**Quick chooser**

| I want… | Use |
|---|---|
| a List | `.toList()` |
| a Set | `Collectors.toSet()` |
| one String | `Collectors.joining(", ")` |
| key → value map | `Collectors.toMap(k, v)` (+ merge function if keys can repeat) |
| key → list of items | `Collectors.groupingBy(k)` |
| key → count / sum / average | `groupingBy(k, counting() / summingInt(f) / averagingInt(f))` |
| key → list of one field | `groupingBy(k, mapping(f, toList()))` |
| split in two | `Collectors.partitioningBy(predicate)` |

### Part 8 — Tracing a pipeline by hand (do this until it feels automatic)

```java
books.stream()
     .filter(b -> b.year() > 1900)      // keep: Dune(1965), Neuromancer(1984), Ulysses(1922), Foundation(1951)
     .map(Book::title)                  // → "Dune", "Neuromancer", "Ulysses", "Foundation"
     .map(String::toUpperCase)          // → "DUNE", "NEUROMANCER", "ULYSSES", "FOUNDATION"
     .sorted()                          // → natural (alphabetical) order
     .toList();                         // → [DUNE, FOUNDATION, NEUROMANCER, ULYSSES]
```

Technique: after each line write down **what one element looks like now** (a `Book`? a `String`? an `int`?). The type changes only at `map`/`mapToInt`/`collect`. If a lambda complains, the type you assumed at that line is wrong.

### Part 9 — Lambdas on collections and maps (no stream needed)

```java
list.removeIf(n -> n % 2 == 0);                    // [1,2,3,4] → [1, 3]
list.replaceAll(n -> n * 10);                      // [1, 3]    → [10, 30]
map.forEach((k, v) -> System.out.println(k + "->" + v));
map.merge("SCIFI", 1, Integer::sum);               // add 1, or insert 1 if absent (counting)
map.computeIfAbsent("SCIFI", k -> new ArrayList<>()).add("Dune");   // get-or-create the list, then add
```

### Part 10 — Sorting recipes

```java
Comparator.comparing(Book::title)                                  // by a String / Comparable field
Comparator.comparingInt(Book::pages)                               // by an int field
Comparator.comparingInt(Book::pages).reversed()                    // biggest first
Comparator.comparing(Book::genre).thenComparing(Book::title)       // genre, then title
Comparator.comparing(Book::genre).thenComparing(Book::year, Comparator.reverseOrder())
//   genre A→Z, newest first inside a genre:
//   [Hamlet, Ulysses, Emma, Neuromancer, Dune, Foundation]
Comparator.comparing(String::length).thenComparing(Comparator.naturalOrder())   // shortest first, then A→Z
// lambda + reversed(): give the parameter a type
Comparator.comparingInt((Book b) -> b.pages() - 100).reversed()
```

### Part 11 — The mistakes beginners hit (and the messages you'll see)

| Mistake | What happens | Fix |
|---|---|---|
| No terminal operation | nothing runs, no error | end with `toList()`, `count()`, `collect(...)`, etc. |
| Reusing a stream variable | `IllegalStateException: stream has already been operated upon or closed` | call `books.stream()` again |
| `toMap` with repeated keys | `IllegalStateException: Duplicate key ...` | add a merge function or use `groupingBy` |
| Adding to the result of `.toList()` | `UnsupportedOperationException` | `new ArrayList<>(stream.toList())` or `Collectors.toList()` |
| `filter(x -> x.getName())` | compile error: String isn't boolean | `filter` needs a boolean lambda; use `map` to transform |
| `map(x -> x > 3)` when you wanted to keep | you get a list of `true/false` | you wanted `filter` |
| Changing a variable used in a lambda | compile error: "must be final or effectively final" | use `count()`/`sum()`/`reduce`, or a plain loop |
| `get()` on an empty `Optional` | `NoSuchElementException` | `orElse(...)`, `orElseThrow(...)`, `ifPresent(...)` |
| Counting with `size()` after filter | can't, a stream has no size | `.count()` (returns `long`) |
| Printing `groupingBy` result and expecting order | random-looking order | use the TreeMap/EnumMap form |

### Part 12 — Practice ladder (answers are below; try before looking)

Using the `books` list above, write each as one pipeline:

1. Print every title.
2. How many books have more than 300 pages?
3. The titles of SCIFI books, sorted A→Z.
4. The title of the book with the fewest pages.
5. The total number of pages of books published before 1960.
6. A map of genre → number of books.
7. A single String of all titles joined with ` | `.
8. Are there any books published after 2000?
9. Titles split into two groups: published before 1900 / 1900 or later.
10. The genre with the most books (hint: `groupingBy` + `counting`, then `max` over `entrySet()`).

<details><summary>Answers</summary>

```java
// 1
books.forEach(b -> System.out.println(b.title()));
// 2   → 3
books.stream().filter(b -> b.pages() > 300).count();
// 3   → [Dune, Foundation, Neuromancer]
books.stream().filter(b -> b.genre().equals("SCIFI")).map(Book::title).sorted().toList();
// 4   → Hamlet
books.stream().min(Comparator.comparingInt(Book::pages)).map(Book::title).orElse("none");
// 5   → 1609   (Emma 474 + Hamlet 150 + Ulysses 730 + Foundation 255)
books.stream().filter(b -> b.year() < 1960).mapToInt(Book::pages).sum();
// 6   → 4 entries: DRAMA=1, NOVEL=1, ROMANCE=1, SCIFI=3 (print order not guaranteed)
books.stream().collect(Collectors.groupingBy(Book::genre, Collectors.counting()));
// 7   → Dune | Emma | Hamlet | Neuromancer | Ulysses | Foundation
books.stream().map(Book::title).collect(Collectors.joining(" | "));
// 8   → false
books.stream().anyMatch(b -> b.year() > 2000);
// 9   → {false=[Dune, Neuromancer, Ulysses, Foundation], true=[Emma, Hamlet]}
books.stream().collect(Collectors.partitioningBy(b -> b.year() < 1900, Collectors.mapping(Book::title, Collectors.toList())));
// 10  → SCIFI
books.stream().collect(Collectors.groupingBy(Book::genre, Collectors.counting()))
     .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("none");
```
</details>

---

## 4b. Lambdas — recap and patterns for LLD code

A lambda is just **an unnamed method you pass as a value**. Java only accepts one where it expects a *functional interface* (an interface with exactly one abstract method). You never write the interface name; the compiler works it out from the place you put the lambda.

```
 (Loan l)   ->   l.getStatus() == ACTIVE
 └─ input ─┘  │  └──────── body (an expression; its value is returned) ────────┘
              └─ "goes to"
```

### Step 1 — turn a loop body into a lambda

```java
// loop version
for (Loan l : loans) {
    if (l.getStatus() == LoanStatus.ACTIVE) { count++; }
}

// same thing: the `if` condition becomes the lambda
long count = loans.stream().filter(l -> l.getStatus() == LoanStatus.ACTIVE).count();
```

Recipe: *take the `if` condition → that is your `filter` lambda; take the expression you compute → that is your `map` lambda.*

### Step 2 — the five shapes you will actually write

| Shape | Interface | Lambda | Used by |
|---|---|---|---|
| test something → boolean | `Predicate<T>` | `l -> l.daysLate(today) > 0` | `filter`, `anyMatch`, `removeIf` |
| transform T → R | `Function<T,R>` | `l -> l.getId()` | `map`, `computeIfAbsent`, `Optional.map` |
| consume, return nothing | `Consumer<T>` | `l -> System.out.println(l)` | `forEach`, `ifPresent` |
| produce from nothing | `Supplier<T>` | `() -> new NoSuchElementException("x")` | `orElseThrow`, `orElseGet` |
| combine two → one | `BinaryOperator<T>` | `(a, b) -> a + b` | `reduce`, `merge` |

If you can say "what goes in, what comes out" you know which shape it is.

### Step 3 — shrink it (4 levels, all identical)

```java
.map((Loan l) -> { return l.getId(); })     // 1. full form: types + braces + return
.map(l -> { return l.getId(); })            // 2. drop the parameter type (inferred)
.map(l -> l.getId())                        // 3. single expression: drop braces and `return`
.map(Loan::getId)                           // 4. method reference: it only forwards its argument
```

**Use a method reference when the lambda only calls one method and passes its argument straight through.**

| Kind | Lambda | Method reference |
|---|---|---|
| instance method of the argument | `l -> l.getId()` | `Loan::getId` |
| static method | `x -> String.valueOf(x)` | `String::valueOf` |
| method on a fixed object | `x -> list.add(x)` | `list::add` |
| constructor | `() -> new ArrayList<>()` | `ArrayList::new` |
| **not** convertible | `l -> l.daysLate(today)` | needs `today` — keep the lambda |

### Step 4 — how to read a stream pipeline

Read it as a sentence, one line per verb. Put each stage on its own line.

```java
List<String> ids = loans.stream()                       // "from all loans"
        .filter(l -> l.getStatus() == OVERDUE)          // "keep the overdue ones"
        .sorted(Comparator.comparing(Loan::getId))      // "ordered by id"
        .map(Loan::getId)                               // "take just the id"
        .toList();                                      // "as a list"
```

Rules of thumb:
1. `filter` lambda returns **boolean**. `map` lambda returns **a value**. `forEach` lambda returns **nothing**.
2. Keep each lambda to one expression. If it needs braces or two statements, extract a named method and use `Class::method`.
3. Do not mutate outside state inside a lambda (`count++`, adding to an outer list). Use `count()`, `collect`, `toList()`.
4. Only effectively-final locals may be captured. `today` is fine; a counter you increment is not.

### Step 5 — the Comparator recipes you need for LLD

```java
Comparator.comparing(Loan::getId)                                    // by a Comparable key
Comparator.comparingInt(Loan::getDueDay)                             // by an int key (no boxing)
Comparator.comparingInt(Loan::getDueDay).thenComparing(Loan::getId)  // tie-break
Comparator.comparingInt(Loan::getDueDay).reversed()                  // descending (method ref: OK)

// Lambda + reversed(): the compiler loses the type. Add the parameter type.
Comparator.comparingInt((Loan l) -> l.daysLate(today)).reversed().thenComparing(Loan::getId)
```

Why: in `comparingInt(l -> ...).reversed()` the call `.reversed()` is evaluated *before* Java knows what `l` is, so it infers `Object`. A method reference or an explicit `(Loan l)` pins the type.

### Step 6 — Map and Optional lambdas (replace if/else on collections)

```java
// count per key — instead of containsKey / put / compute
counts.merge(loan.getStatus(), 1L, Long::sum);

// group-or-create a list — instead of get / null check / put
byMember.computeIfAbsent(loan.getMemberId(), k -> new ArrayList<>()).add(loan);

// lookup or throw — instead of if (!map.containsKey(id)) / get(id)
Loan loan = Optional.ofNullable(store.get(id))
        .orElseThrow(() -> new NoSuchElementException("Loan not found: " + id));
```

The `orElseThrow(() -> ...)` lambda is a **Supplier**: it is only run if the Optional is empty, which is why it is a lambda and not an exception object created up front.

### Before and after (from the Loan Desk exercise)

| Hand-written loop | Lambda / stream |
|---|---|
| `for (...) { if (l.getStatus()==ACTIVE && l.getBook().isbn().equals(i)) throw ...; }` | `store.values().stream().filter(l -> !l.getStatus().isTerminal()).anyMatch(l -> l.getBook().isbn().equals(i))` |
| `Loan best = null; int d = MAX; for (...) { if (... < d) {best = l; d = ...;} }  return Optional.ofNullable(best);` | `.filter(ACTIVE).min(Comparator.comparingInt(Loan::getDueDay).thenComparing(Loan::getId))` |
| `if (map.containsKey(k)) map.compute(k,(a,v)->v+1); else map.put(k,1L);` | `map.merge(k, 1L, Long::sum)` |
| `if (!store.containsKey(id)) throw ...; store.get(id)` | `findById(id).orElseThrow(() -> new NoSuchElementException(...))` |

### When NOT to use a lambda
- The body needs more than one statement or a `try/catch` → write a private method, then pass `this::method`.
- You must modify a local variable → use a loop.
- Early exit with complicated state (e.g. "stop at first failure and remember why") → loop.
- Debugging a long pipeline → temporarily assign each stage to a variable, or add `.peek(System.out::println)`.

### Practice ladder (do these in order, each in 2 minutes)
1. Print every id: `loans.forEach(l -> System.out.println(l.getId()))` → then `System.out::println` after a `map(Loan::getId)`.
2. Count overdue loans: `filter` + `count`.
3. Titles of a member's loans, sorted A→Z: `filter` + `map(l -> l.getBook().title())` + `sorted()`.
4. Loan with the largest fine: `max(Comparator.comparingDouble(...))`.
5. Map member → number of loans: `Collectors.groupingBy(Loan::getMemberId, Collectors.counting())`.

---

## 5. Interview Q&A

**Q1. Why `EnumMap` instead of `HashMap<MyEnum, V>`?**
It is an array indexed by `ordinal()`: no hashing, no collisions, iteration in declaration order, smaller memory.

**Q2. `List.copyOf` vs `Collections.unmodifiableList`?**
`copyOf` snapshots into an immutable list. `unmodifiableList` is a read-only *view*: if the owner keeps mutating the source, the view changes too.

**Q3. Where do you put state-transition rules?**
On the enum (`canTransitionTo`), so the rule lives next to the states and every caller shares one source of truth. The service calls it and throws `IllegalStateException`.

**Q4. `IllegalArgumentException` or `IllegalStateException`?**
IAE: the *argument* is wrong regardless of object state (negative distance). ISE: the argument is fine but the object cannot do it now (completing a cancelled trip).

**Q5. Why return `Optional<T>` from `findById` but throw from `transition`?**
Absence is a normal outcome of a query; the caller decides. In a command (`transition`) a missing target is a caller bug, so fail fast with `NoSuchElementException`.

**Q6. ArrayDeque vs LinkedList vs Stack?**
`ArrayDeque` — faster, less memory, no nulls. `LinkedList` allocates a node per element. `Stack` is a legacy synchronized `Vector`.

**Q7. When do streams hurt?**
Multi-step side effects, early-exit with state, or when the pipeline needs a comment to explain it. A plain loop is fine there.

---

## 6. Common mistakes

- `static` counter for ids in a class where each test expects ids to restart at 001.
- Returning the internal `List` from a getter — callers can mutate your state.
- `HashMap` iteration order assumed in output — use `LinkedHashMap` or sort.
- `isPresent()` followed by `get()` — use `map` / `orElseThrow`.
- `int` division in money/percentage maths (`1 / 2 == 0`) — cast first.
- `==` on `String`s or boxed `Integer`s — use `equals`.
- Using `ordinal()` for persisted values or business meaning — reorder the enum and it silently breaks.
- Modifying a collection while iterating it — `ConcurrentModificationException`.
- Putting the transition table in the service with `instanceof`/`if` chains instead of in the enum.

- Writing `containsKey` then `get` (two lookups, and easy to invert the condition) instead of `Optional.ofNullable(...).orElseThrow(...)`.
- `Comparator.comparing(l -> ...)` without `.reversed()` / `.thenComparing(...)` when the spec asks for descending + tie-break.
- Forgetting that `comparingInt(lambda).reversed()` needs an explicit parameter type.
- A lambda with `{ ... return ...; }` for a single expression — drop the braces.
- Treating only `ACTIVE` as "open": use `!isTerminal()` so `OVERDUE` loans still count.
