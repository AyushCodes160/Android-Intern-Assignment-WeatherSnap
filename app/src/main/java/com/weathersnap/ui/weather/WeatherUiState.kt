package com.weathersnap.ui.weather

import com.weathersnap.domain.model.City
import com.weathersnap.domain.model.WeatherSnapshot

sealed interface WeatherCardState {
    data object Idle : WeatherCardState
    data object Loading : WeatherCardState
    data class Success(val snapshot: WeatherSnapshot) : WeatherCardState
    data class Error(val message: String) : WeatherCardState
}

sealed interface SuggestionsState {
    data object Hidden : SuggestionsState
    data object Loading : SuggestionsState
    data class Success(val cities: List<City>) : SuggestionsState
    data object Empty : SuggestionsState
    data class Error(val message: String) : SuggestionsState
}

data class WeatherUiState(
    val query: String = "",
    val suggestions: SuggestionsState = SuggestionsState.Hidden,
    val card: WeatherCardState = WeatherCardState.Idle,
)
