package com.ramkumar.lld.designpatterns.behavioral.iterator.practice;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Practice Exercise — Iterator Pattern: Order History
 *
 * <p><b>Scenario B — Custom collection with status-filtered iteration</b>
 *
 * <p>An e-commerce platform stores a customer's order history in a fixed-capacity
 * array. Clients need to iterate all orders or only orders with a specific status,
 * without knowing the internal storage is an array.
 *
 * <p><b>Class hierarchy (do not change the names):</b>
 * <pre>
 *   Order                    [DataObject]           ← TODO 1
 *   OrderHistory             [Aggregate/Iterable]   ← TODO 2–5
 *   OrderHistoryIterator     [ConcreteIterator]     ← TODO 6  (inner class of OrderHistory)
 *   StatusIterator           [FilteredIterator]     ← TODO 7–8 (inner class of OrderHistory)
 * </pre>
 *
 * <p><b>Order (TODO 1):</b>
 * <ul>
 *   <li>Fields (all {@code private final}): {@code String orderId}, {@code double amountUsd},
 *       {@code String status} — one of {@code "PENDING"}, {@code "SHIPPED"}, {@code "DELIVERED"}</li>
 *   <li>Constructor: {@code Order(String orderId, double amountUsd, String status)}</li>
 *   <li>Getters: {@code getOrderId()}, {@code getAmountUsd()}, {@code getStatus()}</li>
 *   <li>{@code toString()}: {@code String.format("Order[%s $%.2f %s]", orderId, amountUsd, status)}</li>
 * </ul>
 *
 * <p><b>OrderHistory (TODO 2–5):</b>
 * <ul>
 *   <li>Fields: {@code private final Order[] orders} (size set at construction),
 *       {@code private int count = 0}</li>
 *   <li>Constructor: {@code OrderHistory(int capacity)} — allocates the array</li>
 *   <li>{@code addOrder(Order order)} — appends order; throws {@code IllegalStateException("Order history is full")}
 *       if {@code count >= orders.length}</li>
 *   <li>{@code size()} — returns {@code count}</li>
 *   <li>{@code iterator()} — returns a new {@code OrderHistoryIterator} (enables for-each)</li>
 *   <li>{@code statusIterator(String status)} — returns a new {@code StatusIterator} for the given status</li>
 * </ul>
 *
 * <p><b>OrderHistoryIterator (TODO 6) — inner class inside OrderHistory:</b>
 * <ul>
 *   <li>Implements {@code java.util.Iterator&lt;Order&gt;}</li>
 *   <li>Field: {@code private int cursor = 0}</li>
 *   <li>{@code hasNext()}: returns {@code cursor < count} (count from the enclosing OrderHistory)</li>
 *   <li>{@code next()}: throws {@code NoSuchElementException("No more orders")} if exhausted;
 *       otherwise returns {@code orders[cursor++]}</li>
 * </ul>
 *
 * <p><b>StatusIterator (TODO 7–8) — inner class inside OrderHistory:</b>
 * <ul>
 *   <li>Implements {@code java.util.Iterator&lt;Order&gt;}</li>
 *   <li>Fields: {@code private final String targetStatus}, {@code private int cursor = 0},
 *       {@code private Order peek = null}</li>
 *   <li>Constructor: {@code StatusIterator(String targetStatus)} — stores status, then calls
 *       {@code advance()} to pre-fetch the first match</li>
 *   <li>{@code advance()}: sets {@code peek = null}; scans forward from {@code cursor} until
 *       it finds an order whose {@code status} equals {@code targetStatus}
 *       (use {@code equalsIgnoreCase}); stores that order in {@code peek} and returns; if the
 *       array is exhausted, {@code peek} stays {@code null}</li>
 *   <li>{@code hasNext()}: returns {@code peek != null}</li>
 *   <li>{@code next()}: throws {@code NoSuchElementException("No more " + targetStatus + " orders")}
 *       if exhausted; otherwise captures {@code peek}, calls {@code advance()}, returns the
 *       captured value</li>
 * </ul>
 *
 * <p><b>Design constraints:</b>
 * <ul>
 *   <li>Both iterator classes must be {@code private} inner classes of {@code OrderHistory}.</li>
 *   <li>Clients must only see {@code Iterator&lt;Order&gt;} — never the concrete iterator types.</li>
 *   <li>{@code hasNext()} must NOT advance any cursor — safe to call multiple times.</li>
 *   <li>{@code next()} must throw {@code NoSuchElementException}, never return {@code null}.</li>
 *   <li>No {@code instanceof}, no type-checking anywhere.</li>
 * </ul>
 */
public class OrderHistoryPractice {

    // ── TODO 1: Order — data object ────────────────────────────────────────────
    static class Order {
        private final String orderId;
        private final double amountUsd;
        private final String status;

