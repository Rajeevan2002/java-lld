package com.ramkumar.lld.designpatterns.behavioral.iterator.code;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Worked Example — Iterator Pattern: Book Shelf
 *
 * <p><b>Scenario A — Custom collection with genre-filtered iteration</b>
 *
 * <p>A library manages a BookShelf (array-backed) containing Books.
 * Clients need to iterate all books or only books of a specific genre,
 * without knowing the shelf uses an array internally.
 *
 * <p>Participants:
 * <ul>
 *   <li>{@code Book}             — [DataObject] value holder</li>
 *   <li>{@code BookShelf}        — [Aggregate/Iterable] owns the array; creates iterators</li>
 *   <li>{@code BookShelfIterator}— [ConcreteIterator] full traversal cursor</li>
 *   <li>{@code GenreIterator}    — [FilteredIterator] skips non-matching genres</li>
 * </ul>
 */
public class BookShelfDemo {

    // ── [DataObject] ───────────────────────────────────────────────────────────
    static class Book {
        private final String title;
        private final String genre;   // "fiction" | "nonfiction" | "sci-fi" | ...
        private final int    year;

        Book(String title, String genre, int year) {
            this.title = title;
            this.genre = genre;
            this.year  = year;
        }

        String getTitle() { return title; }
        String getGenre() { return genre; }
        int    getYear()  { return year;  }

        @Override
        public String toString() {
            return String.format("\"%s\" [%s, %d]", title, genre, year);
        }
    }

    // ── [Aggregate] — implements Iterable to enable for-each ──────────────────
    static class BookShelf implements Iterable<Book> {

        private final Book[] books;   // [InternalStructure] hidden from clients
        private int count = 0;

        BookShelf(int capacity) {
            this.books = new Book[capacity];
        }

        void addBook(Book book) {
            if (count >= books.length) throw new IllegalStateException("Shelf is full");
            books[count++] = book;
        }

        int size() { return count; }

        // ── [IteratorFactory] Each call returns a fresh, independent cursor ────
        @Override
        public Iterator<Book> iterator() {
            return new BookShelfIterator();   // new cursor each time — independent
        }

        // [FilteredIteratorFactory] Returns cursor for a single genre
        Iterator<Book> genreIterator(String genre) {
            return new GenreIterator(genre);
        }

        // ── [ConcreteIterator] Full traversal ─────────────────────────────────
        // Private inner class: direct access to books[] and count (no getters needed).
        // Being inner (not static) is deliberate — it sees the enclosing instance's fields.
        private class BookShelfIterator implements Iterator<Book> {

            private int cursor = 0;   // [Cursor] position in the array

            // [hasNext] Pure read — must NOT advance the cursor
            @Override
            public boolean hasNext() {
                return cursor < count;   // count from the enclosing BookShelf
            }

            // [next] Return current element AND advance — two responsibilities in one call
            @Override
            public Book next() {
                // [NoSuchElementException] Contract of java.util.Iterator — NOT null
                if (!hasNext()) throw new NoSuchElementException("No more books");
                return books[cursor++];   // post-increment: return then advance
            }
        }

        // ── [FilteredIterator] Genre-only traversal ───────────────────────────
        // Uses a "peek" / pre-fetch strategy:
        //   - advance() finds the next matching book and stores it in `peek`
        //   - hasNext() checks peek != null  (no cursor movement)
        //   - next()    captures peek, calls advance() for the next match, returns captured
        // This guarantees hasNext() is safe to call multiple times without skipping elements.
        private class GenreIterator implements Iterator<Book> {

            private final String targetGenre;
            private int  cursor = 0;
            private Book peek   = null;   // [PreFetch] next matching element (or null = done)

            GenreIterator(String targetGenre) {
                this.targetGenre = targetGenre;
                advance();   // find the first matching book upfront
            }

            // [advance] Scan forward until we find the next genre match or exhaust the array
            private void advance() {
                peek = null;
                while (cursor < count) {
                    Book candidate = books[cursor++];
                    if (candidate.getGenre().equalsIgnoreCase(targetGenre)) {
                        peek = candidate;
                        return;   // stop as soon as we find one
                    }
                }
                // If we fall through, peek remains null → hasNext() returns false
            }

            @Override
            public boolean hasNext() {
                return peek != null;   // [SafeCheck] no cursor movement — safe to call repeatedly
            }

            @Override
            public Book next() {
                if (!hasNext()) throw new NoSuchElementException("No more " + targetGenre + " books");
                Book result = peek;   // [CaptureFirst] capture before advancing
                advance();            // find the next match
                return result;        // return the captured value (not the new peek)
            }
        }
    }

    // ── Reference main() ──────────────────────────────────────────────────────
    public static void main(String[] args) {

        BookShelf shelf = new BookShelf(10);
        shelf.addBook(new Book("The Great Gatsby",      "fiction",    1925));
        shelf.addBook(new Book("A Brief History",        "nonfiction", 1988));
        shelf.addBook(new Book("Dune",                   "sci-fi",     1965));
        shelf.addBook(new Book("1984",                   "fiction",    1949));
        shelf.addBook(new Book("Sapiens",                "nonfiction", 2011));
        shelf.addBook(new Book("Foundation",             "sci-fi",     1951));

        // ── [ExternalIteration] Manual hasNext/next loop ──────────────────────
        System.out.println("── All books (external iterator) ──");
        Iterator<Book> it = shelf.iterator();
        while (it.hasNext()) {
            System.out.println("  " + it.next());
        }

        // ── [ForEach] Compiler desugars to shelf.iterator() + hasNext/next ─────
        System.out.println("\n── All books (for-each loop) ──");
        for (Book b : shelf) {
            System.out.println("  " + b);
        }

        // ── [MultipleIterators] Two independent cursors on the same shelf ──────
        System.out.println("\n── Two independent iterators ──");
        Iterator<Book> itA = shelf.iterator();
        Iterator<Book> itB = shelf.iterator();
        System.out.println("  itA.next() = " + itA.next());   // first book
        System.out.println("  itB.next() = " + itB.next());   // also first book — independent
        System.out.println("  itA.next() = " + itA.next());   // second book (itA advanced)
        System.out.println("  itB.next() = " + itB.next());   // second book (itB now advances)

        // ── [FilteredIterator] Sci-fi genre only ─────────────────────────────
        System.out.println("\n── Sci-fi books only ──");
        Iterator<Book> scifi = shelf.genreIterator("sci-fi");
        while (scifi.hasNext()) {
            System.out.println("  " + scifi.next());
        }

        // ── [FilteredIterator] Nonfiction only ───────────────────────────────
        System.out.println("\n── Nonfiction books only ──");
        for (Iterator<Book> nf = shelf.genreIterator("nonfiction"); nf.hasNext(); ) {
            System.out.println("  " + nf.next());
        }

        // ── [NoSuchElementException] Exhausted iterator throws, not null ───────
        System.out.println("\n── Exhausted iterator ──");
        Iterator<Book> tiny = new BookShelf(1) {{ addBook(new Book("Solo", "fiction", 2020)); }}.iterator();
        System.out.println("  first: " + tiny.next());
        try {
            tiny.next();   // no more elements
            System.out.println("  FAIL — should have thrown");
        } catch (NoSuchElementException e) {
            System.out.println("  PASS — NoSuchElementException: " + e.getMessage());
        }

        // ── [EmptyCollection] hasNext() immediately false ─────────────────────
        System.out.println("\n── Empty shelf ──");
        Iterator<Book> empty = new BookShelf(5).iterator();
        System.out.println("  hasNext() on empty shelf: " + empty.hasNext());   // false
    }
}
