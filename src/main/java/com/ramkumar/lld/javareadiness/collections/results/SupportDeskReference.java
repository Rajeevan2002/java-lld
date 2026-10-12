package com.ramkumar.lld.javareadiness.collections.results;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Reference solution — Phase 4 | 4.1 Collections Deep Dive
 * Scenario B: Support Desk — LRU article cache + priority ticket queue.
 */
public class SupportDeskReference {

    static class ArticleCache extends LinkedHashMap<String, String> {

        private static final long serialVersionUID = 1L;   // LinkedHashMap is Serializable; silences -Xlint:serial

        private final int capacity;
        private int evictionCount;                          // per-instance, starts at 0

        ArticleCache(int capacity) {
            // Third arg = true -> ACCESS order. Without it this is a FIFO cache, not LRU.
            super(16, 0.75f, true);
            if (capacity < 1) {
                throw new IllegalArgumentException("capacity must be >= 1");
            }
            this.capacity = capacity;
        }

        // Called by put() after insertion. Strictly ">": ">=" would hold only capacity-1 entries.
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            boolean evict = size() > capacity;
            if (evict) {
                evictionCount++;
            }
            return evict;
        }

        int getEvictionCount() {
            return evictionCount;
        }
    }

    static class Ticket {
        private final String id;
        private final String title;
        private final int priority;
        private final int createdAt;

        Ticket(String id, String title, int priority, int createdAt) {
            // Fail fast, in the order the spec lists.
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("id must not be blank");
            }
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("title must not be blank");
            }
            if (priority < 1 || priority > 5) {
                throw new IllegalArgumentException("priority must be 1-5");
            }
            if (createdAt < 0) {
                throw new IllegalArgumentException("createdAt must be >= 0");
            }
            this.id = id;
            this.title = title;
            this.priority = priority;
            this.createdAt = createdAt;
        }

        String getId() { return id; }
        String getTitle() { return title; }
        int getPriority() { return priority; }
        int getCreatedAt() { return createdAt; }

        @Override
        public String toString() {
            // Literal "P" before %d: the format is "T-1[P3, t=10, Login fails]".
            return String.format("%s[P%d, t=%d, %s]", id, priority, createdAt, title);
        }
    }

    static class TicketQueue {
        // comparingInt avoids boxing; method ref lets .reversed() compile.
        // reversed() flips only the priority key (everything built so far).
        // The id tie-break makes the order total, so ties are deterministic.
        private static final Comparator<Ticket> ORDER =
                Comparator.comparingInt(Ticket::getPriority).reversed()
                          .thenComparingInt(Ticket::getCreatedAt)
                          .thenComparing(Ticket::getId);

        private final PriorityQueue<Ticket> queue = new PriorityQueue<>(ORDER);

        void submit(Ticket t) {
            if (t == null) {
                throw new IllegalArgumentException("ticket must not be null");
            }
            queue.offer(t);
        }

        Optional<Ticket> next() {
            return Optional.ofNullable(queue.poll());       // poll() == null when empty
        }

        int size() {
            return queue.size();
        }

        // Iterating the queue gives heap order, so sort a stream of the contents.
        // The queue is untouched; Stream.toList() is unmodifiable.
        List<String> pendingIds() {
            return queue.stream().sorted(ORDER).map(Ticket::getId).toList();
        }
    }

    public static void main(String[] args) {
        // Test 1-3: LRU
        ArticleCache cache = new ArticleCache(2);
        cache.put("kb-1", "Reset password");
        cache.put("kb-2", "Change email");
        System.out.println(cache.size());                       // 2
        System.out.println(cache.keySet());                     // [kb-1, kb-2]
        cache.put("kb-3", "Delete account");
        System.out.println(cache.keySet());                     // [kb-2, kb-3]
        System.out.println(cache.containsKey("kb-1"));          // false
        System.out.println(cache.getEvictionCount());           // 1
        cache.get("kb-2");
        cache.put("kb-4", "Update billing");
        System.out.println(cache.keySet());                     // [kb-2, kb-4]
        System.out.println(cache.getEvictionCount());           // 2

        // Test 4
        try { new ArticleCache(0); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }

        // Test 5
        Ticket t = new Ticket("T-1", "Login fails", 3, 10);
        System.out.println(t);                                  // T-1[P3, t=10, Login fails]
        System.out.println(t.getPriority());                    // 3
        try { new Ticket("T-9", "Crash", 6, 0); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        try { new Ticket(" ", "Crash", 2, 0); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }

        // Test 6-8
        TicketQueue q = new TicketQueue();
        q.submit(new Ticket("T-1", "Login fails", 3, 10));
        q.submit(new Ticket("T-2", "Payment error", 5, 12));
        q.submit(new Ticket("T-3", "Site down", 5, 11));
        q.submit(new Ticket("T-4", "Typo on page", 1, 1));
        q.submit(new Ticket("T-5", "Slow search", 3, 10));
        System.out.println(q.size());                           // 5
        System.out.println(q.pendingIds());                     // [T-3, T-2, T-1, T-5, T-4]
        System.out.println(q.pendingIds().equals(q.pendingIds())); // true
        System.out.println(q.size());                           // 5
        try { q.pendingIds().add("X"); } catch (UnsupportedOperationException e) { System.out.println("unmodifiable"); }
        System.out.println(q.next().map(Ticket::getId).orElse("none")); // T-3
        System.out.println(q.next().map(Ticket::getId).orElse("none")); // T-2
        System.out.println(q.size());                           // 3
        System.out.println(q.pendingIds());                     // [T-1, T-5, T-4]

        // Test 9
        TicketQueue empty = new TicketQueue();
        System.out.println(empty.next().isPresent());           // false
        try { empty.submit(null); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }

        // Extra: catches the most common mistakes (toString format, off-by-one eviction, heap-order trap)
        System.out.println(new Ticket("T-7", "Bug", 2, 4));     // T-7[P2, t=4, Bug]   (missing "P" is the common slip)
        ArticleCache one = new ArticleCache(1);
        one.put("a", "A");
        System.out.println(one.size());                         // 1  (">=" would give 0)
        one.put("b", "B");
        System.out.println(one.keySet() + " " + one.getEvictionCount()); // [b] 1
        TicketQueue rev = new TicketQueue();
        for (int i = 5; i >= 1; i--) rev.submit(new Ticket("R-" + i, "x", i, i));
        System.out.println(rev.pendingIds());                   // [R-5, R-4, R-3, R-2, R-1]
    }
}
