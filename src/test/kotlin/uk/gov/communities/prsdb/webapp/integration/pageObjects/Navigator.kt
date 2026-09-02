package uk.gov.communities.prsdb.webapp.integration.pageObjects

import com.microsoft.playwright.Page
import com.microsoft.playwright.Response
import com.microsoft.playwright.options.RequestOptions
import uk.gov.communities.prsdb.webapp.constants.CANCEL_INVITATION_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.CONFIRMATION_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.CONTEXT_ID_URL_PARAMETER
import uk.gov.communities.prsdb.webapp.constants.DELETE_ADMIN_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.DELETE_INCOMPLETE_PROPERTY_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.EDIT_ADMIN_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.LANDLORD_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.TASK_LIST_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.TOKEN
import uk.gov.communities.prsdb.webapp.constants.enums.OrgType
import uk.gov.communities.prsdb.webapp.constants.enums.RentFrequency
import uk.gov.communities.prsdb.webapp.controllers.AcceptOrRejectJointLandlordInvitationController
import uk.gov.communities.prsdb.webapp.controllers.BetaFeedbackController
import uk.gov.communities.prsdb.webapp.controllers.CancelJointLandlordInvitationController
import uk.gov.communities.prsdb.webapp.controllers.CancelLettingAgentDelegationController
import uk.gov.communities.prsdb.webapp.controllers.CookiesController.Companion.COOKIES_ROUTE
import uk.gov.communities.prsdb.webapp.controllers.DelegateToLettingAgentController
import uk.gov.communities.prsdb.webapp.controllers.DeregisterLandlordController
import uk.gov.communities.prsdb.webapp.controllers.DeregisterOrganisationalLandlordController
import uk.gov.communities.prsdb.webapp.controllers.DeregisterPropertyController
import uk.gov.communities.prsdb.webapp.controllers.FeatureFlagOverrideController.Companion.FEATURE_FLAG_OVERRIDES_ROUTE
import uk.gov.communities.prsdb.webapp.controllers.GeneratePasscodeController.Companion.GENERATE_PASSCODE_URL
import uk.gov.communities.prsdb.webapp.controllers.InviteJointLandlordController
import uk.gov.communities.prsdb.webapp.controllers.LandlordController.Companion.COMPLIANCE_ACTIONS_URL
import uk.gov.communities.prsdb.webapp.controllers.LandlordController.Companion.INCOMPLETE_PROPERTIES_URL
import uk.gov.communities.prsdb.webapp.controllers.LandlordController.Companion.LANDLORD_DASHBOARD_URL
import uk.gov.communities.prsdb.webapp.controllers.LandlordDetailsController
import uk.gov.communities.prsdb.webapp.controllers.LandlordPrivacyNoticeController.Companion.LANDLORD_PRIVACY_NOTICE_ROUTE
import uk.gov.communities.prsdb.webapp.controllers.LeavePropertyController
import uk.gov.communities.prsdb.webapp.controllers.LettingAgentInvitationController
import uk.gov.communities.prsdb.webapp.controllers.LocalCouncilDashboardController.Companion.LOCAL_COUNCIL_DASHBOARD_URL
import uk.gov.communities.prsdb.webapp.controllers.LocalCouncilPrivacyNoticeController
import uk.gov.communities.prsdb.webapp.controllers.ManageLocalCouncilAdminsController
import uk.gov.communities.prsdb.webapp.controllers.ManageLocalCouncilAdminsController.Companion.SYSTEM_OPERATOR_ROUTE
import uk.gov.communities.prsdb.webapp.controllers.ManageLocalCouncilUsersController.Companion.getInviteNewUserRoute
import uk.gov.communities.prsdb.webapp.controllers.ManageLocalCouncilUsersController.Companion.getManageUsersRoute
import uk.gov.communities.prsdb.webapp.controllers.ManageUsersViewType
import uk.gov.communities.prsdb.webapp.controllers.MetricsController.Companion.METRICS_URL
import uk.gov.communities.prsdb.webapp.controllers.PasscodeEntryController.Companion.INVALID_PASSCODE_ROUTE
import uk.gov.communities.prsdb.webapp.controllers.PasscodeEntryController.Companion.PASSCODE_ENTRY_ROUTE
import uk.gov.communities.prsdb.webapp.controllers.PropertyDetailsController
import uk.gov.communities.prsdb.webapp.controllers.RegisterLandlordController.Companion.LANDLORD_REGISTRATION_ROUTE
import uk.gov.communities.prsdb.webapp.controllers.RegisterLandlordController.Companion.LANDLORD_REGISTRATION_START_PAGE_ROUTE
import uk.gov.communities.prsdb.webapp.controllers.RegisterLocalCouncilUserController
import uk.gov.communities.prsdb.webapp.controllers.RegisterPropertyController
import uk.gov.communities.prsdb.webapp.controllers.SearchRegisterController
import uk.gov.communities.prsdb.webapp.controllers.SystemOperatorDashboardController.Companion.SYSTEM_OPERATOR_DASHBOARD_URL
import uk.gov.communities.prsdb.webapp.controllers.UpdateLandlordAddressController
import uk.gov.communities.prsdb.webapp.controllers.UpdateLandlordDateOfBirthController
import uk.gov.communities.prsdb.webapp.controllers.UpdateLandlordNameController
import uk.gov.communities.prsdb.webapp.controllers.UpdateOccupancyController
import uk.gov.communities.prsdb.webapp.controllers.UpdateOrganisationTypeController
import uk.gov.communities.prsdb.webapp.controllers.UpdateOwnershipTypeController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.CancelLocalCouncilAdminInvitationPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.ComplianceActionsPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.CookiesPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.DeleteIncompletePropertyRegistrationAreYouSurePage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.DeleteLocalCouncilAdminPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.EditLocalCouncilAdminPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.FeatureFlagOverridesPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.GeneratePasscodePage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.InviteLocalCouncilAdminPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.InviteNewLocalCouncilUserPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LandlordDashboardPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LandlordDetailsPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LandlordIncompletePropertiesPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LandlordPrivacyNoticePage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LocalCouncilDashboardPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LocalCouncilPrivacyNoticePage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LocalCouncilViewLandlordDetailsPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LocalCouncilViewOrgLandlordDetailsPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LookupAddressFormPageUpdateLandlordDetails
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.ManageLocalCouncilAdminsPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.ManageLocalCouncilUsersPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.MetricsPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.OrgLandlordDetailsPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.PasscodeEntryPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.PropertyDetailsPageLandlordView
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.PropertyDetailsPageLocalCouncilView
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.SearchLandlordRegisterPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.SearchPropertyRegisterPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.SelectAddressFormPageUpdateLandlordDetails
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.SystemOperatorDashboardPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.acceptOrRejectJointLandlordInvitationJourneyPages.AcceptOrRejectPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.acceptOrRejectJointLandlordInvitationJourneyPages.InvitationUnavailablePage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.createValidPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.betaFeedbackPages.LandlordBetaFeedbackPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.betaFeedbackPages.LocalCouncilBetaFeedbackPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.cancelJointLandlordInvitationJourneyPages.AreYouSurePageCancelJointLandlordInvitation
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.cancelLettingAgentDelegationJourneyPages.AreYouSurePageCancelLettingAgentDelegation
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.delegateToLettingAgentJourneyPages.AllowLettingAgentPageDelegateToLettingAgent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.inviteJointLandlordJourneyPages.InviteJointLandlordFormPageInviteJointLandlord
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordDeregistrationJourneyPages.AreYouSureFormPageLandlordDeregistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.CheckAnswersPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.CountryOfResidenceFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.DateOfBirthFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.EmailFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.LeadTrusteeAddressFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.LeadTrusteeDobFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.LeadTrusteeEmailFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.LeadTrusteeNameFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.LeadTrusteePhoneFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.LookupAddressFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.ManualAddressFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.NameFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgAddressFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgCharityNumberEnglandAndWalesFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgCharityNumberNorthernIrelandFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgCharityNumberScotlandFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgCharityRegisteredWithFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgCheckAnswersPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgEmailFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgGovBodyDetailsFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgGovBodyMemberDobFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgGovBodyMemberListFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgGovBodyMemberLookupAddressFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgGovBodyMemberNameFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgGovBodyMemberSelectAddressFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgGovBodyMustProvideInfoFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgGovBodyWhoToProvideFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgIsRegisteredCharityFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgMainContactFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgManualAddressFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgNameFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgPhoneNumberFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.OrgTypeFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.PhoneNumberFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.PrivacyNoticePageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.SelectAddressFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.landlordRegistrationJourneyPages.ServiceInformationStartPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.leavePropertyJourneyPages.ConfirmPageLeaveProperty
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.EnterPasswordPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.SetPasswordPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.ValidateTokenPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.localCouncilUserRegistrationJourneyPages.CheckAnswersPageLocalCouncilUserRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.localCouncilUserRegistrationJourneyPages.EmailFormPageLocalCouncilUserRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.localCouncilUserRegistrationJourneyPages.NameFormPageLocalCouncilUserRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.localCouncilUserRegistrationJourneyPages.PrivacyNoticePageLocalCouncilUserRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.organisationLandlordRegistrationJourneyPages.LandlordTypePageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.organisationLandlordRegistrationJourneyPages.OrgCompanyNumberFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.organisationLandlordRegistrationJourneyPages.OrgIsRegisteredCompanyFormPageLandlordRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.organisationalLandlordDeregistrationJourneyPages.AreYouSureFormPageOrganisationalLandlordDeregistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDeregistrationJourneyPages.CannotDeregisterPropertyJointLandlordsPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDeregistrationJourneyPages.CheckInvitationsPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDeregistrationJourneyPages.ConfirmPagePropertyDeregistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDeregistrationJourneyPages.DeregisterPropertyInfoPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.CheckOccupancyAnswersPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.OccupancyFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.OwnershipTypeFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.BillsIncludedFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckAnswersPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckElectricalSafetyAnswersFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckEpcAnswersFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckGasSafetyAnswersFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ConfirmEpcDetailsRetrievedByCertificateNumberPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ConfirmEpcDetailsRetrievedByUprnFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ConfirmMissingComplianceFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ElectricalCertExpiryDateFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.EpcExemptionFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.EpcExpiredFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.EpcInDateAtStartOfTenancyCheckPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.EpcMissingFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.FindYourEpcFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.FurnishedStatusFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.GasCertIssueDateFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasElectricalCertFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasEpcFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasGasCertFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasGasSupplyFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasJointLandlordsFormBasePagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasMeesExemptionFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HmoAdditionalLicenceFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HmoMandatoryLicenceFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.InviteAnotherJointLandlordFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.InviteJointLandlordFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.IsEpcRequiredFormPagePropertyRegistration
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
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ProvideEpcLaterFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ProvideTenancyDetailsLaterFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.RegisterPropertyStartPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.RentAmountFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.RentFrequencyFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.RentIncludesBillsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.SelectAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.SelectLocalCouncilFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.SelectiveLicenceFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.TaskListPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.updateOrganisationTypeJourneyPages.OrgTypeCyaPageUpdateOrganisationType
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.updateOrganisationTypeJourneyPages.OrgTypeTrustInterruptionPageUpdateOrganisationType
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.landlordDeregistration.stepConfig.AreYouSureStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.CountryOfResidenceStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.DateOfBirthStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.EmailStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.LandlordTypeStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.LeadTrusteeDobStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.LeadTrusteeEmailStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.LeadTrusteeNameStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.LeadTrusteePhoneStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgCharityNumberEnglandAndWalesStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgCharityNumberNorthernIrelandStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgCharityNumberScotlandStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgCharityRegisteredWithStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgCompanyNumberStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgEmailStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgGovBodyDetailsStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgGovBodyMemberDobStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgGovBodyMemberListStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgGovBodyMemberNameStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgGovBodyMustProvideInfoStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgGovBodyWhoToProvideStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgIsRegisteredCharityStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgIsRegisteredCompanyStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgMainContactStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgNameStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgPhoneNumberStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.OrgTypeStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.PhoneNumberStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.PrivacyNoticeStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.update.organisationType.OrgTypeCyaStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.update.organisationType.OrgTypeTrustInterruptionStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.EnterPasswordStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.SetPasswordStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.BedroomsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.BillsIncludedStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CheckElectricalSafetyAnswersStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CheckEpcAnswersStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CheckGasSafetyAnswersStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ConfirmEpcDetailsRetrievedByCertificateNumberStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ConfirmEpcRetrievedByUprnStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ConfirmMissingComplianceStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ElectricalCertExpiryDateStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.EpcExemptionStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.EpcInDateAtStartOfTenancyCheckStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FindYourEpcStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FurnishedStatusStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.GasCertIssueDateStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasElectricalCertStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasEpcStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasCertStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasSupplyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasJointLandlordsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasMeesExemptionStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HmoAdditionalLicenceStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HmoMandatoryLicenceStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HouseholdStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.IsEpcRequiredStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LicensingTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LocalCouncilStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.MeesExemptionStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OccupiedStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OwnershipTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.PropertyRegistrationCyaStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.PropertyTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ProvideEpcLaterStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ProvideTenancyDetailsLaterStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.RentAmountStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.RentFrequencyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.RentIncludesBillsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.SelectiveLicenceStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.TenantsStep
import uk.gov.communities.prsdb.webapp.journeys.shared.inviteJointLandlord.InviteJointLandlordStep
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.AbstractCheckYourAnswersStep
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.CheckPendingInvitationsStep
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.LookupAddressStep
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.ManualAddressStep
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.NameStep
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.SelectAddressStep
import uk.gov.communities.prsdb.webapp.journeys.shared.tasks.GovBodyMemberAddressTask
import uk.gov.communities.prsdb.webapp.journeys.shared.tasks.OrgAddressTask
import uk.gov.communities.prsdb.webapp.journeys.shared.tasks.TrusteeAddressTask
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.GoverningBodyMemberDataModel
import uk.gov.communities.prsdb.webapp.testHelpers.api.controllers.SessionController
import uk.gov.communities.prsdb.webapp.testHelpers.api.requestModels.SetJourneyStateRequestModel
import uk.gov.communities.prsdb.webapp.testHelpers.api.requestModels.StoreInvitationTokenRequestModel
import uk.gov.communities.prsdb.webapp.testHelpers.builders.LandlordStateSessionBuilder
import uk.gov.communities.prsdb.webapp.testHelpers.builders.LettingAgentInvitationStateSessionBuilder
import uk.gov.communities.prsdb.webapp.testHelpers.builders.LocalCouncilUserRegistrationStateSessionBuilder
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyDeregistrationStateSessionBuilder
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyStateSessionBuilder
import uk.gov.communities.prsdb.webapp.testHelpers.builders.UpdateOccupancyJourneyStateSessionBuilder
import uk.gov.communities.prsdb.webapp.testHelpers.builders.UpdateOrganisationTypeJourneyStateSessionBuilder
import java.util.UUID
import kotlin.test.assertTrue
import uk.gov.communities.prsdb.webapp.journeys.organisationalLandlordDeregistration.stepConfig.AreYouSureStep as OrgAreYouSureStep
import uk.gov.communities.prsdb.webapp.journeys.propertyDeregistration.stepConfig.ConfirmStep as DeregistrationConfirmStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.EpcExpiredStep as RegistrationEpcExpiredStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.EpcMissingStep as RegistrationEpcMissingStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LowEnergyRatingStep as RegistrationLowEnergyRatingStep

