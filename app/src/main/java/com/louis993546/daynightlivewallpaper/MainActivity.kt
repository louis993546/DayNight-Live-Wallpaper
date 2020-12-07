package com.louis993546.daynightlivewallpaper

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.louis993546.daynightlivewallpaper.ui.DayNightLiveWallpaperTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

public const val SP_NAME = "DayNightLiveWallpaper"

class MainActivity : AppCompatActivity() {
    private val imagesFlow = MutableStateFlow(Images(Uri.EMPTY, Uri.EMPTY))
    private val onChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            Origin.DAY.key -> {
                lifecycleScope.launch {
                    val uri = sharedPreferences.getString(key, null)?.toUri() ?: Uri.EMPTY
                    Log.d("qqqq 1", uri.toString())
                    imagesFlow.emit(imagesFlow.value.copy(day = uri))
                }
            }
            Origin.NIGHT.key -> {
                lifecycleScope.launch {
                    val uri = sharedPreferences.getString(key, null)?.toUri() ?: Uri.EMPTY
                    Log.d("qqqq 2", uri.toString())
                    imagesFlow.emit(imagesFlow.value.copy(night = uri))
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).run {
            val currentDay = getString(Origin.DAY.key, null)?.toUri() ?: Uri.EMPTY
            val currentNight = getString(Origin.NIGHT.key, null)?.toUri() ?: Uri.EMPTY
            lifecycleScope.launch {
                imagesFlow.emit(Images(currentDay, currentNight))
            }
            registerOnSharedPreferenceChangeListener(onChangeListener)
        }

        setContent {
            DayNightLiveWallpaperTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    val state = imagesFlow.collectAsState()
                    App(
                        state,
                        onDayClicked = { openGalleryIntent(Origin.DAY.code) },
                        onNightClicked = { openGalleryIntent(Origin.NIGHT.code) },
                        getBitmap = {
                            if (Build.VERSION.SDK_INT < 28) {
                                MediaStore.Images.Media.getBitmap(contentResolver, this)
                            } else {
                                val source = ImageDecoder.createSource(contentResolver, this)
                                ImageDecoder.decodeBitmap(source)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun openGalleryIntent(code: Int) {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_OPEN_DOCUMENT
        }
        startActivityForResult(
            Intent.createChooser(intent, "Choose image"),
            code
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            Origin.DAY.code -> processData(Origin.DAY, data)
            Origin.NIGHT.code -> processData(Origin.NIGHT, data)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun processData(origin: Origin, data: Intent?) {
        val uri = data?.data ?: Uri.EMPTY
        processSelectedImage(origin, uri)
    }

    private fun processSelectedImage(origin: Origin, uri: Uri) {
        getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).edit().run {
            putString(origin.key, uri.toString())
            apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(onChangeListener)
    }

    enum class Origin(val key: String, val code: Int) {
        DAY("day", 0),
        NIGHT("night", 1)
    }

    data class Images(
        val day: Uri,
        val night: Uri
    )
}

@Composable
fun App(
    imagesState: State<MainActivity.Images>,
    onDayClicked: () -> Unit,
    onNightClicked: () -> Unit,
    getBitmap: Uri.() -> Bitmap,
) {
    Column {
        WallpaperBlock(
            modifier = Modifier.weight(1f),
            uri = imagesState.value.day,
            getBitmap = getBitmap
        ) {
            onDayClicked()
        }
        WallpaperBlock(
            modifier = Modifier.weight(1f),
            uri = imagesState.value.night,
            getBitmap = getBitmap
        ) {
            onNightClicked()
        }
    }
}

@Composable
fun WallpaperBlock(
    modifier: Modifier = Modifier,
    uri: Uri,
    getBitmap: Uri.() -> Bitmap,
    onClick: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize().clickable(onClick = onClick)) {
        if (uri == Uri.EMPTY) {
            BasicText(text = "No image is selected")
        } else {
            Image(
                bitmap = uri.getBitmap().asImageBitmap(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DayNightLiveWallpaperTheme {
        Greeting("Android")
    }
}