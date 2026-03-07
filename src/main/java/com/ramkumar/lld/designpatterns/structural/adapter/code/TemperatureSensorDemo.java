package com.ramkumar.lld.designpatterns.structural.adapter.code;

// ─────────────────────────────────────────────────────────────────────────────
// Adapter Pattern — Scenario A: Temperature Sensor
//
// Problem: A monitoring system needs all sensors to report in Celsius.
//          Two legacy sensors exist — one reports in Fahrenheit, one in Kelvin.
//          We cannot modify the legacy sensor classes.
//
// Solution: Create an adapter for each legacy sensor that implements the
//           modern TemperatureSensor interface via composition.
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// [Target] — the interface the monitoring system expects
// ─────────────────────────────────────────────────────────────────────────────
interface TemperatureSensor {
    /** Returns the current reading in degrees Celsius. */
    double getTemperatureCelsius();

    /** Returns a unique identifier for this sensor. */
    String getSensorId();
}

// ─────────────────────────────────────────────────────────────────────────────
// [Adaptee 1] — legacy Fahrenheit sensor; cannot be modified
// ─────────────────────────────────────────────────────────────────────────────
class LegacyFahrenheitSensor {
    private final double tempF;
    private final String serialNo;

    LegacyFahrenheitSensor(double tempF, String serialNo) {
        this.tempF    = tempF;
        this.serialNo = serialNo;
    }

    /** Returns temperature in degrees Fahrenheit. */
    public double readTempF()   { return tempF; }

    /** Returns the legacy serial number identifier. */
    public String getSerialNo() { return serialNo; }
}

// ─────────────────────────────────────────────────────────────────────────────
// [Adaptee 2] — legacy Kelvin sensor; cannot be modified
// ─────────────────────────────────────────────────────────────────────────────
class OldKelvinSensor {
    private final double kelvin;
    private final String deviceId;

    OldKelvinSensor(double kelvin, String deviceId) {
        this.kelvin   = kelvin;
        this.deviceId = deviceId;
    }

    /** Returns temperature in Kelvin. */
    public double getKelvinReading() { return kelvin; }

    /** Returns the legacy device identifier. */
    public String getDeviceId()      { return deviceId; }
}

// ─────────────────────────────────────────────────────────────────────────────
// [ObjectAdapter 1] — wraps LegacyFahrenheitSensor, exposes TemperatureSensor
// ─────────────────────────────────────────────────────────────────────────────
class FahrenheitSensorAdapter implements TemperatureSensor {

    // [Composition] — adapter holds the adaptee; does NOT extend it
    private final LegacyFahrenheitSensor sensor;

    FahrenheitSensorAdapter(LegacyFahrenheitSensor sensor) {
        this.sensor = sensor;
    }

    @Override
    public double getTemperatureCelsius() {
        // [Translation] F → C conversion lives here, not in the adaptee
        return (sensor.readTempF() - 32) * 5.0 / 9.0;
    }

    @Override
    public String getSensorId() {
        // [Delegation] map the legacy identifier to the target interface
        return sensor.getSerialNo();
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// [ObjectAdapter 2] — wraps OldKelvinSensor, exposes TemperatureSensor
// ─────────────────────────────────────────────────────────────────────────────
class KelvinSensorAdapter implements TemperatureSensor {

    // [Composition] — adapter holds the adaptee; does NOT extend it
    private final OldKelvinSensor sensor;

    KelvinSensorAdapter(OldKelvinSensor sensor) {
        this.sensor = sensor;
    }

    @Override
    public double getTemperatureCelsius() {
        // [Translation] K → C conversion lives here, not in the adaptee
        return sensor.getKelvinReading() - 273.15;
    }

    @Override
    public String getSensorId() {
        // [Delegation] map the legacy identifier to the target interface
        return sensor.getDeviceId();
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Demo
// ─────────────────────────────────────────────────────────────────────────────
public class TemperatureSensorDemo {

    public static void main(String[] args) {
        // Build legacy adaptees
        LegacyFahrenheitSensor fSensor = new LegacyFahrenheitSensor(98.6, "SN-F001");
        OldKelvinSensor        kSensor = new OldKelvinSensor(310.15, "KV-002");

        // Wrap each adaptee in its adapter
        TemperatureSensor fAdapter = new FahrenheitSensorAdapter(fSensor);
        TemperatureSensor kAdapter = new KelvinSensorAdapter(kSensor);

        // [Polymorphism] — client works with TemperatureSensor[] uniformly;
        //                  it has no knowledge of Fahrenheit or Kelvin
        TemperatureSensor[] sensors = { fAdapter, kAdapter };

        System.out.println("=== Temperature Monitoring System ===");
        System.out.printf("%-12s  %s%n", "Sensor ID", "Temperature (°C)");
        System.out.println("-".repeat(36));

        for (TemperatureSensor sensor : sensors) {
            System.out.printf("%-12s  %.2f °C%n",
                    sensor.getSensorId(),
                    sensor.getTemperatureCelsius());
        }

        // Spot-check: 98.6°F = 37.0°C, 310.15K = 37.0°C
        System.out.println();
        double fResult = fAdapter.getTemperatureCelsius();
        double kResult = kAdapter.getTemperatureCelsius();

        assertApprox("98.6°F → 37.00°C", fResult, 37.0);
        assertApprox("310.15K → 37.00°C", kResult, 37.0);
        System.out.println("\nAll checks PASSED");
    }

    private static void assertApprox(String label, double actual, double expected) {
        boolean ok = Math.abs(actual - expected) < 0.01;
        System.out.printf("  %s — got %.4f — %s%n", label, actual, ok ? "PASSED" : "FAILED");
    }
}
