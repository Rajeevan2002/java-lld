package com.ramkumar.lld.designpatterns.behavioral.command.practice;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/**
 * Practice Exercise — Command Pattern: Smart Home Lighting
 *
 * <p><b>Scenario B — Encapsulate requests as objects with undo support</b>
 *
 * <p>A smart home app sends commands to lights. Each action must be reversible:
 * the controller keeps a LIFO history of executed commands and can undo the most
 * recent one. The controller must never reference concrete command or light types directly.
 *
 * <p><b>Class hierarchy (do not change the names):</b>
 * <pre>
 *   Command               [Command interface]      ← TODO 1
 *   Light                 [Receiver]               ← TODO 2
 *   TurnOnCommand         [ConcreteCommand 1]      ← TODO 3
 *   TurnOffCommand        [ConcreteCommand 2]      ← TODO 4
 *   SetBrightnessCommand  [ConcreteCommand 3]      ← TODO 5
 *   SmartController       [Invoker]                ← TODO 6
 * </pre>
 *
 * <p><b>Command (interface)</b> (TODO 1):
 * <ul>
 *   <li>{@code execute() → void}</li>
 *   <li>{@code undo() → void}</li>
 * </ul>
 *
 * <p><b>Light (Receiver)</b> (TODO 2):
 * <ul>
 *   <li>Fields: {@code private final String name}; {@code private boolean on} (default {@code false});
 *       {@code private int brightness} (default {@code 50}).</li>
 *   <li>Constructor: {@code Light(String name)}.</li>
 *   <li>{@code turnOn()}: sets {@code on = true};
 *       prints {@code System.out.printf("[%s] Light ON (brightness %d%%)%n", name, brightness)}.</li>
 *   <li>{@code turnOff()}: sets {@code on = false};
 *       prints {@code System.out.printf("[%s] Light OFF%n", name)}.</li>
 *   <li>{@code setBrightness(int level)}: validate {@code level < 0 || level > 100} →
 *       throw {@code new IllegalArgumentException("brightness must be 0–100")};
 *       sets {@code brightness = level};
 *       prints {@code System.out.printf("[%s] Brightness → %d%%%n", name, level)}.</li>
 *   <li>{@code isOn() → boolean}, {@code getBrightness() → int}, {@code getName() → String}
 *       — simple getters.</li>
 * </ul>
 *
 * <p><b>TurnOnCommand (ConcreteCommand 1)</b> (TODO 3):
 * <ul>
 *   <li>Field: {@code private final Light light}.</li>
 *   <li>Constructor: {@code TurnOnCommand(Light light)}.</li>
 *   <li>{@code execute()}: calls {@code light.turnOn()}.</li>
 *   <li>{@code undo()}: calls {@code light.turnOff()}.</li>
 * </ul>
 *
 * <p><b>TurnOffCommand (ConcreteCommand 2)</b> (TODO 4):
 * <ul>
 *   <li>Field: {@code private final Light light}.</li>
 *   <li>Constructor: {@code TurnOffCommand(Light light)}.</li>
 *   <li>{@code execute()}: calls {@code light.turnOff()}.</li>
 *   <li>{@code undo()}: calls {@code light.turnOn()}.</li>
 * </ul>
 *
 * <p><b>SetBrightnessCommand (ConcreteCommand 3)</b> (TODO 5):
 * <ul>
 *   <li>Fields: {@code private final Light light}; {@code private final int newLevel};
 *       {@code private int previousLevel} — NOT {@code final}; captured in {@code execute()}.</li>
 *   <li>Constructor: {@code SetBrightnessCommand(Light light, int newLevel)}.</li>
 *   <li>{@code execute()}: saves {@code previousLevel = light.getBrightness()};
 *       then calls {@code light.setBrightness(newLevel)}.</li>
 *   <li>{@code undo()}: calls {@code light.setBrightness(previousLevel)}.</li>
 * </ul>
 *
 * <p><b>SmartController (Invoker)</b> (TODO 6):
 * <ul>
 *   <li>Field: {@code private final Deque<Command> history} — initialised to
 *       {@code new ArrayDeque<>()}.</li>
 *   <li>Constructor: {@code SmartController()}.</li>
 *   <li>{@code execute(Command c)}: calls {@code c.execute()}; then pushes {@code c}
 *       onto the deque with {@code history.push(c)}.</li>
 *   <li>{@code undo()}: if history is empty →
 *       {@code System.out.println("[Controller] Nothing to undo")};
 *       otherwise pops with {@code history.pop()} and calls {@code c.undo()}.</li>
 *   <li>{@code historySize() → int}: returns {@code history.size()}.</li>
 * </ul>
 *
 * <p><b>Design constraints:</b>
 * <ul>
 *   <li>{@code SmartController} must reference only {@code Command} (interface) —
 *       never {@code TurnOnCommand}, {@code TurnOffCommand}, or {@code SetBrightnessCommand}.</li>
 *   <li>{@code previousLevel} in {@code SetBrightnessCommand} must be captured in
 *       {@code execute()}, NOT in the constructor — the light's brightness may change
 *       between construction and execution.</li>
 *   <li>{@code previousLevel} must NOT be declared {@code final}.</li>
 *   <li>No {@code instanceof} or type-checking anywhere in {@code SmartController}.</li>
 * </ul>
 */
