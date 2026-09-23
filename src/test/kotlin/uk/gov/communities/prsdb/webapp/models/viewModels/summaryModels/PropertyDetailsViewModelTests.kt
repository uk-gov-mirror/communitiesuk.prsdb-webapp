package uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.config.YamlMessageSource
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.RentFrequency
import uk.gov.communities.prsdb.webapp.controllers.LandlordUpdateCorrespondenceAddressController
import uk.gov.communities.prsdb.webapp.controllers.UpdateBedroomsController
import uk.gov.communities.prsdb.webapp.database.entity.License
import uk.gov.communities.prsdb.webapp.helpers.DateTimeHelper
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.BedroomsStep
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.LookupAddressStep
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData.Companion.createAddress
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData.Companion.createOccupiedPropertyOwnership
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData.Companion.createPropertyOwnership
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData.Companion.createUnoccupiedPropertyOwnership
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockMessageSource
import java.time.LocalDate
import java.util.Locale

class PropertyDetailsViewModelTests {
    private val mockMessageSource = MockMessageSource()

    private val yamlMessageSource = YamlMessageSource("classpath:messages")

    // A property "occupied when registered" has a lastOccupiedDate matching its registration (created) date.
    private val occupiedAtRegistrationDate = LocalDate.of(2025, 1, 1)
    private val occupiedAtRegistrationInstant =
        occupiedAtRegistrationDate.atStartOfDay(DateTimeHelper.UK_ZONE).toInstant()

    @Test
    fun `property details section is in the correct order`() {
        val propertyOwnership =
            createOccupiedPropertyOwnership(
                address = createAddress(uprn = 1234.toLong()),
                numberOfBedrooms = 2,
            )

        val expectedHeaderList =
            listOf(
                "propertyDetails.propertyRecord.propertyDetails.address",
                "propertyDetails.propertyRecord.localCouncil",
                "propertyDetails.propertyRecord.propertyType",
                "propertyDetails.propertyRecord.tenancyAndRentalInformation.numberOfBedrooms",
            )

        val viewModel =
            PropertyDetailsViewModel(propertyOwnership, messageSource = mockMessageSource)

        assertEquals(expectedHeaderList, viewModel.propertyDetailsSection.map { it.fieldHeading })
    }

    @Test
    fun `property details section renders the address as multiple lines`() {
        val propertyOwnership = createOccupiedPropertyOwnership(address = createAddress(uprn = 1234.toLong()))

        val viewModel =
            PropertyDetailsViewModel(propertyOwnership, messageSource = mockMessageSource)

        val addressValue =
            viewModel.propertyDetailsSection
                .single { it.fieldHeading == "propertyDetails.propertyRecord.propertyDetails.address" }
                .fieldValue

        assertEquals(propertyOwnership.address.toMultiLineAddress().split("\n"), addressValue)
    }

    @Test
    fun `bedrooms row shows the not added message key when no bedroom count was entered`() {
        val propertyOwnership = createUnoccupiedPropertyOwnership()

        val viewModel =
            PropertyDetailsViewModel(propertyOwnership, messageSource = mockMessageSource)

        assertEquals(
            "propertyDetails.propertyRecord.tenancyAndRentalInformation.numberOfBedrooms.notAdded",
            bedroomsRow(viewModel).fieldValue,
        )
    }

    @Test
    fun `bedrooms row shows the bedroom count when a bedroom count was entered`() {
        val propertyOwnership = createOccupiedPropertyOwnership(numberOfBedrooms = 3)

        val viewModel =
            PropertyDetailsViewModel(propertyOwnership, messageSource = mockMessageSource)

        assertEquals(3, bedroomsRow(viewModel).fieldValue)
    }

    @Test
    fun `bedrooms row keeps its update action for a landlord when no bedroom count was entered`() {
        val propertyOwnership = createUnoccupiedPropertyOwnership(id = 123)

        val viewModel =
            PropertyDetailsViewModel(
                propertyOwnership,
                isLandlordView = true,
                messageSource = mockMessageSource,
            )

        assertEquals(
            UpdateBedroomsController.getUpdateBedroomsRoute(propertyOwnership.id) + "/${BedroomsStep.ROUTE_SEGMENT}",
            bedroomsRow(viewModel)
                .actions
                .single()
                .url,
        )
    }

