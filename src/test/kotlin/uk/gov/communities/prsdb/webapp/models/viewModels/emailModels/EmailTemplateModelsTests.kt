package uk.gov.communities.prsdb.webapp.models.viewModels.emailModels

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.communities.prsdb.webapp.constants.enums.RegistrationNumberType
import uk.gov.communities.prsdb.webapp.models.dataModels.RegistrationNumberDataModel
import uk.gov.communities.prsdb.webapp.testHelpers.NotifyTemplateMetadataFactory
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLocalCouncilData
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import kotlin.String

class EmailTemplateModelsTests {
    @ParameterizedTest
    @CsvSource(
        "2026-10-30T14:38:42.082Z, 30 October 2026 at 2:38pm (GMT)",
        "2026-07-01T13:38:00Z, 1 July 2026 at 2:38pm (BST)",
        "2026-07-01T23:05:00Z, 2 July 2026 at 12:05am (BST)",
    )
    fun `manual intervention email formats payment creation time in UK time`(
        timestamp: String,
        expected: String,
    ) {
        val model =
            PaymentRequiresManualInterventionEmail(
                paymentId = "payment-id",
                paymentReference = "payment-reference",
                amountInPence = 2000,
                paymentCreatedAt = Instant.parse(timestamp),
                reason = "reason",
            )

        Assertions.assertEquals(expected, model.toHashMap()["payment created at"])
    }

