package mg.madagascar.mtb501

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val MTBGreen = Color(0xFF39FF14)
private val MTBDark = Color(0xFF090B09)
private val MTBSurface = Color(0xFF171B17)
private val MTBMuted = Color(0xFF9AA39A)

private data class PreviewMember(
    val name: String,
    val memberId: String,
    val badge: String
)

private data class MainTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val newTabs = listOf(
    MainTab("home", "Home", Icons.Default.Home),
    MainTab("club", "501 Profile", Icons.Default.Groups),
    MainTab("record", "Record", Icons.Default.DirectionsBike),
    MainTab("chat", "Chat", Icons.Default.ChatBubbleOutline),
    MainTab("rider", "Rider", Icons.Default.Person)
)

/**
 * New six-screen UI prototype.
 * UI 1-5 use local preview content only. Google Authentication in Settings is live.
 * No Firestore, Storage, website data or ImageKit data is loaded by this prototype.
 */
@Composable
fun New501DesignApp() {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val credentialManager = remember { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf("home") }
    var settingsPage by remember { mutableStateOf("main") }
    var selectedMember by remember { mutableStateOf(PreviewMember("Rider name", "RID-000", "ROOKIE")) }
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authMessage by remember { mutableStateOf<String?>(null) }
    var darkTheme by remember { mutableStateOf(true) }
    var themeChoice by remember { mutableStateOf("Dark") }
    var language by remember { mutableStateOf("English") }
    var fontSize by remember { mutableStateOf("Medium") }
    var showBadgeDialog by remember { mutableStateOf(false) }
    var showChatInfo by remember { mutableStateOf(false) }

    val previewMembers = remember {
        listOf(
            PreviewMember("Member name", "RID-001", "ROOKIE"),
            PreviewMember("Member name", "RID-002", "SUPER"),
            PreviewMember("Member name", "RID-003", "ELITE"),
            PreviewMember("Member name", "RID-004", "LEADER")
        )
    }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { currentUser = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val resolvedDark = when (themeChoice) {
        "Light" -> false
        "System" -> androidx.compose.foundation.isSystemInDarkTheme()
        else -> true
    }
    darkTheme = resolvedDark

    val colors = if (resolvedDark) {
        darkColorScheme(
            primary = MTBGreen,
            onPrimary = Color.Black,
            background = MTBDark,
            surface = MTBSurface,
            onBackground = Color(0xFFF2F4F0),
            onSurface = Color(0xFFF2F4F0),
            secondary = Color(0xFFB5C0B4)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF167A13),
            onPrimary = Color.White,
            background = Color(0xFFF4F6F2),
            surface = Color.White,
            onBackground = Color(0xFF151915),
            onSurface = Color(0xFF151915),
            secondary = Color(0xFF536253)
        )
    }

    MaterialTheme(colorScheme = colors) {
        val inSettings = screen == "settings"
        val mainScreen = screen in listOf("home", "club", "record", "chat", "rider")
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (inSettings) {
                        IconButton(onClick = {
                            if (settingsPage == "main") screen = "home" else settingsPage = "main"
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            when (settingsPage) {
                                "members" -> "ALL MEMBERS"
                                "preferences" -> "APP PREFERENCES"
                                "language" -> "LANGUAGE"
                                "theme" -> "THEME"
                                "font" -> "FONT SIZE"
                                "account" -> "GOOGLE ACCOUNT"
                                else -> "SETTINGS"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            "501MTB",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            settingsPage = "main"
                            screen = "settings"
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            },
            bottomBar = {
                if (mainScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        newTabs.forEach { tab ->
                            NavigationBarItem(
                                selected = screen == tab.route,
                                onClick = { screen = tab.route },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            when (screen) {
                "home" -> HomePreview(Modifier.padding(padding))
                "club" -> ClubProfilePreview(Modifier.padding(padding))
                "record" -> RecordPreview(Modifier.padding(padding))
                "chat" -> GeneralChatPreview(
                    Modifier.padding(padding),
                    onInfo = { showChatInfo = true }
                )
                "rider" -> RiderPreview(
                    Modifier.padding(padding),
                    member = selectedMember,
                    onChangeBadge = { showBadgeDialog = true }
                )
                "settings" -> SettingsPreview(
                    modifier = Modifier.padding(padding),
                    page = settingsPage,
                    members = previewMembers,
                    language = language,
                    themeChoice = themeChoice,
                    fontSize = fontSize,
                    currentUser = currentUser,
                    isSigningIn = isSigningIn,
                    authMessage = authMessage,
                    onOpenPage = { settingsPage = it },
                    onSelectMember = {
                        selectedMember = it
                        screen = "rider"
                    },
                    onLanguage = { language = it },
                    onTheme = { themeChoice = it },
                    onFontSize = { fontSize = it },
                    onSignIn = {
                        scope.launch {
                            isSigningIn = true
                            authMessage = null
                            try {
                                val googleOption = GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(context.getString(R.string.default_web_client_id))
                                    .build()
                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleOption)
                                    .build()
                                val result = credentialManager.getCredential(context, request)
                                val credential = result.credential
                                if (credential is CustomCredential &&
                                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                ) {
                                    val googleToken = GoogleIdTokenCredential.createFrom(credential.data)
                                    val firebaseCredential = GoogleAuthProvider.getCredential(googleToken.idToken, null)
                                    auth.signInWithCredential(firebaseCredential).await()
                                } else {
                                    authMessage = "Google sign-in did not return a valid credential."
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: GetCredentialException) {
                                authMessage = "Google sign-in was cancelled or unavailable. Please try again."
                            } catch (error: Exception) {
                                authMessage = error.localizedMessage ?: "Sign-in failed."
                            } finally {
                                isSigningIn = false
                            }
                        }
                    },
                    onSignOut = {
                        scope.launch {
                            try {
                                auth.signOut()
                                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                authMessage = null
                            } catch (error: Exception) {
                                authMessage = "Sign-out completed, but the account chooser could not be reset."
                            }
                        }
                    }
                )
            }
        }

        if (showBadgeDialog) {
            AlertDialog(
                onDismissRequest = { showBadgeDialog = false },
                title = { Text("Change rider badge") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ROOKIE", "SUPER", "ELITE", "LEADER", "TOP MEMBER").forEach { badge ->
                            TextButton(onClick = {
                                selectedMember = selectedMember.copy(badge = badge)
                                showBadgeDialog = false
                            }) { Text(badge) }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBadgeDialog = false }) { Text("Close") }
                }
            )
        }
        if (showChatInfo) {
            AlertDialog(
                onDismissRequest = { showChatInfo = false },
                title = { Text("General Chat") },
                text = { Text("The chat interface is ready. Messaging will be connected in a later development stage.") },
                confirmButton = { TextButton(onClick = { showChatInfo = false }) { Text("OK") } }
            )
        }
    }
}

@Composable
private fun HomePreview(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(188.dp).clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1A2C18), Color(0xFF080B08)))),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("WHERE LEGENDS RISE", color = MTBGreen, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("RIDE BEYOND\nYOUR LIMITS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
                Text("501MTB • MADAGASCAR", color = Color.LightGray, style = MaterialTheme.typography.labelMedium)
            }
        }
        SectionHeading("UPCOMING RIDES", "See all")
        PreviewTile("NEXT RIDE", "Ride information will appear here", "DATE • DISTANCE • LOCATION")
        SectionHeading("PAST EVENTS", "Explore")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallPreviewTile("EVENT PHOTO", Modifier.weight(1f))
            SmallPreviewTile("EVENT PHOTO", Modifier.weight(1f))
        }
        SectionHeading("RIDERS & LEADERS", "Community")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallPreviewTile("RIDER PROFILE", Modifier.weight(1f))
            SmallPreviewTile("LEADER PROFILE", Modifier.weight(1f))
        }
    }
}

