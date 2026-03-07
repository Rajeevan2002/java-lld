# Review — Abstract Factory Pattern (Creational)
Phase 3, Topic 3.3 | Scenario B: Cloud Infrastructure Provider

---

## What You Got Right

- **All three abstract product interfaces** — `ComputeInstance`, `ObjectStorage`, and `ManagedDatabase` declared with the exact method signatures from the spec. Clean, minimal contracts.
- **`CloudProviderFactory` interface** — all three factory methods (`createCompute()`, `createStorage()`, `createDatabase()`) declared correctly. The factory is the only place the family decision lives.
- **All nine concrete products implement the correct interface** — AWS, Azure, and GCP variants for all three product types, no methods missing.
- **Per-instance (not static) counters in all three compute classes** — `private int counter = 0` ensures each `ComputeInstance` object has its own sequence. If it were `static`, all instances from the same family would share a counter — a subtle correctness bug.
- **Correct instance ID formats** — `aws-i-%05d`, `azure-vm-%05d`, `gcp-gce-%05d` all match the spec exactly.
- **`launch()` validates both `cpuCores >= 1` and `memoryGb >= 1`** — both guards present in all three compute implementations.
- **`query()` returns `Collections.unmodifiableList()`** — the caller cannot mutate the internal list, preventing hidden state corruption.
- **All three concrete factories create only same-family products** — `AWSCloudFactory` only creates `AWSS3Storage`, `AWSRDSDatabase`, `AWSComputeInstance`. Family consistency guaranteed.
- **`InfrastructureDeployer` references only interface types** — `ComputeInstance`, `ObjectStorage`, `ManagedDatabase` stored as fields; concrete class names never appear in the client. This is the core Abstract Factory guarantee.
- **No if/switch on provider in `InfrastructureDeployer`** — `deploy()` calls `compute.launch()`, `database.createTable()`, `storage.upload()` without knowing which family it has. Pure polymorphism.
- **`getCostPerHour()` formulas correct per provider** — AWS 0.048, Azure 0.052, GCP 0.044 per core.
- **`getProviderName()` returns the correct string** — "AWS", "Azure", "GCP" match across all three product types in each family.

---

## Issues Found

### Issue 1 — Bug: All database `insert()` and `query()` throw `NoSuchElementException` for a missing table (should be `IllegalStateException`)
- **Severity**: Bug
- **What**: When `insert()` or `query()` is called on a table that was never created, the code throws `NoSuchElementException`. The correct type is `IllegalStateException`.
- **Your code** (all six database methods across AWS/Azure/GCP):
  ```java
  if (!tables.containsKey(tableName))
      throw new NoSuchElementException("Table Name " + tableName + " not found!!");
  ```
- **Fix**:
  ```java
  if (!tables.containsKey(tableName))
      throw new IllegalStateException("Table not found: " + tableName);
  ```
- **Why it matters**: `NoSuchElementException` has a precise meaning in Java — it signals that an iterator or scanner has no more elements (see `Iterator.next()`, `Scanner.next()`). Using it for "table doesn't exist" tells the caller the wrong story. `IllegalStateException` signals that the *object's state* makes this operation impossible right now — which is exactly what "table not created yet" means. Using the wrong exception type breaks catch blocks, monitoring rules, and callers who handle these exceptions differently.

### Issue 2 — Bug: `upload()` rejects blank data strings (spec says only `null` is invalid)
- **Severity**: Bug
- **What**: All three storage implementations reject blank data with `data.isBlank()`, but the spec only says `null` is invalid — an empty string or whitespace-only string is valid object storage data.
- **Your code** (all three storage classes):
  ```java
  if (data == null || data.isBlank()) {
      throw new IllegalArgumentException("data must not be blank");
  }
  ```
- **Fix**:
  ```java
  if (data == null) throw new IllegalArgumentException("data must not be null");
  ```
- **Why it matters**: A blank string is a perfectly valid blob — a zero-byte config file, an empty CSV header, a tombstone record. Rejecting it based on whitespace content is an API contract violation. If a caller stores `""` (empty marker for "file was deleted") and your implementation throws, you've broken a valid use case that the spec explicitly allows.

### Issue 3 — Bug: `download()` throws with a literal string instead of the evaluated path
- **Severity**: Bug
- **What**: When a key is not found, the exception message is the literal source text `"bucket + key not found"` rather than the actual values of `bucket` and `key`.
- **Your code** (all three storage classes):
  ```java
  throw new NoSuchElementException("bucket + key not found");
  ```
- **Fix**:
  ```java
  String storeKey = bucket + "/" + key;
  if (!store.containsKey(storeKey))
      throw new NoSuchElementException(storeKey + " not found");
  ```
- **Why it matters**: When a caller's `download("prod-configs", "app/settings.json")` fails, the exception message `"bucket + key not found"` gives zero diagnostic information — you cannot tell which bucket or key was missing. The message should say `"prod-configs/app/settings.json not found"`. In production incident response, an unhelpful exception message adds minutes or hours of debugging time.

### Issue 4 — Design: All storage/database maps are package-private and non-final
- **Severity**: Design
- **What**: The internal maps in all six storage and database classes have no access modifier (package-private) and are not declared `final`, exposing internal state and allowing reassignment.
- **Your code** (six map declarations across AWS/Azure/GCP storage and database):
  ```java
  Map<String, String> storageData = new HashMap<>();
  Map<String, List<String>> tableData = new HashMap<>();
  ```
- **Fix**:
  ```java
  private final Map<String, String> store = new HashMap<>();
  private final Map<String, List<String>> tables = new HashMap<>();
  ```
