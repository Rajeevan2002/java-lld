# 4.1 Collections Deep Dive — LRU Cache and PriorityQueue

> Goal: pick the right collection in 10 seconds, write an LRU cache from memory, and build a correctly ordered `PriorityQueue`.
> Companion: the one-page decision table in [`cheatsheet/docs/README.md`](../../cheatsheet/docs/README.md) §1.

---

## 1. The Collections Family

```
                        Iterable
                           │
                       Collection
        ┌──────────────────┼───────────────────┐
       List               Set                Queue ── Deque
   ┌─────┴─────┐      ┌─────┼──────┐         │          │
ArrayList LinkedList HashSet LinkedHashSet TreeSet  PriorityQueue  ArrayDeque
                                              (+ LinkedList is both List and Deque)

                Map  (NOT a Collection)
     ┌───────────┼─────────────┬───────────┐
  HashMap   LinkedHashMap    TreeMap     EnumMap
```

### Decision table

| Need | Use | Why |
|---|---|---|
| Ordered list, index access | `ArrayList` | O(1) `get(i)` |
| Unique items, no order | `HashSet` | O(1) `contains` |
| Unique items, insertion order | `LinkedHashSet` | O(1) + stable iteration |
| Unique items, sorted / range queries | `TreeSet` | O(log n), `floor`/`ceiling` |
| Key → value lookup | `HashMap` | O(1) average |
| Key → value, keep insertion order | `LinkedHashMap` | repository stores, stable output |
| Key → value, **LRU eviction** | `LinkedHashMap(…, true)` | access-order mode |
| Key → value, sorted keys / nearest key | `TreeMap` | `floorKey`, `ceilingKey`, `subMap` |
| Enum keys | `EnumMap` | array-backed, ordered by declaration |
| FIFO queue / LIFO stack | `ArrayDeque` | faster than `LinkedList`, no null |
| "Always give me the most urgent" | `PriorityQueue` | O(log n) offer/poll |

### Complexity at a glance

| Collection | get / contains | add / put | remove | iterate order |
|---|---|---|---|---|
| `ArrayList` | O(1) get, O(n) contains | O(1) amortised at end | O(n) | insertion |
| `HashMap` / `HashSet` | O(1) avg | O(1) avg | O(1) avg | none |
| `LinkedHashMap` | O(1) avg | O(1) avg | O(1) avg | insertion **or access** |
| `TreeMap` / `TreeSet` | O(log n) | O(log n) | O(log n) | sorted |
| `ArrayDeque` | O(n) contains | O(1) at ends | O(1) at ends | insertion |
| `PriorityQueue` | O(1) `peek` | O(log n) `offer` | O(log n) `poll`, O(n) `remove(x)` | **NOT sorted** |

---

## 2. LRU Cache with `LinkedHashMap`

A `LinkedHashMap` is a `HashMap` plus a doubly-linked list through its entries.

```
 HashMap buckets (O(1) lookup)         Linked list (order)
 ┌───┐                                  eldest ───────────────▶ youngest
 │ a │──┐                               [ a ] ⇄ [ b ] ⇄ [ c ]
 │ b │──┼── same entry objects ───────▶   ▲                       ▲
 │ c │──┘                                 │ removeEldestEntry     │ get/put moves
 └───┘                                    │ looks here            │ entry to this end
```

Two switches turn it into an LRU cache:

1. **Access-order mode**: constructor `new LinkedHashMap<>(16, 0.75f, true)` — every `get`/`put` of an existing key moves it to the youngest end.
2. **`removeEldestEntry` hook**: called after every `put`; return `true` and the eldest entry is dropped.

```java
class LruCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    LruCache(int capacity) {
        super(16, 0.75f, true);                 // true = access order
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;               // evict when over capacity
    }
}
```

Trace (capacity 2):

```
put a      [a]
put b      [a, b]
get a      [b, a]        ← a moved to youngest
put c      [b, a, c] → size 3 > 2 → evict eldest b → [a, c]
```

| Variant | Pros | Cons |
|---|---|---|
| `extends LinkedHashMap` | 10 lines, interview-friendly | exposes the whole `Map` API (any `put` can bypass your rules) |
| Wrap a `LinkedHashMap` (composition) | narrow API (`get`, `put`, `size`) | a few more lines |
| Hand-rolled HashMap + doubly linked list | shows you know the internals | 60+ lines, bug-prone; only if the interviewer forbids `LinkedHashMap` |

Not thread-safe. Wrap with `Collections.synchronizedMap(...)` or lock yourself (Phase 5).

---

## 3. `PriorityQueue` and `Comparator`

A `PriorityQueue` is a **binary min-heap** stored in an array. `peek()` is the smallest element *by your comparator*.

```
 heap (array view)                tree view
 [1, 3, 2, 7, 4]                      1
                                    /   \
 poll() → 1, then re-heapify       3     2
                                  / \
                                 7   4
```

```java
PriorityQueue<Task> q = new PriorityQueue<>(
    Comparator.comparingInt(Task::getPriority).reversed()   // highest priority first
              .thenComparingLong(Task::getCreatedAt)         // older first (FIFO among equals)
              .thenComparing(Task::getId));                  // total order, deterministic
```

