package com.ramkumar.lld.designpatterns.structural.bridge.code;

// ─────────────────────────────────────────────────────────────────────────────
// Bridge Pattern — Scenario A: Remote Control + Devices
//
// Problem: A smart home has multiple device types (TV, Radio, Projector) and
//          multiple remote control tiers (BasicRemote, SmartRemote).
//          Without Bridge: BasicTV, SmartTV, BasicRadio, SmartRadio, ... → N×M classes.
//          With Bridge: N Remote classes + M Device classes, all combinations free.
//
// Participants:
//   Device          [Implementor interface]   — what the hardware can do
//   TV, Radio       [ConcreteImplementors]    — specific device behaviour
//   RemoteControl   [Abstraction]             — holds the Device bridge reference
//   BasicRemote     [RefinedAbstraction]      — basic on/off/volume
//   SmartRemote     [RefinedAbstraction]      — adds mute and status display
// ─────────────────────────────────────────────────────────────────────────────

// ── [Implementor] — the hardware interface; independent of the remote API ─────
// Notice: Device methods are low-level primitives (powerOn, setVolume).
//         RemoteControl methods are higher-level (togglePower, volumeUp).
//         Different interfaces on purpose — that's what makes Bridge different from Proxy.
interface Device {
    void powerOn();
    void powerOff();
    void setVolume(int level);    // level 0–100
    String getStatus();
}

// ── [ConcreteImplementor] — TV ────────────────────────────────────────────────
class TV implements Device {

    private boolean on     = false;
    private int     volume = 30;        // default volume

    @Override
    public void powerOn() {
        on = true;
        System.out.println("  [TV] Powered ON");
    }

    @Override
    public void powerOff() {
        on = false;
        System.out.println("  [TV] Powered OFF");
    }

    @Override
    public void setVolume(int level) {
        if (!on) { System.out.println("  [TV] Off — cannot set volume"); return; }
        this.volume = Math.max(0, Math.min(level, 100));  // clamp 0–100
        System.out.printf("  [TV] Volume set to %d%n", this.volume);
    }

    @Override
    public String getStatus() {
        return String.format("TV [%s], Volume: %d", on ? "ON" : "OFF", volume);
    }
}

// ── [ConcreteImplementor] — Radio ─────────────────────────────────────────────
class Radio implements Device {

    private boolean on     = false;
    private int     volume = 50;

    @Override
    public void powerOn() {
        on = true;
        System.out.println("  [Radio] Powered ON");
    }

    @Override
    public void powerOff() {
        on = false;
        System.out.println("  [Radio] Powered OFF");
    }

    @Override
    public void setVolume(int level) {
        this.volume = Math.max(0, Math.min(level, 100));
        System.out.printf("  [Radio] Volume set to %d%n", this.volume);
    }

    @Override
    public String getStatus() {
        return String.format("Radio [%s], Volume: %d", on ? "ON" : "OFF", volume);
    }
}

// ── [Abstraction] — RemoteControl holds the Device bridge reference ───────────
abstract class RemoteControl {

    // [TheBridge] — protected so subclasses can call device.xxx() directly
    protected final Device device;

    RemoteControl(Device device) {
        this.device = device;   // [Injection] implementor provided at construction
    }

    // Concrete methods compose Device primitives into higher-level operations
    void turnOn()           { device.powerOn(); }
    void turnOff()          { device.powerOff(); }
    void setVolume(int v)   { device.setVolume(v); }
    void showStatus()       { System.out.println("  Status → " + device.getStatus()); }
}

// ── [RefinedAbstraction] — BasicRemote: just the base operations ──────────────
class BasicRemote extends RemoteControl {

    BasicRemote(Device device) {
        super(device);  // [ChainToAbstraction] passes device bridge upward
    }
    // No additional behaviour — inherits turnOn/Off/setVolume/showStatus
}

// ── [RefinedAbstraction] — SmartRemote: adds mute and saved-volume restore ────
class SmartRemote extends RemoteControl {

    private int savedVolume = -1;   // [OwnState] SmartRemote remembers the volume before mute

    SmartRemote(Device device) {
        super(device);  // [ChainToAbstraction]
    }

    // mute() is a behaviour only SmartRemote provides — the Device doesn't know about it
    void mute() {
        System.out.println("  [Smart] Muting...");
        // [ComposesImplCalls] calls device.setVolume(0) — the device doesn't need a "mute" method
        device.setVolume(0);
    }

    void unmute(int restoreVolume) {
        System.out.printf("  [Smart] Restoring volume to %d%n", restoreVolume);
        device.setVolume(restoreVolume);
    }
}

// ── Demo ─────────────────────────────────────────────────────────────────────
public class RemoteControlDemo {

    public static void main(String[] args) {

        // ── 1. BasicRemote + TV ───────────────────────────────────────────────
        System.out.println("─── BasicRemote controlling TV ───");
        Device tv = new TV();
        RemoteControl basicRemote = new BasicRemote(tv);  // [Bridge] TV injected into remote
        basicRemote.turnOn();
        basicRemote.setVolume(40);
        basicRemote.showStatus();
        basicRemote.turnOff();
        System.out.println();

        // ── 2. SmartRemote + Radio ────────────────────────────────────────────
        System.out.println("─── SmartRemote controlling Radio ───");
        Device radio = new Radio();
        SmartRemote smartRemote = new SmartRemote(radio);  // [Bridge] Radio injected
        smartRemote.turnOn();
        smartRemote.setVolume(60);
        smartRemote.mute();
        smartRemote.unmute(60);
        smartRemote.showStatus();
        smartRemote.turnOff();
        System.out.println();

        // ── 3. Cross-pairing: SmartRemote + TV ───────────────────────────────
        // [KeyPoint] No new classes needed — just inject a different Device
        System.out.println("─── SmartRemote controlling TV (cross-pairing, zero new classes) ───");
        Device tv2 = new TV();
        SmartRemote smartWithTV = new SmartRemote(tv2);  // [CrossPairing] different Device, same Remote
        smartWithTV.turnOn();
        smartWithTV.setVolume(70);
        smartWithTV.mute();
        smartWithTV.showStatus();
        smartWithTV.turnOff();
        System.out.println();

        // ── 4. Polymorphic — RemoteControl[] with different devices ──────────
        // [Polymorphism] Client uses RemoteControl; doesn't know TV vs Radio
        System.out.println("─── Polymorphic remote array ───");
        RemoteControl[] remotes = {
            new BasicRemote(new TV()),
            new SmartRemote(new Radio()),
            new BasicRemote(new Radio())
        };
        for (RemoteControl r : remotes) {
            r.turnOn();
            r.setVolume(50);
            r.showStatus();
            r.turnOff();
            System.out.println();
        }
    }
}
