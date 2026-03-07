Generate a practice exercise for the following LLD / Java topic: $ARGUMENTS

IMPORTANT RULE — Two different scenarios, always:
- The `code/` directory already contains a worked example with Scenario A.
- The `practice/` exercise MUST use a completely different real-world Scenario B.
- The student must not be able to copy-paste from the worked example.
- Choose Scenario B so it tests the same concepts but in a fresh context.
  Good pairings (Scenario A → Scenario B):
    Employee hierarchy     → Vehicle rental / Media subscription / Insurance policy
    Shape/geometry         → Tax calculator / Shipping carrier / Notification system
    Bank account           → Library book / E-commerce cart / Loyalty rewards
    Animal kingdom         → Electronic device / Food menu / Course catalog

---

Follow this exact structure in your response:

## Context
State the phase and topic from the README.md roadmap.
State Scenario A (what the code/ worked example covers) — one line.
State Scenario B (what this practice exercise will cover) — one line.
Explain in one sentence why Scenario B tests the same concepts differently.

## Scenario B — Problem Statement
Write a realistic, interview-style problem statement for Scenario B. Include:
- A clear description of what the class or system should model
- A numbered list of field requirements per class (mark which are immutable/final)
- A numbered list of constructor requirements (state which should chain and how)
- A numbered list of method requirements with explicit validation rules
- Design constraints (e.g., "no instanceof chains", "list must be unmodifiable", etc.)

## Step 1 — Create the docs/ README
If a docs/README.md does not already exist for this topic, create one covering:
- Core concepts with ASCII diagrams
- Code snippets for each concept
- Comparison tables where relevant
- 5–7 interview Q&A
- Common mistakes section

## Step 2 — Create the code/ worked example (Scenario A)
Create a fully commented Java file in `code/` for Scenario A.
Package: `com.ramkumar.lld.<phase>.<topic>.code`
- Comment every non-obvious decision
- Label which lines demonstrate which concept (Encapsulation, Polymorphism, etc.)
- Include a main() that prints labelled output

## Step 3 — Create the practice/ skeleton (Scenario B)
Create the skeleton Java file in `practice/` for Scenario B.
Package: `com.ramkumar.lld.<phase>.<topic>.practice`
- Javadoc block at the top with the full problem statement (file is self-contained)
- A main() with at least 7 pre-written test cases — ALL test case lines are commented out
  so the file compiles immediately with zero student code. The student uncomments each
  block as they implement the corresponding TODO.
- DO NOT MODIFY comment on main()
- 3-level hints block at the bottom:
    HINT 1 (Gentle)      — points to the right concept without naming the solution
    HINT 2 (Direct)      — names the API / pattern to use
    HINT 3 (Near-solution) — shows the class skeleton without the method bodies

### main() test-case format — CRITICAL

Every test case inside main() must be **fully commented out** in the skeleton file.
The student uncomments each block after implementing the relevant TODO.

**Structure each test block like this:**

```java
public static void main(String[] args) {
    // Uncomment each block after implementing the corresponding TODO.

    // ── Test 1: <one-line description> (uncomment after TODO 3) ──────────
    // MyClass obj = new MyClass("foo", 42);
    // System.out.println(obj.doSomething());   // expected: "result"

    // ── Test 2: <one-line description> (uncomment after TODO 4) ──────────
    // System.out.println(obj.anotherMethod());  // expected: 99
}
```

**Why:** The file must compile before the student writes any code. Commented-out test
cases achieve this without stub methods or `UnsupportedOperationException` hacks, which
either give away signatures or produce misleading runtime output.

**Rules for test blocks:**
1. Every executable line inside main() is a `//`-prefixed comment in the skeleton.
2. Each block has a one-line header comment naming what it tests and which TODO unlocks it.
3. Expected output is shown inline as a `// expected: …` comment on the same line.
4. Group related assertions into one named block — do not scatter single lines.
5. The only non-comment line allowed in main() is the opening instruction comment.

### TODO marker rules — CRITICAL

**The single most important rule: a TODO block must NEVER contain actual code —
not written, not commented out. Code in a TODO gives away the answer.**

#### What belongs in a TODO: problem-statement language only

