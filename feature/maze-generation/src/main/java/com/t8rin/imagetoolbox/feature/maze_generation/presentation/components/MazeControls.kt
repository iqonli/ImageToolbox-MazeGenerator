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

package com.t8rin.imagetoolbox.feature.maze_generation.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.t8rin.imagetoolbox.core.resources.R
import com.t8rin.imagetoolbox.core.ui.utils.helper.Clipboard
import com.t8rin.imagetoolbox.core.ui.utils.provider.ProvideContainerDefaults
import com.t8rin.imagetoolbox.core.ui.widget.controls.selection.ColorRowSelector
import com.t8rin.imagetoolbox.core.ui.widget.controls.selection.DataSelector
import com.t8rin.imagetoolbox.core.ui.widget.controls.selection.SeedSelector
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.EnhancedButton
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.EnhancedSliderItem
import com.t8rin.imagetoolbox.core.ui.widget.modifier.ShapeDefaults
import com.t8rin.imagetoolbox.core.ui.widget.other.ExpandableItem
import com.t8rin.imagetoolbox.core.ui.widget.preferences.PreferenceRowSwitch
import com.t8rin.imagetoolbox.core.ui.widget.text.RoundedTextField
import com.t8rin.imagetoolbox.feature.maze_generation.domain.*
import com.t8rin.imagetoolbox.feature.maze_generation.presentation.screenLogic.MazeGenerationComponent
import kotlin.math.roundToInt

