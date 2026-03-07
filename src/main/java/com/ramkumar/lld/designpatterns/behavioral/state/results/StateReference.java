package com.ramkumar.lld.designpatterns.behavioral.state.results;

/**
 * Reference solution — State Pattern: Document Approval Workflow
 *
 * <p>Key decisions vs common mistakes:
 * <ul>
 *   <li>{@code Document.state} is {@code private} — context never inspects it; all
 *       state-dependent behaviour is reached via delegation only.</li>
 *   <li>{@code setState()} is package-private — only state classes in this file call it;
 *       external callers cannot bypass the lifecycle.</li>
 *   <li>Every concrete state implements all four interface methods — including terminal-state
 *       methods that print rejection messages and do nothing else.</li>
 *   <li>{@code ApprovedState} calls no {@code setState()} anywhere — terminal state stays put.</li>
 *   <li>{@code RejectedState.submit()} transitions back to {@code ReviewState} — re-entry
 *       is a first-class part of the state machine, not a special case.</li>
 *   <li>State objects are created fresh per transition ({@code new ReviewState()}) —
 *       never shared singletons — so two Document instances are fully independent.</li>
 * </ul>
 */
public class StateReference {

    // ── [StateInterface] — declares every action the context can perform ───────
    // Every ConcreteState must implement ALL methods, including ones that are
    // meaningful only as "invalid in this state" error messages.
    interface DocumentState {
        void   submit(Document doc);
        void   approve(Document doc);
        void   reject(Document doc);
        String getStatus();
    }

    // ── [Context] — holds and delegates; never inspects state ─────────────────
    static class Document {

        // [Private] External callers never read or switch on this field.
        // All state-dependent behaviour is reached only through the action methods.
        private DocumentState state;
        private final String  title;

        Document(String title) {
            this.title = title;
            this.state = new DraftState();   // [InitialState] always starts as a draft
        }

        String getTitle()  { return title;                }
        String getStatus() { return state.getStatus();    }

        // [PureDelegation] No instanceof, no switch, no state inspection here.
        // The context's job is to forward — the state's job is to decide.
        void submit()  { state.submit(this);  }
        void approve() { state.approve(this); }
        void reject()  { state.reject(this);  }

        // [PackagePrivate] Only state classes in this file call setState().
        // Making it public would let external callers bypass all transition rules.
        void setState(DocumentState s) { this.state = s; }
    }

    // ── [ConcreteState 1] DRAFT ────────────────────────────────────────────────
    static class DraftState implements DocumentState {

        @Override public String getStatus() { return "DRAFT"; }

        // [Transition] DRAFT → IN_REVIEW: setState() before the print so that if
        // doc.getTitle() or any other method were called afterwards, the state is
        // already correct. Either order is acceptable; choose one and be consistent.
        @Override
        public void submit(Document doc) {
            doc.setState(new ReviewState());
            System.out.printf("[Draft] \"%s\" submitted for review.%n", doc.getTitle());
        }

        // [ErrorBranch] No transition — state stays DRAFT; message explains why
        @Override
        public void approve(Document doc) {
            System.out.printf("[Draft] Cannot approve \"%s\" — submit for review first.%n",
                doc.getTitle());
        }

        @Override
        public void reject(Document doc) {
            System.out.printf("[Draft] Cannot reject \"%s\" — submit for review first.%n",
                doc.getTitle());
        }
    }

    // ── [ConcreteState 2] IN_REVIEW ───────────────────────────────────────────
    static class ReviewState implements DocumentState {

        @Override public String getStatus() { return "IN_REVIEW"; }

        // [NoTransition] Already in review — print and stay
        @Override
        public void submit(Document doc) {
            System.out.printf("[Review] \"%s\" is already under review.%n", doc.getTitle());
        }

        @Override
        public void approve(Document doc) {
            doc.setState(new ApprovedState());
            System.out.printf("[Review] \"%s\" approved.%n", doc.getTitle());
        }

