package cz.grimir.wifimanager.captive.application.devicefingerprint

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class YauaaUserAgentClassifierTest {
    @Test
    fun `returns null while async initialization is still in progress`() {
        val latch = CountDownLatch(1)
        val classifier =
            object : YauaaUserAgentClassifier() {
                override fun buildAnalyzer(): nl.basjes.parse.useragent.UserAgentAnalyzer {
                    latch.await(5, TimeUnit.SECONDS)
                    return super.buildAnalyzer()
                }
            }

        classifier.startAsyncInitialization()

        assertNull(classifier.classify(WINDOWS_CHROME))

        latch.countDown()
        classifier.shutdown()
    }

    @Test
    fun `classifies windows chrome to coarse family major`() {
        val classifier = readyClassifier()

        assertEquals("Chrome/136", classifier.classify(WINDOWS_CHROME))

        classifier.shutdown()
    }

    @Test
    fun `classifies edge distinctly from chrome`() {
        val classifier = readyClassifier()

        assertEquals("Edge/136", classifier.classify(WINDOWS_EDGE))

        classifier.shutdown()
    }

    @Test
    fun `classifies iphone safari to coarse safari major`() {
        val classifier = readyClassifier()

        assertEquals("Safari/18", classifier.classify(IPHONE_SAFARI))

        classifier.shutdown()
    }

    @Test
    fun `generic ambiguous ua does not fall back to mozilla`() {
        val classifier = readyClassifier()

        assertNull(classifier.classify("Mozilla/5.0"))

        classifier.shutdown()
    }

    private fun readyClassifier(): YauaaUserAgentClassifier {
        val classifier = YauaaUserAgentClassifier()
        classifier.startAsyncInitialization()
        repeat(100) {
            val value = classifier.classify(WINDOWS_CHROME)
            if (value != null) {
                return classifier
            }
            Thread.sleep(50)
        }
        error("Yauaa analyzer did not initialize in time")
    }

    companion object {
        private const val WINDOWS_CHROME =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
        private const val WINDOWS_EDGE =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36 Edg/136.0.0.0"
        private const val IPHONE_SAFARI =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1"
    }
}
