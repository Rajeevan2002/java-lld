package com.ramkumar.lld.designpatterns.behavioral.chainofresponsibility.practice;

/**
 * Practice Exercise — Chain of Responsibility Pattern: Purchase Approval
 *
 * <p><b>Scenario B — Amount-based approval escalation pipeline</b>
 *
 * <p>A company routes purchase requests through a chain of approvers. Each approver
 * handles requests within their spending limit and escalates the rest. The sender
 * (the client code) submits every request to the head of the chain and never needs
 * to know which approver will ultimately sign off.
 *
 * <p><b>Class hierarchy (do not change the names):</b>
 * <pre>
 *   PurchaseRequest   [Request]           ← TODO 1
 *   Approver          [AbstractHandler]   ← TODO 2–3
 *   TeamLead          [ConcreteHandler 1] ← TODO 4
 *   Manager           [ConcreteHandler 2] ← TODO 5
 *   Director          [ConcreteHandler 3] ← TODO 6
 *   CEO               [ConcreteHandler 4] ← TODO 7
 * </pre>
 *
 * <p><b>PurchaseRequest (TODO 1):</b>
 * <ul>
 *   <li>Fields (all {@code private final}): {@code String id}, {@code String description},
 *       {@code double amount}</li>
 *   <li>Constructor: {@code PurchaseRequest(String id, String description, double amount)}</li>
 *   <li>Getters: {@code getId()}, {@code getDescription()}, {@code getAmount()}</li>
 *   <li>{@code toString()}: {@code String.format("PurchaseRequest[%s \"%s\" $%.2f]", id, description, amount)}</li>
 * </ul>
 *
 * <p><b>Approver — abstract base class (TODO 2–3):</b>
 * <ul>
 *   <li>Field: {@code private Approver next} — the next handler in the chain</li>
 *   <li>{@code setNext(Approver next) → Approver} — stores {@code next} in the field,
 *       returns {@code next} (NOT {@code this}) so the caller can extend the chain
 *       fluently: {@code teamLead.setNext(manager).setNext(director).setNext(ceo)}</li>
 *   <li>{@code abstract void approve(PurchaseRequest req)} — subclasses implement the
 *       routing decision</li>
 *   <li>{@code protected void passToNext(PurchaseRequest req)} — if {@code next != null},
 *       calls {@code next.approve(req)}; otherwise prints:
 *       {@code System.out.printf("[Chain] No approver for \"%s\" $%.2f%n", req.getDescription(), req.getAmount())}</li>
 * </ul>
 *
 * <p><b>TeamLead (TODO 4):</b>
 * <ul>
 *   <li>Approves if {@code amount <= 1_000.0}; prints:
 *       {@code System.out.printf("[TeamLead] Approved \"%s\" $%.2f%n", req.getDescription(), req.getAmount())}</li>
 *   <li>Otherwise calls {@code passToNext(req)}</li>
 * </ul>
 *
 * <p><b>Manager (TODO 5):</b>
 * <ul>
 *   <li>Approves if {@code amount <= 10_000.0}; prints:
 *       {@code System.out.printf("[Manager] Approved \"%s\" $%.2f%n", req.getDescription(), req.getAmount())}</li>
 *   <li>Otherwise calls {@code passToNext(req)}</li>
 * </ul>
 *
 * <p><b>Director (TODO 6):</b>
 * <ul>
 *   <li>Approves if {@code amount <= 50_000.0}; prints:
 *       {@code System.out.printf("[Director] Approved \"%s\" $%.2f%n", req.getDescription(), req.getAmount())}</li>
 *   <li>Otherwise calls {@code passToNext(req)}</li>
 * </ul>
 *
 * <p><b>CEO (TODO 7):</b>
 * <ul>
 *   <li>Always approves (no upper limit); prints:
 *       {@code System.out.printf("[CEO] Approved \"%s\" $%.2f%n", req.getDescription(), req.getAmount())}</li>
 *   <li>Does NOT call {@code passToNext()} — CEO is the final decision-maker.
 *       (Still call passToNext() if you want future-proofing — either is acceptable.)</li>
 * </ul>
 *
 * <p><b>Design constraints:</b>
 * <ul>
 *   <li>{@code next} must be {@code private} — subclasses use {@code passToNext()}, never {@code next} directly.</li>
 *   <li>{@code passToNext()} must be {@code protected} — subclasses call it; clients cannot.</li>
 *   <li>{@code setNext()} must return {@code next} (not {@code this} and not {@code void}).</li>
 *   <li>No {@code instanceof}, no type-checking, no switch on class names anywhere.</li>
 *   <li>Client code submits all requests to the head of the chain only — never to a specific handler.</li>
 * </ul>
 */
