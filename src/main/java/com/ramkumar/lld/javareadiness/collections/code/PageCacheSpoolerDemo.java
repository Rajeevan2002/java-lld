package com.ramkumar.lld.javareadiness.collections.code;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Worked Example — Phase 4 Java Readiness | 4.1 Collections Deep Dive
 * Scenario A: Browser Page Cache (LRU) + Print Spooler (priority queue)
 *
 * Concepts demonstrated (each is labelled inline):
 *   [LRU-ORDER]   LinkedHashMap access-order constructor (third arg = true)
 *   [LRU-EVICT]   removeEldestEntry hook decides when to evict
 *   [ENCAPSULATE] final capacity, eviction counter exposed read-only
 *   [PQ-CMP]      PriorityQueue + Comparator chain (reversed first key, tie-breakers)
 *   [PQ-NOSORT]   iterating a PriorityQueue is NOT sorted; sort a copy instead
 *   [OPTIONAL]    poll() returns null on empty -> wrapped in Optional
 *   [DEFENSIVE]   getters hand out copies, never internal collections
 */
public class PageCacheSpoolerDemo {

    // ════════════════════════════════════════════════════════════════
    // Part 1 — LRU page cache
    // ════════════════════════════════════════════════════════════════
    static class PageCache extends LinkedHashMap<String, String> {

        private static final long serialVersionUID = 1L;   // LinkedHashMap is Serializable

        // [ENCAPSULATE] capacity never changes after construction -> final
        private final int capacity;
        private int evictionCount;                          // mutated only by removeEldestEntry

        PageCache(int capacity) {
            // [LRU-ORDER] initialCapacity=16, loadFactor=0.75, accessOrder=TRUE.
            // With false (the default) this would be a FIFO cache, not LRU.
            super(16, 0.75f, true);
            if (capacity < 1) {
                throw new IllegalArgumentException("capacity must be >= 1");
            }
            this.capacity = capacity;
        }

        // [LRU-EVICT] Called by put() AFTER inserting. "eldest" = least recently accessed.
        // Strictly greater: with ">=" the cache would only ever hold capacity-1 entries.
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            boolean evict = size() > capacity;
            if (evict) {
                evictionCount++;
                System.out.println("  [CACHE] evicting " + eldest.getKey());
            }
            return evict;
        }

        int getEvictionCount() {
            return evictionCount;
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Part 2 — Print spooler: highest priority first, oldest first on ties
    // ════════════════════════════════════════════════════════════════
    static class PrintJob {
        private final String id;
        private final int priority;      // 1 (low) .. 10 (urgent)
        private final int submittedAt;   // logical tick

        PrintJob(String id, int priority, int submittedAt) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("id must not be blank");
            }
            if (priority < 1 || priority > 10) {
                throw new IllegalArgumentException("priority must be 1-10");
            }
            this.id = id;
            this.priority = priority;
            this.submittedAt = submittedAt;
        }

        String getId() { return id; }
        int getPriority() { return priority; }
        int getSubmittedAt() { return submittedAt; }

        @Override
        public String toString() {
            return id + "[P" + priority + ", t=" + submittedAt + "]";
        }
    }

    static class Spooler {
        // [PQ-CMP] Method reference so the element type is known and .reversed() compiles.
        // reversed() flips only the key built so far (priority) -> highest first.
        // Later keys are ascending: oldest first, then id for a deterministic total order.
        private static final Comparator<PrintJob> ORDER =
                Comparator.comparingInt(PrintJob::getPriority).reversed()
                          .thenComparingInt(PrintJob::getSubmittedAt)
                          .thenComparing(PrintJob::getId);

        private final PriorityQueue<PrintJob> queue = new PriorityQueue<>(ORDER);

        void submit(PrintJob job) {
            if (job == null) {
                throw new IllegalArgumentException("job must not be null");
            }
            queue.offer(job);                               // O(log n)
        }

        // [OPTIONAL] poll() returns null when empty; callers get an explicit "maybe".
        Optional<PrintJob> next() {
            return Optional.ofNullable(queue.poll());       // O(log n)
        }

        // [PQ-NOSORT] queue.toString()/iteration is heap order. Sort a stream of the
        // contents with the same comparator; the queue itself is left untouched.
        // [DEFENSIVE] returns a new immutable list.
        List<String> pendingIds() {
            return queue.stream().sorted(ORDER).map(PrintJob::getId).toList();
        }

        int size() { return queue.size(); }

        // Shows the trap: heap order, usually NOT the print order.
        List<String> rawIterationOrder() {
            List<String> ids = new ArrayList<>();
            for (PrintJob j : queue) ids.add(j.getId());
            return ids;
        }
    }

    // ════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        System.out.println("=== [LRU-ORDER] access order ===");
        PageCache cache = new PageCache(2);
        cache.put("/home", "<home>");
        cache.put("/about", "<about>");
        System.out.println("keys after 2 puts      : " + cache.keySet());   // [/home, /about]
        cache.get("/home");                                                 // /home becomes youngest
        System.out.println("keys after get(/home)  : " + cache.keySet());   // [/about, /home]

        System.out.println("\n=== [LRU-EVICT] eviction ===");
        cache.put("/blog", "<blog>");                                       // evicts /about
        System.out.println("keys                   : " + cache.keySet());   // [/home, /blog]
        System.out.println("evictions              : " + cache.getEvictionCount()); // 1

        System.out.println("\n=== validation ===");
        try {
            new PageCache(0);
        } catch (IllegalArgumentException e) {
            System.out.println("PageCache(0)           : " + e.getMessage());
        }

        System.out.println("\n=== [PQ-CMP] spooler ordering ===");
        Spooler s = new Spooler();
        s.submit(new PrintJob("J-1", 5, 10));
        s.submit(new PrintJob("J-2", 9, 12));
        s.submit(new PrintJob("J-3", 9, 11));
        s.submit(new PrintJob("J-4", 1, 1));
        s.submit(new PrintJob("J-5", 5, 10));
        System.out.println("pendingIds (sorted)    : " + s.pendingIds());   // [J-3, J-2, J-1, J-5, J-4]

        System.out.println("\n=== [PQ-NOSORT] raw iteration is heap order ===");
        System.out.println("raw iteration          : " + s.rawIterationOrder()); // not the print order

        System.out.println("\n=== [OPTIONAL] draining ===");
        System.out.println("next                   : " + s.next().map(PrintJob::getId).orElse("none")); // J-3
        System.out.println("size                   : " + s.size());                                      // 4
        Spooler empty = new Spooler();
        System.out.println("next on empty          : " + empty.next().isPresent());                      // false
    }
}