@Composable
internal fun MazeControls(component: MazeGenerationComponent) {
    val p = component.recipe
    val change = component::updateRecipe
    MazeSection(R.string.maze_structure, true) {
        Choice(
            p.topology,
            MazeTopology.entries,
            R.string.maze_topology,
            { it.title },
            component::setTopology,
        )
        if (p.topology != MazeTopology.Square)
            Text(
                stringResource(R.string.maze_square_algorithms),
                style = MaterialTheme.typography.bodySmall,
            )
        Choice(
            p.algorithm,
            MazeAlgorithm.entries.filter { it.supports(p.topology) },
            R.string.maze_algorithm,
            { it.title },
        ) {
            change(p.copy(algorithm = it))
        }
        Text(stringResource(p.algorithm.description), style = MaterialTheme.typography.bodySmall)
        NumberSlider(
            if (p.topology == MazeTopology.Polar) R.string.maze_rings else R.string.maze_rows,
            p.rows,
            2f..100f,
        ) {
            change(resizeMask(p, it, p.columns))
        }
        if (p.topology != MazeTopology.Polar)
            NumberSlider(R.string.maze_columns, p.columns, 2f..100f) {
                change(resizeMask(p, p.rows, it))
            }
        SeedSelector(
            value = p.seed,
            valueRange = -1000000f..1000000f,
            onValueChange = { change(p.copy(seed = it.roundToInt())) },
            enabled = !component.seedLocked,
        )
        Toggle(R.string.maze_lock_seed, component.seedLocked, component::setSeedLocked)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Action(R.string.maze_generate, component::newMaze)
            Action(R.string.maze_copy_seed) { Clipboard.copy(p.seed.toString()) }
            if (component.isImageLoading) Action(R.string.maze_cancel, component::cancelGeneration)
        }
        NumberSlider(R.string.maze_braid, p.braid, 0f..100f) { change(p.copy(braid = it)) }
        Toggle(R.string.maze_perfect, p.braid == 0) { change(p.copy(braid = if (it) 0 else 100)) }
        Text(stringResource(R.string.maze_braid_hint), style = MaterialTheme.typography.bodySmall)
        if (p.topology == MazeTopology.Mask) {
            Choice(
                p.mask,
                MazeMask.entries.filter { it != MazeMask.Image || p.imageMask.isNotEmpty() },
                R.string.maze_mask,
                { it.title },
            ) {
                change(p.copy(mask = it))
            }
            var threshold by rememberSaveable { mutableIntStateOf(128) }
            var invert by rememberSaveable { mutableStateOf(false) }
            var maskUri by remember { mutableStateOf<Uri?>(null) }
            val picker =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) {
                        maskUri = uri
                        component.importMask(uri, threshold, invert)
                    }
                }
            NumberSlider(R.string.maze_threshold, threshold, 0f..255f) { threshold = it }
            Toggle(R.string.maze_invert_mask, invert) { invert = it }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Action(R.string.maze_load_mask) { picker.launch(arrayOf("image/*")) }
                if (maskUri != null)
                    Action(R.string.maze_apply_mask) {
                        component.importMask(maskUri!!, threshold, invert)
                    }
            }
            Text(
                stringResource(R.string.maze_mask_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (
        p.algorithm in
            setOf(
                MazeAlgorithm.GrowingTree,
                MazeAlgorithm.BinaryTree,
                MazeAlgorithm.Sidewinder,
                MazeAlgorithm.Eller,
                MazeAlgorithm.Houston,
                MazeAlgorithm.OriginShift,
                MazeAlgorithm.RecursiveDivision,
            )
    ) {
        MazeSection(R.string.maze_algorithm_parameters) {
            when (p.algorithm) {
                MazeAlgorithm.GrowingTree -> {
                    Text(
                        stringResource(R.string.maze_weights_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    NumberSlider(R.string.maze_newest, p.newestWeight, 0f..100f) {
                        change(p.copy(newestWeight = it))
                    }
                    NumberSlider(R.string.maze_random, p.randomWeight, 0f..100f) {
                        change(p.copy(randomWeight = it))
                    }
                    NumberSlider(R.string.maze_oldest, p.oldestWeight, 0f..100f) {
                        change(p.copy(oldestWeight = it))
                    }
                    NumberSlider(R.string.maze_middle, p.middleWeight, 0f..100f) {
                        change(p.copy(middleWeight = it))
                    }
                }
                MazeAlgorithm.BinaryTree -> {
                    Choice(
                        p.direction,
                        MazeDirection.entries,
                        R.string.maze_direction,
                        { it.title },
                    ) {
                        change(p.copy(direction = it))
                    }
                    NumberSlider(R.string.maze_horizontal_bias, p.horizontalBias, 0f..100f) {
                        change(p.copy(horizontalBias = it))
                    }
                }
                MazeAlgorithm.Sidewinder ->
                    NumberSlider(R.string.maze_horizontal_bias, p.horizontalBias, 0f..100f) {
                        change(p.copy(horizontalBias = it))
                    }
                MazeAlgorithm.Eller -> {
                    NumberSlider(R.string.maze_eller_join, p.ellerJoin, 0f..100f) {
                        change(p.copy(ellerJoin = it))
                    }
                    NumberSlider(R.string.maze_eller_drop, p.ellerDrop, 0f..100f) {
                        change(p.copy(ellerDrop = it))
                    }
                }
                MazeAlgorithm.Houston ->
                    NumberSlider(R.string.maze_houston_switch, p.houstonSwitch, 0f..100f) {
                        change(p.copy(houstonSwitch = it))
                    }
                MazeAlgorithm.OriginShift ->
                    NumberSlider(R.string.maze_origin_iterations, p.originIterations, 1f..100f) {
                        change(p.copy(originIterations = it))
                    }
                MazeAlgorithm.RecursiveDivision -> {
                    NumberSlider(R.string.maze_horizontal_bias, p.horizontalBias, 0f..100f) {
                        change(p.copy(horizontalBias = it))
                    }
                    NumberSlider(R.string.maze_division_room, p.divisionRoom, 1f..20f) {
                        change(p.copy(divisionRoom = it))
                    }
                }
                else -> Unit
            }
        }
    }
    MazeSection(R.string.maze_endpoints) {
        Choice(p.entrance, MazeEntrance.entries, R.string.maze_entrance, { it.title }) {
            change(p.copy(entrance = it))
        }
        Text(
            stringResource(R.string.maze_farthest_hint),
            style = MaterialTheme.typography.bodySmall,
        )
        if (p.entrance == MazeEntrance.Manual) {
            val max = (component.statistics?.cells?.minus(1) ?: 1).coerceAtLeast(1).toFloat()
            NumberSlider(R.string.maze_start_cell, p.startCell.coerceIn(0, max.toInt()), 0f..max) {
                change(p.copy(startCell = it))
            }
            NumberSlider(R.string.maze_end_cell, p.endCell.coerceIn(0, max.toInt()), 0f..max) {
                change(p.copy(endCell = it))
            }
            Text(stringResource(R.string.maze_tap_hint), style = MaterialTheme.typography.bodySmall)
        }
        Toggle(R.string.maze_open_entrances, p.openEntrances) { change(p.copy(openEntrances = it)) }
        Toggle(R.string.maze_show_solution, p.style.showSolution) {
            change(p.copy(style = p.style.copy(showSolution = it)))
        }
        Toggle(R.string.maze_show_markers, p.style.showMarkers) {
            change(p.copy(style = p.style.copy(showMarkers = it)))
        }
        Toggle(R.string.maze_heatmap, p.style.heatmap) {
            change(p.copy(style = p.style.copy(heatmap = it)))
        }
    }
    MazeSection(R.string.maze_style) {
        FloatSlider(R.string.maze_wall_width, p.style.wallWidth, 0.01f..0.5f) {
            change(p.copy(style = p.style.copy(wallWidth = it)))
        }
        FloatSlider(R.string.maze_solution_width, p.style.solutionWidth, 0.02f..0.8f) {
            change(p.copy(style = p.style.copy(solutionWidth = it)))
        }
        FloatSlider(R.string.maze_marker_size, p.style.markerSize, 0.05f..0.6f) {
            change(p.copy(style = p.style.copy(markerSize = it)))
        }
        FloatSlider(R.string.maze_margin, p.style.margin, 0.1f..5f) {
            change(p.copy(style = p.style.copy(margin = it)))
        }
        Choice(p.style.cap, MazeCap.entries, R.string.maze_cap, { it.title }) {
            change(p.copy(style = p.style.copy(cap = it)))
        }
        Toggle(R.string.maze_transparent, p.style.transparent) {
            change(p.copy(style = p.style.copy(transparent = it)))
        }
        MazeColor(R.string.maze_wall_color, p.style.wallColor) {
            change(p.copy(style = p.style.copy(wallColor = it)))
        }
        MazeColor(R.string.maze_background_color, p.style.backgroundColor) {
            change(p.copy(style = p.style.copy(backgroundColor = it)))
        }
        MazeColor(R.string.maze_solution_color, p.style.solutionColor) {
            change(p.copy(style = p.style.copy(solutionColor = it)))
        }
        MazeColor(R.string.maze_start_color, p.style.startColor) {
            change(p.copy(style = p.style.copy(startColor = it)))
        }
        MazeColor(R.string.maze_end_color, p.style.endColor) {
            change(p.copy(style = p.style.copy(endColor = it)))
        }
    }
    MazeSection(R.string.maze_animation) {
        Toggle(
            R.string.maze_animate_solution,
            component.animateSolution,
            component::setAnimateSolution,
        )
        NumberSlider(
            R.string.maze_animation_speed,
            component.animationSpeed,
            20f..10000f,
            component::setAnimationSpeed,
        )
        FloatSlider(
            R.string.maze_animation_progress,
            component.animationPosition,
            0f..1f,
            component::seekAnimation,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Action(if (component.playing) R.string.maze_pause else R.string.maze_play) {
                if (component.playing) component.pauseAnimation() else component.playAnimation()
            }
            Action(R.string.maze_step, component::stepAnimation)
            Action(R.string.maze_restart) { component.seekAnimation(0f) }
            Action(R.string.maze_finish) { component.seekAnimation(1f) }
        }
        Text(
            stringResource(R.string.maze_animation_hint),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    MazeSection(R.string.maze_presets) {
        var name by rememberSaveable { mutableStateOf("") }
        RoundedTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.maze_preset_name),
            maxSymbols = 80,
        )
        Action(R.string.maze_save_preset) { component.savePreset(name) }
        component.presets.forEach { preset ->
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EnhancedButton(onClick = { change(preset.recipe) }) { Text(preset.name) }
                Action(R.string.maze_delete_preset) { component.deletePreset(preset.name) }
            }
        }
        val defaults =
            listOf(
                MazeRecipe(),
                MazeRecipe(
                    topology = MazeTopology.Hexagonal,
                    rows = 18,
                    columns = 18,
                    algorithm = MazeAlgorithm.Wilson,
                ),
                MazeRecipe(
                    topology = MazeTopology.Polar,
                    rows = 16,
                    algorithm = MazeAlgorithm.GrowingTree,
                ),
                MazeRecipe(
                    topology = MazeTopology.Mask,
                    mask = MazeMask.Heart,
                    rows = 40,
                    columns = 40,
                    braid = 30,
                ),
                MazeRecipe(algorithm = MazeAlgorithm.RecursiveDivision, rows = 40, columns = 40),
            )
        defaults.forEachIndexed { i, default ->
            EnhancedButton(onClick = { change(default.copy(seed = p.seed)) }) {
                Text(stringResource(R.string.maze_builtin_preset, i + 1))
            }
        }
        val import =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
                it?.let(component::importRecipe)
            }
        val export =
            rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json")
            ) {
                it?.let(component::exportRecipe)
            }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Action(R.string.maze_import_recipe) {
                import.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            }
            Action(R.string.maze_export_recipe) { export.launch("maze_${p.seed}.json") }
        }
    }
    component.statistics?.let { s ->
        MazeSection(R.string.maze_statistics, true) {
            Text(
                stringResource(
                    R.string.maze_extra_statistics,
                    s.passages - s.cells + 1,
                    s.deadEnds * 100 / s.cells,
                )
            )
            Text(
                stringResource(
                    R.string.maze_statistics_values,
                    s.cells,
                    s.passages,
                    s.deadEnds,
                    s.junctions,
                    s.solutionLength,
                    component.elapsedMillis,
                )
            )
        }
    }
}

