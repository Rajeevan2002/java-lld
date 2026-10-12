package com.ramkumar.lld.javareadiness.collections.practice;

import com.ramkumar.lld.javareadiness.collections.code.PageCacheSpoolerDemo;

import java.util.*;

/**
 * Practice Exercise — Phase 4 Java Readiness | 4.1 Collections Deep Dive (size S, lite: 6 TODOs)
 * Scenario B: Support Desk — Knowledge-Base Article Cache (LRU) + Ticket Queue (PriorityQueue)
 *
 * ═══════════════════════════════════════════════════════════════════════
 * PROBLEM STATEMENT
 * ═══════════════════════════════════════════════════════════════════════
 *
 * A support desk keeps the most recently read knowledge-base articles in a small
 * in-memory cache, and hands incoming tickets to agents most-urgent first.
 * Build both pieces. Time is a plain int "tick" (0, 1, 2, ...).
 *
 * ── CLASS: ArticleCache  (an LRU cache, String articleId → String body) ──
 *   Must extend LinkedHashMap<String, String> (nested static class in this file).
 *   Fields:
 *     1. int capacity       (final)
 *     2. int evictionCount  (mutable; starts at 0)
 *   Constructor:
 *     1. ArticleCache(int capacity)
 *        - capacity < 1 → IllegalArgumentException("capacity must be >= 1")
 *        - the map must iterate in ACCESS order (least recently used first):
 *          initial capacity 16, load factor 0.75, access-order flag on
 *   Methods:
 *     1. removeEldestEntry hook: evict only when size() is strictly greater than
 *        capacity; every eviction increments evictionCount
 *     2. getEvictionCount() → int
 *
 * ── CLASS: Ticket  (immutable) ─────────────────────────────────────────
 *   Fields (all final):
 *     1. String id
 *     2. String title
 *     3. int    priority    1 (lowest) .. 5 (most urgent)
 *     4. int    createdAt   tick the ticket was raised
 *   Constructor:
 *     1. Ticket(String id, String title, int priority, int createdAt)
 *        Validate in THIS order:
 *          id null/blank    → IllegalArgumentException("id must not be blank")
 *          title null/blank → IllegalArgumentException("title must not be blank")
 *          priority < 1 or > 5 → IllegalArgumentException("priority must be 1-5")
 *          createdAt < 0    → IllegalArgumentException("createdAt must be >= 0")
 *   Methods:
 *     1. getters getId(), getTitle(), getPriority(), getCreatedAt()
 *     2. toString() → "<id>[P<priority>, t=<createdAt>, <title>]"
 *        e.g.  T-1[P3, t=10, Login fails]
 *
 * ── CLASS: TicketQueue ─────────────────────────────────────────────────
 *   Order of service (a single Comparator<Ticket>):
 *     1. higher priority first
 *     2. then lower createdAt first (older ticket first)
 *     3. then id ascending (so ties are deterministic)
 *   Methods:
 *     1. submit(Ticket t)  — null → IllegalArgumentException("ticket must not be null")
 *     2. next() → Optional<Ticket>  — removes and returns the most urgent ticket;
 *        Optional.empty() when the queue is empty (never null, never throws)
 *     3. size() → int
 *     4. pendingIds() → List<String>  — ids in service order, WITHOUT removing
 *        anything from the queue; the returned list must be unmodifiable
 *
 * ── Design constraints ─────────────────────────────────────────────────
 *   - ArticleCache must use LinkedHashMap's built-in hooks; no manual linked list.
 *   - TicketQueue must be backed by java.util.PriorityQueue.
 *   - Do NOT build pendingIds() by iterating the PriorityQueue directly: its
 *     iteration order is heap order, not service order.
 *   - No hand-written sorting loops; use the Comparator API.
 *   - Do not modify main().
 */
public class SupportDeskPractice {

    // ── Nested class shells (hierarchy given; everything inside is yours) ──

