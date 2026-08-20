package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor.captor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.communities.prsdb.webapp.clients.EpcRegisterClient
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.GAS_SAFETY_CERT_VALIDITY_YEARS
import uk.gov.communities.prsdb.webapp.constants.INDIVIDUAL_PROPERTY_REGISTRATION_SURVEY_URL
import uk.gov.communities.prsdb.webapp.constants.MANUAL_ADDRESS_CHOSEN
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING
import uk.gov.communities.prsdb.webapp.constants.enums.EpcExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.FurnishedStatus
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.MeesExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.constants.enums.RentFrequency
import uk.gov.communities.prsdb.webapp.database.entity.LandlordIncompleteProperties
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import uk.gov.communities.prsdb.webapp.database.repository.IncompletePropertiesRepository
import uk.gov.communities.prsdb.webapp.database.repository.JointLandlordInvitationRepository
import uk.gov.communities.prsdb.webapp.database.repository.PropertyOwnershipRepository
import uk.gov.communities.prsdb.webapp.helpers.DateTimeHelper
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BackLink
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent.Companion.assertThat
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LandlordDashboardPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.EpcLookupBasePage.Companion.CURRENT_EPC_CERTIFICATE_NUMBER
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.EpcLookupBasePage.Companion.CURRENT_EXPIRED_EPC_CERTIFICATE_NUMBER
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.EpcLookupBasePage.Companion.NONEXISTENT_EPC_CERTIFICATE_NUMBER
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.EpcLookupBasePage.Companion.SUPERSEDED_EPC_CERTIFICATE_NUMBER
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.BillsIncludedFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckAnswersPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckElectricalCertUploadsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckElectricalSafetyAnswersFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckEpcAnswersFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckGasCertUploadsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckGasSafetyAnswersFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckJointLandlordsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ConfirmEpcDetailsRetrievedByCertificateNumberPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ConfirmEpcDetailsRetrievedByUprnFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ConfirmMissingComplianceFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ConfirmationPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ElectricalCertExpiredFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ElectricalCertExpiryDateFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ElectricalCertMissingFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.EpcExpiredFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.EpcInDateAtStartOfTenancyCheckPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.EpcMissingFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.EpcNotFoundFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.EpcSuperseededFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.FindYourEpcFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.FurnishedStatusFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.GasCertExpiredFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.GasCertIssueDateFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.GasCertMissingFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasElectricalCertFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasEpcFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasGasCertFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasGasSupplyFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasJointLandlordsFormBasePagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.InviteAnotherJointLandlordFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.InviteJointLandlordFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.IsEpcRequiredFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.LettingAgentEmailPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.LicensingTypeFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.LookupAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.LowEnergyRatingFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ManualAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.MeesExemptionFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.NumberOfBedroomsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.NumberOfHouseholdsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.NumberOfPeopleFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.OccupancyFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.OwnershipTypeFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.PropertyTypeFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ProvideElectricalCertLaterFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ProvideEpcLaterFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ProvideGasCertLaterFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ProvideTenancyDetailsLaterFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.RegisterPropertyStartPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.RemoveElectricalCertUploadFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.RemoveGasCertUploadFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.RemoveJointLandlordAreYouSureFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.RentAmountFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.RentFrequencyFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.RentIncludesBillsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.SelectAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.SelectLocalCouncilFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.SelectiveLicenceFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.TaskListPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.UploadElectricalCertFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.UploadGasCertFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.WhoProvidesRentalDetailsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.models.dataModels.RegistrationNumberDataModel
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.JointLandlordInvitationEmail
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.PropertyRegistrationConfirmationEmail
import uk.gov.communities.prsdb.webapp.services.AbsoluteUrlProvider
import uk.gov.communities.prsdb.webapp.services.EmailNotificationService
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyStateSessionBuilder
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockEpcData
import java.net.URI
import java.nio.file.Path
import java.time.format.DateTimeFormatter
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PropertyRegistrationJourneyTests : IntegrationTestWithMutableData("data-local.sql") {
    private val absoluteLandlordUrl = "www.prsd.gov.uk/landlord"
    private val propertyDetailsSectionHeader = "Property details"
    private val ownershipSectionHeader = "Ownership and landlords"
    private val occupiedSectionHeader = "Tell us if your property’s occupied"
    private val licenseSectionHeader = "Tell us if your property needs a license"
    private val gasSafetyHeader = "Gas safety certificate"
    private val electricalSafetyHeader = "Electrical safety certificate"
    private val epcSectionHeader = "Energy performance certificate (EPC)"

    @MockitoSpyBean
    private lateinit var propertyOwnershipRepository: PropertyOwnershipRepository

    @MockitoSpyBean
    private lateinit var jointLandlordInvitationRepository: JointLandlordInvitationRepository

    @MockitoSpyBean
    private lateinit var incompletePropertiesRepository: IncompletePropertiesRepository

    @MockitoBean
    private lateinit var confirmationEmailSender: EmailNotificationService<PropertyRegistrationConfirmationEmail>

    @MockitoBean
    private lateinit var jointLandlordInvitationEmailSender: EmailNotificationService<JointLandlordInvitationEmail>

    @MockitoBean
    private lateinit var epcRegisterClient: EpcRegisterClient

    @MockitoBean
    private lateinit var absoluteUrlProvider: AbsoluteUrlProvider

    @BeforeEach
    fun setup() {
        whenever(absoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI(absoluteLandlordUrl))
        whenever(
            absoluteUrlProvider.buildJointLandlordInvitationUri(any()),
        ).thenReturn(URI("http://localhost/invite/test-token"))
        whenever(
            absoluteUrlProvider.buildPropertyDetailsUri(any()),
        ).thenReturn(URI("http://localhost/property-details/1"))
    }

    @Nested
    inner class RestructureAndSkippingEnabled {
        @BeforeEach
        fun enableRestructureAndSkippingFlag() {
            featureFlagManager.enableFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)
        }

        @Test
        @Suppress("ktlint:standard:max-line-length")
        fun `User can navigate the whole journey if pages are correctly filled in (select address, non-custom property type, selective license, occupied, compliance certificates uploaded)`(
            page: Page,
        ) {
            // Start page (not a journey step, but it is how the user accesses the journey)
            val registerPropertyStartPage = navigator.goToPropertyRegistrationStartPage()
            assertThat(registerPropertyStartPage.heading).containsText("Register a property")
            registerPropertyStartPage.startButton.clickAndWait()
            val taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // Task list page (part of the journey to support redirects)
            taskListPage.clickAboutYourPropertyTaskWithName("Property details")
            val addressLookupPage = assertPageIs(page, LookupAddressFormPagePropertyRegistration::class)

            // Address lookup - render page
            assertThat(addressLookupPage.form.fieldsetHeading).containsText("What is the property address?")
            assertThat(addressLookupPage.form.sectionHeader).containsText(propertyDetailsSectionHeader)
            // fill in and submit
            addressLookupPage.submitPostcodeAndBuildingNameOrNumber("FA1 1AA", "1")
            val selectAddressPage = assertPageIs(page, SelectAddressFormPagePropertyRegistration::class)

            // Select address - render page
            assertThat(selectAddressPage.form.fieldsetHeading).containsText("Select your address")
            assertThat(selectAddressPage.form.sectionHeader).containsText(propertyDetailsSectionHeader)
            // fill in and submit
            selectAddressPage.selectAddressAndSubmit("1 Fictional Road, FA1 1AA")
            val propertyTypePage = assertPageIs(page, PropertyTypeFormPagePropertyRegistration::class)

            // Verify incomplete property is created at this point
            verify(incompletePropertiesRepository).save<LandlordIncompleteProperties>(any())

            // Property type selection - render page
            assertThat(propertyTypePage.form.fieldsetHeading).containsText("What type of property are you registering?")
            assertThat(propertyTypePage.form.sectionHeader).containsText(propertyDetailsSectionHeader)
            // fill in and submit
            propertyTypePage.submitPropertyType(PropertyType.DETACHED_HOUSE)
            val bedroomsPage = assertPageIs(page, NumberOfBedroomsFormPagePropertyRegistration::class)

            // Number of bedrooms - render page
            assertThat(bedroomsPage.header).containsText("How many bedrooms in your property?")
            assertThat(bedroomsPage.form.sectionHeader).containsText(propertyDetailsSectionHeader)
            bedroomsPage.submitNumOfBedrooms(3)
            val ownershipTypePage = assertPageIs(page, OwnershipTypeFormPagePropertyRegistration::class)

            // Ownership type selection - render page
            assertThat(ownershipTypePage.form.fieldsetHeading).containsText("Select the type of ownership you have for your property")
            assertThat(ownershipTypePage.form.sectionHeader).containsText(ownershipSectionHeader)
            // fill in and submit
            ownershipTypePage.submitOwnershipType(OwnershipType.FREEHOLD)
            val hasJointLandlordsPage = assertPageIs(page, HasJointLandlordsFormBasePagePropertyRegistration::class)

            // Has Joint Landlords - render page
            assertThat(hasJointLandlordsPage.header).containsText("Invite joint landlords")
            assertThat(hasJointLandlordsPage.sectionHeader).containsText(ownershipSectionHeader)

            // fill in and submit
            hasJointLandlordsPage.submitHasJointLandlords()
            val inviteJointLandlordPage = assertPageIs(page, InviteJointLandlordFormPagePropertyRegistration::class)

            // Invite joint landlord - render page
            assertThat(inviteJointLandlordPage.heading).containsText("Invite a joint landlord to this property")
            assertThat(inviteJointLandlordPage.form.sectionHeader).containsText(ownershipSectionHeader)

            // fill in and submit
            inviteJointLandlordPage.submitEmail("email@address.com")
            var checkJointLandlordsPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            assertThat(checkJointLandlordsPage.sectionHeader).containsText(ownershipSectionHeader)
            assertThat(checkJointLandlordsPage.summaryList.firstRow.value).containsText("email@address.com")

            // Check joint landlords - render page
            checkJointLandlordsPage
                .form
                .addAnotherButton
                .clickAndWait()

            // Invite another joint landlord - render page
            val addAnotherPage = assertPageIs(page, InviteAnotherJointLandlordFormPagePropertyRegistration::class)
            assertThat(addAnotherPage.form.sectionHeader).containsText(ownershipSectionHeader)
            addAnotherPage.submitEmail("email2@address.com")

            checkJointLandlordsPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            checkJointLandlordsPage.summaryList.firstRow.clickNamedActionLinkAndWait("Remove")

            // Remove Joint Landlord - render page
            val removeJointLandlordsPage =
                assertPageIs(page, RemoveJointLandlordAreYouSureFormPagePropertyRegistration::class)
            removeJointLandlordsPage.submitWantsToProceed()

            checkJointLandlordsPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            checkJointLandlordsPage.form.submit()
            val occupancyPage = assertPageIs(page, OccupancyFormPagePropertyRegistration::class)

            // Occupancy - render page
            assertThat(occupancyPage.form.fieldsetHeading).containsText("Is your property occupied by tenants?")
            assertThat(occupancyPage.form.sectionHeader).containsText(occupiedSectionHeader)
            occupancyPage.submitIsOccupied()
            val whoProvidesRentalDetailsPage = assertPageIs(page, WhoProvidesRentalDetailsFormPagePropertyRegistration::class)
            whoProvidesRentalDetailsPage.submitLandlordProvidesDetails()
            val licensingTypePage = assertPageIs(page, LicensingTypeFormPagePropertyRegistration::class)

            // Licensing type - render page
            assertThat(licensingTypePage.form.fieldsetHeading).containsText("Select the type of licence you have for your property")
            assertThat(licensingTypePage.form.sectionHeader).containsText(licenseSectionHeader)
            licensingTypePage.submitLicensingType(LicensingType.SELECTIVE_LICENCE)
            val selectiveLicencePage = assertPageIs(page, SelectiveLicenceFormPagePropertyRegistration::class)

            // Selective licence - render page
            assertThat(selectiveLicencePage.form.fieldsetHeading).containsText("What is your selective licence number?")
            assertThat(selectiveLicencePage.form.sectionHeader).containsText(licenseSectionHeader)
            selectiveLicencePage.submitLicenseNumber("licence number")

            val hasGasSupplyPage = assertPageIs(page, HasGasSupplyFormPagePropertyRegistration::class)

            // Has Gas Supply - render page
            assertThat(hasGasSupplyPage.sectionHeader).containsText(gasSafetyHeader)
            assertThat(hasGasSupplyPage.heading).containsText("Does the property have a gas supply or any gas appliances?")
            hasGasSupplyPage.submitHasGasSupply()
            val hasGasCertPage = assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)

            // Has Gas Cert - render page
            assertThat(hasGasCertPage.form.sectionHeader).containsText(gasSafetyHeader)
            assertThat(hasGasCertPage.heading).containsText("Do you have a gas safety certificate for this property?")
            hasGasCertPage.submitHasCertificate()
            val gasCertIssueDatePage = assertPageIs(page, GasCertIssueDateFormPagePropertyRegistration::class)

            // Gas Cert Issue Date - render page
            assertThat(gasCertIssueDatePage.form.sectionHeader).containsText(gasSafetyHeader)
            assertThat(gasCertIssueDatePage.heading).containsText("What’s the issue date on the gas safety certificate?")
            gasCertIssueDatePage.submitDate(validGasSafetyCertIssueDate)
            var uploadGasCertPage = assertPageIs(page, UploadGasCertFormPagePropertyRegistration::class)

            // Upload Gas Cert - render page
            assertThat(uploadGasCertPage.form.sectionHeader).containsText(gasSafetyHeader)
            uploadGasCertPage.uploadGasCertificate(Path.of("src/test/resources/test-files/blank.png"))
            var checkGasCertUploadsPage = assertPageIs(page, CheckGasCertUploadsFormPagePropertyRegistration::class)

            // Check Gas Cert Uploads - render page
            assertThat(checkGasCertUploadsPage.sectionHeader).containsText(gasSafetyHeader)
            assertThat(checkGasCertUploadsPage.heading).containsText("You’ve uploaded 1 file")
            assertThat(checkGasCertUploadsPage.table.getCell(0, 0)).containsText("blank.png")
            assertEquals(checkGasCertUploadsPage.table.rows.count(), 1)
            checkGasCertUploadsPage.form.addAnotherButton.clickAndWait()
            uploadGasCertPage = assertPageIs(page, UploadGasCertFormPagePropertyRegistration::class)

            uploadGasCertPage.uploadGasCertificate(Path.of("src/test/resources/test-files/blank.png"))
            checkGasCertUploadsPage = assertPageIs(page, CheckGasCertUploadsFormPagePropertyRegistration::class)

            assertThat(checkGasCertUploadsPage.heading).containsText("You’ve uploaded 2 files")
            assertThat(checkGasCertUploadsPage.table.getCell(0, 0)).containsText("blank.png")
            assertThat(checkGasCertUploadsPage.table.getCell(1, 0)).containsText("blank.png")
            assertEquals(checkGasCertUploadsPage.table.rows.count(), 2)

            checkGasCertUploadsPage.table
                .getClickableCell(0, 2)
                .link
                .clickAndWait()

            val removeGasCertUploadPage = assertPageIs(page, RemoveGasCertUploadFormPagePropertyRegistration::class)

            removeGasCertUploadPage.form.radios.selectValue("true")
            removeGasCertUploadPage.form.submit()

            checkGasCertUploadsPage = assertPageIs(page, CheckGasCertUploadsFormPagePropertyRegistration::class)
            assertThat(checkGasCertUploadsPage.table.getCell(0, 0)).containsText("blank.png")

            assertEquals(checkGasCertUploadsPage.table.rows.count(), 1)
            checkGasCertUploadsPage.form.submit()

            // Remove Gas Cert Upload - render page
            val checkGasSafetyAnswersPage = assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)

            // Check Gas Safety Answers - render page
            assertThat(checkGasSafetyAnswersPage.sectionHeader).isHidden()
            assertThat(checkGasSafetyAnswersPage.heading).containsText("Gas safety certificate")
            checkGasSafetyAnswersPage.form.submit()
            val taskListPageAfterGasSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterGasSafety.clickRentedOutTaskWithName("Electrical safety certificate")
            val hasElectricalCertPage = assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)

            // Has Electrical Cert - render page
            assertThat(hasElectricalCertPage.form.sectionHeader).containsText(electricalSafetyHeader)
            assertThat(hasElectricalCertPage.heading).containsText("Which electrical safety certificate do you have for this property?")
            hasElectricalCertPage.submitHasEic()
            val electricalCertExpiryDatePage =
                assertPageIs(page, ElectricalCertExpiryDateFormPagePropertyRegistration::class)

            // Electrical Cert Expiry Date - render page
            assertThat(electricalCertExpiryDatePage.form.sectionHeader).containsText(electricalSafetyHeader)
            assertThat(
                electricalCertExpiryDatePage.heading,
            ).containsText("What’s the expiry date on the Electrical Installation Certificate?")
            electricalCertExpiryDatePage.submitDate(validExpiryDate)
            var uploadElectricalCertPage = assertPageIs(page, UploadElectricalCertFormPagePropertyRegistration::class)

            // Upload Electrical Cert - render page
            assertThat(uploadElectricalCertPage.form.sectionHeader).containsText(electricalSafetyHeader)
            uploadElectricalCertPage.uploadElectricalCertificate(Path.of("src/test/resources/test-files/blank.png"))
            var checkElectricalCertUploadsPage =
                assertPageIs(page, CheckElectricalCertUploadsFormPagePropertyRegistration::class)

            // Check Electrical Cert Uploads - render page
            assertThat(checkElectricalCertUploadsPage.sectionHeader).containsText(electricalSafetyHeader)
            assertThat(checkElectricalCertUploadsPage.table.getCell(0, 0)).containsText("blank.png")
            assertEquals(checkElectricalCertUploadsPage.table.rows.count(), 1)
            checkElectricalCertUploadsPage.form.addAnotherButton.clickAndWait()
            uploadElectricalCertPage = assertPageIs(page, UploadElectricalCertFormPagePropertyRegistration::class)

            uploadElectricalCertPage.uploadElectricalCertificate(Path.of("src/test/resources/test-files/blank.png"))
            checkElectricalCertUploadsPage =
                assertPageIs(page, CheckElectricalCertUploadsFormPagePropertyRegistration::class)
            assertThat(checkElectricalCertUploadsPage.table.getCell(0, 0)).containsText("blank.png")
            assertThat(checkElectricalCertUploadsPage.table.getCell(1, 0)).containsText("blank.png")
            assertEquals(checkElectricalCertUploadsPage.table.rows.count(), 2)

            checkElectricalCertUploadsPage.table
                .getClickableCell(0, 2)
                .link
                .clickAndWait()

            val removeElectricalCertUploadPage =
                assertPageIs(page, RemoveElectricalCertUploadFormPagePropertyRegistration::class)

            removeElectricalCertUploadPage.form.radios.selectValue("true")
            removeElectricalCertUploadPage.form.submit()

            checkElectricalCertUploadsPage =
                assertPageIs(page, CheckElectricalCertUploadsFormPagePropertyRegistration::class)
            assertThat(checkElectricalCertUploadsPage.table.getCell(0, 0)).containsText("blank.png")

            assertEquals(checkElectricalCertUploadsPage.table.rows.count(), 1)
            checkElectricalCertUploadsPage.form.submit()
            val checkElectricalSafetyAnswersPage =
                assertPageIs(page, CheckElectricalSafetyAnswersFormPagePropertyRegistration::class)

            // Setup EpcLookupByUprnStep being able to find an EPC for this property when the next step submits
            whenever(epcRegisterClient.getByUprn(uprnForSelectedAddress))
                .thenReturn(
                    MockEpcData.createEpcRegisterClientEpcFoundResponse(
                        expiryDate = validExpiryDate,
                    ),
                )

            // Check Electrical Safety Answers - render page
            assertThat(checkElectricalSafetyAnswersPage.sectionHeader).isHidden()
            assertThat(checkElectricalSafetyAnswersPage.heading).containsText("Electrical safety certificate")
            checkElectricalSafetyAnswersPage.form.submit()
            val taskListPageAfterElectricalSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // EpcLookupByUprnStep finds the EPC, so redirects to Check UPRN matched EPC
            taskListPageAfterElectricalSafety.clickRentedOutTaskWithName("Energy performance certificate (EPC)")
            val confirmUprnMatchedEpcDetailsPage =
                assertPageIs(page, ConfirmEpcDetailsRetrievedByUprnFormPagePropertyRegistration::class)

            // Confirm UPRN matched EPC - submit No (don't use this EPC)
            assertThat(confirmUprnMatchedEpcDetailsPage.sectionHeader).containsText(epcSectionHeader)
            confirmUprnMatchedEpcDetailsPage.submitNo()
            val hasEpcPage = assertPageIs(page, HasEpcFormPagePropertyRegistration::class)

            // Has EPC - render page
            assertThat(hasEpcPage.form.sectionHeader).containsText(epcSectionHeader)
            hasEpcPage.submitHasEpc()
            val findYourEpcPage = assertPageIs(page, FindYourEpcFormPagePropertyRegistration::class)

            // EPC Search - render page
            assertThat(findYourEpcPage.form.sectionHeader).containsText(epcSectionHeader)
            whenever(epcRegisterClient.getByRrn(CURRENT_EPC_CERTIFICATE_NUMBER))
                .thenReturn(
                    MockEpcData.createEpcRegisterClientEpcFoundResponse(
                        certificateNumber = CURRENT_EPC_CERTIFICATE_NUMBER,
                        latestCertificateNumberForThisProperty = CURRENT_EPC_CERTIFICATE_NUMBER,
                        expiryDate = validExpiryDate,
                    ),
                )
            findYourEpcPage.submitCurrentEpcNumber()
            val confirmEpcDetailsPage =
                assertPageIs(page, ConfirmEpcDetailsRetrievedByCertificateNumberPagePropertyRegistration::class)

            // Check Matched EPC - render page
            assertThat(confirmEpcDetailsPage.sectionHeader).containsText(epcSectionHeader)
            val expectedExpiryDate =
                validExpiryDate
                    .toJavaLocalDate()
                    .format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
            assertThat(confirmEpcDetailsPage.summaryCard.summaryList.addressRow.value).containsText(MockEpcData.defaultSingleLineAddress)
            assertThat(confirmEpcDetailsPage.summaryCard.summaryList.energyEfficiencyRatingRow.value).containsText("C")
            assertThat(confirmEpcDetailsPage.summaryCard.summaryList.expiryDateRow.value).containsText(
                expectedExpiryDate,
            )
            assertThat(
                confirmEpcDetailsPage.summaryCard.summaryList.certificateNumberRow.value,
            ).containsText(CURRENT_EPC_CERTIFICATE_NUMBER)
            confirmEpcDetailsPage.submitYes()
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.sectionHeader).isHidden()
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            val taskListPageAfterEpc = assertPageIs(page, TaskListPagePropertyRegistration::class)

            taskListPageAfterEpc.clickRentedOutTaskWithName("Tenancy details")
            val householdsPage = assertPageIs(page, NumberOfHouseholdsFormPagePropertyRegistration::class)
            householdsPage.submitNumberOfHouseholds(2)
            val peoplePage = assertPageIs(page, NumberOfPeopleFormPagePropertyRegistration::class)
            peoplePage.submitNumOfPeople(2)
            val rentIncludesBillsPage = assertPageIs(page, RentIncludesBillsFormPagePropertyRegistration::class)
            rentIncludesBillsPage.submitIsIncluded()
            val billsIncludedPage = assertPageIs(page, BillsIncludedFormPagePropertyRegistration::class)
            billsIncludedPage.selectGasElectricityWater()
            billsIncludedPage.selectSomethingElseCheckbox()
            billsIncludedPage.fillCustomBills("Dog Grooming")
            billsIncludedPage.form.submit()
            val furnishedPage = assertPageIs(page, FurnishedStatusFormPagePropertyRegistration::class)
            furnishedPage.submitFurnishedStatus(FurnishedStatus.FURNISHED)
            val rentFrequencyPage = assertPageIs(page, RentFrequencyFormPagePropertyRegistration::class)
            rentFrequencyPage.selectRentFrequency(RentFrequency.OTHER)
            rentFrequencyPage.fillCustomRentFrequency("Fortnightly")
            rentFrequencyPage.form.submit()
            val rentAmountPage = assertPageIs(page, RentAmountFormPagePropertyRegistration::class)
            rentAmountPage.submitRentAmount("400")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            // Check answers - render page
            assertThat(checkAnswersPage.heading).containsText("Check your answers for:")
            assertThat(checkAnswersPage.sectionHeader).containsText("Submit your registration")
            assertThat(checkAnswersPage.complianceCertificatesHeading).isVisible()
            assertThat(checkAnswersPage.gasSafetyHeading).isVisible()
            assertThat(checkAnswersPage.electricalSafetyHeading).isVisible()
            assertThat(checkAnswersPage.epcHeading).isVisible()
            // submit
            checkAnswersPage.confirm()
            val confirmationPage = assertPageIs(page, ConfirmationPagePropertyRegistration::class)

            // Confirmation - render page
            val propertyOwnershipCaptor = captor<PropertyOwnership>()
            verify(propertyOwnershipRepository).save(propertyOwnershipCaptor.capture())
            val expectedPropertyRegNum =
                RegistrationNumberDataModel.fromRegistrationNumber(
                    propertyOwnershipCaptor.value.registrationNumber,
                )
            assertEquals(expectedPropertyRegNum.toString(), confirmationPage.registrationNumberText)
            assertTrue(propertyOwnershipCaptor.value.isOccupied)
            assertFalse(confirmationPage.whatYouNeedToDoNextHeading.isVisible)
            assertTrue(confirmationPage.surveyLink.locator.isVisible)
            assertThat(confirmationPage.surveyLink).hasAttribute("href", INDIVIDUAL_PROPERTY_REGISTRATION_SURVEY_URL)
            assertTrue(confirmationPage.goToDashboardLink.locator.isVisible)

            // Check confirmation email
            verify(confirmationEmailSender).sendEmail(
                "alex.surname@example.com",
                PropertyRegistrationConfirmationEmail(
                    expectedPropertyRegNum.toString(),
                    "1 Fictional Road, FA1 1AA",
                    absoluteLandlordUrl,
                    true,
                    listOf("email2@address.com"),
                ),
            )

            // Go to dashboard
            confirmationPage.goToDashboardLink.clickAndWait()
            assertPageIs(page, LandlordDashboardPage::class)
        }

        @Test
        @Suppress("ktlint:standard:max-line-length")
        fun `User can navigate the whole journey in the enabled flow (manual address, custom property type, no license, unoccupied, no joint landlords, no certificates)`(
            page: Page,
        ) {
            // Start page (not a journey step, but it is how the user accesses the journey)
            val registerPropertyStartPage = navigator.goToPropertyRegistrationStartPage()
            assertThat(registerPropertyStartPage.heading).containsText("Register a property")
            registerPropertyStartPage.startButton.clickAndWait()
            val taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // Task list page (part of the journey to support redirects)
            taskListPage.clickAboutYourPropertyTaskWithName("Property details")
            val addressLookupPage = assertPageIs(page, LookupAddressFormPagePropertyRegistration::class)

            // Address lookup - render page
            assertThat(addressLookupPage.form.fieldsetHeading).containsText("What is the property address?")
            assertThat(addressLookupPage.form.sectionHeader).containsText(propertyDetailsSectionHeader)
            // fill in and submit
            addressLookupPage.submitPostcodeAndBuildingNameOrNumber("FA1 1AB", "2")
            val selectAddressPage = assertPageIs(page, SelectAddressFormPagePropertyRegistration::class)

            // Select address - render page
            assertThat(selectAddressPage.form.fieldsetHeading).containsText("Select your address")
            assertThat(selectAddressPage.form.sectionHeader).containsText(propertyDetailsSectionHeader)
            // fill in and submit
            selectAddressPage.selectAddressAndSubmit(MANUAL_ADDRESS_CHOSEN)
            val manualAddressPage = assertPageIs(page, ManualAddressFormPagePropertyRegistration::class)

            // Manual address - render page
            assertThat(manualAddressPage.form.fieldsetHeading).containsText("What is the property address?")
            assertThat(manualAddressPage.form.sectionHeader).containsText(propertyDetailsSectionHeader)
            // fill in and submit
            manualAddressPage.submitAddress(
                addressLineOne = "Test address line 1",
                townOrCity = "Testville",
                postcode = "EG1 2AB",
            )
            val selectLocalCouncilPage = assertPageIs(page, SelectLocalCouncilFormPagePropertyRegistration::class)

            // Select local council - render page
            assertThat(selectLocalCouncilPage.form.fieldsetHeading).containsText("What local council area is your property in?")
            assertThat(selectLocalCouncilPage.form.sectionHeader).containsText(propertyDetailsSectionHeader)
            // fill in and submit
            selectLocalCouncilPage.submitLocalCouncil(
                "BATH AND NORTH EAST SOMERSET COUNCIL",
                "BATH AND NORTH EAST SOMERSET COUNCIL",
            )
            val propertyTypePage = assertPageIs(page, PropertyTypeFormPagePropertyRegistration::class)

            // Property type selection - render page
            assertThat(propertyTypePage.form.fieldsetHeading).containsText("What type of property are you registering?")
            assertThat(propertyTypePage.form.sectionHeader).containsText(propertyDetailsSectionHeader)
            // fill in and submit
            propertyTypePage.submitCustomPropertyType("End terrace house")
            val bedroomsPage = assertPageIs(page, NumberOfBedroomsFormPagePropertyRegistration::class)

            // Number of bedrooms - render page
            assertThat(bedroomsPage.header).containsText("How many bedrooms in your property?")
            assertThat(bedroomsPage.form.sectionHeader).containsText(propertyDetailsSectionHeader)
            bedroomsPage.submitNumOfBedrooms(3)
            val ownershipTypePage = assertPageIs(page, OwnershipTypeFormPagePropertyRegistration::class)

            // Ownership type selection - render page
            assertThat(ownershipTypePage.form.fieldsetHeading).containsText("Select the type of ownership you have for your property")
            assertThat(ownershipTypePage.form.sectionHeader).containsText(ownershipSectionHeader)
            // fill in and submit
            ownershipTypePage.submitOwnershipType(OwnershipType.FREEHOLD)
            val hasJointLandlordsPage = assertPageIs(page, HasJointLandlordsFormBasePagePropertyRegistration::class)

            // Has Joint Landlords - render page
            assertThat(hasJointLandlordsPage.header).containsText("Invite joint landlords")
            assertThat(hasJointLandlordsPage.sectionHeader).containsText(ownershipSectionHeader)

            // fill in and submit
            hasJointLandlordsPage.submitHasNoJointLandlords()
            val occupancyPage = assertPageIs(page, OccupancyFormPagePropertyRegistration::class)

            // Occupancy - render page
            assertThat(occupancyPage.form.fieldsetHeading).containsText("Is your property occupied by tenants?")
            assertThat(occupancyPage.form.sectionHeader).containsText(occupiedSectionHeader)
            // fill in and submit
            occupancyPage.submitIsVacant()
            val licensingTypePage = assertPageIs(page, LicensingTypeFormPagePropertyRegistration::class)

            // Licensing type - render page
            assertThat(licensingTypePage.form.fieldsetHeading).containsText("Select the type of licence you have for your property")
            assertThat(licensingTypePage.form.sectionHeader).containsText(licenseSectionHeader)
            // fill in and submit
            licensingTypePage.submitLicensingType(LicensingType.NO_LICENSING)
            val hasGasSupplyPage = assertPageIs(page, HasGasSupplyFormPagePropertyRegistration::class)

            // Has Gas Supply - render page
            assertThat(hasGasSupplyPage.sectionHeader).containsText(gasSafetyHeader)
            assertThat(hasGasSupplyPage.heading).containsText("Does the property have a gas supply or any gas appliances?")
            hasGasSupplyPage.submitHasGasSupply()
            val hasGasCertPage = assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)

            // Has Gas Cert - render page
            assertThat(hasGasCertPage.form.sectionHeader).containsText(gasSafetyHeader)
            assertThat(hasGasCertPage.heading).containsText("Do you have a gas safety certificate for this property?")
            hasGasCertPage.submitHasNoCertificate()
            val gasCertMissingPage = assertPageIs(page, GasCertMissingFormPagePropertyRegistration::class)

            // Gas Cert Missing - render page
            assertThat(gasCertMissingPage.sectionHeader).containsText(gasSafetyHeader)
            assertThat(gasCertMissingPage.heading).containsText("You must get a gas safety certificate before a tenant moves in")
            assertThat(gasCertMissingPage.warning).isHidden()
            assertThat(gasCertMissingPage.submitButton).containsText("Continue")
            gasCertMissingPage.form.submit()
            val checkGasSafetyAnswersPage = assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)

            // Check Gas Safety Answers - render page
            assertThat(checkGasSafetyAnswersPage.heading).containsText("Gas safety certificate")
            checkGasSafetyAnswersPage.form.submit()
            val taskListPageAfterGasSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterGasSafety.clickRentedOutTaskWithName("Electrical safety certificate")
            val hasElectricalCertPage = assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)

            // Has Electrical Cert - render page
            assertThat(hasElectricalCertPage.form.sectionHeader).containsText(electricalSafetyHeader)
            assertThat(hasElectricalCertPage.heading).containsText("Which electrical safety certificate do you have for this property?")
            hasElectricalCertPage.submitHasNoCert()
            val electricalCertMissingPage = assertPageIs(page, ElectricalCertMissingFormPagePropertyRegistration::class)

            // Electrical Cert Missing - render page
            assertThat(electricalCertMissingPage.sectionHeader).containsText(electricalSafetyHeader)
            assertThat(
                electricalCertMissingPage.heading,
            ).containsText("You must get an electrical safety certificate before a tenant moves in")
            assertThat(electricalCertMissingPage.warning).isHidden()
            assertThat(electricalCertMissingPage.submitButton).containsText("Continue")
            electricalCertMissingPage.form.submit()
            val checkElectricalSafetyAnswersPage =
                assertPageIs(page, CheckElectricalSafetyAnswersFormPagePropertyRegistration::class)

            // Check Electrical Safety Answers - render page
            assertThat(checkElectricalSafetyAnswersPage.heading).containsText("Electrical safety certificate")
            checkElectricalSafetyAnswersPage.form.submit()
            val taskListPageAfterElectricalSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // We use a manual address, uprn will be null.
            // The internal EpcLookupByUprnStep at the start of the EpcTask will not find an EPC
            taskListPageAfterElectricalSafety.clickRentedOutTaskWithName("Energy performance certificate (EPC)")
            val hasEpcPage = assertPageIs(page, HasEpcFormPagePropertyRegistration::class)

            // Has EPC - render page
            assertThat(hasEpcPage.form.sectionHeader).containsText(epcSectionHeader)
            hasEpcPage.submitHasNoEpc()
            val isEpcRequiredPage = assertPageIs(page, IsEpcRequiredFormPagePropertyRegistration::class)

            // Is EPC required - render page
            assertThat(isEpcRequiredPage.form.sectionHeader).containsText(epcSectionHeader)
            assertThat(isEpcRequiredPage.heading).containsText("Is an EPC required to let this property?")
            isEpcRequiredPage.submitEpcRequired()
            val epcMissingPage = assertPageIs(page, EpcMissingFormPagePropertyRegistration::class)

            // EPC Missing - render page
            assertThat(epcMissingPage.sectionHeader).containsText(epcSectionHeader)
            assertThat(epcMissingPage.heading).containsText("Your property is missing an EPC")
            assertThat(epcMissingPage.continueButton).containsText("Continue")
            assertThat(epcMissingPage.warning).isHidden()
            epcMissingPage.form.submit()
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            val taskListPageAfterEpc = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterEpc.clickSubmitYourRegistrationTaskWithName("Check and submit your answers")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            // Check answers - render page
            assertThat(checkAnswersPage.heading).containsText("Check your answers for:")
            assertThat(checkAnswersPage.sectionHeader).containsText("Submit your registration")
            // submit
            checkAnswersPage.confirm()
            val confirmationPage = assertPageIs(page, ConfirmationPagePropertyRegistration::class)

            // Confirmation - render page
            val propertyOwnershipCaptor = captor<PropertyOwnership>()
            verify(propertyOwnershipRepository).save(propertyOwnershipCaptor.capture())
            val expectedPropertyRegNum =
                RegistrationNumberDataModel.fromRegistrationNumber(
                    propertyOwnershipCaptor.value.registrationNumber,
                )
            assertEquals(expectedPropertyRegNum.toString(), confirmationPage.registrationNumberText)
            assertFalse(propertyOwnershipCaptor.value.isOccupied)
            assertTrue(confirmationPage.whatYouNeedToDoNextHeading.isHidden)
            assertTrue(confirmationPage.surveyLink.locator.isVisible)
            assertThat(confirmationPage.surveyLink).hasAttribute("href", INDIVIDUAL_PROPERTY_REGISTRATION_SURVEY_URL)
            assertTrue(confirmationPage.goToDashboardLink.locator.isVisible)

            // Check confirmation email
            verify(confirmationEmailSender).sendEmail(
                "alex.surname@example.com",
                PropertyRegistrationConfirmationEmail(
                    expectedPropertyRegNum.toString(),
                    "Test address line 1, Testville, EG1 2AB",
                    absoluteLandlordUrl,
                    false,
                    null,
                ),
            )

            // Go to dashboard
            confirmationPage.goToDashboardLink.clickAndWait()
            assertPageIs(page, LandlordDashboardPage::class)
        }

        @Test
        fun `User can choose to provide compliance certificates later if their property is occupied`(page: Page) {
            val hasGasSupplyPage = navigator.skipToPropertyRegistrationHasGasSupplyPage(propertyIsOccupied = true)
            assertThat(hasGasSupplyPage.sectionHeader).containsText(gasSafetyHeader)
            hasGasSupplyPage.submitHasGasSupply()
            val hasGasCertPage = assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)

            // Has Gas Cert. Submit with no option selected
            assertThat(hasGasCertPage.form.sectionHeader).containsText(gasSafetyHeader)
            hasGasCertPage.submitProvideThisLater()
            val provideGasCertLaterPage = assertPageIs(page, ProvideGasCertLaterFormPagePropertyRegistration::class)

            // Provide Gas Cert Later - render page
            assertThat(provideGasCertLaterPage.sectionHeader).containsText(gasSafetyHeader)
            assertThat(provideGasCertLaterPage.insetText).containsText("You must upload your gas safety certificate within 28 days")
            provideGasCertLaterPage.form.submit()
            val checkGasSafetyAnswersPage = assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)

            // Check Gas Safety Answers - render page
            assertThat(checkGasSafetyAnswersPage.heading).containsText("Gas safety certificate")
            checkGasSafetyAnswersPage.form.submit()
            val taskListPageAfterGasSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterGasSafety.clickRentedOutTaskWithName("Electrical safety certificate")
            val hasElectricalCertPage = assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)

            // Has Electrical Cert - render page
            assertThat(hasElectricalCertPage.form.sectionHeader).containsText(electricalSafetyHeader)
            hasElectricalCertPage.submitProvideThisLater()
            val provideElectricalCertLaterPage =
                assertPageIs(page, ProvideElectricalCertLaterFormPagePropertyRegistration::class)

            // Provide Electrical Cert Later - render page
            assertThat(provideElectricalCertLaterPage.sectionHeader).containsText(electricalSafetyHeader)
            assertThat(
                provideElectricalCertLaterPage.insetText,
            ).containsText("You must upload your electrical safety certificate within 28 days.")
            provideElectricalCertLaterPage.form.submit()
            val checkElectricalSafetyAnswersPage =
                assertPageIs(page, CheckElectricalSafetyAnswersFormPagePropertyRegistration::class)

            // Setup EpcLookupByUprnStep NOT finding an EPC for this property when the next step submits
            whenever(epcRegisterClient.getByUprn(uprnForSelectedAddress)).thenReturn(MockEpcData.epcRegisterClientEpcNotFoundResponse)

            // Check Electrical Safety Answers - render page
            assertThat(checkElectricalSafetyAnswersPage.heading).containsText("Electrical safety certificate")
            checkElectricalSafetyAnswersPage.form.submit()
            val taskListPageAfterElectricalSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // The internal EpcLookupByUprnStep at the start of the EpcTask does not find an EPC
            taskListPageAfterElectricalSafety.clickRentedOutTaskWithName("Energy performance certificate (EPC)")
            val hasEpcPage = assertPageIs(page, HasEpcFormPagePropertyRegistration::class)

            // Has EPC - render page
            assertThat(hasEpcPage.form.sectionHeader).containsText(epcSectionHeader)
            hasEpcPage.submitProvideThisLater()
            val provideEpcLaterPage = assertPageIs(page, ProvideEpcLaterFormPagePropertyRegistration::class)

            // Provide EPC Later - render page
            assertThat(provideEpcLaterPage.sectionHeader).containsText(epcSectionHeader)
            assertThat(provideEpcLaterPage.heading).containsText("Provide your EPC details later")
            assertThat(provideEpcLaterPage.insetText).containsText(
                "To keep the property registered, we need all its compliance certificates within 28 days.",
            )
            provideEpcLaterPage.form.submit()

            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            assertPageIs(page, TaskListPagePropertyRegistration::class)
        }

        @Test
        fun `User can choose to provide compliance certificates later if their property is unoccupied`(page: Page) {
            val hasGasSupplyPage = navigator.skipToPropertyRegistrationHasGasSupplyPage(propertyIsOccupied = false)
            assertThat(hasGasSupplyPage.sectionHeader).containsText(gasSafetyHeader)
            hasGasSupplyPage.submitHasGasSupply()
            val hasGasCertPage = assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)

            // Has Gas Cert. Submit with no option selected
            assertThat(hasGasCertPage.form.sectionHeader).containsText(gasSafetyHeader)
            hasGasCertPage.submitProvideThisLater()
            val provideGasCertLaterPage = assertPageIs(page, ProvideGasCertLaterFormPagePropertyRegistration::class)

            // Provide Gas Cert Later - render page
            assertThat(provideGasCertLaterPage.sectionHeader).containsText(gasSafetyHeader)
            assertThat(provideGasCertLaterPage.insetText).isHidden()
            assertTrue(
                provideGasCertLaterPage.page
                    .content()
                    .contains("You must get a gas safety certificate before a tenant moves in."),
            )
            provideGasCertLaterPage.form.submit()
            val checkGasSafetyAnswersPage = assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)

            // Check Gas Safety Answers - render page
            assertThat(checkGasSafetyAnswersPage.heading).containsText("Gas safety certificate")
            checkGasSafetyAnswersPage.form.submit()
            val taskListPageAfterGasSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterGasSafety.clickRentedOutTaskWithName("Electrical safety certificate")
            val hasElectricalCertPage = assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)

            // Has Electrical Cert - render page
            assertThat(hasElectricalCertPage.form.sectionHeader).containsText(electricalSafetyHeader)
            hasElectricalCertPage.submitProvideThisLater()
            val provideElectricalCertLaterPage =
                assertPageIs(page, ProvideElectricalCertLaterFormPagePropertyRegistration::class)

            // Provide Electrical Cert Later - render page (unoccupied variant)
            assertThat(provideElectricalCertLaterPage.sectionHeader).containsText(electricalSafetyHeader)
            assertThat(provideElectricalCertLaterPage.heading).containsText("Provide your electrical safety certificate later")
            assertThat(provideElectricalCertLaterPage.insetText).isHidden()
            assertTrue(
                provideElectricalCertLaterPage.page
                    .content()
                    .contains("You must get an electrical safety certificate before a tenant moves in."),
            )
            provideElectricalCertLaterPage.form.submit()
            val checkElectricalSafetyAnswersPage =
                assertPageIs(page, CheckElectricalSafetyAnswersFormPagePropertyRegistration::class)

            // Setup EpcLookupByUprnStep NOT finding an EPC for this property when the next step submits
            whenever(epcRegisterClient.getByUprn(uprnForSelectedAddress)).thenReturn(MockEpcData.epcRegisterClientEpcNotFoundResponse)

            // Check Electrical Safety Answers - render page
            assertThat(checkElectricalSafetyAnswersPage.heading).containsText("Electrical safety certificate")
            checkElectricalSafetyAnswersPage.form.submit()
            val taskListPageAfterElectricalSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // The internal EpcLookupByUprnStep at the start of the EpcTask does not find an EPC
            taskListPageAfterElectricalSafety.clickRentedOutTaskWithName("Energy performance certificate (EPC)")
            val hasEpcPage = assertPageIs(page, HasEpcFormPagePropertyRegistration::class)

            // Has EPC - render page
            assertThat(hasEpcPage.form.sectionHeader).containsText(epcSectionHeader)
            hasEpcPage.submitProvideThisLater()
            val provideEpcLaterPage = assertPageIs(page, ProvideEpcLaterFormPagePropertyRegistration::class)

            // Provide EPC Later - render page
            assertThat(provideEpcLaterPage.sectionHeader).containsText(epcSectionHeader)
            assertThat(provideEpcLaterPage.heading).containsText("Provide your EPC details later")
            assertThat(provideEpcLaterPage.insetText).isHidden()
            provideEpcLaterPage.form.submit()

            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            assertPageIs(page, TaskListPagePropertyRegistration::class)
        }

        @Test
        fun `User can complete the journey with missing compliance certificates for an occupied property`(page: Page) {
            // Gas supply page
            val hasGasSupplyPage = navigator.skipToPropertyRegistrationHasGasSupplyPage(propertyIsOccupied = true)
            assertThat(hasGasSupplyPage.sectionHeader).containsText(gasSafetyHeader)
            hasGasSupplyPage.submitHasGasSupply()
            val hasGasCertPage = assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)

            // Has Gas Cert page
            assertThat(hasGasCertPage.form.sectionHeader).containsText(gasSafetyHeader)
            hasGasCertPage.submitHasNoCertificate()
            val gasCertMissingPage = assertPageIs(page, GasCertMissingFormPagePropertyRegistration::class)

            // Gas Cert Missing - render page
            assertThat(gasCertMissingPage.sectionHeader).containsText(gasSafetyHeader)
            assertThat(gasCertMissingPage.heading).containsText("You must get a valid gas safety certificate for this property")
            assertThat(gasCertMissingPage.submitButton).containsText("Continue without a valid gas safety certificate")
            assertThat(gasCertMissingPage.warning)
                .containsText("You could face prosecution if you have tenants in a property without a gas safety certificate")
            gasCertMissingPage.form.submit()
            val checkGasSafetyAnswersPage = assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)

            // Check Gas Safety Answers - render page
            assertThat(checkGasSafetyAnswersPage.heading).containsText("Gas safety certificate")
            checkGasSafetyAnswersPage.form.submit()
            val taskListPageAfterGasSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterGasSafety.clickRentedOutTaskWithName("Electrical safety certificate")
            val hasElectricalCertPage = assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)

            // Has Electrical Cert - render page
            assertThat(hasElectricalCertPage.form.sectionHeader).containsText(electricalSafetyHeader)
            assertThat(hasElectricalCertPage.heading).containsText("Which electrical safety certificate do you have for this property?")
            hasElectricalCertPage.submitHasNoCert()
            val electricalCertMissingPage = assertPageIs(page, ElectricalCertMissingFormPagePropertyRegistration::class)

            assertThat(electricalCertMissingPage.sectionHeader).containsText(electricalSafetyHeader)
            assertThat(
                electricalCertMissingPage.heading,
            ).containsText("You must get a valid electrical safety certificate for this property")
            assertThat(electricalCertMissingPage.warning)
                .containsText("You could face prosecution if you have tenants in a property without an electrical safety certificate.")
            assertThat(electricalCertMissingPage.submitButton).containsText("Continue without a valid electrical safety certificate")
            electricalCertMissingPage.form.submit()
            val checkElectricalSafetyAnswersPage =
                assertPageIs(page, CheckElectricalSafetyAnswersFormPagePropertyRegistration::class)

            // Setup EpcLookupByUprnStep NOT finding an EPC for this property when the next step submits
            whenever(epcRegisterClient.getByUprn(uprnForSelectedAddress)).thenReturn(MockEpcData.epcRegisterClientEpcNotFoundResponse)

            // Check Electrical Safety Answers - render page
            assertThat(checkElectricalSafetyAnswersPage.heading).containsText("Electrical safety certificate")
            checkElectricalSafetyAnswersPage.form.submit()
            val taskListPageAfterElectricalSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // The internal EpcLookupByUprnStep at the start of the EpcTask does not find an EPC
            taskListPageAfterElectricalSafety.clickRentedOutTaskWithName("Energy performance certificate (EPC)")
            val hasEpcPage = assertPageIs(page, HasEpcFormPagePropertyRegistration::class)

            // Has EPC - render page
            assertThat(hasEpcPage.form.sectionHeader).containsText(epcSectionHeader)
            hasEpcPage.submitHasNoEpc()
            val isEpcRequiredPage = assertPageIs(page, IsEpcRequiredFormPagePropertyRegistration::class)

            // Is EPC required - render page
            assertThat(isEpcRequiredPage.form.sectionHeader).containsText(epcSectionHeader)
            assertThat(isEpcRequiredPage.heading).containsText("Is an EPC required to let this property?")
            isEpcRequiredPage.submitEpcRequired()
            val epcMissingPage = assertPageIs(page, EpcMissingFormPagePropertyRegistration::class)

            // EPC Missing - render page
            assertThat(epcMissingPage.sectionHeader).containsText(epcSectionHeader)
            assertThat(epcMissingPage.heading).containsText("Your property is missing an EPC")
            assertThat(epcMissingPage.continueAnywayButton).containsText("Continue anyway")
            assertThat(
                epcMissingPage.warning,
            ).containsText("You can be fined for letting a property that does not meet energy efficiency requirements.")
            epcMissingPage.form.submit()
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            val taskListPageAfterEpc = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterEpc.clickSubmitYourRegistrationTaskWithName("Check and submit your answers")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            assertThat(checkAnswersPage.sectionHeader).containsText("Submit your registration")

            // Check Answers - submit to reach Confirm Missing Compliance page
            checkAnswersPage.form.submit()
            val confirmMissingCompliancePage =
                assertPageIs(page, ConfirmMissingComplianceFormPagePropertyRegistration::class)

            // Confirm Missing Compliance - render page
            assertThat(confirmMissingCompliancePage.heading).containsText("Confirm missing compliance certificates")
            assertThat(confirmMissingCompliancePage.warning).isVisible()
            assertThat(confirmMissingCompliancePage.form.sectionHeader).containsText("Submit registration")

            // Confirm Missing Compliance - submit
            confirmMissingCompliancePage.form.radios.selectValue("true")
            confirmMissingCompliancePage.form.submit()
            val confirmationPage = assertPageIs(page, ConfirmationPagePropertyRegistration::class)

            // Confirmation - verify record saved
            val propertyOwnershipCaptor = captor<PropertyOwnership>()
            verify(propertyOwnershipRepository).save(propertyOwnershipCaptor.capture())
            val expectedPropertyRegNum =
                RegistrationNumberDataModel.fromRegistrationNumber(
                    propertyOwnershipCaptor.value.registrationNumber,
                )
            assertEquals(expectedPropertyRegNum.toString(), confirmationPage.registrationNumberText)
            assertTrue(confirmationPage.surveyLink.locator.isVisible)
            assertThat(confirmationPage.surveyLink).hasAttribute("href", INDIVIDUAL_PROPERTY_REGISTRATION_SURVEY_URL)
            assertTrue(confirmationPage.goToDashboardLink.locator.isVisible)
        }

        @Test
        fun `User can complete the journey with expired compliance certificates for an occupied property (epc found by uprn)`(page: Page) {
            // Gas supply page
            val hasGasSupplyPage = navigator.skipToPropertyRegistrationHasGasSupplyPage(propertyIsOccupied = true)
            assertThat(hasGasSupplyPage.sectionHeader).containsText(gasSafetyHeader)
            hasGasSupplyPage.submitHasGasSupply()
            val hasGasCertPage = assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)

            // Has Gas Cert page
            assertThat(hasGasCertPage.form.sectionHeader).containsText(gasSafetyHeader)
            hasGasCertPage.submitHasCertificate()
            var gasCertIssueDatePage = assertPageIs(page, GasCertIssueDateFormPagePropertyRegistration::class)

            // Gas Cert Issue Date - render page
            assertThat(gasCertIssueDatePage.form.sectionHeader).containsText(gasSafetyHeader)
            assertThat(gasCertIssueDatePage.heading).containsText("What’s the issue date on the gas safety certificate?")
            gasCertIssueDatePage.submitDate(expiredGasSafetyCertIssueDate)
            var gasCertExpiredPage = assertPageIs(page, GasCertExpiredFormPagePropertyRegistration::class)

            // Gas Cert Expired - render page then navigate to edit issue date
            assertThat(gasCertExpiredPage.sectionHeader).containsText(gasSafetyHeader)
            assertThat(gasCertExpiredPage.mainHeading).containsText("This gas safety certificate has expired")
            assertThat(gasCertExpiredPage.sectionHeading).containsText("You must get a valid gas safety certificate for this property")
            assertThat(gasCertExpiredPage.warning)
                .containsText("You could face prosecution if you have tenants in a property without a gas safety certificate.")
            assertThat(gasCertExpiredPage.submitButton).containsText("Continue without a valid gas safety certificate")
            gasCertExpiredPage.changeIssueDateLink.clickAndWait()
            gasCertIssueDatePage = assertPageIs(page, GasCertIssueDateFormPagePropertyRegistration::class)

            // Gas Cert Issue Date - render page, prepopulated with previous value, then submit again
            assertThat(gasCertIssueDatePage.form.dayInput).hasValue(expiredGasSafetyCertIssueDate.dayOfMonth.toString())
            assertThat(gasCertIssueDatePage.form.monthInput).hasValue(expiredGasSafetyCertIssueDate.monthNumber.toString())
            assertThat(gasCertIssueDatePage.form.yearInput).hasValue(expiredGasSafetyCertIssueDate.year.toString())
            gasCertIssueDatePage.form.submit()
            gasCertExpiredPage = assertPageIs(page, GasCertExpiredFormPagePropertyRegistration::class)

            // Back on Gas Cert Expired page - submit
            gasCertExpiredPage.form.submit()
            val checkGasSafetyAnswersPage = assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)

            // Check Gas Safety Answers - render page
            assertThat(checkGasSafetyAnswersPage.heading).containsText("Gas safety certificate")

            checkGasSafetyAnswersPage.form.submit()
            val taskListPageAfterGasSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterGasSafety.clickRentedOutTaskWithName("Electrical safety certificate")
            val hasElectricalCertPage = assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)

            // Has Electrical Cert - render page
            assertThat(hasElectricalCertPage.form.sectionHeader).containsText(electricalSafetyHeader)
            assertThat(hasElectricalCertPage.heading).containsText("Which electrical safety certificate do you have for this property?")
            hasElectricalCertPage.submitHasEic()
            var electricalCertExpiryDatePage =
                assertPageIs(page, ElectricalCertExpiryDateFormPagePropertyRegistration::class)

            // Electrical Cert Expiry Date - render page
            assertThat(electricalCertExpiryDatePage.form.sectionHeader).containsText(electricalSafetyHeader)
            electricalCertExpiryDatePage.submitDate(expiredExpiryDate)
            var electricalCertExpiredPage = assertPageIs(page, ElectricalCertExpiredFormPagePropertyRegistration::class)

            // Electrical Cert Expired - render page then check change expiry date link
            assertThat(electricalCertExpiredPage.sectionHeader).containsText(electricalSafetyHeader)
            assertThat(electricalCertExpiredPage.warning)
                .containsText("You could face prosecution if you have tenants in a property without an electrical safety certificate.")
            assertThat(electricalCertExpiredPage.submitButton).containsText("Continue without a valid electrical safety certificate")
            electricalCertExpiredPage.changeExpiryDateLink.clickAndWait()
            electricalCertExpiryDatePage =
                assertPageIs(page, ElectricalCertExpiryDateFormPagePropertyRegistration::class)

            // Electrical Cert Expiry Date again - render page, prepopulated with previous value, then submit again
            assertThat(electricalCertExpiryDatePage.form.dayInput).hasValue(expiredExpiryDate.dayOfMonth.toString())
            assertThat(electricalCertExpiryDatePage.form.monthInput).hasValue(expiredExpiryDate.monthNumber.toString())
            assertThat(electricalCertExpiryDatePage.form.yearInput).hasValue(expiredExpiryDate.year.toString())
            electricalCertExpiryDatePage.form.submit()
            electricalCertExpiredPage = assertPageIs(page, ElectricalCertExpiredFormPagePropertyRegistration::class)

            // Back on Electrical Cert Expired page - submit
            electricalCertExpiredPage.form.submit()
            val checkElectricalSafetyAnswersPage =
                assertPageIs(page, CheckElectricalSafetyAnswersFormPagePropertyRegistration::class)

            // Setup EpcLookupByUprnStep being able to find an EPC for this property when the next step submits
            whenever(epcRegisterClient.getByUprn(uprnForSelectedAddress))
                .thenReturn(
                    MockEpcData.createEpcRegisterClientEpcFoundResponse(
                        expiryDate = expiredExpiryDate,
                    ),
                )

            // Check Electrical Safety Answers - render page
            assertThat(checkElectricalSafetyAnswersPage.heading).containsText("Electrical safety certificate")
            checkElectricalSafetyAnswersPage.form.submit()
            val taskListPageAfterElectricalSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // EpcLookupByUprnStep finds the EPC, so redirects to Check UPRN matched EPCe
            taskListPageAfterElectricalSafety.clickRentedOutTaskWithName("Energy performance certificate (EPC)")
            val confirmUprnMatchedEpcDetailsPage =
                assertPageIs(page, ConfirmEpcDetailsRetrievedByUprnFormPagePropertyRegistration::class)

            // Check UPRN matched EPC - submit Yes (accept this expired EPC, which triggers age/rating check internally)
            assertThat(confirmUprnMatchedEpcDetailsPage.sectionHeader).containsText(epcSectionHeader)
            confirmUprnMatchedEpcDetailsPage.submitYes()
            val epcExpiryCheckPage = assertPageIs(page, EpcInDateAtStartOfTenancyCheckPagePropertyRegistration::class)

            assertThat(epcExpiryCheckPage.form.sectionHeader).containsText(epcSectionHeader)
            epcExpiryCheckPage.submitEpcExpired()
            val epcExpiredPage = assertPageIs(page, EpcExpiredFormPagePropertyRegistration::class)

            // EPC Expired - occupied variant: warning visible, "Continue anyway" button
            assertThat(epcExpiredPage.sectionHeader).containsText(epcSectionHeader)
            assertThat(epcExpiredPage.heading).containsText("This property’s EPC has expired")
            assertThat(epcExpiredPage.warning).isVisible()
            assertThat(epcExpiredPage.submitButton).containsText("Continue anyway")
            epcExpiredPage.form.submit()
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            val taskListPageAfterEpc = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterEpc.clickSubmitYourRegistrationTaskWithName("Check and submit your answers")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            assertThat(checkAnswersPage.sectionHeader).containsText("Submit your registration")
        }

        @Test
        fun `User can complete the journey with expired compliance certificates for an unoccupied property (epc not found by uprn)`(
            page: Page,
        ) {
            // Gas supply page
            val hasGasSupplyPage = navigator.skipToPropertyRegistrationHasGasSupplyPage(propertyIsOccupied = false)
            assertThat(hasGasSupplyPage.sectionHeader).containsText(gasSafetyHeader)
            hasGasSupplyPage.submitHasGasSupply()
            val hasGasCertPage = assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)

            // Has Gas Cert page
            assertThat(hasGasCertPage.form.sectionHeader).containsText(gasSafetyHeader)
            hasGasCertPage.submitHasCertificate()
            var gasCertIssueDatePage = assertPageIs(page, GasCertIssueDateFormPagePropertyRegistration::class)

            // Gas Cert Issue Date - render page
            assertThat(gasCertIssueDatePage.form.sectionHeader).containsText(gasSafetyHeader)
            assertThat(gasCertIssueDatePage.heading).containsText("What’s the issue date on the gas safety certificate?")
            gasCertIssueDatePage.submitDate(expiredGasSafetyCertIssueDate)
            var gasCertExpiredPage = assertPageIs(page, GasCertExpiredFormPagePropertyRegistration::class)

            // Gas Cert Expired - render page then navigate to edit issue date
            assertThat(gasCertExpiredPage.sectionHeader).containsText(gasSafetyHeader)
            assertThat(gasCertExpiredPage.mainHeading).containsText("This gas safety certificate has expired")
            assertThat(gasCertExpiredPage.sectionHeading).containsText("What to do next")
            assertThat(gasCertExpiredPage.warning).isHidden()
            assertThat(gasCertExpiredPage.submitButton).containsText("Save and continue")
            gasCertExpiredPage.changeIssueDateLink.clickAndWait()
            gasCertIssueDatePage = assertPageIs(page, GasCertIssueDateFormPagePropertyRegistration::class)

            // Gas Cert Issue Date - render page, prepopulated with previous value, then submit again
            assertThat(gasCertIssueDatePage.form.dayInput).hasValue(expiredGasSafetyCertIssueDate.dayOfMonth.toString())
            assertThat(gasCertIssueDatePage.form.monthInput).hasValue(expiredGasSafetyCertIssueDate.monthNumber.toString())
            assertThat(gasCertIssueDatePage.form.yearInput).hasValue(expiredGasSafetyCertIssueDate.year.toString())
            gasCertIssueDatePage.form.submit()
            gasCertExpiredPage = assertPageIs(page, GasCertExpiredFormPagePropertyRegistration::class)

            // Back on Gas Cert Expired page - submit
            gasCertExpiredPage.form.submit()
            val checkGasSafetyAnswersPage = assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)

            // Check Gas Safety Answers - render page
            assertThat(checkGasSafetyAnswersPage.heading).containsText("Gas safety certificate")

            checkGasSafetyAnswersPage.form.submit()
            val taskListPageAfterGasSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterGasSafety.clickRentedOutTaskWithName("Electrical safety certificate")
            val hasElectricalCertPage = assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)

            // Has Electrical Cert - render page
            assertThat(hasElectricalCertPage.form.sectionHeader).containsText(electricalSafetyHeader)
            assertThat(hasElectricalCertPage.heading).containsText("Which electrical safety certificate do you have for this property?")
            hasElectricalCertPage.submitHasEic()
            var electricalCertExpiryDatePage =
                assertPageIs(page, ElectricalCertExpiryDateFormPagePropertyRegistration::class)

            // Electrical Cert Expiry Date - render page
            assertThat(electricalCertExpiryDatePage.form.sectionHeader).containsText(electricalSafetyHeader)
            electricalCertExpiryDatePage.submitDate(expiredExpiryDate)
            var electricalCertExpiredPage = assertPageIs(page, ElectricalCertExpiredFormPagePropertyRegistration::class)

            // Electrical Cert Expired - render page then check change expiry date link
            assertThat(electricalCertExpiredPage.sectionHeader).containsText(electricalSafetyHeader)
            assertThat(electricalCertExpiredPage.warning).isHidden()
            assertThat(electricalCertExpiredPage.submitButton).containsText("Save and continue")
            electricalCertExpiredPage.changeExpiryDateLink.clickAndWait()
            electricalCertExpiryDatePage =
                assertPageIs(page, ElectricalCertExpiryDateFormPagePropertyRegistration::class)

            // Electrical Cert Expiry Date again - render page, prepopulated with previous value, then submit again
            assertThat(electricalCertExpiryDatePage.form.dayInput).hasValue(expiredExpiryDate.dayOfMonth.toString())
            assertThat(electricalCertExpiryDatePage.form.monthInput).hasValue(expiredExpiryDate.monthNumber.toString())
            assertThat(electricalCertExpiryDatePage.form.yearInput).hasValue(expiredExpiryDate.year.toString())
            electricalCertExpiryDatePage.form.submit()
            electricalCertExpiredPage = assertPageIs(page, ElectricalCertExpiredFormPagePropertyRegistration::class)

            // Back on Electrical Cert Expired page - submit
            electricalCertExpiredPage.form.submit()
            val checkElectricalSafetyAnswersPage =
                assertPageIs(page, CheckElectricalSafetyAnswersFormPagePropertyRegistration::class)

            // Setup EpcLookupByUprnStep NOT finding an EPC for this property when the next step submits
            whenever(epcRegisterClient.getByUprn(uprnForSelectedAddress)).thenReturn(MockEpcData.epcRegisterClientEpcNotFoundResponse)

            // Check Electrical Safety Answers - render page
            assertThat(checkElectricalSafetyAnswersPage.heading).containsText("Electrical safety certificate")
            checkElectricalSafetyAnswersPage.form.submit()
            val taskListPageAfterElectricalSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // The internal EpcLookupByUprnStep at the start of the EpcTask does not find an EPC
            taskListPageAfterElectricalSafety.clickRentedOutTaskWithName("Energy performance certificate (EPC)")
            val hasEpcPage = assertPageIs(page, HasEpcFormPagePropertyRegistration::class)

            // Has EPC - render page
            assertThat(hasEpcPage.form.sectionHeader).containsText(epcSectionHeader)
            hasEpcPage.submitHasEpc()
            val findYourEpcPage = assertPageIs(page, FindYourEpcFormPagePropertyRegistration::class)

            // EPC Search - render page
            assertThat(findYourEpcPage.form.sectionHeader).containsText(epcSectionHeader)
            whenever(epcRegisterClient.getByRrn(CURRENT_EXPIRED_EPC_CERTIFICATE_NUMBER))
                .thenReturn(
                    MockEpcData.createEpcRegisterClientEpcFoundResponse(
                        certificateNumber = CURRENT_EXPIRED_EPC_CERTIFICATE_NUMBER,
                        expiryDate = expiredExpiryDate,
                        latestCertificateNumberForThisProperty = CURRENT_EXPIRED_EPC_CERTIFICATE_NUMBER,
                    ),
                )
            findYourEpcPage.submitCurrentEpcNumberWhichIsExpired()
            val confirmEpcDetailsPage =
                assertPageIs(page, ConfirmEpcDetailsRetrievedByCertificateNumberPagePropertyRegistration::class)

            // Check Matched EPC - render page
            assertThat(confirmEpcDetailsPage.sectionHeader).containsText(epcSectionHeader)
            val expectedExpiryDate =
                expiredExpiryDate
                    .toJavaLocalDate()
                    .format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
            assertThat(confirmEpcDetailsPage.summaryCard.summaryList.addressRow.value).containsText(MockEpcData.defaultSingleLineAddress)
            assertThat(confirmEpcDetailsPage.summaryCard.summaryList.energyEfficiencyRatingRow.value).containsText("C")
            assertThat(confirmEpcDetailsPage.summaryCard.summaryList.expiryDateRow.value).containsText(
                expectedExpiryDate,
            )
            assertThat(
                confirmEpcDetailsPage.summaryCard.summaryList.certificateNumberRow.value,
            ).containsText(CURRENT_EXPIRED_EPC_CERTIFICATE_NUMBER)
            confirmEpcDetailsPage.submitYes()
            val epcExpiredPage = assertPageIs(page, EpcExpiredFormPagePropertyRegistration::class)

            // EPC Expired - unoccupied variant: no warning, "Continue" button
            assertThat(epcExpiredPage.sectionHeader).containsText(epcSectionHeader)
            assertThat(epcExpiredPage.heading).containsText("This property’s EPC has expired")
            assertThat(epcExpiredPage.warning).isHidden()
            assertThat(epcExpiredPage.submitButton).containsText("Continue")
            epcExpiredPage.form.submit()
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            val taskListPageAfterEpc = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterEpc.clickSubmitYourRegistrationTaskWithName("Check and submit your answers")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            assertThat(checkAnswersPage.sectionHeader).containsText("Submit your registration")
        }

        @Test
        fun `The Electrical Safety task can be completed by the user uploaded an eicr`(page: Page) {
            // Skip to Has Electrical Cert page and submit "Yes"
            val hasElectricalCertPage = navigator.skipToPropertyRegistrationHasElectricalCertPage()
            assertThat(hasElectricalCertPage.form.sectionHeader).containsText(electricalSafetyHeader)
            hasElectricalCertPage.submitHasEicr()
            val electricalCertExpiryDatePage =
                assertPageIs(page, ElectricalCertExpiryDateFormPagePropertyRegistration::class)

            // Electrical Cert Expiry Date - render page
            assertThat(electricalCertExpiryDatePage.form.sectionHeader).containsText(electricalSafetyHeader)
            assertThat(
                electricalCertExpiryDatePage.heading,
            ).containsText("What’s the expiry date on the Electrical Installation Condition Report?")
            electricalCertExpiryDatePage.submitDate(validExpiryDate)
            val uploadElectricalCertPage = assertPageIs(page, UploadElectricalCertFormPagePropertyRegistration::class)

            // Upload Electrical Cert - render page
            assertThat(uploadElectricalCertPage.form.sectionHeader).containsText(electricalSafetyHeader)
            assertThat(uploadElectricalCertPage.heading).containsText("Upload the Electrical Installation Condition Report (EICR)")
            uploadElectricalCertPage.uploadElectricalCertificate(Path.of("src/test/resources/test-files/blank.png"))

            // Check Electrical Safety Answers - EICR variant verified by heading text
        }

        @Test
        fun `The EPC task can be completed when FindYourEpc finds a superseded epc`(page: Page) {
            // Skip to Find Your EPC page and submit "Superseded EPC Found"
            val findYourEpcPage = navigator.skipToPropertyRegistrationFindYourEpcPage()
            assertThat(findYourEpcPage.form.sectionHeader).containsText(epcSectionHeader)
            whenever(epcRegisterClient.getByRrn(SUPERSEDED_EPC_CERTIFICATE_NUMBER)).thenReturn(
                MockEpcData.createEpcRegisterClientEpcFoundResponse(
                    certificateNumber = SUPERSEDED_EPC_CERTIFICATE_NUMBER,
                    expiryDate = MockEpcData.expiryDateInThePast,
                    latestCertificateNumberForThisProperty = CURRENT_EPC_CERTIFICATE_NUMBER,
                ),
            )
            whenever(epcRegisterClient.getByRrn(CURRENT_EPC_CERTIFICATE_NUMBER)).thenReturn(
                MockEpcData.createEpcRegisterClientEpcFoundResponse(
                    certificateNumber = CURRENT_EPC_CERTIFICATE_NUMBER,
                ),
            )
            findYourEpcPage.submitSupersededEpcNumber()
            val epcSupersededPage = assertPageIs(page, EpcSuperseededFormPagePropertyRegistration::class)

            // Check details of superseded and latest epc - render page
            assertThat(epcSupersededPage.sectionHeader).containsText(epcSectionHeader)
            epcSupersededPage.submitContinueWithLatest()
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            assertPageIs(page, TaskListPagePropertyRegistration::class)
        }

        @Test
        fun `The EPC task can be completed when FindYourEpc finds no epc and it is missing`(page: Page) {
            // Skip to Find Your EPC page and submit "No EPC Found"
            val findYourEpcPage = navigator.skipToPropertyRegistrationFindYourEpcPage()
            assertThat(findYourEpcPage.form.sectionHeader).containsText(epcSectionHeader)
            whenever(
                epcRegisterClient.getByRrn(NONEXISTENT_EPC_CERTIFICATE_NUMBER),
            ).thenReturn(MockEpcData.epcRegisterClientEpcNotFoundResponse)
            findYourEpcPage.submitNonexistentEpcNumber()
            val epcNotFoundPage = assertPageIs(page, EpcNotFoundFormPagePropertyRegistration::class)

            // EPC not found - render page
            assertThat(epcNotFoundPage.sectionHeader).containsText(epcSectionHeader)
            assertThat(epcNotFoundPage.heading).containsText("We could not find your EPC")
            assertThat(epcNotFoundPage.certificateNumberText).containsText(NONEXISTENT_EPC_CERTIFICATE_NUMBER)
            assertThat(epcNotFoundPage.searchAgainLink).isVisible()

            // Click 'search again' to return to Find Your EPC and re-submit not found
            epcNotFoundPage.searchAgainLink.click()
            val findYourEpcPageAgain = assertPageIs(page, FindYourEpcFormPagePropertyRegistration::class)
            assertThat(findYourEpcPageAgain.form.sectionHeader).containsText(epcSectionHeader)
            whenever(
                epcRegisterClient.getByRrn(NONEXISTENT_EPC_CERTIFICATE_NUMBER),
            ).thenReturn(MockEpcData.epcRegisterClientEpcNotFoundResponse)
            findYourEpcPageAgain.submitNonexistentEpcNumber()
            assertPageIs(page, EpcNotFoundFormPagePropertyRegistration::class).form.submit()

            val isEpcRequiredPage = assertPageIs(page, IsEpcRequiredFormPagePropertyRegistration::class)

            // Is EPC required - render page
            assertThat(isEpcRequiredPage.form.sectionHeader).containsText(epcSectionHeader)
            assertThat(isEpcRequiredPage.heading).containsText("Is an EPC required to let this property?")
            isEpcRequiredPage.submitEpcRequired()
            val epcMissingPage = assertPageIs(page, EpcMissingFormPagePropertyRegistration::class)

            // EPC Missing - render page
            assertThat(epcMissingPage.sectionHeader).containsText(epcSectionHeader)
            assertThat(epcMissingPage.heading).containsText("Your property is missing an EPC")
            assertThat(epcMissingPage.continueAnywayButton).containsText("Continue anyway")
            assertThat(
                epcMissingPage.warning,
            ).containsText("You can be fined for letting a property that does not meet energy efficiency requirements.")
            epcMissingPage.form.submit()
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            assertPageIs(page, TaskListPagePropertyRegistration::class)
        }

        @Test
        fun `User can navigate the MEES flow when they have a MEES exemption`(page: Page) {
            val hasMeesExemptionPage = navigator.skipToPropertyRegistrationHasMeesExemptionPage()

            // Has MEES Exemption - render page
            assertThat(hasMeesExemptionPage.sectionHeader).containsText(epcSectionHeader)
            assertThat(hasMeesExemptionPage.heading).containsText("You need a registered energy efficiency exemption to let this property")
            hasMeesExemptionPage.submitHasMeesExemption()
            val meesExemptionPage = assertPageIs(page, MeesExemptionFormPagePropertyRegistration::class)

            // MEES Exemption - select exemption reason
            assertThat(meesExemptionPage.form.sectionHeader).containsText(epcSectionHeader)
            meesExemptionPage.submitExemptionReason(MeesExemptionReason.HIGH_COST)
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
        }

        @Test
        fun `User can navigate the MEES flow when they do not have a MEES exemption`(page: Page) {
            val hasMeesExemptionPage = navigator.skipToPropertyRegistrationHasMeesExemptionPage()

            // Has MEES Exemption - submit no exemption
            assertThat(hasMeesExemptionPage.sectionHeader).containsText(epcSectionHeader)
            hasMeesExemptionPage.submitHasNoMeesExemption()
            val lowEnergyRatingPage = assertPageIs(page, LowEnergyRatingFormPagePropertyRegistration::class)

            // Low Energy Rating - render page
            assertThat(lowEnergyRatingPage.sectionHeader).containsText(epcSectionHeader)
            assertThat(lowEnergyRatingPage.heading).containsText("This property does not meet energy efficiency requirements for letting")
            lowEnergyRatingPage.form.submit()
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
        }

        @Test
        fun `User can navigate the EPC exemption flow`(page: Page) {
            val epcExemptionPage = navigator.skipToPropertyRegistrationEpcExemptionPage()

            // EPC Exemption - select exemption reason
            assertThat(epcExemptionPage.form.sectionHeader).containsText(epcSectionHeader)
            epcExemptionPage.submitExemptionReason(EpcExemptionReason.PROTECTED_ARCHITECTURAL_OR_HISTORICAL_MERIT)
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
        }

        @Test
        fun `task list back link navigates to start page after entering from start page`(page: Page) {
            val registerPropertyStartPage = navigator.goToPropertyRegistrationStartPage()
            registerPropertyStartPage.startButton.clickAndWait()
            val taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)

            taskListPage.backLink.clickAndWait()
            assertPageIs(page, RegisterPropertyStartPage::class)
        }

        @Test
        fun `task list back link navigates to start page after entering from start page and returning from a task`(page: Page) {
            val registerPropertyStartPage = navigator.goToPropertyRegistrationStartPage()
            registerPropertyStartPage.startButton.clickAndWait()
            var taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)

            taskListPage.clickRegisterTaskWithName("Property details")
            assertPageIs(page, LookupAddressFormPagePropertyRegistration::class)

            val backLink = BackLink.default(page)
            backLink.clickAndWait()
            taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)

            taskListPage.backLink.clickAndWait()
            assertPageIs(page, RegisterPropertyStartPage::class)
        }

        @Test
        fun `restructured task list shows three sections with expected task order`(page: Page) {
            val registerPropertyStartPage = navigator.goToPropertyRegistrationStartPage()
            registerPropertyStartPage.startButton.clickAndWait()
            val taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)

            assertEquals(3, taskListPage.getSectionCount())
            assertThat(taskListPage.getSectionHeading(0)).hasText("About your property")
            assertThat(taskListPage.getSectionHeading(1)).hasText("How your property’s rented out")
            assertThat(taskListPage.getSectionHeading(2)).hasText("Submit your registration")
            assertEquals(
                listOf("Property details", "Ownership and landlords", "Tell us if your property’s occupied"),
                taskListPage.getAboutYourPropertyTaskNames(),
            )
            assertEquals(
                listOf(
                    "Who will provide these details",
                    "Tell us if your property needs a license",
                    "Gas safety certificate",
                    "Electrical safety certificate",
                    "Energy performance certificate (EPC)",
                    "Tenancy details",
                ),
                taskListPage.getRentedOutTaskNames(),
            )
            assertEquals(
                listOf("Check and submit your answers"),
                taskListPage.getSubmitYourRegistrationTaskNames(),
            )

            assertTrue(taskListPage.getAboutYourPropertyTask("Property details").hasLink)
            taskListPage.clickAboutYourPropertyTaskWithName("Property details")
            assertPageIs(page, LookupAddressFormPagePropertyRegistration::class)
        }

        @Test
        fun `restructured task list shows tenancy details as not required when the property is unoccupied`(page: Page) {
            val taskListPage = navigator.goToRestructuredPropertyRegistrationTaskListUnoccupied()
            val tenancyDetailsTask = taskListPage.getRentedOutTask("Tenancy details")

            // The label uses a non-breaking space so "Not required" doesn't wrap onto two lines when hint text is present
            assertEquals("Not\u00A0required", tenancyDetailsTask.statusText.trim())
            assertEquals(
                "We’ll ask for tenancy details when your property becomes occupied",
                tenancyDetailsTask.hintText.trim(),
            )
            assertFalse(tenancyDetailsTask.hasLink)

            val checkAndSubmitTask = taskListPage.getSubmitYourRegistrationTask("Check and submit your answers")
            assertTrue(checkAndSubmitTask.hasLink)
            taskListPage.clickSubmitYourRegistrationTaskWithName("Check and submit your answers")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            // Bedrooms is collected as a property detail for all properties, so it is shown on the CYA even when unoccupied
            assertThat(checkAnswersPage.summaryList.numberOfBedroomsRow.value).containsText("3")
        }

        @Test
        fun `restructured CYA does not show tenancy details section when the property is unoccupied`(page: Page) {
            val taskListPage = navigator.goToRestructuredPropertyRegistrationTaskListUnoccupied()
            taskListPage.clickSubmitYourRegistrationTaskWithName("Check and submit your answers")

            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            assertThat(checkAnswersPage.restructuredTenancyHeading).isHidden()
        }

        @Test
        fun `restructured CYA shows occupancy section heading and Yes for occupied by tenants when property is occupied`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageOccupied()

            assertThat(checkAnswersPage.occupancyHeading).containsText("Tell us if your property’s occupied")
            assertThat(checkAnswersPage.summaryList.occupancyQuestionRow.value).containsText("Yes")
        }

        @Test
        fun `restructured CYA shows occupancy section heading and No for occupied by tenants when property is unoccupied`(page: Page) {
            val taskListPage = navigator.goToRestructuredPropertyRegistrationTaskListUnoccupied()
            taskListPage.clickSubmitYourRegistrationTaskWithName("Check and submit your answers")

            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            assertThat(checkAnswersPage.occupancyHeading).containsText("Tell us if your property’s occupied")
            assertThat(checkAnswersPage.summaryList.occupancyQuestionRow.value).containsText("No")
        }

        @Test
        fun `the occupancy question change link on the restructured CYA navigates to the occupancy page`(page: Page) {
            val taskListPage =
                navigator.goToRestructuredPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswers()
                        .withBedrooms(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Check and submit your answers")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            checkAnswersPage.summaryList.occupancyQuestionRow.clickFirstActionLinkAndWait()
            assertPageIs(page, OccupancyFormPagePropertyRegistration::class)
        }

        @Test
        @Suppress("ktlint:standard:max-line-length")
        fun `restructured occupied journey reaches check answers after EPC and tenancy details`(page: Page) {
            val registerPropertyStartPage = navigator.goToPropertyRegistrationStartPage()
            registerPropertyStartPage.startButton.clickAndWait()
            var taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)

            taskListPage.clickAboutYourPropertyTaskWithName("Property details")
            val addressLookupPage = assertPageIs(page, LookupAddressFormPagePropertyRegistration::class)
            addressLookupPage.submitPostcodeAndBuildingNameOrNumber("FA1 1AA", "1")
            val selectAddressPage = assertPageIs(page, SelectAddressFormPagePropertyRegistration::class)
            selectAddressPage.selectAddressAndSubmit("1 Fictional Road, FA1 1AA")
            val propertyTypePage = assertPageIs(page, PropertyTypeFormPagePropertyRegistration::class)
            propertyTypePage.submitPropertyType(PropertyType.DETACHED_HOUSE)
            val bedroomsPage = assertPageIs(page, NumberOfBedroomsFormPagePropertyRegistration::class)
            bedroomsPage.submitNumOfBedrooms(3)
            val ownershipTypePage = assertPageIs(page, OwnershipTypeFormPagePropertyRegistration::class)
            ownershipTypePage.submitOwnershipType(OwnershipType.FREEHOLD)

            val hasJointLandlordsPage = assertPageIs(page, HasJointLandlordsFormBasePagePropertyRegistration::class)
            hasJointLandlordsPage.submitHasNoJointLandlords()

            val occupancyPage = assertPageIs(page, OccupancyFormPagePropertyRegistration::class)
            assertThat(occupancyPage.form.fieldsetHeading).containsText("Is your property occupied by tenants?")
            occupancyPage.submitIsOccupied()
            val whoProvidesRentalDetailsPage = assertPageIs(page, WhoProvidesRentalDetailsFormPagePropertyRegistration::class)
            whoProvidesRentalDetailsPage.submitLandlordProvidesDetails()
            val licensingTypePage = assertPageIs(page, LicensingTypeFormPagePropertyRegistration::class)
            assertThat(licensingTypePage.form.fieldsetHeading).containsText("Select the type of licence you have for your property")
            licensingTypePage.submitLicensingType(LicensingType.NO_LICENSING)
            val hasGasSupplyPage = assertPageIs(page, HasGasSupplyFormPagePropertyRegistration::class)
            assertThat(hasGasSupplyPage.heading).containsText("Does the property have a gas supply or any gas appliances?")
            hasGasSupplyPage.submitHasNoGasSupply()
            val checkGasSafetyAnswersPage = assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)
            assertThat(checkGasSafetyAnswersPage.sectionHeader).isHidden()
            checkGasSafetyAnswersPage.form.submit()

            taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPage.clickRentedOutTaskWithName("Electrical safety certificate")
            val hasElectricalCertPage = assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)
            hasElectricalCertPage.submitProvideThisLater()
            val provideElectricalCertLaterPage =
                assertPageIs(page, ProvideElectricalCertLaterFormPagePropertyRegistration::class)
            provideElectricalCertLaterPage.form.submit()
            val checkElectricalSafetyAnswersPage =
                assertPageIs(page, CheckElectricalSafetyAnswersFormPagePropertyRegistration::class)
            assertThat(checkElectricalSafetyAnswersPage.sectionHeader).isHidden()

            whenever(epcRegisterClient.getByUprn(uprnForSelectedAddress)).thenReturn(MockEpcData.epcRegisterClientEpcNotFoundResponse)
            checkElectricalSafetyAnswersPage.form.submit()
            taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPage.clickRentedOutTaskWithName("Energy performance certificate (EPC)")
            val hasEpcPage = assertPageIs(page, HasEpcFormPagePropertyRegistration::class)
            hasEpcPage.submitProvideThisLater()
            val provideEpcLaterPage = assertPageIs(page, ProvideEpcLaterFormPagePropertyRegistration::class)
            provideEpcLaterPage.form.submit()
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)
            assertThat(checkEpcAnswersPage.sectionHeader).isHidden()
            checkEpcAnswersPage.form.submit()

            taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)
            assertTrue(taskListPage.getRentedOutTask("Tenancy details").hasLink)
            taskListPage.clickRentedOutTaskWithName("Tenancy details")
            val householdsPage = assertPageIs(page, NumberOfHouseholdsFormPagePropertyRegistration::class)
            householdsPage.submitNumberOfHouseholds(2)
            val peoplePage = assertPageIs(page, NumberOfPeopleFormPagePropertyRegistration::class)
            peoplePage.submitNumOfPeople(2)
            val rentIncludesBillsPage = assertPageIs(page, RentIncludesBillsFormPagePropertyRegistration::class)
            rentIncludesBillsPage.submitIsNotIncluded()
            val furnishedPage = assertPageIs(page, FurnishedStatusFormPagePropertyRegistration::class)
            furnishedPage.submitFurnishedStatus(FurnishedStatus.FURNISHED)
            val rentFrequencyPage = assertPageIs(page, RentFrequencyFormPagePropertyRegistration::class)
            rentFrequencyPage.selectRentFrequency(RentFrequency.MONTHLY)
            rentFrequencyPage.form.submit()
            val rentAmountPage = assertPageIs(page, RentAmountFormPagePropertyRegistration::class)
            rentAmountPage.submitRentAmount("400")

            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            assertThat(checkAnswersPage.restructuredTenancyHeading).containsText("Tenancy details")
            assertThat(checkAnswersPage.tenancyHeading).isHidden()
        }

        @Test
        fun `User can choose to provide tenancy details later if their property is occupied`(page: Page) {
            navigator.skipToTenancyDetailsHouseholdsPage()
            val householdsPage = assertPageIs(page, NumberOfHouseholdsFormPagePropertyRegistration::class)
            assertThat(householdsPage.provideThisLaterButton).isVisible()

            householdsPage.submitProvideThisLater()
            val provideTenancyDetailsLaterPage =
                assertPageIs(page, ProvideTenancyDetailsLaterFormPagePropertyRegistration::class)

            // Provide Tenancy Details Later - render page
            assertThat(provideTenancyDetailsLaterPage.sectionHeader).containsText("Tenancy details")
            assertThat(provideTenancyDetailsLaterPage.heading).containsText("Provide tenancy details later")
            assertThat(provideTenancyDetailsLaterPage.saveAndContinueButton).containsText("Save and continue")
        }

        @Test
        fun `Occupied journey reaches check answers after landlord has chosen to provide this later`(page: Page) {
            val provideTenancyDetailsLaterPage = navigator.skipToTenancyDetailsProvideTenancyDetailsLaterPage()
            assertThat(provideTenancyDetailsLaterPage.sectionHeader).containsText("Tenancy details")
            assertThat(provideTenancyDetailsLaterPage.heading).containsText("Provide tenancy details later")
            provideTenancyDetailsLaterPage.form.submit()

            // Check Your Answers - render page
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            assertThat(checkAnswersPage.heading).containsText("Check your answers")
            assertThat(checkAnswersPage.restructuredTenancyHeading).containsText("Tenancy details")
            assertThat(checkAnswersPage.tenancyHeading).isHidden()
            assertEquals(listOf("Tenancy details"), checkAnswersPage.restructuredTenancyRowHeadings())
            assertThat(checkAnswersPage.summaryList.tenancyDetailsRow.value).containsText("Provide this later")
        }

        @Test
        fun `Task list shows Tenancy detail task as complete after landlord has chosen to provide this later`() {
            val provideTenancyDetailsLaterPage = navigator.skipToTenancyDetailsProvideTenancyDetailsLaterPage()
            provideTenancyDetailsLaterPage.form.submit()

            val taskListPage = navigator.goToPropertyRegistrationTaskList()

            assertEquals("Completed", taskListPage.getRentedOutTask("Tenancy details").statusText.trim())

            val checkAndSubmitTask = taskListPage.getSubmitYourRegistrationTask("Check and submit your answers")
            assertEquals("Not\u00A0started", checkAndSubmitTask.statusText.trim())
            assertTrue(checkAndSubmitTask.hasLink)
        }

        @Test
        fun `CYA hides tenancy details section after landlord has chosen to provide tenancy details later`(page: Page) {
            val provideTenancyDetailsLaterPage = navigator.skipToTenancyDetailsProvideTenancyDetailsLaterPage()
            provideTenancyDetailsLaterPage.form.submit()

            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            assertThat(checkAnswersPage.restructuredTenancyHeading).containsText("Tenancy details")
            assertThat(checkAnswersPage.tenancyHeading).isHidden()

            checkAnswersPage.summaryList.tenancyDetailsRow.actions
                .getActionLink("Change")
                .clickAndWait()
            assertPageIs(page, NumberOfHouseholdsFormPagePropertyRegistration::class)
        }

        @Test
        fun `Changing tenancy details from CYA and selecting provide this later returns to CYA`(page: Page) {
            val provideTenancyDetailsLaterPage = navigator.skipToTenancyDetailsProvideTenancyDetailsLaterPage()
            provideTenancyDetailsLaterPage.form.submit()
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            checkAnswersPage.summaryList.tenancyDetailsRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val householdsPage = assertPageIs(page, NumberOfHouseholdsFormPagePropertyRegistration::class)
            householdsPage.submitProvideThisLater()

            val provideTenancyDetailsLaterPageAgain =
                assertPageIs(page, ProvideTenancyDetailsLaterFormPagePropertyRegistration::class)
            provideTenancyDetailsLaterPageAgain.form.submit()

            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `Abandoning tenancy details mid journey shows tenancy details as Completed on task list`(page: Page) {
            val provideTenancyDetailsLaterPage = navigator.skipToTenancyDetailsProvideTenancyDetailsLaterPage()
            provideTenancyDetailsLaterPage.form.submit()
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            checkAnswersPage.summaryList.tenancyDetailsRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val householdsPage = assertPageIs(page, NumberOfHouseholdsFormPagePropertyRegistration::class)
            householdsPage.submitNumberOfHouseholds(1)

            val peoplePage = assertPageIs(page, NumberOfPeopleFormPagePropertyRegistration::class)
            peoplePage.submitNumOfPeople(2)

            assertPageIs(page, RentIncludesBillsFormPagePropertyRegistration::class)

            val taskListPage = navigator.goToPropertyRegistrationTaskList()
            assertEquals("Completed", taskListPage.getRentedOutTask("Tenancy details").statusText.trim())
        }

        @Test
        fun `Changing from provide tenancy details later to providing households and people routes through whole tenancy journey to CYA`(
            page: Page,
        ) {
            val provideTenancyDetailsLaterPage = navigator.skipToTenancyDetailsProvideTenancyDetailsLaterPage()
            provideTenancyDetailsLaterPage.form.submit()
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            checkAnswersPage.summaryList.tenancyDetailsRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val householdsPage = assertPageIs(page, NumberOfHouseholdsFormPagePropertyRegistration::class)
            householdsPage.submitNumberOfHouseholds(1)

            val peoplePage = assertPageIs(page, NumberOfPeopleFormPagePropertyRegistration::class)
            peoplePage.submitNumOfPeople(2)

            val rentIncludesBillsPage = assertPageIs(page, RentIncludesBillsFormPagePropertyRegistration::class)
            rentIncludesBillsPage.submitIsNotIncluded()

            val furnishedPage = assertPageIs(page, FurnishedStatusFormPagePropertyRegistration::class)
            furnishedPage.submitFurnishedStatus(FurnishedStatus.FURNISHED)

            val rentFrequencyPage = assertPageIs(page, RentFrequencyFormPagePropertyRegistration::class)
            rentFrequencyPage.selectRentFrequency(RentFrequency.MONTHLY)
            rentFrequencyPage.form.submit()

            val rentAmountPage = assertPageIs(page, RentAmountFormPagePropertyRegistration::class)
            rentAmountPage.submitRentAmount("400")

            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `Completing tenancy details after changing from provide later marks tenancy details task as completed on task list`(
            page: Page,
        ) {
            val provideTenancyDetailsLaterPage = navigator.skipToTenancyDetailsProvideTenancyDetailsLaterPage()
            provideTenancyDetailsLaterPage.form.submit()
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            checkAnswersPage.summaryList.tenancyDetailsRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val householdsPage = assertPageIs(page, NumberOfHouseholdsFormPagePropertyRegistration::class)
            householdsPage.submitNumberOfHouseholds(1)

            val peoplePage = assertPageIs(page, NumberOfPeopleFormPagePropertyRegistration::class)
            peoplePage.submitNumOfPeople(2)

            val rentIncludesBillsPage = assertPageIs(page, RentIncludesBillsFormPagePropertyRegistration::class)
            rentIncludesBillsPage.submitIsNotIncluded()

            val furnishedPage = assertPageIs(page, FurnishedStatusFormPagePropertyRegistration::class)
            furnishedPage.submitFurnishedStatus(FurnishedStatus.FURNISHED)

            val rentFrequencyPage = assertPageIs(page, RentFrequencyFormPagePropertyRegistration::class)
            rentFrequencyPage.selectRentFrequency(RentFrequency.MONTHLY)
            rentFrequencyPage.form.submit()

            val rentAmountPage = assertPageIs(page, RentAmountFormPagePropertyRegistration::class)
            rentAmountPage.submitRentAmount("400")

            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            val taskListPage = navigator.goToPropertyRegistrationTaskList()
            assertEquals("Completed", taskListPage.getRentedOutTask("Tenancy details").statusText.trim())
        }

        @Test
        fun `restructured task list shows grouping tasks as cannot start yet until unlocked on a new journey`(page: Page) {
            val registerPropertyStartPage = navigator.goToPropertyRegistrationStartPage()
            registerPropertyStartPage.startButton.clickAndWait()
            val taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)

            val propertyDetailsTask = taskListPage.getAboutYourPropertyTask("Property details")
            assertEquals("Not\u00A0started", propertyDetailsTask.statusText.trim())
            assertTrue(propertyDetailsTask.hasLink)

            val ownershipTask = taskListPage.getAboutYourPropertyTask("Ownership and landlords")
            assertEquals("Cannot\u00A0start\u00A0yet", ownershipTask.statusText.trim())
            assertFalse(ownershipTask.hasLink)

            assertEquals(
                "Cannot\u00A0start\u00A0yet",
                taskListPage.getAboutYourPropertyTask("Tell us if your property’s occupied").statusText.trim(),
            )
            assertEquals(
                "Cannot\u00A0start\u00A0yet",
                taskListPage.getRentedOutTask("Tell us if your property needs a license").statusText.trim(),
            )
            assertEquals("Cannot\u00A0start\u00A0yet", taskListPage.getRentedOutTask("Gas safety certificate").statusText.trim())
            assertEquals("Cannot\u00A0start\u00A0yet", taskListPage.getRentedOutTask("Tenancy details").statusText.trim())
            assertEquals(
                "Cannot\u00A0start\u00A0yet",
                taskListPage.getSubmitYourRegistrationTask("Check and submit your answers").statusText.trim(),
            )
        }

        @Test
        fun `restructured task list shows a grouping task as in progress when it is partially completed`(page: Page) {
            // The address and property type have been answered, but not the number of bedrooms, so the "Property details"
            // grouping task (which now contains all three) is partway through.
            val taskListPage =
                navigator.goToRestructuredPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder.beforePropertyRegistrationOwnershipType(),
                )

            assertEquals("In progress", taskListPage.getAboutYourPropertyTask("Property details").statusText.trim())
            assertEquals(
                "Cannot\u00A0start\u00A0yet",
                taskListPage.getAboutYourPropertyTask("Ownership and landlords").statusText.trim(),
            )
            assertEquals(
                "Cannot\u00A0start\u00A0yet",
                taskListPage.getAboutYourPropertyTask("Tell us if your property’s occupied").statusText.trim(),
            )
            assertEquals("Cannot\u00A0start\u00A0yet", taskListPage.getRentedOutTask("Gas safety certificate").statusText.trim())
            assertEquals(
                "Cannot\u00A0start\u00A0yet",
                taskListPage.getSubmitYourRegistrationTask("Check and submit your answers").statusText.trim(),
            )
        }

        @Test
        fun `restructured task list shows grouping tasks as complete when their answers are provided`(page: Page) {
            navigator.skipToPropertyRegistrationCheckAnswersPageOccupied()
            val taskListPage = navigator.goToPropertyRegistrationTaskList()

            assertEquals("Completed", taskListPage.getAboutYourPropertyTask("Property details").statusText.trim())
            assertEquals(
                "Completed",
                taskListPage.getAboutYourPropertyTask("Ownership and landlords").statusText.trim(),
            )
            assertEquals(
                "Completed",
                taskListPage.getAboutYourPropertyTask("Tell us if your property’s occupied").statusText.trim(),
            )
            assertEquals("Completed", taskListPage.getRentedOutTask("Tenancy details").statusText.trim())

            val checkAndSubmitTask = taskListPage.getSubmitYourRegistrationTask("Check and submit your answers")
            assertEquals("Not\u00A0started", checkAndSubmitTask.statusText.trim())
            assertTrue(checkAndSubmitTask.hasLink)
        }

        @Test
        fun `restructured occupied journey completes full flow and shows answers on check answers`(page: Page) {
            featureFlagManager.enableFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)

            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageOccupied()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `numeric values with leading zeros are displayed without leading zeros on the CYA page`(page: Page) {
            val checkAnswersPage =
                navigator.skipToPropertyRegistrationCheckAnswersPageOccupied(
                    households = 2,
                    people = 4,
                    bedrooms = 3,
                    rentAmount = "0000000.1",
                )

            assertThat(checkAnswersPage.summaryList.rentAmountRow.value).containsText("£0.1")
            assertThat(checkAnswersPage.summaryList.numberOfHouseholdsRow.value).containsText("2")
            assertThat(checkAnswersPage.summaryList.numberOfTenantsRow.value).containsText("4")
            assertThat(checkAnswersPage.summaryList.numberOfBedroomsRow.value).containsText("3")
        }

        @Test
        fun `CYA does not show Which bills are included row when rent does not include bills`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageOccupied(billsIncluded = false)

            assertFalse(checkAnswersPage.restructuredTenancyRowHeadings().contains("Which bills are included"))
        }

        @Test
        fun `Changing number of households from CYA only goes through households and people then returns to CYA`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageOccupied()

            checkAnswersPage.summaryList.numberOfHouseholdsRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val householdsPage = assertPageIs(page, NumberOfHouseholdsFormPagePropertyRegistration::class)
            householdsPage.submitNumberOfHouseholds(3)

            val peoplePage = assertPageIs(page, NumberOfPeopleFormPagePropertyRegistration::class)
            peoplePage.submitNumOfPeople(5)

            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `CYA joint landlords row shows a change link to the check joint landlords page when landlords are invited`(page: Page) {
            val taskListPage =
                navigator.goToRestructuredPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswersOccupied()
                        .withCheckedJointLandlords(mutableListOf("email@address.com")),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Check and submit your answers")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            val changeLink =
                checkAnswersPage.summaryList.jointLandlordsInvitationsRow.actions
                    .getActionLink("Change")
            assertThat(changeLink).isVisible()

            changeLink.clickAndWait()
            val checkJointLandlordsPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            assertThat(checkJointLandlordsPage.summaryList.firstRow.value).containsText("email@address.com")
        }

        @Test
        fun `CYA joint landlords row shows a change link to the has joint landlords page when there are no joint landlords`(page: Page) {
            val taskListPage =
                navigator.goToRestructuredPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckAnswersOccupied(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Check and submit your answers")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            val changeLink =
                checkAnswersPage.summaryList.jointLandlordsAreThereRow.actions
                    .getActionLink("Change")
            assertThat(changeLink).isVisible()

            changeLink.clickAndWait()
            assertPageIs(page, HasJointLandlordsFormBasePagePropertyRegistration::class)
        }

        @Test
        fun `a landlord cannot invite themselves as a joint landlord`(page: Page) {
            val inviteJointLandlordPage = navigator.skipToPropertyRegistrationInviteJointLandlordPage()

            inviteJointLandlordPage.submitEmail("alex.surname@example.com")

            val inviteJointLandlordPageWithError =
                assertPageIs(page, InviteJointLandlordFormPagePropertyRegistration::class)
            assertThat(inviteJointLandlordPageWithError.form.getErrorMessage())
                .containsText("You cannot invite yourself as a joint landlord")

            inviteJointLandlordPageWithError.submitEmail("someone.else@example.com")
            assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
        }

        @Test
        @Suppress("ktlint:standard:max-line-length")
        fun `details can be delegated to a letting agent for an occupied property`(page: Page) {
            val taskListPage =
                navigator.goToRestructuredPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder.beforePropertyRegistrationRestructuredOccupancy().withOccupancyStatus(true),
                )

            taskListPage.clickRentedOutTaskWithName("Who will provide these details")
            val whoProvidesRentalDetailsPage = assertPageIs(page, WhoProvidesRentalDetailsFormPagePropertyRegistration::class)

            whoProvidesRentalDetailsPage.submitLettingAgentProvidesDetails()

            val lettingAgentEmailPage = assertPageIs(page, LettingAgentEmailPagePropertyRegistration::class)
            lettingAgentEmailPage.submitEmail("agent@example.com")

            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        // TODO PDJB-1022: Remove this nested class when the DELEGATE_TO_LETTING_AGENT feature flag is removed
        @Nested
        inner class DelegateToLettingAgentDisabled {
            @BeforeEach
            fun disableDelegateToLettingAgentFlag() {
                featureFlagManager.disableFeature(DELEGATE_TO_LETTING_AGENT)
            }

            @Test
            fun `occupied journey routes straight from occupancy to licensing without asking who provides the details`(page: Page) {
                val occupancyPage = navigator.skipToPropertyRegistrationRestructuredOccupancyPage()
                occupancyPage.submitIsOccupied()

                assertPageIs(page, LicensingTypeFormPagePropertyRegistration::class)
            }

            @Test
            fun `who provides details task is absent from the rented out section`() {
                val taskListPage =
                    navigator.goToRestructuredPropertyRegistrationTaskList(
                        PropertyStateSessionBuilder.beforePropertyRegistrationRestructuredOccupancy().withOccupancyStatus(true),
                    )

                assertFalse(taskListPage.getRentedOutTaskNames().contains("Who will provide these details"))
            }
        }
    }

    companion object {
        val validGasSafetyCertIssueDate =
            DateTimeHelper()
                .getCurrentDateInUK()
                .minus(DatePeriod(years = GAS_SAFETY_CERT_VALIDITY_YEARS))
                .plus(DatePeriod(days = 5))

        val expiredGasSafetyCertIssueDate =
            DateTimeHelper()
                .getCurrentDateInUK()
                .minus(DatePeriod(years = GAS_SAFETY_CERT_VALIDITY_YEARS, days = 5))

        val validExpiryDate =
            DateTimeHelper()
                .getCurrentDateInUK()
                .plus(DatePeriod(days = 5))

        val expiredExpiryDate =
            DateTimeHelper()
                .getCurrentDateInUK()
                .minus(DatePeriod(days = 5))

        val uprnForSelectedAddress = 1L // This matches the uprn in data-local.sql for address 1 Fictional Road, FA1 1AA
    }

    // TODO PDJB-1340: Remove tests when the PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING Feature Flag is removed
    @Nested
    inner class RestructureAndSkippingDisabled {
        private val propertyRegistrationSectionHeader = "Section 1 of 2 — Add property details"

        @BeforeEach
        fun disableRestructureAndSkippingFlag() {
            featureFlagManager.disableFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)
        }

        @Test
        @Suppress("ktlint:standard:max-line-length")
        fun `User can navigate the whole journey if pages are correctly filled in (select address, non-custom property type, selective license, occupied, compliance certificates uploaded)`(
            page: Page,
        ) {
            // Start page (not a journey step, but it is how the user accesses the journey)
            val registerPropertyStartPage = navigator.goToPropertyRegistrationStartPage()
            assertThat(registerPropertyStartPage.heading).containsText("Register a property")
            registerPropertyStartPage.startButton.clickAndWait()
            val taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // Task list page (part of the journey to support redirects)
            taskListPage.clickRegisterTaskWithName("Property address")
            val addressLookupPage = assertPageIs(page, LookupAddressFormPagePropertyRegistration::class)

            // Address lookup - render page
            assertThat(addressLookupPage.form.fieldsetHeading).containsText("What is the property address?")
            assertThat(addressLookupPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            addressLookupPage.submitPostcodeAndBuildingNameOrNumber("FA1 1AA", "1")
            val selectAddressPage = assertPageIs(page, SelectAddressFormPagePropertyRegistration::class)

            // Select address - render page
            assertThat(selectAddressPage.form.fieldsetHeading).containsText("Select your address")
            assertThat(selectAddressPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            selectAddressPage.selectAddressAndSubmit("1 Fictional Road, FA1 1AA")
            val propertyTypePage = assertPageIs(page, PropertyTypeFormPagePropertyRegistration::class)

            // Verify incomplete property is created at this point
            verify(incompletePropertiesRepository).save<LandlordIncompleteProperties>(any())

            // Property type selection - render page
            assertThat(propertyTypePage.form.fieldsetHeading).containsText("What type of property are you registering?")
            assertThat(propertyTypePage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            propertyTypePage.submitPropertyType(PropertyType.DETACHED_HOUSE)
            val ownershipTypePage = assertPageIs(page, OwnershipTypeFormPagePropertyRegistration::class)

            // Ownership type selection - render page
            assertThat(ownershipTypePage.form.fieldsetHeading).containsText("Select the type of ownership you have for your property")
            assertThat(ownershipTypePage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            ownershipTypePage.submitOwnershipType(OwnershipType.FREEHOLD)
            val licensingTypePage = assertPageIs(page, LicensingTypeFormPagePropertyRegistration::class)

            // Licensing type - render page
            assertThat(licensingTypePage.form.fieldsetHeading).containsText("Select the type of licence you have for your property")
            assertThat(licensingTypePage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            licensingTypePage.submitLicensingType(LicensingType.SELECTIVE_LICENCE)
            val selectiveLicencePage = assertPageIs(page, SelectiveLicenceFormPagePropertyRegistration::class)

            // Selective licence - render page
            assertThat(selectiveLicencePage.form.fieldsetHeading).containsText("What is your selective licence number?")
            assertThat(selectiveLicencePage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            selectiveLicencePage.submitLicenseNumber("licence number")
            val occupancyPage = assertPageIs(page, OccupancyFormPagePropertyRegistration::class)

            // Occupancy - render page
            assertThat(occupancyPage.form.fieldsetHeading).containsText("Is your property occupied by tenants?")
            assertThat(occupancyPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            occupancyPage.submitIsOccupied()
            val householdsPage = assertPageIs(page, NumberOfHouseholdsFormPagePropertyRegistration::class)

            // Number of households - render page
            assertThat(householdsPage.header).containsText("Households in your property")
            assertThat(householdsPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(householdsPage.provideThisLaterButton).isHidden()
            // fill in and submit
            householdsPage.submitNumberOfHouseholds(2)
            val peoplePage = assertPageIs(page, NumberOfPeopleFormPagePropertyRegistration::class)

            // Number of people - render page
            assertThat(peoplePage.header).containsText("How many people live in your property?")
            assertThat(peoplePage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            peoplePage.submitNumOfPeople(2)
            val bedroomsPage = assertPageIs(page, NumberOfBedroomsFormPagePropertyRegistration::class)

            // Number of bedrooms - render page
            assertThat(bedroomsPage.header).containsText("How many bedrooms in your property?")
            assertThat(bedroomsPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            bedroomsPage.submitNumOfBedrooms(3)
            val rentIncludesBillsPage = assertPageIs(page, RentIncludesBillsFormPagePropertyRegistration::class)

            // Does the rent include bills - render page
            assertThat(rentIncludesBillsPage.form.fieldsetHeading).containsText("Does the rent include bills?")
            assertThat(rentIncludesBillsPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            rentIncludesBillsPage.submitIsIncluded()
            val billsIncludedPage = assertPageIs(page, BillsIncludedFormPagePropertyRegistration::class)

            // Bills included - render page
            assertThat(billsIncludedPage.form.fieldsetHeading).containsText("Which of these do you include in the rent?")
            assertThat(billsIncludedPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            billsIncludedPage.selectGasElectricityWater()
            billsIncludedPage.selectSomethingElseCheckbox()
            billsIncludedPage.fillCustomBills("Dog Grooming")
            billsIncludedPage.form.submit()
            val furnishedPage = assertPageIs(page, FurnishedStatusFormPagePropertyRegistration::class)

            // Furnished - render page
            assertThat(furnishedPage.form.fieldsetHeading).containsText("Is the property furnished?")
            assertThat(furnishedPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            furnishedPage.submitFurnishedStatus(FurnishedStatus.FURNISHED)
            val rentFrequencyPage = assertPageIs(page, RentFrequencyFormPagePropertyRegistration::class)

            // Rent frequency - render page
            assertThat(rentFrequencyPage.header).containsText("When you charge rent")
            assertThat(rentFrequencyPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            rentFrequencyPage.selectRentFrequency(RentFrequency.OTHER)
            rentFrequencyPage.fillCustomRentFrequency("Fortnightly")
            rentFrequencyPage.form.submit()
            val rentAmountPage = assertPageIs(page, RentAmountFormPagePropertyRegistration::class)

            // Rent amount - render page
            assertThat(rentAmountPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            rentAmountPage.submitRentAmount("400")
            val hasJointLandlordsPage = assertPageIs(page, HasJointLandlordsFormBasePagePropertyRegistration::class)

            // Has Joint Landlords - render page
            assertThat(hasJointLandlordsPage.header).containsText("Invite joint landlords")
            assertThat(hasJointLandlordsPage.sectionHeader).containsText(propertyRegistrationSectionHeader)

            // fill in and submit
            hasJointLandlordsPage.submitHasJointLandlords()
            val inviteJointLandlordPage = assertPageIs(page, InviteJointLandlordFormPagePropertyRegistration::class)

            // Invite joint landlord - render page
            assertThat(inviteJointLandlordPage.heading).containsText("Invite a joint landlord to this property")
            assertThat(inviteJointLandlordPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)

            // fill in and submit
            inviteJointLandlordPage.submitEmail("email@address.com")
            var checkJointLandlordsPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            assertThat(checkJointLandlordsPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(checkJointLandlordsPage.summaryList.firstRow.value).containsText("email@address.com")

            // Check joint landlords - render page
            checkJointLandlordsPage
                .form
                .addAnotherButton
                .clickAndWait()

            // Invite another joint landlord - render page
            val addAnotherPage = assertPageIs(page, InviteAnotherJointLandlordFormPagePropertyRegistration::class)
            assertThat(addAnotherPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            addAnotherPage.submitEmail("email2@address.com")

            checkJointLandlordsPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            checkJointLandlordsPage.summaryList.firstRow.clickNamedActionLinkAndWait("Remove")

            // Remove Joint Landlord - render page
            val removeJointLandlordsPage =
                assertPageIs(page, RemoveJointLandlordAreYouSureFormPagePropertyRegistration::class)
            removeJointLandlordsPage.submitWantsToProceed()

            checkJointLandlordsPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            checkJointLandlordsPage.form.submit()

            val hasGasSupplyPage = assertPageIs(page, HasGasSupplyFormPagePropertyRegistration::class)

            // Has Gas Supply - render page
            assertThat(hasGasSupplyPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(hasGasSupplyPage.heading).containsText("Does the property have a gas supply or any gas appliances?")
            hasGasSupplyPage.submitHasGasSupply()
            val hasGasCertPage = assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)

            // Has Gas Cert - render page
            assertThat(hasGasCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(hasGasCertPage.heading).containsText("Do you have a gas safety certificate for this property?")
            hasGasCertPage.submitHasCertificate()
            val gasCertIssueDatePage = assertPageIs(page, GasCertIssueDateFormPagePropertyRegistration::class)

            // Gas Cert Issue Date - render page
            assertThat(gasCertIssueDatePage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(gasCertIssueDatePage.heading).containsText("What’s the issue date on the gas safety certificate?")
            gasCertIssueDatePage.submitDate(validGasSafetyCertIssueDate)
            var uploadGasCertPage = assertPageIs(page, UploadGasCertFormPagePropertyRegistration::class)

            // Upload Gas Cert - render page
            assertThat(uploadGasCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            uploadGasCertPage.uploadGasCertificate(Path.of("src/test/resources/test-files/blank.png"))
            var checkGasCertUploadsPage = assertPageIs(page, CheckGasCertUploadsFormPagePropertyRegistration::class)

            // Check Gas Cert Uploads - render page
            assertThat(checkGasCertUploadsPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(checkGasCertUploadsPage.heading).containsText("You’ve uploaded 1 file")
            assertThat(checkGasCertUploadsPage.table.getCell(0, 0)).containsText("blank.png")
            assertEquals(checkGasCertUploadsPage.table.rows.count(), 1)
            checkGasCertUploadsPage.form.addAnotherButton.clickAndWait()
            uploadGasCertPage = assertPageIs(page, UploadGasCertFormPagePropertyRegistration::class)

            uploadGasCertPage.uploadGasCertificate(Path.of("src/test/resources/test-files/blank.png"))
            checkGasCertUploadsPage = assertPageIs(page, CheckGasCertUploadsFormPagePropertyRegistration::class)

            assertThat(checkGasCertUploadsPage.heading).containsText("You’ve uploaded 2 files")
            assertThat(checkGasCertUploadsPage.table.getCell(0, 0)).containsText("blank.png")
            assertThat(checkGasCertUploadsPage.table.getCell(1, 0)).containsText("blank.png")
            assertEquals(checkGasCertUploadsPage.table.rows.count(), 2)

            checkGasCertUploadsPage.table
                .getClickableCell(0, 2)
                .link
                .clickAndWait()

            val removeGasCertUploadPage = assertPageIs(page, RemoveGasCertUploadFormPagePropertyRegistration::class)

            removeGasCertUploadPage.form.radios.selectValue("true")
            removeGasCertUploadPage.form.submit()

            checkGasCertUploadsPage = assertPageIs(page, CheckGasCertUploadsFormPagePropertyRegistration::class)
            assertThat(checkGasCertUploadsPage.table.getCell(0, 0)).containsText("blank.png")

            assertEquals(checkGasCertUploadsPage.table.rows.count(), 1)
            checkGasCertUploadsPage.form.submit()

            // Remove Gas Cert Upload - render page
            val checkGasSafetyAnswersPage = assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)

            // Check Gas Safety Answers - render page
            assertThat(checkGasSafetyAnswersPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(checkGasSafetyAnswersPage.heading).containsText("Gas safety certificate")
            checkGasSafetyAnswersPage.form.submit()
            val taskListPageAfterGasSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterGasSafety.clickRegisterTaskWithName("Electrical safety certificate")
            val hasElectricalCertPage = assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)

            // Has Electrical Cert - render page
            assertThat(hasElectricalCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(hasElectricalCertPage.heading).containsText("Which electrical safety certificate do you have for this property?")
            hasElectricalCertPage.submitHasEic()
            val electricalCertExpiryDatePage =
                assertPageIs(page, ElectricalCertExpiryDateFormPagePropertyRegistration::class)

            // Electrical Cert Expiry Date - render page
            assertThat(electricalCertExpiryDatePage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(
                electricalCertExpiryDatePage.heading,
            ).containsText("What’s the expiry date on the Electrical Installation Certificate?")
            electricalCertExpiryDatePage.submitDate(validExpiryDate)
            var uploadElectricalCertPage = assertPageIs(page, UploadElectricalCertFormPagePropertyRegistration::class)

            // Upload Electrical Cert - render page
            assertThat(uploadElectricalCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            uploadElectricalCertPage.uploadElectricalCertificate(Path.of("src/test/resources/test-files/blank.png"))
            var checkElectricalCertUploadsPage =
                assertPageIs(page, CheckElectricalCertUploadsFormPagePropertyRegistration::class)

            // Check Electrical Cert Uploads - render page
            assertThat(checkElectricalCertUploadsPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(checkElectricalCertUploadsPage.table.getCell(0, 0)).containsText("blank.png")
            assertEquals(checkElectricalCertUploadsPage.table.rows.count(), 1)
            checkElectricalCertUploadsPage.form.addAnotherButton.clickAndWait()
            uploadElectricalCertPage = assertPageIs(page, UploadElectricalCertFormPagePropertyRegistration::class)

            uploadElectricalCertPage.uploadElectricalCertificate(Path.of("src/test/resources/test-files/blank.png"))
            checkElectricalCertUploadsPage =
                assertPageIs(page, CheckElectricalCertUploadsFormPagePropertyRegistration::class)
            assertThat(checkElectricalCertUploadsPage.table.getCell(0, 0)).containsText("blank.png")
            assertThat(checkElectricalCertUploadsPage.table.getCell(1, 0)).containsText("blank.png")
            assertEquals(checkElectricalCertUploadsPage.table.rows.count(), 2)

            checkElectricalCertUploadsPage.table
                .getClickableCell(0, 2)
                .link
                .clickAndWait()

            val removeElectricalCertUploadPage =
                assertPageIs(page, RemoveElectricalCertUploadFormPagePropertyRegistration::class)

            removeElectricalCertUploadPage.form.radios.selectValue("true")
            removeElectricalCertUploadPage.form.submit()

            checkElectricalCertUploadsPage =
                assertPageIs(page, CheckElectricalCertUploadsFormPagePropertyRegistration::class)
            assertThat(checkElectricalCertUploadsPage.table.getCell(0, 0)).containsText("blank.png")

            assertEquals(checkElectricalCertUploadsPage.table.rows.count(), 1)
            checkElectricalCertUploadsPage.form.submit()
            val checkElectricalSafetyAnswersPage =
                assertPageIs(page, CheckElectricalSafetyAnswersFormPagePropertyRegistration::class)

            // Setup EpcLookupByUprnStep being able to find an EPC for this property when the next step submits
            whenever(epcRegisterClient.getByUprn(uprnForSelectedAddress))
                .thenReturn(
                    MockEpcData.createEpcRegisterClientEpcFoundResponse(
                        expiryDate = validExpiryDate,
                    ),
                )

            // Check Electrical Safety Answers - render page
            assertThat(checkElectricalSafetyAnswersPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(checkElectricalSafetyAnswersPage.heading).containsText("Electrical safety certificate")
            checkElectricalSafetyAnswersPage.form.submit()
            val taskListPageAfterElectricalSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // EpcLookupByUprnStep finds the EPC, so redirects to Check UPRN matched EPC
            taskListPageAfterElectricalSafety.clickRegisterTaskWithName("Energy performance certificate (EPC)")
            val confirmUprnMatchedEpcDetailsPage =
                assertPageIs(page, ConfirmEpcDetailsRetrievedByUprnFormPagePropertyRegistration::class)

            // Confirm UPRN matched EPC - submit No (don't use this EPC)
            assertThat(confirmUprnMatchedEpcDetailsPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            confirmUprnMatchedEpcDetailsPage.submitNo()
            val hasEpcPage = assertPageIs(page, HasEpcFormPagePropertyRegistration::class)

            // Has EPC - render page
            assertThat(hasEpcPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasEpcPage.submitHasEpc()
            val findYourEpcPage = assertPageIs(page, FindYourEpcFormPagePropertyRegistration::class)

            // EPC Search - render page
            assertThat(findYourEpcPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            whenever(epcRegisterClient.getByRrn(CURRENT_EPC_CERTIFICATE_NUMBER))
                .thenReturn(
                    MockEpcData.createEpcRegisterClientEpcFoundResponse(
                        certificateNumber = CURRENT_EPC_CERTIFICATE_NUMBER,
                        latestCertificateNumberForThisProperty = CURRENT_EPC_CERTIFICATE_NUMBER,
                        expiryDate = validExpiryDate,
                    ),
                )
            findYourEpcPage.submitCurrentEpcNumber()
            val confirmEpcDetailsPage =
                assertPageIs(page, ConfirmEpcDetailsRetrievedByCertificateNumberPagePropertyRegistration::class)

            // Check Matched EPC - render page
            assertThat(confirmEpcDetailsPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            val expectedExpiryDate =
                validExpiryDate
                    .toJavaLocalDate()
                    .format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
            assertThat(confirmEpcDetailsPage.summaryCard.summaryList.addressRow.value).containsText(MockEpcData.defaultSingleLineAddress)
            assertThat(confirmEpcDetailsPage.summaryCard.summaryList.energyEfficiencyRatingRow.value).containsText("C")
            assertThat(confirmEpcDetailsPage.summaryCard.summaryList.expiryDateRow.value).containsText(
                expectedExpiryDate,
            )
            assertThat(
                confirmEpcDetailsPage.summaryCard.summaryList.certificateNumberRow.value,
            ).containsText(CURRENT_EPC_CERTIFICATE_NUMBER)
            confirmEpcDetailsPage.submitYes()
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            val taskListPageAfterEpc = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterEpc.clickCheckAndSubmitTaskWithName("Check and submit your answers")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            // Check answers - render page
            assertThat(checkAnswersPage.heading).containsText("Check your answers for:")
            assertThat(checkAnswersPage.sectionHeader).containsText("Section 2 of 2 — Check and submit your property details")
            assertThat(checkAnswersPage.tenancyHeading).isVisible()
            assertThat(checkAnswersPage.restructuredTenancyHeading).isHidden()
            assertThat(checkAnswersPage.occupancyHeading).isHidden()
            assertThat(checkAnswersPage.summaryList.occupancyQuestionRow).isHidden()
            assertThat(checkAnswersPage.summaryList.occupiedByTenantsRow.key).containsText("Occupied by tenants")
            assertThat(checkAnswersPage.complianceCertificatesHeading).isVisible()
            assertThat(checkAnswersPage.gasSafetyHeading).isVisible()
            assertThat(checkAnswersPage.electricalSafetyHeading).isVisible()
            assertThat(checkAnswersPage.epcHeading).isVisible()
            // submit
            checkAnswersPage.confirm()
            val confirmationPage = assertPageIs(page, ConfirmationPagePropertyRegistration::class)

            // Confirmation - render page
            val propertyOwnershipCaptor = captor<PropertyOwnership>()
            verify(propertyOwnershipRepository).save(propertyOwnershipCaptor.capture())
            val expectedPropertyRegNum =
                RegistrationNumberDataModel.fromRegistrationNumber(
                    propertyOwnershipCaptor.value.registrationNumber,
                )
            assertEquals(expectedPropertyRegNum.toString(), confirmationPage.registrationNumberText)
            assertTrue(propertyOwnershipCaptor.value.isOccupied)
            assertFalse(confirmationPage.whatYouNeedToDoNextHeading.isVisible)
            assertTrue(confirmationPage.surveyLink.locator.isVisible)
            assertThat(confirmationPage.surveyLink).hasAttribute("href", INDIVIDUAL_PROPERTY_REGISTRATION_SURVEY_URL)
            assertTrue(confirmationPage.goToDashboardLink.locator.isVisible)

            // Check confirmation email
            verify(confirmationEmailSender).sendEmail(
                "alex.surname@example.com",
                PropertyRegistrationConfirmationEmail(
                    expectedPropertyRegNum.toString(),
                    "1 Fictional Road, FA1 1AA",
                    absoluteLandlordUrl,
                    true,
                    listOf("email2@address.com"),
                ),
            )

            // Go to dashboard
            confirmationPage.goToDashboardLink.clickAndWait()
            assertPageIs(page, LandlordDashboardPage::class)
        }

        @Test
        @Suppress("ktlint:standard:max-line-length")
        fun `User can navigate the whole journey if pages are correctly filled in (manual address, custom property type, no license, unoccupied, no joint landlords, no certificates)`(
            page: Page,
        ) {
            // Start page (not a journey step, but it is how the user accesses the journey)
            val registerPropertyStartPage = navigator.goToPropertyRegistrationStartPage()
            assertThat(registerPropertyStartPage.heading).containsText("Register a property")
            registerPropertyStartPage.startButton.clickAndWait()
            val taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // Task list page (part of the journey to support redirects)
            taskListPage.clickRegisterTaskWithName("Property address")
            val addressLookupPage = assertPageIs(page, LookupAddressFormPagePropertyRegistration::class)

            // Address lookup - render page
            assertThat(addressLookupPage.form.fieldsetHeading).containsText("What is the property address?")
            assertThat(addressLookupPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            addressLookupPage.submitPostcodeAndBuildingNameOrNumber("FA1 1AB", "2")
            val selectAddressPage = assertPageIs(page, SelectAddressFormPagePropertyRegistration::class)

            // Select address - render page
            assertThat(selectAddressPage.form.fieldsetHeading).containsText("Select your address")
            assertThat(selectAddressPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            selectAddressPage.selectAddressAndSubmit(MANUAL_ADDRESS_CHOSEN)
            val manualAddressPage = assertPageIs(page, ManualAddressFormPagePropertyRegistration::class)

            // Manual address - render page
            assertThat(manualAddressPage.form.fieldsetHeading).containsText("What is the property address?")
            assertThat(manualAddressPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            manualAddressPage.submitAddress(
                addressLineOne = "Test address line 1",
                townOrCity = "Testville",
                postcode = "EG1 2AB",
            )
            val selectLocalCouncilPage = assertPageIs(page, SelectLocalCouncilFormPagePropertyRegistration::class)

            // Select local council - render page
            assertThat(selectLocalCouncilPage.form.fieldsetHeading).containsText("What local council area is your property in?")
            assertThat(selectLocalCouncilPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            selectLocalCouncilPage.submitLocalCouncil(
                "BATH AND NORTH EAST SOMERSET COUNCIL",
                "BATH AND NORTH EAST SOMERSET COUNCIL",
            )
            val propertyTypePage = assertPageIs(page, PropertyTypeFormPagePropertyRegistration::class)

            // Property type selection - render page
            assertThat(propertyTypePage.form.fieldsetHeading).containsText("What type of property are you registering?")
            assertThat(propertyTypePage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            propertyTypePage.submitCustomPropertyType("End terrace house")
            val ownershipTypePage = assertPageIs(page, OwnershipTypeFormPagePropertyRegistration::class)

            // Ownership type selection - render page
            assertThat(ownershipTypePage.form.fieldsetHeading).containsText("Select the type of ownership you have for your property")
            assertThat(ownershipTypePage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            ownershipTypePage.submitOwnershipType(OwnershipType.FREEHOLD)
            val licensingTypePage = assertPageIs(page, LicensingTypeFormPagePropertyRegistration::class)

            // Licensing type - render page
            assertThat(licensingTypePage.form.fieldsetHeading).containsText("Select the type of licence you have for your property")
            assertThat(licensingTypePage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            licensingTypePage.submitLicensingType(LicensingType.NO_LICENSING)
            val occupancyPage = assertPageIs(page, OccupancyFormPagePropertyRegistration::class)

            // Occupancy - render page
            assertThat(occupancyPage.form.fieldsetHeading).containsText("Is your property occupied by tenants?")
            assertThat(occupancyPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            // fill in and submit
            occupancyPage.submitIsVacant()
            val hasJointLandlordsPage = assertPageIs(page, HasJointLandlordsFormBasePagePropertyRegistration::class)

            // Has Joint Landlords - render page
            assertThat(hasJointLandlordsPage.header).containsText("Invite joint landlords")
            assertThat(hasJointLandlordsPage.sectionHeader).containsText(propertyRegistrationSectionHeader)

            // fill in and submit
            hasJointLandlordsPage.submitHasNoJointLandlords()
            val hasGasSupplyPage = assertPageIs(page, HasGasSupplyFormPagePropertyRegistration::class)

            // Has Gas Supply - render page
            assertThat(hasGasSupplyPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(hasGasSupplyPage.heading).containsText("Does the property have a gas supply or any gas appliances?")
            hasGasSupplyPage.submitHasGasSupply()
            val hasGasCertPage = assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)

            // Has Gas Cert - render page
            assertThat(hasGasCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(hasGasCertPage.heading).containsText("Do you have a gas safety certificate for this property?")
            hasGasCertPage.submitHasNoCertificate()
            val gasCertMissingPage = assertPageIs(page, GasCertMissingFormPagePropertyRegistration::class)

            // Gas Cert Missing - render page
            assertThat(gasCertMissingPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(gasCertMissingPage.heading).containsText("You must get a gas safety certificate before a tenant moves in")
            assertThat(gasCertMissingPage.warning).isHidden()
            assertThat(gasCertMissingPage.submitButton).containsText("Continue")
            gasCertMissingPage.form.submit()
            val checkGasSafetyAnswersPage = assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)

            // Check Gas Safety Answers - render page
            assertThat(checkGasSafetyAnswersPage.heading).containsText("Gas safety certificate")
            checkGasSafetyAnswersPage.form.submit()
            val taskListPageAfterGasSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterGasSafety.clickRegisterTaskWithName("Electrical safety certificate")
            val hasElectricalCertPage = assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)

            // Has Electrical Cert - render page
            assertThat(hasElectricalCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(hasElectricalCertPage.heading).containsText("Which electrical safety certificate do you have for this property?")
            hasElectricalCertPage.submitHasNoCert()
            val electricalCertMissingPage = assertPageIs(page, ElectricalCertMissingFormPagePropertyRegistration::class)

            // Electrical Cert Missing - render page
            assertThat(electricalCertMissingPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(
                electricalCertMissingPage.heading,
            ).containsText("You must get an electrical safety certificate before a tenant moves in")
            assertThat(electricalCertMissingPage.warning).isHidden()
            assertThat(electricalCertMissingPage.submitButton).containsText("Continue")
            electricalCertMissingPage.form.submit()
            val checkElectricalSafetyAnswersPage =
                assertPageIs(page, CheckElectricalSafetyAnswersFormPagePropertyRegistration::class)

            // Check Electrical Safety Answers - render page
            assertThat(checkElectricalSafetyAnswersPage.heading).containsText("Electrical safety certificate")
            checkElectricalSafetyAnswersPage.form.submit()
            val taskListPageAfterElectricalSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // We use a manual address, uprn will be null.
            // The internal EpcLookupByUprnStep at the start of the EpcTask will not find an EPC
            taskListPageAfterElectricalSafety.clickRegisterTaskWithName("Energy performance certificate (EPC)")
            val hasEpcPage = assertPageIs(page, HasEpcFormPagePropertyRegistration::class)

            // Has EPC - render page
            assertThat(hasEpcPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasEpcPage.submitHasNoEpc()
            val isEpcRequiredPage = assertPageIs(page, IsEpcRequiredFormPagePropertyRegistration::class)

            // Is EPC required - render page
            assertThat(isEpcRequiredPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(isEpcRequiredPage.heading).containsText("Is an EPC required to let this property?")
            isEpcRequiredPage.submitEpcRequired()
            val epcMissingPage = assertPageIs(page, EpcMissingFormPagePropertyRegistration::class)

            // EPC Missing - render page
            assertThat(epcMissingPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(epcMissingPage.heading).containsText("Your property is missing an EPC")
            assertThat(epcMissingPage.continueButton).containsText("Continue")
            assertThat(epcMissingPage.warning).isHidden()
            epcMissingPage.form.submit()
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            val taskListPageAfterEpc = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterEpc.clickCheckAndSubmitTaskWithName("Check and submit your answers")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            // Check answers - render page
            assertThat(checkAnswersPage.heading).containsText("Check your answers for:")
            assertThat(checkAnswersPage.sectionHeader).containsText("Section 2 of 2 — Check and submit your property details")
            // submit
            checkAnswersPage.confirm()
            val confirmationPage = assertPageIs(page, ConfirmationPagePropertyRegistration::class)

            // Confirmation - render page
            val propertyOwnershipCaptor = captor<PropertyOwnership>()
            verify(propertyOwnershipRepository).save(propertyOwnershipCaptor.capture())
            val expectedPropertyRegNum =
                RegistrationNumberDataModel.fromRegistrationNumber(
                    propertyOwnershipCaptor.value.registrationNumber,
                )
            assertEquals(expectedPropertyRegNum.toString(), confirmationPage.registrationNumberText)
            assertFalse(propertyOwnershipCaptor.value.isOccupied)
            assertTrue(confirmationPage.whatYouNeedToDoNextHeading.isHidden)
            assertTrue(confirmationPage.surveyLink.locator.isVisible)
            assertThat(confirmationPage.surveyLink).hasAttribute("href", INDIVIDUAL_PROPERTY_REGISTRATION_SURVEY_URL)
            assertTrue(confirmationPage.goToDashboardLink.locator.isVisible)

            // Check confirmation email
            verify(confirmationEmailSender).sendEmail(
                "alex.surname@example.com",
                PropertyRegistrationConfirmationEmail(
                    expectedPropertyRegNum.toString(),
                    "Test address line 1, Testville, EG1 2AB",
                    absoluteLandlordUrl,
                    false,
                    null,
                ),
            )

            // Go to dashboard
            confirmationPage.goToDashboardLink.clickAndWait()
            assertPageIs(page, LandlordDashboardPage::class)
        }

        @Test
        fun `Changing number of households from CYA does not go through rent and bills and returns to CYA`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageOccupied()

            checkAnswersPage.summaryList.numberOfHouseholdsRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val householdsPage = assertPageIs(page, NumberOfHouseholdsFormPagePropertyRegistration::class)
            householdsPage.submitNumberOfHouseholds(3)

            val peoplePage = assertPageIs(page, NumberOfPeopleFormPagePropertyRegistration::class)
            peoplePage.submitNumOfPeople(5)

            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `User can choose to provide compliance certificates later if their property is occupied`(page: Page) {
            val hasGasSupplyPage = navigator.skipToPropertyRegistrationHasGasSupplyPage(propertyIsOccupied = true)
            assertThat(hasGasSupplyPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasGasSupplyPage.submitHasGasSupply()
            val hasGasCertPage = assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)

            // Has Gas Cert. Submit with no option selected
            assertThat(hasGasCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasGasCertPage.submitProvideThisLater()
            val provideGasCertLaterPage = assertPageIs(page, ProvideGasCertLaterFormPagePropertyRegistration::class)

            // Provide Gas Cert Later - render page
            assertThat(provideGasCertLaterPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(provideGasCertLaterPage.insetText).containsText("You must upload your gas safety certificate within 28 days")
            provideGasCertLaterPage.form.submit()
            val checkGasSafetyAnswersPage = assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)

            // Check Gas Safety Answers - render page
            assertThat(checkGasSafetyAnswersPage.heading).containsText("Gas safety certificate")
            checkGasSafetyAnswersPage.form.submit()
            val taskListPageAfterGasSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterGasSafety.clickRegisterTaskWithName("Electrical safety certificate")
            val hasElectricalCertPage = assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)

            // Has Electrical Cert - render page
            assertThat(hasElectricalCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasElectricalCertPage.submitProvideThisLater()
            val provideElectricalCertLaterPage =
                assertPageIs(page, ProvideElectricalCertLaterFormPagePropertyRegistration::class)

            // Provide Electrical Cert Later - render page
            assertThat(provideElectricalCertLaterPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(
                provideElectricalCertLaterPage.insetText,
            ).containsText("You must upload your electrical safety certificate within 28 days.")
            provideElectricalCertLaterPage.form.submit()
            val checkElectricalSafetyAnswersPage =
                assertPageIs(page, CheckElectricalSafetyAnswersFormPagePropertyRegistration::class)

            // Setup EpcLookupByUprnStep NOT finding an EPC for this property when the next step submits
            whenever(epcRegisterClient.getByUprn(uprnForSelectedAddress)).thenReturn(MockEpcData.epcRegisterClientEpcNotFoundResponse)

            // Check Electrical Safety Answers - render page
            assertThat(checkElectricalSafetyAnswersPage.heading).containsText("Electrical safety certificate")
            checkElectricalSafetyAnswersPage.form.submit()
            val taskListPageAfterElectricalSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // The internal EpcLookupByUprnStep at the start of the EpcTask does not find an EPC
            taskListPageAfterElectricalSafety.clickRegisterTaskWithName("Energy performance certificate (EPC)")
            val hasEpcPage = assertPageIs(page, HasEpcFormPagePropertyRegistration::class)

            // Has EPC - render page
            assertThat(hasEpcPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasEpcPage.submitProvideThisLater()
            val provideEpcLaterPage = assertPageIs(page, ProvideEpcLaterFormPagePropertyRegistration::class)

            // Provide EPC Later - render page
            assertThat(provideEpcLaterPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(provideEpcLaterPage.heading).containsText("Provide your EPC details later")
            assertThat(provideEpcLaterPage.insetText).containsText(
                "To keep the property registered, we need all its compliance certificates within 28 days.",
            )
            provideEpcLaterPage.form.submit()

            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            assertPageIs(page, TaskListPagePropertyRegistration::class)
        }

        @Test
        fun `User can choose to provide compliance certificates later if their property is unoccupied`(page: Page) {
            val hasGasSupplyPage = navigator.skipToPropertyRegistrationHasGasSupplyPage(propertyIsOccupied = false)
            assertThat(hasGasSupplyPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasGasSupplyPage.submitHasGasSupply()
            val hasGasCertPage = assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)

            // Has Gas Cert. Submit with no option selected
            assertThat(hasGasCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasGasCertPage.submitProvideThisLater()
            val provideGasCertLaterPage = assertPageIs(page, ProvideGasCertLaterFormPagePropertyRegistration::class)

            // Provide Gas Cert Later - render page
            assertThat(provideGasCertLaterPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(provideGasCertLaterPage.insetText).isHidden()
            assertTrue(
                provideGasCertLaterPage.page
                    .content()
                    .contains("You must get a gas safety certificate before a tenant moves in."),
            )
            provideGasCertLaterPage.form.submit()
            val checkGasSafetyAnswersPage = assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)

            // Check Gas Safety Answers - render page
            assertThat(checkGasSafetyAnswersPage.heading).containsText("Gas safety certificate")
            checkGasSafetyAnswersPage.form.submit()
            val taskListPageAfterGasSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterGasSafety.clickRegisterTaskWithName("Electrical safety certificate")
            val hasElectricalCertPage = assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)

            // Has Electrical Cert - render page
            assertThat(hasElectricalCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasElectricalCertPage.submitProvideThisLater()
            val provideElectricalCertLaterPage =
                assertPageIs(page, ProvideElectricalCertLaterFormPagePropertyRegistration::class)

            // Provide Electrical Cert Later - render page (unoccupied variant)
            assertThat(provideElectricalCertLaterPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(provideElectricalCertLaterPage.heading).containsText("Provide your electrical safety certificate later")
            assertThat(provideElectricalCertLaterPage.insetText).isHidden()
            assertTrue(
                provideElectricalCertLaterPage.page
                    .content()
                    .contains("You must get an electrical safety certificate before a tenant moves in."),
            )
            provideElectricalCertLaterPage.form.submit()
            val checkElectricalSafetyAnswersPage =
                assertPageIs(page, CheckElectricalSafetyAnswersFormPagePropertyRegistration::class)

            // Setup EpcLookupByUprnStep NOT finding an EPC for this property when the next step submits
            whenever(epcRegisterClient.getByUprn(uprnForSelectedAddress)).thenReturn(MockEpcData.epcRegisterClientEpcNotFoundResponse)

            // Check Electrical Safety Answers - render page
            assertThat(checkElectricalSafetyAnswersPage.heading).containsText("Electrical safety certificate")
            checkElectricalSafetyAnswersPage.form.submit()
            val taskListPageAfterElectricalSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // The internal EpcLookupByUprnStep at the start of the EpcTask does not find an EPC
            taskListPageAfterElectricalSafety.clickRegisterTaskWithName("Energy performance certificate (EPC)")
            val hasEpcPage = assertPageIs(page, HasEpcFormPagePropertyRegistration::class)

            // Has EPC - render page
            assertThat(hasEpcPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasEpcPage.submitProvideThisLater()
            val provideEpcLaterPage = assertPageIs(page, ProvideEpcLaterFormPagePropertyRegistration::class)

            // Provide EPC Later - render page
            assertThat(provideEpcLaterPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(provideEpcLaterPage.heading).containsText("Provide your EPC details later")
            assertThat(provideEpcLaterPage.insetText).isHidden()
            provideEpcLaterPage.form.submit()

            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            assertPageIs(page, TaskListPagePropertyRegistration::class)
        }

        @Test
        fun `User can complete the journey with missing compliance certificates for an occupied property`(page: Page) {
            // Gas supply page
            val hasGasSupplyPage = navigator.skipToPropertyRegistrationHasGasSupplyPage(propertyIsOccupied = true)
            assertThat(hasGasSupplyPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasGasSupplyPage.submitHasGasSupply()
            val hasGasCertPage = assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)

            // Has Gas Cert page
            assertThat(hasGasCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasGasCertPage.submitHasNoCertificate()
            val gasCertMissingPage = assertPageIs(page, GasCertMissingFormPagePropertyRegistration::class)

            // Gas Cert Missing - render page
            assertThat(gasCertMissingPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(gasCertMissingPage.heading).containsText("You must get a valid gas safety certificate for this property")
            assertThat(gasCertMissingPage.submitButton).containsText("Continue without a valid gas safety certificate")
            assertThat(gasCertMissingPage.warning)
                .containsText("You could face prosecution if you have tenants in a property without a gas safety certificate")
            gasCertMissingPage.form.submit()
            val checkGasSafetyAnswersPage = assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)

            // Check Gas Safety Answers - render page
            assertThat(checkGasSafetyAnswersPage.heading).containsText("Gas safety certificate")
            checkGasSafetyAnswersPage.form.submit()
            val taskListPageAfterGasSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterGasSafety.clickRegisterTaskWithName("Electrical safety certificate")
            val hasElectricalCertPage = assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)

            // Has Electrical Cert - render page
            assertThat(hasElectricalCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(hasElectricalCertPage.heading).containsText("Which electrical safety certificate do you have for this property?")
            hasElectricalCertPage.submitHasNoCert()
            val electricalCertMissingPage = assertPageIs(page, ElectricalCertMissingFormPagePropertyRegistration::class)

            assertThat(electricalCertMissingPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(
                electricalCertMissingPage.heading,
            ).containsText("You must get a valid electrical safety certificate for this property")
            assertThat(electricalCertMissingPage.warning)
                .containsText("You could face prosecution if you have tenants in a property without an electrical safety certificate.")
            assertThat(electricalCertMissingPage.submitButton).containsText("Continue without a valid electrical safety certificate")
            electricalCertMissingPage.form.submit()
            val checkElectricalSafetyAnswersPage =
                assertPageIs(page, CheckElectricalSafetyAnswersFormPagePropertyRegistration::class)

            // Setup EpcLookupByUprnStep NOT finding an EPC for this property when the next step submits
            whenever(epcRegisterClient.getByUprn(uprnForSelectedAddress)).thenReturn(MockEpcData.epcRegisterClientEpcNotFoundResponse)

            // Check Electrical Safety Answers - render page
            assertThat(checkElectricalSafetyAnswersPage.heading).containsText("Electrical safety certificate")
            checkElectricalSafetyAnswersPage.form.submit()
            val taskListPageAfterElectricalSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // The internal EpcLookupByUprnStep at the start of the EpcTask does not find an EPC
            taskListPageAfterElectricalSafety.clickRegisterTaskWithName("Energy performance certificate (EPC)")
            val hasEpcPage = assertPageIs(page, HasEpcFormPagePropertyRegistration::class)

            // Has EPC - render page
            assertThat(hasEpcPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasEpcPage.submitHasNoEpc()
            val isEpcRequiredPage = assertPageIs(page, IsEpcRequiredFormPagePropertyRegistration::class)

            // Is EPC required - render page
            assertThat(isEpcRequiredPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(isEpcRequiredPage.heading).containsText("Is an EPC required to let this property?")
            isEpcRequiredPage.submitEpcRequired()
            val epcMissingPage = assertPageIs(page, EpcMissingFormPagePropertyRegistration::class)

            // EPC Missing - render page
            assertThat(epcMissingPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(epcMissingPage.heading).containsText("Your property is missing an EPC")
            assertThat(epcMissingPage.continueAnywayButton).containsText("Continue anyway")
            assertThat(
                epcMissingPage.warning,
            ).containsText("You can be fined for letting a property that does not meet energy efficiency requirements.")
            epcMissingPage.form.submit()
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            val taskListPageAfterEpc = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterEpc.clickCheckAndSubmitTaskWithName("Check and submit your answers")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            assertThat(checkAnswersPage.sectionHeader).containsText("Section 2 of 2 — Check and submit your property details")

            // Check Answers - submit to reach Confirm Missing Compliance page
            checkAnswersPage.form.submit()
            val confirmMissingCompliancePage =
                assertPageIs(page, ConfirmMissingComplianceFormPagePropertyRegistration::class)

            // Confirm Missing Compliance - render page
            assertThat(confirmMissingCompliancePage.heading).containsText("Confirm missing compliance certificates")
            assertThat(confirmMissingCompliancePage.warning).isVisible()
            assertThat(confirmMissingCompliancePage.form.sectionHeader).containsText("Submit registration")

            // Confirm Missing Compliance - submit
            confirmMissingCompliancePage.form.radios.selectValue("true")
            confirmMissingCompliancePage.form.submit()
            val confirmationPage = assertPageIs(page, ConfirmationPagePropertyRegistration::class)

            // Confirmation - verify record saved
            val propertyOwnershipCaptor = captor<PropertyOwnership>()
            verify(propertyOwnershipRepository).save(propertyOwnershipCaptor.capture())
            val expectedPropertyRegNum =
                RegistrationNumberDataModel.fromRegistrationNumber(
                    propertyOwnershipCaptor.value.registrationNumber,
                )
            assertEquals(expectedPropertyRegNum.toString(), confirmationPage.registrationNumberText)
            assertTrue(confirmationPage.surveyLink.locator.isVisible)
            assertThat(confirmationPage.surveyLink).hasAttribute("href", INDIVIDUAL_PROPERTY_REGISTRATION_SURVEY_URL)
            assertTrue(confirmationPage.goToDashboardLink.locator.isVisible)
        }

        @Test
        fun `User can complete the journey with expired compliance certificates for an occupied property (epc found by uprn)`(page: Page) {
            // Gas supply page
            val hasGasSupplyPage = navigator.skipToPropertyRegistrationHasGasSupplyPage(propertyIsOccupied = true)
            assertThat(hasGasSupplyPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasGasSupplyPage.submitHasGasSupply()
            val hasGasCertPage = assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)

            // Has Gas Cert page
            assertThat(hasGasCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasGasCertPage.submitHasCertificate()
            var gasCertIssueDatePage = assertPageIs(page, GasCertIssueDateFormPagePropertyRegistration::class)

            // Gas Cert Issue Date - render page
            assertThat(gasCertIssueDatePage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(gasCertIssueDatePage.heading).containsText("What’s the issue date on the gas safety certificate?")
            gasCertIssueDatePage.submitDate(expiredGasSafetyCertIssueDate)
            var gasCertExpiredPage = assertPageIs(page, GasCertExpiredFormPagePropertyRegistration::class)

            // Gas Cert Expired - render page then navigate to edit issue date
            assertThat(gasCertExpiredPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(gasCertExpiredPage.mainHeading).containsText("This gas safety certificate has expired")
            assertThat(gasCertExpiredPage.sectionHeading).containsText("You must get a valid gas safety certificate for this property")
            assertThat(gasCertExpiredPage.warning)
                .containsText("You could face prosecution if you have tenants in a property without a gas safety certificate.")
            assertThat(gasCertExpiredPage.submitButton).containsText("Continue without a valid gas safety certificate")
            gasCertExpiredPage.changeIssueDateLink.clickAndWait()
            gasCertIssueDatePage = assertPageIs(page, GasCertIssueDateFormPagePropertyRegistration::class)

            // Gas Cert Issue Date - render page, prepopulated with previous value, then submit again
            assertThat(gasCertIssueDatePage.form.dayInput).hasValue(expiredGasSafetyCertIssueDate.dayOfMonth.toString())
            assertThat(gasCertIssueDatePage.form.monthInput).hasValue(expiredGasSafetyCertIssueDate.monthNumber.toString())
            assertThat(gasCertIssueDatePage.form.yearInput).hasValue(expiredGasSafetyCertIssueDate.year.toString())
            gasCertIssueDatePage.form.submit()
            gasCertExpiredPage = assertPageIs(page, GasCertExpiredFormPagePropertyRegistration::class)

            // Back on Gas Cert Expired page - submit
            gasCertExpiredPage.form.submit()
            val checkGasSafetyAnswersPage = assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)

            // Check Gas Safety Answers - render page
            assertThat(checkGasSafetyAnswersPage.heading).containsText("Gas safety certificate")

            checkGasSafetyAnswersPage.form.submit()
            val taskListPageAfterGasSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterGasSafety.clickRegisterTaskWithName("Electrical safety certificate")
            val hasElectricalCertPage = assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)

            // Has Electrical Cert - render page
            assertThat(hasElectricalCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(hasElectricalCertPage.heading).containsText("Which electrical safety certificate do you have for this property?")
            hasElectricalCertPage.submitHasEic()
            var electricalCertExpiryDatePage =
                assertPageIs(page, ElectricalCertExpiryDateFormPagePropertyRegistration::class)

            // Electrical Cert Expiry Date - render page
            assertThat(electricalCertExpiryDatePage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            electricalCertExpiryDatePage.submitDate(expiredExpiryDate)
            var electricalCertExpiredPage = assertPageIs(page, ElectricalCertExpiredFormPagePropertyRegistration::class)

            // Electrical Cert Expired - render page then check change expiry date link
            assertThat(electricalCertExpiredPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(electricalCertExpiredPage.warning)
                .containsText("You could face prosecution if you have tenants in a property without an electrical safety certificate.")
            assertThat(electricalCertExpiredPage.submitButton).containsText("Continue without a valid electrical safety certificate")
            electricalCertExpiredPage.changeExpiryDateLink.clickAndWait()
            electricalCertExpiryDatePage =
                assertPageIs(page, ElectricalCertExpiryDateFormPagePropertyRegistration::class)

            // Electrical Cert Expiry Date again - render page, prepopulated with previous value, then submit again
            assertThat(electricalCertExpiryDatePage.form.dayInput).hasValue(expiredExpiryDate.dayOfMonth.toString())
            assertThat(electricalCertExpiryDatePage.form.monthInput).hasValue(expiredExpiryDate.monthNumber.toString())
            assertThat(electricalCertExpiryDatePage.form.yearInput).hasValue(expiredExpiryDate.year.toString())
            electricalCertExpiryDatePage.form.submit()
            electricalCertExpiredPage = assertPageIs(page, ElectricalCertExpiredFormPagePropertyRegistration::class)

            // Back on Electrical Cert Expired page - submit
            electricalCertExpiredPage.form.submit()
            val checkElectricalSafetyAnswersPage =
                assertPageIs(page, CheckElectricalSafetyAnswersFormPagePropertyRegistration::class)

            // Setup EpcLookupByUprnStep being able to find an EPC for this property when the next step submits
            whenever(epcRegisterClient.getByUprn(uprnForSelectedAddress))
                .thenReturn(
                    MockEpcData.createEpcRegisterClientEpcFoundResponse(
                        expiryDate = expiredExpiryDate,
                    ),
                )

            // Check Electrical Safety Answers - render page
            assertThat(checkElectricalSafetyAnswersPage.heading).containsText("Electrical safety certificate")
            checkElectricalSafetyAnswersPage.form.submit()
            val taskListPageAfterElectricalSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // EpcLookupByUprnStep finds the EPC, so redirects to Check UPRN matched EPCe
            taskListPageAfterElectricalSafety.clickRegisterTaskWithName("Energy performance certificate (EPC)")
            val confirmUprnMatchedEpcDetailsPage =
                assertPageIs(page, ConfirmEpcDetailsRetrievedByUprnFormPagePropertyRegistration::class)

            // Check UPRN matched EPC - submit Yes (accept this expired EPC, which triggers age/rating check internally)
            assertThat(confirmUprnMatchedEpcDetailsPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            confirmUprnMatchedEpcDetailsPage.submitYes()
            val epcExpiryCheckPage = assertPageIs(page, EpcInDateAtStartOfTenancyCheckPagePropertyRegistration::class)

            assertThat(epcExpiryCheckPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            epcExpiryCheckPage.submitEpcExpired()
            val epcExpiredPage = assertPageIs(page, EpcExpiredFormPagePropertyRegistration::class)

            // EPC Expired - occupied variant: warning visible, "Continue anyway" button
            assertThat(epcExpiredPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(epcExpiredPage.heading).containsText("This property’s EPC has expired")
            assertThat(epcExpiredPage.warning).isVisible()
            assertThat(epcExpiredPage.submitButton).containsText("Continue anyway")
            epcExpiredPage.form.submit()
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            val taskListPageAfterEpc = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterEpc.clickCheckAndSubmitTaskWithName("Check and submit your answers")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            assertThat(checkAnswersPage.sectionHeader).containsText("Section 2 of 2 — Check and submit your property details")
        }

        @Test
        fun `User can complete the journey with expired compliance certificates for an unoccupied property (epc not found by uprn)`(
            page: Page,
        ) {
            // Gas supply page
            val hasGasSupplyPage = navigator.skipToPropertyRegistrationHasGasSupplyPage(propertyIsOccupied = false)
            assertThat(hasGasSupplyPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasGasSupplyPage.submitHasGasSupply()
            val hasGasCertPage = assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)

            // Has Gas Cert page
            assertThat(hasGasCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasGasCertPage.submitHasCertificate()
            var gasCertIssueDatePage = assertPageIs(page, GasCertIssueDateFormPagePropertyRegistration::class)

            // Gas Cert Issue Date - render page
            assertThat(gasCertIssueDatePage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(gasCertIssueDatePage.heading).containsText("What’s the issue date on the gas safety certificate?")
            gasCertIssueDatePage.submitDate(expiredGasSafetyCertIssueDate)
            var gasCertExpiredPage = assertPageIs(page, GasCertExpiredFormPagePropertyRegistration::class)

            // Gas Cert Expired - render page then navigate to edit issue date
            assertThat(gasCertExpiredPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(gasCertExpiredPage.mainHeading).containsText("This gas safety certificate has expired")
            assertThat(gasCertExpiredPage.sectionHeading).containsText("What to do next")
            assertThat(gasCertExpiredPage.warning).isHidden()
            assertThat(gasCertExpiredPage.submitButton).containsText("Save and continue")
            gasCertExpiredPage.changeIssueDateLink.clickAndWait()
            gasCertIssueDatePage = assertPageIs(page, GasCertIssueDateFormPagePropertyRegistration::class)

            // Gas Cert Issue Date - render page, prepopulated with previous value, then submit again
            assertThat(gasCertIssueDatePage.form.dayInput).hasValue(expiredGasSafetyCertIssueDate.dayOfMonth.toString())
            assertThat(gasCertIssueDatePage.form.monthInput).hasValue(expiredGasSafetyCertIssueDate.monthNumber.toString())
            assertThat(gasCertIssueDatePage.form.yearInput).hasValue(expiredGasSafetyCertIssueDate.year.toString())
            gasCertIssueDatePage.form.submit()
            gasCertExpiredPage = assertPageIs(page, GasCertExpiredFormPagePropertyRegistration::class)

            // Back on Gas Cert Expired page - submit
            gasCertExpiredPage.form.submit()
            val checkGasSafetyAnswersPage = assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)

            // Check Gas Safety Answers - render page
            assertThat(checkGasSafetyAnswersPage.heading).containsText("Gas safety certificate")

            checkGasSafetyAnswersPage.form.submit()
            val taskListPageAfterGasSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterGasSafety.clickRegisterTaskWithName("Electrical safety certificate")
            val hasElectricalCertPage = assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)

            // Has Electrical Cert - render page
            assertThat(hasElectricalCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(hasElectricalCertPage.heading).containsText("Which electrical safety certificate do you have for this property?")
            hasElectricalCertPage.submitHasEic()
            var electricalCertExpiryDatePage =
                assertPageIs(page, ElectricalCertExpiryDateFormPagePropertyRegistration::class)

            // Electrical Cert Expiry Date - render page
            assertThat(electricalCertExpiryDatePage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            electricalCertExpiryDatePage.submitDate(expiredExpiryDate)
            var electricalCertExpiredPage = assertPageIs(page, ElectricalCertExpiredFormPagePropertyRegistration::class)

            // Electrical Cert Expired - render page then check change expiry date link
            assertThat(electricalCertExpiredPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(electricalCertExpiredPage.warning).isHidden()
            assertThat(electricalCertExpiredPage.submitButton).containsText("Save and continue")
            electricalCertExpiredPage.changeExpiryDateLink.clickAndWait()
            electricalCertExpiryDatePage =
                assertPageIs(page, ElectricalCertExpiryDateFormPagePropertyRegistration::class)

            // Electrical Cert Expiry Date again - render page, prepopulated with previous value, then submit again
            assertThat(electricalCertExpiryDatePage.form.dayInput).hasValue(expiredExpiryDate.dayOfMonth.toString())
            assertThat(electricalCertExpiryDatePage.form.monthInput).hasValue(expiredExpiryDate.monthNumber.toString())
            assertThat(electricalCertExpiryDatePage.form.yearInput).hasValue(expiredExpiryDate.year.toString())
            electricalCertExpiryDatePage.form.submit()
            electricalCertExpiredPage = assertPageIs(page, ElectricalCertExpiredFormPagePropertyRegistration::class)

            // Back on Electrical Cert Expired page - submit
            electricalCertExpiredPage.form.submit()
            val checkElectricalSafetyAnswersPage =
                assertPageIs(page, CheckElectricalSafetyAnswersFormPagePropertyRegistration::class)

            // Setup EpcLookupByUprnStep NOT finding an EPC for this property when the next step submits
            whenever(epcRegisterClient.getByUprn(uprnForSelectedAddress)).thenReturn(MockEpcData.epcRegisterClientEpcNotFoundResponse)

            // Check Electrical Safety Answers - render page
            assertThat(checkElectricalSafetyAnswersPage.heading).containsText("Electrical safety certificate")
            checkElectricalSafetyAnswersPage.form.submit()
            val taskListPageAfterElectricalSafety = assertPageIs(page, TaskListPagePropertyRegistration::class)

            // The internal EpcLookupByUprnStep at the start of the EpcTask does not find an EPC
            taskListPageAfterElectricalSafety.clickRegisterTaskWithName("Energy performance certificate (EPC)")
            val hasEpcPage = assertPageIs(page, HasEpcFormPagePropertyRegistration::class)

            // Has EPC - render page
            assertThat(hasEpcPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasEpcPage.submitHasEpc()
            val findYourEpcPage = assertPageIs(page, FindYourEpcFormPagePropertyRegistration::class)

            // EPC Search - render page
            assertThat(findYourEpcPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            whenever(epcRegisterClient.getByRrn(CURRENT_EXPIRED_EPC_CERTIFICATE_NUMBER))
                .thenReturn(
                    MockEpcData.createEpcRegisterClientEpcFoundResponse(
                        certificateNumber = CURRENT_EXPIRED_EPC_CERTIFICATE_NUMBER,
                        expiryDate = expiredExpiryDate,
                        latestCertificateNumberForThisProperty = CURRENT_EXPIRED_EPC_CERTIFICATE_NUMBER,
                    ),
                )
            findYourEpcPage.submitCurrentEpcNumberWhichIsExpired()
            val confirmEpcDetailsPage =
                assertPageIs(page, ConfirmEpcDetailsRetrievedByCertificateNumberPagePropertyRegistration::class)

            // Check Matched EPC - render page
            assertThat(confirmEpcDetailsPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            val expectedExpiryDate =
                expiredExpiryDate
                    .toJavaLocalDate()
                    .format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
            assertThat(confirmEpcDetailsPage.summaryCard.summaryList.addressRow.value).containsText(MockEpcData.defaultSingleLineAddress)
            assertThat(confirmEpcDetailsPage.summaryCard.summaryList.energyEfficiencyRatingRow.value).containsText("C")
            assertThat(confirmEpcDetailsPage.summaryCard.summaryList.expiryDateRow.value).containsText(
                expectedExpiryDate,
            )
            assertThat(
                confirmEpcDetailsPage.summaryCard.summaryList.certificateNumberRow.value,
            ).containsText(CURRENT_EXPIRED_EPC_CERTIFICATE_NUMBER)
            confirmEpcDetailsPage.submitYes()
            val epcExpiredPage = assertPageIs(page, EpcExpiredFormPagePropertyRegistration::class)

            // EPC Expired - unoccupied variant: no warning, "Continue" button
            assertThat(epcExpiredPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(epcExpiredPage.heading).containsText("This property’s EPC has expired")
            assertThat(epcExpiredPage.warning).isHidden()
            assertThat(epcExpiredPage.submitButton).containsText("Continue")
            epcExpiredPage.form.submit()
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            val taskListPageAfterEpc = assertPageIs(page, TaskListPagePropertyRegistration::class)
            taskListPageAfterEpc.clickCheckAndSubmitTaskWithName("Check and submit your answers")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            assertThat(checkAnswersPage.sectionHeader).containsText("Section 2 of 2 — Check and submit your property details")
        }

        @Test
        fun `The Electrical Safety task can be completed by the user uploaded an eicr`(page: Page) {
            // Skip to Has Electrical Cert page and submit "Yes"
            val hasElectricalCertPage = navigator.skipToPropertyRegistrationHasElectricalCertPage()
            assertThat(hasElectricalCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasElectricalCertPage.submitHasEicr()
            val electricalCertExpiryDatePage =
                assertPageIs(page, ElectricalCertExpiryDateFormPagePropertyRegistration::class)

            // Electrical Cert Expiry Date - render page
            assertThat(electricalCertExpiryDatePage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(
                electricalCertExpiryDatePage.heading,
            ).containsText("What’s the expiry date on the Electrical Installation Condition Report?")
            electricalCertExpiryDatePage.submitDate(validExpiryDate)
            val uploadElectricalCertPage = assertPageIs(page, UploadElectricalCertFormPagePropertyRegistration::class)

            // Upload Electrical Cert - render page
            assertThat(uploadElectricalCertPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(uploadElectricalCertPage.heading).containsText("Upload the Electrical Installation Condition Report (EICR)")
            uploadElectricalCertPage.uploadElectricalCertificate(Path.of("src/test/resources/test-files/blank.png"))

            // Check Electrical Safety Answers - EICR variant verified by heading text
        }

        @Test
        fun `The EPC task can be completed when FindYourEpc finds a superseded epc`(page: Page) {
            // Skip to Find Your EPC page and submit "Superseded EPC Found"
            val findYourEpcPage = navigator.skipToPropertyRegistrationFindYourEpcPage()
            assertThat(findYourEpcPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            whenever(epcRegisterClient.getByRrn(SUPERSEDED_EPC_CERTIFICATE_NUMBER)).thenReturn(
                MockEpcData.createEpcRegisterClientEpcFoundResponse(
                    certificateNumber = SUPERSEDED_EPC_CERTIFICATE_NUMBER,
                    expiryDate = MockEpcData.expiryDateInThePast,
                    latestCertificateNumberForThisProperty = CURRENT_EPC_CERTIFICATE_NUMBER,
                ),
            )
            whenever(epcRegisterClient.getByRrn(CURRENT_EPC_CERTIFICATE_NUMBER)).thenReturn(
                MockEpcData.createEpcRegisterClientEpcFoundResponse(
                    certificateNumber = CURRENT_EPC_CERTIFICATE_NUMBER,
                ),
            )
            findYourEpcPage.submitSupersededEpcNumber()
            val epcSupersededPage = assertPageIs(page, EpcSuperseededFormPagePropertyRegistration::class)

            // Check details of superseded and latest epc - render page
            assertThat(epcSupersededPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            epcSupersededPage.submitContinueWithLatest()
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            assertPageIs(page, TaskListPagePropertyRegistration::class)
        }

        @Test
        fun `The EPC task can be completed when FindYourEpc finds no epc and it is missing`(page: Page) {
            // Skip to Find Your EPC page and submit "No EPC Found"
            val findYourEpcPage = navigator.skipToPropertyRegistrationFindYourEpcPage()
            assertThat(findYourEpcPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            whenever(
                epcRegisterClient.getByRrn(NONEXISTENT_EPC_CERTIFICATE_NUMBER),
            ).thenReturn(MockEpcData.epcRegisterClientEpcNotFoundResponse)
            findYourEpcPage.submitNonexistentEpcNumber()
            val epcNotFoundPage = assertPageIs(page, EpcNotFoundFormPagePropertyRegistration::class)

            // EPC not found - render page
            assertThat(epcNotFoundPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(epcNotFoundPage.heading).containsText("We could not find your EPC")
            assertThat(epcNotFoundPage.certificateNumberText).containsText(NONEXISTENT_EPC_CERTIFICATE_NUMBER)
            assertThat(epcNotFoundPage.searchAgainLink).isVisible()

            // Click 'search again' to return to Find Your EPC and re-submit not found
            epcNotFoundPage.searchAgainLink.click()
            val findYourEpcPageAgain = assertPageIs(page, FindYourEpcFormPagePropertyRegistration::class)
            assertThat(findYourEpcPageAgain.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            whenever(
                epcRegisterClient.getByRrn(NONEXISTENT_EPC_CERTIFICATE_NUMBER),
            ).thenReturn(MockEpcData.epcRegisterClientEpcNotFoundResponse)
            findYourEpcPageAgain.submitNonexistentEpcNumber()
            assertPageIs(page, EpcNotFoundFormPagePropertyRegistration::class).form.submit()

            val isEpcRequiredPage = assertPageIs(page, IsEpcRequiredFormPagePropertyRegistration::class)

            // Is EPC required - render page
            assertThat(isEpcRequiredPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(isEpcRequiredPage.heading).containsText("Is an EPC required to let this property?")
            isEpcRequiredPage.submitEpcRequired()
            val epcMissingPage = assertPageIs(page, EpcMissingFormPagePropertyRegistration::class)

            // EPC Missing - render page
            assertThat(epcMissingPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(epcMissingPage.heading).containsText("Your property is missing an EPC")
            assertThat(epcMissingPage.continueAnywayButton).containsText("Continue anyway")
            assertThat(
                epcMissingPage.warning,
            ).containsText("You can be fined for letting a property that does not meet energy efficiency requirements.")
            epcMissingPage.form.submit()
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
            checkEpcAnswersPage.form.submit()
            assertPageIs(page, TaskListPagePropertyRegistration::class)
        }

        @Test
        fun `User can navigate the MEES flow when they have a MEES exemption`(page: Page) {
            val hasMeesExemptionPage = navigator.skipToPropertyRegistrationHasMeesExemptionPage()

            // Has MEES Exemption - render page
            assertThat(hasMeesExemptionPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(hasMeesExemptionPage.heading).containsText("You need a registered energy efficiency exemption to let this property")
            hasMeesExemptionPage.submitHasMeesExemption()
            val meesExemptionPage = assertPageIs(page, MeesExemptionFormPagePropertyRegistration::class)

            // MEES Exemption - select exemption reason
            assertThat(meesExemptionPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            meesExemptionPage.submitExemptionReason(MeesExemptionReason.HIGH_COST)
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
        }

        @Test
        fun `User can navigate the MEES flow when they do not have a MEES exemption`(page: Page) {
            val hasMeesExemptionPage = navigator.skipToPropertyRegistrationHasMeesExemptionPage()

            // Has MEES Exemption - submit no exemption
            assertThat(hasMeesExemptionPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            hasMeesExemptionPage.submitHasNoMeesExemption()
            val lowEnergyRatingPage = assertPageIs(page, LowEnergyRatingFormPagePropertyRegistration::class)

            // Low Energy Rating - render page
            assertThat(lowEnergyRatingPage.sectionHeader).containsText(propertyRegistrationSectionHeader)
            assertThat(lowEnergyRatingPage.heading).containsText("This property does not meet energy efficiency requirements for letting")
            lowEnergyRatingPage.form.submit()
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
        }

        @Test
        fun `User can navigate the EPC exemption flow`(page: Page) {
            val epcExemptionPage = navigator.skipToPropertyRegistrationEpcExemptionPage()

            // EPC Exemption - select exemption reason
            assertThat(epcExemptionPage.form.sectionHeader).containsText(propertyRegistrationSectionHeader)
            epcExemptionPage.submitExemptionReason(EpcExemptionReason.PROTECTED_ARCHITECTURAL_OR_HISTORICAL_MERIT)
            val checkEpcAnswersPage = assertPageIs(page, CheckEpcAnswersFormPagePropertyRegistration::class)

            // Check EPC Answers - render page
            assertThat(checkEpcAnswersPage.heading).containsText("Energy performance certificate (EPC)")
        }

        @Test
        fun `task list back link navigates to start page after entering from start page`(page: Page) {
            val registerPropertyStartPage = navigator.goToPropertyRegistrationStartPage()
            registerPropertyStartPage.startButton.clickAndWait()
            val taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)

            taskListPage.backLink.clickAndWait()
            assertPageIs(page, RegisterPropertyStartPage::class)
        }

        @Test
        fun `task list back link navigates to start page after entering from start page and returning from a task`(page: Page) {
            val registerPropertyStartPage = navigator.goToPropertyRegistrationStartPage()
            registerPropertyStartPage.startButton.clickAndWait()
            var taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)

            taskListPage.clickRegisterTaskWithName("Property address")
            assertPageIs(page, LookupAddressFormPagePropertyRegistration::class)

            val backLink = BackLink.default(page)
            backLink.clickAndWait()
            taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)

            taskListPage.backLink.clickAndWait()
            assertPageIs(page, RegisterPropertyStartPage::class)
        }

        @Test
        fun `numeric values with leading zeros are displayed without leading zeros on the CYA page`(page: Page) {
            val checkAnswersPage =
                navigator.skipToPropertyRegistrationCheckAnswersPageOccupied(
                    households = 2,
                    people = 4,
                    bedrooms = 3,
                    rentAmount = "0000000.1",
                )

            assertThat(checkAnswersPage.summaryList.rentAmountRow.value).containsText("£0.1")
            assertThat(checkAnswersPage.summaryList.numberOfHouseholdsRow.value).containsText("2")
            assertThat(checkAnswersPage.summaryList.numberOfTenantsRow.value).containsText("4")
            assertThat(checkAnswersPage.summaryList.numberOfBedroomsRow.value).containsText("3")
        }

        @Test
        fun `CYA joint landlords row shows a change link to the check joint landlords page when landlords are invited`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageWithJointLandlords()

            val changeLink =
                checkAnswersPage.summaryList.jointLandlordsInvitationsRow.actions
                    .getActionLink("Change")
            assertThat(changeLink).isVisible()

            changeLink.clickAndWait()
            val checkJointLandlordsPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            assertThat(checkJointLandlordsPage.summaryList.firstRow.value).containsText("email@address.com")
        }

        @Test
        fun `CYA joint landlords row shows a change link to the has joint landlords page when there are no joint landlords`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPage()

            val changeLink =
                checkAnswersPage.summaryList.jointLandlordsAreThereRow.actions
                    .getActionLink("Change")
            assertThat(changeLink).isVisible()

            changeLink.clickAndWait()
            assertPageIs(page, HasJointLandlordsFormBasePagePropertyRegistration::class)
        }

        @Test
        fun `a landlord cannot invite themselves as a joint landlord`(page: Page) {
            val inviteJointLandlordPage = navigator.skipToPropertyRegistrationInviteJointLandlordPage()

            inviteJointLandlordPage.submitEmail("alex.surname@example.com")

            val inviteJointLandlordPageWithError =
                assertPageIs(page, InviteJointLandlordFormPagePropertyRegistration::class)
            assertThat(inviteJointLandlordPageWithError.form.getErrorMessage())
                .containsText("You cannot invite yourself as a joint landlord")

            inviteJointLandlordPageWithError.submitEmail("someone.else@example.com")
            assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
        }
    }
}
