package uk.gov.communities.prsdb.webapp.integration

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.PaymentStatus
import uk.gov.communities.prsdb.webapp.database.entity.LandlordIncompleteProperty
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import uk.gov.communities.prsdb.webapp.database.entity.SavedJourneyState
import uk.gov.communities.prsdb.webapp.database.repository.FreeholderRepository
import uk.gov.communities.prsdb.webapp.database.repository.JointLandlordInvitationRepository
import uk.gov.communities.prsdb.webapp.database.repository.LettingAgentAccessRepository
import uk.gov.communities.prsdb.webapp.database.repository.PaymentRepository
import uk.gov.communities.prsdb.webapp.database.repository.PropertyOwnershipRepository
import uk.gov.communities.prsdb.webapp.database.repository.PrsdbUserRepository
import uk.gov.communities.prsdb.webapp.database.repository.SuperiorLandlordRepository
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.FreeholderDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.PropertyManagerDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.SuperiorLandlordDataModel
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.DelegateToLettingAgentInvitationWithDeadlineEmail
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.JointLandlordInvitationConfirmationEmail
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.JointLandlordInvitationEmail
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.PropertyRegistrationConfirmationEmail
import uk.gov.communities.prsdb.webapp.services.EmailNotificationService
import uk.gov.communities.prsdb.webapp.services.PropertyRegistrationConfirmationService
import uk.gov.communities.prsdb.webapp.services.PropertyRegistrationService
import uk.gov.communities.prsdb.webapp.testHelpers.JourneyTestHelper
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockPaymentData
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockPropertyRegistrationData

class PropertyRegistrationServicePersistenceTests : IntegrationTestWithMutableData("data-local.sql") {
    @Autowired
    private lateinit var propertyRegistrationService: PropertyRegistrationService

    @Autowired
    private lateinit var propertyOwnershipRepository: PropertyOwnershipRepository

    @Autowired
    private lateinit var freeholderRepository: FreeholderRepository

    @Autowired
    private lateinit var superiorLandlordRepository: SuperiorLandlordRepository

    @Autowired
    private lateinit var jointLandlordInvitationRepository: JointLandlordInvitationRepository

    @Autowired
    private lateinit var lettingAgentAccessRepository: LettingAgentAccessRepository

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var prsdbUserRepository: PrsdbUserRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @MockitoBean
    private lateinit var confirmationEmailSender: EmailNotificationService<PropertyRegistrationConfirmationEmail>

    @MockitoBean
    private lateinit var jointLandlordInvitationEmailSender: EmailNotificationService<JointLandlordInvitationEmail>

    @MockitoBean
    private lateinit var jointLandlordInvitationConfirmationEmailSender: EmailNotificationService<JointLandlordInvitationConfirmationEmail>

    @MockitoBean
    private lateinit var lettingAgentInvitationEmailSender: EmailNotificationService<DelegateToLettingAgentInvitationWithDeadlineEmail>

    @MockitoBean
    private lateinit var mockConfirmationService: PropertyRegistrationConfirmationService

    private val registrationData =
        MockPropertyRegistrationData.createPropertyRegistrationDataModel(
            addressModel = AddressDataModel(singleLineAddress = "1 Example Road, EG1 2AB", postcode = "EG1 2AB"),
            jointLandlordEmails = listOf(JOINT_LANDLORD_EMAIL),
            lettingAgentEmail = LETTING_AGENT_EMAIL,
        )

    @BeforeEach
    fun setMockUser() {
        JourneyTestHelper.setMockUser(USER_ID)
    }

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `registerProperty neither sets the last PRN registered this session nor sends emails when its transaction rolls back`() {
        // Act
        val propertyOwnership =
            TransactionTemplate(transactionManager).execute { transaction ->
                propertyRegistrationService.registerProperty(registrationData).also { transaction.setRollbackOnly() }
            }!!

        // Assert
        verifyNoEmailsSent()
        verifyNoInteractions(mockConfirmationService)
        assertTrue(propertyOwnershipRepository.findById(propertyOwnership.id).isEmpty)
        assertTrue(jointLandlordInvitationRepository.findByRegisteredOwnershipId(propertyOwnership.id).isEmpty())
        assertNull(lettingAgentAccessRepository.findByPropertyOwnershipId(propertyOwnership.id))
    }

    @Test
    fun `registerProperty sets the last PRN registered this session and sends its emails once its transaction commits`() {
        // Act
        val propertyOwnership =
            TransactionTemplate(transactionManager).execute {
                registerPropertyWithPayment()
            }!!

        // Assert
        verify(mockConfirmationService).setLastPrnRegisteredThisSession(propertyOwnership.registrationNumber.number)
        verify(confirmationEmailSender).sendEmail(eq(LANDLORD_EMAIL), any())
        verify(lettingAgentInvitationEmailSender).sendEmail(eq(LETTING_AGENT_EMAIL), any())
        verify(jointLandlordInvitationEmailSender).sendEmail(eq(JOINT_LANDLORD_EMAIL), any())
        verify(jointLandlordInvitationConfirmationEmailSender).sendEmail(eq(LANDLORD_EMAIL), any())
    }