    static class ArticleCache extends LinkedHashMap<String, String> {

        // ── TODO 1: Declare the fields and constructor of ArticleCache
        //    Fields:
        //      int capacity        final
        //      int evictionCount   mutable, starts at 0
        //    Constructor ArticleCache(int capacity):
        //      Call the superclass constructor with initial capacity 16,
        //        load factor 0.75f, and access-order mode ON
        //      capacity < 1 → throw new IllegalArgumentException("capacity must be >= 1")
        //      Store capacity
        //    Also add a serialVersionUID if your IDE warns (optional, not tested)
        private final int capacity;
        private int evictionCount = 0;

        ArticleCache(int capacity) {
            super(16, 0.75f, true);
            if(capacity < 1) throw new IllegalArgumentException("capacity must be >= 1");
            this.capacity = capacity;
        }

        // ── TODO 2: removeEldestEntry hook + getEvictionCount() → int
        //    Override the protected LinkedHashMap hook that receives the eldest entry
        //    Evict (return true) only when size() > capacity — strictly greater
        //    On every eviction, increment evictionCount (per-instance field)
        //    Return false when nothing is evicted
        //    getEvictionCount() returns the current evictionCount
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            if(size() > capacity) {
                this.evictionCount += 1;
                return true;
            }
            return false;
        }

