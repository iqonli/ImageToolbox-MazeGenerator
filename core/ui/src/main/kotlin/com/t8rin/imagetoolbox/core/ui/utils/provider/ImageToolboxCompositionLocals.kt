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

package com.t8rin.imagetoolbox.core.ui.utils.provider
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import com.t8rin.imagetoolbox.core.settings.presentation.model.UiSettingsState
import com.t8rin.imagetoolbox.core.settings.presentation.provider.LocalSettingsState
import com.t8rin.imagetoolbox.core.settings.presentation.provider.LocalEditPresetsController
import com.t8rin.imagetoolbox.core.settings.presentation.provider.rememberEditPresetsController
import com.t8rin.imagetoolbox.core.ui.theme.ImageToolboxThemeSurface
import com.t8rin.imagetoolbox.core.ui.utils.confetti.ConfettiHost
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.rememberEnhancedHapticFeedback
import com.t8rin.imagetoolbox.core.ui.widget.other.ToastHost
@Composable
fun ImageToolboxCompositionLocals(settingsState: UiSettingsState, content: @Composable BoxScope.() -> Unit) {
 CompositionLocalProvider(
  LocalSettingsState provides settingsState,
  LocalEditPresetsController provides rememberEditPresetsController(),
  LocalHapticFeedback provides rememberEnhancedHapticFeedback(settingsState.hapticsStrength),
  LocalScreenSize provides rememberScreenSize()
 ) {
  ImageToolboxThemeSurface { content(); ConfettiHost(); ToastHost() }
 }
}
@Composable fun currentScreenTwoToneIcon(default: ImageVector): ImageVector = default
