package com.weathersnap.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.weathersnap.ui.components.PrimaryPillButton
import com.weathersnap.ui.components.SecondaryPillButton
import com.weathersnap.ui.components.SurfaceCard
import com.weathersnap.ui.components.WeatherCardContent
import java.io.File

@Composable
fun CreateReportScreen(
    draftId: String,
    incomingSnapshotJson: String,
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CreateReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(draftId, incomingSnapshotJson) {
        viewModel.init(draftId, incomingSnapshotJson)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CreateReportEvent.Saved -> onSaved()
                is CreateReportEvent.Error -> snackbar.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeaderCard(onBack = onBack)
            state.snapshot?.let { snap ->
                SurfaceCard { WeatherCardContent(snap) }
            }
            PhotoCard(
                imagePath = state.imagePath,
                isProcessing = state.isProcessingPhoto,
                originalBytes = state.originalImageBytes,
                compressedBytes = state.compressedImageBytes,
                onCapture = onOpenCamera,
            )
            NotesCard(
                notes = state.notes,
                onNotesChange = viewModel::onNotesChange,
            )
            PrimaryPillButton(
                text = if (state.isSaving) "Saving…" else "Save Report",
                enabled = !state.isSaving && !state.isProcessingPhoto && state.imagePath != null && state.snapshot != null,
                onClick = viewModel::onSave,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HeaderCard(onBack: () -> Unit) {
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Create Report",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Capture, compress, annotate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SecondaryPillButton(text = "Back", onClick = onBack)
        }
    }
}

@Composable
private fun PhotoCard(
    imagePath: String?,
    isProcessing: Boolean,
    originalBytes: Long,
    compressedBytes: Long,
    onCapture: () -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    SurfaceCard(padding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isProcessing -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Compressing…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    imagePath != null -> AsyncImage(
                        model = ImageRequest.Builder(ctx)
                            .data(File(imagePath))
                            .build(),
                        contentDescription = "Captured photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    else -> Text(
                        "Photo preview",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (imagePath != null && originalBytes > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.weathersnap.ui.components.MetricChip(
                        label = "Original",
                        value = formatBytes(originalBytes),
                        modifier = Modifier.weight(1f),
                    )
                    com.weathersnap.ui.components.MetricChip(
                        label = "Compressed",
                        value = formatBytes(compressedBytes),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            PrimaryPillButton(text = "Capture Photo", onClick = onCapture)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val kb = bytes / 1024.0
    return if (kb < 1024) String.format(java.util.Locale.getDefault(), "%.1f KB", kb)
    else String.format(java.util.Locale.getDefault(), "%.2f MB", kb / 1024.0)
}

@Composable
private fun NotesCard(notes: String, onNotesChange: (String) -> Unit) {
    SurfaceCard(padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Field Notes",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                placeholder = { Text("Notes") },
                modifier = Modifier.fillMaxWidth().height(110.dp),
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
                shape = RoundedCornerShape(12.dp),
            )
        }
    }
}