public class PurchaseApprovalPractice {
    static class PurchaseRequest {
        private final String id;
        private final String description;
        private final double amount;

        public PurchaseRequest(String id, String description, double amount){
            this.id = id;
            this.description = description;
            this.amount = amount;
        }

        public String toString() {
            return String.format("PurchaseRequest[%s \"%s\" $%.2f", id, description, amount);

        }

        public double getAmount() {
            return amount;
        }

        public String getDescription() {
            return description;
        }

        public String getId() {
            return id;
        }
    }

    // ── Approver ───────────────────────────────────────────────────────────────
    static abstract class Approver {
        private Approver next;

        public Approver setNext(Approver next) {
            this.next = next;
            return next;
        }
        abstract void approve(PurchaseRequest request);

        protected void passToNext(PurchaseRequest request){
            if(next != null){
                next.approve(request);
            } else {
                System.out.printf("[Chain] No approver for \"%s\" $%.2f%n",
                        request.getDescription(), request.getAmount());
            }
        }
    }

    static class TeamLead extends Approver {
        @Override
        void approve(PurchaseRequest request){
            if(request.getAmount() <= 1_000.0) {
                System.out.printf("[TeamLead] Approved \"%s\" $%.2f%n", request.getDescription(), request.getAmount());
            } else {
                passToNext(request);
            }
        }
    }

    static class Manager extends Approver {
        @Override
        void approve(PurchaseRequest request){
            if(request.getAmount() <= 10_000.0) {
                System.out.printf("[Manager] Approved \"%s\" $%.2f%n", request.getDescription(), request.getAmount());
            } else {
                passToNext(request);
            }
        }
    }

    // ── TODO 6: Director extends Approver ─────────────────────────────────────
    //    approve(PurchaseRequest req):
    //      if amount <= 50_000.0:
    //        System.out.printf("[Director] Approved \"%s\" $%.2f%n",
    //                          req.getDescription(), req.getAmount())
    //      else:
    //        passToNext(req)

    static class Director extends Approver {
        @Override
        void approve(PurchaseRequest request){
            if(request.getAmount() <= 50_000.0) {
                System.out.printf("[Director] Approved \"%s\" $%.2f%n", request.getDescription(), request.getAmount());
            } else {
                passToNext(request);
            }
        }
    }

    // ── TODO 7: CEO extends Approver ──────────────────────────────────────────
    //    approve(PurchaseRequest req):
    //      always approve — no upper limit
    //      System.out.printf("[CEO] Approved \"%s\" $%.2f%n",
    //                        req.getDescription(), req.getAmount())
    //      (does NOT call passToNext — CEO is the final decision-maker)

    static class CEO extends Approver {
        @Override
        void approve(PurchaseRequest request){
            System.out.printf("[CEO] Approved \"%s\" $%.2f%n", request.getDescription(), request.getAmount());
        }
    }

    // ── DO NOT MODIFY — test harness ──────────────────────────────────────────
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: TeamLead approves small request ≤ $1,000 (TODO 1, 2–4) ───────
         TeamLead tl = new TeamLead();
         tl.approve(new PurchaseRequest("PR-001", "Office Supplies", 499.99));
         // expected: [TeamLead] Approved "Office Supplies" $499.99

        // ── Test 2: Manager approves mid request ≤ $10,000 (TODO 5) ──────────────
         Manager mgr = new Manager();
         mgr.approve(new PurchaseRequest("PR-002", "Ergonomic Chairs x10", 4_500.00));
         // expected: [Manager] Approved "Ergonomic Chairs x10" $4500.00

        // ── Test 3: Full chain — each request reaches the right approver (TODO 2–7) ─
         TeamLead chain = new TeamLead();
         chain.setNext(new Manager()).setNext(new Director()).setNext(new CEO());
         chain.approve(new PurchaseRequest("PR-003", "Keyboard",       800.00));
         chain.approve(new PurchaseRequest("PR-004", "Laptop Fleet",   7_200.00));
         chain.approve(new PurchaseRequest("PR-005", "Office Reno",   35_000.00));
         chain.approve(new PurchaseRequest("PR-006", "New HQ",       500_000.00));
         // expected:
         //   [TeamLead] Approved "Keyboard"     $800.00
         //   [Manager]  Approved "Laptop Fleet" $7200.00
         //   [Director] Approved "Office Reno"  $35000.00
         //   [CEO]      Approved "New HQ"       $500000.00

