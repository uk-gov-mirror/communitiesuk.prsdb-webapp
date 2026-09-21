package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.controllers.RegisterPropertyController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Heading
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.LookupAddressFormPage
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.LookupAddressStep
import uk.gov.communities.prsdb.webapp.journeys.shared.tasks.CorrespondenceAddressTask

class CorrespondenceLookupAddressFormPagePropertyRegistration(
    page: Page,
) : LookupAddressFormPage(
        page,
        "${RegisterPropertyController.PROPERTY_REGISTRATION_ROUTE}/" +
            "${CorrespondenceAddressTask.ROUTE_SEGMENT}/${LookupAddressStep.ROUTE_SEGMENT}",
    ) {
    val heading = Heading(page.locator("h1"))
}
