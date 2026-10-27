package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import uk.gov.communities.prsdb.webapp.constants.COMPLIANCE_INFO_FRAGMENT
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_PHASE_THREE
import uk.gov.communities.prsdb.webapp.integration.IntegrationTestWithImmutableData.NestedIntegrationTestWithImmutableData
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent.Companion.assertThat
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.PropertyDetailsPageLandlordView
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.ComplianceActionViewModelBuilder.Companion.DATE_FORMATTER
import java.time.LocalDate
import java.util.regex.Pattern
import kotlin.test.Test
import kotlin.test.assertEquals

// TODO PDJB-2006: Delete this class when the PROPERTY_REGISTRATION_PHASE_THREE flag is removed
class ComplianceActionsPageTests : IntegrationTest() {
    @BeforeEach
    fun disableFeatureFlag() {
        featureFlagManager.disableFeature(PROPERTY_REGISTRATION_PHASE_THREE)
    }

    @Nested
    inner class LandlordsWithoutComplianceActions :
        NestedIntegrationTestWithImmutableData("data-mockuser-landlord-with-properties.sql") {
        @Test
        fun `the page loads with heading and inset text`() {
            val complianceActionsPage = navigator.goToComplianceActions()
            assertThat(complianceActionsPage.heading).containsText("Compliance actions")
            assertThat(complianceActionsPage.hintText).isHidden()
            assertThat(complianceActionsPage.insetText).containsText("The certificates for your occupied properties are up to date.")
        }
    }