@Composable
private fun ClubProfilePreview(modifier: Modifier = Modifier) {
    var title by remember { mutableStateOf("501MTB") }
    var description by remember { mutableStateOf("Mountain biking community in Madagascar") }
    var vision by remember { mutableStateOf("") }
    var mission by remember { mutableStateOf("") }
    var community by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<String?>(null) }
    var editValue by remember { mutableStateOf("") }

    val parts = listOf(
        Triple("TITLE", title, "title"),
        Triple("DESCRIPTION", description, "description"),
        Triple("OUR VISION", vision, "vision"),
        Triple("OUR MISSION", mission, "mission"),
        Triple("OUR COMMUNITY", community, "community")
    )

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("501 PROFILE", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        parts.forEach { (label, value, key) ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Edit $label")
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (value.isBlank()) "Add" else "Modify") },
                                    onClick = {
                                        editValue = value
                                        editing = key
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Text(value.ifBlank { "Content not added yet" }, color = if (value.isBlank()) MTBMuted else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    if (editing != null) {
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit section") },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text("Content") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when (editing) {
                        "title" -> title = editValue
                        "description" -> description = editValue
                        "vision" -> vision = editValue
                        "mission" -> mission = editValue
                        "community" -> community = editValue
                    }
                    editing = null
                }) { Text("Save preview") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RecordPreview(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("RECORD", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
                Text("YOUR RIDES", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Your ride history and performance will be displayed here.", color = MTBMuted, textAlign = TextAlign.Center)
                Button(onClick = {}, enabled = false) { Text("START RECORDING") }
            }
        }
        PreviewTile("RIDE HISTORY", "No ride recorded yet", "DISTANCE • TIME • ELEVATION")
        PreviewTile("PERFORMANCE", "Your statistics will appear here", "KM • RIDES • TOTAL CLIMB")
    }
}

@Composable
private fun GeneralChatPreview(modifier: Modifier = Modifier, onInfo: () -> Unit) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("GENERAL CHAT", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("501MTB community", color = MTBMuted)
            }
            IconButton(onClick = onInfo) { Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Chat information") }
        }
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = MTBGreen, modifier = Modifier.size(42.dp))
                Text("Start the conversation", fontWeight = FontWeight.Bold)
                Text("Messages will appear here when chat is connected.", color = MTBMuted, textAlign = TextAlign.Center)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = "", onValueChange = {}, modifier = Modifier.weight(1f), enabled = false, placeholder = { Text("Write a message…") }, shape = RoundedCornerShape(24.dp))
            Button(onClick = {}, enabled = false) { Text("SEND") }
        }
    }
}

