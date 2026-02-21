package com.ramkumar.lld.oop.inheritance.practice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ============================================================
 *  PRACTICE: Employee Payroll System
 * ============================================================
 *
 * Problem Statement:
 * ------------------
 * Design an Employee Payroll System that models a company with
 * three types of employees. The system must demonstrate all four
 * OOP pillars: Encapsulation, Abstraction, Inheritance, Polymorphism.
 *
 * System Overview:
 * ----------------
 * A company has FullTime, PartTime, and Contract employees.
 * A PayrollProcessor can accept any mix of employee types and
 * compute salaries without knowing the concrete type.
 *
 * ── Class 1: Employee (abstract base) ─────────────────────────
 * Fields:
 *   1. employeeId  (String)  — immutable, set at construction
 *   2. name        (String)  — mutable with validation
 *   3. department  (String)  — mutable
 *
 * Constructors:
 *   1. Employee(employeeId, name, department)
 *      — validate employeeId is not blank
 *      — all concrete subclasses must call this via super()
 *
 * Methods:
 *   1. calculateMonthlySalary() — abstract; each subclass computes differently
 *   2. getDetails()             — concrete; returns a formatted string using
 *                                 calculateMonthlySalary() internally
 *   3. Standard getters; setName() with blank check
 *
 * ── Class 2: FullTimeEmployee extends Employee ─────────────────
 * Additional field:
 *   1. annualSalary (double) — must be >= 0
 *
 * Constructor:
 *   1. FullTimeEmployee(id, name, dept, annualSalary) — chains via super()
 *
 * calculateMonthlySalary():
 *   → annualSalary / 12.0
 *
 * ── Class 3: PartTimeEmployee extends Employee ─────────────────
 * Additional fields:
 *   1. hourlyRate           (double) — rate per hour
 *   2. hoursWorkedThisMonth (int)    — updated each month
 *
 * Constructor:
 *   1. PartTimeEmployee(id, name, dept, hourlyRate, hoursWorked)
 *
 * Methods:
 *   1. calculateMonthlySalary() → hourlyRate * hoursWorkedThisMonth
 *   2. updateHoursWorked(int hours) — validates hours >= 0
 *
 * ── Class 4: ContractEmployee extends Employee ─────────────────
 * Additional fields:
 *   1. contractAmount  (double) — total contract value
 *   2. durationMonths  (int)    — must be > 0
 *
 * Constructor:
 *   1. ContractEmployee(id, name, dept, contractAmount, durationMonths)
 *
 * calculateMonthlySalary():
 *   → contractAmount / durationMonths
 *
 * ── Class 5: PayrollProcessor ──────────────────────────────────
 * Fields:
 *   1. employees — a List<Employee> (internal, never exposed as mutable)
 *
 * Methods:
 *   1. addEmployee(Employee e) — adds to internal list
 *   2. getEmployees()          — returns an UNMODIFIABLE view of the list
 *   3. processSalaries()       — prints getDetails() for each employee
 *   4. getTotalPayroll()       — sum of all monthly salaries
 *   5. getHighestPaid()        — returns the Employee with max monthly salary
 *
 * Design Rules:
 * -------------
 *  - Employee must be abstract (cannot be instantiated directly)
 *  - All salary fields must be private (encapsulation)
 *  - No instanceof chains in PayrollProcessor — use polymorphism
 *  - getEmployees() must NOT return the internal mutable list directly
 *
 * ============================================================
 *  Write your solution below. Delete this comment block when done.
 * ============================================================
 */
public class PayrollPractice {

    // =========================================================================
    // ── TODO 1: Declare the abstract class Employee ───────────────────────────
    //    Fields: employeeId (final String), name (String), department (String)
    //    Constructor: validate employeeId not blank
    //    Abstract method: calculateMonthlySalary()
    //    Concrete method: getDetails() — use calculateMonthlySalary() inside
    //    Getters + setName() with validation
    // =========================================================================
    abstract static class Employee{
        private final String employeeId;
        private String name;
        private String department;


        public Employee(String employeedId, String name, String department){
            if(employeedId == null || employeedId.isBlank()){
                throw new IllegalArgumentException("EmployeeId cannot be blank");
            }
            this.employeeId = employeedId;
            this.name = name;
            this.department = department;
        }

        public abstract double calculateMonthlySalary();

        public String getDetails(){
            return "Employee{employeeId=" +  employeeId
                    + ",name=" + name + ",department=" + department + ","
                    + "monthlySalary=" + calculateMonthlySalary()+"}";
        }

