package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentUpdateGasSafetyJourneyPages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.constants.PROVIDE_THIS_LATER_BUTTON_ACTION_NAME
import uk.gov.communities.prsdb.webapp.controllers.LettingAgentUpdateGasSafetyController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.HasGasCertFormBasePage
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasCertStep
import java.util.UUID

class HasGasCertFormPageLettingAgentUpdate(
    page: Page,
    urlArguments: Map<String, String>,
) : HasGasCertFormBasePage(
        page,
        LettingAgentUpdateGasSafetyController.getUpdateGasSafetyRoute(UUID.fromString(urlArguments["token"]!!)) +
            "/${HasGasCertStep.ROUTE_SEGMENT}",
    ) {
    val provideThisLaterButton: Locator =
        page.locator("button[name='action'][value='$PROVIDE_THIS_LATER_BUTTON_ACTION_NAME']")
}
