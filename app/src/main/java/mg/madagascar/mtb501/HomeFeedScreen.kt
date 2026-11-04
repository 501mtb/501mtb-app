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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
private fun JSONObject.v2Text(vararg keys: String): String {
    for (key in keys) {
        if (!has(key) || isNull(key)) continue
        val value = opt(key)?.toString()?.trim().orEmpty()
        if (value.isNotBlank() && value != "null") return value
    }
    return ""
}

private fun v2AssetUrl(path: String): String? {
    val value = path.trim()
    if (value.isBlank()) return null
    if (value.startsWith("https://") || value.startsWith("http://")) return value
    val clean = value.removePrefix("./").removePrefix("/")
    return "https://501mtb.github.io/" + clean
}

private fun showcaseDate(value: String): String {
    val raw = value.trim().substringBefore("T")
    val parts = raw.split("-")
    if (parts.size == 3 && parts[0].length == 4) return parts[2] + "/" + parts[1] + "/" + parts[0]
    return raw.ifBlank { "DATE TBA" }
}
private fun showcaseDateKey(value: String): String {
    val parts = value.split("/")
    return if (parts.size == 3) parts[2] + parts[1] + parts[0] else value
}

@Composable
fun HomeFeedScreen(onOpenCommunityFeed: () -> Unit = {}) {
    val nextRides = remember {
        mutableStateListOf(
            ShowcaseRide(1, "15 NOV 2026", "IVOLOINA TRAIL RIDE", "ALL LEVELS", "07:30", "Loca Velo Tanamakoa", "32 km", false),
            ShowcaseRide(2, "29 NOV 2026", "MAKUA FESTIVAL RIDE", "ALL LEVELS", "12:30", "Loca Velo Tanamakoa", "18 km", false),
            ShowcaseRide(3, "13 DEC 2026", "LAC ANDOVOLALINA", "INTERMEDIATE", "08:00", "Before Barikadimy", "54 km", false)
        )
    }
    val pastEvents = remember {
        mutableStateListOf(
            ShowcaseRide(11, "15 OCT 2026", "IVOLOINA • BASSINDRANO", "INTERMEDIATE", "07:00", "Toamasina", "34 km", true),
            ShowcaseRide(13, "20 SEP 2026", "TOAMASINA DISCOVERY", "ALL LEVELS", "08:00", "Loca Velo", "26 km", true),
            ShowcaseRide(12, "30 AUG 2026", "2ND XC 501MTB RACE", "ELITE", "07:00", "Antanambao TMF", "54 km", true)
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
    var dataSourceLabel by remember { mutableStateOf("SHOWCASE FALLBACK • V2 DATA EXPORT PENDING") }
    LaunchedEffect(Unit) {
        val jsonText = withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL("https://raw.githubusercontent.com/501mtb/501mtb.github.io/main/data/app-data.json").openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                try {
                    if (connection.responseCode !in 200..299) error("V2 data export not published")
                    connection.inputStream.bufferedReader().use { it.readText() }
                } finally { connection.disconnect() }
            }.getOrNull()
        }
        if (jsonText != null) {
            runCatching {
                val sheets = JSONObject(jsonText).optJSONObject("sheets") ?: JSONObject()
                val eventRows = sheets.optJSONArray("Events") ?: JSONArray()
                val events = (0 until eventRows.length()).mapNotNull { index ->
                    val row = eventRows.optJSONObject(index) ?: return@mapNotNull null
                    val status = row.v2Text("status").lowercase()
                    if (status !in listOf("planned", "open", "completed")) return@mapNotNull null
                    val title = row.v2Text("title_en", "title_fr", "title_mg", "title")
                    if (title.isBlank()) return@mapNotNull null
                    val distanceRaw = row.v2Text("distance_km", "distance", "km")
                    ShowcaseRide(
                        id = row.v2Text("id").ifBlank { status + "-" + index }.hashCode(),
                        date = showcaseDate(row.v2Text("date_start", "date")),
                        title = title.uppercase(),
                        level = row.v2Text("level", "difficulty", "category").ifBlank { "ALL LEVELS" }.uppercase(),
                        time = row.v2Text("time_start", "time", "start_time").ifBlank { "TBA" },
                        meeting = row.v2Text("location_en", "location_fr", "location_mg", "location", "meeting_point").ifBlank { "Toamasina" },
                        distance = if (distanceRaw.isBlank()) "—" else if (distanceRaw.contains("km", true)) distanceRaw else distanceRaw + " km",
                        completed = status == "completed",
                        photoUrl = v2AssetUrl(row.v2Text("image", "photo")),
                        externalUrl = row.v2Text("url", "facebook_url", "link").takeIf { it.isNotBlank() }
                    )
                }
                val upcoming = events.filter { !it.completed }.sortedBy { showcaseDateKey(it.date) }
                val past = events.filter { it.completed }.sortedByDescending { showcaseDateKey(it.date) }
                if (upcoming.isNotEmpty()) { nextRides.clear(); nextRides.addAll(upcoming) }
                if (past.isNotEmpty()) { pastEvents.clear(); pastEvents.addAll(past) }
                val riderRows = sheets.optJSONArray("Riders") ?: JSONArray()
                val riders = (0 until riderRows.length()).mapNotNull { index ->
                    val row = riderRows.optJSONObject(index) ?: return@mapNotNull null
                    if (row.has("published") && row.v2Text("published").lowercase() != "yes") return@mapNotNull null
                    if (row.v2Text("name").isBlank()) return@mapNotNull null
                    row
                }
                val leaderRows = sheets.optJSONArray("Leaders") ?: JSONArray()
                val leaders = (0 until leaderRows.length()).mapNotNull { index ->
                    val row = leaderRows.optJSONObject(index) ?: return@mapNotNull null
                    if (row.has("published") && row.v2Text("published").lowercase() != "yes") return@mapNotNull null
                    if (row.v2Text("name").isBlank()) return@mapNotNull null
                    row
                }
                val allMembers = (riders + leaders).distinctBy { it.v2Text("name").lowercase() }
                val registryNames = listOf("MemberRides", "Member Rides", "Member_Rides", "Member Ride", "Member_Ride", "IndividualRides", "Individual Rides", "Individual_Rides", "Member Ride Registry", "RideLog", "Ride Log", "RideRegistry", "Ride Registry")
                val registry = registryNames.firstNotNullOfOrNull { sheets.optJSONArray(it) }
                val memberStories = mutableListOf<ShowcaseStory>()
                if (registry != null) {
                    for (index in 0 until registry.length()) {
                        val row = registry.optJSONObject(index) ?: continue
                        val name = row.v2Text("rider_name", "member_name", "rider", "member", "name")
                        if (name.isBlank()) continue
                        val distanceRaw = row.v2Text("distance_km", "distance", "km")
                        val member = allMembers.firstOrNull { it.v2Text("name").equals(name, true) }
                        memberStories += ShowcaseStory(
                            id = row.v2Text("id").ifBlank { "member-ride-" + index }.hashCode(),
                            name = name.uppercase(),
                            badge = member?.v2Text("category", "badge", "status", "role_en", "role")?.uppercase().orEmpty().ifBlank { "RIDER" },
                            ride = row.v2Text("ride_title", "title", "ride_name", "event").ifBlank { "MEMBER RIDE" },
                            distance = if (distanceRaw.isBlank()) "—" else if (distanceRaw.contains("km", true)) distanceRaw else distanceRaw + " km",
                            date = showcaseDate(row.v2Text("date", "ride_date", "date_start")),
                            photoUrl = v2AssetUrl(member?.v2Text("photo").orEmpty()),
                            gpsUrl = row.v2Text("gps_url", "gpx_url", "track_url").takeIf { it.isNotBlank() },
                            profileUrl = member?.v2Text("profile_url")?.takeIf { it.isNotBlank() }
                        )
                    }
                }
                if (memberStories.isEmpty()) {
                    allMembers.forEachIndexed { index, row ->
                        memberStories += ShowcaseStory(
                            id = row.v2Text("id").ifBlank { "rider-" + index }.hashCode(),
                            name = row.v2Text("name").uppercase(),
                            badge = row.v2Text("category", "badge", "status", "role_en", "role").uppercase().ifBlank { "RIDER" },
                            ride = "MEMBER RIDE", distance = "—", date = "501MTB RIDER",
                            photoUrl = v2AssetUrl(row.v2Text("photo")),
                            profileUrl = row.v2Text("profile_url").takeIf { it.isNotBlank() }
                        )
                    }
                }
                if (memberStories.isNotEmpty()) { stories.clear(); stories.addAll(memberStories.shuffled()) }
                dataSourceLabel = "LIVE DATA • 501MTB V2 WORKBOOK"
            }.onFailure { dataSourceLabel = "SHOWCASE FALLBACK • V2 DATA FORMAT CHECK" }
        }
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
        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AsyncImage(
                model = "https://501mtb.github.io/assets/images/logo/logoh.png",
                contentDescription = "501MTB",
                modifier = Modifier.height(42.dp).width(156.dp),
                contentScale = ContentScale.Fit
            )
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
        modifier = Modifier.width(270.dp),
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
        modifier = Modifier.width(218.dp),
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
        modifier = Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onFire, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.Favorite, contentDescription = if (isFired) "Remove Fire reaction" else "Fire",
                tint = if (isFired) Color(0xFFFF4D4D) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = onComment, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comments",
                modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.Share, contentDescription = "Share",
                modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = onOpen, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Ride details",
                modifier = Modifier.size(19.dp))
        }
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
