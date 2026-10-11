package com.ramkumar.lld.javareadiness.cheatsheet.practice;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Practice Exercise — Phase 4 Java Readiness for LLD (4.0 Cheat Sheet: collections + LLD patterns + idioms + enums)
 * Scenario B: Library Loan Desk
 *
 * ═══════════════════════════════════════════════════════════════════════
 * PROBLEM STATEMENT
 * ═══════════════════════════════════════════════════════════════════════
 *
 * A library lends books to members. Each loan has a due day, moves through a
 * fixed life cycle, and accrues a late fine that depends on the member's tier.
 * Build the in-memory loan desk. "Days" are plain ints (day 0, day 1, ...).
 *
 * ── ENUM: LoanStatus ───────────────────────────────────────────────────
 *   Constants, in THIS order: ACTIVE, OVERDUE, RETURNED, LOST
 *   Allowed transitions:
 *       ACTIVE   → OVERDUE, RETURNED, LOST
 *       OVERDUE  → RETURNED, LOST
 *       RETURNED → (none)      LOST → (none)
 *   1. canTransitionTo(LoanStatus next) → boolean
 *   2. isTerminal() → boolean   (true for RETURNED and LOST only)
 *
 * ── ENUM: MembershipTier ───────────────────────────────────────────────
 *   Constants, in THIS order, each with (maxLoans, loanDays, finePerDay):
 *       BASIC(3, 14, 0.50)   STUDENT(5, 28, 0.10)   PREMIUM(10, 21, 0.25)
 *   1. All three data fields are private final; getters getMaxLoans(),
 *      getLoanDays(), getFinePerDay().
 *   2. fineFor(int daysLate) → double: daysLate <= 0 gives 0.0, otherwise
 *      daysLate * finePerDay.
 *
 * ── RECORD: Book ───────────────────────────────────────────────────────
 *   Components: String isbn, String title, int year
 *   Validation in a compact constructor, in this order:
 *     isbn null/blank  → IllegalArgumentException("isbn must not be blank")
 *     title null/blank → IllegalArgumentException("title must not be blank")
 *     year < 1450      → IllegalArgumentException("year must be >= 1450")
 *
 * ── CLASS: Loan ────────────────────────────────────────────────────────
 *   Fields:
 *     1. String         id          (final)
 *     2. String         memberId    (final)
 *     3. Book           book        (final)
 *     4. MembershipTier tier        (final)
 *     5. int            dueDay      (final)
 *     6. LoanStatus     status      (mutable — the ONLY mutable field)
 *   Constructor:
 *     1. Loan(String id, String memberId, Book book, MembershipTier tier, int dueDay)
 *        copies the five arguments; status starts as ACTIVE. No validation here
 *        (LoanDesk validates before constructing).
 *   Methods:
 *     1. getters for all six fields (getId, getMemberId, getBook, getTier,
 *        getDueDay, getStatus)
 *     2. daysLate(int today) → int: today - dueDay, never negative
 *     3. setStatus(LoanStatus) — package-private (NOT public)
 *     4. toString() → "<id>[<memberId>, <book title>, <status>, due=<dueDay>]"
 *        e.g.  LN-001[M1, Dune, ACTIVE, due=14]
 *
 * ── CLASS: LoanDesk (the service + repository) ─────────────────────────
 *   State: a LinkedHashMap<String, Loan> keyed by loan id (creation order is
 *   preserved) and an int id counter that belongs to each desk instance.
 *
 *   1. checkout(String memberId, Book book, MembershipTier tier, int today) → Loan
 *        memberId null/blank → IllegalArgumentException("memberId must not be blank")
 *        book null           → IllegalArgumentException("book must not be null")
 *        tier null           → IllegalArgumentException("tier must not be null")
 *        today < 0           → IllegalArgumentException("today must be >= 0")
 *        Then state checks, in this order. "Open" = status is not terminal.
 *        book's isbn is in any open loan → IllegalStateException("Book already on loan: <isbn>")
 *        member's open-loan count >= tier.getMaxLoans()
 *                                        → IllegalStateException("Loan limit reached for member: <memberId>")
 *        On success: id = "LN-001", "LN-002", ... (3-digit zero padded), dueDay =
 *        today + tier.getLoanDays(), store the loan, print
 *        "[LoanDesk] checkout <loan>" and return it.
 *        A rejected checkout must NOT consume an id.
 *   2. findById(String id) → Optional<Loan>
 *   3. findAll() → List<Loan>   immutable snapshot, creation order
 *   4. transition(String id, LoanStatus next) → void
 *        unknown id → NoSuchElementException("Loan not found: <id>")
 *        not allowed → IllegalStateException("Cannot move from <current> to <next>")
 *        print "[LoanDesk] <id> -> <next>"
 *   5. markOverdue(int today) → int: every ACTIVE loan with daysLate(today) > 0
 *        becomes OVERDUE (reuse transition); returns how many changed.
 *   6. returnBook(String id, int today) → double
 *        Moves the loan to RETURNED (illegal/unknown ids fail exactly as in
 *        transition), then returns tier.fineFor(daysLate(today)).
 *        print "[LoanDesk] returned <id> fine=<fine, 2 decimals>"
 *   7. countByStatus() → Map<LoanStatus, Long>: every status appears, zero if none,
 *        iteration order = declaration order.
 *   8. overdueReport(int today) → List<Loan>: loans whose status is OVERDUE, most
 *        days late first; ties broken by id ascending.
 *   9. nextDue() → Optional<Loan>: among ACTIVE loans the one with the smallest
 *        dueDay (ties: smaller id); empty if there are none.
 *
 * ── DESIGN CONSTRAINTS ─────────────────────────────────────────────────
 *   - Transition rules live in LoanStatus, not in LoanDesk (no if/else chain
 *     over statuses inside the desk).
 *   - Tier numbers live in MembershipTier constants, not in LoanDesk.
 *   - The id counter is per-instance, not static.
 *   - findAll() must be immutable (add() throws UnsupportedOperationException).
 *   - findById and nextDue never return null.
 *   - No isPresent()+get(); use map/orElseThrow.
 *   - countByStatus uses EnumMap; the EnumSet/EnumMap classes are already imported.
 *   - overdueReport and nextDue use Comparator chaining, not hand-written compare().
 *
 * ═══════════════════════════════════════════════════════════════════════
 */
