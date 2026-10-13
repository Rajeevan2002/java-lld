# 4.2 Recurring LLD Patterns in Java

> Seven small patterns that appear in nearly every machine-coding problem. Write them from memory.
> Quick table versions live in [`cheatsheet/docs/README.md`](../../cheatsheet/docs/README.md) §3; this page explains the *why* and the traps.

---

## 1. The Skeleton Every Exercise Shares

```
 caller ──▶ Service ──────────▶ Repository ──────────▶ Map<String, Entity>
            │ validates          │ stores / finds        (LinkedHashMap)
            │ generates ids      │ returns copies
            │ prints [Service]   │ Optional<T> findById
            ▼                    ▼
         Entity (immutable id, validated in constructor)
```

Layer rules: the **entity** guards its own invariants, the **repository** only stores, the **service** owns rules and ids.

---

## 2. ID Generation

| Approach | Use when | Gotcha |
|---|---|---|
| instance `int counter` | one service owns ids, deterministic tests | not thread-safe |
| `static AtomicInteger` | ids unique across instances/threads | shared between tests, never resets |
| `UUID.randomUUID()` | distributed, no ordering needed | unreadable output, untestable |

```java
private int counter;                                   // per instance, NOT static
String id = String.format("BKG-%03d", ++counter);      // BKG-001, BKG-002 ...
```

**Burned-id trap:** if validation can fail *after* `++counter`, the failed call consumes an id.
Validate (or build the object) first, then commit the counter:

```java
String id = String.format("BKG-%03d", counter + 1);
Booking b = new Booking(id, ...);     // may throw -> counter untouched
counter++;
```

---

## 3. Constructor Validation

Cheap checks first, in the order the spec lists, then assign. Never assign a field before every check passes.

```java
Booking(String id, String organiser, int minutes) {
    if (id == null || id.isBlank())  throw new IllegalArgumentException("id must not be blank");
    Objects.requireNonNull(organiser, "organiser must not be null");   // NPE with message
    if (minutes <= 0)                throw new IllegalArgumentException("minutes must be > 0");
    this.id = id; this.organiser = organiser; this.minutes = minutes;
}
```

| `IllegalArgumentException` | `NullPointerException` (`requireNonNull`) |
|---|---|
| value is present but wrong | a required reference is missing |
| blank string, negative number | null where an object is mandatory |

Follow the spec: some problems say "null or blank → IAE", others split null and blank. Don't choose for yourself.

---

## 4. Exception Decision Tree

```
 problem with an ARGUMENT?  ── yes ─▶ null? ─ yes ─▶ NullPointerException
        │ no                                └ no ──▶ IllegalArgumentException
 object in the wrong STATE for this call? ── yes ─▶ IllegalStateException
        │ no
 lookup by key found nothing? ── yes ─▶ NoSuchElementException   (find* returns Optional.empty())
        │ no
 operation intentionally unsupported ─▶ UnsupportedOperationException (JDK throws it for immutable views)
```

Unchecked by default in LLD code. Never swallow with an empty `catch`; never catch `Exception` just to print.

---

## 5. Repository Skeleton

```java
class BookingRepository {
    private final Map<String, Booking> store = new LinkedHashMap<>();   // keeps insertion order

    void save(Booking b) {
        Objects.requireNonNull(b, "booking must not be null");
        if (store.putIfAbsent(b.getId(), b) != null)                    // one lookup, no containsKey+put
            throw new IllegalArgumentException("duplicate id: " + b.getId());
    }
    Optional<Booking> findById(String id) { return Optional.ofNullable(store.get(id)); }
    List<Booking> findAll()               { return List.copyOf(store.values()); }
}
```

Missing in `findById` → `Optional.empty()`. The **service** decides whether "missing" is an error:

```java
Booking b = repo.findById(id).orElseThrow(() -> new NoSuchElementException("booking not found: " + id));
```

---

## 6. Defensive Copies

| Call | Reflects later changes to source? | Caller can mutate it? |
|---|---|---|
| `store.values()` (raw) | live view of your internals | can `remove` → corrupts the map |
| `new ArrayList<>(src)` | no | yes (own copy) |
| `List.copyOf(src)` | no (snapshot) | no — `UnsupportedOperationException` |
| `Collections.unmodifiableList(src)` | **yes** | no (through the view) |
| `stream()...toList()` | no | no |

Default for getters and query methods: `List.copyOf(...)` or `.toList()`.

---

## 7. `String.format` and Logging

| Spec | Meaning | Example |
|---|---|---|
| `%d` | int/long | `7` |
| `%03d` | zero-padded width 3 | `007` |
| `%s` | any object (`toString`) | `Asha` |
| `%.2f` | 2 decimals | `3.50` |
| `%-10s` | left-aligned, width 10 | `Asha      ` |
| `%n` | platform newline (use with `printf`) | |

```java
System.out.printf("[BookingService] created %s for %s%n", id, organiser);   // [Service] action: detail
```

A literal character in the format (like the `P` in `P%d`) must be typed — it is not added for you.

---

## 8. Package Layout Sketch (4.5 preview)

```
com.example.booking
 ├── domain/       Booking, Room, BookingStatus     (entities, enums, no I/O)
 ├── repository/   BookingRepository                (storage only)
 ├── service/      BookingService                   (rules, ids, logging)
 └── Main.java                                      (demo / wiring)
```
Dependencies point one way: service → repository → domain. Names: `*Service`, `*Repository`, `*Factory`, `*Exception`.

---

## 9. Interview Q&A

**Q1. Why an instance counter instead of a static one?**
Deterministic ids per service instance (tests start at 001); static is shared state that leaks between tests and needs `AtomicInteger` for threads.

**Q2. IAE vs ISE?**
IAE: the *argument* is bad. ISE: the argument is fine but the *object's state* forbids the operation (cancelling a cancelled booking).

**Q3. Why return `Optional` from `findById` rather than null or an exception?**
It forces the caller to decide; the repository doesn't know whether "missing" is an error.

**Q4. `List.copyOf` vs `Collections.unmodifiableList`?**
Snapshot vs live read-only view.

**Q5. Why `putIfAbsent` instead of `containsKey` + `put`?**
One lookup, and no race window between the check and the write.

**Q6. Where does validation belong?**
In the entity constructor for its own invariants (fail fast, object never exists invalid); in the service for rules involving other objects (limits, duplicates, state).

**Q7. How do you keep the id counter correct when validation fails?**
Build the object with the candidate id first, increment only after success.

---

## 10. Common Mistakes

| Mistake | Fix |
|---|---|
| `static int counter` for ids | per-instance field |
| `++counter` before validation → gaps in ids | commit the counter after the object is built |
| `containsKey` then `get` | `get`/`putIfAbsent`/`Optional` |
| Returning `store.values()` or the internal list | `List.copyOf(...)` |
| Throwing `IllegalArgumentException` for wrong state | `IllegalStateException` |
| Returning `null` from `find*` | `Optional.empty()` |
| Wrong validation order vs the spec | check in the order listed |
| Missing literal characters in `String.format` | compare output to every `expected:` line |
| Catching an exception and doing nothing | let it propagate or handle it meaningfully |
