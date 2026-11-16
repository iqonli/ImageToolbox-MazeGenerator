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