public class LoanDeskPractice {

    // =========================================================================
    // ENUM — LoanStatus
    // =========================================================================
    enum LoanStatus {

        // ── TODO 1: Declare the four constants in this order: ACTIVE, OVERDUE, RETURNED, LOST
        //    (no fields needed)
        ACTIVE,  OVERDUE, RETURNED, LOST;

        // ── TODO 2: canTransitionTo(LoanStatus next) → boolean
        //    ACTIVE   → allowed next: OVERDUE, RETURNED, LOST
        //    OVERDUE  → allowed next: RETURNED, LOST
        //    RETURNED → nothing allowed
        //    LOST     → nothing allowed
        //    The rules must be held by the enum itself, one rule per constant.
        //    A transition to the same status is never allowed.
        boolean canTransitionTo(LoanStatus next){
            return switch (this) {
                case ACTIVE ->  next == OVERDUE || next == RETURNED || next == LOST ;
                case OVERDUE ->  next == RETURNED || next == LOST ;
                case RETURNED, LOST ->  false;
            };
        }

        // ── TODO 3: isTerminal() → boolean
        //    true for RETURNED and LOST, false otherwise
        //    Must not repeat the transition table — derive it from the rules above.
        boolean isTerminal(){
            return (this == RETURNED || this == LOST);
        }
    }

    // =========================================================================
    // ENUM — MembershipTier
    // =========================================================================
    enum MembershipTier {