public class SmartHomePractice {

    // ── Command interface ──────────────────────────────────────────────────────

    interface Command {
        void execute();
        void undo();
    }

    // ── Receiver ───────────────────────────────────────────────────────────────

    static class Light {
        private final String name;
        private boolean on;
        private int brightness = 50;

        Light(String name){
            this.name = name;
        }

        public void turnOn(){
            on = true;
            System.out.printf("[%s] Light ON (brightness %d%%)%n", name, brightness);
        }

        public void turnOff(){
            on = false;
            System.out.printf("[%s] Light OFF%n", name);
        }

        public void setBrightness(int level) {
            if(level < 0 || level > 100){
                throw new IllegalArgumentException("brightness must be 0-100");
            }
            brightness = level;
            System.out.printf("[%s] Brightness → %d%%%n", name, level);
        }

        public boolean isOn() { return on;}
        public int getBrightness() { return brightness; }
        public String getName() { return name; }
    }

    // ── ConcreteCommand 1 ──────────────────────────────────────────────────────

    static class TurnOnCommand implements Command {
        private final Light light;
        TurnOnCommand(Light light){
            this.light = light;
        }

        @Override
        public void execute() {
            light.turnOn();
        }

        @Override
        public void undo() {
            light.turnOff();
        }
    }

    // ── ConcreteCommand 2 ──────────────────────────────────────────────────────


    static class TurnOffCommand implements Command {
        private final Light light;
        TurnOffCommand(Light light){
            this.light = light;
        }

        @Override
        public void execute() {
            light.turnOff();
        }

        @Override
        public void undo() {
            light.turnOn();
        }

    }

    // ── ConcreteCommand 3 ──────────────────────────────────────────────────────

    static class SetBrightnessCommand implements Command {
        private final Light light;
        private final int newLevel;
        private int previousLevel;

        SetBrightnessCommand(Light light, int newLevel){
            this.light = light;
            this.newLevel = newLevel;
        }

        @Override
        public void execute() {
            previousLevel = light.getBrightness();
            light.setBrightness(newLevel);
        }

        @Override
        public void undo() {
            light.setBrightness(previousLevel);
        }
    }

    // ── Invoker ────────────────────────────────────────────────────────────────

    // ── TODO 6: Implement SmartController (Invoker)
    //    Private final field: history — Deque<Command> initialised to new ArrayDeque<>()
    //    Constructor: SmartController()
    //    execute(Command c):
    //      Call c.execute()
    //      Push c onto history with history.push(c)   ← push = addFirst (LIFO)
    //    undo():
    //      If history.isEmpty():
    //        Print: System.out.println("[Controller] Nothing to undo")
    //        Return
    //      Otherwise:
    //        Pop: Command c = history.pop()            ← pop = removeFirst (most recent)
    //        Call c.undo()
    //    historySize() → int: returns history.size()
    //    NOTE: SmartController must hold Deque<Command> — never a concrete command type.
    //    NOTE: No instanceof checks anywhere in this class.

    static class SmartController {
        private final Deque<Command> history;

        SmartController() {
            this.history = new ArrayDeque<>();
        }

