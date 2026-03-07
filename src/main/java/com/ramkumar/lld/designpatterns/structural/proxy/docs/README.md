# 3.2.4 Proxy Pattern

## What Problem Does It Solve?

Sometimes you need to control access to an object — because creating it is expensive,
because callers need authorisation, because you want to cache results, or because the
object lives on a remote server. The naive fix is to push all that logic into the real
object — but that violates SRP (the object now has two reasons to change: its job and
its access policy).

The **Proxy** wraps an object behind the **same interface**. The client treats the proxy
exactly like the real subject and never knows the difference. The proxy intercepts the
call, does its cross-cutting job (lazy init, auth check, cache lookup…), then optionally
delegates to the real object.

---

## Proxy Types

| Type | Core job | Typical example |
|---|---|---|
| **Virtual** | Defer expensive creation until first use | `LazyImageProxy` — loads from disk only on `display()` |
| **Protection** | Enforce access control before delegating | Role-based guard before `delete()` |
| **Caching** | Return a cached result; skip repeat calls | `Map<Key,Result>` in front of a DB query |
| **Remote** | Represent an object on another machine | gRPC stub, RMI stub |
| **Logging** | Record every call transparently | Timing/audit wrapper around a service |

In interviews the most frequently tested types are **Virtual** and **Protection**.

---

## ASCII Structure Diagram

```
Client ──► Subject (interface)
                ▲                   ▲
                │                   │
           RealSubject            Proxy             ← implements Subject
                                                      AND has-a Subject
                                  ─────────────────
                                  - real: Subject   ← [stored reference]
                                  + request()       ← intercept + delegate
                                  ─────────────────
```

**Key rule:** Proxy and Decorator are structurally identical.
The difference is **intent**:
- Decorator — adds stackable behaviour. Designed to be layered many times.
- Proxy      — controls access/lifecycle. Usually one-to-one with the subject.

---

## Code Skeleton — Virtual Proxy (Lazy Initialisation)

```java
// ── [Subject] — shared contract ──────────────────────────────────────────────
interface Image {
    void display();
    String getFilename();
}

// ── [RealSubject] — expensive; loads the pixel data when constructed ──────────
class RealImage implements Image {
    private final String filename;

    RealImage(String filename) {
        this.filename = filename;
        System.out.println("[DISK] Loading: " + filename);  // ← expensive!
    }

    @Override public void display()         { System.out.println("[Display] " + filename); }
    @Override public String getFilename()   { return filename; }
}

// ── [Virtual Proxy] — defers RealImage creation until display() is called ────
class LazyImageProxy implements Image {
    private final String filename;   // ← enough to identify + later construct the real image
    private RealImage realImage;     // ← null until first display(); assigned at most once

    LazyImageProxy(String filename) {
        this.filename = filename;    // ← cheap — no disk I/O
    }

    @Override
    public void display() {
        if (realImage == null) {
            realImage = new RealImage(filename);  // ← [LazyInit] first call only
        }
        realImage.display();                      // ← [Delegation] every call
    }

    @Override
    public String getFilename() { return filename; }  // ← proxy answers this itself; no load
}
```

---

## Code Skeleton — Protection Proxy (Access Control)

```java
interface AdminService {
    void deleteUser(String userId);
    String viewAuditLog();
}

class RealAdminService implements AdminService {
    @Override public void deleteUser(String userId) {
        System.out.println("[Admin] Deleted user: " + userId);
    }
    @Override public String viewAuditLog() { return "audit-log-2024"; }
}

class SecureAdminProxy implements AdminService {
    private final AdminService real;   // ← [has-a] the real subject
    private final String role;         // ← [OwnState] caller's role

    SecureAdminProxy(AdminService real, String role) {
        this.real = real;
        this.role = role;
    }

    @Override
    public void deleteUser(String userId) {
        if (!"ADMIN".equals(role)) {
            throw new SecurityException("Access denied: " + role + " cannot delete users");
        }
        real.deleteUser(userId);   // ← delegate only when authorised
    }

    @Override
    public String viewAuditLog() {
        // VIEWER and above may read logs
        return real.viewAuditLog();
    }
}
```

---

## Comparison: Proxy vs Decorator vs Adapter

