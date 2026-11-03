package mg.madagascar.mtb501

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FeedScreen() {
    val repository = remember { ClubRepository() }
    val context = LocalContext.current
    var rides by remember { mutableStateOf<List<ClubRide>>(emptyList()) }
    var members by remember { mutableStateOf<List<ClubMember>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var expandedRide by remember { mutableStateOf<ClubRide?>(null) }
    var commentRide by remember { mutableStateOf<ClubRide?>(null) }
    var commentDraft by remember { mutableStateOf("") }
    val fired = remember { mutableStateMapOf<String, Boolean>() }
    val comments = remember { mutableStateMapOf<String, List<String>>() }

    LaunchedEffect(Unit) {
        try {
            rides = repository.getRides()
            members = repository.getPublicMembers()
        } catch (error: Exception) {
            loadError = "Feed data is temporarily unavailable. Please check your connection."
        } finally {
            loading = false
        }
    }

    val upcoming = rides.filter { it.status == RideStatus.PLANNED }.sortedBy { it.date }
    val past = rides.filter { it.status == RideStatus.COMPLETED }.sortedByDescending { it.date }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "501MTB FEED",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black
            )
            Text(
                "The latest rides and stories from our community",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (loading) {
            Box(Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (loadError != null) {
            Text(loadError!!, color = MaterialTheme.colorScheme.error)
        } else {
            FeedSectionTitle("NEXT RIDE")
            val nextRide = upcoming.firstOrNull()
            if (nextRide == null) {
                FeedEmptyCard("No upcoming ride published yet.")
            } else {
                RideFeedCard(
                    ride = nextRide,
                    aspectRatio = 16f / 9f,
                    isFired = fired[nextRide.id] == true,
                    fireCount = if (fired[nextRide.id] == true) 1 else 0,
                    commentCount = comments[nextRide.id]?.size ?: 0,
                    onOpen = { expandedRide = nextRide },
                    onFire = { fired[nextRide.id] = fired[nextRide.id] != true },
                    onComment = { commentRide = nextRide },
                    onShare = {
                        val shareText = "${nextRide.title}\n${nextRide.date} • ${nextRide.location}\n${nextRide.distanceKm} km"
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share ride"))
                    }
                )
            }

            FeedSectionTitle("PAST RIDES")
            if (past.isEmpty()) {
                FeedEmptyCard("Completed rides will appear here as stories.")
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(past, key = { it.id }) { ride ->
                        RideFeedCard(
                            ride = ride,
                            aspectRatio = 16f / 9f,
                            modifier = Modifier.fillParentMaxWidth(0.86f),
                            isFired = fired[ride.id] == true,
                            fireCount = if (fired[ride.id] == true) 1 else 0,
                            commentCount = comments[ride.id]?.size ?: 0,
                            onOpen = { expandedRide = ride },
                            onFire = { fired[ride.id] = fired[ride.id] != true },
                            onComment = { commentRide = ride },
                            onShare = {
                                val shareText = "${ride.title}\n${ride.date} • ${ride.location}\n${ride.distanceKm} km"
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share ride"))
                            }
                        )
                    }
                }
            }

            FeedSectionTitle("RIDE RECORD USERS")
            if (members.isEmpty()) {
                FeedEmptyCard("Public rider profiles will appear here.")
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(members, key = { it.id }) { member ->
                        Card(
                            modifier = Modifier
                                .fillParentMaxWidth(0.40f)
                                .aspectRatio(3f / 4f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF183018), Color(0xFF080808), Color(0xFF263326))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    member.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(member.name, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(member.badge, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    expandedRide?.let { ride ->
        AlertDialog(
            onDismissRequest = { expandedRide = null },
            title = { Text(ride.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RidePhotoPlaceholder(Modifier.fillMaxWidth(), 16f / 9f, ride.title)
                    Text(ride.date)
                    Text(ride.location)
                    Text("${ride.distanceKm} km • ${ride.type}")
                }
            },
            confirmButton = {
                TextButton(onClick = { expandedRide = null }) { Text("Close") }
            }
        )
    }

    commentRide?.let { ride ->
        AlertDialog(
            onDismissRequest = { commentRide = null; commentDraft = "" },
            title = { Text("Comments · ${ride.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val rideComments = comments[ride.id].orEmpty()
                    if (rideComments.isEmpty()) {
                        Text("Be the first to comment.")
                    } else {
                        rideComments.forEach { comment -> Text("• $comment") }
                    }
                    OutlinedTextField(
                        value = commentDraft,
                        onValueChange = { commentDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Write a comment") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = commentDraft.isNotBlank(),
                    onClick = {
                        comments[ride.id] = comments[ride.id].orEmpty() + commentDraft.trim()
                        commentDraft = ""
                        commentRide = null
                    }
                ) { Text("Post") }
            },
            dismissButton = {
                TextButton(onClick = { commentRide = null; commentDraft = "" }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FeedSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun FeedEmptyCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RideFeedCard(
    ride: ClubRide,
    aspectRatio: Float,
    modifier: Modifier = Modifier.fillMaxWidth(),
    isFired: Boolean,
    fireCount: Int,
    commentCount: Int,
    onOpen: () -> Unit,
    onFire: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            RidePhotoPlaceholder(
                modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio).clickable(onClick = onOpen),
                aspectRatio = aspectRatio,
                title = ride.title
            )
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(ride.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("${ride.date} • ${ride.location}", style = MaterialTheme.typography.bodySmall)
                Text("${ride.distanceKm} km", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onFire) {
                            Icon(
                                Icons.Filled.Whatshot,
                                contentDescription = "Fire reaction",
                                tint = if (isFired) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(fireCount.toString(), style = MaterialTheme.typography.labelMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onComment) {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comments")
                        }
                        Text(commentCount.toString(), style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share")
                    }
                }
            }
        }
    }
}

@Composable
private fun RidePhotoPlaceholder(
    modifier: Modifier = Modifier,
    aspectRatio: Float,
    title: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF243D24), Color(0xFF080808), Color(0xFF315031))
                )
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            title.uppercase(),
            modifier = Modifier.padding(14.dp),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
    }
}