        public String getDepartment(){
            return department;
        }

        public String getEmployeedId(){
            return employeeId;
        }

        public String getName(){
            return name;
        }

        public void setName(String name){
            if(name == null || name.isBlank()){
                throw new IllegalArgumentException("Employee Name cannot be Blank!!");
            }
            this.name = name;
        }
    }

    // =========================================================================
    // ── TODO 2: Implement FullTimeEmployee extends Employee ───────────────────
    //    Additional field: annualSalary (double, private)
    //    Constructor: call super(), validate annualSalary >= 0
    //    calculateMonthlySalary(): annualSalary / 12.0
    //    Getter: getAnnualSalary()
    // =========================================================================
    static class FullTimeEmployee extends Employee{

        private double annualSalary;

        public FullTimeEmployee(String employeedId, String name, String department, double annualSalary){
            super(employeedId, name, department);
            if(annualSalary <0.0){
                throw new IllegalArgumentException("Annual Salary cannot be negative or 0");
            }
            this.annualSalary = annualSalary;
        }

        @Override
        public double calculateMonthlySalary(){
            return annualSalary / 12.0;
        }

        public double getAnnualSalary(){
            return annualSalary;
        }

    }

    // =========================================================================
    // ── TODO 3: Implement PartTimeEmployee extends Employee ───────────────────
    //    Additional fields: hourlyRate (double), hoursWorkedThisMonth (int)
    //    Constructor: call super()
    //    calculateMonthlySalary(): hourlyRate * hoursWorkedThisMonth
    //    updateHoursWorked(int hours): validate >= 0
    // =========================================================================
    static class PartTimeEmployee extends Employee{
        private double hourlyRate;
        private int hoursWorkedThisMonth;

        public PartTimeEmployee(String employeedId,
                                String name,
                                String department,
                                double hourlyRate,
                                int hoursWorkedThisMonth){
            super(employeedId, name, department);
            if(hourlyRate < 0.0 ){
                throw new IllegalArgumentException("Hourly Rate cannot be < 0 for employee");
            }
            this.hourlyRate = hourlyRate;
            this.hoursWorkedThisMonth = hoursWorkedThisMonth;

        }

        public double getHourlyRate() {return hourlyRate;};
        public double getHoursWorkedThisMonth() {return hoursWorkedThisMonth;};
        public void updateHoursWorked(int hours){
            if(hours < 0){
                throw new IllegalArgumentException("Hours Worked cannot be les than or equal to zero");
            }
            hoursWorkedThisMonth = hours;
        }

        @Override
        public double calculateMonthlySalary(){
            return hoursWorkedThisMonth * hourlyRate;
        }
    }

    // =========================================================================
    // ── TODO 4: Implement ContractEmployee extends Employee ───────────────────
    //    Additional fields: contractAmount (double), durationMonths (int)
    //    Constructor: call super(), validate durationMonths > 0
    //    calculateMonthlySalary(): contractAmount / durationMonths
    // =========================================================================
    static class ContractEmployee extends Employee{
        private double contractAmount;
        private int durationMonths;

        public ContractEmployee(String employeedId,
                               String name,
                               String department,
                               double contractAmount,
                               int durationMonths){
            super(employeedId, name, department);
            if(contractAmount <= 0.0 ){
                throw new IllegalArgumentException("Contract Amount cannot be Zero!!!");
            }
            if(durationMonths <= 0) {
                throw new IllegalArgumentException("Duration Amount cannot be < 0");
            }
            this.contractAmount = contractAmount;
            this.durationMonths = durationMonths;
        }

        @Override
        public double calculateMonthlySalary() {
            return contractAmount / durationMonths;
        }

        public double getContractAmount(){
            return contractAmount;
        }

        public int getDurationMonths(){
            return durationMonths;
        }
    }


    // =========================================================================
    // ── TODO 5: Implement PayrollProcessor ───────────────────────────────────
    //    Field: List<Employee> employees (private, mutable internally)
    //    addEmployee(Employee e)
    //    getEmployees() → return unmodifiable view (hint: Collections.unmodifiableList)
    //    processSalaries() → print getDetails() for each; NO instanceof checks
    //    getTotalPayroll() → sum of all calculateMonthlySalary()
    //    getHighestPaid() → Employee with max monthly salary
    // =========================================================================
    static class PayrollProcessor {
        private List<Employee> employees;

