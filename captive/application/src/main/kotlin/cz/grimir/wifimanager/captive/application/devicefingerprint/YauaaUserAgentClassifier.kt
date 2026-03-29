package cz.grimir.wifimanager.captive.application.devicefingerprint

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import nl.basjes.parse.useragent.UserAgent
import nl.basjes.parse.useragent.UserAgentAnalyzer
import org.springframework.stereotype.Service
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

@Service
open class YauaaUserAgentClassifier : UserAgentClassifier {
    private val analyzerRef = AtomicReference<UserAgentAnalyzer?>()
    private lateinit var initializationExecutor: ExecutorService

    @PostConstruct
    fun startAsyncInitialization() {
        initializationExecutor = createInitializationExecutor()
        initializationExecutor.execute {
            try {
                analyzerRef.set(buildAnalyzer())
                logger.info { "Yauaa user-agent analyzer initialized" }
            } catch (ex: Exception) {
                logger.error(ex) { "Failed to initialize Yauaa user-agent analyzer; user-agent extraction will be skipped" }
            }
        }
    }

    @PreDestroy
    fun shutdown() {
        if (::initializationExecutor.isInitialized) {
            initializationExecutor.shutdownNow()
        }
        analyzerRef.set(null)
    }

    override fun classify(userAgent: String?): String? {
        val value = userAgent?.trim().orEmpty()
        if (value.isBlank()) {
            return null
        }

        val analyzer = analyzerRef.get() ?: return null
        val parsed = analyzer.parse(value)

        val agentClass = normalizedField(parsed, "AgentClass")
        val agentName = normalizedField(parsed, "AgentName")
        val agentVersionMajor = normalizedField(parsed, "AgentVersionMajor")
        val operatingSystemName = normalizedField(parsed, "OperatingSystemName")
        val webviewAppName = normalizedField(parsed, "WebviewAppName")
        val webviewAppVersionMajor = normalizedField(parsed, "WebviewAppVersionMajor")

        val isAndroid = operatingSystemName?.contains("android", ignoreCase = true) == true
        val isIos =
            operatingSystemName?.let {
                it.contains("ios", ignoreCase = true) ||
                    it.contains("iphone", ignoreCase = true) ||
                    it.contains("ipad", ignoreCase = true)
            } == true
        val isWebview =
            agentClass.equals("Browser Webview", ignoreCase = true) ||
                !webviewAppName.isNullOrBlank()

        if (isWebview) {
            val version = webviewAppVersionMajor ?: agentVersionMajor
            return when {
                isAndroid && version != null -> "AndroidWebView/$version"
                isIos && version != null -> "IOSWebView/$version"
                else -> null
            }
        }

        val family =
            when {
                agentName.equals("Edge", ignoreCase = true) -> "Edge"
                agentName.equals("Chrome", ignoreCase = true) -> "Chrome"
                agentName.equals("Firefox", ignoreCase = true) -> "Firefox"
                agentName.equals("Safari", ignoreCase = true) -> "Safari"
                else -> null
            }

        return if (family != null && agentVersionMajor != null) "$family/$agentVersionMajor" else null
    }

    protected open fun createInitializationExecutor(): ExecutorService =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "yauaa-initializer").apply { isDaemon = true }
        }

    protected open fun buildAnalyzer(): UserAgentAnalyzer =
        UserAgentAnalyzer
            .newBuilder()
            .hideMatcherLoadStats()
            .withCache(256)
            .withField("AgentClass")
            .withField("AgentName")
            .withField("AgentVersionMajor")
            .withField("AgentNameVersionMajor")
            .withField("OperatingSystemName")
            .withField("OperatingSystemVersionMajor")
            .withField("WebviewAppName")
            .withField("WebviewAppVersionMajor")
            .build()

    private fun normalizedField(
        agent: UserAgent,
        fieldName: String,
    ): String? =
        agent
            .getValue(fieldName)
            ?.trim()
            ?.takeIf {
                it.isNotBlank() &&
                    !it.equals("Unknown", ignoreCase = true) &&
                    it != "??"
            }
            ?.lowercase(Locale.ROOT)
            ?.let { normalized ->
                when (fieldName) {
                    "AgentName", "OperatingSystemName", "WebviewAppName", "AgentClass" ->
                        agent.getValue(fieldName)?.trim()?.takeIf { original -> original.isNotBlank() && !original.equals("Unknown", ignoreCase = true) && original != "??" }

                    else -> normalized
                }
            }
}