### The `.reversed()` trap

```java
Comparator.comparingInt(Task::getPriority).reversed();           // OK: type known (method ref)
Comparator.comparingInt(t -> t.getPriority()).reversed();        // ✗ compile error: t is Object
Comparator.comparingInt((Task t) -> t.getPriority()).reversed(); // OK: explicit lambda type
```

`reversed()` flips **everything built so far**, so put it right after the first key, then chain `thenComparing`.

### Iterating a `PriorityQueue` does NOT give sorted order

```java
q.toString();                      // heap order, e.g. [T-3, T-1, T-2]  ✗ not sorted
q.stream().toList();               // same heap order ✗
q.stream().sorted(cmp).toList();   // ✓ sorted, queue untouched
// or: copy into a new PriorityQueue and poll() until empty
```

### Ties

If two elements compare equal, `poll()` order between them is **unspecified**. Always finish the comparator with a unique key (id) so output is deterministic.

---

## 4. Other tools you need

### `Map` utility methods

```java
Map<String, Integer> counts = new HashMap<>();
counts.merge("a", 1, Integer::sum);                       // count words
counts.getOrDefault("z", 0);                              // 0, no null check
Map<String, List<String>> groups = new HashMap<>();
groups.computeIfAbsent("k", k -> new ArrayList<>()).add("v");   // multimap
```

### Immutable vs unmodifiable

| | `List.of(a, b)` | `List.copyOf(src)` | `Collections.unmodifiableList(src)` |
|---|---|---|---|
| Mutation | throws `UnsupportedOperationException` | throws | throws |
| Snapshot of `src`? | n/a | **yes** (independent copy) | **no** (live view — changes to `src` show through) |
| `null` elements | NPE | NPE | allowed |

Getter rule: return `List.copyOf(items)` (or `Collections.unmodifiableList`) — never the raw internal list.

### `ArrayDeque` as stack and queue

```java
Deque<Integer> stack = new ArrayDeque<>();  stack.push(1); stack.pop();     // LIFO
Queue<Integer> queue = new ArrayDeque<>();  queue.offer(1); queue.poll();   // FIFO
```
Do not use `Stack` or `Vector` (legacy, synchronised). `poll()`/`peek()` return `null` when empty; `pop()`/`remove()` throw.

### `TreeMap` nearest-key queries

```java
TreeMap<Integer, String> t = new TreeMap<>();
t.floorKey(10);    // greatest key <= 10
t.ceilingKey(10);  // smallest key >= 10
```

---

## 5. Interview Q&A

**Q1. How does `HashMap` work and what happens on collision?**
Key's `hashCode()` picks a bucket; collisions chain in a list (tree when a bucket grows past 8). Needs consistent `equals`/`hashCode`. Resizes (rehash) at load factor 0.75.

**Q2. Build an LRU cache.**
`LinkedHashMap` with access-order `true` and `removeEldestEntry` returning `size() > capacity`. O(1) get/put. Mention the manual HashMap + doubly linked list alternative.

**Q3. Why isn't iterating a `PriorityQueue` sorted?**
It is a heap — only the root is guaranteed minimal. Use `poll()` repeatedly or sort a copy.

**Q4. `ArrayList` vs `LinkedList`?**
`ArrayList` almost always: contiguous memory, O(1) index. `LinkedList` only wins for O(1) insert at a *known node*, which `List` APIs rarely give you; use `ArrayDeque` for queues.

**Q5. `HashMap` vs `TreeMap` vs `LinkedHashMap`?**
Unordered O(1) / sorted O(log n) / insertion-or-access order O(1).

**Q6. `Comparable` vs `Comparator`?**
`Comparable` = the class's one natural order (`compareTo`). `Comparator` = an external, swappable order — use for domain objects with several valid orders (priority, deadline).

**Q7. `List.copyOf` vs `Collections.unmodifiableList`?**
Copy (snapshot) vs live read-only view of the original.

---

## 6. Common Mistakes

| Mistake | Fix |
|---|---|
| Forgetting `true` (access order) → FIFO cache, not LRU | `super(16, 0.75f, true)` |
| `removeEldestEntry` uses `>=` → cache holds `capacity-1` | `size() > capacity` |
| Printing/iterating a `PriorityQueue` and expecting sorted output | `stream().sorted(cmp)` or drain a copy |
| `comparingInt(t -> …).reversed()` won't compile | method ref or `(T t) -> …` |
| Comparator without a final unique key → non-deterministic ties | end with `thenComparing(Task::getId)` |
| Mutating an element's priority after it is in the queue | heap is not re-sorted; remove + re-add |
| Returning the internal list from a getter | `List.copyOf(...)` |
| `containsKey` then `get` (two lookups, NPE risk) | `get` once / `getOrDefault` / `computeIfAbsent` |
| Using `Stack`, `Vector`, `Hashtable` | `ArrayDeque`, `ArrayList`, `HashMap`/`ConcurrentHashMap` |
| Modifying a collection while for-each iterating it | iterator `remove()`, `removeIf`, or collect then remove |
