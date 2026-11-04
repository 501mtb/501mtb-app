package mg.madagascar.mtb501

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class FeedPost(
    val id: String,
    val authorUid: String,
    val authorName: String,
    val caption: String,
    val imageUrl: String,
    val storagePath: String,
    val visibility: String
)

@Composable
fun FeedScreen(onOpenPhotoLibrary: () -> Unit = {}) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val storage = remember { FirebaseStorage.getInstance() }
    val repository = remember { ClubRepository() }
    val scope = rememberCoroutineScope()

    var account by remember { mutableStateOf<ClubUser?>(null) }
    var posts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var rides by remember { mutableStateOf<List<ClubRide>>(emptyList()) }
    var events by remember { mutableStateOf<List<ClubEvent>>(emptyList()) }
    var selectedPhoto by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var officialPost by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var uploading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedPhoto = uri
        if (uri != null) message = null
    }

    suspend fun refreshFeed() {
        val snapshot = db.collection("feedPosts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get().await()
        posts = snapshot.documents.mapNotNull { doc ->
            val imageUrl = doc.getString("imageUrl") ?: return@mapNotNull null
            FeedPost(
                id = doc.id,
                authorUid = doc.getString("authorUid") ?: "",
                authorName = doc.getString("authorName") ?: "501MTB member",
                caption = doc.getString("caption") ?: "",
                imageUrl = imageUrl,
                storagePath = doc.getString("storagePath") ?: "",
                visibility = doc.getString("visibility") ?: "COMMUNITY"
            )
        }
        rides = repository.getRides()
        events = repository.getEvents()
    }

    LaunchedEffect(auth.currentUser?.uid) {
        loading = true
        try {
            account = repository.getMyAccount()
            refreshFeed()
        } catch (e: Exception) {
            message = e.localizedMessage ?: "Feed could not be loaded. Check Firebase setup."
        } finally {
            loading = false
        }
    }

    val isAdmin = account?.role == AppRole.ADMIN
    val canPost = account != null && (isAdmin || account?.approvalStatus == ApprovalStatus.APPROVED)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("501MTB", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text("COMMUNITY • RIDES • EVENTS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

        Text("NEXT RIDE", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        val nextRide = rides.filter { it.status == RideStatus.PLANNED }.minByOrNull { it.date }
        if (nextRide == null) {
            Text("No upcoming ride published yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(nextRide.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text("${nextRide.date} • ${nextRide.location}")
                    Text("${nextRide.distanceKm} km • ${nextRide.type}")
                    if (!nextRide.facebookLink.isNullOrBlank()) Text("Event information available via the ride link.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text("COMMUNITY FEED", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text("Photos, ride moments and 501MTB events shared by the community.", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onOpenPhotoLibrary, modifier = Modifier.fillMaxWidth()) {
            Text("OPEN SHARED PHOTO LIBRARY")
        }

        if (canPost) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Share a photo", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("Caption (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    if (isAdmin) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("Official 501MTB post", modifier = Modifier.weight(1f))
                            Switch(checked = officialPost, onCheckedChange = { officialPost = it })
                        }
                    }
                    selectedPhoto?.let { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Selected photo preview",
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = !uploading, onClick = { photoPicker.launch("image/*") }) {
                            Text(if (selectedPhoto == null) "Choose photo" else "Change photo")
                        }
                        Button(
                            enabled = !uploading && selectedPhoto != null,
                            onClick = {
                                val photo = selectedPhoto ?: return@Button
                                val user = auth.currentUser ?: return@Button
                                scope.launch {
                                    uploading = true
                                    message = null
                                    try {
                                        val fileName = "${UUID.randomUUID()}.jpg"
                                        val path = "feed/${user.uid}/$fileName"
                                        val contentType = context.contentResolver.getType(photo) ?: "image/jpeg"
                                        val ref = storage.reference.child(path)
                                        val metadata = StorageMetadata.Builder().setContentType(contentType).build()
                                        ref.putFile(photo, metadata).await()
                                        val downloadUrl = ref.downloadUrl.await().toString()
                                        db.collection("feedPosts").document().set(
                                            mapOf(
                                                "authorUid" to user.uid,
                                                "authorName" to (account?.displayName?.ifBlank { null } ?: user.displayName ?: "501MTB member"),
                                                "caption" to caption.trim(),
                                                "imageUrl" to downloadUrl,
                                                "storagePath" to path,
                                                "visibility" to if (isAdmin && officialPost) "OFFICIAL" else "COMMUNITY",
                                                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                            )
                                        ).await()
                                        selectedPhoto = null
                                        caption = ""
                                        officialPost = false
                                        refreshFeed()
                                        message = "Photo published successfully."
                                    } catch (e: Exception) {
                                        message = e.localizedMessage ?: "Photo upload failed. Check Firebase Storage and security rules."
                                    } finally {
                                        uploading = false
                                    }
                                }
                            }
                        ) {
                            if (uploading) CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                            else Text("Publish")
                        }
                    }
                }
            }
        } else if (auth.currentUser == null) {
            Text("Sign in with Google to share photos. Only approved members can publish.", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Your account is awaiting administrator approval. You can browse the Feed, but cannot publish yet.", style = MaterialTheme.typography.bodySmall)
        }

        message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }

        when {
            loading -> CircularProgressIndicator()
            posts.isEmpty() -> Text("No photos published yet.")
            else -> posts.forEach { post ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(post.authorName, style = MaterialTheme.typography.titleSmall)
                                if (post.visibility == "OFFICIAL") Text("501MTB OFFICIAL", color = MaterialTheme.colorScheme.primary)
                            }
                            AsyncImage(
                                model = post.imageUrl,
                                contentDescription = post.caption.ifBlank { "501MTB community photo" },
                                modifier = Modifier.fillMaxWidth().height(260.dp)
                            )
                            if (post.caption.isNotBlank()) Text(post.caption)
                            if (auth.currentUser?.uid == post.authorUid || isAdmin) {
                                TextButton(onClick = {
                                    scope.launch {
                                        try {
                                            db.collection("feedPosts").document(post.id).delete().await()
                                            if (post.storagePath.isNotBlank()) storage.reference.child(post.storagePath).delete().await()
                                            refreshFeed()
                                            message = "Publication deleted."
                                        } catch (e: Exception) {
                                            message = e.localizedMessage ?: "Publication could not be deleted."
                                        }
                                    }
                                }) { Text("Delete") }
                            }
                        }
                    }
                }
        }

        events.forEach { event ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(event.type.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(event.title, style = MaterialTheme.typography.titleMedium)
                    Text("${event.date} • ${event.location}")
                    if (event.description.isNotBlank()) Text(event.description)
                }
            }
        }

        Text("PAST RIDES", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        val pastRides = rides.filter { it.status == RideStatus.COMPLETED }.sortedByDescending { it.date }
        if (pastRides.isEmpty()) Text("Completed rides will appear here.")
        pastRides.forEach { ride ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(ride.title, style = MaterialTheme.typography.titleMedium)
                    Text("${ride.date} • ${ride.location}")
                    Text("${ride.distanceKm} km", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
