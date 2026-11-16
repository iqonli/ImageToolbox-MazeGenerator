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

package com.t8rin.imagetoolbox.core.domain.saving

/** Preserve the configured name and its extension; suffix only when that name is occupied. */
fun uniqueFilename(filename: String, exists: (String) -> Boolean): String {
    require(filename.isNotBlank())
    if (!exists(filename)) return filename
    val dot = filename.lastIndexOf('.').takeIf { it > 0 } ?: filename.length
    val stem = filename.substring(0, dot)
    val extension = filename.substring(dot)
    var index = 1L
    while (true) {
        val candidate = "$stem ($index)$extension"
        if (!exists(candidate)) return candidate
        index++
    }
}