class Navigator(
    private val page: Page,
    private val port: Int,
) {
    fun goToManageLocalCouncilUsers(councilId: Int): ManageLocalCouncilUsersPage {
        navigate(getManageUsersRoute(councilId, ManageUsersViewType.LocalAuthorityView))
        return createValidPage(page, ManageLocalCouncilUsersPage::class)
    }

    fun goToSystemOperatorManageLocalCouncilUsers(councilId: Int): ManageLocalCouncilUsersPage {
        navigate(getManageUsersRoute(councilId, ManageUsersViewType.SystemOperatorView))
        return createValidPage(page, ManageLocalCouncilUsersPage::class)
    }

    fun goToInviteNewLocalCouncilUser(councilId: Int): InviteNewLocalCouncilUserPage {
        navigate(getInviteNewUserRoute(councilId, ManageUsersViewType.LocalAuthorityView))
        return createValidPage(page, InviteNewLocalCouncilUserPage::class)
    }

    fun goToLandlordSearchPage(): SearchLandlordRegisterPage {
        navigate(SearchRegisterController.SEARCH_LANDLORD_URL)
        return createValidPage(page, SearchLandlordRegisterPage::class)
    }

    fun goToPropertySearchPage(): SearchPropertyRegisterPage {
        navigate(SearchRegisterController.SEARCH_PROPERTY_URL)
        return createValidPage(page, SearchPropertyRegisterPage::class)
    }

    fun goToPasscodeEntryPage(): PasscodeEntryPage {
        navigate(PASSCODE_ENTRY_ROUTE)
        return createValidPage(page, PasscodeEntryPage::class)
    }

    fun navigateToLandlordRegistrationStartPage() {
        navigate(LANDLORD_REGISTRATION_START_PAGE_ROUTE)
    }

    fun goToLandlordRegistrationServiceInformationStartPage(): ServiceInformationStartPageLandlordRegistration {
        navigate(LANDLORD_REGISTRATION_START_PAGE_ROUTE)
        return createValidPage(page, ServiceInformationStartPageLandlordRegistration::class)
    }

    fun skipToLandlordRegistrationLandlordTypePage(): LandlordTypePageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeLandlordType().build())
        navigateToLandlordRegistrationJourneyStep(LandlordTypeStep.ROUTE_SEGMENT)
        return createValidPage(page, LandlordTypePageLandlordRegistration::class)
    }

    fun skipToLandlordRegistrationOrganisationTypePage(): OrgTypeFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgType().build())
        navigateToLandlordRegistrationJourneyStep(OrgTypeStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgTypeFormPageLandlordRegistration::class)
    }

    fun skipToLandlordRegistrationOrgCompanyNumberPage(): OrgCompanyNumberFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgCompanyNumber().build())
        navigateToLandlordRegistrationJourneyStep(OrgCompanyNumberStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgCompanyNumberFormPageLandlordRegistration::class)
    }

    fun skipToLandlordRegistrationOrgCheckAnswersPage(): OrgCheckAnswersPageLandlordRegistration =
        skipToLandlordRegistrationOrgCheckAnswersPage(LandlordStateSessionBuilder.beforeOrgCheckAnswers())

    fun skipToLandlordRegistrationOrgCheckAnswersPageForRegisteredCompany(): OrgCheckAnswersPageLandlordRegistration =
        skipToLandlordRegistrationOrgCheckAnswersPage(
            LandlordStateSessionBuilder
                .beforeOrgCheckAnswers()
                .withOrgIsRegisteredCompany(registeredWithCompaniesHouse = true)
                .withOrgCompanyNumber(),
        )

    private fun skipToLandlordRegistrationOrgCheckAnswersPage(
        stateBuilder: LandlordStateSessionBuilder,
    ): OrgCheckAnswersPageLandlordRegistration {
        setJourneyStateInSession(stateBuilder.build())
        navigateToLandlordRegistrationJourneyStep(AbstractCheckYourAnswersStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgCheckAnswersPageLandlordRegistration::class)
    }

    fun goToLandlordRegistrationPrivacyNoticePage(): PrivacyNoticePageLandlordRegistration {
        navigate("$LANDLORD_REGISTRATION_ROUTE/${PrivacyNoticeStep.ROUTE_SEGMENT}")
        return createValidPage(page, PrivacyNoticePageLandlordRegistration::class)
    }

    fun navigateToLandlordRegistrationPrivacyNoticePage() {
        navigate("$LANDLORD_REGISTRATION_ROUTE/${PrivacyNoticeStep.ROUTE_SEGMENT}")
    }

    fun skipToLandlordRegistrationNamePage(): NameFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeName().build())
        navigateToLandlordRegistrationJourneyStep(NameStep.ROUTE_SEGMENT)
        return createValidPage(page, NameFormPageLandlordRegistration::class)
    }

    fun skipToLandlordRegistrationDateOfBirthPage(): DateOfBirthFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeDob().build())
        navigateToLandlordRegistrationJourneyStep(DateOfBirthStep.ROUTE_SEGMENT)
        return createValidPage(page, DateOfBirthFormPageLandlordRegistration::class)
    }

    fun skipToLandlordRegistrationEmailPage(): EmailFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeEmail().build())
        navigateToLandlordRegistrationJourneyStep(EmailStep.ROUTE_SEGMENT)
        return createValidPage(page, EmailFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationEmailPage(): OrgEmailFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgEmail().build())
        navigateToLandlordRegistrationJourneyStep(OrgEmailStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgEmailFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationPhoneNumberPage(): OrgPhoneNumberFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgPhoneNumber().build())
        navigateToLandlordRegistrationJourneyStep(OrgPhoneNumberStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgPhoneNumberFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationLeadTrusteeEmailPage(): LeadTrusteeEmailFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeLeadTrusteeEmail().build())
        navigateToLandlordRegistrationJourneyStep(LeadTrusteeEmailStep.ROUTE_SEGMENT)
        return createValidPage(page, LeadTrusteeEmailFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationLeadTrusteePhonePage(): LeadTrusteePhoneFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeLeadTrusteePhone().build())
        navigateToLandlordRegistrationJourneyStep(LeadTrusteePhoneStep.ROUTE_SEGMENT)
        return createValidPage(page, LeadTrusteePhoneFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationLeadTrusteeAddressTaskRoute(): LeadTrusteeAddressFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeLeadTrusteeAddress().build())
        navigateToLandlordRegistrationJourneyStep(TrusteeAddressTask.ROUTE_SEGMENT)
        return createValidPage(page, LeadTrusteeAddressFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationLeadTrusteeDobPage(): LeadTrusteeDobFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeLeadTrusteeDob().build())
        navigateToLandlordRegistrationJourneyStep(LeadTrusteeDobStep.ROUTE_SEGMENT)
        return createValidPage(page, LeadTrusteeDobFormPageLandlordRegistration::class)
    }

    fun skipToLandlordRegistrationPhoneNumberPage(): PhoneNumberFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforePhoneNumber().build())
        navigateToLandlordRegistrationJourneyStep(PhoneNumberStep.ROUTE_SEGMENT)
        return createValidPage(page, PhoneNumberFormPageLandlordRegistration::class)
    }

    fun skipToLandlordRegistrationCountryOfResidencePage(): CountryOfResidenceFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeCountryOfResidence().build())
        navigateToLandlordRegistrationJourneyStep(CountryOfResidenceStep.ROUTE_SEGMENT)
        return createValidPage(page, CountryOfResidenceFormPageLandlordRegistration::class)
    }

    fun skipToLandlordRegistrationLookupAddressPage(): LookupAddressFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeLookupAddress().build())
        navigateToLandlordRegistrationJourneyStep(LookupAddressStep.ROUTE_SEGMENT)
        return createValidPage(page, LookupAddressFormPageLandlordRegistration::class)
    }

    fun skipToLandlordRegistrationSelectAddressPage(): SelectAddressFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeSelectAddress().build())
        navigateToLandlordRegistrationJourneyStep(SelectAddressStep.ROUTE_SEGMENT)
        return createValidPage(page, SelectAddressFormPageLandlordRegistration::class)
    }

    fun skipToLandlordRegistrationManualAddressPage(): ManualAddressFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeManualAddress().build())
        navigateToLandlordRegistrationJourneyStep(ManualAddressStep.ROUTE_SEGMENT)
        return createValidPage(page, ManualAddressFormPageLandlordRegistration::class)
    }

    fun skipToLandlordRegistrationCheckAnswersPage(
        stateBuilder: LandlordStateSessionBuilder = LandlordStateSessionBuilder.beforeCheckAnswers(),
    ): CheckAnswersPageLandlordRegistration {
        setJourneyStateInSession(stateBuilder.build())
        navigateToLandlordRegistrationJourneyStep(AbstractCheckYourAnswersStep.ROUTE_SEGMENT)
        return createValidPage(page, CheckAnswersPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationOrgNamePage(): OrgNameFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgName().build())
        navigateToLandlordRegistrationJourneyStep(OrgNameStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgNameFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationOrgAddressPage(): OrgAddressFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgAddress().build())
        navigateToLandlordRegistrationJourneyStep("${OrgAddressTask.ROUTE_SEGMENT}/${LookupAddressStep.ROUTE_SEGMENT}")
        return createValidPage(page, OrgAddressFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationManualAddressPage(): OrgManualAddressFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgManualAddress().build())
        navigateToLandlordRegistrationJourneyStep("${OrgAddressTask.ROUTE_SEGMENT}/${ManualAddressStep.ROUTE_SEGMENT}")
        return createValidPage(page, OrgManualAddressFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationMainContactPage(
        stateBuilder: LandlordStateSessionBuilder = LandlordStateSessionBuilder.beforeOrgMainContact(),
    ): OrgMainContactFormPageLandlordRegistration {
        setJourneyStateInSession(stateBuilder.build())
        navigateToLandlordRegistrationJourneyStep(OrgMainContactStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgMainContactFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationCheckAnswersPage(
        stateBuilder: LandlordStateSessionBuilder = LandlordStateSessionBuilder.beforeOrgCheckAnswers(),
    ): OrgCheckAnswersPageLandlordRegistration {
        setJourneyStateInSession(stateBuilder.build())
        navigateToLandlordRegistrationJourneyStep(AbstractCheckYourAnswersStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgCheckAnswersPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationMustProvideInfoPage(): OrgGovBodyMustProvideInfoFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgGovBodyMustProvideInfo().build())
        navigateToLandlordRegistrationJourneyStep(OrgGovBodyMustProvideInfoStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgGovBodyMustProvideInfoFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationLeadTrusteeNamePage(): LeadTrusteeNameFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeLeadTrusteeName().build())
        navigateToLandlordRegistrationJourneyStep(LeadTrusteeNameStep.ROUTE_SEGMENT)
        return createValidPage(page, LeadTrusteeNameFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationGovBodyMemberNamePage(): OrgGovBodyMemberNameFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgGovBodyMemberName().build())
        navigateToLandlordRegistrationJourneyStep(OrgGovBodyMemberNameStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgGovBodyMemberNameFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationGovBodyMemberListPage(
        members: Map<Int, GoverningBodyMemberDataModel>,
    ): OrgGovBodyMemberListFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgGovBodyMemberList(members).build())
        navigateToLandlordRegistrationJourneyStep(OrgGovBodyMemberListStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgGovBodyMemberListFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationIsRegisteredCompanyPage(): OrgIsRegisteredCompanyFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgIsRegisteredCompany().build())
        navigateToLandlordRegistrationJourneyStep(OrgIsRegisteredCompanyStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgIsRegisteredCompanyFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationIsRegisteredCharityPage(): OrgIsRegisteredCharityFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgIsRegisteredCharity().build())
        navigateToLandlordRegistrationJourneyStep(OrgIsRegisteredCharityStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgIsRegisteredCharityFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationCharityRegisteredWithPage(): OrgCharityRegisteredWithFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgCharityRegisteredWith().build())
        navigateToLandlordRegistrationJourneyStep(OrgCharityRegisteredWithStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgCharityRegisteredWithFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationCharityNumberEnglandAndWalesPage(): OrgCharityNumberEnglandAndWalesFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgCharityNumberEnglandAndWales().build())
        navigateToLandlordRegistrationJourneyStep(OrgCharityNumberEnglandAndWalesStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgCharityNumberEnglandAndWalesFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationCharityNumberNorthernIrelandPage(): OrgCharityNumberNorthernIrelandFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgCharityNumberNorthernIreland().build())
        navigateToLandlordRegistrationJourneyStep(OrgCharityNumberNorthernIrelandStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgCharityNumberNorthernIrelandFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationCharityNumberScotlandPage(): OrgCharityNumberScotlandFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgCharityNumberScotland().build())
        navigateToLandlordRegistrationJourneyStep(OrgCharityNumberScotlandStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgCharityNumberScotlandFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationGovBodyDetailsPage(): OrgGovBodyDetailsFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgGovBodyDetails().build())
        navigateToLandlordRegistrationJourneyStep(OrgGovBodyDetailsStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgGovBodyDetailsFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationGovBodyMemberLookupAddressPage(): OrgGovBodyMemberLookupAddressFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgGovBodyMemberAddress().build())
        navigateToLandlordRegistrationJourneyStep(
            "${GovBodyMemberAddressTask.ROUTE_SEGMENT}/${LookupAddressStep.ROUTE_SEGMENT}",
        )
        return createValidPage(page, OrgGovBodyMemberLookupAddressFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationGovBodyMemberSelectAddressPage(): OrgGovBodyMemberSelectAddressFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgGovBodyMemberSelectAddress().build())
        navigateToLandlordRegistrationJourneyStep(
            "${GovBodyMemberAddressTask.ROUTE_SEGMENT}/${SelectAddressStep.ROUTE_SEGMENT}",
        )
        return createValidPage(page, OrgGovBodyMemberSelectAddressFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationGovBodyWhoToProvidePage(): OrgGovBodyWhoToProvideFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgGovBodyWhoToProvide().build())
        navigateToLandlordRegistrationJourneyStep(OrgGovBodyWhoToProvideStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgGovBodyWhoToProvideFormPageLandlordRegistration::class)
    }

    fun skipToOrgLandlordRegistrationGovBodyMemberDobPage(): OrgGovBodyMemberDobFormPageLandlordRegistration {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeOrgGovBodyMemberDob().build())
        navigateToLandlordRegistrationJourneyStep(OrgGovBodyMemberDobStep.ROUTE_SEGMENT)
        return createValidPage(page, OrgGovBodyMemberDobFormPageLandlordRegistration::class)
    }

    fun navigateToLandlordRegistrationConfirmationPage() {
        navigate("$LANDLORD_REGISTRATION_ROUTE/$CONFIRMATION_PATH_SEGMENT")
    }

    fun navigateToLocalCouncilUserRegistrationAcceptInvitationRoute(token: String) {
        navigate("${RegisterLocalCouncilUserController.LOCAL_COUNCIL_USER_REGISTRATION_ROUTE}?$TOKEN=$token")
    }

    fun navigateToLocalCouncilUserRegistrationLandingPage(token: UUID) {
        storeInvitationTokenInSession(token)
        setJourneyStateInSession(
            LocalCouncilUserRegistrationStateSessionBuilder.beforeLandingPage().build(),
        )
        navigateToLocalCouncilUserRegistrationJourneyStep("landing-page")
    }

    fun skipToLocalCouncilUserRegistrationPrivacyNoticePage(token: UUID): PrivacyNoticePageLocalCouncilUserRegistration {
        storeInvitationTokenInSession(token)
        setJourneyStateInSession(
            LocalCouncilUserRegistrationStateSessionBuilder.beforePrivacyNotice().build(),
        )
        navigateToLocalCouncilUserRegistrationJourneyStep("privacy-notice")
        return createValidPage(page, PrivacyNoticePageLocalCouncilUserRegistration::class)
    }

    fun skipToLocalCouncilUserRegistrationNameFormPage(token: UUID): NameFormPageLocalCouncilUserRegistration {
        storeInvitationTokenInSession(token)
        setJourneyStateInSession(
            LocalCouncilUserRegistrationStateSessionBuilder.beforeName().build(),
        )
        navigateToLocalCouncilUserRegistrationJourneyStep("name")
        return createValidPage(page, NameFormPageLocalCouncilUserRegistration::class)
    }

    fun skipToLocalCouncilUserRegistrationEmailFormPage(token: UUID): EmailFormPageLocalCouncilUserRegistration {
        storeInvitationTokenInSession(token)
        setJourneyStateInSession(
            LocalCouncilUserRegistrationStateSessionBuilder.beforeEmail().build(),
        )
        navigateToLocalCouncilUserRegistrationJourneyStep("email")
        return createValidPage(page, EmailFormPageLocalCouncilUserRegistration::class)
    }

    fun skipToLocalCouncilUserRegistrationCheckAnswersPage(token: UUID): CheckAnswersPageLocalCouncilUserRegistration {
        storeInvitationTokenInSession(token)
        setJourneyStateInSession(
            LocalCouncilUserRegistrationStateSessionBuilder.beforeCheckAnswers().build(),
        )
        navigateToLocalCouncilUserRegistrationJourneyStep("check-answers")
        return createValidPage(page, CheckAnswersPageLocalCouncilUserRegistration::class)
    }

    fun navigateToLocalCouncilUserRegistrationConfirmationPage() {
        navigate("${RegisterLocalCouncilUserController.LOCAL_COUNCIL_USER_REGISTRATION_ROUTE}/$CONFIRMATION_PATH_SEGMENT")
    }

    private fun navigateToLocalCouncilUserRegistrationJourneyStep(stepName: String) {
        navigate(
            "${RegisterLocalCouncilUserController.LOCAL_COUNCIL_USER_REGISTRATION_ROUTE}/$stepName?journeyId=$TEST_JOURNEY_ID",
        )
    }

    fun goToPropertyRegistrationStartPage(): RegisterPropertyStartPage {
        navigateToPropertyRegistrationJourneyStep()
        return createValidPage(page, RegisterPropertyStartPage::class)
    }

    fun goToPropertyRegistrationTaskList(): TaskListPagePropertyRegistration {
        navigateToPropertyRegistrationJourneyStep(TASK_LIST_PATH_SEGMENT)
        return createValidPage(page, TaskListPagePropertyRegistration::class)
    }

    fun navigateToPropertyRegistrationCheckYourAnswers() =
        navigateToPropertyRegistrationJourneyStep(
            PropertyRegistrationCyaStep.ROUTE_SEGMENT,
        )

    fun goToRestructuredPropertyRegistrationTaskList(stateBuilder: PropertyStateSessionBuilder): TaskListPagePropertyRegistration {
        setJourneyStateInSession(stateBuilder.build())
        navigateToPropertyRegistrationJourneyStep(TASK_LIST_PATH_SEGMENT)
        return createValidPage(page, TaskListPagePropertyRegistration::class)
    }

    fun goToRestructuredPropertyRegistrationTaskListUnoccupied(): TaskListPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder
                .beforePropertyRegistrationCheckAnswers()
                // The restructured "Property details" task includes the number of bedrooms for all properties, so it
                // must be set for that task to be complete even when the property is unoccupied.
                .withBedrooms()
                .withAdditionalData("cachedOccupied", "false")
                .build(),
        )
        navigateToPropertyRegistrationJourneyStep(TASK_LIST_PATH_SEGMENT)
        return createValidPage(page, TaskListPagePropertyRegistration::class)
    }

    fun goToPropertyRegistrationLookupAddressPage(): LookupAddressFormPagePropertyRegistration {
        navigateToPropertyRegistrationJourneyStep(LookupAddressStep.ROUTE_SEGMENT)
        return createValidPage(page, LookupAddressFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationSelectAddressPage(
        customLookedUpAddresses: List<AddressDataModel>? = null,
    ): SelectAddressFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationSelectAddress(customLookedUpAddresses).build(),
        )
        navigateToPropertyRegistrationJourneyStep(SelectAddressStep.ROUTE_SEGMENT)
        return createValidPage(page, SelectAddressFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationManualAddressPage(): ManualAddressFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationManualAddress().build(),
        )
        navigateToPropertyRegistrationJourneyStep(ManualAddressStep.ROUTE_SEGMENT)
        return createValidPage(page, ManualAddressFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationSelectLocalCouncilPage(): SelectLocalCouncilFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationSelectLocalCouncil().build(),
        )
        navigateToPropertyRegistrationJourneyStep(LocalCouncilStep.ROUTE_SEGMENT)
        return createValidPage(page, SelectLocalCouncilFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationPropertyTypePage(): PropertyTypeFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationPropertyType().build(),
        )
        navigateToPropertyRegistrationJourneyStep(PropertyTypeStep.ROUTE_SEGMENT)
        return createValidPage(page, PropertyTypeFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationOwnershipTypePage(): OwnershipTypeFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationOwnershipType().withBedrooms().build(),
        )
        navigateToPropertyRegistrationJourneyStep(OwnershipTypeStep.ROUTE_SEGMENT)
        return createValidPage(page, OwnershipTypeFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationLicensingTypePage(): LicensingTypeFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationLicensingType().build(),
        )
        navigateToPropertyRegistrationJourneyStep(LicensingTypeStep.ROUTE_SEGMENT)
        return createValidPage(page, LicensingTypeFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationOccupiedLicensingTypePage(): LicensingTypeFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationOccupiedLicensingType().build(),
        )
        navigateToPropertyRegistrationJourneyStep(LicensingTypeStep.ROUTE_SEGMENT)
        return createValidPage(page, LicensingTypeFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationSelectiveLicencePage(): SelectiveLicenceFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationSelectiveLicence().build(),
        )
        navigateToPropertyRegistrationJourneyStep(SelectiveLicenceStep.ROUTE_SEGMENT)
        return createValidPage(page, SelectiveLicenceFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationOccupiedSelectiveLicencePage(): SelectiveLicenceFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationRentedOutSelectiveLicence().build(),
        )
        navigateToPropertyRegistrationJourneyStep(SelectiveLicenceStep.ROUTE_SEGMENT)
        return createValidPage(page, SelectiveLicenceFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationHmoMandatoryLicencePage(): HmoMandatoryLicenceFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationHmoMandatoryLicence().build(),
        )
        navigateToPropertyRegistrationJourneyStep(HmoMandatoryLicenceStep.ROUTE_SEGMENT)
        return createValidPage(page, HmoMandatoryLicenceFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationOccupiedHmoMandatoryLicencePage(): HmoMandatoryLicenceFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationRentedOutHmoMandatoryLicence().build(),
        )
        navigateToPropertyRegistrationJourneyStep(HmoMandatoryLicenceStep.ROUTE_SEGMENT)
        return createValidPage(page, HmoMandatoryLicenceFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationOccupiedHmoAdditionalLicencePage(): HmoAdditionalLicenceFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationRentedOutHmoAdditionalLicence().build(),
        )
        navigateToPropertyRegistrationJourneyStep(HmoAdditionalLicenceStep.ROUTE_SEGMENT)
        return createValidPage(page, HmoAdditionalLicenceFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationHmoAdditionalLicencePage(): HmoAdditionalLicenceFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationHmoAdditionalLicence().build(),
        )
        navigateToPropertyRegistrationJourneyStep(HmoAdditionalLicenceStep.ROUTE_SEGMENT)
        return createValidPage(page, HmoAdditionalLicenceFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationOccupancyPage(): OccupancyFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationOccupancy().build(),
        )
        navigateToPropertyRegistrationJourneyStep(OccupiedStep.ROUTE_SEGMENT)
        return createValidPage(page, OccupancyFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationRestructuredOccupancyPage(): OccupancyFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationRestructuredOccupancy().build(),
        )
        navigateToPropertyRegistrationJourneyStep(OccupiedStep.ROUTE_SEGMENT)
        return createValidPage(page, OccupancyFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationHouseholdsPage(): NumberOfHouseholdsFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationHouseholds().build(),
        )
        navigateToPropertyRegistrationJourneyStep(HouseholdStep.ROUTE_SEGMENT)
        return createValidPage(page, NumberOfHouseholdsFormPagePropertyRegistration::class)
    }

    fun skipToTenancyDetailsHouseholdsPage(): NumberOfHouseholdsFormPagePropertyRegistration {
        val taskListPage =
            goToRestructuredPropertyRegistrationTaskList(
                PropertyStateSessionBuilder.beforeTenancyDetails(),
            )
        taskListPage.clickRentedOutTaskWithName("Tenancy details")
        return createValidPage(page, NumberOfHouseholdsFormPagePropertyRegistration::class)
    }

    fun skipToTenancyDetailsProvideTenancyDetailsLaterPage(): ProvideTenancyDetailsLaterFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforeTenancyDetails().withProvideTenancyDetailsLater().build(),
        )
        navigateToPropertyRegistrationJourneyStep(ProvideTenancyDetailsLaterStep.ROUTE_SEGMENT)
        return createValidPage(page, ProvideTenancyDetailsLaterFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationPeoplePage(): NumberOfPeopleFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationPeople().build(),
        )
        navigateToPropertyRegistrationJourneyStep(TenantsStep.ROUTE_SEGMENT)
        return createValidPage(page, NumberOfPeopleFormPagePropertyRegistration::class)
    }

    fun skipToTenancyDetailsPeoplePage(): NumberOfPeopleFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforeTenancyDetailsHouseholds().build(),
        )
        navigateToPropertyRegistrationJourneyStep(TenantsStep.ROUTE_SEGMENT)
        return createValidPage(page, NumberOfPeopleFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationBedroomsPage(): NumberOfBedroomsFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationBedrooms().build(),
        )
        navigateToPropertyRegistrationJourneyStep(BedroomsStep.ROUTE_SEGMENT)
        return createValidPage(page, NumberOfBedroomsFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationRentIncludesBillsPage(): RentIncludesBillsFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationRentIncludesBills().build(),
        )
        navigateToPropertyRegistrationJourneyStep(RentIncludesBillsStep.ROUTE_SEGMENT)
        return createValidPage(page, RentIncludesBillsFormPagePropertyRegistration::class)
    }

    fun skipToTenancyDetailsRentIncludesBillsPage(): RentIncludesBillsFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforeTenancyDetailsRentIncludesBills().build(),
        )
        navigateToPropertyRegistrationJourneyStep(RentIncludesBillsStep.ROUTE_SEGMENT)
        return createValidPage(page, RentIncludesBillsFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationBillsIncludedPage(): BillsIncludedFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationBillsIncluded().build(),
        )
        navigateToPropertyRegistrationJourneyStep(BillsIncludedStep.ROUTE_SEGMENT)
        return createValidPage(page, BillsIncludedFormPagePropertyRegistration::class)
    }

    fun skipToTenancyDetailsBillsIncludedPage(): BillsIncludedFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforeTenancyDetailsBillsIncluded().build(),
        )
        navigateToPropertyRegistrationJourneyStep(BillsIncludedStep.ROUTE_SEGMENT)
        return createValidPage(page, BillsIncludedFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationFurnishedStatusPage(): FurnishedStatusFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationFurnished().build(),
        )
        navigateToPropertyRegistrationJourneyStep(FurnishedStatusStep.ROUTE_SEGMENT)
        return createValidPage(page, FurnishedStatusFormPagePropertyRegistration::class)
    }

    fun skipToTenancyDetailsFurnishedStatusPage(): FurnishedStatusFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforeTenancyDetailsFurnished().build(),
        )
        navigateToPropertyRegistrationJourneyStep(FurnishedStatusStep.ROUTE_SEGMENT)
        return createValidPage(page, FurnishedStatusFormPagePropertyRegistration::class)
    }

    fun skipToTenancyDetailsRentFrequencyPage(): RentFrequencyFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforeTenancyDetailsRentFrequency().build(),
        )
        navigateToPropertyRegistrationJourneyStep(RentFrequencyStep.ROUTE_SEGMENT)
        return createValidPage(page, RentFrequencyFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationRentFrequencyPage(): RentFrequencyFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationRentFrequency().build(),
        )
        navigateToPropertyRegistrationJourneyStep(RentFrequencyStep.ROUTE_SEGMENT)
        return createValidPage(page, RentFrequencyFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationRentAmountPage(
        rentFrequency: RentFrequency = RentFrequency.MONTHLY,
    ): RentAmountFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationRentAmount(rentFrequency).build(),
        )
        navigateToPropertyRegistrationJourneyStep(RentAmountStep.ROUTE_SEGMENT)
        return createValidPage(page, RentAmountFormPagePropertyRegistration::class)
    }

    fun skipToTenancyDetailsRentAmountPage(rentFrequency: RentFrequency = RentFrequency.MONTHLY): RentAmountFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforeTenancyDetailsRentAmount(rentFrequency).build(),
        )
        navigateToPropertyRegistrationJourneyStep(RentAmountStep.ROUTE_SEGMENT)
        return createValidPage(page, RentAmountFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationHasJointLandlordsPage(): HasJointLandlordsFormBasePagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationHasJointLandlords().build(),
        )
        navigateToPropertyRegistrationJourneyStep(HasJointLandlordsStep.ROUTE_SEGMENT)
        return createValidPage(page, HasJointLandlordsFormBasePagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationInviteJointLandlordPage(): InviteJointLandlordFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationInviteJointLandlords().build(),
        )
        navigateToPropertyRegistrationJourneyStep(InviteJointLandlordStep.INVITE_FIRST_ROUTE_SEGMENT)
        return createValidPage(page, InviteJointLandlordFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationInviteAnotherJointLandlordPage(
        alreadyInvitedEmails: MutableList<String>? = null,
    ): InviteAnotherJointLandlordFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationInviteJointLandlords(alreadyInvitedEmails).build(),
        )
        navigateToPropertyRegistrationJourneyStep(InviteJointLandlordStep.INVITE_ANOTHER_ROUTE_SEGMENT)
        return createValidPage(page, InviteAnotherJointLandlordFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationHasGasSupplyPage(propertyIsOccupied: Boolean = true): HasGasSupplyFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationHasGasSupply(propertyIsOccupied).build(),
        )
        navigateToPropertyRegistrationJourneyStep(HasGasSupplyStep.ROUTE_SEGMENT)
        return createValidPage(page, HasGasSupplyFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationHasGasCertPage(): HasGasCertFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationHasGasCert().build(),
        )
        navigateToPropertyRegistrationJourneyStep(HasGasCertStep.ROUTE_SEGMENT)
        return createValidPage(page, HasGasCertFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationGasCertIssueDatePage(): GasCertIssueDateFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationGasCertIssueDate().build(),
        )
        navigateToPropertyRegistrationJourneyStep(GasCertIssueDateStep.ROUTE_SEGMENT)
        return createValidPage(page, GasCertIssueDateFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationCheckGasSafetyAnswersPage(
        stateBuilder: PropertyStateSessionBuilder,
    ): CheckGasSafetyAnswersFormPagePropertyRegistration {
        setJourneyStateInSession(stateBuilder.build())
        navigateToPropertyRegistrationJourneyStep(CheckGasSafetyAnswersStep.ROUTE_SEGMENT)
        return createValidPage(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationHasElectricalCertPage(): HasElectricalCertFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationHasElectricalCert().build(),
        )
        navigateToPropertyRegistrationJourneyStep(HasElectricalCertStep.ROUTE_SEGMENT)
        return createValidPage(page, HasElectricalCertFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationElectricalCertExpiryDatePage(): ElectricalCertExpiryDateFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationEicExpiryDate().build(),
        )
        navigateToPropertyRegistrationJourneyStep(ElectricalCertExpiryDateStep.ROUTE_SEGMENT)
        return createValidPage(page, ElectricalCertExpiryDateFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationCheckElectricalSafetyAnswersPage(
        stateBuilder: PropertyStateSessionBuilder,
    ): CheckElectricalSafetyAnswersFormPagePropertyRegistration {
        setJourneyStateInSession(stateBuilder.build())
        navigateToPropertyRegistrationJourneyStep(CheckElectricalSafetyAnswersStep.ROUTE_SEGMENT)
        return createValidPage(page, CheckElectricalSafetyAnswersFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationHasEpcPage(): HasEpcFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationHasEpc().build(),
        )
        navigateToPropertyRegistrationJourneyStep(HasEpcStep.ROUTE_SEGMENT)
        return createValidPage(page, HasEpcFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationFindYourEpcPage(propertyIsOccupied: Boolean = true): FindYourEpcFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationFindYourEpc(propertyIsOccupied).build(),
        )
        navigateToPropertyRegistrationJourneyStep(FindYourEpcStep.ROUTE_SEGMENT)
        return createValidPage(page, FindYourEpcFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationConfirmEpcDetailsRetrievedByCertificateNumberPage():
        ConfirmEpcDetailsRetrievedByCertificateNumberPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder
                .beforePropertyRegistrationConfirmEpcDetailsRetrievedByCertificateNumber()
                .build(),
        )
        navigateToPropertyRegistrationJourneyStep(ConfirmEpcDetailsRetrievedByCertificateNumberStep.ROUTE_SEGMENT)
        return createValidPage(page, ConfirmEpcDetailsRetrievedByCertificateNumberPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationIsEpcRequiredPage(): IsEpcRequiredFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationIsEpcRequired().build(),
        )
        navigateToPropertyRegistrationJourneyStep(IsEpcRequiredStep.ROUTE_SEGMENT)
        return createValidPage(page, IsEpcRequiredFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationConfirmEpcDetailsByUprnPage(): ConfirmEpcDetailsRetrievedByUprnFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationConfirmEpcDetailsByUprn().build(),
        )
        navigateToPropertyRegistrationJourneyStep(ConfirmEpcRetrievedByUprnStep.ROUTE_SEGMENT)
        return createValidPage(page, ConfirmEpcDetailsRetrievedByUprnFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationEpcInDateAtStartOfTenancyCheckPage(): EpcInDateAtStartOfTenancyCheckPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationEpcInDateAtStartOfTenancyCheck().build(),
        )
        navigateToPropertyRegistrationJourneyStep(EpcInDateAtStartOfTenancyCheckStep.ROUTE_SEGMENT)
        return createValidPage(page, EpcInDateAtStartOfTenancyCheckPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationHasMeesExemptionPage(): HasMeesExemptionFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationHasMeesExemption().build(),
        )
        navigateToPropertyRegistrationJourneyStep(HasMeesExemptionStep.ROUTE_SEGMENT)
        return createValidPage(page, HasMeesExemptionFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationMeesExemptionPage(): MeesExemptionFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationMeesExemptionReason().build(),
        )
        navigateToPropertyRegistrationJourneyStep(MeesExemptionStep.ROUTE_SEGMENT)
        return createValidPage(page, MeesExemptionFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationEpcExemptionPage(): EpcExemptionFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationEpcExemption().build(),
        )
        navigateToPropertyRegistrationJourneyStep(EpcExemptionStep.ROUTE_SEGMENT)
        return createValidPage(page, EpcExemptionFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationProvideEpcLaterPage(propertyIsOccupied: Boolean = true): ProvideEpcLaterFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationProvideEpcLater(propertyIsOccupied).build(),
        )
        navigateToPropertyRegistrationJourneyStep(ProvideEpcLaterStep.ROUTE_SEGMENT)
        return createValidPage(page, ProvideEpcLaterFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationEpcExpiredPage(propertyIsOccupied: Boolean = true): EpcExpiredFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationEpcExpired(propertyIsOccupied).build(),
        )
        navigateToPropertyRegistrationJourneyStep(RegistrationEpcExpiredStep.ROUTE_SEGMENT)
        return createValidPage(page, EpcExpiredFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationEpcMissingPage(propertyIsOccupied: Boolean = true): EpcMissingFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationEpcMissing(propertyIsOccupied).build(),
        )
        navigateToPropertyRegistrationJourneyStep(RegistrationEpcMissingStep.ROUTE_SEGMENT)
        return createValidPage(page, EpcMissingFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationLowEnergyRatingPage(propertyIsOccupied: Boolean = true): LowEnergyRatingFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationLowEnergyRating(propertyIsOccupied).build(),
        )
        navigateToPropertyRegistrationJourneyStep(RegistrationLowEnergyRatingStep.ROUTE_SEGMENT)
        return createValidPage(page, LowEnergyRatingFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationCheckEpcAnswers(stateBuilder: PropertyStateSessionBuilder): CheckEpcAnswersFormPagePropertyRegistration {
        setJourneyStateInSession(stateBuilder.build())
        navigateToPropertyRegistrationJourneyStep(CheckEpcAnswersStep.ROUTE_SEGMENT)
        return createValidPage(page, CheckEpcAnswersFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationConfirmMissingCompliancePage(): ConfirmMissingComplianceFormPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationConfirmMissingCompliance().build(),
        )
        navigateToPropertyRegistrationJourneyStep(ConfirmMissingComplianceStep.ROUTE_SEGMENT)
        return createValidPage(page, ConfirmMissingComplianceFormPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationCheckAnswersPage(): CheckAnswersPagePropertyRegistration {
        setJourneyStateInSession(PropertyStateSessionBuilder.beforePropertyRegistrationCheckAnswers().build())
        navigateToPropertyRegistrationJourneyStep(PropertyRegistrationCyaStep.ROUTE_SEGMENT)
        return createValidPage(page, CheckAnswersPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationCheckAnswersPageWithJointLandlords(): CheckAnswersPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationCheckAnswersWithJointLandlords().build(),
        )
        navigateToPropertyRegistrationJourneyStep(PropertyRegistrationCyaStep.ROUTE_SEGMENT)
        return createValidPage(page, CheckAnswersPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationCheckAnswersPageWithSelectiveLicence(): CheckAnswersPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationCheckAnswersWithSelectiveLicence().build(),
        )
        navigateToPropertyRegistrationJourneyStep(PropertyRegistrationCyaStep.ROUTE_SEGMENT)
        return createValidPage(page, CheckAnswersPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationCheckAnswersPageWithProvideTenancyDetailsLater(): CheckAnswersPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationCheckAnswersWithProvideTenancyDetailsLater().build(),
        )
        navigateToPropertyRegistrationJourneyStep(PropertyRegistrationCyaStep.ROUTE_SEGMENT)
        return createValidPage(page, CheckAnswersPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationCheckAnswersPageOccupied(
        households: Int = 2,
        people: Int = 4,
        bedrooms: Int = 3,
        rentAmount: String = "400",
        billsIncluded: Boolean = true,
    ): CheckAnswersPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder
                .beforePropertyRegistrationCheckAnswersOccupied(
                    households = households,
                    people = people,
                    bedrooms = bedrooms,
                    rentAmount = rentAmount,
                    billsIncluded = billsIncluded,
                ).build(),
        )
        navigateToPropertyRegistrationJourneyStep(PropertyRegistrationCyaStep.ROUTE_SEGMENT)
        return createValidPage(page, CheckAnswersPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationCheckAnswersPageUnoccupiedWithTenancyDetails(): CheckAnswersPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder
                .beforePropertyRegistrationCheckAnswersOccupied()
                .withOccupancyStatus(false)
                .build(),
        )
        navigateToPropertyRegistrationJourneyStep(PropertyRegistrationCyaStep.ROUTE_SEGMENT)
        return createValidPage(page, CheckAnswersPagePropertyRegistration::class)
    }

    fun skipToPropertyRegistrationCheckAnswersPageNoEpc(): CheckAnswersPagePropertyRegistration {
        setJourneyStateInSession(
            PropertyStateSessionBuilder.beforePropertyRegistrationCheckAnswersNoEpcExempt().build(),
        )
        navigateToPropertyRegistrationJourneyStep(PropertyRegistrationCyaStep.ROUTE_SEGMENT)
        return createValidPage(page, CheckAnswersPagePropertyRegistration::class)
    }

    private fun navigateToPropertyRegistrationJourneyStep(segment: String? = "") =
        navigate("${RegisterPropertyController.PROPERTY_REGISTRATION_ROUTE}/$segment?journeyId=$TEST_JOURNEY_ID")

    fun navigateToPropertyRegistrationConfirmationPage() {
        navigate("${RegisterPropertyController.PROPERTY_REGISTRATION_ROUTE}/$CONFIRMATION_PATH_SEGMENT")
    }

    fun goToLandlordDetails(): LandlordDetailsPage {
        navigate(LandlordDetailsController.LANDLORD_DETAILS_FOR_LANDLORD_ROUTE)
        return createValidPage(page, LandlordDetailsPage::class)
    }

    fun goToOrgLandlordDetails(): OrgLandlordDetailsPage {
        navigate(LandlordDetailsController.LANDLORD_DETAILS_FOR_LANDLORD_ROUTE)
        return createValidPage(page, OrgLandlordDetailsPage::class)
    }

    fun goToLandlordDetailsAsALocalCouncilUser(id: Long): LocalCouncilViewLandlordDetailsPage {
        navigate(LandlordDetailsController.getLandlordDetailsForLocalCouncilUserPath(id))
        return createValidPage(page, LocalCouncilViewLandlordDetailsPage::class, mapOf("id" to id.toString()))
    }

    fun goToOrgLandlordDetailsAsALocalCouncilUser(id: Long): LocalCouncilViewOrgLandlordDetailsPage {
        navigate(LandlordDetailsController.getLandlordDetailsForLocalCouncilUserPath(id))
        return createValidPage(page, LocalCouncilViewOrgLandlordDetailsPage::class, mapOf("id" to id.toString()))
    }

    fun goToUpdateLandlordDetailsUpdateLookupAddressPage(): LookupAddressFormPageUpdateLandlordDetails {
        navigate("${UpdateLandlordAddressController.UPDATE_ADDRESS_ROUTE}/${LookupAddressStep.ROUTE_SEGMENT}")
        return createValidPage(page, LookupAddressFormPageUpdateLandlordDetails::class)
    }

    fun skipToLandlordDetailsUpdateSelectAddressPage(): SelectAddressFormPageUpdateLandlordDetails {
        setJourneyStateInSession(LandlordStateSessionBuilder.beforeSelectAddress().build())
        navigate(
            JourneyStateService.urlWithJourneyState(
                "${UpdateLandlordAddressController.UPDATE_ADDRESS_ROUTE}/${SelectAddressStep.ROUTE_SEGMENT}",
                TEST_JOURNEY_ID,
            ),
        )
        return createValidPage(page, SelectAddressFormPageUpdateLandlordDetails::class)
    }

    fun navigateToLandlordDetailsUpdateNamePage() {
        navigate("${UpdateLandlordNameController.UPDATE_NAME_ROUTE}/${NameStep.ROUTE_SEGMENT}")
    }

    fun navigateToLandlordDetailsUpdateDateOfBirthPage() {
        navigate("${UpdateLandlordDateOfBirthController.UPDATE_DATE_OF_BIRTH_ROUTE}/${DateOfBirthStep.ROUTE_SEGMENT}")
    }

    fun skipToUpdateOrgTypeTrustInterruptionPage(orgTypes: List<OrgType>): OrgTypeTrustInterruptionPageUpdateOrganisationType {
        setJourneyStateInSession(
            UpdateOrganisationTypeJourneyStateSessionBuilder.beforeTrustInterruption(orgTypes).build(),
        )
        navigate(
            JourneyStateService.urlWithJourneyState(
                "${UpdateOrganisationTypeController.UPDATE_ORG_TYPE_ROUTE}/${OrgTypeTrustInterruptionStep.ROUTE_SEGMENT}",
                TEST_JOURNEY_ID,
            ),
        )
        return createValidPage(page, OrgTypeTrustInterruptionPageUpdateOrganisationType::class)
    }

    fun skipToUpdateOrgTypeCyaPageTrustUnchanged(orgTypes: List<OrgType>): OrgTypeCyaPageUpdateOrganisationType {
        setJourneyStateInSession(
            UpdateOrganisationTypeJourneyStateSessionBuilder.beforeCyaTrustUnchanged(orgTypes).build(),
        )
        navigate(
            JourneyStateService.urlWithJourneyState(
                "${UpdateOrganisationTypeController.UPDATE_ORG_TYPE_ROUTE}/${OrgTypeCyaStep.ROUTE_SEGMENT}",
                TEST_JOURNEY_ID,
            ),
        )
        return createValidPage(page, OrgTypeCyaPageUpdateOrganisationType::class)
    }

    fun skipToUpdateOrgTypeCyaPageAddingTrust(
        orgTypes: List<OrgType> = listOf(OrgType.COMPANY, OrgType.TRUST),
        trusteeName: String = "Lead Trustee",
        trusteeEmail: String = "trustee@test.com",
        trusteePhone: String = "07123456789",
    ): OrgTypeCyaPageUpdateOrganisationType {
        setJourneyStateInSession(
            UpdateOrganisationTypeJourneyStateSessionBuilder
                .beforeCyaAddingTrust(
                    orgTypes,
                    trusteeName,
                    trusteeEmail,
                    trusteePhone,
                ).build(),
        )
        navigate(
            JourneyStateService.urlWithJourneyState(
                "${UpdateOrganisationTypeController.UPDATE_ORG_TYPE_ROUTE}/${OrgTypeCyaStep.ROUTE_SEGMENT}",
                TEST_JOURNEY_ID,
            ),
        )
        return createValidPage(page, OrgTypeCyaPageUpdateOrganisationType::class)
    }

    fun goToPropertyDetailsLandlordView(id: Long): PropertyDetailsPageLandlordView {
        navigate(PropertyDetailsController.getPropertyDetailsPath(id, isLocalCouncilView = false))
        return createValidPage(
            page,
            PropertyDetailsPageLandlordView::class,
            mapOf("propertyOwnershipId" to id.toString()),
        )
    }

    fun goToPropertyDetailsLocalCouncilView(id: Long): PropertyDetailsPageLocalCouncilView {
        navigate(PropertyDetailsController.getPropertyDetailsPath(id, isLocalCouncilView = true))
        return createValidPage(
            page,
            PropertyDetailsPageLocalCouncilView::class,
            mapOf("propertyOwnershipId" to id.toString()),
        )
    }

    fun goToPropertyDetailsUpdateOwnershipTypePage(propertyOwnershipId: Long): OwnershipTypeFormPagePropertyDetailsUpdate {
        navigate(
            UpdateOwnershipTypeController.getUpdateOwnershipTypeRoute(propertyOwnershipId) +
                "/${OwnershipTypeStep.ROUTE_SEGMENT}",
        )
        return createValidPage(
            page,
            OwnershipTypeFormPagePropertyDetailsUpdate::class,
            mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
        )
    }

    fun skipToPropertyDetailsUpdateCheckOccupancyToOccupiedAnswersPage(
        propertyOwnershipId: Long,
    ): CheckOccupancyAnswersPagePropertyDetailsUpdate {
        setJourneyStateInSession(
            UpdateOccupancyJourneyStateSessionBuilder.withTenants().build(),
        )
        return goToPropertyDetailsUpdateCheckOccupancyAnswersPage(propertyOwnershipId)
    }

    fun skipToPropertyDetailsUpdateCheckOccupancyToVacantAnswersPage(
        propertyOwnershipId: Long,
    ): CheckOccupancyAnswersPagePropertyDetailsUpdate {
        setJourneyStateInSession(
            UpdateOccupancyJourneyStateSessionBuilder.withNoTenants().build(),
        )
        return goToPropertyDetailsUpdateCheckOccupancyAnswersPage(propertyOwnershipId)
    }

    fun goToPropertyDetailsUpdateOccupancy(propertyOwnershipId: Long): OccupancyFormPagePropertyDetailsUpdate {
        navigate(
            UpdateOccupancyController.getUpdateOccupancyRoute(propertyOwnershipId) +
                "/${OccupiedStep.ROUTE_SEGMENT}",
        )
        return createValidPage(
            page,
            OccupancyFormPagePropertyDetailsUpdate::class,
            mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
        )
    }

    fun goToPropertyDetailsUpdateCheckOccupancyAnswersPage(propertyOwnershipId: Long): CheckOccupancyAnswersPagePropertyDetailsUpdate {
        navigate(
            UpdateOccupancyController.getUpdateOccupancyRoute(propertyOwnershipId) +
                "/${PropertyRegistrationCyaStep.ROUTE_SEGMENT}",
        )
        return createValidPage(
            page,
            CheckOccupancyAnswersPagePropertyDetailsUpdate::class,
            mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
        )
    }

    fun goToDeregisterPropertyInfoPage(propertyOwnershipId: Long): DeregisterPropertyInfoPage {
        navigate(DeregisterPropertyController.getPropertyDeregistrationPath(propertyOwnershipId))
        return createValidPage(
            page,
            DeregisterPropertyInfoPage::class,
            mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
        )
    }

    fun goToCannotDeregisterPropertyJointLandlordsPage(propertyOwnershipId: Long): CannotDeregisterPropertyJointLandlordsPage {
        navigate(DeregisterPropertyController.getPropertyDeregistrationPath(propertyOwnershipId))
        return createValidPage(
            page,
            CannotDeregisterPropertyJointLandlordsPage::class,
            mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
        )
    }

    fun goToDelegateToLettingAgentAllowLettingAgentPage(propertyOwnershipId: Long): AllowLettingAgentPageDelegateToLettingAgent {
        navigate(DelegateToLettingAgentController.getDelegateToLettingAgentPath(propertyOwnershipId))
        return createValidPage(
            page,
            AllowLettingAgentPageDelegateToLettingAgent::class,
            mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
        )
    }

    fun goToLeavePropertyConfirmPage(propertyOwnershipId: Long): ConfirmPageLeaveProperty {
        navigate(LeavePropertyController.getLeavePropertyPath(propertyOwnershipId))
        return createValidPage(
            page,
            ConfirmPageLeaveProperty::class,
            mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
        )
    }

    fun skipToPropertyDeregistrationConfirmPage(propertyOwnershipId: Long): ConfirmPagePropertyDeregistration {
        setJourneyStateInSession(
            PropertyDeregistrationStateSessionBuilder.beforePropertyDeregistrationReasonViaInfo().build(),
        )
        navigate(
            DeregisterPropertyController.getPropertyDeregistrationBasePath(propertyOwnershipId) +
                "/${DeregistrationConfirmStep.ROUTE_SEGMENT}?journeyId=$TEST_JOURNEY_ID",
        )
        return createValidPage(
            page,
            ConfirmPagePropertyDeregistration::class,
            mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
        )
    }

    fun skipToPropertyDeregistrationCheckPendingInvitationsPage(propertyOwnershipId: Long): CheckInvitationsPage {
        setJourneyStateInSession(
            PropertyDeregistrationStateSessionBuilder().withDeregisterInfoCompleted().build(),
        )
        navigate(
            DeregisterPropertyController.getPropertyDeregistrationBasePath(propertyOwnershipId) +
                "/${CheckPendingInvitationsStep.ROUTE_SEGMENT}?journeyId=$TEST_JOURNEY_ID",
        )
        return createValidPage(
            page,
            CheckInvitationsPage::class,
            mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
        )
    }

    fun goToLandlordDeregistrationAreYouSurePage(): AreYouSureFormPageLandlordDeregistration {
        navigate("${DeregisterLandlordController.LANDLORD_DEREGISTRATION_ROUTE}/${AreYouSureStep.ROUTE_SEGMENT}")
        return createValidPage(page, AreYouSureFormPageLandlordDeregistration::class)
    }

    fun goToOrgLandlordDeregistrationAreYouSurePage(): AreYouSureFormPageOrganisationalLandlordDeregistration {
        navigate(
            "${DeregisterOrganisationalLandlordController.ORGANISATIONAL_LANDLORD_DEREGISTRATION_ROUTE}/${OrgAreYouSureStep.ROUTE_SEGMENT}",
        )
        return createValidPage(page, AreYouSureFormPageOrganisationalLandlordDeregistration::class)
    }

    fun goToLocalCouncilDashboard(): LocalCouncilDashboardPage {
        navigate(LOCAL_COUNCIL_DASHBOARD_URL)
        return createValidPage(page, LocalCouncilDashboardPage::class)
    }

    fun goToSystemOperatorDashboard(): SystemOperatorDashboardPage {
        navigate(SYSTEM_OPERATOR_DASHBOARD_URL)
        return createValidPage(page, SystemOperatorDashboardPage::class)
    }

    fun goToFeatureFlagOverrides(): FeatureFlagOverridesPage {
        navigate(FEATURE_FLAG_OVERRIDES_ROUTE)
        return createValidPage(page, FeatureFlagOverridesPage::class)
    }

    fun goToGeneratePasscodePage(): GeneratePasscodePage {
        navigate(GENERATE_PASSCODE_URL)
        return createValidPage(page, GeneratePasscodePage::class)
    }

    fun goToMetricsPage(): MetricsPage {
        navigate(METRICS_URL)
        return createValidPage(page, MetricsPage::class)
    }

    fun navigateToPasscodeEntryPage() {
        navigate(PASSCODE_ENTRY_ROUTE)
    }

    fun navigateToInvalidPasscodePage() {
        navigate(INVALID_PASSCODE_ROUTE)
    }

    fun navigateToLandlordDashboard() {
        navigate(LANDLORD_DASHBOARD_URL)
    }

    fun goToLandlordDashboard(): LandlordDashboardPage {
        navigate(LANDLORD_DASHBOARD_URL)
        return createValidPage(page, LandlordDashboardPage::class)
    }

    fun goToLandlordPrivacyNoticePage(): LandlordPrivacyNoticePage {
        navigate(LANDLORD_PRIVACY_NOTICE_ROUTE)
        return createValidPage(page, LandlordPrivacyNoticePage::class)
    }

    fun goToLocalCouncilPrivacyNoticePage(): LocalCouncilPrivacyNoticePage {
        navigate(LocalCouncilPrivacyNoticeController.LOCAL_COUNCIL_PRIVACY_NOTICE_ROUTE)
        return createValidPage(page, LocalCouncilPrivacyNoticePage::class)
    }

    fun goToLandlordIncompleteProperties(): LandlordIncompletePropertiesPage {
        navigate(INCOMPLETE_PROPERTIES_URL)
        return createValidPage(page, LandlordIncompletePropertiesPage::class)
    }

    fun goToComplianceActions(): ComplianceActionsPage {
        navigate(COMPLIANCE_ACTIONS_URL)
        return createValidPage(page, ComplianceActionsPage::class)
    }

    fun goToDeleteIncompletePropertyRegistrationAreYouSurePage(contextId: String): DeleteIncompletePropertyRegistrationAreYouSurePage {
        navigate(
            "/$LANDLORD_PATH_SEGMENT/$DELETE_INCOMPLETE_PROPERTY_PATH_SEGMENT" +
                "?$CONTEXT_ID_URL_PARAMETER=$contextId",
        )
        return createValidPage(
            page,
            DeleteIncompletePropertyRegistrationAreYouSurePage::class,
            mapOf("contextId" to contextId),
        )
    }

    fun goToInviteLocalCouncilAdmin(): InviteLocalCouncilAdminPage {
        navigate(ManageLocalCouncilAdminsController.INVITE_LOCAL_COUNCIL_ADMIN_ROUTE)
        return createValidPage(page, InviteLocalCouncilAdminPage::class)
    }

    fun goToManageLocalCouncilAdminsPage(): ManageLocalCouncilAdminsPage {
        navigate(ManageLocalCouncilAdminsController.MANAGE_LOCAL_COUNCIL_ADMINS_ROUTE)
        return createValidPage(page, ManageLocalCouncilAdminsPage::class)
    }

    fun goToEditAdminsPage(localCouncilAdminId: Long): EditLocalCouncilAdminPage {
        navigate("$SYSTEM_OPERATOR_ROUTE/$EDIT_ADMIN_PATH_SEGMENT/$localCouncilAdminId")
        return createValidPage(
            page,
            EditLocalCouncilAdminPage::class,
            mapOf("localCouncilAdminId" to localCouncilAdminId.toString()),
        )
    }

    fun goToDeleteLocalCouncilAdminPage(localCouncilAdminId: Long): DeleteLocalCouncilAdminPage {
        navigate("$SYSTEM_OPERATOR_ROUTE/$DELETE_ADMIN_PATH_SEGMENT/$localCouncilAdminId")
        return createValidPage(
            page,
            DeleteLocalCouncilAdminPage::class,
            mapOf("localCouncilAdminId" to localCouncilAdminId.toString()),
        )
    }

    fun goToCancelAdminInvitePage(invitationId: Long): CancelLocalCouncilAdminInvitationPage {
        navigate("$SYSTEM_OPERATOR_ROUTE/$CANCEL_INVITATION_PATH_SEGMENT/$invitationId")
        return createValidPage(
            page,
            CancelLocalCouncilAdminInvitationPage::class,
            mapOf("invitationId" to invitationId.toString()),
        )
    }

    fun goToCookiesPage(): CookiesPage {
        navigate(COOKIES_ROUTE)
        return createValidPage(page, CookiesPage::class)
    }

    fun goToLandlordBetaFeedbackPage(): LandlordBetaFeedbackPage {
        navigate(BetaFeedbackController.LANDLORD_FEEDBACK_URL)
        return createValidPage(page, LandlordBetaFeedbackPage::class)
    }

    fun goToLocalCouncilBetaFeedbackPage(): LocalCouncilBetaFeedbackPage {
        navigate(BetaFeedbackController.LOCAL_COUNCIL_FEEDBACK_URL)
        return createValidPage(page, LocalCouncilBetaFeedbackPage::class)
    }

    fun navigate(path: String): Response? = page.navigate("http://localhost:$port$path")

    private fun navigateToLandlordRegistrationJourneyStep(stepRouteSegment: String) {
        navigate(
            JourneyStateService.urlWithJourneyState(
                "$LANDLORD_REGISTRATION_ROUTE/$stepRouteSegment",
                TEST_JOURNEY_ID,
            ),
        )
    }

    private fun setJourneyStateInSession(journeyState: Map<String, Any>) {
        val response =
            page.request().post(
                "http://localhost:$port/${SessionController.SET_JOURNEY_STATE_ROUTE}",
                RequestOptions.create().setData(SetJourneyStateRequestModel(TEST_JOURNEY_ID, journeyState)),
            )
        assertTrue(response.ok(), "Failed to set journey state. Received status code: ${response.status()}")
        response.dispose()
    }

    private fun storeInvitationTokenInSession(token: UUID) {
        val response =
            page.request().post(
                "http://localhost:$port/${SessionController.STORE_INVITATION_TOKEN_ROUTE}",
                RequestOptions.create().setData(StoreInvitationTokenRequestModel(token)),
            )
        assertTrue(response.ok(), "Failed to store invitation token. Received status code: ${response.status()}")
        response.dispose()
    }

    fun goToAcceptOrRejectValidJointLandlordInvitationJourney(token: String): AcceptOrRejectPage {
        navigate(
            "${AcceptOrRejectJointLandlordInvitationController.ACCEPT_OR_REJECT_JOINT_LANDLORD_INVITATION_ROUTE}?$TOKEN=$token",
        )
        return createValidPage(page, AcceptOrRejectPage::class)
    }

    fun goToAcceptOrRejectJointInvalidLandlordInvitationJourney(token: String): InvitationUnavailablePage {
        navigate(
            "${AcceptOrRejectJointLandlordInvitationController.ACCEPT_OR_REJECT_JOINT_LANDLORD_INVITATION_ROUTE}?$TOKEN=$token",
        )
        return createValidPage(page, InvitationUnavailablePage::class)
    }

    fun goToCancelJointLandlordInvitationAreYouSurePage(invitationId: Long): AreYouSurePageCancelJointLandlordInvitation {
        navigate(CancelJointLandlordInvitationController.getCancelJointLandlordInvitationPath(invitationId))
        return createValidPage(page, AreYouSurePageCancelJointLandlordInvitation::class)
    }

    fun goToCancelLettingAgentDelegationAreYouSurePage(propertyOwnershipId: Long): AreYouSurePageCancelLettingAgentDelegation {
        navigate(CancelLettingAgentDelegationController.getRemoveLettingAgentPath(propertyOwnershipId))
        return createValidPage(page, AreYouSurePageCancelLettingAgentDelegation::class)
    }

    fun goToInviteJointLandlordPage(propertyOwnershipId: Long): InviteJointLandlordFormPageInviteJointLandlord {
        navigate(
            "${InviteJointLandlordController.getInviteJointLandlordRoute(propertyOwnershipId)}/" +
                InviteJointLandlordStep.INVITE_FIRST_ROUTE_SEGMENT,
        )
        return createValidPage(
            page,
            InviteJointLandlordFormPageInviteJointLandlord::class,
            mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
        )
    }

    fun goToLettingAgentInvitationJourney(token: String): ValidateTokenPage {
        navigate(
            "${LettingAgentInvitationController.LETTING_AGENT_INVITATION_ROUTE}?$TOKEN=$token",
        )
        return createValidPage(page, ValidateTokenPage::class)
    }

    fun skipToLettingAgentInvitationSetPasswordPage(token: String): SetPasswordPage {
        setJourneyStateInSession(
            LettingAgentInvitationStateSessionBuilder.beforeSetPassword(token).build(),
        )
        navigate(
            "${LettingAgentInvitationController.LETTING_AGENT_INVITATION_ROUTE}/${SetPasswordStep.ROUTE_SEGMENT}" +
                "?journeyId=$TEST_JOURNEY_ID",
        )
        return createValidPage(page, SetPasswordPage::class)
    }

    fun skipToLettingAgentInvitationEnterPasswordPage(token: String): EnterPasswordPage {
        setJourneyStateInSession(
            LettingAgentInvitationStateSessionBuilder.beforeEnterPassword(token).build(),
        )
        navigate(
            "${LettingAgentInvitationController.LETTING_AGENT_INVITATION_ROUTE}/${EnterPasswordStep.ROUTE_SEGMENT}" +
                "?journeyId=$TEST_JOURNEY_ID",
        )
        return createValidPage(page, EnterPasswordPage::class)
    }

    companion object {
        const val TEST_JOURNEY_ID = "test-journey-id"
    }
}
