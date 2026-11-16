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

package com.t8rin.imagetoolbox.core.ui.widget.dialogs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.t8rin.imagetoolbox.core.resources.Icons
import com.t8rin.imagetoolbox.core.resources.R
import com.t8rin.imagetoolbox.core.resources.icons.StadiaController
import com.t8rin.imagetoolbox.core.settings.presentation.provider.LocalSettingsState
import com.t8rin.imagetoolbox.core.ui.utils.helper.isPortraitOrientationAsState
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.BasicEnhancedAlertDialog
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.EnhancedButton
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.EnhancedIconButton
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.EnhancedLoadingIndicator
import com.t8rin.imagetoolbox.core.ui.widget.modifier.tappable

@Composable
fun LoadingDialog(
    visible: Boolean,
    onCancelLoading: () -> Unit = {},
    canCancel: Boolean = true,
    isForSaving: Boolean = true
) {
    LoadingDialogContent(
        visible = visible,
        onCancelLoading = onCancelLoading,
        canCancel = canCancel,
        isForSaving = isForSaving
    ) {
        EnhancedLoadingIndicator(modifier = Modifier.size(108.dp))
    }
}

@Composable
fun LoadingDialog(
    visible: Boolean,
    done: Int,
    left: Int,
    onCancelLoading: () -> Unit,
    canCancel: Boolean = true,
) {
    LoadingDialogContent(
        visible = visible,
        onCancelLoading = onCancelLoading,
        canCancel = canCancel && visible
    ) {
        if (left < 0) {
            EnhancedLoadingIndicator(modifier = Modifier.size(108.dp))
        } else {
            EnhancedLoadingIndicator(done = done, left = left)
        }
    }
}

@Composable
fun LoadingDialog(
    visible: Boolean,
    progress: () -> Float,
    onCancelLoading: () -> Unit = {},
    canCancel: Boolean = true,
    loaderSize: Dp = 60.dp,
    switchToIndicator: Boolean = false,
    isLayoutSwappable: Boolean = true,
    isForSaving: Boolean = true,
    additionalContent: @Composable (Dp) -> Unit = {}
) {
    val progress = progress()

    LoadingDialogContent(
        visible = visible,
        onCancelLoading = onCancelLoading,
        canCancel = canCancel,
        isForSaving = isForSaving
    ) {
        AnimatedContent(
            targetState = switchToIndicator || progress == 1f && visible && isLayoutSwappable,
            modifier = Modifier.fillMaxSize()
        ) { isIndicator ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isIndicator) {
                    EnhancedLoadingIndicator(modifier = Modifier.size(108.dp))
                } else {
                    EnhancedLoadingIndicator(
                        progress = progress,
                        loaderSize = loaderSize,
                        additionalContent = additionalContent
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingDialogContent(
    visible: Boolean,
    onCancelLoading: () -> Unit,
    canCancel: Boolean,
    isForSaving: Boolean = true,
    indicator: @Composable BoxScope.() -> Unit
) {
    var showWantDismissDialog by remember(canCancel, visible) { mutableStateOf(false) }
    BasicEnhancedAlertDialog(
        visible = visible,
        onDismissRequest = { showWantDismissDialog = canCancel },
        dismissOnClickOutside = canCancel,
        modifier = Modifier.keepScreenOn()
    ) {
        Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center, content = indicator)
    }
    WantCancelLoadingDialog(
        visible = visible && showWantDismissDialog,
        onCancelLoading = onCancelLoading,
        onDismissDialog = { showWantDismissDialog = false },
        isForSaving = isForSaving
    )
}