        // ── Test 4: setNext() returns next for fluent chaining (TODO 2) ────────
         TeamLead tl4 = new TeamLead();
         Manager  m4  = new Manager();
         Approver returned = tl4.setNext(m4);
         System.out.println("Test 4 — setNext returns next: "
             + (returned == m4 ? "PASSED" : "FAILED"));

        // ── Test 5: Request falls through incomplete chain (TODO 3) ─────────────
         TeamLead partial = new TeamLead();
         partial.setNext(new Manager());   // no Director, no CEO
         partial.approve(new PurchaseRequest("PR-007", "Corporate Jet", 2_000_000.00));
         // expected: [Chain] No approver for "Corporate Jet" $2000000.00

        // ── Test 6: Head-of-chain handles request itself — passToNext NOT called ─
         TeamLead tl6 = new TeamLead();
         tl6.setNext(new Manager()).setNext(new Director()).setNext(new CEO());
         tl6.approve(new PurchaseRequest("PR-008", "USB Hub", 25.00));
         // expected: [TeamLead] Approved "USB Hub" $25.00   (Manager/Director/CEO not printed)

        // ── Test 7: Boundary values — exact limit amounts (TODO 4–7) ─────────────
         TeamLead tl7 = new TeamLead();
         tl7.setNext(new Manager()).setNext(new Director()).setNext(new CEO());
         tl7.approve(new PurchaseRequest("PR-009", "At TL limit",  1_000.00));   // TeamLead
         tl7.approve(new PurchaseRequest("PR-010", "Just over TL", 1_000.01));   // Manager
         tl7.approve(new PurchaseRequest("PR-011", "At Mgr limit", 10_000.00));  // Manager
         tl7.approve(new PurchaseRequest("PR-012", "Just over Mgr",10_000.01));  // Director
         // expected:
         //   [TeamLead] Approved "At TL limit"   $1000.00
         //   [Manager]  Approved "Just over TL"  $1000.01
         //   [Manager]  Approved "At Mgr limit"  $10000.00
         //   [Director] Approved "Just over Mgr" $10000.01

        // ── Test 8: Polymorphic sender — submits to Approver reference, not concrete type ──
         Approver chain8 = new TeamLead();
         chain8.setNext(new Manager()).setNext(new Director()).setNext(new CEO());
         chain8.approve(new PurchaseRequest("PR-013", "Monitor",    350.00));
         chain8.approve(new PurchaseRequest("PR-014", "Workshop",  8_000.00));
         chain8.approve(new PurchaseRequest("PR-015", "Data Center", 200_000.00));
         // expected:
         //   [TeamLead] Approved "Monitor"     $350.00
         //   [Manager]  Approved "Workshop"    $8000.00
         //   [CEO]      Approved "Data Center" $200000.00
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HINTS — read only if stuck
    // ════════════════════════════════════════════════════════════════════════════

    // HINT 1 (Gentle):
    //   Each approver has a spending limit. A request within the limit is approved
    //   immediately. A request above the limit is handed off to the next approver.
    //   The sender always submits to the first approver — it never needs to know
    //   which approver will ultimately say yes. Each approver only needs to know
    //   about the next one, not about the whole chain.

    // HINT 2 (Direct):
    //   Use the Chain of Responsibility pattern.
    //   Approver is an abstract class with a private Approver next field.
    //   setNext(Approver) stores next and returns it (for fluent chaining).
    //   abstract void approve(PurchaseRequest) is the routing decision.
    //   protected passToNext(PurchaseRequest) forwards to next (or prints "no approver").
    //   Each concrete approver checks the amount: if within limit → approve + print;
    //   otherwise → passToNext(req).

    // HINT 3 (Near-solution skeleton — class outlines only, no method bodies):
    //
    //   static class PurchaseRequest {
    //       private final String id, description;
    //       private final double amount;
    //       PurchaseRequest(String id, String description, double amount) { ... }
    //       String getId()          { ... }
    //       String getDescription() { ... }
    //       double getAmount()      { ... }
    //       @Override public String toString() { ... }
    //   }
    //
    //   static abstract class Approver {
    //       private Approver next;
    //       Approver setNext(Approver next) { this.next = next; return next; }
    //       abstract void approve(PurchaseRequest req);
    //       protected void passToNext(PurchaseRequest req) {
    //           if (next != null) next.approve(req);
    //           else System.out.printf("[Chain] No approver for \"%s\" $%.2f%n", ...);
    //       }
    //   }
    //
    //   static class TeamLead extends Approver {
    //       @Override void approve(PurchaseRequest req) {
    //           if (req.getAmount() <= 1_000.0) { /* print */ }
    //           else passToNext(req);
    //       }
    //   }
    //   // Manager, Director, CEO follow the same pattern with their own limits
}
