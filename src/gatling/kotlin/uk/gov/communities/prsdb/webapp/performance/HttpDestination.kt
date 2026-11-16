package uk.gov.communities.prsdb.webapp.performance

import java.net.URI
import java.util.UUID

object HttpDestination {
    const val CERTIFICATE_LINK_SELECTOR = "tbody.govuk-table__body tr td:first-child a.govuk-link[href]"

    fun certificate(
        value: String,
        appOrigin: String,
        downloadOrigin: String?,
    ): String {
        val destination =
            runCatching { URI.create(appOrigin).resolve(value) }
                .getOrElse { throw IllegalArgumentException("Invalid certificate download URL") }
        require(destination.rawFragment == null) { "Unexpected certificate download fragment" }
        val origin =
            if (destination.host == URI.create(appOrigin).host) {
                appOrigin
            } else {
                requireNotNull(downloadOrigin) {
                    "Certificate download origin is not configured"
                }
            }
        return resolve(destination.toString(), origin)
    }

    fun resolve(
        value: String,
        origin: String,
        relativeTo: String = origin,
    ): String {
        val allowed = URI.create(origin)
        val current = URI.create(relativeTo)
        val destination = if (value.isEmpty()) current else current.resolve(value)
        require(
            destination.scheme == allowed.scheme &&
                destination.host == allowed.host &&
                destination.port == allowed.port &&
                destination.rawUserInfo == null,
        ) { "Unexpected HTTP destination origin" }
        return destination.toString()
    }

    // CloudFront keeps every cacheable GET for at least 1s (min TTL), even when the app sends no-store, so a repeat
    // read of the same URL by the same session can return a stale page. The query string is part of the cache key.
    fun withCacheBuster(url: String): String = url + (if ('?' in url) "&" else "?") + "perfCacheBuster=" + UUID.randomUUID()
}
