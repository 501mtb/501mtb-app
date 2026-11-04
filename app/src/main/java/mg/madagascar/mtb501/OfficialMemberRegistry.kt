package mg.madagascar.mtb501

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

data class OfficialMember(
    val memberId: String,
    val name: String,
    val gender: String,
    val category: String,
    val active: Boolean = true
)

/** Parse and validate the bundled official register before any Firestore operation. */
internal fun parseOfficialRegister(text: String): List<OfficialMember> {
    val root = JSONObject(text)
    require(root.optString("registry") == "501MTB" && root.optString("status") == "official") {
        "The bundled register is not the official 501MTB register."
    }
    val array = root.getJSONArray("members")
    require(array.length() == 55) { "The official register must contain 55 members." }
    val parsed = (0 until array.length()).map { i ->
        val item = array.getJSONObject(i)
        val id = item.getString("memberId")
        val gender = item.getString("gender")
        val name = item.getString("name").trim()
        val category = item.getString("category").trim()
        require(id.matches(Regex("[MW]-\\d{3}")) && id[0].toString() == gender) {
            "Invalid official member ID or gender: $id."
        }
        require(name.isNotBlank() && category.isNotBlank()) {
            "Official member name and category are required."
        }
        OfficialMember(id, name, gender, category)
    }
    val numbers = parsed.map { it.memberId.substring(2).toInt() }
    require(parsed.map { it.memberId }.toSet().size == 55) { "Duplicate official member IDs." }
    require(numbers.toSet().size == 55 && numbers.sorted() == (1..55).toList()) {
        "Official member IDs must use the complete sequence 001 to 055."
    }
    return parsed
}

/** M and W members share one global sequence. */
internal fun nextOfficialMemberId(gender: String, lastIssued: Long): String {
    require(gender == "M" || gender == "W") { "Gender must be M or W." }
    val next = lastIssued + 1L
    require(next in 1L..999L) { "The official member sequence is exhausted." }
    return "$gender-${next.toString().padStart(3, '0')}"
}

/** Official register. Member document IDs are immutable; M/W share one global sequence. */
class OfficialMemberRegistry(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val members = db.collection("members")
    private val sequence = db.collection("counters").document("memberSequence")

    suspend fun getOfficialMembers(): List<OfficialMember> =
        members.get().await().documents.mapNotNull { doc ->
            val name = doc.getString("name") ?: return@mapNotNull null
            OfficialMember(
                memberId = doc.id,
                name = name,
                gender = doc.getString("gender") ?: "",
                category = doc.getString("category") ?: doc.getString("badge") ?: "ROOKIE",
                active = doc.getBoolean("active") ?: true
            )
        }.sortedBy { it.memberId }

    /** One-time import. Existing documents are never overwritten. */
    suspend fun initializeOfficialRegister(context: Context): Int {
        checkAdmin()
        val text = context.assets.open("501mtb_member_registry.json").bufferedReader().use { it.readText() }
        val parsed = parseOfficialRegister(text)

        return db.runTransaction { tx ->
            val existing = parsed.map { tx.get(members.document(it.memberId)) }
            val counter = tx.get(sequence)
            require(existing.none { it.exists() }) {
                "Some official member IDs already exist. Reconcile Firestore before importing."
            }
            require(!counter.exists() || (counter.getLong("lastIssued") ?: 0L) <= 55L) {
                "The member sequence is already ahead. Manual reconciliation is required."
            }
            val uid = auth.currentUser!!.uid
            parsed.forEach { member ->
                tx.set(members.document(member.memberId), mapOf(
                    "memberId" to member.memberId, "name" to member.name,
                    "gender" to member.gender, "category" to member.category,
                    "badge" to member.category, "active" to true, "publicProfile" to true,
                    "createdAt" to FieldValue.serverTimestamp(), "createdBy" to uid
                ))
            }
            tx.set(sequence, mapOf(
                "lastIssued" to 55L, "updatedAt" to FieldValue.serverTimestamp(), "updatedBy" to uid
            ))
            parsed.size
        }.await()
    }

    /** Atomic global ID allocation prevents two admins receiving the same number. */
    suspend fun addMember(name: String, gender: String, category: String): OfficialMember {
        checkAdmin()
        require(name.isNotBlank() && (gender == "M" || gender == "W") && category.isNotBlank())
        val uid = auth.currentUser!!.uid
        return db.runTransaction { tx ->
            val counter = tx.get(sequence)
            require(counter.exists()) { "Initialize the official register before adding members." }
            val lastIssued = counter.getLong("lastIssued") ?: 55L
            val id = nextOfficialMemberId(gender, lastIssued)
            val ref = members.document(id)
            require(!tx.get(ref).exists()) { "Generated ID already exists." }
            val next = id.substring(2).toLong()
            tx.set(ref, mapOf(
                "memberId" to id, "name" to name.trim(), "gender" to gender,
                "category" to category.trim(), "badge" to category.trim(),
                "active" to true, "publicProfile" to true,
                "createdAt" to FieldValue.serverTimestamp(), "createdBy" to uid
            ))
            tx.update(sequence, mapOf(
                "lastIssued" to next, "updatedAt" to FieldValue.serverTimestamp(), "updatedBy" to uid
            ))
            OfficialMember(id, name.trim(), gender, category.trim())
        }.await()
    }

    private suspend fun checkAdmin() {
        val uid = auth.currentUser?.uid ?: error("Administrator sign-in is required.")
        val user = db.collection("users").document(uid).get().await()
        check(user.getString("role") == "ADMIN") { "Administrator permission is required." }
    }
}
