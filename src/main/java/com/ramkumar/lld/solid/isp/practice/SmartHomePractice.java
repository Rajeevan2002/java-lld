package com.ramkumar.lld.solid.isp.practice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Practice Exercise — Interface Segregation Principle (ISP)
 * Phase 2, Topic 4 | Scenario B: Smart Home Device System
 *
 * ═══════════════════════════════════════════════════════════════════════
 * PROBLEM STATEMENT
 * ═══════════════════════════════════════════════════════════════════════
 *
 * You are building a Smart Home control system. A smart home contains
 * multiple device types: bulbs, thermostats, speakers, and smart locks.
 * Each device supports a DIFFERENT set of capabilities.
 *
 * ── Part 1: Show the ISP Violation ─────────────────────────────────────
 *
 * Create a fat interface FatSmartDevice with ALL operations:
 *   turnOn(), turnOff(), isOn()
 *   setTemperature(int celsius), getTemperature()
 *   playMusic(String track), stopMusic(), getCurrentTrack()
 *   lock(), unlock(), isLocked()
 *   showNotification(String message)
 *   callEmergency()
 *
 * Create ViolatingSmartBulb implementing FatSmartDevice:
 *   - Implements turnOn(), turnOff(), isOn() correctly
 *   - All other methods throw UnsupportedOperationException("SmartBulb: not supported")
 *   This demonstrates the ISP violation.
 *
 * ── Part 2: Fix with Role Interfaces ───────────────────────────────────
 *
 * Define six role interfaces:
 *
 *   Switchable              — turnOn(), turnOff(), boolean isOn()
 *   TemperatureControllable — void setTemperature(int celsius), int getTemperature()
 *   MusicPlayable           — void playMusic(String track), void stopMusic(), String getCurrentTrack()
 *   Lockable                — void lock(), void unlock(), boolean isLocked()
 *   NotifiableDevice        — void showNotification(String message)
 *   EmergencyCallable       — void callEmergency()
 *
 * Create four concrete device classes (all fields private, all final where appropriate):
 *
 *   SmartBulb    — implements Switchable only
 *     Fields: String deviceId (immutable), boolean on (mutable state)
 *     Constructor: SmartBulb(String deviceId) — validates deviceId not null/blank
 *     turnOn()  → sets on = true,  prints "[SmartBulb:<id>] Turned ON"
 *     turnOff() → sets on = false, prints "[SmartBulb:<id>] Turned OFF"
 *     isOn()    → returns on
 *
 *   SmartThermostat — implements Switchable, TemperatureControllable
 *     Fields: String deviceId (immutable), boolean on (mutable), int temperature (mutable, default 20)
 *     Constructor: SmartThermostat(String deviceId) — validates deviceId not null/blank
 *     turnOn() / turnOff() / isOn() — same pattern as SmartBulb
 *     setTemperature(int celsius) — throws IllegalArgumentException if celsius < 10 or > 35
 *                                   prints "[SmartThermostat:<id>] Temperature set to <c>°C"
 *     getTemperature() — returns temperature
 *
 *   SmartSpeaker — implements Switchable, MusicPlayable, NotifiableDevice
 *     Fields: String deviceId (immutable), boolean on (mutable), String currentTrack (mutable, default null)
 *     Constructor: SmartSpeaker(String deviceId) — validates deviceId not null/blank
 *     turnOn() / turnOff() / isOn() — same pattern
 *     playMusic(String track) — throws IllegalArgumentException if track null/blank
 *                               sets currentTrack = track
 *                               prints "[SmartSpeaker:<id>] Playing: <track>"
 *     stopMusic()      — sets currentTrack = null, prints "[SmartSpeaker:<id>] Music stopped"
 *     getCurrentTrack() — returns currentTrack (may be null when no music playing)
 *     showNotification(String message) — prints "[SmartSpeaker:<id>] 🔔 <message>"
 *
 *   SmartLock — implements Switchable, Lockable, EmergencyCallable
 *     Fields: String deviceId (immutable), boolean on (mutable), boolean locked (mutable, default true)
 *     Constructor: SmartLock(String deviceId) — validates deviceId not null/blank
 *     turnOn() / turnOff() / isOn() — same pattern
 *     lock()     → sets locked = true,  prints "[SmartLock:<id>] LOCKED"
 *     unlock()   → sets locked = false, prints "[SmartLock:<id>] UNLOCKED"
 *     isLocked() → returns locked
 *     callEmergency() — prints "[SmartLock:<id>] 🚨 EMERGENCY SERVICES CALLED"
 *
 * ── Part 3: SmartHub Orchestrator ──────────────────────────────────────
 *
 * Create SmartHub with separate typed lists for each capability:
 *   private List<Switchable>              switchables      — all devices that can be turned on/off
 *   private List<TemperatureControllable> tempControllers  — devices that control temperature
 *   private List<MusicPlayable>           musicPlayers     — devices that play music
 *   private List<EmergencyCallable>       emergencyDevices — devices that can call emergency
 *
 * Registration methods (each validates that argument is not null):
 *   void addSwitchable(Switchable s)
 *   void addTempController(TemperatureControllable t)
 *   void addMusicPlayer(MusicPlayable m)
 *   void addEmergencyDevice(EmergencyCallable e)
 *
 * Action methods:
 *   void allOn()         — calls turnOn() on every Switchable (no instanceof)
 *   void allOff()        — calls turnOff() on every Switchable (no instanceof)
 *   void setAllTemp(int celsius) — calls setTemperature(celsius) on every TemperatureControllable
 *   void broadcastNotification(String message) — calls showNotification(message) on every
 *                                                device in musicPlayers that is also NotifiableDevice
 *                                                (hint: check instanceof here is acceptable because
 *                                                 SmartHub's NotifiableDevice list is intentionally
 *                                                 separate — but for simplicity, reuse musicPlayers
 *                                                 and do an instanceof check)
 *   void triggerEmergency() — calls callEmergency() on every EmergencyCallable
 *
 * Query methods:
 *   List<String> getOnlineDevices() — returns unmodifiable list of deviceIds for
 *                                      all Switchable devices where isOn() == true.
 *                                      (Hint: cast Switchable to a common accessor if needed,
 *                                       or keep it simple — check how SmartBulb exposes deviceId)
 *
 * DESIGN CONSTRAINTS:
 *   1. No instanceof in allOn(), allOff(), setAllTemp(), triggerEmergency()
 *   2. ViolatingSmartBulb must throw UnsupportedOperationException for non-switch methods
 *   3. SmartThermostat.setTemperature() validates 10 ≤ celsius ≤ 35 (inclusive)
 *   4. SmartSpeaker.playMusic() validates track not null/blank
 *   5. All deviceId fields are final (set once in constructor)
 *   6. SmartHub lists are private (not accessible directly)
 *   7. SmartHub.getOnlineDevices() returns an unmodifiable list
 *
 * ═══════════════════════════════════════════════════════════════════════
 * DO NOT MODIFY the main() method — fill in the TODOs to make tests pass
 * ═══════════════════════════════════════════════════════════════════════
 */
