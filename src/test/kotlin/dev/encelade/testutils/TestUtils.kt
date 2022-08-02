package dev.encelade.testutils

import com.google.common.io.Resources
import dev.encelade.ocr.OCR
import dev.encelade.ocr.model.Page
import dev.encelade.utils.LazyLogging
import junit.framework.TestCase.assertTrue
import org.apache.commons.lang3.StringUtils
import java.awt.image.BufferedImage
import java.io.File
import java.io.File.separator
import javax.imageio.ImageIO

object TestUtils : LazyLogging {

    fun formatPageNum(pageNum: Int): String {
        return StringUtils.leftPad(pageNum.toString(), 4, '0')
    }

    fun loadPageFromImage(fileName: String, pageNum: Double): Page {
        val imageFilePath = "images" + separator + fileName + separator + formatPageNum(pageNum.toInt()) + ".png"
        val imageFile = File(Resources.getResource(imageFilePath).file)
        val image = ImageIO.read(imageFile)
        return loadImageAsPage(image, pageNum)
    }

    private fun loadImageAsPage(inputImage: BufferedImage, pageNum: Double): Page {
        val split = splitPageNum(pageNum)
        logger.info(split.toList().toString())
        assert(split.size == 2)
        val integerPart = getIntegerPart(pageNum)
        when (split[1].toInt()) {
            0 -> {
                val page = OCR().analyze(inputImage, integerPart)
                page.detectTextBlocks()
                return page
            }

            1 -> return getLeftPage(inputImage, integerPart)
            2 -> return getRightPage(inputImage, integerPart)
        }
        throw IllegalArgumentException("pageNum not parsable -> $pageNum")
    }

    private fun getIntegerPart(pageNum: Double): Int {
        return splitPageNum(pageNum)[0].toInt()
    }

    private fun splitPageNum(pageNum: Double): Array<String> {
        return pageNum
            .toString()
            .split("\\.".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()
    }

    private fun getLeftPage(image: BufferedImage, pageNum: Int): Page {
        return getLayoutPage(image, pageNum, 0)
    }

    private fun getRightPage(image: BufferedImage, pageNum: Int): Page {
        return getLayoutPage(image, pageNum, 1)
    }

    private fun getLayoutPage(image: BufferedImage, pageNum: Int, i: Int): Page {
        val page = OCR().analyze(image, getIntegerPart(pageNum.toDouble()))
        assertTrue(page.detectTwoPagesLayout())
        val split = page.splitForLayout()
        val splitPage = split[i]
        splitPage.detectTextBlocks()
        return splitPage
    }

}
