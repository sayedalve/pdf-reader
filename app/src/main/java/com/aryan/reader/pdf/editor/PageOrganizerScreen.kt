package com.aryan.reader.pdf.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageOrganizerScreen(
    viewModel: PageOrganizerViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onSelectFile(context, it) }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Page Organizer") },
                actions = {
                    if (uiState.sourceUri != null) {
                        IconButton(onClick = { viewModel.save(context, "Organized_" + uiState.sourceFileName) }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.sourceUri == null) {
                FloatingActionButton(onClick = { filePicker.launch(arrayOf("application/pdf")) }) {
                    Icon(Icons.Default.Add, contentDescription = "Select PDF")
                }
            } else if (uiState.selectedIds.isNotEmpty()) {
                FloatingActionButton(onClick = { viewModel.extractSelected(context) }) {
                    Icon(Icons.Default.Save, contentDescription = "Extract Selected")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
            } else if (uiState.resultMessage != null) {
                Text(uiState.resultMessage!!, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
            } else if (uiState.errorMessage != null) {
                Text(uiState.errorMessage!!, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
            }
            
            if (uiState.pages.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { viewModel.selectAll() }) { Text("Select All") }
                    TextButton(onClick = { viewModel.clearSelection() }) { Text("Clear") }
                    IconButton(onClick = { viewModel.deleteSelected() }) { Icon(Icons.Default.Delete, contentDescription = "Delete Selected") }
                    IconButton(onClick = { viewModel.rotateSelected() }) { Icon(Icons.Default.Refresh, contentDescription = "Rotate Selected") }
                }
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(uiState.pages, key = { it.originalIndex }) { page ->
                        val isSelected = uiState.selectedIds.contains(page.originalIndex)
                        Card(
                            modifier = Modifier
                                .padding(4.dp)
                                .clickable { viewModel.toggleSelect(page.originalIndex) }
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (page.thumbnail != null) {
                                    Image(
                                        bitmap = page.thumbnail.asImageBitmap(),
                                        contentDescription = "Page {page.originalIndex + 1}",
                                        modifier = Modifier.height(150.dp).fillMaxWidth().padding(4.dp)
                                    )
                                }
                                Text("Page {page.originalIndex + 1}", style = MaterialTheme.typography.bodySmall)
                                Row {
                                    IconButton(onClick = { viewModel.rotatePage(page.originalIndex) }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Rotate")
                                    }
                                    IconButton(onClick = { viewModel.deletePage(uiState.pages.indexOf(page)) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (uiState.sourceUri == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a PDF to organize")
                }
            }
        }
    }
}
