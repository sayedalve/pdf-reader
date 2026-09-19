package com.aryan.reader.pdf.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitPdfScreen(
    viewModel: SplitPdfViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var rangeText by remember { mutableStateOf("") }
    
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onSelectFile(context, it) }
    }
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Split PDF") }) },
        floatingActionButton = {
            if (uiState.sourceUri == null) {
                FloatingActionButton(onClick = { filePicker.launch(arrayOf("application/pdf")) }) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Select PDF")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (uiState.isSplitting) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (uiState.resultMessage != null) {
                Text(uiState.resultMessage!!, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
            } else if (uiState.errorMessage != null) {
                Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
            }
            
            if (uiState.sourceUri == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a PDF to split")
                }
            } else {
                Text("Selected: {uiState.sourceFileName}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 24.dp))
                
                Button(
                    onClick = { viewModel.splitAllPages(context) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                ) {
                    Text("Split into 1-page PDFs")
                }
                
                Text("Or extract specific pages:", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = rangeText,
                    onValueChange = { rangeText = it },
                    label = { Text("e.g. 1-5, 8, 11-13") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                Button(
                    onClick = { viewModel.extractRange(context, rangeText) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = rangeText.isNotBlank()
                ) {
                    Text("Extract Pages")
                }
            }
        }
    }
}
