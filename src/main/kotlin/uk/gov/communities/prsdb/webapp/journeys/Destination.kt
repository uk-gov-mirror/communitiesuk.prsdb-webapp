package uk.gov.communities.prsdb.webapp.journeys

import org.springframework.http.HttpStatus
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.support.RequestContextUtils
import org.springframework.web.util.UriComponentsBuilder
import kotlin.collections.plus

sealed class Destination {
    abstract fun toModelAndView(): ModelAndView

    abstract fun toUrlStringOrNull(): String?

    open fun withModelContent(content: Map<String, Any?>): Destination = this

    open fun withUrlParameter(
        parameterName: String,
        parameterValue: String,
    ): Destination = this

    fun withUrlParameter(parameterPair: Pair<String, String>): Destination = withUrlParameter(parameterPair.first, parameterPair.second)

    class VisitableStep(
        val step: JourneyStep.RequestableStep<*, *, *>,
        val journeyId: String,
    ) : Destination() {
        var urlParams: Map<String, String> = mapOf()
            private set

        override fun withUrlParameter(
            parameterName: String,
            parameterValue: String,
        ): VisitableStep {
            urlParams += (parameterName to parameterValue)
            return this
        }

        override fun toModelAndView() =
            ModelAndView("redirect:${JourneyStateService.urlWithJourneyState(step.urlPath, journeyId)}", mapOf<String, String>())

        override fun toUrlStringOrNull() =
            if (step.isStepReachable) JourneyStateService.urlWithJourneyState(step.urlPath, journeyId, urlParams) else null
    }

    class StepRoute(
        val routeSegment: String,
        val journeyId: String,
    ) : Destination() {
        var urlParams: Map<String, String> = mapOf()
            private set

        override fun withUrlParameter(
            parameterName: String,
            parameterValue: String,
        ): StepRoute {
            urlParams += (parameterName to parameterValue)
            return this
        }

        override fun toModelAndView() =
            ModelAndView("redirect:${JourneyStateService.urlWithJourneyState(routeSegment, journeyId)}", mapOf<String, String>())

        override fun toUrlStringOrNull() = JourneyStateService.urlWithJourneyState(routeSegment, journeyId, urlParams)
    }

    class ExternalUrl(
        val externalUrl: String,
        params: Map<String, String> = mapOf(),
    ) : Destination() {
        var params: Map<String, String> = params
            private set

        var flashAttributes: Map<String, String> = mapOf()
            private set

        override fun toModelAndView(): ModelAndView {
            if (flashAttributes.isNotEmpty()) {
                val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
                RequestContextUtils.getOutputFlashMap(request).putAll(flashAttributes)
            }
            return ModelAndView("redirect:$externalUrl", params)
        }

        override fun withUrlParameter(
            parameterName: String,
            parameterValue: String,
        ): ExternalUrl {
            params += (parameterName to parameterValue)
            return this
        }

        fun withFlashAttribute(
            attributeName: String,
            attributeValue: String,
        ): ExternalUrl {
            flashAttributes += (attributeName to attributeValue)
            return this
        }

        override fun toUrlStringOrNull() =
            UriComponentsBuilder
                .fromUriString(externalUrl)
                .apply {
                    params.forEach { (key, value) -> queryParam(key, value) }
                }.toUriString()
    }

    class Template(
        val templateName: String,
        val content: Map<String, Any?> = mapOf(),
    ) : Destination() {
        override fun toModelAndView() = ModelAndView(templateName, content)

        override fun withModelContent(content: Map<String, Any?>) = Template(templateName, this.content + content)

        override fun toUrlStringOrNull() = null
    }

    class NavigationalStep(
        val step: JourneyStep.InternalStep<*, *>,
    ) : Destination() {
        override fun toModelAndView() = step.lifecycleOrchestrator.getStepModelAndView()

        override fun toUrlStringOrNull() = if (step.isStepReachable) step.getNextDestination().toUrlStringOrNull() else null
    }

    class Nowhere : Destination() {
        override fun toModelAndView(): ModelAndView = throw ResponseStatusException(HttpStatus.NOT_FOUND, "Navigated to Nowhere")

        override fun toUrlStringOrNull(): String? = null
    }

    companion object {
        operator fun invoke(step: JourneyStep<*, *, *>?): Destination =
            when (step) {
                is JourneyStep.RequestableStep -> VisitableStep(step, step.currentJourneyId)
                is JourneyStep.InternalStep -> NavigationalStep(step)
                null -> Nowhere()
            }
    }
}
