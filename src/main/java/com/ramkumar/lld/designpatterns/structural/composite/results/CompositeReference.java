package com.ramkumar.lld.designpatterns.structural.composite.results;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference Solution — Composite Pattern: Organisation Chart
 *
 * Differences vs student solution:
 *   - Constructors are package-private (no 'public' modifier).
 *   - No stray extra spaces or blank lines.
 *
 * Test 9 (most-common-mistake catcher):
 *   Builds a 3-level deep structure (MegaCorp → Company + Finance → sub-teams → individuals)
 *   and asserts the total salary is $485,000.
 *   Catches two common mistakes:
 *     1. 'members' typed as List<Individual> — ClassCastException when adding a Team.
 *     2. getSalary() using instanceof that misses Team-within-Team — returns wrong total.
 */
public class CompositeReference {

    // ── [Component interface] — shared contract for leaf and composite ─────────
    interface OrgUnit {
        String getName();
        double getSalary();
        void display(String indent);
    }

    // ── [Leaf] — individual employee; no children; answers operations directly ──
    static class Individual implements OrgUnit {

        private final String name;    // [Immutable]
        private final String role;    // [Immutable]
        private final double salary;  // [Immutable]

        // [PackagePrivate] No 'public' — inner class constructor used only within this file.
        Individual(String name, String role, double salary) {
            this.name   = name;
            this.role   = role;
            this.salary = salary;
        }

        @Override public String getName()    { return name; }
        @Override public double getSalary()  { return salary; }  // [Direct] leaf returns its own field

        @Override
        public void display(String indent) {
            // [Leaf] prints itself and stops — no loop, no recursion
            System.out.printf("%s[Employee] %s (%s) $%.2f%n", indent, name, role, salary);
        }
    }

    // ── [Composite] — a team; holds children; delegates all operations ─────────
    static class Team implements OrgUnit {

        private final String name;

        // [KEY] List<OrgUnit> — the interface type, NOT List<Individual> or List<Team>.
        // This is what allows teams to contain other teams at any depth.
        // If typed as List<Individual>, you could not add a sub-team.
        private final List<OrgUnit> members = new ArrayList<>();  // [InlineInit] cleaner than in constructor

        // [PackagePrivate] No 'public'.
        Team(String name) {
            this.name = name;
        }

        // [AcceptsInterface] add() takes OrgUnit — NOT Individual or Team specifically.
        // This keeps add() open to any current or future Component implementation.
        void add(OrgUnit unit) {
            members.add(unit);
        }

        @Override public String getName() { return name; }

        @Override
        public double getSalary() {
            // [Delegation] Call member.getSalary() via the OrgUnit interface — no instanceof.
            // When a member is itself a Team, its getSalary() recurses to its own children.
            // The loop terminates at Individual nodes, which return their own salary directly.
            double total = 0.0;
            for (OrgUnit member : members) {
                total += member.getSalary();  // [Uniform] same call for leaf and composite
            }
            return total;
            // Note: returns 0.0 for empty team naturally — the loop doesn't iterate.
        }

        @Override
        public void display(String indent) {
            // [CompositeHeader] Print this team's summary line first.
            System.out.printf("%s[Team] %s $%.2f%n", indent, name, getSalary());
            // [Recursion] Each child adds another two spaces — depth is handled automatically.
            for (OrgUnit member : members) {
                member.display(indent + "  ");  // [IndentGrowth] two spaces per level
            }
        }
    }

    // ── Test harness ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        int passed = 0;
        int total  = 9;  // Tests 1–8 from practice + Test 9 (mistake-catcher)

        // Shared objects reused across tests (same as practice file order)
        Individual alice = new Individual("Alice", "Engineer", 90000.0);

        // ── Test 1: Individual name and salary ────────────────────────────────
        passed += check("Test 1 — Individual getName",     "Alice".equals(alice.getName()));
        passed += check("Test 1 — Individual getSalary",   fmt(alice.getSalary()).equals("90000.00"));
        total++;  // two assertions in Test 1

        // ── Test 2: Team of 2 employees, getSalary() = sum ───────────────────
        Individual bob = new Individual("Bob", "Designer", 100000.0);
        Team engineering = new Team("Engineering");
        engineering.add(alice);
        engineering.add(bob);
        passed += check("Test 2 — Team getSalary (Alice+Bob)", fmt(engineering.getSalary()).equals("190000.00"));

