package mg.madagascar.mtb501

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real Android FirebaseAuth/Firestore SDK and OfficialMemberRegistry methods.
 * Both Firebase services point only to the local emulators (10.0.2.2), never production.
 */
@RunWith(AndroidJUnit4::class)
class OfficialMemberRegistryEmulatorTest {
    private lateinit var registry: OfficialMemberRegistry

    @Before
    fun signInAndResetEmulatorData() = runBlocking {
        auth.signOut()
        auth.signInWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD).await()
        registry = OfficialMemberRegistry(db, auth)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val registerText = context.assets.open("501mtb_member_registry.json")
            .bufferedReader().use { it.readText() }
        val ids = parseOfficialRegister(registerText).map { it.memberId } +
            listOf("M-056", "W-057", "M-058", "W-059")
        val batch = db.batch()
        ids.forEach { id -> batch.delete(db.collection("members").document(id)) }
        batch.delete(db.collection("counters").document("memberSequence"))
        batch.commit().await()
    }

    @Test
    fun initializeOfficialRegisterWritesAll55MembersAndCounter() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assertEquals(55, registry.initializeOfficialRegister(context))
        assertEquals(55, registry.getOfficialMembers().size)
        assertEquals(
            55L,
            db.collection("counters").document("memberSequence").get().await()
                .getLong("lastIssued")
        )
    }

    @Test
    fun secondInitializationIsRejectedWithoutChangingImportedMembers() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        registry.initializeOfficialRegister(context)

        val failure = runCatching { registry.initializeOfficialRegister(context) }.exceptionOrNull()
        assertNotNull("A second import must be rejected.", failure)
        assertEquals(55, registry.getOfficialMembers().size)
        assertEquals(
            55L,
            db.collection("counters").document("memberSequence").get().await()
                .getLong("lastIssued")
        )
    }

    @Test
    fun partialExistingRegisterIsPreservedAndBlocksInitialization() = runBlocking {
        db.collection("members").document("M-001").set(
            mapOf(
                "memberId" to "M-001",
                "name" to "KEEP EXISTING RECORD",
                "gender" to "M",
                "category" to "ROOKIE",
                "publicProfile" to true,
                "active" to true
            )
        ).await()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val failure = runCatching { registry.initializeOfficialRegister(context) }.exceptionOrNull()

        assertNotNull("A partial register must block the import.", failure)
        assertEquals(
            "KEEP EXISTING RECORD",
            db.collection("members").document("M-001").get().await().getString("name")
        )
        assertEquals(1, registry.getOfficialMembers().size)
    }

    @Test
    fun addMemberUsesOneSharedSequenceForMaleAndFemaleMembers() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        registry.initializeOfficialRegister(context)

        assertEquals("M-056", registry.addMember("Integration Rider M", "M", "ROOKIE").memberId)
        assertEquals("W-057", registry.addMember("Integration Rider W", "W", "ROOKIE").memberId)
        assertEquals(
            57L,
            db.collection("counters").document("memberSequence").get().await()
                .getLong("lastIssued")
        )
    }

    @Test
    fun concurrentAndroidTransactionsAllocateDistinctSequentialIds() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        registry.initializeOfficialRegister(context)

        val created = coroutineScope {
            listOf(
                async { registry.addMember("Concurrent Android A", "M", "ROOKIE") },
                async { registry.addMember("Concurrent Android B", "W", "ROOKIE") }
            ).awaitAll()
        }

        val numbers = created.map { it.memberId.substring(2).toInt() }.sorted()
        assertEquals(listOf(56, 57), numbers)
        assertEquals(2, created.map { it.memberId }.toSet().size)
        assertEquals(
            57L,
            db.collection("counters").document("memberSequence").get().await()
                .getLong("lastIssued")
        )
    }

    @Test
    fun addMemberBeforeInitializationIsRejected() = runBlocking {
        val failure = runCatching {
            registry.addMember("Early Android Rider", "M", "ROOKIE")
        }.exceptionOrNull()
        assertNotNull("Adding before initialization must be rejected.", failure)
        assertTrue(
            db.collection("counters").document("memberSequence").get().await().exists().not()
        )
    }

    companion object {
        private const val ADMIN_EMAIL = "android-registry-admin@501mtb.test"
        private const val ADMIN_PASSWORD = "local-emulator-only-password-501mtb"

        private lateinit var auth: FirebaseAuth
        private lateinit var db: FirebaseFirestore

        @BeforeClass
        @JvmStatic
        fun connectOnlyToLocalFirebaseEmulators() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:000000000000:android:501mtbtest")
                .setApiKey("fake-api-key")
                .setProjectId("demo-501mtb")
                .build()
            val app = FirebaseApp.initializeApp(context, options, "501MTBRegistryEmulatorTests")
            auth = FirebaseAuth.getInstance(app)
            auth.useEmulator("10.0.2.2", 9099)
            db = FirebaseFirestore.getInstance(app)
            db.useEmulator("10.0.2.2", 8080)
        }
    }
}
