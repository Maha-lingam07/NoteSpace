package com.notespace.app

import android.os.Bundle
import android.content.Context
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private data class Note(val id: String, val category: String, val topic: String, val content: String)

private class NoteStore(context: Context) {
    private val prefs = context.getSharedPreferences("notespace", Context.MODE_PRIVATE)
    fun categories(): List<String> = JSONArray(prefs.getString("categories", "[]")).let { a -> (0 until a.length()).map { a.getString(it) } }
    fun notes(): List<Note> = JSONArray(prefs.getString("notes", "[]")).let { a -> (0 until a.length()).map { i -> a.getJSONObject(i).let { Note(it.getString("id"), it.getString("category"), it.getString("topic"), it.getString("content")) } } }
    fun saveCategories(value: List<String>) { prefs.edit().putString("categories", JSONArray(value.distinct()).toString()).apply() }
    fun saveNotes(value: List<Note>) { prefs.edit().putString("notes", JSONArray().apply { value.forEach { put(JSONObject().put("id", it.id).put("category", it.category).put("topic", it.topic).put("content", it.content)) } }.toString()).apply() }
    fun name() = prefs.getString("name", "Guest") ?: "Guest"
    fun setName(value: String) { prefs.edit().putString("name", value).apply() }
    fun dark() = prefs.getBoolean("dark", false)
    fun setDark(value: Boolean) { prefs.edit().putBoolean("dark", value).apply() }
    fun showNotePreviews() = prefs.getBoolean("showNotePreviews", false)
    fun setShowNotePreviews(value: Boolean) { prefs.edit().putBoolean("showNotePreviews", value).apply() }
    fun hasWelcomed() = prefs.getBoolean("welcomed", false)
    fun setWelcomed() { prefs.edit().putBoolean("welcomed", true).apply() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = NoteStore(this)
        setContent { NoteSpaceApp(store) }
    }
}

private val Ink = Color(0xFF171717)
private val Amber = Color(0xFFF9CD35)