    companion object {
        @JvmStatic
        private fun templateList() =
            listOf(
                EmailTemplateTestData(
                    LocalCouncilInvitationEmail(
                        MockLocalCouncilData.createLocalCouncil(),
                        URI("invitationUri"),
                    ),
                    "/emails/LocalCouncilInvitation.md",
                ),
                EmailTemplateTestData(
                    LocalCouncilInvitationCancellationEmail(MockLocalCouncilData.createLocalCouncil()),
                    "/emails/LocalCouncilInvitationCancellation.md",
                ),
                EmailTemplateTestData(
                    LocalCouncilAdminInvitationEmail(MockLocalCouncilData.createLocalCouncil(), URI("invitationUri")),
                    "/emails/LocalCouncilAdminInvitation.md",
                ),
                EmailTemplateTestData(
                    LandlordRegistrationConfirmationEmail("L-CCCC_CCCC", "prsdUrl"),
                    "/emails/LandlordRegistrationConfirmation.md",
                ),
                EmailTemplateTestData(
                    OrganisationalLandlordRegistrationConfirmationEmail(
                        registrantName = "Jill Jones",
                        organisationName = "Test Organisation Name",
                        lrn = "L-CCCC-CCCC",
                        prsdURL = "prsdUrl",
                        // TODO: PDJB-1828: Remove when the MULTI_USER_ORGANISATIONS flag is removed
                        isMultiUserOrganisationsEnabled = true,
                    ),
                    "/emails/OrganisationalLandlordRegistrationConfirmation.md",
                    allowExtraKeys = true,
                ),
                EmailTemplateTestData(
                    OrganisationalLandlordDeregistrationConfirmationEmail(
                        registrantName = "Sam Smith",
                        organisationName = "Example Housing Association",
                    ),
                    "/emails/OrganisationalLandlordDeregistrationConfirmation.md",
                ),
                EmailTemplateTestData(
                    OrganisationalLandlordInvitationEmail(
                        organisationName = "Example Housing Association",
                        invitationUri = URI("https://example.com/landlord/accept-invitation?token=abc123"),
                    ),
                    "/emails/OrganisationalLandlordInvitation.md",
                ),
                EmailTemplateTestData(
                    OrganisationalLandlordInvitationAcceptedEmail(
                        recipientName = "Jane Smith",
                        roleName = "editor",
                        organisationName = "Example Organisation",
                        isAdmin = false,
                        dashboardUrl = URI("dashboardUrl"),
                    ),
                    "/emails/OrganisationalLandlordInvitationAccepted.md",
                ),
                EmailTemplateTestData(
                    OrganisationalLandlordInvitationAcceptedEmail(
                        recipientName = "Jane Smith",
                        roleName = "administrator",
                        organisationName = "Example Organisation",
                        isAdmin = true,
                        dashboardUrl = URI("dashboardUrl"),
                    ),
                    "/emails/OrganisationalLandlordInvitationAccepted.md",
                ),
                // TODO PDJB-1734: Remove this entry when the PAYMENTS flag is removed
                EmailTemplateTestData(
                    PropertyRegistrationConfirmationEmail(
                        "P-XXX-YYY",
                        "1 Street Name\nAB1 2CD",
                        "prsdUrl",
                        isOccupied = true,
                        jointLandlordEmails = listOf("joint1@example.com", "joint2@example.com"),
                    ),
                    "/emails/PropertyRegistrationConfirmation.md",
                    allowExtraKeys = true,
                ),
                EmailTemplateTestData(
                    PropertyRegistrationConfirmationEmail(
                        "P-XXX-YYY",
                        "1 Street Name\nAB1 2CD",
                        "prsdUrl",
                        isOccupied = true,
                        paymentDetails =
                            PropertyRegistrationConfirmationEmail.PaymentDetails(
                                amountInPence = 1000,
                                paymentDate = LocalDate.of(2026, 10, 5),
                                renewalDate = LocalDate.of(2027, 3, 31),
                            ),
                    ),
                    "/emails/PropertyRegistrationConfirmationWithPayment.md",
                    allowExtraKeys = true,
                ),
                EmailTemplateTestData(
                    PropertyDeregistrationConfirmationEmail("James", "Flat 1\n11 Elm Street\nLondon\nNE1 2EB"),
                    "/emails/PropertyDeregistrationConfirmation.md",
                ),
                EmailTemplateTestData(
                    PropertyDeregistrationInviteeCancellationEmail(
                        "Flat 1\n11 Elm Drive\nReal Town\nLondon\nNW8 2DK",
                        "signInUrl",
                    ),
                    "/emails/PropertyDeregistrationInviteeCancellation.md",
                ),
                EmailTemplateTestData(
                    LandlordNoPropertiesDeregistrationConfirmationEmail("Alexander Smith"),
                    "/emails/LandlordNoPropertiesDeregistrationConfirmation.md",
                ),
                EmailTemplateTestData(
                    LandlordWithPropertiesDeregistrationConfirmationEmail(
                        fullName = "Alexander Smith",
                        propertyListMarkdown =
                            PropertyDetailsEmailSectionList(
                                listOf(
                                    PropertyDetailsEmailSection(
                                        propertyNumber = 1,
                                        "P-WWW-XXX",
                                        "1 Fake Street, Mirageville",
                                    ),
                                ),
                            ),
                    ),
                    "/emails/LandlordWithPropertiesDeregistrationConfirmation.md",
                    allowExtraKeys = true,
                ),
                EmailTemplateTestData(
                    VirusScanUnsuccessfulEmail(
                        certificateType = "Gas safety certificate",
                        propertyAddress = "1 Street Name, Town, Country, AB1 2CD",
                    ),
                    "/emails/VirusScanUnsuccessful.md",
                ),
                EmailTemplateTestData(
                    PropertyUpdateConfirmation(
                        "1 Street Name, Town, AB1 2CD",
                        "P-XXXX-XXXX",
                        listOf("Thing you changed"),
                        URI("dashboardUrl"),
                    ),
                    "/emails/PropertyUpdateConfirmation.md",
                ),
                EmailTemplateTestData(
                    IndividualLandlordUpdateConfirmation("L-XXXX-XXXX", URI("dashboardUrl"), "Thing you changed"),
                    "/emails/IndividualLandlordUpdateConfirmation.md",
                ),
                EmailTemplateTestData(
                    BetaFeedbackEmail("feedback", "email@test.com", "referrer"),
                    "/emails/BetaFeedbackEmail.md",
                ),
                EmailTemplateTestData(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = "landlordName",
                        multiLineAddress = "multi\nline\naddress",
                        registrationNumber =
                            RegistrationNumberDataModel(
                                type = RegistrationNumberType.PROPERTY,
                                number = 123456L,
                            ),
                        dashboardUrl = URI("dashboardUrl"),
                        newCertificateUrl = URI("newCertificateUrl"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.CERTIFICATE_ADDED,
                        certificateType = "gas safety certificate",
                        certificateTypeLabel = "Gas safety certificate",
                        certificateDate = "1 January 2027",
                    ),
                    "/emails/ComplianceUpdatedConfirmation.md",
                    allowExtraKeys = true,
                ),
                EmailTemplateTestData(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = "landlordName",
                        multiLineAddress = "multi\nline\naddress",
                        registrationNumber =
                            RegistrationNumberDataModel(
                                type = RegistrationNumberType.PROPERTY,
                                number = 123456L,
                            ),
                        dashboardUrl = URI("dashboardUrl"),
                        newCertificateUrl = URI("newCertificateUrl"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_CERTIFICATE_OCCUPIED,
                        certificateType = "gas safety certificate",
                        certificateTypeLabel = "Gas safety certificate",
                        deadlineDate = "1 January 2027",
                    ),
                    "/emails/ComplianceExpiredOccupiedConfirmation.md",
                    allowExtraKeys = true,
                ),
                EmailTemplateTestData(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = "landlordName",
                        multiLineAddress = "multi\nline\naddress",
                        registrationNumber =
                            RegistrationNumberDataModel(
                                type = RegistrationNumberType.PROPERTY,
                                number = 123456L,
                            ),
                        dashboardUrl = URI("dashboardUrl"),
                        newCertificateUrl = URI("newCertificateUrl"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_CERTIFICATE_UNOCCUPIED,
                        certificateType = "gas safety certificate",
                        certificateTypeLabel = "Gas safety certificate",
                    ),
                    "/emails/ComplianceExpiredUnoccupiedConfirmation.md",
                    allowExtraKeys = true,
                ),
                EmailTemplateTestData(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = "landlordName",
                        multiLineAddress = "multi\nline\naddress",
                        registrationNumber =
                            RegistrationNumberDataModel(
                                type = RegistrationNumberType.PROPERTY,
                                number = 123456L,
                            ),
                        dashboardUrl = URI("dashboardUrl"),
                        newCertificateUrl = URI("newCertificateUrl"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_EPC_OCCUPIED,
                        certificateType = "energy performance certificate (EPC)",
                        certificateTypeLabel = "Energy performance certificate (EPC)",
                    ),
                    "/emails/ComplianceExpiredOccupiedEpcConfirmation.md",
                    allowExtraKeys = true,
                ),
                EmailTemplateTestData(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = "landlordName",
                        multiLineAddress = "multi\nline\naddress",
                        registrationNumber =
                            RegistrationNumberDataModel(
                                type = RegistrationNumberType.PROPERTY,
                                number = 123456L,
                            ),
                        dashboardUrl = URI("dashboardUrl"),
                        newCertificateUrl = URI("newCertificateUrl"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.CERTIFICATE_ADDED,
                        certificateType = "gas safety certificate",
                        certificateTypeLabel = "Gas safety certificate",
                        certificateDate = "1 January 2027",
                        isJointLandlord = true,
                    ),
                    "/emails/JointLandlordComplianceUpdatedConfirmation.md",
                    allowExtraKeys = true,
                ),
                EmailTemplateTestData(
                    LocalCouncilRegistrationConfirmationEmail("councilName", "prsdUrl", isAdmin = true),
                    "/emails/LocalCouncilRegistrationConfirmation.md",
                    allowExtraKeys = true,
                ),
                EmailTemplateTestData(
                    LocalCouncilUserDeletionEmail("councilName"),
                    "/emails/LocalCouncilUserDeletion.md",
                ),
                EmailTemplateTestData(
                    LocalCouncilUserDeletionInformAdminEmail("councilName", "email", "userName", "prsdUrl"),
                    "/emails/LocalCouncilUserDeletionAdminEmail.md",
                ),
                EmailTemplateTestData(
                    LocalCouncilUserInvitationInformAdminEmail("councilName", "email", "prsdURL"),
                    "/emails/LocalCouncilUserInvitationInformAdminEmail.md",
                ),
                EmailTemplateTestData(
                    IncompletePropertyReminderEmail("propertyAddress", 7, "prsdUrl"),
                    "/emails/IncompletePropertyReminder.md",
                ),
                EmailTemplateTestData(
                    JointLandlordInvitationEmail("John Smith", "1 Fake Street, London", URI("invitationUrl")),
                    "/emails/JointLandlordInvitation.md",
                ),
                EmailTemplateTestData(
                    JointLandlordInvitationExpiryEmail(
                        recipientName = "Lois",
                        invitedEmail = "invited@example.com",
                        propertyAddress = "1 Fake Street\nLondon\nSW1A 1AA",
                        propertyRecordUri = URI("propertyRecordUrl"),
                        expiryDays = 28,
                    ),
                    "/emails/JointLandlordInvitationExpiry.md",
                ),
                EmailTemplateTestData(
                    JointLandlordInvitationCancellationInviteeEmail(
                        propertyAddress = "1 Fake Street, London",
                    ),
                    "/emails/JointLandlordInvitationCancellationInvitee.md",
                ),
                EmailTemplateTestData(
                    JointLandlordInvitationCancellationCancellerEmail(
                        recipientName = "John Smith",
                        invitedEmail = "invitee@example.com",
                        propertyAddress = "1 Fake Street, London",
                        propertyRecordUrl = "https://example.com/property",
                    ),
                    "/emails/JointLandlordInvitationCancellationCanceller.md",
                ),
                EmailTemplateTestData(
                    JointLandlordInvitationCancellationOtherLandlordEmail(
                        recipientName = "Jane Doe",
                        invitedEmail = "invitee@example.com",
                        propertyAddress = "1 Fake Street, London",
                        propertyRecordUrl = "https://example.com/property",
                    ),
                    "/emails/JointLandlordInvitationCancellationOtherLandlord.md",
                ),
                EmailTemplateTestData(
                    JointLandlordInvitationRejectionEmail(
                        recipientName = "Lois Lane",
                        inviteeEmail = "invitee@example.com",
                        propertyAddress = "Flat 1\n11 Elm Drive\nLondon\nNW8 2DK",
                        propertyRecordUrl = "https://example.com/property/42",
                    ),
                    "/emails/JointLandlordInvitationRejection.md",
                ),
                EmailTemplateTestData(
                    JointLandlordInvitationAcceptedEmail(
                        recipientName = "Jill",
                        propertyAddress = "Flat 1\n11 Elm Drive\nLondon\nNW2 2DK",
                        propertyRecordUrl = "https://example.com/property",
                        propertyRegistrationNumber = "P-GD47-39FX",
                    ),
                    "/emails/JointLandlordInvitationAccepted.md",
                ),
                EmailTemplateTestData(
                    JointLandlordInvitationAcceptedOtherLandlordEmail(
                        recipientName = "Lois",
                        inviteeName = "Noel James",
                        propertyAddress = "Flat 1\n11 Elm Drive\nLondon\nNW8 2DK",
                        propertyRecordUrl = "https://example.com/property",
                    ),
                    "/emails/JointLandlordInvitationAcceptedOtherLandlord.md",
                ),
                EmailTemplateTestData(
                    JointLandlordPropertyUpdateNotificationEmail(
                        recipientName = "Lois",
                        propertyAddress = "Flat 1\n11 Elm Street\nLondon\nNW1 1AA",
                        updatedBullets = listOf("The ownership type"),
                        propertyRecordUrl = "https://example.com/property",
                    ),
                    "/emails/JointLandlordPropertyUpdateNotification.md",
                ),
                EmailTemplateTestData(
                    SwitchToIndividualConfirmationEmail(
                        landlordName = "John Smith",
                        propertyAddress = "1 Fake Street, London",
                    ),
                    "/emails/SwitchToIndividualConfirmation.md",
                ),
                EmailTemplateTestData(
                    SwapToIndividualNudgeEmail(
                        recipientName = "Lois",
                        propertyAddress = "Flat 1\n11 Elm Drive\nLondon\nNW8 2DK",
                        propertyRecordUrl = "https://example.com/property",
                    ),
                    "/emails/SwapToIndividualNudge.md",
                ),
                EmailTemplateTestData(
                    JointLandlordYouLeftConfirmationAccountDeactivated(
                        recipientName = "Lois",
                        propertyAddress = "Flat 1\n11 Elm Drive\nLondon\nNW8 2DK",
                        dashboardUrl = "https://example.com/landlord/dashboard",
                    ),
                    "/emails/JointLandlordYouLeftConfirmationAccountDeactivated.md",
                ),
                EmailTemplateTestData(
                    JointLandlordOtherLandlordLeftNotification(
                        leavingLandlord = "Alice",
                        notifiedLandlord = "Bob",
                        address = "Flat 1\n11 Elm Drive\nLondon\nNW8 2DK",
                        propertyRecordUrl = "https://example.com/property",
                    ),
                    "/emails/JointLandlordOtherLandlordLeftNotification.md",
                ),
                EmailTemplateTestData(
                    PropertyDeactivationConfirmationEmail(
                        recipientName = "Alice",
                        multiLineAddress = "Flat 1\n11 Elm Drive\nLondon\nNW8 2DK",
                        registrationNumber = RegistrationNumberDataModel(RegistrationNumberType.PROPERTY, 123456L).toString(),
                        landlordDeactivated = true,
                        dashboardUrl = "https://example.com/landlord/dashboard",
                    ),
                    "/emails/PropertyDeactivationConfirmation.md",
                    allowExtraKeys = true,
                ),
                EmailTemplateTestData(
                    CancelDelegationLandlordConfirmationEmail(
                        landlordName = "Alice",
                        propertyAddress = "Flat 1\n11 Elm Street\nLondon\nNW1 1AA",
                        lettingAgentEmail = "agent@example.com",
                        propertyRecordUrl = "https://example.com/property",
                    ),
                    "/emails/CancelDelegationLandlordConfirmation.md",
                ),
                EmailTemplateTestData(
                    CancelDelegationJointLandlordNotificationEmail(
                        jointLandlordName = "Bob",
                        propertyAddress = "Flat 1\n11 Elm Street\nLondon\nNW1 1AA",
                        lettingAgentEmail = "agent@example.com",
                        propertyRecordUrl = "https://example.com/property",
                    ),
                    "/emails/CancelDelegationJointLandlordNotification.md",
                ),
                EmailTemplateTestData(
                    CancelDelegationLettingAgentNotificationEmail(
                        propertyAddress = "Flat 1\n11 Elm Street\nLondon\nNW1 1AA",
                        singleLineAddress = "Flat 1, 11 Elm Street, London, NW1 1AA",
                    ),
                    "/emails/CancelDelegationLettingAgentNotification.md",
                ),
                EmailTemplateTestData(
                    DelegateToLettingAgentConfirmationEmail(
                        recipientName = "James",
                        propertyAddress = "Flat 1\n11 Elm Street\nLondon\nNW1 1AA",
                        lettingAgentEmail = "agent-1@example.com",
                        propertyRecordUrl = "https://example.com/property/1",
                    ),
                    "/emails/DelegateToLettingAgentConfirmation.md",
                ),
                EmailTemplateTestData(
                    JointLandlordDelegateToLettingAgentNotificationEmail(
                        recipientName = "James",
                        propertyAddress = "Flat 1\n11 Elm Street\nLondon\nNW1 1AA",
                        lettingAgentEmail = "agent-1@example.com",
                        propertyRecordUrl = "https://example.com/property/1",
                    ),
                    "/emails/JointLandlordDelegateToLettingAgentNotification.md",
                ),
                EmailTemplateTestData(
                    PropertyUpdateWithLettingAgentRemovedConfirmation(
                        name = "James",
                        propertyAddress = "Flat 1\n11 Elm Street\nLondon\nNW1 1AA",
                        updatedMessage = "The property was made unoccupied",
                        lettingAgentEmail = "agent@example.com",
                        propertyRecordUrl = "https://example.com/property/1",
                    ),
                    "/emails/PropertyUpdateWithLettingAgentRemovedConfirmation.md",
                ),
                EmailTemplateTestData(
                    JointLandlordPropertyUpdateWithLettingAgentRemovedNotification(
                        recipientName = "James",
                        propertyAddress = "Flat 1\n11 Elm Street\nLondon\nNW1 1AA",
                        updatedMessage = "The property was made unoccupied",
                        lettingAgentEmail = "agent@example.com",
                        propertyRecordUrl = "https://example.com/property/1",
                    ),
                    "/emails/JointLandlordPropertyUpdateWithLettingAgentRemovedNotification.md",
                ),
                EmailTemplateTestData(
                    DelegateToLettingAgentInvitationEmail(
                        landlordName = "Wallis Smith",
                        propertyAddress = "11 Street\nTown\nPostcode",
                        invitationLink = "https://example.com/start/abc123",
                        singleLineAddress = "11 Street, Town, Postcode",
                    ),
                    "/emails/DelegateToLettingAgentInvitation.md",
                ),
                EmailTemplateTestData(
                    DelegateToLettingAgentInvitationWithDeadlineEmail(
                        landlordName = "Wallis Smith",
                        propertyAddress = "11 Street\nTown\nPostcode",
                        invitationLink = "https://example.com/start/abc123",
                        deadlineDate = "13 June 2026",
                        singleLineAddress = "11 Street, Town, Postcode",
                    ),
                    "/emails/DelegateToLettingAgentInvitationWithDeadline.md",
                ),
                EmailTemplateTestData(
                    LettingAgentPropertyUpdateNotificationEmail(
                        recipientName = "James Smithson",
                        propertyAddress = "Flat 1\n11 Elm Street\nLondon\nNW1 1AA",
                        registrationNumber = "P-XXXX-YYYY",
                        updatedBullets = listOf("The licensing information"),
                        propertyRecordUrl = "https://example.com/property/1",
                    ),
                    "/emails/LettingAgentPropertyUpdateNotification.md",
                ),
                EmailTemplateTestData(
                    LettingAgentComplianceUpdateNotificationEmail(
                        recipientName = "James Smithson",
                        multiLineAddress = "Flat 1\n11 Elm Street\nLondon\nNE1 2EB",
                        registrationNumber = "P-XXXX-YYYY",
                        certificateType = "gas safety certificate",
                        certificateTypeLabel = "Gas safety certificate",
                        certificateDate = "1 November 2026",
                        propertyRecordUrl = "https://example.com/property/1",
                    ),
                    "/emails/LettingAgentComplianceUpdateNotification.md",
                ),
                EmailTemplateTestData(
                    ChangeOrgTeamMemberAccessLevelEmail(
                        memberName = "Anita Locke",
                        roleName = "administrator",
                        organisationName = "Test Organisation Name",
                        isAdmin = true,
                        dashboardUrl = URI("dashboardUrl"),
                    ),
                    "/emails/ChangeOrgTeamMemberAccessLevel.md",
                    allowExtraKeys = true,
                ),
                EmailTemplateTestData(
                    RemoveOrgTeamMemberConfirmationEmail(
                        adminName = "Alex Admin",
                        memberName = "Anita Locke",
                        organisationName = "Keystone Living Group",
                        teamMembersUrl = URI("teamMembersUrl"),
                    ),
                    "/emails/RemoveOrgTeamMemberConfirmation.md",
                ),
                EmailTemplateTestData(
                    PaymentRequiresManualInterventionEmail(
                        paymentId = "payment-id",
                        paymentReference = "payment-reference",
                        amountInPence = 2000,
                        paymentCreatedAt = Instant.parse("2026-01-01T12:00:00Z"),
                        reason = "reason",
                    ),
                    "/emails/PaymentRequiresManualIntervention.md",
                ),
                EmailTemplateTestData(
                    RemoveOrgTeamMemberNotificationEmail(
                        memberName = "Anita Locke",
                        organisationName = "Keystone Living Group",
                    ),
                    "/emails/RemoveOrgTeamMemberNotification.md",
                ),
            )
    }

