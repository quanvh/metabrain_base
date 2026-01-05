package com.meta.brain.module.language

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.meta.brain.R
import de.hdodenhof.circleimageview.CircleImageView

class LanguageAdapter(
    private val context: Context,
    private val listLanguage: MutableList<LanguageModel>,
    private val callback: LanguageAdapterCallBack?,
    @LayoutRes private val customItemLayoutId: Int = 0
) : RecyclerView.Adapter<LanguageAdapter.LanguageAdapterVH>() {

    private var selectedPosition: Int = -1

    init {
        // Ensure only the first selected item is honored
        val firstSelectedIndex = listLanguage.indexOfFirst { it.isSelected }
        if (firstSelectedIndex != -1) {
            selectedPosition = firstSelectedIndex
            // Make sure no other items are marked as selected
            for (i in listLanguage.indices) {
                if (i != selectedPosition) {
                    listLanguage[i].isSelected = false
                }
            }
        }
    }

    inner class LanguageAdapterVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bgLayout: ConstraintLayout = itemView.findViewById(R.id.bg_layout)
        private val imgCircleChoose: ImageView = itemView.findViewById(R.id.imgCircleChoose)
        private val imgRoundChoose: ImageView = itemView.findViewById(R.id.imgRoundChoose)
        private val tvtNameCountry: TextView = itemView.findViewById(R.id.tvtNameCountry)
        private val countryImage: CircleImageView = itemView.findViewById(R.id.countryImage)

        fun onBind(languageModel: LanguageModel, position: Int) {
            itemView.setOnClickListener {
                if (selectedPosition != position) {
                    val previouslySelectedPosition = selectedPosition
                    if (previouslySelectedPosition != -1) {
                        listLanguage[previouslySelectedPosition].isSelected = false
                        notifyItemChanged(previouslySelectedPosition)
                    }

                    selectedPosition = position
                    listLanguage[selectedPosition].isSelected = true
                    notifyItemChanged(selectedPosition)
                    callback?.onSelectLanguage(listLanguage[selectedPosition])
                }
            }

            // Use selectedPosition as the single source of truth for UI state
            if (position == selectedPosition) {
                imgCircleChoose.visibility = View.VISIBLE
                imgRoundChoose.visibility = View.GONE
                bgLayout.setBackgroundResource(R.drawable.bg_language_item_selected)
            } else {
                imgCircleChoose.visibility = View.GONE
                imgRoundChoose.visibility = View.VISIBLE
                bgLayout.setBackgroundResource(R.drawable.bg_language_item_unselected)
            }

            tvtNameCountry.text = languageModel.name

            val flagDrawableRes = getFlagDrawableRes(languageModel.languageCode)
            if (flagDrawableRes != null) {
                Glide.with(context)
                    .load(flagDrawableRes)
                    .listener(object : RequestListener<Drawable?> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable?>,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable?>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }
                    })
                    .into(countryImage)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageAdapterVH {
        val layoutId = if (customItemLayoutId != 0) {
            customItemLayoutId
        } else {
            R.layout.language_item
        }
        val view = LayoutInflater.from(context).inflate(layoutId, parent, false)
        return LanguageAdapterVH(view)
    }

    override fun onBindViewHolder(holder: LanguageAdapterVH, position: Int) {
        val languageModel = listLanguage[position]
        holder.onBind(languageModel, position)
    }

    override fun getItemCount(): Int = listLanguage.size

    /**
     * Maps language code to drawable resource ID for flag images
     * @param languageCode The language code (e.g., "en", "pt", "es", "tr-TR", "pt-BR")
     * @return Drawable resource ID or null if not found
     */
    private fun getFlagDrawableRes(languageCode: String): Int? {
        // Extract base language code (part before hyphen) for codes like "tr-TR", "pt-BR"
        val baseCode = languageCode.lowercase().substringBefore("-")
        val fullCode = languageCode.lowercase()
        
        return when {
            // Handle full codes with region first (e.g., "pt-br", "tr-tr")
            fullCode == "pt-br" -> R.drawable.ic_language_brazil
            fullCode == "tr-tr" -> R.drawable.ic_language_tr
            // Handle base language codes
            baseCode == "en" -> R.drawable.ic_language_en
            baseCode == "in" -> R.drawable.ic_language_indo
            baseCode == "pt" -> R.drawable.ic_language_pt
            baseCode == "es" -> R.drawable.ic_language_es
            baseCode == "hi" -> R.drawable.ic_language_hi
            baseCode == "tr" -> R.drawable.ic_language_tr
            baseCode == "fr" -> R.drawable.ic_language_fr
            baseCode == "vi" -> R.drawable.ic_language_vi
            baseCode == "ru" -> R.drawable.ic_language_ru
            baseCode == "de" -> R.drawable.ic_language_de
            baseCode == "cn" || baseCode == "zh" -> R.drawable.ic_language_cn
            baseCode == "ja" -> R.drawable.ic_language_ja
            baseCode == "ko" -> R.drawable.ic_language_ko
            baseCode == "ma" || baseCode == "ms" -> R.drawable.ic_language_ma
            baseCode == "nl" -> R.drawable.ic_language_nl
            baseCode == "phi" || baseCode == "fil" || baseCode == "tl" -> R.drawable.ic_language_phi
            baseCode == "ita" || baseCode == "it" -> R.drawable.ic_language_ita
            baseCode == "br" -> R.drawable.ic_language_brazil
            else -> null
        }
    }

    interface LanguageAdapterCallBack {
        fun onSelectLanguage(languageModel: LanguageModel)
    }
}
