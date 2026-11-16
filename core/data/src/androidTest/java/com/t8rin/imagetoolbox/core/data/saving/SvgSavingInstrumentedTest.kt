/*
 * ezMaze, Copyright (c) 2026 IQ Online Studio
 * Licensed under the Apache License, Version 2.0.
 */
package com.t8rin.imagetoolbox.core.data.saving

import android.net.Uri
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.t8rin.imagetoolbox.core.data.resource.AndroidResourceManager
import com.t8rin.imagetoolbox.core.domain.coroutines.DispatchersHolder
import com.t8rin.imagetoolbox.core.domain.image.model.ImageInfo
import com.t8rin.imagetoolbox.core.domain.model.MimeType
import com.t8rin.imagetoolbox.core.domain.saving.RandomStringGenerator
import com.t8rin.imagetoolbox.core.domain.saving.io.use
import com.t8rin.imagetoolbox.core.domain.saving.model.FileSaveTarget
import com.t8rin.imagetoolbox.core.domain.saving.model.ImageSaveTarget
import com.t8rin.imagetoolbox.core.domain.saving.verifySavedFile
import com.t8rin.imagetoolbox.core.settings.domain.SettingsManager
import com.t8rin.imagetoolbox.core.settings.domain.model.FilenameBehavior
import com.t8rin.imagetoolbox.core.settings.domain.model.SettingsState
import java.lang.reflect.Proxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SvgSavingInstrumentedTest {
    @Test
    fun configuredSvgTemplatesKeepExactlyOneExtension() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settings =
            MutableStateFlow(
                SettingsState.Default.copy(
                    filenamePrefix = "My.custom.maze",
                    filenameSuffix = "",
                    filenameBehavior = FilenameBehavior.None(),
                    addTimestampToFilename = false,
                    addOriginalFilename = false,
                    addSizeInFilename = false,
                    addPresetInfoToFilename = false,
                    addImageScaleModeInfoToFilename = false,
                )
            )
        // Only the state getter is used by the filename creator; do not initialize app DI in a
        // library test.
        val manager =
            Proxy.newProxyInstance(
                SettingsManager::class.java.classLoader,
                arrayOf(SettingsManager::class.java),
            ) { _, method, _ ->
                check(method.name == "getSettingsState" && method.parameterCount == 0)
                settings
            } as SettingsManager
        val dispatchers =
            object : DispatchersHolder {
                override val uiDispatcher = Dispatchers.Main
                override val ioDispatcher = Dispatchers.IO
                override val encodingDispatcher = Dispatchers.Default
                override val decodingDispatcher = Dispatchers.Default
                override val defaultDispatcher = Dispatchers.Default
            }
        val creator =
            AndroidFilenameCreator(
                object : RandomStringGenerator {
                    override fun generate(length: Int) = "x".repeat(length)
                },
                context,
                manager,
                dispatchers,
                AndroidResourceManager(context),
            )
        val target =
            ImageSaveTarget(
                imageInfo = ImageInfo(800, 600),
                originalUri = "",
                sequenceNumber = null,
                data = "<svg/>".toByteArray(),
                mimeType = MimeType.Svg,
                extension = "svg",
            )
        for ((pattern, expected) in
            listOf(
                "\\p.\\e" to "My.custom.maze.svg",
                "Chosen.svg" to "Chosen.svg",
                "Chosen.SVG" to "Chosen.SVG",
                "Chosen" to "Chosen.svg",
            )) {
            settings.value = settings.value.copy(filenamePattern = pattern)
            assertEquals(expected, creator.constructImageFilename(target))
        }
    }

    @Test
    fun consecutiveSvgSavesPreserveCustomNamesAndReadableFiles() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val filename = "ezMaze.test.${System.nanoTime()}.svg"
        val created = mutableListOf<Uri>()
        val actualNames = mutableSetOf<String>()
        try {
            repeat(3) { index ->
                val data =
                    "<svg xmlns=\"http://www.w3.org/2000/svg\"><title>$index</title></svg>"
                        .toByteArray()
                val folder =
                    requireNotNull(
                        SavingFolder.getInstance(
                            context = context,
                            treeUri = null,
                            saveTarget = FileSaveTarget("", filename, data, MimeType.Svg, "svg"),
                            saveToOriginalFolder = false,
                        )
                    )
                created += folder.fileUri
                folder.use { it.writeBytes(data) }
                verifySavedFile(
                    data,
                    requireNotNull(resolver.openInputStream(folder.fileUri)),
                    "Incomplete SVG save",
                )
                resolver.query(folder.fileUri, arrayOf("_display_name"), null, null, null)!!.use {
                    cursor ->
                    assertTrue(cursor.moveToFirst())
                    val actual = cursor.getString(0)
                    assertTrue(actual.endsWith(".svg"))
                    assertTrue(actual.startsWith(filename.removeSuffix(".svg")))
                    if (index == 0) assertEquals(filename, actual)
                    assertTrue(
                        "Each save must have its own filename: $actual",
                        actualNames.add(actual),
                    )
                }
                assertEquals("image/svg+xml", resolver.getType(folder.fileUri))
            }
            assertEquals(3, created.toSet().size)
            // Earlier files must still contain their own document after later saves.
            created.forEachIndexed { index, uri ->
                resolver.openInputStream(uri)!!.use {
                    assertTrue(it.reader().readText().contains("<title>$index</title>"))
                }
            }
        } finally {
            created.forEach { resolver.delete(it, null, null) }
        }
    }
}
