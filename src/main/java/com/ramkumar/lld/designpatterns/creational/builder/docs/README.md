# 3.4 — Builder Pattern (Creational)

> "Separate the construction of a complex object from its representation so that
>  the same construction process can create different representations."
> — Gang of Four (GoF)

---

## 1. The Problem Builder Solves

### Telescoping Constructors (the anti-pattern)

When an object has many optional parameters, you end up with a chain of
constructors, each adding one more parameter:

```java
// Which argument is which? Easy to swap timeoutMs and maxConnections.
new HttpRequest("GET", "https://api.example.com", null, null, 5000, true)
new HttpRequest("POST", "https://api.example.com", headers, body, 5000, true)
new HttpRequest("POST", "https://api.example.com", headers, body, 10000, false)
```

### JavaBeans Setters (the other anti-pattern)

Setters let you build in stages but the object is **mutable and never valid**
between calls — any thread could observe a half-constructed object:

```java
HttpRequest r = new HttpRequest();
r.setMethod("POST");        // invalid here — no url yet
r.setUrl("https://...");    // valid here
r.setTimeoutMs(5000);
// but r is mutable forever — nothing enforces immutability
```

### Builder solves both

```java
HttpRequest request = new HttpRequest.Builder("POST", "https://api.example.com")
    .header("Authorization", "Bearer token123")
    .body("{\"key\":\"value\"}")
    .timeoutMs(10_000)
    .followRedirects(false)
    .build();                // ← validated here; returns immutable HttpRequest
```

---

## 2. Structure

```
«Product»                «Builder»
─────────────            ───────────────────────────────────────────
HttpRequest              HttpRequest.Builder
─────────────            ───────────────────────────────────────────
- method: String  ←────  - method: String          (required, final)
- url: String     ←────  - url: String             (required, final)
- headers: Map    ←────  - headers: Map            (optional, default = {})
- body: String    ←────  - body: String            (optional, default = null)
- timeoutMs: int  ←────  - timeoutMs: int          (optional, default = 5000)
                         ───────────────────────────────────────────
+ getMethod()            + Builder(method, url)    ← required fields
+ getUrl()               + header(k, v): Builder   ← fluent setter
+ getHeaders()           + body(b): Builder        ← fluent setter
+ getBody()              + timeoutMs(t): Builder   ← fluent setter
+ getTimeoutMs()         + validate()              ← private
                         + build(): HttpRequest    ← creates immutable product
```

**The five key design decisions:**

| Decision | Rule |
|---|---|
| Product constructor | `private` — only Builder can call it |
| Required fields | `final` in Builder; passed in Builder constructor |
| Optional fields | non-final; have sensible defaults |
| Fluent setters | return `this` — enable method chaining |
| Validation | happens in `build()`, not in individual setters |

---

## 3. Builder vs Alternatives

| | Telescoping Constructors | JavaBeans Setters | Builder |
|---|---|---|---|
| **Immutability** | ✅ possible | ❌ mutable forever | ✅ enforced |
| **Readability** | ❌ positional args | ✅ named | ✅ named |
| **Validation** | scattered | scattered | ✅ one place (`build()`) |
| **Optional params** | ❌ combinatorial explosion | ✅ | ✅ |
| **Thread safety** | ✅ | ❌ | ✅ (builder is local; product is immutable) |
| **Required vs optional** | ❌ unclear | ❌ unclear | ✅ constructor = required; setters = optional |

---

## 4. Code Skeleton

```java
public class ServerConfig {
    // ── Product fields — ALL private final ────────────────────────────────
    private final String host;          // required
    private final int    port;          // required
    private final int    maxConnections; // optional
    private final boolean sslEnabled;   // optional

    // ── Private constructor — only Builder can instantiate ─────────────────
    private ServerConfig(Builder b) {
        this.host           = b.host;
        this.port           = b.port;
        this.maxConnections = b.maxConnections;
        this.sslEnabled     = b.sslEnabled;
    }

    // ── Getters only — no setters ──────────────────────────────────────────
    public String  getHost()           { return host; }
    public int     getPort()           { return port; }
    public int     getMaxConnections() { return maxConnections; }
    public boolean isSslEnabled()      { return sslEnabled; }

    // ── Static nested Builder ──────────────────────────────────────────────
    public static class Builder {
        // Required — stored as final; passed in constructor
        private final String host;
        private final int    port;

        // Optional — have defaults
        private int     maxConnections = 100;
        private boolean sslEnabled     = false;

        public Builder(String host, int port) {
            this.host = host;
            this.port = port;
        }

        // Fluent setters — return this
        public Builder maxConnections(int val) { this.maxConnections = val; return this; }
        public Builder sslEnabled(boolean val) { this.sslEnabled = val;    return this; }

        // Validation — called only at build() time
        private void validate() {
            if (host == null || host.isBlank())
                throw new IllegalArgumentException("host must not be blank");
            if (port < 1 || port > 65535)
                throw new IllegalArgumentException("port must be between 1 and 65535");
            if (maxConnections < 1)
                throw new IllegalArgumentException("maxConnections must be >= 1");
        }

        public ServerConfig build() {
            validate();
            return new ServerConfig(this);
        }
    }
}

// Usage
ServerConfig cfg = new ServerConfig.Builder("localhost", 8080)
    .maxConnections(200)
    .sslEnabled(true)
    .build();
```

