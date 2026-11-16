package uk.gov.communities.prsdb.webapp.performance

import java.net.URI
import java.net.URISyntaxException
import java.time.Duration

enum class PerformanceTarget(
    val propertyValue: String,
) {
    LOCAL("local"),
    NFT("nft"),
}

enum class MeasurementMode(
    val propertyValue: String,
) {
    BASELINE("baseline"),
    GATED("gated"),
}

data class ResponseTimeLimits(
    val meanMs: Int,
    val maxMs: Int,
)

data class BasicRunConfig(
    val target: PerformanceTarget,
    val mode: MeasurementMode,
    val baseUrl: String,
    val simulatorUrl: String,
    val landlordSubject: String,
    val limits: ResponseTimeLimits?,
    val iterations: Int,
    val maxIterations: Int,
    val certificateDownloadOrigin: String? = null,
    val thinkTime: Duration = Duration.ZERO,
) {
    companion object {
        fun from(properties: Map<String, String>): BasicRunConfig {
            val targetValue = requiredProperty(properties, "gatling.target")
            val target =
                PerformanceTarget.entries.singleOrNull { it.propertyValue == targetValue }
                    ?: throw IllegalArgumentException("gatling.target must be local or nft")
            val modeValue = requiredProperty(properties, "gatling.basic.mode")
            val mode =
                MeasurementMode.entries.singleOrNull { it.propertyValue == modeValue }
                    ?: throw IllegalArgumentException("gatling.basic.mode must be baseline or gated")
            val iterations = positiveInteger(properties, "gatling.basic.iterations", 10)
            val maxIterations = positiveInteger(properties, "gatling.basic.maxIterations", 100)
            require(iterations <= maxIterations) {
                "gatling.basic.iterations must not exceed gatling.basic.maxIterations"
            }
            val maxKey = "gatling.${target.propertyValue}.basic.maxResponseTimeMs"
            val meanKey = "gatling.${target.propertyValue}.basic.meanResponseTimeMs"
            val limits =
                when (mode) {
                    MeasurementMode.BASELINE -> {
                        require(maxKey !in properties && meanKey !in properties) {
                            "Timing limits for ${target.propertyValue} require gatling.basic.mode=gated"
                        }
                        null
                    }
                    MeasurementMode.GATED -> {
                        val meanMs = positiveMilliseconds(properties, meanKey)
                        val maxMs = positiveMilliseconds(properties, maxKey)
                        require(maxMs >= meanMs) {
                            "$maxKey must be greater than or equal to $meanKey"
                        }
                        ResponseTimeLimits(meanMs, maxMs)
                    }
                }

            val certificateDownloadOrigin =
                properties["gatling.certificate.downloadOrigin"]?.let {
                    val origin = originUrl(properties, "gatling.certificate.downloadOrigin")
                    val uri = URI.create(origin)
                    require(
                        when (target) {
                            PerformanceTarget.NFT ->
                                uri.scheme == "https" && uri.port == -1 && uri.host.endsWith(".s3.eu-west-2.amazonaws.com")
                            PerformanceTarget.LOCAL -> uri.host in setOf("localhost", "127.0.0.1", "[::1]")
                        } && uri.host != URI.create(originUrl(properties, "gatling.baseUrl")).host,
                    ) { "Certificate downloads require a separate NFT S3 bucket host or local loopback host" }
                    origin
                }

            return BasicRunConfig(
                target = target,
                mode = mode,
                baseUrl = originUrl(properties, "gatling.baseUrl"),
                simulatorUrl = originUrl(properties, "gatling.simulatorUrl"),
                landlordSubject = requiredProperty(properties, "gatling.basic.landlordSubject"),
                limits = limits,
                iterations = iterations,
                maxIterations = maxIterations,
                certificateDownloadOrigin = certificateDownloadOrigin,
            )
        }

        private fun requiredProperty(
            properties: Map<String, String>,
            key: String,
        ): String {
            val value = properties[key]
            require(!value.isNullOrBlank()) { "$key must be provided and nonblank" }
            return value
        }

        private fun positiveMilliseconds(
            properties: Map<String, String>,
            key: String,
        ): Int {
            val value = requiredProperty(properties, key).toIntOrNull()
            require(value != null && value > 0) { "$key must be a positive integer number of milliseconds" }
            return value
        }

        private fun positiveInteger(
            properties: Map<String, String>,
            key: String,
            default: Int,
        ): Int {
            val value = properties[key]?.toIntOrNull() ?: if (key in properties) null else default
            require(value != null && value > 0) { "$key must be a positive integer" }
            return value
        }

        private fun originUrl(
            properties: Map<String, String>,
            key: String,
        ): String {
            val value = requiredProperty(properties, key)
            val uri =
                try {
                    URI(value)
                } catch (_: URISyntaxException) {
                    throw IllegalArgumentException("$key must be a valid HTTP or HTTPS origin URL")
                }
            require(
                uri.scheme in setOf("http", "https") &&
                    !uri.host.isNullOrBlank() &&
                    uri.userInfo == null &&
                    uri.rawQuery == null &&
                    uri.rawFragment == null &&
                    uri.rawPath in setOf("", "/") &&
                    (uri.port == -1 || uri.port in 1..65535),
            ) { "$key must be an HTTP or HTTPS origin URL without credentials, path, query or fragment" }
            return value.removeSuffix("/")
        }
    }
}
