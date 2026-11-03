package mg.madagascar.mtb501

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

enum class ApprovalStatus { PENDING, APPROVED, REJECTED, SUSPENDED }
enum class AppRole { USER, ADMIN }
enum class RideStatus { PLANNED, COMPLETED, CANCELLED }
enum class ClubEventType { CARNIVAL, RACE, FESTIVAL, COMMUNITY }

data class ClubUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoURL: String? = null,
    val role: AppRole = AppRole.USER,
    val approvalStatus: ApprovalStatus = ApprovalStatus.PENDING,
    val memberId: String? = null
)

data class ClubRide(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val distanceKm: Double = 0.0,
    val location: String = "",
    val type: String = "RIDE",
    val status: RideStatus = RideStatus.PLANNED,
    val facebookLink: String? = null
)

data class ClubEvent(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val location: String = "",
    val type: ClubEventType = ClubEventType.COMMUNITY,
    val description: String = ""
)

data class ClubMember(
    val id: String = "",
    val name: String = "",
    val badge: String = "Rookie",
    val photoURL: String? = null,
    val publicProfile: Boolean = true
)

data class ClubSummary(
    val totalRides: Long = 0,
    val totalDistanceKm: Double = 0.0,
    val activeMembers: Long = 0
)

/**
 * Firestore access layer. Security is enforced by firestore.rules, never by UI checks alone.
 * Public collections are readable without login; member actions require an approved account.
 */
class ClubRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun ensurePendingAccount() {
        val user = auth.currentUser ?: return
        val ref = db.collection("users").document(user.uid)
        val existing = ref.get().await()
        if (!existing.exists()) {
            ref.set(
                mapOf(
                    "uid" to user.uid,
                    "email" to (user.email ?: ""),
                    "displayName" to (user.displayName ?: ""),
                    "photoURL" to user.photoUrl?.toString(),
                    "role" to AppRole.USER.name,
                    "approvalStatus" to ApprovalStatus.PENDING.name,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun getMyAccount(): ClubUser? {
        val uid = auth.currentUser?.uid ?: return null
        val snapshot = db.collection("users").document(uid).get().await()
        if (!snapshot.exists()) return null
        return ClubUser(
            uid = uid,
            email = snapshot.getString("email") ?: "",
            displayName = snapshot.getString("displayName") ?: "",
            photoURL = snapshot.getString("photoURL"),
            role = enumValueOrDefault(snapshot.getString("role"), AppRole.USER),
            approvalStatus = enumValueOrDefault(snapshot.getString("approvalStatus"), ApprovalStatus.PENDING),
            memberId = snapshot.getString("memberId")
        )
    }

    suspend fun getRides(): List<ClubRide> =
        db.collection("rides").orderBy("date", Query.Direction.DESCENDING).get().await()
            .documents.mapNotNull { doc ->
                val title = doc.getString("title") ?: return@mapNotNull null
                ClubRide(
                    id = doc.id,
                    title = title,
                    date = doc.getString("date") ?: "",
                    distanceKm = doc.getDouble("distanceKm") ?: 0.0,
                    location = doc.getString("location") ?: "",
                    type = doc.getString("type") ?: "RIDE",
                    status = enumValueOrDefault(doc.getString("status"), RideStatus.PLANNED),
                    facebookLink = doc.getString("facebookLink")
                )
            }

    suspend fun getEvents(): List<ClubEvent> =
        db.collection("events").orderBy("date", Query.Direction.ASCENDING).get().await()
            .documents.mapNotNull { doc ->
                val title = doc.getString("title") ?: return@mapNotNull null
                ClubEvent(
                    id = doc.id,
                    title = title,
                    date = doc.getString("date") ?: "",
                    location = doc.getString("location") ?: "",
                    type = enumValueOrDefault(doc.getString("type"), ClubEventType.COMMUNITY),
                    description = doc.getString("description") ?: ""
                )
            }

    suspend fun getPublicMembers(): List<ClubMember> =
        db.collection("members").whereEqualTo("publicProfile", true).get().await()
            .documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                ClubMember(
                    id = doc.id,
                    name = name,
                    badge = doc.getString("badge") ?: "Rookie",
                    photoURL = doc.getString("photoURL"),
                    publicProfile = true
                )
            }.sortedBy { it.name }

    suspend fun getClubSummary(): ClubSummary {
        val snapshot = db.collection("publicStats").document("clubSummary").get().await()
        return ClubSummary(
            totalRides = snapshot.getLong("totalRides") ?: 0,
            totalDistanceKm = snapshot.getDouble("totalDistanceKm") ?: 0.0,
            activeMembers = snapshot.getLong("activeMembers") ?: 0
        )
    }

    suspend fun registerForRide(rideId: String) {
        val user = auth.currentUser ?: error("Sign in is required.")
        db.collection("rides").document(rideId)
            .collection("registrations").document(user.uid)
            .set(
                mapOf(
                    "uid" to user.uid,
                    "status" to "REGISTERED",
                    "registeredAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun cancelRideRegistration(rideId: String) {
        val uid = auth.currentUser?.uid ?: error("Sign in is required.")
        db.collection("rides").document(rideId)
            .collection("registrations").document(uid).delete().await()
    }

    fun listenToRides(onChange: (List<ClubRide>) -> Unit, onError: (Exception) -> Unit): ListenerRegistration =
        db.collection("rides").orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { onError(error); return@addSnapshotListener }
                val rides = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    ClubRide(
                        id = doc.id,
                        title = title,
                        date = doc.getString("date") ?: "",
                        distanceKm = doc.getDouble("distanceKm") ?: 0.0,
                        location = doc.getString("location") ?: "",
                        type = doc.getString("type") ?: "RIDE",
                        status = enumValueOrDefault(doc.getString("status"), RideStatus.PLANNED),
                        facebookLink = doc.getString("facebookLink")
                    )
                }
                onChange(rides)
            }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback
}