    @Test
    fun `bedrooms row shows the not added message key with no update action for a council`() {
        val propertyOwnership = createUnoccupiedPropertyOwnership()

        val viewModel =
            PropertyDetailsViewModel(
                propertyOwnership,
                isLandlordView = false,
                messageSource = mockMessageSource,
            )

        assertEquals(
            "propertyDetails.propertyRecord.tenancyAndRentalInformation.numberOfBedrooms.notAdded",
            bedroomsRow(viewModel).fieldValue,
        )
        assertTrue(bedroomsRow(viewModel).actions.isEmpty()) {
            "The local council view must not show a change link for the bedrooms row"
        }
    }

    @Test
    fun `bedrooms not added message key resolves to a message`() {
        val propertyOwnership = createUnoccupiedPropertyOwnership()

        val viewModel =
            PropertyDetailsViewModel(propertyOwnership, messageSource = mockMessageSource)

        val messageKey = bedroomsRow(viewModel).fieldValue as String

        val resolvedMessage = yamlMessageSource.getMessage(messageKey, null, messageKey, Locale.getDefault())

        assertNotEquals(messageKey, resolvedMessage) {
            "Message key '$messageKey' does not resolve — it would display as the raw key on the page"
        }
    }

    @Test
    fun `registration, ownership and occupation sections contain the expected rows`() {
        val propertyOwnership = createOccupiedPropertyOwnership()

        val viewModel =
            PropertyDetailsViewModel(propertyOwnership, messageSource = mockMessageSource)

        assertEquals(
            listOf(
                "propertyDetails.propertyRecord.registrationNumber",
                "propertyDetails.propertyRecord.registrationDate",
            ),
            viewModel.registrationDetails.map { it.fieldHeading },
        )
        assertEquals(
            listOf("propertyDetails.propertyRecord.ownership.ownershipType"),
            viewModel.ownershipSection.map { it.fieldHeading },
        )
        assertEquals(
            listOf("propertyDetails.propertyRecord.occupation.isOccupied"),
            viewModel.occupiedSection.map { it.fieldHeading },
        )
    }

    @Test
    fun `shows a licensing provide-later row for a landlord when licensing is skipped on an occupied property`() {
        val propertyOwnership =
            createOccupiedPropertyOwnership(
                license = null,
                lastOccupiedDate = LocalDate.of(2025, 1, 1),
                licenseProvideLater = true,
            )

        val viewModel =
            PropertyDetailsViewModel(
                propertyOwnership,
                isLandlordView = true,
                messageSource = mockMessageSource,
            )

        assertEquals(
            listOf("propertyDetails.propertyRecord.licensing.rowName"),
            viewModel.licensingSection.map { it.fieldHeading },
        )
        assertNull(viewModel.licensingProvideLaterParagraph)
    }

    @Test
    fun `shows a licensing deadline paragraph for a council when an occupied-at-registration property skips licensing`() {
        val propertyOwnership =
            createOccupiedPropertyOwnership(
                license = null,
                createdDate = occupiedAtRegistrationInstant,
                lastOccupiedDate = occupiedAtRegistrationDate,
                licenseProvideLater = true,
            )

        val viewModel =
            PropertyDetailsViewModel(
                propertyOwnership,
                isLandlordView = false,
                messageSource = mockMessageSource,
            )

        assertTrue(viewModel.licensingSection.isEmpty())
        assertEquals(
            "Message for propertyDetails.propertyRecord.licensing.councilOccupied",
            viewModel.licensingProvideLaterParagraph,
        )
    }

    @Test
    fun `shows a not-provided licensing paragraph for a council when a property became occupied after registration`() {
        val propertyOwnership =
            createOccupiedPropertyOwnership(
                license = null,
                createdDate = occupiedAtRegistrationInstant,
                lastOccupiedDate = occupiedAtRegistrationDate.plusDays(30),
                licenseProvideLater = true,
            )

        val viewModel =
            PropertyDetailsViewModel(
                propertyOwnership,
                isLandlordView = false,
                messageSource = mockMessageSource,
            )

        assertTrue(viewModel.licensingSection.isEmpty())
        assertEquals(
            "Message for propertyDetails.propertyRecord.licensing.councilNotProvided",
            viewModel.licensingProvideLaterParagraph,
        )
    }

    @Test
    fun `shows a no-deadline licensing provide-later row for a landlord when a property became occupied after registration`() {
        val propertyOwnership =
            createOccupiedPropertyOwnership(
                license = null,
                createdDate = occupiedAtRegistrationInstant,
                lastOccupiedDate = occupiedAtRegistrationDate.plusDays(30),
                licenseProvideLater = true,
            )

        val viewModel =
            PropertyDetailsViewModel(
                propertyOwnership,
                isLandlordView = true,
                messageSource = mockMessageSource,
            )

        val licensingRow = viewModel.licensingSection.single()
        assertEquals("propertyDetails.propertyRecord.licensing.provideLaterNoDeadline", licensingRow.fieldValue)
    }

