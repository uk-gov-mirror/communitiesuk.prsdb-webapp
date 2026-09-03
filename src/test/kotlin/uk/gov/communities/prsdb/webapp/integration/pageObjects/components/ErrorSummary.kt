package uk.gov.communities.prsdb.webapp.integration.pageObjects.components

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

class ErrorSummary(
    parentLocator: Locator,
) : BaseComponent(parentLocator.locator(".govuk-error-summary")) {
    constructor(page: Page) : this(page.locator("html"))

    fun errorLinks(text: String): Locator = locator.locator("li", Locator.LocatorOptions().setHasText(text))
}
