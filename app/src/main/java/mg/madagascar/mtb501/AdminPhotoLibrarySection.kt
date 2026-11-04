package mg.madagascar.mtb501

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private val photoTargets = listOf(
    "501mtb.pdp" to "PDP · 1:1",
    "501mtb.pdc" to "PDC · 16:9 slideshow",
    "rides" to "Rides · 16:9",
    "events" to "Events · 16:9",
    "riders" to "Riders · 3:4",
    "leaders" to "Leaders · 3:4",
    "gallery" to "Gallery · 16:9"
)

@Composable
fun AdminPhotoLibrarySection(
    onMessage: (String) -> Unit
) {
    val repository = remember { PhotoLibraryRepository() }
    val scope = rememberCoroutineScope()
    var library by remember { mutableStateOf<PhotoLibrary?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTarget by remember { mutableStateOf(photoTargets.first().first) }
    var targetMenuExpanded by remember { mutableStateOf(false) }
    var selectedPhoto by remember { mutableStateOf<PhotoLibraryImage?>(null) }

    suspend fun reload() {
        loading = true
        error = null
        try {
            library = repository.load()
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Photo library could not be loaded."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    val currentLibrary = library
    val currentSlot = currentLibrary?.slots?.get(selectedTarget)
    val requiredRatio = currentSlot?.requiredRatio ?: when (selectedTarget) {
        "riders", "leaders" -> "3:4"
        "501mtb.pdp" -> "1:1"
        else -> "16:9"
    }
    val photos = when (selectedTarget) {
        "501mtb.pdp", "501mtb.pdc" -> currentLibrary?.imagesForSlot(selectedTarget).orEmpty()
        else -> currentLibrary?.imagesForRatio(requiredRatio)
            ?.filter { it.category == selectedTarget }.orEmpty()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Shared photo library",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Photos are loaded directly from the official 501MTB website. The app does not upload or duplicate these files.",
                style = MaterialTheme.typography.bodySmall
            )

            OutlinedButton(onClick = { targetMenuExpanded = true }) {
                Text("Target: ${photoTargets.firstOrNull { it.first == selectedTarget }?.second ?: selectedTarget}")
            }
            DropdownMenu(
                expanded = targetMenuExpanded,
                onDismissRequest = { targetMenuExpanded = false }
            ) {
                photoTargets.forEach { (id, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            selectedTarget = id
                            selectedPhoto = null
                            targetMenuExpanded = false
                        }
                    )
                }
            }

            Text("Required ratio: $requiredRatio", style = MaterialTheme.typography.labelLarge)

            when {
                loading -> CircularProgressIndicator()
                error != null -> {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { scope.launch { reload() } }) { Text("Retry") }
                }
                photos.isEmpty() -> {
                    Text(
                        "No compatible photos are listed for this target yet. Add WebP images and their metadata to the website photo catalogue first.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { scope.launch { reload() } }) { Text("Refresh library") }
                }
                else -> {
                    Text("${photos.size} compatible photo(s)")
                    photos.chunked(2).forEach { rowPhotos ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowPhotos.forEach { photo ->
                                Card(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedPhoto = photo
                                        onMessage("Selected ${photo.fileName}. Saving its assignment is the next step.")
                                    }
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        AsyncImage(
                                            model = photo.url,
                                            contentDescription = photo.alt,
                                            modifier = Modifier.fillMaxWidth().aspectRatio(
                                                when (photo.ratio) {
                                                    "3:4" -> 3f / 4f
                                                    "1:1" -> 1f
                                                    else -> 16f / 9f
                                                }
                                            ),
                                            contentScale = ContentScale.Crop
                                        )
                                        Text(
                                            photo.fileName,
                                            modifier = Modifier.padding(horizontal = 7.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 2
                                        )
                                        Text(
                                            "${photo.width} × ${photo.height} · ${photo.ratio}",
                                            modifier = Modifier.padding(start = 7.dp, end = 7.dp, bottom = 7.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            if (rowPhotos.size == 1) {
                                Column(Modifier.weight(1f)) {}
                            }
                        }
                    }
                    selectedPhoto?.let { photo ->
                        Text(
                            "Selected: ${photo.fileName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(onClick = { scope.launch { reload() } }) { Text("Refresh library") }
                }
            }
        }
    }
}
