package dev.encelade.testutils

import dev.encelade.ocr.OCR
import dev.encelade.ocr.model.Page
import dev.encelade.utils.LazyLogging
import junit.framework.TestCase.assertTrue
import java.awt.image.BufferedImage
import java.util.*

object TestUtils : LazyLogging {

    fun getIntegerPart(pageNum: Double): Int {
        return splitPageNum(pageNum)[0].toInt()
    }

    fun loadImageAsPage(inputImage: BufferedImage, pageNum: Double): Page {
        return loadImageAsPage(pageNum, inputImage)
    }

    private fun loadImageAsPage(pageNum: Double, inputImage: BufferedImage): Page {
        val split = splitPageNum(pageNum)
        logger.info(Arrays.asList(*split).toString())
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

    private fun splitPageNum(pageNum: Double): Array<String> {
        return java.lang.Double.toString(pageNum)
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
