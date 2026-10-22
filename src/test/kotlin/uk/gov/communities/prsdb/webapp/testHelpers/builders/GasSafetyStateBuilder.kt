package uk.gov.communities.prsdb.webapp.testHelpers.builders

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import uk.gov.communities.prsdb.webapp.constants.CONTINUE_BUTTON_ACTION_NAME
import uk.gov.communities.prsdb.webapp.constants.PROVIDE_THIS_LATER_BUTTON_ACTION_NAME
import uk.gov.communities.prsdb.webapp.helpers.DateTimeHelper
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.CertificateUpload
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CheckGasCertUploadsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CheckGasSafetyAnswersStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.GasCertExpiredStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.GasCertIssueDateStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.GasCertMissingStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasCertStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasSupplyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ProvideGasCertLaterStep
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.FormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.GasSupplyFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.HasGasCertFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NoInputFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.TodayOrPastDateFormModel

interface GasSafetyStateBuilder<SelfType : GasSafetyStateBuilder<SelfType>> {
    val additionalDataMap: MutableMap<String, String>

    fun withSubmittedValue(
        key: String,
        value: FormModel,
    ): SelfType

    fun self(): SelfType

    fun withNoGasSupply(): SelfType {
        val hasGasSupplyFormModel =
            GasSupplyFormModel().apply {
                hasGasSupply = false
                action = CONTINUE_BUTTON_ACTION_NAME
            }
        withSubmittedValue(HasGasSupplyStep.ROUTE_SEGMENT, hasGasSupplyFormModel)
        return self()
    }

    fun withGasSupply(): SelfType {
        val hasGasSupplyFormModel =
            GasSupplyFormModel().apply {
                hasGasSupply = true
                action = CONTINUE_BUTTON_ACTION_NAME
            }
        withSubmittedValue(HasGasSupplyStep.ROUTE_SEGMENT, hasGasSupplyFormModel)
        return self()
    }

    fun withGasSafetyTaskCompletedWithNoGasSupply(): SelfType {
        withNoGasSupply()
        withSubmittedValue(CheckGasSafetyAnswersStep.ROUTE_SEGMENT, NoInputFormModel())
        return self()
    }

    fun withGasSafetyTaskCompletedWithUploadedCert(): SelfType {
        withGasSupply()
        withGasCertificate()
        withGasCertIssueDate()
        withGasCertUploads()
        withSubmittedValue(CheckGasSafetyAnswersStep.ROUTE_SEGMENT, NoInputFormModel())
        return self()
    }

    fun withGasCertificate(): SelfType {
        val hasGasCertificateFormModel =
            HasGasCertFormModel().apply {
                hasCert = true
            }
        withSubmittedValue(HasGasCertStep.ROUTE_SEGMENT, hasGasCertificateFormModel)
        return self()
    }

    fun withNoGasCertificate(): SelfType {
        val hasGasCertFormModel =
            HasGasCertFormModel().apply {
                hasCert = false
            }
        withSubmittedValue(HasGasCertStep.ROUTE_SEGMENT, hasGasCertFormModel)
        withSubmittedValue(GasCertMissingStep.ROUTE_SEGMENT, NoInputFormModel())
        return self()
    }

    fun withProvideGasCertLaterFromGasSupply(): SelfType {
        val hasGasSupplyFormModel =
            GasSupplyFormModel().apply {
                action = PROVIDE_THIS_LATER_BUTTON_ACTION_NAME
            }
        withSubmittedValue(HasGasSupplyStep.ROUTE_SEGMENT, hasGasSupplyFormModel)
        withSubmittedValue(ProvideGasCertLaterStep.ROUTE_SEGMENT, NoInputFormModel())
        return self()
    }

    fun withGasCertIssueDate(issueDate: LocalDate = DateTimeHelper().getCurrentDateInUK()): SelfType {
        val formModel =
            TodayOrPastDateFormModel().apply {
                day = issueDate.dayOfMonth.toString()
                month = issueDate.monthNumber.toString()
                year = issueDate.year.toString()
            }
        withSubmittedValue(GasCertIssueDateStep.ROUTE_SEGMENT, formModel)
        return self()
    }

    fun withGasCertUploads(
        uploads: Map<Int, CertificateUpload> = mapOf(1 to CertificateUpload(fileUploadId = 1L, fileName = "gas-safety-cert.pdf")),
    ): SelfType {
        additionalDataMap["gasUploadMap"] = Json.encodeToString(serializer(), uploads)
        additionalDataMap["nextGasUploadMemberId"] = Json.encodeToString(serializer(), (uploads.keys.max()) + 1)
        withSubmittedValue(CheckGasCertUploadsStep.ROUTE_SEGMENT, NoInputFormModel())
        return self()
    }

    fun withGasCertExpiredAcknowledged(): SelfType {
        withSubmittedValue(GasCertExpiredStep.ROUTE_SEGMENT, NoInputFormModel())
        return self()
    }
}
