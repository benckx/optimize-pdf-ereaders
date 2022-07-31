package dev.encelade.processing.corrections

import dev.encelade.utils.LazyLogging
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class AngleCorrectionTest : LazyLogging {

    @ParameterizedTest
    @MethodSource("buildParameters")
    fun angleCorrectionTest(fileName: String, pageNum: Double, expectedCorrection: Double) {
        logger.info("filePath:          $fileName")
        logger.info("pageNum:           $pageNum")
        logger.info("angle [EXPECTED]:  $expectedCorrection")
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
