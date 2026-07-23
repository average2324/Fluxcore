package com.luminadigitale.fluxcore.core.premium

data class PremiumProduct(
    val productId: String,
    val title: String,
    val description: String,
    val priceLabel: String
)

data class PremiumStatus(
    val isOwned: Boolean,
    val isLoading: Boolean = false,
    val isAvailableForPurchase: Boolean = false,
    val product: PremiumProduct? = null,
    val message: String? = null
)

sealed class PremiumPurchaseResult {
    data class Success(val product: PremiumProduct?) : PremiumPurchaseResult()
    data object Cancelled : PremiumPurchaseResult()
    data class Failed(val reason: String) : PremiumPurchaseResult()
}

interface PremiumPurchaseService {
    fun refreshStatus(onResult: (PremiumStatus) -> Unit)
    fun launchPremiumPurchase(onResult: (PremiumPurchaseResult) -> Unit)

    // User-tapped "Restore Purchases" (App Store Guideline 3.1.1). Default is a plain
    // status refresh; platforms that need an explicit restore call (iOS StoreKit) override.
    fun restorePurchases(onResult: (PremiumStatus) -> Unit) = refreshStatus(onResult)
}

object UnavailablePremiumPurchaseService : PremiumPurchaseService {
    override fun refreshStatus(onResult: (PremiumStatus) -> Unit) {
        onResult(
            PremiumStatus(
                isOwned = false,
                isAvailableForPurchase = false,
                message = "Premium billing unavailable"
            )
        )
    }

    override fun launchPremiumPurchase(onResult: (PremiumPurchaseResult) -> Unit) {
        onResult(PremiumPurchaseResult.Failed("Premium billing unavailable"))
    }
}

class SimulatedPremiumPurchaseService(
    private val productId: String = "fluxcore_premium",
    private val title: String = "FluxCore Premium",
    private val description: String = "One-time premium unlock with unlimited lives.",
    private val priceLabel: String = "$4.99"
) : PremiumPurchaseService {
    private var owned = false

    override fun refreshStatus(onResult: (PremiumStatus) -> Unit) {
        onResult(
            PremiumStatus(
                isOwned = owned,
                isAvailableForPurchase = true,
                product = PremiumProduct(
                    productId = productId,
                    title = title,
                    description = description,
                    priceLabel = priceLabel
                )
            )
        )
    }

    override fun launchPremiumPurchase(onResult: (PremiumPurchaseResult) -> Unit) {
        owned = true
        onResult(
            PremiumPurchaseResult.Success(
                product = PremiumProduct(
                    productId = productId,
                    title = title,
                    description = description,
                    priceLabel = priceLabel
                )
            )
        )
    }
}
