package com.ramkumar.lld.designpatterns.behavioral.iterator.results;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Reference solution — Iterator Pattern: Order History
 *
 * <p>Key decisions vs common mistakes:
 * <ul>
 *   <li>{@code advance()} is {@code private} — it mutates the cursor and {@code peek};
 *       exposing it publicly allows external callers to corrupt iterator state silently.</li>
 *   <li>{@code hasNext()} is a pure read — it checks {@code peek != null} without moving
 *       any cursor. Callers may invoke it multiple times safely.</li>
 *   <li>Each {@code iterator()} / {@code statusIterator()} call returns a NEW inner-class
 *       instance with its own {@code cursor} field — independent cursors.</li>
 *   <li>Inner classes (non-static) are used so they can access {@code orders[]} and
 *       {@code count} directly from the enclosing {@code OrderHistory} instance.</li>
 *   <li>{@code next()} throws {@code NoSuchElementException} — never returns {@code null}.</li>
 * </ul>
 */
public class IteratorReference {

    // ── [DataObject] ───────────────────────────────────────────────────────────
    static class Order {
        private final String orderId;
        private final double amountUsd;
        private final String status;

        Order(String orderId, double amountUsd, String status) {
            this.orderId    = orderId;
            this.amountUsd  = amountUsd;
            this.status     = status;
        }

        String getOrderId()   { return orderId;   }
        double getAmountUsd() { return amountUsd; }
        String getStatus()    { return status;    }

        @Override
        public String toString() {
            return String.format("Order[%s $%.2f %s]", orderId, amountUsd, status);
        }
    }

    // ── [Aggregate] ────────────────────────────────────────────────────────────
    static class OrderHistory implements Iterable<Order> {

        private final Order[] orders;   // [InternalStructure] hidden from clients
        private int count = 0;

        OrderHistory(int capacity) {
            this.orders = new Order[capacity];
        }

        void addOrder(Order order) {
            if (count >= orders.length) throw new IllegalStateException("Order history is full");
            orders[count++] = order;
        }

        int size() { return count; }

        // [IteratorFactory] New instance each call — independent cursors
        @Override
        public Iterator<Order> iterator() {
            return new OrderHistoryIterator();
        }

        // [FilteredIteratorFactory] New StatusIterator each call — independent cursors
        Iterator<Order> statusIterator(String status) {
            return new StatusIterator(status);
        }

        // ── [ConcreteIterator] Full traversal ─────────────────────────────────
        // Non-static inner class: sees orders[] and count from the enclosing OrderHistory.
        // private — callers hold Iterator<Order>, never OrderHistoryIterator directly.
        private class OrderHistoryIterator implements Iterator<Order> {

            private int cursor = 0;   // [Cursor] per-instance; new instance = fresh cursor

            // [PureRead] hasNext() checks only — does NOT advance cursor.
            // Safe to call zero or many times without skipping elements.
            @Override
            public boolean hasNext() {
                return cursor < count;
            }

            @Override
            public Order next() {
                // [NoSuchElementException] Never return null — contract of java.util.Iterator
                if (!hasNext()) throw new NoSuchElementException("No more orders");
                return orders[cursor++];   // post-increment: return then advance
            }
        }

        // ── [FilteredIterator] Status-only traversal ──────────────────────────
        // Uses the pre-fetch (peek) strategy:
        //   advance() → finds next matching order → stores in peek (or null if exhausted)
        //   hasNext() → peek != null               (pure read, no cursor movement)
        //   next()    → captures peek, calls advance(), returns captured value
        //
        // This guarantees:
        //   - hasNext() is side-effect-free (safe to call multiple times)
        //   - next() never scans twice for the same element
        //   - No element is skipped regardless of hasNext/next call order
        private class StatusIterator implements Iterator<Order> {

            private final String targetStatus;
            private int  cursor = 0;
            private Order peek  = null;   // [PreFetch] null = no more matches

            StatusIterator(String targetStatus) {
                this.targetStatus = targetStatus;
                advance();   // pre-fetch the first match so hasNext() is immediately correct
            }

            // [advance] Scans forward from cursor until a matching order is found.
            // Stores the match in peek and returns. If exhausted, peek stays null.
            // MUST be private — mutates cursor and peek; external calls would corrupt state.
            private void advance() {
                peek = null;
                while (cursor < count) {
                    Order candidate = orders[cursor++];
                    if (candidate.getStatus().equalsIgnoreCase(targetStatus)) {
                        peek = candidate;
                        return;   // stop; next call to advance() continues from here
                    }
                }
                // loop exhausted without a match → peek stays null → hasNext() returns false
            }

