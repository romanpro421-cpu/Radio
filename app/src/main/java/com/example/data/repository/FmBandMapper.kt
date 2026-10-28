package com.example.data.repository

import com.example.data.local.FmStationEntity
import com.example.data.remote.RadioStationDto
import com.example.model.FmFrequency
import kotlin.math.abs

object FmBandMapper {
    private const val MAX_STATIONS_ON_BAND = 52

    fun mapToBand(regionCode: String, rawStations: List<RadioStationDto>): List<FmStationEntity> {
        val valid = rawStations
            .filter { dto ->
                val url = dto.playableUrl
                dto.stationUuid.isNotBlank() &&
                    dto.name.isNotBlank() &&
                    (url.startsWith("https://", ignoreCase = true) ||
                        url.startsWith("http://", ignoreCase = true))
            }
            .distinctBy { it.playableUrl.lowercase() }
            .distinctBy { cleanStationName(it.name).lowercase() }

        val occupied = HashSet<Int>()
        val result = ArrayList<FmStationEntity>()

        fun isWellSpaced(candidate: Int, minGap: Int): Boolean {
            if (candidate !in FmFrequency.MIN_TENTHS..FmFrequency.MAX_TENTHS) return false
            for (used in occupied) {
                if (abs(used - candidate) < minGap) return false
            }
            return true
        }

        // Pass 1: Place stations that advertise a real terrestrial FM frequency in name or tags
        val remaining = ArrayList<RadioStationDto>()
        for (dto in valid) {
            if (result.size >= MAX_STATIONS_ON_BAND) break
            val extracted = FmFrequency.extractFromText(dto.name)
                ?: FmFrequency.extractFromText(dto.tags)
            if (extracted != null) {
                val chosen = when {
                    isWellSpaced(extracted, minGap = 2) -> extracted
                    !occupied.contains(extracted) -> extracted
                    else -> findNearestOpenSlot(extracted, occupied, minGap = 2)
                }
                if (chosen != null) {
                    occupied.add(chosen)
                    result.add(dto.toEntity(regionCode, chosen))
                } else {
                    remaining.add(dto)
                }
            } else {
                remaining.add(dto)
            }
        }

        // Pass 2: Deterministically allocate remaining stations across 87.5..108.0 MHz with 0.3-0.4 MHz carrier spacing
        val candidateSlots = (FmFrequency.MIN_TENTHS..FmFrequency.MAX_TENTHS).filter { it % 2 == 1 }
        for (dto in remaining) {
            if (result.size >= MAX_STATIONS_ON_BAND) break
            if (candidateSlots.isEmpty()) break
            val seedIndex = (dto.stationUuid.hashCode() and 0x7fffffff) % candidateSlots.size
            var placedSlot: Int? = null
            for (offset in candidateSlots.indices) {
                val slot = candidateSlots[(seedIndex + offset) % candidateSlots.size]
                if (isWellSpaced(slot, minGap = 3)) {
                    placedSlot = slot
                    break
                }
            }
            if (placedSlot == null) {
                for (offset in candidateSlots.indices) {
                    val slot = candidateSlots[(seedIndex + offset) % candidateSlots.size]
                    if (isWellSpaced(slot, minGap = 2)) {
                        placedSlot = slot
                        break
                    }
                }
            }
            if (placedSlot != null) {
                occupied.add(placedSlot)
                result.add(dto.toEntity(regionCode, placedSlot))
            }
        }

        return result.sortedBy { it.freqTenths }
    }

    private fun findNearestOpenSlot(target: Int, occupied: Set<Int>, minGap: Int): Int? {
        for (delta in 1..40) {
            val higher = target + delta
            if (higher <= FmFrequency.MAX_TENTHS && occupied.none { abs(it - higher) < minGap }) {
                return higher
            }
            val lower = target - delta
            if (lower >= FmFrequency.MIN_TENTHS && occupied.none { abs(it - lower) < minGap }) {
                return lower
            }
        }
        return null
    }

    private fun cleanStationName(raw: String): String {
        return raw.trim()
            .replace(Regex("\\s+"), " ")
            .removePrefix("-")
            .removePrefix("_")
            .trim()
    }

    private fun RadioStationDto.toEntity(regionCode: String, freqTenths: Int): FmStationEntity {
        return FmStationEntity(
            regionCode = regionCode,
            freqTenths = freqTenths,
            stationUuid = stationUuid,
            name = cleanStationName(name),
            streamUrl = playableUrl,
            faviconUrl = favicon.trim(),
            tags = tags.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() && it.length <= 22 }
                .take(4)
                .joinToString(" · "),
            country = country.trim(),
            state = state.trim(),
            codec = codec.trim().uppercase().ifEmpty { "FM" },
            bitrate = bitrate.coerceAtLeast(0)
        )
    }
}
