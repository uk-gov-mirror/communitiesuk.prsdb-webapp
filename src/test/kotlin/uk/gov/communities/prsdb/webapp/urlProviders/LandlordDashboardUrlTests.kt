package uk.gov.communities.prsdb.webapp.urlProviders

import jakarta.validation.Validator
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.get
import org.springframework.web.context.WebApplicationContext
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.controllers.ControllerTest
import uk.gov.communities.prsdb.webapp.controllers.LandlordController
import uk.gov.communities.prsdb.webapp.controllers.LandlordController.Companion.LANDLORD_DASHBOARD_URL
import uk.gov.communities.prsdb.webapp.controllers.RegisterLandlordController
import uk.gov.communities.prsdb.webapp.controllers.RegisterPropertyController
import uk.gov.communities.prsdb.webapp.database.entity.PrsdbUser
import uk.gov.communities.prsdb.webapp.helpers.CertificateUploadHelper
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.LandlordRegistrationJourneyFactory
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.PropertyRegistrationJourneyFactory
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.EmailTemplateModel
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.LandlordRegistrationConfirmationEmail
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.PropertyRegistrationConfirmationEmail
import uk.gov.communities.prsdb.webapp.services.AbsoluteUrlProvider
import uk.gov.communities.prsdb.webapp.services.EmailNotificationService
import uk.gov.communities.prsdb.webapp.services.FileUploadCookieService
import uk.gov.communities.prsdb.webapp.services.LandlordRegistrationService
import uk.gov.communities.prsdb.webapp.services.LandlordService
import uk.gov.communities.prsdb.webapp.services.OneLoginIdentityService
import uk.gov.communities.prsdb.webapp.services.PropertyComplianceService
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import uk.gov.communities.prsdb.webapp.services.PropertyRegistrationConfirmationService
import uk.gov.communities.prsdb.webapp.services.PropertyRegistrationService
import uk.gov.communities.prsdb.webapp.services.PrsdbUserService
import uk.gov.communities.prsdb.webapp.services.UploadService
import uk.gov.communities.prsdb.webapp.services.UserToLandlordService
import uk.gov.communities.prsdb.webapp.services.UsersIncompletePropertyService
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData.Companion.createIndividualLandlord
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData.Companion.createPropertyOwnership
import java.time.LocalDate
import kotlin.test.Test

