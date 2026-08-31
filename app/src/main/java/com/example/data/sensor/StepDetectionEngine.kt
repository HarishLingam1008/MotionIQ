package com.example.data.sensor

import android.util.Log

/**
 * Authoritative Hardware Step Detection Engine for MotionIQ.
 *
 * Core Principles:
 * 1. Hardware-First Hierarchy:
 *    - Priority 1: Sensor.TYPE_STEP_COUNTER (Authoritative Primary Source)
 *      Formula: todaySteps = max(0, (currentSensorValue - sensorBaseline) + baseOffsetSteps)
 *    - Priority 2: Sensor.TYPE_STEP_DETECTOR (Secondary Hardware Fallback)
 *      Formula: todaySteps += 1 (debounced)
 *    - If neither hardware step sensor is available: reports "Step sensor unavailable on this device"
 *      and strictly generates ZERO fake/synthetic/accelerometer steps.
 *
 * 2. Robust Baseline & Rollover Management:
 *    - Calendar Day Rollover (Midnight): Anchors new baseline to current raw sensor total, starts today at 0.
 *    - Device Reboot Handling: Detects counter reset (raw < baseline or raw < lastRaw), preserves prior steps in baseOffsetSteps.
 *    - Manual Step Reset: Anchors baseline to current raw sensor total, clears baseOffsetSteps, sets today to 0.
 *    - App Restart Persistence: Restores baseline, offset, last raw sensor, and todaySteps without jumps or loss.
 *
 * 3. Validation & Anti-Duplicate Protection:
 *    - Delta check: rejects negative, duplicate, or corrupt jumps.
 */
class StepDetectionEngine {

    companion object {
        private const val TAG = "MotionIQ_StepEngine"
        const val MIN_STEP_INTERVAL_MS = 200L
        const val MAX_REASONABLE_SINGLE_DELTA = 50000

        private fun safeLog(priority: Int, tag: String, msg: String) {
            try {
                if (priority == Log.INFO) Log.i(tag, msg) else Log.d(tag, msg)
            } catch (_: Throwable) {
                // Ignore in pure JVM unit tests without Android Log mock
            }
        }
    }

    // Step counting state
    var todaySteps: Int = 0; private set
    var sensorBaseline: Float = -1f; private set
    var baseOffsetSteps: Int = 0; private set
    var lastRawSensorValue: Float = -1f; private set
    var currentDateStr: String = ""; private set

    // Authoritative Sensor Source in use
    var activeSensorSource: String = "INITIALIZING"; private set

    // Diagnostics & Validation state
    var lastStepTimestamp: Long = 0L; private set
    var totalStepsValidated: Int = 0; private set

    /**
     * Initializes baseline state from persistent storage.
     */
    fun initializeBaseline(
        todayDate: String,
        savedDate: String,
        savedBaseline: Float,
        savedBaseOffset: Int,
        savedLastRaw: Float,
        savedSteps: Int
    ) {
        currentDateStr = todayDate
        if (savedDate == todayDate && savedDate.isNotBlank()) {
            sensorBaseline = savedBaseline
            baseOffsetSteps = savedBaseOffset
            lastRawSensorValue = savedLastRaw
            todaySteps = savedSteps.coerceAtLeast(0)
            safeLog(
                Log.INFO,
                TAG,
                "Restored today's baseline: baseline=$sensorBaseline, offset=$baseOffsetSteps, todaySteps=$todaySteps"
            )
        } else {
            // New calendar day rollover
            sensorBaseline = if (savedLastRaw >= 0f) savedLastRaw else -1f
            baseOffsetSteps = 0
            lastRawSensorValue = savedLastRaw
            todaySteps = 0
            lastStepTimestamp = 0L
            safeLog(Log.INFO, TAG, "New calendar day initialized: $todayDate (0 steps, baseline=$sensorBaseline)")
        }
    }

    /**
     * Handles Calendar Day Rollover (e.g., Midnight).
     * Re-anchors baseline to the last known hardware sensor value and starts today's count from zero.
     */
    fun handleDateRollover(todayDate: String): Boolean {
        if (todayDate.isNotBlank() && todayDate != currentDateStr) {
            safeLog(Log.INFO, TAG, "Daily midnight rollover: $currentDateStr -> $todayDate")
            currentDateStr = todayDate
            sensorBaseline = if (lastRawSensorValue >= 0f) lastRawSensorValue else -1f
            baseOffsetSteps = 0
            todaySteps = 0
            lastStepTimestamp = 0L
            return true
        }
        return false
    }

