package com.meta.brain.module.language

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import androidx.core.net.toUri
import com.meta.brain.R
import com.meta.brain.databinding.LanguageItemBinding
import com.meta.brain.module.base.BaseViewHolder

class LanguageAdapter(
    private val context: Context, 
    private val listLanguage: MutableList<LanguageModel>,
    private val callback: LanguageAdapterCallBack?
): RecyclerView.Adapter<LanguageAdapter.LanguageAdapterVH>() {

    var itemPosition: Int = 0
    inner class LanguageAdapterVH(binding: LanguageItemBinding) : BaseViewHolder<LanguageItemBinding>(binding){
        fun onBind(languageModel: LanguageModel, position: Int){
            binding.root.setOnClickListener {
                languageModel.isSelected = !languageModel.isSelected
                if(itemPosition != position){
                    notifyItemChanged(itemPosition)
                    itemPosition = position
                    notifyItemChanged(position)
                    callback?.onSelectLanguage(languageModel)
                }
            }
            if(itemPosition == position){
                binding.imgCircleChoose.visibility = View.VISIBLE
                binding.imgRoundChoose.visibility = View.GONE
                binding.bgLayout.setBackgroundResource(R.drawable.bg_language_item_selected)
            }else{
                binding.imgCircleChoose.visibility = View.GONE
                binding.imgRoundChoose.visibility = View.VISIBLE
                binding.bgLayout.setBackgroundResource(R.drawable.bg_language_item_unselected)
            }
            binding.tvtNameCountry.text = languageModel.name
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
                .into(binding.countryImage)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageAdapterVH {
        return LanguageAdapterVH(
            LanguageItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
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