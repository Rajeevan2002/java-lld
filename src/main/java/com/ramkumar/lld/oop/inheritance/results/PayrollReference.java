package com.ramkumar.lld.oop.inheritance.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reference solution for the Employee Payroll System practice problem.
 *
 * Key decisions vs the practice submission:
 *  1. FullTimeEmployee validates annualSalary < 0 (not <= 0) — zero is a valid salary.
 *  2. processSalaries() actually prints — return value of getDetails() is not discarded.
 *  3. updateHoursWorked SETS hours (=), not accumulates (+=).
 *  4. updateHoursWorked allows 0 — validate < 0, not <= 0.
 *  5. hoursWorkedThisMonth is int, not double — hours are whole numbers.
 *  6. Employee constructor null-checks before calling isBlank().
 *  7. Field name is employeeId (not employeedId).
 */
public class PayrollReference {

    // =========================================================================
    // ABSTRACTION + ENCAPSULATION — Abstract base class
    // =========================================================================

    abstract static class Employee {

        private final String employeeId;   // final — immutable after construction
        private String name;
        private String department;

        // protected — subclasses must call super(); no one else can call this directly
        protected Employee(String employeeId, String name, String department) {
            // null check BEFORE isBlank() — null.isBlank() throws NPE
            if (employeeId == null || employeeId.isBlank())
                throw new IllegalArgumentException("Employee ID cannot be null or blank");
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Name cannot be null or blank");
            this.employeeId  = employeeId;
            this.name        = name;
            this.department  = department;
        }

        // Abstract — every concrete subclass MUST provide their own formula
        public abstract double calculateMonthlySalary();

        // Concrete — shared formatting logic; calls the abstract method (Template Method)
        // This is abstraction in action: getDetails() doesn't care HOW salary is computed
        public String getDetails() {
            return String.format("Employee{id=%s, name=%s, dept=%s, monthlySalary=%.2f}",
                    employeeId, name, department, calculateMonthlySalary());
        }

        public String getEmployeeId() { return employeeId; }
        public String getName()       { return name; }
        public String getDepartment() { return department; }

        public void setName(String name) {
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Name cannot be blank");
            this.name = name;
        }
    }

    // =========================================================================
    // INHERITANCE — Concrete subclasses
    // =========================================================================

    static class FullTimeEmployee extends Employee {

        private double annualSalary;

        public FullTimeEmployee(String id, String name, String dept, double annualSalary) {
            super(id, name, dept);                 // MUST be first — chains to parent
            if (annualSalary < 0)                  // < 0, NOT <= 0 — zero IS valid
                throw new IllegalArgumentException("Annual salary cannot be negative");
            this.annualSalary = annualSalary;
        }

        @Override
        public double calculateMonthlySalary() {
            return annualSalary / 12.0;
        }

        public double getAnnualSalary() { return annualSalary; }
    }

    static class PartTimeEmployee extends Employee {

        private double hourlyRate;
        private int    hoursWorkedThisMonth;   // int — hours are whole numbers

        public PartTimeEmployee(String id, String name, String dept,
                                double hourlyRate, int hoursWorked) {
            super(id, name, dept);
            if (hourlyRate < 0)
                throw new IllegalArgumentException("Hourly rate cannot be negative");
            if (hoursWorked < 0)
                throw new IllegalArgumentException("Hours worked cannot be negative");
            this.hourlyRate           = hourlyRate;
            this.hoursWorkedThisMonth = hoursWorked;
        }

        // SET, not accumulate — this is the total hours worked THIS month
        // Accumulation would double salaries on repeated calls
        public void updateHoursWorked(int hours) {
            if (hours < 0)    // < 0, NOT <= 0 — zero hours (no-pay leave) is valid
                throw new IllegalArgumentException("Hours cannot be negative");
            this.hoursWorkedThisMonth = hours;   // = not +=
        }

        @Override
        public double calculateMonthlySalary() {
            return hourlyRate * hoursWorkedThisMonth;
        }

        public double getHourlyRate()          { return hourlyRate; }
        public int    getHoursWorkedThisMonth(){ return hoursWorkedThisMonth; }
    }

    static class ContractEmployee extends Employee {

        private double contractAmount;
        private int    durationMonths;

        public ContractEmployee(String id, String name, String dept,
                                double contractAmount, int durationMonths) {
            super(id, name, dept);
            if (contractAmount < 0)
                throw new IllegalArgumentException("Contract amount cannot be negative");
            if (durationMonths <= 0)
                throw new IllegalArgumentException("Duration must be positive");
            this.contractAmount = contractAmount;
            this.durationMonths = durationMonths;
        }

        @Override
        public double calculateMonthlySalary() {
            return contractAmount / durationMonths;
        }

        public double getContractAmount() { return contractAmount; }
        public int    getDurationMonths() { return durationMonths; }
    }

    // =========================================================================
    // POLYMORPHISM — PayrollProcessor never checks concrete type
    // =========================================================================

