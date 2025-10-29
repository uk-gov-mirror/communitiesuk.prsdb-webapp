package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent.Companion.assertThat
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LocalAuthorityDashboardPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LocalAuthorityViewLandlordDetailsPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.PropertyDetailsPageLocalAuthorityView
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.SearchLandlordRegisterPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.SearchLandlordRegisterPage.Companion.ADDRESS_COL_INDEX
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.SearchLandlordRegisterPage.Companion.CONTACT_INFO_COL_INDEX
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.SearchLandlordRegisterPage.Companion.LANDLORD_COL_INDEX
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.SearchLandlordRegisterPage.Companion.LISTED_PROPERTY_COL_INDEX
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.SearchPropertyRegisterPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.SearchPropertyRegisterPage.Companion.LA_COL_INDEX
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.SearchPropertyRegisterPage.Companion.PROPERTY_COL_INDEX
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.SearchPropertyRegisterPage.Companion.PROPERTY_LANDLORD_COL_INDEX
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.SearchPropertyRegisterPage.Companion.REG_NUM_COL_INDEX
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import kotlin.collections.mapOf
import kotlin.test.assertContains
import kotlin.test.assertTrue

class SearchRegisterTests : IntegrationTestWithImmutableData("data-search.sql") {
    @Nested
    inner class LandlordSearchTests {
        @Test
        fun `results table does not show before search has been requested`() {
            val searchLandlordRegisterPage = navigator.goToLandlordSearchPage()

            assertThat(searchLandlordRegisterPage.resultTable).isHidden()
        }

        @Test
        fun `results table does not show when blank search term requested`() {
            val searchLandlordRegisterPage = navigator.goToLandlordSearchPage()
            searchLandlordRegisterPage.searchBar.search("")

            assertThat(searchLandlordRegisterPage.resultTable).isHidden()
        }

        @Test
        fun `results table shows after (LRN) search has been requested`() {
            val searchLandlordRegisterPage = navigator.goToLandlordSearchPage()
            searchLandlordRegisterPage.searchBar.search("L-CKSQ-3SX9")
            val resultTable = searchLandlordRegisterPage.resultTable

            assertThat(resultTable.headerRow.getCell(LANDLORD_COL_INDEX)).containsText("Landlord")
            assertThat(resultTable.getCell(0, LANDLORD_COL_INDEX)).containsText("Alexander Smith\nL-CKSQ-3SX9")

            assertThat(resultTable.headerRow.getCell(ADDRESS_COL_INDEX)).containsText("Contact address")
            assertThat(resultTable.getCell(0, ADDRESS_COL_INDEX)).containsText("1 Fictional Road")

            assertThat(resultTable.headerRow.getCell(CONTACT_INFO_COL_INDEX)).containsText("Contact information")
            assertThat(
                resultTable.getCell(0, CONTACT_INFO_COL_INDEX),
            ).containsText("7111111111\nalex.surname@example.com")

            assertThat(resultTable.headerRow.getCell(LISTED_PROPERTY_COL_INDEX)).containsText("Listed properties")
            assertThat(resultTable.getCell(0, LISTED_PROPERTY_COL_INDEX)).containsText("30")

            assertTrue(searchLandlordRegisterPage.noResultErrorMessage.isHidden)
        }

        @Test
        fun `fuzzy search functionality produces table of matching results in expected order`() {
            val searchLandlordRegisterPage = navigator.goToLandlordSearchPage()
            searchLandlordRegisterPage.searchBar.search("Alex")
            val resultTable = searchLandlordRegisterPage.resultTable

            assertThat(resultTable.getCell(0, LANDLORD_COL_INDEX)).containsText("Alexander Smith")
            assertThat(resultTable.getCell(1, LANDLORD_COL_INDEX)).containsText("Alexandra Davies")
            assertThat(resultTable.getCell(2, LANDLORD_COL_INDEX)).containsText("Evan Alenandrescu")
        }

        @Test
        fun `landlord link goes to landlord details page`(page: Page) {
            val searchLandlordRegisterPage = navigator.goToLandlordSearchPage()
            searchLandlordRegisterPage.searchBar.search("L-CKSQ-3SX9")
            searchLandlordRegisterPage.getLandlordLink(rowIndex = 0).clickAndWait()

            assertPageIs(page, LocalAuthorityViewLandlordDetailsPage::class, mapOf("id" to "1"))
        }

        @Test
        fun `error shows if search has no results`() {
            val searchLandlordRegisterPage = navigator.goToLandlordSearchPage()
            searchLandlordRegisterPage.searchBar.search("non-matching searchTerm")

            assertContains(searchLandlordRegisterPage.errorMessageText!!, "No landlord record found")
        }

        @Test
        fun `property search link shows if search has no results`(page: Page) {
            val searchLandlordRegisterPage = navigator.goToLandlordSearchPage()
            searchLandlordRegisterPage.searchBar.search("non-matching searchTerm")
            searchLandlordRegisterPage.getPropertySearchLink().clickAndWait()

            assertPageIs(page, SearchPropertyRegisterPage::class)
        }

        @Test
        fun `pagination component does not show if there is only one page of results`(page: Page) {
            val searchLandlordRegisterPage = navigator.goToLandlordSearchPage()
            searchLandlordRegisterPage.searchBar.search("Alex")

            assertThat(searchLandlordRegisterPage.paginationComponent).isHidden()
        }

        @Test
        fun `pagination links lead to the intended pages`(page: Page) {
            val searchLandlordRegisterPage = navigator.goToLandlordSearchPage()
            searchLandlordRegisterPage.searchBar.search("PRSDB")

            searchLandlordRegisterPage.paginationComponent.nextLink.clickAndWait()
            assertContains(page.url(), "page=2")
            val nextPage = assertPageIs(page, SearchLandlordRegisterPage::class)

            nextPage.paginationComponent.previousLink.clickAndWait()
            assertContains(page.url(), "page=1")
            val previousPage = assertPageIs(page, SearchLandlordRegisterPage::class)

            previousPage.paginationComponent.getPageNumberLink(2).clickAndWait()
            assertContains(page.url(), "page=2")
            assertPageIs(page, SearchLandlordRegisterPage::class)
        }

        @Test
        fun `filter panel can be toggled and used to refine search results`(page: Page) {
            val searchLandlordRegisterPage = navigator.goToLandlordSearchPage()
            searchLandlordRegisterPage.searchBar.search("Alex")

            val filter = searchLandlordRegisterPage.filterPanel

            // Toggle filter
            filter.closeFilterPanelButton.clickAndWait()
            assertThat(filter.panel).isHidden()

            filter.showFilterPanelButton.clickAndWait()
            assertThat(filter.panel).isVisible()

            // Apply LA filter
            val laFilter = filter.getFilterCheckboxes("Show landlords operating in my council")
            laFilter.checkCheckbox("true")
            filter.clickApplyFiltersButton()

            val laFilterSelectedHeadingText = filter.selectedHeadings.first().innerText()
            assertContains(laFilterSelectedHeadingText, "Show landlords operating in my council")
            val resultTable = searchLandlordRegisterPage.resultTable
            assertEquals(1, resultTable.rows.count())

            // Remove LA filter
            filter.getRemoveFilterTag("Landlords in my council").clickAndWait()
            assertThat(filter.selectedHeadings).hasCount(0)
            assertThat(resultTable.rows).not().hasCount(0)

            // Clear all filters
            laFilter.checkCheckbox("true")
            filter.clickApplyFiltersButton()

            filter.clearFiltersLink.clickAndWait()
            assertThat(filter.clearFiltersLink).isHidden()
            assertThat(filter.noFiltersSelectedTextNode).isVisible()
            assertThat(resultTable.rows).not().hasCount(0)
        }

        @Test
        fun `selected filters persist across searches`(page: Page) {
            // Search
            val searchLandlordRegisterPage = navigator.goToLandlordSearchPage()
            searchLandlordRegisterPage.searchBar.search("Alex")

            // Apply LA filter
            val filter = searchLandlordRegisterPage.filterPanel
            val laFilter = filter.getFilterCheckboxes("Show landlords operating in my council")
            laFilter.checkCheckbox("true")
            filter.clickApplyFiltersButton()

            // Search again
            searchLandlordRegisterPage.searchBar.search("PRSD")
            assertThat(filter.getRemoveFilterTag("Landlords in my council")).isVisible()
        }

        @Test
        fun `Back link returns to the LA dashboard page`(page: Page) {
            val searchLandlordRegisterPage = navigator.goToLandlordSearchPage()
            searchLandlordRegisterPage.backLink.clickAndWait()
            assertPageIs(page, LocalAuthorityDashboardPage::class)
        }

        @Test
        fun `Back link on a landlord returns to the search`(page: Page) {
            val searchLandlordRegisterPage = navigator.goToLandlordSearchPage()
            searchLandlordRegisterPage.searchBar.search("L-CKSQ-3SX9")
            val resultTable = searchLandlordRegisterPage.resultTable

            resultTable.getClickableCell(0, LANDLORD_COL_INDEX).link.clickAndWait()

            val landlordPage = assertPageIs(page, LocalAuthorityViewLandlordDetailsPage::class, mapOf("id" to "1"))
            landlordPage.backLink.clickAndWait()

            assertPageIs(page, SearchLandlordRegisterPage::class)
            assertThat(resultTable.getCell(0, LANDLORD_COL_INDEX)).containsText("Alexander Smith\nL-CKSQ-3SX9")
        }
    }

