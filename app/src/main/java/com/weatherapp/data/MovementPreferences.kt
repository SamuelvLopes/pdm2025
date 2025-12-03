package com.weatherapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val MOVEMENT_PREFS = "movement_prefs"

val Context.movementDataStore: DataStore<Preferences> by preferencesDataStore(
    name = MOVEMENT_PREFS
)

object MovementPreferences {
    private val keyLastWorkoutAt = longPreferencesKey("last_workout_at")
    private val keyStreak = intPreferencesKey("last_streak")
    private val keyPoints = intPreferencesKey("last_points")

    fun cacheFlow(context: Context): Flow<MovementCache> {
        return context.movementDataStore.data.map { prefs ->
            MovementCache(
                lastWorkoutAt = prefs[keyLastWorkoutAt] ?: 0L,
                streakCount = prefs[keyStreak] ?: 0,
                pointsTotal = prefs[keyPoints] ?: 0
            )
        }
    }

    suspend fun updateCache(
        context: Context,
        lastWorkoutAt: Long,
        streakCount: Int,
        pointsTotal: Int
    ) {
        context.movementDataStore.edit { prefs ->
            prefs[keyLastWorkoutAt] = lastWorkoutAt
            prefs[keyStreak] = streakCount
            prefs[keyPoints] = pointsTotal
        }
    }
}
