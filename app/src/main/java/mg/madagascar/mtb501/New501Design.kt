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
import androidx.compose.ui.platform.LocalDensity
import org.json.JSONObject
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

private fun tr(language: String, en: String, fr: String, mg: String): String = when (language) {
    "Français" -> fr
    "Malagasy" -> mg
    else -> en
}

private fun loadWorkbookRoster(context: Context): List<PreviewMember> = runCatching {
    val json = context.assets.open("501mtb-roster.json").bufferedReader().use { it.readText() }
    val members = JSONObject(json).getJSONArray("members")
    (0 until members.length()).mapNotNull { index ->
        val item = members.getJSONObject(index)
        val id = item.optString("id").trim()
        val name = item.optString("name").trim()
        if (id.isBlank() || name.isBlank()) null
        else PreviewMember(name, id, item.optString("badge", "Rookie"))
    }
}.getOrDefault(emptyList())

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
 * New six-screen 501MTB app UI.
 * Google Authentication and existing Firestore account/member records are connected.
 * No account migration or automatic member creation is performed here.
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
    val appPrefs = remember { context.getSharedPreferences("501mtb_app_preferences", Context.MODE_PRIVATE) }
    var themeChoice by remember { mutableStateOf(appPrefs.getString("theme", "Dark") ?: "Dark") }
    var language by remember { mutableStateOf(appPrefs.getString("language", "English") ?: "English") }
    var fontSize by remember { mutableStateOf(appPrefs.getString("fontSize", "Medium") ?: "Medium") }

    LaunchedEffect(themeChoice, language, fontSize) {
        appPrefs.edit()
            .putString("theme", themeChoice)
            .putString("language", language)
            .putString("fontSize", fontSize)
            .apply()
    }
    var showBadgeDialog by remember { mutableStateOf(false) }
    var badgeTargetId by remember { mutableStateOf<String?>(null) }
    var showChatInfo by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }
    var isLoadingMembers by remember { mutableStateOf(false) }
    val previewMembers = remember { mutableStateListOf<PreviewMember>() }
    val pendingAccounts = remember { mutableStateListOf<PendingAccount>() }
    var isLoadingApprovals by remember { mutableStateOf(false) }
    var approvalMessage by remember { mutableStateOf<String?>(null) }
    val repository = remember { ClubRepository() }

    LaunchedEffect(currentUser?.uid) {
        previewMembers.clear()
        isAdmin = false
        if (currentUser != null) {
            isLoadingMembers = true
            try {
                // Every authenticated Google account receives a pending 501MTB record
                // and member ID before its approval status is checked.
                repository.ensurePendingAccount()
                val account = repository.getMyAccount()
                isAdmin = account?.role == AppRole.ADMIN

                // The published V2 workbook export is the base registry. Firestore
                // records overlay it when a badge has been changed by an administrator.
                val merged = linkedMapOf<String, PreviewMember>()
                loadWorkbookRoster(context).forEach { merged[it.name.trim().lowercase()] = it }
                val records = if (isAdmin) repository.getAllMembers() else repository.getPublicMembers()
                records.forEach {
                    merged[it.name.trim().lowercase()] = PreviewMember(it.name, it.id, it.badge)
                }
                previewMembers.addAll(merged.values.sortedBy { it.name.lowercase() })
                if (account == null) {
                    authMessage = "Google account connected, but no 501MTB user record was found."
                }
            } catch (error: Exception) {
                authMessage = error.localizedMessage ?: "Unable to load 501MTB member records."
            } finally {
                isLoadingMembers = false
            }
        }
    }

    LaunchedEffect(screen, settingsPage, isAdmin) {
        if (screen == "settings" && settingsPage == "approvals" && isAdmin) {
            isLoadingApprovals = true
            approvalMessage = null
            try {
                pendingAccounts.clear()
                pendingAccounts.addAll(repository.getPendingAccounts())
            } catch (error: Exception) {
                approvalMessage = error.localizedMessage ?: "Unable to load pending accounts."
            } finally {
                isLoadingApprovals = false
            }
        }
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

    val baseDensity = LocalDensity.current
    val fontScaleFactor = when (fontSize) {
        "Small" -> 0.90f
        "Large" -> 1.15f
        else -> 1.0f
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalDensity provides baseDensity.copy(fontScale = baseDensity.fontScale * fontScaleFactor)
    ) {
    MaterialTheme(colorScheme = colors) {
        val inSettings = screen == "settings"
        val mainScreen = screen in listOf("home", "club", "record", "chat", "rider")
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (screen != "home") Row(
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
                                "members" -> tr(language, "ALL MEMBERS", "TOUS LES MEMBRES", "NY MPAMBARA REHETRA")
                                "preferences" -> tr(language, "APP PREFERENCES", "PRÉFÉRENCES", "SAFIDIN'NY RINDRANA")
                                "language" -> tr(language, "LANGUAGE", "LANGUE", "TENY")
                                "theme" -> tr(language, "THEME", "THÈME", "ENDRIKA")
                                "font" -> tr(language, "FONT SIZE", "TAILLE DU TEXTE", "HABEN'NY SORATRA")
                                "account" -> tr(language, "GOOGLE ACCOUNT", "COMPTE GOOGLE", "KAONTY GOOGLE")
                                "approvals" -> tr(language, "PENDING APPROVALS", "DEMANDES EN ATTENTE", "FANKATOAVANA MIANDRY")
                                else -> tr(language, "SETTINGS", "PARAMÈTRES", "FANDAMINANA")
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
                                label = { Text(
                                    when (tab.route) {
                                        "home" -> tr(language, "Home", "Accueil", "Fandraisana")
                                        "club" -> tr(language, "501 Profile", "Profil 501", "Mombamomba 501")
                                        "record" -> tr(language, "Record", "Activité", "Dia")
                                        "chat" -> tr(language, "Chat", "Discussion", "Resaka")
                                        "rider" -> tr(language, "Rider", "Cycliste", "Mpitaingina")
                                        else -> tab.label
                                    },
                                    fontSize = 10.sp
                                ) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            when (screen) {
                "home" -> DemoAdminHomeScreen(onOpenChat = { screen = "chat" }, onOpenSettings = { settingsPage = "main"; screen = "settings" }, onAdminModeChange = { /* Preview mode only; never grants administrator access. */ })
                "club" -> ClubProfilePreview(Modifier.padding(padding), isAdmin = isAdmin, language = language)
                "record" -> RecordPreview(Modifier.padding(padding))
                "chat" -> GeneralChatPreview(
                    Modifier.padding(padding),
                    onInfo = { showChatInfo = true }
                )
                "rider" -> RiderPreview(
                    Modifier.padding(padding),
                    member = selectedMember,
                    isAdminPreview = isAdmin,
                    onChangeBadge = { if (isAdmin) { badgeTargetId = selectedMember.memberId; showBadgeDialog = true } }
                )
                "settings" -> SettingsPreview(
                    modifier = Modifier.padding(padding),
                    page = settingsPage,
                    members = previewMembers,
                    isAdminPreview = isAdmin,
                    isLoadingMembers = isLoadingMembers,
                    pendingAccounts = pendingAccounts,
                    isLoadingApprovals = isLoadingApprovals,
                    approvalMessage = approvalMessage,
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
                    onChangeMemberBadge = {
                        selectedMember = it
                        badgeTargetId = it.memberId
                        showBadgeDialog = true
                    },
                    onReviewAccount = { account, approve ->
                        scope.launch {
                            try {
                                repository.reviewAccount(account.uid, approve)
                                pendingAccounts.removeAll { it.uid == account.uid }
                                approvalMessage = if (approve) "Account approved." else "Account rejected."
                            } catch (error: Exception) {
                                approvalMessage = error.localizedMessage ?: "Unable to review this account."
                            }
                        }
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
                        listOf("Rookie", "Super", "Elite", "Leader", "Top Member").forEach { badge ->
                            TextButton(onClick = {
                                if (!isAdmin || badgeTargetId.isNullOrBlank()) {
                                    authMessage = "Administrator access is required to change a badge."
                                    showBadgeDialog = false
                                } else {
                                    scope.launch {
                                        try {
                                            repository.updateMemberBadge(badgeTargetId!!, badge, selectedMember.name)
                                            selectedMember = selectedMember.copy(badge = badge)
                                            val targetIndex = previewMembers.indexOfFirst { it.memberId == badgeTargetId }
                                            if (targetIndex >= 0) previewMembers[targetIndex] = previewMembers[targetIndex].copy(badge = badge)
                                            authMessage = "Badge updated."
                                        } catch (error: Exception) {
                                            authMessage = error.localizedMessage ?: "Badge update failed."
                                        } finally {
                                            showBadgeDialog = false
                                        }
                                    }
                                }
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
private fun ClubProfilePreview(
    modifier: Modifier = Modifier,
    isAdmin: Boolean,
    language: String
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var vision by remember { mutableStateOf("") }
    var mission by remember { mutableStateOf("") }
    var community by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<String?>(null) }
    var editValue by remember { mutableStateOf("") }

    val sections = listOf(
        Triple("OUR VISION", vision, "vision"),
        Triple("OUR MISSION", mission, "mission"),
        Triple("OUR COMMUNITY", community, "community")
    )

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            tr(language, "501 PROFILE", "PROFIL 501", "MOMBA NY 501"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )

        listOf(
            Triple("TITLE", title, "title"),
            Triple("DESCRIPTION", description, "description")
        ).forEach { (label, value, key) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (isAdmin) SectionEditMenu(value.isBlank(), label) {
                        editValue = value
                        editing = key
                    }
                }
                Text(
                    value.ifBlank { tr(language, "Content not added yet", "Contenu à ajouter", "Mbola tsy nampidirina") },
                    color = if (value.isBlank()) MTBMuted else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        sections.forEach { (label, value, key) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(
                            value.ifBlank { tr(language, "Content not added yet", "Contenu à ajouter", "Mbola tsy nampidirina") },
                            color = if (value.isBlank()) MTBMuted else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (isAdmin) SectionEditMenu(value.isBlank(), label) {
                        editValue = value
                        editing = key
                    }
                }
            }
        }
    }

    if (editing != null) {
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(tr(language, "Edit section", "Modifier la section", "Hanova ny fizarana")) },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text(tr(language, "Content", "Contenu", "Votoaty")) }
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
                }) { Text(tr(language, "Save preview", "Enregistrer l'aperçu", "Tehirizo ny topimaso")) }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) {
                    Text(tr(language, "Cancel", "Annuler", "Hanafoana"))
                }
            }
        )
    }
}

@Composable
private fun SectionEditMenu(isEmpty: Boolean, label: String, onEdit: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Edit $label")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(if (isEmpty) "Add" else "Modify") },
                onClick = {
                    expanded = false
                    onEdit()
                }
            )
        }
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
private fun RiderPreview(modifier: Modifier = Modifier, member: PreviewMember, isAdminPreview: Boolean, onChangeBadge: () -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(94.dp).clip(CircleShape).background(Color(0xFF263126)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MTBGreen, modifier = Modifier.size(48.dp))
                }
                Text(member.name.uppercase(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(member.memberId, color = MTBMuted)
                BadgePill(member.badge)
                if (isAdminPreview) OutlinedButton(onClick = onChangeBadge) { Text("Change badge • ADMIN PREVIEW") }
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
    isAdminPreview: Boolean,
    isLoadingMembers: Boolean,
    pendingAccounts: List<PendingAccount>,
    isLoadingApprovals: Boolean,
    approvalMessage: String?,
    language: String,
    themeChoice: String,
    fontSize: String,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    isSigningIn: Boolean,
    authMessage: String?,
    onOpenPage: (String) -> Unit,
    onSelectMember: (PreviewMember) -> Unit,
    onChangeMemberBadge: (PreviewMember) -> Unit,
    onReviewAccount: (PendingAccount, Boolean) -> Unit,
    onLanguage: (String) -> Unit,
    onTheme: (String) -> Unit,
    onFontSize: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    when (page) {
        "main" -> Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(tr(language, "SETTINGS", "PARAMÈTRES", "FANDAMINANA"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            SettingsBar(tr(language, "All Members", "Tous les membres", "Mpikambana rehetra"), tr(language, "Browse the complete 501MTB member register", "Consulter le registre complet des membres 501MTB", "Hijery ny lisitry ny mpikambana 501MTB"), Icons.Default.Groups) { onOpenPage("members") }
            if (isAdminPreview) {
                SettingsBar(tr(language, "Pending Approvals", "Demandes en attente", "Fankatoavana miandry"), tr(language, "Review new member account requests", "Valider les nouvelles demandes de compte", "Hamarino ny fangatahana kaonty vaovao"), Icons.Default.Person) { onOpenPage("approvals") }
            }
            SettingsBar(tr(language, "App Preferences", "Préférences", "Safidin'ny rindrana"), tr(language, "Language, theme and font size", "Langue, thème et taille du texte", "Teny, endrika ary haben'ny soratra"), Icons.Default.Palette) { onOpenPage("preferences") }
            SettingsBar(tr(language, "Google Account", "Compte Google", "Kaonty Google"), currentUser?.email ?: tr(language, "Sign in or manage your account", "Se connecter ou gérer le compte", "Hiditra na hitantana ny kaonty"), Icons.Default.Person) { onOpenPage("account") }
        }
        "members" -> LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 14.dp)) {
            item {
                Text(tr(language, "MEMBERS", "MEMBRES", "MPIKAMBANA"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(
                    if (isAdminPreview) tr(language, "Complete 501MTB registry", "Registre complet 501MTB", "Lisitry ny mpikambana 501MTB")
                    else tr(language, "501MTB member registry", "Registre des membres 501MTB", "Lisitry ny mpikambana 501MTB"),
                    color = MTBMuted
                )
                if (members.isEmpty()) {
                    Text(
                        if (isLoadingMembers) tr(language, "Loading members…", "Chargement des membres…", "Fampidirana ny mpikambana…") else tr(language, "No member profiles available.", "Aucun membre disponible.", "Tsy misy mpikambana hita."),
                        color = MTBMuted
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            items(members) { member ->
                MemberRow(member = member, isAdminPreview = isAdminPreview, onView = { onSelectMember(member) }, onChangeBadge = { onChangeMemberBadge(member) })
            }
        }
        "approvals" -> {
            if (isAdminPreview) {
                LazyColumn(
                    modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    item {
                        Text(tr(language, "NEW MEMBER REQUESTS", "NOUVELLES DEMANDES", "FANGATAHANA VAOVAO"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text(tr(language, "Approve or reject accounts waiting for 501MTB validation.", "Approuver ou refuser les comptes en attente de validation 501MTB.", "Ekeo na lavina ny kaonty miandry fanamarinana 501MTB."), color = MTBMuted)
                        if (isLoadingApprovals) {
                            CircularProgressIndicator(Modifier.padding(vertical = 12.dp))
                        } else if (pendingAccounts.isEmpty()) {
                            Text(tr(language, "No pending requests.", "Aucune demande en attente.", "Tsy misy fangatahana miandry."), color = MTBMuted, modifier = Modifier.padding(top = 12.dp))
                        }
                        approvalMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
                    }
                    items(pendingAccounts, key = { it.uid }) { account ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(account.displayName.ifBlank { "New 501MTB member" }, fontWeight = FontWeight.Bold)
                                Text(account.email, color = MTBMuted, style = MaterialTheme.typography.bodySmall)
                                Text("501MTB ID: ${account.memberId.ifBlank { "Not assigned" }}", color = MTBMuted, style = MaterialTheme.typography.bodySmall)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { onReviewAccount(account, true) }, modifier = Modifier.weight(1f)) {
                                        Text(tr(language, "APPROVE", "APPROUVER", "EKENA"))
                                    }
                                    OutlinedButton(onClick = { onReviewAccount(account, false) }, modifier = Modifier.weight(1f)) {
                                        Text(tr(language, "REJECT", "REFUSER", "LAVINA"))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Text("Administrator access required.", modifier = modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
            }
        }
        "preferences" -> Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsBar(tr(language, "Language", "Langue", "Teny"), language, Icons.Default.Language) { onOpenPage("language") }
            SettingsBar(tr(language, "Theme", "Thème", "Endrika"), themeChoice, Icons.Default.Palette) { onOpenPage("theme") }
            SettingsBar(tr(language, "Font Size", "Taille du texte", "Haben'ny soratra"), fontSize, Icons.Default.TextFields) { onOpenPage("font") }
        }
        "language" -> ChoicePage(modifier, tr(language, "Choose language", "Choisir la langue", "Misafidiana teny"), listOf("English", "Français", "Malagasy"), language, onLanguage)
        "theme" -> ChoicePage(modifier, tr(language, "Choose theme", "Choisir le thème", "Misafidiana endrika"), listOf("Light", "Dark", "System"), themeChoice, onTheme)
        "font" -> ChoicePage(modifier, tr(language, "Choose font size", "Choisir la taille du texte", "Misafidiana haben'ny soratra"), listOf("Small", "Medium", "Large"), fontSize, onFontSize)
        "account" -> Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(tr(language, "GOOGLE ACCOUNT", "COMPTE GOOGLE", "KAONTY GOOGLE"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
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
                    Text(
                        if (currentUser == null) "Sign in with one of the existing 501MTB Google accounts."
                        else "Your existing 501MTB account and permissions are read from Firestore.",
                        color = MTBMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberRow(member: PreviewMember, isAdminPreview: Boolean, onView: () -> Unit, onChangeBadge: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                IconButton(onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Member actions") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("View profile") }, onClick = { expanded = false; onView() })
                    if (isAdminPreview) {
                        DropdownMenuItem(
                            text = { Text("Change badge (ADMIN)") },
                            onClick = { expanded = false; onChangeBadge() }
                        )
                    }
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
