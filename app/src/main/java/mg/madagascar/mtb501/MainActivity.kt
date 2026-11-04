package mg.madagascar.mtb501

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClubTheme {
                ClubApp()
            }
        }
    }
}

private data class AppTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val appTabs = listOf(
    AppTab("feed", "Home", Icons.Default.Home),
    AppTab("home", "501 Profile", Icons.Default.Groups),
    AppTab("record", "Record Ride", Icons.Default.DirectionsBike),
    AppTab("chat", "Chat", Icons.Default.ChatBubbleOutline),
    AppTab("rider", "Rider", Icons.Default.Person)
)

@Composable
private fun ClubApp() {
    val navController = rememberNavController()
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUserUid by remember { mutableStateOf(auth.currentUser?.uid) }
    var isAdmin by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUserUid = firebaseAuth.currentUser?.uid
            if (firebaseAuth.currentUser == null) isAdmin = false
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(currentUserUid) {
        isAdmin = false
        if (currentUserUid != null) {
            try {
                isAdmin = ClubRepository().getMyAccount()?.role == AppRole.ADMIN
            } catch (_: Exception) {
                isAdmin = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (isAdmin) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        navController.navigate("admin") { launchSingleTop = true }
                    }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Administrator panel",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            )
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = R.drawable.mtb501_app_icon),
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                appTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { androidx.compose.material3.Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "feed",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("feed") { HomeFeedScreen(onOpenCommunityFeed = { navController.navigate("community-feed") }) }
            composable("community-feed") { FeedScreen(onOpenPhotoLibrary = { navController.navigate("photos") }) }
            composable("photos") { SharedPhotoLibraryScreen() }
            composable("home") { HomeScreen() }
            composable("record") { RecordRideScreen() }
            composable("chat") { ChatScreen() }
            composable("rider") { if (isAdmin) AdminRiderManagementScreen() else RiderProfileScreen() }
            composable("rides") { RidesScreen() }
            composable("events") { EventsScreen() }
            composable("riders") { RidersScreen() }
            composable("admin") {
                if (isAdmin) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                    ) {
                        Text(
                            "Administrator",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        AdminPanel()
                    }
                } else {
                    Text(
                        "Administrator access required.",
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen() {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val credentialManager = remember { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var memberId by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authMessage by remember { mutableStateOf<String?>(null) }
    var coverUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var coverIndex by remember { mutableStateOf(0) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
            if (firebaseAuth.currentUser == null) memberId = null
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            try {
                val repository = ClubRepository()
                repository.ensurePendingAccount()
                memberId = repository.getMyAccount()?.memberId
            } catch (error: Exception) {
                authMessage = error.localizedMessage ?: "Your 501MTB ID could not be loaded."
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val repository = ClubRepository()
            val library = PhotoLibraryRepository().load()
            val selectedIds = repository.getPhotoAssignment("501mtb.pdc")
            val available = library.imagesForSlot("501mtb.pdc").associateBy { it.id }
            coverUrls = selectedIds.mapNotNull { available[it]?.url }
                .ifEmpty { library.imagesForSlot("501mtb.pdc").map { it.url } }
            coverIndex = 0
        } catch (_: Exception) {
            // Keep the profile usable if the remote photo catalogue is temporarily unavailable.
        }
    }

    LaunchedEffect(coverUrls) {
        while (coverUrls.size > 1) {
            delay(6000)
            coverIndex = (coverIndex + 1) % coverUrls.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.mtb501_app_icon),
                    contentDescription = "501MTB emblem",
                    modifier = Modifier.size(112.dp)
                )
                Text(
                    "501MTB",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "MADAGASCAR • MOUNTAIN BIKING",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "WHERE LEGENDS RISE",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Discover Yourself, Broaden Your Horizon, Explore The World",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (coverUrls.isNotEmpty()) {
            AsyncImage(
                model = coverUrls.getOrNull(coverIndex),
                contentDescription = "501MTB official profile cover",
                modifier = Modifier.fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )
        }

        ProfileInfoCard(
            title = "OUR VISION",
            description = "To become the leading reference for mountain biking in Madagascar."
        )
        ProfileInfoCard(
            title = "OUR MISSION",
            description = "To develop and promote mountain biking by organizing events, supporting riders, and building a strong and united MTB community."
        )
        ProfileInfoCard(
            title = "OUR COMMUNITY",
            description = "501MTB brings riders together through events, trail rides, race experiences and community support across Madagascar."
        )

        AccountCard(
            displayName = currentUser?.displayName,
            email = currentUser?.email,
            memberId = memberId,
            isSigningIn = isSigningIn,
            message = authMessage,
            onSignIn = {
                scope.launch {
                    isSigningIn = true
                    authMessage = null
                    try {
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(context.getString(R.string.default_web_client_id))
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        val result = credentialManager.getCredential(
                            context = context,
                            request = request
                        )

                        val credential = result.credential
                        if (credential is CustomCredential &&
                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        ) {
                            val googleToken = GoogleIdTokenCredential.createFrom(credential.data)
                            val firebaseCredential = GoogleAuthProvider.getCredential(
                                googleToken.idToken,
                                null
                            )
                            auth.signInWithCredential(firebaseCredential).await()
                        } else {
                            authMessage = "Google did not return a valid sign-in credential. Please try again."
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: GetCredentialException) {
                        authMessage = "Google sign-in was cancelled or could not be started. Please try again."
                    } catch (error: Exception) {
                        authMessage = error.localizedMessage ?: "Sign-in failed. Please try again."
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
                        authMessage = "You are signed out, but the Google account chooser could not be reset."
                    }
                }
            }
        )
    }
}

@Composable
private fun ProfileInfoCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AccountCard(
    displayName: String?,
    email: String?,
    memberId: String?,
    isSigningIn: Boolean,
    message: String?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Your Account",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            if (email == null) {
                Text(
                    "Sign in with your Google account to create your 501MTB app account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = onSignIn,
                    enabled = !isSigningIn,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.height(0.dp))
                    } else {
                        Text("Continue with Google")
                    }
                }
            } else {
                Text(
                    displayName ?: "501MTB member",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                memberId?.let {
                    Text("501MTB ID: $it", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    "Your account is signed in. Member access may require administrator approval.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onSignOut) {
                    Text("Sign out")
                }
            }

            if (message != null) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun InfoCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