public class SmartHomePractice {

    // =========================================================================
    // ── TODO 1: Define FatSmartDevice interface (ISP violation — Part 1)
    //            All 13 methods: turnOn, turnOff, isOn, setTemperature,
    //            getTemperature, playMusic, stopMusic, getCurrentTrack,
    //            lock, unlock, isLocked, showNotification, callEmergency
    // =========================================================================
    interface  FatSmartDevice {
        void turnOn();
        void turnOff();
        boolean isOn();
        void setTemperature(int celsius);
        int getTemperature();
        void playMusic(String track);
        void stopMusic();
        String getCurrentTrack();
        void lock();
        void unlock();
        boolean isLocked();
        void showNotification(String message);
        void callEmergency();

    }


    // =========================================================================
    // ── TODO 2: Implement ViolatingSmartBulb implements FatSmartDevice
    //            Only turnOn/turnOff/isOn are real; everything else throws
    //            UnsupportedOperationException("SmartBulb: not supported")
    // =========================================================================
    static class ViolatingSmartBulb implements FatSmartDevice {

        private final String bulbName;
        private boolean bublSwitchedOn;

        public ViolatingSmartBulb(String bulbName){
            if(bulbName == null || bulbName.isBlank()){
                throw new IllegalArgumentException("Bulb Name cannot be null or Blank!!!");
            }
            this.bulbName = bulbName;
            bublSwitchedOn = false;
        }

        @Override public void turnOn() {
            bublSwitchedOn = true;
        }

        @Override public void turnOff() {
            bublSwitchedOn = false;
        }

        @Override public boolean isOn() {
            return bublSwitchedOn;
        }