        // ── TODO 4: Constants, fields, constructor, getters
        //    Constants in this order, each created with (maxLoans, loanDays, finePerDay):
        //        BASIC   (3, 14, 0.50)
        //        STUDENT (5, 28, 0.10)
        //        PREMIUM (10, 21, 0.25)
        //    Fields (all private final): int maxLoans, int loanDays, double finePerDay
        //    Constructor assigns all three.
        //    Getters: getMaxLoans(), getLoanDays(), getFinePerDay()
        BASIC (3, 14, 0.50),
        STUDENT(5, 28, 0.10),
        PREMIUM(10, 21, 0.25);

        private final int maxLoans;
        private final int loanDays;
        private final double finePerDay;


        MembershipTier(int maxLoans, int loanDays, double finePerDay){
            this.maxLoans = maxLoans;
            this.loanDays = loanDays;
            this.finePerDay = finePerDay;
        }

        public int getMaxLoans() { return maxLoans; }
        public int getLoanDays() { return loanDays; }
        public double getFinePerDay() { return finePerDay;}
        // ── TODO 5: fineFor(int daysLate) → double
        //    daysLate <= 0 → return 0.0
        //    otherwise     → return daysLate * finePerDay
        double fineFor(int daysLate){
            if(daysLate<=0){
                return 0.0;
            }
            return daysLate * finePerDay;
        }
    }

    // =========================================================================
    // RECORD — Book
    // =========================================================================

    // ── TODO 6: Declare record Book (nested in LoanDeskPractice) with components
    //    String isbn, String title, int year
    //    Validate in a compact constructor, in this order:
    //      isbn null or blank  → throw new IllegalArgumentException("isbn must not be blank")
    //      title null or blank → throw new IllegalArgumentException("title must not be blank")
    //      year < 1450         → throw new IllegalArgumentException("year must be >= 1450")
    record Book(String isbn, String title, int year) {
        Book {
            if(isbn == null || isbn.isBlank())
                throw new IllegalArgumentException("isbn must not be blank");
            if(title == null || title.isBlank())
                throw new IllegalArgumentException("title must not be blank");
            if(year < 1450)
                throw new IllegalArgumentException("year must be >= 1450");
        }
    }
    // =========================================================================
    // CLASS — Loan
    // =========================================================================
    static final class Loan {

        // ── TODO 7: Declare 6 fields in Loan
        //    String         id         final
        //    String         memberId   final
        //    Book           book       final
        //    MembershipTier tier       final
        //    int            dueDay     final
        //    LoanStatus     status     NOT final (the only mutable field)
        private final String id;
        private final String memberId;
        private final Book book;
        private final MembershipTier tier;
        private final int dueDay;
        private LoanStatus status;


        // ── TODO 8: Constructor Loan(String id, String memberId, Book book, MembershipTier tier, int dueDay)
        //    Package-private. Copies the five arguments. No validation.
        //    status starts as LoanStatus.ACTIVE.
        Loan(String id, String memberId, Book book, MembershipTier tier, int dueDay){
            this.id = id;
            this.memberId = memberId;
            this.book = book;
            this.tier = tier;
            this.dueDay = dueDay;
            this.status = LoanStatus.ACTIVE;
        }

        // ── TODO 9: Methods on Loan
        //    getId(), getMemberId(), getBook(), getTier(), getDueDay(), getStatus() — plain getters
        //    daysLate(int today) → int: today - dueDay, but never below 0
        //    setStatus(LoanStatus s): package-private (no modifier), assigns status
        //    toString(): String.format of "%s[%s, %s, %s, due=%d]" using
        //        id, memberId, book title, status, dueDay
        //        e.g.  LN-001[M1, Dune, ACTIVE, due=14]
        public String  getId() { return  id;}
        public int  daysLate(int today) {
            return Math.max((today - dueDay), 0);
        }

        public int getDueDay() { return dueDay;}
        public String getMemberId() { return memberId;}
        public Book getBook() { return book; }
        public MembershipTier getTier() { return tier; }
        public LoanStatus getStatus() { return status; }

        public void setStatus(LoanStatus s) { this.status = s; }
        public String toString()
        { return String.format("%s[%s, %s, %s, due=%d]", id, memberId, book.title(), status, dueDay);}
    }

    // =========================================================================
    // CLASS — LoanDesk
    // =========================================================================
    static final class LoanDesk {

