package uk.gov.communities.prsdb.webapp.integration.pageObjects.components

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

class ServiceNavigation(
    parentLocator: Locator,
) : BaseComponent(parentLocator.locator(SELECTOR)) {
    constructor(page: Page) : this(page.locator("html"))

    val serviceName = parentLocator.locator("$SELECTOR .govuk-service-navigation__service-name")

    companion object {
        const val SELECTOR = ".govuk-service-navigation"
    }
}
