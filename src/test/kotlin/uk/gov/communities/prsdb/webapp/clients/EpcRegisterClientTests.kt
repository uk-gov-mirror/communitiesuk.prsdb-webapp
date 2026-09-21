package uk.gov.communities.prsdb.webapp.clients

import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import java.net.URI

class EpcRegisterClientTests {
    private lateinit var mockServer: MockRestServiceServer
    private lateinit var epcRegisterClient: EpcRegisterClient

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder().baseUrl(BASE_URL)
        mockServer = MockRestServiceServer.bindTo(builder).build()
        epcRegisterClient = EpcRegisterClient(builder.build())
    }

    @Test
    fun `getByRrn returns the response body for a successful response`() {
        val responseBody = """{"data":{"epcRrn":"$CERTIFICATE_NUMBER"}}"""
        mockServer
            .expect(requestTo(URI.create("$BASE_URL/api/prsdatabase/assessments/search?rrn=$CERTIFICATE_NUMBER")))
            .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON))

        val response = epcRegisterClient.getByRrn(CERTIFICATE_NUMBER)

        assertEquals(responseBody, response)
        mockServer.verify()
    }

    @Test
    fun `getByRrn returns the response body for an HTTP 404 with a NOT_FOUND body`() {
        val responseBody = errorBody("NOT_FOUND", "Certificate not found")
        mockServer
            .expect(requestTo(URI.create("$BASE_URL/api/prsdatabase/assessments/search?rrn=$CERTIFICATE_NUMBER")))
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody),
            )

        val response = epcRegisterClient.getByRrn(CERTIFICATE_NUMBER)

        assertEquals("NOT_FOUND", errorCodeOf(response))
        mockServer.verify()
    }

    @Test
    fun `getByRrn normalises an HTTP 404 with an INVALID_REQUEST body to a NOT_FOUND-shaped response`() {
        val responseBody = errorBody("INVALID_REQUEST", "Invalid certificate number")
        mockServer
            .expect(requestTo(URI.create("$BASE_URL/api/prsdatabase/assessments/search?rrn=$CERTIFICATE_NUMBER")))
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody),
            )

        val response = epcRegisterClient.getByRrn(CERTIFICATE_NUMBER)

        assertEquals("NOT_FOUND", errorCodeOf(response))
        mockServer.verify()
    }

    @Test
    fun `getByRrn returns INVALID_REQUEST for an HTTP 400 with an INVALID_REQUEST code`() {
        val responseBody = errorBody("INVALID_REQUEST", "Invalid certificate number")
        mockServer
            .expect(requestTo(URI.create("$BASE_URL/api/prsdatabase/assessments/search?rrn=$CERTIFICATE_NUMBER")))
            .andRespond(
                withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody),
            )

        val response = epcRegisterClient.getByRrn(CERTIFICATE_NUMBER)

        assertEquals("INVALID_REQUEST", errorCodeOf(response))
        mockServer.verify()
    }

    @Test
    fun `getByRrn throws PrsdbWebException for an HTTP 400 with an unrecognised error code`() {
        val responseBody = errorBody("UNEXPECTED_ERROR", "Something else went wrong")
        mockServer
            .expect(requestTo(URI.create("$BASE_URL/api/prsdatabase/assessments/search?rrn=$CERTIFICATE_NUMBER")))
            .andRespond(
                withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody),
            )

        assertThrows(PrsdbWebException::class.java) {
            epcRegisterClient.getByRrn(CERTIFICATE_NUMBER)
        }
        mockServer.verify()
    }

    private fun errorBody(
        code: String,
        title: String,
    ): String = """{"errors":[{"code":"$code","title":"$title"}]}"""

    private fun errorCodeOf(response: String): String =
        JSONObject(response)
            .getJSONArray("errors")
            .getJSONObject(0)
            .getString("code")

    companion object {
        private const val BASE_URL = "http://epc.test"
        private const val CERTIFICATE_NUMBER = "1234-5678-9012-3456-7890"
    }
}
