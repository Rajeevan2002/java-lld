package com.ramkumar.lld.designpatterns.behavioral.state.practice;

import javax.print.Doc;

/**
 * Practice Exercise — State Pattern: Document Approval Workflow
 *
 * <p><b>Scenario B — Lifecycle state machine with terminal and re-entry states</b>
 *
 * <p>A content platform manages documents through an approval workflow. A document
 * starts as a DRAFT, gets submitted for review, and is either approved or rejected.
 * A rejected document can be re-submitted. Each state handles actions differently —
 * the {@code Document} context never inspects which state it is in.
 *
 * <p><b>Class hierarchy (do not change the names):</b>
 * <pre>
 *   DocumentState   [State interface]    ← TODO 1
 *   Document        [Context]            ← TODO 2–3
 *   DraftState      [ConcreteState 1]    ← TODO 4
 *   ReviewState     [ConcreteState 2]    ← TODO 5
 *   ApprovedState   [ConcreteState 3]    ← TODO 6
 *   RejectedState   [ConcreteState 4]    ← TODO 7
 * </pre>
 *
 * <p><b>DocumentState interface (TODO 1):</b>
 * <ul>
 *   <li>{@code void submit(Document doc)}</li>
 *   <li>{@code void approve(Document doc)}</li>
 *   <li>{@code void reject(Document doc)}</li>
 *   <li>{@code String getStatus()}</li>
 * </ul>
 *
 * <p><b>Document context (TODO 2–3):</b>
 * <ul>
 *   <li>Fields: {@code private DocumentState state}, {@code private final String title}</li>
 *   <li>Constructor: {@code Document(String title)} — initialises {@code state = new DraftState()}</li>
 *   <li>{@code getTitle() → String}, {@code getStatus() → String} (delegates to {@code state.getStatus()})</li>
 *   <li>{@code submit()}, {@code approve()}, {@code reject()} — each delegates to the equivalent
 *       state method, passing {@code this}</li>
 *   <li>{@code setState(DocumentState state)} — package-private setter; only state classes call it</li>
 * </ul>
 *
 * <p><b>DraftState (TODO 4):</b>
 * <ul>
 *   <li>{@code getStatus()}: returns {@code "DRAFT"}</li>
 *   <li>{@code submit()}: transitions to {@code ReviewState};
 *       prints {@code System.out.printf("[Draft] \"%s\" submitted for review.%n", doc.getTitle())}</li>
 *   <li>{@code approve()}: prints {@code System.out.printf("[Draft] Cannot approve \"%s\" — submit for review first.%n", doc.getTitle())}</li>
 *   <li>{@code reject()}: prints {@code System.out.printf("[Draft] Cannot reject \"%s\" — submit for review first.%n", doc.getTitle())}</li>
 * </ul>
 *
 * <p><b>ReviewState (TODO 5):</b>
 * <ul>
 *   <li>{@code getStatus()}: returns {@code "IN_REVIEW"}</li>
 *   <li>{@code submit()}: prints {@code System.out.printf("[Review] \"%s\" is already under review.%n", doc.getTitle())}</li>
 *   <li>{@code approve()}: transitions to {@code ApprovedState};
 *       prints {@code System.out.printf("[Review] \"%s\" approved.%n", doc.getTitle())}</li>
 *   <li>{@code reject()}: transitions to {@code RejectedState};
 *       prints {@code System.out.printf("[Review] \"%s\" rejected.%n", doc.getTitle())}</li>
 * </ul>
 *
 * <p><b>ApprovedState (TODO 6) — terminal state:</b>
 * <ul>
 *   <li>{@code getStatus()}: returns {@code "APPROVED"}</li>
 *   <li>{@code submit()}: prints {@code System.out.printf("[Approved] \"%s\" cannot be re-submitted.%n", doc.getTitle())}</li>
 *   <li>{@code approve()}: prints {@code System.out.printf("[Approved] \"%s\" is already approved.%n", doc.getTitle())}</li>
 *   <li>{@code reject()}: prints {@code System.out.printf("[Approved] \"%s\" cannot be rejected after approval.%n", doc.getTitle())}</li>
 * </ul>
 *
 * <p><b>RejectedState (TODO 7) — re-entry state:</b>
 * <ul>
 *   <li>{@code getStatus()}: returns {@code "REJECTED"}</li>
 *   <li>{@code submit()}: transitions to {@code ReviewState} (re-submission allowed);
 *       prints {@code System.out.printf("[Rejected] \"%s\" re-submitted for review.%n", doc.getTitle())}</li>
 *   <li>{@code approve()}: prints {@code System.out.printf("[Rejected] \"%s\" cannot be approved — re-submit first.%n", doc.getTitle())}</li>
 *   <li>{@code reject()}: prints {@code System.out.printf("[Rejected] \"%s\" is already rejected.%n", doc.getTitle())}</li>
 * </ul>
 *
 * <p><b>Design constraints:</b>
 * <ul>
 *   <li>{@code Document.state} must be {@code private} — nothing outside the context reads it.</li>
 *   <li>{@code setState()} must be package-private — only state classes in this file call it.</li>
 *   <li>No {@code instanceof}, no type-checking, no {@code if (state == X)} anywhere.</li>
 *   <li>Every method in every state class must be implemented — even terminal-state methods
 *       that print a rejection message and do nothing else.</li>
 *   <li>State transitions only through {@code doc.setState(new XxxState())} inside state classes.</li>
 * </ul>
 */
