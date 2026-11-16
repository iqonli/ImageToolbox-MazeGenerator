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

package com.t8rin.imagetoolbox.core.resources

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

fun Context.getHelpString(@StringRes id: Int): String {
    return getString(id)
}

fun Context.getHelpString(@StringRes id: Int, vararg formatArgs: Any): String =
    String.format(resources.configuration.locales[0], getHelpString(id), *formatArgs)

@Composable
fun helpStringResource(@StringRes id: Int): String {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration, id) { context.getHelpString(id) }
}
