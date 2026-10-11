package com.ramkumar.lld.javareadiness.cheatsheet.results;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Reference solution — Phase 4 Java Readiness | Scenario B: Library Loan Desk
 */
public class LoanDeskReference {

    // ── LoanStatus: the enum owns the transition rules ──────────────────────
    enum LoanStatus {
        ACTIVE, OVERDUE, RETURNED, LOST;

        boolean canTransitionTo(LoanStatus next) {
            // switch expression: exhaustive, no fall-through, one rule per constant
            return switch (this) {
                case ACTIVE -> next == OVERDUE || next == RETURNED || next == LOST;
                case OVERDUE -> next == RETURNED || next == LOST;
                case RETURNED, LOST -> false;
            };
        }

        // Derived from the rules above, so adding a state can never leave this stale.
        // "Terminal" == "there is no status I can move to".
        boolean isTerminal() {
            return Arrays.stream(values()).noneMatch(this::canTransitionTo);   // method reference
        }
    }

    // ── MembershipTier: data lives on the constant, not in the desk ─────────
    enum MembershipTier {
        BASIC(3, 14, 0.50),
        STUDENT(5, 28, 0.10),
        PREMIUM(10, 21, 0.25);

        private final int maxLoans;
        private final int loanDays;
        private final double finePerDay;

        MembershipTier(int maxLoans, int loanDays, double finePerDay) {
            this.maxLoans = maxLoans;
            this.loanDays = loanDays;
            this.finePerDay = finePerDay;
        }

        int getMaxLoans() { return maxLoans; }
        int getLoanDays() { return loanDays; }
        double getFinePerDay() { return finePerDay; }

        double fineFor(int daysLate) {
            return daysLate <= 0 ? 0.0 : daysLate * finePerDay;
        }
    }

    // ── Book: record, validated in the compact constructor ──────────────────
    record Book(String isbn, String title, int year) {
        Book {
            if (isbn == null || isbn.isBlank()) throw new IllegalArgumentException("isbn must not be blank");
            if (title == null || title.isBlank()) throw new IllegalArgumentException("title must not be blank");
            if (year < 1450) throw new IllegalArgumentException("year must be >= 1450");
        }
    }

    // ── Loan: only `status` is mutable, and only the desk may change it ────
    static final class Loan {
        private final String id;
        private final String memberId;
        private final Book book;
        private final MembershipTier tier;
        private final int dueDay;
        private LoanStatus status;

        Loan(String id, String memberId, Book book, MembershipTier tier, int dueDay) {
            this.id = id;
            this.memberId = memberId;
            this.book = book;
            this.tier = tier;
            this.dueDay = dueDay;
            this.status = LoanStatus.ACTIVE;
        }

        String getId() { return id; }
        String getMemberId() { return memberId; }
        Book getBook() { return book; }
        MembershipTier getTier() { return tier; }
        int getDueDay() { return dueDay; }
        LoanStatus getStatus() { return status; }

        int daysLate(int today) { return Math.max(0, today - dueDay); }   // clamp, never negative

        void setStatus(LoanStatus s) { this.status = s; }                  // package-private on purpose

        @Override
        public String toString() {
            return String.format("%s[%s, %s, %s, due=%d]", id, memberId, book.title(), status, dueDay);
        }
    }

    // ── LoanDesk: service + repository ──────────────────────────────────────
    static final class LoanDesk {
        private final Map<String, Loan> store = new LinkedHashMap<>();   // creation order
        private int counter = 0;                                          // per instance