        @Override
        public void reject(Document doc) {
            doc.setState(new RejectedState());
            System.out.printf("[Review] \"%s\" rejected.%n", doc.getTitle());
        }
    }

    // ── [ConcreteState 3] APPROVED — terminal ─────────────────────────────────
    // [TerminalState] No setState() call anywhere in this class.
    // All actions are "guard" methods: they print an error and do nothing else.
    // This is MANDATORY — the interface requires all four methods, even in a terminal state.
    static class ApprovedState implements DocumentState {

        @Override public String getStatus() { return "APPROVED"; }

        // [Guard] No transition allowed from APPROVED
        @Override
        public void submit(Document doc) {
            System.out.printf("[Approved] \"%s\" cannot be re-submitted.%n", doc.getTitle());
        }

        @Override
        public void approve(Document doc) {
            System.out.printf("[Approved] \"%s\" is already approved.%n", doc.getTitle());
        }

        @Override
        public void reject(Document doc) {
            System.out.printf("[Approved] \"%s\" cannot be rejected after approval.%n",
                doc.getTitle());
        }
    }

    // ── [ConcreteState 4] REJECTED — re-entry ─────────────────────────────────
    // [ReEntryState] submit() is valid: REJECTED → IN_REVIEW lets the author revise
    // and resubmit. This is not a terminal state — the lifecycle can continue.
    static class RejectedState implements DocumentState {

        @Override public String getStatus() { return "REJECTED"; }

        // [ReEntry] REJECTED → IN_REVIEW: rejection is recoverable via re-submission
        @Override
        public void submit(Document doc) {
            doc.setState(new ReviewState());
            System.out.printf("[Rejected] \"%s\" re-submitted for review.%n", doc.getTitle());
        }

        @Override
        public void approve(Document doc) {
            System.out.printf("[Rejected] \"%s\" cannot be approved — re-submit first.%n",
                doc.getTitle());
        }

        @Override
        public void reject(Document doc) {
            System.out.printf("[Rejected] \"%s\" is already rejected.%n", doc.getTitle());
        }
    }

