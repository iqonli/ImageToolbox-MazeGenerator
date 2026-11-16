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

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.t8rin.imagetoolbox.core.di.entryPoint
import com.t8rin.imagetoolbox.core.resources.R
import com.t8rin.imagetoolbox.core.settings.di.SettingsStateEntryPoint
import com.t8rin.imagetoolbox.core.settings.presentation.provider.LocalSettingsState
import com.t8rin.imagetoolbox.core.ui.widget.controls.selection.DataSelector
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.*
import com.t8rin.imagetoolbox.core.ui.widget.modifier.ShapeDefaults
import com.t8rin.imagetoolbox.core.ui.widget.preferences.PreferenceRowSwitch
import com.t8rin.imagetoolbox.core.ui.widget.text.RoundedTextField
import com.t8rin.imagetoolbox.core.utils.appContext
import kotlinx.coroutines.launch

@Composable
fun MazePreferencesDialog(visible: Boolean, onDismiss: () -> Unit) {
    val manager = remember { appContext.entryPoint<SettingsStateEntryPoint>().settingsManager }
    val scope = rememberCoroutineScope()
    val settings = LocalSettingsState.current
    var prefix by remember(visible) { mutableStateOf(manager.settingsState.value.filenamePrefix) }
    var suffix by remember(visible) { mutableStateOf(manager.settingsState.value.filenameSuffix) }
    var pattern by
        remember(visible) { mutableStateOf(manager.settingsState.value.filenamePattern.orEmpty()) }
    EnhancedAlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ezmaze_preferences)) },
        confirmButton = {
            EnhancedButton(
                onClick = {
                    scope.launch {
                        manager.setFilenamePrefix(prefix)
                        manager.setFilenameSuffix(suffix)
                        manager.setFilenamePattern(pattern.ifBlank { null })
                        onDismiss()
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth().enhancedVerticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val language = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                DataSelector(
                    value = language,
                    entries = listOf("", "en", "zh-CN", "zh-TW"),
                    title = stringResource(R.string.language),
                    titleIcon = null,
                    itemContentText = {
                        when (it) {
                            "en" -> stringResource(R.string.ezmaze_english)
                            "zh-CN" -> stringResource(R.string.ezmaze_simplified)
                            "zh-TW" -> stringResource(R.string.ezmaze_traditional)
                            else -> stringResource(R.string.ezmaze_system)
                        }
                    },
                    onValueChange = {
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(it)
                        )
                    },
                    shape = ShapeDefaults.top,
                )
                RoundedTextField(
                    value = prefix,
                    onValueChange = { prefix = it },
                    label = stringResource(R.string.ezmaze_prefix),
                    modifier = Modifier.fillMaxWidth(),
                )
                RoundedTextField(
                    value = suffix,
                    onValueChange = { suffix = it },
                    label = stringResource(R.string.ezmaze_suffix),
                    modifier = Modifier.fillMaxWidth(),
                )
                RoundedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = stringResource(R.string.ezmaze_filename_pattern),
                    modifier = Modifier.fillMaxWidth(),
                )
                PreferenceRowSwitch(
                    title = stringResource(R.string.ezmaze_timestamp),
                    checked = settings.addTimestampToFilename,
                    startIcon = null,
                    onClick = { scope.launch { manager.toggleAddTimestampToFilename() } },
                    shape = ShapeDefaults.center,
                )
                Text(stringResource(R.string.ezmaze_credits))
            }
        },
    )
}
