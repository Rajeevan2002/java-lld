package com.ramkumar.lld.oop.classes.practice;

/**
 * ============================================================
 *  PRACTICE: Design a BankAccount class
 * ============================================================
 *
 * Problem Statement:
 * ------------------
 * Design a BankAccount class that models a real bank account.
 * Focus on applying proper encapsulation, constructor design,
 * and the `this` keyword.
 *
 * Requirements:
 * -------------
 * 1. Fields:
 *    - accountNumber (String) — auto-generated, immutable
 *    - holderName    (String) — mutable
 *    - balance       (double) — starts at 0.0 by default
 *    - accountType   (String) — "SAVINGS" or "CURRENT"
 *
 * 2. Constructors:
 *    a) BankAccount(String holderName)
 *       → defaults: accountType = "SAVINGS", balance = 0.0
 *    b) BankAccount(String holderName, String accountType)
 *       → defaults: balance = 0.0
 *    c) BankAccount(String holderName, String accountType, double initialDeposit)
 *       → full initialization
 *    d) Copy constructor — creates a new account with the SAME holder and type,
 *       but a balance of 0.0 and a NEW account number (it's a new account)
 *
 * 3. Methods:
 *    - deposit(double amount)   — add to balance; reject if amount <= 0
 *    - withdraw(double amount)  — deduct from balance; reject if amount <= 0 or > balance
 *    - getBalance()             — return balance
 *    - toString()               — readable representation
 *
 * 4. accountNumber generation:
 *    - Use a static counter to generate sequential IDs, e.g., "ACC-001", "ACC-002"
 *
 * Hint 1 (if stuck):
 *    → Use constructor chaining so that the initialization logic lives in ONE place.
 *
 * Hint 2 (if stuck on copy constructor):
 *    → The copy constructor should call the full 3-arg constructor with balance = 0.0.
 *
 * Hint 3 (if stuck on validation):
 *    → Throw IllegalArgumentException for invalid inputs with a clear message.
 *
 * ============================================================
 *  Write your solution below. Remove this comment when done.
 * ============================================================
 */
public class BankAccountPractice {

    // ── TODO 1: Declare your fields here ─────────────────────────────────────
    private String accountNumber;
    private String holderName;
    private double balance;
    private String accountType;


    // ── TODO 2: Implement auto account number generation (static counter) ────
    private static int accountNumberCounter = 0;
    {
        ++accountNumberCounter;
    }


    private String generateAccountNumber(int accountNumberCounter){
        return "ACC-" + accountNumberCounter;
    }


    // ── TODO 3: Constructor — holderName only ─────────────────────────────────
    public BankAccountPractice(String holderName){
        this.accountNumber = generateAccountNumber(accountNumberCounter);
        this.holderName = holderName;
        this.balance = 0.0;
        this.accountType = "SAVINGS";
    }


    // ── TODO 4: Constructor — holderName + accountType ───────────────────────
    public BankAccountPractice(String holderName, String accountType){
        this.accountNumber = generateAccountNumber(accountNumberCounter);
        this.holderName = holderName;
        this.balance = 0.0;
        this.accountType = accountType;
    }

    // ── TODO 5: Constructor — holderName + accountType + initialDeposit ───────
    public BankAccountPractice(String holderName, String accountType, double initialDeposit){
        this.accountNumber = generateAccountNumber(accountNumberCounter);
        this.holderName = holderName;
        this.balance = initialDeposit;
        this.accountType = accountType;
    }


    // ── TODO 6: Copy constructor ──────────────────────────────────────────────
    public BankAccountPractice(BankAccountPractice otherBankAccountPractice){
        this(otherBankAccountPractice.getHolderName(), otherBankAccountPractice.getAccountType(),
             otherBankAccountPractice.getBalance());
    }


    // ── TODO 7: deposit(double amount) ───────────────────────────────────────
    public void deposit(double amount){
        balance += amount;
    }


    // ── TODO 8: withdraw(double amount) ──────────────────────────────────────
    public void withdraw(double amount){
        if(amount > balance){
            throw new IllegalArgumentException("Withdrawal Amount Cannot be Greater than Balance");
        }
        balance -= amount;
    }


    // ── TODO 9: Getters (balance, accountNumber, holderName, accountType) ─────
    public double getBalance(){
        return balance;
    }

    public String getAccountNumber(){
        return accountNumber;
    }

    public String getHolderName(){
        return holderName;
    }

    public String getAccountType(){
        return accountType;
    }


    // ── TODO 10: toString() ───────────────────────────────────────────────────
    public String toString(){
        return "[Bank-Account Account Number = [" + getAccountNumber() + "] "
                + " Account Holder = [ " + getHolderName() + " ] "
                + " Balance = [ " + getBalance() + " ] "
                + " Account Type = [ " + getAccountType() + "]   ]";
    }

    // ── Main — test your implementation ───────────────────────────────────────

    public static void main(String[] args) {
        // Test 1: no-arg-style (holderName only)
        BankAccountPractice acc1 = new BankAccountPractice("Alice");
        System.out.println(acc1);

        // Test 2: with account type
        BankAccountPractice acc2 = new BankAccountPractice("Bob", "CURRENT");
        System.out.println(acc2);

        // Test 3: full constructor
        BankAccountPractice acc3 = new BankAccountPractice("Carol", "SAVINGS", 5000.0);
        System.out.println(acc3);

        // Test 4: deposit and withdraw
        acc1.deposit(1000.0);
        acc1.withdraw(250.0);
        System.out.println("acc1 after transactions: " + acc1.getBalance());

        // Test 5: copy constructor
        BankAccountPractice acc4 = new BankAccountPractice(acc3);
        System.out.println("Copy of acc3: " + acc4);
        System.out.println("Same object? " + (acc3 == acc4));               // false
        System.out.println("Same account number? " + acc3.getAccountNumber().equals(acc4.getAccountNumber())); // false

        // Test 6: invalid withdraw
        try {
            acc1.withdraw(99999.0);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }
    }
}