        @Override public void setTemperature(int celsius) {throw new UnsupportedOperationException("SmartBulb: not supported");}
        @Override public int getTemperature() {throw new UnsupportedOperationException("SmartBulb: not supported");}
        @Override public void playMusic(String track){throw new UnsupportedOperationException("SmartBulb: not supported");}
        @Override public void stopMusic(){throw new UnsupportedOperationException("SmartBulb: not supported");}
        @Override public String getCurrentTrack(){throw new UnsupportedOperationException("SmartBulb: not supported");}
        @Override public void lock(){throw new UnsupportedOperationException("SmartBulb: not supported");}
        @Override public void unlock(){throw new UnsupportedOperationException("SmartBulb: not supported");}
        @Override public boolean isLocked(){throw new UnsupportedOperationException("SmartBulb: not supported");}
        @Override public void showNotification(String message){throw new UnsupportedOperationException("SmartBulb: not supported");}
        @Override public void callEmergency(){throw new UnsupportedOperationException("SmartBulb: not supported");}
    }

    // =========================================================================
    // ── TODO 3: Define Switchable interface
    //            Methods: void turnOn(), void turnOff(), boolean isOn()
    // =========================================================================
    interface Switchable extends IdentifiableDevice {
        void turnOn();
        void turnOff();
        boolean isOn();
    }

    // =========================================================================
    // ── TODO 4: Define TemperatureControllable interface
    //            Methods: void setTemperature(int celsius), int getTemperature()
    // =========================================================================
    interface TemperatureControllable {
        void setTemperature(int celsius);
        int getTemperature();
    }


    // =========================================================================
    // ── TODO 5: Define MusicPlayable interface
    //            Methods: void playMusic(String track), void stopMusic(),
    //                     String getCurrentTrack()
    // =========================================================================
    interface MusicPlayable {
        void playMusic(String track);
        void stopMusic();
        String getCurrentTrack();
    }

    // =========================================================================
    // ── TODO 6: Define Lockable interface
    //            Methods: void lock(), void unlock(), boolean isLocked()
    // =========================================================================
    interface Lockable {
        void lock();
        void unlock();
        boolean isLocked();
    }

    // =========================================================================
    // ── TODO 7: Define NotifiableDevice interface
    //            Methods: void showNotification(String message)
    // =========================================================================
    interface NotifiableDevice {
        void showNotification(String message);
    }

    // =========================================================================
    // ── TODO 8: Define EmergencyCallable interface
    //            Methods: void callEmergency()
    // =========================================================================
    interface EmergencyCallable {
        void callEmergency();
    }

    // =========================================================================
    // ── TODO 9: Implement SmartBulb implements Switchable
    //            Fields: private final String deviceId; private boolean on;
    //            Constructor: SmartBulb(String deviceId) — validate not null/blank
    // =========================================================================
    static class SmartBulb implements Switchable {
        private final String deviceId;
        private boolean on;

        public SmartBulb(String deviceId){
            if(deviceId == null || deviceId.isBlank()){
                throw  new IllegalArgumentException("Device Id Cannot be null / Blank ");
            }
            this.deviceId = deviceId;
            this.on = false;
        }

        public String getDeviceId() {
            return deviceId;
        }

        @Override
        public boolean isOn() {
            return on;
        }

        @Override
        public void turnOn(){
            if(on){
                throw  new UnsupportedOperationException("Bulb is already on!!!");
            }
            on = true;
        }
        @Override
        public void turnOff() {
            if(!on){
                throw  new UnsupportedOperationException("Bulb is already off!!!");
            }
            on = false;
        }
    }

    // =========================================================================
    // ── TODO 10: Implement SmartThermostat implements Switchable, TemperatureControllable
    //             Fields: private final String deviceId; private boolean on;
    //                     private int temperature = 20;
    //             setTemperature validates: 10 <= celsius <= 35
    // =========================================================================
    static class SmartThermostat implements Switchable, TemperatureControllable {
        private final String deviceId;
        private boolean on;
        private int temperature;

        public SmartThermostat(String deviceId){
            if(deviceId == null || deviceId.isBlank()) {
                throw new UnsupportedOperationException("Device Id cannot be null / Blank");
            }
            this.deviceId = deviceId;
            this.on = false;
            this.temperature = 20;
        }

        @Override public void setTemperature(int celsius) {
            if(celsius < 10 || celsius > 35) {
                throw new IllegalArgumentException("Temperature should be >=10 and <=35");
            }
            this.temperature = celsius;
        }

        public String getDeviceId() {
            return deviceId;
        }

        @Override
        public int getTemperature() {return temperature;}


        @Override
        public boolean isOn() {
            return on;
        }

