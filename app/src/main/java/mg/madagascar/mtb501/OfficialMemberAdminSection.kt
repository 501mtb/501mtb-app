package mg.madagascar.mtb501

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun OfficialMemberAdminSection(onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val registry = remember { OfficialMemberRegistry() }
    val scope = rememberCoroutineScope()
    var members by remember { mutableStateOf<List<OfficialMember>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("M") }
    var category by remember { mutableStateOf("ROOKIE") }
    var busy by remember { mutableStateOf(false) }

    suspend fun reload() { members = registry.getOfficialMembers() }
    LaunchedEffect(Unit) {
        try { reload() } catch (e: Exception) {
            onMessage(e.localizedMessage ?: "Official member list could not be loaded.")
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Official 501MTB register", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
            Text("Official members: ${members.size}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        try {
                            val count = registry.initializeOfficialRegister(context)
                            reload()
                            onMessage("$count official members imported successfully.")
                        } catch (e: Exception) {
                            onMessage(e.localizedMessage ?: "Register import failed.")
                        } finally { busy = false }
                    }
                }) { Text("Initialize official register") }
                Button(enabled = !busy, onClick = {
                    scope.launch {
                        try { reload() } catch (e: Exception) {
                            onMessage(e.localizedMessage ?: "Refresh failed.")
                        }
                    }
                }) { Text("Refresh") }
            }
            Text("Add a member", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(name, { name = it }, label = { Text("Full name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { gender = "M" }, enabled = gender != "M") { Text("M") }
                Button(onClick = { gender = "W" }, enabled = gender != "W") { Text("W") }
            }
            OutlinedTextField(category, { category = it.uppercase() }, label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(enabled = !busy && name.isNotBlank() && category.isNotBlank(), onClick = {
                scope.launch {
                    busy = true
                    try {
                        val created = registry.addMember(name, gender, category)
                        name = ""
                        reload()
                        onMessage("Member created: ${created.memberId}")
                    } catch (e: Exception) {
                        onMessage(e.localizedMessage ?: "Member could not be created.")
                    } finally { busy = false }
                }
            }) { Text(if (busy) "Please wait…" else "Add member") }

            Text("Member directory", style = MaterialTheme.typography.titleSmall)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                items(members, key = { it.memberId }) { member ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(member.memberId, style = MaterialTheme.typography.bodySmall)
                        Text(member.name, style = MaterialTheme.typography.bodyMedium)
                        Text(member.category, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
