package com.ramkumar.lld.designpatterns.behavioral.chainofresponsibility.code;

/**
 * Worked Example — Chain of Responsibility Pattern: Support Ticket Triage
 *
 * <p><b>Scenario A — Priority-based ticket escalation</b>
 *
 * <p>A tech company's support system routes incoming tickets through three tiers.
 * Each tier handles tickets at its priority level and escalates anything it cannot handle.
 * Adding a new tier or changing routing logic requires zero changes to existing handlers.
 *
 * <p>Participants:
 * <ul>
 *   <li>{@code SupportTicket}   — [Request] data object</li>
 *   <li>{@code SupportHandler}  — [AbstractHandler] owns the chain link and forwarding logic</li>
 *   <li>{@code FrontlineSupport}— [ConcreteHandler 1] LOW priority</li>
 *   <li>{@code TechnicalSupport}— [ConcreteHandler 2] MEDIUM and HIGH priority</li>
 *   <li>{@code CriticalTeam}    — [ConcreteHandler 3] CRITICAL priority</li>
 * </ul>
 */
public class SupportTicketDemo {

    // ── [Request] ──────────────────────────────────────────────────────────────
    enum Priority { LOW, MEDIUM, HIGH, CRITICAL }

    static class SupportTicket {
        private final String id;
        private final String description;
        private final Priority priority;

        SupportTicket(String id, String description, Priority priority) {
            this.id          = id;
            this.description = description;
            this.priority    = priority;
        }

        String   getId()          { return id;          }
        String   getDescription() { return description; }
        Priority getPriority()    { return priority;    }

        @Override
        public String toString() {
            return String.format("Ticket[%s %s \"%s\"]", id, priority, description);
        }
    }

    // ── [AbstractHandler] — owns the chain link; subclasses own the decision ──
    static abstract class SupportHandler {

        // [ChainLink] private so subclasses cannot bypass setNext() or corrupt the chain
        private SupportHandler next;

        // [FluentSetter] Returns next so chains can be assembled without temp variables:
        //   frontline.setNext(technical).setNext(critical)
        SupportHandler setNext(SupportHandler next) {
            this.next = next;
            return next;   // return NEXT (not this) to extend the chain rightward
        }

        // [TemplateMethod] Subclasses define the routing decision
        abstract void handle(SupportTicket ticket);

        // [ForwardingUtility] Centralises null check — subclasses call this instead of next.handle()
        // protected: visible to subclasses but not to external callers
        protected void passToNext(SupportTicket ticket) {
            if (next != null) {
                next.handle(ticket);
            } else {
                // [FallThrough] Last handler in the chain — request is unhandled
                System.out.printf("[Chain] No handler for %s%n", ticket);
            }
        }
    }

    // ── [ConcreteHandler 1] Frontline — handles LOW priority only ─────────────
    static class FrontlineSupport extends SupportHandler {
        @Override
        void handle(SupportTicket ticket) {
            if (ticket.getPriority() == Priority.LOW) {
                // [Handle] This is my responsibility — process and stop
                System.out.printf("[Frontline] Resolved %s%n", ticket);
            } else {
                // [Forward] Out of my tier — escalate to next handler
                passToNext(ticket);
            }
        }
    }

    // ── [ConcreteHandler 2] Technical — handles MEDIUM and HIGH ───────────────
    static class TechnicalSupport extends SupportHandler {
        @Override
        void handle(SupportTicket ticket) {
            if (ticket.getPriority() == Priority.MEDIUM
                    || ticket.getPriority() == Priority.HIGH) {
                System.out.printf("[Technical] Resolved %s%n", ticket);
            } else {
                passToNext(ticket);
            }
        }
    }

    // ── [ConcreteHandler 3] Critical Team — handles CRITICAL ──────────────────
    // [EndOfChain] Last handler in the normal chain — still calls passToNext()
    // so that if someone later inserts a handler after it, the code still works.
    static class CriticalTeam extends SupportHandler {
        @Override
        void handle(SupportTicket ticket) {
            if (ticket.getPriority() == Priority.CRITICAL) {
                System.out.printf("[CriticalTeam] Escalated %s — all hands on deck%n", ticket);
            } else {
                passToNext(ticket);
            }
        }
    }

    // ── main() ────────────────────────────────────────────────────────────────
    public static void main(String[] args) {

        // ── [ChainAssembly] Build the chain — handlers linked left to right ──
        FrontlineSupport frontline = new FrontlineSupport();
        frontline.setNext(new TechnicalSupport())
                 .setNext(new CriticalTeam());
        // setNext(b) returns b — so the second setNext acts on TechnicalSupport

        // ── [Routing] Each ticket finds its handler without the sender knowing ──
        System.out.println("── Ticket routing ──");
        frontline.handle(new SupportTicket("T001", "Password reset", Priority.LOW));
        frontline.handle(new SupportTicket("T002", "App crashes on login", Priority.MEDIUM));
        frontline.handle(new SupportTicket("T003", "Data corruption in prod", Priority.HIGH));
        frontline.handle(new SupportTicket("T004", "Full outage — all regions", Priority.CRITICAL));

        // ── [FallThrough] Chain without a handler for CRITICAL ────────────────
        System.out.println("\n── Incomplete chain (no CriticalTeam) ──");
        FrontlineSupport partial = new FrontlineSupport();
        partial.setNext(new TechnicalSupport());
        partial.handle(new SupportTicket("T005", "Full outage again", Priority.CRITICAL));
        // passToNext() at end of chain prints "[Chain] No handler for ..."

        // ── [PolymorphicSender] Sender holds AbstractHandler, not concrete type ─
        System.out.println("\n── Polymorphic sender ──");
        SupportHandler[] chains = {
            buildFull(),   // full chain
            buildLimitedToFrontline()  // only frontline
        };
        SupportTicket sample = new SupportTicket("T006", "Sample", Priority.MEDIUM);
        for (SupportHandler chain : chains) {
            chain.handle(sample);
        }
    }

    private static SupportHandler buildFull() {
        FrontlineSupport h = new FrontlineSupport();
        h.setNext(new TechnicalSupport()).setNext(new CriticalTeam());
        return h;
    }

    private static SupportHandler buildLimitedToFrontline() {
        return new FrontlineSupport();   // no next — MEDIUM will fall through
    }
}
