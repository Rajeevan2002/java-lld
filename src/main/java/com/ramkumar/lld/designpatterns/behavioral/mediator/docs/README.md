# Mediator Design Pattern

## Intent

Define an object (the mediator) that **encapsulates how a set of objects interact**. Participants communicate only through the mediator — never directly with each other. This reduces many-to-many coupling to many-to-one coupling.

---

## The Problem Without Mediator

```java
// Without mediator: every user knows every other user
class User {
    List<User> peers = new ArrayList<>();   // tightly coupled to all others

    void send(String msg) {
        for (User u : peers) u.receive(msg, this);
    }
    // Adding a new User requires updating every existing User's peer list
}
```

`n` participants → `n*(n-1)` direct connections. Adding a participant requires wiring it to all existing ones. Removing one requires unwiring from all.

---

## Structure

```
     Participant A ──┐
                     │     ┌──────────────┐
     Participant B ──┼────▶│   Mediator   │
                     │     │  (ChatRoom)  │
     Participant C ──┘     └──────────────┘
                                  │
                     ┌────────────┼────────────┐
                     ▼            ▼            ▼
               Participant A  Participant B  Participant C
                (notified)    (notified)    (notified)
```

**Before (n²):** A→B, A→C, B→A, B→C, C→A, C→B — 6 connections for 3 participants.
**After (n):** A→M, B→M, C→M, M→A, M→B, M→C — 6 connections but all through one object.

Adding a fourth participant (D) requires only wiring D to the mediator — A, B, C are untouched.

---

## Minimal Implementation

```java
// Mediator interface
interface ChatMediator {
    void send(String message, User sender);
    void addUser(User user);
}

// Concrete Mediator
class ChatRoom implements ChatMediator {
    private final List<User> users = new ArrayList<>();

    @Override public void addUser(User user) { users.add(user); }

    @Override public void send(String message, User sender) {
        for (User u : users) {
            if (u != sender) {           // don't echo back to sender
                u.receive(message, sender.getName());
            }
        }
    }
}

// Abstract Participant — knows only the mediator, not other participants
abstract class User {
    protected final ChatMediator mediator;
    protected final String name;

    User(String name, ChatMediator mediator) {
        this.name     = name;
        this.mediator = mediator;
        mediator.addUser(this);           // self-register on construction
    }

    void send(String message) {
        mediator.send(message, this);     // delegate to mediator
    }

    abstract void receive(String message, String fromName);
}
```

---

## Key Structural Rules

1. **Participant → Mediator (not → other participants)**
   Every participant holds a reference to the mediator. It never holds references to other participants. All communication goes through the mediator.

2. **Mediator holds the participant list**
   The mediator knows all participants (added via `register()` or `addUser()`). Participants don't know who else is registered.

3. **Self-registration in constructor (optional but common)**
   The participant's constructor calls `mediator.register(this)`. This ensures every participant is automatically in the mediator's list without requiring client setup code.

4. **Sender is excluded from broadcast (usually)**
   When routing a message, the mediator skips the sender: `if (participant != sender)`. Without this, the sender receives its own message.

---

## Mediator vs Observer

| Concern | Mediator | Observer |
|---|---|---|
| Communication direction | Many ↔ Many (through mediator) | One → Many (subject → observers) |
| Who drives interaction? | Participants initiate; mediator routes | Subject notifies all observers |
| Participants know each other? | No — only know mediator | No — observers don't know each other |
| Mediator/Subject knows participants? | Yes — holds participant list | Yes — holds observer list |
| Typical use | Chat rooms, ATC towers, UI form coordination | Event systems, pub/sub, domain events |
| Added complexity? | Higher — mediator encapsulates complex routing | Lower — single broadcast pattern |

**Rule of thumb:** Use Observer when one thing changes and many things react. Use Mediator when many things interact with many other things in complex, context-dependent ways.

---

## Mediator vs Facade

| Concern | Mediator | Facade |
|---|---|---|
| Direction of knowledge | Mediator knows participants; participants know mediator | Facade knows subsystems; clients know facade |
| Do subsystems know facade? | Yes — participants hold mediator reference | No — subsystems are unaware of facade |
| Purpose | Coordinate peers | Simplify a complex subsystem |

---

## Interview Q&A

**Q1: What problem does Mediator solve?**
A: It eliminates tight coupling between participants that need to communicate. Without a mediator, `n` participants need `n*(n-1)` direct connections. The mediator centralises all interaction logic, so adding or removing a participant requires zero changes to existing participants.

**Q2: How is Mediator different from Observer?**
A: Observer is a one-to-many broadcast — one subject notifies many observers of a change. Mediator is many-to-many coordination — participants communicate through a shared object that may apply routing logic, filtering, or transformation. A chat room is a mediator; a stock ticker is more like an observer.

**Q3: Why should participants not hold references to each other?**
A: Direct peer references create `O(n²)` coupling. Adding participant D requires wiring it to all existing participants AND wiring all existing ones to it. With a mediator, D registers with the mediator only — existing participants are untouched.

**Q4: What is the risk of the Mediator pattern?**
A: The mediator can become a "God object" — a single class that knows too much and does too much. If the routing logic grows very complex, the mediator becomes a maintenance burden. Mitigate by keeping each mediator focused on one interaction domain, or by splitting into multiple mediators.

**Q5: Why does the mediator skip the sender when broadcasting?**
A: The sender already knows what it sent — receiving its own message back is redundant and potentially confusing. The filter `if (participant != sender)` uses reference equality (identity), which is correct because each participant is a distinct object.

**Q6: Should participants self-register in their constructor?**
A: It's a design choice. Self-registration (`mediator.register(this)` in constructor) ensures no participant is accidentally unregistered. The trade-off is that the mediator is passed into the constructor, making the participant's creation slightly more coupled. Explicit registration from client code is more visible but requires more setup. Both are acceptable; mention the trade-off.

---

## Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Participant holds references to other participants | Recreates the n² coupling problem | Participant should only hold mediator reference |
| Mediator holds concrete class references (not abstract) | Mediator is coupled to concrete types; new subtypes require mediator changes | Mediator holds `Participant` (abstract/interface) references |
| Sender receives its own broadcast | Redundant self-notification; possible infinite loop if sender reacts to its own message | Skip sender: `if (p != sender)` |
| Mediator becomes a God object | Hard to maintain; every interaction is crammed into one class | Split mediators by interaction domain; keep routing logic simple |
| Confusing Mediator with Facade | Subsystems in Facade don't know the facade exists; participants in Mediator actively use it | Mediator is bidirectional; Facade is unidirectional |
