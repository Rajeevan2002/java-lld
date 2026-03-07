package com.ramkumar.lld.designpatterns.behavioral.command.results;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Reference solution — Command Pattern: Smart Home Lighting
 *
 * <p>Key decisions vs common mistakes:
 * <ul>
 *   <li>{@code previousLevel} is NOT {@code final} and is captured in {@code execute()},
 *       not the constructor. The receiver's state may change between construction and
 *       execution — the snapshot must be taken at the moment the action actually runs.</li>
 *   <li>Exception message uses an en dash (–), not a plain hyphen (-)
 *       — exact-match string contracts matter.</li>
 *   <li>{@code history.push(c)} comes AFTER {@code c.execute()} — if execute throws,
 *       the command should not enter history (it was never applied).</li>
 *   <li>{@code SmartController} holds {@code Deque<Command>} only — never a concrete type.</li>
 * </ul>
 */
public class CommandReference {

    // ── [Command interface] ───────────────────────────────────────────────────
    interface Command {
        void execute();
        void undo();
    }

    // ── [Receiver] ────────────────────────────────────────────────────────────
    static class Light {

        private final String name;
        private boolean on;          // default false
        private int brightness = 50; // default 50

        Light(String name) {
            this.name = name;
        }

        void turnOn() {
            on = true;
            System.out.printf("[%s] Light ON (brightness %d%%)%n", name, brightness);
        }

        void turnOff() {
            on = false;
            System.out.printf("[%s] Light OFF%n", name);
        }

        void setBrightness(int level) {
            // [ExactMessage] En dash (–), not a hyphen (-) — the spec is an exact contract.
            if (level < 0 || level > 100) {
                throw new IllegalArgumentException("brightness must be 0–100");
            }
            brightness = level;
            System.out.printf("[%s] Brightness → %d%%%n", name, level);
        }

        boolean isOn()          { return on; }
        int     getBrightness() { return brightness; }
        String  getName()       { return name; }
    }

    // ── [ConcreteCommand 1] — reverse-operation undo ──────────────────────────
    static class TurnOnCommand implements Command {

        private final Light light;

        TurnOnCommand(Light light) { this.light = light; }

        @Override public void execute() { light.turnOn(); }
        @Override public void undo()    { light.turnOff(); }
    }

    // ── [ConcreteCommand 2] — reverse-operation undo ──────────────────────────
    static class TurnOffCommand implements Command {

        private final Light light;

        TurnOffCommand(Light light) { this.light = light; }

        @Override public void execute() { light.turnOff(); }
        @Override public void undo()    { light.turnOn(); }
    }

    // ── [ConcreteCommand 3] — saved-state undo ────────────────────────────────
    static class SetBrightnessCommand implements Command {

        private final Light light;
        private final int   newLevel;
        // [NotFinal] previousLevel must NOT be final — it is assigned in execute(),
        // not in the constructor. final fields can only be assigned in constructors.
        private int previousLevel;

        SetBrightnessCommand(Light light, int newLevel) {
            this.light    = light;
            this.newLevel = newLevel;
        }

        @Override
        public void execute() {
            // [CaptureFirst] Save the current brightness BEFORE changing it.
            // If captured in the constructor instead, the snapshot would record the
            // state at creation time — which may be stale by the time this runs.
            previousLevel = light.getBrightness();
            light.setBrightness(newLevel);
        }

        @Override
        public void undo() {
            light.setBrightness(previousLevel);
        }
    }

    // ── [Invoker] ─────────────────────────────────────────────────────────────
    static class SmartController {

        // [DequeAsStack] push() = addFirst (LIFO); pop() = removeFirst (most recent first).
        // Typed as Deque<Command> — invoker knows nothing about TurnOnCommand or Light.
        private final Deque<Command> history = new ArrayDeque<>();

        void execute(Command c) {
            c.execute();      // [RunFirst] execute before push — if c.execute() throws,
            history.push(c);  // the command never enters history (was never applied).
        }

        void undo() {
            if (history.isEmpty()) {
                System.out.println("[Controller] Nothing to undo");
                return;
            }
            Command c = history.pop();
            c.undo();
        }

        int historySize() { return history.size(); }
    }