    data class EmailTemplateTestData(
        val model: EmailTemplateModel,
        val markdownLocation: String,
        val allowExtraKeys: Boolean = false,
    ) {
        override fun toString(): String = model.javaClass.simpleName
    }

    @ParameterizedTest(name = "{0} keys match markdown")
    @MethodSource("templateList")
    fun `EmailTemplateModels hashmaps have keys that match the parameters in their markdown templates`(testData: EmailTemplateTestData) {
        // Arrange
        val emailTemplateMetadata = NotifyTemplateMetadataFactory(null, "/emails/emailTemplates.json")
        val storedBody = javaClass.getResource(testData.markdownLocation)?.readText() ?: ""
        val storedMetadata =
            emailTemplateMetadata.metadataList.single { metadata ->
                metadata.enumName == testData.model.template.name
            }

        val subjectParameters = extractParameters(storedMetadata.subject)
        val bodyParameters = extractParameters(storedBody)
        val parameters = (subjectParameters + bodyParameters).distinct()

        // Act
        val modelHashMap = testData.model.toHashMap()

        // Assert
        if (!testData.allowExtraKeys) {
            Assertions.assertEquals(parameters.size, modelHashMap.size)
        }

        for (parameter in parameters) {
            if (isOptionalContentParameter(parameter)) {
                modelHashMap[extractOptionalContentParameter(parameter)] in listOf("yes", "no")
            } else {
                modelHashMap.keys.single { key -> key == parameter }
            }
        }
    }

