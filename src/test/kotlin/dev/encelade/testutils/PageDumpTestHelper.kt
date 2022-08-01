package dev.encelade.testutils

import com.google.common.base.CaseFormat
import com.google.common.base.CaseFormat.UPPER_CAMEL
import dev.encelade.ocr.model.Page
import dev.encelade.testutils.TestUtils.formatPageNum
import dev.encelade.utils.Printer

interface PageDumpTestHelper {

    fun dumpPage(page: Page, fileName: String, suffix: String) {
        val testName = UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, javaClass.simpleName)
        val dumpFileName = testName + "_" + fileName.uppercase() + "_" + formatPageNum(page.idx.first()) + "_" + suffix
        Printer(page, dumpFileName).dumpToImageFile()
    }

}
