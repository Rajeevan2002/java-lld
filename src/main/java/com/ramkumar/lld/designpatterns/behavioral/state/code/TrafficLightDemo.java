package com.ramkumar.lld.designpatterns.behavioral.state.code;

/**
 * Worked Example — State Pattern: Traffic Light
 *
 * <p><b>Scenario A — Cyclic state machine with autonomous transitions</b>
 *
 * <p>A traffic light cycles through RED → GREEN → YELLOW → RED.
 * Each state knows its successor and transitions the context when {@code changeLight()}
 * is called. The context ({@code TrafficLight}) never inspects which state it is in —
 * it delegates every action to the current state object.
 *
 * <p>Participants:
 * <ul>
 *   <li>{@code TrafficLightState} — [State interface] declares all actions</li>
 *   <li>{@code RedState}, {@code GreenState}, {@code YellowState} — [ConcreteStates]</li>
 *   <li>{@code TrafficLight} — [Context] holds current state; delegates to it</li>
 * </ul>
 */
public class TrafficLightDemo {

    // ── [StateInterface] — declares every action the context supports ──────────
    // All three action methods are declared here. Every ConcreteState must implement
    // all of them — even if the implementation is a no-op or an error message.
    interface TrafficLightState {
        // [Transition] Moves the light to the next state in the cycle
        void change(TrafficLight light);

        // [Query] Returns a human-readable label for this state
        String display();
    }

    // ── [Context] — owns the state; delegates all actions to it ───────────────
    static class TrafficLight {

        // [StateField] private — external callers never inspect it directly.
        // All state-dependent behaviour is reached via action methods, not by
        // reading this field and switching on it.
        private TrafficLightState state;

        TrafficLight() {
            this.state = new RedState();   // [InitialState] traffic lights start at RED
        }

        // [PublicAction] Context exposes this; delegates entirely to state
        void changeLight() {
            state.change(this);   // state decides what "change" means and what comes next
        }

        // [Query] Delegates to state — context doesn't know the label itself
        String display() {
            return state.display();
        }

        // [StateSetter] Package-private — only state classes (in this file) call it.
        // External callers cannot force a transition; transitions happen via actions.
        void setState(TrafficLightState state) {
            this.state = state;
        }
    }

    // ── [ConcreteState 1] RED — Stop ──────────────────────────────────────────
    static class RedState implements TrafficLightState {

        // [Transition] RED → GREEN: state knows its own successor
        // It calls context.setState() — the ONLY way to mutate the context's state.
        @Override
        public void change(TrafficLight light) {
            System.out.println("RED → GREEN");
            light.setState(new GreenState());   // new instance per transition
        }

        @Override
        public String display() { return "RED (Stop)"; }
    }

    // ── [ConcreteState 2] GREEN — Go ──────────────────────────────────────────
    static class GreenState implements TrafficLightState {

        @Override
        public void change(TrafficLight light) {
            System.out.println("GREEN → YELLOW");
            light.setState(new YellowState());
        }

        @Override
        public String display() { return "GREEN (Go)"; }
    }

    // ── [ConcreteState 3] YELLOW — Caution ────────────────────────────────────
    static class YellowState implements TrafficLightState {

        // [Cyclic] YELLOW completes the cycle back to RED
        @Override
        public void change(TrafficLight light) {
            System.out.println("YELLOW → RED");
            light.setState(new RedState());
        }

        @Override
        public String display() { return "YELLOW (Caution)"; }
    }

    // ── main() ────────────────────────────────────────────────────────────────
    public static void main(String[] args) {

        TrafficLight light = new TrafficLight();

        // ── [InitialState] Starts at RED ──────────────────────────────────────
        System.out.println("Initial: " + light.display());   // RED (Stop)

        // ── [Cycle] One full RED → GREEN → YELLOW → RED cycle ─────────────────
        System.out.println("\n── Full cycle ──");
        light.changeLight();
        System.out.println("Now: " + light.display());       // GREEN (Go)

        light.changeLight();
        System.out.println("Now: " + light.display());       // YELLOW (Caution)

        light.changeLight();
        System.out.println("Now: " + light.display());       // RED (Stop)

        light.changeLight();
        System.out.println("Now: " + light.display());       // GREEN (Go) — cycle repeats

        // ── [NoConditionals] Context never inspects state — pure delegation ───
        // The following loop calls changeLight() 9 more times.
        // There is no if/else or switch on state anywhere in TrafficLight.
        System.out.println("\n── 9 more changes (context never inspects state) ──");
        for (int i = 0; i < 9; i++) {
            light.changeLight();
            System.out.println("  " + light.display());
        }

        // ── [Polymorphism] Context typed as TrafficLight; state is opaque ─────
        System.out.println("\n── New light starts at RED regardless of external knowledge ──");
        TrafficLight light2 = new TrafficLight();
        System.out.println("light2 initial: " + light2.display());   // RED (Stop)
        light2.changeLight();
        System.out.println("light2 after change: " + light2.display()); // GREEN (Go)
    }
}
