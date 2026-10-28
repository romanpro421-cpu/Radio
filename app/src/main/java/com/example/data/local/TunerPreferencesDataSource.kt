package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.FmFrequency
import com.example.model.FmRegion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tunerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "onda_fm_tuner_prefs"
)

data class TunerPreferences(
    val currentFreqTenths: Int = FmFrequency.DEFAULT_TENTHS,
    val selectedRegion: FmRegion = FmRegion.SPAIN,
    val analogStaticEnabled: Boolean = true
)

class TunerPreferencesDataSource(private val context: Context) {
    private object Keys {
        val FREQ_TENTHS = intPreferencesKey("current_freq_tenths")
        val REGION_CODE = stringPreferencesKey("selected_region_code")
        val ANALOG_STATIC = booleanPreferencesKey("analog_static_enabled")
    }

    val preferencesFlow: Flow<TunerPreferences> = context.tunerDataStore.data.map { prefs ->
        TunerPreferences(
            currentFreqTenths = FmFrequency.clamp(
                prefs[Keys.FREQ_TENTHS] ?: FmFrequency.DEFAULT_TENTHS
            ),
            selectedRegion = FmRegion.fromCode(prefs[Keys.REGION_CODE]),
            analogStaticEnabled = prefs[Keys.ANALOG_STATIC] ?: true
        )
    }

    suspend fun saveFrequency(freqTenths: Int) {
        context.tunerDataStore.edit { prefs ->
            prefs[Keys.FREQ_TENTHS] = FmFrequency.clamp(freqTenths)
        }
    }

    suspend fun saveRegion(region: FmRegion) {
        context.tunerDataStore.edit { prefs ->
            prefs[Keys.REGION_CODE] = region.code
        }
    }

    suspend fun saveAnalogStaticEnabled(enabled: Boolean) {
        context.tunerDataStore.edit { prefs ->
            prefs[Keys.ANALOG_STATIC] = enabled
        }
    }
}
