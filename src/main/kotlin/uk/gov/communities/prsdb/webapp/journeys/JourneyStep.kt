package uk.gov.communities.prsdb.webapp.journeys

import org.springframework.beans.MutablePropertyValues
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import uk.gov.communities.prsdb.webapp.constants.BACK_URL_ATTR_NAME
import uk.gov.communities.prsdb.webapp.exceptions.JourneyInitialisationException
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.FormModel
import kotlin.reflect.cast
import kotlin.reflect.full.createInstance

sealed class JourneyStep<out TEnum : Enum<out TEnum>, TFormModel : FormModel, in TState : JourneyState>(
    val stepConfig: AbstractStepConfig<TEnum, TFormModel, TState>,
) {
    open class RequestableStep<out TEnum : Enum<out TEnum>, TFormModel : FormModel, in TState : JourneyState>(
        stepConfig: AbstractRequestableStepConfig<TEnum, TFormModel, TState>,
    ) : JourneyStep<TEnum, TFormModel, TState>(stepConfig) {
        val urlPath: String get() = stepConfig.urlPath

        override fun getUrlPathOrNull(): String? = stepConfig.urlPath

        override fun isUrlPathInitialised(): Boolean = stepConfig.isUrlPathInitialised()

        override fun initialiseStepPath(path: String?) {
            if (isUrlPathInitialised()) {
                throw JourneyInitialisationException("urlPath is already initialised")
            }
            if (path == null) {
                throw JourneyInitialisationException("urlPath cannot be null for a requestable step")
            }

            stepConfig.urlPath = path
        }

        override fun submitFormData(bindingResult: BindingResult) =
            addStepData(stepConfig.stepDataKey, stepConfig.formModelClass.cast(bindingResult.target).toPageData())

        fun clearFormData() = clearFormData(stepConfig.stepDataKey)
    }

    open class InternalStep<out TEnum : Enum<out TEnum>, in TState : JourneyState>(
        stepConfig: AbstractInternalStepConfig<TEnum, TState>,
    ) : JourneyStep<TEnum, Nothing, TState>(stepConfig) {
        override fun getUrlPathOrNull(): String? = null

        override fun isUrlPathInitialised(): Boolean = true

        override fun initialiseStepPath(path: String?) {
            path?.let {
                throw JourneyInitialisationException(
                    "step path cannot be set for an internal step - was set to $path",
                )
            }
        }

        override fun submitFormData(bindingResult: BindingResult) {}
    }

    abstract fun getUrlPathOrNull(): String?

    abstract fun isUrlPathInitialised(): Boolean

    protected abstract fun initialiseStepPath(path: String?)

    abstract fun submitFormData(bindingResult: BindingResult)

    fun attemptToReachStep(): Boolean {
        if (stepConfig.beforeAttemptingToReachStep(state) && isStepReachable) {
            stepConfig.afterStepIsReached(state)
            return true
        }
        stepConfig.whenStepIsUnreachable(state)
        return false
    }

    fun getPageVisitContent(): Map<String, Any?> {
        val contentWithFormModel =
            stepConfig.getStepSpecificContent(state) +
                additionalContentProvider() +
                mapOf(
                    BACK_URL_ATTR_NAME to backUrl,
                    "formModel" to (formModelOrNull ?: stepConfig.formModelClass.createInstance()),
                )
        return stepConfig.resolvePageContent(state, contentWithFormModel)
    }

    fun chooseTemplate(): Destination {
        val templateName = stepConfig.chooseTemplate(state)
        return stepConfig.resolveChosenTemplate(state, templateName)
    }

    fun validateSubmittedData(submittedData: FormData): BindingResult {
        val enrichedFormData = stepConfig.enrichSubmittedDataBeforeValidation(state, submittedData)

        val binder = WebDataBinder(stepConfig.formModelClass.createInstance())
        binder.validator = stepConfig.validator
        binder.bind(MutablePropertyValues(enrichedFormData))
        binder.validate()

        stepConfig.afterPrimaryValidation(state, binder.bindingResult)
        return binder.bindingResult
    }

    protected fun addStepData(
        stepDataKey: String,
        data: FormData,
    ) {
        stepConfig.beforeStepDataIsAdded(state, data)
        val dataToStore = stepConfig.enrichStepDataBeforeItIsAdded(state, data)
        state.addStepData(stepDataKey, dataToStore)
        stepConfig.afterStepDataIsAdded(state)
    }

    fun clearFormData(stepDataKey: String) {
        state.clearStepData(stepDataKey)
    }

    fun saveStateIfAllowed() {
        if (shouldSaveOnCompletion) {
            stepConfig.beforeSaveState(state)
            val savedState = stepConfig.saveState(state)
            stepConfig.afterSaveState(state, savedState)
        }
    }

    fun getNextDestination(): Destination {
        stepConfig.beforeChoosingNextDestination(state)
        val defaultDestination =
            stepConfig.mode(state)?.let { nextDestination(it) }
                ?: throw UnrecoverableJourneyStateException(currentJourneyId, "Determining next destination failed - step mode is null")
        return stepConfig.resolveNextDestination(state, defaultDestination)
    }

    fun getInvalidSubmissionContent(bindingResult: BindingResult): Map<String, Any?> {
        val contentWithBindingResult =
            stepConfig.getStepSpecificContent(state) +
                additionalContentProvider() +
                mapOf(
                    BACK_URL_ATTR_NAME to backUrl,
                    BindingResult.MODEL_KEY_PREFIX + "formModel" to bindingResult,
                )
        return stepConfig.resolvePageContent(state, contentWithBindingResult)
    }

    fun getUnreachableStepDestination(): Destination {
        stepConfig.beforeChosingUnreachableStepDestination(state)
        val defaultDestination = unreachableStepDestination()
        return stepConfig.resolveUnreachableStepDestination(state, defaultDestination)
    }

    private lateinit var unreachableStepDestination: () -> Destination

    private var shouldSaveOnCompletion: Boolean = false

    val lifecycleOrchestrator: StepLifecycleOrchestrator
        get() = stepConfig.getStepLifecycleOrchestrator(this)

    val isStepReachable: Boolean by lazy(LazyThreadSafetyMode.NONE) { parentage.allowsChild() }

    val formModelOrNull: TFormModel?
        get() = stepConfig.getFormModelFromStateOrNull(state)

    val formModel: TFormModel
        get() = stepConfig.getFormModelFromState(state)

    val formModelIfReachableOrNull: TFormModel?
        get() = if (isStepReachable) formModelOrNull else null

    lateinit var parentage: Parentage

    private lateinit var state: TState

    val outcome: TEnum? by lazy(LazyThreadSafetyMode.NONE) { if (isStepReachable) stepConfig.mode(state) else null }

    private lateinit var nextDestination: (mode: TEnum) -> Destination

    private var backUrlOverride: (() -> Destination)? = null

    private var additionalContentProvider: () -> Map<String, Any?> = { mapOf() }

    // We use StepLifecycleOrchestrator type here as requestable steps with redirecting orchestrators can't be used as backUrls
    val backUrl: String?
        get() {
            val singleParentStep = parentage.allowingParentSteps.singleOrNull()
            val singleParentUrl =
                when (singleParentStep?.lifecycleOrchestrator) {
                    is StepLifecycleOrchestrator.RedirectingStepLifecycleOrchestrator -> singleParentStep.backUrl
                    is StepLifecycleOrchestrator.VisitableStepLifecycleOrchestrator -> Destination(singleParentStep).toUrlStringOrNull()
                    null -> null
                }
            val backUrlOverrideValue = this.backUrlOverride?.let { it().toUrlStringOrNull() }
            return if (backUrlOverride != null) backUrlOverrideValue else singleParentUrl
        }

    val initialisationStage: StepInitialisationStage
        get() =
            when {
                !isBaseClassInitialised -> StepInitialisationStage.UNINITIALISED
                isBaseClassInitialised && !stepConfig.isSubClassInitialised() -> StepInitialisationStage.PARTIALLY_INITIALISED
                else -> StepInitialisationStage.FULLY_INITIALISED
            }

    private val isBaseClassInitialised: Boolean
        get() =
            isUrlPathInitialised() && ::state.isInitialized && ::nextDestination.isInitialized && ::parentage.isInitialized

    fun initialize(
        path: String?,
        state: TState,
        backDestinationOverride: (() -> Destination)?,
        redirectDestinationProvider: (mode: TEnum) -> Destination,
        parentage: Parentage,
        unreachableStepDestinationProvider: () -> Destination,
        shouldSaveOnCompletion: Boolean,
        additionalContentProvider: (() -> Map<String, Any?>)? = null,
    ) {
        if (initialisationStage != StepInitialisationStage.UNINITIALISED) {
            throw JourneyInitialisationException("Step $this has already been initialised")
        }
        initialiseStepPath(path)
        this.state = state
        this.backUrlOverride = backDestinationOverride
        this.nextDestination = redirectDestinationProvider
        this.parentage = parentage
        this.unreachableStepDestination = unreachableStepDestinationProvider
        this.shouldSaveOnCompletion = shouldSaveOnCompletion
        additionalContentProvider?.let { this.additionalContentProvider = it }
    }

    val currentJourneyId: String
        get() = state.journeyId
}

enum class StepInitialisationStage {
    UNINITIALISED,
    PARTIALLY_INITIALISED,
    FULLY_INITIALISED,
}
