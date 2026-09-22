package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration

import kotlinx.datetime.Instant
import org.springframework.beans.factory.ObjectFactory
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.CONFIRMATION_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.CORRESPONDENCE_ADDRESS
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING
import uk.gov.communities.prsdb.webapp.constants.TASK_LIST_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.enums.WhoProvidesRentalDetails
import uk.gov.communities.prsdb.webapp.controllers.RegisterPropertyController.Companion.PROPERTY_REGISTRATION_ROUTE
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.journeys.AbstractJourneyState
import uk.gov.communities.prsdb.webapp.journeys.AndParents
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.OrParents
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.always
import uk.gov.communities.prsdb.webapp.journeys.builders.JourneyBuilder.Companion.journey
import uk.gov.communities.prsdb.webapp.journeys.hasOutcome
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.checkAnswersChangeJourneys.occupancyChangeCyaJourney
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.checkAnswersChangeJourneys.whoProvidesChangeCyaJourney
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.CombinedComplianceCheckState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.OccupationState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.BedroomsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.BeforePdjb1022HasGasCertStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.BeforePdjb1022HasGasSupplyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.BillsIncludedStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CheckElectricalCertUploadsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CheckGasCertUploadsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ConfirmChangeToLettingAgentStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ConfirmMissingComplianceCheckResult
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ConfirmMissingComplianceMode
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ConfirmMissingComplianceStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CorrespondenceEmailStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ElectricalCertExpiryDateStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.EpcExemptionStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.EpcInDateAtStartOfTenancyCheckStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FinishCyaJourneyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FurnishedStatusStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.GasCertIssueDateStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasElectricalCertStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasEpcStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasJointLandlordsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasMeesExemptionStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasMissingComplianceStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HmoAdditionalLicenceStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HmoMandatoryLicenceStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HouseholdStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.IsEpcRequiredStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LettingAgentEmailStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LicensingTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LocalCouncilStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.MeesExemptionStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OccupancyChangeInterruptionStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OccupancyChangeRoutingStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OccupiedStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OwnershipTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.PropertyRegistrationCyaStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.PropertyRegistrationTaskListStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.PropertyTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ProvideTenancyDetailsLaterStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.RentAmountStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.RentFrequencyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.RentIncludesBillsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.SavePropertyRegistrationDataStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.SelectiveLicenceStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.StartEpcStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.TenantsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.WhoProvidesRentalDetailsMode
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.WhoProvidesRentalDetailsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.WhoProvidesUpdateRoutingStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.CorrespondenceDependencies
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.CorrespondenceTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.ElectricalSafetyDependencies
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.ElectricalSafetyTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.EpcDependencies
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.EpcTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.GasSafetyDependencies
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.GasSafetyTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.HouseHoldsAndTenantsDependencies
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.LicensingDependencies
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.LicensingTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.OccupationTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.OwnershipAndLandlordsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.PropertyDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.TenancyDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.WhoProvidesDetailsDependencies
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.WhoProvidesDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.shared.YesOrNo
import uk.gov.communities.prsdb.webapp.journeys.shared.inviteJointLandlord.CheckJointLandlordsStep
import uk.gov.communities.prsdb.webapp.journeys.shared.inviteJointLandlord.InviteJointLandlordsTaskDependencies
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState.Companion.checkAnswerStep
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState.Companion.checkAnswerTask
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.LookupAddressStep
import uk.gov.communities.prsdb.webapp.journeys.shared.tasks.CorrespondenceAddressTask
import uk.gov.communities.prsdb.webapp.models.viewModels.SectionHeaderViewModel
import uk.gov.communities.prsdb.webapp.services.UserToLandlordService
import java.security.Principal

