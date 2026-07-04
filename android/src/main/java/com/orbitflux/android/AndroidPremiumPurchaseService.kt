package com.orbitflux.android

import android.app.Activity
import android.os.Handler
import android.os.Looper
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
import com.orbitflux.core.premium.PremiumProduct
import com.orbitflux.core.premium.PremiumPurchaseResult
import com.orbitflux.core.premium.PremiumPurchaseService
import com.orbitflux.core.premium.PremiumStatus

class AndroidPremiumPurchaseService(
    private val activity: Activity
) : PremiumPurchaseService, PurchasesUpdatedListener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val productId = BuildConfig.PREMIUM_PRODUCT_ID
    private val billingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    @Volatile
    private var connected = false
    private var connecting = false
    private var cachedOwned = false
    private var cachedProduct: PremiumProduct? = null
    private var latestProductDetails: ProductDetails? = null
    private var activePurchaseCallback: ((PremiumPurchaseResult) -> Unit)? = null

    init {
        ensureConnected(null)
    }

    override fun refreshStatus(onResult: (PremiumStatus) -> Unit) {
        onResult(
            PremiumStatus(
                isOwned = isPremiumOwned(),
                isLoading = true,
                isAvailableForPurchase = latestProductDetails != null,
                product = cachedProduct
            )
        )

        ensureConnected { connectionOk ->
            if (!connectionOk) {
                onResult(
                    PremiumStatus(
                        isOwned = isPremiumOwned(),
                        isAvailableForPurchase = false,
                        product = cachedProduct,
                        message = "Google Play Billing is unavailable"
                    )
                )
                return@ensureConnected
            }

            queryProductAndOwnership(onResult)
        }
    }

    override fun launchPremiumPurchase(onResult: (PremiumPurchaseResult) -> Unit) {
        if (isPremiumOwned()) {
            onResult(PremiumPurchaseResult.Success(cachedProduct))
            return
        }
        if (activePurchaseCallback != null) {
            onResult(PremiumPurchaseResult.Failed("A premium purchase is already in progress"))
            return
        }

        ensureConnected { connectionOk ->
            if (!connectionOk) {
                onResult(PremiumPurchaseResult.Failed("Google Play Billing is unavailable"))
                return@ensureConnected
            }

            loadProductDetails { productDetails, errorMessage ->
                if (productDetails == null) {
                    onResult(PremiumPurchaseResult.Failed(errorMessage ?: "Premium product is unavailable"))
                    return@loadProductDetails
                }

                runOnMain {
                    val flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                            listOf(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails)
                                    .build()
                            )
                        )
                        .build()
                    activePurchaseCallback = onResult
                    val billingResult = billingClient.launchBillingFlow(activity, flowParams)
                    if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        val callback = activePurchaseCallback
                        activePurchaseCallback = null
                        callback?.invoke(
                            PremiumPurchaseResult.Failed(
                                billingResult.debugMessage.ifBlank {
                                    "Purchase flow could not be opened"
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val matchedPurchases = purchases.orEmpty().filter { purchase ->
                    purchase.products.contains(productId)
                }
                if (matchedPurchases.isEmpty()) {
                    cachedOwned = false
                    finishPurchaseFlow(PremiumPurchaseResult.Failed("Premium purchase did not return a valid entitlement"))
                    return
                }
                handleOwnedPurchases(matchedPurchases) { owned, error ->
                    if (owned) {
                        finishPurchaseFlow(PremiumPurchaseResult.Success(cachedProduct))
                    } else {
                        finishPurchaseFlow(
                            PremiumPurchaseResult.Failed(
                                error ?: "Premium entitlement could not be verified"
                            )
                        )
                    }
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> finishPurchaseFlow(PremiumPurchaseResult.Cancelled)

            else -> finishPurchaseFlow(
                PremiumPurchaseResult.Failed(
                    billingResult.debugMessage.ifBlank {
                        "Purchase failed with code ${billingResult.responseCode}"
                    }
                )
            )
        }
    }

    fun destroy() {
        runOnMain {
            activePurchaseCallback = null
            if (billingClient.isReady) {
                billingClient.endConnection()
            }
            connected = false
            connecting = false
        }
    }

    private fun ensureConnected(onReady: ((Boolean) -> Unit)?) {
        runOnMain {
            if (billingClient.isReady || connected) {
                connected = true
                onReady?.invoke(true)
                return@runOnMain
            }
            if (connecting) {
                onReady?.let { callback ->
                    mainHandler.postDelayed({ callback.invoke(billingClient.isReady || connected) }, 180L)
                }
                return@runOnMain
            }
            connecting = true
            billingClient.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        connecting = false
                        connected = result.responseCode == BillingClient.BillingResponseCode.OK
                        onReady?.invoke(connected)
                    }

                    override fun onBillingServiceDisconnected() {
                        connected = false
                    }
                }
            )
        }
    }

    private fun queryProductAndOwnership(onResult: (PremiumStatus) -> Unit) {
        loadProductDetails { _, detailsError ->
            queryOwnership { owned, ownershipError ->
                val message = ownershipError ?: detailsError
                onResult(
                    PremiumStatus(
                        isOwned = owned,
                        isAvailableForPurchase = latestProductDetails != null,
                        product = cachedProduct,
                        message = message
                    )
                )
            }
        }
    }

    private fun loadProductDetails(onComplete: (ProductDetails?, String?) -> Unit) {
        runOnMain {
            if (!billingClient.isReady) {
                onComplete(null, "Google Play Billing is unavailable")
                return@runOnMain
            }
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                )
                .build()

            billingClient.queryProductDetailsAsync(params) { result, products ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    latestProductDetails = null
                    cachedProduct = null
                    onComplete(
                        null,
                        result.debugMessage.ifBlank {
                            "Premium product details could not be loaded"
                        }
                    )
                    return@queryProductDetailsAsync
                }

                val details = products.firstOrNull { it.productId == productId }
                latestProductDetails = details
                cachedProduct = details?.toPremiumProduct()
                onComplete(
                    details,
                    if (details == null) "Premium product is not configured in Google Play yet" else null
                )
            }
        }
    }

    private fun queryOwnership(onComplete: (Boolean, String?) -> Unit) {
        runOnMain {
            if (!billingClient.isReady) {
                onComplete(false, "Google Play Billing is unavailable")
                return@runOnMain
            }
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            billingClient.queryPurchasesAsync(params) { result, purchases ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    onComplete(
                        false,
                        result.debugMessage.ifBlank {
                            "Premium ownership could not be refreshed"
                        }
                    )
                    return@queryPurchasesAsync
                }
                val matchedPurchases = purchases.filter { purchase ->
                    purchase.products.contains(productId)
                }
                if (matchedPurchases.isEmpty()) {
                    cachedOwned = false
                    onComplete(false, null)
                    return@queryPurchasesAsync
                }
                handleOwnedPurchases(matchedPurchases, onComplete)
            }
        }
    }

    private fun handleOwnedPurchases(
        purchases: List<Purchase>,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val purchase = purchases.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (purchase == null) {
            val pending = purchases.firstOrNull { it.purchaseState == Purchase.PurchaseState.PENDING }
            if (pending != null) {
                onComplete(false, "Premium purchase is pending approval")
            } else {
                onComplete(false, "Premium entitlement is not active")
            }
            return
        }

        if (purchase.isAcknowledged) {
            cachedOwned = true
            onComplete(true, null)
            return
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { acknowledgeResult ->
            if (acknowledgeResult.responseCode == BillingClient.BillingResponseCode.OK) {
                cachedOwned = true
                onComplete(true, null)
            } else {
                cachedOwned = false
                onComplete(
                    false,
                    acknowledgeResult.debugMessage.ifBlank {
                        "Premium purchase could not be acknowledged"
                    }
                )
            }
        }
    }

    private fun finishPurchaseFlow(result: PremiumPurchaseResult) {
        val callback = activePurchaseCallback
        activePurchaseCallback = null
        callback?.invoke(result)
    }

    private fun ProductDetails.toPremiumProduct(): PremiumProduct {
        val titleValue = title.substringBefore(" (").ifBlank { "FluxCore Premium" }
        val descriptionValue = description.ifBlank { "One-time premium unlock" }
        val priceValue = oneTimePurchaseOfferDetails?.formattedPrice ?: "ONE-TIME"
        return PremiumProduct(
            productId = productId,
            title = titleValue,
            description = descriptionValue,
            priceLabel = priceValue
        )
    }

    private fun isPremiumOwned(): Boolean {
        return cachedOwned
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}