@Composable
private fun NoteSpaceApp(store: NoteStore) {
    var dark by remember { mutableStateOf(store.dark()) }
    var showNotePreviews by remember { mutableStateOf(store.showNotePreviews()) }
    val scheme = if (dark) darkColorScheme(primary = Amber, onPrimary = Ink, secondary = Color(0xFFE9BC28), background = Color(0xFF11110F), surface = Color(0xFF1D1D1A), onSurface = Color(0xFFF5F2E9), onBackground = Color(0xFFF5F2E9)) else lightColorScheme(primary = Ink, onPrimary = Color.White, secondary = Amber, background = Color(0xFFFAF9F5), surface = Color.White, onSurface = Ink, onBackground = Ink)
    MaterialTheme(colorScheme = scheme) {
        val view = LocalView.current
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                window.statusBarColor = scheme.background.toArgb()
                window.navigationBarColor = scheme.background.toArgb()
                WindowInsetsControllerCompat(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
        var categories by remember { mutableStateOf(store.categories()) }
        var notes by remember { mutableStateOf(store.notes()) }
        var name by remember { mutableStateOf(store.name()) }
        var showWelcome by remember { mutableStateOf(!store.hasWelcomed()) }
        var page by remember { mutableStateOf("home") }
        var selected by remember { mutableStateOf<Note?>(null) }
        var editing by remember { mutableStateOf(false) }
        var filter by remember { mutableStateOf("All notes") }
        var createMenu by remember { mutableStateOf(false) }
        var categoryDialog by remember { mutableStateOf(false) }
        var draftCategory by remember { mutableStateOf("") }
        var topic by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var noteCategory by remember { mutableStateOf("") }
        var categoryExpanded by remember { mutableStateOf(false) }

        Scaffold(
            floatingActionButton = { if (page == "home") FloatingActionButton(onClick = { createMenu = true }, containerColor = Amber, contentColor = Ink, shape = CircleShape) { Icon(Icons.Default.Add, contentDescription = "Create") } },
            containerColor = MaterialTheme.colorScheme.background
        ) { insets ->
            Column(Modifier.fillMaxSize().padding(insets)) {
                when (page) {
                    "home" -> {
                        Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 14.dp, top = 22.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("NOTESPACE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f), fontWeight = FontWeight.Bold, letterSpacing = 1.8f.sp)
                                Text("Welcome, $name", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { page = "settings" }) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                        }
                        Text("A little space for every thought.", Modifier.padding(horizontal = 22.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f), style = MaterialTheme.typography.bodyMedium)
                        var expanded by remember { mutableStateOf(false) }
                        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                                Text(filter, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                                Icon(Icons.Default.ExpandMore, contentDescription = "Choose category")
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                (listOf("All notes") + categories).forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { filter = c; expanded = false }) }
                            }
                        }
                        val visible = notes.filter { filter == "All notes" || it.category == filter }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (filter == "All notes") "YOUR NOTES" else filter.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 1.2f.sp, modifier = Modifier.weight(1f))
                            Text("${visible.size} ${if (visible.size == 1) "note" else "notes"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                        }
                        if (visible.isEmpty()) {
                            Column(Modifier.fillMaxSize().padding(bottom = 60.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Box(Modifier.size(72.dp).clip(CircleShape).background(Amber.copy(alpha = .25f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.EditNote, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(34.dp)) }
                                Spacer(Modifier.height(18.dp)); Text(if (categories.isEmpty()) "Start your space" else "Nothing here yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp)); Text(if (categories.isEmpty()) "Create a category, then add your first note." else "Tap + to add a note to this category.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                            }
                        } else LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(visible, key = { it.id }) { note ->
                                Card(onClick = { selected = note; editing = false; page = "detail" }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                                    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(note.topic.ifBlank { "Untitled note" }, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f))
                                        }
                                        if (showNotePreviews) {
                                            Spacer(Modifier.height(7.dp))
                                            Text(note.content.ifBlank { "No content yet" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f), maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 21f.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "detail" -> {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { page = "home" }) { Icon(Icons.Default.ArrowBack, "Back") }
                            Text(if (editing) "Edit note" else "Note", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            if (!editing) IconButton(onClick = { selected?.let { topic = it.topic; content = it.content; editing = true } }) { Icon(Icons.Default.Edit, "Edit note") }
                        }
                        if (editing) {
                            OutlinedTextField(topic, { topic = it }, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), label = { Text("Topic") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                            OutlinedTextField(content, { content = it }, Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp, vertical = 8.dp), label = { Text("Your notes") }, shape = RoundedCornerShape(14.dp), minLines = 8)
                            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { editing = false }, Modifier.weight(1f).height(52.dp)) { Text("Cancel") }
                                Button(onClick = { selected?.let { old -> val updated = old.copy(topic = topic.trim(), content = content); notes = notes.map { if (it.id == old.id) updated else it }; store.saveNotes(notes); selected = updated }; editing = false }, Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Ink)) { Text("Save changes") }
                            }
                        } else selected?.let { note ->
                            Surface(color = Amber.copy(alpha = .24f), shape = RoundedCornerShape(30.dp), modifier = Modifier.padding(start = 22.dp, top = 8.dp)) { Text(note.category, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) }
                            Text(note.topic, Modifier.padding(horizontal = 22.dp, vertical = 16.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            HorizontalDivider(Modifier.padding(horizontal = 22.dp))
                            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp)) { item { Text(note.content.ifBlank { "No content yet." }, style = MaterialTheme.typography.bodyLarge, lineHeight = 27f.sp) } }
                        }
                    }
                    "create" -> {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { page = "home" }) { Icon(Icons.Default.Close, "Close") }; Text("New note", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                        Text("Capture a thought", Modifier.padding(start = 22.dp, top = 12.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Choose where it belongs, then write away.", Modifier.padding(start = 22.dp, top = 5.dp, bottom = 14.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                            OutlinedButton(onClick = { categoryExpanded = true }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) { Text(noteCategory.ifBlank { "Choose a category" }, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface); Icon(Icons.Default.ExpandMore, null) }
                            DropdownMenu(categoryExpanded, { categoryExpanded = false }) { categories.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { noteCategory = c; categoryExpanded = false }) } }
                        }
                        OutlinedTextField(topic, { topic = it }, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), label = { Text("Topic") }, placeholder = { Text("Give this note a title") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                        OutlinedTextField(content, { content = it }, Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp, vertical = 4.dp), label = { Text("Your notes") }, placeholder = { Text("Start writing…") }, shape = RoundedCornerShape(14.dp), minLines = 8)
                        Button(onClick = { if (noteCategory.isNotBlank() && topic.isNotBlank()) { val note = Note(UUID.randomUUID().toString(), noteCategory, topic.trim(), content); notes = listOf(note) + notes; store.saveNotes(notes); filter = noteCategory; topic = ""; content = ""; page = "home" } }, enabled = noteCategory.isNotBlank() && topic.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(20.dp).height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Ink)) { Text("Save note", fontWeight = FontWeight.Bold) }
                    }
                    "settings" -> {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { page = "home" }) { Icon(Icons.Default.ArrowBack, "Back") }; Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                        Text("YOUR PROFILE", Modifier.padding(start = 22.dp, top = 20.dp, bottom = 8.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.1f.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                        OutlinedTextField(name, { name = it; store.setName(it.ifBlank { "Guest" }) }, Modifier.fillMaxWidth().padding(horizontal = 20.dp), label = { Text("Your name") }, supportingText = { Text("Shown in your home greeting") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                        Text("APPEARANCE", Modifier.padding(start = 22.dp, top = 28.dp, bottom = 8.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.1f.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                        Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (dark) Icons.Default.DarkMode else Icons.Default.LightMode, null, modifier = Modifier.padding(end = 14.dp))
                                Column(Modifier.weight(1f)) { Text(if (dark) "Dark theme" else "Light theme", fontWeight = FontWeight.SemiBold); Text("Use ${if (dark) "dark" else "light"} colors", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)) }
                                Switch(dark, { dark = it; store.setDark(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Ink, checkedTrackColor = Amber))
                            }
                        }
                        Text("NOTE TILES", Modifier.padding(start = 22.dp, top = 24.dp, bottom = 8.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.1f.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                        Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Subject, null, modifier = Modifier.padding(end = 14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Show note previews", fontWeight = FontWeight.SemiBold)
                                    Text("Show note text on home tiles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                                }
                                Switch(showNotePreviews, { showNotePreviews = it; store.setShowNotePreviews(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Ink, checkedTrackColor = Amber))
                            }
                        }
                        Text("YOUR SPACE", Modifier.padding(start = 22.dp, top = 28.dp, bottom = 8.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.1f.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                        Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.FolderOpen, null, Modifier.padding(end = 14.dp)); Column { Text("${categories.size} categories", fontWeight = FontWeight.SemiBold); Text("Your notes stay on this device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)) } } }
                    }
                }
            }
        }

        if (showWelcome) AlertDialog(onDismissRequest = {}, icon = { Icon(Icons.Default.WavingHand, null, tint = Amber) }, title = { Text("Welcome to NoteSpace") }, text = { Column { Text("What should we call you?"); Spacer(Modifier.height(12.dp)); OutlinedTextField(name, { name = it }, label = { Text("Your name") }, placeholder = { Text("Guest") }, singleLine = true, shape = RoundedCornerShape(14.dp)) } }, confirmButton = { TextButton(onClick = { name = name.trim().ifBlank { "Guest" }; store.setName(name); store.setWelcomed(); showWelcome = false }) { Text("Let’s go", color = MaterialTheme.colorScheme.primary) } }, shape = RoundedCornerShape(24.dp))

        if (createMenu) AlertDialog(onDismissRequest = { createMenu = false }, title = { Text("Make some space") }, text = { Text("What would you like to create?") }, confirmButton = { TextButton(onClick = { createMenu = false; draftCategory = ""; categoryDialog = true }) { Text("New category") } }, dismissButton = { TextButton(onClick = { createMenu = false; if (categories.isEmpty()) { draftCategory = ""; categoryDialog = true } else { topic = ""; content = ""; noteCategory = categories.first(); page = "create" } }) { Text(if (categories.isEmpty()) "Create category first" else "New note") } }, shape = RoundedCornerShape(24.dp))

        if (categoryDialog) AlertDialog(onDismissRequest = { categoryDialog = false }, title = { Text("New category") }, text = { OutlinedTextField(draftCategory, { draftCategory = it }, label = { Text("Category name") }, placeholder = { Text("e.g. Ideas, Work, Journal") }, singleLine = true, shape = RoundedCornerShape(14.dp)) }, confirmButton = { TextButton(onClick = { val c = draftCategory.trim(); if (c.isNotEmpty() && !categories.contains(c)) { categories = categories + c; store.saveCategories(categories); filter = c }; categoryDialog = false }) { Text("Create") } }, dismissButton = { TextButton(onClick = { categoryDialog = false }) { Text("Cancel") } }, shape = RoundedCornerShape(24.dp))
    }
}
