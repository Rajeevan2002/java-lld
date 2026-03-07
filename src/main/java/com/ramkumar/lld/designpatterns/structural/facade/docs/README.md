# 3.2.2 Facade Pattern

## What Problem Does It Solve?

A subsystem of several classes that work together can become difficult to use — the client must know the right sequence of calls, the right order of initialisation, and the right object to call at each step. This creates tight coupling: every caller is aware of internal details.

The **Facade** provides a single, simplified entry point that hides all of that complexity. The client talks to the Facade; the Facade talks to the subsystem.

```
Without Facade                        With Facade

Client ──► ServiceA                   Client ──► Facade
Client ──► ServiceB                              │
Client ──► ServiceC                              ├──► ServiceA
Client ──► ServiceD  (8 call sites)              ├──► ServiceB
                                                 ├──► ServiceC
                                                 └──► ServiceD  (4 call sites inside Facade)
```

---

## ASCII Structure Diagram

```
┌──────────────┐
│    Client    │ ──────────────────────────► Facade
└──────────────┘                            │  watchMovie(title)
                                            │  endMovie()
                                            │
                        ┌───────────────────┼──────────────────────┐
                        │                   │                      │
                        ▼                   ▼                      ▼
               ┌──────────────┐   ┌──────────────┐       ┌──────────────┐
               │  Subsystem A │   │  Subsystem B │  ...  │  Subsystem N │
               └──────────────┘   └──────────────┘       └──────────────┘

Key: Subsystems do NOT know about the Facade (no back-reference).
     The Facade knows the subsystems; the subsystems do not know the Facade.
```

---

## Code Skeleton

```java
// Subsystems — each focused on its own concern
class Projector {
    public void on()  { System.out.println("[Projector] On");  }
    public void off() { System.out.println("[Projector] Off"); }
    public void setInput(String src) { System.out.printf("[Projector] Input: %s%n", src); }
}

class Amplifier {
    public void on()                { System.out.println("[Amp] On");  }
    public void off()               { System.out.println("[Amp] Off"); }
    public void setVolume(int level){ System.out.printf("[Amp] Volume: %d%n", level); }
}

// Facade — hides the subsystem complexity behind a simple interface
class HomeTheaterFacade {                          // [Facade]
    private final Projector projector;             // [Composition — holds subsystems]
    private final Amplifier amplifier;

    HomeTheaterFacade(Projector p, Amplifier a) {  // [Constructor injection]
        this.projector = p;
        this.amplifier = a;
    }

    public void watchMovie(String title) {         // [SimplifiedInterface]
        projector.on();
        projector.setInput("HDMI");
        amplifier.on();
        amplifier.setVolume(5);
        System.out.printf("[HomeTheater] Movie starting: %s%n", title);
    }

    public void endMovie() {
        amplifier.off();
        projector.off();
        System.out.println("[HomeTheater] Goodnight!");
    }
}
```

---

## Instance Facade vs Static Utility Class

| Dimension | Instance Facade (preferred) | Static Utility Class |
|---|---|---|
| Subsystems injected via | Constructor | Hard-coded inside static methods |
| Testability | Easy — swap real subsystems for test doubles | Hard — static dependencies baked in |
| Multiple configurations | Yes — different Facade instances can use different subsystems | No — one global configuration |
| Follows Dependency Inversion | Yes | No |
| **When to use** | **Always** | Only for pure stateless helpers with no external deps |

---

## Comparison: Facade vs Adapter vs Mediator vs Proxy

| Pattern | Intent | Key difference from Facade |
|---|---|---|
| **Facade** | Simplify access to a complex subsystem | Client ↔ Facade ↔ many subsystems (one-directional simplification) |
| **Adapter** | Make an incompatible interface compatible | Translates one interface; usually wraps one class; applied to legacy code |
| **Mediator** | Manage complex many-to-many interactions between objects | Objects communicate *through* the mediator; mediator is two-way; objects know it exists |
| **Proxy** | Control access to a single object | One-to-one; adds behaviour (lazy init, auth, caching) but doesn't simplify multiple subsystems |

**Memory aid:** Adapter fixes *incompatibility*. Facade fixes *complexity*. Mediator fixes *coupling between peers*. Proxy fixes *access control*.

---

## When to Use (and When Not to)

**Use Facade when:**
- A library or subsystem requires many steps that always happen in the same order
- You want a single, stable entry point that insulates clients from subsystem changes
- You are building a layered architecture and want a clean layer boundary

**Do NOT use Facade when:**
- The subsystem is already simple (one or two classes) — it adds overhead for no benefit
- Different callers need different subsets of the subsystem — a Facade forces the same simplified view on everyone
- You need to expose all subsystem features to all clients — the Facade would just re-expose everything

---

## Interview Q&A

**Q1. What is the intent of the Facade pattern?**
To provide a unified, simplified interface to a complex subsystem. The client gets a convenient entry point; the subsystem internals are hidden and free to change without affecting the client.

**Q2. Does Facade prevent clients from using subsystem classes directly?**
No. The Facade is a convenience layer, not an access-control mechanism. Clients *can* bypass it and talk to subsystems directly if they need fine-grained control. Facade is opt-in simplification, not forced encapsulation. (Proxy is the pattern for enforced access control.)

**Q3. What is the difference between Facade and Adapter?**
Adapter makes an incompatible interface work with an existing one — it's applied retroactively to legacy or third-party code. Facade simplifies a complex interface that is already compatible — it's applied proactively to reduce coupling. Adapter changes an interface; Facade wraps multiple interfaces behind a new, simpler one.

**Q4. What is the difference between Facade and Mediator?**
Both sit between other objects. Facade is one-directional — client calls Facade, which calls subsystems; subsystems do not know the Facade exists. Mediator is bidirectional — components send messages through the mediator, and the mediator routes them back. Facade is about simplifying a client's interaction with a subsystem; Mediator is about managing complex communication between peers.

**Q5. Should Facade fields be private final? Why?**
Yes. Subsystem references set in the constructor should be `private final`. `private` prevents subsystems from being accessed or replaced from outside, keeping the Facade in control. `final` guarantees the reference never changes after construction, making the Facade thread-safe for reads and easier to reason about.

**Q6. Name real-world Java examples of the Facade pattern.**
- `javax.faces.context.FacesContext` — wraps HTTP request/response, lifecycle, and rendering into one object
- SLF4J's `LoggerFactory` — a facade over multiple logging backends (Logback, Log4j, JUL)
- Spring's `JdbcTemplate` — wraps JDBC's `Connection`, `PreparedStatement`, `ResultSet` management
- `java.net.URL.openStream()` — a facade that opens a connection, builds a stream, and handles redirects in one call

---

## Common Mistakes

1. **Putting business logic in the Facade.** The Facade's only job is to orchestrate. Validation, transformation, and decisions belong in the service layer or domain layer, not in the Facade.

2. **Making Facade methods too granular.** If `HomeTheaterFacade` exposes `turnOnProjector()`, it is no longer a simplification — it is just a pass-through. Facade methods should represent *complete use cases* (`watchMovie`, `bookTrip`), not single subsystem calls.

3. **Creating a God Facade.** One Facade per feature area or bounded context. A `SystemFacade` that wraps your entire application is an anti-pattern.

4. **Forgetting that subsystems remain accessible.** Facade does not lock down the subsystem. If the team needs to prevent direct subsystem access, use a package-private access modifier or a module boundary, not a Facade.

5. **Static Facade methods.** Static methods make subsystems impossible to swap in tests. Always inject subsystems via the constructor.
