package uk.gov.communities.prsdb.webapp.performance

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HttpDestinationTests {
    private val origin = "http://localhost:8080"

    @Test
    fun `relative paths resolve against the origin`() {
        assertEquals("http://localhost:8080/form?x=1", HttpDestination.resolve("/form?x=1", origin))
    }

    @Test
    fun `relative paths resolve against the current page`() {
        assertEquals(
            "http://localhost:8080/a/c",
            HttpDestination.resolve("c", origin, relativeTo = "http://localhost:8080/a/b"),
        )
    }

    @Test
    fun `empty value resolves to the current page`() {
        assertEquals(
            "http://localhost:8080/a/b",
            HttpDestination.resolve("", origin, relativeTo = "http://localhost:8080/a/b"),
        )
    }

    @Test
    fun `absolute same-origin URLs are allowed`() {
        assertEquals("http://localhost:8080/form", HttpDestination.resolve("http://localhost:8080/form", origin))
    }

    @Test
    fun `off-origin destinations are rejected`() {
        listOf(
            "http://evil.example/form",
            "http://localhost:9090/form",
            "https://localhost:8080/form",
            "//evil.example/form",
        ).forEach { value ->
            assertFailsWith<IllegalArgumentException>(value) { HttpDestination.resolve(value, origin) }
        }
    }

    @Test
    fun `destinations containing credentials are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            HttpDestination.resolve("http://user:pass@localhost:8080/form", origin)
        }
    }

    @Test
    fun `certificate download keeps the exact signed query for the configured S3 bucket`() {
        val downloadOrigin = "https://prsdb-uploaded-files-nft.s3.eu-west-2.amazonaws.com"
        val signed = "$downloadOrigin/certificate.pdf?X-Amz-Signature=a%2Bb%2Fc&filename=a%20b.pdf"
        assertEquals(signed, HttpDestination.certificate(signed, origin, downloadOrigin))
        assertEquals(
            "$origin/local-file/safe/certificate.pdf",
            HttpDestination.certificate("/local-file/safe/certificate.pdf", origin, downloadOrigin),
        )
    }

    @Test
    fun `certificate downloads reject unconfigured origins credentials fragments and alternate ports`() {
        val downloadOrigin = "https://prsdb-uploaded-files-nft.s3.eu-west-2.amazonaws.com"
        for (value in listOf(
            "https://other-bucket.s3.eu-west-2.amazonaws.com/certificate.pdf",
            "$downloadOrigin.evil.example/certificate.pdf",
            downloadOrigin.replace("https:", "http:") + "/certificate.pdf",
            "$downloadOrigin:443/certificate.pdf",
            downloadOrigin.replace("https://", "https://user@") + "/certificate.pdf",
            "$downloadOrigin/certificate.pdf#fragment",
        )) {
            assertFailsWith<IllegalArgumentException> { HttpDestination.certificate(value, origin, downloadOrigin) }
        }
        assertFailsWith<IllegalArgumentException> { HttpDestination.certificate("$downloadOrigin/certificate.pdf", origin, null) }
    }
}