            // [PureRead] Checks pre-fetched result only — zero cursor movement.
            @Override
            public boolean hasNext() {
                return peek != null;
            }

            @Override
            public Order next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more " + targetStatus + " orders");
                }
                Order result = peek;   // [CaptureFirst] save before overwriting peek
                advance();             // find the next match
                return result;         // return the saved value, NOT the new peek
            }
        }
    }

    // ── Reference main() ──────────────────────────────────────────────────────
    public static void main(String[] args) {

        // ── Test 1: Empty OrderHistory — hasNext() immediately false ──────────
        OrderHistory empty = new OrderHistory(5);
        Iterator<Order> emptyIt = empty.iterator();
        System.out.println("Test 1 — empty hasNext(): "
            + (!emptyIt.hasNext() ? "PASSED" : "FAILED"));

        // ── Test 2: Single order — next() returns it, then hasNext() false ────
        OrderHistory single = new OrderHistory(3);
        single.addOrder(new Order("ORD-001", 29.99, "PENDING"));
        Iterator<Order> sit = single.iterator();
        boolean t2a = sit.hasNext();
        Order o = sit.next();
        boolean t2b = !sit.hasNext();
        System.out.println("Test 2 — single order iterator: "
            + (t2a && "ORD-001".equals(o.getOrderId()) && t2b ? "PASSED" : "FAILED"));

        // ── Test 3: Multiple orders returned in insertion order ───────────────
        OrderHistory hist = new OrderHistory(10);
        hist.addOrder(new Order("ORD-001", 49.99, "PENDING"));
        hist.addOrder(new Order("ORD-002", 19.99, "SHIPPED"));
        hist.addOrder(new Order("ORD-003", 99.00, "DELIVERED"));
        hist.addOrder(new Order("ORD-004", 5.50,  "PENDING"));
        Iterator<Order> it = hist.iterator();
        String ids = it.next().getOrderId() + "," + it.next().getOrderId()
                   + "," + it.next().getOrderId() + "," + it.next().getOrderId();
        System.out.println("Test 3 — insertion order: "
            + ("ORD-001,ORD-002,ORD-003,ORD-004".equals(ids) ? "PASSED" : "FAILED (got: " + ids + ")"));

        // ── Test 4: next() on exhausted iterator throws NoSuchElementException ─
        OrderHistory h4 = new OrderHistory(1);
        h4.addOrder(new Order("ORD-X", 10.0, "SHIPPED"));
        Iterator<Order> it4 = h4.iterator();
        it4.next();
        try {
            it4.next();
            System.out.println("Test 4 — NoSuchElementException: FAILED (no exception thrown)");
        } catch (NoSuchElementException e) {
            System.out.println("Test 4 — NoSuchElementException: "
                + ("No more orders".equals(e.getMessage()) ? "PASSED" : "FAILED (msg: " + e.getMessage() + ")"));
        }

        // ── Test 5: Two independent iterators don't interfere ─────────────────
        OrderHistory h5 = new OrderHistory(5);
        h5.addOrder(new Order("A", 1.0, "PENDING"));
        h5.addOrder(new Order("B", 2.0, "PENDING"));
        h5.addOrder(new Order("C", 3.0, "PENDING"));
        Iterator<Order> itX = h5.iterator();
        Iterator<Order> itY = h5.iterator();
        String xFirst  = itX.next().getOrderId();
        String yFirst  = itY.next().getOrderId();
        String xSecond = itX.next().getOrderId();
        String ySecond = itY.next().getOrderId();
        boolean t5 = "A".equals(xFirst) && "A".equals(yFirst)
                  && "B".equals(xSecond) && "B".equals(ySecond);
        System.out.println("Test 5 — independent iterators: " + (t5 ? "PASSED" : "FAILED"));

        // ── Test 6: for-each loop works via Iterable ──────────────────────────
        OrderHistory h6 = new OrderHistory(3);
        h6.addOrder(new Order("ORD-A", 10.0, "DELIVERED"));
        h6.addOrder(new Order("ORD-B", 20.0, "DELIVERED"));
        int t6count = 0;
        for (Order ord : h6) { t6count++; }
        System.out.println("Test 6 — for-each count: "
            + (t6count == 2 ? "PASSED" : "FAILED (got: " + t6count + ")"));

        // ── Test 7: StatusIterator returns only PENDING orders ────────────────
        OrderHistory h7 = new OrderHistory(10);
        h7.addOrder(new Order("P1", 10.0, "PENDING"));
        h7.addOrder(new Order("S1", 20.0, "SHIPPED"));
        h7.addOrder(new Order("P2", 30.0, "PENDING"));
        h7.addOrder(new Order("D1", 40.0, "DELIVERED"));
        h7.addOrder(new Order("P3", 50.0, "PENDING"));
        Iterator<Order> pending = h7.statusIterator("PENDING");
        String pendingIds = pending.next().getOrderId() + ","
                          + pending.next().getOrderId() + ","
                          + pending.next().getOrderId();
        System.out.println("Test 7 — status filter PENDING: "
            + ("P1,P2,P3".equals(pendingIds) ? "PASSED" : "FAILED (got: " + pendingIds + ")"));

        // ── Test 8: StatusIterator — no match → hasNext() false immediately ───
        OrderHistory h8 = new OrderHistory(3);
        h8.addOrder(new Order("X1", 9.99, "SHIPPED"));
        h8.addOrder(new Order("X2", 9.99, "DELIVERED"));
        Iterator<Order> noMatch = h8.statusIterator("PENDING");
        System.out.println("Test 8 — status filter no match: "
            + (!noMatch.hasNext() ? "PASSED" : "FAILED (expected no PENDING orders)"));

        // ── Test 9: size() tracks count + overflow throws ─────────────────────
        OrderHistory h9 = new OrderHistory(5);
        System.out.println("Test 9a — size before add: "
            + (h9.size() == 0 ? "PASSED" : "FAILED (got: " + h9.size() + ")"));
        h9.addOrder(new Order("Z1", 1.0, "PENDING"));
        h9.addOrder(new Order("Z2", 2.0, "SHIPPED"));
        System.out.println("Test 9b — size after 2 adds: "
            + (h9.size() == 2 ? "PASSED" : "FAILED (got: " + h9.size() + ")"));
        OrderHistory full = new OrderHistory(1);
        full.addOrder(new Order("F1", 1.0, "PENDING"));
        try {
            full.addOrder(new Order("F2", 1.0, "PENDING"));
            System.out.println("Test 9c — full throws: FAILED (no exception)");
        } catch (IllegalStateException e) {
            System.out.println("Test 9c — full throws: "
                + ("Order history is full".equals(e.getMessage()) ? "PASSED" : "FAILED (msg: " + e.getMessage() + ")"));
        }

        // ── Test 10 (hasNext() idempotent — most common mistake) ──────────────
        // The most common mistake: hasNext() advances the cursor inside StatusIterator
        // (e.g. by calling advance() conditionally). This causes the second hasNext()
        // call to skip past the pre-fetched element, and next() returns a DIFFERENT
        // order than expected. This test calls hasNext() three times before each next().
        System.out.println("\n── Test 10: hasNext() is side-effect-free (call it N times safely) ──");
        OrderHistory h10 = new OrderHistory(5);
        h10.addOrder(new Order("Q1", 1.0, "SHIPPED"));
        h10.addOrder(new Order("Q2", 2.0, "SHIPPED"));
        h10.addOrder(new Order("Q3", 3.0, "PENDING"));
        h10.addOrder(new Order("Q4", 4.0, "SHIPPED"));

        // Full iterator — calling hasNext() 3 times before each next()
        Iterator<Order> it10 = h10.iterator();
        it10.hasNext(); it10.hasNext(); it10.hasNext();   // 3 calls — must not advance
        String first = it10.next().getOrderId();          // must still be Q1
        it10.hasNext(); it10.hasNext();
        String second = it10.next().getOrderId();         // must be Q2
        System.out.println("Test 10a — full iterator hasNext() idempotent: "
            + ("Q1".equals(first) && "Q2".equals(second) ? "PASSED"
               : "FAILED (got: " + first + ", " + second + ")"));

        // StatusIterator — calling hasNext() 3 times before each next()
        Iterator<Order> si10 = h10.statusIterator("SHIPPED");
        si10.hasNext(); si10.hasNext(); si10.hasNext();
        String sf = si10.next().getOrderId();   // Q1
        si10.hasNext(); si10.hasNext();
        String ss = si10.next().getOrderId();   // Q2 (Q3 is PENDING — skipped)
        si10.hasNext(); si10.hasNext();
        String st = si10.next().getOrderId();   // Q4
        boolean noMore = !si10.hasNext();
        System.out.println("Test 10b — StatusIterator hasNext() idempotent: "
            + ("Q1".equals(sf) && "Q2".equals(ss) && "Q4".equals(st) && noMore
               ? "PASSED" : "FAILED (got: " + sf + ", " + ss + ", " + st + ")"));
    }
}
