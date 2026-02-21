package com.ramkumar.lld.oop.composition.code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scenario A: Smart Home Device System
 *
 * Demonstrates composition over inheritance by showing BOTH the problem and the solution.
 *
 * THE PROBLEM (inheritance approach — commented out):
 *   SmartDevice
 *   ├── WiFiSmartDevice
 *   │   ├── WiFiSmartDeviceWithEmailAlert
 *   │   └── WiFiSmartDeviceWithPushAlert
 *   └── BluetoothSmartDevice
 *       ├── BluetoothSmartDeviceWithEmailAlert  ← 2 connectivity × 3 alerts = 6 classes
 *       └── BluetoothSmartDeviceWithNoAlert      and we haven't added any concrete devices yet!
 *
 * THE SOLUTION (composition approach — implemented below):
 *   SmartDevice HAS-A ConnectivityBehavior + HAS-A AlertBehavior
 *   Any combination: 2 × 3 = 5 classes total (not 6 per device type)
 *   And behaviors can be swapped at runtime.
 */
public class SmartHomeDemo {

    // =========================================================================
    // BEHAVIOR INTERFACES — the "strategies" that devices compose
    // =========================================================================

    // ConnectivityBehavior — HOW the device connects to the network
    // This is the "strategy" for network connection
    interface ConnectivityBehavior {
        boolean connect();
        void    disconnect();
        boolean isConnected();
        String  getProtocol();   // "WiFi", "Bluetooth", etc.
    }

    // AlertBehavior — HOW the device notifies users
    interface AlertBehavior {
        void   sendAlert(String message);
        String getAlertType();
    }

    // =========================================================================
    // BEHAVIOR IMPLEMENTATIONS — small, focused, single-responsibility classes
    // =========================================================================

    static class WiFiConnectivity implements ConnectivityBehavior {
        private boolean connected;
        private final String ssid;

        public WiFiConnectivity(String ssid) { this.ssid = ssid; }

        @Override public boolean connect() {
            connected = true;
            System.out.println("  [WiFi] Connected to " + ssid);
            return true;
        }
        @Override public void    disconnect() { connected = false; System.out.println("  [WiFi] Disconnected"); }
        @Override public boolean isConnected() { return connected; }
        @Override public String  getProtocol() { return "WiFi"; }
    }

    static class BluetoothConnectivity implements ConnectivityBehavior {
        private boolean connected;
        private final String pairedDevice;

        public BluetoothConnectivity(String pairedDevice) { this.pairedDevice = pairedDevice; }

        @Override public boolean connect() {
            connected = true;
            System.out.println("  [BT] Paired with " + pairedDevice);
            return true;
        }
        @Override public void    disconnect() { connected = false; }
        @Override public boolean isConnected() { return connected; }
        @Override public String  getProtocol() { return "Bluetooth"; }
    }

    static class EmailAlert implements AlertBehavior {
        private final String toEmail;
        public EmailAlert(String toEmail) { this.toEmail = toEmail; }

        @Override public void   sendAlert(String message) {
            System.out.println("  [EMAIL → " + toEmail + "] " + message);
        }
        @Override public String getAlertType() { return "Email"; }
    }

    static class PushAlert implements AlertBehavior {
        private final String deviceToken;
        public PushAlert(String deviceToken) { this.deviceToken = deviceToken; }

        @Override public void   sendAlert(String message) {
            System.out.println("  [PUSH → " + deviceToken.substring(0, 8) + "...] " + message);
        }
        @Override public String getAlertType() { return "Push"; }
    }

    // NoAlert — Null Object pattern: a valid implementation that does nothing
    // Better than null checks: device.setAlertBehavior(new NoAlert()) instead of null
    static class NoAlert implements AlertBehavior {
        @Override public void   sendAlert(String message) { /* silent — no alert */ }
        @Override public String getAlertType() { return "None"; }
    }

    // =========================================================================
    // SMARTDEVICE — composed, not inherited
    // HAS-A ConnectivityBehavior, HAS-A AlertBehavior
    // =========================================================================

    abstract static class SmartDevice {

        private final String deviceId;
        private final String name;

        // COMPOSITION — behaviors injected, not inherited
        private ConnectivityBehavior connectivity;   // HAS-A
        private AlertBehavior        alertBehavior;  // HAS-A

        protected SmartDevice(String deviceId, String name,
                               ConnectivityBehavior connectivity,
                               AlertBehavior alertBehavior) {
            this.deviceId     = deviceId;
            this.name         = name;
            this.connectivity = connectivity;
            this.alertBehavior = alertBehavior;
        }

        // DELEGATION — SmartDevice doesn't know HOW to connect; it delegates
        public boolean connect()              { return connectivity.connect(); }
        public void    disconnect()           { connectivity.disconnect(); }
        public boolean isConnected()          { return connectivity.isConnected(); }

        // DELEGATION — same pattern for alerts
        public void    alert(String message)  { alertBehavior.sendAlert(message); }