    /**
     * Process hardware Sensor.TYPE_STEP_COUNTER (Authoritative Primary Source).
     *
     * TYPE_STEP_COUNTER returns the cumulative steps taken by the user since the last device boot.
     * Calculated steps = max(0, (rawSensorSteps - sensorBaseline)) + baseOffsetSteps
     *
     * Returns true if step count increased.
     */
    fun processHardwareStepCounter(
        rawSensorSteps: Float,
        now: Long
    ): Boolean {
        activeSensorSource = "STEP_COUNTER"

        if (rawSensorSteps < 0f) return false

        // 1. Initial Baseline Setup (first reading of the day or after fresh app install)
        if (sensorBaseline < 0f) {
            sensorBaseline = rawSensorSteps
            lastRawSensorValue = rawSensorSteps
            baseOffsetSteps = todaySteps // Preserve any pre-existing step count
            safeLog(
                Log.INFO,
                TAG,
                "[STEP_COUNTER] Initialized baseline=$sensorBaseline, baseOffset=$baseOffsetSteps, todaySteps=$todaySteps"
            )
            return false
        }

        // 2. Mid-Day Device Reboot Recovery
        // When device reboots, hardware step counter resets to 0 (or small count < sensorBaseline / lastRawSensorValue).
        if (rawSensorSteps < sensorBaseline || (lastRawSensorValue >= 0f && rawSensorSteps < lastRawSensorValue)) {
            safeLog(
                Log.INFO,
                TAG,
                "[STEP_COUNTER] Device reboot detected! Raw ($rawSensorSteps) < Baseline ($sensorBaseline). Preserving $todaySteps steps."
            )
            baseOffsetSteps = todaySteps // Preserve steps counted before reboot
            sensorBaseline = rawSensorSteps // New baseline from current boot
            lastRawSensorValue = rawSensorSteps
            return false
        }

        // 3. Duplicate callback check & Delta validation
        val deltaFromLast = if (lastRawSensorValue >= 0f) (rawSensorSteps - lastRawSensorValue).toInt() else 1
        if (deltaFromLast <= 0) {
            // Duplicate event or zero change
            lastRawSensorValue = rawSensorSteps
            return false
        }

        if (deltaFromLast > MAX_REASONABLE_SINGLE_DELTA) {
            safeLog(Log.WARN, TAG, "[STEP_COUNTER] Rejected unreasonable sensor jump of $deltaFromLast steps.")
            lastRawSensorValue = rawSensorSteps
            return false
        }

        // 4. Calculate today's steps
        lastRawSensorValue = rawSensorSteps
        val hardwareDelta = (rawSensorSteps - sensorBaseline).toInt().coerceAtLeast(0)
        val calculatedSteps = hardwareDelta + baseOffsetSteps

        if (calculatedSteps > todaySteps) {
            val stepDiff = calculatedSteps - todaySteps
            todaySteps = calculatedSteps
            totalStepsValidated += stepDiff
            lastStepTimestamp = now
            safeLog(
                Log.INFO,
                TAG,
                "[STEP_COUNTER] Real step detected (+${stepDiff}). todaySteps=$todaySteps, raw=$rawSensorSteps, baseline=$sensorBaseline, offset=$baseOffsetSteps"
            )
            return true
        }

        return false
    }

    /**
     * Process hardware Sensor.TYPE_STEP_DETECTOR (Secondary Fallback).
     * Used ONLY when Sensor.TYPE_STEP_COUNTER is absent on the device.
     *
     * Each valid event represents exactly 1 detected step.
     */
    fun processHardwareStepDetector(now: Long): Boolean {
        activeSensorSource = "STEP_DETECTOR"

        val interval = now - lastStepTimestamp
        if (lastStepTimestamp > 0L && interval < MIN_STEP_INTERVAL_MS) {
            // Debounce impossible cadence (< 200ms = > 300 steps/min)
            return false
        }

        lastStepTimestamp = now
        todaySteps += 1
        totalStepsValidated += 1
        if (lastRawSensorValue >= 0f) {
            lastRawSensorValue += 1f
        }
        safeLog(Log.INFO, TAG, "[STEP_DETECTOR] Hardware step detected (+1). todaySteps=$todaySteps")
        return true
    }

    /**
     * Resets today's tracked steps back to 0.
     * Sets current hardware step sensor reading as the new daily baseline.
     */
    fun manualReset() {
        if (lastRawSensorValue >= 0f) {
            sensorBaseline = lastRawSensorValue
        } else {
            sensorBaseline = -1f
        }
        baseOffsetSteps = 0
        todaySteps = 0
        totalStepsValidated = 0
        lastStepTimestamp = 0L
        safeLog(Log.INFO, TAG, "[RESET] Today's steps reset to 0. New baseline=$sensorBaseline")
    }

    /**
     * Synchronizes in-memory step engine with persisted today steps from Database,
     * ensuring step count is never lost on cold start.
     */
    fun syncWithSavedSteps(steps: Int) {
        if (steps > todaySteps) {
            val diff = steps - todaySteps
            baseOffsetSteps += diff
            todaySteps = steps
            safeLog(Log.INFO, TAG, "Synchronized with saved DB steps: $steps (baseline=$sensorBaseline, offset=$baseOffsetSteps)")
        }
    }
}