| Dimension | Proxy | Decorator | Adapter |
|---|---|---|---|
| **Intent** | Control access / lifecycle | Add stackable behaviour | Change the interface |
| **Interface relationship** | Same as subject | Same as component | Converts Adaptee → Target |
| **Stacking** | Rarely; usually one proxy per subject | Designed to stack many times | Not stacked |
| **Own state** | Yes — role, cache Map, lazy field | Yes — per-decorator config | Stores adaptee |
| **Client awareness** | Client is unaware it holds a proxy | Client is unaware it holds a decorator | Client knows the Target interface |
| **Java examples** | `java.lang.reflect.Proxy`, Hibernate lazy proxies, gRPC stubs | `java.io` stream wrappers, `BufferedReader` | `InputStreamReader` |

---

## When to Use (and When Not To)

**Use Proxy when:**
- Construction is expensive and might not be needed (Virtual Proxy).
- Access needs role or permission checks (Protection Proxy).
- Avoiding redundant computation with a result cache (Caching Proxy).
- Adding logging or metrics without touching the real class (Logging Proxy).

**Do NOT use Proxy when:**
- You need to stack multiple independent features — use Decorator instead.
- The Subject interface has dozens of methods — every proxy must implement them all.
- The subject is a simple data holder — the extra indirection adds nothing.

---

## Interview Q&A

**Q1. What is the intent of the Proxy pattern?**
To provide a surrogate for another object in order to control access to it. The proxy
implements the same interface as the real subject so clients remain unaware of the
substitution.

**Q2. How is Proxy different from Decorator? They look identical structurally.**
Intent and usage pattern. A Proxy *controls* the subject's lifecycle or access — often
managing lazy initialisation, auth checks, or remote representation. It is usually set
up once and is one-to-one with its subject. A Decorator *adds behaviour* and is designed
to be stacked in multiple combinations at runtime. If you find yourself creating layers
of the same type to compose features, it's a Decorator; if you're intercepting and
controlling who/when something is accessed, it's a Proxy.

**Q3. What is a Virtual Proxy? Give a real-world Java example.**
A Virtual Proxy defers instantiation of an expensive object until it is first used.
Hibernate's lazy-loading collections are a real example: when you load an entity with
a `@OneToMany` field, Hibernate inserts a proxy collection object. The actual SQL
`SELECT` fires only when you first iterate the collection.

**Q4. What is a Protection Proxy? Why not put the checks inside the real subject?**
A Protection Proxy enforces access control before delegating. Moving checks into the
real subject violates SRP — the real subject has two reasons to change: its business
logic and its access policy. The proxy separates concerns: the real subject does its
job; the proxy decides who may ask.

**Q5. Can a Proxy have its own state?**
Yes. A Virtual Proxy stores whatever information it needs to construct the real subject
(a filename, a primary key). A Protection Proxy stores the caller's role. A Caching
Proxy stores a `Map<Key,Result>`. This state belongs to the proxy — the real subject
does not know about it.

**Q6. Name real-world Java examples of the Proxy pattern.**
- `java.lang.reflect.Proxy` — creates runtime proxy classes for interfaces.
- Hibernate / JPA lazy-loading proxies — Virtual Proxy.
- Spring `@Transactional`, `@Cacheable`, `@Secured` — implemented via Spring AOP
  using either CGLib subclass proxies or JDK `Proxy` instances.
- gRPC and Java RMI stubs — Remote Proxy for objects on a different JVM.

---

## Common Mistakes

1. **Extending RealSubject instead of composing it.**
   `class ImageProxy extends RealImage` forces the real subject's constructor to run
   during proxy construction — exactly what Virtual Proxy is trying to avoid. Always
   compose; never extend the real subject.

2. **Not implementing the same interface as the real subject.**
   If the proxy exposes a different type, the client must know which one it holds —
   that's not transparent substitution, it's just another class.

3. **Accessing the real subject's internal fields from the proxy.**
   The proxy should interact only through the subject's public interface methods. Direct
   field access tightly couples the proxy to one specific implementation.

4. **Forgetting to propagate exceptions from the real subject.**
   The proxy's pre-call logic (auth check, cache lookup) should not silently swallow
   exceptions thrown by the real subject's method unless it explicitly needs to.

5. **Putting too much into one proxy.**
   A Protection Proxy should only check access; a Virtual Proxy should only manage
   lifecycle. When a single proxy does both, split it — two focused proxies can be
   stacked if necessary (which makes them look like Decorators, intentionally).
