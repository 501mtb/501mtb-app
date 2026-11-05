package mg.madagascar.mtb501

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Facebook
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val SITE_BASE = "https://501mtb.github.io/"
private const val RIDE_PHONE = "034 93 715 00"

private data class ProfileLeader(val name: String, val role: String, val photo: String)
private data class ProfileRide(
    val title: String,
    val date: String,
    val location: String,
    val description: String,
    val image: String,
    val album: String
)

private data class ProfileSiteData(
    val leaders: List<ProfileLeader>,
    val rides: List<ProfileRide>
)

private suspend fun loadProfileSiteData(): ProfileSiteData = withContext(Dispatchers.IO) {
    // Read the public, deployed V2 pages. The app does not use the V2 repository
    // workbook or its internal JSON as a data source.
    val document = org.jsoup.Jsoup.connect(SITE_BASE)
        .userAgent("501MTB-Android-App")
        .timeout(20000)
        .get()

    fun cleanText(value: String): String = value
        .replace(Regex("\\s+"), " ")
        .trim()

    fun imageUrl(element: org.jsoup.nodes.Element): String {
        val candidates = listOf(
            element.attr("src"),
            element.attr("data-src"),
            element.attr("data-lazy-src"),
            element.attr("data-original"),
            element.attr("data-image"),
            element.attr("srcset").substringBefore(",").substringBefore(" ").trim(),
            element.parent()?.selectFirst("source[srcset]")?.attr("srcset")
                ?.substringBefore(",")?.substringBefore(" ")?.trim().orEmpty()
        )
        val raw = candidates.firstOrNull { it.isNotBlank() && !it.startsWith("data:") }.orEmpty()
        return if (raw.isBlank()) "" else resolveSiteAsset(raw)
    }

    val teamHeading = document.select("h1,h2,h3")
        .firstOrNull { cleanText(it.text()).equals("Our Team", true) }
    val teamSection = teamHeading?.parents()?.firstOrNull { it.tagName() == "section" }
        ?: teamHeading?.parents()?.firstOrNull { ancestor ->
            val text = cleanText(ancestor.text())
            ancestor.select("img").isNotEmpty() &&
                text.contains("Founder", true) && text.contains("Admin", true)
        }
        ?: teamHeading?.parent()?.parent()
    val leaders = mutableListOf<ProfileLeader>()
    teamSection?.select("img")?.forEach { image ->
        val card = image.parents().firstOrNull { ancestor ->
            val text = cleanText(ancestor.text())
            text.length in 8..260 && Regex("Founder|Admin|Ambassador|Legend|Champion", RegexOption.IGNORE_CASE)
                .containsMatchIn(text)
        } ?: image.parent() ?: image
        val cardText = cleanText(card.text())
        val alt = cleanText(image.attr("alt"))
        val role = listOf("Founder", "Admin", "Ambassador", "Legend", "Champion")
            .firstOrNull { cardText.contains(it, true) || alt.contains(it, true) } ?: "Team"
        if (role.equals("Legend", true) || role.equals("Champion", true)) return@forEach
        val name = alt.substringBefore("—").substringBefore(" - ").trim()
            .ifBlank { cardText.replace(role, "", ignoreCase = true).trim() }
        val photo = imageUrl(image)
        if (name.isNotBlank() && photo.isNotBlank() &&
            leaders.none { it.name.equals(name, true) }) {
            leaders += ProfileLeader(name, role, photo)
        }
    }

    val rides = mutableListOf<ProfileRide>()
    document.select("img[alt*='501MTB mountain biking ride or event'], img[alt*='501MTB ride or event']")
        .forEach { image ->
            val card = image.parents().firstOrNull { ancestor ->
                val text = cleanText(ancestor.text())
                text.length in 20..900 &&
                    Regex("\\d{2}/\\d{2}/\\d{4}").containsMatchIn(text)
            } ?: image.parent() ?: image
            val cardText = cleanText(card.text())
            val date = Regex("\\d{2}/\\d{2}/\\d{4}").find(cardText)?.value.orEmpty()
            val title = cleanText(image.attr("alt"))
                .substringBefore(" — 501MTB")
                .substringBefore(" - 501MTB")
                .ifBlank { cardText }
            val photo = imageUrl(image)
            val album = card.select("a[href]").firstOrNull {
                it.absUrl("href").contains("facebook.com", true)
            }?.absUrl("href").orEmpty()
            val details = cardText.removePrefix(date).trim()
            if (date.isNotBlank() && title.isNotBlank() && photo.isNotBlank() &&
                rides.none { it.title.equals(title, true) }) {
                rides += ProfileRide(
                    title = title,
                    date = date,
                    location = details,
                    description = details,
                    image = photo,
                    album = album
                )
            }
        }

    ProfileSiteData(leaders, rides)
}