        @Override
        public void turnOn(){
            if(on){
                throw  new UnsupportedOperationException("Thermostat is already on!!!");
            }
            on = true;
        }
        @Override
        public void turnOff() {
            if(!on){
                throw  new UnsupportedOperationException("Thermostat is already off!!!");
            }
            on = false;
        }
    }

    // =========================================================================
    // ── TODO 11: Implement SmartSpeaker implements Switchable, MusicPlayable, NotifiableDevice
    //             Fields: private final String deviceId; private boolean on;
    //                     private String currentTrack = null;
    //             playMusic validates: track not null/blank
    // =========================================================================
    static class SmartSpeaker implements Switchable, MusicPlayable, NotifiableDevice {
        private final String deviceId;
        private boolean on;
        private String currentTrack;

        public SmartSpeaker(String deviceId){
            if(deviceId == null || deviceId.isBlank()){
                throw new IllegalArgumentException("deviceId cannot be blank");
            }
            this.deviceId = deviceId;
            this.on = false;
            this.currentTrack = null;
        }

        public String getDeviceId() {
            return deviceId;
        }

        @Override  public void playMusic(String track){
            if(track == null || track.isBlank()) {
                throw new IllegalArgumentException("Track cannot be null");
            }
            System.out.println("Currently Playing Track : " + track);
        };
        @Override public void stopMusic() {currentTrack = null;}
        @Override public String getCurrentTrack() {
            return currentTrack;
        }

        @Override
        public boolean isOn() {
            return on;
        }

        @Override
        public void turnOn(){
            if(on){
                throw  new UnsupportedOperationException("Speaker is already on!!!");
            }
            on = true;
        }
        @Override
        public void turnOff() {
            if(!on){
                throw  new UnsupportedOperationException("Speaker is already off!!!");
            }
            on = false;
        }

        @Override
        public void showNotification(String message){
            if(message == null || message.isBlank()) {
                throw new IllegalArgumentException("Message cannot be null or blank");
            }
            System.out.println( "[SmartSpeaker: " + deviceId + "] 🔔 " + message);
        }
    }


    // =========================================================================
    // ── TODO 12: Implement SmartLock implements Switchable, Lockable, EmergencyCallable
    //             Fields: private final String deviceId; private boolean on;
    //                     private boolean locked = true; (default: locked)
    // =========================================================================
    static class SmartLock implements Switchable, Lockable, EmergencyCallable {
        private final String deviceId;
        private boolean on;
        private boolean locked;

        public SmartLock(String deviceId){
            if(deviceId == null || deviceId.isBlank()){
                throw new IllegalArgumentException("Device ID cannot be null");
            }
            this.deviceId = deviceId;
            this.on = false;
            this.locked = true;
        }

        public String getDeviceId() {return deviceId;}

        @Override public void turnOn() { this.on = true; }
        @Override public void turnOff() { this.on = false; }
        @Override public boolean isOn() { return on;}
        @Override public void lock() {this.locked = true;}
        @Override public void unlock() { this.locked = false;}
        @Override public boolean isLocked() { return locked; }
        @Override public void callEmergency() {
            System.out.println("Emergency is being called !!!");
        }
    }


    // =========================================================================
    // ── TODO 13: Implement SmartHub
    //             Four private lists: switchables, tempControllers, musicPlayers, emergencyDevices
    //             Registration: addSwitchable, addTempController, addMusicPlayer, addEmergencyDevice
    //             Actions: allOn, allOff, setAllTemp, broadcastNotification, triggerEmergency
    //             Query: getOnlineDevices() — returns unmodifiable List<String> of device IDs
    //
    //             Hint for getOnlineDevices(): Switchable does not expose getDeviceId().
    //             You can define a helper interface or add a getDeviceId() to Switchable,
    //             OR keep a parallel list — choose one approach and be consistent.
    // =========================================================================
    static class SmartHub {
        private List<Switchable> switchables;
        private List<TemperatureControllable> temperatureControllables;
        private List<MusicPlayable> musicPlayables;
        private List<EmergencyCallable> emergencyCallables;


        public SmartHub(){
            this.switchables = new ArrayList<>();
            this.temperatureControllables = new ArrayList<>();
            this.musicPlayables = new ArrayList<>();
            this.emergencyCallables = new ArrayList<>();
        }

        public void allOn(){
            for(Switchable switchable: switchables){
                switchable.turnOn();
            }
        }

        public void allOff(){
            for(Switchable switchable: switchables){
                switchable.turnOff();
            }
        }

