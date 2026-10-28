package com.example.data.repository

import com.example.data.local.FavoriteStationEntity
import com.example.data.local.FmDao
import com.example.data.local.FmStationEntity
import com.example.data.local.HardwarePresetEntity
import com.example.data.local.TunerPreferences
import com.example.data.local.TunerPreferencesDataSource
import com.example.data.remote.RadioBrowserClient
import com.example.model.FmRegion
import kotlinx.coroutines.flow.Flow

class FmRadioRepository(
    private val fmDao: FmDao,
    private val apiClient: RadioBrowserClient,
    private val preferencesDataSource: TunerPreferencesDataSource
) {
    val preferencesFlow: Flow<TunerPreferences> = preferencesDataSource.preferencesFlow
    val favoritesFlow: Flow<List<FavoriteStationEntity>> = fmDao.observeFavorites()
    val hardwarePresetsFlow: Flow<List<HardwarePresetEntity>> = fmDao.observeHardwarePresets()

    fun observeBandStations(region: FmRegion): Flow<List<FmStationEntity>> {
        return fmDao.observeBandStations(region.code)
    }

    suspend fun ensureRegionLoaded(region: FmRegion, forceRefresh: Boolean = false): Result<Int> {
        val existingCount = fmDao.countStationsForRegion(region.code)
        if (!forceRefresh && existingCount > 0) {
            return Result.success(existingCount)
        }
        return refreshRegionBand(region)
    }

    suspend fun refreshRegionBand(region: FmRegion): Result<Int> {
        return apiClient.fetchBandStations(region.apiCountryCode).mapCatching { rawStations ->
            val mapped = FmBandMapper.mapToBand(region.code, rawStations)
            if (mapped.isNotEmpty()) {
                fmDao.replaceRegionBand(region.code, mapped)
            }
            mapped.size
        }
    }

    suspend fun saveFrequency(freqTenths: Int) {
        preferencesDataSource.saveFrequency(freqTenths)
    }

    suspend fun saveRegion(region: FmRegion) {
        preferencesDataSource.saveRegion(region)
    }

    suspend fun saveAnalogStaticEnabled(enabled: Boolean) {
        preferencesDataSource.saveAnalogStaticEnabled(enabled)
    }

    suspend fun toggleFavorite(station: FmStationEntity) {
        if (fmDao.isFavorite(station.stationUuid)) {
            fmDao.deleteFavorite(station.stationUuid)
        } else {
            fmDao.upsertFavorite(
                FavoriteStationEntity(
                    stationUuid = station.stationUuid,
                    freqTenths = station.freqTenths,
                    regionCode = station.regionCode,
                    name = station.name,
                    streamUrl = station.streamUrl,
                    faviconUrl = station.faviconUrl,
                    tags = station.tags,
                    codec = station.codec,
                    bitrate = station.bitrate
                )
            )
        }
    }

    suspend fun removeFavorite(stationUuid: String) {
        fmDao.deleteFavorite(stationUuid)
    }

    suspend fun saveHardwarePreset(
        slotIndex: Int,
        freqTenths: Int,
        regionCode: String,
        station: FmStationEntity?
    ) {
        fmDao.upsertHardwarePreset(
            HardwarePresetEntity(
                slotIndex = slotIndex.coerceIn(1, 6),
                freqTenths = freqTenths,
                regionCode = regionCode,
                stationUuid = station?.stationUuid,
                stationName = station?.name ?: "",
                streamUrl = station?.streamUrl,
                faviconUrl = station?.faviconUrl
            )
        )
    }

    suspend fun clearHardwarePreset(slotIndex: Int) {
        fmDao.clearHardwarePreset(slotIndex)
    }
}