    @Nested
    inner class LandlordsWithComplianceActions :
        NestedIntegrationTestWithImmutableData("data-mockuser-landlord-with-compliance-actions.sql") {
        @Test
        fun `the page loads with heading and body text`() {
            val complianceActionsPage = navigator.goToComplianceActions()
            assertThat(complianceActionsPage.heading).containsText("Compliance actions")
            assertThat(complianceActionsPage.bodyText).containsText("Add certificates to these properties.")
        }

        @Test
        fun `summary cards are populated with correct content and actions`(page: Page) {
            var complianceActionsPage = navigator.goToComplianceActions()
            // Check compliance card - OCCUPIED, gas missing, eicr exempt, epc expired
            val completedComplianceCard = complianceActionsPage.findSummaryCard("4 Pretend Crescent")
            assertThat(completedComplianceCard.summaryList.registrationNumRow).containsText("P-CCCT-GRKC")
            assertThat(completedComplianceCard.summaryList.gasSafetyRow).containsText("No valid gas safety certificate")
            assertThat(completedComplianceCard.summaryList.electricalSafetyRow).isHidden()
            assertThat(completedComplianceCard.summaryList.energyPerformanceRow).containsText("Expired")

            completedComplianceCard.getAction("Go to property").link.clickAndWait()
            var propertyDetailsPage = assertPageIs(page, PropertyDetailsPageLandlordView::class, mapOf("propertyOwnershipId" to "3"))
            assertEquals(COMPLIANCE_INFO_FRAGMENT, propertyDetailsPage.tabs.activeTabPanelId)

            // Check compliance card - UNOCCUPIED, gas missing, eicr exempt, epc expired
            complianceActionsPage = navigator.goToComplianceActions()
            val secondComplianceCard = complianceActionsPage.findSummaryCard("5 Invented Lane")
            assertThat(secondComplianceCard.summaryList.registrationNumRow).containsText("P-CCCT-GRKF")
            assertThat(secondComplianceCard.summaryList.gasSafetyRow).isHidden()
            assertThat(secondComplianceCard.summaryList.electricalSafetyRow).isHidden()
            assertThat(secondComplianceCard.summaryList.energyPerformanceRow).containsText("Expired on")

            secondComplianceCard.getAction("Go to property").link.clickAndWait()
            propertyDetailsPage = assertPageIs(page, PropertyDetailsPageLandlordView::class, mapOf("propertyOwnershipId" to "4"))
            assertEquals(COMPLIANCE_INFO_FRAGMENT, propertyDetailsPage.tabs.activeTabPanelId)

            // Check compliance card - OCCUPIED, gas valid, eicr missing, epc in date but low rating
            complianceActionsPage = navigator.goToComplianceActions()
            val thirdComplianceCard = complianceActionsPage.findSummaryCard("2 Fake Way")
            assertThat(thirdComplianceCard.summaryList.registrationNumRow).containsText("P-CCCT-GRJ5")
            assertThat(thirdComplianceCard.summaryList.gasSafetyRow).isHidden()
            assertThat(thirdComplianceCard.summaryList.electricalSafetyRow).containsText("No valid electrical safety certificate")
            assertThat(thirdComplianceCard.summaryList.energyPerformanceRow).containsText("No valid energy performance certificate (EPC)")

            // Check compliance card - UNOCCUPIED, gas valid, eicr missing, epc low rating
            complianceActionsPage = navigator.goToComplianceActions()
            assertThat(complianceActionsPage.findSummaryCard("3 Imaginary Street")).isHidden()
        }

        @Test
        fun `summary cards show occupied status tag for occupied properties`() {
            val complianceActionsPage = navigator.goToComplianceActions()
            val occupiedCard = complianceActionsPage.findSummaryCard("4 Pretend Crescent")
            assertThat(occupiedCard.summaryList.statusRow).containsText("Occupied")
            val tag =
                occupiedCard.summaryList.statusRow.value
                    .locator(".govuk-tag")
            PlaywrightAssertions.assertThat(tag).isVisible()
            PlaywrightAssertions.assertThat(tag).hasClass(Pattern.compile(".*govuk-tag--pink.*"))
        }

        @Test
        fun `summary cards show unoccupied status tag for unoccupied properties`() {
            val complianceActionsPage = navigator.goToComplianceActions()
            val unoccupiedCard = complianceActionsPage.findSummaryCard("5 Invented Lane")
            assertThat(unoccupiedCard.summaryList.statusRow).containsText("Unoccupied")
            val tag =
                unoccupiedCard.summaryList.statusRow.value
                    .locator(".govuk-tag")
            PlaywrightAssertions.assertThat(tag).isVisible()
            PlaywrightAssertions.assertThat(tag).hasClass(Pattern.compile(".*govuk-tag--grey.*"))
        }

        @Test
        fun `gas safety row shows provide this later message for provide later status`() {
            val complianceActionsPage = navigator.goToComplianceActions()
            val card = complianceActionsPage.findSummaryCard("6 Fabricated Avenue")
            val expectedDate = LocalDate.now().plusDays(28).format(DATE_FORMATTER)
            assertThat(card.summaryList.gasSafetyRow).containsText("Provide this later (before $expectedDate)")
        }

        @Test
        fun `gas safety row shows expired on date for expired certificate`() {
            val complianceActionsPage = navigator.goToComplianceActions()
            val card = complianceActionsPage.findSummaryCard("7 Mythical Drive")
            val expectedDate = LocalDate.now().minusDays(1).format(DATE_FORMATTER)
            assertThat(card.summaryList.gasSafetyRow).containsText("Expired on $expectedDate")
        }

        @Test
        fun `gas safety row shows no valid certificate for not added status`() {
            val complianceActionsPage = navigator.goToComplianceActions()
            val card = complianceActionsPage.findSummaryCard("4 Pretend Crescent")
            assertThat(card.summaryList.gasSafetyRow).containsText("No valid gas safety certificate")
        }

        @Test
        fun `electrical safety row shows provide this later message for provide later status`() {
            val complianceActionsPage = navigator.goToComplianceActions()
            val card = complianceActionsPage.findSummaryCard("6 Fabricated Avenue")
            val expectedDate = LocalDate.now().plusDays(28).format(DATE_FORMATTER)
            assertThat(card.summaryList.electricalSafetyRow).containsText("Provide this later (before $expectedDate)")
        }

        @Test
        fun `electrical safety row shows expired on date for expired certificate`() {
            val complianceActionsPage = navigator.goToComplianceActions()
            val card = complianceActionsPage.findSummaryCard("7 Mythical Drive")
            val expectedDate = LocalDate.now().minusDays(1).format(DATE_FORMATTER)
            assertThat(card.summaryList.electricalSafetyRow).containsText("Expired on $expectedDate")
        }

        @Test
        fun `electrical safety row shows no valid certificate for not added status`() {
            val complianceActionsPage = navigator.goToComplianceActions()
            val card = complianceActionsPage.findSummaryCard("2 Fake Way")
            assertThat(card.summaryList.electricalSafetyRow).containsText("No valid electrical safety certificate")
        }

        @Nested
        inner class EpcComplianceActions :
            NestedIntegrationTestWithImmutableData("data-mockuser-landlord-with-epc-compliance-actions.sql") {
            @Test
            fun `occupied property with provide later shows provide this later message`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC Provide Later Occupied")
                val expectedDate = LocalDate.now().plusDays(28).format(DATE_FORMATTER)
                assertThat(card.summaryList.energyPerformanceRow).containsText("Provide this later (before $expectedDate)")
            }

            @Test
            fun `occupied after registration property with provide later shows undated provide this later message`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC Provide Later Occupied After Registration")
                assertThat(card.summaryList.energyPerformanceRow)
                    .containsText("Provide this later (within 28 days of the property being occupied)")
            }

