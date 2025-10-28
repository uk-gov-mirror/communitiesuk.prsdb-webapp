package uk.gov.communities.prsdb.webapp.forms.journeys

import jakarta.persistence.EntityExistsException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.validation.Validator
import uk.gov.communities.prsdb.webapp.constants.BACK_URL_ATTR_NAME
import uk.gov.communities.prsdb.webapp.constants.CONFIRMATION_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.FIND_LOCAL_AUTHORITY_URL
import uk.gov.communities.prsdb.webapp.constants.enums.JourneyType
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.controllers.LandlordController
import uk.gov.communities.prsdb.webapp.controllers.RegisterPropertyController
import uk.gov.communities.prsdb.webapp.forms.JourneyData
import uk.gov.communities.prsdb.webapp.forms.pages.AlreadyRegisteredPage
import uk.gov.communities.prsdb.webapp.forms.pages.Page
import uk.gov.communities.prsdb.webapp.forms.pages.PropertyRegistrationCheckAnswersPage
import uk.gov.communities.prsdb.webapp.forms.pages.PropertyRegistrationNumberOfPeoplePage
import uk.gov.communities.prsdb.webapp.forms.pages.SelectAddressPage
import uk.gov.communities.prsdb.webapp.forms.pages.SelectLocalAuthorityPage
import uk.gov.communities.prsdb.webapp.forms.steps.LookupAddressStep
import uk.gov.communities.prsdb.webapp.forms.steps.RegisterPropertyStepId
import uk.gov.communities.prsdb.webapp.forms.steps.Step
import uk.gov.communities.prsdb.webapp.forms.tasks.JourneySection
import uk.gov.communities.prsdb.webapp.forms.tasks.JourneyTask
import uk.gov.communities.prsdb.webapp.helpers.JourneyDataHelper
import uk.gov.communities.prsdb.webapp.helpers.PropertyRegistrationJourneyDataHelper
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.HmoAdditionalLicenceFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.HmoMandatoryLicenceFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.LicensingTypeFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.LookupAddressFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.ManualAddressFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NoInputFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NumberOfHouseholdsFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NumberOfPeopleFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.OccupancyFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.OwnershipTypeFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.PropertyTypeFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.SelectAddressFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.SelectiveLicenceFormModel
import uk.gov.communities.prsdb.webapp.models.viewModels.formModels.HMOAdditionalDetailModel
import uk.gov.communities.prsdb.webapp.models.viewModels.formModels.RadiosButtonViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.formModels.RadiosDividerViewModel
import uk.gov.communities.prsdb.webapp.services.AddressService
import uk.gov.communities.prsdb.webapp.services.JourneyDataService
import uk.gov.communities.prsdb.webapp.services.LocalAuthorityService
import uk.gov.communities.prsdb.webapp.services.PropertyRegistrationService

