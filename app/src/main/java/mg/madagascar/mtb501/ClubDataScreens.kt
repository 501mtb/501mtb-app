package mg.madagascar.mtb501

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun RidesScreen() {
    val repository = remember { ClubRepository() }
    val signedIn = FirebaseAuth.getInstance().currentUser != null
    val scope = rememberCoroutineScope()
    var rides by remember { mutableStateOf<List<ClubRide>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var approved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            rides = repository.getRides()
            if (signedIn) approved = repository.getMyAccount()?.approvalStatus == ApprovalStatus.APPROVED
        } catch (e: Exception) {
            error = "Ride data is not available yet. Check the connection and Firestore setup."
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Rides", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text("Upcoming rides and completed ride history", style = MaterialTheme.typography.bodyMedium)
        when {
            loading -> CircularProgressIndicator(Modifier.padding(20.dp))
            error != null -> Text(error!!, modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.error)
            rides.isEmpty() -> Text("No rides published yet.", modifier = Modifier.padding(top = 16.dp))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                items(rides, key = { it.id }) { ride ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(ride.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text("${ride.date} • ${ride.location}")
                            Text("${ride.distanceKm} km • ${ride.type} • ${ride.status}")
                            if (ride.status == RideStatus.PLANNED) {
                                Button(
                                    enabled = signedIn && approved,
                                    onClick = {
                                        scope.launch {
                                            try {
                                                repository.registerForRide(ride.id)
                                                actionMessage = "Your registration has been saved."
                                            } catch (e: Exception) {
                                                actionMessage = "Registration unavailable. Sign in and wait for administrator approval."
                                            }
                                        }
                                    }
                                ) { Text("Register for this ride") }
                                if (!signedIn) Text("Sign in with Google to register.", style = MaterialTheme.typography.bodySmall)
                                else if (!approved) Text("Registration opens after administrator approval.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        actionMessage?.let { Text(it, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun EventsScreen() {
    var events by remember { mutableStateOf<List<ClubEvent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            events = ClubRepository().getEvents()
        } catch (e: Exception) {
            error = "Events are not available yet. Check the connection and Firestore setup."
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Events", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text("Races, carnivals, festivals and community activities", style = MaterialTheme.typography.bodyMedium)
        when {
            loading -> CircularProgressIndicator(Modifier.padding(20.dp))
            error != null -> Text(error!!, modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.error)
            events.isEmpty() -> Text("No events published yet.", modifier = Modifier.padding(top = 16.dp))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                items(events, key = { it.id }) { event ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(event.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text("${event.date} • ${event.location}")
                            Text(event.type.name)
                            if (event.description.isNotBlank()) Text(event.description)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RidersScreen() {
    var members by remember { mutableStateOf<List<ClubMember>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            members = ClubRepository().getPublicMembers()
        } catch (e: Exception) {
            error = "Member profiles are not available yet. Check the connection and Firestore setup."
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Riders", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text("Discover the 501MTB community", style = MaterialTheme.typography.bodyMedium)
        when {
            loading -> CircularProgressIndicator(Modifier.padding(20.dp))
            error != null -> Text(error!!, modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.error)
            members.isEmpty() -> Text("No public rider profiles yet.", modifier = Modifier.padding(top = 16.dp))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                items(members, key = { it.id }) { member ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(member.name, style = MaterialTheme.typography.titleMedium)
                            Text(member.badge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AdminPanel() {
    val repository = remember { ClubRepository() }
    val scope = rememberCoroutineScope()
    var isAdmin by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var accounts by remember { mutableStateOf<List<PendingAccount>>(emptyList()) }
    var memberIds by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        val account = repository.getMyAccount()
        isAdmin = account?.role == AppRole.ADMIN
        if (isAdmin) accounts = repository.getPendingAccounts()
    }

    LaunchedEffect(Unit) {
        try {
            refresh()
        } catch (e: Exception) {
            message = "Admin data could not be loaded. Check administrator permissions and Firestore."
        } finally {
            loading = false
        }
    }

    if (loading) {
        CircularProgressIndicator(Modifier.padding(16.dp))
    } else if (isAdmin) {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Administrator", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("Pending account approvals: ${accounts.size}")
            if (accounts.isEmpty()) Text("No accounts waiting for approval.")
            accounts.forEach { account ->
                var localMemberId by remember(account.uid) { mutableStateOf("") }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(account.displayName.ifBlank { "New member" }, style = MaterialTheme.typography.titleMedium)
                        Text(account.email, style = MaterialTheme.typography.bodySmall)
                        androidx.compose.material3.OutlinedTextField(
                            value = localMemberId,
                            onValueChange = { localMemberId = it },
                            label = { Text("Existing member ID (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                scope.launch {
                                    try {
                                        repository.reviewAccount(account.uid, true, localMemberId.trim().ifBlank { null })
                                        message = "Account approved."
                                        refresh()
                                    } catch (e: Exception) {
                                        message = e.localizedMessage ?: "Approval failed."
                                    }
                                }
                            }) { Text("Approve") }
                            Button(onClick = {
                                scope.launch {
                                    try {
                                        repository.reviewAccount(account.uid, false)
                                        message = "Account rejected."
                                        refresh()
                                    } catch (e: Exception) {
                                        message = e.localizedMessage ?: "Rejection failed."
                                    }
                                }
                            }) { Text("Reject") }
                        }
                    }
                }
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    } else if (FirebaseAuth.getInstance().currentUser != null) {
        Text("Administrator access only.", modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.error)
    }
}
