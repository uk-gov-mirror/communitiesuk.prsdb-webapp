package uk.gov.communities.prsdb.webapp.performance

import java.net.URI

object JointAgentJourneySupport {
    // Initialisable journeys answer a step URL without a journeyId by redirecting to the same step with a new journeyId.
    fun initialisedStep(
        location: String,
        config: BasicRunConfig,
        entry: String,
    ): String {
        val resolved = HttpDestination.resolve(location, config.baseUrl)
        val uri = URI.create(resolved)
        require(uri.path == URI.create(HttpDestination.resolve(entry, config.baseUrl)).path) {
            "Journey initialisation redirected away from its entry step: ${uri.path}"
        }
        require(journeyId(resolved) != null) { "Journey initialisation did not assign a journeyId: $resolved" }
        return resolved
    }

    fun journeyId(url: String): String? =
        URI.create(
            url,
        ).rawQuery.orEmpty().split('&').firstOrNull { it.startsWith("journeyId=") }?.substringAfter('=')?.takeIf { it.isNotBlank() }

    // Journey forms render action="" so they post back to the page URL, including its journeyId.
    fun formAction(
        action: String,
        config: BasicRunConfig,
        page: String,
    ): String {
        val pageUrl = HttpDestination.resolve(page, config.baseUrl)
        val resolved = HttpDestination.resolve(action, config.baseUrl, pageUrl)
        require(URI.create(resolved).path == URI.create(pageUrl).path) { "Form action does not post back to its current step" }
        return resolved
    }
}