    @Test
    fun `shows a licensing not-provided paragraph for a local council when licensing is skipped on an unoccupied property`() {
        val propertyOwnership = createPropertyOwnership(license = null, licenseProvideLater = true)

        val viewModel =
            PropertyDetailsViewModel(
                propertyOwnership,
                isLandlordView = false,
                messageSource = mockMessageSource,
            )

        assertEquals(
            "Message for propertyDetails.propertyRecord.licensing.councilNotProvided",
            viewModel.licensingProvideLaterParagraph,
        )
    }

    @Test
    fun `shows a no-deadline licensing provide-later row for a landlord when licensing is skipped on an unoccupied property`() {
        val propertyOwnership = createUnoccupiedPropertyOwnership(licenseProvideLater = true)

        val viewModel =
            PropertyDetailsViewModel(
                propertyOwnership,
                isLandlordView = true,
                messageSource = mockMessageSource,
            )

        val licensingRow = viewModel.licensingSection.single()
        assertEquals("propertyDetails.propertyRecord.licensing.rowName", licensingRow.fieldHeading)
        assertEquals("propertyDetails.propertyRecord.licensing.provideLaterNoDeadline", licensingRow.fieldValue)
        assertNull(viewModel.licensingProvideLaterParagraph)
    }

    @Test
    fun `shows the licensing type row when a license is present`() {
        val propertyOwnership =
            createOccupiedPropertyOwnership(
                license = License(LicensingType.HMO_MANDATORY_LICENCE, "L1234"),
            )

        val viewModel =
            PropertyDetailsViewModel(propertyOwnership, messageSource = mockMessageSource)

        assertEquals(
            listOf(
                "propertyDetails.propertyRecord.licensingInformation.licensingType",
                "propertyDetails.propertyRecord.licensingInformation.licensingNumber",
            ),
            viewModel.licensingSection.map { it.fieldHeading },
        )
        assertNull(viewModel.licensingProvideLaterParagraph)
    }

    @Test
    fun `hides the licensing number row and shows a no-licensing type for a no-licensing record or no license`() {
        val declaredNoLicenseViewModel =
            PropertyDetailsViewModel(
                createOccupiedPropertyOwnership(license = License(LicensingType.NO_LICENSING, "")),
                messageSource = mockMessageSource,
            )
        val nullLicenseViewModel =
            PropertyDetailsViewModel(
                createOccupiedPropertyOwnership(license = null),
                messageSource = mockMessageSource,
            )

        listOf(declaredNoLicenseViewModel, nullLicenseViewModel).forEach { viewModel ->
            assertEquals(
                listOf("propertyDetails.propertyRecord.licensingInformation.licensingType"),
                viewModel.licensingSection.map { it.fieldHeading },
            )
            assertEquals(
                "forms.checkPropertyAnswers.propertyDetails.noLicensing.old",
                viewModel.licensingSection.single().fieldValue,
            )
        }
    }

    @Test
    fun `shows a tenancy provide-later row for a landlord when tenancy is skipped on an occupied property`() {
        val propertyOwnership =
            createOccupiedPropertyOwnership(
                currentNumHouseholds = 0,
                lastOccupiedDate = LocalDate.of(2025, 1, 1),
                tenancyProvideLater = true,
            )

        val viewModel =
            PropertyDetailsViewModel(
                propertyOwnership,
                isLandlordView = true,
                messageSource = mockMessageSource,
            )

        assertEquals("propertyDetails.propertyRecord.tenancy.heading", viewModel.tenancyHeadingKey)
        assertEquals(
            listOf("propertyDetails.propertyRecord.tenancy.rowName"),
            viewModel.tenancySection.map { it.fieldHeading },
        )
        assertNull(viewModel.tenancyProvideLaterParagraph)
    }

    @Test
    fun `shows a deadline tenancy provide-later row for a landlord for an occupied-at-registration property`() {
        val propertyOwnership =
            createOccupiedPropertyOwnership(
                currentNumHouseholds = 0,
                createdDate = occupiedAtRegistrationInstant,
                lastOccupiedDate = occupiedAtRegistrationDate,
                tenancyProvideLater = true,
            )

        val viewModel =
            PropertyDetailsViewModel(
                propertyOwnership,
                isLandlordView = true,
                messageSource = mockMessageSource,
            )

        assertEquals(
            "Message for propertyDetails.propertyRecord.tenancy.provideLaterWithDeadline",
            viewModel.tenancySection.single().fieldValue,
        )
    }