        int getEvictionCount() { return evictionCount;}
    }

    static class Ticket {

        // ── TODO 3: Declare 4 fields and the constructor of Ticket — all fields private final
        //    String id          required
        //    String title       required
        //    int    priority    1..5
        //    int    createdAt   >= 0
        //    Constructor Ticket(String id, String title, int priority, int createdAt)
        //    Validate in this order:
        //      id null or blank    → IllegalArgumentException("id must not be blank")
        //      title null or blank → IllegalArgumentException("title must not be blank")
        //      priority < 1 or > 5 → IllegalArgumentException("priority must be 1-5")
        //      createdAt < 0       → IllegalArgumentException("createdAt must be >= 0")
        //    Then assign all four fields
        private final String id;
        private final String title;
        private final int priority;
        private final int createdAt;

        public Ticket(String id, String title, int priority, int createdAt){
            if(id == null || id.isBlank()) { throw new IllegalArgumentException("id must not be blank");}
            if(title == null || title.isBlank()) { throw new IllegalArgumentException("title must not be blank");}
            if(priority < 1 || priority > 5 ) {throw new IllegalArgumentException("priority must be 1-5"); }
            if(createdAt < 0) { throw new IllegalArgumentException("createdAt must be >= 0"); }

            this.id = id;
            this.title = title;
            this.priority = priority;
            this.createdAt = createdAt;
        }

        // ── TODO 4: getters and toString() for Ticket
        //    getId() → String, getTitle() → String, getPriority() → int, getCreatedAt() → int
        //    toString() → String.format-style: "<id>[P<priority>, t=<createdAt>, <title>]"
        //      e.g. "T-1[P3, t=10, Login fails]"
        public int getPriority() { return this.priority; }
        public int getCreatedAt() { return this.createdAt; }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        @Override
        public String toString() {
            return String.format("%s[%d, t=%d, %s]", id, priority, createdAt, title);
        }
    }

    static class TicketQueue {

        // ── TODO 5: Declare the queue state of TicketQueue
        //    One static final Comparator<Ticket> that encodes the service order:
        //      priority DESCENDING, then createdAt ASCENDING, then id ASCENDING
        //      Build it with the Comparator factory methods (no lambdas that
        //      subtract ints, no manual compare loops). Remember where the
        //      "reverse" must apply so ONLY priority is flipped.
        //    One private final PriorityQueue<Ticket> field, created with that comparator
        //    (Reuse the same comparator in TODO 6 — define it once.)
        private final static Comparator<Ticket> comparator = Comparator.comparing(Ticket::getPriority).reversed()
                .thenComparing(Ticket::getCreatedAt).thenComparing(Ticket::getId);
        private final PriorityQueue<Ticket> field = new PriorityQueue<>(comparator);

        // ── TODO 6: Methods of TicketQueue
        //    submit(Ticket t) → void
        //      Validate: t == null → throw new IllegalArgumentException("ticket must not be null")
        //      Add t to the priority queue
        //    next() → Optional<Ticket>
        //      Remove and return the most urgent ticket wrapped in an Optional
        //      Empty queue → Optional.empty()   (poll() returns null; do NOT throw)
        //    size() → int
        //      Number of tickets still waiting
        //    pendingIds() → List<String>
        //      Ids in service order; the queue must be left unchanged
        //      Do NOT iterate the PriorityQueue directly (heap order ≠ service order)
        //      Result must be unmodifiable (adding to it throws UnsupportedOperationException)
        public void submit(Ticket t){
            if(t== null) throw new IllegalArgumentException("ticket must not be null");
            field.offer(t);
        }

        public Optional<Ticket> next(){
            return Optional.ofNullable(field.poll());
        }

        public int size() { return field.size(); }

        public List<String> pendingIds(){
            return field.stream().sorted(comparator).map(SupportDeskPractice.Ticket::getId).toList();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // DO NOT MODIFY main()
    // ══════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: LRU basics — size, keys in access order (uncomment after TODO 1 and 2) ──
         ArticleCache cache = new ArticleCache(2);
         cache.put("kb-1", "Reset password");
         cache.put("kb-2", "Change email");
         System.out.println(cache.size());                    // expected: 2
         System.out.println(cache.keySet());                  // expected: [kb-1, kb-2]

        // ── Test 2: eviction of the least recently used (uncomment after TODO 2) ──
         cache.put("kb-3", "Delete account");
         System.out.println(cache.keySet());                  // expected: [kb-2, kb-3]
         System.out.println(cache.containsKey("kb-1"));       // expected: false
         System.out.println(cache.getEvictionCount());        // expected: 1

        // ── Test 3: get() refreshes recency (uncomment after TODO 2) ──
         cache.get("kb-2");
         cache.put("kb-4", "Update billing");
         System.out.println(cache.keySet());                  // expected: [kb-2, kb-4]
         System.out.println(cache.getEvictionCount());        // expected: 2

        // ── Test 4: invalid capacity (uncomment after TODO 1) ──
         try {
             new ArticleCache(0);
         } catch (IllegalArgumentException e) {
             System.out.println(e.getMessage());              // expected: capacity must be >= 1
         }

        // ── Test 5: Ticket construction, toString, validation (uncomment after TODO 3 and 4) ──
         Ticket t = new Ticket("T-1", "Login fails", 3, 10);
         System.out.println(t);                               // expected: T-1[P3, t=10, Login fails]
         System.out.println(t.getPriority());                 // expected: 3
         try {
             new Ticket("T-9", "Crash", 6, 0);
         } catch (IllegalArgumentException e) {
             System.out.println(e.getMessage());              // expected: priority must be 1-5
         }
         try {
             new Ticket(" ", "Crash", 2, 0);
         } catch (IllegalArgumentException e) {
             System.out.println(e.getMessage());              // expected: id must not be blank
         }
//
//         ── Test 6: service order — priority desc, createdAt asc, id asc (uncomment after TODO 5 and 6) ──
         TicketQueue q = new TicketQueue();
         q.submit(new Ticket("T-1", "Login fails", 3, 10));
         q.submit(new Ticket("T-2", "Payment error", 5, 12));
         q.submit(new Ticket("T-3", "Site down", 5, 11));
         q.submit(new Ticket("T-4", "Typo on page", 1, 1));
         q.submit(new Ticket("T-5", "Slow search", 3, 10));
         System.out.println(q.size());                        // expected: 5
         System.out.println(q.pendingIds());                  // expected: [T-3, T-2, T-1, T-5, T-4]
//
        // ── Test 7: pendingIds is non-destructive and unmodifiable (uncomment after TODO 6) ──
         System.out.println(q.pendingIds().equals(q.pendingIds()));   // expected: true
         System.out.println(q.size());                        // expected: 5
         try {
             q.pendingIds().add("X");
         } catch (UnsupportedOperationException e) {
             System.out.println("unmodifiable");              // expected: unmodifiable
         }
//
        // ── Test 8: draining with next() (uncomment after TODO 6) ──
         System.out.println(q.next().map(Ticket::getId).orElse("none"));  // expected: T-3
         System.out.println(q.next().map(Ticket::getId).orElse("none"));  // expected: T-2
         System.out.println(q.size());                        // expected: 3
         System.out.println(q.pendingIds());                  // expected: [T-1, T-5, T-4]
//
//         ── Test 9: empty queue and null submit (uncomment after TODO 6) ──
         TicketQueue empty = new TicketQueue();
         System.out.println(empty.next().isPresent());        // expected: false
         try {
             empty.submit(null);
         } catch (IllegalArgumentException e) {
             System.out.println(e.getMessage());              // expected: ticket must not be null
         }
    }
}

