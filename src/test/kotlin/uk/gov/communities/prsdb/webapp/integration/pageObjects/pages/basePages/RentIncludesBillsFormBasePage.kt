package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BackLink
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.FormWithSectionHeader
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Radios

abstract class RentIncludesBillsFormBasePage(
    page: Page,
    urlSegment: String,
) : BasePage(page, urlSegment) {
    val form = RentIncludesBillsForm(page)
    val backLink = BackLink.default(page)

    fun submitIsIncluded() {
        form.includesBillsRadios.selectValue("true")
        form.submit()
    }

    fun submitIsNotIncluded() {
        form.includesBillsRadios.selectValue("false")
        form.submit()
    }

    class RentIncludesBillsForm(
        page: Page,
    ) : FormWithSectionHeader(page) {
        val includesBillsRadios = Radios(locator)
    }
}