private fun resizeMask(p: MazeRecipe, rows: Int, columns: Int): MazeRecipe {
    val mask =
        if (p.imageMask.size == p.rows * p.columns)
            List(rows * columns) { i ->
                val r = (i / columns * p.rows / rows).coerceAtMost(p.rows - 1)
                val c = (i % columns * p.columns / columns).coerceAtMost(p.columns - 1)
                p.imageMask[r * p.columns + c]
            }
        else emptyList()
    return p.copy(rows = rows, columns = columns, imageMask = mask)
}

@Composable
private fun MazeSection(
    title: Int,
    expanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    ExpandableItem(
        initialState = expanded,
        visibleContent = {
            Text(
                stringResource(title),
                Modifier.padding(8.dp),
                style = MaterialTheme.typography.titleSmall,
            )
        },
        expandableContent = {
            ProvideContainerDefaults(color = MaterialTheme.colorScheme.surface) {
                Column(
                    Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    content = content,
                )
            }
        },
    )
}

@Composable
private fun <T : Any> Choice(
    value: T,
    entries: List<T>,
    title: Int,
    label: (T) -> Int,
    onChange: (T) -> Unit,
) {
    DataSelector(
        value = value,
        entries = entries,
        title = stringResource(title),
        titleIcon = null,
        itemContentText = { stringResource(label(it)) },
        onValueChange = onChange,
        spanCount = 2,
        shape = ShapeDefaults.center,
    )
}

