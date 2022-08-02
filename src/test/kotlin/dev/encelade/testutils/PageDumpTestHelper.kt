package dev.encelade.testutils

import com.google.common.base.CaseFormat.UPPER_CAMEL
import com.google.common.base.CaseFormat.UPPER_UNDERSCORE
import dev.encelade.ocr.model.Page
import dev.encelade.testutils.TestUtils.formatPageNum
import dev.encelade.utils.Printer
import java.io.File

interface PageDumpTestHelper {

    fun dumpPage(page: Page, fileName: String, suffix: String) {
        createFolderIfNotExists()

        val testName = UPPER_CAMEL.to(UPPER_UNDERSCORE, javaClass.simpleName)
        val dumpFileNameBuilder = mutableListOf<String>()
        dumpFileNameBuilder += testName
        dumpFileNameBuilder += fileName.uppercase()
        dumpFileNameBuilder += page.idx.map { formatPageNum(it) }
        dumpFileNameBuilder += suffix
        val dumpFileName = OUTPUT_FOLDER + File.separator + dumpFileNameBuilder.joinToString("_")
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
