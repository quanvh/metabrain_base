package com.meta.brain.module.utils

import android.app.Activity
import android.app.Dialog
import android.content.*
import android.graphics.Color
import android.util.Log
import android.view.*
import android.widget.Toast
import com.meta.brain.databinding.UpdateDialogBinding
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import com.google.android.play.core.review.*
import com.meta.brain.R
import com.meta.brain.databinding.RateDialogBinding


fun Activity.showToast(msg: String, gravity: Int) {
    val toast: Toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT)
    toast.setGravity(gravity, 0, 0)
    toast.show()
}

fun Context.showUpdateDialog() {
    val bindingDialog = UpdateDialogBinding.inflate(LayoutInflater.from(this))

    val dialog = Dialog(this)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setContentView(bindingDialog.root)
    dialog.setCancelable(false)

    dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
    dialog.window?.setLayout(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT
    )
    dialog.window?.setGravity(Gravity.CENTER)

    bindingDialog.tvUpdate.setOnClickListener {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri()))
        }
    }

    dialog.show()
}

fun Activity.showRateDialog(onEvent: RateEvent?) {
    val dialogBinding = RateDialogBinding.inflate(LayoutInflater.from(this))


    val dialog = Dialog(this)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setContentView(dialogBinding.root)
    dialog.setCancelable(false)

    dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
    dialog.window?.setLayout(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT
    )

    var rating = 5

    dialogBinding.rate5.setOnClickListener {
        dialogBinding.apply {
            rate5.setImageResource(R.drawable.ic_rate_star_on)
            rate4.setImageResource(R.drawable.ic_rate_star_on)
            rate3.setImageResource(R.drawable.ic_rate_star_on)
            rate2.setImageResource(R.drawable.ic_rate_star_on)
            rate1.setImageResource(R.drawable.ic_rate_star_on)

            ivIconRate.setImageResource(R.drawable.img_rate_5)
            tvTitleRate.text = getString(R.string.des_rate_4_5)
            tvDesRate.text = getString(R.string.thanks_for_your_feedback)
        }
        rating = 5
    }
    dialogBinding.rate4.setOnClickListener {
        dialogBinding.apply {
            rate5.setImageResource(R.drawable.ic_rate_star_off_5)
            rate4.setImageResource(R.drawable.ic_rate_star_on)
            rate3.setImageResource(R.drawable.ic_rate_star_on)
            rate2.setImageResource(R.drawable.ic_rate_star_on)
            rate1.setImageResource(R.drawable.ic_rate_star_on)

            ivIconRate.setImageResource(R.drawable.img_rate_4)
            tvTitleRate.text = getString(R.string.des_rate_4_5)
            tvDesRate.text = getString(R.string.thanks_for_your_feedback)
        }
        rating = 4
    }
    dialogBinding.rate3.setOnClickListener {
        dialogBinding.apply {
            rate5.setImageResource(R.drawable.ic_rate_star_off_5)
            rate4.setImageResource(R.drawable.ic_rate_star_off)
            rate3.setImageResource(R.drawable.ic_rate_star_on)
            rate2.setImageResource(R.drawable.ic_rate_star_on)
            rate1.setImageResource(R.drawable.ic_rate_star_on)

            ivIconRate.setImageResource(R.drawable.img_rate_3)
            tvTitleRate.text = getString(R.string.des_rate_1_2_3)
            tvDesRate.text = getString(R.string.please_give_us_some_feedback)
        }
        rating = 3
    }
    dialogBinding.rate2.setOnClickListener {
        dialogBinding.apply {
            rate5.setImageResource(R.drawable.ic_rate_star_off_5)
            rate4.setImageResource(R.drawable.ic_rate_star_off)
            rate3.setImageResource(R.drawable.ic_rate_star_off)
            rate2.setImageResource(R.drawable.ic_rate_star_on)
            rate1.setImageResource(R.drawable.ic_rate_star_on)

            ivIconRate.setImageResource(R.drawable.img_rate_2)
            tvTitleRate.text = getString(R.string.des_rate_1_2_3)
            tvDesRate.text = getString(R.string.please_give_us_some_feedback)
        }
        rating = 2
    }
    dialogBinding.rate1.setOnClickListener {
        dialogBinding.apply {
            rate5.setImageResource(R.drawable.ic_rate_star_off_5)
            rate4.setImageResource(R.drawable.ic_rate_star_off)
            rate3.setImageResource(R.drawable.ic_rate_star_off)
            rate2.setImageResource(R.drawable.ic_rate_star_off)
            rate1.setImageResource(R.drawable.ic_rate_star_on)

            ivIconRate.setImageResource(R.drawable.img_rate_1)
            tvTitleRate.text = getString(R.string.des_rate_1_2_3)
            tvDesRate.text = getString(R.string.please_give_us_some_feedback)
        }
        rating = 1
    }

    dialogBinding.tvRate.setOnClickListener {
        if (rating == 5) rateApp(this, dialog) else sendFeedback(this)
        dialog.cancel()
        onEvent?.onRate()
    }
    dialogBinding.tvCancel.setOnClickListener {
        dialog.cancel()
        onEvent?.onDismiss()
    }

    dialog.show()
}

private fun rateApp(activity: Activity, dialog: Dialog) {
    val manager = ReviewManagerFactory.create(activity)
    val request = manager.requestReviewFlow()

    request.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val reviewInfo = task.result
            if (reviewInfo != null) {
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    activity.showToast(activity.getString(R.string.rate_thanks), Gravity.CENTER)
                        dialog.dismiss()
                }
            } else {
                // Review info is not yet available
                Log.d("[RateDialog]", "Review info is not yet available")
            }
        } else {
            Log.e("[RateDialog]", "Rate flow error: "+ (task.exception as ReviewException).errorCode)
        }
    }
}

private const val GMAIL = "abc@gmail.com"
fun sendFeedback(activity: Activity?) {
    val selectorIntent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:".toUri()
    }
    val emailIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(GMAIL))
        putExtra(Intent.EXTRA_SUBJECT, activity?.resources?.getString(R.string.app_name) + " feedback")
        putExtra(Intent.EXTRA_TEXT, "")
        selector = selectorIntent
    }
    try {
        activity?.startActivity(Intent.createChooser(emailIntent, ""))
    } catch (ex: ActivityNotFoundException) {
        Toast.makeText(activity, "No email clients installed.", Toast.LENGTH_SHORT).show()
    }
}

public abstract class RateEvent{
    open fun onRate(){}
    open fun onDismiss(){}
}