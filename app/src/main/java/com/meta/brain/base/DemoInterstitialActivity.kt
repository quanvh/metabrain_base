package com.meta.brain.base

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.meta.brain.module.ads.AdEvent
import com.meta.brain.module.ads.AdsController
import com.meta.brain.module.ads.AdsInter
import com.meta.brain.module.base.BaseActivity

/**
 * Demo Activity để test AdsInter
 *
 * Cách sử dụng AdsInter:
 * 1. Qua AdsController (khuyến nghị - như trong LoadingAdFragment):
 *    - AdsController.loadInter(context, AdEvent)
 *    - AdsController.showInter(activity, AdEvent)
 *
 * 2. Trực tiếp với AdsInter:
 *    - Tạo instance: AdsInter(preload = true/false)
 *    - Load: adsInter.loadInter(context, adUnit, AdEvent)
 *    - Show: adsInter.showInter(activity, AdEvent)
 *
 * Liên quan đến thư mục loading:
 * - LoadingAdFragment.kt sử dụng AdsController.loadInter() và showInter()
 * - LoadingActivity.kt sử dụng AdsController.showInterOpen() khi mở app
 */
class DemoInterstitialActivity : BaseActivity() {

    companion object {
        private const val TAG = "[DemoInterstitialActivity]"
    }

    private lateinit var btnLoadViaController: Button
    private lateinit var btnShowViaController: Button
    private lateinit var btnLoadDirect: Button
    private lateinit var btnShowDirect: Button
    private lateinit var btnLoadAndShow: Button
    private lateinit var tvStatus: TextView

    // Cách 1: Sử dụng AdsInter trực tiếp
    private var adsInterDirect: AdsInter? = null