        public PayrollProcessor(){
            this.employees = new ArrayList<>();
        }

        public void addEmployee(Employee e){
            employees.add(e);
        }

        public List<Employee> getEmployees(){
            return Collections.unmodifiableList(employees);
        }

        public void processSalaries(){
            for(Employee e : employees){
                System.out.println(e.getDetails());
            }
        }

        public double getTotalPayroll(){
            double totalPayroll = 0.0;
            for(Employee e: employees){
                totalPayroll += e.calculateMonthlySalary();
            }
            return totalPayroll;
        }

        public Employee getHighestPaid(){
            return employees.stream()
                    .max((a, b) -> Double.compare(
                            a.calculateMonthlySalary(),
                            b.calculateMonthlySalary()))
                    .orElseThrow(() -> new IllegalStateException("No employees"));
        }
    }

    // =========================================================================
    // Main — DO NOT MODIFY — implement the TODOs above to make these pass
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

        // Test 2: processSalaries() — polymorphic dispatch
        System.out.println("\n── Test 2: processSalaries() ────────────────────────");
        processor.processSalaries();

        // Test 3: Total payroll
        double total = processor.getTotalPayroll();
        System.out.printf("%nTest 3: Total payroll = ₹%,.2f%n", total);
        assert total > 0 : "Total payroll must be positive";

        // Test 4: Highest paid
        Employee highest = processor.getHighestPaid();
        System.out.println("Test 4: Highest paid = " + highest.getName());
        assert highest.getName().equals("Dave") : "Dave has highest annual salary";

        // Test 5: updateHoursWorked changes salary
        List<Employee> all = processor.getEmployees();
        for (Employee e : all) {
            if (e instanceof PartTimeEmployee pte && pte.getName().equals("Bob")) {
                double before = pte.calculateMonthlySalary();
                pte.updateHoursWorked(160);
                double after  = pte.calculateMonthlySalary();
                System.out.printf("Test 5: Bob salary %,.2f → %,.2f%n", before, after);
                assert after > before : "More hours = more pay";
            }
        }

        // Test 6: getEmployees() returns unmodifiable list
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

        System.out.println("\nAll tests completed.");
    }

    // =========================================================================
    // HINTS — read only when stuck
    // =========================================================================

    /*
     * ── HINT LEVEL 1 (Gentle) ─────────────────────────────────────────────────
     *
     * If you're unsure where to start:
     *  - Employee cannot be instantiated — what keyword makes a class un-instantiable?
     *  - Each subclass has ONE extra concept their salary formula uses. What is it?
     *  - PayrollProcessor should never need to check "which type is this?" to call
     *    the salary method. Think about what that means for how Employee declares it.
     */

    /*
     * ── HINT LEVEL 2 (Direct) ─────────────────────────────────────────────────
     *
     *  - Employee is `abstract class Employee { ... }` — it has an abstract method
     *    `public abstract double calculateMonthlySalary();`
     *  - Each subclass constructor MUST call `super(id, name, dept)` as the FIRST line.
     *  - `PayrollProcessor.getEmployees()` should return
     *    `Collections.unmodifiableList(employees)` — not `employees` directly.
     *  - `getTotalPayroll()` can be done with a simple loop or stream:
     *    employees.stream().mapToDouble(Employee::calculateMonthlySalary).sum()
     */

    /*
     * ── HINT LEVEL 3 (Near-Solution) ──────────────────────────────────────────
     *
     *  abstract static class Employee {
     *      private final String employeeId;
     *      private String name, department;
     *      protected Employee(String id, String name, String dept) {
     *          if (id == null || id.isBlank()) throw new IllegalArgumentException("...");
     *          this.employeeId = id; this.name = name; this.department = dept;
     *      }
     *      public abstract double calculateMonthlySalary();
     *      public String getDetails() {
     *          return employeeId + " | " + name + " | ₹" + calculateMonthlySalary();
     *      }
     *      // getters and setName() with validation
     *  }
     *
     *  static class FullTimeEmployee extends Employee {
     *      private double annualSalary;
     *      public FullTimeEmployee(String id, String name, String dept, double annual) {
     *          super(id, name, dept);
     *          this.annualSalary = annual;
     *      }
     *      @Override public double calculateMonthlySalary() { return annualSalary / 12.0; }
     *  }
     *  // PartTimeEmployee and ContractEmployee follow the same pattern.
     */
}
