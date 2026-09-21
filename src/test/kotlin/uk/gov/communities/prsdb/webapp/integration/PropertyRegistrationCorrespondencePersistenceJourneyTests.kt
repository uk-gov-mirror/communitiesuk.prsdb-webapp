package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.communities.prsdb.webapp.clients.EpcRegisterClient
import uk.gov.communities.prsdb.webapp.constants.CORRESPONDENCE_ADDRESS
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.MANUAL_ADDRESS_CHOSEN
import uk.gov.communities.prsdb.webapp.constants.PAYMENTS
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING
import uk.gov.communities.prsdb.webapp.constants.enums.CorrespondenceEmailOption
import uk.gov.communities.prsdb.webapp.constants.enums.EpcExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.RegistrationNumberType
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import uk.gov.communities.prsdb.webapp.database.repository.AddressRepository
import uk.gov.communities.prsdb.webapp.database.repository.LettingAgentAccessRepository
import uk.gov.communities.prsdb.webapp.database.repository.PropertyOwnershipRepository
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckAnswersPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ConfirmationPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CorrespondenceEmailFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CorrespondenceLookupAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CorrespondenceManualAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CorrespondenceSelectAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CorrespondenceEmailStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LettingAgentEmailStep
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.RegistrationNumberDataModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.AllowLettingAgentEmailFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.CorrespondenceEmailFormModel
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.PropertyRegistrationConfirmationEmail
import uk.gov.communities.prsdb.webapp.services.AbsoluteUrlProvider
import uk.gov.communities.prsdb.webapp.services.DelegateToLettingAgentEmailService
import uk.gov.communities.prsdb.webapp.services.EmailNotificationService
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyStateSessionBuilder
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLocalCouncilData.Companion.createLocalCouncil
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class PropertyRegistrationCorrespondencePersistenceJourneyTests : IntegrationTestWithMutableData("data-local.sql") {
    private val accountEmail = "alex.surname@example.com"
    private val differentEmail = "council.contact@example.com"
    private val lettingAgentEmail = "letting.agent@example.com"
    private val propertyAddress =
        AddressDataModel(
            singleLineAddress = "Test address line 1, Testville, EG1 2AB",
            localCouncilId = 1,
            townName = "Testville",
            postcode = "EG1 2AB",
        )

    @Autowired
    private lateinit var propertyOwnershipRepository: PropertyOwnershipRepository

    @Autowired
    private lateinit var addressRepository: AddressRepository

    @Autowired
    private lateinit var lettingAgentAccessRepository: LettingAgentAccessRepository

    @MockitoBean
    private lateinit var confirmationEmailSender: EmailNotificationService<PropertyRegistrationConfirmationEmail>

    @MockitoBean
    private lateinit var delegateToLettingAgentEmailService: DelegateToLettingAgentEmailService

    @MockitoBean
    private lateinit var epcRegisterClient: EpcRegisterClient

    @MockitoBean
    private lateinit var absoluteUrlProvider: AbsoluteUrlProvider

    @BeforeEach
    fun setup() {
        featureFlagManager.enableFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)
        featureFlagManager.enableFeature(CORRESPONDENCE_ADDRESS)
        featureFlagManager.disableFeature(DELEGATE_TO_LETTING_AGENT)
        featureFlagManager.disableFeature(PAYMENTS)
        whenever(absoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("http://localhost/landlord"))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `final registration persists CYA email and manual postal edits including when delegated`(
        delegated: Boolean,
        page: Page,
    ) {
        val stateBuilder = completedState().withCompletedCorrespondence()
        if (delegated) {
            featureFlagManager.enableFeature(DELEGATE_TO_LETTING_AGENT)
            stateBuilder
                .withOccupancyStatus(true)
                .withLettingAgentProvidesRentalDetails()
                .withSubmittedValue(
                    LettingAgentEmailStep.ROUTE_SEGMENT,
                    AllowLettingAgentEmailFormModel().apply { emailAddress = lettingAgentEmail },
                )
        }
        val landlordAddress = addressRepository.findById(1L).orElseThrow()
        val originalLandlordAddress = AddressDataModel.fromAddress(landlordAddress)
        var checkAnswersPage = goToCheckAnswers(page, stateBuilder)

        checkAnswersPage.summaryList.correspondenceEmailRow.clickFirstActionLinkAndWait()
        val emailPage = assertPageIs(page, CorrespondenceEmailFormPagePropertyRegistration::class)
        emailPage.submitDifferentEmail(differentEmail)
        checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

        checkAnswersPage.summaryList.correspondencePostalAddressRow.clickFirstActionLinkAndWait()
        val lookupPage = assertPageIs(page, CorrespondenceLookupAddressFormPagePropertyRegistration::class)
        lookupPage.submitPostcodeAndBuildingNameOrNumber("FA1 1AB", "2")
        val selectPage = assertPageIs(page, CorrespondenceSelectAddressFormPagePropertyRegistration::class)
        selectPage.selectAddressAndSubmit(MANUAL_ADDRESS_CHOSEN)
        val manualPage = assertPageIs(page, CorrespondenceManualAddressFormPagePropertyRegistration::class)
        manualPage.submitAddress(
            addressLineOne = "Flat 4",
            addressLineTwo = "12 Test Road",
            townOrCity = "Leeds",
            county = "West Yorkshire",
            postcode = "LS1 1AA",
        )

        val postalAddressLines = listOf("Flat 4", "12 Test Road", "Leeds", "West Yorkshire", "LS1 1AA")
        checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        assertThat(checkAnswersPage.summaryList.correspondenceEmailRow.value).hasText(differentEmail)
        assertThat(checkAnswersPage.summaryList.correspondencePostalAddressRow.value.locator("p"))
            .hasText(postalAddressLines.toTypedArray())

        val savedOwnership = confirmAndRetrieveOwnership(page, checkAnswersPage)

        assertEquals(differentEmail, savedOwnership.correspondenceEmail)
        assertEquals(
            AddressDataModel(
                singleLineAddress = "Flat 4, 12 Test Road, Leeds, West Yorkshire, LS1 1AA",
                townName = "Leeds",
                postcode = "LS1 1AA",
            ),
            AddressDataModel.fromAddress(savedOwnership.correspondenceAddress),
        )
        assertEquals(postalAddressLines, savedOwnership.correspondenceAddress.toMultiLineAddress().lines())
        assertPropertyAddressUnchanged(savedOwnership)
        assertNotEquals(landlordAddress.id, savedOwnership.correspondenceAddress.id)
        assertEquals(
            originalLandlordAddress,
            AddressDataModel.fromAddress(addressRepository.findById(landlordAddress.id).orElseThrow()),
        )
        if (delegated) {
            val delegation = assertNotNull(lettingAgentAccessRepository.findByPropertyOwnershipId(savedOwnership.id))
            assertEquals(lettingAgentEmail, delegation.invitedEmail)
        }
    }

    @Test
    fun `final registration saves account email instead of retained different email and snapshots a lookup address`(page: Page) {
        val selectedAddress = addressRepository.findById(2L).orElseThrow()
        val selectedAddressData = AddressDataModel.fromAddress(selectedAddress)
        assertEquals(2L, selectedAddressData.uprn)
        var checkAnswersPage =
            goToCheckAnswers(
                page,
                completedState()
                    .withCompletedCorrespondence()
                    .withSubmittedValue(
                        CorrespondenceEmailStep.ROUTE_SEGMENT,
                        CorrespondenceEmailFormModel().apply {
                            correspondenceEmailOption = CorrespondenceEmailOption.DIFFERENT_EMAIL
                            differentEmailAddress = differentEmail
                        },
                    ),
            )

        checkAnswersPage.summaryList.correspondenceEmailRow.clickFirstActionLinkAndWait()
        var emailPage = assertPageIs(page, CorrespondenceEmailFormPagePropertyRegistration::class)
        BaseComponent.assertThat(emailPage.form.differentEmailInput).hasValue(differentEmail)
        emailPage.submitAccountEmail()
        checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

        // Confirm the old input is still retained: persistence must honour the selection, not input presence.
        checkAnswersPage.summaryList.correspondenceEmailRow.clickFirstActionLinkAndWait()
        emailPage = assertPageIs(page, CorrespondenceEmailFormPagePropertyRegistration::class)
        assertEquals(CorrespondenceEmailOption.ACCOUNT_EMAIL.name, emailPage.form.whichEmailRadios.selectedValue)
        BaseComponent.assertThat(emailPage.form.differentEmailInput).hasValue(differentEmail)
        emailPage.submitAccountEmail()
        checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

        checkAnswersPage.summaryList.correspondencePostalAddressRow.clickFirstActionLinkAndWait()
        val lookupPage = assertPageIs(page, CorrespondenceLookupAddressFormPagePropertyRegistration::class)
        lookupPage.submitPostcodeAndBuildingNameOrNumber("FA1 1AB", "2")
        val selectPage = assertPageIs(page, CorrespondenceSelectAddressFormPagePropertyRegistration::class)
        selectPage.selectAddressAndSubmit("2 Fake Way")
        checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        assertThat(checkAnswersPage.summaryList.correspondenceEmailRow.value).hasText(accountEmail)
        assertThat(checkAnswersPage.summaryList.correspondencePostalAddressRow.value.locator("p"))
            .hasText(arrayOf("2 Fake Way", "FA1 1AB"))

        val savedOwnership = confirmAndRetrieveOwnership(page, checkAnswersPage)

        assertEquals(accountEmail, savedOwnership.correspondenceEmail)
        assertNotEquals(differentEmail, savedOwnership.correspondenceEmail)
        // UPRNs uniquely identify lookup rows; a correspondence snapshot must not claim the source row's UPRN.
        assertEquals(
            selectedAddressData.copy(uprn = null),
            AddressDataModel.fromAddress(savedOwnership.correspondenceAddress),
        )
        assertNotEquals(selectedAddress.id, savedOwnership.correspondenceAddress.id)
        assertNotEquals(1L, savedOwnership.correspondenceAddress.id)
        assertEquals(
            selectedAddressData,
            AddressDataModel.fromAddress(addressRepository.findById(selectedAddress.id).orElseThrow()),
        )
        assertPropertyAddressUnchanged(savedOwnership)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `final registration without correspondence steps saves account email and a separate property address snapshot`(
        restructured: Boolean,
        page: Page,
    ) {
        if (restructured) {
            featureFlagManager.disableFeature(CORRESPONDENCE_ADDRESS)
        } else {
            // Leave correspondence enabled to prove the legacy journey does not access its unwired steps.
            featureFlagManager.disableFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)
        }
        val checkAnswersPage = goToCheckAnswers(page, completedState(), restructured)
        BaseComponent.assertThat(checkAnswersPage.correspondenceHeading).isHidden()
        assertThat(checkAnswersPage.correspondenceRowKeys).hasCount(0)
        assertThat(checkAnswersPage.summaryList.correspondenceEmailRow.key).hasCount(0)
        assertThat(checkAnswersPage.summaryList.correspondencePostalAddressRow.key).hasCount(0)

        val savedOwnership = confirmAndRetrieveOwnership(page, checkAnswersPage)

        assertEquals(accountEmail, savedOwnership.correspondenceEmail)
        assertEquals(propertyAddress, AddressDataModel.fromAddress(savedOwnership.correspondenceAddress))
        assertPropertyAddressUnchanged(savedOwnership)
        assertNotEquals(1L, savedOwnership.correspondenceAddress.id)
    }

    private fun completedState() =
        PropertyStateSessionBuilder()
            .withLookupAddress()
            // Use an unregistered manual property address so final submission cannot collide with a seeded property.
            .withManualAddress(
                addressLineOne = "Test address line 1",
                townOrCity = "Testville",
                postcode = "EG1 2AB",
                localCouncil = createLocalCouncil(id = 1),
            ).withPropertyType()
            .withBedrooms()
            .withOwnershipType()
            .withHasNoJointLandlords()
            .withOccupancyStatus(false)
            .withLicensingType(LicensingType.NO_LICENSING)
            .withGasSafetyTaskCompletedWithNoGasSupply()
            .withElectricalSafetyCertificateMissing()
            .withPropertyHasNoEpc()
            .withIsEpcNotRequired()
            .withEpcExemptionReason(EpcExemptionReason.TEMPORARY_BUILDING)
            .withCheckEpcAnswersComplete()

    private fun goToCheckAnswers(
        page: Page,
        stateBuilder: PropertyStateSessionBuilder,
        restructured: Boolean = true,
    ): CheckAnswersPagePropertyRegistration {
        val taskListPage = navigator.goToRestructuredPropertyRegistrationTaskList(stateBuilder)
        if (restructured) {
            taskListPage.clickSubmitYourRegistrationTaskWithName("Check and submit your answers")
        } else {
            navigator.navigateToPropertyRegistrationCheckYourAnswers()
        }
        return assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
    }

    private fun confirmAndRetrieveOwnership(
        page: Page,
        checkAnswersPage: CheckAnswersPagePropertyRegistration,
    ): PropertyOwnership {
        val originalOwnershipCount = propertyOwnershipRepository.count()
        checkAnswersPage.confirm()
        val confirmationPage = assertPageIs(page, ConfirmationPagePropertyRegistration::class)
        val registrationNumber =
            assertNotNull(
                RegistrationNumberDataModel.parseTypeOrNull(
                    confirmationPage.registrationNumberText,
                    RegistrationNumberType.PROPERTY,
                ),
            )

        // Read the committed row identified by the browser confirmation, rather than capturing a repository save argument.
        val savedOwnership =
            assertNotNull(
                propertyOwnershipRepository.findByRegistrationNumber_Number(registrationNumber.number),
                "The confirmed property registration must exist in the database",
            )
        assertEquals(originalOwnershipCount + 1, propertyOwnershipRepository.count())
        return savedOwnership
    }

    private fun assertPropertyAddressUnchanged(savedOwnership: PropertyOwnership) {
        assertEquals(propertyAddress, AddressDataModel.fromAddress(savedOwnership.address))
        assertNotEquals(savedOwnership.address.id, savedOwnership.correspondenceAddress.id)
    }
}