    @ParameterizedTest
    @CsvSource(
        "true, false, true, yes",
        "false, false, true, no",
        "true, true, true, no",
        "true, false, false, no",
    )
    fun `property registration email shows missing details only when occupied not delegated and details are missing`(
        isOccupied: Boolean,
        isDelegatedToLettingAgent: Boolean,
        licenseProvideLater: Boolean,
        expectedHasMissingDetails: String,
    ) {
        // Arrange
        val email =
            PropertyRegistrationConfirmationEmail(
                prn = "P-XXX-YYY",
                multiLineAddress = "1 Street Name\nAB1 2CD",
                prsdUrl = "prsdUrl",
                isOccupied = isOccupied,
                isDelegatedToLettingAgent = isDelegatedToLettingAgent,
                licenseProvideLater = licenseProvideLater,
            )

        // Act
        val hashMap = email.toHashMap()

        // Assert
        Assertions.assertEquals(expectedHasMissingDetails, hashMap["hasMissingDetails"])
        if (expectedHasMissingDetails == "yes") {
            Assertions.assertTrue(hashMap["missingDetailsList"].orEmpty().contains("licensing details"))
        } else {
            Assertions.assertEquals("", hashMap["missingDetailsList"])
        }
    }

    @ParameterizedTest
    @CsvSource("true,yes", "false,no")
    fun `organisation registration email includes the feature flag as yes or no personalisation`(
        isMultiUserOrganisationsEnabled: Boolean,
        expectedPersonalisation: String,
    ) {
        val email =
            OrganisationalLandlordRegistrationConfirmationEmail(
                registrantName = "Jill Jones",
                organisationName = "Test Organisation Name",
                lrn = "L-CCCC-CCCC",
                prsdURL = "prsdUrl",
                isMultiUserOrganisationsEnabled = isMultiUserOrganisationsEnabled,
            )

        Assertions.assertEquals(
            hashMapOf(
                "registrant name" to "Jill Jones",
                "organisation name" to "Test Organisation Name",
                "LRN" to "L-CCCC-CCCC",
                "PRSD URL" to "prsdUrl",
                "isMultiUserOrganisationsEnabled" to expectedPersonalisation,
            ),
            email.toHashMap(),
        )
    }

