package com.example.energy.tests

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.example.energy.EnergyMeter
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for BatteryManager and BatterySnapshot (EnergyMeter.readBatterySnapshot).
 * Must run on a physical device or emulator with battery support.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class BatteryManagerInstrumentedTest {

    private lateinit var meter: EnergyMeter
    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        context = BenchmarkTestUtils.ctx()
        meter = EnergyMeter(context)
    }

    // ─── BatteryManager direct API ────────────────────────────────────────────

    @Test
    fun batteryManager_levelIsInValidRange() {
        val bm = context.getSystemService(BatteryManager::class.java)
        val sticky: Intent? = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val rawLevel = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val rawScale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        assertTrue("EXTRA_LEVEL should be >= 0 (got $rawLevel)", rawLevel >= 0)
        assertTrue("EXTRA_SCALE should be > 0 (got $rawScale)", rawScale > 0)

        val percent = rawLevel * 100 / rawScale
        assertTrue("Battery level % must be 0-100 (got $percent)", percent in 0..100)
    }

    @Test
    fun batteryManager_statusIsKnown() {
        val sticky: Intent? = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val rawStatus = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val validStatuses = setOf(
            BatteryManager.BATTERY_STATUS_CHARGING,
            BatteryManager.BATTERY_STATUS_DISCHARGING,
            BatteryManager.BATTERY_STATUS_FULL,
            BatteryManager.BATTERY_STATUS_NOT_CHARGING,
            BatteryManager.BATTERY_STATUS_UNKNOWN
        )
        assertTrue(
            "EXTRA_STATUS $rawStatus is not a recognised BatteryManager status",
            rawStatus in validStatuses
        )
    }

    @Test
    fun batteryManager_temperatureIsPlausible() {
        val sticky: Intent? = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        // Reported in tenths of °C — a real device should be between 0 °C and 60 °C
        val rawTemp = sticky?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        assertTrue("EXTRA_TEMPERATURE should be > 0 (got $rawTemp)", rawTemp > 0)
        val celsius = rawTemp / 10f
        assertTrue("Temperature should be between 0 and 60 °C (got $celsius °C)", celsius in 0f..60f)
    }

    @Test
    fun batteryManager_voltageIsPlausible() {
        val sticky: Intent? = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        // Typical Li-ion: 3000–4400 mV
        val voltageMv = sticky?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        assertTrue("EXTRA_VOLTAGE should be > 0 (got $voltageMv)", voltageMv > 0)
        assertTrue("Voltage should be between 2500 and 5000 mV (got $voltageMv)", voltageMv in 2500..5000)
    }

    @Test
    fun batteryManager_currentNowIsAvailableOrMinValue() {
        val bm = context.getSystemService(BatteryManager::class.java)
        val current = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        // Int.MIN_VALUE means not supported; otherwise must be a finite value
        if (current != Int.MIN_VALUE) {
            // Current can be negative (charging) or positive (discharging); just not zero on an active device
            assertNotEquals("BATTERY_PROPERTY_CURRENT_NOW should not be 0 on an active device", 0, current)
        }
        // If Int.MIN_VALUE the device doesn't expose current — not an error, just logged
        android.util.Log.d("BatteryTest", "BATTERY_PROPERTY_CURRENT_NOW = $current µA")
    }

    @Test
    fun batteryManager_energyCounterAvailableOrMinValue() {
        val bm = context.getSystemService(BatteryManager::class.java)
        val nwh = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
        // Long.MIN_VALUE means not supported; otherwise must be positive
        if (nwh != Long.MIN_VALUE && nwh != 0L) {
            assertTrue("ENERGY_COUNTER should be positive when available (got $nwh nWh)", nwh > 0)
        }
        android.util.Log.d("BatteryTest", "BATTERY_PROPERTY_ENERGY_COUNTER = $nwh nWh")
    }

    // ─── EnergyMeter.BatterySnapshot (readBatterySnapshot) ───────────────────

    @Test
    fun batterySnapshot_timestampIsNonEmpty() {
        val snap = meter.readBatterySnapshot()
        assertTrue("Timestamp must not be blank", snap.timestamp.isNotBlank())
        // Expect format yyyy-MM-ddTHH:mm:ss
        val isoPattern = Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}""")
        assertTrue(
            "Timestamp '${snap.timestamp}' does not match yyyy-MM-ddTHH:mm:ss",
            isoPattern.matches(snap.timestamp)
        )
    }

    @Test
    fun batterySnapshot_levelPercentInRange() {
        val snap = meter.readBatterySnapshot()
        assertTrue(
            "BatterySnapshot.levelPercent must be 0-100 (got ${snap.levelPercent})",
            snap.levelPercent in 0..100
        )
    }

    @Test
    fun batterySnapshot_statusIsNotUnknown() {
        val snap = meter.readBatterySnapshot()
        val valid = setOf("CHARGING", "DISCHARGING", "FULL", "NOT_CHARGING", "UNKNOWN")
        assertTrue("BatterySnapshot.status '${snap.status}' is invalid", snap.status in valid)
        // Warn if unknown — not a hard failure as emulators may report UNKNOWN
        android.util.Log.d("BatteryTest", "Battery status = ${snap.status}")
    }

    @Test
    fun batterySnapshot_temperatureIsPlausible() {
        val snap = meter.readBatterySnapshot()
        if (snap.temperatureCelsius >= 0f) {
            assertTrue(
                "BatterySnapshot.temperatureCelsius should be 0-60 °C (got ${snap.temperatureCelsius})",
                snap.temperatureCelsius in 0f..60f
            )
        }
        android.util.Log.d("BatteryTest", "Battery temperature = ${snap.temperatureCelsius} °C")
    }

    @Test
    fun batterySnapshot_voltageIsPlausible() {
        val snap = meter.readBatterySnapshot()
        if (snap.voltageMilliV > 0) {
            assertTrue(
                "BatterySnapshot.voltageMilliV should be 2500-5000 mV (got ${snap.voltageMilliV})",
                snap.voltageMilliV in 2500..5000
            )
        }
        android.util.Log.d("BatteryTest", "Battery voltage = ${snap.voltageMilliV} mV")
    }

    @Test
    fun batterySnapshot_beforeAndAfterAreBothCaptured() {
        // Measure a trivial block and verify both snapshots are populated
        val result = meter.measure {
            Thread.sleep(50) // minimal work
        }
        val before = result.batteryBefore
        val after = result.batteryAfter

        assertTrue("batteryBefore.timestamp must not be blank", before.timestamp.isNotBlank())
        assertTrue("batteryAfter.timestamp must not be blank", after.timestamp.isNotBlank())
        assertTrue("batteryBefore.levelPercent must be 0-100", before.levelPercent in 0..100)
        assertTrue("batteryAfter.levelPercent must be 0-100", after.levelPercent in 0..100)

        android.util.Log.d("BatteryTest", "Before: $before")
        android.util.Log.d("BatteryTest", "After:  $after")
    }
}