        // ── TODO 10: Declare the desk's state
        //    A Map<String, Loan> store, final, backed by an insertion-ordered map
        //    An int counter starting at 0 — an instance field, NOT static
        private final LinkedHashMap<String, Loan> store = new LinkedHashMap<>();
        private int counter = 0;

        // ── TODO 11: checkout(String memberId, Book book, MembershipTier tier, int today) → Loan
        //    Argument checks in this order (all IllegalArgumentException):
        //      memberId null/blank → "memberId must not be blank"
        //      book == null        → "book must not be null"
        //      tier == null        → "tier must not be null"
        //      today < 0           → "today must be >= 0"
        //    State checks in this order (both IllegalStateException). "Open loan" = status is not terminal:
        //      book's isbn matches an open loan  → "Book already on loan: <isbn>"
        //      member already has >= tier.getMaxLoans() open loans
        //                                        → "Loan limit reached for member: <memberId>"
        //    Success:
        //      id     = String.format("LN-%03d", ++counter)   — increment ONLY after every check passed
        //      dueDay = today + tier.getLoanDays()
        //      store the new Loan keyed by id
        //      print:  System.out.printf("[LoanDesk] checkout %s%n", loan)
        //      return the loan
        public Loan checkout(String memberId, Book book, MembershipTier tier, int today) {
            if(memberId == null || memberId.isBlank()) throw new IllegalArgumentException("memberId must not be blank");
            if(book == null) throw new IllegalArgumentException("book must not be null");
            if(tier == null) throw new IllegalArgumentException("tier must not be null");
            if(today < 0) throw new IllegalArgumentException("today must be >= 0");

            for(Loan loan : store.sequencedValues()) {
                if(loan.getStatus() == LoanStatus.ACTIVE) {
                    if(loan.getBook().isbn().equals(book.isbn())) {
                        throw new IllegalStateException("Book already on loan: " + book.isbn());
                    }
                }
            }
            String id = String.format("LN-%03d" , ++counter);
            int dueDay =  today +  tier.getLoanDays();
            Loan loan = new Loan(id, memberId, book, tier, dueDay);
            store.put(id, loan);
            return loan;
        }

        // ── TODO 12: findById(String id) → Optional<Loan>
        //            findAll() → List<Loan>
        //    findById: never returns null; empty Optional if the id is unknown
        //    findAll:  immutable snapshot of all loans in creation order;
        //              calling add() on the result throws UnsupportedOperationException
        public Optional<Loan> findById(String id){
            return Optional.ofNullable(store.get(id));
        }

        public List<Loan> findAll() {
            return List.copyOf(store.values());
        }


        // ── TODO 13: transition(String id, LoanStatus next) → void
        //    Unknown id → throw new NoSuchElementException("Loan not found: " + id)
        //    next not allowed from the loan's current status
        //        → throw new IllegalStateException("Cannot move from " + current + " to " + next)
        //    Use the enum's own rule — no per-status if/else here.
        //    On success set the status and print:
        //        System.out.printf("[LoanDesk] %s -> %s%n", id, next)
        public void transition(String id, LoanStatus next){
            if(store.containsKey(id)) throw new NoSuchElementException("Loan not found: " + id);

            Loan loan = store.get(id);
            if(!loan.getStatus().canTransitionTo(next))
            {
                throw new IllegalStateException("Cannot move from "  + loan.getStatus() + " to " + next);
            }

            loan.setStatus(next);
            System.out.printf("[LoanDesk] %s -> %s%n", id, next);
        }

        // ── TODO 14: markOverdue(int today) → int
        //    For every ACTIVE loan whose daysLate(today) > 0, move it to OVERDUE
        //    via transition(). Return the number of loans moved.
        //    Take care not to modify the map's contents structure while iterating
        //    over it (changing a loan's status is fine; adding/removing entries is not).
        public int markOverdue(int today) {
            int overDue = 0;
            for(Loan loan: store.values()){
                if(loan.getStatus() == LoanStatus.ACTIVE  && loan.daysLate(today) > 0) {
                    transition(loan.getId(), LoanStatus.OVERDUE);
                    overDue += 1;
                }
            }
            return overDue;
        }

