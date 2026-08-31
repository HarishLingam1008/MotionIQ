package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speedKmh: Float = 0f,
    val accuracyMeters: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_routes")
data class SavedRoute(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "guest",
    val activityType: String = "Walking",
    val dateString: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0L,
    val distanceMeters: Double = 0.0,
    val caloriesBurned: Double = 0.0,
    val stepCount: Int = 0,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val startLatitude: Double = 0.0,
    val startLongitude: Double = 0.0,
    val endLatitude: Double = 0.0,
    val endLongitude: Double = 0.0,
    val routePointsJson: String = "[]"
) {
    fun parsePoints(): List<RoutePoint> {
        return try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, RoutePoint::class.java)
            val adapter = moshi.adapter<List<RoutePoint>>(type)
            adapter.fromJson(routePointsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        fun encodePoints(points: List<RoutePoint>): String {
            return try {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val type = Types.newParameterizedType(List::class.java, RoutePoint::class.java)
                val adapter = moshi.adapter<List<RoutePoint>>(type)
                adapter.toJson(points)
            } catch (e: Exception) {
                "[]"
            }
        }
    }
}