@WebMvcTest(
    controllers = [
        LandlordController::class,
        RegisterLandlordController::class,
        RegisterPropertyController::class,
    ],
    properties = ["base-url.landlord=http://localhost:8080/landlord"],
)
@Import(AbsoluteUrlProvider::class)
class LandlordDashboardUrlTests(
    context: WebApplicationContext,
) : ControllerTest(context) {
    @MockitoBean
    private lateinit var mockLandlordRegistrationJourneyFactory: LandlordRegistrationJourneyFactory

    @MockitoBean
    private lateinit var mockPropertyRegistrationJourneyFactory: PropertyRegistrationJourneyFactory

    @MockitoBean
    lateinit var mockEmailNotificationService: EmailNotificationService<EmailTemplateModel>

    @MockitoBean
    private lateinit var mockLandlordService: LandlordService

    @MockitoBean
    private lateinit var propertyConfirmationService: PropertyRegistrationConfirmationService

    @MockitoBean
    private lateinit var mockIdentityService: OneLoginIdentityService

    @MockitoBean
    private lateinit var mockPropertyOwnershipService: PropertyOwnershipService

    @MockitoBean
    private lateinit var mockFileUploadCookieService: FileUploadCookieService

    @MockitoBean
    private lateinit var mockFileUploadService: UploadService

    @MockitoBean
    private lateinit var mockValidator: Validator

    @MockitoBean
    private lateinit var mockPropertyComplianceService: PropertyComplianceService

    @MockitoBean
    private lateinit var certificateUploadHelper: CertificateUploadHelper

    @MockitoBean
    private lateinit var featureFlagManager: FeatureFlagManager

    @MockitoBean
    private lateinit var mockUserToLandlordService: UserToLandlordService

    @MockitoBean
    private lateinit var usersIncompletePropertyService: UsersIncompletePropertyService

    @Autowired
    private lateinit var absoluteUrlProvider: AbsoluteUrlProvider

    @Test
    @WithMockUser(roles = ["LANDLORD"])
    fun `The sign in url generated when a landlord is registered is routed to the landlord dashboard`() {
        // Arrange
        val prsdbUserService = mock<PrsdbUserService>()
        val landlordService = mock<LandlordService>()
        val landlordRegistrationService =
            LandlordRegistrationService(
                landlordService,
                prsdbUserService,
                mock(),
                mock(),
                mockEmailNotificationService,
                mock(),
                absoluteUrlProvider,
            )

        whenever(prsdbUserService.findOrCreatePrsdbUser(any()))
            .thenReturn(PrsdbUser("baseUserId"))
        whenever(landlordService.createIndividualLandlord(any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(createIndividualLandlord())

        val confirmationCaptor = argumentCaptor<LandlordRegistrationConfirmationEmail>()
        Mockito
            .doNothing()
            .whenever(mockEmailNotificationService)
            .sendEmail(any(), confirmationCaptor.capture())

        // Act
        landlordRegistrationService.registerIndividualLandlord(
            "userId",
            "Test Name",
            "email",
            "phone",
            mock(),
            dateOfBirth = LocalDate.of(1990, 1, 1),
            countryOfResidence = "Test Country",
            isVerified = true,
            hasAcceptedPrivacyNotice = true,
            nonEnglandOrWalesAddress = null,
        )

        // Assert
        mvc
            .get(confirmationCaptor.firstValue.prsdURL)
            .andExpect { status { is3xxRedirection() } }
            .andExpect { redirectedUrl(LANDLORD_DASHBOARD_URL) }
    }

    @Test
    @WithMockUser(roles = ["LANDLORD"])
    fun `The sign in url generated when a property is registered is routed to the landlord dashboard`() {
        // Arrange
        val landlord = createIndividualLandlord()
        val propertyOwnership = createPropertyOwnership(landlords = mutableSetOf(landlord))
        val mockUserToLandlordService = mock<UserToLandlordService>()
        val propertyRegistrationService =
            PropertyRegistrationService(
                addressService = mock(),
                licenseService = mock(),
                propertyOwnershipService = mockPropertyOwnershipService,
                userToLandlordService = mockUserToLandlordService,
                absoluteUrlProvider = absoluteUrlProvider,
                confirmationEmailSender = mockEmailNotificationService,
                propertyOwnershipRepository = mock(),
                confirmationService = mock(),
                jointLandlordInvitationService = mock(),
                propertyComplianceService = mock(),
                lettingAgentAccessService = mock(),
                delegateToLettingAgentEmailService = mock(),
            )

        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                correspondenceEmail = anyOrNull(),
                correspondenceAddressModel = anyOrNull(),
            ),
        ).thenReturn(propertyOwnership)

        val confirmationCaptor = argumentCaptor<PropertyRegistrationConfirmationEmail>()
        Mockito
            .doNothing()
            .whenever(mockEmailNotificationService)
            .sendEmail(any(), confirmationCaptor.capture())

        // Act
        propertyRegistrationService.registerProperty(
            addressModel = AddressDataModel.fromAddress(propertyOwnership.address),
            propertyType = propertyOwnership.propertyBuildType,
            customPropertyType = propertyOwnership.customPropertyType,
            licenseType = propertyOwnership.license?.licenseType ?: LicensingType.NO_LICENSING,
            licenceNumber = propertyOwnership.license?.licenseNumber ?: "",
            ownershipType = propertyOwnership.ownershipType,
            isOccupied = propertyOwnership.isOccupied,
            numberOfHouseholds = propertyOwnership.currentNumHouseholds,
            numberOfPeople = propertyOwnership.currentNumTenants,
            numBedrooms = propertyOwnership.numBedrooms,
            billsIncludedList = propertyOwnership.billsIncludedList,
            customBillsIncluded = propertyOwnership.customBillsIncluded,
            furnishedStatus = propertyOwnership.furnishedStatus,
            rentFrequency = propertyOwnership.rentFrequency,
            customRentFrequency = propertyOwnership.customRentFrequency,
            rentAmount = propertyOwnership.rentAmount,
        )

        // Assert
        mvc
            .get(confirmationCaptor.firstValue.prsdUrl)
            .andExpect { status { is3xxRedirection() } }
            .andExpect { redirectedUrl(LANDLORD_DASHBOARD_URL) }
    }
}
