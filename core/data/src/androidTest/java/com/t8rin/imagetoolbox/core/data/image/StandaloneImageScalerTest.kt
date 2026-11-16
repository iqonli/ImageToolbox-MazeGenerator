/*
 * ezMaze, Copyright (c) 2026 IQ Online Studio
 * Licensed under the Apache License, Version 2.0.
 */
package com.t8rin.imagetoolbox.core.data.image

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import com.t8rin.imagetoolbox.core.domain.coroutines.DispatchersHolder
import com.t8rin.imagetoolbox.core.domain.image.model.ImageScaleMode
import com.t8rin.imagetoolbox.core.domain.image.model.ResizeType
import com.t8rin.imagetoolbox.core.settings.domain.SettingsProvider
import com.t8rin.imagetoolbox.core.settings.domain.model.SettingsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StandaloneImageScalerTest {
    @Test
    fun sharedFitResizeKeepsItsBlurredBackgroundWithoutTheEditorModule() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dispatchers =
            object : DispatchersHolder {
                override val uiDispatcher = Dispatchers.Main
                override val ioDispatcher = Dispatchers.IO
                override val encodingDispatcher = Dispatchers.Default
                override val decodingDispatcher = Dispatchers.Default
                override val defaultDispatcher = Dispatchers.Default
            }
        val settings =
            object : SettingsProvider {
                override val settingsState = MutableStateFlow(SettingsState.Default)

                override suspend fun getSettingsState() = settingsState.value
            }
        val loader = ImageLoader.Builder(context).build()
        val scaler =
            AndroidImageScaler(
                settings,
                AndroidImageTransformer(context, loader, dispatchers),
                dispatchers,
            )
        val source =
            Bitmap.createBitmap(10, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        try {
            val result =
                scaler.scaleImage(
                    source,
                    200,
                    100,
                    ResizeType.Fit(canvasColor = null),
                    ImageScaleMode.Default,
                )
            try {
                assertEquals(200, result.width)
                assertEquals(100, result.height)
                assertEquals(255, Color.alpha(result.getPixel(0, 0)))
                assertEquals(Color.RED, result.getPixel(100, 50))
            } finally {
                if (result !== source) result.recycle()
            }
        } finally {
            source.recycle()
            loader.shutdown()
        }
    }
}
