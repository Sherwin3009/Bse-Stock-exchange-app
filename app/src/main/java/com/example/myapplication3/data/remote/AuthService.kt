package com.example.myapplication3.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    suspend fun signUp(email: String, pass: String, fullName: String): Result<String> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid ?: throw Exception("User creation failed")

            val userData = hashMapOf(
                "uid" to uid,
                "name" to fullName,
                "email" to email,
                "balance" to 980763.06,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(uid).set(userData).await()

            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, pass: String): Result<String> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid ?: throw Exception("Login failed")
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // --- Wallet & Database Operations ---

    suspend fun getUserBalance(): Double {
        val uid = currentUserId ?: return 0.0
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            snapshot.getDouble("balance") ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun buyStock(
        symbol: String,
        companyName: String,
        quantity: Int,
        pricePerShare: Double
    ): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(Exception("User not authenticated"))
        val totalCost = quantity * pricePerShare

        return try {
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection("users").document(uid)
                val holdingRef = userRef.collection("holdings").document(symbol)

                // --- 1. ALL READS MUST HAPPEN FIRST ---
                val userSnapshot = transaction.get(userRef)
                val holdingSnapshot = transaction.get(holdingRef)

                val currentBalance = if (userSnapshot.exists() && userSnapshot.contains("balance")) {
                    userSnapshot.getDouble("balance") ?: 0.0
                } else {
                    1000000.0 // Default demo balance if document or field is new
                }

                if (currentBalance < totalCost) {
                    throw Exception("Insufficient funds")
                }

                val newBalance = currentBalance - totalCost

                // --- 2. ALL WRITES HAPPEN AFTER ALL READS ---
                if (!userSnapshot.exists()) {
                    val newUserData = hashMapOf(
                        "uid" to uid,
                        "email" to (auth.currentUser?.email ?: ""),
                        "balance" to newBalance,
                        "createdAt" to System.currentTimeMillis()
                    )
                    transaction.set(userRef, newUserData)
                } else {
                    transaction.update(userRef, "balance", newBalance)
                }

                if (holdingSnapshot.exists()) {
                    val existingQty = holdingSnapshot.getLong("quantity")?.toInt() ?: 0
                    val existingAvg = holdingSnapshot.getDouble("avgPrice") ?: pricePerShare
                    val newQty = existingQty + quantity
                    val newAvgPrice = ((existingAvg * existingQty) + (pricePerShare * quantity)) / newQty

                    transaction.update(
                        holdingRef,
                        mapOf(
                            "quantity" to newQty,
                            "avgPrice" to newAvgPrice,
                            "lastUpdated" to System.currentTimeMillis()
                        )
                    )
                } else {
                    val holdingData = hashMapOf(
                        "symbol" to symbol,
                        "companyName" to companyName,
                        "quantity" to quantity,
                        "avgPrice" to pricePerShare,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    transaction.set(holdingRef, holdingData)
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sellStock(
        symbol: String,
        quantity: Int,
        pricePerShare: Double
    ): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(Exception("User not authenticated"))
        val totalCredit = quantity * pricePerShare

        return try {
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection("users").document(uid)
                val holdingRef = userRef.collection("holdings").document(symbol)

                // 1. All Reads First
                val userSnapshot = transaction.get(userRef)
                val holdingSnapshot = transaction.get(holdingRef)

                if (!holdingSnapshot.exists()) {
                    throw Exception("You do not own any shares of $symbol")
                }

                val currentQty = holdingSnapshot.getLong("quantity")?.toInt() ?: 0
                if (currentQty < quantity) {
                    throw Exception("Insufficient stock holding")
                }

                val currentBalance = userSnapshot.getDouble("balance") ?: 0.0
                val newBalance = currentBalance + totalCredit
                val remainingQty = currentQty - quantity

                // 2. All Writes After Reads
                transaction.update(userRef, "balance", newBalance)

                if (remainingQty > 0) {
                    // Reduce quantity
                    transaction.update(
                        holdingRef,
                        mapOf(
                            "quantity" to remainingQty,
                            "lastUpdated" to System.currentTimeMillis()
                        )
                    )
                } else {
                    // Delete document completely if all shares are sold
                    transaction.delete(holdingRef)
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}