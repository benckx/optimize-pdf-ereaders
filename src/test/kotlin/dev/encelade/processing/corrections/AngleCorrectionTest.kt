package dev.encelade.processing.corrections

import com.google.common.base.CaseFormat.UPPER_CAMEL
import com.google.common.base.CaseFormat.UPPER_UNDERSCORE
import com.google.common.io.Resources
import dev.encelade.ocr.model.Page
import dev.encelade.testutils.TestUtils
import dev.encelade.testutils.TestUtils.loadImageAsPage
import dev.encelade.utils.LazyLogging
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import javax.imageio.ImageIO

class AngleCorrectionTest : LazyLogging {

    @ParameterizedTest
    @MethodSource("buildParameters")
    fun angleCorrectionTest(fileName: String, pageNum: Double, expectedCorrection: Double) {
        logger.info("fileName: $fileName")
        logger.info("pageNum: $pageNum")
        logger.info("angle [EXPECTED]: $expectedCorrection")

        val formattedPageNum = StringUtils.leftPad(pageNum.toInt().toString(), 4, '0')
        val imageFilePath = "images" + File.separator + fileName + File.separator + formattedPageNum + ".png"
        val imageFile = File(Resources.getResource(imageFilePath).file)
        val image = ImageIO.read(imageFile)
        val page = loadImageAsPage(image, pageNum)
        page.detectCorrectiveAngle()
        val correctedPage = page.correctAngle()

        dumpPage(fileName, "BEFORE", page)
        dumpPage(fileName, "FIXED", correctedPage)

        assertEquals(expectedCorrection, correctedPage.correctedAngleValue, 0.1)
    }

    private fun dumpPage(fileName: String, prefix: String, page: Page) {
        val dumpFileName =
            fileName.uppercase() +
                    "_" + prefix +
                    "_" + UPPER_CAMEL.to(UPPER_UNDERSCORE, javaClass.simpleName) + "_"

        TestUtils.dumpPage(dumpFileName, page)
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
