package dev.encelade.processing.corrections

import dev.encelade.testutils.PageDumpTestHelper
import dev.encelade.testutils.TestUtils.loadPageFromImage
import dev.encelade.utils.LazyLogging
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class AngleCorrectionTest : PageDumpTestHelper, LazyLogging {

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

        assertEquals(expectedCorrection, correctedPage.correctedAngleValue, 0.05)
    }

    companion object {

        @JvmStatic
        fun buildParameters(): List<Arguments> {
            return listOf(
                Arguments.of("baudrillard", 3.0, 0),
                Arguments.of("baudrillard", 5.0, 0.88),
                Arguments.of("baudrillard", 8.0, -1.07),
                Arguments.of("baudrillard", 9.0, 0.69),
                Arguments.of("baudrillard", 17.0, 1.49),
                Arguments.of("baudrillard", 20.0, 0.46),
                Arguments.of("baudrillard", 22.0, -0.84),
                Arguments.of("baudrillard", 23.0, 1.37),
                Arguments.of("baudrillard", 29.0, 1.83),
                Arguments.of("baudrillard", 41.0, 3.23)
            )
        }

    }

}
