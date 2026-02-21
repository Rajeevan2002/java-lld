package com.ramkumar.lld.oop.classes.results;

/**
 * Reference solution for the BankAccount practice problem.
 *
 * Key decisions explained:
 *  1. Constructor chaining — only the 3-arg master constructor initializes state.
 *     All other constructors supply defaults and delegate via this().
 *  2. accountNumber is final — immutability enforced by the compiler.
 *  3. Counter incremented inside the master constructor, not in an instance block.
 *  4. Both deposit() and withdraw() validate their input fully.
 *  5. Copy constructor delegates to the 2-arg constructor (balance stays 0.0).
 */
public class BankAccountReference {

    // ─── Fields ───────────────────────────────────────────────────────────────

    private static int counter = 0;

    private final String accountNumber;  // immutable — set once, never changed
    private String holderName;
    private double balance;
    private String accountType;

    // ─── Constructors — all chain down to the master ───────────────────────────

    /** holderName only → defaults: SAVINGS, balance 0.0 */
    public BankAccountReference(String holderName) {
        this(holderName, "SAVINGS", 0.0);
    }

    /** holderName + type → defaults: balance 0.0 */
    public BankAccountReference(String holderName, String accountType) {
        this(holderName, accountType, 0.0);
    }

    /**
     * Master constructor — the ONLY place where fields are initialized.
     * Every other constructor eventually reaches here.
     */
    public BankAccountReference(String holderName, String accountType, double initialDeposit) {
        if (initialDeposit < 0)
            throw new IllegalArgumentException("Initial deposit cannot be negative: " + initialDeposit);

        this.accountNumber = String.format("ACC-%03d", ++counter); // ACC-001, ACC-002, ...
        this.holderName    = holderName;
        this.accountType   = accountType;
        this.balance       = initialDeposit;
    }

    /**
     * Copy constructor — same holder and type, but balance resets to 0.0.
     * Delegates to the 2-arg constructor, not the 3-arg, so balance is not copied.
     */
    public BankAccountReference(BankAccountReference other) {
        this(other.holderName, other.accountType);  // balance intentionally omitted → 0.0
    }

    // ─── Mutators ─────────────────────────────────────────────────────────────

    public void deposit(double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Deposit amount must be positive: " + amount);
        balance += amount;
    }

    public void withdraw(double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Withdrawal amount must be positive: " + amount);
        if (amount > balance)
            throw new IllegalArgumentException(
                "Insufficient balance. Requested: " + amount + ", Available: " + balance);
        balance -= amount;
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    public String getAccountNumber() { return accountNumber; }
    public String getHolderName()    { return holderName; }
    public double getBalance()       { return balance; }
    public String getAccountType()   { return accountType; }

    // ─── toString ─────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "BankAccount{number=" + accountNumber
             + ", holder=" + holderName
             + ", type=" + accountType
             + ", balance=" + balance + "}";
    }

    // ─── Main — same test cases as the practice file ──────────────────────────

    public static void main(String[] args) {
        BankAccountReference acc1 = new BankAccountReference("Alice");
        System.out.println(acc1);  // ACC-001, SAVINGS, 0.0

        BankAccountReference acc2 = new BankAccountReference("Bob", "CURRENT");
        System.out.println(acc2);  // ACC-002, CURRENT, 0.0

        BankAccountReference acc3 = new BankAccountReference("Carol", "SAVINGS", 5000.0);
        System.out.println(acc3);  // ACC-003, SAVINGS, 5000.0

        acc1.deposit(1000.0);
        acc1.withdraw(250.0);
        System.out.println("acc1 after transactions: " + acc1.getBalance());  // 750.0

        BankAccountReference acc4 = new BankAccountReference(acc3);
        System.out.println("Copy of acc3: " + acc4);
        System.out.println("Same object?         " + (acc3 == acc4));                                    // false
        System.out.println("Same account number? " + acc3.getAccountNumber().equals(acc4.getAccountNumber())); // false
        System.out.println("Copy balance is 0.0? " + (acc4.getBalance() == 0.0));                        // true ← your code fails this

        try {
            acc1.withdraw(99999.0);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        try {
            acc1.deposit(-100);  // your code silently accepts this — this catches it
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }
    }
}
