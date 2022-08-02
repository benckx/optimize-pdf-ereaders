package dev.encelade.testutils

import com.google.common.base.CaseFormat
import com.google.common.base.CaseFormat.UPPER_CAMEL
import dev.encelade.ocr.model.Page
import dev.encelade.testutils.TestUtils.formatPageNum
import dev.encelade.utils.Printer
import java.io.File

interface PageDumpTestHelper {

    fun dumpPage(page: Page, fileName: String, suffix: String) {
        createFolderIfNotExists()

        val testName = UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, javaClass.simpleName)
        val dumFileNameBuilder = mutableListOf<String>()
        dumFileNameBuilder += testName
        dumFileNameBuilder += fileName.uppercase()
        dumFileNameBuilder += formatPageNum(page.idx.first()) + "_" + suffix
        val dumpFileName = OUTPUT_FOLDER + File.separator + dumFileNameBuilder.joinToString("_")
        Printer(page, dumpFileName).dumpToImageFile()
    }

    private companion object {

        const val OUTPUT_FOLDER = "test-output"

        fun createFolderIfNotExists() {
            val outputFolderFile = File(OUTPUT_FOLDER)
            if (!outputFolderFile.exists()) {
                outputFolderFile.mkdir()
            }
        }

    }

}
