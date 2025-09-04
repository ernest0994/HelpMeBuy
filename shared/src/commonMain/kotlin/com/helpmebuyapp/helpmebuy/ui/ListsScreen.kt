package com.helpmebuyapp.helpmebuy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helpmebuyapp.helpmebuy.model.ListEntity
import com.helpmebuyapp.helpmebuy.repository.ListRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Simple reactive Lists UI implemented in shared module using Compose Multiplatform.
 *
 * - TextField to edit the selected list name
 * - LazyColumn displaying items (as text) with add/remove controls
 * - Dark mode toggle to switch theme dynamically
 */
@Composable
fun ListsScreen(
    lists: List<ListEntity>,
    selected: ListEntity?,
    onSelect: (ListEntity) -> Unit,
    onInsert: (String) -> Unit,
    onUpdate: (ListEntity) -> Unit,
    onDelete: (ListEntity) -> Unit,
    isDark: Boolean,
    onToggleDark: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {
        Surface(modifier = modifier.fillMaxSize()) {
            var newName by remember { mutableStateOf("") }
            var editingName by remember(selected?.id) { mutableStateOf(selected?.name ?: "") }

            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Lists", style = MaterialTheme.typography.headlineSmall)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("Dark")
                        Switch(checked = isDark, onCheckedChange = onToggleDark)
                    }
                }

                // Create new list
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New list name") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { if (newName.isNotBlank()) { onInsert(newName.trim()); newName = "" } }) {
                        Text("Add")
                    }
                }

                // Selected list editor
                if (selected != null) {
                    OutlinedTextField(
                        value = editingName,
                        onValueChange = { editingName = it },
                        label = { Text("Selected list name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onUpdate(SimpleList(selected.id, editingName, selected.category)) }) { Text("Save") }
                        OutlinedButton(onClick = { onDelete(selected) }) { Text("Delete") }
                    }
                }

                HorizontalDivider()

                Text("All Lists", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(lists, key = { it.id }) { list ->
                        ListRow(
                            list = list,
                            selected = selected?.id == list.id,
                            onClick = { onSelect(list) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListRow(list: ListEntity, selected: Boolean, onClick: () -> Unit) {
    Surface(
        tonalElevation = if (selected) 4.dp else 0.dp,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(text = list.name, style = MaterialTheme.typography.bodyLarge)
                Text(text = list.category, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onClick) { Text(if (selected) "Selected" else "Select") }
        }
    }
}

// Minimal common ListEntity implementation for UI editing purposes
private data class SimpleList(
    override val id: Int,
    override val name: String,
    override val category: String,
) : ListEntity

/**
 * Repository-backed wrapper that collects flows and wires callbacks.
 */
@Composable
fun ListsScreen(repo: ListRepository) {
    // Local UI state
    var isDark by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Collect lists reactively
    var lists by remember { mutableStateOf<List<ListEntity>>(emptyList()) }
    var selected by remember { mutableStateOf<ListEntity?>(null) }

    LaunchedEffect(repo) {
        repo.getAll().collectLatest { newLists ->
            lists = newLists
            if (selected == null) selected = newLists.firstOrNull()
        }
    }

    ListsScreen(
        lists = lists,
        selected = selected,
        onSelect = { selected = it },
        onInsert = { name ->
            // Default category placeholder; platform repos may enrich later
            val nextId = (lists.maxOfOrNull { it.id } ?: 0) + 1
            scope.launch { repo.insert(SimpleList(id = nextId, name = name, category = "General")) }
        },
        onUpdate = { upd -> scope.launch { repo.update(upd) } },
        onDelete = { del -> scope.launch { repo.delete(del) } },
        isDark = isDark,
        onToggleDark = { isDark = it },
        modifier = Modifier.fillMaxSize()
    )
}

