package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.bodyBytes
import io.gatling.javaapi.core.CoreDsl.bodyString
import io.gatling.javaapi.core.CoreDsl.css
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.repeat
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.http.HttpDsl.RawFileBodyPart
import io.gatling.javaapi.http.HttpDsl.StringBodyPart
import io.gatling.javaapi.http.HttpDsl.header
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import org.slf4j.LoggerFactory
import uk.gov.communities.prsdb.webapp.controllers.PropertyDetailsController
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

enum class ComplianceUpdateKind(
    val route: String,
    val firstStep: String,
    val checkAnswers: String,
    val title: String,
    val uploads: Int,
    val card: String,
) {
    GAS("update-gas-safety", "has-gas-supply", "check-gas-safety-answers", "gas safety", 1, CertificateChecks.GAS_CARD),
    ELECTRICAL(
        "update-electrical-safety",
        "has-electrical-safety",
        "check-electrical-safety-answers",
        "electrical safety",
        1,
        CertificateChecks.ELECTRICAL_CARD,
    ),
    EPC("update-epc", "start", "check-epc-answers", "EPC", 0, CertificateChecks.EPC_CARD),
}

object ComplianceUpdateJourney {
    private val logger = LoggerFactory.getLogger(ComplianceUpdateJourney::class.java)
    private val defaultScanWaitLimit = Duration.ofMinutes(5)
    private val defaultScanPollInterval = Duration.ofMillis(250)

    val updatedVariant = RegistrationVariant.OCCUPIED_SELECTED_CERTIFICATES
    private val permittedQuery = setOf("journeyId", "memberId")
    val LANDLORD_PROPERTY_PATH = Regex("/landlord/property-details/[0-9]+")

    fun requestNames(kind: ComplianceUpdateKind) =
        listOf("Compliance update ${kind.title}: entry link", "Compliance update ${kind.title}: saved certificate on property")

    val finalRequest = "Compliance update: persisted updated compliance"
    val completedPropertyKinds = listOf(ComplianceUpdateKind.GAS, ComplianceUpdateKind.ELECTRICAL)

    fun completedPropertyChain(
        config: BasicRunConfig,
        fixture: CompletedPropertyFixture,
        certificatePath: String,
        prefix: String = "",
    ): ChainBuilder {
        val details =
            config.baseUrl +
                PropertyDetailsController.LANDLORD_PROPERTY_DETAILS_ROUTE.replace(
                    "{propertyOwnershipId}",
                    fixture.propertyOwnershipId.toString(),
                )
        return exec { session -> session.set("regPropertyUrl", details) }
            .exec(completedPropertyKinds.map { kind -> chain(config, null, kind, certificatePath, prefix) })
    }

