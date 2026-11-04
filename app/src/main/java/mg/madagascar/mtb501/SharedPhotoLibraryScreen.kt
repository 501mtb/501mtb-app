package mg.madagascar.mtb501

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

private data class SharedPhoto(
    val id: String,
    val authorUid: String,
    val authorName: String,
    val caption: String,
    val imageUrl: String,
    val storagePath: String
)

@Composable
fun SharedPhotoLibraryScreen() {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val storage = remember { FirebaseStorage.getInstance() }
    val repository = remember { ClubRepository() }
    val scope = rememberCoroutineScope()

    var account by remember { mutableStateOf<ClubUser?>(null) }
    var photos by remember { mutableStateOf<List<SharedPhoto>>(emptyList()) }
    var selectedPhoto by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var uploading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedPhoto = uri
        if (uri != null) message = null
    }

    suspend fun refreshPhotos() {
        val snapshot = db.collection("sharedPhotos")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .get().await()
        photos = snapshot.documents.mapNotNull { doc ->
            val imageUrl = doc.getString("imageUrl") ?: return@mapNotNull null
            SharedPhoto(
                id = doc.id,
                authorUid = doc.getString("authorUid") ?: "",
                authorName = doc.getString("authorName") ?: "501MTB member",
                caption = doc.getString("caption") ?: "",
                imageUrl = imageUrl,
                storagePath = doc.getString("storagePath") ?: ""
            )
        }
    }

    LaunchedEffect(auth.currentUser?.uid) {
        loading = true
        try {
            account = repository.getMyAccount()
            if (account != null && (account?.role == AppRole.ADMIN || account?.approvalStatus == ApprovalStatus.APPROVED)) {
                refreshPhotos()
            } else {
                photos = emptyList()
            }
        } catch (error: Exception) {
            message = error.localizedMessage ?: "The shared photo library could not be loaded."
        } finally {
            loading = false
        }
    }

    val user = auth.currentUser
    val isAdmin = account?.role == AppRole.ADMIN
    val canAccess = account != null && (isAdmin || account?.approvalStatus == ApprovalStatus.APPROVED)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("SHARED PHOTO LIBRARY", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text("A common collection of 501MTB ride, event and community photos.", style = MaterialTheme.typography.bodyMedium)

        if (!canAccess) {
            Text(
                if (user == null) "Sign in with an approved 501MTB account to access the shared photo library."
                else "Your account must be approved by an administrator to access the shared photo library.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("ADD TO THE LIBRARY", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("Photo description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    selectedPhoto?.let { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Selected photo preview",
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Button(
                        onClick = { photoPicker.launch("image/*") },
                        enabled = !uploading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedPhoto == null) "Choose a photo" else "Change selected photo")
                    }
                    Button(
                        onClick = {
                            val photo = selectedPhoto ?: return@Button
                            val currentUser = user ?: return@Button
                            scope.launch {
                                uploading = true
                                message = null
                                try {
                                    val fileName = "${UUID.randomUUID()}.jpg"
                                    val path = "sharedPhotos/${currentUser.uid}/$fileName"
                                    val contentType = context.contentResolver.getType(photo) ?: "image/jpeg"
                                    val ref = storage.reference.child(path)
                                    val metadata = StorageMetadata.Builder().setContentType(contentType).build()
                                    ref.putFile(photo, metadata).await()
                                    val downloadUrl = ref.downloadUrl.await().toString()
                                    db.collection("sharedPhotos").document().set(
                                        mapOf(
                                            "authorUid" to currentUser.uid,
                                            "authorName" to (account?.displayName?.ifBlank { null } ?: currentUser.displayName ?: "501MTB member"),
                                            "caption" to caption.trim(),
                                            "imageUrl" to downloadUrl,
                                            "storagePath" to path,
                                            "createdAt" to FieldValue.serverTimestamp()
                                        )
                                    ).await()
                                    selectedPhoto = null
                                    caption = ""
                                    refreshPhotos()
                                    message = "Photo added to the shared library."
                                } catch (error: Exception) {
                                    message = error.localizedMessage ?: "Photo upload failed. Check Firebase Storage and Firestore rules."
                                } finally {
                                    uploading = false
                                }
                            }
                        },
                        enabled = selectedPhoto != null && !uploading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uploading) CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                        else Text("Upload to shared library")
                    }
                }
            }

            RowHeader(
                title = "COMMUNITY PHOTOS",
                count = photos.size,
                onRefresh = {
                    scope.launch {
                        loading = true
                        try {
                            refreshPhotos()
                            message = null
                        } catch (error: Exception) {
                            message = error.localizedMessage ?: "Refresh failed."
                        } finally {
                            loading = false
                        }
                    }
                }
            )

            when {
                loading -> CircularProgressIndicator()
                photos.isEmpty() -> Text("No photos have been shared yet. Add the first one.")
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(photos, key = { it.id }) { photo ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                AsyncImage(
                                    model = photo.imageUrl,
                                    contentDescription = photo.caption.ifBlank { "501MTB shared photo" },
                                    modifier = Modifier.fillMaxWidth().height(150.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(photo.authorName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    if (photo.caption.isNotBlank()) Text(photo.caption, style = MaterialTheme.typography.bodySmall)
                                    if (user?.uid == photo.authorUid || isAdmin) {
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        if (photo.storagePath.isNotBlank()) storage.reference.child(photo.storagePath).delete().await()
                                                        db.collection("sharedPhotos").document(photo.id).delete().await()
                                                        photos = photos.filterNot { it.id == photo.id }
                                                        message = "Photo deleted."
                                                    } catch (error: Exception) {
                                                        message = error.localizedMessage ?: "Photo could not be deleted."
                                                    }
                                                }
                                            }
                                        ) { Text("Delete") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun RowHeader(title: String, count: Int, onRefresh: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$title ($count)", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onRefresh) { Text("Refresh") }
    }
}