            @Test
            fun `unoccupied property with provide later does not show epc row`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC Provide Later Unoccupied")
                assertThat(card.summaryList.energyPerformanceRow).isHidden()
            }

            @Test
            fun `occupied property with valid high rating does not show epc row`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC Valid High Rating Occupied")
                assertThat(card.summaryList.energyPerformanceRow).isHidden()
            }

            @Test
            fun `unoccupied property with valid high rating does not show epc row`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC Valid High Rating Unoccupied")
                assertThat(card.summaryList.energyPerformanceRow).isHidden()
            }

            @Test
            fun `occupied property with valid low rating and exemption does not show epc row`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC Valid Low Exempt Occupied")
                assertThat(card.summaryList.energyPerformanceRow).isHidden()
            }

            @Test
            fun `unoccupied property with valid low rating and exemption does not show epc row`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC Valid Low Exempt Unoccupied")
                assertThat(card.summaryList.energyPerformanceRow).isHidden()
            }

            @Test
            fun `occupied property with valid low rating and no exemption shows no valid certificate`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC Valid Low No Exempt Occupied")
                assertThat(card.summaryList.energyPerformanceRow).containsText("No valid energy performance certificate (EPC)")
            }

            @Test
            fun `unoccupied property with valid low rating and no exemption does not show epc row`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC Valid Low No Exempt Unoccupied")
                assertThat(card.summaryList.energyPerformanceRow).isHidden()
            }

            @Test
            fun `occupied property with expired epc and tenancy before expiry and high rating shows expired on date`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC Expired Tenancy Before High Occupied")
                val expectedDate = LocalDate.now().minusDays(1).format(DATE_FORMATTER)
                assertThat(card.summaryList.energyPerformanceRow).containsText("Expired on $expectedDate")
                assertThat(card.epcInsetText).isVisible()
            }

            @Test
            fun `unoccupied property with expired epc and high rating shows expired on date`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC Expired Tenancy Before High Unoccupied")
                val expectedDate = LocalDate.now().minusDays(1).format(DATE_FORMATTER)
                assertThat(card.summaryList.energyPerformanceRow).containsText("Expired on $expectedDate")
                assertThat(card.epcInsetText).isHidden()
            }

            @Test
            fun `occupied property with expired epc and tenancy before expiry and low rating with exemption shows expired on date`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC Expired Tenancy Before Low Exempt Occupied")
                val expectedDate = LocalDate.now().minusDays(1).format(DATE_FORMATTER)
                assertThat(card.summaryList.energyPerformanceRow).containsText("Expired on $expectedDate")
                assertThat(card.epcInsetText).isVisible()
            }

            @Test
            fun `unoccupied property with expired epc and low rating with exemption shows expired on date`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC Expired Tenancy Before Low Exempt Unoccupied")
                val expectedDate = LocalDate.now().minusDays(1).format(DATE_FORMATTER)
                assertThat(card.summaryList.energyPerformanceRow).containsText("Expired on $expectedDate")
                assertThat(card.epcInsetText).isHidden()
            }

            @Test
            fun `occupied property with expired epc and tenancy before expiry and low rating without exemption shows no valid cert`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC Expired Tenancy Before Low No Exempt Occupied")
                assertThat(card.summaryList.energyPerformanceRow).containsText("No valid energy performance certificate (EPC)")
                assertThat(card.epcInsetText).isHidden()
            }

            @Test
            fun `unoccupied property with expired epc and low rating without exemption shows expired on date`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC Expired Tenancy Before Low No Exempt Unoccupied")
                val expectedDate = LocalDate.now().minusDays(1).format(DATE_FORMATTER)
                assertThat(card.summaryList.energyPerformanceRow).containsText("Expired on $expectedDate")
                assertThat(card.epcInsetText).isHidden()
            }

            @Test
            fun `occupied property with expired epc not in date when tenancy began shows expired`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC Expired Not In Date Occupied")
                val expectedDate = LocalDate.now().minusDays(1).format(DATE_FORMATTER)
                assertThat(card.summaryList.energyPerformanceRow).containsText("Expired on $expectedDate")
                assertThat(card.epcInsetText).isHidden()
            }

            @Test
            fun `unoccupied property with expired epc not in date when tenancy began shows expired on date`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC Expired Not In Date Unoccupied")
                val expectedDate = LocalDate.now().minusDays(1).format(DATE_FORMATTER)
                assertThat(card.summaryList.energyPerformanceRow).containsText("Expired on $expectedDate")
                assertThat(card.epcInsetText).isHidden()
            }

            @Test
            fun `occupied property with no epc required shows no valid certificate`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC No EPC Required Occupied")
                assertThat(card.summaryList.energyPerformanceRow).containsText("No valid energy performance certificate (EPC)")
            }

            @Test
            fun `unoccupied property with no epc required does not show epc row`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC No EPC Required Unoccupied")
                assertThat(card.summaryList.energyPerformanceRow).isHidden()
            }

            @Test
            fun `occupied property with no epc not required does not show epc row`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC No EPC Not Required Occupied")
                assertThat(card.summaryList.energyPerformanceRow).isHidden()
            }

            @Test
            fun `unoccupied property with no epc not required does not show epc row`() {
                val complianceActionsPage = navigator.goToComplianceActions()
                val card = complianceActionsPage.findSummaryCard("EPC No EPC Not Required Unoccupied")
                assertThat(card.summaryList.energyPerformanceRow).isHidden()
            }
        }
    }

    @Nested
    inner class ComplianceActionsPagination :
        NestedIntegrationTestWithImmutableData("data-mockuser-landlord-with-many-compliance-actions.sql") {
        @Test
        fun `pagination is displayed when there are more items than the page size`(page: Page) {
            val complianceActionsPage = navigator.goToComplianceActions()
            assertThat(complianceActionsPage.pagination).isVisible()
        }
    }

    @Nested
    inner class LandlordsWithFewComplianceActions :
        NestedIntegrationTestWithImmutableData("data-mockuser-landlord-with-compliance-actions.sql") {
        @Test
        fun `pagination is not displayed when items fit on one page`(page: Page) {
            val complianceActionsPage = navigator.goToComplianceActions()
            assertThat(complianceActionsPage.pagination).not().isVisible()
        }
    }
}
