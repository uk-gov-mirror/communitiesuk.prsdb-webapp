package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckPropertyManagerFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CorrespondenceEmailFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.PropertyManagerDetailsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.PropertyManagerFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.RemovePropertyManagerFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.TaskListPagePropertyRegistration
import kotlin.test.assertEquals

class PropertyRegistrationPropertyManagerSinglePageTests : IntegrationTestWithImmutableData("data-local.sql") {
    @Test
    fun `Submitting with no option selected returns an error`() {
        val propertyManagerPage = navigator.skipToPropertyRegistrationPropertyManagerPage()

        propertyManagerPage.form.submit()

        assertThat(propertyManagerPage.form.getErrorMessage()).containsText("Select if you are using a property manager")
    }

    @Test
    fun `answering no to using a property manager leaves the task`(page: Page) {
        val propertyManagerPage = navigator.skipToPropertyRegistrationPropertyManagerPage()

        propertyManagerPage.submitNo()

        assertPageIs(page, CorrespondenceEmailFormPagePropertyRegistration::class)
    }

    @Test
    fun `answering yes goes through the details and summary pages then leaves the task`(page: Page) {
        val checkPropertyManagerPage = addPropertyManager(page)

        checkPropertyManagerPage.form.submit()

        assertPageIs(page, CorrespondenceEmailFormPagePropertyRegistration::class)
    }

    @Test
    fun `not confirming removal returns to the summary page`(page: Page) {
        addPropertyManager(page)
        navigator.navigateToPropertyRegistrationRemovePropertyManagerPage()
        val removePropertyManagerPage = assertPageIs(page, RemovePropertyManagerFormPagePropertyRegistration::class)

        removePropertyManagerPage.submitNo()

        assertPageIs(page, CheckPropertyManagerFormPagePropertyRegistration::class)
    }

    @Test
    fun `confirming removal clears the details and returns to the question with yes still selected`(page: Page) {
        addPropertyManager(page)
        navigator.navigateToPropertyRegistrationRemovePropertyManagerPage()
        val removePropertyManagerPage = assertPageIs(page, RemovePropertyManagerFormPagePropertyRegistration::class)

        removePropertyManagerPage.submitYes()

        val propertyManagerPage = assertPageIs(page, PropertyManagerFormPagePropertyRegistration::class)
        assertEquals("true", propertyManagerPage.form.radios.selectedValue)

        navigator.navigateToPropertyRegistrationCheckPropertyManagerPage()
        assertPageIs(page, TaskListPagePropertyRegistration::class)
    }

    private fun addPropertyManager(page: Page): CheckPropertyManagerFormPagePropertyRegistration {
        val propertyManagerPage = navigator.skipToPropertyRegistrationPropertyManagerPage()
        propertyManagerPage.submitYes()
        val propertyManagerDetailsPage = assertPageIs(page, PropertyManagerDetailsFormPagePropertyRegistration::class)
        propertyManagerDetailsPage.form.submit()
        return assertPageIs(page, CheckPropertyManagerFormPagePropertyRegistration::class)
    }
}