@Composable
private fun RiderPreview(modifier: Modifier = Modifier, member: PreviewMember, onChangeBadge: () -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(94.dp).clip(CircleShape).background(Color(0xFF263126)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MTBGreen, modifier = Modifier.size(48.dp))
                }
                Text(member.name.uppercase(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(member.memberId, color = MTBMuted)
                BadgePill(member.badge)
                OutlinedButton(onClick = onChangeBadge) { Text("Change badge • ADMIN PREVIEW") }
            }
        }
        PreviewTile("RIDER PROFILE", "Personal information will appear here", "MEMBER DETAILS")
        PreviewTile("RIDE PERFORMANCE", "Ride statistics will appear here", "DISTANCE • RIDE COUNT")
        PreviewTile("ACHIEVEMENTS", "Badges and achievements will appear here", "501MTB STATUS")
    }
}

@Composable
private fun SettingsPreview(
    modifier: Modifier,
    page: String,
    members: List<PreviewMember>,
    language: String,
    themeChoice: String,
    fontSize: String,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    isSigningIn: Boolean,
    authMessage: String?,
    onOpenPage: (String) -> Unit,
    onSelectMember: (PreviewMember) -> Unit,
    onLanguage: (String) -> Unit,
    onTheme: (String) -> Unit,
    onFontSize: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    when (page) {
        "main" -> Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("SETTINGS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            SettingsBar("All Members", "Browse the 501MTB member list", Icons.Default.Groups) { onOpenPage("members") }
            SettingsBar("App Preferences", "Language, theme and font size", Icons.Default.Palette) { onOpenPage("preferences") }
            SettingsBar("Google Account", currentUser?.email ?: "Sign in or manage your account", Icons.Default.Person) { onOpenPage("account") }
        }
        "members" -> LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 14.dp)) {
            item {
                Text("MEMBERS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("Preview list • real member data will be connected later", color = MTBMuted)
                Spacer(Modifier.height(8.dp))
            }
            items(members) { member ->
                MemberRow(member = member, onView = { onSelectMember(member) })
            }
        }
        "preferences" -> Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsBar("Language", language, Icons.Default.Language) { onOpenPage("language") }
            SettingsBar("Theme", themeChoice, Icons.Default.Palette) { onOpenPage("theme") }
            SettingsBar("Font Size", fontSize, Icons.Default.TextFields) { onOpenPage("font") }
        }
        "language" -> ChoicePage(modifier, "Choose language", listOf("English", "Français", "Malagasy"), language, onLanguage)
        "theme" -> ChoicePage(modifier, "Choose theme", listOf("Light", "Dark", "System"), themeChoice, onTheme)
        "font" -> ChoicePage(modifier, "Choose font size", listOf("Small", "Medium", "Large"), fontSize, onFontSize)
        "account" -> Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("GOOGLE ACCOUNT", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (currentUser == null) {
                        Text("Connect your Google account to access your 501MTB identity.")
                        Button(onClick = onSignIn, enabled = !isSigningIn, modifier = Modifier.fillMaxWidth()) {
                            if (isSigningIn) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("SIGN IN WITH GOOGLE")
                        }
                    } else {
                        Text(currentUser.displayName ?: "501MTB member", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(currentUser.email ?: "")
                        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Logout, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("SIGN OUT")
                        }
                    }
                    if (authMessage != null) Text(authMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Text("Authentication is connected. Member records and permissions will be integrated later.", color = MTBMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun MemberRow(member: PreviewMember, onView: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                IconButton(onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Member actions") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("View profile") }, onClick = { expanded = false; onView() })
                    DropdownMenuItem(
                        text = { Text("Change badge (ADMIN)") },
                        onClick = { expanded = false }
                    )
                }
            }
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF263126)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MTBGreen)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(member.name, fontWeight = FontWeight.Bold)
                Text(member.memberId, color = MTBMuted, style = MaterialTheme.typography.bodySmall)
            }
            BadgePill(member.badge)
        }
    }
}

@Composable
private fun ChoicePage(modifier: Modifier, title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        options.forEach { option ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)
                    .clickable { onSelect(option) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(option, modifier = Modifier.weight(1f))
                RadioButton(selected = selected == option, onClick = { onSelect(option) })
            }
        }
    }
}

@Composable
private fun SettingsBar(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MTBMuted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MTBMuted)
        }
    }
}

@Composable
private fun PreviewTile(title: String, description: String, meta: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(description, fontWeight = FontWeight.SemiBold)
            Text(meta, color = MTBMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SmallPreviewTile(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier.height(108.dp).clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF283A27), Color(0xFF111711)))),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SectionHeading(title: String, action: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        Text(action, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun BadgePill(label: String) {
    Surface(color = MTBGreen.copy(alpha = 0.14f), shape = RoundedCornerShape(30.dp)) {
        Text(label, color = MTBGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
