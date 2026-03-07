package com.ramkumar.lld.designpatterns.structural.composite.practice;

import java.util.ArrayList;
import java.util.List;

/**
 * Practice Exercise — Composite Pattern: Organisation Chart
 *
 * <p><b>Scenario B — Part-Whole Hierarchy</b>
 *
 * <p>Model an organisation chart where individual employees and whole teams can be
 * treated uniformly. A team may contain individual employees or other (sub-)teams.
 * Any node in the tree should answer the same two questions: "What is your name?"
 * and "What is your total salary cost?"
 *
 * <p><b>Class hierarchy (do not change the names):</b>
 * <pre>
 *   OrgUnit       [Component interface]   ← TODO 1
 *   Individual    [Leaf]                  ← TODOs 2–4
 *   Team          [Composite]             ← TODOs 5–9
 * </pre>
 *
 * <p><b>OrgUnit interface</b> (TODO 1) — declare exactly these 3 methods:
 * <ol>
 *   <li>{@code String getName()} — returns the name of this unit.</li>
 *   <li>{@code double getSalary()} — returns the total salary cost of this unit.</li>
 *   <li>{@code void display(String indent)} — prints this unit (and its children) to stdout.</li>
 * </ol>
 *
 * <p><b>Individual (Leaf)</b> (TODOs 2–4):
 * <ul>
 *   <li>Fields: {@code private final String name}, {@code private final String role},
 *       {@code private final double salary} — all immutable.</li>
 *   <li>Constructor: {@code Individual(String name, String role, double salary)}.</li>
 *   <li>{@code getName()} → returns name.</li>
 *   <li>{@code getSalary()} → returns salary (no delegation — leaf has no children).</li>
 *   <li>{@code display(String indent)} → prints exactly:
 *       {@code System.out.printf("%s[Employee] %s (%s) $%.2f%n", indent, name, role, salary)}</li>
 * </ul>
 *
 * <p><b>Team (Composite)</b> (TODOs 5–9):
 * <ul>
 *   <li>Field: {@code private final String name}.</li>
 *   <li>Field: {@code private final List<OrgUnit> members} — typed as the interface,
 *       initialized to {@code new ArrayList<>()} (inline or in constructor).</li>
 *   <li>Constructor: {@code Team(String name)}.</li>
 *   <li>{@code add(OrgUnit unit)} → appends unit to members. No return value.</li>
 *   <li>{@code getName()} → returns name.</li>
 *   <li>{@code getSalary()} → iterates members, sums each member's {@code getSalary()}.
 *       Returns 0.0 for an empty team. Do NOT use instanceof — call the interface method.</li>
 *   <li>{@code display(String indent)} → prints the team header first, then calls
 *       {@code display(indent + "  ")} on each member:
 *       <pre>
 *         System.out.printf("%s[Team] %s $%.2f%n", indent, name, getSalary())
 *         for each member → member.display(indent + "  ")
 *       </pre>
 *   </li>
 * </ul>
 *
 * <p><b>Design constraints:</b>
 * <ul>
 *   <li>{@code members} must be {@code List<OrgUnit>} — NOT {@code List<Individual>}
 *       or {@code List<Team>}.</li>
 *   <li>{@code add()} must accept {@code OrgUnit} — NOT {@code Individual} or {@code Team}.</li>
 *   <li>No {@code instanceof} anywhere in {@code Team.getSalary()} or {@code Team.display()}.</li>
 *   <li>{@code add()} belongs only on {@code Team}, not on the {@code OrgUnit} interface.</li>
 * </ul>
 */
public class OrgChartPractice {

    // ── Component interface ────────────────────────────────────────────────────
    interface OrgUnit {
        String getName();
        double getSalary();
        void display(String indent);
    }

    // ── Leaf ───────────────────────────────────────────────────────────────────
    static class Individual implements OrgUnit {


        private final String name;
        private final String role;
        private final double salary;

        public Individual(String name, String role, double salary){
            this.name = name;
            this.role = role;
            this.salary = salary;
        }

        @Override
        public String getName() {return name;}

        @Override
        public double getSalary() {
            return salary;
        }

        @Override
        public void display(String indent) {
            System.out.printf("%s[Employee] %s (%s) $%.2f%n", indent, name, role, salary);
        }
    }

    // ── Composite ──────────────────────────────────────────────────────────────
    static class Team implements OrgUnit {

        private final String name;
        private final List<OrgUnit> members;

        public Team(String name) {
            this.name = name;
            this.members = new ArrayList<>();
        }


        public void add(OrgUnit unit){
            members.add(unit);
        }

        @Override
        public String getName(){
            return name;
        }

        @Override
        public double getSalary(){
            double teamSalary  = 0.0;
            for(OrgUnit member: members){
                teamSalary += member.getSalary();
            }
            return teamSalary;
        }

        @Override
        public void display(String indent){
            System.out.printf("%s[Team] %s $%.2f%n", indent, name, getSalary());
            for(OrgUnit member: members) {
                member.display(indent + "  ");
            }
        }
    }