    @Test
    fun `shows a no-deadline tenancy provide-later row for a landlord when a property became occupied after registration`() {
        val propertyOwnership =
            createOccupiedPropertyOwnership(
                currentNumHouseholds = 0,
                createdDate = occupiedAtRegistrationInstant,
                lastOccupiedDate = occupiedAtRegistrationDate.plusDays(30),
                tenancyProvideLater = true,
            )

        val viewModel =
            PropertyDetailsViewModel(
                propertyOwnership,
                isLandlordView = true,
                messageSource = mockMessageSource,
            )

        assertEquals(
            "propertyDetails.propertyRecord.tenancy.provideLaterNoDeadline",
            viewModel.tenancySection.single().fieldValue,
        )
    }

    @Test
    fun `shows a tenancy deadline paragraph for a council when an occupied-at-registration property skips tenancy`() {
        val propertyOwnership =
            createOccupiedPropertyOwnership(
                currentNumHouseholds = 0,
                createdDate = occupiedAtRegistrationInstant,
                lastOccupiedDate = occupiedAtRegistrationDate,
                tenancyProvideLater = true,
            )

        val viewModel =
            PropertyDetailsViewModel(
                propertyOwnership,
                isLandlordView = false,
                messageSource = mockMessageSource,
            )

        assertTrue(viewModel.tenancySection.isEmpty())
        assertEquals(
            "Message for propertyDetails.propertyRecord.tenancy.councilOccupied",
            viewModel.tenancyProvideLaterParagraph,
        )
    }

    @Test
    fun `shows a not-provided tenancy paragraph for a council when a property became occupied after registration`() {
        val propertyOwnership =
            createOccupiedPropertyOwnership(
                currentNumHouseholds = 0,
                createdDate = occupiedAtRegistrationInstant,
                lastOccupiedDate = occupiedAtRegistrationDate.plusDays(30),
                tenancyProvideLater = true,
            )

        val viewModel =
            PropertyDetailsViewModel(
                propertyOwnership,
                isLandlordView = false,
                messageSource = mockMessageSource,
            )

        assertTrue(viewModel.tenancySection.isEmpty())
        assertEquals(
            "Message for propertyDetails.propertyRecord.tenancy.councilNotProvided",
            viewModel.tenancyProvideLaterParagraph,
        )
    }

    @Test
    fun `hides the tenancy section for a landlord when the property is unoccupied`() {
        val propertyOwnership = createUnoccupiedPropertyOwnership()

        val viewModel =
            PropertyDetailsViewModel(
                propertyOwnership,
                isLandlordView = true,
                messageSource = mockMessageSource,
            )

        assertTrue(viewModel.tenancySection.isEmpty())
    }

    @Test
    fun `hides the tenancy section for a council when the property is unoccupied`() {
        val propertyOwnership = createUnoccupiedPropertyOwnership()

        val viewModel =
            PropertyDetailsViewModel(
                propertyOwnership,
                isLandlordView = false,
                messageSource = mockMessageSource,
            )

        assertTrue(viewModel.tenancySection.isEmpty())
        assertNull(viewModel.tenancyProvideLaterParagraph)
    }

    @Test
    fun `shows the full tenancy rows when tenancy details are provided`() {
        val propertyOwnership =
            createOccupiedPropertyOwnership(
                billsIncludedList = null,
                customBillsIncluded = null,
                rentFrequency = RentFrequency.MONTHLY,
                customRentFrequency = null,
            )

        val expectedHeaderList =
            listOf(
                "propertyDetails.propertyRecord.tenancyAndRentalInformation.numberOfHouseholds.rowName",
                "propertyDetails.propertyRecord.tenancyAndRentalInformation.numberOfPeople",
                "propertyDetails.propertyRecord.tenancyAndRentalInformation.rentIncludesBills.rowName",
                "propertyDetails.propertyRecord.tenancyAndRentalInformation.furnishedStatus",
                "propertyDetails.propertyRecord.tenancyAndRentalInformation.rentFrequency.rowName",
                "propertyDetails.propertyRecord.tenancyAndRentalInformation.rentAmount",
            )

        val viewModel =
            PropertyDetailsViewModel(propertyOwnership, messageSource = mockMessageSource)

        assertEquals(expectedHeaderList, viewModel.tenancySection.map { it.fieldHeading })
        assertEquals("propertyDetails.propertyRecord.tenancy.heading", viewModel.tenancyHeadingKey)
        assertNull(viewModel.tenancyProvideLaterParagraph)
    }