        public void execute(Command c){
            c.execute();
            history.push(c);
        }

        public void undo() {
            if(history.isEmpty()){
                System.out.println("[Controller] Nothing to undo");
                return;
            }

            Command c = history.pop();
            c.undo();

        }

        public int historySize(){
            return history.size();
        }
    }

    // ── DO NOT MODIFY — test harness ──────────────────────────────────────────
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: Light receiver — turnOn and turnOff (uncomment after TODO 2) ──────────
         Light kitchen = new Light("Kitchen");
         System.out.println("initially on: " + kitchen.isOn());       // expected: false
         System.out.println("default brightness: " + kitchen.getBrightness()); // expected: 50
         kitchen.turnOn();    // expected: [Kitchen] Light ON (brightness 50%)
         System.out.println("on after turnOn: " + kitchen.isOn());    // expected: true
         kitchen.turnOff();   // expected: [Kitchen] Light OFF
         System.out.println("on after turnOff: " + kitchen.isOn());   // expected: false

        // ── Test 2: Light.setBrightness — normal and out-of-range (uncomment after TODO 2) ─
         Light lamp = new Light("Lamp");
         lamp.setBrightness(80);   // expected: [Lamp] Brightness → 80%
         System.out.println("brightness: " + lamp.getBrightness());   // expected: 80
         try {
             lamp.setBrightness(150);
             System.out.println("Test 2 — FAILED (no exception)");
         } catch (IllegalArgumentException e) {
             System.out.println("Test 2 — out-of-range: "
                 + ("brightness must be 0–100".equals(e.getMessage()) ? "PASSED" : "FAILED (msg: " + e.getMessage() + ")"));
         }

        // ── Test 3: TurnOnCommand execute + undo (uncomment after TODOs 2–3) ───────────────
         Light living = new Light("Living Room");
         SmartController ctrl = new SmartController();
         ctrl.execute(new TurnOnCommand(living));  // expected: [Living Room] Light ON (brightness 50%)
         System.out.println("on: " + living.isOn());   // expected: true
         ctrl.undo();   // expected: [Living Room] Light OFF
         System.out.println("on after undo: " + living.isOn());  // expected: false

        // ── Test 4: TurnOffCommand execute + undo (uncomment after TODOs 2–4) ─────────────
         Light bed = new Light("Bedroom");
         SmartController ctrl4 = new SmartController();
         bed.turnOn();    // set initial state to ON
         ctrl4.execute(new TurnOffCommand(bed));   // expected: [Bedroom] Light OFF
         System.out.println("on: " + bed.isOn());  // expected: false
         ctrl4.undo();    // expected: [Bedroom] Light ON (brightness 50%)
         System.out.println("on after undo: " + bed.isOn());  // expected: true

        // ── Test 5: SetBrightnessCommand — previousLevel captured in execute() ────────────
         Light studio = new Light("Studio");
         SmartController ctrl5 = new SmartController();
         studio.setBrightness(30);                               // set initial brightness to 30
         SetBrightnessCommand setBright = new SetBrightnessCommand(studio, 90);
         studio.setBrightness(60);   // change brightness AFTER constructing the command
         ctrl5.execute(setBright);   // expected: [Studio] Brightness → 90%
         System.out.println("brightness: " + studio.getBrightness());  // expected: 90
         ctrl5.undo();               // must restore 60 (not 30) — captured at execute time
         System.out.println("after undo brightness: " + studio.getBrightness());  // expected: 60

        // ── Test 6: LIFO undo order across mixed commands (uncomment after TODO 6) ─────────
         Light hall = new Light("Hall");
         SmartController ctrl6 = new SmartController();
         ctrl6.execute(new TurnOnCommand(hall));          // history: [TurnOn]
         ctrl6.execute(new SetBrightnessCommand(hall, 70)); // history: [SetBrightness, TurnOn]
         ctrl6.execute(new TurnOffCommand(hall));         // history: [TurnOff, SetBrightness, TurnOn]
         System.out.println("history size: " + ctrl6.historySize());  // expected: 3
         ctrl6.undo();   // undoes TurnOff → expected: [Hall] Light ON (brightness 70%)
         ctrl6.undo();   // undoes SetBrightness → expected: [Hall] Brightness → 50%
         ctrl6.undo();   // undoes TurnOn → expected: [Hall] Light OFF
         System.out.println("history after 3 undos: " + ctrl6.historySize());  // expected: 0

