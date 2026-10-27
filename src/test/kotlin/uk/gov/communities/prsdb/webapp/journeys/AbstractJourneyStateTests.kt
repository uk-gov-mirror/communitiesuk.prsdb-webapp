package uk.gov.communities.prsdb.webapp.journeys

import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.exceptions.JourneyInitialisationException

class AbstractJourneyStateTests {
    @Test
    fun `getStepData returns the corresponding submap of the submitted step data`() {
        // Arrange
        val journeyStateService: JourneyStateService = mock()
        val answerMap =
            mapOf(
                "field1" to "value1",
                "field2" to 21,
            )
        whenever(journeyStateService.getSubmittedStepData()).thenReturn(
            mapOf(
                "stepKey" to answerMap,
                "otherStepKey" to
                    mapOf(
                        "fieldA" to "valueA",
                    ),
            ),
        )

        val journeyState = object : AbstractJourneyState(journeyStateService) {}

        // Act
        val result = journeyState.getStepData("stepKey")

        // Assert
        assertEquals(result?.keys, answerMap.keys)
        answerMap.keys.forEach { key ->
            assertEquals(answerMap[key], result?.get(key))
        }
    }

    @Test
    fun `addStepData adds the step data as a submap to the submitted step data`() {
        // Arrange
        val journeyStateService: JourneyStateService = mock()
        val journeyState = object : AbstractJourneyState(journeyStateService) {}
        val stepData =
            mapOf(
                "field1" to "value1",
                "field2" to 21,
            )

        // Act
        journeyState.addStepData("stepKey", stepData)

        // Assert
        verify(journeyStateService).addSingleStepData("stepKey", stepData)
    }

    @Test
    fun `markAnswersChanged stores a new answersLastChanged value on every call`() {
        // Arrange
        val values = mutableMapOf<String, Any?>()
        val journeyStateService: JourneyStateService = mock()
        whenever(journeyStateService.getValue(any())).thenAnswer { values[it.getArgument(0)] }
        whenever(journeyStateService.setValue(any(), anyOrNull())).thenAnswer {
            values[it.getArgument(0)] = it.getArgument<Any?>(1)
            Unit
        }
        val journeyState = object : AbstractJourneyState(journeyStateService) {}
        assertNull(journeyState.answersLastChanged)

        // Act
        journeyState.markAnswersChanged()
        val first = journeyState.answersLastChanged
        journeyState.markAnswersChanged()
        val second = journeyState.answersLastChanged

        // Assert
        assertNotNull(first)
        assertNotNull(second)
        assertNotEquals(first, second)
        assertEquals(second.toString(), values["answersLastChanged"])
    }

    @Test
    fun `answersConfirmedAt is stored and read back under a fixed key`() {
        // Arrange
        val values = mutableMapOf<String, Any?>()
        val journeyStateService: JourneyStateService = mock()
        whenever(journeyStateService.getValue(any())).thenAnswer { values[it.getArgument(0)] }
        whenever(journeyStateService.setValue(any(), anyOrNull())).thenAnswer {
            values[it.getArgument(0)] = it.getArgument<Any?>(1)
            Unit
        }
        val journeyState = object : AbstractJourneyState(journeyStateService) {}
        val instant = Instant.parse("2026-01-01T10:15:30.123456789Z")

        // Act
        journeyState.answersConfirmedAt = instant

        // Assert
        assertEquals(instant, journeyState.answersConfirmedAt)
        assertEquals(instant.toString(), values["answersConfirmedAt"])
    }

    @Test
    fun `journeyId retrieves the journeyId from the JourneyStateService`() {
        // Arrange
        val journeyStateService: JourneyStateService = mock()
        whenever(journeyStateService.journeyId).thenReturn("testJourneyId")
        val journeyState =
            object : AbstractJourneyState(journeyStateService) {}

        // Act
        val result = journeyState.journeyId

        // Assert
        assertEquals("testJourneyId", result)
    }
}

class JourneyStateDelegateProviderTests {
    @Test
    fun `setting a var property implemented by nullableDelegate saves the value in the state`() {
        // Arrange
        val journeyStateService: JourneyStateService = mock()
        val delegateProvider = JourneyStateDelegateProvider(journeyStateService)
        var testProperty: String? by delegateProvider.nullableDelegate("testProperty")

        // Act
        testProperty = "testValue"

        // Assert
        verify(journeyStateService).setValue("testProperty", "\"testValue\"")
    }

    @Test
    fun `getting a var property implemented by nullableDelegate retrieves the value from the state if present`() {
        // Arrange
        val journeyStateService: JourneyStateService = mock()
        whenever(journeyStateService.getValue("testProperty")).thenReturn("\"testValue\"")
        val delegateProvider = JourneyStateDelegateProvider(journeyStateService)
        var testProperty: String? by delegateProvider.nullableDelegate("testProperty")

        // Act
        val result = testProperty

        // Assert
        assertEquals("testValue", result)
    }

