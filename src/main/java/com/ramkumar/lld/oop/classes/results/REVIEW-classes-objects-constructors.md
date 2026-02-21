# Review: Classes, Objects, and Constructors
**Topic:** Phase 1 — OOP Fundamentals / Classes, Objects, Constructors
**Reference solution:** `BankAccountReference.java` (same directory)

---

## What You Implemented

A `BankAccountPractice` class with:
- 4 constructors (no-arg-style, 2-arg, 3-arg, copy)
- `deposit()` and `withdraw()` with partial validation
- Getters and a custom `toString()`
- Static counter via instance initializer block

All 6 test cases in `main()` ran and produced output. One test case silently produced
a wrong result (copy constructor balance — see below).

---

## Issues Found

### 1. Bug — Copy constructor copies balance (should be 0.0)

**Spec said:** copy gets balance = 0.0 (it's a *new* account, not a snapshot)

```java
// Your code — wrong
public BankAccountPractice(BankAccountPractice other) {
    this(other.getHolderName(), other.getAccountType(), other.getBalance()); // ← copies balance
}

// Fix — delegate to 2-arg, balance stays 0.0
public BankAccountReference(BankAccountReference other) {
    this(other.holderName, other.accountType);  // balance intentionally omitted
}
```

**Why it matters:** Copy constructors model "create a similar account", not "clone an account".
Copying financial state silently is a dangerous design error.

---

### 2. No constructor chaining — the core lesson

Each constructor re-initialized all fields independently. This is brittle — if you later
add a field, you must update every constructor.

```java
// Your approach — initialization repeated 3 times
public BankAccountPractice(String holderName) {
    this.accountNumber = generateAccountNumber(accountNumberCounter); // duplicated
    this.holderName = holderName;                                     // duplicated
    this.balance = 0.0;                                               // duplicated
    this.accountType = "SAVINGS";
}

// Correct — one master constructor, others just supply defaults
public BankAccountReference(String holderName) {
    this(holderName, "SAVINGS", 0.0);   // done
}

public BankAccountReference(String holderName, String accountType, double initialDeposit) {
    // ALL initialization happens here, once
    this.accountNumber = String.format("ACC-%03d", ++counter);
    this.holderName    = holderName;
    this.accountType   = accountType;
    this.balance       = initialDeposit;
}
```

---

### 3. Missing validation in `deposit()`

```java
// Your code — silently accepts negative deposits
public void deposit(double amount) {
    balance += amount;   // deposit(-500) increases balance silently
}

// Fix
public void deposit(double amount) {
    if (amount <= 0)
        throw new IllegalArgumentException("Deposit amount must be positive: " + amount);
    balance += amount;
}
```

---

### 4. Missing validation in `withdraw()` for amount <= 0

```java
// Your code — only checks > balance, not <= 0
public void withdraw(double amount) {
    if (amount > balance) { throw ...; }
    balance -= amount;  // withdraw(-500) increases balance silently
}

// Fix — two guards
public void withdraw(double amount) {
    if (amount <= 0)    throw new IllegalArgumentException("...");
    if (amount > balance) throw new IllegalArgumentException("...");
    balance -= amount;
}
```

---

### 5. `accountNumber` should be `final`

```java
// Your code
private String accountNumber;   // can be reassigned after construction

// Fix
private final String accountNumber;  // compiler enforces immutability
```

---

### 6. `generateAccountNumber()` is over-engineered

Taking the static counter as a parameter when it's already accessible as a static field
adds noise. One expression in the master constructor is cleaner:

```java
// Your code
private String generateAccountNumber(int accountNumberCounter) {
    return "ACC-" + accountNumberCounter;
}
// called as: this.accountNumber = generateAccountNumber(accountNumberCounter);

// Reference — just inline it
this.accountNumber = String.format("ACC-%03d", ++counter);
```

---

## Score Card

| Criterion | Result |
|-----------|--------|
| All fields declared | ✅ |
| Constructor chaining | ❌ — each ctor initializes independently |
| Copy constructor correct | ❌ — balance copied instead of reset |
| `deposit()` validation | ❌ — missing `amount <= 0` check |
| `withdraw()` validation | ⚠️ — `> balance` caught, `<= 0` missed |
| `accountNumber` immutable | ⚠️ — works but not `final` |
| Getters | ✅ |
| `toString()` | ✅ |
| Tests pass | ⚠️ — 5/6 correct, copy balance silently wrong |

---

## Key Takeaways — Do Not Miss These

### TK-1: One master constructor, always
> Every class should have exactly ONE constructor that initializes ALL fields.
> Every other constructor is just a convenience wrapper that calls `this(...)` with defaults.
> If you find yourself repeating `this.x = x` in multiple constructors, you're doing it wrong.

### TK-2: `this()` must be the first statement
> Constructor chaining via `this()` must be the very first line — no code before it.
> This is also why you cannot use both `this()` and `super()` in the same constructor.

### TK-3: Copy constructors copy identity-neutral state only
> A copy constructor creates a *new entity* that resembles another, not a clone.
> Fields that represent identity (IDs, account numbers) or transient state (balance)
> should usually be reset, not copied. Ask: "would this field belong to the new object
> from day 1?" If not, don't copy it.

### TK-4: Validate at every entry point
> Public methods are trust boundaries. Anything can be passed to `deposit()` or `withdraw()`.
> Never assume the caller is well-behaved. Validate before mutating state.

### TK-5: Use `final` for fields that never change after construction
> `final` is not just a safety net — it's documentation. A reader seeing `final` immediately
> knows this field won't change. Use it for IDs, account numbers, creation timestamps, etc.

### TK-6: Instance initializer blocks are a code smell for counters
> Instance blocks run before every constructor (including chained ones, once).
> They're valid but surprising. Prefer incrementing the counter in the master constructor —
> it's explicit, predictable, and readable.

### TK-7: Interview tip — narrate your constructor chain
> In an LLD interview, say: "I'll have one master constructor that does all initialization,
> and the others will just supply defaults via this()." This signals that you understand
> DRY, maintainability, and the purpose of constructor chaining.