    @Nested
    inner class PropertySearchTests {
        @Test
        fun `results table does not show before search has been requested`() {
            val searchPropertyRegisterPage = navigator.goToPropertySearchPage()

            assertThat(searchPropertyRegisterPage.resultTable).isHidden()
        }

        @Test
        fun `results table does not show when blank search term requested`() {
            val searchPropertyRegisterPage = navigator.goToPropertySearchPage()
            searchPropertyRegisterPage.searchBar.search("")

            assertThat(searchPropertyRegisterPage.resultTable).isHidden()
        }

        @Test
        fun `results table shows after (PRN) search has been requested`() {
            val searchPropertyRegisterPage = navigator.goToPropertySearchPage()
            searchPropertyRegisterPage.searchBar.search("P-CCCT-GRKQ")
            val resultTable = searchPropertyRegisterPage.resultTable

            assertThat(resultTable.headerRow.getCell(PROPERTY_COL_INDEX)).containsText("Property address")
            assertThat(resultTable.getCell(0, PROPERTY_COL_INDEX)).containsText("11 PRSDB Square, EG1 2AK")

            assertThat(resultTable.headerRow.getCell(REG_NUM_COL_INDEX)).containsText("Registration number")
            assertThat(resultTable.getCell(0, ADDRESS_COL_INDEX)).containsText("P-CCCT-GRKQ")

            assertThat(resultTable.headerRow.getCell(LA_COL_INDEX)).containsText("Local council")
            assertThat(resultTable.getCell(0, LA_COL_INDEX)).containsText("BATH AND NORTH EAST SOMERSET COUNCIL")

            assertThat(resultTable.headerRow.getCell(PROPERTY_LANDLORD_COL_INDEX)).containsText("Registered landlord")
            assertThat(resultTable.getCell(0, PROPERTY_LANDLORD_COL_INDEX)).containsText("Alexander Smith")

            assertTrue(searchPropertyRegisterPage.noResultErrorMessage.isHidden)
        }

        @Test
        fun `UPRN search produces an exact result`() {
            val searchPropertyRegisterPage = navigator.goToPropertySearchPage()
            searchPropertyRegisterPage.searchBar.search("1123456")
            val resultTable = searchPropertyRegisterPage.resultTable

            assertThat(resultTable.getCell(0, PROPERTY_COL_INDEX)).containsText("1, Example Road, EG")
        }

        @Test
        fun `Fuzzy search produces table of matching results`(page: Page) {
            val searchPropertyRegisterPage = navigator.goToPropertySearchPage()
            searchPropertyRegisterPage.searchBar.search("Fake Way")
            val resultTable = searchPropertyRegisterPage.resultTable

            assertThat(resultTable.getCell(0, PROPERTY_COL_INDEX)).containsText("3 Fake Way")
            assertThat(resultTable.getCell(1, PROPERTY_COL_INDEX)).containsText("5 Fake Crescent Way")
        }

        @Test
        fun `property link goes to property details page`(page: Page) {
            val searchPropertyRegisterPage = navigator.goToPropertySearchPage()
            searchPropertyRegisterPage.searchBar.search("P-C5YY-J34H")
            searchPropertyRegisterPage.getPropertyLink(rowIndex = 0).clickAndWait()
            assertPageIs(page, PropertyDetailsPageLocalAuthorityView::class, mapOf("propertyOwnershipId" to "1"))
        }

        @Test
        fun `landlord link goes to landlord details page`(page: Page) {
            val searchPropertyRegisterPage = navigator.goToPropertySearchPage()
            searchPropertyRegisterPage.searchBar.search("P-C5YY-J34H")
            searchPropertyRegisterPage.getLandlordLink(rowIndex = 0).clickAndWait()

            assertPageIs(page, LocalAuthorityViewLandlordDetailsPage::class, mapOf("id" to "1"))
        }

        @Test
        fun `error shows if search has no results`() {
            val searchPropertyRegisterPage = navigator.goToPropertySearchPage()
            searchPropertyRegisterPage.searchBar.search("non-matching searchTerm")

            assertContains(searchPropertyRegisterPage.errorMessageText!!, "No property record found")
        }

        @Test
        fun `landlord search link shows if search has no results`(page: Page) {
            val searchPropertyRegisterPage = navigator.goToPropertySearchPage()
            searchPropertyRegisterPage.searchBar.search("non-matching searchTerm")
            searchPropertyRegisterPage.getLandlordSearchLink().clickAndWait()

            assertPageIs(page, SearchLandlordRegisterPage::class)
        }

        @Test
        fun `pagination component does not show if there is only one page of results`(page: Page) {
            val searchPropertyRegisterPage = navigator.goToPropertySearchPage()
            searchPropertyRegisterPage.searchBar.search("Way")

            assertThat(searchPropertyRegisterPage.paginationComponent).isHidden()
        }

        @Test
        fun `pagination links lead to the intended pages`(page: Page) {
            val searchPropertyRegisterPage = navigator.goToPropertySearchPage()
            searchPropertyRegisterPage.searchBar.search("PRSDB")

            searchPropertyRegisterPage.paginationComponent.nextLink.clickAndWait()
            assertContains(page.url(), "page=2")
            val nextPage = assertPageIs(page, SearchPropertyRegisterPage::class)

            nextPage.paginationComponent.previousLink.clickAndWait()
            assertContains(page.url(), "page=1")
            val previousPage = assertPageIs(page, SearchPropertyRegisterPage::class)

            previousPage.paginationComponent.getPageNumberLink(2).clickAndWait()
            assertContains(page.url(), "page=2")
            assertPageIs(page, SearchPropertyRegisterPage::class)
        }

        @Test
        fun `filter panel can be toggled and used to refine search results`(page: Page) {
            val expectedMatchingPropertyCount = 5
            val expectedPropertyInLACount = 4
            val expectedPropertyInLAWithSelectiveLicenseCount = 1
            val expectedPropertyInLAWithSelectiveOrNoLicenseCount = 3
            val expectedPropertyWithSelectiveOrNoLicenseCount = 4

            // Initial search
            val searchPropertyRegisterPage = navigator.goToPropertySearchPage()
            searchPropertyRegisterPage.searchBar.search("Way")

            val resultTable = searchPropertyRegisterPage.resultTable
            assertEquals(expectedMatchingPropertyCount, resultTable.rows.count())

            // Toggle filter
            val filter = searchPropertyRegisterPage.filterPanel
            filter.closeFilterPanelButton.clickAndWait()
            assertTrue(filter.panel.isHidden)

            filter.showFilterPanelButton.clickAndWait()
            assertTrue(filter.panel.isVisible)

            // Apply LA filter
            val laFilter = filter.getFilterCheckboxes("Show properties in my council")
            laFilter.checkCheckbox("true")
            filter.clickApplyFiltersButton()

            val laFilterSelectedHeadingText = filter.selectedHeadings.nth(0).innerText()
            assertContains(laFilterSelectedHeadingText, "Show properties in my council")
            assertEquals(expectedPropertyInLACount, resultTable.rows.count())

            // Apply Selective license filter
            val licenseFilter = filter.getFilterCheckboxes("Property licence")
            licenseFilter.checkCheckbox(LicensingType.SELECTIVE_LICENCE.name)
            filter.clickApplyFiltersButton()

            val licenseFilterSelectedHeadingText = filter.selectedHeadings.nth(1).innerText()
            assertContains(licenseFilterSelectedHeadingText, "Property licence")
            assertEquals(expectedPropertyInLAWithSelectiveLicenseCount, resultTable.rows.count())

            // Apply No license filter
            licenseFilter.checkCheckbox(LicensingType.NO_LICENSING.name)
            filter.clickApplyFiltersButton()
            assertEquals(expectedPropertyInLAWithSelectiveOrNoLicenseCount, resultTable.rows.count())

            // Remove LA filter
            filter.getRemoveFilterTag("Properties in my council").clickAndWait()
            assertEquals(1, filter.selectedHeadings.count())
            assertEquals(expectedPropertyWithSelectiveOrNoLicenseCount, resultTable.rows.count())

            // Clear all filters
            filter.clearFiltersLink.clickAndWait()
            assertThat(filter.clearFiltersLink).isHidden()
            assertThat(filter.noFiltersSelectedTextNode).isVisible()
            assertEquals(expectedMatchingPropertyCount, resultTable.rows.count())
        }

        @Test
        fun `selected filters persist across searches`(page: Page) {
            // Search
            val searchPropertyRegisterPage = navigator.goToPropertySearchPage()
            searchPropertyRegisterPage.searchBar.search("Way")

            // Apply LA filter
            val filter = searchPropertyRegisterPage.filterPanel
            val laFilter = filter.getFilterCheckboxes("Show properties in my council")
            laFilter.checkCheckbox("true")
            filter.clickApplyFiltersButton()

            // Search again
            searchPropertyRegisterPage.searchBar.search("PRSD")
            assertThat(filter.getRemoveFilterTag("Properties in my council")).isVisible()
        }

        @Test
        fun `Back link returns to the LA dashboard page`(page: Page) {
            val searchPropertyRegisterPage = navigator.goToLandlordSearchPage()
            searchPropertyRegisterPage.backLink.clickAndWait()
            assertPageIs(page, LocalAuthorityDashboardPage::class)
        }

        @Test
        fun `Back link on a landlord returns to the search`(page: Page) {
            val searchPropertyRegisterPage = navigator.goToPropertySearchPage()
            searchPropertyRegisterPage.searchBar.search("P-CCCT-GRKQ")
            val resultTable = searchPropertyRegisterPage.resultTable

            resultTable.getClickableCell(0, PROPERTY_LANDLORD_COL_INDEX).link.clickAndWait()

            val landlordPage = assertPageIs(page, LocalAuthorityViewLandlordDetailsPage::class, mapOf("id" to "1"))
            landlordPage.backLink.clickAndWait()

            assertPageIs(page, SearchPropertyRegisterPage::class)
            assertThat(resultTable.getCell(0, PROPERTY_COL_INDEX)).containsText("11 PRSDB Square, EG1 2AK")
        }

        @Test
        fun `Back link on a property returns to the search`(page: Page) {
            val searchPropertyRegisterPage = navigator.goToPropertySearchPage()
            searchPropertyRegisterPage.searchBar.search("P-CCCT-GRKQ")
            val resultTable = searchPropertyRegisterPage.resultTable

            resultTable.getClickableCell(0, PROPERTY_COL_INDEX).link.clickAndWait()

            val landlordPage =
                assertPageIs(
                    page,
                    PropertyDetailsPageLocalAuthorityView::class,
                    mapOf("propertyOwnershipId" to "18"),
                )
            landlordPage.backLink.clickAndWait()

            assertPageIs(page, SearchPropertyRegisterPage::class)
            assertThat(resultTable.getCell(0, PROPERTY_COL_INDEX)).containsText("11 PRSDB Square, EG1 2AK")
        }
    }
}