public class DocumentApprovalPractice {

    interface DocumentState {
        void submit(Document doc);
        void approve(Document doc);
        void reject(Document doc);
        String getStatus();

    }

    // ── Document (Context) ────────────────────────────────────────────────────

    static class Document {
        // student writes all fields, constructor, and methods here
        private DocumentState state;
        private final String title;

        public Document(String title){
            this.title = title;
            this.state = new DraftState();
        }

        public String getTitle() {
            return title;
        }

        public String getStatus() { return state.getStatus(); }
        public void submit() { state.submit(this); }
        public void approve() { state.approve(this); }
        public void reject(){ state.reject(this); }
        void setState(DocumentState s) { this.state = s;}

    }

    // ── TODO 4: DraftState implements DocumentState ────────────────────────────
    //    getStatus()  → "DRAFT"
    //    submit()     → doc.setState(new ReviewState())
    //                   System.out.printf("[Draft] \"%s\" submitted for review.%n", doc.getTitle())
    //    approve()    → System.out.printf("[Draft] Cannot approve \"%s\" — submit for review first.%n", doc.getTitle())
    //    reject()     → System.out.printf("[Draft] Cannot reject \"%s\" — submit for review first.%n", doc.getTitle())
    //
    //    NOTE: setState() BEFORE the print, or print BEFORE setState()? Either is fine —
    //    but be consistent. The tests check status AFTER the call, so transition first.

    static class DraftState implements DocumentState {
        @Override
        public String getStatus() {
            return "DRAFT";
        }

        @Override
        public void submit(Document doc) {
            doc.setState(new ReviewState());
            System.out.printf("[Draft] \"%s\" submitted for review.%n", doc.getTitle());
        }

        @Override
        public void approve(Document doc) {
            System.out.printf("[Draft] Cannot approve \"%s\" — submit for review first.%n", doc.getTitle());
        }

        @Override
        public void reject(Document doc) {
            System.out.printf("[Draft] Cannot reject \"%s\" — submit for review first.%n", doc.getTitle());
        }
    }

    static class ReviewState implements DocumentState {
        @Override
        public String getStatus() {
            return "IN_REVIEW";
        }

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

    // ── TODO 6: ApprovedState implements DocumentState — TERMINAL STATE ────────


    static class ApprovedState implements DocumentState {
        @Override
        public String getStatus() {
            return "APPROVED";
        }

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
            System.out.printf("[Approved] \"%s\" cannot be rejected after approval.%n", doc.getTitle());
        }
    }

