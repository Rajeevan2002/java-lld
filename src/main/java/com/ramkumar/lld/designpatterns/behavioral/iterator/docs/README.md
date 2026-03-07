# Iterator Design Pattern

## Intent

Provide a way to access elements of a collection sequentially **without exposing its underlying structure** (array, linked list, tree, etc.).

The pattern separates the traversal algorithm from the collection — the collection owns the data; the iterator owns the cursor.

---

## The Problem Without Iterator

```java
// Client is coupled to BookShelf's internal array — breaks encapsulation
BookShelf shelf = new BookShelf();
for (int i = 0; i < shelf.books.length; i++) {   // forced to know it's an array
    System.out.println(shelf.books[i]);            // forced to access internal field
}
```

If `BookShelf` later switches to a `LinkedList`, every client loop breaks.

---

## Structure

```
        «interface»                    «interface»
         Iterable<E>                   Iterator<E>
        ┌──────────┐                  ┌───────────┐
        │iterator()│ ─── creates ──▶  │ hasNext() │
        └──────────┘                  │ next()    │
              ▲                       └───────────┘
              │                             ▲
      ┌───────────────┐             ┌───────────────┐
      │  BookShelf    │ ─creates──▶ │BookShelfIter  │
      │  (Aggregate)  │             │  (Concrete)   │
      │  Book[]books  │             │  int cursor   │
      │  int count    │             └───────────────┘
      └───────────────┘
```

**Participants:**
| Role | Java type | Responsibility |
|---|---|---|
| `Iterable<E>` | `java.lang.Iterable<E>` | factory: creates an iterator |
| `Iterator<E>` | `java.util.Iterator<E>` | cursor: traverses the collection |
| Aggregate (Concrete Iterable) | your collection class | holds the data, implements `Iterable` |
| Concrete Iterator | inner class | holds a cursor into the aggregate |

---

## Core Java Interfaces

```java
// java.lang.Iterable<E> — makes a class usable in for-each
public interface Iterable<E> {
    Iterator<E> iterator();   // factory method
}

// java.util.Iterator<E>
public interface Iterator<E> {
    boolean hasNext();        // true if more elements remain
    E next();                 // return current element, advance cursor
    // default void remove() — optional; throws UnsupportedOperationException by default
}
```

---

## Minimal Implementation

```java
import java.util.Iterator;
import java.util.NoSuchElementException;

class BookShelf implements Iterable<Book> {
    private Book[] books;
    private int count = 0;

    BookShelf(int capacity) { books = new Book[capacity]; }
    void addBook(Book b)    { books[count++] = b; }

    @Override
    public Iterator<Book> iterator() {
        return new BookShelfIterator();   // new cursor each time
    }

    // Inner class: sees books[] and count directly (no getters needed)
    private class BookShelfIterator implements Iterator<Book> {
        private int cursor = 0;

        @Override public boolean hasNext() { return cursor < count; }

        @Override public Book next() {
            if (!hasNext()) throw new NoSuchElementException();
            return books[cursor++];
        }
    }
}

// Usage — for-each works automatically because BookShelf implements Iterable
BookShelf shelf = new BookShelf(10);
shelf.addBook(new Book("Clean Code"));
for (Book b : shelf) {           // compiler calls shelf.iterator()
    System.out.println(b);
}
```

---

## Multiple Independent Iterators

Each call to `iterator()` returns a **new** cursor — iterators are independent.

```java
Iterator<Book> it1 = shelf.iterator();
Iterator<Book> it2 = shelf.iterator();

it1.next();   // advances it1's cursor; it2 is unaffected
it2.next();   // also returns first book — independent
```

---

## Filtered Iterator

A common extension: an iterator that skips elements not matching a predicate.

```java
private class GenreIterator implements Iterator<Book> {
    private final String target;
    private int cursor = 0;
    private Book peek = null;       // next matching book (pre-fetched)

    GenreIterator(String target) {
        this.target = target;
        advance();                  // find the first match
    }

    private void advance() {
        peek = null;
        while (cursor < count) {
            Book b = books[cursor++];
            if (b.getGenre().equalsIgnoreCase(target)) {
                peek = b;
                return;
            }
        }
    }

    @Override public boolean hasNext() { return peek != null; }

    @Override public Book next() {
        if (!hasNext()) throw new NoSuchElementException();
        Book result = peek;
        advance();                  // find the next match
        return result;
    }
}
```