/*
 * ═══════════════════════════════════════════════════════════════════════
 * HINTS — read one level at a time, only when stuck
 * ═══════════════════════════════════════════════════════════════════════
 *
 * ── HINT 1 (Gentle) ────────────────────────────────────────────────────
 *   - A HashMap that remembers order has a sibling class. Look at its
 *     constructors: one of them lets you choose WHAT the order means.
 *   - That class also has a protected method that runs after every insertion
 *     and can decide whether the oldest entry should go.
 *   - A queue that always yields the "smallest" element needs a definition of
 *     "smallest". Think about how to say "bigger priority counts as smaller".
 *   - Printing a heap-backed queue shows its internal layout, not the order
 *     you will poll in. How else could you see the order without polling?
 *
 * ── HINT 2 (Direct) ────────────────────────────────────────────────────
 *   - super(16, 0.75f, true) → access-order LinkedHashMap
 *   - protected boolean removeEldestEntry(Map.Entry<String, String> eldest)
 *   - Comparator.comparingInt(Ticket::getPriority).reversed()
 *         .thenComparingInt(Ticket::getCreatedAt).thenComparing(Ticket::getId)
 *     (use a method reference in the first call so .reversed() compiles)
 *   - new PriorityQueue<>(COMPARATOR); queue.offer(t); queue.poll()
 *   - Optional.ofNullable(queue.poll())
 *   - queue.stream().sorted(COMPARATOR).map(Ticket::getId).toList()
 *     (Stream.toList() is already unmodifiable)
 *
 * ── HINT 3 (Near-solution) ─────────────────────────────────────────────
 *   static class ArticleCache extends LinkedHashMap<String, String> {
 *       private final int capacity;
 *       private int evictionCount;
 *       ArticleCache(int capacity) {
 *           super(16, 0.75f, true);
 *           // validate, then assign
 *       }
 *       @Override
 *       protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
 *           // compare size() with capacity; count + return
 *       }
 *       int getEvictionCount() { ... }
 *   }
 *
 *   static class Ticket {
 *       private final String id; private final String title;
 *       private final int priority; private final int createdAt;
 *       Ticket(String id, String title, int priority, int createdAt) { ...ordered validation... }
 *       // getters, toString
 *   }
 *
 *   static class TicketQueue {
 *       private static final Comparator<Ticket> ORDER = ...;
 *       private final PriorityQueue<Ticket> queue = new PriorityQueue<>(ORDER);
 *       void submit(Ticket t) { ... }
 *       Optional<Ticket> next() { ... }
 *       int size() { ... }
 *       List<String> pendingIds() { ... }
 *   }
 */