        // RUNTIME SWAP — the key advantage over inheritance
        // With inheritance, you'd have to instantiate a completely new subclass
        public void setAlertBehavior(AlertBehavior a) {
            System.out.println("  [Config] " + name + " alert switched to " + a.getAlertType());
            this.alertBehavior = a;
        }

        public void setConnectivity(ConnectivityBehavior c) {
            this.connectivity = c;
        }

        // Template method — subclasses provide device-specific status
        public abstract String getDeviceStatus();

        public String getInfo() {
            return String.format("[%s] %s | %s | Alert: %s | %s",
                    deviceId, name, connectivity.getProtocol(),
                    alertBehavior.getAlertType(), getDeviceStatus());
        }

        public String getDeviceId() { return deviceId; }
        public String getName()     { return name; }
    }

    // Concrete devices — each picks its own defaults but CAN be overridden
    static class SmartLight extends SmartDevice {
        private boolean isOn;

        public SmartLight(String id, ConnectivityBehavior c, AlertBehavior a) {
            super(id, "Smart Light", c, a);
        }

        public void turnOn()  { isOn = true;  alert("Light turned ON"); }
        public void turnOff() { isOn = false; alert("Light turned OFF"); }

        @Override public String getDeviceStatus() { return isOn ? "ON" : "OFF"; }
    }

    static class SmartLock extends SmartDevice {
        private boolean isLocked;

        public SmartLock(String id, ConnectivityBehavior c, AlertBehavior a) {
            super(id, "Smart Lock", c, a);
            this.isLocked = true;
        }

        public void lock()   { isLocked = true;  alert("Door LOCKED"); }
        public void unlock() { isLocked = false; alert("Door UNLOCKED — security alert!"); }

        @Override public String getDeviceStatus() { return isLocked ? "LOCKED" : "UNLOCKED"; }
    }

    // =========================================================================
    // SmartHomeHub — manages devices; codes to interface, not concrete types
    // =========================================================================

    static class SmartHomeHub {
        private final List<SmartDevice> devices = new ArrayList<>();

        public void registerDevice(SmartDevice d) { devices.add(d); }

        public List<SmartDevice> getDevices() { return Collections.unmodifiableList(devices); }

        // POLYMORPHISM — connects all devices regardless of protocol
        public void connectAll() {
            System.out.println("Connecting all devices...");
            devices.forEach(SmartDevice::connect);
        }

        public void broadcastAlert(String message) {
            System.out.println("Broadcasting: " + message);
            devices.forEach(d -> d.alert(message));
        }
    }

    // =========================================================================
    // Main — labelled output for each concept
    // =========================================================================

    public static void main(String[] args) {

        System.out.println("═══ Composition: behavior injected at construction ═══");
        SmartLight light = new SmartLight("L-001",
                new WiFiConnectivity("HomeNetwork"),
                new EmailAlert("owner@home.com"));

        SmartLock lock = new SmartLock("LK-001",
                new BluetoothConnectivity("Owner-Phone"),
                new PushAlert("token-abc-123-def"));

        System.out.println(light.getInfo());
        System.out.println(lock.getInfo());

        System.out.println("\n═══ Delegation: SmartDevice calls connect() on composed object ═══");
        light.connect();
        lock.connect();

        System.out.println("\n═══ Delegation: alert dispatched to composed AlertBehavior ═══");
        light.turnOn();    // internally calls alert()
        lock.unlock();     // internally calls alert()

        System.out.println("\n═══ Runtime Swap: change behavior without new subclass ═══");
        System.out.println("Before: " + light.getInfo());
        light.setAlertBehavior(new NoAlert());   // quiet mode
        System.out.println("After:  " + light.getInfo());
        light.turnOn();   // now silent — same object, different behavior

        System.out.println("\n═══ Same device class, different behavior combo ═══");
        SmartLight securityLight = new SmartLight("L-002",
                new BluetoothConnectivity("SecurityHub"),   // different connectivity
                new PushAlert("security-token-xyz"));       // different alert
        System.out.println(securityLight.getInfo());

        System.out.println("\n═══ Hub: polymorphic operations over all devices ═══");
        SmartHomeHub hub = new SmartHomeHub();
        hub.registerDevice(light);
        hub.registerDevice(lock);
        hub.registerDevice(securityLight);
        hub.connectAll();
        hub.broadcastAlert("Power outage detected");

        System.out.println("\n═══ If inheritance were used, we'd need ═══");
        System.out.println("  WiFiSmartLightWithEmail");
        System.out.println("  WiFiSmartLightWithPush");
        System.out.println("  WiFiSmartLightWithNoAlert");
        System.out.println("  BluetoothSmartLightWithEmail  ... etc");
        System.out.println("  2 connectivity × 3 alerts × 2 device types = 12 classes");
        System.out.println("  With composition: 2 + 3 + 2 = 7 classes. Same power.");
    }
}
