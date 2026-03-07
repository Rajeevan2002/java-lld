# State Design Pattern

## Intent

Allow an object to **alter its behaviour when its internal state changes**. The object will appear to change its class. State-specific behaviour is extracted into separate state classes; the context delegates all actions to the current state object.

---

## The Problem Without State

```java
class TrafficLight {
    private String state = "RED";

    void change() {
        // Growing if/else — add a new state and every method must be updated
        if (state.equals("RED"))    { state = "GREEN";  System.out.println("GO");      }
        else if (state.equals("GREEN"))  { state = "YELLOW"; System.out.println("CAUTION"); }
        else if (state.equals("YELLOW")) { state = "RED";    System.out.println("STOP");    }
    }
}
```

Every new state adds branches to every method. The class grows without bound and violates OCP.

---

## Structure

```
         «interface»
        TrafficLightState
        ┌──────────────────┐
        │ change(ctx): void│
        │ display(): void  │
        └──────────────────┘
                ▲
   ┌────────────┼────────────┐
 RedState   GreenState  YellowState
 change()   change()    change()    ← each knows its successor state
 display()  display()   display()

        TrafficLight  (Context)
        ┌──────────────────────┐
        │ -state: State        │
        │ +changeLight(): void │──▶ delegates to state.change(this)
        │ +setState(s): void   │◀── called by state to transition
        └──────────────────────┘
```

**Participants:**
| Role | Responsibility |
|---|---|
| State (interface) | Declares all actions the context can perform |
| ConcreteState | Implements actions for one specific state; transitions the context |
| Context | Holds the current state; delegates all actions to it; exposes `setState()` |

---

## Minimal Implementation

```java
interface TrafficLightState {
    void change(TrafficLight light);
    String display();
}

class RedState implements TrafficLightState {
    @Override
    public void change(TrafficLight light) {
        System.out.println("RED → GREEN");
        light.setState(new GreenState());   // transition: RED → GREEN
    }
    @Override public String display() { return "RED (Stop)"; }
}

class TrafficLight {
    private TrafficLightState state = new RedState();   // initial state

    void changeLight()              { state.change(this); }
    void setState(TrafficLightState s) { this.state = s; }
    String display()                { return state.display(); }
}
```

---

## State Transition Diagram

```
                 ┌─────────────────┐
       ┌────────▶│    RED State     │◀────────┐
       │         │  display: STOP  │         │
       │         └────────┬────────┘         │
       │               change()              │
       │                  │                  │
       │         ┌────────▼────────┐         │
       │         │   GREEN State   │      change()
    change()     │  display: GO   │         │
       │         └────────┬────────┘         │
       │               change()              │
       │                  │                  │
       │         ┌────────▼────────┐         │
       └─────────│  YELLOW State   │─────────┘
                 │ display: CAUTION│
                 └─────────────────┘
```

---

## Two Transition Strategies

**Option 1 — State transitions itself (used here):**
```java
// ConcreteState calls light.setState(new NextState())
// Pro: each state owns its own successor — easy to modify one transition
// Con: states are coupled to other state classes (they instantiate them)
class GreenState implements TrafficLightState {
    @Override public void change(TrafficLight light) {
        light.setState(new YellowState());   // Green knows it goes to Yellow
    }
}
```

**Option 2 — Context owns all transitions:**
```java
// Context holds a transition table; states don't know successors
// Pro: transition logic is centralised — easy to see all transitions at once
// Con: context grows with each new state; states cannot vary their own transitions
```

Interview tip: mention both options. Option 1 is more common in practice.

---

## State vs Strategy

| Concern | State | Strategy |
|---|---|---|
| What changes? | Behaviour changes as object changes state | Algorithm is swapped by client |
| Who sets the active variant? | Context itself (via state transitions) | External client |
| Are variants aware of each other? | Yes — states transition to successor states | No — strategies are independent |
| Intent | Model lifecycle / state machine | Make algorithms interchangeable |
| Object changes over time? | Yes — context starts in one state, evolves | No — context uses one strategy at a time |

---

## State vs CoR

| Concern | State | Chain of Responsibility |
|---|---|---|
| Request handled by? | Current state only | First matching handler in the chain |
| Handlers linked? | Yes — states know successors | Yes — handlers know next |
| Context evolves? | Yes — state changes per action | No — chain doesn't change context |

---

## Interview Q&A

**Q1: What problem does the State pattern solve?**
A: It eliminates sprawling if/else or switch statements that check an object's state in every method. Instead, each state is its own class that handles every action for that state. Adding a new state adds a new class — no existing code changes.

**Q2: Who is responsible for state transitions — the context or the state?**
A: Either is valid. Commonly, concrete states transition the context by calling `context.setState(new NextState())`. This keeps transition logic close to the state that triggers it. Alternatively, the context can own a transition table — useful when transitions need to be data-driven or configurable.

**Q3: What is the role of `setState()` on the context?**
A: It is the only way state objects mutate the context. It should be package-private or restricted so only state classes (not arbitrary callers) can invoke it. The context's public API never exposes its state field directly.

**Q4: How is State different from Strategy?**
A: In Strategy, the client picks and swaps the algorithm; strategies are independent of each other. In State, the context evolves through states autonomously; states know their successors and transition the context themselves. A traffic light knows its own next state — a sorting algorithm doesn't know about other sorting algorithms.

**Q5: What happens in a terminal state?**
A: A terminal state (e.g., DELIVERED, APPROVED) handles all actions as no-ops or error messages — it does not transition the context. Every action method in a terminal state must still be implemented; it just does nothing meaningful or prints a rejection message.

**Q6: Why extract state into objects instead of using an enum?**
A: An enum encodes the state as a value but not the behaviour. You still need if/switch blocks in the context to dispatch behaviour per enum value. State objects encapsulate both the identity and the behaviour of each state — the context never needs to know which state it's in; it just calls the method.

---

## Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| State logic stays in context (if/else on state field) | Pattern not applied; context grows unbounded | Move each branch into its own ConcreteState |
| State field is public or returned by getter | External code bypasses delegation and checks state directly | Keep state `private`; context only exposes action methods |
| Terminal state has no method implementations | Compile error or NPE if action called in terminal state | Implement every interface method in every state (even no-ops) |
| State transitions in the wrong direction | Lifecycle is violated (e.g., APPROVED → DRAFT) | Draw the state diagram first; encode only valid transitions |
| Mutable state objects shared across context instances | One context's actions corrupt another's state | Create a new state instance per transition, or use stateless singletons if states have no fields |