    @Test
    fun `registerProperty keeps the registration and its invitations when an email fails to send`() {
        // Arrange
        doThrow(RuntimeException("Email failed to send")).whenever(jointLandlordInvitationEmailSender).sendEmail(any(), any())

        // Act
        val propertyOwnership = TransactionTemplate(transactionManager).execute { registerPropertyWithPayment() }!!

        // Assert
        assertTrue(propertyOwnershipRepository.findById(propertyOwnership.id).isPresent)
        val invitedEmails = jointLandlordInvitationRepository.findByRegisteredOwnershipId(propertyOwnership.id).map { it.invitedEmail }
        assertEquals(listOf(JOINT_LANDLORD_EMAIL), invitedEmails)
        assertEquals(LETTING_AGENT_EMAIL, lettingAgentAccessRepository.findByPropertyOwnershipId(propertyOwnership.id)?.invitedEmail)
        verify(confirmationEmailSender).sendEmail(eq(LANDLORD_EMAIL), any())
        verify(lettingAgentInvitationEmailSender).sendEmail(eq(LETTING_AGENT_EMAIL), any())
        verify(jointLandlordInvitationConfirmationEmailSender).sendEmail(eq(LANDLORD_EMAIL), any())
    }

    @Test
    fun `registerProperty persists the freeholders and superior landlords from the registration data`() {
        // Arrange
        val registrationDataWithParties =
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                addressModel = AddressDataModel(singleLineAddress = "2 Example Road, EG1 2AB", postcode = "EG1 2AB"),
                ownershipType = OwnershipType.LEASEHOLD_ONLY,
                freeholders = listOf(FreeholderDataModel(detailsKnown = true, name = "Freda Holder", email = "freda@example.com")),
                superiorLandlords = listOf(SuperiorLandlordDataModel(detailsKnown = true, name = "Sue Perior")),
            )

        // Act
        val propertyOwnership = propertyRegistrationService.registerProperty(registrationDataWithParties)

        // Assert
        assertEquals(listOf("Freda Holder"), freeholderRepository.findAllByPropertyOwnership_Id(propertyOwnership.id).map { it.name })
        assertEquals(
            listOf("Sue Perior"),
            superiorLandlordRepository.findAllByPropertyOwnership_Id(propertyOwnership.id).map { it.name },
        )
    }

    @Test
    fun `registerProperty persists and links the property manager from the registration data`() {
        // Arrange
        val registrationDataWithPropertyManager =
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                addressModel = AddressDataModel(singleLineAddress = "4 Example Road, EG1 2AB", postcode = "EG1 2AB"),
                propertyManager = PropertyManagerDataModel(name = "Pat Manager", email = "pat@example.com"),
            )

        // Act
        val propertyOwnership = propertyRegistrationService.registerProperty(registrationDataWithPropertyManager)

        // Assert
        val savedPropertyOwnership = propertyOwnershipRepository.findById(propertyOwnership.id).get()
        assertEquals("Pat Manager", savedPropertyOwnership.propertyManager?.name)
        assertEquals("pat@example.com", savedPropertyOwnership.propertyManager?.email)
    }

    @Test
    fun `registerProperty persists no property manager when none was collected`() {
        // Arrange
        val registrationDataWithoutPropertyManager =
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                addressModel = AddressDataModel(singleLineAddress = "5 Example Road, EG1 2AB", postcode = "EG1 2AB"),
                propertyManager = null,
            )

        // Act
        val propertyOwnership = propertyRegistrationService.registerProperty(registrationDataWithoutPropertyManager)

        // Assert
        assertNull(propertyOwnershipRepository.findById(propertyOwnership.id).get().propertyManager)
    }

    // TODO PDJB-2016: delete this class along with the ASSOCIATED_PARTIES flag. The flag-off journey never asks
    //  who the freeholders are, so PropertyOwnershipService supplies a placeholder when the ownership type
    //  requires freeholder records.
    @Nested
    inner class BeforePdjb955 {
        @Test
        fun `registerProperty persists a placeholder freeholder when no freeholders were collected`() {
            // Arrange
            val registrationDataWithoutParties =
                MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                    addressModel = AddressDataModel(singleLineAddress = "3 Example Road, EG1 2AB", postcode = "EG1 2AB"),
                    ownershipType = OwnershipType.LEASEHOLD_ONLY,
                    freeholders = null,
                )

            // Act
            val propertyOwnership = propertyRegistrationService.registerProperty(registrationDataWithoutParties)

            // Assert
            assertNull(freeholderRepository.findAllByPropertyOwnership_Id(propertyOwnership.id).single().name)
            assertTrue(superiorLandlordRepository.findAllByPropertyOwnership_Id(propertyOwnership.id).isEmpty())
        }
    }

    // The confirmation email needs a succeeded payment linked to the property, which PaymentService would normally save
    private fun registerPropertyWithPayment(): PropertyOwnership =
        propertyRegistrationService.registerProperty(registrationData).also { propertyOwnership ->
            val user = prsdbUserRepository.findById(USER_ID).get()
            paymentRepository.save(
                MockPaymentData
                    .createPayment(
                        paymentId = PAYMENT_ID,
                        status = PaymentStatus.SUCCEEDED,
                        incompleteProperty = LandlordIncompleteProperty(user, SavedJourneyState(user, JOURNEY_ID)),
                    ).apply { associateWithProperty(propertyOwnership) },
            )
        }

    private fun verifyNoEmailsSent() =
        verifyNoInteractions(
            confirmationEmailSender,
            lettingAgentInvitationEmailSender,
            jointLandlordInvitationEmailSender,
            jointLandlordInvitationConfirmationEmailSender,
        )

    companion object {
        private const val USER_ID = "urn:fdc:gov.uk:2022:UVWXY"
        private const val LANDLORD_EMAIL = "alex.surname@example.com"
        private const val JOINT_LANDLORD_EMAIL = "joint.landlord@example.com"
        private const val LETTING_AGENT_EMAIL = "letting.agent@example.com"
        private const val PAYMENT_ID = "property-registration-payment-id"
        private const val JOURNEY_ID = "property-registration-journey-id"
    }
}