    @Test
    fun `shows the bills-included row with conditional and custom tenancy values when they are provided`() {
        val propertyOwnership = createOccupiedPropertyOwnership()

        val viewModel =
            PropertyDetailsViewModel(propertyOwnership, messageSource = mockMessageSource)

        assertEquals(
            listOf(
                "propertyDetails.propertyRecord.tenancyAndRentalInformation.numberOfHouseholds.rowName",
                "propertyDetails.propertyRecord.tenancyAndRentalInformation.numberOfPeople",
                "propertyDetails.propertyRecord.tenancyAndRentalInformation.rentIncludesBills.rowName",
                "propertyDetails.propertyRecord.tenancyAndRentalInformation.billsIncluded",
                "propertyDetails.propertyRecord.tenancyAndRentalInformation.furnishedStatus",
                "propertyDetails.propertyRecord.tenancyAndRentalInformation.rentFrequency.rowName",
                "propertyDetails.propertyRecord.tenancyAndRentalInformation.rentAmount",
            ),
            viewModel.tenancySection.map { it.fieldHeading },
        )

        assertEquals(
            listOf(true, false, true, false, false, true, false),
            viewModel.tenancySection.map { it.withoutBottomBorder },
        )

        fun rowValue(heading: String) = viewModel.tenancySection.single { it.fieldHeading == heading }.fieldValue

        assertEquals(
            "commonText.yes",
            rowValue("propertyDetails.propertyRecord.tenancyAndRentalInformation.rentIncludesBills.rowName"),
        )
        assertEquals(
            "Message for forms.billsIncluded.checkbox.electricity, Message for forms.billsIncluded.checkbox.water, Cat sitting",
            rowValue("propertyDetails.propertyRecord.tenancyAndRentalInformation.billsIncluded"),
        )
        assertEquals(
            "Fortnightly",
            rowValue("propertyDetails.propertyRecord.tenancyAndRentalInformation.rentFrequency.rowName"),
        )
        assertEquals(
            "£200 Message for forms.checkPropertyAnswers.tenancyDetails.customFrequencyRentAmountSuffix",
            rowValue("propertyDetails.propertyRecord.tenancyAndRentalInformation.rentAmount"),
        )
    }

    private fun bedroomsRow(viewModel: PropertyDetailsViewModel): SummaryListRowViewModel =
        viewModel.propertyDetailsSection.single {
            it.fieldHeading == "propertyDetails.propertyRecord.tenancyAndRentalInformation.numberOfBedrooms"
        }

    @Test
    fun `correspondenceSection is null when showCorrespondenceSection is false`() {
        val viewModel =
            PropertyDetailsViewModel(
                createPropertyOwnership(),
                isLandlordView = true,
                messageSource = mockMessageSource,
                showCorrespondenceSection = false,
            )

        assertNull(viewModel.correspondenceSection)
    }

    @Test
    fun `correspondenceSection shows saved contact details when enabled`() {
        val propertyOwnership =
            createPropertyOwnership(
                correspondenceEmail = "contact@example.com",
                correspondenceAddress = createAddress("Flat 2, 25 Contact Road, Bristol, BS1 2AB"),
            )
        val viewModel =
            PropertyDetailsViewModel(
                propertyOwnership,
                isLandlordView = true,
                messageSource = mockMessageSource,
                showCorrespondenceSection = true,
            )

        val section = viewModel.correspondenceSection
        assertNotNull(section)
        assertEquals(2, section!!.size)

        val emailRow = section[0]
        assertEquals("propertyDetails.propertyRecord.correspondence.emailAddress", emailRow.fieldHeading)
        assertEquals("contact@example.com", emailRow.fieldValue)
        assertFalse(emailRow.hasActions)

        val addressRow = section[1]
        assertEquals("propertyDetails.propertyRecord.correspondence.address", addressRow.fieldHeading)
        assertEquals(listOf("Flat 2", "25 Contact Road", "Bristol", "BS1 2AB"), addressRow.fieldValue)
        assertTrue(addressRow.hasActions)
        val action = addressRow.actions.single()
        val expectedUrl =
            LandlordUpdateCorrespondenceAddressController
                .getUpdateCorrespondenceAddressRoute(propertyOwnership.id) +
                "/${LookupAddressStep.ROUTE_SEGMENT}"
        assertEquals(expectedUrl, action.url)
    }

    @Test
    fun `correspondenceSection rows have no change actions in the local council view`() {
        val viewModel =
            PropertyDetailsViewModel(
                createPropertyOwnership(),
                isLandlordView = false,
                messageSource = mockMessageSource,
                showCorrespondenceSection = true,
            )

        val section = viewModel.correspondenceSection
        assertNotNull(section)
        assertTrue(section!!.none { it.hasActions })
    }
}