    // ── TODO 7: RejectedState implements DocumentState — RE-ENTRY STATE ────────
    //    getStatus()  → "REJECTED"
    //    submit()     → doc.setState(new ReviewState())    ← re-submission is allowed
    //                   System.out.printf("[Rejected] \"%s\" re-submitted for review.%n", doc.getTitle())
    //    approve()    → System.out.printf("[Rejected] \"%s\" cannot be approved — re-submit first.%n", doc.getTitle())
    //    reject()     → System.out.printf("[Rejected] \"%s\" is already rejected.%n", doc.getTitle())

    static class RejectedState implements DocumentState {
        @Override
        public String getStatus() {
            return "REJECTED";
        }

        @Override
        public void submit(Document doc) {
            doc.setState(new ReviewState());
            System.out.printf("[Rejected] \"%s\" re-submitted for review.%n", doc.getTitle());
        }

        @Override
        public void approve(Document doc) {
            System.out.printf("[Rejected] \"%s\" cannot be approved — re-submit first.%n", doc.getTitle());
        }

        @Override
        public void reject(Document doc) {
            System.out.printf("[Rejected] \"%s\" is already rejected.%n", doc.getTitle());
        }
    }

    // ── DO NOT MODIFY — test harness ──────────────────────────────────────────
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: New document starts in DRAFT (TODO 2–3) ──────────────────────
         Document doc = new Document("Architecture Proposal");
         System.out.println("Test 1 — initial status: "
             + ("DRAFT".equals(doc.getStatus()) ? "PASSED" : "FAILED (got: " + doc.getStatus() + ")"));

        // ── Test 2: submit() moves DRAFT → IN_REVIEW (TODO 3–4) ─────────────────
         Document doc2 = new Document("API Design");
         doc2.submit();
         // expected print: [Draft] "API Design" submitted for review.
         System.out.println("Test 2 — after submit: "
             + ("IN_REVIEW".equals(doc2.getStatus()) ? "PASSED" : "FAILED (got: " + doc2.getStatus() + ")"));

        // ── Test 3: approve() from DRAFT prints error, stays DRAFT (TODO 4) ──────
         Document doc3 = new Document("Budget Report");
         doc3.approve();
         // expected print: [Draft] Cannot approve "Budget Report" — submit for review first.
         System.out.println("Test 3 — approve from DRAFT stays DRAFT: "
             + ("DRAFT".equals(doc3.getStatus()) ? "PASSED" : "FAILED (got: " + doc3.getStatus() + ")"));

        // ── Test 4: approve() moves IN_REVIEW → APPROVED (TODO 5) ───────────────
         Document doc4 = new Document("Security Policy");
         doc4.submit();
         doc4.approve();
         // expected prints:
         //   [Draft]  "Security Policy" submitted for review.
         //   [Review] "Security Policy" approved.
         System.out.println("Test 4 — after approve: "
             + ("APPROVED".equals(doc4.getStatus()) ? "PASSED" : "FAILED (got: " + doc4.getStatus() + ")"));

        // ── Test 5: reject() moves IN_REVIEW → REJECTED (TODO 5) ────────────────
         Document doc5 = new Document("Hiring Plan");
         doc5.submit();
         doc5.reject();
         // expected prints:
         //   [Draft]  "Hiring Plan" submitted for review.
         //   [Review] "Hiring Plan" rejected.
         System.out.println("Test 5 — after reject: "
             + ("REJECTED".equals(doc5.getStatus()) ? "PASSED" : "FAILED (got: " + doc5.getStatus() + ")"));

        // ── Test 6: APPROVED is a terminal state — all actions print errors (TODO 6) ──
         Document doc6 = new Document("Privacy Policy");
         doc6.submit();
         doc6.approve();
         String beforeSubmit = doc6.getStatus();
         doc6.submit();    // [Approved] "Privacy Policy" cannot be re-submitted.
         doc6.approve();   // [Approved] "Privacy Policy" is already approved.
         doc6.reject();    // [Approved] "Privacy Policy" cannot be rejected after approval.
         System.out.println("Test 6 — APPROVED is terminal: "
             + ("APPROVED".equals(doc6.getStatus()) ? "PASSED" : "FAILED (got: " + doc6.getStatus() + ")"));

