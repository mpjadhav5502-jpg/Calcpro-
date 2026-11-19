package com.example.data.repository

import com.example.data.local.CurrencyRateDao
import com.example.data.local.CurrencyRateEntity
import com.example.data.remote.CurrencyApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class SupportedCurrency(
    val code: String,
    val name: String,
    val symbol: String,
    val flagEmoji: String
)

data class CurrencySnapshot(
    val ratesByCode: Map<String, CurrencyRateEntity>,
    val lastUpdatedUtc: String?,
    val fetchedAtMillis: Long?,
    val isOutdated: Boolean,
    val hasAnyRate: Boolean
)

class CurrencyRepository(
    private val currencyRateDao: CurrencyRateDao,
    private val apiService: CurrencyApiService
) {
    companion object {
        const val OUTDATED_THRESHOLD_MS = 24L * 60L * 60L * 1000L // 24 hours
        private val MC = MathContext(34, RoundingMode.HALF_UP)

        val supportedCurrencies = listOf(
            SupportedCurrency("INR", "Indian Rupee", "₹", "🇮🇳"),
            SupportedCurrency("USD", "US Dollar", "$", "🇺🇸"),
            SupportedCurrency("EUR", "Euro", "€", "🇪🇺"),
            SupportedCurrency("GBP", "British Pound", "£", "🇬🇧"),
            SupportedCurrency("AED", "UAE Dirham", "د.إ", "🇦🇪"),
            SupportedCurrency("JPY", "Japanese Yen", "¥", "🇯🇵"),
            SupportedCurrency("AUD", "Australian Dollar", "A$", "🇦🇺"),
            SupportedCurrency("CAD", "Canadian Dollar", "C$", "🇨🇦"),
            SupportedCurrency("CHF", "Swiss Franc", "CHF", "🇨🇭"),
            SupportedCurrency("CNY", "Chinese Yuan", "¥", "🇨🇳")
        )
    }

    fun observeSnapshot(): Flow<CurrencySnapshot> {
        return currencyRateDao.getAllRates().map { entities ->
            val map = entities.associateBy { it.currencyCode }
            val latestFetched = entities.mapNotNull { it.fetchedAtMillis }.maxOrNull()
            val latestUtc = entities.firstOrNull { !it.lastUpdatedUtc.isNullOrBlank() }?.lastUpdatedUtc
            val now = System.currentTimeMillis()
            val outdated = latestFetched == null || (now - latestFetched) > OUTDATED_THRESHOLD_MS
            val hasRate = entities.any { it.rateFromUsd != null || (it.isManualOverride && it.manualRateFromUsd != null) }
            CurrencySnapshot(
                ratesByCode = map,
                lastUpdatedUtc = latestUtc,
                fetchedAtMillis = latestFetched,
                isOutdated = outdated,
                hasAnyRate = hasRate
            )
        }
    }

    suspend fun fetchLiveRates(): Result<Unit> {
        return try {
            val response = apiService.getLatestUsdRates()
            if (!response.result.equals("success", ignoreCase = true) || response.rates.isEmpty()) {
                return Result.failure(IllegalStateException("Unexpected API response"))
            }
            val now = System.currentTimeMillis()
            val existingByCode = currencyRateDao.getAllRatesOnce().associateBy { it.currencyCode }

            val updatedEntities = supportedCurrencies.mapNotNull { currency ->
                val liveRate = if (currency.code == "USD") 1.0 else response.rates[currency.code]
                liveRate?.let { rate ->
                    val existing = existingByCode[currency.code]
                    CurrencyRateEntity(
                        currencyCode = currency.code,
                        rateFromUsd = rate,
                        lastUpdatedUtc = response.timeLastUpdateUtc ?: "",
                        fetchedAtMillis = now,
                        manualRateFromUsd = existing?.manualRateFromUsd,
                        isManualOverride = existing?.isManualOverride ?: false
                    )
                }
            }
            currencyRateDao.upsertAll(updatedEntities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Allows the user to enter a manual conversion rate for 1 [fromCode] = [pairRate] [toCode].
     * Never invents live rates; persists explicitly entered manual override in Room.
     */
    suspend fun setManualPairRate(fromCode: String, toCode: String, pairRate: Double) {
        if (pairRate <= 0.0 || fromCode == toCode) return
        val existingMap = currencyRateDao.getAllRatesOnce().associateBy { it.currencyCode }

        if (fromCode == "USD") {
            val current = existingMap[toCode]
            currencyRateDao.upsert(
                (current ?: CurrencyRateEntity(toCode, null, null, null)).copy(
                    manualRateFromUsd = pairRate,
                    isManualOverride = true
                )
            )
        } else if (toCode == "USD") {
            val current = existingMap[fromCode]
            currencyRateDao.upsert(
                (current ?: CurrencyRateEntity(fromCode, null, null, null)).copy(
                    manualRateFromUsd = 1.0 / pairRate,
                    isManualOverride = true
                )
            )
        } else {
            val fromEntity = existingMap[fromCode]
            val fromUsd = effectiveUsdRate(fromCode, fromEntity) ?: 1.0
            if (fromEntity == null || (fromEntity.rateFromUsd == null && !fromEntity.isManualOverride)) {
                currencyRateDao.upsert(
                    CurrencyRateEntity(
                        currencyCode = fromCode,
                        rateFromUsd = fromEntity?.rateFromUsd,
                        lastUpdatedUtc = fromEntity?.lastUpdatedUtc,
                        fetchedAtMillis = fromEntity?.fetchedAtMillis,
                        manualRateFromUsd = 1.0,
                        isManualOverride = true
                    )
                )
            }
            val targetUsdRate = fromUsd * pairRate
            val toEntity = existingMap[toCode]
            currencyRateDao.upsert(
                (toEntity ?: CurrencyRateEntity(toCode, null, null, null)).copy(
                    manualRateFromUsd = targetUsdRate,
                    isManualOverride = true
                )
            )
        }
    }

    suspend fun clearAllManualOverrides() {
        currencyRateDao.clearAllManualOverrides()
    }

    fun convert(
        amount: BigDecimal,
        fromCode: String,
        toCode: String,
        ratesByCode: Map<String, CurrencyRateEntity>
    ): BigDecimal? {
        if (fromCode == toCode) return amount
        val fromRate = effectiveUsdRate(fromCode, ratesByCode[fromCode]) ?: return null
        val toRate = effectiveUsdRate(toCode, ratesByCode[toCode]) ?: return null
        if (fromRate <= 0.0 || toRate <= 0.0) return null

        val ratio = BigDecimal.valueOf(toRate).divide(BigDecimal.valueOf(fromRate), MC)
        return amount.multiply(ratio, MC)
    }

    fun getEffectivePairRate(
        fromCode: String,
        toCode: String,
        ratesByCode: Map<String, CurrencyRateEntity>
    ): BigDecimal? {
        return convert(BigDecimal.ONE, fromCode, toCode, ratesByCode)
    }

    fun isPairUsingManualOverride(
        fromCode: String,
        toCode: String,
        ratesByCode: Map<String, CurrencyRateEntity>
    ): Boolean {
        val fromManual = ratesByCode[fromCode]?.isManualOverride == true
        val toManual = ratesByCode[toCode]?.isManualOverride == true
        return fromManual || toManual
    }

    private fun effectiveUsdRate(code: String, entity: CurrencyRateEntity?): Double? {
        if (entity != null && entity.isManualOverride && entity.manualRateFromUsd != null) {
            return entity.manualRateFromUsd
        }
        if (code == "USD") return 1.0
        return entity?.rateFromUsd
    }
}