    @Test
    fun `property registration email uses the template with payment information and formats the payment details when they are given`() {
        val email =
            PropertyRegistrationConfirmationEmail(
                prn = "P-XXX-YYY",
                multiLineAddress = "1 Street Name\nAB1 2CD",
                prsdUrl = "prsdUrl",
                isOccupied = true,
                paymentDetails =
                    PropertyRegistrationConfirmationEmail.PaymentDetails(
                        amountInPence = 2550,
                        paymentDate = LocalDate.of(2026, 10, 5),
                        renewalDate = LocalDate.of(2027, 3, 31),
                    ),
            )

        val hashMap = email.toHashMap()

        Assertions.assertEquals(EmailTemplate.PROPERTY_REGISTRATION_CONFIRMATION_WITH_PAYMENT, email.template)
        Assertions.assertEquals("£25.50", hashMap["payment amount"])
        Assertions.assertEquals("5 October 2026", hashMap["payment date"])
        Assertions.assertEquals("31 March 2027", hashMap["renewal date"])
    }

    // TODO PDJB-1734: Remove this test when the PAYMENTS flag is removed
    @Test
    fun `property registration email uses the template without payment information when payments is disabled`() {
        val email =
            PropertyRegistrationConfirmationEmail(
                prn = "P-XXX-YYY",
                multiLineAddress = "1 Street Name\nAB1 2CD",
                prsdUrl = "prsdUrl",
                isOccupied = true,
            )

        val hashMap = email.toHashMap()

        Assertions.assertEquals(EmailTemplate.PROPERTY_REGISTRATION_CONFIRMATION, email.template)
        Assertions.assertFalse(hashMap.keys.any { it in listOf("payment amount", "payment date", "renewal date") })
    }

    private fun isOptionalContentParameter(parameter: String): Boolean = extractOptionalContentParameter(parameter).isNotEmpty()

    private fun extractOptionalContentParameter(parameter: String): String = parameter.substringBefore("??", "")

    private fun extractParameters(body: String): List<String> {
        val parameterRegex = Regex("\\(\\((.*?)\\)\\)")
        return parameterRegex.findAll(body).map { result -> result.value.trim(')').trim('(') }.toList()
    }
}
