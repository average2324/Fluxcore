package com.luminadigitale.fluxcore.ios

import com.luminadigitale.fluxcore.core.premium.PremiumProduct
import com.luminadigitale.fluxcore.core.premium.PremiumPurchaseResult
import com.luminadigitale.fluxcore.core.premium.PremiumPurchaseService
import com.luminadigitale.fluxcore.core.premium.PremiumStatus
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.foundation.NSError
import org.robovm.apple.foundation.NSNumberFormatter
import org.robovm.apple.foundation.NSNumberFormatterStyle
import org.robovm.apple.storekit.SKPayment
import org.robovm.apple.storekit.SKPaymentQueue
import org.robovm.apple.storekit.SKPaymentTransaction
import org.robovm.apple.storekit.SKPaymentTransactionObserverAdapter
import org.robovm.apple.storekit.SKPaymentTransactionState
import org.robovm.apple.storekit.SKProduct
import org.robovm.apple.storekit.SKProductsRequest
import org.robovm.apple.storekit.SKProductsRequestDelegateAdapter
import org.robovm.apple.storekit.SKProductsResponse
import org.robovm.apple.storekit.SKRequest

class IosPremiumPurchaseService(
    private val productId: String =
        System.getenv("ORBITFLUX_IOS_PREMIUM_PRODUCT_ID")
            ?.takeIf { it.isNotBlank() }
            ?: "fluxcore_premium",
) : PremiumPurchaseService {
    private val queue: SKPaymentQueue = SKPaymentQueue.getDefaultQueue()
    private var product: SKProduct? = null
    private var cachedProduct: PremiumProduct? = null
    private var cachedOwned = false
    private var activeRequest: SKProductsRequest? = null
    private var activePurchaseCallback: ((PremiumPurchaseResult) -> Unit)? = null
    private var activeRestoreCallback: ((PremiumStatus) -> Unit)? = null

    private val transactionObserver =
        object : SKPaymentTransactionObserverAdapter() {
            override fun updatedTransactions(
                queue: SKPaymentQueue,
                transactions: NSArray<SKPaymentTransaction>,
            ) {
                for (transaction in transactions) {
                    handleTransaction(queue, transaction)
                }
            }

            override fun restoreCompletedTransactionsFinished(queue: SKPaymentQueue) {
                activePurchaseCallback?.invoke(
                    if (cachedOwned) {
                        PremiumPurchaseResult.Success(cachedProduct)
                    } else {
                        PremiumPurchaseResult.Failed("No previous purchase found to restore")
                    },
                )
                activePurchaseCallback = null
                activeRestoreCallback?.invoke(
                    currentStatus(
                        message = if (cachedOwned) null else "No previous purchase found to restore",
                    ),
                )
                activeRestoreCallback = null
            }

            override fun restoreCompletedTransactionsFailed(
                queue: SKPaymentQueue,
                error: NSError,
            ) {
                activePurchaseCallback?.invoke(PremiumPurchaseResult.Failed(error.localizedDescription))
                activePurchaseCallback = null
                activeRestoreCallback?.invoke(currentStatus(message = error.localizedDescription))
                activeRestoreCallback = null
            }
        }

    init {
        queue.addTransactionObserver(transactionObserver)
    }

    override fun refreshStatus(onResult: (PremiumStatus) -> Unit) {
        onResult(
            PremiumStatus(
                isOwned = cachedOwned,
                isLoading = true,
                isAvailableForPurchase = product != null,
                product = cachedProduct,
            ),
        )
        loadProduct { premiumProduct, error ->
            onResult(
                PremiumStatus(
                    isOwned = cachedOwned,
                    isAvailableForPurchase = premiumProduct != null && SKPaymentQueue.canMakePayments(),
                    product = premiumProduct,
                    message = error,
                ),
            )
        }
    }

    override fun launchPremiumPurchase(onResult: (PremiumPurchaseResult) -> Unit) {
        if (cachedOwned) {
            onResult(PremiumPurchaseResult.Success(cachedProduct))
            return
        }
        if (!SKPaymentQueue.canMakePayments()) {
            onResult(PremiumPurchaseResult.Failed("App Store purchases are disabled on this device"))
            return
        }
        if (activePurchaseCallback != null) {
            onResult(PremiumPurchaseResult.Failed("An App Store purchase is already in progress"))
            return
        }
        loadProduct { _, error ->
            val skProduct = product
            if (skProduct == null) {
                onResult(PremiumPurchaseResult.Failed(error ?: "FluxCore Premium is not configured in App Store Connect"))
                return@loadProduct
            }
            activePurchaseCallback = onResult
            queue.addPayment(SKPayment(skProduct))
        }
    }

    override fun restorePurchases(onResult: (PremiumStatus) -> Unit) {
        if (!SKPaymentQueue.canMakePayments()) {
            onResult(currentStatus(message = "App Store purchases are disabled on this device"))
            return
        }
        onResult(currentStatus(isLoading = true))
        activeRestoreCallback = onResult
        queue.restoreCompletedTransactions()
    }

    private fun currentStatus(
        isLoading: Boolean = false,
        message: String? = null,
    ): PremiumStatus =
        PremiumStatus(
            isOwned = cachedOwned,
            isLoading = isLoading,
            isAvailableForPurchase = product != null && SKPaymentQueue.canMakePayments(),
            product = cachedProduct,
            message = message,
        )

    private fun loadProduct(onComplete: (PremiumProduct?, String?) -> Unit) {
        cachedProduct?.let {
            onComplete(it, null)
            return
        }
        activeRequest?.cancel()
        val request = SKProductsRequest(setOf(productId))
        request.setDelegate(
            object : SKProductsRequestDelegateAdapter() {
                override fun didReceiveResponse(
                    request: SKProductsRequest,
                    response: SKProductsResponse,
                ) {
                    val matchedProduct = response.products.firstOrNull { it.productIdentifier == productId }
                    product = matchedProduct
                    cachedProduct = matchedProduct?.toPremiumProduct()
                    val error =
                        if (matchedProduct == null) {
                            "FluxCore Premium is not configured in App Store Connect"
                        } else {
                            null
                        }
                    activeRequest = null
                    onComplete(cachedProduct, error)
                }

                override fun didFail(
                    request: SKRequest,
                    error: NSError,
                ) {
                    activeRequest = null
                    onComplete(null, error.localizedDescription)
                }
            },
        )
        activeRequest = request
        request.start()
    }

    private fun handleTransaction(
        queue: SKPaymentQueue,
        transaction: SKPaymentTransaction,
    ) {
        val transactionProductId = transaction.payment.productIdentifier
        if (transactionProductId != productId) {
            return
        }
        when (transaction.transactionState) {
            SKPaymentTransactionState.Purchased,
            SKPaymentTransactionState.Restored,
            -> {
                cachedOwned = true
                queue.finishTransaction(transaction)
                activePurchaseCallback?.invoke(PremiumPurchaseResult.Success(cachedProduct))
                activePurchaseCallback = null
            }

            SKPaymentTransactionState.Failed -> {
                queue.finishTransaction(transaction)
                val errorMessage = transaction.error?.localizedDescription ?: "App Store purchase failed"
                activePurchaseCallback?.invoke(PremiumPurchaseResult.Failed(errorMessage))
                activePurchaseCallback = null
            }

            SKPaymentTransactionState.Deferred -> {
                // Ask to Buy: no further callback may arrive, so release the UI now.
                activePurchaseCallback?.invoke(
                    PremiumPurchaseResult.Failed(
                        "Purchase is awaiting approval. Premium unlocks automatically once approved.",
                    ),
                )
                activePurchaseCallback = null
            }

            SKPaymentTransactionState.Purchasing -> Unit
        }
    }

    private fun SKProduct.toPremiumProduct(): PremiumProduct {
        val formatter =
            NSNumberFormatter().apply {
                numberStyle = NSNumberFormatterStyle.Currency
                locale = priceLocale
            }
        return PremiumProduct(
            productId = productIdentifier,
            title = localizedTitle.ifBlank { "FluxCore Premium" },
            description = localizedDescription.ifBlank { "One-time premium unlock" },
            priceLabel = formatter.format(price) ?: price.toString(),
        )
    }
}