private fun resolveSiteAsset(path: String): String {
    return try {
        java.net.URI(SITE_BASE).resolve(path.trim()).toString()
    } catch (_: Exception) {
        path
    }
}

@Composable
fun Profile501Screen(
    isAdmin: Boolean,
    onOpenAdmin: () -> Unit
) {
    val context = LocalContext.current
    var siteData by remember { mutableStateOf(ProfileSiteData(emptyList(), emptyList())) }
    var coverUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var coverIndex by remember { mutableIntStateOf(0) }
    var profilePhoto by remember { mutableStateOf<String?>(null) }
    var selectedRide by remember { mutableStateOf<ProfileRide?>(null) }
    var expandedLeader by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            siteData = loadProfileSiteData()
        } catch (_: Exception) {
            loadError = true
        }
        try {
            val repository = ClubRepository()
            val library = PhotoLibraryRepository().load()
            val coverIds = repository.getPhotoAssignment("501mtb.pdc")
            val coverImages = library.imagesForSlot("501mtb.pdc").associateBy { it.id }
            coverUrls = coverIds.mapNotNull { coverImages[it]?.url }
                .ifEmpty { library.imagesForSlot("501mtb.pdc").map { it.url } }
            val profileIds = repository.getPhotoAssignment("501mtb.pdp")
            profilePhoto = library.imagesForSlot("501mtb.pdp")
                .firstOrNull { it.id in profileIds }?.url
                ?: "https://501mtb.github.io/pdp.jpg"
        } catch (_: Exception) {
            // The official emblem and site content remain visible if remote photos are unavailable.
        } finally {
            loading = false
        }
    }

    LaunchedEffect(coverUrls) {
        while (coverUrls.size > 1) {
            kotlinx.coroutines.delay(6000)
            coverIndex = (coverIndex + 1) % coverUrls.size
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // PDC — official 16:9 cover image.
        Box(Modifier.fillMaxWidth()) {
            if (coverUrls.isNotEmpty()) {
                AsyncImage(
                    model = coverUrls[coverIndex.coerceIn(0, coverUrls.lastIndex)],
                    contentDescription = "501MTB cover",
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF171717)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("501MTB", color = Color.White, style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black)
                }
            }
            if (isAdmin) {
                var menuExpanded by remember { mutableStateOf(false) }
                Box(Modifier.align(Alignment.TopEnd)) {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, "Manage cover", tint = Color.White)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Manage profile photos") },
                            leadingIcon = { Icon(Icons.Default.PhotoLibrary, null) },
                            onClick = { menuExpanded = false; onOpenAdmin() }
                        )
                    }
                }
            }
        }

        // PDP — centre emblem with the profile photo overlapping the cover edge.
        Row(
            modifier = Modifier.fillMaxWidth().offset(y = (-30).dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SponsorSlot()
            Box(
                Modifier.size(116.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(5.dp).clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (profilePhoto != null) {
                    AsyncImage(profilePhoto, "501MTB profile image", Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop)
                } else {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.mtb501_app_icon),
                        contentDescription = "501MTB emblem",
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }
            }
            SponsorSlot()
        }

        Text("À PROPOS", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        Text("USEFUL LINKS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val links = listOf(
                listOf(Triple("501MTB", "https://www.facebook.com/501MTB", Color(0xFF1877F2)) to Icons.Default.Facebook,
                    Triple("Team 501", "https://facebook.com/groups/739270103999083/", Color(0xFF1877F2)) to Icons.Default.Groups),
                listOf(Triple(RIDE_PHONE, "https://wa.me/261349371500", Color(0xFF25D366)) to Icons.Default.Chat,
                    Triple("501 GitHub", "https://github.com/501mtb", Color.White) to Icons.Default.Code),
                listOf(Triple("toamasina501mtb@gmail.com", "mailto:toamasina501mtb@gmail.com", Color.White) to Icons.Default.Email,
                    Triple("Call", "tel:+261349371500", Color(0xFF25D366)) to Icons.Default.Call)
            )
            links.forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { (item, icon) ->
                        ProfileLinkBar(item.first, item.second, item.third, icon, Modifier.weight(1f)) {
                            try {
                                val intent = when {
                                    item.second.startsWith("mailto:") -> Intent(Intent.ACTION_SENDTO, Uri.parse(item.second))
                                    item.second.startsWith("tel:") -> Intent(Intent.ACTION_DIAL, Uri.parse(item.second))
                                    else -> Intent(Intent.ACTION_VIEW, Uri.parse(item.second))
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) { }
                        }
                    }
                }
            }
        }

        Text("OUR VISION • MISSION • COMMUNITY", style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold)
        ProfileInfoBar("OUR VISION",
            "To become the leading reference for mountain biking in Madagascar.")
        ProfileInfoBar("OUR MISSION",
            "To develop and promote mountain biking by organizing events, supporting riders, and building a strong and united MTB community.")
        ProfileInfoBar("OUR COMMUNITY",
            "501MTB brings riders together through events, trail rides, race experiences and community support across Madagascar.")

        Text("ESPACE TEAM", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        if (loading && siteData.leaders.isEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        } else if (siteData.leaders.isEmpty()) {
            Text(if (loadError) "Team information is temporarily unavailable." else "No team leader published.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                siteData.leaders.forEach { leader ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                expandedLeader = if (expandedLeader == leader.name) null else leader.name
                            }.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(leader.name, fontWeight = FontWeight.Bold)
                                Text(leader.role, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            Text(if (expandedLeader == leader.name) "−" else "+",
                                color = MaterialTheme.colorScheme.primary)
                        }
                        if (expandedLeader == leader.name) {
                            Text(
                                "Contact : $RIDE_PHONE",
                                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                                    .clickable {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+261349371500")))
                                    },
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Text("ESPACE RIDE", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        Text("Select a photo to view ride details and its Facebook album.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (siteData.rides.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                siteData.rides.chunked(3).forEach { columnRides ->
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        columnRides.forEach { ride ->
                            AsyncImage(
                                model = ride.image,
                                contentDescription = ride.title,
                                modifier = Modifier.width(190.dp).height(104.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedRide = ride },
                                contentScale = ContentScale.Crop
                            )
                        }
                        repeat(3 - columnRides.size) { Spacer(Modifier.height(104.dp)) }
                    }
                }
            }
        } else {
            Text("Ride photos will appear when the V2 catalogue is available.")
        }

        Text("ESPACE DOCUMENT", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DocumentBar("Ride 2025", "https://501mtb.github.io/documents.html")
            DocumentBar("Ride 2026", "https://github.com/501mtb/501mtb.github.io/blob/main/Document/Archives/501MTB_Ride_Links_2026.xlsx")
            DocumentBar("Code of Conduct", "https://501mtb.github.io/conduct-en.html")
            DocumentBar("501 MTB Full Story", "https://501mtb.github.io/about.html")
        }

        if (isAdmin) {
            TextButton(onClick = onOpenAdmin) { Text("ADMIN PHOTO & CONTENT MANAGEMENT") }
        }
        if (loadError) Text("Some V2 content could not be refreshed. Check your connection.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }

    selectedRide?.let { ride ->
        AlertDialog(
            onDismissRequest = { selectedRide = null },
            title = { Text(ride.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    AsyncImage(ride.image, ride.title, Modifier.fillMaxWidth().height(170.dp)
                        .clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                    Text(ride.date)
                    Text(ride.location)
                    Text(ride.description)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedRide = null
                    if (ride.album.isNotBlank()) context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(ride.album))
                    )
                }) { Text("Open Facebook album") }
            },
            dismissButton = { TextButton(onClick = { selectedRide = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun SponsorSlot() {
    Box(
        Modifier.size(64.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text("501", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProfileLinkBar(
    title: String,
    url: String,
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val dark = accent == Color.White
    Row(
        modifier.clip(RoundedCornerShape(10.dp))
            .background(if (dark) Color.White else accent.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = if (dark) Color.Black else accent,
            modifier = Modifier.size(18.dp))
        Text(title, color = if (dark) Color.Black else accent,
            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ProfileInfoBar(title: String, description: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.surface).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DocumentBar(title: String, url: String) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
        Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("↗", color = MaterialTheme.colorScheme.primary)
    }
}