    fun chain(
        config: BasicRunConfig,
        fixture: RegistrationFixture?,
        kind: ComplianceUpdateKind,
        certificatePath: String,
        prefix: String = "",
        propertyPathPattern: Regex = LANDLORD_PROPERTY_PATH,
        // Anonymous letting-agent sessions cannot fetch authenticated /local-file links; the safe link must still appear.
        verifySafeDownload: Boolean = true,
        // The landlord property page never renders the flashed updateSuccessBanner; the letting-agent page does.
        expectSuccessBanner: Boolean = false,
        scanPollInterval: Duration = defaultScanPollInterval,
        scanWaitLimit: Duration = defaultScanWaitLimit,
    ): ChainBuilder {
        val variant = updatedVariant
        val electrical = "eicr"
        require(fixture != null || kind != ComplianceUpdateKind.EPC) { "An EPC update requires a registration fixture" }
        val digest = CertificateChecks.sha256(Files.readAllBytes(Path.of(certificatePath)))
        val form = "form[method=post]"
        val names = requestNames(kind)
        val post =
            http { session -> "${prefix}Compliance update ${kind.title}: POST ${step(session)}" }
                .post("#{cuAction}").disableFollowRedirect()
                .formParamMap { session -> session.get<Map<String, String>>("cuFields")!! }
                .check(status().`is`(302)).check(header("Location").saveAs("cuRedirect"))
        val upload =
            http { session -> "${prefix}Compliance update ${kind.title}: POST ${step(session)}" }
                .post("#{cuAction}").disableFollowRedirect()
                .bodyPart(StringBodyPart("_csrf", "#{cuCsrf}"))
                .bodyPart(
                    RawFileBodyPart("certificate", certificatePath).fileName("performance-certificate.pdf").contentType("application/pdf"),
                )
                .asMultipartForm()
                .check(status().`is`(302)).check(header("Location").saveAs("cuRedirect"))

        return exec(
            http("$prefix${names[0]}").get { session -> HttpDestination.withCacheBuster(propertyUrl(session)) }
                .disableFollowRedirect().check(status().`is`(200))
                .check(css("a[href*='/${kind.route}/${kind.firstStep}']", "href").findAll().saveAs("cuEntries")),
        ).exitHereIfFailed().exec { session ->
            val entry = session.getList<String>("cuEntries").distinct().single()
            val property = propertyUrl(session)
            val destination = resolve(entry, config.baseUrl, property, kind, null, propertyPathPattern)
            require(URI.create(destination).path.endsWith("/${kind.route}/${kind.firstStep}")) { "Unexpected ${kind.title} update entry" }
            session.removeAll("cuJourneyId", "cuEntries").setAll(
                mapOf(
                    "cuNext" to destination,
                    "cuDone" to false,
                    "cuDownloads" to 0,
                    "cuVisited" to emptySet<String>(),
                    "cuScanStartedAt" to 0L,
                ),
            )
        }.exitHereIfFailed().exec(
            repeat(1300, "cuCount").on(
                exec { session ->
                    if (session.getBoolean("cuDone")) {
                        session
                    } else {
                        session.removeAll(
                            "cuRedirect", "cuActions", "cuHiddenNames", "cuHiddenValues", "cuInputNames",
                            "cuCookies", "cuErrors", "cuContentType", "cuSafeLinks",
                        ).set(
                            "cuCurrent",
                            session.getString("cuNext"),
                        ).set("cuPost", false).set("cuUpload", false).set("cuScanPending", false)
                    }
                }.doIf { session -> !session.getBoolean("cuDone") }.then(
                    exec(
                        http { session -> "${prefix}Compliance update ${kind.title}: GET ${step(session)}" }
                            .get { session ->
                                val current = requireNotNull(session.getString("cuCurrent"))
                                if (CertificateChecks.isUploadCheck(step(session))) HttpDestination.withCacheBuster(current) else current
                            }.disableFollowRedirect()
                            .check(status().`in`(200, 302).saveAs("cuStatus"))
                            .check(header("Location").optional().saveAs("cuRedirect"))
                            .check(header("Set-Cookie").findAll().optional().saveAs("cuCookies"))
                            .check(header("Content-Type").optional().saveAs("cuContentType"))
                            .check(bodyString().saveAs("cuBody"))
                            .check(css(".govuk-error-summary").optional().saveAs("cuErrors"))
                            .check(css(form, "action").findAll().optional().saveAs("cuActions"))
                            .check(css("$form input[type=hidden]", "name").findAll().optional().saveAs("cuHiddenNames"))
                            .check(css("$form input[type=hidden]", "value").findAll().optional().saveAs("cuHiddenValues"))
                            .check(
                                css(
                                    "$form input[name], $form button[name], $form select[name]",
                                    "name",
                                ).findAll().optional().saveAs("cuInputNames"),
                            )
                            .check(css(HttpDestination.CERTIFICATE_LINK_SELECTOR, "href").findAll().optional().saveAs("cuSafeLinks")),
                    ).exitHereIfFailed()
                        .exec {
                                session ->
                            prepare(session, config, fixture, kind, variant, propertyPathPattern, scanWaitLimit)
                        }.exitHereIfFailed()
                        .doIf { session -> session.getBoolean("cuScanPending") }.then(exec { it }.pause(scanPollInterval))
                        .doIf { session -> session.getBoolean("cuPost") && !session.getBoolean("cuScanPending") }.then(
                            exec { it }.pause(config.thinkTime),
                        )
                        .doIf {
                                session ->
                            verifySafeDownload && CertificateChecks.isUploadCheck(step(session)) && !session.getBoolean("cuScanPending")
                        }.then(
                            exec(
                                http("${prefix}Compliance update ${kind.title}: scanned certificate download")
                                    .get {
                                            session ->
                                        HttpDestination.certificate(
                                            session.getList<String>("cuSafeLinks").distinct().single(),
                                            config.baseUrl,
                                            config.certificateDownloadOrigin,
                                        )
                                    }
                                    .ignoreProtocolHeaders().disableUrlEncoding().disableFollowRedirect().check(status().`is`(200))
                                    .check(
                                        header("Content-Type").transform { it.substringBefore(';').trim().lowercase() }
                                            .`in`("application/pdf", "application/octet-stream"),
                                    )
                                    .check(bodyBytes().transform { CertificateChecks.sha256(it) }.`is`(digest)),
                            )
                                .exitHereIfFailed().exec { session -> session.set("cuDownloads", session.getInt("cuDownloads") + 1) },
                        )
                        .doIf {
                                session ->
                            session.getBoolean("cuPost") && !session.getBoolean("cuUpload")
                        }.then(exec(post).exitHereIfFailed())
                        .doIf { session -> session.getBoolean("cuUpload") }.then(
                            exec { session -> session.set("cuScanStartedAt", System.nanoTime()) }
                                .exec(upload)
                                .exitHereIfFailed(),
                        )
                        .exec { session ->
                            if (!session.getBoolean("cuPost")) session else afterRedirect(session, config, kind, propertyPathPattern)
                        }.exitHereIfFailed(),
                ),
            ),
        ).exec { session ->
            require(session.getBoolean("cuDone")) { "${kind.title} update exceeded bounded navigation budget" }
            require(session.getInt("cuDownloads") == if (verifySafeDownload) kind.uploads else 0) {
                "Real scanner safe-file download was not verified for ${kind.title}"
            }
            val visited = session.get<Set<String>>("cuVisited")!!
            val required =
                when (kind) {
                    ComplianceUpdateKind.GAS ->
                        setOf(
                            "has-gas-supply",
                            "has-gas-safety",
                            "gas-safety-certificate-issue-date",
                            "gas-safety-certificate-file-upload",
                            "check-gas-safety-certificate-uploads",
                            "check-gas-safety-answers",
                        )
                    ComplianceUpdateKind.ELECTRICAL ->
                        setOf(
                            "has-electrical-safety",
                            "$electrical-expiry-date",
                            "$electrical-file-upload",
                            "check-$electrical-uploads",
                            "check-electrical-safety-answers",
                        )
                    ComplianceUpdateKind.EPC -> setOf("find-your-epc", "confirm-epc-details", "check-epc-answers")
                }
            require(required.all { it in visited }) { "${kind.title} update skipped required steps: ${required - visited}" }
            session
        }.exitHereIfFailed().exec(
            http("$prefix${names[1]}").get { session -> HttpDestination.withCacheBuster(propertyUrl(session)) }
                .disableFollowRedirect().check(status().`is`(200)).check(css(".govuk-error-summary").notExists())
                .check(
                    css(".govuk-summary-card").findAll().transform { cards ->
                        CertificateChecks.persistedCompliance(cards, variant, fixture, only = kind.card)
                    }.`is`(true),
                )
                // Gatling passes null to transform when no banner matches.
                .check(
                    css(".govuk-notification-banner--success").findAll().transform { banners: List<String>? ->
                        val updated = banners.orEmpty().count { it.contains("Compliance certificates updated.") }
                        updated == if (expectSuccessBanner) 1 else 0
                    }.`is`(true),
                ),
        ).exitHereIfFailed()
    }

