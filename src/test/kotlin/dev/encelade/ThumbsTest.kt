package dev.encelade

import dev.encelade.utils.LazyLogging
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.streams.toList

/**
 * Generate the files used in the README.
 */
class ThumbsTest : LazyLogging {

    private val inputFolder = "thumbs"
    private val outputFolder = "thumbs-test-output"

    init {
        val outputFolderFile = File(outputFolder)
        if (outputFolderFile.exists()) {
            outputFolderFile.delete()
        }
        outputFolderFile.mkdir()
    }

    @Test
    fun thumbTest() {
        val inputs = Files
            .list(Paths.get(inputFolder))
            .toList()
            .filter { path -> path.name.endsWith("_extract.pdf") }
            .mapNotNull { path -> path.toFile() }

        inputs.forEach { file -> logger.info(file.name) }
    }

}