        public List<Switchable> getSwitchables() { return Collections.unmodifiableList(switchables); }
        public List<TemperatureControllable> getTemperatureControllables() { return Collections.unmodifiableList(temperatureControllables);}
        public List<MusicPlayable> getMusicPlayables() { return Collections.unmodifiableList(musicPlayables);}
        public List<EmergencyCallable> getEmergencyCallables() { return Collections.unmodifiableList(emergencyCallables);}

        public void setAllTemp(int celsius){
            for(TemperatureControllable temperatureControllable : temperatureControllables){
                temperatureControllable.setTemperature(celsius);
            }
        }

        public void triggerEmergency(){
            for(EmergencyCallable emergencyCallable : emergencyCallables){
                emergencyCallable.callEmergency();
            }
        }


        public void broadcastNotification(String message){
            for(MusicPlayable musicPlayable : musicPlayables){
                if(musicPlayable instanceof NotifiableDevice) {
                    ((NotifiableDevice) musicPlayable).showNotification(message);
                }
            }
        }

        public void addSwitchable(Switchable switchable){
            if(switchable == null){
                throw new IllegalArgumentException("Switchable cannot be null !!!");
            }
            switchables.add(switchable);
        }

        public void addTempController(TemperatureControllable temperatureControllable){
            if(temperatureControllable == null){
                throw new IllegalArgumentException("Temperature Controllable cannot be null!!!");
            }
            temperatureControllables.add(temperatureControllable);
        }

        public void addMusicPlayer(MusicPlayable musicPlayable){
            if(musicPlayable == null){
                throw new IllegalArgumentException("Music Playable cannot be null!!!");
            }
            musicPlayables.add(musicPlayable);
        }

        public void addEmergencyDevice(EmergencyCallable emergencyCallable){
            if(emergencyCallable == null){
                throw new IllegalArgumentException("Emergency Callable cannot be null!!!");
            }
            emergencyCallables.add(emergencyCallable);
        }

        public List<String> getOnlineDevices() {
            List<String> ids = new ArrayList<>();
            for(Switchable s: switchables){
                if(s.isOn()){
                    ids.add(s.getDeviceId());
                }
            }
            return Collections.unmodifiableList(ids);
        }
    }

     interface IdentifiableDevice {
        String getDeviceId();
    }

