package dev.encelade

import dev.encelade.utils.ImageUtils
import dev.encelade.utils.LazyLogging
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.io.path.name
import kotlin.streams.toList

/**
 * Generate the files used in the README.
 */
class ThumbsTest : LazyLogging {

    init {
        val outputFolderFile = File(outputFolder)
        if (outputFolderFile.exists()) {
            outputFolderFile.deleteRecursively()
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

        inputs.forEach { file ->
            logger.info(file.name)
            writeThumbs(file, "_input_page_")
        }
    }

    private companion object : LazyLogging {

        const val inputFolder = "thumbs"
        const val outputFolder = "thumbs-test-output"
        const val THUMBS_WIDTH = 150

        fun writeThumbs(pdfFile: File, suffix: String) {
            pdfToImages(pdfFile)
                .forEachIndexed { index, image ->
                    val fileName = pdfFile.name.replace("_extract.pdf_", "_") + suffix + index + ".jpg"
                    val filePath = outputFolder + File.separator + fileName
                    ImageIO.write(resize(image), "jpg", File(filePath))
                }
        }

        fun pdfToImages(pdfFile: File, imageType: ImageType = ImageType.RGB): List<BufferedImage> {
            return PDDocument
                .load(pdfFile)
                .use { doc ->
                    val pdfRenderer = PDFRenderer(doc)
                    IntRange(1, doc.numberOfPages)
                        .map { pageIdx ->
                            logger.debug("loading $pageIdx of ${pdfFile.name}...")
                            pdfRenderer.renderImageWithDPI(pageIdx - 1, 300f, imageType)
                        }
                }
        }

        fun resize(image: BufferedImage): BufferedImage {
            val height = image.height
            val width = image.width
            val ratio = THUMBS_WIDTH.toFloat() / width.toFloat()
            val newHeight = height.toFloat() * ratio
            return ImageUtils.resize(image, THUMBS_WIDTH, newHeight.toInt(), image.type)
        }

    }

}