Key: `advance()` sets `peek` to the next matching element (or `null`). `hasNext()` checks `peek != null`. `next()` captures `peek`, advances, returns the captured value.

---

## External vs Internal Iteration

| | External | Internal |
|---|---|---|
| Who drives? | Client calls `hasNext()`/`next()` | Collection drives via `forEach(Consumer)` |
| Pause/resume? | Yes — stateful cursor | No — runs to completion |
| Java example | `Iterator<E>` | `Iterable.forEach()`, Streams |
| When to use | Need `break`, stateful traversal | Simple transformations |

```java
// External — client controls the loop
Iterator<Book> it = shelf.iterator();
while (it.hasNext()) {
    Book b = it.next();
    if (b.getYear() > 2020) break;   // stop early
}

// Internal — collection drives
shelf.forEach(b -> System.out.println(b));   // can't break early
```

---

## Comparison: Iterator vs For-Loop vs Stream

| Concern | Iterator | For-loop | Stream |
|---|---|---|---|
| Hides collection internals | Yes | No | Yes |
| Works with for-each | Yes (Iterable) | No | No |
| Lazy evaluation | Yes | Yes | Yes (terminal op) |
| Filtering | Manual (FilteredIterator) | Manual | `.filter()` built-in |
| Stateful cursor | Yes | Manual index | No |
| Best for | Custom collections, lazy traversal | Array/index access | Data pipelines |

---

## Interview Q&A

**Q1: What problem does Iterator solve?**
A: It decouples traversal logic from the collection's internal structure. Clients iterate through a stable interface (`hasNext`/`next`) regardless of whether the collection is an array, linked list, or tree. Changing the internal structure does not break any client code.

**Q2: Why implement `Iterator` as an inner class of the collection?**
A: An inner class has direct access to the outer class's private fields (`books[]`, `count`) without requiring getters. This keeps the iterator efficient and keeps internals private from all external callers.

**Q3: Why does `next()` throw `NoSuchElementException` instead of returning `null`?**
A: `null` is ambiguous — a collection may legitimately store `null` elements. `NoSuchElementException` is unambiguous and matches the contract of `java.util.Iterator`. It also fails fast rather than silently returning a wrong value.

**Q4: How do you support multiple simultaneous iterators?**
A: Each call to `iterator()` returns a **new** instance of the concrete iterator with its own `cursor` field. Because cursor state is per-instance, two iterators on the same collection are fully independent.

**Q5: What is a filtered iterator and when is it useful?**
A: A filtered iterator wraps a collection (or reuses the same array) but skips elements that don't match a predicate. It avoids building a filtered copy of the collection, keeping memory constant. Use it when you need lazy, on-demand filtering over a collection you cannot or should not modify.

**Q6: Iterator vs Iterable — what's the difference?**
A: `Iterable<E>` is a factory: it has one method `iterator()` that creates a new cursor. `Iterator<E>` is the cursor itself: it holds position and has `hasNext()`/`next()`. A class implementing `Iterable` can be used in a for-each loop; implementing `Iterator` alone does not enable for-each.

**Q7: How does Iterator relate to the Hollywood Principle?**
A: The client does not reach into the collection; instead the collection hands the client a cursor object. The collection "calls" the iterator into existence — the client just uses the interface it receives. This is inversion of control at the data-access level.

---

## Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Exposing array/list directly (`getBooks()`) | Breaks encapsulation; client couples to structure | Return `Iterator<Book>` or use `Iterable` |
| Returning `null` from `next()` when empty | Ambiguous; may hide bugs | Throw `NoSuchElementException` |
| Sharing one cursor across multiple callers | Second caller sees wrong position | Return a new iterator instance each call |
| Forgetting to pre-fetch in filtered iterator | `hasNext()` must not advance cursor; `next()` must not advance twice | Pre-fetch in constructor and `advance()` helper |
| Making the concrete iterator public | Exposes internal type; clients should use the `Iterator<E>` interface | Declare it `private class` inside the collection |
| Advancing cursor in `hasNext()` | Calling `hasNext()` twice skips an element | Only advance in `next()` (or in `advance()` helper called once) |