        // ── TODO 15: returnBook(String id, int today) → double
        //    First move the loan to RETURNED via transition(), so unknown ids and
        //    illegal moves fail exactly as in TODO 13 (message "Cannot move from RETURNED to RETURNED"
        //    when returning twice).
        //    Then fine = loan's tier.fineFor(loan.daysLate(today))
        //    Print:  System.out.printf("[LoanDesk] returned %s fine=%.2f%n", id, fine)
        //    Return the fine.
        public double returnBook(String id, int today){
            if(!store.containsKey(id)) throw new NoSuchElementException(String.format("Loan Id %s doesn't exist%n", id ));
            Loan loan =  store.get(id);
            transition(loan.getId(), LoanStatus.RETURNED);

            double fine = loan.getTier().fineFor(loan.daysLate(today));
            System.out.printf("[LoanDesk] returned %s fine=%.2f%n",id, fine);
            return fine;
        }

        // ── TODO 16: countByStatus() → Map<LoanStatus, Long>
        //    Every LoanStatus constant must appear as a key (0 if no loans have it).
        //    Iteration order must be declaration order of LoanStatus.
        //    Use a map type built for enum keys.
        public Map<LoanStatus, Long> countByStatus() {
            Map<LoanStatus, Long> statusCounts = new HashMap<>();
            for(Loan loan: store.values()) {
                if(statusCounts.containsKey(loan.getStatus())){
                    statusCounts.compute(loan.getStatus(), (k, value) -> value + 1);
                } else {
                    statusCounts.put(loan.getStatus(), 1L);
                }
            }
            return Map.copyOf(statusCounts);
        }

        // ── TODO 17: overdueReport(int today) → List<Loan>
        //    Only loans whose status is OVERDUE.
        //    Order: most days late (daysLate(today)) first; ties by id ascending.
        //    Build the ordering with Comparator factory/chaining methods.
        //    Result must be an immutable list.
        List<Loan> overdueReport(int today){
            return store.values().stream().filter(loan ->  loan.getStatus() == LoanStatus.OVERDUE)
                    .sorted(Comparator.comparing(loan -> loan.daysLate(today))).toList();
        }

        // ── TODO 18: nextDue() → Optional<Loan>
        //    Among ACTIVE loans, the one with the smallest dueDay; ties → smaller id.
        //    No ACTIVE loans → empty Optional. Never return null.
        Optional<Loan> nextDue() {
            Loan smallestLoan = null;
            int dueDay = Integer.MAX_VALUE;

            for(Loan loan: store.sequencedValues()) {
                if(loan.getStatus() == LoanStatus.ACTIVE && dueDay < loan.getDueDay()) {
                    smallestLoan = loan;
                    dueDay = loan.getDueDay();
                }
            }

            return Optional.ofNullable(smallestLoan);
        }
    }

    // DO NOT MODIFY
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: LoanStatus rules (uncomment after TODO 3) ───────────────
         System.out.println(LoanStatus.ACTIVE.canTransitionTo(LoanStatus.OVERDUE));    // expected: true
         System.out.println(LoanStatus.OVERDUE.canTransitionTo(LoanStatus.ACTIVE));    // expected: false
         System.out.println(LoanStatus.RETURNED.canTransitionTo(LoanStatus.LOST));     // expected: false
         System.out.println(LoanStatus.ACTIVE.canTransitionTo(LoanStatus.ACTIVE));     // expected: false
         System.out.println(LoanStatus.LOST.isTerminal() + " " + LoanStatus.OVERDUE.isTerminal());  // expected: true false
         System.out.println(java.util.Arrays.toString(LoanStatus.values()));           // expected: [ACTIVE, OVERDUE, RETURNED, LOST]

