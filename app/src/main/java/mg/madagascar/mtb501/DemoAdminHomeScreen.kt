package mg.madagascar.mtb501

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private data class ShowcaseRide(
    val id: Int,
    val date: String,
    val title: String,
    val level: String,
    val time: String,
    val meeting: String,
    val distance: String,
    val completed: Boolean,
    val photoUrl: String? = null,
    val externalUrl: String? = null
)

private data class ShowcaseStory(
    val id: Int,
    val name: String,
    val badge: String,
    val ride: String,
    val distance: String,
    val date: String,
    val photoUrl: String? = null,
    val gpsUrl: String? = null,
    val profileUrl: String? = null
)

/**
 * UI 1 is intentionally a local SHOWCASE prototype.
 * Sample content and all reactions/actions are held in Compose state only.
 * Nothing on this screen writes to Firestore or publishes to Facebook.
 */
@Composable
fun DemoAdminHomeScreen(onOpenChat: () -> Unit = {}, onOpenSettings: () -> Unit = {}) {
    val nextRides = remember {
        mutableStateListOf(
            ShowcaseRide(1, "DATE TBA", "UPCOMING RIDE 01", "ALL LEVELS", "—", "Location to be announced", "—", false),
            ShowcaseRide(2, "DATE TBA", "UPCOMING RIDE 02", "ALL LEVELS", "—", "Location to be announced", "—", false),
            ShowcaseRide(3, "DATE TBA", "UPCOMING RIDE 03", "ALL LEVELS", "—", "Location to be announced", "—", false)
        )
    }
    val pastEvents = remember {
        mutableStateListOf(
            ShowcaseRide(11, "DATE TBA", "PAST EVENT 01", "ALL LEVELS", "—", "Location", "—", true),
            ShowcaseRide(12, "DATE TBA", "PAST EVENT 02", "ALL LEVELS", "—", "Location", "—", true),
            ShowcaseRide(13, "DATE TBA", "PAST EVENT 03", "ALL LEVELS", "—", "Location", "—", true)
        )
    }
    val stories = remember {
        mutableStateListOf(
            ShowcaseStory(21, "RIDER 01", "ROOKIE", "Member ride", "—", "DATE TBA"),
            ShowcaseStory(22, "RIDER 02", "SUPER", "Member ride", "—", "DATE TBA"),
            ShowcaseStory(23, "RIDER 03", "ELITE", "Member ride", "—", "DATE TBA"),
            ShowcaseStory(24, "RIDER 04", "LEADER", "Member ride", "—", "DATE TBA"),
            ShowcaseStory(25, "RIDER 05", "ROOKIE", "Member ride", "—", "DATE TBA"),
            ShowcaseStory(26, "RIDER 06", "SUPER", "Member ride", "—", "DATE TBA")
        )
    }
    val dataSourceLabel = "DEMO PREVIEW • NO CONNECTED DATA"
    var storyPage by remember { mutableStateOf(0) }
    var visibleStoryCount by remember { mutableStateOf(6) }

    var demoAdmin by remember { mutableStateOf(false) }
    var selectedRide by remember { mutableStateOf<ShowcaseRide?>(null) }
    var selectedStory by remember { mutableStateOf<ShowcaseStory?>(null) }
    var gpsStory by remember { mutableStateOf<ShowcaseStory?>(null) }
    var profileStory by remember { mutableStateOf<ShowcaseStory?>(null) }
    var commentsFor by remember { mutableStateOf<String?>(null) }
    var shareMessage by remember { mutableStateOf<String?>(null) }
    var adminMessage by remember { mutableStateOf<String?>(null) }
    val fired = remember { mutableStateMapOf<Int, Boolean>() }
    val demoComments = remember { mutableStateMapOf<String, List<String>>() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("501MTB", style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                Text("MADAGASCAR • MOUNTAIN BIKING",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(dataSourceLabel, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { demoAdmin = !demoAdmin }) {
                Text(if (demoAdmin) "EXIT ADMIN DEMO" else "ADMIN DEMO")
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (demoAdmin) {
            Text(
                "SHOWCASE ADMIN MODE • actions are demonstrations only",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }

        ShowcaseSectionHeader("NEXT RIDE", "UPCOMING MTB RIDES", demoAdmin) {
            adminMessage = "Add a new Next Ride"
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(nextRides, key = { it.id }) { ride ->
                NextRideCard(
                    ride = ride,
                    admin = demoAdmin,
                    onOpen = { selectedRide = ride },
                    onAdmin = { action -> adminMessage = "$action: ${ride.title}" },
                    onFire = { fired[ride.id] = !(fired[ride.id] ?: false) },
                    isFired = fired[ride.id] ?: false,
                    onComment = { commentsFor = ride.title },
                    onShare = { shareMessage = "Share preview: ${ride.title}" }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ShowcaseSectionHeader("PAST EVENT", "MTB EVENT", demoAdmin) {
                adminMessage = "Add a Past Event"
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(pastEvents, key = { it.id }) { ride ->
                    PastEventCard(
                        ride = ride,
                        admin = demoAdmin,
                        onOpen = { selectedRide = ride },
                        onAdmin = { action -> adminMessage = "$action: ${ride.title}" },
                        onFire = { fired[ride.id] = !(fired[ride.id] ?: false) },
                        isFired = fired[ride.id] ?: false,
                        onComment = { commentsFor = ride.title },
                        onShare = { shareMessage = "Share preview: ${ride.title}" }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeading("COMMUNITY", "MEMBER RIDE STORIES")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stories.drop(storyPage * 8).take(visibleStoryCount), key = { it.id }) { story ->
                    CommunityStoryCard(
                        story = story,
                        admin = demoAdmin,
                        isFired = fired[story.id] ?: false,
                        onOpen = { selectedStory = story },
                        onFire = { fired[story.id] = !(fired[story.id] ?: false) },
                        onGps = { gpsStory = story },
                        onProfile = { profileStory = story },
                        onAdmin = { adminMessage = "$it: ${story.name}" }
                    )
                }
            }
            if (storyPage * 8 + visibleStoryCount < stories.size) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        if (visibleStoryCount < 8 && storyPage * 8 + visibleStoryCount < stories.size) {
                            visibleStoryCount = (visibleStoryCount + 2).coerceAtMost(8)
                        } else if ((storyPage + 1) * 8 < stories.size) {
                            storyPage += 1
                            visibleStoryCount = 6
                        }
                    }) {
                        Text(
                            if (visibleStoryCount < 8 && storyPage * 8 + visibleStoryCount < stories.size)
                                "LOAD MORE STORIES" else "NEXT STORIES  →"
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onOpenCommunityFeed,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp)
        ) { Text("OPEN COMMUNITY FEED") }
    }

    selectedRide?.let { ride ->
        RideShowcaseDialog(
            ride = ride,
            isFired = fired[ride.id] ?: false,
            onFire = { fired[ride.id] = !(fired[ride.id] ?: false) },
            onComment = { commentsFor = ride.title },
            onShare = { shareMessage = "Share preview: ${ride.title}" },
            onDismiss = { selectedRide = null }
        )
    }
    selectedStory?.let { story ->
        StoryDetailDialog(
            story = story,
            isFired = fired[story.id] ?: false,
            onFire = { fired[story.id] = !(fired[story.id] ?: false) },
            onGps = { gpsStory = story },
            onProfile = { profileStory = story },
            onDismiss = { selectedStory = null }
        )
    }
    gpsStory?.let { story ->
        GpsShowcaseDialog(story = story, onDismiss = { gpsStory = null })
    }
    profileStory?.let { story ->
        RiderProfileShowcaseDialog(story = story, onDismiss = { profileStory = null })
    }
    commentsFor?.let { target ->
        var draft by remember(target) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { commentsFor = null },
            title = { Text("501MTB COMMENTS") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(target, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    val existingComments = demoComments[target].orEmpty()
                    if (existingComments.isEmpty()) {
                        Text("No comments yet. Be the first to join the conversation.")
                    } else {
                        existingComments.forEach { comment ->
                            Text("• $comment", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    androidx.compose.material3.OutlinedTextField(
                        value = draft, onValueChange = { draft = it },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Write a demo comment") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (draft.isNotBlank()) {
                        demoComments[target] = demoComments[target].orEmpty() + draft.trim()
                    }
                    commentsFor = null
                }) { Text("POST DEMO COMMENT") }
            },
            dismissButton = { TextButton(onClick = { commentsFor = null }) { Text("CLOSE") } }
        )
    }
    shareMessage?.let { message ->
        ShowcaseMessageDialog(
            title = "FACEBOOK SHARE PREVIEW",
            message = "$message\n\nThis preview does not publish anything to Facebook.",
            onDismiss = { shareMessage = null }
        )
    }
    adminMessage?.let { message ->
        AdminShowcaseDialog(
            action = message,
            onDismiss = { adminMessage = null }
        )
    }
}

@Composable
private fun ShowcaseSectionHeader(
    title: String,
    subtitle: String,
    admin: Boolean,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            Text(subtitle, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (admin) TextButton(onClick = onAdd) { Text("+ ADD") }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        Text(subtitle, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NextRideCard(
    ride: ShowcaseRide,
    admin: Boolean,
    onOpen: () -> Unit,
    onAdmin: (String) -> Unit,
    onFire: () -> Unit,
    isFired: Boolean,
    onComment: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.width(318.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 7.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (admin) AdminMenuButton(onAction = onAdmin)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(ride.date, color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                    Text(ride.title, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${ride.level}  •  ${ride.time}",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("MEET: ${ride.meeting}",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            PhotoArtwork(ride.title, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clickable(onClick = onOpen), 0, ridePhotoUrl(ride))
            RideActions(isFired, onFire, onComment, onShare, onOpen)
        }
    }
}

@Composable
private fun PastEventCard(
    ride: ShowcaseRide,
    admin: Boolean,
    onOpen: () -> Unit,
    onAdmin: (String) -> Unit,
    onFire: () -> Unit,
    isFired: Boolean,
    onComment: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.width(258.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box {
                PhotoArtwork(ride.title, Modifier.fillMaxWidth().aspectRatio(4f / 3f).clickable(onClick = onOpen), 1, ridePhotoUrl(ride))
                if (admin) {
                    Box(Modifier.align(Alignment.TopEnd).padding(5.dp)) {
                        AdminMenuButton(onAction = onAdmin)
                    }
                }
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .88f))))
                        .padding(11.dp)
                ) {
                    Text(ride.title, color = Color.White, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${ride.distance}  •  ${ride.date}", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
            RideActions(isFired, onFire, onComment, onShare, onOpen)
        }
    }
}

@Composable
private fun CommunityStoryCard(
    story: ShowcaseStory,
    admin: Boolean,
    isFired: Boolean,
    onOpen: () -> Unit,
    onFire: () -> Unit,
    onGps: () -> Unit,
    onProfile: () -> Unit,
    onAdmin: (String) -> Unit
) {
    Card(
        modifier = Modifier.width(164.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
            PhotoArtwork(story.name, Modifier.fillMaxSize().clickable(onClick = onOpen), 2, story.photoUrl)
            Column(
                modifier = Modifier.align(Alignment.TopStart).padding(7.dp)
                    .clip(RoundedCornerShape(7.dp)).background(Color.Black.copy(alpha = .76f)).padding(7.dp)
            ) {
                Text(story.badge, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
            }
            if (admin) Box(Modifier.align(Alignment.TopEnd).padding(3.dp)) {
                AdminMenuButton(onAction = onAdmin)
            }
            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .92f))))
                    .padding(horizontal = 9.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(story.name, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text("${story.ride} • ${story.distance}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onFire, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Favorite, contentDescription = "Fire reaction",
                            tint = if (isFired) Color(0xFFFF4D4D) else Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onGps, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.GpsFixed, contentDescription = "GPS track", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    TextButton(onClick = onProfile, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Text("PROFILE", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RideActions(
    isFired: Boolean,
    onFire: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TextButton(onClick = onFire) {
            Icon(Icons.Default.Favorite, contentDescription = null,
                tint = if (isFired) Color(0xFFFF4D4D) else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (isFired) "FIRED" else "FIRE")
        }
        TextButton(onClick = onComment) {
            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null)
            Text("COMMENT")
        }
        TextButton(onClick = onShare) {
            Icon(Icons.Default.Share, contentDescription = null)
            Text("SHARE")
        }
        TextButton(onClick = onOpen) { Text("DETAILS") }
    }
}

@Composable
private fun AdminMenuButton(onAction: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(34.dp)
            .clip(CircleShape).background(Color.Black.copy(alpha = .72f))) {
            Icon(Icons.Default.MoreVert, contentDescription = "Admin options", tint = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("Edit", "Delete").forEach { action ->
                DropdownMenuItem(text = { Text(action) }, onClick = {
                    expanded = false
                    onAction(action)
                })
            }
        }
    }
}

@Composable
private fun PhotoArtwork(title: String, modifier: Modifier = Modifier, variant: Int, imageUrl: String? = null) {
    val colors = when (variant % 3) {
        0 -> listOf(Color(0xFF102A20), Color(0xFF527A42), Color(0xFF17251C))
        1 -> listOf(Color(0xFF172C38), Color(0xFF66816B), Color(0xFF18231F))
        else -> listOf(Color(0xFF263B2B), Color(0xFF7D8750), Color(0xFF1C2B22))
    }
    Box(
        modifier = modifier.background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .12f)))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("501", color = Color.White.copy(alpha = .92f), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                Text("MTB  •  MADAGASCAR", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(title, color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

private fun ridePhotoUrl(ride: ShowcaseRide): String {
    return ride.photoUrl ?: "https://raw.githubusercontent.com/501mtb/501mtb.github.io/main/assets/images/event/event-001.jpg"
}
private fun galleryPhotoUrl(index: Int): String {
    val file = when (index % 3) {
        0 -> "event/event-001.jpg"
        1 -> "event/event-002.jpg"
        else -> "event/event-003.jpg"
    }
    return "https://raw.githubusercontent.com/501mtb/501mtb.github.io/main/assets/images/$file"
}

@Composable
private fun RideShowcaseDialog(
    ride: ShowcaseRide,
    isFired: Boolean,
    onFire: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ride.title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                PhotoArtwork(ride.title, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)), if (ride.completed) 1 else 0, ridePhotoUrl(ride))
                Text(ride.date, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("${ride.distance}  •  ${ride.level}")
                Text("TIME: ${ride.time}")
                Text("MEETING POINT: ${ride.meeting}")
                Divider()
                Text("PHOTO GALLERY", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { index ->
                        PhotoArtwork("PHOTO ${index + 1}", Modifier.weight(1f).aspectRatio(1f), index, galleryPhotoUrl(index))
                    }
                }
                ride.externalUrl?.let { postUrl ->
                    OutlinedButton(onClick = { uriHandler.openUri(postUrl) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Text("OPEN ORIGINAL EVENT LINK")
                    }
                }
                Text("501MTB reactions and comments are shown here in the finished app.")
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onFire) { Text(if (isFired) "FIRED" else "FIRE") }
                TextButton(onClick = onComment) { Text("COMMENTS") }
                TextButton(onClick = onShare) { Text("SHARE") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } }
    )
}

@Composable
private fun StoryDetailDialog(
    story: ShowcaseStory,
    isFired: Boolean,
    onFire: () -> Unit,
    onGps: () -> Unit,
    onProfile: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${story.name} • MEMBER RIDE") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                PhotoArtwork(story.ride, Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(12.dp)), 2, story.photoUrl)
                Text(story.date, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("${story.ride}  •  ${story.distance}")
                Text("RIDER BADGE: ${story.badge}")
                Text("Ride story detail, photos and route information.")
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onFire) { Text(if (isFired) "FIRED" else "FIRE") }
                TextButton(onClick = onGps) { Text("GPS") }
                TextButton(onClick = onProfile) { Text("PROFILE") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } }
    )
}

@Composable
private fun AdminShowcaseDialog(action: String, onDismiss: () -> Unit) {
    var title by remember(action) { mutableStateOf(action.substringAfter(": ", action)) }
    var date by remember(action) { mutableStateOf("01 DEC 2026") }
    var details by remember(action) { mutableStateOf("Add level, time, meeting point or event details") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ADMIN • $action") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    "SHOWCASE FORM",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                androidx.compose.material3.OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    singleLine = true
                )
                androidx.compose.material3.OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Date") },
                    singleLine = true
                )
                androidx.compose.material3.OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Details") },
                    minLines = 2
                )
                Text(
                    "Demo only: Save or Delete will not change any real 501MTB records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (action.startsWith("Delete")) "CONFIRM DEMO DELETE" else "SAVE DEMO")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
private fun GpsShowcaseDialog(story: ShowcaseStory, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GPS TRACK • ${story.ride}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(190.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF17251F))
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val route = Path().apply {
                            moveTo(size.width * .12f, size.height * .76f)
                            cubicTo(size.width * .26f, size.height * .12f, size.width * .48f, size.height * .92f, size.width * .58f, size.height * .48f)
                            cubicTo(size.width * .68f, size.height * .08f, size.width * .78f, size.height * .82f, size.width * .9f, size.height * .22f)
                        }
                        drawPath(route, Color(0xFF75E36B), style = Stroke(width = 8f))
                        drawCircle(Color(0xFF35C96B), radius = 11f, center = Offset(size.width * .12f, size.height * .76f))
                        drawCircle(Color(0xFFFF6B6B), radius = 11f, center = Offset(size.width * .9f, size.height * .22f))
                    }
                    Text(
                        "ROUTE PREVIEW",
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                            .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = .65f)).padding(6.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(story.ride, fontWeight = FontWeight.Bold)
                Text("${story.distance}  •  ${story.date}")
                Text(
                    "Illustrative GPS route only. No real GPX track is connected yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row {
                story.gpsUrl?.let { url -> TextButton(onClick = { uriHandler.openUri(url) }) { Text("OPEN GPS TRACK") } }
                TextButton(onClick = onDismiss) { Text("CLOSE") }
            }
        }
    )
}

@Composable
private fun RiderProfileShowcaseDialog(story: ShowcaseStory, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("RIDER PROFILE") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF102A20), Color(0xFF527A42))))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("501MTB", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.labelSmall)
                        Text(story.name, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text(story.badge, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Text("LATEST RIDE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(story.ride, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
                        Text("DISTANCE", style = MaterialTheme.typography.labelSmall)
                        Text(story.distance, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
                        Text("RIDE DATE", style = MaterialTheme.typography.labelSmall)
                        Text(story.date, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    "Showcase profile. Member statistics and personal records will be connected later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row {
                story.profileUrl?.let { url -> TextButton(onClick = { uriHandler.openUri(url) }) { Text("OPEN PROFILE") } }
                TextButton(onClick = onDismiss) { Text("CLOSE") }
            }
        }
    )
}

@Composable
private fun ShowcaseMessageDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}
