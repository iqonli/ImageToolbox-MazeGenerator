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

package com.t8rin.imagetoolbox.feature.maze_generation.data

import com.t8rin.imagetoolbox.core.domain.saving.uniqueFilename
import com.t8rin.imagetoolbox.core.domain.saving.verifySavedFile
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SvgSaveContractTest {
    @Test
    fun customNameAndExtensionArePreserved() {
        assertEquals("My maze.svg", uniqueFilename("My maze.svg") { false })
        assertEquals("My.maze (1).SVG", uniqueFilename("My.maze.SVG") { it == "My.maze.SVG" })
        assertEquals("maze (1)", uniqueFilename("maze") { it == "maze" })
    }

    @Test
    fun consecutiveSavesKeepEveryPreviousFile() {
        val files = mutableSetOf<String>()
        repeat(4) { files += uniqueFilename("configured.svg", files::contains) }
        assertEquals(
            setOf(
                "configured.svg",
                "configured (1).svg",
                "configured (2).svg",
                "configured (3).svg",
            ),
            files,
        )
    }

    @Test
    fun svgBytesAreVerifiedAfterWriting() {
        val bytes = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".toByteArray()
        verifySavedFile(bytes, bytes.inputStream(), "write incomplete")
        assertThrows(IOException::class.java) {
            verifySavedFile(
                bytes,
                bytes.dropLast(1).toByteArray().inputStream(),
                "write incomplete",
            )
        }
        assertThrows(IOException::class.java) {
            verifySavedFile(bytes, (bytes + 0).inputStream(), "write incomplete")
        }
        assertThrows(IOException::class.java) {
            verifySavedFile(bytes, "wrong data".byteInputStream(), "write incomplete")
        }
    }
}
