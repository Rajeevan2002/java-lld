package com.ramkumar.lld.designpatterns.structural.facade.code;

// ─────────────────────────────────────────────────────────────────────────────
// Facade Pattern — Scenario A: Home Theater System
//
// Problem: Watching a movie requires coordinating five independent subsystems
//          (Projector, Amplifier, StreamingPlayer, TheaterLights, Screen) in the
//          correct sequence. Exposing all that to the client creates tight coupling
//          and forces every caller to know the right order of 8+ method calls.
//
// Solution: HomeTheaterFacade wraps all five subsystems and exposes two simple
//           methods — watchMovie() and endMovie() — that handle the orchestration.
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// [Subsystem 1] — Projector; knows nothing about the Facade or other subsystems
// ─────────────────────────────────────────────────────────────────────────────
class Projector {
    public void on()                    { System.out.println("  [Projector] Powered on"); }
    public void off()                   { System.out.println("  [Projector] Powered off"); }
    public void setInput(String source) { System.out.printf("  [Projector] Input set to %s%n", source); }
}

// ─────────────────────────────────────────────────────────────────────────────
// [Subsystem 2] — Amplifier
// ─────────────────────────────────────────────────────────────────────────────
class Amplifier {
    public void on()                { System.out.println("  [Amplifier] Powered on"); }
    public void off()               { System.out.println("  [Amplifier] Powered off"); }
    public void setVolume(int level){ System.out.printf("  [Amplifier] Volume set to %d%n", level); }
}

// ─────────────────────────────────────────────────────────────────────────────
// [Subsystem 3] — StreamingPlayer
// ─────────────────────────────────────────────────────────────────────────────
class StreamingPlayer {
    public void login(String account){ System.out.printf("  [Streaming] Logged in as %s%n", account); }
    public void play(String title)   { System.out.printf("  [Streaming] Playing \"%s\"%n", title); }
    public void stop()               { System.out.println("  [Streaming] Playback stopped"); }
}

// ─────────────────────────────────────────────────────────────────────────────
// [Subsystem 4] — TheaterLights
// ─────────────────────────────────────────────────────────────────────────────
class TheaterLights {
    public void dim(int percent){ System.out.printf("  [Lights] Dimmed to %d%%%n", percent); }
    public void on()            { System.out.println("  [Lights] Full brightness"); }
}

// ─────────────────────────────────────────────────────────────────────────────
// [Subsystem 5] — Screen
// ─────────────────────────────────────────────────────────────────────────────
class Screen {
    public void lower(){ System.out.println("  [Screen] Lowered"); }
    public void raise(){ System.out.println("  [Screen] Raised"); }
}

// ─────────────────────────────────────────────────────────────────────────────
// [Facade] — HomeTheaterFacade
// Holds all five subsystems; exposes two high-level use-case methods.
// The client never imports or instantiates a subsystem class directly.
// ─────────────────────────────────────────────────────────────────────────────
class HomeTheaterFacade {

    // [Composition] — each subsystem is a private final field; injected, never replaced
    private final Projector       projector;
    private final Amplifier       amplifier;
    private final StreamingPlayer player;
    private final TheaterLights   lights;
    private final Screen          screen;

    // [ConstructorInjection] — subsystems are provided by the caller, not created here.
    //                          This keeps the Facade testable and the dependencies visible.
    HomeTheaterFacade(Projector projector, Amplifier amplifier,
                      StreamingPlayer player, TheaterLights lights, Screen screen) {
        this.projector = projector;
        this.amplifier = amplifier;
        this.player    = player;
        this.lights    = lights;
        this.screen    = screen;
    }

    // [SimplifiedInterface] — one call replaces 8+ subsystem calls in the correct sequence.
    //                         The client doesn't need to know what happens or in what order.
    public void watchMovie(String title) {
        System.out.println("[HomeTheater] Getting ready to watch a movie...");
        lights.dim(10);             // dim first — so you can see the screen lower
        screen.lower();             // screen before projector so nothing is missed
        projector.on();
        projector.setInput("HDMI");
        amplifier.on();
        amplifier.setVolume(5);     // safe default volume; not too loud at startup
        player.login("user@home.com");
        player.play(title);         // start playback last — everything must be ready
        System.out.printf("[HomeTheater] Enjoy \"%s\"!%n", title);
    }

    // [Orchestration] — correct shutdown sequence is the reverse of startup;
    //                   the Facade owns this knowledge so no caller has to.
    public void endMovie() {
        System.out.println("[HomeTheater] Shutting down...");
        player.stop();
        amplifier.off();
        projector.off();
        screen.raise();
        lights.on();                // lights on last — so occupants can see safely
        System.out.println("[HomeTheater] Goodnight!");
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Demo
// ─────────────────────────────────────────────────────────────────────────────
public class HomeTheaterDemo {

    public static void main(String[] args) {
        // Build each subsystem — the client does this once at setup time
        Projector       projector = new Projector();
        Amplifier       amplifier = new Amplifier();
        StreamingPlayer player    = new StreamingPlayer();
        TheaterLights   lights    = new TheaterLights();
        Screen          screen    = new Screen();

        // [Facade] Wrap all subsystems — from here, the client only touches the Facade
        HomeTheaterFacade theater = new HomeTheaterFacade(
                projector, amplifier, player, lights, screen);

        System.out.println("=== Watch Movie ===");
        // [SimplifiedInterface] One call replaces 8 coordinated subsystem calls
        theater.watchMovie("Inception");

        System.out.println();

        System.out.println("=== End Movie ===");
        // [Orchestration] Facade knows the correct shutdown order; client does not need to
        theater.endMovie();

        System.out.println();

        // Contrast: without the Facade, the client would need to write:
        //   lights.dim(10); screen.lower(); projector.on(); projector.setInput("HDMI");
        //   amplifier.on(); amplifier.setVolume(5); player.login(...); player.play(...);
        // — every caller must know the sequence, and any change in order breaks every caller.
        System.out.println("(Without Facade the client would require 8+ direct subsystem calls.)");
    }
}
