package com.example.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BillingProductUi(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val isSubscription: Boolean,
    val productDetails: ProductDetails,
    val offerToken: String? = null
)

sealed class BillingPurchaseState {
    object Idle : BillingPurchaseState()
    object Loading : BillingPurchaseState()
    object Pending : BillingPurchaseState()
    object Cancelled : BillingPurchaseState()
    data class Purchased(val productId: String) : BillingPurchaseState()
    data class Error(val message: String) : BillingPurchaseState()
}

/**
 * Real Google Play Billing integration.
 * - Never invents product prices (shows only actual ProductDetails returned by Play Billing).
 * - Never unlocks Premium from local button clicks; only verified PurchaseState.PURCHASED tokens unlock Premium.
 * - Handles Pending, Cancelled, Failed, Acknowledge, and Restore Purchases cleanly.
 */
class BillingRepository(context: Context) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _products = MutableStateFlow<List<BillingProductUi>>(emptyList())
    val products: StateFlow<List<BillingProductUi>> = _products.asStateFlow()

    private val _billingReady = MutableStateFlow(false)
    val billingReady: StateFlow<Boolean> = _billingReady.asStateFlow()

    private val _purchaseState = MutableStateFlow<BillingPurchaseState>(BillingPurchaseState.Idle)
    val purchaseState: StateFlow<BillingPurchaseState> = _purchaseState.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        connectAndQuery()
    }

    fun connectAndQuery() {
        if (billingClient.isReady) {
            queryAllProducts()
            restorePurchases(silent = true)
            return
        }
        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        _billingReady.value = true
                        queryAllProducts()
                        restorePurchases(silent = true)
                    } else {
                        _billingReady.value = false
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _billingReady.value = false
                }
            })
        } catch (_: Exception) {
            _billingReady.value = false
        }
    }

    private fun queryAllProducts() {
        val loaded = mutableListOf<BillingProductUi>()

        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BuildConfig.SKU_PREMIUM_LIFETIME)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(inAppParams) { result, detailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                detailsList.forEach { details ->
                    val price = details.oneTimePurchaseOfferDetails?.formattedPrice
                    if (!price.isNullOrBlank()) {
                        loaded.add(
                            BillingProductUi(
                                productId = details.productId,
                                title = details.name,
                                description = details.description,
                                formattedPrice = price,
                                isSubscription = false,
                                productDetails = details
                            )
                        )
                    }
                }
            }

            val subsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(BuildConfig.SKU_PREMIUM_MONTHLY)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build(),
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(BuildConfig.SKU_PREMIUM_YEARLY)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                )
                .build()

            billingClient.queryProductDetailsAsync(subsParams) { subResult, subDetailsList ->
                if (subResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    subDetailsList.forEach { details ->
                        val offer = details.subscriptionOfferDetails?.firstOrNull()
                        val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                        if (!price.isNullOrBlank() && offer != null) {
                            loaded.add(
                                BillingProductUi(
                                    productId = details.productId,
                                    title = details.name,
                                    description = details.description,
                                    formattedPrice = price,
                                    isSubscription = true,
                                    productDetails = details,
                                    offerToken = offer.offerToken
                                )
                            )
                        }
                    }
                }
                _products.value = loaded.toList()
            }
        }
    }

    fun launchPurchase(activity: Activity, product: BillingProductUi) {
        if (!billingClient.isReady) {
            _purchaseState.value = BillingPurchaseState.Error("Google Play Billing is not connected.")
            return
        }
        _purchaseState.value = BillingPurchaseState.Loading

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product.productDetails)
            .apply {
                if (product.isSubscription && !product.offerToken.isNullOrBlank()) {
                    setOfferToken(product.offerToken)
                }
            }
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val launchResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseState.value = BillingPurchaseState.Error(
                launchResult.debugMessage.ifBlank { "Unable to start purchase flow." }
            )
        }
    }

    fun restorePurchases(silent: Boolean = false) {
        if (!billingClient.isReady) {
            if (!silent) {
                _purchaseState.value = BillingPurchaseState.Error("Google Play service unavailable.")
            }
            return
        }
        if (!silent) _purchaseState.value = BillingPurchaseState.Loading

        val validSkus = setOf(
            BuildConfig.SKU_PREMIUM_LIFETIME,
            BuildConfig.SKU_PREMIUM_MONTHLY,
            BuildConfig.SKU_PREMIUM_YEARLY
        )

        var foundActive = false

        val inAppQuery = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(inAppQuery) { inAppRes, inAppPurchases ->
            if (inAppRes.responseCode == BillingClient.BillingResponseCode.OK) {
                inAppPurchases.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.products.any { it in validSkus }
                    ) {
                        foundActive = true
                        acknowledgeIfNeeded(purchase)
                    }
                }
            }

            val subsQuery = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            billingClient.queryPurchasesAsync(subsQuery) { subsRes, subsPurchases ->
                if (subsRes.responseCode == BillingClient.BillingResponseCode.OK) {
                    subsPurchases.forEach { purchase ->
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            purchase.products.any { it in validSkus }
                        ) {
                            foundActive = true
                            acknowledgeIfNeeded(purchase)
                        }
                    }
                }
                _isPremium.value = foundActive
                if (!silent) {
                    _purchaseState.value = if (foundActive) {
                        BillingPurchaseState.Purchased("restored")
                    } else {
                        BillingPurchaseState.Error("No active CalcPro Premium purchases found for this Google account.")
                    }
                }
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val list = purchases ?: emptyList()
                if (list.isEmpty()) {
                    _purchaseState.value = BillingPurchaseState.Idle
                    return
                }
                list.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = BillingPurchaseState.Cancelled
            }
            else -> {
                _purchaseState.value = BillingPurchaseState.Error(
                    billingResult.debugMessage.ifBlank { "Purchase failed (code ${billingResult.responseCode})." }
                )
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                acknowledgeIfNeeded(purchase) { acknowledged ->
                    if (acknowledged) {
                        _isPremium.value = true
                        _purchaseState.value = BillingPurchaseState.Purchased(
                            purchase.products.firstOrNull() ?: ""
                        )
                    } else {
                        _purchaseState.value = BillingPurchaseState.Error("Purchase verification failed.")
                    }
                }
            }
            Purchase.PurchaseState.PENDING -> {
                _purchaseState.value = BillingPurchaseState.Pending
            }
            else -> {
                _purchaseState.value = BillingPurchaseState.Idle
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase, onDone: ((Boolean) -> Unit)? = null) {
        if (purchase.isAcknowledged) {
            onDone?.invoke(true)
            return
        }
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        scope.launch {
            billingClient.acknowledgePurchase(params) { result ->
                onDone?.invoke(result.responseCode == BillingClient.BillingResponseCode.OK)
            }
        }
    }

    fun clearPurchaseMessage() {
        _purchaseState.value = BillingPurchaseState.Idle
    }
}
