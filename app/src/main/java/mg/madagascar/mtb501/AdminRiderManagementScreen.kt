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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val memberBadgeOptions = listOf(
    "Rookie", "Super", "Elite", "Leader", "Top Member", "Admin", "Ambassador"
)

@Composable
fun AdminRiderManagementScreen() {
    val repository = remember { ClubRepository() }
    val scope = rememberCoroutineScope()
    var members by remember { mutableStateOf<List<ClubMember>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var savingId by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        members = repository.getAllMembers()
    }

    LaunchedEffect(Unit) {
        try {
            if (repository.getMyAccount()?.role != AppRole.ADMIN) {
                error = "Administrator access required."
            } else {
                reload()
            }
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Member list could not be loaded."
        } finally {
            loading = false
        }
    }

    val filteredMembers = members.filter {
        it.name.contains(search, ignoreCase = true) ||
            it.id.contains(search, ignoreCase = true) ||
            it.badge.contains(search, ignoreCase = true)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Member Management", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text("All registered 501MTB members • Manual administration", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search name, member ID or badge") }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Members: ${members.size}", style = MaterialTheme.typography.titleMedium)
            TextButton(
                enabled = !loading,
                onClick = {
                    scope.launch {
                        loading = true
                        try {
                            reload()
                            message = "Member list refreshed."
                            error = null
                        } catch (e: Exception) {
                            error = e.localizedMessage ?: "Refresh failed."
                        } finally {
                            loading = false
                        }
                    }
                }
            ) { Text("Refresh") }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        when {
            loading -> CircularProgressIndicator(Modifier.padding(20.dp))
            error != null -> Unit
            members.isEmpty() -> Text("No member records found.")
            filteredMembers.isEmpty() -> Text("No matching members.")
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMembers, key = { it.id }) { member ->
                    AdminMemberCard(
                        member = member,
                        saving = savingId == member.id,
                        onBadgeChange = { badge ->
                            scope.launch {
                                savingId = member.id
                                try {
                                    repository.updateMemberBadge(member.id, badge)
                                    members = members.map { if (it.id == member.id) it.copy(badge = badge) else it }
                                    message = "${member.name}: badge changed to $badge."
                                } catch (e: Exception) {
                                    message = e.localizedMessage ?: "Badge update failed."
                                } finally {
                                    savingId = null
                                }
                            }
                        },
                        onVisibilityChange = { visible ->
                            scope.launch {
                                savingId = member.id
                                try {
                                    repository.updateMemberVisibility(member.id, visible)
                                    members = members.map { if (it.id == member.id) it.copy(publicProfile = visible) else it }
                                    message = "${member.name}: public profile ${if (visible) "enabled" else "disabled"}."
                                } catch (e: Exception) {
                                    message = e.localizedMessage ?: "Visibility update failed."
                                } finally {
                                    savingId = null
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminMemberCard(
    member: ClubMember,
    saving: Boolean,
    onBadgeChange: (String) -> Unit,
    onVisibilityChange: (Boolean) -> Unit
) {
    var badgeMenuOpen by remember(member.id) { mutableStateOf(false) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(member.name, style = MaterialTheme.typography.titleMedium)
                    Text("Member record: ${member.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(member.badge, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Manual badge", style = MaterialTheme.typography.labelMedium)
                    TextButton(
                        enabled = !saving,
                        onClick = { badgeMenuOpen = true }
                    ) { Text("Change badge") }
                    DropdownMenu(
                        expanded = badgeMenuOpen,
                        onDismissRequest = { badgeMenuOpen = false }
                    ) {
                        memberBadgeOptions.forEach { badge ->
                            DropdownMenuItem(
                                text = { Text(badge) },
                                onClick = {
                                    badgeMenuOpen = false
                                    if (badge != member.badge) onBadgeChange(badge)
                                }
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Public profile", style = MaterialTheme.typography.labelMedium)
                    Switch(
                        checked = member.publicProfile,
                        onCheckedChange = onVisibilityChange,
                        enabled = !saving
                    )
                }
            }
            if (saving) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator()
                    Text("Saving changes…", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