        // ── Test 2: MembershipTier data + fines (uncomment after TODO 5) ─────
         System.out.println(MembershipTier.BASIC.getMaxLoans() + " " + MembershipTier.STUDENT.getLoanDays());  // expected: 3 28
         System.out.println(String.format("%.2f", MembershipTier.BASIC.fineFor(6)));     // expected: 3.00
         System.out.println(String.format("%.2f", MembershipTier.STUDENT.fineFor(10)));  // expected: 1.00
         System.out.println(String.format("%.2f", MembershipTier.PREMIUM.fineFor(-3)));  // expected: 0.00

        // ── Test 3: Book record validation (uncomment after TODO 6) ──────────
         Book dune = new Book("111", "Dune", 1965);
         System.out.println(dune.title() + " " + dune.equals(new Book("111", "Dune", 1965)));  // expected: Dune true
         try { new Book(" ", "X", 2000); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }  // expected: isbn must not be blank
         try { new Book("1", null, 2000); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); } // expected: title must not be blank
         try { new Book("1", "X", 1200); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }  // expected: year must be >= 1450

        // ── Test 4: checkout, ids, due day, toString (uncomment after TODO 11) ─
         LoanDesk desk4 = new LoanDesk();
         Loan a = desk4.checkout("M1", new Book("111", "Dune", 1965), MembershipTier.BASIC, 0);
         Loan b = desk4.checkout("M2", new Book("222", "Emma", 1815), MembershipTier.PREMIUM, 3);
         System.out.println(a);                          // expected: LN-001[M1, Dune, ACTIVE, due=14]
         System.out.println(b.getId() + " " + b.getDueDay());  // expected: LN-002 24
         System.out.println(new LoanDesk().checkout("M9", new Book("9", "Z", 2000), MembershipTier.STUDENT, 1).getId());  // expected: LN-001 (counter is per instance)

        // ── Test 5: checkout validation; rejected checkout consumes no id (uncomment after TODO 11) ─
         LoanDesk desk5 = new LoanDesk();
         Book dune5 = new Book("111", "Dune", 1965);
         desk5.checkout("M1", dune5, MembershipTier.BASIC, 0);
         try { desk5.checkout("M2", dune5, MembershipTier.BASIC, 0); } catch (IllegalStateException e) { System.out.println(e.getMessage()); }  // expected: Book already on loan: 111
         try { desk5.checkout("", new Book("5", "A", 2000), MembershipTier.BASIC, 0); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }  // expected: memberId must not be blank
         try { desk5.checkout("M2", null, MembershipTier.BASIC, 0); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }  // expected: book must not be null
         try { desk5.checkout("M2", new Book("5", "A", 2000), null, 0); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }  // expected: tier must not be null
         try { desk5.checkout("M2", new Book("5", "A", 2000), MembershipTier.BASIC, -1); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }  // expected: today must be >= 0
         System.out.println(desk5.checkout("M2", new Book("5", "A", 2000), MembershipTier.BASIC, 0).getId());  // expected: LN-002

        // ── Test 6: member loan limit counts only open loans (uncomment after TODO 13) ─
         LoanDesk desk6 = new LoanDesk();
         desk6.checkout("M1", new Book("1", "A", 2000), MembershipTier.BASIC, 0);
         desk6.checkout("M1", new Book("2", "B", 2000), MembershipTier.BASIC, 0);
         desk6.checkout("M1", new Book("3", "C", 2000), MembershipTier.BASIC, 0);
         try { desk6.checkout("M1", new Book("4", "D", 2000), MembershipTier.BASIC, 0); } catch (IllegalStateException e) { System.out.println(e.getMessage()); }  // expected: Loan limit reached for member: M1
         desk6.transition("LN-001", LoanStatus.RETURNED);
         System.out.println(desk6.checkout("M1", new Book("4", "D", 2000), MembershipTier.BASIC, 0).getId());  // expected: LN-004 (failed attempt used no id)

        // ── Test 7: find, transition errors, immutable findAll (uncomment after TODO 13) ─
         LoanDesk desk7 = new LoanDesk();
         desk7.checkout("M1", new Book("1", "A", 2000), MembershipTier.STUDENT, 0);
         System.out.println(desk7.findById("LN-001").map(Loan::getMemberId).orElse("none"));  // expected: M1
         System.out.println(desk7.findById("LN-777").map(Loan::getMemberId).orElse("none"));  // expected: none
         desk7.transition("LN-001", LoanStatus.RETURNED);
         try { desk7.transition("LN-001", LoanStatus.OVERDUE); } catch (IllegalStateException e) { System.out.println(e.getMessage()); }  // expected: Cannot move from RETURNED to OVERDUE
         try { desk7.transition("LN-999", LoanStatus.LOST); } catch (NoSuchElementException e) { System.out.println(e.getMessage()); }  // expected: Loan not found: LN-999
         try { desk7.findAll().add(null); } catch (UnsupportedOperationException e) { System.out.println("findAll is immutable"); }  // expected: findAll is immutable

        // ── Test 8: overdue pipeline, report ordering, EnumMap counts (uncomment after TODO 17) ─
         LoanDesk desk8 = new LoanDesk();
         desk8.checkout("M1", new Book("111", "Dune", 1965), MembershipTier.BASIC, 0);       // LN-001 due 14
         desk8.checkout("M2", new Book("222", "Emma", 1815), MembershipTier.STUDENT, 0);     // LN-002 due 28
         desk8.checkout("M3", new Book("333", "Ulysses", 1922), MembershipTier.BASIC, 2);    // LN-003 due 16
         desk8.checkout("M4", new Book("444", "Hamlet", 1603), MembershipTier.PREMIUM, 5);   // LN-004 due 26
         System.out.println(desk8.markOverdue(20));                       // expected: 2
         System.out.println(desk8.overdueReport(20).stream().map(Loan::getId).collect(Collectors.joining(", ")));  // expected: LN-001, LN-003
         System.out.println(desk8.countByStatus());                       // expected: {ACTIVE=2, OVERDUE=2, RETURNED=0, LOST=0}
         desk8.transition("LN-003", LoanStatus.LOST);
         System.out.println(desk8.countByStatus());                       // expected: {ACTIVE=2, OVERDUE=1, RETURNED=0, LOST=1}
         System.out.println(desk8.overdueReport(20).size());              // expected: 1

        // ── Test 9: returning with fines; re-lending a returned book (uncomment after TODO 15) ─
         LoanDesk desk9 = new LoanDesk();
         desk9.checkout("M1", new Book("111", "Dune", 1965), MembershipTier.BASIC, 0);       // LN-001 due 14
         desk9.checkout("M4", new Book("444", "Hamlet", 1603), MembershipTier.PREMIUM, 5);   // LN-002 due 26
         desk9.markOverdue(20);
         System.out.println(String.format("%.2f", desk9.returnBook("LN-001", 20)));  // expected: 3.00
         System.out.println(String.format("%.2f", desk9.returnBook("LN-002", 20)));  // expected: 0.00
         try { desk9.returnBook("LN-001", 21); } catch (IllegalStateException e) { System.out.println(e.getMessage()); }  // expected: Cannot move from RETURNED to RETURNED
         System.out.println(desk9.checkout("M5", new Book("111", "Dune", 1965), MembershipTier.BASIC, 21).getId());  // expected: LN-003

        // ── Test 10: nextDue with Optional (uncomment after TODO 18) ─────────
         LoanDesk desk10 = new LoanDesk();
         System.out.println(desk10.nextDue().isPresent());                // expected: false
         desk10.checkout("M1", new Book("1", "A", 2000), MembershipTier.BASIC, 0);     // due 14
         desk10.checkout("M2", new Book("2", "B", 2000), MembershipTier.STUDENT, 0);   // due 28
         desk10.checkout("M3", new Book("3", "C", 2000), MembershipTier.PREMIUM, 0);   // due 21
         System.out.println(desk10.nextDue().map(Loan::getId).orElse("none"));  // expected: LN-001
         desk10.transition("LN-001", LoanStatus.RETURNED);
         System.out.println(desk10.nextDue().map(Loan::getId).orElse("none"));  // expected: LN-003
    }
}

