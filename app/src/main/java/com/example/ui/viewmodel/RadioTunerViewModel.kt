package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlaybackState
import com.example.audio.Media3FmAudioController
import com.example.audio.RadioAudioController
import com.example.data.local.FavoriteStationEntity
import com.example.data.local.FmStationEntity
import com.example.data.local.HardwarePresetEntity
import com.example.data.local.OndaFmDatabase
import com.example.data.local.TunerPreferencesDataSource
import com.example.data.remote.RadioBrowserClient
import com.example.data.repository.FmRadioRepository
import com.example.model.FmFrequency
import com.example.model.FmRegion
import kotlin.math.abs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppDestination {
    TUNER,
    STATIONS,
    MEMORIES
}

sealed interface BandSyncStatus {
    data object Loading : BandSyncStatus
    data object Ready : BandSyncStatus
    data class Error(val reason: String?) : BandSyncStatus
}

data class SignalMatch(
    val station: FmStationEntity?,
    val signalQuality: Float, // 0f..1f
    val signalBars: Int,      // 0..5
    val isExactLock: Boolean
)

data class RadioUiState(
    val destination: AppDestination = AppDestination.TUNER,
    val currentFreqTenths: Int = FmFrequency.DEFAULT_TENTHS,
    val selectedRegion: FmRegion = FmRegion.SPAIN,
    val isPowerOn: Boolean = false,
    val isSeeking: Boolean = false,
    val analogStaticEnabled: Boolean = true,
    val sleepTimerMinutesLeft: Int? = null,
    val bandSyncStatus: BandSyncStatus = BandSyncStatus.Loading,
    val bandStations: List<FmStationEntity> = emptyList(),
    val filteredStations: List<FmStationEntity> = emptyList(),
    val searchQuery: String = "",
    val tunedStation: FmStationEntity? = null,
    val signalBars: Int = 0,
    val isStereoLocked: Boolean = false,
    val playback: AudioPlaybackState = AudioPlaybackState(),
    val favorites: List<FavoriteStationEntity> = emptyList(),
    val favoriteUuids: Set<String> = emptySet(),
    val hardwarePresets: Map<Int, HardwarePresetEntity> = emptyMap(),
    val directOverrideStreamUrl: String? = null,
    val directOverrideStationName: String? = null
) {
    val formattedFrequency: String
        get() = FmFrequency.formatMhz(currentFreqTenths)

    val isCurrentStationFavorite: Boolean
        get() = tunedStation != null && favoriteUuids.contains(tunedStation.stationUuid)
}

