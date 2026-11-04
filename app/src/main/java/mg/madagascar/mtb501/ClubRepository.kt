package mg.madagascar.mtb501

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

enum class ApprovalStatus { PENDING, APPROVED, REJECTED, SUSPENDED }
enum class AppRole { USER, ADMIN }
enum class RideStatus { PLANNED, COMPLETED, CANCELLED }
enum class ClubEventType { CARNAVAL, RACE, FESTIVAL, COMMUNITY }

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

data class ClubAttendance(
    val memberId: String = "",
    val memberName: String = "",
    val attended: Boolean = false
)

data class PendingAccount(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val approvalStatus: ApprovalStatus = ApprovalStatus.PENDING
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

    suspend fun getAllMembers(): List<ClubMember> =
        db.collection("members").get().await().documents.mapNotNull { doc ->
            val name = doc.getString("name") ?: return@mapNotNull null
            ClubMember(
                id = doc.id,
                name = name,
                badge = doc.getString("badge") ?: "Rookie",
                photoURL = doc.getString("photoURL"),
                publicProfile = doc.getBoolean("publicProfile") ?: false
            )
        }.sortedBy { it.name }

    suspend fun getRideAttendance(rideId: String): Map<String, ClubAttendance> {
        require(rideId.isNotBlank()) { "Ride ID is missing." }
        return db.collection("rides").document(rideId).collection("attendance")
            .get().await().documents.associate { doc ->
                doc.id to ClubAttendance(
                    memberId = doc.id,
                    memberName = doc.getString("memberName") ?: "",
                    attended = doc.getBoolean("attended") ?: false
                )
            }
    }

    suspend fun setRideAttendance(rideId: String, member: ClubMember, attended: Boolean) {
        require(rideId.isNotBlank()) { "Ride ID is missing." }
        require(member.id.isNotBlank()) { "Member ID is missing." }
        val adminUid = auth.currentUser?.uid ?: error("Administrator sign-in is required.")
        db.collection("rides").document(rideId).collection("attendance").document(member.id)
            .set(
                mapOf(
                    "memberId" to member.id,
                    "memberName" to member.name,
                    "attended" to attended,
                    "recordedBy" to adminUid,
                    "recordedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun getPendingAccounts(): List<PendingAccount> =
        db.collection("users").whereEqualTo("approvalStatus", ApprovalStatus.PENDING.name)
            .get().await().documents.map { doc ->
                PendingAccount(
                    uid = doc.id,
                    email = doc.getString("email") ?: "",
                    displayName = doc.getString("displayName") ?: ""
                )
            }.sortedBy { it.displayName }

    suspend fun reviewAccount(uid: String, approve: Boolean, memberId: String? = null) {
        require(uid.isNotBlank()) { "Account ID is missing." }
        val adminUid = auth.currentUser?.uid ?: error("Administrator sign-in is required.")
        val userRef = db.collection("users").document(uid)
        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            require(userSnapshot.exists()) { "Account no longer exists." }
            require(userSnapshot.getString("approvalStatus") == ApprovalStatus.PENDING.name) {
                "This account has already been reviewed."
            }
            val updates = mutableMapOf<String, Any>(
                "approvalStatus" to if (approve) ApprovalStatus.APPROVED.name else ApprovalStatus.REJECTED.name,
                "reviewedBy" to adminUid,
                "reviewedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            if (approve && !memberId.isNullOrBlank()) {
                val memberRef = db.collection("members").document(memberId)
                val memberSnapshot = transaction.get(memberRef)
                require(memberSnapshot.exists()) { "The selected legacy member does not exist." }
                val linkRef = db.collection("memberLinks").document(memberId)
                transaction.set(linkRef, mapOf(
                    "memberId" to memberId,
                    "userUid" to uid,
                    "linkedBy" to adminUid,
                    "linkedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ))
                updates["memberId"] = memberId
            }
            transaction.update(userRef, updates)
            null
        }.await()
    }

    suspend fun createRide(
        title: String,
        date: String,
        distanceKm: Double,
        location: String,
        facebookLink: String?
    ) {
        require(title.isNotBlank()) { "Ride title is required." }
        require(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { "Date must use YYYY-MM-DD." }
        require(distanceKm > 0.0) { "Distance must be greater than zero." }
        require(location.isNotBlank()) { "Ride location is required." }
        val adminUid = auth.currentUser?.uid ?: error("Administrator sign-in is required.")
        val data = mutableMapOf<String, Any>(
            "title" to title.trim(),
            "date" to date,
            "distanceKm" to distanceKm,
            "location" to location.trim(),
            "type" to "RIDE",
            "status" to RideStatus.PLANNED.name,
            "createdBy" to adminUid,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        if (!facebookLink.isNullOrBlank()) data["facebookLink"] = facebookLink.trim()
        db.collection("rides").document().set(data).await()
    }

    suspend fun createEvent(
        title: String,
        date: String,
        location: String,
        type: ClubEventType,
        description: String
    ) {
        require(title.isNotBlank()) { "Event title is required." }
        require(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { "Date must use YYYY-MM-DD." }
        require(location.isNotBlank()) { "Event location is required." }
        val adminUid = auth.currentUser?.uid ?: error("Administrator sign-in is required.")
        db.collection("events").document().set(
            mapOf(
                "title" to title.trim(),
                "date" to date,
                "location" to location.trim(),
                "type" to type.name,
                "description" to description.trim(),
                "createdBy" to adminUid,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
    }


    /**
     * Saves photo references only. Image files remain hosted on the 501MTB website.
     * PDC accepts an ordered list for its cover slideshow; other targets are single-photo.
     */
    suspend fun savePhotoAssignment(
        slotId: String,
        category: String,
        requiredRatio: String,
        photos: List<PhotoLibraryImage>
    ) {
        require(slotId.isNotBlank()) { "Photo target is missing." }
        require(photos.isNotEmpty()) { "Select at least one photo." }
        val adminUid = auth.currentUser?.uid ?: error("Administrator sign-in is required.")
        val isSlideshow = slotId == "501mtb.pdc"
        require(isSlideshow || photos.size == 1) { "This target accepts one photo only." }
        require(photos.all { it.category == category && it.ratio == requiredRatio }) {
            "One or more photos do not match the target category and ratio."
        }
        db.collection("photoAssignments").document(slotId).set(
            mapOf(
                "slotId" to slotId,
                "category" to category,
                "requiredRatio" to requiredRatio,
                "imageIds" to photos.map { it.id },
                "fileNames" to photos.map { it.fileName },
                "imageUrls" to photos.map { it.url },
                "selectionMode" to if (isSlideshow) "slideshow" else "single",
                "updatedBy" to adminUid,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun getPhotoAssignment(slotId: String): List<String> {
        val snapshot = db.collection("photoAssignments").document(slotId).get().await()
        return snapshot.get("imageIds") as? List<String> ?: emptyList()
    }

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