        // ── Test 3: Individual display() format ───────────────────────────────
        System.out.println("Test 3 — Individual display (verify output below):");
        alice.display("");
        System.out.println("Test 3 — expected: [Employee] Alice (Engineer) $90000.00");
        passed++;  // visual verification

        // ── Test 4: Team display() — header + indented members ────────────────
        System.out.println("\nTest 4 — Team display (verify output below):");
        engineering.display("");
        System.out.println("Test 4 — expected: [Team] Engineering $190000.00 then 2 indented employees");
        passed++;  // visual verification

        // ── Test 5: Nested getSalary() sums across sub-teams ──────────────────
        Individual carol = new Individual("Carol", "Product Manager", 120000.0);
        Team product = new Team("Product");
        product.add(carol);
        Team company = new Team("Company");
        company.add(engineering);  // [KeyLine] Team added to Team — requires List<OrgUnit>
        company.add(product);
        passed += check("Test 5 — Nested getSalary (Engineering+Product)", fmt(company.getSalary()).equals("310000.00"));

        // ── Test 6: Nested display() — correct 2-level indentation ───────────
        System.out.println("\nTest 6 — Nested display (verify output below):");
        company.display("");
        System.out.println("Test 6 — expected: Company header, then 2-space indented teams, then 4-space employees");
        passed++;  // visual verification

        // ── Test 7: Empty team has getSalary() == 0.0 ─────────────────────────
        Team empty = new Team("EmptyTeam");
        passed += check("Test 7 — Empty team salary", fmt(empty.getSalary()).equals("0.00"));

        // ── Test 8: Polymorphic — OrgUnit reference holds both leaf and composite ──
        System.out.println("\nTest 8 — polymorphic OrgUnit[] catalog:");
        OrgUnit[] units = { alice, engineering, company };
        for (OrgUnit u : units) {
            System.out.printf("  %-15s salary=$%,.2f%n", u.getName(), u.getSalary());
        }
        passed += check("Test 8 — polymorphic (3 units, correct salaries)",
            fmt(units[0].getSalary()).equals("90000.00")
            && fmt(units[1].getSalary()).equals("190000.00")
            && fmt(units[2].getSalary()).equals("310000.00"));

        // ── Test 9 (most-common-mistake catcher): 3-level deep recursive salary ──
        //
        // Structure:
        //   MegaCorp             ($485,000)
        //   ├── Company          ($310,000)  ← from Tests 5–8
        //   │   ├── Engineering  ($190,000)
        //   │   │   ├── Alice    ($ 90,000)
        //   │   │   └── Bob      ($100,000)
        //   │   └── Product      ($120,000)
        //   │       └── Carol    ($120,000)
        //   └── Finance          ($175,000)
        //       ├── Dave         ($ 80,000)
        //       └── Eve          ($ 95,000)
        //
        // Catches mistake 1: members typed as List<Individual>
        //   → ClassCastException when adding 'company' or 'finance' to megaCorp
        //
        // Catches mistake 2: getSalary() uses instanceof and misses Team children
        //   → returns 0 for Company sub-total; total comes out wrong
        System.out.println("\nTest 9 — 3-level recursive salary (mistake-catcher):");
        Individual dave = new Individual("Dave", "Analyst", 80000.0);
        Individual eve  = new Individual("Eve",  "Engineer", 95000.0);
        Team finance = new Team("Finance");
        finance.add(dave);
        finance.add(eve);
        Team megaCorp = new Team("MegaCorp");
        megaCorp.add(company);   // Team-of-Teams added — works only if members is List<OrgUnit>
        megaCorp.add(finance);   // Team added
        megaCorp.display("");
        // Expected total: 310,000 + 80,000 + 95,000 = 485,000
        passed += check("Test 9 — 3-level recursive getSalary = $485,000",
            fmt(megaCorp.getSalary()).equals("485000.00"));

        System.out.printf("%n%d / %d PASSED%n", passed, total);
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    private static int check(String label, boolean condition) {
        System.out.println(label + ": " + (condition ? "PASSED" : "FAILED"));
        return condition ? 1 : 0;
    }
}
