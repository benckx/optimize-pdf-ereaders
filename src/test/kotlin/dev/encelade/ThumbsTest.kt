package dev.encelade

import dev.encelade.processing.Processor
import dev.encelade.processing.RequestConfig
import dev.encelade.utils.ImageUtils
import dev.encelade.utils.LazyLogging
import org.apache.commons.io.FileUtils.writeByteArrayToFile
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
    fun generateThumbsFolder() {
        val inputFiles = Files
            .list(Paths.get(inputFolder))
            .toList()
            .filter { path -> path.name.endsWith("_extract.pdf") }
            .mapNotNull { path -> path.toFile() }
            .sortedBy { file -> file.name }

        inputFiles
            .forEach { inputFile ->
                // write thumbs of input file
                writeThumbs(inputFile, "_input_page_")

                // process the file with the lib
                val requestConfig = RequestConfig.builder().pdfFile(inputFile)
                val processor = Processor(requestConfig)
                processor.process()
                processor.joinThread()

                // write the output
                val outputFile = File(outputFolder + File.separator + cleanFileName(inputFile) + "_output.pdf")
                writeByteArrayToFile(outputFile, processor.writeToByteArray())

                // write thumbs of output file
                writeThumbs(outputFile, "_output_page_")
            }
    }

    private companion object : LazyLogging {

        const val inputFolder = "thumbs"
        const val outputFolder = "thumbs-test-output"
        const val THUMBS_WIDTH = 150

        fun writeThumbs(pdfFile: File, suffix: String) {
            pdfToThumbs(pdfFile)
                .forEachIndexed { index, image ->
                    val fileName = cleanFileName(pdfFile) + suffix + (index + 1) + ".jpg"
                    val filePath = outputFolder + File.separator + fileName
                    ImageIO.write(image, "jpg", File(filePath))
                }
        }

        fun pdfToThumbs(pdfFile: File, imageType: ImageType = ImageType.RGB): List<BufferedImage> {
            return PDDocument.load(pdfFile)
                .use { doc ->
                    val pdfRenderer = PDFRenderer(doc)
                    IntRange(1, doc.numberOfPages)
                        .map { pageIdx ->
                            logger.debug("rendering page $pageIdx of ${pdfFile.name} to BufferedImage...")
                            resizeToThumb(pdfRenderer.renderImageWithDPI(pageIdx - 1, 300f, imageType))
                        }
                }
        }

        fun resizeToThumb(image: BufferedImage): BufferedImage {
            val height = image.height
            val width = image.width
            val ratio = THUMBS_WIDTH.toFloat() / width.toFloat()
            val newHeight = height.toFloat() * ratio
            return ImageUtils.resize(image, THUMBS_WIDTH, newHeight.toInt(), image.type)
        }

        fun cleanFileName(pdfFile: File): String {
            return pdfFile.name.split("_").first()
        }

    }

}
