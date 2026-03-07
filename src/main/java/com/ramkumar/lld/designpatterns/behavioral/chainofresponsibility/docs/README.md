# Chain of Responsibility Design Pattern

## Intent

Pass a request along a chain of handlers. Each handler either **processes the request** or **forwards it to the next handler**. The sender does not know which handler will ultimately handle the request.

---

## The Problem Without CoR

```java
// Client must know about every handler type and the routing logic
void handleTicket(SupportTicket ticket) {
    if (ticket.getPriority() == LOW)       frontline.resolve(ticket);
    else if (ticket.getPriority() == HIGH) technical.resolve(ticket);
    else                                   critical.resolve(ticket);
    // Adding a new tier forces changes here — violates OCP
}
```

Every new tier or rule change requires modifying the client. The routing logic is tangled with business logic.

---

## Structure

```
         «abstract»
          Handler
        ┌──────────────────────────────┐
        │ - next: Handler              │
        │ + setNext(h): Handler        │ ← returns next for fluent chaining
        │ + handle(req): void          │ ← abstract (or default pass-through)
        │ # passToNext(req): void      │ ← utility: delegate or log "unhandled"
        └──────────────────────────────┘
                     ▲
        ┌────────────┼────────────┐
   ConcreteA    ConcreteB    ConcreteC
   handles      handles      handles
   condition A  condition B  anything
```

**Key relationships:**
- Each handler holds a reference to the **next** handler (same type — polymorphism).
- `setNext()` returns the next handler so the chain can be assembled fluently.
- `passToNext()` is a protected utility — calling it from concrete handlers keeps the forwarding logic in one place.

---

## Minimal Implementation

```java
static abstract class Approver {
    private Approver next;

    // Returns next to enable fluent chaining: a.setNext(b).setNext(c)
    Approver setNext(Approver next) {
        this.next = next;
        return next;
    }

    abstract void approve(PurchaseRequest req);

    // Shared forwarding logic — subclasses call this instead of accessing next directly
    protected void passToNext(PurchaseRequest req) {
        if (next != null) {
            next.approve(req);
        } else {
            System.out.printf("[Chain] No approver for \"%s\" $%.2f%n",
                req.getDescription(), req.getAmount());
        }
    }
}

static class TeamLead extends Approver {
    @Override
    void approve(PurchaseRequest req) {
        if (req.getAmount() <= 1_000.0) {
            System.out.printf("[TeamLead] Approved \"%s\" $%.2f%n",
                req.getDescription(), req.getAmount());
        } else {
            passToNext(req);   // not my call — escalate
        }
    }
}
```

---

## Fluent Chain Assembly

```java
Approver chain = new TeamLead();
chain.setNext(new Manager())
     .setNext(new Director())
     .setNext(new CEO());

chain.approve(new PurchaseRequest("Laptop", 800));     // TeamLead
chain.approve(new PurchaseRequest("Server", 12_000));  // Director
chain.approve(new PurchaseRequest("Campus", 2_000_000)); // CEO
```

`setNext(b)` returns `b`, so each `.setNext()` call operates on the handler just added — the chain extends right-to-left without a temp variable.

---

## Pure vs Impure Chain

| Variant | Behaviour | Example |
|---|---|---|
| **Pure** | Exactly one handler processes each request | Approval by amount — only one tier approves |
| **Impure** | Multiple handlers may act on the same request | Logging — every handler at or above threshold prints |
| **Short-circuit** | First matching handler stops the chain | Auth middleware — fail fast on bad token |

---

## Comparison: CoR vs Strategy vs Command

| Concern | CoR | Strategy | Command |
|---|---|---|---|
| How many handlers act? | One (pure) or many (impure) | Exactly one | Exactly one |
| Who selects the handler? | The chain itself (runtime routing) | Client (sets strategy) | Client (creates command) |
| Handlers linked? | Yes — each knows its next | No — independent | No — independent |
| Undo support? | No | No | Yes (undo/redo) |
| Best for | Routing/escalation pipelines | Swappable algorithms | Encapsulated, undoable actions |

---

## Interview Q&A

**Q1: What problem does Chain of Responsibility solve?**
A: It decouples the sender from the receiver. The sender submits a request to the head of the chain without knowing which handler will process it. Adding or reordering handlers requires zero changes to the sender or to other handlers.

**Q2: Why does `setNext()` return the next handler rather than `void`?**
A: Returning the next handler allows fluent chaining: `a.setNext(b).setNext(c)` — each call returns the handler just added, extending the chain without temporary variables. If it returned `void` or `this`, the caller would need a variable for each handler.

**Q3: What is `passToNext()` and why should it be `protected`?**
A: `passToNext()` is a shared utility on the abstract base class that forwards a request to the next handler (or logs "unhandled" if the chain is exhausted). It's `protected` so concrete handlers can call it, but clients can't invoke it directly — clients call `handle()`/`approve()`, not the forwarding mechanism.

**Q4: What happens when no handler in the chain accepts a request?**
A: In a well-designed chain, `passToNext()` on the last handler finds `next == null` and handles the fallback — typically logging a "no handler found" message or throwing an exception. Silently dropping the request is a bug.

**Q5: How is CoR different from a list of if/else statements?**
A: An if/else chain is baked into a single method — adding a new condition requires modifying that method (violates OCP). CoR externalises each condition into its own class. A new handler is added by inserting a new class and updating the chain assembly — existing handlers are untouched.

**Q6: When would you choose CoR over Strategy?**
A: CoR when the routing decision is runtime and you don't know upfront which handler applies (e.g., approval limits, middleware pipelines). Strategy when you always know exactly one algorithm to use and want to swap it. The key distinction: CoR has linked handlers; Strategy has one interchangeable handler.

**Q7: Can a handler in the chain act AND forward the request?**
A: Yes — this is the impure variant. A logging chain where every handler at or above a severity threshold prints the message and then forwards it is impure CoR. The handler acts, then calls `passToNext()`. Use this only when the requirement explicitly calls for multiple handlers to process the same request.

---

## Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Calling `next.handle()` directly instead of `passToNext()` | NPE when `next` is null; null check duplicated in every subclass | Centralise null check in `protected passToNext()` |
| `setNext()` returns `void` or `this` | Forces caller to use a variable for every handler; can't chain fluently | Return the `next` handler |
| Handler that never calls `passToNext()` | Silently swallows requests it can't handle | Every conditional branch must either handle or forward |
| Handler that always calls `passToNext()` | Handler never processes anything; chain is broken | Exactly one branch handles; else branch forwards |
| Making `next` protected or public | External code can corrupt the chain | Declare `next` as `private`; expose only `setNext()` and `passToNext()` |
| Building the chain inside the handlers | Tight coupling; handlers know about each other | Assemble the chain in the client / main() |
