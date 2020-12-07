package com.louis993546.daynightlivewallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.service.wallpaper.WallpaperService
import androidx.core.net.toUri

class MyWallpaperService : WallpaperService() {
    private val images: MainActivity.Images by lazy {
        getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).run {
            MainActivity.Images(
                day = getString(MainActivity.Origin.DAY.key, null)?.toUri() ?: Uri.EMPTY,
                night = getString(MainActivity.Origin.NIGHT.key, null)?.toUri() ?: Uri.EMPTY
            )
        }
    }

    private val getBitmap = { uri: Uri ->
        if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(contentResolver,  uri)
        } else {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        }
    }



    override fun onCreateEngine(): Engine = MyWallpaperEngine(images, getBitmap)

    inner class MyWallpaperEngine(
        val images: MainActivity.Images,
        val bitmapGetter: (Uri) -> Bitmap
    ) : Engine() {
        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                val canvas = surfaceHolder.lockCanvas()
                val bitmap = bitmapGetter(images.day)
                val centerX = (canvas.width - bitmap.width) / 2f
                val centerY = (canvas.height - bitmap.height) / 2f
                canvas.drawBitmap(bitmap, centerX, centerY, null)
            }
        }
    }
}