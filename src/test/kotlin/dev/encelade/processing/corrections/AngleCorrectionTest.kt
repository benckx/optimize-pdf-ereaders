package dev.encelade.processing.corrections

import com.google.common.base.CaseFormat.UPPER_CAMEL
import com.google.common.base.CaseFormat.UPPER_UNDERSCORE
import dev.encelade.ocr.model.Page
import dev.encelade.testutils.TestUtils.formatPageNum
import dev.encelade.testutils.TestUtils.loadPageFromImage
import dev.encelade.utils.LazyLogging
import dev.encelade.utils.Printer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class AngleCorrectionTest : LazyLogging {

    @ParameterizedTest
    @MethodSource("buildParameters")
    fun angleCorrectionTest(fileName: String, pageNum: Double, expectedCorrection: Double) {
        logger.info("fileName: $fileName")
        logger.info("pageNum: $pageNum")
        logger.info("angle [EXPECTED]: $expectedCorrection")

        val page = loadPageFromImage(fileName, pageNum)
        page.detectCorrectiveAngle()
        dumpPage(fileName, "BEFORE", page)

        val correctedPage = page.correctAngle()
        dumpPage(fileName, "FIXED", correctedPage)

        assertEquals(expectedCorrection, correctedPage.correctedAngleValue, 0.1)
    }

    private fun dumpPage(fileName: String, suffix: String, page: Page) {
        val testName = UPPER_CAMEL.to(UPPER_UNDERSCORE, javaClass.simpleName)
        val dumpFileName = testName + "_" + fileName.uppercase() + "_" + formatPageNum(page.idx.first()) + "_" + suffix
        Printer(page, dumpFileName).dumpToImageFile()
    }

    companion object {

        @JvmStatic
        fun buildParameters(): List<Arguments> {
            return listOf(
                Arguments.of("baudrillard", 17.0, 1.44),
                Arguments.of("baudrillard", 20.0, 0.46)
            )
        }

    }

}
