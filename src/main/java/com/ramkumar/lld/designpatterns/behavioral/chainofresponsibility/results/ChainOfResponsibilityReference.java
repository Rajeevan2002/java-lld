package com.ramkumar.lld.designpatterns.behavioral.chainofresponsibility.results;

/**
 * Reference solution — Chain of Responsibility Pattern: Purchase Approval
 *
 * <p>Key decisions vs common mistakes:
 * <ul>
 *   <li>{@code next} is {@code private} — subclasses cannot access it; all forwarding
 *       goes through {@code passToNext()}, which centralises the null-guard.</li>
 *   <li>{@code setNext()} returns {@code next} (not {@code this}) — enables fluent
 *       chaining {@code a.setNext(b).setNext(c)} without temp variables.
 *       Returning {@code this} silently collapses the chain to {@code a → c}.</li>
 *   <li>{@code passToNext()} is {@code protected} — subclasses call it; external
 *       callers use {@code approve()} only.</li>
 *   <li>Every concrete handler has exactly two branches: approve (stop) OR forward —
 *       never both, never neither.</li>
 *   <li>The fallthrough case in {@code passToNext()} prints explicitly — never silent.</li>
 * </ul>
 */
public class ChainOfResponsibilityReference {

    // ── [Request] ──────────────────────────────────────────────────────────────
    static class PurchaseRequest {
        private final String id;
        private final String description;
        private final double amount;

        PurchaseRequest(String id, String description, double amount) {
            this.id          = id;
            this.description = description;
            this.amount      = amount;
        }

        String getId()          { return id;          }
        String getDescription() { return description; }
        double getAmount()      { return amount;      }

        @Override
        public String toString() {
            // [ClosingBracket] Easy to miss — the format string must be symmetric
            return String.format("PurchaseRequest[%s \"%s\" $%.2f]", id, description, amount);
        }
    }

    // ── [AbstractHandler] ─────────────────────────────────────────────────────
    static abstract class Approver {

        // [Private] next is private — subclasses CANNOT call next.approve() directly.
        // This forces all forwarding through passToNext(), keeping the null-guard in
        // one place rather than duplicated (and potentially omitted) in every subclass.
        private Approver next;

        // [FluentReturn] Returns next (NOT this) so the caller can extend from the
        // handler just added: a.setNext(b).setNext(c) → a→b→c.
        // Returning `this` would make all calls operate on `a` → silent chain collapse.
        Approver setNext(Approver next) {
            this.next = next;
            return next;   // ← CRITICAL: next, not this
        }

        // [AbstractDecision] Each subclass decides: handle or forward
        abstract void approve(PurchaseRequest req);

        // [CentralisedForward] Single null-check. If subclasses called next.approve()
        // directly, each would need its own null-check and its own "no handler" message.
        // Protected: subclasses can call it; external callers cannot.
        protected void passToNext(PurchaseRequest req) {
            if (next != null) {
                next.approve(req);
            } else {
                // [ExplicitFallthrough] Never silently drop an unhandled request
                System.out.printf("[Chain] No approver for \"%s\" $%.2f%n",
                    req.getDescription(), req.getAmount());
            }
        }
    }

    // ── [ConcreteHandler 1] TeamLead — limit $1,000 ───────────────────────────
    static class TeamLead extends Approver {
        @Override
        void approve(PurchaseRequest req) {
            if (req.getAmount() <= 1_000.0) {
                // [Stop] Handle and stop — do NOT call passToNext() after approving
                System.out.printf("[TeamLead] Approved \"%s\" $%.2f%n",
                    req.getDescription(), req.getAmount());
            } else {
                // [Forward] Outside my authority — escalate
                passToNext(req);
            }
        }
    }

    // ── [ConcreteHandler 2] Manager — limit $10,000 ───────────────────────────
    static class Manager extends Approver {
        @Override
        void approve(PurchaseRequest req) {
            if (req.getAmount() <= 10_000.0) {
                System.out.printf("[Manager] Approved \"%s\" $%.2f%n",
                    req.getDescription(), req.getAmount());
            } else {
                passToNext(req);
            }
        }
    }

    // ── [ConcreteHandler 3] Director — limit $50,000 ──────────────────────────
    static class Director extends Approver {
        @Override
        void approve(PurchaseRequest req) {
            if (req.getAmount() <= 50_000.0) {
                System.out.printf("[Director] Approved \"%s\" $%.2f%n",
                    req.getDescription(), req.getAmount());
            } else {
                passToNext(req);
            }
        }
    }

    // ── [ConcreteHandler 4] CEO — no limit, terminal handler ──────────────────
    // [TerminalHandler] CEO always approves — no passToNext() needed.
    // Still safe to call passToNext() here for future-proofing (it would just
    // print the "[Chain] No approver" line since CEO has no next by convention).
    static class CEO extends Approver {
        @Override
        void approve(PurchaseRequest req) {
            System.out.printf("[CEO] Approved \"%s\" $%.2f%n",
                req.getDescription(), req.getAmount());
        }
    }

