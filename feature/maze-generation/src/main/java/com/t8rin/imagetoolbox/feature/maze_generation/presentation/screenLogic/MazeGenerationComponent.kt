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

package com.t8rin.imagetoolbox.feature.maze_generation.presentation.screenLogic

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import com.arkivanov.decompose.ComponentContext
import com.t8rin.imagetoolbox.core.domain.coroutines.DispatchersHolder
import com.t8rin.imagetoolbox.core.domain.image.ImageCompressor
import com.t8rin.imagetoolbox.core.domain.image.ImageShareProvider
import com.t8rin.imagetoolbox.core.domain.image.model.ImageFormat
import com.t8rin.imagetoolbox.core.domain.image.model.ImageInfo
import com.t8rin.imagetoolbox.core.domain.image.model.Quality
import com.t8rin.imagetoolbox.core.domain.model.ColorModel
import com.t8rin.imagetoolbox.core.domain.model.IntegerSize
import com.t8rin.imagetoolbox.core.domain.model.MimeType
import com.t8rin.imagetoolbox.core.domain.saving.FileController
import com.t8rin.imagetoolbox.core.domain.saving.model.FileSaveTarget
import com.t8rin.imagetoolbox.core.domain.saving.model.ImageSaveTarget
import com.t8rin.imagetoolbox.core.domain.saving.model.SaveResult
import com.t8rin.imagetoolbox.core.settings.domain.SettingsManager
import com.t8rin.imagetoolbox.core.ui.utils.BaseHistoryComponent
import com.t8rin.imagetoolbox.core.ui.utils.helper.AppToastHost
import com.t8rin.imagetoolbox.core.ui.utils.navigation.Screen
import com.t8rin.imagetoolbox.feature.maze_generation.data.MazePreset
import com.t8rin.imagetoolbox.feature.maze_generation.data.MazeRecipeStore
import com.t8rin.imagetoolbox.feature.maze_generation.domain.*
import com.t8rin.imagetoolbox.feature.maze_generation.rendering.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class MazeGenerationComponent
@AssistedInject
internal constructor(
    @Assisted componentContext: ComponentContext,
    @Assisted val onGoBack: () -> Unit,
    @Assisted val onNavigate: (Screen) -> Unit,
    dispatchersHolder: DispatchersHolder,
    private val fileController: FileController,
    private val shareProvider: ImageShareProvider<Bitmap>,
    private val imageCompressor: ImageCompressor<Bitmap>,
    private val settingsManager: SettingsManager,
    private val store: MazeRecipeStore,
) :
    BaseHistoryComponent<MazeGenerationComponent.HistorySnapshot>(
        dispatchersHolder,
        componentContext,
    ) {
    var recipe by mutableStateOf(MazeRecipe())
        private set

    var outputSize by mutableStateOf(IntegerSize(2048, 2048))
        private set

    var imageFormat by mutableStateOf(ImageFormat.Default)
        private set

    var quality by mutableStateOf<Quality>(Quality.Base(100))
        private set

    var svg by mutableStateOf(false)
        private set

    var previewBitmap by mutableStateOf<Bitmap?>(null)
        private set

    var statistics by mutableStateOf<MazeStatistics?>(null)
        private set

    var elapsedMillis by mutableStateOf(0L)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var presets by mutableStateOf<List<MazePreset>>(emptyList())
        private set

    var seedLocked by mutableStateOf(false)
        private set

    var playing by mutableStateOf(false)
        private set

    var animationPosition by mutableStateOf(1f)
        private set

    var animationSpeed by mutableStateOf(1000)
        private set

    var animateSolution by mutableStateOf(false)
        private set

    private var revision = 0
    private var generationJob: Job? = null
    private var animationJob: Job? = null
    private var savingJob: Job? = null
    private var persistenceJob: Job? = null
    private var cached: Pair<MazeRecipe, MazeResult>? = null
    private var currentSolution: MazeSolution? = null

    init {
        resetHistory()
        componentScope.launch {
            val last = store.loadLast()
            presets = store.presets()
            if (revision == 0) {
                recipe = last
                resetHistory()
                updatePreview()
            }
        }
    }

    private inline fun change(block: () -> Unit) {
        beginPendingHistoryTransaction()
        block()
        registerChanges()
        schedulePendingHistoryCommit()
    }

    fun updateRecipe(value: MazeRecipe) {
        if (value == recipe) return
        if (!value.algorithm.supports(value.topology)) {
            error =
                com.t8rin.imagetoolbox.core.utils.getString(
                    com.t8rin.imagetoolbox.core.resources.R.string.maze_incompatible
                )
            return
        }
        try {
            value.validated()
        } catch (exception: IllegalArgumentException) {
            error = exception.message ?: exception.javaClass.simpleName
            return
        }
        change { recipe = value }
        updatePreview()
        persistenceJob?.cancel()
        persistenceJob =
            componentScope.launch {
                delay(500)
                store.saveLast(recipe)
            }
    }

    fun setTopology(value: MazeTopology) = updateRecipe(recipe.copy(topology = value))

    fun updateSeedLocked(value: Boolean) {
        seedLocked = value
    }

    fun newMaze() {
        if (!seedLocked)
            updateRecipe(recipe.copy(seed = kotlin.random.Random.nextInt(-1000000, 1000001)))
        else updatePreview()
    }

    fun setWidth(value: Int) {
        val width = value.coerceIn(16, 8192).coerceAtMost(32_000_000 / outputSize.height)
        change { outputSize = outputSize.copy(width = width) }
        updatePreview()
    }

    fun setHeight(value: Int) {
        val height = value.coerceIn(16, 8192).coerceAtMost(32_000_000 / outputSize.width)
        change { outputSize = outputSize.copy(height = height) }
        updatePreview()
    }

    fun updateImageFormat(value: ImageFormat) {
        change { imageFormat = value }
    }

    fun updateQuality(value: Quality) {
        change { quality = value }
    }

    fun updateSvg(value: Boolean) {
        change { svg = value }
    }

    fun dismissError() {
        error = null
    }

    fun recipeJson() = store.encode(recipe)

    private fun updatePreview() {
        revision++
        val ticket = revision
        val input = recipe
        val size = outputSize
        generationJob?.cancel()
        pauseAnimation()
        animationPosition = 1f
        _isImageLoading.value = true
        generationJob =
            componentScope.launch {
                try {
                    delay(200)
                    val cache = cached
                    val result =
                        withContext(defaultDispatcher) {
                            if (cache?.first == input.graphKey()) cache.second
                            else MazeGenerator.generate(input)
                        }
                    val (solution, bitmap) =
                        withContext(defaultDispatcher) {
                            val solution = MazeSolver.solve(result, input)
                            val scene = MazeSceneBuilder.build(result, solution, input)
                            solution to preview(scene, size)
                        }
                    ensureActive()
                    if (ticket == revision) {
                        cached = input.graphKey() to result
                        currentSolution = solution
                        previewBitmap = bitmap
                        statistics = MazeSolver.statistics(result, solution)
                        elapsedMillis = result.elapsedMillis
                        error = null
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    if (ticket == revision) {
                        error = t.message ?: t.javaClass.simpleName
                        previewBitmap = null
                        statistics = null
                    }
                } finally {
                    if (ticket == revision) _isImageLoading.value = false
                }
            }
    }

    private suspend fun preview(scene: MazeScene, size: IntegerSize): Bitmap {
        val scale = 768f / max(size.width, size.height)
        return MazeBitmapRenderer.render(
            scene,
            (size.width * scale).roundToInt().coerceAtLeast(1),
            (size.height * scale).roundToInt().coerceAtLeast(1),
        )
    }

    fun cancelGeneration() {
        revision++
        generationJob?.cancel()
        _isImageLoading.value = false
    }

    fun pauseAnimation() {
        animationJob?.cancel()
        animationJob = null
        playing = false
    }

    fun updateAnimationSpeed(value: Int) {
        animationSpeed = value.coerceIn(20, 10000)
    }

    fun updateAnimateSolution(value: Boolean) {
        pauseAnimation()
        animateSolution = value
        seekAnimation(0f)
    }

    fun stepAnimation() {
        val total =
            if (animateSolution) (currentSolution?.path?.size ?: 1)
            else (cached?.second?.events?.size ?: 1)
        seekAnimation((animationPosition + 1f / total.coerceAtLeast(1)).coerceAtMost(1f))
    }

    fun seekAnimation(value: Float) {
        pauseAnimation()
        animate(value.coerceIn(0f, 1f), false)
    }

    fun playAnimation() {
        pauseAnimation()
        animate(if (animationPosition >= 1f) 0f else animationPosition, true)
    }

    private fun animate(from: Float, play: Boolean) {
        val result = cached?.takeIf { it.first == recipe.graphKey() }?.second ?: return
        val solution = currentSolution ?: return
        if (isImageLoading) return
        val input = recipe
        val size = outputSize
        val solutionMode = animateSolution
        val ticket = revision
        animationJob =
            componentScope.launch {
                playing = play
                try {
                    val total =
                        if (solutionMode) solution.path.size.coerceAtLeast(1)
                        else result.events.size.coerceAtLeast(1)
                    var index = (from * total).toInt()
                    val links = result.initialPassages.copyOf()
                    suspend fun apply(begin: Int, end: Int) {
                        for (i in begin until end.coerceAtMost(result.events.size)) {
                            if (i % 1024 == 0)
                                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                            val event = result.events[i]
                            links[abs(event) - 1] = event > 0
                        }
                    }
                    if (!solutionMode) withContext(defaultDispatcher) { apply(0, index) }
                    while (true) {
                        ensureActive()
                        val progress = index.toFloat() / total
                        val bitmap =
                            withContext(defaultDispatcher) {
                                val effective =
                                    input.copy(
                                        style =
                                            input.style.copy(
                                                showSolution =
                                                    solutionMode ||
                                                        (index == total && input.style.showSolution)
                                            )
                                    )
                                preview(
                                    MazeSceneBuilder.build(
                                        result,
                                        solution,
                                        effective,
                                        if (solutionMode) result.passages else links,
                                        if (solutionMode) progress else 1f,
                                    ),
                                    size,
                                )
                            }
                        ensureActive()
                        if (ticket != revision) break
                        previewBitmap = bitmap
                        animationPosition = progress
                        if (!play || index >= total) break
                        delay(33)
                        val next =
                            (index + (animationSpeed / 30).coerceAtLeast(1)).coerceAtMost(total)
                        if (!solutionMode) withContext(defaultDispatcher) { apply(index, next) }
                        index = next
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    error = t.message ?: t.javaClass.simpleName
                } finally {
                    playing = false
                }
            }
    }

    /** Manual indices can be set accessibly with controls; tapping selects the nearest cell. */
    fun selectCell(x: Float, y: Float, start: Boolean) {
        val result = cached?.takeIf { it.first == recipe.graphKey() }?.second ?: return
        val solution = currentSolution ?: return
        val scene = MazeSceneBuilder.build(result, solution, recipe)
        val scale = minOf(outputSize.width / scene.width, outputSize.height / scene.height)
        val px =
            (x * outputSize.width - (outputSize.width - scene.width * scale) / 2) / scale +
                scene.minX
        val py =
            (y * outputSize.height - (outputSize.height - scene.height * scale) / 2) / scale +
                scene.minY
        val cell =
            result.graph.cells.indices.minBy { i ->
                val c = result.graph.cells[i].center
                (c.x - px) * (c.x - px) + (c.y - py) * (c.y - py)
            }
        updateRecipe(
            if (start) recipe.copy(entrance = MazeEntrance.Manual, startCell = cell)
            else recipe.copy(entrance = MazeEntrance.Manual, endCell = cell)
        )
    }

    private data class Export(
        val recipe: MazeRecipe,
        val size: IntegerSize,
        val format: ImageFormat,
        val quality: Quality,
        val svg: Boolean,
    )

    private suspend fun scene(export: Export): MazeScene =
        withContext(defaultDispatcher) {
            val result =
                cached?.takeIf { it.first == export.recipe.graphKey() }?.second
                    ?: MazeGenerator.generate(export.recipe)
            MazeSceneBuilder.build(result, MazeSolver.solve(result, export.recipe), export.recipe)
        }

    private fun export(action: suspend (Export, MazeScene) -> Unit) {
        if (isSaving) return
        val snapshot = Export(recipe, outputSize, imageFormat, quality, svg)
        isSaving = true
        savingJob =
            componentScope.launch {
                try {
                    action(snapshot, scene(snapshot))
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    error = t.message ?: t.javaClass.simpleName
                    parseSaveResult(SaveResult.Error.Exception(t))
                } finally {
                    isSaving = false
                }
            }
    }

    fun saveMaze(oneTimeSaveLocationUri: String?) = export { e, scene ->
        val target =
            if (e.svg) {
                val data =
                    withContext(defaultDispatcher) {
                        MazeSvgRenderer.render(
                                scene,
                                e.size.width,
                                e.size.height,
                                store.encode(e.recipe),
                            )
                            .toByteArray()
                    }
                FileSaveTarget("", "maze_${e.recipe.seed}", data, MimeType.Svg, "svg")
            } else {
                val bitmap =
                    withContext(defaultDispatcher) {
                        MazeBitmapRenderer.render(scene, e.size.width, e.size.height)
                    }
                try {
                    ImageSaveTarget(
                        imageInfo =
                            ImageInfo(
                                width = bitmap.width,
                                height = bitmap.height,
                                quality = e.quality,
                                imageFormat = e.format,
                            ),
                        metadata = null,
                        originalUri = "",
                        sequenceNumber = null,
                        data =
                            imageCompressor.compress(
                                image = bitmap,
                                imageFormat = e.format,
                                quality = e.quality,
                            ),
                    )
                } finally {
                    bitmap.recycle()
                }
            }
        parseSaveResult(
            fileController
                .save(
                    saveTarget = target,
                    keepOriginalMetadata = false,
                    oneTimeSaveLocationUri = oneTimeSaveLocationUri,
                )
                .onSuccess {
                    if (
                        recipe == e.recipe &&
                            outputSize == e.size &&
                            imageFormat == e.format &&
                            quality == e.quality &&
                            svg == e.svg
                    )
                        registerSave()
                }
        )
    }

    fun cacheCurrentMaze(onComplete: (Uri) -> Unit) = export { e, scene ->
        val uri =
            if (e.svg) {
                val bytes =
                    withContext(defaultDispatcher) {
                        MazeSvgRenderer.render(
                                scene,
                                e.size.width,
                                e.size.height,
                                store.encode(e.recipe),
                            )
                            .toByteArray()
                    }
                shareProvider.cacheByteArray(bytes, "maze_${e.recipe.seed}.svg")
            } else {
                val bitmap =
                    withContext(defaultDispatcher) {
                        MazeBitmapRenderer.render(scene, e.size.width, e.size.height)
                    }
                try {
                    shareProvider.cacheImage(
                        bitmap,
                        ImageInfo(
                            width = bitmap.width,
                            height = bitmap.height,
                            imageFormat = e.format,
                            quality = e.quality,
                        ),
                    )
                } finally {
                    bitmap.recycle()
                }
            }
        uri?.let { onComplete(it.toUri()) }
    }

    fun shareMaze() = cacheCurrentMaze { uri ->
        componentScope.launch {
            shareProvider.shareUri(uri.toString(), onComplete = AppToastHost::showConfetti)
        }
    }

    fun cancelSaving() {
        savingJob?.cancel()
    }

    fun savePreset(name: String) {
        val title = name.trim().take(80)
        if (title.isEmpty()) return
        componentScope.launch {
            presets =
                (presets.filterNot { it.name == title } + MazePreset(title, recipe)).takeLast(50)
            store.savePresets(presets)
        }
    }

    fun deletePreset(name: String) {
        componentScope.launch {
            presets = presets.filterNot { it.name == name }
            store.savePresets(presets)
        }
    }

    fun importRecipe(uri: Uri) {
        componentScope.launch {
            try {
                updateRecipe(store.decode(store.read(uri)))
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                error = t.message ?: t.javaClass.simpleName
            }
        }
    }

    fun exportRecipe(uri: Uri) {
        val value = recipeJson()
        componentScope.launch {
            try {
                store.write(uri, value.toByteArray())
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                error = t.message ?: t.javaClass.simpleName
            }
        }
    }

    fun importMask(uri: Uri, threshold: Int, invert: Boolean) {
        val input = recipe
        componentScope.launch {
            try {
                val mask = store.sampleMask(uri, input.rows, input.columns, threshold, invert)
                if (recipe != input) return@launch
                updateRecipe(
                    input.copy(
                        topology = MazeTopology.Mask,
                        mask = MazeMask.Image,
                        imageMask = mask,
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                error = t.message ?: t.javaClass.simpleName
            }
        }
    }

    override fun currentHistorySnapshot() =
        HistorySnapshot(
            recipe,
            outputSize,
            imageFormat,
            quality,
            svg,
            settingsManager.settingsState.value.backgroundForNoAlphaImageFormats,
        )

    override fun applyHistorySnapshot(snapshot: HistorySnapshot) {
        recipe = snapshot.recipe
        outputSize = snapshot.size
        imageFormat = snapshot.format
        quality = snapshot.quality
        svg = snapshot.svg
        restoreBackgroundColorForNoAlphaFormats(settingsManager, snapshot.background)
        updatePreview()
        persistenceJob?.cancel()
        persistenceJob = componentScope.launch { store.saveLast(recipe) }
    }

    data class HistorySnapshot(
        val recipe: MazeRecipe,
        val size: IntegerSize,
        val format: ImageFormat,
        val quality: Quality,
        val svg: Boolean,
        val background: ColorModel,
    )

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            onGoBack: () -> Unit,
            onNavigate: (Screen) -> Unit,
        ): MazeGenerationComponent
    }
}
