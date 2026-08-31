package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.sensor.SensorFusionDiagnostics
import com.example.ui.theme.*

@Composable
fun SensorDiagnosticsDialog(
    diagnostics: SensorFusionDiagnostics,
    onDismiss: () -> Unit,
    onResetSteps: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sensor_diagnostics_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Sensor Diagnostics",
                        tint = CyberPrimaryCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sensor Fusion Diagnostics",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. HARDWARE SENSOR AVAILABILITY
                SectionHeader(title = "HARDWARE SENSORS")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SensorBadge(name = "Step Counter", isAvailable = diagnostics.hasStepCounter, modifier = Modifier.weight(1f))
                    SensorBadge(name = "Step Detector", isAvailable = diagnostics.hasStepDetector, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SensorBadge(name = "Accelerometer", isAvailable = diagnostics.hasAccelerometer, modifier = Modifier.weight(1f))
                    SensorBadge(name = "Gyroscope", isAvailable = diagnostics.hasGyroscope, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SensorBadge(name = "Magnetometer", isAvailable = diagnostics.hasMagnetometer, modifier = Modifier.weight(1f))
                    SensorBadge(name = "Rotation Vector", isAvailable = diagnostics.hasRotationVector, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. MOTION CLASSIFICATION & CONFIDENCES
                SectionHeader(title = "MOTION STATE & CONFIDENCE")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current State:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when (diagnostics.movementState.name) {
                                            "WALKING", "RUNNING" -> CyberAccentMint.copy(alpha = 0.2f)
                                            "VEHICLE", "PHONE_MOVEMENT" -> CyberDanger.copy(alpha = 0.2f)
                                            else -> CyberPrimaryCyan.copy(alpha = 0.2f)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = diagnostics.movementState.displayName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = when (diagnostics.movementState.name) {
                                        "WALKING", "RUNNING" -> CyberAccentMint
                                        "VEHICLE", "PHONE_MOVEMENT" -> CyberDanger
                                        else -> CyberPrimaryCyan
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        ConfidenceRow(label = "Movement", value = diagnostics.confidence.movementConfidence, color = CyberPrimaryCyan)
                        ConfidenceRow(label = "Walking", value = diagnostics.confidence.walkingConfidence, color = CyberAccentMint)
                        ConfidenceRow(label = "Running", value = diagnostics.confidence.runningConfidence, color = CyberPinkGlow)
                        ConfidenceRow(label = "Vehicle", value = diagnostics.confidence.vehicleConfidence, color = CyberDanger)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. STEP COUNT & BASELINE METRICS
                SectionHeader(title = "STEP ENGINE & PEDOMETER BASELINE")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        DiagnosticMetricRow("Today's Detected Steps", "${diagnostics.todaySteps} steps")
                        DiagnosticMetricRow("Total Validated In Engine", "${diagnostics.totalStepsValidated} steps")
                        DiagnosticMetricRow("Daily Hardware Baseline", if (diagnostics.dailyBaseline >= 0) "%.0f".format(diagnostics.dailyBaseline) else "Not Established")
                        DiagnosticMetricRow("Raw Hardware Counter", if (diagnostics.currentHardwareStepValue >= 0) "%.0f".format(diagnostics.currentHardwareStepValue) else "N/A")
                        DiagnosticMetricRow("Rejected Shakes/Spikes", "${diagnostics.rejectedShakesCount} events")
                        DiagnosticMetricRow("Primary Source", diagnostics.primarySensor)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. REAL-TIME 3-AXIS SENSOR TELEMETRY
                SectionHeader(title = "REAL-TIME SENSOR TELEMETRY")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val t = diagnostics.telemetry
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Accelerometer (m/s²)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyberPrimaryCyan
                        )
                        TelemetryVectorRow(x = t.ax, y = t.ay, z = t.az)
                        DiagnosticMetricRow("Raw Magnitude", "%.2f m/s²".format(t.rawMag))
                        DiagnosticMetricRow("Linear (No Gravity)", "%.2f m/s²".format(t.linearMag))
                        DiagnosticMetricRow("Filtered Acceleration", "%.2f m/s²".format(t.smoothedMag))
                        DiagnosticMetricRow("Adaptive Threshold", "%.2f m/s²".format(t.dynamicThreshold))

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Gyroscope (rad/s)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyberPinkGlow
                        )
                        TelemetryVectorRow(x = t.gx, y = t.gy, z = t.gz)
                        DiagnosticMetricRow("Angular Velocity Mag", "%.2f rad/s".format(t.gyroMag))

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Magnetometer (µT)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyberAccentMint
                        )
                        TelemetryVectorRow(x = t.mx, y = t.my, z = t.mz)
                        DiagnosticMetricRow("Magnetic Magnitude", "%.1f µT".format(t.magMag))

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Fused Orientation (°)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TelemetryVectorRow(
                            x = t.azimuthDeg,
                            y = t.pitchDeg,
                            z = t.rollDeg,
                            labels = listOf("Azimuth", "Pitch", "Roll")
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onResetSteps) {
                Text("Reset Baseline", color = CyberDanger)
            }
        }
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun SensorBadge(name: String, isAvailable: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isAvailable) CyberAccentMint.copy(alpha = 0.12f) else CyberDanger.copy(alpha = 0.08f),
        modifier = modifier.border(
            width = 1.dp,
            color = if (isAvailable) CyberAccentMint.copy(alpha = 0.3f) else CyberDanger.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isAvailable) CyberAccentMint else CyberDanger)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = if (isAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ConfidenceRow(label: String, value: Float, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(70.dp)
        )
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            color = color,
            trackColor = color.copy(alpha = 0.2f),
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "%.0f%%".format(value * 100f),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(36.dp)
        )
    }
}

@Composable
private fun DiagnosticMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TelemetryVectorRow(
    x: Float,
    y: Float,
    z: Float,
    labels: List<String> = listOf("X", "Y", "Z")
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AxisValueBox(label = labels[0], value = x, modifier = Modifier.weight(1f))
        AxisValueBox(label = labels[1], value = y, modifier = Modifier.weight(1f))
        AxisValueBox(label = labels[2], value = z, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AxisValueBox(label: String, value: Float, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Text(
                text = "%+.2f".format(value),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
