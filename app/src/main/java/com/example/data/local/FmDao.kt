package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FmDao {
    @Query("SELECT * FROM fm_stations WHERE regionCode = :regionCode ORDER BY freqTenths ASC")
    fun observeBandStations(regionCode: String): Flow<List<FmStationEntity>>

    @Query("SELECT COUNT(*) FROM fm_stations WHERE regionCode = :regionCode")
    suspend fun countStationsForRegion(regionCode: String): Int

    @Query("DELETE FROM fm_stations WHERE regionCode = :regionCode")
    suspend fun clearRegionStations(regionCode: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBandStations(stations: List<FmStationEntity>)

    @Transaction
    suspend fun replaceRegionBand(regionCode: String, stations: List<FmStationEntity>) {
        clearRegionStations(regionCode)
        insertBandStations(stations)
    }

    @Query("SELECT * FROM favorite_stations ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteStationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorite(favorite: FavoriteStationEntity)

    @Query("DELETE FROM favorite_stations WHERE stationUuid = :stationUuid")
    suspend fun deleteFavorite(stationUuid: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_stations WHERE stationUuid = :stationUuid)")
    suspend fun isFavorite(stationUuid: String): Boolean

    @Query("SELECT * FROM hardware_presets ORDER BY slotIndex ASC")
    fun observeHardwarePresets(): Flow<List<HardwarePresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHardwarePreset(preset: HardwarePresetEntity)

    @Query("DELETE FROM hardware_presets WHERE slotIndex = :slotIndex")
    suspend fun clearHardwarePreset(slotIndex: Int)
}