    // =========================================================================
    // DO NOT MODIFY — fill in TODOs above to make all tests pass
    // =========================================================================
    public static void main(String[] args) {

        System.out.println("═══ Test 1: ISP Violation — ViolatingSmartBulb ══════════");
        ViolatingSmartBulb violator = new ViolatingSmartBulb("BULB-V1");
        violator.turnOn();
        System.out.println("isOn: " + violator.isOn());   // true
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
        System.out.println("Bulb on: " + bulb.isOn());    // true
        bulb.turnOff();
        System.out.println("Bulb on: " + bulb.isOn());    // false
        System.out.println("Test 2 PASSED: " + !bulb.isOn());

        System.out.println("\n═══ Test 3: SmartThermostat — temperature control ═══════");
        SmartThermostat thermo = new SmartThermostat("THERMO-001");
        thermo.turnOn();
        System.out.println("Default temp: " + thermo.getTemperature());  // 20
        thermo.setTemperature(24);
        System.out.println("After set: " + thermo.getTemperature());     // 24
        System.out.println("Test 3 PASSED: " + (thermo.getTemperature() == 24));

        System.out.println("\n═══ Test 4: SmartThermostat — temperature validation ════");
        try {
            thermo.setTemperature(5);   // below min (10)
            System.out.println("Test 4 FAILED — should have thrown");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
            System.out.println("Test 4 PASSED — temperature < 10 rejected");
        }
        try {
            thermo.setTemperature(40);  // above max (35)
            System.out.println("Test 4b FAILED — should have thrown");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
            System.out.println("Test 4b PASSED — temperature > 35 rejected");
        }

        System.out.println("\n═══ Test 5: SmartSpeaker — music and notification ═══════");
        SmartSpeaker speaker = new SmartSpeaker("SPEAKER-001");
        speaker.turnOn();
        System.out.println("Track before play: " + speaker.getCurrentTrack());  // null
        speaker.playMusic("Bohemian Rhapsody");
        System.out.println("Track: " + speaker.getCurrentTrack());  // Bohemian Rhapsody
        speaker.showNotification("Dinner is ready!");
        speaker.stopMusic();
        System.out.println("Track after stop: " + speaker.getCurrentTrack());   // null
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
        System.out.println("Initially locked: " + lock.isLocked());    // true
        lock.unlock();
        System.out.println("After unlock: " + lock.isLocked());         // false
        lock.lock();
        System.out.println("After lock: " + lock.isLocked());           // true
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
        System.out.println("Thermostat temp: " + t1.getTemperature());  // 22
        System.out.println("Test 9 PASSED: " + (t1.getTemperature() == 22));

        System.out.println("\n═══ Test 10: SmartHub — triggerEmergency ════════════════");
        hub.triggerEmergency();
        System.out.println("Test 10 PASSED — emergency triggered on all emergency devices");

        System.out.println("\n═══ Test 11: SmartHub — getOnlineDevices ════════════════");
        hub.allOff();
        b1.turnOn();   // only bulb and thermostat online
        t1.turnOn();
        List<String> online = hub.getOnlineDevices();
        System.out.println("Online devices: " + online);
        System.out.println("Count: " + online.size());   // 2
        System.out.println("Test 11 PASSED: " + (online.size() == 2));

        // Unmodifiable check
        try {
            online.add("FAKE");
            System.out.println("Test 11b FAILED — list should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            System.out.println("Test 11b PASSED — getOnlineDevices() returns unmodifiable list");
        }
    }

    // =========================================================================
    // HINTS (read only if stuck)
    // =========================================================================

    /*
     * ── HINT 1 (Gentle) ────────────────────────────────────────────────────
     * The ISP fix requires you to split one fat interface into several small,
     * focused interfaces. Each interface should represent one capability.
     * A class can implement multiple such interfaces without violating ISP —
     * that's intentional. Think about which capability each device actually has.
     *
     * ── HINT 2 (Direct) ────────────────────────────────────────────────────
     * Java interfaces can be implemented multiple times:
     *   class SmartSpeaker implements Switchable, MusicPlayable, NotifiableDevice { ... }
     * Use ArrayList<Switchable>, ArrayList<MusicPlayable>, etc. for SmartHub's lists.
     * Collections.unmodifiableList() makes a list unmodifiable.
     * IllegalArgumentException for invalid inputs (temp out of range, blank track).
     *
     * ── HINT 3 (Near-Solution) ─────────────────────────────────────────────
     * Skeleton for SmartBulb:
     *
     *   static class SmartBulb implements Switchable {
     *       private final String deviceId;
     *       private boolean on;
     *
     *       public SmartBulb(String deviceId) {
     *           if (deviceId == null || deviceId.isBlank())
     *               throw new IllegalArgumentException("deviceId cannot be blank");
     *           this.deviceId = deviceId;
     *       }
     *
     *       @Override public void turnOn()  { on = true;  System.out.println("[SmartBulb:" + deviceId + "] Turned ON"); }
     *       @Override public void turnOff() { on = false; System.out.println("[SmartBulb:" + deviceId + "] Turned OFF"); }
     *       @Override public boolean isOn() { return on; }
     *       public String getDeviceId() { return deviceId; }
     *   }
     *
     * Skeleton for SmartHub:
     *
     *   static class SmartHub {
     *       private final List<Switchable>              switchables      = new ArrayList<>();
     *       private final List<TemperatureControllable> tempControllers  = new ArrayList<>();
     *       private final List<MusicPlayable>           musicPlayers     = new ArrayList<>();
     *       private final List<EmergencyCallable>       emergencyDevices = new ArrayList<>();
     *
     *       public void addSwitchable(Switchable s)              { if (s == null) throw new ...; switchables.add(s); }
     *       public void addTempController(TemperatureControllable t) { ... }
     *       public void addMusicPlayer(MusicPlayable m)          { ... }
     *       public void addEmergencyDevice(EmergencyCallable e)  { ... }
     *
     *       public void allOn()  { for (Switchable s : switchables) s.turnOn(); }
     *       public void allOff() { for (Switchable s : switchables) s.turnOff(); }
     *       public void setAllTemp(int celsius) { for (TemperatureControllable t : tempControllers) t.setTemperature(celsius); }
     *       public void triggerEmergency() { for (EmergencyCallable e : emergencyDevices) e.callEmergency(); }
     *
     *       // For getOnlineDevices: add getDeviceId() to Switchable, or define
     *       // an IdentifiableDevice interface — pick one approach.
     *       public List<String> getOnlineDevices() {
     *           List<String> ids = new ArrayList<>();
     *           for (Switchable s : switchables) {
     *               if (s.isOn()) ids.add(/* get device id somehow *\/);
     *           }
     *           return Collections.unmodifiableList(ids);
     *       }
     *   }
     */
}
