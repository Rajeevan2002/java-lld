package com.ramkumar.lld.solid.isp.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reference solution — Interface Segregation Principle (ISP)
 * Phase 2, Topic 4 | Scenario B: Smart Home Device System
 *
 * Fixes from the practice review:
 *   Issue 1: SmartSpeaker.playMusic() — missing `this.currentTrack = track` assignment
 *   Issue 2: SmartThermostat constructor — throws UnsupportedOperationException (wrong);
 *            should throw IllegalArgumentException
 *   Issue 3: SmartBulb/SmartThermostat.turnOn()/turnOff() — threw UnsupportedOperationException
 *            for already-on/off; switches must be idempotent (no-op, not exception)
 *   Issue 4: SmartHub lists not `final`; should be `private final`
 *
 * Design approach for getOnlineDevices():
 *   APPROACH A (used here, same as user's approach):
 *     IdentifiableDevice interface with getDeviceId().
 *     Switchable extends IdentifiableDevice so all Switchable implementors
 *     provide a device ID — no casting needed in getOnlineDevices().
 *
 *   APPROACH B (alternative — purer ISP):
 *     Switchable stays clean (no IdentifiableDevice extension).
 *     SmartHub keeps a Map<Switchable, String> keyed on the device,
 *     storing the ID at registration time. getOnlineDevices() uses the map.
 *     See comment block at the end of SmartHub for Approach B skeleton.
 */
public class ISPReference {

    // =========================================================================
    // IdentifiableDevice — helper interface for getOnlineDevices()
    // All device classes implement this; Switchable extends it so SmartHub
    // can call s.getDeviceId() directly without casting.
    // =========================================================================
    interface IdentifiableDevice {
        String getDeviceId();
    }

    // =========================================================================
    // STEP 1 — VIOLATION: FatSmartDevice (Part 1 of exercise)
    // =========================================================================

    /** ❌ Fat interface — forces every implementor to carry all 13 methods */
    interface FatSmartDevice {
        void   turnOn();
        void   turnOff();
        boolean isOn();
        void   setTemperature(int celsius);
        int    getTemperature();
        void   playMusic(String track);
        void   stopMusic();
        String getCurrentTrack();
        void   lock();
        void   unlock();
        boolean isLocked();
        void   showNotification(String message);
        void   callEmergency();
    }

    /**
     * ❌ SmartBulb forced to implement 10 methods it doesn't support.
     * This IS the ISP violation — every unused method is dead weight that can throw.
     */
    static class ViolatingSmartBulb implements FatSmartDevice {

        private final String  deviceId;
        private       boolean on;

        public ViolatingSmartBulb(String deviceId) {
            if (deviceId == null || deviceId.isBlank())
                throw new IllegalArgumentException("deviceId cannot be blank");
            this.deviceId = deviceId;
        }

        // Only these three methods make sense for a bulb
        @Override public void    turnOn()  { on = true; }
        @Override public void    turnOff() { on = false; }
        @Override public boolean isOn()    { return on; }

        // All other methods: ISP violation — forced to implement, only option is throw
        @Override public void    setTemperature(int c)        { throw new UnsupportedOperationException("SmartBulb: not supported"); }
        @Override public int     getTemperature()             { throw new UnsupportedOperationException("SmartBulb: not supported"); }
        @Override public void    playMusic(String track)      { throw new UnsupportedOperationException("SmartBulb: not supported"); }
        @Override public void    stopMusic()                  { throw new UnsupportedOperationException("SmartBulb: not supported"); }
        @Override public String  getCurrentTrack()            { throw new UnsupportedOperationException("SmartBulb: not supported"); }
        @Override public void    lock()                       { throw new UnsupportedOperationException("SmartBulb: not supported"); }
        @Override public void    unlock()                     { throw new UnsupportedOperationException("SmartBulb: not supported"); }
        @Override public boolean isLocked()                   { throw new UnsupportedOperationException("SmartBulb: not supported"); }
        @Override public void    showNotification(String msg) { throw new UnsupportedOperationException("SmartBulb: not supported"); }
        @Override public void    callEmergency()              { throw new UnsupportedOperationException("SmartBulb: not supported"); }
    }

    // =========================================================================
    // STEP 2 — FIX: Role interfaces (each = one capability)
    // =========================================================================

    /**
     * Role 1: On/off switching capability.
     * Extends IdentifiableDevice so SmartHub can call getDeviceId() without casting.
     * (APPROACH A — see class javadoc for APPROACH B alternative)
     */
    interface Switchable extends IdentifiableDevice {
        void    turnOn();
        void    turnOff();
        boolean isOn();
    }

    /** Role 2: Temperature control capability */
    interface TemperatureControllable {
        void setTemperature(int celsius);
        int  getTemperature();
    }

    /** Role 3: Music playback capability */
    interface MusicPlayable {
        void   playMusic(String track);
        void   stopMusic();
        String getCurrentTrack();
    }

    /** Role 4: Physical lock capability */
    interface Lockable {
        void    lock();
        void    unlock();
        boolean isLocked();
    }

    /** Role 5: Notification display capability */
    interface NotifiableDevice {
        void showNotification(String message);
    }

    /** Role 6: Emergency call capability */
    interface EmergencyCallable {
        void callEmergency();
    }

    // =========================================================================
    // Concrete device classes — implement ONLY the roles they fill
    // =========================================================================

    /**
     * ✅ SmartBulb: only Switchable.
     * KEY FIX 3: turnOn()/turnOff() are IDEMPOTENT — calling twice is safe (no throws).
     * A power switch that throws for repeated presses is a broken contract.
     */
    static class SmartBulb implements Switchable {

        private final String  deviceId;   // immutable — KEY FIX 4: field is final
        private       boolean on;

        public SmartBulb(String deviceId) {
            if (deviceId == null || deviceId.isBlank())
                throw new IllegalArgumentException("deviceId cannot be blank");
            this.deviceId = deviceId;
        }

        @Override public String  getDeviceId() { return deviceId; }
        @Override public boolean isOn()        { return on; }

        // Idempotent: on = true whether or not it was already true — no exception
        @Override public void turnOn()  { on = true;  System.out.println("[SmartBulb:" + deviceId + "] Turned ON"); }
        @Override public void turnOff() { on = false; System.out.println("[SmartBulb:" + deviceId + "] Turned OFF"); }
    }

    /**
     * ✅ SmartThermostat: Switchable + TemperatureControllable.
     * KEY FIX 2: constructor uses IllegalArgumentException (not UnsupportedOperationException).
     * KEY FIX 3: turnOn()/turnOff() are idempotent.
     */
    static class SmartThermostat implements Switchable, TemperatureControllable {

        private final String  deviceId;
        private       boolean on;
        private       int     temperature = 20;   // default 20°C

        public SmartThermostat(String deviceId) {
            // KEY FIX 2: IllegalArgumentException for invalid input — not UnsupportedOperationException
            if (deviceId == null || deviceId.isBlank())
                throw new IllegalArgumentException("deviceId cannot be blank");
            this.deviceId = deviceId;
        }

        @Override public String  getDeviceId() { return deviceId; }
        @Override public boolean isOn()        { return on; }
        @Override public void    turnOn()      { on = true;  System.out.println("[SmartThermostat:" + deviceId + "] Turned ON"); }
        @Override public void    turnOff()     { on = false; System.out.println("[SmartThermostat:" + deviceId + "] Turned OFF"); }
        @Override public int     getTemperature() { return temperature; }

        @Override
        public void setTemperature(int celsius) {
            if (celsius < 10 || celsius > 35)
                throw new IllegalArgumentException(
                    "Temperature must be between 10 and 35°C, got: " + celsius);
            this.temperature = celsius;
            System.out.println("[SmartThermostat:" + deviceId + "] Temperature set to " + celsius + "°C");
        }
    }

    /**
     * ✅ SmartSpeaker: Switchable + MusicPlayable + NotifiableDevice.
     * KEY FIX 1: playMusic() assigns this.currentTrack = track (was missing).
     * KEY FIX 3: turnOn()/turnOff() are idempotent.
     */
    static class SmartSpeaker implements Switchable, MusicPlayable, NotifiableDevice {

        private final String  deviceId;
        private       boolean on;
        private       String  currentTrack = null;   // null = no music playing

        public SmartSpeaker(String deviceId) {
            if (deviceId == null || deviceId.isBlank())
                throw new IllegalArgumentException("deviceId cannot be blank");
            this.deviceId = deviceId;
        }

        @Override public String  getDeviceId() { return deviceId; }
        @Override public boolean isOn()        { return on; }
        @Override public void    turnOn()      { on = true;  System.out.println("[SmartSpeaker:" + deviceId + "] Turned ON"); }
        @Override public void    turnOff()     { on = false; System.out.println("[SmartSpeaker:" + deviceId + "] Turned OFF"); }

        @Override
        public void playMusic(String track) {
            if (track == null || track.isBlank())
                throw new IllegalArgumentException("Track cannot be null or blank");
            this.currentTrack = track;   // KEY FIX 1: state assignment — was missing in practice
            System.out.println("[SmartSpeaker:" + deviceId + "] Playing: " + track);
        }

        @Override
        public void stopMusic() {
            this.currentTrack = null;
            System.out.println("[SmartSpeaker:" + deviceId + "] Music stopped");
        }

        @Override
        public String getCurrentTrack() { return currentTrack; }

        @Override
        public void showNotification(String message) {
            // No validation required by spec — but null guard is defensive
            System.out.println("[SmartSpeaker:" + deviceId + "] \uD83D\uDD14 " + message);
        }
    }

    /**
     * ✅ SmartLock: Switchable + Lockable + EmergencyCallable.
     * Default locked = true — starts in safe state.
     */
    static class SmartLock implements Switchable, Lockable, EmergencyCallable {

        private final String  deviceId;
        private       boolean on;
        private       boolean locked = true;   // default: locked (safe state)

        public SmartLock(String deviceId) {
            if (deviceId == null || deviceId.isBlank())
                throw new IllegalArgumentException("deviceId cannot be blank");
            this.deviceId = deviceId;
        }

        @Override public String  getDeviceId() { return deviceId; }
        @Override public boolean isOn()        { return on; }
        @Override public void    turnOn()      { on = true;  System.out.println("[SmartLock:" + deviceId + "] Turned ON"); }
        @Override public void    turnOff()     { on = false; System.out.println("[SmartLock:" + deviceId + "] Turned OFF"); }
        @Override public boolean isLocked()    { return locked; }
        @Override public void    lock()        { locked = true;  System.out.println("[SmartLock:" + deviceId + "] LOCKED"); }
        @Override public void    unlock()      { locked = false; System.out.println("[SmartLock:" + deviceId + "] UNLOCKED"); }

        @Override
        public void callEmergency() {
            System.out.println("[SmartLock:" + deviceId + "] \uD83D\uDEA8 EMERGENCY SERVICES CALLED");
        }
    }

    // =========================================================================
    // SmartHub — ISP-clean orchestrator
    // KEY FIX 4: all lists are private final
    // No instanceof in allOn, allOff, setAllTemp, triggerEmergency
    // =========================================================================

    /**
     * SmartHub uses APPROACH A: Switchable extends IdentifiableDevice,
     * so getOnlineDevices() can call s.getDeviceId() without casting.
     *
     * APPROACH B alternative (pure ISP, Switchable stays clean):
     * ─────────────────────────────────────────────────────────────
     *   private final Map<Switchable, String> switchableIds = new LinkedHashMap<>();
     *
     *   public void addSwitchable(Switchable s, String deviceId) {
     *       if (s == null || deviceId == null) throw new IllegalArgumentException(...);
     *       switchableIds.put(s, deviceId);
     *   }
     *
     *   public void allOn()  { for (Switchable s : switchableIds.keySet()) s.turnOn(); }
     *   public void allOff() { for (Switchable s : switchableIds.keySet()) s.turnOff(); }
     *
     *   public List<String> getOnlineDevices() {
     *       List<String> ids = new ArrayList<>();
     *       for (Map.Entry<Switchable, String> e : switchableIds.entrySet())
     *           if (e.getKey().isOn()) ids.add(e.getValue());
     *       return Collections.unmodifiableList(ids);
     *   }
     * Trade-off: addSwitchable() now needs a deviceId arg — API is slightly more complex.
     * In return: Switchable interface is a pure behavior contract (no identity concern).
     */
    static class SmartHub {

        // KEY FIX 4: private final — reference can never be replaced
        private final List<Switchable>              switchables      = new ArrayList<>();
        private final List<TemperatureControllable> tempControllers  = new ArrayList<>();
        private final List<MusicPlayable>           musicPlayers     = new ArrayList<>();
        private final List<EmergencyCallable>       emergencyDevices = new ArrayList<>();

        // Registration — each validates null
        public void addSwitchable(Switchable s) {
            if (s == null) throw new IllegalArgumentException("Switchable cannot be null");
            switchables.add(s);
        }
        public void addTempController(TemperatureControllable t) {
            if (t == null) throw new IllegalArgumentException("TemperatureControllable cannot be null");
            tempControllers.add(t);
        }
        public void addMusicPlayer(MusicPlayable m) {
            if (m == null) throw new IllegalArgumentException("MusicPlayable cannot be null");
            musicPlayers.add(m);
        }
        public void addEmergencyDevice(EmergencyCallable e) {
            if (e == null) throw new IllegalArgumentException("EmergencyCallable cannot be null");
            emergencyDevices.add(e);
        }

        // Action methods — pure polymorphic loops, zero instanceof
        public void allOn()  { for (Switchable s : switchables) s.turnOn(); }
        public void allOff() { for (Switchable s : switchables) s.turnOff(); }

        public void setAllTemp(int celsius) {
            for (TemperatureControllable t : tempControllers) t.setTemperature(celsius);
        }

        public void triggerEmergency() {
            for (EmergencyCallable e : emergencyDevices) e.callEmergency();
        }

        /**
         * The one acceptable instanceof: broadcastNotification checks if a MusicPlayable
         * is also NotifiableDevice. This is acceptable because the problem spec explicitly
         * notes it. The cleaner alternative is a separate List<NotifiableDevice> list.
         */
        public void broadcastNotification(String message) {
            for (MusicPlayable m : musicPlayers) {
                if (m instanceof NotifiableDevice) {
                    ((NotifiableDevice) m).showNotification(message);
                }
            }
        }

        /**
         * APPROACH A: getDeviceId() is available directly via Switchable extends IdentifiableDevice.
         * No casting, no instanceof — clean and type-safe.
         */
        public List<String> getOnlineDevices() {
            List<String> ids = new ArrayList<>();
            for (Switchable s : switchables) {
                if (s.isOn()) ids.add(s.getDeviceId());   // no cast needed — Switchable IS IdentifiableDevice
            }
            return Collections.unmodifiableList(ids);
        }
    }

    // =========================================================================
    // Main — same 11 tests as practice + Test 12 (catches the missing assignment bug)
    // =========================================================================
    public static void main(String[] args) {

        System.out.println("═══ Test 1: ISP Violation — ViolatingSmartBulb ══════════");
        ViolatingSmartBulb violator = new ViolatingSmartBulb("BULB-V1");
        violator.turnOn();
        System.out.println("isOn: " + violator.isOn());
        try {
            violator.setTemperature(22);
        } catch (UnsupportedOperationException e) {
            System.out.println("❌ UnsupportedOperationException: " + e.getMessage());
        }
        try {
            violator.callEmergency();
        } catch (UnsupportedOperationException e) {
            System.out.println("❌ UnsupportedOperationException: " + e.getMessage());
        }
        System.out.println("Test 1 PASSED — ViolatingSmartBulb shows ISP violation");

        System.out.println("\n═══ Test 2: SmartBulb — Switchable only ════════════════");
        SmartBulb bulb = new SmartBulb("BULB-001");
        bulb.turnOn();
        System.out.println("Bulb on: " + bulb.isOn());
        bulb.turnOff();
        System.out.println("Bulb on: " + bulb.isOn());
        System.out.println("Test 2 PASSED: " + !bulb.isOn());

        System.out.println("\n═══ Test 3: SmartThermostat — temperature control ═══════");
        SmartThermostat thermo = new SmartThermostat("THERMO-001");
        thermo.turnOn();
        System.out.println("Default temp: " + thermo.getTemperature());
        thermo.setTemperature(24);
        System.out.println("After set: " + thermo.getTemperature());
        System.out.println("Test 3 PASSED: " + (thermo.getTemperature() == 24));

        System.out.println("\n═══ Test 4: SmartThermostat — temperature validation ════");
        try {
            thermo.setTemperature(5);
            System.out.println("Test 4 FAILED — should have thrown");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
            System.out.println("Test 4 PASSED — temperature < 10 rejected");
        }
        try {
            thermo.setTemperature(40);
            System.out.println("Test 4b FAILED — should have thrown");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
            System.out.println("Test 4b PASSED — temperature > 35 rejected");
        }

        System.out.println("\n═══ Test 5: SmartSpeaker — music and notification ═══════");
        SmartSpeaker speaker = new SmartSpeaker("SPEAKER-001");
        speaker.turnOn();
        System.out.println("Track before play: " + speaker.getCurrentTrack());
        speaker.playMusic("Bohemian Rhapsody");
        System.out.println("Track: " + speaker.getCurrentTrack());   // Bohemian Rhapsody
        speaker.showNotification("Dinner is ready!");
        speaker.stopMusic();
        System.out.println("Track after stop: " + speaker.getCurrentTrack());
        System.out.println("Test 5 PASSED: " + (speaker.getCurrentTrack() == null));

        System.out.println("\n═══ Test 6: SmartSpeaker — blank track validation ═══════");
        try {
            speaker.playMusic("   ");
            System.out.println("Test 6 FAILED — should have thrown for blank track");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
            System.out.println("Test 6 PASSED — blank track rejected");
        }

        System.out.println("\n═══ Test 7: SmartLock — lock/unlock/emergency ═══════════");
        SmartLock lock = new SmartLock("LOCK-001");
        lock.turnOn();
        System.out.println("Initially locked: " + lock.isLocked());
        lock.unlock();
        System.out.println("After unlock: " + lock.isLocked());
        lock.lock();
        System.out.println("After lock: " + lock.isLocked());
        lock.callEmergency();
        System.out.println("Test 7 PASSED: " + lock.isLocked());

        System.out.println("\n═══ Test 8: SmartHub — allOn / allOff ══════════════════");
        SmartHub hub = new SmartHub();
        SmartBulb       b1 = new SmartBulb("BULB-H1");
        SmartThermostat t1 = new SmartThermostat("THERMO-H1");
        SmartSpeaker    s1 = new SmartSpeaker("SPEAKER-H1");
        SmartLock       l1 = new SmartLock("LOCK-H1");

        hub.addSwitchable(b1);
        hub.addSwitchable(t1);
        hub.addSwitchable(s1);
        hub.addSwitchable(l1);
        hub.addTempController(t1);
        hub.addMusicPlayer(s1);
        hub.addEmergencyDevice(l1);

        hub.allOn();
        boolean allOn = b1.isOn() && t1.isOn() && s1.isOn() && l1.isOn();
        System.out.println("All devices on: " + allOn);
        System.out.println("Test 8 PASSED: " + allOn);

        System.out.println("\n═══ Test 9: SmartHub — setAllTemp ══════════════════════");
        hub.setAllTemp(22);
        System.out.println("Thermostat temp: " + t1.getTemperature());
        System.out.println("Test 9 PASSED: " + (t1.getTemperature() == 22));

        System.out.println("\n═══ Test 10: SmartHub — triggerEmergency ════════════════");
        hub.triggerEmergency();
        System.out.println("Test 10 PASSED — emergency triggered on all emergency devices");

        System.out.println("\n═══ Test 11: SmartHub — getOnlineDevices ════════════════");
        hub.allOff();
        b1.turnOn();
        t1.turnOn();
        List<String> online = hub.getOnlineDevices();
        System.out.println("Online devices: " + online);
        System.out.println("Count: " + online.size());
        System.out.println("Test 11 PASSED: " + (online.size() == 2));

        try {
            online.add("FAKE");
            System.out.println("Test 11b FAILED — list should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            System.out.println("Test 11b PASSED — getOnlineDevices() returns unmodifiable list");
        }

        // ── Test 12 (Extra) — catches Issue 1: missing currentTrack assignment ─────
        // If playMusic() does not assign this.currentTrack = track, getCurrentTrack()
        // returns null even after playMusic() is called. This test catches that bug.
        System.out.println("\n═══ Test 12 (Extra): playMusic() must update currentTrack state ═");
        SmartSpeaker s2 = new SmartSpeaker("SPEAKER-T12");
        s2.turnOn();
        System.out.println("Before play — currentTrack: " + s2.getCurrentTrack());   // null
        s2.playMusic("Stairway to Heaven");
        String track = s2.getCurrentTrack();
        System.out.println("After play  — currentTrack: " + track);
        boolean t12 = "Stairway to Heaven".equals(track);
        System.out.println("Test 12 PASSED: " + t12);
        if (!t12) {
            System.out.println("Test 12 FAILED — playMusic() forgot: this.currentTrack = track");
        }

        // Additional idempotency check (catches Issue 3)
        System.out.println("\n═══ Test 12b (Extra): turnOn() is idempotent — calling twice is safe ═");
        SmartBulb b2 = new SmartBulb("BULB-T12");
        b2.turnOn();
        try {
            b2.turnOn();   // second call — should NOT throw
            System.out.println("Test 12b PASSED — turnOn() called twice: no exception");
        } catch (UnsupportedOperationException e) {
            System.out.println("Test 12b FAILED — turnOn() threw for already-on: " + e.getMessage());
        }
    }
}
