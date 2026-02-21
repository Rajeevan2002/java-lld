package com.ramkumar.lld.oop.interfaces.code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scenario A: Media Device System
 *
 * Demonstrates when to use interface vs abstract class:
 *
 *  Interfaces (capabilities — CAN-DO):
 *    - Playable   → any device that can play media
 *    - Recordable → any device that can record media
 *
 *  Abstract class (shared IS-A identity + state):
 *    - MediaDevice → all devices share deviceId, brand, isPoweredOn
 *
 *  Concrete classes (multiple interface impl):
 *    - MusicPlayer  extends MediaDevice implements Playable
 *    - VideoCamera  extends MediaDevice implements Recordable
 *    - Smartphone   extends MediaDevice implements Playable, Recordable  ← both!
 *
 *  Key insight: A Smartphone IS-A MediaDevice (abstract class)
 *               AND CAN play (Playable) AND CAN record (Recordable).
 *               You cannot model this cleanly with abstract class alone (single inheritance).
 */
public class MediaDeviceDemo {

    // =========================================================================
    // INTERFACES — capabilities, no state, multiple implementable
    // =========================================================================

    // "Playable" is a capability — any object that can be played
    // Notice: no fields, only the contract of what playing means
    interface Playable {
        void play(String mediaTitle);
        void pause();
        void stop();

        // INTERFACE — Default method (Java 8+)
        // Provides a base implementation — implementors get this for free
        // They CAN override it, but don't have to
        default String getNowPlaying() {
            return "Currently playing on " + getClass().getSimpleName();
        }
    }

    // Separate "Recordable" interface — Interface Segregation
    // Not everything that plays can record, and vice versa
    // Keeping them separate means a MusicPlayer doesn't have to fake a record() method
    interface Recordable {
        void startRecording();
        void stopRecording();
        boolean isRecording();

        // Default method — implementors get this behaviour for free
        default String getRecordingStatus() {
            return isRecording() ? "Recording in progress" : "Not recording";
        }
    }

    // =========================================================================
    // ABSTRACT CLASS — shared identity and state
    // =========================================================================

    // MediaDevice IS-A base for all devices — it HAS state (deviceId, brand)
    // This is why it's an abstract class and not an interface:
    // interfaces cannot hold instance fields like deviceId and brand
    abstract static class MediaDevice {

        private final String deviceId;   // ENCAPSULATION — private + final
        private final String brand;
        private boolean isPoweredOn;

        protected MediaDevice(String deviceId, String brand) {
            if (deviceId == null || deviceId.isBlank())
                throw new IllegalArgumentException("Device ID cannot be blank");
            this.deviceId    = deviceId;
            this.brand       = brand;
            this.isPoweredOn = false;
        }

        // Concrete method shared by all subclasses — no override needed
        public void powerOn()  {
            isPoweredOn = true;
            System.out.println(brand + " [" + deviceId + "] powered ON");
        }

        public void powerOff() {
            isPoweredOn = false;
            System.out.println(brand + " [" + deviceId + "] powered OFF");
        }

        // Abstract — each device type describes itself differently
        public abstract String getDeviceType();

        // Template Method — defines skeleton, calls abstract method for specifics
        public String getInfo() {
            return String.format("[%s] %s (%s) — %s",
                    deviceId, brand, getDeviceType(),
                    isPoweredOn ? "ON" : "OFF");
        }

        public String getDeviceId()   { return deviceId; }
        public String getBrand()      { return brand; }
        public boolean isPoweredOn()  { return isPoweredOn; }
    }

    // =========================================================================
    // CONCRETE CLASSES — each picks exactly the interfaces it needs
    // =========================================================================

    // MusicPlayer IS-A MediaDevice AND CAN-DO play
    // It does NOT implement Recordable — it can't record
    static class MusicPlayer extends MediaDevice implements Playable {

        private String nowPlaying;

        public MusicPlayer(String deviceId, String brand) {
            super(deviceId, brand);
        }

        // INHERITANCE — from MediaDevice
        @Override public String getDeviceType() { return "Music Player"; }

        // POLYMORPHISM — Playable interface fulfilled
        @Override
        public void play(String mediaTitle) {
            this.nowPlaying = mediaTitle;
            System.out.println(getBrand() + ": Playing ♪ " + mediaTitle);
        }

        @Override public void pause() { System.out.println(getBrand() + ": Paused"); }
        @Override public void stop()  { nowPlaying = null; System.out.println(getBrand() + ": Stopped"); }

        // Can OVERRIDE the default method for a more specific implementation
        @Override
        public String getNowPlaying() {
            return nowPlaying != null ? "♪ " + nowPlaying : "Nothing playing";
        }
    }

    // VideoCamera IS-A MediaDevice AND CAN-DO record
    // It does NOT implement Playable — it doesn't play back (simplified model)
    static class VideoCamera extends MediaDevice implements Recordable {

        private boolean recording;

        public VideoCamera(String deviceId, String brand) {
            super(deviceId, brand);
        }

        @Override public String getDeviceType() { return "Video Camera"; }

