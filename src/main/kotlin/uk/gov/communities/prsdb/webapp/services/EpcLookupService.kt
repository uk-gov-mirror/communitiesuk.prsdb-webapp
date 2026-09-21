package uk.gov.communities.prsdb.webapp.services

import org.json.JSONObject
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.clients.EpcRegisterClient
import uk.gov.communities.prsdb.webapp.models.dataModels.EpcDataModel

@PrsdbWebService
class EpcLookupService(
    private val epcRegisterClient: EpcRegisterClient,
) {
    fun getEpcByCertificateNumber(certificateNumber: String): EpcDataModel? {
        val formattedCertificateNumber =
            EpcDataModel.parseCertificateNumberOrNull(certificateNumber)
                ?: throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Certificate number should be of the form XXXX-XXXX-XXXX-XXXX-XXXX",
                )
        val response = epcRegisterClient.getByRrn(formattedCertificateNumber)
        val jsonResponse = JSONObject(response)

        return if (requestSuccessful(jsonResponse)) {
            EpcDataModel.fromJsonObject(jsonResponse)
        } else {
            null
        }
    }

    fun getEpcByUprn(uprn: Long): EpcDataModel? {
        val response = epcRegisterClient.getByUprn(uprn)
        val jsonResponse = JSONObject(response)

        return if (requestSuccessful(jsonResponse)) {
            EpcDataModel.fromJsonObject(jsonResponse)
        } else {
            null
        }
    }

    private fun requestSuccessful(jsonResponse: JSONObject): Boolean {
        if (jsonResponse.has("errors")) {
            val errorCode = getErrorCode(jsonResponse)
            if (errorCode == "NOT_FOUND") {
                return false
            }
            if (errorCode == "INVALID_REQUEST" || errorCode == "BAD_REQUEST") {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, getErrorMessage(jsonResponse))
            }
        }
        return true
    }

    companion object {
        fun getErrorCode(jsonObject: JSONObject): String? {
            val errors = jsonObject.getJSONArray("errors")
            return if (errors.count() == 1) {
                errors.getJSONObject(0).getString("code")
            } else {
                null
            }
        }

        fun setErrorCode(
            jsonObject: JSONObject,
            code: String,
        ) {
            val errors = jsonObject.getJSONArray("errors")
            if (errors.count() == 1) {
                errors.getJSONObject(0).put("code", code)
            }
        }

        fun getErrorMessage(jsonObject: JSONObject): String? {
            val errors = jsonObject.getJSONArray("errors")
            return if (errors.count() == 1) {
                errors.getJSONObject(0).getString("title")
            } else {
                null
            }
        }
    }
}
