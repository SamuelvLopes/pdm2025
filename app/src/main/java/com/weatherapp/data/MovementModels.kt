package com.weatherapp.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class UserProfile(
    val displayName: String,
    val ageGroup: String,
    val level: String,
    val pointsTotal: Int,
    val streakCount: Int,
    val lastWorkoutAt: Long,
    val pairId: String?
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "displayName" to displayName,
            "ageGroup" to ageGroup,
            "level" to level,
            "pointsTotal" to pointsTotal,
            "streakCount" to streakCount,
            "lastWorkoutAt" to lastWorkoutAt,
            "pairId" to pairId
        )
    }

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): UserProfile {
            val displayName = snapshot.getString("displayName") ?: "Usuario"
            val ageGroup = snapshot.getString("ageGroup") ?: ""
            val level = snapshot.getString("level") ?: ""
            val pointsTotal = (snapshot.getLong("pointsTotal") ?: 0L).toInt()
            val streakCount = (snapshot.getLong("streakCount") ?: 0L).toInt()
            val lastWorkoutAt = readLong(snapshot, "lastWorkoutAt")
            val pairId = snapshot.getString("pairId")
            return UserProfile(
                displayName = displayName,
                ageGroup = ageGroup,
                level = level,
                pointsTotal = pointsTotal,
                streakCount = streakCount,
                lastWorkoutAt = lastWorkoutAt,
                pairId = pairId
            )
        }

        private fun readLong(snapshot: DocumentSnapshot, field: String): Long {
            val value = snapshot.get(field)
            return when (value) {
                is Long -> value
                is Int -> value.toLong()
                is Timestamp -> value.toDate().time
                else -> 0L
            }
        }
    }
}

data class WorkoutSession(
    val type: String,
    val durationSec: Int,
    val steps: Int,
    val points: Int,
    val createdAt: Long,
    val pairId: String?
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "type" to type,
            "durationSec" to durationSec,
            "steps" to steps,
            "points" to points,
            "createdAt" to createdAt,
            "pairId" to pairId
        )
    }
}

data class PairInfo(
    val userAUid: String,
    val userBUid: String,
    val createdAt: Long
)

data class MovementCache(
    val lastWorkoutAt: Long,
    val streakCount: Int,
    val pointsTotal: Int
)

data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val capturedAt: Long,
    val cityName: String? = null
)

enum class TrainingType(val id: String, val label: String) {
    Walk("walk", "Caminhada leve"),
    Stretch("stretch", "Alongamento guiado");

    companion object {
        fun fromId(id: String): TrainingType {
            return entries.firstOrNull { it.id == id } ?: Walk
        }
    }
}

data class TrainingSummary(
    val type: String,
    val steps: Int,
    val points: Int,
    val streak: Int
)