@PrsdbWebService
class PropertyRegistrationJourneyFactory(
    private val stateFactory: ObjectFactory<PropertyRegistrationJourneyState>,
    private val featureFlagManager: FeatureFlagManager,
    private val userToLandlordService: UserToLandlordService,
) {
    final fun createJourneySteps(): Map<String, StepLifecycleOrchestrator> {
        val state = stateFactory.getObject()

        if (!state.isStateInitialized) {
            state.loggedInLandlordEmail = userToLandlordService.getCurrentLandlordForUser().email
            state.isStateInitialized = true
        }

        val checkingAnswersFor = state.checkingAnswersFor
        return if (checkingAnswersFor == null) {
            mainJourneyMap(state)
        } else {
            checkYourAnswersJourneyMap(state, checkingAnswersFor)
        }
    }

    private fun checkYourAnswersJourneyMap(
        state: PropertyRegistrationJourneyState,
        checkingAnswersFor: String,
    ): Map<String, StepLifecycleOrchestrator> =
        journey(state) {
            unreachableStepDestination { journey.returnToCyaPageDestination }
            configure {
                withAdditionalContentProperty { "title" to "registerProperty.title" }
            }
            configureFirst { backDestination { journey.returnToCyaPageDestination } }

            when (checkingAnswersFor) {
                WhoProvidesRentalDetailsStep.ROUTE_SEGMENT -> {
                    if (featureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)) {
                        whoProvidesChangeCyaJourney()
                    } else {
                        throw IllegalStateException("Unknown checkable element $checkingAnswersFor")
                    }
                }

                LettingAgentEmailStep.ROUTE_SEGMENT -> {
                    if (featureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)) {
                        fromTask(journey.whoProvidesDetailsTask, journey) {
                            checkAnswerStep(task.lettingAgentEmailStep, LettingAgentEmailStep.ROUTE_SEGMENT)
                        }
                    } else {
                        throw IllegalStateException("Unknown checkable element $checkingAnswersFor")
                    }
                }

                CorrespondenceEmailStep.ROUTE_SEGMENT -> {
                    if (featureFlagManager.checkFeature(CORRESPONDENCE_ADDRESS) &&
                        featureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)
                    ) {
                        fromTask(journey.correspondenceTask, journey) {
                            checkAnswerStep(task.correspondenceEmailStep, CorrespondenceEmailStep.ROUTE_SEGMENT)
                        }
                    } else {
                        throw IllegalStateException("Unknown checkable element $checkingAnswersFor")
                    }
                }

                "${CorrespondenceAddressTask.ROUTE_SEGMENT}/${LookupAddressStep.ROUTE_SEGMENT}" -> {
                    if (featureFlagManager.checkFeature(CORRESPONDENCE_ADDRESS) &&
                        featureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)
                    ) {
                        checkAnswerTask(journey.correspondenceTask.addressTask, CorrespondenceAddressTask.ROUTE_SEGMENT)
                    } else {
                        throw IllegalStateException("Unknown checkable element $checkingAnswersFor")
                    }
                }

                LookupAddressStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.propertyDetailsTask.addressTask)
                }

                LocalCouncilStep.ROUTE_SEGMENT -> {
                    fromTask(journey.propertyDetailsTask.addressTask) {
                        checkAnswerStep(task.localCouncilStep, LocalCouncilStep.ROUTE_SEGMENT)
                    }
                }

                PropertyTypeStep.ROUTE_SEGMENT -> {
                    fromTask(journey.propertyDetailsTask) {
                        checkAnswerStep(task.propertyTypeStep, PropertyTypeStep.ROUTE_SEGMENT)
                    }
                }

                OwnershipTypeStep.ROUTE_SEGMENT -> {
                    fromTask(journey.ownershipAndLandlordsTask) {
                        checkAnswerStep(task.ownershipTypeStep, OwnershipTypeStep.ROUTE_SEGMENT)
                    }
                }

                LicensingTypeStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.licensingTask, { journey })
                }

                SelectiveLicenceStep.ROUTE_SEGMENT -> {
                    fromTask(journey.licensingTask, journey) {
                        checkAnswerStep(task.selectiveLicenceStep, SelectiveLicenceStep.ROUTE_SEGMENT)
                    }
                }

                HmoMandatoryLicenceStep.ROUTE_SEGMENT -> {
                    fromTask(journey.licensingTask, journey) {
                        checkAnswerStep(task.hmoMandatoryLicenceStep, HmoMandatoryLicenceStep.ROUTE_SEGMENT)
                    }
                }

                HmoAdditionalLicenceStep.ROUTE_SEGMENT -> {
                    fromTask(journey.licensingTask, journey) {
                        checkAnswerStep(task.hmoAdditionalLicenceStep, HmoAdditionalLicenceStep.ROUTE_SEGMENT)
                    }
                }

                ProvideTenancyDetailsLaterStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.tenancyDetailsTask)
                }

                OccupiedStep.ROUTE_SEGMENT -> {
                    val isSkippingEnabled = featureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)
                    val isDelegateEnabled = featureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)
                    when {
                        isSkippingEnabled && isDelegateEnabled -> occupancyChangeCyaJourney()
                        isSkippingEnabled -> checkAnswerStep(journey.occupied, OccupiedStep.ROUTE_SEGMENT)
                        else -> checkAnswerTask(journey.occupationTask.inJourney(journey))
                    }
                }

                HouseholdStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.householdsAndTenantsTask, { HouseHoldsAndTenantsDependencies(true) })
                }

                TenantsStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.householdsAndTenantsTask, { HouseHoldsAndTenantsDependencies(true) })
                    configureStep(journey.householdsAndTenantsTask.tenants) {
                        backDestination { journey.returnToCyaPageDestination }
                    }
                }

                BedroomsStep.ROUTE_SEGMENT -> {
                    checkAnswerStep(journey.bedrooms, BedroomsStep.ROUTE_SEGMENT)
                }

                RentIncludesBillsStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.rentIncludesBillsTask)
                }

                BillsIncludedStep.ROUTE_SEGMENT -> {
                    fromTask(journey.rentIncludesBillsTask) {
                        checkAnswerStep(task.billsIncluded, BillsIncludedStep.ROUTE_SEGMENT)
                    }
                }

                FurnishedStatusStep.ROUTE_SEGMENT -> {
                    checkAnswerStep(journey.furnishedStatus, FurnishedStatusStep.ROUTE_SEGMENT)
                }

                RentFrequencyStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.rentFrequencyAndAmountTask)
                }

                RentAmountStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.rentFrequencyAndAmountTask)
                    configureStep(journey.rentFrequencyAndAmountTask.rentAmount) {
                        backDestination { journey.returnToCyaPageDestination }
                    }
                }

                HasJointLandlordsStep.ROUTE_SEGMENT,
                CheckJointLandlordsStep.ROUTE_SEGMENT,
                -> {
                    checkAnswerTask(journey.ownershipAndLandlordsTask.jointLandlordsTask, { journey })
                    configureStep(journey.ownershipAndLandlordsTask.jointLandlordsTask.inviteJointLandlordsTask.checkJointLandlordsStep) {
                        backDestination { journey.returnToCyaPageDestination }
                    }
                }

                BeforePdjb1022HasGasSupplyStep.ROUTE_SEGMENT,
                BeforePdjb1022HasGasCertStep.ROUTE_SEGMENT,
                CheckGasCertUploadsStep.ROUTE_SEGMENT,
                -> {
                    checkAnswerTask(journey.gasSafetyTask.gasSafetyDetailsTask, { journey })
                    configureStep(journey.gasSafetyTask.gasSafetyDetailsTask.beforePdjb1022HasGasCertStep) {
                        backDestination { journey.returnToCyaPageDestination }
                    }
                    configureStep(journey.gasSafetyTask.gasSafetyDetailsTask.hasGasCertStep) {
                        backDestination { journey.returnToCyaPageDestination }
                    }
                }

                GasCertIssueDateStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.gasSafetyTask.gasSafetyDetailsTask, { journey })
                    configureStep(journey.gasSafetyTask.gasSafetyDetailsTask.gasCertIssueDateStep) {
                        backDestination { journey.returnToCyaPageDestination }
                    }
                }

                HasElectricalCertStep.ROUTE_SEGMENT,
                CheckElectricalCertUploadsStep.ROUTE_SEGMENT,
                -> {
                    checkAnswerTask(journey.electricalSafetyTask.electricalSafetyDetailsTask, { journey })
                    configureStep(journey.electricalSafetyTask.electricalSafetyDetailsTask.checkElectricalCertUploadsStep) {
                        backDestination { journey.returnToCyaPageDestination }
                    }
                }

                ElectricalCertExpiryDateStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.electricalSafetyTask.electricalSafetyDetailsTask, { journey })
                    configureStep(journey.electricalSafetyTask.electricalSafetyDetailsTask.electricalCertExpiryDateStep) {
                        backDestination { journey.returnToCyaPageDestination }
                    }
                }

                StartEpcStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.epcTask.epcDetailsTask, { journey })
                }

                HasEpcStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.epcTask.epcDetailsTask, { journey })
                    configureStep(journey.epcTask.epcDetailsTask.hasEpcStep) {
                        backDestination { journey.returnToCyaPageDestination }
                    }
                }

                EpcInDateAtStartOfTenancyCheckStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.epcTask.epcDetailsTask, { journey })
                    configureStep(journey.epcTask.epcDetailsTask.epcInDateAtStartOfTenancyCheckStep) {
                        backDestination { journey.returnToCyaPageDestination }
                    }
                }

                IsEpcRequiredStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.epcTask.epcDetailsTask, { journey })
                    configureStep(journey.epcTask.epcDetailsTask.isEpcRequiredStep) {
                        backDestination { journey.returnToCyaPageDestination }
                    }
                }

                EpcExemptionStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.epcTask.epcDetailsTask, { journey })
                    configureStep(journey.epcTask.epcDetailsTask.epcExemptionStep) {
                        backDestination { journey.returnToCyaPageDestination }
                    }
                }

                HasMeesExemptionStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.epcTask.epcDetailsTask, { journey })
                    configureStep(journey.epcTask.epcDetailsTask.hasMeesExemptionStep) {
                        backDestination { journey.returnToCyaPageDestination }
                    }
                }

                MeesExemptionStep.ROUTE_SEGMENT -> {
                    fromTask(journey.epcTask.epcDetailsTask, journey) {
                        checkAnswerStep(task.meesExemptionStep, MeesExemptionStep.ROUTE_SEGMENT)
                    }
                }

                else -> {
                    throw IllegalStateException("Unknown checkable element $checkingAnswersFor")
                }
            }
            step(journey.finishCyaStep) {
                initialStep()
                nextDestination { Destination.Nowhere() }
                saveProgress()
            }
        }

    private fun mainJourneyMap(state: PropertyRegistrationJourneyState): Map<String, StepLifecycleOrchestrator> =
        if (featureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)) {
            restructuredMainJourneyMap(state)
        } else {
            legacyMainJourneyMap(state)
        }

    private fun legacyMainJourneyMap(state: PropertyRegistrationJourneyState): Map<String, StepLifecycleOrchestrator> =
        journey(state) {
            unreachableStepStep { journey.taskListStep }
            configure {
                withAdditionalContentProperty { "title" to "registerProperty.title" }
            }
            configureFirst { backDestination { journey.returnToCyaPageDestination } }
            configureStep(journey.confirmMissingComplianceStep) {
                withAdditionalContentProperty {
                    "sectionHeaderInfo" to
                        SectionHeaderViewModel(
                            sectionNameKey = "registerProperty.submitRegistration",
                            sectionNumber = 0,
                            totalSections = 0,
                            useNumbering = false,
                        )
                }
            }
            step(journey.taskListStep) {
                routeSegment(TASK_LIST_PATH_SEGMENT)
                initialStep()
                noNextDestination()
            }
            section {
                if (featureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)) {
                    withHeadingMessageKey("registerProperty.taskList.register.restructureAndSkipping.heading", false)
                } else {
                    withHeadingMessageKey("registerProperty.taskList.register.heading")
                }
                fromTask(journey.propertyDetailsTask) {
                    task(task.addressTask) {
                        parents { journey.taskListStep.always() }
                        nextStep { task.addToLandlordIncompletePropertiesStep }
                    }
                    step(task.addToLandlordIncompletePropertiesStep) {
                        parents { task.addressTask.isComplete() }
                        nextStep { task.propertyTypeStep }
                        saveProgress()
                    }
                    step(task.propertyTypeStep) {
                        routeSegment(PropertyTypeStep.ROUTE_SEGMENT)
                        parents { task.addressTask.isComplete() }
                        nextStep { journey.ownershipAndLandlordsTask.ownershipTypeStep }
                        saveProgress()
                    }
                }
                fromTask(journey.ownershipAndLandlordsTask) {
                    step(task.ownershipTypeStep) {
                        routeSegment(OwnershipTypeStep.ROUTE_SEGMENT)
                        parents { journey.propertyDetailsTask.propertyTypeStep.isComplete() }
                        nextStep { journey.licensingTask.firstStep }
                        saveProgress()
                    }
                }
                task(journey.licensingTask) {
                    withDependencies { journey }
                    parents { journey.ownershipAndLandlordsTask.ownershipTypeStep.isComplete() }
                    nextStep { journey.occupationTask.firstStep }
                    saveProgress()
                }

                task(journey.occupationTask.inJourney(journey)) {
                    parents { journey.licensingTask.isComplete() }
                    nextStep { journey.ownershipAndLandlordsTask.jointLandlordsTask.firstStep }
                    saveProgress()
                }
                task(journey.ownershipAndLandlordsTask.jointLandlordsTask) {
                    withDependencies { journey }
                    parents { journey.occupationTask.isComplete() }
                    nextStep { journey.gasSafetyTask.firstStep }
                    saveProgress()
                }
                task(journey.gasSafetyTask) {
                    withDependencies { journey }
                    parents { journey.ownershipAndLandlordsTask.jointLandlordsTask.isComplete() }
                    nextStep { journey.taskListStep }
                    saveProgress()
                }
                task(journey.electricalSafetyTask) {
                    withDependencies { journey }
                    parents { journey.gasSafetyTask.isComplete() }
                    backStep { journey.taskListStep }
                    nextStep { journey.taskListStep }
                    saveProgress()
                }
                configureStep(journey.electricalSafetyTask.electricalSafetyDetailsTask.checkElectricalCertUploadsStep) {
                    backStep { journey.electricalSafetyTask.electricalSafetyDetailsTask.electricalCertExpiryDateStep }
                }
                task(journey.epcTask) {
                    withDependencies { journey }
                    parents { journey.electricalSafetyTask.isComplete() }
                    backStep { journey.taskListStep }
                    nextStep { journey.taskListStep }
                    saveProgress()
                }
            }
            section {
                withHeadingMessageKey("registerProperty.taskList.checkAndSubmit.heading")
                step(journey.cyaStep) {
                    routeSegment(PropertyRegistrationCyaStep.ROUTE_SEGMENT)
                    backStep { journey.taskListStep }
                    parents {
                        journey.epcTask.isComplete()
                    }
                    nextStep { journey.hasMissingComplianceStep }
                }
                step(journey.hasMissingComplianceStep) {
                    parents { journey.cyaStep.isComplete() }
                    nextStep { mode ->
                        when (mode) {
                            ConfirmMissingComplianceCheckResult.OCCUPIED_AND_HAS_INVALID_CERTIFICATES -> {
                                journey.confirmMissingComplianceStep
                            }

                            ConfirmMissingComplianceCheckResult.UNOCCUPIED_OR_VALID_CERTIFICATES_OR_DELEGATED -> {
                                journey.savePropertyRegistrationDataStep
                            }
                        }
                    }
                }
                step(journey.confirmMissingComplianceStep) {
                    routeSegment(ConfirmMissingComplianceStep.ROUTE_SEGMENT)
                    parents {
                        journey.hasMissingComplianceStep.hasOutcome(
                            ConfirmMissingComplianceCheckResult.OCCUPIED_AND_HAS_INVALID_CERTIFICATES,
                        )
                    }
                    nextDestination { mode ->
                        when (mode) {
                            ConfirmMissingComplianceMode.GO_BACK -> {
                                Destination(journey.cyaStep)
                            }

                            ConfirmMissingComplianceMode.CONFIRMED -> {
                                Destination(journey.savePropertyRegistrationDataStep)
                            }
                        }
                    }
                }
                step(journey.savePropertyRegistrationDataStep) {
                    parents {
                        OrParents(
                            journey.hasMissingComplianceStep.hasOutcome(
                                ConfirmMissingComplianceCheckResult.UNOCCUPIED_OR_VALID_CERTIFICATES_OR_DELEGATED,
                            ),
                            journey.confirmMissingComplianceStep.hasOutcome(ConfirmMissingComplianceMode.CONFIRMED),
                        )
                    }
                    nextUrl { "$PROPERTY_REGISTRATION_ROUTE/$CONFIRMATION_PATH_SEGMENT" }
                }
            }
        }

    private fun restructuredMainJourneyMap(state: PropertyRegistrationJourneyState): Map<String, StepLifecycleOrchestrator> =
        journey(state) {
            unreachableStepStep { journey.taskListStep }
            configure {
                withAdditionalContentProperty { "title" to "registerProperty.title" }
            }
            configureFirst { backDestination { journey.returnToCyaPageDestination } }
            configureStep(journey.confirmMissingComplianceStep) {
                withAdditionalContentProperty {
                    "sectionHeaderInfo" to
                        SectionHeaderViewModel(
                            sectionNameKey = "registerProperty.submitRegistration",
                            sectionNumber = 0,
                            totalSections = 0,
                            useNumbering = false,
                        )
                }
            }
            // We don't have a section header for these pages, as their titles are the same as the respective page header
            listOf(
                journey.gasSafetyTask.checkGasSafetyAnswersStep,
                journey.electricalSafetyTask.checkElectricalSafetyAnswersStep,
                journey.epcTask.checkEpcAnswersStep,
            ).forEach { checkAnswersStep ->
                configureStep(checkAnswersStep) {
                    withAdditionalContentProperty { "sectionHeaderInfo" to null }
                }
            }
            step(journey.taskListStep) {
                routeSegment(TASK_LIST_PATH_SEGMENT)
                initialStep()
                noNextDestination()
            }
            section {
                withHeadingMessageKey("registerProperty.taskList.aboutYourProperty.propertyDetails", shouldUseNumbering = false)
                task(journey.propertyDetailsTask) {
                    parents { journey.taskListStep.always() }
                    nextStep { journey.ownershipAndLandlordsTask.firstStep }
                    saveProgress()
                }
            }
            val correspondenceEnabled = featureFlagManager.checkFeature(CORRESPONDENCE_ADDRESS)
            section {
                withHeadingMessageKey("registerProperty.taskList.aboutYourProperty.ownershipAndLandlords", shouldUseNumbering = false)
                task(journey.ownershipAndLandlordsTask) {
                    withDependencies { journey }
                    parents { journey.propertyDetailsTask.isComplete() }
                    nextStep { if (correspondenceEnabled) journey.correspondenceTask.firstStep else journey.occupied }
                    saveProgress()
                }
            }
            if (correspondenceEnabled) {
                section {
                    withHeadingMessageKey("registerProperty.taskList.aboutYourProperty.correspondence", shouldUseNumbering = false)
                    task(journey.correspondenceTask) {
                        withDependencies { journey }
                        parents { journey.ownershipAndLandlordsTask.isComplete() }
                        nextStep { journey.occupied }
                        saveProgress()
                    }
                }
            }
            val delegateEnabled = featureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)
            section {
                withHeadingMessageKey("registerProperty.taskList.aboutYourProperty.occupied", shouldUseNumbering = false)
                step(journey.occupied) {
                    routeSegment(OccupiedStep.ROUTE_SEGMENT)
                    parents {
                        if (correspondenceEnabled) {
                            journey.correspondenceTask.isComplete()
                        } else {
                            journey.ownershipAndLandlordsTask.isComplete()
                        }
                    }
                    nextStep { occupancy ->
                        if (delegateEnabled) {
                            when (occupancy) {
                                YesOrNo.YES -> journey.whoProvidesDetailsTask.firstStep
                                YesOrNo.NO -> journey.licensingTask.firstStep
                            }
                        } else {
                            journey.licensingTask.firstStep
                        }
                    }
                    saveProgress()
                }
            }
            if (delegateEnabled) {
                task(journey.whoProvidesDetailsTask) {
                    withDependencies { journey }
                    parents { journey.occupied.hasOutcome(YesOrNo.YES) }
                    nextStep {
                        if (journey.whoProvidesDetailsTask.whoProvidesRentalDetailsStep.outcome ==
                            WhoProvidesRentalDetailsMode.LETTING_AGENT_PROVIDES
                        ) {
                            journey.cyaStep
                        } else {
                            journey.licensingTask.firstStep
                        }
                    }
                    saveProgress()
                }
            }
            section {
                withHeadingMessageKey("registerProperty.taskList.rentedOut.licensing", shouldUseNumbering = false)
                task(journey.licensingTask) {
                    withDependencies { journey }
                    parents {
                        if (delegateEnabled) {
                            OrParents(
                                journey.occupied.hasOutcome(YesOrNo.NO),
                                AndParents(
                                    journey.whoProvidesDetailsTask.isComplete(),
                                    journey.whoProvidesDetailsTask.whoProvidesRentalDetailsStep.hasOutcome(
                                        WhoProvidesRentalDetailsMode.LANDLORD_PROVIDES,
                                    ),
                                ),
                            )
                        } else {
                            OrParents(
                                journey.occupied.hasOutcome(YesOrNo.YES),
                                journey.occupied.hasOutcome(YesOrNo.NO),
                            )
                        }
                    }
                    backStep {
                        if (delegateEnabled && journey.occupied.outcome == YesOrNo.YES) {
                            journey.whoProvidesDetailsTask.whoProvidesRentalDetailsStep
                        } else {
                            journey.occupied
                        }
                    }
                    nextStep { journey.gasSafetyTask.firstStep }
                    saveProgress()
                }
            }
            section {
                withHeadingMessageKey("registerProperty.taskList.gasSafety", shouldUseNumbering = false)
                task(journey.gasSafetyTask) {
                    withDependencies { journey }
                    parents { journey.licensingTask.isComplete() }
                    nextStep { journey.taskListStep }
                    saveProgress()
                }
            }
            section {
                withHeadingMessageKey("registerProperty.taskList.electricalSafety", shouldUseNumbering = false)
                task(journey.electricalSafetyTask) {
                    withDependencies { journey }
                    parents { journey.gasSafetyTask.isComplete() }
                    backStep { journey.taskListStep }
                    nextStep { journey.taskListStep }
                    saveProgress()
                }
                configureStep(journey.electricalSafetyTask.electricalSafetyDetailsTask.checkElectricalCertUploadsStep) {
                    backStep { journey.electricalSafetyTask.electricalSafetyDetailsTask.electricalCertExpiryDateStep }
                }
            }
            section {
                withHeadingMessageKey("registerProperty.taskList.epc", shouldUseNumbering = false)
                task(journey.epcTask) {
                    withDependencies { journey }
                    parents { journey.electricalSafetyTask.isComplete() }
                    backStep { journey.taskListStep }
                    nextStep { journey.taskListStep }
                    saveProgress()
                }
            }
            section {
                withHeadingMessageKey("registerProperty.taskList.rentedOut.tenancyDetails", shouldUseNumbering = false)
                task(journey.tenancyDetailsTask) {
                    parents {
                        AndParents(
                            journey.epcTask.isComplete(),
                            journey.occupied.hasOutcome(YesOrNo.YES),
                        )
                    }
                    backStep { journey.taskListStep }
                    nextStep { journey.cyaStep }
                    saveProgress()
                }
            }
            section {
                withHeadingMessageKey("registerProperty.taskList.submitYourRegistration.heading", shouldUseNumbering = false)
                step(journey.cyaStep) {
                    routeSegment(PropertyRegistrationCyaStep.ROUTE_SEGMENT)
                    backStep { journey.taskListStep }
                    parents {
                        val landlordProvidesPath =
                            AndParents(
                                journey.epcTask.isComplete(),
                                OrParents(
                                    journey.tenancyDetailsTask.isComplete(),
                                    journey.occupied.hasOutcome(YesOrNo.NO),
                                ),
                            )
                        if (delegateEnabled) {
                            OrParents(
                                landlordProvidesPath,
                                AndParents(
                                    journey.whoProvidesDetailsTask.isComplete(),
                                    journey.whoProvidesDetailsTask.whoProvidesRentalDetailsStep.hasOutcome(
                                        WhoProvidesRentalDetailsMode.LETTING_AGENT_PROVIDES,
                                    ),
                                ),
                            )
                        } else {
                            landlordProvidesPath
                        }
                    }
                    nextStep { journey.hasMissingComplianceStep }
                }
                step(journey.hasMissingComplianceStep) {
                    parents { journey.cyaStep.isComplete() }
                    nextStep { mode ->
                        when (mode) {
                            ConfirmMissingComplianceCheckResult.OCCUPIED_AND_HAS_INVALID_CERTIFICATES -> {
                                journey.confirmMissingComplianceStep
                            }

                            ConfirmMissingComplianceCheckResult.UNOCCUPIED_OR_VALID_CERTIFICATES_OR_DELEGATED -> {
                                journey.savePropertyRegistrationDataStep
                            }
                        }
                    }
                }
                step(journey.confirmMissingComplianceStep) {
                    routeSegment(ConfirmMissingComplianceStep.ROUTE_SEGMENT)
                    parents {
                        journey.hasMissingComplianceStep.hasOutcome(
                            ConfirmMissingComplianceCheckResult.OCCUPIED_AND_HAS_INVALID_CERTIFICATES,
                        )
                    }
                    nextDestination { mode ->
                        when (mode) {
                            ConfirmMissingComplianceMode.GO_BACK -> {
                                Destination(journey.cyaStep)
                            }

                            ConfirmMissingComplianceMode.CONFIRMED -> {
                                Destination(journey.savePropertyRegistrationDataStep)
                            }
                        }
                    }
                }
                step(journey.savePropertyRegistrationDataStep) {
                    parents {
                        OrParents(
                            journey.hasMissingComplianceStep.hasOutcome(
                                ConfirmMissingComplianceCheckResult.UNOCCUPIED_OR_VALID_CERTIFICATES_OR_DELEGATED,
                            ),
                            journey.confirmMissingComplianceStep.hasOutcome(ConfirmMissingComplianceMode.CONFIRMED),
                        )
                    }
                    nextUrl { "$PROPERTY_REGISTRATION_ROUTE/$CONFIRMATION_PATH_SEGMENT" }
                }
            }
        }

    fun initializeJourneyState(user: Principal): String = stateFactory.getObject().initializeOrRestoreState(user)
}

