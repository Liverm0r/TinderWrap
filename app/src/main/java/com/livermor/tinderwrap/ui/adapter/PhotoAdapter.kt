package com.livermor.tinderwrap.ui.adapter

import android.annotation.SuppressLint
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.livermor.tinderwrap.UiPhoto
import com.livermor.tinderwrap.databinding.ItemPhotoBinding
import com.livermor.tinderwrap.factory.color

class PhotoAdapter() : ViewBindingDelegateAdapter<UiPhoto, ItemPhotoBinding>(ItemPhotoBinding::inflate) {

    @SuppressLint("ClickableViewAccessibility")
    override fun ItemPhotoBinding.onBind(item: UiPhoto) {
        Log.i(PhotoAdapter::class.java.simpleName, "onBind: trying to load image")
        val context = ivPhoto.context
        Glide
            .with(context)
            .load(item.url)
            .apply(requestOptions)
            .into(ivPhoto)

        vIndicator.setBackgroundColor(item.type.color())
    }

    override fun isForViewType(item: Any) = item is UiPhoto
    override fun UiPhoto.getItemId() = id

    companion object {
        private val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
    }
}