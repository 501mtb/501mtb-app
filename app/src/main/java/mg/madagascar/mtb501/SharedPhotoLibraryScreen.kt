package mg.madagascar.mtb501

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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

@Composable
fun SharedPhotoLibraryScreen() {
    val repository = remember { PhotoLibraryRepository() }
    val scope = rememberCoroutineScope()
    var library by remember { mutableStateOf<PhotoLibrary?>(null) }
    var selectedCategory by remember { mutableStateOf("all") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        loading = true
        error = null
        try {
            library = repository.load()
        } catch (e: Exception) {
            error = e.localizedMessage ?: "The GitHub photo library could not be loaded."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    val availablePhotos = library?.images?.filter { it.active }.orEmpty()
    val categories = availablePhotos.map { it.category }.distinct().sorted()
    val visiblePhotos = if (selectedCategory == "all") {
        availablePhotos
    } else {
        availablePhotos.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "501MTB PHOTO LIBRARY",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Official photos hosted on GitHub. No photo files are uploaded to Firebase.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!loading && error == null && availablePhotos.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == "all",
                    onClick = { selectedCategory = "all" },
                    label = { Text("All (" + availablePhotos.size + ")") }
                )
                categories.forEach { category ->
                    val count = availablePhotos.count { it.category == category }
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.replaceFirstChar { it.uppercase() } + " (" + count + ")") }
                    )
                }
            }
        }

        when {
            loading -> CircularProgressIndicator(Modifier.padding(20.dp))
            error != null -> {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Button(onClick = { scope.launch { reload() } }) { Text("Retry") }
            }
            visiblePhotos.isEmpty() -> Text("No photos are available in this category yet.")
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(148.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visiblePhotos, key = { it.id }) { photo ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                modifier = Modifier.padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 2
                            )
                            Text(
                                photo.category.uppercase(),
                                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