@JourneyFrameworkComponent
class PropertyRegistrationJourney(
    // Task list step
    override val taskListStep: PropertyRegistrationTaskListStep,
    // Licensing task
    override val licensingTask: LicensingTask,
    // Occupation steps
    override val occupied: OccupiedStep,
    // ===== Journey-structure tasks (the two alternative flows diverge here) =====
    // Legacy journey only (flag-off) — delete this (and OccupationTask, legacyMainJourneyMap,
    // legacySectionViewModels, bedrooms override) when the old journey is removed.
    override val occupationTask: OccupationTask,
    // Restructured journey only (flag-on) — grouping tasks for the new task-list structure.
    override val propertyDetailsTask: PropertyDetailsTask,
    override val ownershipAndLandlordsTask: OwnershipAndLandlordsTask,
    override val correspondenceTask: CorrespondenceTask,
    override val tenancyDetailsTask: TenancyDetailsTask,
    override val whoProvidesDetailsTask: WhoProvidesDetailsTask,
    // Gas safety task
    override val gasSafetyTask: GasSafetyTask,
    // Electrical safety task
    override val electricalSafetyTask: ElectricalSafetyTask,
    // EPC task
    override val epcTask: EpcTask,
    // Check your answers step
    override val cyaStep: PropertyRegistrationCyaStep,
    override val finishCyaStep: FinishCyaJourneyStep,
    // Occupancy-change interruption steps (occupied -> unoccupied while delegated to a letting agent)
    override val occupancyChangeRoutingStep: OccupancyChangeRoutingStep,
    override val occupancyChangeInterruptionStep: OccupancyChangeInterruptionStep,
    // Confirm missing compliance steps
    override val hasMissingComplianceStep: HasMissingComplianceStep,
    override val confirmMissingComplianceStep: ConfirmMissingComplianceStep,
    // Who-provides-details CYA change steps
    override val whoProvidesUpdateRoutingStep: WhoProvidesUpdateRoutingStep,
    override val confirmChangeToLettingAgentStep: ConfirmChangeToLettingAgentStep,
    // Save data step
    override val savePropertyRegistrationDataStep: SavePropertyRegistrationDataStep,
    journeyStateService: JourneyStateService,
    private val userToLandlordService: UserToLandlordService,
    override val stateFactory: ObjectFactory<PropertyRegistrationJourneyState>,
) : AbstractJourneyState(journeyStateService),
    PropertyRegistrationJourneyState {
    override var isStateInitialized: Boolean by delegateProvider.requiredDelegate("isStateInitialized", false)
    override var loggedInLandlordEmail: String? by delegateProvider.nullableDelegate("loggedInLandlordEmail")
    override var cachedOccupied: Boolean? by delegateProvider.nullableDelegate("cachedOccupied")

    // Hoists the who-provides answer onto the base journey state so the occupancy-change routing can read it
    // from the base journey. The who-provides step isn't declared in that CYA journey, so its urlPath is unset
    // and a form-model read would throw (see TODO PDJB-585). Populated in WhoProvidesRentalDetailsStepConfig.afterStepDataIsAdded.
    override var cachedWhoProvidesRentalDetails: WhoProvidesRentalDetails? by
        delegateProvider.nullableDelegate("cachedWhoProvidesRentalDetails")
    override val householdsAndTenantsDependencies = HouseHoldsAndTenantsDependencies(true)
    override var cyaJourneys: Map<String, String> = mapOf()
    override var originalJourneyUpdated: Instant? by delegateProvider.nullableDelegate("originalJourneyUpdated")

    override var checkingAnswersFor: String? by delegateProvider.nullableDelegate("checkingAnswersFor")

    override var cyaUrlPath: String? by delegateProvider.nullableDelegate("cyaRouteSegment")

    // Reads cachedOccupied first; falls back to the step's submitted form data when the upstream step
    // is wired up (parent journey context). The cache is essential when a CYA child journey reads this
    // value: in that context only steps in the child's journey graph are wired up, so the fallback would
    // throw. The cache is populated on step submission (see OccupiedStepConfig.afterStepDataIsAdded).
    override val isOccupied: Boolean
        get() {
            cachedOccupied?.let { return it }
            return occupied.formModelOrNull?.occupied
                ?: throw PrsdbWebException("Cannot use isOccupied until after the occupation step")
        }

    // Legacy steps and tasks that should be removed when the legacy structure is retired
    override val bedrooms = propertyDetailsTask.bedrooms
    override val householdsAndTenantsTask = tenancyDetailsTask.householdsAndTenantsTask
    override val rentIncludesBillsTask = tenancyDetailsTask.rentIncludesBillsTask
    override val rentFrequencyAndAmountTask = tenancyDetailsTask.rentFrequencyAndAmountTask
    override val furnishedStatus = tenancyDetailsTask.furnishedStatus

    override var registrationNumberValue: Long? by delegateProvider.nullableDelegate("registrationNumberValue")

    // Cache reasoning matches isOccupied above. The cached value is the raw selected address string so we can
    // distinguish "not yet submitted" (null) from "manual address chosen" (cached non-null but resolves to no UPRN).
    // Cache is populated on step submission (see SelectAddressStepConfig.afterStepDataIsAdded).
    override val uprn: Long?
        get() {
            val addressTask = propertyDetailsTask.addressTask
            addressTask.cachedSelectedAddress?.let { return addressTask.getMatchingAddress(it)?.uprn }
            val submittedAddress = addressTask.selectAddressStep.formModelOrNull?.address ?: return null
            return addressTask.getMatchingAddress(submittedAddress)?.uprn
        }

    override var backUrlKey: Int? by delegateProvider.nullableDelegate("backUrlKey")

    override val allowProvideCertificateLaterRoute: Boolean = true
    override val allowProvideLicensingLaterRoute: Boolean = true

    override fun generateJourneyId(seed: Any?): String {
        val user = seed as? Principal

        return super<AbstractJourneyState>.generateJourneyId(user?.let { generateSeedForUser(it) } ?: seed)
    }

    companion object {
        fun generateSeedForUser(user: Principal): String = "Prop reg journey for user ${user.name} at time ${System.currentTimeMillis()}"
    }
}

