package dev.encelade.processing.corrections

import com.google.common.io.Resources
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
        val filePath = "images" + File.separator + fileName + File.separator + formattedPageNum + ".png"
        val imageFile = File(Resources.getResource(filePath).file)
        val image = ImageIO.read(imageFile)
        val page = loadImageAsPage(image, pageNum)
        page.detectCorrectiveAngle()

        assertEquals(expectedCorrection, page.correctAngle().correctedAngleValue, 0.1)
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