    // Cách 2: Sử dụng qua AdsController (như trong LoadingAdFragment)
    private var isAdLoadedViaController = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.meta.brain.base.R.layout.demo_interstitial_activity)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnLoadViaController = findViewById(R.id.btnLoadViaController)
        btnShowViaController = findViewById(R.id.btnShowViaController)
        btnLoadDirect = findViewById(R.id.btnLoadDirect)
        btnShowDirect = findViewById(R.id.btnShowDirect)
        btnLoadAndShow = findViewById(R.id.btnLoadAndShow)
        tvStatus = findViewById(R.id.tvStatus)

        updateStatus("Sẵn sàng. Chọn một phương thức để test ads.")
    }

    private fun setupClickListeners() {
        // ========== CÁCH 1: Sử dụng qua AdsController (như LoadingAdFragment) ==========
        btnLoadViaController.setOnClickListener {
            updateStatus("Đang load ads qua AdsController...")
            loadInterViaController()
        }

        btnShowViaController.setOnClickListener {
            if (isAdLoadedViaController) {
                updateStatus("Đang show ads qua AdsController...")
                showInterViaController()
            } else {
                updateStatus("❌ Chưa load ads! Load trước khi show.")
                Toast.makeText(this, "Load ads trước!", Toast.LENGTH_SHORT).show()
            }
        }

        // ========== CÁCH 2: Sử dụng AdsInter trực tiếp ==========
        btnLoadDirect.setOnClickListener {
            updateStatus("Đang load ads trực tiếp với AdsInter...")
            loadInterDirect()
        }

        btnShowDirect.setOnClickListener {
            if (adsInterDirect != null) {
                updateStatus("Đang show ads trực tiếp...")
                showInterDirect()
            } else {
                updateStatus("❌ Chưa load ads! Load trước khi show.")
                Toast.makeText(this, "Load ads trước!", Toast.LENGTH_SHORT).show()
            }
        }

        // ========== CÁCH 3: Load và show tự động (như LoadingAdFragment) ==========
        btnLoadAndShow.setOnClickListener {
            updateStatus("Đang load và show tự động...")
            loadAndShowAuto()
        }
    }

    /**
     * CÁCH 1: Load ads qua AdsController
     * Giống như trong LoadingAdFragment.kt dòng 103-112
     */
    private fun loadInterViaController() {
        AdsController.Companion.loadInter(this, object : AdEvent() {
            override fun onLoaded() {
                isAdLoadedViaController = true
                updateStatus("✅ Ads đã load thành công qua AdsController!")
                Log.d(TAG, "Ads loaded via AdsController")
                Toast.makeText(this@DemoInterstitialActivity, "Ads loaded!", Toast.LENGTH_SHORT).show()
            }

            override fun onLoadFail() {
                isAdLoadedViaController = false
                updateStatus("❌ Load ads thất bại qua AdsController")
                Log.e(TAG, "Ads load failed via AdsController")
                Toast.makeText(this@DemoInterstitialActivity, "Load failed!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * CÁCH 1: Show ads qua AdsController
     * Giống như trong LoadingAdFragment.kt dòng 105-108
     */
    private fun showInterViaController() {
        AdsController.Companion.showInter(this, object : AdEvent() {
            override fun onComplete() {
                isAdLoadedViaController = false
                updateStatus("✅ Ads đã show và đóng thành công!")
                Log.d(TAG, "Ads shown and dismissed via AdsController")
                Toast.makeText(this@DemoInterstitialActivity, "Ads completed!", Toast.LENGTH_SHORT).show()
            }

            override fun onShowFail() {
                isAdLoadedViaController = false
                updateStatus("❌ Show ads thất bại")
                Log.e(TAG, "Ads show failed via AdsController")
            }
        })
    }

    /**
     * CÁCH 2: Load ads trực tiếp với AdsInter
     * Tương tự như trong AdsInter.kt hàm loadInter()
     */
    private fun loadInterDirect() {
        // Tạo instance AdsInter với preload = true (tự động load lại sau khi show)
        adsInterDirect = AdsInter(preload = true)

        val adUnit = getString(R.string.inter_default)
        adsInterDirect?.loadInter(this, adUnit, object : AdEvent() {
            override fun onLoaded() {
                updateStatus("✅ Ads đã load thành công trực tiếp!")
                Log.d(TAG, "Ads loaded directly")
                Toast.makeText(this@DemoInterstitialActivity, "Ads loaded!", Toast.LENGTH_SHORT).show()
            }

            override fun onLoadFail() {
                adsInterDirect = null
                updateStatus("❌ Load ads thất bại trực tiếp")
                Log.e(TAG, "Ads load failed directly")
                Toast.makeText(this@DemoInterstitialActivity, "Load failed!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * CÁCH 2: Show ads trực tiếp với AdsInter
     * Tương tự như trong AdsInter.kt hàm showInter()
     */
    private fun showInterDirect() {
        adsInterDirect?.showInter(this, object : AdEvent() {
            override fun onComplete() {
                updateStatus("✅ Ads đã show và đóng thành công!")
                Log.d(TAG, "Ads shown and dismissed directly")
                Toast.makeText(this@DemoInterstitialActivity, "Ads completed!", Toast.LENGTH_SHORT).show()
                // Nếu preload = true, ads sẽ tự động load lại
            }

            override fun onShowFail() {
                updateStatus("❌ Show ads thất bại")
                Log.e(TAG, "Ads show failed directly")
            }
        })
    }

    /**
     * CÁCH 3: Load và show tự động (giống LoadingAdFragment)
     * Load xong tự động show, giống pattern trong LoadingAdFragment.kt
     */
    private fun loadAndShowAuto() {
        AdsController.Companion.loadInter(this, object : AdEvent() {
            override fun onLoaded() {
                // Tự động show khi load thành công
                AdsController.Companion.showInter(this@DemoInterstitialActivity, object : AdEvent() {
                    override fun onComplete() {
                        updateStatus("✅ Load và show tự động thành công!")
                        Log.d(TAG, "Auto load and show completed")
                        Toast.makeText(this@DemoInterstitialActivity, "Auto completed!", Toast.LENGTH_SHORT).show()
                    }

                    override fun onShowFail() {
                        updateStatus("❌ Show ads thất bại trong auto mode")
                        Log.e(TAG, "Auto show failed")
                    }
                })
            }

            override fun onLoadFail() {
                updateStatus("❌ Load ads thất bại trong auto mode")
                Log.e(TAG, "Auto load failed")
                Toast.makeText(this@DemoInterstitialActivity, "Auto load failed!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateStatus(message: String) {
        tvStatus.text = message
        Log.d(TAG, message)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup nếu cần
        adsInterDirect = null
    }
}