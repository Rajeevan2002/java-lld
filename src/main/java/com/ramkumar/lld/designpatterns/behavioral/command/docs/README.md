# 3.3.3 Command Pattern

## What Problem Does It Solve?

You need to parameterise objects with operations, queue or log operations, and support
undoable operations — without the invoker knowing what operation it is running or
the receiver knowing who triggered it.

```
Without Command — Invoker coupled to Receiver directly:
  controller.execute("turnOn", light);    // string-based dispatch — no type safety
  controller.execute("setBrightness", light, 80);  // invoker knows receiver API
  controller.undo();   // impossible — no history, no saved state

With Command — Invoker decoupled from Receiver:
  controller.execute(new TurnOnCommand(light));        // invoker calls c.execute()
  controller.execute(new SetBrightnessCommand(light, 80));
  controller.undo();   // pops history, calls c.undo() — invoker knows nothing else
```

Key insight: wrap each request as an object (`Command`). The command object holds the
receiver reference, the parameters, and knows how to reverse itself.

---

## Core Structure

```
Client ──► Invoker ──────────────► «interface» Command
             │                        execute()
             │                        undo()
             │                           ▲
             │                 ┌─────────┼──────────┐
             │                 │         │          │
             │          TurnOnCmd  TurnOffCmd  SetBrightCmd
             │            - light    - light     - light
             │                                   - newLevel
             │                                   - previousLevel
             ▼
           Receiver (Light)
             turnOn()
             turnOff()
             setBrightness(int)
```

Four participants:
| Role | Class | Knows | Does NOT know |
|---|---|---|---|
| **Command** | `Command` interface | — | — |
| **ConcreteCommand** | `TurnOnCommand` etc. | Receiver + parameters | Invoker |
| **Receiver** | `Light` | How to do the work | Commands or Invoker |
| **Invoker** | `SmartController` | Command interface | Receiver or concrete commands |

---

## Undo — The State-Capture Rule

The most common interview follow-up is "how do you implement undo?".

There are two strategies:

**1. Reverse operation** — ConcreteCommand calls the inverse method on the receiver.
```java
class TurnOnCommand implements Command {
    private final Light light;
    public void execute() { light.turnOn(); }
    public void undo()    { light.turnOff(); }  // simple inverse
}
```

**2. Saved state** — ConcreteCommand captures the receiver's state before `execute()`
and restores it in `undo()`. Used when there is no clean inverse method.
```java
class SetBrightnessCommand implements Command {
    private final Light light;
    private final int   newLevel;
    private int         previousLevel;   // NOT final — captured at execute time

    public void execute() {
        previousLevel = light.getBrightness();   // save BEFORE changing
        light.setBrightness(newLevel);
    }
    public void undo() {
        light.setBrightness(previousLevel);      // restore saved state
    }
}
```

**Why capture in `execute()`, not the constructor?**
The receiver's state may change between the time the command is built and the time
it is executed. Capturing in `execute()` records the true "before" state.

---

## Invoker — History Stack

```java
class SmartController {
    private final Deque<Command> history = new ArrayDeque<>();

    void execute(Command c) {
        c.execute();       // run the command
        history.push(c);   // save for undo (Deque as a stack: push = addFirst)
    }

    void undo() {
        if (history.isEmpty()) { System.out.println("Nothing to undo"); return; }
        Command c = history.pop();   // pop = removeFirst (most recent)
        c.undo();
    }
}
```

`Deque<Command>` used as a stack: `push()` = `addFirst()`, `pop()` = `removeFirst()`.
This gives LIFO order — most recently executed command is undone first.

---

## Command vs Other Patterns

| Dimension | Command | Strategy | Chain of Responsibility |
|---|---|---|---|
| **Purpose** | Encapsulate a request; support undo/queue | Swap algorithm at runtime | Pass request along a handler chain |
| **Receiver** | Command holds receiver reference | Strategy IS the algorithm | Handler may forward to next |
| **Undo support** | Yes — built in via history | No | No |
| **Queuing** | Yes — commands are objects | No | No |
| **Knows receiver** | ConcreteCommand does | No one — strategy IS behaviour | Handler may call services |

---

## Interview Q&A

**Q1. What is the Command pattern? State the intent in one sentence.**
Command encapsulates a request as an object, decoupling the sender (invoker) from
the object that performs the action (receiver), and enabling undo, queuing, and logging.

**Q2. Why is the `previousLevel` field not `final` in `SetBrightnessCommand`?**
It is captured at `execute()` time, not at construction time. Making it `final` would
require setting it in the constructor — but the receiver's state at that point may differ
from its state when the command actually runs. `final` would also prevent assignment in
a non-constructor method.

**Q3. What data structure does the Invoker use for undo, and why?**
A `Deque<Command>` used as a stack (`push`/`pop`) gives LIFO order — the most recently
executed command is undone first, which matches user expectations ("undo the last action").

**Q4. How does Command differ from Strategy?**
Strategy replaces the algorithm inside a context (one strategy at a time; no history).
Command wraps a request to a receiver and stores it for undo/replay (many commands
accumulate in history; the invoker is separate from the receiver).

**Q5. What is a Macro Command?**
A `MacroCommand` implements `Command` and holds a `List<Command>`. Its `execute()`
calls `execute()` on each sub-command in order; its `undo()` calls `undo()` in reverse
order. This lets you group many operations into a single undoable unit.

**Q6. How do you implement redo?**
Maintain a second stack: `undoStack` and `redoStack`. On `undo()`, pop from `undoStack`,
call `undo()`, push to `redoStack`. On `redo()`, pop from `redoStack`, call `execute()`,
push back to `undoStack`.

**Q7. Name real-world Java examples of Command.**
- `java.lang.Runnable` — a `Runnable` is a command with no undo; `ExecutorService`
  is the invoker.
- `javax.swing.Action` — a Swing action encapsulates a UI operation with metadata;
  the button is the invoker.
- `java.util.concurrent.Callable` — a command that returns a result.

---

## Common Mistakes

1. **Receiver logic leaks into the Invoker.**
   `controller.execute(new TurnOnCommand(light)); controller.turnOnLight();` — the invoker
   should know only `Command.execute()` and `Command.undo()`, never receiver methods.

2. **Capturing previous state in the constructor, not in `execute()`.**
   `previousLevel = light.getBrightness()` in the constructor records state at creation
   time. If another command changes brightness before this one runs, the saved state is wrong.

3. **`previousLevel` declared `final`.**
   A `final` field must be assigned in a constructor. You cannot assign it in `execute()`.
   Remove `final` from state-capture fields.

4. **Invoker holds concrete command types.**
   `private TurnOnCommand lastOn;` — this defeats the abstraction. The invoker must hold
   `Deque<Command>` and never cast or check command types.

5. **Forgetting to push the command to history after execute.**
   `c.execute()` without `history.push(c)` means `undo()` has nothing to pop — the history
   is always empty and undo silently does nothing.
