package mg.madagascar.mtb501

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth

@Composable
fun RiderProfileScreen() {
    val auth = remember { FirebaseAuth.getInstance() }
    var account by remember { mutableStateOf<ClubUser?>(null) }
    var assignedBadge by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var settingsOpen by remember { mutableStateOf(false) }
    var clubSummary by remember { mutableStateOf<ClubSummary?>(null) }
    var loadMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(auth.currentUser?.uid) {
        loading = true
        try {
            val repository = ClubRepository()
            account = repository.getMyAccount()
            val currentName = account?.displayName?.trim().orEmpty()
            assignedBadge = if (currentName.isNotBlank()) {
                repository.getPublicMembers()
                    .firstOrNull { it.name.trim().equals(currentName, ignoreCase = true) }
                    ?.badge
                    ?.takeIf { it.isNotBlank() }
            } else null
            clubSummary = repository.getClubSummary()
            loadMessage = null
        } catch (error: Exception) {
            loadMessage = "Some profile information is unavailable while offline."
        } finally {
            loading = false
        }
    }

    val user = auth.currentUser
    val riderName = account?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.displayName?.takeIf { it.isNotBlank() }
        ?: "501MTB Rider"
    val initials = riderName.trim().split(Regex("\\s+")).take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Rider Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Your personal MTB journey", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { settingsOpen = !settingsOpen }) {
                Icon(Icons.Default.Settings, contentDescription = "Parameters and settings", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (settingsOpen) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    SettingLine("Account", "Google account and sign-in status")
                    SettingLine("Profile visibility", "Public rider profile preferences")
                    SettingLine("Notifications", "Ride and community notification preferences")
                    SettingLine("App preferences", "Language and display options")
                    Text("These settings are a visual entry point; preference controls will be connected in a later step.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF071B0B), Color(0xFF174A20), Color(0xFF050805))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = Color(0xFF7DFF7D), modifier = Modifier.size(48.dp))
                Text("501MTB • RIDER", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("WHERE LEGENDS RISE", color = Color(0xFF7DFF7D), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(112.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!user?.photoUrl?.toString().isNullOrBlank()) {
                    AsyncImage(
                        model = user?.photoUrl,
                        contentDescription = "Rider profile photo",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(initials.ifBlank { "501" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(riderName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(assignedBadge ?: "BADGE NOT ASSIGNED", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("501MTB ID: ${account?.memberId ?: "Not assigned"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (account?.approvalStatus == ApprovalStatus.PENDING) {
                Text("Account pending approval", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }

        Text("STATISTICS GROUP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("501MTB community overview", fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        ProfileStat("Club rides", clubSummary?.totalRides?.toString() ?: "—")
                        ProfileStat("Club distance", clubSummary?.let { "%.0f km".format(it.totalDistanceKm) } ?: "—")
                        ProfileStat("Members", clubSummary?.activeMembers?.toString() ?: "—")
                    }
                    Text("Club-wide figures, not personal rider totals.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Text("STATISTICS INDIVIDUAL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("My riding performance", fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        ProfileStat("Rides", "—")
                        ProfileStat("Distance", "—")
                        ProfileStat("Elevation", "—")
                    }
                    Text("Your personal GPS and attendance statistics will appear here when recorded ride data is connected.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Text("ABOUT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("About this rider", fontWeight = FontWeight.Bold)
                Text("A member of the 501MTB mountain biking community in Madagascar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(user?.email ?: "Sign in to connect your rider account.", style = MaterialTheme.typography.bodySmall)
                if (loading) Text("Loading profile…", style = MaterialTheme.typography.labelSmall)
                loadMessage?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun SettingLine(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

