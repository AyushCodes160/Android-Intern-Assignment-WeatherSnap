package com.weathersnap.ui.weather

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.weathersnap.domain.model.City
import com.weathersnap.domain.model.WeatherSnapshot
import com.weathersnap.ui.components.PrimaryPillButton
import com.weathersnap.ui.components.SecondaryPillButton
import com.weathersnap.ui.components.SurfaceCard
import com.weathersnap.ui.components.WeatherCardContent

@Composable
fun WeatherScreen(
    onOpenCreateReport: (WeatherSnapshot) -> Unit,
    onOpenSavedReports: () -> Unit,
    viewModel: WeatherViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeaderCard(onOpenSavedReports = onOpenSavedReports)

            SearchCard(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                onSubmit = {
                    viewModel.onSearchSubmit()
                    keyboard?.hide()
                },
            )

            AnimatedVisibility(
                visible = state.suggestions !is SuggestionsState.Hidden,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                SuggestionsBlock(
                    state = state.suggestions,
                    onSelect = { city ->
                        viewModel.selectCity(city)
                        keyboard?.hide()
                    },
                )
            }

            AnimatedContent(
                targetState = state.card,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 4 }).togetherWith(fadeOut())
                },
                label = "weather-card",
            ) { card ->
                when (card) {
                    WeatherCardState.Idle -> Spacer(Modifier.height(0.dp))
                    WeatherCardState.Loading -> LoadingCard()
                    is WeatherCardState.Error -> ErrorCard(card.message)
                    is WeatherCardState.Success -> SuccessBlock(
                        snapshot = card.snapshot,
                        onCreateReport = { onOpenCreateReport(card.snapshot) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(onOpenSavedReports: () -> Unit) {
    SurfaceCard(
        background = MaterialTheme.colorScheme.surfaceVariant,
        padding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "WeatherSnap",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Live weather reports with camera evidence",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SecondaryPillButton(text = "Reports", onClick = onOpenSavedReports)
        }
    }
}

@Composable
private fun SearchCard(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    SurfaceCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("City") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.size(8.dp))
                SecondaryPillButton(text = "Search", onClick = onSubmit)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Enter more than 2 letters to start city suggestions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SuggestionsBlock(state: SuggestionsState, onSelect: (City) -> Unit) {
    SurfaceCard(padding = PaddingValues(8.dp)) {
        when (state) {
            SuggestionsState.Hidden -> Unit
            SuggestionsState.Loading -> Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(10.dp))
                Text("Searching cities…", style = MaterialTheme.typography.bodySmall)
            }
            SuggestionsState.Empty -> Text(
                "No matches",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            is SuggestionsState.Error -> Text(
                state.message,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            is SuggestionsState.Success -> Column {
                state.cities.forEachIndexed { index, city ->
                    SuggestionRow(city = city, onClick = { onSelect(city) })
                    if (index < state.cities.lastIndex) HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(city: City, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(10.dp))
        Column {
            Text(
                city.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                listOfNotNull(city.admin1, city.country).joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoadingCard() {
    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(12.dp))
            Text("Loading weather…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    SurfaceCard(
        background = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = MaterialTheme.colorScheme.error,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun SuccessBlock(snapshot: WeatherSnapshot, onCreateReport: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SurfaceCard { WeatherCardContent(snapshot) }
        SurfaceCard(padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Report readiness",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        "Camera and Room DB enabled",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        PrimaryPillButton(text = "Create Report", onClick = onCreateReport)
    }
}
