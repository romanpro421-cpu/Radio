package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "fm_stations",
    primaryKeys = ["regionCode", "freqTenths"]
)
data class FmStationEntity(
    val regionCode: String,
    val freqTenths: Int,
    val stationUuid: String,
    val name: String,
    val streamUrl: String,
    val faviconUrl: String,
    val tags: String,
    val country: String,
    val state: String,
    val codec: String,
    val bitrate: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_stations")
data class FavoriteStationEntity(
    @PrimaryKey val stationUuid: String,
    val freqTenths: Int,
    val regionCode: String,
    val name: String,
    val streamUrl: String,
    val faviconUrl: String,
    val tags: String,
    val codec: String,
    val bitrate: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "hardware_presets")
data class HardwarePresetEntity(
    @PrimaryKey val slotIndex: Int, // 1..6
    val freqTenths: Int,
    val regionCode: String,
    val stationUuid: String?,
    val stationName: String,
    val streamUrl: String?,
    val faviconUrl: String?
)
