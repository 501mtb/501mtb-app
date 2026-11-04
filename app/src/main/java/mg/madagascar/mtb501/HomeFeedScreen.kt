package mg.madagascar.mtb501

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * First implementation pass of UI 1 (Home).
 * Uses the existing rides and public-member registries without changing their schema.
 * Event-specific image assignment, ride GPS coordinates and 501MTB reactions/comments
 * require dedicated data fields and admin publishing workflows in a later pass.
 */
@Composable
fun HomeFeedScreen(onOpenCommunityFeed: () -> Unit = {}) {
    val repository = remember { ClubRepository() }
    var rides by remember { mutableStateOf<List<ClubRide>>(emptyList()) }
    var members by remember { mutableStateOf<List<ClubMember>>(emptyList()) }
    var rideCoverUrl by remember { mutableStateOf<String?>(null) }
    var eventCoverUrl by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var selectedRide by remember { mutableStateOf<ClubRide?>(null) }
    var ridesFailed by remember { mutableStateOf(false) }
    var membersFailed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            rides = repository.getRides()
        } catch (_: Exception) {
            ridesFailed = true
        }
        try {
            members = repository.getPublicMembers().shuffled()
        } catch (_: Exception) {
            membersFailed = true
        }
        // Resolve the administrator-selected shared-library covers. Photo binaries
        // remain on GitHub Pages; only their IDs are stored in Firestore.
        try {
            val photoRepository = PhotoLibraryRepository()
            val library = photoRepository.load()
            val ridePhotoIds = repository.getPhotoAssignment("rides")
            rideCoverUrl = ridePhotoIds.firstOrNull()?.let { id ->
                library.imagesForSlot("rides").firstOrNull { it.id == id }?.url
            }
            val eventPhotoIds = repository.getPhotoAssignment("events")
            eventCoverUrl = eventPhotoIds.firstOrNull()?.let { id ->
                library.imagesForSlot("events").firstOrNull { it.id == id }?.url
            }
        } catch (_: Exception) {
            // Keep the branded placeholders when the online catalogue is unavailable.
        }
        loading = false
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Text("501MTB", modifier = Modifier.padding(horizontal = 18.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)

        if (loading) {
            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (ridesFailed) {
                SectionLoadError("Ride information is temporarily unavailable.")
            } else {
                NextRideSection(
                    rides.filter { it.status == RideStatus.PLANNED }.sortedBy { it.date },
                    onRideClick = { selectedRide = it },
                    coverUrl = rideCoverUrl
                )
                PastEventSection(
                    rides.filter { it.status == RideStatus.COMPLETED }
                        .sortedByDescending { it.date }.take(50),
                    onRideClick = { selectedRide = it },
                    coverUrl = eventCoverUrl
                )
            }
            if (membersFailed) {
                SectionLoadError("Community information is temporarily unavailable.")
            } else {
                CommunitySection(members.take(8))
            }
            TextButton(
                onClick = onOpenCommunityFeed,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp)
            ) {
                Text("OPEN COMMUNITY FEED")
            }
        }
    }

    selectedRide?.let { ride ->
        RideDetailsDialog(ride = ride, onDismiss = { selectedRide = null })
    }
}

@Composable
private fun NextRideSection(
    rides: List<ClubRide>,
    onRideClick: (ClubRide) -> Unit,
    coverUrl: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeading("NEXT RIDE")
        if (rides.isEmpty()) {
            EmptySectionMessage("No upcoming ride published yet.")
        } else {
            LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(rides, key = { it.id }) { ride ->
                    Card(
                        modifier = Modifier.width(310.dp).clickable { onRideClick(ride) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            RidePhotoPlaceholder(ride.title, Modifier.fillMaxWidth().aspectRatio(16f / 9f), coverUrl)
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(ride.date, color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text(ride.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("${ride.type} • ${ride.location}", style = MaterialTheme.typography.bodyMedium)
                                Text("Open ride details  →", style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PastEventSection(
    rides: List<ClubRide>,
    onRideClick: (ClubRide) -> Unit,
    coverUrl: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeading("PAST EVENT")
        Text("MTB Event", modifier = Modifier.padding(horizontal = 18.dp),
            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (rides.isEmpty()) {
            EmptySectionMessage("Completed rides will appear here.")
        } else {
            LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(rides, key = { it.id }) { ride ->
                    Card(
                        modifier = Modifier.width(240.dp).clickable { onRideClick(ride) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            RidePhotoPlaceholder(ride.title, Modifier.fillMaxWidth().aspectRatio(4f / 3f), coverUrl)
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(ride.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${ride.distanceKm} km • ${ride.date}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunitySection(members: List<ClubMember>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeading("COMMUNITY")
        if (members.isEmpty()) {
            EmptySectionMessage("Rider stories will appear here.")
        } else {
            LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(members, key = { it.id }) { member ->
                    Column(Modifier.width(142.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                            if (!member.photoURL.isNullOrBlank()) {
                                AsyncImage(model = member.photoURL, contentDescription = member.name,
                                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Text("501MTB", modifier = Modifier.align(Alignment.Center),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Black)
                            }
                            AssistChip(onClick = { }, label = { Text(member.badge) },
                                modifier = Modifier.align(Alignment.TopStart).padding(5.dp))
                            Text("PROFILE →", modifier = Modifier.align(Alignment.BottomEnd)
                                .padding(8.dp).background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 5.dp),
                                color = Color.White, style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold)
                        }
                        Text(member.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RidePhotoPlaceholder(
    title: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null
) {
    Box(
        modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                "501MTB",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(title, modifier = Modifier.padding(horizontal = 18.dp),
        style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black)
}

@Composable
private fun EmptySectionMessage(message: String) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(message, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionLoadError(message: String) {
    Text(
        message,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun RideDetailsDialog(ride: ClubRide, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ride.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ride.date, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("${ride.distanceKm} km")
                Text(ride.location)
                Text(ride.type)
            }
        },
        confirmButton = {
            if (!ride.facebookLink.isNullOrBlank()) {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ride.facebookLink)))
                }) { Text("VIEW FACEBOOK ALBUM") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } }
    )
}
