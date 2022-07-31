package dev.encelade.processing.corrections

import dev.encelade.testutils.PageDumpHelper
import dev.encelade.testutils.TestUtils.loadPageFromImage
import dev.encelade.utils.LazyLogging
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class AngleCorrectionTest : PageDumpHelper, LazyLogging {

    @ParameterizedTest
    @MethodSource("buildParameters")
    fun angleCorrectionTest(fileName: String, pageNum: Double, expectedCorrection: Double) {
        logger.info("fileName: $fileName")
        logger.info("pageNum: $pageNum")
        logger.info("angle [EXPECTED]: $expectedCorrection")

        val page = loadPageFromImage(fileName, pageNum)
        page.detectCorrectiveAngle()
        dumpPage(page, fileName, "ANALYZED")

        val correctedPage = page.correctAngle()
        dumpPage(correctedPage, fileName, "CORRECTED")

        assertEquals(expectedCorrection, correctedPage.correctedAngleValue, 0.1)
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
