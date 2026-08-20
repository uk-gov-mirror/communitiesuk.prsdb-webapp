package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.controllers.PropertyDetailsController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Button
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Link
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.NotificationBanner
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.SummaryCard
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.SummaryList
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.PropertyDetailsBasePage

class PropertyDetailsPageLandlordView(
    page: Page,
    urlArguments: Map<String, String>,
) : PropertyDetailsBasePage(
        page,
        PropertyDetailsController.getPropertyDetailsPath(
            urlArguments["propertyOwnershipId"]!!.toLong(),
            isLocalCouncilView = false,
        ),
    ) {
    val deregisterPropertyLink = Link.byText(page, "Deregister property")
    val delegateToLettingAgentLink = Link.byText(page, "Allow your letting agent or property manager to update details")
    val removeLettingAgentLink = Link.byText(page, "Remove your letting agent or property manager")
    val inviteJointLandlordLink = Link.byText(page, "invite them to join the property")
    val inviteJointLandlordButton = Button.byText(page, "Invite a joint landlord")

    val markAsSingleLandlordInsetText: Locator
        get() = page.locator(".govuk-inset-text:has-text(\"confirm that you're the only landlord\")")

    val switchToIndividualLink = Link.byText(page, "confirm that you're the only landlord")

    val inviteJointLandlordIndividualText: Locator
        get() = page.locator("p:has-text('If there are other landlords')")

    val notificationBanner = NotificationBannerPropertyDetailsLandlordView(page)

    val pendingInvitationsSummary: Locator
        get() = page.locator(".govuk-details__summary:has-text('Pending invitations')")

    val pendingInvitationsDetails: Locator
        get() = page.locator("details", Page.LocatorOptions().setHasText("Pending invitations"))

    val cancelInvitationLink = Link.byText(page, "Cancel invitation")

    val resendInvitationLink = Link.byText(page, "Send a new email invitation")

    val expiredInvitationsDetails: Locator
        get() = page.locator("details", Page.LocatorOptions().setHasText("Expired invitations"))

    val landlordSummaryCards: List<LandlordSummaryCard>
        get() {
            val count = page.locator("#landlord-details .govuk-summary-card").count()
            return (0 until count).map { LandlordSummaryCard(page.locator("#landlord-details .govuk-summary-card").nth(it)) }
        }

    class LandlordSummaryCard(
        locator: Locator,
    ) : SummaryCard(locator) {
        override val summaryList = LandlordCardSummaryList(locator)
    }

    class LandlordCardSummaryList(
        locator: Locator,
    ) : SummaryList(locator) {
        val registrationNumberRow = getRow("Landlord Registration Number")
        val emailAddressRow = getRow("Email address")
    }

    class NotificationBannerPropertyDetailsLandlordView(
        page: Page,
    ) : NotificationBanner(page) {
        val viewComplianceCertificatesLink =
            Link.byText(
                page,
                "View compliance certificates",
                selectorOrLocator = ".govuk-notification-banner__link",
            )
    }
}