    // ── Reference main() ──────────────────────────────────────────────────────
    public static void main(String[] args) {

        // ── Test 1: Light receiver — turnOn and turnOff ───────────────────────
        Light kitchen = new Light("Kitchen");
        System.out.println("initially on: " + kitchen.isOn());
        System.out.println("default brightness: " + kitchen.getBrightness());
        kitchen.turnOn();
        System.out.println("on after turnOn: " + kitchen.isOn());
        kitchen.turnOff();
        System.out.println("on after turnOff: " + kitchen.isOn());

        // ── Test 2: setBrightness — normal and out-of-range ────────────────────
        Light lamp = new Light("Lamp");
        lamp.setBrightness(80);
        System.out.println("brightness: " + lamp.getBrightness());
        try {
            lamp.setBrightness(150);
            System.out.println("Test 2 — FAILED (no exception)");
        } catch (IllegalArgumentException e) {
            System.out.println("Test 2 — out-of-range: "
                + ("brightness must be 0–100".equals(e.getMessage()) ? "PASSED" : "FAILED (msg: " + e.getMessage() + ")"));
        }

        // ── Test 3: TurnOnCommand execute + undo ──────────────────────────────
        Light living = new Light("Living Room");
        SmartController ctrl = new SmartController();
        ctrl.execute(new TurnOnCommand(living));
        System.out.println("on: " + living.isOn());
        ctrl.undo();
        System.out.println("on after undo: " + living.isOn());

        // ── Test 4: TurnOffCommand execute + undo ─────────────────────────────
        Light bed = new Light("Bedroom");
        SmartController ctrl4 = new SmartController();
        bed.turnOn();
        ctrl4.execute(new TurnOffCommand(bed));
        System.out.println("on: " + bed.isOn());
        ctrl4.undo();
        System.out.println("on after undo: " + bed.isOn());

        // ── Test 5: SetBrightnessCommand — previousLevel captured in execute() ─
        Light studio = new Light("Studio");
        SmartController ctrl5 = new SmartController();
        studio.setBrightness(30);
        SetBrightnessCommand setBright = new SetBrightnessCommand(studio, 90);
        studio.setBrightness(60);    // changes brightness AFTER construction
        ctrl5.execute(setBright);    // previousLevel captured as 60 (not 30)
        System.out.println("brightness: " + studio.getBrightness());
        ctrl5.undo();                // must restore 60, not 30
        System.out.println("after undo brightness: " + studio.getBrightness());

        // ── Test 6: LIFO undo order across mixed commands ─────────────────────
        Light hall = new Light("Hall");
        SmartController ctrl6 = new SmartController();
        ctrl6.execute(new TurnOnCommand(hall));
        ctrl6.execute(new SetBrightnessCommand(hall, 70));
        ctrl6.execute(new TurnOffCommand(hall));
        System.out.println("history size: " + ctrl6.historySize());
        ctrl6.undo();
        ctrl6.undo();
        ctrl6.undo();
        System.out.println("history after 3 undos: " + ctrl6.historySize());

        // ── Test 7: Undo on empty history prints message ───────────────────────
        SmartController ctrl7 = new SmartController();
        ctrl7.undo();
        Light spare = new Light("Spare");
        ctrl7.execute(new TurnOnCommand(spare));
        ctrl7.undo();
        ctrl7.undo();

        // ── Test 8: Command batch schedule ────────────────────────────────────
        Light garage = new Light("Garage");
        SmartController ctrl8 = new SmartController();
        Command[] schedule = {
            new TurnOnCommand(garage),
            new SetBrightnessCommand(garage, 100),
            new SetBrightnessCommand(garage, 20),
            new TurnOffCommand(garage)
        };
        for (Command c : schedule) ctrl8.execute(c);
        System.out.println("after schedule, history: " + ctrl8.historySize());
        System.out.println("final state on: " + garage.isOn());
        ctrl8.undo();
        ctrl8.undo();
        System.out.println("history left: " + ctrl8.historySize());

        // ── Test 9 (catches the most common mistake: previousLevel in constructor) ──
        // If previousLevel were captured in the constructor, re-executing the same
        // command would restore the ORIGINAL construction-time brightness rather than
        // the brightness that existed just before each execution.
        System.out.println("\n── Test 9: re-execution captures fresh state each time ──");
        Light desk = new Light("Desk");
        SmartController ctrl9 = new SmartController();

        // Step 1: execute setBrightness(80) — previousLevel captured as 50
        SetBrightnessCommand cmd = new SetBrightnessCommand(desk, 80);
        ctrl9.execute(cmd);   // previousLevel = 50, brightness = 80
        System.out.println("brightness after execute: " + desk.getBrightness()); // 80
        ctrl9.undo();         // restores 50
        System.out.println("brightness after undo: " + desk.getBrightness());    // 50

        // Step 2: re-execute the SAME command — previousLevel must be re-captured as 50
        ctrl9.execute(cmd);   // previousLevel = 50 (re-captured fresh), brightness = 80
        System.out.println("brightness after re-execute: " + desk.getBrightness()); // 80
        ctrl9.undo();         // must still restore 50
        System.out.println("brightness after second undo: " + desk.getBrightness()); // 50

        // Step 3: manually change brightness then re-execute — previousLevel = 30 now
        desk.setBrightness(30);
        ctrl9.execute(cmd);   // previousLevel captured as 30 (fresh), brightness = 80
        ctrl9.undo();         // must restore 30, not 50 (the original construction value)
        System.out.println("Test 9 — re-execution captures fresh state: "
            + (desk.getBrightness() == 30 ? "PASSED" : "FAILED (got: " + desk.getBrightness() + ")"));
    }
}
