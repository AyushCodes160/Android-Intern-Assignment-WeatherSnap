package com.weathersnap.ui.weather

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weathersnap.data.repository.WeatherRepository
import com.weathersnap.domain.model.City
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(
        WeatherUiState(query = savedStateHandle.get<String>(KEY_QUERY).orEmpty())
    )
    val state = _state.asStateFlow()

    private val queryFlow = MutableStateFlow(_state.value.query)
    private var suggestionsJob: Job? = null

    init {
        queryFlow
            .debounce(300L)
            .distinctUntilChanged()
            .filter { it.trim().length > 2 }
            .onEach { runSuggestions(it) }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)
        if (query.trim().length <= 2) {
            suggestionsJob?.cancel()
            _state.value = _state.value.copy(suggestions = SuggestionsState.Hidden)
        } else {
            _state.value = _state.value.copy(suggestions = SuggestionsState.Loading)
        }
        queryFlow.value = query
    }

    fun onSearchSubmit() {
        val q = _state.value.query
        if (q.trim().length > 2) runSuggestions(q, force = true)
    }

    fun selectCity(city: City) {
        _state.value = _state.value.copy(
            query = city.displayLine,
            suggestions = SuggestionsState.Hidden,
            card = WeatherCardState.Loading,
        )
        viewModelScope.launch {
            runCatching { repository.fetchCurrentWeather(city) }
                .onSuccess { _state.value = _state.value.copy(card = WeatherCardState.Success(it)) }
                .onFailure {
                    _state.value = _state.value.copy(
                        card = WeatherCardState.Error(it.message ?: "Unable to load weather")
                    )
                }
        }
    }

    fun retryWeather(snapshotCity: City) = selectCity(snapshotCity)

    private fun runSuggestions(query: String, force: Boolean = false) {
        suggestionsJob?.cancel()
        suggestionsJob = viewModelScope.launch {
            if (!force) _state.value = _state.value.copy(suggestions = SuggestionsState.Loading)
            runCatching { repository.searchCities(query) }
                .onSuccess { results ->
                    _state.value = _state.value.copy(
                        suggestions = if (results.isEmpty()) SuggestionsState.Empty
                        else SuggestionsState.Success(results)
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        suggestions = SuggestionsState.Error(it.message ?: "Search failed")
                    )
                }
        }
    }

    companion object { private const val KEY_QUERY = "weather_query" }
}
