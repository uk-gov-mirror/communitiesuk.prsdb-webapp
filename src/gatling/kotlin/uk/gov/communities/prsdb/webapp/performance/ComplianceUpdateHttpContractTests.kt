package uk.gov.communities.prsdb.webapp.performance

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComplianceUpdateHttpContractTests {
    @Test
    fun `updates gas electrical and EPC through correlated forms scanned uploads and persisted cards`() {
        UpdateServer().use { server ->
            val result = run(server)
            assertEquals(0, result.exitCode, result.output)
            assertTrue(server.posts.all { it["_csrf"] == "update-csrf" })
            assertTrue(server.posts.any { it["hasCert"] == "true" && it["step"] == "has-gas-safety" })
            assertTrue(server.posts.any { it["electricalCertType"] == "HAS_EICR" })
            assertTrue(server.posts.any { it["certificateNumber"] == "0000-0000-0000-0000-9011" })
            assertEquals(2, server.uploads)
            assertEquals(setOf("gas", "electrical", "epc"), server.completed)
        }
    }

    @Test
    fun `downloads signed off-app filename links without app credentials or changing the signature`() {
        UpdateServer(externalDownloads = true).use { server ->
            val result =
                run(
                    server,
                    mapOf(
                        "gatling.certificate.downloadOrigin" to server.downloadOrigin,
                        "gatling.contract.scanWaitLimitMillis" to "2000",
                    ),
                )
            assertEquals(0, result.exitCode, result.output)
            assertEquals(2, server.downloadRequests.size)
            assertTrue(server.downloadRequests.all { it == "/certificate.pdf?${UpdateServer.SIGNED_QUERY}" })
            assertTrue(server.downloadCredentials.all { it.isEmpty() })
            assertEquals(setOf("gas", "electrical", "epc"), server.completed)
        }
    }

    @Test
    fun `S3 binary downloads complete all updates only when bytes match the uploaded PDF`() {
        for (altered in listOf(false, true)) {
            UpdateServer(externalDownloads = true, binaryDownload = true, alteredDownload = altered).use { server ->
                val result = run(server, mapOf("gatling.certificate.downloadOrigin" to server.downloadOrigin))
                assertEquals(if (altered) 2 else 0, result.exitCode, result.output)
                assertEquals(if (altered) emptySet() else setOf("gas", "electrical", "epc"), server.completed)
            }
        }
    }

    @Test
    fun `existing completed property uploads gas and electrical without a registration or EPC fixture`() {
        UpdateServer(externalDownloads = true, binaryDownload = true).use { server ->
            val result =
                run(
                    server,
                    mapOf(
                        "gatling.contract.completedProperty" to "true",
                        "gatling.certificate.downloadOrigin" to server.downloadOrigin,
                    ),
                )
            assertEquals(0, result.exitCode, result.output)
            assertEquals(setOf("gas", "electrical"), server.completed)
            assertEquals(2, server.uploads)
        }
    }

    @Test
    fun `completes certificate scan after the navigation iteration limit`() {
        UpdateServer(scanReadyAfterPolls = 301).use { server ->
            val result = run(server, mapOf("gatling.contract.scanPollIntervalMillis" to "1"))
            assertEquals(0, result.exitCode, result.output)
            assertEquals(2, server.uploads)
            assertTrue(result.output.contains("Certificate scan for gas safety completed in"), result.output)
        }
    }

    @Test
    fun `scan status polls preserve the journey and use fresh cache busters`() {
        UpdateServer(scanReadyAfterPolls = 2).use { server ->
            val result = run(server, mapOf("gatling.contract.scanPollIntervalMillis" to "1"))
            assertEquals(0, result.exitCode, result.output)
            assertTrue(server.scanRequests.size >= 6)
            val cacheBusters =
                server.scanRequests.map { uri ->
                    val fields = uri.rawQuery.split("&").associate { it.substringBefore("=") to it.substringAfter("=") }
                    assertEquals("journey-u", fields["journeyId"])
                    requireNotNull(fields["perfCacheBuster"]).also { assertTrue(it.isNotBlank()) }
                }
            assertEquals(cacheBusters.size, cacheBusters.toSet().size)
        }
    }

    @Test
    fun `rejects safe link returned after the scan deadline`() {
        UpdateServer(scanReadyAfterPolls = 0, safeLinkResponseDelayMillis = 20).use { server ->
            val result = run(server, mapOf("gatling.contract.scanWaitLimitMillis" to "10"))
            assertEquals(2, result.exitCode, result.output)
            assertTrue(result.output.contains("within"), result.output)
        }
    }

    @Test
    fun `changed update journey stops before a second mutation`() {
        UpdateServer("journey").use { server ->
            val result = run(server)
            assertEquals(2, result.exitCode, result.output)
            assertEquals(1, server.posts.size)
            assertTrue(server.completed.isEmpty())
        }
    }

    @Test
    fun `insecure upload cookie, missing banner, or return without check answers or durable outcome fails`() {
        for (defect in listOf("early", "banner", "persisted", "cookie", "stale")) {
            UpdateServer(defect).use { server ->
                // The landlord page never renders the success banner, so a chain that expects one must fail.
                val result = run(server, if (defect == "banner") mapOf("gatling.contract.expectSuccessBanner" to "true") else emptyMap())
                assertEquals(2, result.exitCode, "$defect: ${result.output}")
            }
        }
    }

    private fun run(
        server: UpdateServer,
        extra: Map<String, String> = emptyMap(),
    ): GatlingTestRunner.Result {
        val directory = Files.createDirectories(Path.of("build/compliance-update-contract-tests", UUID.randomUUID().toString()))
        val certificate = directory.resolve("performance-certificate.pdf").also { Files.write(it, UpdateServer.PDF) }
        return GatlingTestRunner(directory).run(
            ComplianceUpdateContractSimulation::class.java,
            mapOf(
                "gatling.target" to "local", "gatling.baseUrl" to server.baseUrl, "gatling.simulatorUrl" to server.baseUrl,
                "gatling.basic.landlordSubject" to "synthetic-landlord", "gatling.basic.mode" to "baseline",
                "gatling.basic.iterations" to "1",
                "gatling.contract.certificatePath" to certificate.toString(),
            ) + extra,
        )
    }

    private class UpdateServer(
        private val defect: String? = null,
        electrical: String = "eicr",
        private val scanReadyAfterPolls: Int = 1,
        private val safeLinkResponseDelayMillis: Long = 0,
        private val externalDownloads: Boolean = false,
        private val binaryDownload: Boolean = false,
        private val alteredDownload: Boolean = false,
    ) : AutoCloseable {
        companion object {
            val PDF = "%PDF-1.4\n% synthetic contract certificate\n%%EOF\n".toByteArray(StandardCharsets.US_ASCII)
            const val SIGNED_QUERY =
                "X-Amz-Signature=a%2Bb%2Fc&response-content-disposition=attachment%3B%20filename%3D%22certificate.pdf%22"
        }

        private val server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
        val baseUrl = "http://localhost:${server.address.port}"
        private val downloads = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val downloadOrigin = "http://127.0.0.1:${downloads.address.port}"
        val downloadRequests = ConcurrentLinkedQueue<String>()
        val downloadCredentials = ConcurrentLinkedQueue<String>()
        val posts = ConcurrentLinkedQueue<Map<String, String>>()
        val scanRequests = ConcurrentLinkedQueue<URI>()
        val completed = mutableSetOf<String>()
        var uploads = 0
        private val scanPolls = mutableMapOf<String, Int>()
        private val property = "/landlord/property-details/100"
        private val flows =
            mapOf(
                "gas" to
                    listOf(
                        "has-gas-supply" to listOf("hasGasSupply"), "has-gas-safety" to listOf("hasCert"),
                        "gas-safety-certificate-issue-date" to listOf("day", "month", "year"),
                        "gas-safety-certificate-file-upload" to listOf("certificate"),
                        "check-gas-safety-certificate-uploads" to emptyList(), "check-gas-safety-answers" to emptyList(),
                    ),
                "electrical" to
                    listOf(
                        "has-electrical-safety" to listOf("electricalCertType"),
                        "$electrical-expiry-date" to listOf("day", "month", "year"),
                        "$electrical-file-upload" to listOf("certificate"), "check-$electrical-uploads" to emptyList(),
                        "check-electrical-safety-answers" to emptyList(),
                    ),
                "epc" to
                    listOf(
                        "check-epc-details" to listOf("matchedEpcIsCorrect"), "find-your-epc" to listOf("certificateNumber"),
                        "confirm-epc-details" to listOf("matchedEpcIsCorrect"), "check-epc-answers" to emptyList(),
                    ),
            )
        private val routes = mapOf("gas" to "update-gas-safety", "electrical" to "update-electrical-safety", "epc" to "update-epc")

        init {
            downloads.createContext("/") { exchange ->
                downloadRequests.add(exchange.requestURI.toString())
                downloadCredentials.add(
                    listOf("Cookie", "Authorization", "X-CSRF-TOKEN").mapNotNull { exchange.requestHeaders.getFirst(it) }.joinToString(),
                )
                val bytes = if (alteredDownload) PDF + "altered".toByteArray(StandardCharsets.US_ASCII) else PDF
                exchange.responseHeaders.add("Content-Type", if (binaryDownload) "application/octet-stream" else "application/pdf")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
                exchange.close()
            }
            downloads.start()
            server.createContext("/") { exchange ->
                val uri = exchange.requestURI
                val path = uri.path
                var code = 200
                var body = ""

                fun redirect(destination: String) {
                    code = 302
                    exchange.responseHeaders.add("Location", destination)
                }
                val kind = routes.entries.singleOrNull { path.startsWith("$property/${it.value}/") }?.key
                val step = path.substringAfterLast('/')

                fun location(next: String) =
                    "$property/${routes.getValue(
                        kind!!,
                    )}/$next?journeyId=${if (defect == "journey" && posts.isNotEmpty()) "journey-b" else "journey-u"}"
                val steps = kind?.let { flows.getValue(it).map { pair -> pair.first } }
                if (path == property) {
                    exchange.responseHeaders.add("Set-Cookie", "UPDATE=actor-u; Path=/; HttpOnly")
                    val valid = defect != "persisted"
                    // Like propertyDetailsView.html: the flashed updateSuccessBanner is not rendered on the landlord page.
                    body =
                        routes.values.joinToString("") {
                            """<a href="$property/$it/${if (it == "update-gas-safety") {
                                "has-gas-supply"
                            } else if (it == "update-epc") {
                                "start"
                            } else {
                                "has-electrical-safety"
                            }}">Change</a>"""
                        } +
                        """<div class="govuk-summary-card">Gas safety certificate """ +
                        (if (valid && defect != "stale" && "gas" in completed) "Valid performance-certificate.pdf" else "No") +
                        "</div>" +
                        """<div class="govuk-summary-card">Electrical safety certificate """ +
                        (
                            if (valid && "electrical" in completed) {
                                "Valid performance-certificate.pdf (${electrical.uppercase()})"
                            } else {
                                "None"
                            }
                        ) +
                        "</div>" +
                        """<div class="govuk-summary-card">Energy performance certificate (EPC) """ +
                        (if (valid && "epc" in completed) "0000-0000-0000-0000-9011 Valid" else "No") +
                        "</div>"
                } else if (!exchange.requestHeaders.getFirst("Cookie").orEmpty().contains("UPDATE=actor-u")) {
                    code = 403
                } else if (path == "/local-file/safe/certificate.pdf") {
                    exchange.responseHeaders.add("Content-Type", "application/pdf")
                    exchange.sendResponseHeaders(200, PDF.size.toLong())
                    exchange.responseBody.use { it.write(PDF) }
                    exchange.close()
                    return@createContext
                } else if (kind == null) {
                    code = 404
                } else if (uri.rawQuery == null) {
                    redirect(location(if (step == "start") "check-epc-details" else step))
                } else if (!uri.rawQuery.contains("journeyId=journey-u")) {
                    code = 404
                } else if (step == "start") {
                    redirect(location("check-epc-details"))
                } else if (exchange.requestMethod == "POST") {
                    val raw = exchange.requestBody.readAllBytes()
                    val text = String(raw, StandardCharsets.ISO_8859_1)
                    if (exchange.requestHeaders.getFirst("Content-Type").orEmpty().startsWith("multipart/form-data")) {
                        if (text.contains("update-csrf") && text.contains("performance-certificate.pdf") && text.contains("%PDF-1.4") &&
                            exchange.requestHeaders.getFirst("Cookie").orEmpty().contains("file-upload-cookie=upload-u")
                        ) {
                            uploads++
                            posts.add(mapOf("_csrf" to "update-csrf", "step" to step))
                            redirect(location(steps!![steps.indexOf(step) + 1]))
                        } else {
                            code = 403
                        }
                    } else {
                        val fields =
                            text.split('&').filter { it.isNotBlank() }.associate { value ->
                                val pair = value.split('=', limit = 2)
                                URLDecoder.decode(pair[0], StandardCharsets.UTF_8) to
                                    URLDecoder.decode(pair.getOrElse(1) { "" }, StandardCharsets.UTF_8)
                            }
                        posts.add(fields + ("step" to step))
                        if (fields["_csrf"] != "update-csrf") {
                            code = 403
                        } else if (defect == "early" || step == steps!!.last()) {
                            if (step == steps!!.last()) completed.add(kind)
                            redirect("$property#compliance-information")
                        } else {
                            redirect(location(steps[steps.indexOf(step) + 1]))
                        }
                    }
                } else if (steps != null && step in steps) {
                    val fields = flows.getValue(kind).toMap().getValue(step)
                    if (step.endsWith(
                            "file-upload",
                        )
                    ) {
                        exchange.responseHeaders.add(
                            "Set-Cookie",
                            "file-upload-cookie=upload-u; Path=${exchange.requestURI.path}; " +
                                (if (defect == "cookie") "" else "Secure; ") + "HttpOnly",
                        )
                    }
                    val polls = scanPolls.merge(step, 1, Int::plus)!!
                    val review = step.startsWith("check-") && step.endsWith("uploads")
                    if (review) scanRequests.add(uri)
                    val download =
                        if (externalDownloads) "$downloadOrigin/certificate.pdf?$SIGNED_QUERY" else "/local-file/safe/certificate.pdf"
                    val safeLink =
                        if (polls > scanReadyAfterPolls) {
                            """<a class="govuk-link" href="${download.replace("&", "&amp;")}">performance-certificate.pdf</a>"""
                        } else {
                            "performance-certificate.pdf"
                        }
                    if (polls > scanReadyAfterPolls && safeLinkResponseDelayMillis > 0) Thread.sleep(safeLinkResponseDelayMillis)
                    body = (
                        if (review) {
                            """<table><tbody class="govuk-table__body"><tr><td>$safeLink</td><td>status</td>""" +
                                """<td><a class="govuk-link" href="/remove">Remove</a></td></tr></tbody></table>"""
                        } else {
                            ""
                        }
                    ) +
                        """<form method="post" action="${location(step)}">""" +
                        """<input type="hidden" name="_csrf" value="update-csrf">""" +
                        fields.joinToString("") { """<input name="$it">""" } + "</form>"
                } else {
                    code = 404
                }
                exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
                val bytes = body.toByteArray(StandardCharsets.UTF_8)
                exchange.sendResponseHeaders(code, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
                exchange.close()
            }
            server.start()
        }

        override fun close() {
            server.stop(0)
            downloads.stop(0)
        }
    }
}