        Order(String orderId, double amountUsd, String status){
            this.orderId = orderId;
            this.amountUsd = amountUsd;
            this.status = status;
        }

        public String getOrderId() { return orderId; }

        public String getStatus() { return status; }

        public double getAmountUsd() { return amountUsd; }

        @Override
        public String toString() {
            return String.format("Order[%s $%.2f %s]", orderId, amountUsd, status);
        }
    }


    // ── OrderHistory ───────────────────────────────────────────────────────────

    static class OrderHistory  implements Iterable<Order> {   // student adds: implements Iterable<Order>  (TODO 5)
        private final Order[] orders;
        private int count = 0;

        public OrderHistory(int capacity){
            this.orders = new Order[capacity];
        }

        public void addOrder(Order order){
            if(count >= orders.length) {
                throw new IllegalStateException("Order history is full");
            }
            orders[count++] = order;
        }

        public int size() { return count;}
        public Iterator<Order> iterator() { return new OrderHistoryIterator();}
        public Iterator<Order> statusIterator(String status) { return new StatusIterator(status);}
        private class OrderHistoryIterator implements Iterator<Order> {
            private int cursor = 0;
            public boolean hasNext() {
                return cursor < count;
            }

            public Order next() {
                if(!hasNext()) {
                    throw new NoSuchElementException("No more orders");
                }
                return orders[cursor++];
            }
        }

        private class StatusIterator implements  Iterator<Order> {
            private final String targetStatus;
            private int cursor = 0;
            private Order peek = null;

            public StatusIterator(String targetStatus){
                this.targetStatus = targetStatus;
                advance();
            }

            public void advance(){
                peek = null;
                while(cursor < count) {
                    Order candidate = orders[cursor++];
                    if(candidate.getStatus().equalsIgnoreCase(targetStatus)) {
                        peek = candidate;
                        return;
                    }
                }
            }

            public boolean hasNext() {
                return peek != null;
            }

            public Order next() {
                if(!hasNext()) throw new NoSuchElementException("No more " +  targetStatus + " orders");
                Order result = peek;
                advance();
                return result;
            }
        }
    }

    // ── DO NOT MODIFY — test harness ──────────────────────────────────────────
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: Empty OrderHistory — hasNext() is immediately false (TODO 2–5) ──
         OrderHistory empty = new OrderHistory(5);
         Iterator<Order> emptyIt = empty.iterator();
         System.out.println("Test 1 — empty hasNext(): "
             + (!emptyIt.hasNext() ? "PASSED" : "FAILED"));

        // ── Test 2: Single order — next() returns it, then hasNext() false (TODO 1, 4–6) ──
         OrderHistory single = new OrderHistory(3);
         single.addOrder(new Order("ORD-001", 29.99, "PENDING"));
         Iterator<Order> sit = single.iterator();
         boolean t2a = sit.hasNext();
         Order o = sit.next();
         boolean t2b = !sit.hasNext();
         System.out.println("Test 2 — single order iterator: "
             + (t2a && "ORD-001".equals(o.getOrderId()) && t2b ? "PASSED" : "FAILED"));

        // ── Test 3: Multiple orders returned in insertion order (TODO 1, 4–6) ──────
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

        // ── Test 4: next() on exhausted iterator throws NoSuchElementException (TODO 6) ──
         OrderHistory h4 = new OrderHistory(1);
         h4.addOrder(new Order("ORD-X", 10.0, "SHIPPED"));
         Iterator<Order> it4 = h4.iterator();
         it4.next();   // consume the only element
         try {
             it4.next();
             System.out.println("Test 4 — NoSuchElementException: FAILED (no exception thrown)");
         } catch (NoSuchElementException e) {
             System.out.println("Test 4 — NoSuchElementException: "
                 + ("No more orders".equals(e.getMessage()) ? "PASSED" : "FAILED (msg: " + e.getMessage() + ")"));
         }

        // ── Test 5: Two independent iterators don't interfere (TODO 5–6) ─────────
         OrderHistory h5 = new OrderHistory(5);
         h5.addOrder(new Order("A", 1.0, "PENDING"));
         h5.addOrder(new Order("B", 2.0, "PENDING"));
         h5.addOrder(new Order("C", 3.0, "PENDING"));
         Iterator<Order> itX = h5.iterator();
         Iterator<Order> itY = h5.iterator();
         String xFirst = itX.next().getOrderId();   // "A"
         String yFirst = itY.next().getOrderId();   // "A" — independent cursor
         String xSecond = itX.next().getOrderId();  // "B"
         String ySecond = itY.next().getOrderId();  // "B" — independent cursor
         boolean t5 = "A".equals(xFirst) && "A".equals(yFirst)
                   && "B".equals(xSecond) && "B".equals(ySecond);
         System.out.println("Test 5 — independent iterators: " + (t5 ? "PASSED" : "FAILED"));