        Loan checkout(String memberId, Book book, MembershipTier tier, int today) {
            // 1. argument checks (cheap, no store access)
            if (memberId == null || memberId.isBlank()) throw new IllegalArgumentException("memberId must not be blank");
            if (book == null) throw new IllegalArgumentException("book must not be null");
            if (tier == null) throw new IllegalArgumentException("tier must not be null");
            if (today < 0) throw new IllegalArgumentException("today must be >= 0");

            // 2. state checks. "Open" = NOT terminal, so an OVERDUE loan still holds the book.
            boolean bookOnLoan = store.values().stream()
                    .filter(l -> !l.getStatus().isTerminal())
                    .anyMatch(l -> l.getBook().isbn().equals(book.isbn()));
            if (bookOnLoan) throw new IllegalStateException("Book already on loan: " + book.isbn());

            long openForMember = store.values().stream()
                    .filter(l -> l.getMemberId().equals(memberId))
                    .filter(l -> !l.getStatus().isTerminal())
                    .count();
            if (openForMember >= tier.getMaxLoans()) {
                throw new IllegalStateException("Loan limit reached for member: " + memberId);
            }

            // 3. only now consume an id, so rejected checkouts leave no gaps
            String id = String.format("LN-%03d", ++counter);
            Loan loan = new Loan(id, memberId, book, tier, today + tier.getLoanDays());
            store.put(id, loan);
            System.out.printf("[LoanDesk] checkout %s%n", loan);
            return loan;
        }

        Optional<Loan> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        List<Loan> findAll() {
            return List.copyOf(store.values());                // immutable snapshot
        }

        void transition(String id, LoanStatus next) {
            // orElseThrow handles "missing" in one line; no containsKey + get double lookup
            Loan loan = findById(id).orElseThrow(() -> new NoSuchElementException("Loan not found: " + id));
            if (!loan.getStatus().canTransitionTo(next)) {
                throw new IllegalStateException("Cannot move from " + loan.getStatus() + " to " + next);
            }
            loan.setStatus(next);
            System.out.printf("[LoanDesk] %s -> %s%n", id, next);
        }

        int markOverdue(int today) {
            // snapshot first (.toList()), then mutate: never act on a live stream of the map
            List<Loan> late = store.values().stream()
                    .filter(l -> l.getStatus() == LoanStatus.ACTIVE)
                    .filter(l -> l.daysLate(today) > 0)
                    .toList();
            late.forEach(l -> transition(l.getId(), LoanStatus.OVERDUE));
            return late.size();
        }

        double returnBook(String id, int today) {
            transition(id, LoanStatus.RETURNED);               // single source of truth for all errors
            Loan loan = store.get(id);                         // safe: transition proved it exists
            double fine = loan.getTier().fineFor(loan.daysLate(today));
            System.out.printf("[LoanDesk] returned %s fine=%.2f%n", id, fine);
            return fine;
        }

        Map<LoanStatus, Long> countByStatus() {
            Map<LoanStatus, Long> counts = new EnumMap<>(LoanStatus.class);   // declaration order
            for (LoanStatus s : LoanStatus.values()) counts.put(s, 0L);       // zero-fill
            store.values().forEach(l -> counts.merge(l.getStatus(), 1L, Long::sum));
            return counts;
        }

        List<Loan> overdueReport(int today) {
            return store.values().stream()
                    .filter(l -> l.getStatus() == LoanStatus.OVERDUE)
                    // explicit (Loan l): .reversed() sits after the lambda, so Java can't infer its type
                    .sorted(Comparator.comparingInt((Loan l) -> l.daysLate(today)).reversed()
                            .thenComparing(Loan::getId))
                    .toList();
        }

        Optional<Loan> nextDue() {
            return store.values().stream()
                    .filter(l -> l.getStatus() == LoanStatus.ACTIVE)
                    .min(Comparator.comparingInt(Loan::getDueDay).thenComparing(Loan::getId));
        }
    }