---

## 5. Static Nested Builder vs Separate Builder Class

Prefer the **static nested class** form (shown above). It has two advantages:
1. `ServerConfig`'s constructor can be `private` — no other class can bypass the Builder.
2. The Builder and Product live together in one file — the spec and the class are co-located.

Use a separate `ServerConfigBuilder` class only when the product class is generated code
(e.g., from a schema) and you can't add a nested class.

---

## 6. Validate at `build()`, Not at Each Setter

```java
// ❌ Wrong — eager validation in setter
public Builder port(int val) {
    if (val < 1 || val > 65535) throw new IAE("...");  // fires on every call
    this.port = val;
    return this;
}

// ✅ Right — deferred validation in build()
public Builder port(int val) { this.port = val; return this; }

public ServerConfig build() {
    validate();           // one place, validates the final assembled state
    return new ServerConfig(this);
}
```

Deferred validation lets you set fields in any order without spurious failures,
and lets you write cross-field validation (e.g., `sslEnabled` requires `certPath`) in one place.

---

## 7. Cross-Field Validation

The Builder shines for rules that span multiple fields — impossible with per-setter validation:

```java
private void validate() {
    // Single-field rules
    if (host == null || host.isBlank())
        throw new IllegalArgumentException("host must not be blank");
    if (port < 1 || port > 65535)
        throw new IllegalArgumentException("port must be between 1 and 65535");

    // Cross-field rule — only expressible at build() time
    if (sslEnabled && (certPath == null || certPath.isBlank()))
        throw new IllegalStateException("sslEnabled requires certPath");
    if (sslEnabled && (keyPath == null || keyPath.isBlank()))
        throw new IllegalStateException("sslEnabled requires keyPath");
}
```

---

## 8. Interview Q&A

**Q1: Why is the product constructor `private`?**
A: It forces all callers to go through the Builder, which is the only path through
   `validate()`. Without `private`, a caller could do `new ServerConfig()` and get an
   invalid object. Making the constructor `private` makes "invalid product" a compile error.

**Q2: Why are required fields `final` in the Builder?**
A: `final` in the Builder makes it impossible to accidentally reassign a required field
   inside a fluent setter. It also signals to the reader: "this was set in the constructor
   and never changes." Optional fields are non-final because they start with a default
   and can be overridden.

**Q3: Why validate at `build()` rather than at each setter?**
A: (1) Cross-field validation requires all fields to be set first — impossible in individual
   setters. (2) Deferred validation lets setters be called in any order. (3) All validation
   lives in one place, making it easy to find and test.

**Q4: What exception type for invalid state vs invalid argument?**
A: `IllegalArgumentException` for single bad argument values (port out of range, blank host).
   `IllegalStateException` for cross-field constraint violations (sslEnabled=true but no certPath)
   because the error is not one argument being bad — the *state* of the builder is inconsistent.

**Q5: How does Builder relate to Factory Method?**
A: Factory Method creates one product type but you don't control the construction steps.
   Builder gives you control over each construction step and is ideal when the product
   has many optional configuration points. Factories create; Builders construct step by step.

**Q6: How is Builder implemented in the Java standard library?**
A: `StringBuilder` / `StringBuffer` — the classic example (append, insert, delete steps).
   `java.util.stream.Stream.Builder`. `ProcessBuilder` — builds a system process.
   `HttpClient.newBuilder()` and `HttpRequest.newBuilder()` in `java.net.http` (Java 11+).
   Lombok's `@Builder` annotation generates the Builder at compile time.

**Q7: What is the Director and when do you need it?**
A: The Director is an optional class that calls Builder methods in a specific sequence
   to produce a standard configuration. It separates "what steps to take" from "how each
   step is implemented." In Java it is rarely used — callers usually call the Builder
   directly. Use a Director when you have multiple fixed recipes (e.g., `buildDevConfig()`,
   `buildProdConfig()`) that you want to centralize.

---

## 9. Common Mistakes

| Mistake | Why it's wrong | Fix |
|---|---|---|
| Mutable product (setters on the product) | Defeats immutability — any code can change the object after `build()` | Remove all setters from the product; use only getters |
| Validating in every setter | Cross-field rules are impossible; setters fire before all fields are set | Move all validation to `build()` → `validate()` |
| Required fields as optional (with defaults) in Builder | Callers forget to set them; default is usually wrong for required data | Required fields go in the Builder constructor, not as fluent setters |
| Returning new product on every setter (`return new Product(this)`) | Breaks fluency; intermediate objects are invalid | Setters return `this` (the Builder); only `build()` creates the product |
| `public` product constructor alongside Builder | Caller bypasses `validate()` | Make product constructor `private` |
| Not making Builder fields match product fields | Forgetting to copy a field in the `Product(Builder)` constructor | Review every field: Builder → product copy |