/*
 * ═══════════════════════════════════════════════════════════════════════
 * HINTS — read one level at a time; try for 10 minutes before each unlock
 * ═══════════════════════════════════════════════════════════════════════
 *
 * HINT 1 (Gentle)
 *   - An enum constant is an object. Rules about "what comes next" can live on
 *     the constant itself, and "terminal" is simply "has nowhere to go".
 *   - Tier numbers are data: put them on the constants, not in the desk.
 *   - A record's compact constructor runs before the fields are assigned — that
 *     is the place to validate.
 *   - Lookups that may find nothing should say so in their return type.
 *   - Do all the checks first; only then touch the counter and the map.
 *   - A report that must list every enum constant, even with zero, needs you to
 *     seed it first. A pipeline can sort with a chain of comparators.
 *
 * HINT 2 (Direct)
 *   - LoanStatus: a switch expression over `this`, or an EnumSet<LoanStatus> per
 *     constant held in a field. isTerminal() can call canTransitionTo for each value
 *     or test whether the allowed set is empty.
 *   - MembershipTier: constructor + three private final fields.
 *   - Store: new LinkedHashMap<>(); lookup with Optional.ofNullable(store.get(id)).
 *   - findAll: List.copyOf(store.values()).
 *   - transition: findById(id).orElseThrow(() -> new NoSuchElementException(...)).
 *   - Open loan count: store.values().stream().filter(l -> !l.getStatus().isTerminal()) ...
 *     .filter(...).count() — or anyMatch for the duplicate-isbn rule.
 *   - countByStatus: new EnumMap<>(LoanStatus.class), put 0L for every values(),
 *     then merge(status, 1L, Long::sum).
 *   - overdueReport: Comparator.comparingInt(...).reversed().thenComparing(Loan::getId).
 *     Note: a lambda inside comparingInt(...).reversed() loses its type; write the
 *     parameter type explicitly, e.g. (Loan l) -> l.daysLate(today).
 *   - nextDue: stream().filter(ACTIVE).min(Comparator.comparingInt(Loan::getDueDay).thenComparing(Loan::getId)).
 *   - markOverdue: collect the matching loans into a list first, then call transition on each.
 *
 * HINT 3 (Near-solution skeleton — bodies are yours)
 *   enum LoanStatus {
 *       ACTIVE, OVERDUE, RETURNED, LOST;
 *       boolean canTransitionTo(LoanStatus next) { return switch (this) { ... }; }
 *       boolean isTerminal()                      { ... }
 *   }
 *   enum MembershipTier {
 *       BASIC(3, 14, 0.50), STUDENT(5, 28, 0.10), PREMIUM(10, 21, 0.25);
 *       private final int maxLoans; private final int loanDays; private final double finePerDay;
 *       MembershipTier(int maxLoans, int loanDays, double finePerDay) { ... }
 *       int getMaxLoans() { ... }  int getLoanDays() { ... }  double getFinePerDay() { ... }
 *       double fineFor(int daysLate) { ... }
 *   }
 *   record Book(String isbn, String title, int year) { Book { ... } }
 *   static final class Loan {
 *       private final String id; ... private LoanStatus status;
 *       Loan(String id, String memberId, Book book, MembershipTier tier, int dueDay) { ... }
 *       int daysLate(int today) { ... }
 *       void setStatus(LoanStatus s) { ... }
 *   }
 *   static final class LoanDesk {
 *       private final Map<String, Loan> store = new LinkedHashMap<>();
 *       private int counter = 0;
 *       Loan checkout(String memberId, Book book, MembershipTier tier, int today) { ... }
 *       Optional<Loan> findById(String id) { ... }
 *       List<Loan> findAll() { ... }
 *       void transition(String id, LoanStatus next) { ... }
 *       int markOverdue(int today) { ... }
 *       double returnBook(String id, int today) { ... }
 *       Map<LoanStatus, Long> countByStatus() { ... }
 *       List<Loan> overdueReport(int today) { ... }
 *       Optional<Loan> nextDue() { ... }
 *   }
 * ═══════════════════════════════════════════════════════════════════════
 */