class PropertyRegistrationJourney(
    validator: Validator,
    journeyDataService: JourneyDataService,
    private val addressService: AddressService,
    private val propertyRegistrationService: PropertyRegistrationService,
    private val localAuthorityService: LocalAuthorityService,
) : JourneyWithTaskList<RegisterPropertyStepId>(
        journeyType = JourneyType.PROPERTY_REGISTRATION,
        initialStepId = RegisterPropertyStepId.LookupAddress,
        validator = validator,
        journeyDataService = journeyDataService,
    ) {
    override val stepRouter = GroupedStepRouter(this)
    override val checkYourAnswersStepId = RegisterPropertyStepId.CheckAnswers
    override val sections =
        listOf(
            JourneySection(registerPropertyTasks(), "registerProperty.taskList.register.heading", "register-property"),
            JourneySection(checkAndSubmitPropertiesTasks(), "registerProperty.taskList.checkAndSubmit.heading", "check-and-submit"),
        )

    override val taskListFactory =
        getTaskListViewModelFactory(
            "registerProperty.title",
            "registerProperty.taskList.heading",
            listOf("registerProperty.taskList.subtitle.one", "registerProperty.taskList.subtitle.two"),
            backUrl = RegisterPropertyController.PROPERTY_REGISTRATION_ROUTE,
        )

    private fun registerPropertyTasks(): List<JourneyTask<RegisterPropertyStepId>> =
        listOf(
            addressTask(),
            JourneyTask.withOneStep(
                propertyTypeStep(),
                "registerProperty.taskList.register.selectType",
            ),
            JourneyTask.withOneStep(
                ownershipTypeStep(),
                "registerProperty.taskList.register.selectOwnership",
                "registerProperty.taskList.register.selectOwnership.hint",
            ),
            licensingTask(),
            occupancyTask(),
        )

    private fun checkAndSubmitPropertiesTasks(): List<JourneyTask<RegisterPropertyStepId>> =
        listOf(
            JourneyTask.withOneStep(
                checkAnswersStep(),
                "registerProperty.taskList.checkAndSubmit.checkAnswers",
            ),
        )

    private fun addressTask() =
        JourneyTask(
            RegisterPropertyStepId.LookupAddress,
            setOf(
                lookupAddressStep(),
                noAddressFoundStep(),
                selectAddressStep(),
                alreadyRegisteredStep(),
                manualAddressStep(),
                localAuthorityStep(),
            ),
            "registerProperty.taskList.register.addAddress",
        )

    private fun licensingTask() =
        JourneyTask(
            RegisterPropertyStepId.LicensingType,
            setOf(
                licensingTypeStep(),
                selectiveLicenceStep(),
                hmoMandatoryLicenceStep(),
                hmoAdditionalLicenceStep(),
            ),
            "registerProperty.taskList.register.addLicensing",
        )

    private fun occupancyTask() =
        JourneyTask(
            RegisterPropertyStepId.Occupancy,
            setOf(
                occupancyStep(),
                numberOfHouseholdsStep(),
                numberOfPeopleStep(),
            ),
            "registerProperty.taskList.register.addTenancyInfo",
            "registerProperty.taskList.register.addTenancyInfo.hint",
        )

    private fun lookupAddressStep() =
        LookupAddressStep(
            id = RegisterPropertyStepId.LookupAddress,
            page =
                Page(
                    formModel = LookupAddressFormModel::class,
                    templateName = "forms/lookupAddressForm",
                    content =
                        mapOf(
                            "title" to "registerProperty.title",
                            "fieldSetHeading" to "forms.lookupAddress.propertyRegistration.fieldSetHeading",
                            "fieldSetHint" to "forms.lookupAddress.propertyRegistration.fieldSetHint",
                            "postcodeLabel" to "forms.lookupAddress.postcode.label",
                            "postcodeHint" to "forms.lookupAddress.postcode.hint",
                            "houseNameOrNumberLabel" to "forms.lookupAddress.houseNameOrNumber.label",
                            "houseNameOrNumberHint" to "forms.lookupAddress.houseNameOrNumber.hint",
                            "submitButtonText" to "forms.buttons.continue",
                            BACK_URL_ATTR_NAME to LandlordController.LANDLORD_DASHBOARD_URL,
                        ),
                    shouldDisplaySectionHeader = true,
                ),
            nextStepIfAddressesFound = RegisterPropertyStepId.SelectAddress,
            nextStepIfNoAddressesFound = RegisterPropertyStepId.NoAddressFound,
            addressService = addressService,
            journeyDataService = journeyDataService,
            saveAfterSubmit = false,
        )

    private fun selectAddressStep() =
        Step(
            id = RegisterPropertyStepId.SelectAddress,
            page =
                SelectAddressPage(
                    formModel = SelectAddressFormModel::class,
                    templateName = "forms/selectAddressForm",
                    content =
                        mapOf(
                            "title" to "registerProperty.title",
                            "fieldSetHeading" to "forms.selectAddress.fieldSetHeading",
                            "submitButtonText" to "forms.buttons.useThisAddress",
                            "searchAgainUrl" to
                                "${RegisterPropertyController.PROPERTY_REGISTRATION_ROUTE}/" +
                                RegisterPropertyStepId.LookupAddress.urlPathSegment,
                        ),
                    lookupAddressPathSegment = RegisterPropertyStepId.LookupAddress.urlPathSegment,
                    journeyDataService = journeyDataService,
                    displaySectionHeader = true,
                ),
            nextAction = { filteredJourneyData, _ -> selectAddressNextAction(filteredJourneyData, propertyRegistrationService) },
            saveAfterSubmit = false,
        )

    private fun alreadyRegisteredStep() =
        Step(
            id = RegisterPropertyStepId.AlreadyRegistered,
            page =
                AlreadyRegisteredPage(
                    formModel = NoInputFormModel::class,
                    templateName = "alreadyRegisteredPropertyPage",
                    content =
                        mapOf(
                            "title" to "registerProperty.title",
                            "searchAgainUrl" to
                                "${RegisterPropertyController.PROPERTY_REGISTRATION_ROUTE}/" +
                                RegisterPropertyStepId.LookupAddress.urlPathSegment,
                        ),
                    selectedAddressPathSegment = RegisterPropertyStepId.SelectAddress.urlPathSegment,
                ),
            saveAfterSubmit = false,
        )

    private fun noAddressFoundStep() =
        Step(
            id = RegisterPropertyStepId.NoAddressFound,
            page =
                Page(
                    formModel = NoInputFormModel::class,
                    templateName = "forms/noAddressFoundForm",
                    content =
                        mapOf(
                            "title" to "registerProperty.title",
                            "postcode" to getHouseNameOrNumberAndPostcode().second,
                            "houseNameOrNumber" to getHouseNameOrNumberAndPostcode().first,
                            "searchAgainUrl" to RegisterPropertyStepId.LookupAddress.urlPathSegment,
                        ),
                    shouldDisplaySectionHeader = true,
                ),
            nextAction = { _, _ -> Pair(RegisterPropertyStepId.ManualAddress, null) },
            saveAfterSubmit = false,
        )

    private fun getHouseNameOrNumberAndPostcode() =
        JourneyDataHelper
            .getLookupAddressHouseNameOrNumberAndPostcode(
                journeyDataService.getJourneyDataFromSession(),
                RegisterPropertyStepId.LookupAddress.urlPathSegment,
            ) ?: Pair("", "")

    private fun manualAddressStep() =
        Step(
            id = RegisterPropertyStepId.ManualAddress,
            page =
                Page(
                    formModel = ManualAddressFormModel::class,
                    templateName = "forms/manualAddressForm",
                    content =
                        mapOf(
                            "title" to "registerProperty.title",
                            "fieldSetHeading" to "forms.manualAddress.propertyRegistration.fieldSetHeading",
                            "fieldSetHint" to "forms.manualAddress.fieldSetHint",
                            "addressLineOneLabel" to "forms.manualAddress.addressLineOne.label",
                            "addressLineTwoLabel" to "forms.manualAddress.addressLineTwo.label",
                            "townOrCityLabel" to "forms.manualAddress.townOrCity.label",
                            "countyLabel" to "forms.manualAddress.county.label",
                            "postcodeLabel" to "forms.manualAddress.postcode.label",
                            "submitButtonText" to "forms.buttons.continue",
                        ),
                    shouldDisplaySectionHeader = true,
                ),
            nextAction = { _, _ -> Pair(RegisterPropertyStepId.LocalAuthority, null) },
            saveAfterSubmit = false,
        )

    private fun localAuthorityStep() =
        Step(
            id = RegisterPropertyStepId.LocalAuthority,
            page =
                SelectLocalAuthorityPage(
                    content =
                        mapOf(
                            "title" to "registerProperty.title",
                            "fieldSetHeading" to "forms.selectLocalAuthority.fieldSetHeading",
                            "fieldSetHint" to "forms.selectLocalAuthority.fieldSetHint",
                            "selectLabel" to "forms.selectLocalAuthority.select.label",
                            "findLocalAuthorityUrl" to FIND_LOCAL_AUTHORITY_URL,
                        ),
                    localAuthorityService = localAuthorityService,
                    displaySectionHeader = true,
                ),
            nextAction = { _, _ -> Pair(RegisterPropertyStepId.PropertyType, null) },
            saveAfterSubmit = false,
        )

    private fun propertyTypeStep() =
        Step(
            id = RegisterPropertyStepId.PropertyType,
            page =
                Page(
                    formModel = PropertyTypeFormModel::class,
                    templateName = "forms/propertyTypeForm.html",
                    content =
                        mapOf(
                            "title" to "registerProperty.title",
                            "fieldSetHeading" to "forms.propertyType.fieldSetHeading",
                            "radioOptions" to
                                listOf(
                                    RadiosButtonViewModel(
                                        value = PropertyType.DETACHED_HOUSE,
                                        labelMsgKey = "forms.propertyType.radios.option.detachedHouse.label",
                                        hintMsgKey = "forms.propertyType.radios.option.detachedHouse.hint",
                                    ),
                                    RadiosButtonViewModel(
                                        value = PropertyType.SEMI_DETACHED_HOUSE,
                                        labelMsgKey = "forms.propertyType.radios.option.semiDetachedHouse.label",
                                        hintMsgKey = "forms.propertyType.radios.option.semiDetachedHouse.hint",
                                    ),
                                    RadiosButtonViewModel(
                                        value = PropertyType.TERRACED_HOUSE,
                                        labelMsgKey = "forms.propertyType.radios.option.terracedHouse.label",
                                        hintMsgKey = "forms.propertyType.radios.option.terracedHouse.hint",
                                    ),
                                    RadiosButtonViewModel(
                                        value = PropertyType.FLAT,
                                        labelMsgKey = "forms.propertyType.radios.option.flat.label",
                                        hintMsgKey = "forms.propertyType.radios.option.flat.hint",
                                    ),
                                    RadiosButtonViewModel(
                                        value = PropertyType.OTHER,
                                        labelMsgKey = "forms.propertyType.radios.option.other.label",
                                        hintMsgKey = "forms.propertyType.radios.option.other.hint",
                                        conditionalFragment = "customPropertyTypeInput",
                                    ),
                                ),
                        ),
                    shouldDisplaySectionHeader = true,
                ),
            nextAction = { _, _ -> Pair(RegisterPropertyStepId.OwnershipType, null) },
        )

    private fun ownershipTypeStep() =
        Step(
            id = RegisterPropertyStepId.OwnershipType,
            page =
                Page(
                    formModel = OwnershipTypeFormModel::class,
                    templateName = "forms/ownershipTypeForm.html",
                    content =
                        mapOf(
                            "title" to "registerProperty.title",
                            "fieldSetHeading" to "forms.ownershipType.fieldSetHeading",
                            "radioOptions" to
                                listOf(
                                    RadiosButtonViewModel(
                                        value = OwnershipType.FREEHOLD,
                                        labelMsgKey = "forms.ownershipType.radios.option.freehold.label",
                                        hintMsgKey = "forms.ownershipType.radios.option.freehold.hint",
                                    ),
                                    RadiosButtonViewModel(
                                        value = OwnershipType.LEASEHOLD,
                                        labelMsgKey = "forms.ownershipType.radios.option.leasehold.label",
                                        hintMsgKey = "forms.ownershipType.radios.option.leasehold.hint",
                                    ),
                                    RadiosButtonViewModel(
                                        value = OwnershipType.SHARE_OF_FREEHOLD,
                                        labelMsgKey = "forms.ownershipType.radios.option.shareOfFreehold.label",
                                        hintMsgKey = "forms.ownershipType.radios.option.shareOfFreehold.hint",
                                    ),
                                    RadiosButtonViewModel(
                                        value = OwnershipType.COMMONHOLD,
                                        labelMsgKey = "forms.ownershipType.radios.option.commonhold.label",
                                        hintMsgKey = "forms.ownershipType.radios.option.commonhold.hint",
                                    ),
                                ),
                            "submitButtonText" to "forms.buttons.saveAndContinue",
                        ),
                    shouldDisplaySectionHeader = true,
                ),
            nextAction = { _, _ -> Pair(RegisterPropertyStepId.LicensingType, null) },
        )

    private fun licensingTypeStep() =
        Step(
            id = RegisterPropertyStepId.LicensingType,
            page =
                Page(
                    formModel = LicensingTypeFormModel::class,
                    templateName = "forms/licensingTypeForm",
                    content =
                        mapOf(
                            "title" to "registerProperty.title",
                            "fieldSetHeading" to "forms.licensingType.fieldSetHeading",
                            "fieldSetHint" to "forms.licensingType.fieldSetHint",
                            "radioOptions" to
                                listOf(
                                    RadiosButtonViewModel(
                                        value = LicensingType.SELECTIVE_LICENCE,
                                        labelMsgKey = "forms.licensingType.radios.option.selectiveLicence.label",
                                        hintMsgKey = "forms.licensingType.radios.option.selectiveLicence.hint",
                                    ),
                                    RadiosButtonViewModel(
                                        value = LicensingType.HMO_MANDATORY_LICENCE,
                                        labelMsgKey = "forms.licensingType.radios.option.hmoMandatory.label",
                                        hintMsgKey = "forms.licensingType.radios.option.hmoMandatory.hint",
                                    ),
                                    RadiosButtonViewModel(
                                        value = LicensingType.HMO_ADDITIONAL_LICENCE,
                                        labelMsgKey = "forms.licensingType.radios.option.hmoAdditional.label",
                                        hintMsgKey = "forms.licensingType.radios.option.hmoAdditional.hint",
                                    ),
                                    RadiosDividerViewModel("forms.radios.dividerText"),
                                    RadiosButtonViewModel(
                                        value = LicensingType.NO_LICENSING,
                                        labelMsgKey = "forms.licensingType.radios.option.noLicensing.label",
                                    ),
                                ),
                        ),
                    shouldDisplaySectionHeader = true,
                ),
            nextAction = { filteredJourneyData, _ -> licensingTypeNextAction(filteredJourneyData) },
        )

    private fun selectiveLicenceStep() =
        Step(
            id = RegisterPropertyStepId.SelectiveLicence,
            page =
                Page(
                    formModel = SelectiveLicenceFormModel::class,
                    templateName = "forms/licenceNumberForm",
                    content =
                        mapOf(
                            "title" to "registerProperty.title",
                            "fieldSetHeading" to "forms.selectiveLicence.fieldSetHeading",
                            "label" to "forms.selectiveLicence.label",
                            "detailSummary" to "forms.selectiveLicence.detail.summary",
                            "detailMainText" to "forms.selectiveLicence.detail.text",
                        ),
                    shouldDisplaySectionHeader = true,
                ),
            nextAction = { _, _ -> Pair(RegisterPropertyStepId.Occupancy, null) },
        )

    private fun hmoMandatoryLicenceStep() =
        Step(
            id = RegisterPropertyStepId.HmoMandatoryLicence,
            page =
                Page(
                    formModel = HmoMandatoryLicenceFormModel::class,
                    templateName = "forms/licenceNumberForm",
                    content =
                        mapOf(
                            "title" to "registerProperty.title",
                            "fieldSetHeading" to "forms.hmoMandatoryLicence.fieldSetHeading",
                            "label" to "forms.hmoMandatoryLicence.label",
                            "detailSummary" to "forms.hmoMandatoryLicence.detail.summary",
                            "detailAdditionalContent" to
                                HMOAdditionalDetailModel(
                                    "forms.hmoMandatoryLicence.detail.paragraph.two",
                                    "forms.hmoMandatoryLicence.detail.paragraph.three",
                                    listOf(
                                        "forms.hmoMandatoryLicence.detail.bullet.one",
                                        "forms.hmoMandatoryLicence.detail.bullet.two",
                                        "forms.hmoMandatoryLicence.detail.bullet.three",
                                    ),
                                ),
                        ),
                    shouldDisplaySectionHeader = true,
                ),
            nextAction = { _, _ -> Pair(RegisterPropertyStepId.Occupancy, null) },
        )

    private fun hmoAdditionalLicenceStep() =
        Step(
            id = RegisterPropertyStepId.HmoAdditionalLicence,
            page =
                Page(
                    formModel = HmoAdditionalLicenceFormModel::class,
                    templateName = "forms/licenceNumberForm",
                    content =
                        mapOf(
                            "title" to "registerProperty.title",
                            "fieldSetHeading" to "forms.hmoAdditionalLicence.fieldSetHeading",
                            "label" to "forms.hmoAdditionalLicence.label",
                            "detailSummary" to "forms.hmoAdditionalLicence.detail.summary",
                            "detailAdditionalContent" to
                                HMOAdditionalDetailModel(
                                    "forms.hmoAdditionalLicence.detail.paragraph.two",
                                    "forms.hmoAdditionalLicence.detail.paragraph.three",
                                ),
                        ),
                    shouldDisplaySectionHeader = true,
                ),
            nextAction = { _, _ -> Pair(RegisterPropertyStepId.Occupancy, null) },
        )

    private fun occupancyStep() =
        Step(
            id = RegisterPropertyStepId.Occupancy,
            page =
                Page(
                    formModel = OccupancyFormModel::class,
                    templateName = "forms/propertyOccupancyForm",
                    content =
                        mapOf(
                            "title" to "registerProperty.title",
                            "fieldSetHeading" to "forms.occupancy.fieldSetHeading",
                            "fieldSetHint" to "forms.occupancy.fieldSetHint",
                            "radioOptions" to
                                listOf(
                                    RadiosButtonViewModel(
                                        value = true,
                                        labelMsgKey = "forms.radios.option.yes.label",
                                        hintMsgKey = "forms.occupancy.radios.option.yes.hint",
                                    ),
                                    RadiosButtonViewModel(
                                        value = false,
                                        labelMsgKey = "forms.radios.option.no.label",
                                        hintMsgKey = "forms.occupancy.radios.option.no.hint",
                                    ),
                                ),
                        ),
                    shouldDisplaySectionHeader = true,
                ),
            nextAction = { filteredJourneyData, _ -> occupancyNextAction(filteredJourneyData) },
        )

    private fun numberOfHouseholdsStep() =
        Step(
            id = RegisterPropertyStepId.NumberOfHouseholds,
            page =
                Page(
                    formModel = NumberOfHouseholdsFormModel::class,
                    templateName = "forms/numberOfHouseholdsForm",
                    content =
                        mapOf(
                            "title" to "registerProperty.title",
                            "fieldSetHeading" to "forms.numberOfHouseholds.fieldSetHeading",
                            "label" to "forms.numberOfHouseholds.label",
                        ),
                    shouldDisplaySectionHeader = true,
                ),
            nextAction = { _, _ -> Pair(RegisterPropertyStepId.NumberOfPeople, null) },
        )

    private fun numberOfPeopleStep() =
        Step(
            id = RegisterPropertyStepId.NumberOfPeople,
            page =
                PropertyRegistrationNumberOfPeoplePage(
                    formModel = NumberOfPeopleFormModel::class,
                    templateName = "forms/numberOfPeopleForm",
                    content =
                        mapOf(
                            "title" to "registerProperty.title",
                            "fieldSetHeading" to "forms.numberOfPeople.fieldSetHeading",
                            "fieldSetHint" to "forms.numberOfPeople.fieldSetHint",
                            "label" to "forms.numberOfPeople.label",
                        ),
                    shouldDisplaySectionHeader = true,
                    latestNumberOfHouseholds =
                        PropertyRegistrationJourneyDataHelper.getNumberOfHouseholds(
                            journeyDataService.getJourneyDataFromSession(),
                        ),
                ),
            nextAction = { _, _ -> Pair(RegisterPropertyStepId.CheckAnswers, null) },
        )

    private fun checkAnswersStep() =
        Step(
            id = RegisterPropertyStepId.CheckAnswers,
            page = PropertyRegistrationCheckAnswersPage(journeyDataService, localAuthorityService, unreachableStepRedirect),
            handleSubmitAndRedirect = { filteredJourneyData, _, _ -> checkAnswersSubmitAndRedirect(filteredJourneyData) },
        )

    private fun occupancyNextAction(filteredJourneyData: JourneyData): Pair<RegisterPropertyStepId, Int?> =
        if (PropertyRegistrationJourneyDataHelper.getIsOccupied(filteredJourneyData)!!) {
            Pair(RegisterPropertyStepId.NumberOfHouseholds, null)
        } else {
            Pair(RegisterPropertyStepId.CheckAnswers, null)
        }

    private fun selectAddressNextAction(
        filteredJourneyData: JourneyData,
        propertyRegistrationService: PropertyRegistrationService,
    ): Pair<RegisterPropertyStepId, Int?> =
        if (PropertyRegistrationJourneyDataHelper.isManualAddressChosen(filteredJourneyData)) {
            Pair(RegisterPropertyStepId.ManualAddress, null)
        } else {
            val selectedAddress = PropertyRegistrationJourneyDataHelper.getAddress(filteredJourneyData)!!
            if (selectedAddress.uprn != null && propertyRegistrationService.getIsAddressRegistered(selectedAddress.uprn)) {
                Pair(RegisterPropertyStepId.AlreadyRegistered, null)
            } else {
                Pair(RegisterPropertyStepId.PropertyType, null)
            }
        }

    private fun licensingTypeNextAction(filteredJourneyData: JourneyData): Pair<RegisterPropertyStepId, Int?> =
        when (PropertyRegistrationJourneyDataHelper.getLicensingType(filteredJourneyData)!!) {
            LicensingType.SELECTIVE_LICENCE -> Pair(RegisterPropertyStepId.SelectiveLicence, null)
            LicensingType.HMO_MANDATORY_LICENCE -> Pair(RegisterPropertyStepId.HmoMandatoryLicence, null)
            LicensingType.HMO_ADDITIONAL_LICENCE -> Pair(RegisterPropertyStepId.HmoAdditionalLicence, null)
            LicensingType.NO_LICENSING -> Pair(RegisterPropertyStepId.Occupancy, null)
        }

    private fun checkAnswersSubmitAndRedirect(filteredJourneyData: JourneyData): String {
        try {
            val address = PropertyRegistrationJourneyDataHelper.getAddress(filteredJourneyData)!!
            val baseUserId = SecurityContextHolder.getContext().authentication.name
            propertyRegistrationService.registerProperty(
                address = address,
                propertyType = PropertyRegistrationJourneyDataHelper.getPropertyType(filteredJourneyData)!!,
                licenseType = PropertyRegistrationJourneyDataHelper.getLicensingType(filteredJourneyData)!!,
                licenceNumber = PropertyRegistrationJourneyDataHelper.getLicenseNumber(filteredJourneyData)!!,
                ownershipType = PropertyRegistrationJourneyDataHelper.getOwnershipType(filteredJourneyData)!!,
                numberOfHouseholds = PropertyRegistrationJourneyDataHelper.getNumberOfHouseholds(filteredJourneyData),
                numberOfPeople = PropertyRegistrationJourneyDataHelper.getNumberOfTenants(filteredJourneyData),
                baseUserId = baseUserId,
            )

            journeyDataService.deleteJourneyData()

            return CONFIRMATION_PATH_SEGMENT
        } catch (exception: EntityExistsException) {
            return RegisterPropertyStepId.AlreadyRegistered.urlPathSegment
        }
    }
}
