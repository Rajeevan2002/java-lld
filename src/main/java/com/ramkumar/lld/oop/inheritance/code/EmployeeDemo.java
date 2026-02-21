package com.ramkumar.lld.oop.inheritance.code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Worked example for Inheritance, Polymorphism, Encapsulation, Abstraction.
 *
 * Models an Employee hierarchy:
 *   Employee (abstract)
 *     ├── FullTimeEmployee
 *     ├── PartTimeEmployee
 *     └── ContractEmployee
 *
 * PayrollProcessor uses polymorphism to process any mix of Employee types.
 */
public class EmployeeDemo {

    // =========================================================================
    // ABSTRACTION + ENCAPSULATION — Abstract base class
    // =========================================================================

    abstract static class Employee {

        // private — encapsulated, subclasses access via super() or protected getters
        private final String employeeId;
        private String name;
        private String department;

        // Abstract classes CAN have constructors — called via super() in subclass
        protected Employee(String employeeId, String name, String department) {
            if (employeeId == null || employeeId.isBlank())
                throw new IllegalArgumentException("Employee ID cannot be blank");
            this.employeeId  = employeeId;
            this.name        = name;
            this.department  = department;
        }

        // ── Abstract method — MUST be overridden by every concrete subclass ───
        public abstract double calculateMonthlySalary();

        // ── Concrete method — shared logic using the abstract method ──────────
        // This is the Template Method pattern in miniature:
        //   getDetails() defines the format; calculateMonthlySalary() fills in the value
        public String getDetails() {
            return String.format("[%s] %-20s | Dept: %-12s | Monthly: ₹%,.2f",
                    employeeId, name, department, calculateMonthlySalary());
        }

        // ── Encapsulated getters — controlled read access ─────────────────────
        public String getEmployeeId() { return employeeId; }
        public String getName()       { return name; }
        public String getDepartment() { return department; }

        // Setter with validation — controlled write access
        public void setName(String name) {
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Name cannot be blank");
            this.name = name;
        }
    }

    // =========================================================================
    // INHERITANCE — Concrete subclasses
    // =========================================================================

    /** Salaried employee — paid a fixed annual amount divided by 12. */
    static class FullTimeEmployee extends Employee {

        private double annualSalary;

        public FullTimeEmployee(String id, String name, String dept, double annualSalary) {
            super(id, name, dept);         // MUST be first — chains to parent constructor
            if (annualSalary < 0) throw new IllegalArgumentException("Salary cannot be negative");
            this.annualSalary = annualSalary;
        }

        // POLYMORPHISM — overrides the abstract method with FullTime-specific logic
        @Override
        public double calculateMonthlySalary() {
            return annualSalary / 12.0;
        }

        public double getAnnualSalary() { return annualSalary; }
    }

    /** Hourly employee — paid for actual hours worked each month. */
    static class PartTimeEmployee extends Employee {

        private double hourlyRate;
        private int    hoursWorkedThisMonth;

        public PartTimeEmployee(String id, String name, String dept,
                                double hourlyRate, int hoursWorked) {
            super(id, name, dept);
            this.hourlyRate            = hourlyRate;
            this.hoursWorkedThisMonth  = hoursWorked;
        }

        // POLYMORPHISM — different formula, same method name
        @Override
        public double calculateMonthlySalary() {
            return hourlyRate * hoursWorkedThisMonth;
        }

        public void updateHoursWorked(int hours) {
            if (hours < 0) throw new IllegalArgumentException("Hours cannot be negative");
            this.hoursWorkedThisMonth = hours;
        }
    }

    /** Contract employee — paid a fixed contract amount split across duration. */
    static class ContractEmployee extends Employee {

        private double contractAmount;
        private int    durationMonths;

        public ContractEmployee(String id, String name, String dept,
                                double contractAmount, int durationMonths) {
            super(id, name, dept);
            if (durationMonths <= 0) throw new IllegalArgumentException("Duration must be positive");
            this.contractAmount  = contractAmount;
            this.durationMonths  = durationMonths;
        }

        // POLYMORPHISM — yet another formula
        @Override
        public double calculateMonthlySalary() {
            return contractAmount / durationMonths;
        }
    }

    // =========================================================================
    // POLYMORPHISM in action — PayrollProcessor works with any Employee type
    // =========================================================================

    static class PayrollProcessor {

        // Returns unmodifiable view — protects internal list (encapsulation)
        private final List<Employee> employees = new ArrayList<>();

        public void addEmployee(Employee e) { employees.add(e); }

        public List<Employee> getEmployees() {
            return Collections.unmodifiableList(employees);   // don't expose mutable internals
        }

        // Key: calls calculateMonthlySalary() without knowing the concrete type
        // The JVM dispatches to the right override at runtime — runtime polymorphism
        public void processSalaries() {
            System.out.println("\n─── Monthly Payroll ───────────────────────────────────");
            employees.forEach(e -> System.out.println(e.getDetails()));
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
                    .orElseThrow(() -> new IllegalStateException("No employees"));
        }
    }

    // =========================================================================
    // Main — demonstrates all four pillars
    // =========================================================================

    public static void main(String[] args) {
        PayrollProcessor processor = new PayrollProcessor();

        // Upcasting — all stored as Employee (supertype reference)
        processor.addEmployee(new FullTimeEmployee ("E001", "Alice",  "Engineering", 1_200_000));
        processor.addEmployee(new PartTimeEmployee ("E002", "Bob",    "Design",      500, 80));
        processor.addEmployee(new ContractEmployee ("E003", "Carol",  "Marketing",   360_000, 6));
        processor.addEmployee(new FullTimeEmployee ("E004", "Dave",   "Engineering", 2_400_000));
        processor.addEmployee(new PartTimeEmployee ("E005", "Eve",    "Support",     400, 60));

        // Polymorphism — one loop, five different salary calculations
        processor.processSalaries();

        System.out.printf("%nTotal payroll: ₹%,.2f%n", processor.getTotalPayroll());

        Employee highest = processor.getHighestPaid();
        System.out.println("Highest paid : " + highest.getName()
                + " (₹" + String.format("%,.2f", highest.calculateMonthlySalary()) + "/month)");

        // instanceof pattern matching (Java 16+) — safe downcasting
        System.out.println("\n─── Type-specific details ──────────────────────────────");
        for (Employee e : processor.getEmployees()) {
            if (e instanceof FullTimeEmployee fte) {
                System.out.println(fte.getName() + " annual: ₹" +
                        String.format("%,.2f", fte.getAnnualSalary()));
            } else if (e instanceof PartTimeEmployee pte) {
                System.out.println(pte.getName() + " is hourly (part-time)");
            } else if (e instanceof ContractEmployee) {
                System.out.println(e.getName() + " is on a fixed contract");
            }
            // Note: if you find yourself writing this instanceof chain everywhere,
            // it's a sign that the logic belongs inside the class as a virtual method.
        }
    }
}
