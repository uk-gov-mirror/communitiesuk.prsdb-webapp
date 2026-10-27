package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import jakarta.servlet.http.HttpServletRequest
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.ASSOCIATED_PARTIES
import uk.gov.communities.prsdb.webapp.constants.PAYMENTS
import uk.gov.communities.prsdb.webapp.constants.WITH_BACK_URL_PARAMETER_NAME
import uk.gov.communities.prsdb.webapp.constants.enums.TaskStatus
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.Task
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.PropertyRegistrationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NoInputFormModel
import uk.gov.communities.prsdb.webapp.models.viewModels.taskModels.TaskListItemViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.taskModels.TaskListViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.taskModels.TaskSectionViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.taskModels.TaskStatusViewModel
import uk.gov.communities.prsdb.webapp.services.BackUrlStorageService

@JourneyFrameworkComponent
class PropertyRegistrationTaskListStepConfig(
    private val featureFlagManager: FeatureFlagManager,
    private val httpServletRequest: HttpServletRequest,
    private val backUrlStorageService: BackUrlStorageService,
) : AbstractRequestableStepConfig<Complete, NoInputFormModel, PropertyRegistrationJourneyState>() {
    override val formModelClass = NoInputFormModel::class

    override fun getStepSpecificContent(state: PropertyRegistrationJourneyState): Map<String, Any> =
        mapOf("taskListViewModel" to getTaskListViewModel(state))

    fun getTaskListViewModel(state: PropertyRegistrationJourneyState): TaskListViewModel {
        val backRequestUrl: Int? = httpServletRequest.getParameter(WITH_BACK_URL_PARAMETER_NAME)?.toIntOrNull()
        if (backRequestUrl != null) {
            state.backUrlKey = backRequestUrl
        }

        val sectionViewModels = sectionViewModels(state)

        val backUrlFromState =
            state
                .backUrlKey
                ?.let { backUrlStorageService.getBackUrl(it) }

        return TaskListViewModel(
            "registerProperty.taskList.heading",
            listOf("registerProperty.taskList.subtitle"),
            sectionViewModels,
            numberSections = false,
            backUrl = backUrlFromState,
        )
    }

    private fun sectionViewModels(state: PropertyRegistrationJourneyState): List<TaskSectionViewModel> =
        listOf(
            TaskSectionViewModel(
                "registerProperty.taskList.aboutYourProperty.heading",
                "about-your-property",
                buildList {
                    add(
                        TaskListItemViewModel.fromTask(
                            "registerProperty.taskList.aboutYourProperty.propertyDetails",
                            state.propertyDetailsTask,
                        ),
                    )
                    add(
                        TaskListItemViewModel.fromTask(
                            "registerProperty.taskList.aboutYourProperty.ownershipAndLandlords",
                            // TODO PDJB-955: use ownershipAndLandlordsTask directly once ASSOCIATED_PARTIES is released
                            if (featureFlagManager.checkFeature(ASSOCIATED_PARTIES)) {
                                state.ownershipAndLandlordsTask
                            } else {
                                state.ownershipAndLandlordsTaskBeforeAssociatedParties
                            },
                        ),
                    )
                    add(
                        TaskListItemViewModel.fromTask(
                            "registerProperty.taskList.aboutYourProperty.correspondence",
                            state.correspondenceTask,
                        ),
                    )
                    add(
                        TaskListItemViewModel.fromStep(
                            "registerProperty.taskList.aboutYourProperty.occupied",
                            state.occupied,
                        ),
                    )
                },
            ),
            TaskSectionViewModel(
                "registerProperty.taskList.rentedOut.heading",
                "rented-out",
                buildList {
                    add(whoProvidesDetailsItem(state))
                    add(rentedOutComplianceItem("registerProperty.taskList.rentedOut.licensing", state.licensingTask, state))
                    add(rentedOutComplianceItem("registerProperty.taskList.gasSafety", state.gasSafetyTask, state))
                    add(rentedOutComplianceItem("registerProperty.taskList.electricalSafety", state.electricalSafetyTask, state))
                    add(rentedOutComplianceItem("registerProperty.taskList.epc", state.epcTask, state))
                    add(tenancyDetailsItem(state))
                },
            ),
            TaskSectionViewModel(
                "registerProperty.taskList.submitYourRegistration.heading",
                "submit-your-registration",
                listOf(
                    TaskListItemViewModel.fromStep(
                        if (featureFlagManager.checkFeature(PAYMENTS)) {
                            "registerProperty.taskList.checkAndSubmit.checkAnswersWithPayment"
                        } else {
                            "registerProperty.taskList.checkAndSubmit.checkAnswers"
                        },
                        state.cyaStep,
                    ),
                ),
            ),
        )

    private fun tenancyDetailsItem(state: PropertyRegistrationJourneyState): TaskListItemViewModel =
        if (state.cachedOccupied == false) {
            TaskListItemViewModel(
                nameKey = "registerProperty.taskList.rentedOut.tenancyDetails",
                status = TaskStatusViewModel.fromStatus(TaskStatus.NOT_NEEDED_YET),
                hintKey = "registerProperty.taskList.rentedOut.tenancyDetailsNotNeededYetHint",
                url = null,
            )
        } else if (state.isDelegatedToLettingAgent()) {
            notRequiredLettingAgentItem("registerProperty.taskList.rentedOut.tenancyDetails")
        } else {
            TaskListItemViewModel.fromTask("registerProperty.taskList.rentedOut.tenancyDetails", state.tenancyDetailsTask)
        }

    private fun whoProvidesDetailsItem(state: PropertyRegistrationJourneyState): TaskListItemViewModel =
        if (state.cachedOccupied == false) {
            TaskListItemViewModel(
                nameKey = "registerProperty.taskList.rentedOut.whoProvidesDetails",
                status = TaskStatusViewModel.fromStatus(TaskStatus.NOT_NEEDED_YET),
                hintKey = "registerProperty.taskList.rentedOut.whoProvidesDetailsNotRequiredHint",
                url = null,
            )
        } else {
            TaskListItemViewModel.fromTask(
                "registerProperty.taskList.rentedOut.whoProvidesDetails",
                state.whoProvidesDetailsTask,
                "registerProperty.taskList.rentedOut.whoProvidesDetailsOccupiedHint",
            )
        }

    private fun rentedOutComplianceItem(
        nameKey: String,
        task: Task<*, *>,
        state: PropertyRegistrationJourneyState,
    ): TaskListItemViewModel =
        if (state.isDelegatedToLettingAgent()) {
            notRequiredLettingAgentItem(nameKey)
        } else {
            TaskListItemViewModel.fromTask(nameKey, task)
        }

    private fun notRequiredLettingAgentItem(nameKey: String): TaskListItemViewModel =
        TaskListItemViewModel(
            nameKey = nameKey,
            status = TaskStatusViewModel.fromStatus(TaskStatus.NOT_REQUIRED),
            hintKey = "registerProperty.taskList.rentedOut.lettingAgentProvidesHint",
            url = null,
        )

    override fun chooseTemplate(state: PropertyRegistrationJourneyState): String = "taskList"

    override fun mode(state: PropertyRegistrationJourneyState): Nothing? = null
}

@JourneyFrameworkComponent
final class PropertyRegistrationTaskListStep(
    stepConfig: PropertyRegistrationTaskListStepConfig,
) : RequestableStep<Complete, NoInputFormModel, PropertyRegistrationJourneyState>(stepConfig)