        // POLYMORPHISM — Recordable interface fulfilled
        @Override
        public void startRecording() {
            recording = true;
            System.out.println(getBrand() + ": 🔴 Recording started");
        }

        @Override
        public void stopRecording() {
            recording = false;
            System.out.println(getBrand() + ": ⏹ Recording stopped");
        }

        @Override public boolean isRecording() { return recording; }
        // getRecordingStatus() — inherited from Recordable default method, no override needed
    }

    // Smartphone IS-A MediaDevice AND CAN-DO play AND CAN-DO record
    // MULTIPLE INTERFACE IMPLEMENTATION — this is the key advantage of interfaces
    // You CANNOT achieve this with abstract classes alone (single extends only)
    static class Smartphone extends MediaDevice implements Playable, Recordable {

        private String nowPlaying;
        private boolean recording;

        public Smartphone(String deviceId, String brand) {
            super(deviceId, brand);
        }

        @Override public String getDeviceType() { return "Smartphone"; }

        // Playable implementation
        @Override public void play(String title)  { nowPlaying = title; System.out.println(getBrand() + ": Playing " + title); }
        @Override public void pause()             { System.out.println(getBrand() + ": Paused"); }
        @Override public void stop()              { nowPlaying = null; }

        // Recordable implementation
        @Override public void startRecording() { recording = true;  System.out.println(getBrand() + ": Recording..."); }
        @Override public void stopRecording()  { recording = false; System.out.println(getBrand() + ": Stopped recording"); }
        @Override public boolean isRecording() { return recording; }

        // Both default methods inherited — no conflict since different interfaces
        // getNowPlaying()      ← from Playable.default
        // getRecordingStatus() ← from Recordable.default
    }

    // =========================================================================
    // CODING TO INTERFACE — MediaManager works with interfaces, not concrete types
    // =========================================================================

    static class MediaManager {

        // Stored as Playable — doesn't care if it's MusicPlayer or Smartphone
        private final List<Playable>    playables    = new ArrayList<>();
        private final List<Recordable>  recordables  = new ArrayList<>();

        public void registerPlayable(Playable p)    { playables.add(p); }
        public void registerRecordable(Recordable r) { recordables.add(r); }

        // POLYMORPHISM — calls play() on each without knowing the concrete type
        public void playAll(String title) {
            System.out.println("\n── Playing '" + title + "' on all devices ──");
            playables.forEach(p -> p.play(title));
        }

        public void startAllRecordings() {
            System.out.println("\n── Starting all recordings ──");
            recordables.forEach(Recordable::startRecording);
        }

        // Returns only the devices that implement BOTH — safe instanceof for a different interface
        public List<Object> getMultiCapabilityDevices() {
            return playables.stream()
                    .filter(p -> p instanceof Recordable)
                    .collect(Collectors.toList());
        }
    }

    // =========================================================================
    // Main — labelled output for each concept
    // =========================================================================

    public static void main(String[] args) {

        // Create devices
        MusicPlayer sony   = new MusicPlayer("MP-001", "Sony");
        VideoCamera canon  = new VideoCamera("VC-001", "Canon");
        Smartphone  apple  = new Smartphone("SP-001", "Apple");

        sony.powerOn();
        canon.powerOn();
        apple.powerOn();

        System.out.println("\n── Device Info (Template Method in abstract class) ──");
        System.out.println(sony.getInfo());
        System.out.println(canon.getInfo());
        System.out.println(apple.getInfo());

        // Coding to interface — stored as Playable reference
        System.out.println("\n── Playable — interface reference ──");
        Playable p1 = sony;    // upcast: MusicPlayer → Playable
        Playable p2 = apple;   // upcast: Smartphone  → Playable
        p1.play("Bohemian Rhapsody");
        p2.play("Blinding Lights");
        System.out.println(sony.getNowPlaying());    // overridden default method
        System.out.println(apple.getNowPlaying());   // inherited default method

        // Recordable — different interface, same object (apple)
        System.out.println("\n── Recordable — interface reference ──");
        Recordable r1 = canon;
        Recordable r2 = apple;   // apple is BOTH Playable AND Recordable
        r1.startRecording();
        r2.startRecording();
        System.out.println(canon.getRecordingStatus());  // default method
        System.out.println(apple.getRecordingStatus());  // default method

        // MediaManager — codes to interface
        MediaManager manager = new MediaManager();
        manager.registerPlayable(sony);
        manager.registerPlayable(apple);
        manager.registerRecordable(canon);
        manager.registerRecordable(apple);

        manager.playAll("Yesterday");
        manager.startAllRecordings();

        System.out.println("\n── Multi-capability devices (both Playable + Recordable) ──");
        List<Object> multi = manager.getMultiCapabilityDevices();
        System.out.println("Count: " + multi.size() + " (only Smartphone qualifies)");

        // Show that MusicPlayer cannot be used as Recordable
        System.out.println("\n── Interface segregation proof ──");
        System.out.println("sony instanceof Recordable: " + (sony instanceof Recordable));  // false
        System.out.println("apple instanceof Recordable: " + (apple instanceof Recordable)); // true
    }
}