- **Why it matters**: Without `private`, any class in the same package can directly read or modify `storageData` — bypassing all your validation logic. Without `final`, the field reference can be reassigned (`storageData = null` or `storageData = someOtherMap`) which silently drops all stored data. Both are encapsulation failures; interviewers will notice missing `private final` on fields immediately.

### Issue 5 — Minor: `%n\n` in `printf` produces a double newline
- **Severity**: Minor
- **What**: All compute `launch()`, storage `upload()`, and database `insert()` print statements use `%n\n`, which inserts two newlines per line — one from `%n` (platform newline) and one from `\n` (Unix newline).
- **Your code** (nine printf calls):
  ```java
  System.out.printf("[EC2] Launching %d-core/%dGB → %s%n\n", cpuCores, memoryGb, instanceId);
  ```
- **Fix**:
  ```java
  System.out.printf("[EC2] Launching %d-core/%dGB → %s%n", cpuCores, memoryGb, instanceId);
  ```
- **Why it matters**: In log files, double newlines create blank lines between every entry, making logs harder to scan. In terminal output, it creates visual gaps that look like errors. The rule: `%n` is already a complete newline in `printf` — never combine it with `\n`.

---

## Score Card

| Requirement | Result |
|---|---|
| `ComputeInstance` interface — `launch()`, `terminate()`, `getProviderName()`, `getCostPerHour()` | ✅ |
| `ObjectStorage` interface — `upload()`, `download()`, `getProviderName()` | ✅ |
| `ManagedDatabase` interface — `createTable()`, `insert()`, `query()`, `getProviderName()` | ✅ |
| `CloudProviderFactory` interface — `createCompute()`, `createStorage()`, `createDatabase()` | ✅ |
| AWS compute: `aws-i-%05d` ID format, per-instance counter | ✅ |
| Azure compute: `azure-vm-%05d` ID format, per-instance counter | ✅ |
| GCP compute: `gcp-gce-%05d` ID format, per-instance counter | ✅ |
| `launch()` validates `cpuCores >= 1`, `memoryGb >= 1` | ✅ |
| `upload()` validates bucket/key not blank, data not null (not isBlank) | ❌ (also rejects blank data via isBlank) |
| `download()` throws with evaluated bucket+"/"+key in message | ❌ (literal string "bucket + key not found") |
| `insert()` / `query()` throw `IllegalStateException` for missing table | ❌ (throws NoSuchElementException) |
| `query()` returns unmodifiable list | ✅ |
| All storage/database maps are `private final` | ❌ (package-private, non-final) |
| `getCostPerHour()` formulas correct (AWS 0.048, Azure 0.052, GCP 0.044) | ✅ |
| All 3 concrete factories create only same-family products | ✅ |
| `InfrastructureDeployer` references only interface types — no concrete names | ✅ |
| No if/switch on provider type in `InfrastructureDeployer` | ✅ |
| `printf` uses `%n` only (not `%n\n`) | ❌ (double newline in 9 printf calls) |
| `getProviderName()` returns "AWS" / "Azure" / "GCP" consistently | ✅ |

---

## Key Takeaways — Do Not Miss These

**TK-1: `IllegalStateException` = wrong object state; `NoSuchElementException` = iterator exhausted — choose by semantics.**
`NoSuchElementException` is for `Iterator.next()` when there are no more elements; `IllegalStateException` is for "the object's current state makes this operation impossible." A missing table is a state problem, not an iterator problem.
*Why it matters in interviews*: Using the wrong exception type signals you're guessing rather than reasoning; reviewers check exception semantics explicitly.

**TK-2: Validate exactly what the spec says — no more, no less.**
`isBlank()` goes beyond the spec's "null is invalid" — it silently rejects valid use cases like empty marker files or zero-byte tombstone records. Read the spec word for word before writing validation logic.
*Why it matters in interviews*: Over-validation is a common trap; it shows the candidate didn't read the requirements carefully and will require clarification loops in a real project.

**TK-3: Exception messages must contain evaluated values, not source-code descriptions.**
`"bucket + key not found"` is what the Java source says, not what the caller needs. The message must say `"prod-configs/app/settings.json not found"` so whoever reads the stack trace knows exactly what failed.
*Why it matters in interviews*: Good exception messages are a production readiness signal; an interviewer running your code who sees `"bucket + key not found"` knows you haven't thought about debuggability.

**TK-4: Internal collections must always be `private final`.**
No access modifier = package-private, which means any test or class in the same package can read and overwrite your internal map. `final` prevents accidental reassignment. `private final Map<...> store = new HashMap<>()` is the correct form every time.
*Why it matters in interviews*: Missing `private final` on fields is one of the top encapsulation mistakes interviewers flag on code review rounds.

**TK-5: `%n` in `printf` is a complete newline — never pair it with `\n`.**
`%n` expands to the platform line separator (`\r\n` on Windows, `\n` on Unix). Adding `\n` afterward produces a blank line after every log entry.
*Why it matters in interviews*: When asked to run your code live, double newlines stand out immediately and suggest lack of attention to output formatting.

**TK-6: The Abstract Factory guarantee is family consistency — the compiler, not the runtime, enforces it.**
By injecting only the factory interface and using only product interfaces, it becomes *impossible* to write code that mixes AWS compute with Azure storage. The type system prevents it at compile time — no runtime check needed.
*Why it matters in interviews*: This is the core value proposition interviewers want you to articulate: Abstract Factory makes cross-family mixing a compile error, not a runtime bug.
