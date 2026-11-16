/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2024 T8RIN (Malik Mukhametzyanov)
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

package com.t8rin.imagetoolbox.core.ui.utils.helper

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import com.t8rin.imagetoolbox.core.domain.utils.Flavor
import com.t8rin.imagetoolbox.core.resources.BuildConfig

val AppActivityClass: Class<*> by lazy { Class.forName("com.iqonli.ezmaze.MazeActivity") }

// No embedded gallery is shipped in ezMaze. Existing picker clients use Android's document UI.
fun createMediaPickerIntent(
    context: Context,
    allowMultiple: Boolean,
    currentAccent: Color,
    imageExtension: String,
): Intent =
    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        type = "image/$imageExtension"
        addCategory(Intent.CATEGORY_OPENABLE)
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )
    }

val AppVersionPreRelease: String by lazy {
    BuildConfig.VERSION_NAME.replace(BuildConfig.FLAVOR, "")
        .split("-")
        .takeIf { it.size > 1 }
        ?.drop(1)
        ?.first()
        ?.takeWhile { it.isLetter() } ?: ""
}

val AppVersionPreReleaseFlavored: String by lazy {
    if (!Flavor.isFoss()) {
            AppVersionPreRelease
        } else {
            "${BuildConfig.FLAVOR} $AppVersionPreRelease"
        }
        .uppercase()
}

val AppVersion: String by lazy { BuildConfig.VERSION_NAME + if (Flavor.isFoss()) "-foss" else "" }

const val ColorSchemeName = "scheme"
