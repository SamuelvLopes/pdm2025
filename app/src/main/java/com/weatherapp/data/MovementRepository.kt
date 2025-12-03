package com.weatherapp.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object MovementRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun fetchUserProfile(uid: String): UserProfile? {
        val doc = db.collection("users").document(uid).get().await()
        return if (doc.exists()) UserProfile.fromSnapshot(doc) else null
    }

    suspend fun createUserProfile(uid: String, profile: UserProfile) {
        db.collection("users").document(uid).set(profile.toMap(), SetOptions.merge()).await()
    }

    suspend fun updateUserProfile(uid: String, updates: Map<String, Any?>) {
        db.collection("users").document(uid).set(updates, SetOptions.merge()).await()
    }

    suspend fun fetchUserLocation(uid: String): LocationInfo? {
        val doc = db.collection("users").document(uid).get().await()
        if (!doc.exists()) return null
        val lat = doc.getDouble("lastLat") ?: return null
        val lng = doc.getDouble("lastLng") ?: return null
        val accuracy = doc.getDouble("lastLocAcc")?.toFloat()
        val capturedAt = doc.getLong("lastLocAt") ?: 0L
        val cityName = doc.getString("lastCity")
        return LocationInfo(
            latitude = lat,
            longitude = lng,
            accuracyMeters = accuracy,
            capturedAt = capturedAt,
            cityName = cityName
        )
    }

    suspend fun updateUserLocation(uid: String, info: LocationInfo) {
        val data = mutableMapOf<String, Any?>(
            "lastLat" to info.latitude,
            "lastLng" to info.longitude,
            "lastLocAcc" to info.accuracyMeters,
            "lastLocAt" to info.capturedAt
        )
        if (!info.cityName.isNullOrBlank()) {
            data["lastCity"] = info.cityName
        }
        db.collection("users").document(uid).set(data, SetOptions.merge()).await()
    }

    suspend fun addSession(uid: String, session: WorkoutSession): String {
        val ref = db.collection("users").document(uid).collection("sessions").document()
        ref.set(session.toMap()).await()
        return ref.id
    }

    suspend fun addPairSession(pairId: String, session: WorkoutSession) {
        val ref = db.collection("pairs").document(pairId).collection("sessions").document()
        ref.set(session.toMap()).await()
    }

    suspend fun fetchPair(pairId: String): PairInfo? {
        val doc = db.collection("pairs").document(pairId).get().await()
        if (!doc.exists()) return null
        val userAUid = doc.getString("userAUid") ?: return null
        val userBUid = doc.getString("userBUid") ?: return null
        val createdAt = (doc.getLong("createdAt") ?: 0L)
        return PairInfo(userAUid = userAUid, userBUid = userBUid, createdAt = createdAt)
    }

    suspend fun fetchUserDisplayName(uid: String): String? {
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("displayName")
    }

    suspend fun createInvite(inviteCode: String, ownerUid: String) {
        val inviteRef = db.collection("invites").document(inviteCode)
        val data = mapOf(
            "ownerUid" to ownerUid,
            "createdAt" to System.currentTimeMillis(),
            "used" to false
        )
        inviteRef.set(data).await()
    }

    suspend fun redeemInvite(inviteCode: String, scannerUid: String): String? {
        val inviteRef = db.collection("invites").document(inviteCode)
        val pairRef = db.collection("pairs").document()
        val usersRef = db.collection("users")
        val result = db.runTransaction { txn ->
            val inviteSnap = txn.get(inviteRef)
            if (!inviteSnap.exists()) return@runTransaction null
            val used = inviteSnap.getBoolean("used") ?: false
            if (used) return@runTransaction null
            val ownerUid = inviteSnap.getString("ownerUid") ?: return@runTransaction null
            if (ownerUid == scannerUid) return@runTransaction null
            val createdAt = System.currentTimeMillis()
            val pairData = mapOf(
                "userAUid" to ownerUid,
                "userBUid" to scannerUid,
                "createdAt" to createdAt
            )
            txn.set(pairRef, pairData)
            txn.update(
                inviteRef,
                mapOf(
                    "used" to true,
                    "usedByUid" to scannerUid,
                    "usedAt" to createdAt,
                    "pairId" to pairRef.id
                )
            )
            txn.set(usersRef.document(ownerUid), mapOf("pairId" to pairRef.id), SetOptions.merge())
            txn.set(usersRef.document(scannerUid), mapOf("pairId" to pairRef.id), SetOptions.merge())
            pairRef.id
        }.await()
        return result as? String
    }

    suspend fun incrementPoints(uid: String, points: Int) {
        db.collection("users").document(uid)
            .update("pointsTotal", FieldValue.increment(points.toLong()))
            .await()
    }

    suspend fun countUsersInCity(cityName: String): Int {
        val snapshot = db.collection("users")
            .whereEqualTo("lastCity", cityName)
            .get()
            .await()
        return snapshot.size()
    }
}