    fun verifyAll(
        config: BasicRunConfig,
        fixture: RegistrationFixture,
        prefix: String = "",
    ): ChainBuilder =
        exec(
            http("$prefix$finalRequest").get { session -> HttpDestination.withCacheBuster(propertyUrl(session)) }
                .disableFollowRedirect().check(status().`is`(200)).check(css(".govuk-error-summary").notExists())
                .check(
                    css(".govuk-summary-card").findAll().transform { cards ->
                        ComplianceUpdateKind.entries.all { kind ->
                            CertificateChecks.persistedCompliance(cards, updatedVariant, fixture, only = kind.card)
                        }
                    }.`is`(true),
                ),
        ).exitHereIfFailed()

    private fun prepare(
        session: Session,
        config: BasicRunConfig,
        fixture: RegistrationFixture?,
        kind: ComplianceUpdateKind,
        variant: RegistrationVariant,
        propertyPathPattern: Regex,
        scanWaitLimit: Duration,
    ): Session {
        val current = requireNotNull(session.getString("cuCurrent"))
        val currentStep = step(session)
        val result = session.set("cuVisited", session.get<Set<String>>("cuVisited").orEmpty() + currentStep)
        if (session.getInt("cuStatus") == 302) return afterRedirect(result.set("cuPost", false), config, kind, propertyPathPattern, current)
        require(
            session.getString("cuContentType").orEmpty().substringBefore(';').trim() == "text/html",
        ) { "Compliance update did not return HTML" }
        require(!session.contains("cuErrors")) { "Compliance update validation error at $current" }
        val journeyId = requireNotNull(session.getString("cuJourneyId")) { "No update journey ID was correlated before mutation" }
        if (CertificateChecks.isUploadCheck(currentStep)) {
            require(session.getString("cuBody").orEmpty().contains("performance-certificate.pdf")) { "Uploaded certificate is missing" }
            val existingStart = session.get<Long>("cuScanStartedAt") ?: 0L
            val scanStartedAt = if (existingStart > 0L) existingStart else System.nanoTime()
            val elapsedNanos = System.nanoTime() - scanStartedAt
            require(elapsedNanos < scanWaitLimit.toNanos()) {
                "Actual local scan-processor did not produce a safe downloadable certificate within " +
                    "${Duration.ofNanos(elapsedNanos).toMillis()} ms"
            }
            if (!session.contains("cuSafeLinks")) {
                return result.set("cuScanPending", true)
                    .set("cuScanStartedAt", scanStartedAt)
                    .set("cuNext", current)
            }
            logger.info("Certificate scan for {} completed in {} ms", kind.title, Duration.ofNanos(elapsedNanos).toMillis())
        }
        val actions = session.getList<String>("cuActions")
        require(actions.size == 1) { "Expected one compliance update form at $current" }
        val action = resolve(actions.single(), config.baseUrl, current, kind, journeyId, propertyPathPattern)
        require(URI.create(action).path == URI.create(current).path) { "Compliance update form posts to a different step" }
        val hidden = CertificateChecks.hiddenFields(session.getList("cuHiddenNames"), session.getList("cuHiddenValues"))
        val upload = currentStep.endsWith("file-upload")
        val fields =
            if (upload) {
                emptyMap()
            } else {
                ComplianceJourney.fields(currentStep, variant, fixture)
                    ?: error("Unimplemented compliance update step: ${URI.create(current).path}")
            }
        val inputNames = session.getList<String>("cuInputNames").toSet()
        require(fields.keys.all { it in inputNames }) { "Unexpected fields at $currentStep: ${fields.keys - inputNames}" }
        if (upload) {
            require(hidden.keys == setOf("_csrf")) { "Unexpected multipart hidden fields; refusing to discard them" }
            require(session.getList<String>("cuCookies").any { CertificateChecks.isUploadCookie(it, URI.create(current).path) }) {
                "A correlated one-use upload cookie is required"
            }
        }
        return result.setAll(
            mapOf(
                "cuAction" to action,
                "cuFields" to hidden + fields,
                "cuCsrf" to hidden.getValue("_csrf"),
                "cuPost" to true,
                "cuUpload" to upload,
            ),
        )
    }

