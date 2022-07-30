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
import java.awt.Color
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
        val outputFolderFile = File(OUTPUT_FOLDER)
        if (outputFolderFile.exists()) {
            outputFolderFile.deleteRecursively()
        }
        outputFolderFile.mkdir()
    }

    @Test
    fun generateThumbsFolder() {
        val inputFiles = Files
            .list(Paths.get(INPUT_FOLDER))
            .toList()
            .filter { path -> path.name.endsWith("_extract.pdf") }
            .mapNotNull { path -> path.toFile() }
            .sortedBy { file -> file.name }

        inputFiles
            .forEach { inputFile ->
                // write thumbs of input file
                writeThumbs(inputFile, "input_page")

                // process the file with the lib
                val requestConfig = RequestConfig.builder().pdfFile(inputFile)
                val processor = Processor(requestConfig)
                processor.process()
                processor.joinThread()

                // write the output
                val outputFile = File(OUTPUT_FOLDER + File.separator + canonicalFileName(inputFile) + "_output.pdf")
                writeByteArrayToFile(outputFile, processor.writeToByteArray())

                // write thumbs of output file
                writeThumbs(outputFile, "output_page")
            }
    }

    private companion object : LazyLogging {

        const val INPUT_FOLDER = "thumbs"
        const val OUTPUT_FOLDER = "thumbs-test-output"
        const val THUMBS_WIDTH = 190

        fun writeThumbs(pdfFile: File, suffix: String) {
            return PDDocument.load(pdfFile)
                .use { doc ->
                    val pdfRenderer = PDFRenderer(doc)
                    IntRange(1, doc.numberOfPages)
                        .forEach { pageNum ->
                            logger.debug("rendering page $pageNum of ${pdfFile.name} to BufferedImage...")
                            val thumbFileName = canonicalFileName(pdfFile) + "_" + suffix + "_" + pageNum + ".jpg"
                            val thumbFilePath = OUTPUT_FOLDER + File.separator + thumbFileName
                            val pageAsImage = pdfRenderer.renderImageWithDPI(pageNum - 1, 150f, ImageType.RGB)
                            val thumbImage = addBorder(resizeToThumb(pageAsImage))
                            ImageIO.write(thumbImage, "jpg", File(thumbFilePath))
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

        fun addBorder(image: BufferedImage): BufferedImage {
            val newImage = BufferedImage(image.width, image.height, image.type)
            val g2d = newImage.createGraphics()
            g2d.drawImage(image, 0, 0, null)
            g2d.color = Color.BLACK
            g2d.drawRect(0, 0, image.width - 1, image.height - 1)
            g2d.dispose()

            return newImage
        }

        fun canonicalFileName(pdfFile: File): String {
            return pdfFile.name.split("_").first()
        }

    }

}
