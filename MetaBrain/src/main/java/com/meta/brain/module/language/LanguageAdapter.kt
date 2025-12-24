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
import androidx.core.net.toUri
import com.meta.brain.R
import de.hdodenhof.circleimageview.CircleImageView

class LanguageAdapter(
    private val context: Context, 
    private val listLanguage: MutableList<LanguageModel>,
    private val callback: LanguageAdapterCallBack?,
    @LayoutRes private val customItemLayoutId: Int = 0
): RecyclerView.Adapter<LanguageAdapter.LanguageAdapterVH>() {

    var itemPosition: Int = -1
    
    inner class LanguageAdapterVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bgLayout: ConstraintLayout = itemView.findViewById(R.id.bg_layout)
        private val imgCircleChoose: ImageView = itemView.findViewById(R.id.imgCircleChoose)
        private val imgRoundChoose: ImageView = itemView.findViewById(R.id.imgRoundChoose)
        private val tvtNameCountry: TextView = itemView.findViewById(R.id.tvtNameCountry)
        private val countryImage: CircleImageView = itemView.findViewById(R.id.countryImage)
        
        fun onBind(languageModel: LanguageModel, position: Int) {
            itemView.setOnClickListener {
                languageModel.isSelected = !languageModel.isSelected
                if (itemPosition != position) {
                    notifyItemChanged(itemPosition)
                    itemPosition = position
                    notifyItemChanged(position)
                    callback?.onSelectLanguage(languageModel)
                }
            }
            
            if (itemPosition == position) {
                imgCircleChoose.visibility = View.VISIBLE
                imgRoundChoose.visibility = View.GONE
                bgLayout.setBackgroundResource(R.drawable.bg_language_item_selected)
            } else {
                imgCircleChoose.visibility = View.GONE
                imgRoundChoose.visibility = View.VISIBLE
                bgLayout.setBackgroundResource(R.drawable.bg_language_item_unselected)
            }
            
            tvtNameCountry.text = languageModel.name
            
            Glide.with(context)
                .load("file:///android_asset/flags/${languageModel.languageCode}.png".toUri())
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

    interface LanguageAdapterCallBack{
        fun onSelectLanguage(languageModel: LanguageModel)
    }
}