        // ── Test 7: Undo on empty history prints message (uncomment after TODO 6) ──────────
         SmartController ctrl7 = new SmartController();
         ctrl7.undo();   // expected: [Controller] Nothing to undo
         Light spare = new Light("Spare");
         ctrl7.execute(new TurnOnCommand(spare));
         ctrl7.undo();   // undoes TurnOn
         ctrl7.undo();   // history empty again → expected: [Controller] Nothing to undo

        // ── Test 8: Command is queued and run later (unbounded queue use) ─────────────────
         Light garage = new Light("Garage");
         SmartController ctrl8 = new SmartController();
         Command[] schedule = {
             new TurnOnCommand(garage),
             new SetBrightnessCommand(garage, 100),
             new SetBrightnessCommand(garage, 20),
             new TurnOffCommand(garage)
         };
         for (Command c : schedule) ctrl8.execute(c);
         System.out.println("after schedule, history: " + ctrl8.historySize());  // expected: 4
         System.out.println("final state on: " + garage.isOn());  // expected: false
         ctrl8.undo();  // undoes TurnOff → [Garage] Light ON (brightness 20%)
         ctrl8.undo();  // undoes SetBrightness(20) → [Garage] Brightness → 100%
         System.out.println("history left: " + ctrl8.historySize());  // expected: 2
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HINTS — read only if stuck
    // ════════════════════════════════════════════════════════════════════════════

    // HINT 1 (Gentle):
    //   Each action (turn on, turn off, change brightness) needs to be stored as an
    //   object so the controller can queue it and reverse it later. Think about what
    //   information each action needs to carry — both the "what to do" and the
    //   "how to get back to where we were". The controller should never need to know
    //   which specific type of action it is running.

    // HINT 2 (Direct):
    //   Use the Command pattern.
    //   Command is an interface with execute() and undo().
    //   TurnOnCommand, TurnOffCommand, SetBrightnessCommand each implement it and hold
    //   a reference to the Light receiver.
    //   SetBrightnessCommand must capture previousLevel = light.getBrightness() at the
    //   START of execute() — before calling setBrightness() — so undo() can restore it.
    //   SmartController holds a Deque<Command> history; execute() calls c.execute() then
    //   history.push(c); undo() calls history.pop() then c.undo().

    // HINT 3 (Near-solution skeleton — class outlines only, no method bodies):
    //
    //   interface Command {
    //       void execute();
    //       void undo();
    //   }
    //
    //   static class Light {
    //       private final String name;
    //       private boolean on;          // default false
    //       private int brightness;      // default 50
    //       Light(String name) { ... }
    //       void turnOn() { ... }
    //       void turnOff() { ... }
    //       void setBrightness(int level) { ... }   // validate 0–100
    //       boolean isOn() { ... }
    //       int getBrightness() { ... }
    //       String getName() { ... }
    //   }
    //
    //   static class TurnOnCommand implements Command {
    //       private final Light light;
    //       TurnOnCommand(Light light) { ... }
    //       @Override public void execute() { ... }
    //       @Override public void undo()    { ... }
    //   }
    //
    //   static class TurnOffCommand implements Command {
    //       private final Light light;
    //       TurnOffCommand(Light light) { ... }
    //       @Override public void execute() { ... }
    //       @Override public void undo()    { ... }
    //   }
    //
    //   static class SetBrightnessCommand implements Command {
    //       private final Light light;
    //       private final int   newLevel;
    //       private int         previousLevel;   // NOT final — assigned in execute()
    //       SetBrightnessCommand(Light light, int newLevel) { ... }
    //       @Override public void execute() { ... }   // save previousLevel FIRST
    //       @Override public void undo()    { ... }
    //   }
    //
    //   static class SmartController {
    //       private final Deque<Command> history = new ArrayDeque<>();
    //       SmartController() { }
    //       void execute(Command c) { ... }   // c.execute() then history.push(c)
    //       void undo()            { ... }   // empty check, then pop + c.undo()
    //       int  historySize()     { ... }
    //   }
}
