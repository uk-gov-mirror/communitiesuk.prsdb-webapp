package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import org.junit.jupiter.api.Nested
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent.Companion.assertThat
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LandlordDetailsPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LandlordIncompletePropertiesPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.OutstandingActionsPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.RegisterPropertyStartPage
import kotlin.test.Test
import kotlin.test.assertEquals

class LandlordDashboardTests : IntegrationTestWithImmutableData("data-local.sql") {
    @Test
    fun `the dashboard loads displaying the user's name and lrn`() {
        val dashboard = navigator.goToLandlordDashboard()
        assertThat(dashboard.dashboardBannerHeading).containsText("Alexander Smith")
        assertThat(dashboard.dashboardBannerSubHeading).containsText("Landlord registration number")
        assertThat(dashboard.dashboardBannerSubHeading).containsText("L-CKSQ-3SX9")
    }

    @Test
    fun `the register property buttons links to the property registration journey`(page: Page) {
        val dashboard = navigator.goToLandlordDashboard()
        dashboard.registerPropertyButton.clickAndWait()
        assertPageIs(page, RegisterPropertyStartPage::class)
    }

    @Test
    fun `the incomplete property details button links to the incomplete properties page`(page: Page) {
        val dashboard = navigator.goToLandlordDashboard()
        dashboard.viewIncompletePropertiesButton.clickAndWait()
        assertPageIs(page, LandlordIncompletePropertiesPage::class)
    }

    @Test
    fun `the view property records button links to property records tab on the landlord details page`(page: Page) {
        val dashboard = navigator.goToLandlordDashboard()
        dashboard.viewPropertyRecordsButton.clickAndWait()
        val detailsPage = assertPageIs(page, LandlordDetailsPage::class)
        assertEquals("registered-properties", detailsPage.tabs.activeTabPanelId)
    }

    @Test
    fun `the view landlord record button links to the landlord details page`(page: Page) {
        val dashboard = navigator.goToLandlordDashboard()
        dashboard.viewLandlordRecordButton.clickAndWait()
        assertPageIs(page, LandlordDetailsPage::class)
    }

    @Test
    fun `the outstanding actions button links to the outstanding actions page`(page: Page) {
        val dashboard = navigator.goToLandlordDashboard()
        dashboard.outstandingActionsButton.clickAndWait()
        assertPageIs(page, OutstandingActionsPage::class)
    }

    @Nested
    inner class NotificationBanner {
        @Nested
        inner class LandlordWithoutIncompletePropertiesOrComplianceActions :
            NestedIntegrationTestWithImmutableData("data-mockuser-landlord-with-properties.sql") {
            @Test
            fun `the dashboard loads without a notification banner`() {
                val dashboard = navigator.goToLandlordDashboard()
                assertThat(dashboard.notificationBanner).isHidden()
            }
        }

        @Nested
        inner class WithOnlyIncompleteProperties {
            @Nested
            inner class WithOne : NestedIntegrationTestWithImmutableData("data-mockuser-landlord-with-one-incomplete-property.sql") {
                @Test
                fun `the notification banner loads with correct message for one incomplete property`(page: Page) {
                    val dashboard = navigator.goToLandlordDashboard()
                    assertThat(dashboard.notificationBanner.title).containsText("Important")
                    assertThat(dashboard.notificationBanner.content.heading)
                        .containsText("You have 1 incomplete property: View incomplete property details")
                }
            }

            @Nested
            inner class WithMultiple : NestedIntegrationTestWithImmutableData("data-mockuser-landlord-with-incomplete-properties.sql") {
                @Test
                fun `the notification banner loads with correct message for multiple incomplete properties`(page: Page) {
                    val dashboard = navigator.goToLandlordDashboard()
                    assertThat(dashboard.notificationBanner.title).containsText("Important")
                    assertThat(dashboard.notificationBanner.content.heading)
                        .containsText("You have 2 incomplete properties: View incomplete property details")
                }

                @Test
                fun `the view incomplete properties link redirects to the incomplete properties page`(page: Page) {
                    val dashboard = navigator.goToLandlordDashboard()
                    dashboard.notificationBanner.incompletePropertiesLink.clickAndWait()
                    assertPageIs(page, LandlordIncompletePropertiesPage::class)
                }
            }
        }

        @Nested
        inner class WithOnlyComplianceActions {
            @Nested
            inner class WithOne : NestedIntegrationTestWithImmutableData("data-mockuser-landlord-with-one-compliance-action.sql") {
                @Test
                fun `the notification banner loads with correct message for one compliance action`(page: Page) {
                    val dashboard = navigator.goToLandlordDashboard()
                    assertThat(dashboard.notificationBanner.content.heading)
                        .containsText("You have 1 property awaiting compliance information: View compliance actions")
                }
            }

            @Nested
            inner class WithMultiple : NestedIntegrationTestWithImmutableData("data-mockuser-landlord-with-compliance-actions.sql") {
                @Test
                fun `the notification banner loads with correct message for multiple compliance actions`(page: Page) {
                    val dashboard = navigator.goToLandlordDashboard()
                    assertThat(dashboard.notificationBanner.content.heading)
                        .containsText("You have 5 properties awaiting compliance information: View compliance actions")
                }

                @Test
                fun `the add compliance information link redirects to the compliance actions page`(page: Page) {
                    val dashboard = navigator.goToLandlordDashboard()
                    dashboard.notificationBanner.addComplianceInformationLink.clickAndWait()
                    assertPageIs(page, OutstandingActionsPage::class)
                }
            }
        }

        @Nested
        inner class WithIncompletePropertiesAndComplianceActions :
            NestedIntegrationTestWithImmutableData("data-mockuser-landlord-with-incomplete-properties-and-compliance-actions.sql") {
            @Test
            fun `the notification banner loads with correct heading and messages`(page: Page) {
                val dashboard = navigator.goToLandlordDashboard()
                assertThat(dashboard.notificationBanner.content.heading).containsText("You have 4 outstanding property actions")
            }

            @Test
            fun `the view incomplete properties link redirects to the incomplete properties page`(page: Page) {
                val dashboard = navigator.goToLandlordDashboard()
                dashboard.notificationBanner.incompletePropertiesLink.clickAndWait()
                assertPageIs(page, LandlordIncompletePropertiesPage::class)
            }

            @Test
            fun `the add compliance information link redirects to the compliance actions page`(page: Page) {
                val dashboard = navigator.goToLandlordDashboard()
                dashboard.notificationBanner.addComplianceInformationLink.clickAndWait()
                assertPageIs(page, OutstandingActionsPage::class)
            }
        }
    }
}

@WithOrgLandlordProfile
class OrgLandlordDashboardTests : IntegrationTestWithImmutableData("data-local.sql") {
    @Test
    fun `the dashboard loads displaying the organisation name and lrn`() {
        val dashboard = navigator.goToLandlordDashboard()
        assertThat(dashboard.dashboardBannerHeading).containsText("Local Organisation Landlord")
        assertThat(dashboard.dashboardBannerSubHeading).containsText("Landlord registration number")
        assertThat(dashboard.dashboardBannerSubHeading).containsText("L-CH5R-2T2J")
    }
}
