package com.aryan.reader.pdf.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergePdfScreen(
    viewModel: MergePdfViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addFiles(context, uris)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Merge PDFs") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { filePicker.launch(arrayOf("application/pdf")) }) {
                Icon(Icons.Default.Add, contentDescription = "Add PDFs")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (uiState.isMerging) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (uiState.resultMessage != null) {
                Text(uiState.resultMessage!!, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
            } else if (uiState.errorMessage != null) {
                Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
            }
            
            if (uiState.selectedFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select multiple PDFs to merge")
                }
            } else {
                Button(
                    onClick = { viewModel.onMerge(context) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    enabled = uiState.selectedFiles.size >= 2
                ) {
                    Text("Merge {uiState.selectedFiles.size} PDFs")
                }
                
                LazyColumn {
                    itemsIndexed(uiState.selectedFiles) { index, fileName ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(fileName, modifier = Modifier.weight(1f))
                                Row {
                                    IconButton(
                                        onClick = { viewModel.moveFile(index, index - 1) },
                                        enabled = index > 0
                                    ) { Icon(Icons.Default.ArrowUpward, "Move Up") }
                                    IconButton(
                                        onClick = { viewModel.moveFile(index, index + 1) },
                                        enabled = index < uiState.selectedFiles.size - 1
                                    ) { Icon(Icons.Default.ArrowDownward, "Move Down") }
                                    IconButton(onClick = { viewModel.onRemoveFile(index) }) {
                                        Icon(Icons.Default.Delete, "Remove")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