@Composable
private fun NumberSlider(
    title: Int,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Int) -> Unit,
) {
    EnhancedSliderItem(
        value = value,
        title = stringResource(title),
        valueRange = range,
        internalStateTransformation = { it.roundToInt() },
        onValueChange = { onChange(it.roundToInt()) },
        shape = ShapeDefaults.center,
    )
}

@Composable
private fun FloatSlider(
    title: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    EnhancedSliderItem(
        value = value,
        title = stringResource(title),
        valueRange = range,
        internalStateTransformation = { (it * 100).roundToInt() / 100f },
        onValueChange = { onChange((it * 100).roundToInt() / 100f) },
        shape = ShapeDefaults.center,
    )
}

@Composable
private fun Toggle(title: Int, value: Boolean, onChange: (Boolean) -> Unit) {
    PreferenceRowSwitch(
        title = stringResource(title),
        checked = value,
        onClick = onChange,
        shape = ShapeDefaults.center,
    )
}

@Composable
private fun Action(title: Int, onClick: () -> Unit) {
    EnhancedButton(onClick = onClick) { Text(stringResource(title)) }
}

@Composable
private fun MazeColor(title: Int, value: Int, onChange: (Int) -> Unit) {
    ColorRowSelector(
        value = Color(value),
        onValueChange = { onChange(it.toArgb()) },
        title = stringResource(title),
    )
}
