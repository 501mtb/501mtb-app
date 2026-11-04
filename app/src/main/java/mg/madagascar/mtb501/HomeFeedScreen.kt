package mg.madagascar.mtb501

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    val completed: Boolean
)

private data class ShowcaseStory(
    val id: Int,
    val name: String,
    val badge: String,
    val ride: String,
    val distance: String,
    val date: String
)

/**
 * UI 1 is intentionally a local SHOWCASE prototype.
 * Sample content and all reactions/actions are held in Compose state only.
 * Nothing on this screen writes to Firestore or publishes to Facebook.
 */
@Composable
fun HomeFeedScreen(onOpenCommunityFeed: () -> Unit = {}) {
    val nextRides = remember {
        mutableStateListOf(
            ShowcaseRide(1, "01 NOV 2026", "LAC ANDOVOLALINA", "INTERMEDIATE", "08:00", "Before Barikadimy", "54 km", false),
            ShowcaseRide(2, "15 NOV 2026", "IVOLOINA TRAIL RIDE", "ALL LEVELS", "07:30", "Loca Velo Tanamakoa", "32 km", false),
            ShowcaseRide(3, "29 NOV 2026", "MAKUA FESTIVAL RIDE", "ALL LEVELS", "12:30", "Loca Velo Tanamakoa", "18 km", false)
        )
    }
    val pastEvents = remember {
        mutableStateListOf(
            ShowcaseRide(11, "15 OCT 2026", "IVOLOINA • BASSINDRANO", "INTERMEDIATE", "07:00", "Toamasina", "34 km", true),
            ShowcaseRide(12, "30 AUG 2026", "2ND XC 501MTB RACE", "ELITE", "07:00", "Antanambao TMF", "54 km", true),
            ShowcaseRide(13, "20 SEP 2026", "TOAMASINA DISCOVERY", "ALL LEVELS", "08:00", "Loca Velo", "26 km", true)
        )
    }
    val stories = remember {
        mutableStateListOf(
            ShowcaseStory(21, "NJAKAH", "LEADER", "Ivoloina Trail", "34 km", "15 OCT 2026"),
            ShowcaseStory(22, "TOKY", "LEADER", "Forest Challenge", "42 km", "12 OCT 2026"),
            ShowcaseStory(23, "JOE", "SUPER", "Coastal Ride", "28 km", "08 OCT 2026"),
            ShowcaseStory(24, "MIKA", "SUPER", "Sunday Ride", "31 km", "05 OCT 2026"),
            ShowcaseStory(25, "SYLVANO", "ELITE", "XC Training", "46 km", "02 OCT 2026"),
            ShowcaseStory(26, "YAKI", "SUPER", "Ivoloina Trail", "34 km", "28 SEP 2026"),
            ShowcaseStory(27, "JAURES", "ROOKIE", "First Forest Ride", "18 km", "24 SEP 2026"),
            ShowcaseStory(28, "JOEL", "ROOKIE", "Coastal Discovery", "22 km", "22 SEP 2026"),
            ShowcaseStory(29, "KEN", "ROOKIE", "Sunday Trail", "16 km", "20 SEP 2026"),
            ShowcaseStory(30, "HERVER", "ROOKIE", "Ivoloina Loop", "20 km", "18 SEP 2026"),
            ShowcaseStory(31, "MANDRESY", "ROOKIE", "Forest Challenge", "24 km", "16 SEP 2026"),
            ShowcaseStory(32, "YAINA", "ELITE", "XC Training", "42 km", "14 SEP 2026")
        ).apply { shuffle() }
    }
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
            }
            TextButton(onClick = { demoAdmin = !demoAdmin }) {
                Text(if (demoAdmin) "EXIT ADMIN DEMO" else "ADMIN DEMO")
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
                items(pastEvents.sortedByDescending { it.id }, key = { it.id }) { ride ->
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
            if (stories.size > 6) {
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
            onComment = { commentsFor = story.ride },
            onGps = { gpsStory = story },
            onProfile = { profileStory = story },
            onDismiss = { selectedStory = null }
        )
    }
    gpsStory?.let { story ->
        ShowcaseMessageDialog(
            title = "GPS TRACK • ${story.ride}",
            message = "A GPS route preview for ${story.name} will appear here. This is a visual demo; no real GPX track is connected.",
            onDismiss = { gpsStory = null }
        )
    }
    profileStory?.let { story ->
        ShowcaseMessageDialog(
            title = "${story.name} • RIDER PROFILE",
            message = "501MTB member profile preview\nBadge: ${story.badge}\nLatest ride: ${story.ride}\nDistance: ${story.distance}",
            onDismiss = { profileStory = null }
        )
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
            PhotoArtwork(ride.title, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clickable(onClick = onOpen), 0)
            RideActions(ride.id, isFired, onFire, onComment, onShare, onOpen)
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
                PhotoArtwork(ride.title, Modifier.fillMaxWidth().aspectRatio(4f / 3f).clickable(onClick = onOpen), 1)
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
            RideActions(ride.id, isFired, onFire, onComment, onShare, onOpen)
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
            PhotoArtwork(story.name, Modifier.fillMaxSize().clickable(onClick = onOpen), 2)
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
    id: Int,
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
private fun PhotoArtwork(title: String, modifier: Modifier = Modifier, variant: Int) {
    val colors = when (variant % 3) {
        0 -> listOf(Color(0xFF102A20), Color(0xFF527A42), Color(0xFF17251C))
        1 -> listOf(Color(0xFF172C38), Color(0xFF66816B), Color(0xFF18231F))
        else -> listOf(Color(0xFF263B2B), Color(0xFF7D8750), Color(0xFF1C2B22))
    }
    Box(
        modifier = modifier.background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("501", color = Color.White.copy(alpha = .92f), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
            Text("MTB  •  MADAGASCAR", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(title, color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ride.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                PhotoArtwork(ride.title, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)), if (ride.completed) 1 else 0)
                Text(ride.date, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("${ride.distance}  •  ${ride.level}")
                Text("TIME: ${ride.time}")
                Text("MEETING POINT: ${ride.meeting}")
                Divider()
                Text("PHOTO GALLERY", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { index ->
                        PhotoArtwork("PHOTO ${index + 1}", Modifier.weight(1f).aspectRatio(1f), index)
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
    onComment: () -> Unit,
    onGps: () -> Unit,
    onProfile: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${story.name} • MEMBER RIDE") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                PhotoArtwork(story.ride, Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(12.dp)), 2)
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
private fun ShowcaseMessageDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}
