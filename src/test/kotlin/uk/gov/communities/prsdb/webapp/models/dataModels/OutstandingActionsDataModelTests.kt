package uk.gov.communities.prsdb.webapp.models.dataModels

import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import uk.gov.communities.prsdb.webapp.constants.PROVIDE_LATER_DEADLINE_DAYS
import uk.gov.communities.prsdb.webapp.helpers.DateTimeHelper
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyComplianceBuilder
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockPropertyComplianceData
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OutstandingActionsDataModelTests {
    @Test
    fun `fromPropertyOwnership shows licensing and tenancy actions for an occupied property with both provided later`() {
        val propertyOwnership =
            MockLandlordData.createOccupiedPropertyOwnership(licenseProvideLater = true, tenancyProvideLater = true)

        val dataModel = OutstandingActionsDataModel.fromPropertyOwnership(propertyOwnership)

        assertTrue(dataModel.shouldShowLicensingAction)
        assertTrue(dataModel.shouldShowTenancyAction)
        assertTrue(dataModel.hasPropertyDetailsAction)
        assertTrue(dataModel.shouldShowOnOutstandingActionsPage)
    }

    @Test
    fun `fromPropertyOwnership does not show a licensing action for an unoccupied property with licensing provided later`() {
        val propertyOwnership = MockLandlordData.createUnoccupiedPropertyOwnership(licenseProvideLater = true)

        val dataModel = OutstandingActionsDataModel.fromPropertyOwnership(propertyOwnership)

        assertFalse(dataModel.shouldShowLicensingAction)
        assertFalse(dataModel.shouldShowTenancyAction)
        assertFalse(dataModel.shouldShowOnOutstandingActionsPage)
    }

    @Test
    fun `fromPropertyOwnership has no compliance status when the property has no compliance record`() {
        val propertyOwnership = MockLandlordData.createOccupiedPropertyOwnership(tenancyProvideLater = true)

        val dataModel = OutstandingActionsDataModel.fromPropertyOwnership(propertyOwnership)

        assertNull(dataModel.complianceStatus)
        assertTrue(dataModel.shouldShowOnOutstandingActionsPage)
    }

    @Test
    fun `fromPropertyOwnership shows the property when only compliance actions are outstanding`() {
        val propertyOwnership = MockLandlordData.createOccupiedPropertyOwnership()
        val propertyCompliance =
            PropertyComplianceBuilder()
                .withPropertyOwnership(propertyOwnership)
                .withHasGasSupply()
                .withGasSafetyCertProvideLater()
                .build()
        ReflectionTestUtils.setField(propertyOwnership, "propertyCompliance", propertyCompliance)

        val dataModel = OutstandingActionsDataModel.fromPropertyOwnership(propertyOwnership)

        assertNotNull(dataModel.complianceStatus)
        assertFalse(dataModel.hasPropertyDetailsAction)
        assertTrue(dataModel.shouldShowOnOutstandingActionsPage)
    }

    @Test
    fun `fromPropertyOwnership does not show the property when nothing is outstanding`() {
        val propertyOwnership = MockLandlordData.createOccupiedPropertyOwnership()
        MockPropertyComplianceData.createPropertyComplianceForOwnership(propertyOwnership)

        val dataModel = OutstandingActionsDataModel.fromPropertyOwnership(propertyOwnership)

        assertFalse(dataModel.shouldShowOnOutstandingActionsPage)
    }

    @Test
    fun `fromPropertyOwnership anchors the provide-later deadline to the registration date when occupied since registration`() {
        val registrationDate = LocalDate.of(2025, 3, 1)
        val propertyOwnership =
            MockLandlordData.createOccupiedPropertyOwnership(
                createdDate = registrationDate.atStartOfDay(DateTimeHelper.UK_ZONE).toInstant(),
                lastOccupiedDate = registrationDate,
                licenseProvideLater = true,
            )

        val dataModel = OutstandingActionsDataModel.fromPropertyOwnership(propertyOwnership)

        assertEquals(registrationDate.plusDays(PROVIDE_LATER_DEADLINE_DAYS.toLong()), dataModel.provideLaterDeadline)
    }
}
