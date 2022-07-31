package dev.encelade.testutils

import dev.encelade.processing.Processor
import dev.encelade.processing.ProcessorStatus
import dev.encelade.utils.LazyLogging
import dev.encelade.utils.TimeUtils.formatMillis
import java.text.DecimalFormat
import java.time.Instant

class StatusLogger(private val processor: Processor) : Runnable, LazyLogging {

    private val df = DecimalFormat("##.##%")

    override fun run() {
        val start = Instant.now()

        while (processor.status != ProcessorStatus.FINISHED) {
            val now = Instant.now()
            val elapsedTimeMillis = now.toEpochMilli() - start.toEpochMilli()
            val millisForOnePercent = elapsedTimeMillis / (100 * processor.progress)
            val remainingPercents = (1 - processor.progress) * 100
            val eta = formatMillis((remainingPercents * millisForOnePercent).toLong())
            val fileName = processor.requestConfig.pdfFile.name
            logger.info("[${processor.status}] progress of $fileName: ${df.format(processor.progress)} / ETA: $eta")
            Thread.sleep(2 * 1000L)
        }
    }

}