    // ── Reference main() ──────────────────────────────────────────────────────
    public static void main(String[] args) {

        // ── Test 1: TeamLead approves small request ≤ $1,000 ─────────────────
        TeamLead tl = new TeamLead();
        tl.approve(new PurchaseRequest("PR-001", "Office Supplies", 499.99));
        // [TeamLead] Approved "Office Supplies" $499.99

        // ── Test 2: Manager approves mid request ≤ $10,000 ───────────────────
        Manager mgr = new Manager();
        mgr.approve(new PurchaseRequest("PR-002", "Ergonomic Chairs x10", 4_500.00));
        // [Manager] Approved "Ergonomic Chairs x10" $4500.00

        // ── Test 3: Full chain — each request reaches the right approver ──────
        TeamLead chain = new TeamLead();
        chain.setNext(new Manager()).setNext(new Director()).setNext(new CEO());
        chain.approve(new PurchaseRequest("PR-003", "Keyboard",       800.00));
        chain.approve(new PurchaseRequest("PR-004", "Laptop Fleet",   7_200.00));
        chain.approve(new PurchaseRequest("PR-005", "Office Reno",   35_000.00));
        chain.approve(new PurchaseRequest("PR-006", "New HQ",       500_000.00));

        // ── Test 4: setNext() returns next (not this) ─────────────────────────
        TeamLead tl4 = new TeamLead();
        Manager  m4  = new Manager();
        Approver returned = tl4.setNext(m4);
        System.out.println("Test 4 — setNext returns next: "
            + (returned == m4 ? "PASSED" : "FAILED"));

        // ── Test 5: Request falls through incomplete chain ────────────────────
        TeamLead partial = new TeamLead();
        partial.setNext(new Manager());   // no Director, no CEO
        partial.approve(new PurchaseRequest("PR-007", "Corporate Jet", 2_000_000.00));
        // [Chain] No approver for "Corporate Jet" $2000000.00

        // ── Test 6: Head-of-chain handles request itself ──────────────────────
        TeamLead tl6 = new TeamLead();
        tl6.setNext(new Manager()).setNext(new Director()).setNext(new CEO());
        tl6.approve(new PurchaseRequest("PR-008", "USB Hub", 25.00));
        // [TeamLead] Approved "USB Hub" $25.00   (one line only)

        // ── Test 7: Boundary values ───────────────────────────────────────────
        TeamLead tl7 = new TeamLead();
        tl7.setNext(new Manager()).setNext(new Director()).setNext(new CEO());
        tl7.approve(new PurchaseRequest("PR-009", "At TL limit",   1_000.00));
        tl7.approve(new PurchaseRequest("PR-010", "Just over TL",  1_000.01));
        tl7.approve(new PurchaseRequest("PR-011", "At Mgr limit", 10_000.00));
        tl7.approve(new PurchaseRequest("PR-012", "Just over Mgr",10_000.01));

        // ── Test 8: Polymorphic sender ────────────────────────────────────────
        Approver chain8 = new TeamLead();
        chain8.setNext(new Manager()).setNext(new Director()).setNext(new CEO());
        chain8.approve(new PurchaseRequest("PR-013", "Monitor",      350.00));
        chain8.approve(new PurchaseRequest("PR-014", "Workshop",   8_000.00));
        chain8.approve(new PurchaseRequest("PR-015", "Data Center",200_000.00));

        // ── Test 9: Handler stops the chain — only ONE approval per request ───
        // The most common mistake: a handler that approves AND calls passToNext().
        //
        //   void approve(PurchaseRequest req) {
        //       if (req.getAmount() <= 1_000.0) {
        //           System.out.printf("[TeamLead] ...");
        //           passToNext(req);  ← WRONG — fires a second approval downstream
        //       } else {
        //           passToNext(req);
        //       }
        //   }
        //
        // A $500 request would print "[TeamLead] Approved..." AND "[Manager] Approved..."
        // This test visually verifies only one line is printed for a $500 request.
        // If two lines appear, the handler is not stopping the chain after approving.
        System.out.println("\n── Test 9: Only one approver fires per request ──");
        System.out.println("(only [TeamLead] should print for $500 — no [Manager]/[Director]/[CEO])");
        TeamLead tl9 = new TeamLead();
        tl9.setNext(new Manager()).setNext(new Director()).setNext(new CEO());
        tl9.approve(new PurchaseRequest("PR-T9", "USB Cable", 500.00));
        // Expected: [TeamLead] Approved "USB Cable" $500.00   ← one line, chain stops here
    }
}
