import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*

class IAPBilling(
    private val context: Context,
    private val listener: (Purchase) -> Unit
) {

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (p in purchases) handlePurchase(p)
        } else {
            Log.w("Billing", "onPurchasesUpdated: ${result.debugMessage}")
        }
    }

    private val billingClient: BillingClient =
        BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

    fun startConnection(onReady: () -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) onReady()
            }
            override fun onBillingServiceDisconnected() {
            }
        })
    }

    fun queryInApp(productId: String, onResult: (ProductDetails?) -> Unit) {
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

        billingClient.queryProductDetailsAsync(params) {
                _: BillingResult,
                result: QueryProductDetailsResult ->
            val list: List<ProductDetails> = result.productDetailsList
            val first: ProductDetails? = list.firstOrNull()
            onResult(first)
        }
    }

    fun querySubs(
        productId: String,
        onResult: (ProductDetails?, ProductDetails.SubscriptionOfferDetails?) -> Unit
    ) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) {
                _: BillingResult,
                result: QueryProductDetailsResult ->
            val list = result.productDetailsList
            val pd = list.firstOrNull()
            val offer = pd?.subscriptionOfferDetails?.firstOrNull()
            onResult(pd, offer)
        }
    }

    fun launchInApp(activity: Activity, details: ProductDetails) {
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    fun launchSubs(
        activity: Activity,
        details: ProductDetails,
        offer: ProductDetails.SubscriptionOfferDetails
    ) {
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offer.offerToken)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    fun queryActivePurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { _: BillingResult, list: MutableList<Purchase> ->
            for (p in list) handlePurchase(p)
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { _: BillingResult, list: MutableList<Purchase> ->
            for (p in list) handlePurchase(p)
        }
    }

    private val testID = "coins_100"
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        if (purchase.products.contains(testID)) {
            val params = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.consumeAsync(params) { result: BillingResult, _: String? ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    listener(purchase) // cấp coin
                }
            }
        } else {
            if (!purchase.isAcknowledged) {
                val ack = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ack) { result: BillingResult ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        listener(purchase)
                    }
                }
            } else {
                listener(purchase)
            }
        }
    }

    fun endConnection() {
        billingClient.endConnection()
    }

}

class IAPTest{
    private lateinit var billing: IAPBilling

    private fun onBuyTest(activity: Activity) {

        billing = IAPBilling(activity) { purchase ->
            when {
                purchase.products.contains("coins_100") -> grantCoins()
                purchase.products.contains("premium_remove_ads") -> unlockPremium()
                purchase.products.contains("pro_monthly") -> unlockSubscription()
            }
        }

        billing.startConnection {
            billing.queryActivePurchases()

            /*
            billing.queryInApp("coins_100") { pd ->
                findViewById<View>(R.id.btn_buy_coins).setOnClickListener {
                    pd?.let { billing.launchInApp(activity , it) }
                }
            }

            // Ví dụ SUBS: pro_monthly
            billing.querySubs("pro_monthly") { pd, offer ->
                findViewById<View>(R.id.btn_subscribe).setOnClickListener {
                    if (pd != null && offer != null) billing.launchSubs(activity, pd, offer)
                }
            }
            */
        }
    }

    private fun grantCoins() {}
    private fun unlockPremium() {}
    private fun unlockSubscription() {}
}