interface PropertyRegistrationJourneyState :
    OccupationState,
    InviteJointLandlordsTaskDependencies,
    GasSafetyDependencies,
    ElectricalSafetyDependencies,
    EpcDependencies,
    LicensingDependencies,
    WhoProvidesDetailsDependencies,
    CorrespondenceDependencies,
    CombinedComplianceCheckState,
    CheckYourAnswersJourneyState {
    var isStateInitialized: Boolean
    override var loggedInLandlordEmail: String?
    val taskListStep: PropertyRegistrationTaskListStep
    val licensingTask: LicensingTask

    // No property ownership exists yet during registration.
    override val propertyOwnershipId: Long? get() = null

    // Journey-structure tasks (the two alternative flows)
    // Legacy journey only (flag-off) — remove with the old journey
    val occupationTask: OccupationTask

    // Restructured journey only (flag-on)
    val propertyDetailsTask: PropertyDetailsTask
    val ownershipAndLandlordsTask: OwnershipAndLandlordsTask

    val correspondenceTask: CorrespondenceTask
    val tenancyDetailsTask: TenancyDetailsTask
    val whoProvidesDetailsTask: WhoProvidesDetailsTask
    override val finishCyaStep: FinishCyaJourneyStep
    override val gasSafetyTask: GasSafetyTask
    override val electricalSafetyTask: ElectricalSafetyTask
    override val epcTask: EpcTask
    override val cyaStep: PropertyRegistrationCyaStep
    val hasMissingComplianceStep: HasMissingComplianceStep
    val confirmMissingComplianceStep: ConfirmMissingComplianceStep
    val whoProvidesUpdateRoutingStep: WhoProvidesUpdateRoutingStep
    val confirmChangeToLettingAgentStep: ConfirmChangeToLettingAgentStep
    val savePropertyRegistrationDataStep: SavePropertyRegistrationDataStep
    val occupancyChangeRoutingStep: OccupancyChangeRoutingStep
    val occupancyChangeInterruptionStep: OccupancyChangeInterruptionStep
    var registrationNumberValue: Long?
    var backUrlKey: Int?

    // Check both flags before reading the step outcome: the who-provides step is not wired into
    // the legacy journey, so accessing its outcome there would throw.
    fun isDelegatedToLettingAgent(featureFlagManager: FeatureFlagManager): Boolean =
        featureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT) &&
            featureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING) &&
            whoProvidesDetailsTask.whoProvidesRentalDetailsStep.outcome == WhoProvidesRentalDetailsMode.LETTING_AGENT_PROVIDES
}
