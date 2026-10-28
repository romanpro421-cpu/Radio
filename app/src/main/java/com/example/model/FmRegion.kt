package com.example.model

import androidx.annotation.StringRes
import com.example.R

enum class FmRegion(
    val code: String,
    val apiCountryCode: String?,
    val badge: String,
    @StringRes val labelRes: Int
) {
    SPAIN("ES", "ES", "ES", R.string.region_es),
    MEXICO("MX", "MX", "MX", R.string.region_mx),
    ARGENTINA("AR", "AR", "AR", R.string.region_ar),
    COLOMBIA("CO", "CO", "CO", R.string.region_co),
    CHILE("CL", "CL", "CL", R.string.region_cl),
    PERU("PE", "PE", "PE", R.string.region_pe),
    USA("US", "US", "US", R.string.region_us),
    GLOBAL("ALL", null, "FM", R.string.region_all);

    companion object {
        fun fromCode(code: String?): FmRegion {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SPAIN
        }
    }
}