    // ── Reference main() ──────────────────────────────────────────────────────
    public static void main(String[] args) {

        // ── Test 1: New document starts in DRAFT ──────────────────────────────
        Document doc = new Document("Architecture Proposal");
        System.out.println("Test 1 — initial status: "
            + ("DRAFT".equals(doc.getStatus()) ? "PASSED" : "FAILED (got: " + doc.getStatus() + ")"));

        // ── Test 2: submit() moves DRAFT → IN_REVIEW ──────────────────────────
        Document doc2 = new Document("API Design");
        doc2.submit();
        System.out.println("Test 2 — after submit: "
            + ("IN_REVIEW".equals(doc2.getStatus()) ? "PASSED" : "FAILED (got: " + doc2.getStatus() + ")"));

        // ── Test 3: approve() from DRAFT prints error, stays DRAFT ────────────
        Document doc3 = new Document("Budget Report");
        doc3.approve();
        System.out.println("Test 3 — approve from DRAFT stays DRAFT: "
            + ("DRAFT".equals(doc3.getStatus()) ? "PASSED" : "FAILED (got: " + doc3.getStatus() + ")"));

        // ── Test 4: approve() moves IN_REVIEW → APPROVED ─────────────────────
        Document doc4 = new Document("Security Policy");
        doc4.submit();
        doc4.approve();
        System.out.println("Test 4 — after approve: "
            + ("APPROVED".equals(doc4.getStatus()) ? "PASSED" : "FAILED (got: " + doc4.getStatus() + ")"));

        // ── Test 5: reject() moves IN_REVIEW → REJECTED ───────────────────────
        Document doc5 = new Document("Hiring Plan");
        doc5.submit();
        doc5.reject();
        System.out.println("Test 5 — after reject: "
            + ("REJECTED".equals(doc5.getStatus()) ? "PASSED" : "FAILED (got: " + doc5.getStatus() + ")"));

        // ── Test 6: APPROVED is terminal — all actions print errors ───────────
        Document doc6 = new Document("Privacy Policy");
        doc6.submit();
        doc6.approve();
        doc6.submit();    // [Approved] cannot be re-submitted
        doc6.approve();   // [Approved] already approved
        doc6.reject();    // [Approved] cannot be rejected after approval
        System.out.println("Test 6 — APPROVED is terminal: "
            + ("APPROVED".equals(doc6.getStatus()) ? "PASSED" : "FAILED (got: " + doc6.getStatus() + ")"));

        // ── Test 7: REJECTED → submit() re-enters review cycle ───────────────
        Document doc7 = new Document("Refund Policy");
        doc7.submit();
        doc7.reject();
        doc7.submit();
        System.out.println("Test 7 — re-submitted from REJECTED: "
            + ("IN_REVIEW".equals(doc7.getStatus()) ? "PASSED" : "FAILED (got: " + doc7.getStatus() + ")"));

        // ── Test 8: Full happy path DRAFT → IN_REVIEW → APPROVED ─────────────
        Document doc8 = new Document("Employee Handbook");
        System.out.println("Test 8 — full happy path:");
        System.out.println("  " + doc8.getStatus());   // DRAFT
        doc8.submit();
        System.out.println("  " + doc8.getStatus());   // IN_REVIEW
        doc8.approve();
        System.out.println("  " + doc8.getStatus());   // APPROVED
        System.out.println("Test 8 — APPROVED at end: "
            + ("APPROVED".equals(doc8.getStatus()) ? "PASSED" : "FAILED (got: " + doc8.getStatus() + ")"));

        // ── Test 9: Full reject-resubmit-approve path ─────────────────────────
        Document doc9 = new Document("Travel Policy");
        doc9.submit();   // DRAFT → IN_REVIEW
        doc9.reject();   // IN_REVIEW → REJECTED
        doc9.approve();  // error: cannot approve rejected doc
        doc9.submit();   // REJECTED → IN_REVIEW (re-entry)
        doc9.approve();  // IN_REVIEW → APPROVED
        System.out.println("Test 9 — reject → resubmit → approve: "
            + ("APPROVED".equals(doc9.getStatus()) ? "PASSED" : "FAILED (got: " + doc9.getStatus() + ")"));

        // ── Test 10: Two Document instances are fully independent ─────────────
        // The most common mistake: using static state fields or returning the same
        // state singleton instance from setState(). This causes one document's
        // transition to corrupt another document's state.
        //
        // DraftState, ReviewState etc. must be created fresh per transition
        // (new ReviewState()) — NOT stored as static fields or returned as singletons.
        System.out.println("\n── Test 10: Independent Document instances — no shared state ──");
        Document alpha = new Document("Alpha Doc");
        Document beta  = new Document("Beta Doc");

        alpha.submit();   // alpha → IN_REVIEW; beta must remain DRAFT

        System.out.println("Test 10a — alpha after submit: "
            + ("IN_REVIEW".equals(alpha.getStatus()) ? "PASSED"
               : "FAILED (got: " + alpha.getStatus() + ")"));
        System.out.println("Test 10b — beta unaffected (still DRAFT): "
            + ("DRAFT".equals(beta.getStatus()) ? "PASSED"
               : "FAILED (got: " + beta.getStatus() + " — state objects may be shared)"));

        beta.submit();
        beta.approve();   // beta → APPROVED; alpha must remain IN_REVIEW
        System.out.println("Test 10c — beta after approve: "
            + ("APPROVED".equals(beta.getStatus()) ? "PASSED"
               : "FAILED (got: " + beta.getStatus() + ")"));
        System.out.println("Test 10d — alpha unaffected (still IN_REVIEW): "
            + ("IN_REVIEW".equals(alpha.getStatus()) ? "PASSED"
               : "FAILED (got: " + alpha.getStatus() + " — state objects may be shared)"));
    }
}