    public static void main(String[] args) {
        System.out.println("── Test 1: LoanStatus rules");
        System.out.println(LoanStatus.ACTIVE.canTransitionTo(LoanStatus.OVERDUE));    // true
        System.out.println(LoanStatus.OVERDUE.canTransitionTo(LoanStatus.ACTIVE));    // false
        System.out.println(LoanStatus.RETURNED.canTransitionTo(LoanStatus.LOST));     // false
        System.out.println(LoanStatus.ACTIVE.canTransitionTo(LoanStatus.ACTIVE));     // false
        System.out.println(LoanStatus.LOST.isTerminal() + " " + LoanStatus.OVERDUE.isTerminal());  // true false
        System.out.println(Arrays.toString(LoanStatus.values()));                     // [ACTIVE, OVERDUE, RETURNED, LOST]

        System.out.println("── Test 2: MembershipTier");
        System.out.println(MembershipTier.BASIC.getMaxLoans() + " " + MembershipTier.STUDENT.getLoanDays());  // 3 28
        System.out.println(String.format("%.2f", MembershipTier.BASIC.fineFor(6)));     // 3.00
        System.out.println(String.format("%.2f", MembershipTier.STUDENT.fineFor(10)));  // 1.00
        System.out.println(String.format("%.2f", MembershipTier.PREMIUM.fineFor(-3)));  // 0.00

        System.out.println("── Test 3: Book record");
        Book dune = new Book("111", "Dune", 1965);
        System.out.println(dune.title() + " " + dune.equals(new Book("111", "Dune", 1965)));  // Dune true
        try { new Book(" ", "X", 2000); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        try { new Book("1", null, 2000); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        try { new Book("1", "X", 1200); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }

        System.out.println("── Test 4: checkout, ids, due day");
        LoanDesk desk4 = new LoanDesk();
        Loan a = desk4.checkout("M1", new Book("111", "Dune", 1965), MembershipTier.BASIC, 0);
        Loan b = desk4.checkout("M2", new Book("222", "Emma", 1815), MembershipTier.PREMIUM, 3);
        System.out.println(a);                                   // LN-001[M1, Dune, ACTIVE, due=14]
        System.out.println(b.getId() + " " + b.getDueDay());     // LN-002 24
        System.out.println(new LoanDesk().checkout("M9", new Book("9", "Z", 2000), MembershipTier.STUDENT, 1).getId());  // LN-001

        System.out.println("── Test 5: checkout validation, no id consumed on failure");
        LoanDesk desk5 = new LoanDesk();
        Book dune5 = new Book("111", "Dune", 1965);
        desk5.checkout("M1", dune5, MembershipTier.BASIC, 0);
        try { desk5.checkout("M2", dune5, MembershipTier.BASIC, 0); } catch (IllegalStateException e) { System.out.println(e.getMessage()); }
        try { desk5.checkout("", new Book("5", "A", 2000), MembershipTier.BASIC, 0); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        try { desk5.checkout("M2", null, MembershipTier.BASIC, 0); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        try { desk5.checkout("M2", new Book("5", "A", 2000), null, 0); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        try { desk5.checkout("M2", new Book("5", "A", 2000), MembershipTier.BASIC, -1); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        System.out.println(desk5.checkout("M2", new Book("5", "A", 2000), MembershipTier.BASIC, 0).getId());  // LN-002

        System.out.println("── Test 6: member limit counts open loans only");
        LoanDesk desk6 = new LoanDesk();
        desk6.checkout("M1", new Book("1", "A", 2000), MembershipTier.BASIC, 0);
        desk6.checkout("M1", new Book("2", "B", 2000), MembershipTier.BASIC, 0);
        desk6.checkout("M1", new Book("3", "C", 2000), MembershipTier.BASIC, 0);
        try { desk6.checkout("M1", new Book("4", "D", 2000), MembershipTier.BASIC, 0); } catch (IllegalStateException e) { System.out.println(e.getMessage()); }
        desk6.transition("LN-001", LoanStatus.RETURNED);
        System.out.println(desk6.checkout("M1", new Book("4", "D", 2000), MembershipTier.BASIC, 0).getId());  // LN-004

        System.out.println("── Test 7: find, transition errors, immutable findAll");
        LoanDesk desk7 = new LoanDesk();
        desk7.checkout("M1", new Book("1", "A", 2000), MembershipTier.STUDENT, 0);
        System.out.println(desk7.findById("LN-001").map(Loan::getMemberId).orElse("none"));  // M1
        System.out.println(desk7.findById("LN-777").map(Loan::getMemberId).orElse("none"));  // none
        desk7.transition("LN-001", LoanStatus.RETURNED);
        try { desk7.transition("LN-001", LoanStatus.OVERDUE); } catch (IllegalStateException e) { System.out.println(e.getMessage()); }
        try { desk7.transition("LN-999", LoanStatus.LOST); } catch (NoSuchElementException e) { System.out.println(e.getMessage()); }
        try { desk7.findAll().add(null); } catch (UnsupportedOperationException e) { System.out.println("findAll is immutable"); }

        System.out.println("── Test 8: overdue pipeline, report order, EnumMap counts");
        LoanDesk desk8 = new LoanDesk();
        desk8.checkout("M1", new Book("111", "Dune", 1965), MembershipTier.BASIC, 0);
        desk8.checkout("M2", new Book("222", "Emma", 1815), MembershipTier.STUDENT, 0);
        desk8.checkout("M3", new Book("333", "Ulysses", 1922), MembershipTier.BASIC, 2);
        desk8.checkout("M4", new Book("444", "Hamlet", 1603), MembershipTier.PREMIUM, 5);
        System.out.println(desk8.markOverdue(20));                                                          // 2
        System.out.println(desk8.overdueReport(20).stream().map(Loan::getId).collect(Collectors.joining(", ")));  // LN-001, LN-003
        System.out.println(desk8.countByStatus());          // {ACTIVE=2, OVERDUE=2, RETURNED=0, LOST=0}
        desk8.transition("LN-003", LoanStatus.LOST);
        System.out.println(desk8.countByStatus());          // {ACTIVE=2, OVERDUE=1, RETURNED=0, LOST=1}
        System.out.println(desk8.overdueReport(20).size()); // 1

        System.out.println("── Test 9: return with fines; re-lend a returned book");
        LoanDesk desk9 = new LoanDesk();
        desk9.checkout("M1", new Book("111", "Dune", 1965), MembershipTier.BASIC, 0);
        desk9.checkout("M4", new Book("444", "Hamlet", 1603), MembershipTier.PREMIUM, 5);
        desk9.markOverdue(20);
        System.out.println(String.format("%.2f", desk9.returnBook("LN-001", 20)));  // 3.00
        System.out.println(String.format("%.2f", desk9.returnBook("LN-002", 20)));  // 0.00
        try { desk9.returnBook("LN-001", 21); } catch (IllegalStateException e) { System.out.println(e.getMessage()); }
        System.out.println(desk9.checkout("M5", new Book("111", "Dune", 1965), MembershipTier.BASIC, 21).getId());  // LN-003

        System.out.println("── Test 10: nextDue");
        LoanDesk desk10 = new LoanDesk();
        System.out.println(desk10.nextDue().isPresent());                                   // false
        desk10.checkout("M1", new Book("1", "A", 2000), MembershipTier.BASIC, 0);
        desk10.checkout("M2", new Book("2", "B", 2000), MembershipTier.STUDENT, 0);
        desk10.checkout("M3", new Book("3", "C", 2000), MembershipTier.PREMIUM, 0);
        System.out.println(desk10.nextDue().map(Loan::getId).orElse("none"));               // LN-001
        desk10.transition("LN-001", LoanStatus.RETURNED);
        System.out.println(desk10.nextDue().map(Loan::getId).orElse("none"));               // LN-003

        // EXTRA — the most common mistake: treating only ACTIVE as "on loan".
        // An OVERDUE book is still out of the building and must not be lent again.
        System.out.println("── Test 11 (extra): OVERDUE loans still hold their book and count toward the limit");
        LoanDesk desk11 = new LoanDesk();
        Book hamlet = new Book("444", "Hamlet", 1603);
        desk11.checkout("M1", hamlet, MembershipTier.BASIC, 0);          // due 14
        desk11.markOverdue(30);                                           // LN-001 -> OVERDUE
        try { desk11.checkout("M2", hamlet, MembershipTier.BASIC, 30); } catch (IllegalStateException e) { System.out.println(e.getMessage()); }  // Book already on loan: 444
        desk11.checkout("M1", new Book("1", "A", 2000), MembershipTier.BASIC, 30);
        desk11.checkout("M1", new Book("2", "B", 2000), MembershipTier.BASIC, 30);
        try { desk11.checkout("M1", new Book("3", "C", 2000), MembershipTier.BASIC, 30); } catch (IllegalStateException e) { System.out.println(e.getMessage()); }  // Loan limit reached for member: M1
    }
}