    @Test
    fun `getting a var property implemented by nullableDelegate returns null if value not present in state`() {
        // Arrange
        val journeyStateService: JourneyStateService = mock()
        whenever(journeyStateService.getValue("testProperty")).thenReturn(null)
        val delegateProvider = JourneyStateDelegateProvider(journeyStateService)
        var testProperty: String? by delegateProvider.nullableDelegate("testProperty")

        // Act
        val result = testProperty

        // Assert
        assertNull(result)
    }

    @Test
    fun `getting a val property implemented by requiredDelegate retrieves the value from the state if present`() {
        // Arrange
        val journeyStateService: JourneyStateService = mock()
        whenever(journeyStateService.getValue("testProperty")).thenReturn("\"testValue\"")
        val delegateProvider = JourneyStateDelegateProvider(journeyStateService)
        val testProperty: String by delegateProvider.requiredDelegate("testProperty")

        // Act
        val result = testProperty

        // Assert
        assertEquals("testValue", result)
    }

    @Test
    fun `getting a val property implemented by requiredDelegate throws and deletes state if value not present in state`() {
        // Arrange
        val journeyStateService: JourneyStateService = mock()
        whenever(journeyStateService.getValue("testProperty")).thenReturn(null)
        val delegateProvider = JourneyStateDelegateProvider(journeyStateService)
        val testProperty: String by delegateProvider.requiredDelegate("testProperty")

        // Act & Assert
        assertThrows<IllegalStateException> {
            testProperty
        }

        verify(journeyStateService).deleteState()
    }

    @Test
    fun `setting a mutable property with a key already in use throws an exception`() {
        // Arrange
        val journeyStateService: JourneyStateService = mock()
        val delegateProvider = JourneyStateDelegateProvider(journeyStateService)

        // Act & Assert
        assertThrows<JourneyInitialisationException> {
            var testProperty1: String? by delegateProvider.nullableDelegate("testProperty")
            var testProperty2: Int? by delegateProvider.nullableDelegate("testProperty")
        }
    }

    @Test
    fun `setting a required property with a key already in use throws an exception`() {
        // Arrange
        val journeyStateService: JourneyStateService = mock()
        val delegateProvider = JourneyStateDelegateProvider(journeyStateService)

        // Act & Assert
        assertThrows<JourneyInitialisationException> {
            var testProperty1: String? by delegateProvider.nullableDelegate("testProperty")
            val testProperty2: Int by delegateProvider.requiredDelegate("testProperty")
        }
    }

    @Test
    fun `bindKeyRegistry flushes bare keys when no route is bound`() {
        val journeyStateService: JourneyStateService = mock()
        val registry = DelegateKeyRegistry()
        val provider = JourneyStateDelegateProvider(journeyStateService)
        provider.registerKey("cachedAddresses")

        provider.bindKeyRegistry(registry)

        val other = JourneyStateDelegateProvider(journeyStateService).apply { registerKey("cachedAddresses") }
        assertThrows<JourneyInitialisationException> { other.bindKeyRegistry(registry) }
    }

    @Test
    fun `bindKeyRegistry flushes route-scoped keys when a route is bound`() {
        val journeyStateService: JourneyStateService = mock()
        val registry = DelegateKeyRegistry()
        val provider = JourneyStateDelegateProvider(journeyStateService)
        provider.registerKey("cachedAddresses")
        provider.bindRoutePrefix("lead-trustee-address")

        provider.bindKeyRegistry(registry)

        assertDoesNotThrow {
            JourneyStateDelegateProvider(journeyStateService)
                .apply { registerKey("cachedAddresses") }
                .bindKeyRegistry(registry)
        }
        assertThrows<JourneyInitialisationException> {
            registry.register("lead-trustee-address/cachedAddresses")
        }
    }

    @Test
    fun `a key registered after bindKeyRegistry forwards to the registry live`() {
        val journeyStateService: JourneyStateService = mock()
        val registry = DelegateKeyRegistry()
        val provider = JourneyStateDelegateProvider(journeyStateService)
        provider.bindKeyRegistry(registry)

        provider.registerKey("addedLater")

        assertThrows<JourneyInitialisationException> { registry.register("addedLater") }
    }

    @Test
    fun `two same-route providers with an equal key collide in one registry`() {
        val journeyStateService: JourneyStateService = mock()
        val registry = DelegateKeyRegistry()
        JourneyStateDelegateProvider(journeyStateService)
            .apply {
                registerKey("cachedAddresses")
                bindRoutePrefix("addr")
            }.bindKeyRegistry(registry)

        assertThrows<JourneyInitialisationException> {
            JourneyStateDelegateProvider(journeyStateService)
                .apply {
                    registerKey("cachedAddresses")
                    bindRoutePrefix("addr")
                }.bindKeyRegistry(registry)
        }
    }
}