private data class InternalRuntimeState(
    val destination: AppDestination = AppDestination.TUNER,
    val currentFreqTenths: Int = FmFrequency.DEFAULT_TENTHS,
    val selectedRegion: FmRegion = FmRegion.SPAIN,
    val isPowerOn: Boolean = false,
    val isSeeking: Boolean = false,
    val analogStaticEnabled: Boolean = true,
    val sleepTimerMinutesLeft: Int? = null,
    val bandSyncStatus: BandSyncStatus = BandSyncStatus.Loading,
    val searchQuery: String = "",
    val directOverrideStreamUrl: String? = null,
    val directOverrideStationName: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class RadioTunerViewModel(
    private val repository: FmRadioRepository,
    private val audioController: RadioAudioController
) : ViewModel() {

    private val runtime = MutableStateFlow(InternalRuntimeState())
    private var seekJob: Job? = null
    private var audioDebounceJob: Job? = null
    private var saveFreqDebounceJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var hasUserTunedManually: Boolean = false

    private val bandStationsFlow = runtime
        .combine(MutableStateFlow(Unit)) { state, _ -> state.selectedRegion }
        .distinctUntilChanged()
        .flatMapLatest { region -> repository.observeBandStations(region) }

    val uiState: StateFlow<RadioUiState> = combine(
        runtime,
        bandStationsFlow,
        audioController.playbackState,
        repository.favoritesFlow,
        repository.hardwarePresetsFlow
    ) { rt, band, playback, favorites, presets ->
        val match = evaluateSignal(rt.currentFreqTenths, band, rt.isPowerOn)
        val query = rt.searchQuery.trim()
        val filtered = if (query.isEmpty()) {
            band
        } else {
            val parsedTenths = FmFrequency.parseUserFrequencyTenths(query)
            band.filter { st ->
                st.name.contains(query, ignoreCase = true) ||
                    st.tags.contains(query, ignoreCase = true) ||
                    st.state.contains(query, ignoreCase = true) ||
                    FmFrequency.formatMhz(st.freqTenths).contains(query) ||
                    (parsedTenths != null && abs(st.freqTenths - parsedTenths) <= 2)
            }
        }
        val favUuids = favorites.mapTo(HashSet()) { it.stationUuid }
        val presetsMap = presets.associateBy { it.slotIndex }

        RadioUiState(
            destination = rt.destination,
            currentFreqTenths = rt.currentFreqTenths,
            selectedRegion = rt.selectedRegion,
            isPowerOn = rt.isPowerOn,
            isSeeking = rt.isSeeking,
            analogStaticEnabled = rt.analogStaticEnabled,
            sleepTimerMinutesLeft = rt.sleepTimerMinutesLeft,
            bandSyncStatus = rt.bandSyncStatus,
            bandStations = band,
            filteredStations = filtered,
            searchQuery = rt.searchQuery,
            tunedStation = match.station,
            signalBars = match.signalBars,
            isStereoLocked = match.isExactLock && rt.isPowerOn,
            playback = playback,
            favorites = favorites,
            favoriteUuids = favUuids,
            hardwarePresets = presetsMap,
            directOverrideStreamUrl = rt.directOverrideStreamUrl,
            directOverrideStationName = rt.directOverrideStationName
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RadioUiState()
    )

    init {
        viewModelScope.launch {
            val initialPrefs = repository.preferencesFlow.first()
            runtime.update {
                it.copy(
                    currentFreqTenths = initialPrefs.currentFreqTenths,
                    selectedRegion = initialPrefs.selectedRegion,
                    analogStaticEnabled = initialPrefs.analogStaticEnabled
                )
            }
            loadRegionBand(initialPrefs.selectedRegion, forceRefresh = false)
        }

        // Synchronize live audio & static balance whenever frequency, band, or power changes
        viewModelScope.launch {
            combine(runtime, bandStationsFlow) { rt, band -> rt to band }
                .collect { (rt, band) ->
                    scheduleAudioSync(rt, band)
                }
        }
    }

    private fun evaluateSignal(
        freqTenths: Int,
        band: List<FmStationEntity>,
        isPowerOn: Boolean
    ): SignalMatch {
        val exact = band.firstOrNull { it.freqTenths == freqTenths }
        if (exact != null) {
            return SignalMatch(
                station = exact,
                signalQuality = 1.0f,
                signalBars = if (isPowerOn) 5 else 4,
                isExactLock = true
            )
        }
        val adjacent = band.firstOrNull { abs(it.freqTenths - freqTenths) == 1 }
        if (adjacent != null) {
            return SignalMatch(
                station = adjacent,
                signalQuality = 0.45f,
                signalBars = if (isPowerOn) 2 else 1,
                isExactLock = false
            )
        }
        return SignalMatch(
            station = null,
            signalQuality = 0f,
            signalBars = if (isPowerOn) 1 else 0,
            isExactLock = false
        )
    }

    private fun scheduleAudioSync(rt: InternalRuntimeState, band: List<FmStationEntity>) {
        audioDebounceJob?.cancel()
        if (!rt.isPowerOn) {
            audioController.tuneAudio(
                streamUrl = null,
                powerOn = false,
                signalQuality = 0f,
                analogStaticEnabled = rt.analogStaticEnabled
            )
            return
        }

        if (rt.isSeeking) {
            // While scanning across frequencies, play brief FM carrier static without connecting HTTP streams
            audioController.tuneAudio(
                streamUrl = null,
                powerOn = true,
                signalQuality = 0f,
                analogStaticEnabled = rt.analogStaticEnabled
            )
            return
        }

        val match = evaluateSignal(rt.currentFreqTenths, band, isPowerOn = true)
        val targetStream = rt.directOverrideStreamUrl ?: match.station?.streamUrl
        val targetQuality = if (rt.directOverrideStreamUrl != null) 1f else match.signalQuality

        audioDebounceJob = viewModelScope.launch {
            if (targetStream != null) {
                // Slight settle delay when rapidly dragging rotary knob
                delay(160)
            }
            audioController.tuneAudio(
                streamUrl = targetStream,
                powerOn = true,
                signalQuality = targetQuality,
                analogStaticEnabled = rt.analogStaticEnabled
            )
        }
    }

    private fun loadRegionBand(region: FmRegion, forceRefresh: Boolean) {
        viewModelScope.launch {
            runtime.update { it.copy(bandSyncStatus = BandSyncStatus.Loading) }
            val result = repository.ensureRegionLoaded(region, forceRefresh = forceRefresh)
            result.onSuccess {
                runtime.update { it.copy(bandSyncStatus = BandSyncStatus.Ready) }
                // Snap to a live station on initial load if current frequency has no carrier
                val loadedBand = repository.observeBandStations(region).first()
                val curFreq = runtime.value.currentFreqTenths
                if (!hasUserTunedManually && loadedBand.isNotEmpty() &&
                    loadedBand.none { abs(it.freqTenths - curFreq) <= 1 }
                ) {
                    val nearest = loadedBand.minByOrNull { abs(it.freqTenths - curFreq) }
                    if (nearest != null) {
                        runtime.update { it.copy(currentFreqTenths = nearest.freqTenths) }
                        repository.saveFrequency(nearest.freqTenths)
                    }
                }
            }.onFailure { err ->
                val cachedCount = repository.observeBandStations(region).first().size
                runtime.update {
                    it.copy(
                        bandSyncStatus = if (cachedCount > 0) {
                            BandSyncStatus.Ready
                        } else {
                            BandSyncStatus.Error(err.localizedMessage)
                        }
                    )
                }
            }
        }
    }

    fun selectDestination(destination: AppDestination) {
        runtime.update { it.copy(destination = destination) }
    }

    fun togglePower() {
        seekJob?.cancel()
        runtime.update { state ->
            state.copy(isPowerOn = !state.isPowerOn, isSeeking = false)
        }
    }

    fun tuneToFrequency(freqTenths: Int, autoPowerOn: Boolean = false) {
        hasUserTunedManually = true
        seekJob?.cancel()
        val clamped = FmFrequency.clamp(freqTenths)
        runtime.update { state ->
            state.copy(
                currentFreqTenths = clamped,
                isSeeking = false,
                isPowerOn = if (autoPowerOn) true else state.isPowerOn,
                directOverrideStreamUrl = null,
                directOverrideStationName = null
            )
        }
        persistFrequencyDebounced(clamped)
    }

    fun stepFrequency(deltaTenths: Int) {
        val next = FmFrequency.clamp(runtime.value.currentFreqTenths + deltaTenths)
        tuneToFrequency(next)
    }

    fun seekNextStation(forward: Boolean) {
        hasUserTunedManually = true
        seekJob?.cancel()
        val band = uiState.value.bandStations
        if (band.isEmpty()) {
            stepFrequency(if (forward) 5 else -5)
            return
        }

        seekJob = viewModelScope.launch {
            val startFreq = runtime.value.currentFreqTenths
            runtime.update {
                it.copy(
                    isPowerOn = true,
                    isSeeking = true,
                    directOverrideStreamUrl = null,
                    directOverrideStationName = null
                )
            }
            val stationFreqs = band.mapTo(HashSet()) { it.freqTenths }
            val step = if (forward) 1 else -1
            var cursor = startFreq
            var stepsTaken = 0
            val maxSteps = FmFrequency.MAX_TENTHS - FmFrequency.MIN_TENTHS + 1

            while (stepsTaken < maxSteps) {
                cursor = FmFrequency.wrap(cursor + step)
                stepsTaken++
                runtime.update { it.copy(currentFreqTenths = cursor) }
                if (stationFreqs.contains(cursor)) {
                    break
                }
                delay(24)
            }

            runtime.update { it.copy(isSeeking = false, currentFreqTenths = cursor) }
            repository.saveFrequency(cursor)
        }
    }

    fun selectRegion(region: FmRegion) {
        if (region == runtime.value.selectedRegion) return
        seekJob?.cancel()
        hasUserTunedManually = false
        runtime.update {
            it.copy(
                selectedRegion = region,
                isSeeking = false,
                directOverrideStreamUrl = null,
                directOverrideStationName = null
            )
        }
        viewModelScope.launch {
            repository.saveRegion(region)
        }
        loadRegionBand(region, forceRefresh = false)
    }

    fun refreshCurrentBand() {
        loadRegionBand(runtime.value.selectedRegion, forceRefresh = true)
    }

    fun retryStream() {
        runtime.update { it.copy(isPowerOn = true) }
        audioController.retryCurrentStream()
    }

    fun toggleAnalogStatic() {
        val next = !runtime.value.analogStaticEnabled
        runtime.update { it.copy(analogStaticEnabled = next) }
        viewModelScope.launch {
            repository.saveAnalogStaticEnabled(next)
        }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            runtime.update { it.copy(sleepTimerMinutesLeft = null) }
            return
        }
        runtime.update { it.copy(sleepTimerMinutesLeft = minutes) }
        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes
            while (remaining > 0) {
                delay(60_000L)
                remaining--
                runtime.update { it.copy(sleepTimerMinutesLeft = remaining.takeIf { m -> m > 0 }) }
            }
            runtime.update {
                it.copy(isPowerOn = false, isSeeking = false, sleepTimerMinutesLeft = null)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        runtime.update { it.copy(searchQuery = query) }
    }

    fun tuneAndPlayStation(station: FmStationEntity, switchToTuner: Boolean = true) {
        hasUserTunedManually = true
        seekJob?.cancel()
        val targetRegion = FmRegion.fromCode(station.regionCode)
        runtime.update {
            it.copy(
                destination = if (switchToTuner) AppDestination.TUNER else it.destination,
                selectedRegion = targetRegion,
                currentFreqTenths = station.freqTenths,
                isPowerOn = true,
                isSeeking = false,
                directOverrideStreamUrl = null,
                directOverrideStationName = null
            )
        }
        viewModelScope.launch {
            repository.saveRegion(targetRegion)
            repository.saveFrequency(station.freqTenths)
        }
    }

    fun playFavoriteStation(favorite: FavoriteStationEntity) {
        hasUserTunedManually = true
        seekJob?.cancel()
        val region = FmRegion.fromCode(favorite.regionCode)
        runtime.update {
            it.copy(
                destination = AppDestination.TUNER,
                selectedRegion = region,
                currentFreqTenths = favorite.freqTenths,
                isPowerOn = true,
                isSeeking = false,
                directOverrideStreamUrl = favorite.streamUrl,
                directOverrideStationName = favorite.name
            )
        }
        viewModelScope.launch {
            repository.saveRegion(region)
            repository.saveFrequency(favorite.freqTenths)
            repository.ensureRegionLoaded(region, forceRefresh = false)
        }
    }

    fun toggleCurrentStationFavorite() {
        val station = uiState.value.tunedStation ?: return
        toggleFavorite(station)
    }

    fun toggleFavorite(station: FmStationEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(station)
        }
    }

    fun removeFavorite(stationUuid: String) {
        viewModelScope.launch {
            repository.removeFavorite(stationUuid)
        }
    }

    fun onHardwarePresetTapped(slotIndex: Int) {
        val existing = uiState.value.hardwarePresets[slotIndex]
        if (existing == null) {
            saveCurrentToPreset(slotIndex)
            return
        }
        hasUserTunedManually = true
        seekJob?.cancel()
        val region = FmRegion.fromCode(existing.regionCode)
        runtime.update {
            it.copy(
                destination = AppDestination.TUNER,
                selectedRegion = region,
                currentFreqTenths = existing.freqTenths,
                isPowerOn = true,
                isSeeking = false,
                directOverrideStreamUrl = existing.streamUrl,
                directOverrideStationName = existing.stationName.takeIf { s -> s.isNotBlank() }
            )
        }
        viewModelScope.launch {
            repository.saveRegion(region)
            repository.saveFrequency(existing.freqTenths)
            repository.ensureRegionLoaded(region, forceRefresh = false)
        }
    }

    fun saveCurrentToPreset(slotIndex: Int) {
        val snapshot = uiState.value
        viewModelScope.launch {
            repository.saveHardwarePreset(
                slotIndex = slotIndex,
                freqTenths = snapshot.currentFreqTenths,
                regionCode = snapshot.selectedRegion.code,
                station = snapshot.tunedStation
            )
        }
    }

    fun clearHardwarePreset(slotIndex: Int) {
        viewModelScope.launch {
            repository.clearHardwarePreset(slotIndex)
        }
    }

    private fun persistFrequencyDebounced(freqTenths: Int) {
        saveFreqDebounceJob?.cancel()
        saveFreqDebounceJob = viewModelScope.launch {
            delay(350)
            repository.saveFrequency(freqTenths)
        }
    }

    override fun onCleared() {
        super.onCleared()
        seekJob?.cancel()
        audioDebounceJob?.cancel()
        sleepTimerJob?.cancel()
        audioController.release()
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = OndaFmDatabase.getInstance(appContext)
                    val repository = FmRadioRepository(
                        fmDao = database.fmDao(),
                        apiClient = RadioBrowserClient(),
                        preferencesDataSource = TunerPreferencesDataSource(appContext)
                    )
                    val audioController = Media3FmAudioController(appContext)
                    return RadioTunerViewModel(repository, audioController) as T
                }
            }
        }
    }
}
