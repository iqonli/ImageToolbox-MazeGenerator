/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2026 T8RIN (Malik Mukhametzyanov)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/LICENSE-2.0>.
 */

package com.t8rin.imagetoolbox.core.settings.data.model

import com.t8rin.imagetoolbox.core.settings.domain.model.DomainFontFamily
import com.t8rin.imagetoolbox.core.settings.domain.model.FontType
import java.io.File

fun FontType?.toDomainFontFamily(): DomainFontFamily =
    when (this) {
        is FontType.File ->
            DomainFontFamily.Custom(
                name = File(path).nameWithoutExtension.replace("[:\\-_.,]".toRegex(), " "),
                filePath = path,
            )

        is FontType.Resource ->
            bundledFonts.entries.firstOrNull { it.value == resId }?.key ?: DomainFontFamily.System

        null -> DomainFontFamily.System
    }

fun DomainFontFamily?.toFontType(): FontType? =
    when (this) {
        is DomainFontFamily.Custom -> FontType.File(filePath)
        else -> bundledFonts[this]?.let { FontType.Resource(it) }
    }

// ezMaze uses the system font; retain the model conversion for persisted settings compatibility.
private val bundledFonts: Map<DomainFontFamily, Int> = emptyMap()
