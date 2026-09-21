package uk.gov.communities.prsdb.webapp.journeys.example

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.FlashMap
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.support.RequestContextUtils
import org.springframework.web.servlet.view.RedirectView
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator

class DestinationTests {
    private lateinit var mockRequest: MockHttpServletRequest
    private lateinit var flashAttributes: FlashMap

    fun setRequestContext() {
        mockRequest = MockHttpServletRequest()
        flashAttributes = FlashMap()
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))
        mockRequest.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, flashAttributes)
    }

    @AfterEach
    fun resetRequestContext() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun `VisitableStep Destination with just a step returns a redirect for the current step and journey`() {
        // Arrange
        val mockStep = mock<JourneyStep.RequestableStep<*, *, *>>()
        val journeyId = "test-journey-id"
        val routeSegment = "test-segment"

        whenever(mockStep.currentJourneyId).thenReturn(journeyId)
        whenever(mockStep.urlPath).thenReturn(routeSegment)

        // Act
        val destination = Destination(mockStep)
        val modelAndView = destination.toModelAndView()
        val finalUrl = resolveModelAndViewToRedirectUrl(modelAndView)

        // Assert
        assertEquals(finalUrl, "$routeSegment?journeyId=$journeyId")
    }

    @Test
    fun `VisitableStep Destination with explicit journeyId returns a redirect for the specified step and journey`() {
        // Arrange
        val mockStep = mock<JourneyStep.RequestableStep<*, *, *>>()
        val journeyId = "explicit-journey-id"
        val routeSegment = "explicit-segment"

        whenever(mockStep.urlPath).thenReturn(routeSegment)

        // Act
        val destination = Destination.VisitableStep(mockStep, journeyId)
        val modelAndView = destination.toModelAndView()
        val finalUrl = resolveModelAndViewToRedirectUrl(modelAndView)

        // Assert
        assertEquals(finalUrl, "$routeSegment?journeyId=$journeyId")
    }

    @Test
    fun `VisitableStep Destination for an unreachable step returns null URL`() {
        // Arrange
        val mockStep = mock<JourneyStep.RequestableStep<*, *, *>>()
        val journeyId = "test-journey-id"
        val routeSegment = "test-segment"

        whenever(mockStep.currentJourneyId).thenReturn(journeyId)
        whenever(mockStep.urlPath).thenReturn(routeSegment)
        whenever(mockStep.isStepReachable).thenReturn(false)

        // Act
        val destination = Destination(mockStep)
        val urlString = destination.toUrlStringOrNull()

        // Assert
        assertNull(urlString)
    }

    @Test
    fun `VisitableStep Destination for a reachable step returns correct URL`() {
        // Arrange
        val mockStep = mock<JourneyStep.RequestableStep<*, *, *>>()
        val journeyId = "test-journey-id"
        val routeSegment = "test-segment"

        whenever(mockStep.currentJourneyId).thenReturn(journeyId)
        whenever(mockStep.urlPath).thenReturn(routeSegment)
        whenever(mockStep.isStepReachable).thenReturn(true)

        // Act
        val destination = Destination(mockStep)
        val urlString = destination.toUrlStringOrNull()

        // Assert
        assertEquals("$routeSegment?journeyId=$journeyId", urlString)
    }

    @Test
    fun `ExternalUrl Destination returns a redirect to the specified URL with parameters`() {
        // Arrange
        val externalUrl = "http://example.com"
        val params = mapOf("param1" to "value1", "param2" to "value2")

        // Act
        val destination = Destination.ExternalUrl(externalUrl, params)
        val modelAndView = destination.toModelAndView()

        // Assert
        assertEquals("redirect:$externalUrl", modelAndView.viewName)
        assertEquals("value1", modelAndView.model["param1"])
        assertEquals("value2", modelAndView.model["param2"])
    }

    @Test
    fun `ExternalUrl Destination without parameters returns a redirect to the specified URL`() {
        // Arrange
        val externalUrl = "http://example.com"

        // Act
        val destination = Destination.ExternalUrl(externalUrl)
        val modelAndView = destination.toModelAndView()

        // Assert
        assertEquals("redirect:$externalUrl", modelAndView.viewName)
        assertTrue(modelAndView.model.isEmpty())
    }

    @Test
    fun `ExternalUrl Destination returns correct URL string with parameters`() {
        // Arrange
        val externalUrl = "http://example.com"
        val params = mapOf("param1" to "value1", "param2" to "value2")

        // Act
        val destination = Destination.ExternalUrl(externalUrl, params)
        val urlString = destination.toUrlStringOrNull()

        // Assert
        assertEquals("http://example.com?param1=value1&param2=value2", urlString)
    }

    @Test
    fun `ExternalUrl Destination withFlashAttribute adds an attribute to the output flash map`() {
        // Arrange
        setRequestContext()
        val destination = Destination.ExternalUrl("http://example.com").withFlashAttribute("bannerKey", "bannerValue")

        // Act
        destination.toModelAndView()

        // Assert
        val flashMap = RequestContextUtils.getOutputFlashMap(mockRequest)
        assertEquals("bannerValue", flashMap["bannerKey"])
    }

    @Test
    fun `ExternalUrl Destination withFlashAttribute called multiple times accumulates all attributes`() {
        // Arrange
        setRequestContext()
        val destination =
            Destination
                .ExternalUrl("http://example.com")
                .withFlashAttribute("firstKey", "firstValue")
                .withFlashAttribute("secondKey", "secondValue")

        // Act
        destination.toModelAndView()

        // Assert
        val flashMap = RequestContextUtils.getOutputFlashMap(mockRequest)
        assertEquals("firstValue", flashMap["firstKey"])
        assertEquals("secondValue", flashMap["secondKey"])
    }

    @Test
    fun `ExternalUrl Destination withFlashAttribute does not affect the redirect view name or url parameters`() {
        // Arrange
        setRequestContext()
        val destination =
            Destination
                .ExternalUrl("http://example.com", mapOf("param1" to "value1"))
                .withFlashAttribute("bannerKey", "bannerValue")

        // Act
        val modelAndView = destination.toModelAndView()

        // Assert
        assertEquals("redirect:http://example.com", modelAndView.viewName)
        assertEquals("value1", modelAndView.model["param1"])
        assertNull(modelAndView.model["bannerKey"])
    }

    @Test
    fun `ExternalUrl Destination without flash attributes does not touch the request flash map`() {
        // Arrange
        setRequestContext()
        val destination = Destination.ExternalUrl("http://example.com")

        // Act
        destination.toModelAndView()

        // Assert
        val flashMap = RequestContextUtils.getOutputFlashMap(mockRequest)
        assertTrue(flashMap.isEmpty())
    }

    @Test
    fun `Template Destination returns the correct template name and content`() {
        // Arrange
        val templateName = "test-template"
        val content = mapOf("key1" to "value1", "key2" to "value2")

        // Act
        val destination = Destination.Template(templateName, content)
        val modelAndView = destination.toModelAndView()

        // Assert
        assertEquals(templateName, modelAndView.viewName)
        assertEquals("value1", modelAndView.model["key1"])
        assertEquals("value2", modelAndView.model["key2"])
    }

    @Test
    fun `Template Destination with empty content returns the correct template name`() {
        // Arrange
        val templateName = "test-template"

        // Act
        val destination = Destination.Template(templateName)
        val modelAndView = destination.toModelAndView()

        // Assert
        assertEquals(templateName, modelAndView.viewName)
        assertTrue(modelAndView.model.isEmpty())
    }

    @Test
    fun `Template Destination withContent creates a new Destination with that ModelContent`() {
        // Arrange
        val templateName = "test-template"
        val initialContent = mapOf("key1" to "value1")
        val additionalContent = mapOf("key2" to "value2")

        // Act
        val destination = Destination.Template(templateName, initialContent)
        val updatedDestination = destination.withModelContent(additionalContent)
        val updatedModelAndView = updatedDestination.toModelAndView()
        val oldModelAndView = destination.toModelAndView()

        // Assert
        assertEquals(templateName, updatedModelAndView.viewName)
        assertEquals("value1", updatedModelAndView.model["key1"])
        assertEquals("value2", updatedModelAndView.model["key2"])

        assertEquals(templateName, oldModelAndView.viewName)
        assertEquals("value1", oldModelAndView.model["key1"])
        assertNull(oldModelAndView.model["key2"])
    }

    @Test
    fun `Template Destination converted to url returns null`() {
        // Arrange
        val templateName = "test-template"

        // Act
        val destination = Destination.Template(templateName)
        val urlString = destination.toUrlStringOrNull()

        // Assert
        assertNull(urlString)
    }

    @Test
    fun `withModelContent on non-Template Destination returns the same Destination`() {
        // Arrange
        val mockStep = mock<JourneyStep.RequestableStep<*, *, *>>()
        val nonTemplateDestinations =
            listOf(
                Destination.VisitableStep(mockStep, "test-journey-id"),
                Destination.ExternalUrl("http://example.com"),
                Destination.NavigationalStep(mock()),
                Destination.Nowhere(),
            )

        val contentToAdd = mapOf("key" to "value")

        // Act & Assert
        nonTemplateDestinations.forEach { destination ->
            val updatedDestination = destination.withModelContent(contentToAdd)
            assertSame(destination, updatedDestination)
        }

        Destination::class.sealedSubclasses.all { type ->
            type == Destination.Template::class || nonTemplateDestinations.any { type.isInstance(it) }
        }
    }

    @Test
    fun `NavigationalStep Destination calls through to a navigation step lifecycle orchestrator for that step`() {
        // Arrange
        val mockStep = mock<JourneyStep.InternalStep<*, *>>()
        val mockLifecycleOrchestrator = mock<StepLifecycleOrchestrator.RedirectingStepLifecycleOrchestrator>()
        val modelAndView = ModelAndView()
        whenever(mockStep.lifecycleOrchestrator).thenReturn(mockLifecycleOrchestrator)
        whenever(mockLifecycleOrchestrator.getStepModelAndView()).thenReturn(modelAndView)

        // Act
        val destination = Destination.NavigationalStep(mockStep)
        val result = destination.toModelAndView()

        // Assert
        assertSame(modelAndView, result)
    }

    @Test
    fun `Nowhere Destination throws when converted to  a ModelAndView`() {
        // Arrange
        val destination = Destination.Nowhere()

        // Act & Assert
        assertThrows<Exception> {
            destination.toModelAndView()
        }
    }

    @Test
    fun `Nowhere Destination withModelContent returns the same Nowhere Destination`() {
        // Arrange
        val contentToAdd = mapOf("key" to "value")
        val destination = Destination.Nowhere()

        // Act
        val updatedDestination = destination.withModelContent(contentToAdd)

        // Assert
        assertSame(destination, updatedDestination)
    }

    @Test
    fun `Nowhere Destination converted to url returns null`() {
        // Act
        val destinationString = Destination.Nowhere().toUrlStringOrNull()

        // Assert
        assertNull(destinationString)
    }

    @Test
    fun `Companion invoke returns the correct Destination type based on the JourneyStep provided`() {
        // Arrange
        val mockRequestableStep = mock<JourneyStep.RequestableStep<*, *, *>>()
        whenever(mockRequestableStep.currentJourneyId).thenReturn("journeyId")

        val mockInternalStep = mock<JourneyStep.InternalStep<*, *>>()

        // Act
        val visitableDestination = Destination(mockRequestableStep)
        val navigationalDestination = Destination(mockInternalStep)

        // Assert
        assertTrue(visitableDestination is Destination.VisitableStep)
        assertTrue(navigationalDestination is Destination.NavigationalStep)
    }

    private fun resolveModelAndViewToRedirectUrl(modelAndView: ModelAndView): String? {
        val viewName = modelAndView.viewName
        if (viewName == null) {
            return null
        }

        if (!viewName.startsWith("redirect:")) {
            return null
        }

        val redirectUrl = viewName.removePrefix("redirect:")
        val redirectView = RedirectView(redirectUrl)
        redirectView.setAttributesMap(modelAndView.model)
        return redirectView.url
    }
}