Each TODO repeats the relevant slice of the problem statement inline so the student
never needs to scroll up. Use requirement phrasing, not Java syntax.

**A TODO block is ONLY the comment. The student writes the field declaration,
constructor, or method entirely — signature and body — from scratch.**

**Fields TODO:**

```java
// ── TODO 1: Declare 9 fields in ServerConfig — all private final
//    String  host                   required; set from Builder.host
//    int     port                   required; set from Builder.port
//    int     maxConnections         optional; default 100
//    long    connectionTimeoutMs    optional; default 5_000
//    long    readTimeoutMs          optional; default 30_000
//    boolean keepAlive              optional; default false
//    boolean sslEnabled             optional; default false
//    String  certPath               optional; default null
//    String  keyPath                optional; default null
```

**Constructor TODO:**

```java
// ── TODO 2: private constructor ServerConfig(Builder b)
//    Must be private — only Builder.build() may call it
//    Copy every field from b: host, port, maxConnections, connectionTimeoutMs,
//    readTimeoutMs, keepAlive, sslEnabled, certPath, keyPath
```

**Method TODO:**

```java
// ── TODO 5: launch(int cpuCores, int memoryGb) → String
//    Validate: cpuCores < 1 → throw new IllegalArgumentException("cpuCores must be >= 1")
//    Validate: memoryGb  < 1 → throw new IllegalArgumentException("memoryGb must be >= 1")
//    Increment a per-instance counter field (NOT static — each instance counts separately)
//    ID format:  String.format("aws-i-%05d", ++counter)
//    Print:      System.out.printf("[EC2] Launching %d-core/%dGB → %s%n", cpuCores, memoryGb, id)
//    Return:     the generated id string
```

#### ❌ Anti-patterns — NEVER do these

```java
// ❌ BAD: field declarations written after the TODO
// ── TODO 1: Declare fields
private final String host;   // ← student should write this, not the exercise

// ❌ BAD: constructor/method signature + empty body written after the TODO
// ── TODO 2: private constructor
private ServerConfig(Builder b) {   // ← student should write this
    // your code here               // ← student should write this
}

// ❌ BAD: method signature + empty body written after the TODO
// ── TODO 5: launch()
@Override
public String launch(int cpuCores, int memoryGb) {  // ← student should write this
    // your code here
}

// ❌ BAD: code commented out inside a TODO
// ── TODO 2: private constructor
//    private ServerConfig(Builder b) {
//        this.host = b.host;     // ← commented code gives away the answer
//    }

// ❌ BAD: one-liner TODO with no spec detail
// ── TODO 3: Implement launch()   ← forces student to scroll to find the rules

// ❌ BAD: uncommented test case in main()
MyClass obj = new MyClass("foo");   // ← won't compile until student writes MyClass
obj.doSomething();

// ❌ BAD: stub method to make test compile
@Override
public String doSomething() {
    throw new UnsupportedOperationException("TODO");  // ← gives away the signature
}
```

#### Rules checklist for every TODO

1. **Comment only** — the TODO block is the entire entry. No signature, no body after it.
2. **No code** — not written, not commented out. Spec language only.
3. **Fields absent** — field declarations do not exist in the file at all; only the TODO comment.
4. **Constructors absent** — constructor signatures + bodies do not exist; only the TODO comment.
5. **Methods absent** — method signatures + bodies do not exist; only the TODO comment.
6. **Validation** — exact condition, exact exception type, exact exception message string.
7. **Return value** — exact format string or formula (e.g. `cpuCores * 0.048`).
8. **Print statement** — exact format string including the tag (e.g. `[EC2]`, `[RDS]`).
9. **Constraints** — any "must be X, NOT Y" notes (e.g. "per-instance counter, NOT static").

The only pre-written Java in the file is the outer class shell, inner class/interface shells
(so the student knows the class hierarchy), import statements, and the `main()` method shell
(with all test lines commented out). Everything else — fields, constructors, methods — is a
TODO comment the student implements.

## Step 4 — Create results/.gitkeep
Ensure `results/.gitkeep` exists as an empty placeholder.

## What This Exercise Tests
Table: concept → exactly where in the skeleton it is tested.

## Suggested file paths
List all files created.
