# Review — 4.1 Collections Deep Dive (Support Desk: LRU cache + ticket queue)

## What You Got Right
- **`super(16, 0.75f, true)`** — you used the access-order flag, the one switch that makes a `LinkedHashMap` an LRU rather than a FIFO cache. Tests 2 and 3 only pass because of it.
- **`size() > capacity`** (strict) in `removeEldestEntry`, with the counter incremented only on the eviction path.
- **Constructor validation order** matches the spec (id → title → priority → createdAt), uses `isBlank()` after the null check, and assigns the fields only after every check passes. Fields are all `private final`.
- **Comparator chain**: you put `.reversed()` straight after the priority key, so only priority is flipped, and you end with `id` as a unique tie-break.
- **`pendingIds()`** sorts a stream of the queue contents instead of iterating the `PriorityQueue`, so the queue is left untouched. You also avoided the heap-order trap and used `.toList()` for an unmodifiable result.
- **`Optional.ofNullable(field.poll())`** turns the empty-queue `null` into an `Optional` and never throws.
- All 9 test blocks run and print the expected output, apart from the `toString` line in Test 5 (Issue 1).

## Issues Found

### 1. Bug — `toString()` is missing the literal `P`
- **Your code:** `return String.format("%s[%d, t=%d, %s]", id, priority, createdAt, title);`
- **Output:** `T-1[3, t=10, Login fails]`. The spec and Test 5 expect `T-1[P3, t=10, Login fails]`.
- **Fix:** `String.format("%s[P%d, t=%d, %s]", id, priority, createdAt, title)`
- **Why it matters:** exact output formats are graded literally in machine-coding rounds. Run the tests and compare each `// expected:` line, don't just check that nothing crashed.

### 2. Design — `Comparator.comparing(Ticket::getPriority)` boxes every `int`
- **Your code:** `Comparator.comparing(Ticket::getPriority).reversed().thenComparing(Ticket::getCreatedAt).thenComparing(Ticket::getId)`
- **Fix:** `Comparator.comparingInt(Ticket::getPriority).reversed().thenComparingInt(Ticket::getCreatedAt).thenComparing(Ticket::getId)`
- **Why it matters:** the primitive variants avoid allocating an `Integer` for every comparison, and they show you know the API. The behaviour is the same.

### 3. Minor — unused import couples practice to the worked example
- **Your code:** `import com.ramkumar.lld.javareadiness.collections.code.PageCacheSpoolerDemo;`
- **Fix:** delete it. IDEs sometimes auto-add it when you reference the wrong class; also check you didn't copy from `code/`.
- **Why it matters:** a stray import ties your practice file to another file, and the file breaks if that one is renamed or removed.

### 4. Minor — redundant qualifier and weak names
- **Your code:** `.map(SupportDeskPractice.Ticket::getId)`, field named `field`, constant named `comparator`
- **Fix:** `.map(Ticket::getId)`; `queue`; `ORDER` (constants are `UPPER_SNAKE`).
- **Why it matters:** names such as `field` say nothing about what the member is, and interviewers read them first.

### 5. Minor — missing `serialVersionUID` (`-Xlint:serial` warning)
- **Fix:** `private static final long serialVersionUID = 1L;`
- **Why it matters:** it's harmless here, but any class extending a serializable JDK type should declare it.

## Score Card
| Requirement | Result |
|---|---|
| `ArticleCache` extends `LinkedHashMap`, access order, 16 / 0.75 | ✅ |
| `capacity` final, `evictionCount` per-instance | ✅ |
| `capacity < 1` → exact IAE message | ✅ |
| Evict only when `size() > capacity`, count each eviction | ✅ |
| `getEvictionCount()` | ✅ |
| `Ticket` fields final, validation order and messages | ✅ |
| `Ticket` getters | ✅ |
| `Ticket.toString()` format | ❌ (missing `P`) |
| Comparator: priority desc, createdAt asc, id asc | ✅ (⚠️ boxing) |
| Backed by `PriorityQueue`, comparator defined once | ✅ |
| `submit` null → IAE message | ✅ |
| `next()` returns `Optional`, empty-safe | ✅ |
| `size()` | ✅ |
| `pendingIds()` non-destructive, sorted, unmodifiable | ✅ |
| No iteration of the PQ directly, no manual sort loops | ✅ |
| Naming / cleanliness | ⚠️ |

## Key Takeaways — Do Not Miss These
1. **TK-1: Diff your output against every `expected:` line.** Run, read each line, and compare it character by character. *Interview value: a one-character format slip costs a test case you otherwise passed.*
2. **TK-2: `LinkedHashMap(16, 0.75f, true)` + `removeEldestEntry(size() > capacity)` is the whole LRU.** *Interview value: this is the most common "design an LRU cache" opener and takes about two minutes.*
3. **TK-3: A `PriorityQueue` only guarantees its head.** Print or iterate it and you get heap order, so sort a copy or drain a copy. *Interview value: it separates people who have used one from people who have read about one.*
4. **TK-4: Use `comparingInt(...)` with a method reference, put `.reversed()` right after the key it should flip, and finish with a unique key.** *Interview value: it gives deterministic ordering and is one clean line to explain.*
5. **TK-5: Name constants `UPPER_SNAKE` and fields for what they are (`queue`, `ORDER`).** *Interview value: readability is scored, and reviewers read your names first.*
6. **TK-6: Delete imports you don't use, especially ones from `code/`.** *Interview value: a clean file looks deliberate.*