    static class PayrollProcessor {

        // Declared as ArrayList internally so we can mutate it
        private final List<Employee> employees = new ArrayList<>();

        public void addEmployee(Employee e) {
            if (e == null) throw new IllegalArgumentException("Cannot add null employee");
            employees.add(e);
        }

        // Return unmodifiable view — callers can read, never mutate the internal list
        public List<Employee> getEmployees() {
            return Collections.unmodifiableList(employees);
        }

        // Pure polymorphism — same call on every element, JVM dispatches to the right override
        // No instanceof. No type check. If you add a 4th employee type, this method needs ZERO changes.
        public void processSalaries() {
            employees.forEach(e -> System.out.println(e.getDetails()));  // println is critical!
        }

        public double getTotalPayroll() {
            return employees.stream()
                    .mapToDouble(Employee::calculateMonthlySalary)  // method reference
                    .sum();
        }

        public Employee getHighestPaid() {
            return employees.stream()
                    .max((a, b) -> Double.compare(
                            a.calculateMonthlySalary(),
                            b.calculateMonthlySalary()))
                    .orElseThrow(() -> new IllegalStateException("No employees in payroll"));
        }
    }

    // =========================================================================
    // Main — same 7 test cases + 1 extra that catches the most common mistake
    // =========================================================================

    public static void main(String[] args) {

        PayrollProcessor processor = new PayrollProcessor();

        // Test 1: Add all three types — upcasting to Employee
        processor.addEmployee(new FullTimeEmployee("E001", "Alice",  "Engineering", 1_200_000));
        processor.addEmployee(new PartTimeEmployee("E002", "Bob",    "Design",      500, 80));
        processor.addEmployee(new ContractEmployee("E003", "Carol",  "Marketing",   360_000, 6));
        processor.addEmployee(new FullTimeEmployee("E004", "Dave",   "Engineering", 2_400_000));
        processor.addEmployee(new PartTimeEmployee("E005", "Eve",    "Support",     400, 60));
        System.out.println("Test 1 PASSED: All employee types added");

        // Test 2: processSalaries() — must actually print lines, not silently compute
        System.out.println("\n── Test 2: processSalaries() ────────────────────────");
        processor.processSalaries();   // 5 lines of output expected

        // Test 3: Total payroll
        double total = processor.getTotalPayroll();
        System.out.printf("%nTest 3: Total payroll = ₹%,.2f%n", total);
        assert total > 0 : "Total payroll must be positive";

        // Test 4: Highest paid
        Employee highest = processor.getHighestPaid();
        System.out.println("Test 4: Highest paid = " + highest.getName());
        assert highest.getName().equals("Dave") : "Dave has highest annual salary";

        // Test 5: updateHoursWorked SETS (not accumulates)
        List<Employee> all = processor.getEmployees();
        for (Employee e : all) {
            if (e instanceof PartTimeEmployee pte && pte.getName().equals("Bob")) {
                double before = pte.calculateMonthlySalary();   // 80 * 500 = 40,000
                pte.updateHoursWorked(160);                     // SET to 160
                double after  = pte.calculateMonthlySalary();   // 160 * 500 = 80,000
                System.out.printf("Test 5: Bob salary %,.2f → %,.2f%n", before, after);
                assert after == 80_000.0 : "160 hours * 500/hr = 80,000 (set, not accumulate)";
            }
        }

        // Test 6: getEmployees() is unmodifiable — passes 0 salary to trigger this path
        try {
            processor.getEmployees().add(new FullTimeEmployee("X", "Hacker", "None", 0));
            System.out.println("Test 6 FAILED: list should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            System.out.println("Test 6 PASSED: getEmployees() is unmodifiable");
        }

        // Test 7: Invalid employeeId throws
        try {
            new FullTimeEmployee("", "Ghost", "Unknown", 100_000);
            System.out.println("Test 7 FAILED: blank ID should be rejected");
        } catch (IllegalArgumentException e) {
            System.out.println("Test 7 PASSED: blank employeeId rejected — " + e.getMessage());
        }

        // Test 8 (extra): The most common mistake — updateHoursWorked accumulates
        // If += was used, calling update twice would give 3x salary instead of 2x
        PartTimeEmployee pte = new PartTimeEmployee("E006", "Frank", "QA", 300, 80);
        pte.updateHoursWorked(100);   // should SET to 100, not 80+100=180
        double salary = pte.calculateMonthlySalary();
        System.out.printf("Test 8: Frank salary after updateHoursWorked(100) = %,.2f%n", salary);
        assert salary == 30_000.0
            : "Expected 100 * 300 = 30,000 (set), but got " + salary + " (likely accumulated)";
        System.out.println("Test 8 " + (salary == 30_000.0 ? "PASSED" : "FAILED") +
                ": updateHoursWorked sets, not accumulates");

        System.out.println("\nAll tests completed.");
    }
}