        // ── Test 7: REJECTED → submit() re-enters review cycle (TODO 7) ─────────
         Document doc7 = new Document("Refund Policy");
         doc7.submit();
         doc7.reject();
         // expected: REJECTED
         doc7.submit();   // re-submission
         // expected print: [Rejected] "Refund Policy" re-submitted for review.
         System.out.println("Test 7 — re-submitted from REJECTED: "
             + ("IN_REVIEW".equals(doc7.getStatus()) ? "PASSED" : "FAILED (got: " + doc7.getStatus() + ")"));

        // ── Test 8: Full happy path DRAFT → IN_REVIEW → APPROVED (TODO 1–6) ─────
         Document doc8 = new Document("Employee Handbook");
         System.out.println("Test 8 — full happy path:");
         System.out.println("  " + doc8.getStatus());        // DRAFT
         doc8.submit();
         System.out.println("  " + doc8.getStatus());        // IN_REVIEW
         doc8.approve();
         System.out.println("  " + doc8.getStatus());        // APPROVED
         System.out.println("Test 8 — APPROVED at end: "
             + ("APPROVED".equals(doc8.getStatus()) ? "PASSED" : "FAILED (got: " + doc8.getStatus() + ")"));

        // ── Test 9: Full reject-resubmit-approve path (TODO 1–7) ─────────────────
         Document doc9 = new Document("Travel Policy");
         doc9.submit();   // DRAFT → IN_REVIEW
         doc9.reject();   // IN_REVIEW → REJECTED
         doc9.approve();  // [Rejected] "Travel Policy" cannot be approved — re-submit first.
         doc9.submit();   // REJECTED → IN_REVIEW (re-entry)
         doc9.approve();  // IN_REVIEW → APPROVED
         System.out.println("Test 9 — reject → resubmit → approve: "
             + ("APPROVED".equals(doc9.getStatus()) ? "PASSED" : "FAILED (got: " + doc9.getStatus() + ")"));
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HINTS — read only if stuck
    // ════════════════════════════════════════════════════════════════════════════

    // HINT 1 (Gentle):
    //   A document behaves differently in each phase of its lifecycle. Instead of
    //   putting if/else blocks in Document that check the current phase, create a
    //   separate class for each phase. Each class knows exactly what submit(),
    //   approve(), and reject() mean in that phase, and is responsible for moving
    //   the document to the next phase when appropriate.

    // HINT 2 (Direct):
    //   Use the State pattern.
    //   DocumentState is an interface with submit(), approve(), reject(), getStatus().
    //   Document holds a private DocumentState field, initialised to new DraftState().
    //   Document.submit() calls state.submit(this) — it never inspects the state itself.
    //   Each concrete state implements all four methods. Transitioning states is done
    //   by calling doc.setState(new NextState()) inside the state's method body.

    // HINT 3 (Near-solution skeleton — class outlines only, no method bodies):
    //
    //   interface DocumentState {
    //       void submit(Document doc);
    //       void approve(Document doc);
    //       void reject(Document doc);
    //       String getStatus();
    //   }
    //
    //   static class Document {
    //       private DocumentState state;
    //       private final String title;
    //       Document(String title) { this.title = title; this.state = new DraftState(); }
    //       String getTitle()  { return title; }
    //       String getStatus() { return state.getStatus(); }
    //       void submit()  { state.submit(this);  }
    //       void approve() { state.approve(this); }
    //       void reject()  { state.reject(this);  }
    //       void setState(DocumentState s) { this.state = s; }
    //   }
    //
    //   static class DraftState implements DocumentState {
    //       @Override public String getStatus() { return "DRAFT"; }
    //       @Override public void submit(Document doc) {
    //           doc.setState(new ReviewState());
    //           System.out.printf("[Draft] \"%s\" submitted for review.%n", doc.getTitle());
    //       }
    //       @Override public void approve(Document doc) { /* error message */ }
    //       @Override public void reject(Document doc)  { /* error message */ }
    //   }
    //   // ReviewState, ApprovedState, RejectedState follow the same structure
}