    private fun afterRedirect(
        session: Session,
        config: BasicRunConfig,
        kind: ComplianceUpdateKind,
        propertyPathPattern: Regex,
        current: String = session.getString("cuCurrent")!!,
    ): Session {
        val location = requireNotNull(session.getString("cuRedirect")) { "Compliance update redirect is missing" }
        val property = propertyUrl(session)
        val resolved = HttpDestination.resolve(location, config.baseUrl, current)
        if (URI.create(resolved).path == URI.create(property).path) {
            require(kind.checkAnswers in session.get<Set<String>>("cuVisited").orEmpty()) {
                "Compliance update returned to the property before its check-answers submission"
            }
            require(URI.create(resolved).rawQuery == null) { "Unexpected query on compliance update completion" }
            return session.set("cuDone", true)
        }
        val destination = resolve(location, config.baseUrl, current, kind, session.getString("cuJourneyId"), propertyPathPattern)
        val journeyId = parameters(URI.create(destination))["journeyId"]?.single()
        return if (session.getString("cuJourneyId") == null) {
            session.set(
                "cuNext",
                destination,
            ).set("cuJourneyId", requireNotNull(journeyId) { "Update journey was not initialised with a journey ID" })
        } else {
            session.set("cuNext", destination)
        }
    }

    internal fun resolve(
        value: String,
        baseUrl: String,
        current: String,
        kind: ComplianceUpdateKind,
        journeyId: String?,
        propertyPathPattern: Regex = LANDLORD_PROPERTY_PATH,
    ): String {
        val resolved = HttpDestination.resolve(value, baseUrl, current)
        val uri = URI.create(resolved)
        val propertyPath = URI.create(current).path.substringBefore("/${kind.route}").removeSuffix("/")
        require(propertyPath.matches(propertyPathPattern)) { "Compliance update is not anchored to a property" }
        require(uri.path.matches(Regex("${Regex.escape(propertyPath)}/${kind.route}/[a-z-]+")) && uri.fragment == null) {
            "Unexpected compliance update path: ${uri.path}"
        }
        val query = parameters(uri)
        require(
            query.keys.all {
                it in permittedQuery
            } && query.values.all { it.size == 1 },
        ) { "Unexpected compliance update query: ${uri.rawQuery}" }
        query["memberId"]?.single()?.let { require(it.toIntOrNull()?.let { member -> member >= 0 } == true) { "Invalid memberId" } }
        query["journeyId"]?.single()?.let { require(it.matches(Regex("[A-Za-z0-9._-]+"))) { "Unsafe update journey ID" } }
        if (journeyId != null) require(query["journeyId"] == listOf(journeyId)) { "Compliance update journey changed or disappeared" }
        return resolved
    }

    private fun propertyUrl(session: Session) =
        requireNotNull(session.getString("regPropertyUrl")) {
            "A durable registered property is required"
        }

    private fun step(session: Session): String = URI.create(requireNotNull(session.getString("cuCurrent"))).path.substringAfterLast('/')

    private fun parameters(uri: URI): Map<String, List<String>> =
        uri.rawQuery?.split('&')?.filter { it.isNotBlank() }?.map { pair ->
            val parts = pair.split('=', limit = 2)
            URLDecoder.decode(parts[0], StandardCharsets.UTF_8) to URLDecoder.decode(parts.getOrElse(1) { "" }, StandardCharsets.UTF_8)
        }?.groupBy({ it.first }, { it.second }) ?: emptyMap()
}
