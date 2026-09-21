package uk.gov.communities.prsdb.webapp.clients

import org.json.JSONObject
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.services.EpcLookupService
import java.net.URLEncoder

@PrsdbWebService
class EpcRegisterClient(
    @Qualifier("epc-client") private val client: RestClient,
) {
    fun getByUprn(uprn: Long): String {
        val uprnString = uprn.toString().padStart(12, '0')
        val query = URLEncoder.encode(uprnString, "UTF-8")
        return searchFor("uprn", "UPRN-$query")!!
    }

    fun getByRrn(rrn: String): String {
        val query = URLEncoder.encode(rrn, "UTF-8")
        return searchFor("rrn", query)!!
    }

    private fun searchFor(
        parameterName: String,
        parameterValue: String,
    ): String? {
        var errorString: String? = null
        val bodyString =
            client
                .get()
                .uri(
                    UriComponentsBuilder
                        .fromUriString("/api/prsdatabase/assessments/search")
                        .queryParam(parameterName, parameterValue)
                        .build()
                        .toUri(),
                ).retrieve()
                // We return the body rather than throwing and the caller can act upon a NOT_FOUND EPC.
                // There are some cases where we were receiving a 404 with a different error, but this is wrong.
                // So, we normalise so all 404 statuses come through as a NOT_FOUND error.
                .onStatus({ it == HttpStatus.NOT_FOUND }) { request, response ->
                    val json = JSONObject(response.body.reader().readText())
                    EpcLookupService.setErrorCode(json, "NOT_FOUND")
                    errorString = json.toString()
                }
                // This parses the response body and depending on the content either sets a fallback value as a side
                // effect or throws a custom exception. We read the body stream without closing it as it will be read
                // by the `body<String>` below.
                .onStatus({ it == HttpStatus.BAD_REQUEST }) { request, response ->
                    val bodyString = response.body.reader().readText()
                    val code = getErrorCode(bodyString)
                    if (code == "INVALID_REQUEST" || code == "BAD_REQUEST") {
                        errorString = bodyString
                    } else {
                        throw PrsdbWebException("Example fallback error: $bodyString")
                    }
                }.body<String>()

        return bodyString ?: errorString
    }

    private fun getErrorCode(bodyString: String): String? = EpcLookupService.getErrorCode(JSONObject(bodyString))
}