        // ── Test 6: for-each loop works via Iterable (TODO 5) ────────────────────
         OrderHistory h6 = new OrderHistory(3);
         h6.addOrder(new Order("ORD-A", 10.0, "DELIVERED"));
         h6.addOrder(new Order("ORD-B", 20.0, "DELIVERED"));
         int t6count = 0;
         for (Order ord : h6) { t6count++; }
         System.out.println("Test 6 — for-each count: "
             + (t6count == 2 ? "PASSED" : "FAILED (got: " + t6count + ")"));

        // ── Test 7: StatusIterator returns only PENDING orders (TODO 7–8) ─────────
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

        // ── Test 8: StatusIterator — no match → hasNext() false immediately (TODO 7–8) ──
         OrderHistory h8 = new OrderHistory(3);
         h8.addOrder(new Order("X1", 9.99, "SHIPPED"));
         h8.addOrder(new Order("X2", 9.99, "DELIVERED"));
         Iterator<Order> noMatch = h8.statusIterator("PENDING");
         System.out.println("Test 8 — status filter no match: "
             + (!noMatch.hasNext() ? "PASSED" : "FAILED (expected no PENDING orders)"));

        // ── Test 9: size() tracks count correctly (TODO 3–4) ─────────────────────
         OrderHistory h9 = new OrderHistory(5);
         System.out.println("Test 9a — size before add: "
             + (h9.size() == 0 ? "PASSED" : "FAILED (got: " + h9.size() + ")"));
         h9.addOrder(new Order("Z1", 1.0, "PENDING"));
         h9.addOrder(new Order("Z2", 2.0, "SHIPPED"));
         System.out.println("Test 9b — size after 2 adds: "
             + (h9.size() == 2 ? "PASSED" : "FAILED (got: " + h9.size() + ")"));
         // addOrder beyond capacity → IllegalStateException
         OrderHistory full = new OrderHistory(1);
         full.addOrder(new Order("F1", 1.0, "PENDING"));
         try {
             full.addOrder(new Order("F2", 1.0, "PENDING"));
             System.out.println("Test 9c — full throws: FAILED (no exception)");
         } catch (IllegalStateException e) {
             System.out.println("Test 9c — full throws: "
                 + ("Order history is full".equals(e.getMessage()) ? "PASSED" : "FAILED (msg: " + e.getMessage() + ")"));
         }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HINTS — read only if stuck
    // ════════════════════════════════════════════════════════════════════════════

    // HINT 1 (Gentle):
    //   Clients should not need to know whether OrderHistory uses an array,
    //   a linked list, or a tree. Define a cursor object that clients use to
    //   step through orders one at a time. The cursor, not the client, knows
    //   how to advance through the internal structure.

    // HINT 2 (Direct):
    //   Use the Iterator pattern.
    //   OrderHistory implements java.lang.Iterable<Order>.
    //   Its iterator() method returns a new OrderHistoryIterator — an inner class
    //   that implements java.util.Iterator<Order> and holds a cursor field (int).
    //   For filtered iteration, a second inner class StatusIterator pre-fetches
    //   the next matching order in a `peek` field; advance() scans forward for
    //   the next match.

    // HINT 3 (Near-solution skeleton — class outlines only, no method bodies):
    //
    //   static class Order {
    //       private final String orderId;
    //       private final double amountUsd;
    //       private final String status;
    //       Order(String orderId, double amountUsd, String status) { ... }
    //       String getOrderId()  { ... }
    //       double getAmountUsd(){ ... }
    //       String getStatus()   { ... }
    //       @Override public String toString() { ... }
    //   }
    //
    //   static class OrderHistory implements Iterable<Order> {
    //       private final Order[] orders;
    //       private int count = 0;
    //       OrderHistory(int capacity) { ... }
    //       void addOrder(Order order) { ... }
    //       int size() { ... }
    //       @Override public Iterator<Order> iterator() { return new OrderHistoryIterator(); }
    //       Iterator<Order> statusIterator(String status) { return new StatusIterator(status); }
    //
    //       private class OrderHistoryIterator implements Iterator<Order> {
    //           private int cursor = 0;
    //           @Override public boolean hasNext() { ... }
    //           @Override public Order next()      { ... }
    //       }
    //
    //       private class StatusIterator implements Iterator<Order> {
    //           private final String targetStatus;
    //           private int cursor = 0;
    //           private Order peek = null;
    //           StatusIterator(String targetStatus) { ...; advance(); }
    //           private void advance() { ... }
    //           @Override public boolean hasNext() { ... }
    //           @Override public Order next()      { ... }
    //       }
    //   }
}
