package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey val currencyCode: String,
    val rateFromUsd: Double?,
    val lastUpdatedUtc: String?,
    val fetchedAtMillis: Long?,
    val manualRateFromUsd: Double? = null,
    val isManualOverride: Boolean = false
)
