/*
 * ezMaze is an independent maze generator based on Image Toolbox
 * Copyright (c) 2026 IQ Online Studio
 * Based on Image Toolbox, Copyright (c) 2024-2026 T8RIN (Malik Mukhametzyanov)
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

package com.iqonli.ezmaze

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arkivanov.decompose.retainedComponent
import com.t8rin.imagetoolbox.core.resources.Icons
import com.t8rin.imagetoolbox.core.resources.R
import com.t8rin.imagetoolbox.core.resources.icons.Settings
import com.t8rin.imagetoolbox.core.settings.presentation.model.toUiState
import com.t8rin.imagetoolbox.core.ui.utils.ComposeActivity
import com.t8rin.imagetoolbox.core.ui.utils.provider.ImageToolboxCompositionLocals
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.EnhancedIconButton
import com.t8rin.imagetoolbox.feature.maze_generation.presentation.MazeGenerationContent
import com.t8rin.imagetoolbox.feature.maze_generation.presentation.screenLogic.MazeGenerationComponent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MazeActivity : ComposeActivity() {
    @Inject lateinit var factory: MazeGenerationComponent.Factory
    private val component by lazy { retainedComponent { factory(it) } }

    override fun onFirstLaunch() {
        super.onFirstLaunch()
        if (
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1,
            )
        }
    }

    @Composable
    override fun Content() {
        ImageToolboxCompositionLocals(settingsState.toUiState()) {
            var preferences by remember { mutableStateOf(false) }
            MazeGenerationContent(
                component,
                onGoBack = ::finish,
                extraActions = {
                    EnhancedIconButton(onClick = { preferences = true }) {
                        Icon(Icons.Rounded.Settings, stringResource(R.string.ezmaze_preferences))
                    }
                },
            )
            MazePreferencesDialog(preferences) { preferences = false }
        }
    }
}