    // ── DO NOT MODIFY — test harness ──────────────────────────────────────────
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: Individual name and salary (uncomment after TODO 4) ───────────────────
         Individual alice = new Individual("Alice", "Engineer", 90000.0);
         System.out.println("Test 1 — Individual getName: "
             + ("Alice".equals(alice.getName()) ? "PASSED" : "FAILED (got: " + alice.getName() + ")"));
         System.out.println("Test 1 — Individual getSalary: "
             + (fmt(alice.getSalary()).equals("90000.00") ? "PASSED" : "FAILED (got: " + fmt(alice.getSalary()) + ")"));

        // ── Test 2: Team of 2 employees, getSalary() = sum (uncomment after TODO 9) ───────
         Individual bob = new Individual("Bob", "Designer", 100000.0);
         Team engineering = new Team("Engineering");
         engineering.add(alice);
         engineering.add(bob);
         System.out.println("Test 2 — Team getSalary (Alice+Bob): "
             + (fmt(engineering.getSalary()).equals("190000.00") ? "PASSED" : "FAILED (got: " + fmt(engineering.getSalary()) + ")"));

        // ── Test 3: Individual display() format (uncomment after TODO 4) ─────────────────
         System.out.println("Test 3 — Individual display (verify output below):");
         alice.display("");
         // expected: [Employee] Alice (Engineer) $90000.00

        // ── Test 4: Team display() format — team header + indented members (uncomment after TODO 9) ──
         System.out.println("Test 4 — Team display (verify output below):");
         engineering.display("");
         // expected:
         // [Team] Engineering $190000.00
         //   [Employee] Alice (Engineer) $90000.00
         //   [Employee] Bob (Designer) $100000.00

        // ── Test 5: Nested team — getSalary() sums across sub-teams recursively (uncomment after TODO 9) ──
         Individual carol = new Individual("Carol", "Product Manager", 120000.0);
         Team product = new Team("Product");
         product.add(carol);
         Team company = new Team("Company");
         company.add(engineering);   // Team added to Team — only works if members is List<OrgUnit>
         company.add(product);
         System.out.println("Test 5 — Nested getSalary (Engineering+Product): "
             + (fmt(company.getSalary()).equals("310000.00") ? "PASSED" : "FAILED (got: " + fmt(company.getSalary()) + ")"));

        // ── Test 6: Nested display() — correct 2-level indentation (uncomment after TODO 9) ──
         System.out.println("Test 6 — Nested display (verify output below):");
         company.display("");
         // expected:
         // [Team] Company $310000.00
         //   [Team] Engineering $190000.00
         //     [Employee] Alice (Engineer) $90000.00
         //     [Employee] Bob (Designer) $100000.00
         //   [Team] Product $120000.00
         //     [Employee] Carol (Product Manager) $120000.00

        // ── Test 7: Empty team has getSalary() == 0.0 (uncomment after TODO 9) ─────────
         Team empty = new Team("EmptyTeam");
         System.out.println("Test 7 — Empty team salary: "
             + (fmt(empty.getSalary()).equals("0.00") ? "PASSED" : "FAILED (got: " + fmt(empty.getSalary()) + ")"));

        // ── Test 8: Polymorphic — OrgUnit reference holds both Individual and Team (uncomment after all TODOs) ──
         System.out.println("Test 8 — polymorphic OrgUnit[] catalog:");
         OrgUnit[] units = { alice, engineering, company };
         for (OrgUnit u : units) {
             System.out.printf("  %-15s salary=$%,.2f%n", u.getName(), u.getSalary());
         }
         System.out.println("Test 8 — PASSED if 3 lines printed with correct salaries (90000, 190000, 310000)");
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HINTS — read only if stuck
    // ════════════════════════════════════════════════════════════════════════════

    // HINT 1 (Gentle):
    //   Both Individual and Team should answer getName(), getSalary(), and display()
    //   the same way from the outside. Individual answers directly from its fields.
    //   Team answers by asking each of its children the same questions — and each
    //   child knows how to answer for itself.

    // HINT 2 (Direct):
    //   Use the Composite pattern.
    //   Individual is the Leaf: implements OrgUnit directly, no children.
    //   Team is the Composite: holds private final List<OrgUnit> members (interface type!).
    //   Team.getSalary() sums member.getSalary() for each member — no instanceof.
    //   Team.display() prints its header, then calls member.display(indent + "  ").

    // HINT 3 (Near-solution skeleton — class outlines only, no method bodies):
    //
    //   interface OrgUnit {
    //       String getName();
    //       double getSalary();
    //       void display(String indent);
    //   }
    //
    //   static class Individual implements OrgUnit {
    //       private final String name, role;
    //       private final double salary;
    //       Individual(String name, String role, double salary) { ... }
    //       @Override public String getName()             { ... }
    //       @Override public double getSalary()           { ... }
    //       @Override public void   display(String indent){ ... }
    //   }
    //
    //   static class Team implements OrgUnit {
    //       private final String name;
    //       private final List<OrgUnit> members = new ArrayList<>();
    //       Team(String name) { ... }
    //       void add(OrgUnit unit) { ... }
    //       @Override public String getName()             { ... }
    //       @Override public double getSalary()           { ... }
    //       @Override public void   display(String indent){ ... }
    //   }
}
