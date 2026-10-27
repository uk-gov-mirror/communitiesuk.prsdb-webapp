package uk.gov.communities.prsdb.webapp.helpers.converters

import uk.gov.communities.prsdb.webapp.constants.enums.BillsIncluded
import uk.gov.communities.prsdb.webapp.constants.enums.CharityRegulator
import uk.gov.communities.prsdb.webapp.constants.enums.ComplianceCertStatus
import uk.gov.communities.prsdb.webapp.constants.enums.EpcExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.FileUploadStatus
import uk.gov.communities.prsdb.webapp.constants.enums.FurnishedStatus
import uk.gov.communities.prsdb.webapp.constants.enums.GoverningBodyMemberType
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.MeesExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.OrgType
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.constants.enums.RentFrequency

class MessageKeyConverter {
    companion object {
        fun convert(boolean: Boolean): String =
            when (boolean) {
                true -> "commonText.yes"
                false -> "commonText.no"
            }

        fun convert(enum: Enum<*>): String =
            when (enum) {
                is PropertyType -> convertPropertyType(enum)

                is OwnershipType -> convertOwnershipType(enum)

                is LicensingType -> convertLicensingType(enum)

                is FurnishedStatus -> convertFurnishedStatus(enum)

                is GoverningBodyMemberType -> convertGoverningBodyMemberType(enum)

                is RentFrequency -> convertRentFrequency(enum)

                is BillsIncluded -> convertBillsIncluded(enum)

                is EpcExemptionReason -> convertEpcExemptionReason(enum)

                is MeesExemptionReason -> convertMeesExemptionReason(enum)

                is ComplianceCertStatus -> convertComplianceCertStatus(enum)

                is FileUploadStatus -> convertUploadStatus(enum)

                is OrgType -> convertOrgType(enum)

                is CharityRegulator -> convertCharityRegulator(enum)

                else -> throw NotImplementedError(
                    "Was not able to convert Enum as ${this::class.simpleName} does not have a conversion for ${enum::class.simpleName}",
                )
            }

        private fun convertGoverningBodyMemberType(type: GoverningBodyMemberType): String =
            when (type) {
                GoverningBodyMemberType.DIRECTOR -> "landlordDetails.org.governingBody.type.director"
                GoverningBodyMemberType.TRUSTEE -> "landlordDetails.org.governingBody.type.trustee"
                GoverningBodyMemberType.PARTNER -> "landlordDetails.org.governingBody.type.partner"
                GoverningBodyMemberType.OTHER -> "landlordDetails.org.governingBody.type.other"
            }

        private fun convertOrgType(orgType: OrgType): String =
            when (orgType) {
                OrgType.COMPANY -> "registerAsALandlord.orgType.company"
                OrgType.CHARITY -> "registerAsALandlord.orgType.charity"
                OrgType.TRUST -> "registerAsALandlord.orgType.trust"
                OrgType.NONE -> "registerAsALandlord.orgType.none"
            }

        private fun convertCharityRegulator(charityRegulator: CharityRegulator): String =
            when (charityRegulator) {
                CharityRegulator.ENGLAND_AND_WALES -> "forms.orgCharityRegisteredWith.radios.option.englandAndWales"
                CharityRegulator.NORTHERN_IRELAND -> "forms.orgCharityRegisteredWith.radios.option.northernIreland"
                CharityRegulator.SCOTLAND -> "forms.orgCharityRegisteredWith.radios.option.scotland"
                CharityRegulator.NONE -> "commonText.other"
            }

        private fun convertBillsIncluded(billsIncluded: BillsIncluded): String =
            when (billsIncluded) {
                BillsIncluded.GAS -> "forms.billsIncluded.checkbox.gas"
                BillsIncluded.ELECTRICITY -> "forms.billsIncluded.checkbox.electricity"
                BillsIncluded.WATER -> "forms.billsIncluded.checkbox.water"
                BillsIncluded.COUNCIL_TAX -> "forms.billsIncluded.checkbox.councilTax"
                BillsIncluded.LANDLINE -> "forms.billsIncluded.checkbox.landline"
                BillsIncluded.BROADBAND -> "forms.billsIncluded.checkbox.broadband"
                BillsIncluded.TV_LICENCE -> "forms.billsIncluded.checkbox.tvLicence"
                BillsIncluded.CABLE_OR_SATELLITE_TV -> "forms.billsIncluded.checkbox.cableSatelliteTV"
                BillsIncluded.PARKING -> "forms.billsIncluded.checkbox.parking"
                BillsIncluded.GARDENING -> "forms.billsIncluded.checkbox.gardening"
                BillsIncluded.CLEANER_FOR_COMMUNAL_AREAS -> "forms.billsIncluded.checkbox.communalAreasCleaner"
                BillsIncluded.SOMETHING_ELSE -> ""
            }

        private fun convertRentFrequency(rentFrequency: RentFrequency): String =
            when (rentFrequency) {
                RentFrequency.WEEKLY -> "forms.rentFrequency.radios.option.weekly.label"
                RentFrequency.FOUR_WEEKLY -> "forms.rentFrequency.radios.option.fourWeekly.label"
                RentFrequency.MONTHLY -> "forms.rentFrequency.radios.option.monthly.label"
                RentFrequency.OTHER -> ""
            }

        private fun convertFurnishedStatus(furnishedStatus: FurnishedStatus): String =
            when (furnishedStatus) {
                FurnishedStatus.FURNISHED -> "forms.furnishedStatus.radios.options.furnished.label"
                FurnishedStatus.PART_FURNISHED -> "forms.furnishedStatus.radios.options.partFurnished.label"
                FurnishedStatus.UNFURNISHED -> "forms.furnishedStatus.radios.options.unfurnished.label"
            }

        private fun convertLicensingType(licensingType: LicensingType): String =
            when (licensingType) {
                LicensingType.SELECTIVE_LICENCE -> "forms.licensingType.radios.option.selectiveLicence.label"
                LicensingType.HMO_MANDATORY_LICENCE -> "forms.licensingType.radios.option.hmoMandatory.label"
                LicensingType.HMO_ADDITIONAL_LICENCE -> "forms.licensingType.radios.option.hmoAdditional.label"
                LicensingType.NO_LICENSING -> "forms.checkPropertyAnswers.propertyDetails.noLicensing"
                LicensingType.PROVIDE_LATER -> "forms.checkPropertyAnswers.propertyDetails.licensingProvideLater"
            }

        private fun convertOwnershipType(ownershipType: OwnershipType): String =
            when (ownershipType) {
                OwnershipType.FREEHOLD -> "forms.ownershipType.radios.option.freehold.label"
                OwnershipType.LEASEHOLD -> "forms.ownershipType.radios.option.leasehold.label"
                OwnershipType.SHARE_OF_FREEHOLD -> "forms.ownershipType.radios.option.shareOfFreehold.label"
                OwnershipType.COMMONHOLD -> "forms.ownershipType.radios.option.commonhold.label"
            }

        private fun convertPropertyType(propertyType: PropertyType): String =
            when (propertyType) {
                PropertyType.OTHER -> "forms.propertyType.radios.option.other.label"
                PropertyType.DETACHED_HOUSE -> "forms.propertyType.radios.option.detachedHouse.label"
                PropertyType.SEMI_DETACHED_HOUSE -> "forms.propertyType.radios.option.semiDetachedHouse.label"
                PropertyType.TERRACED_HOUSE -> "forms.propertyType.radios.option.terracedHouse.label"
                PropertyType.FLAT -> "forms.propertyType.radios.option.flat.label"
            }

        private fun convertEpcExemptionReason(epcExemptionReason: EpcExemptionReason): String =
            when (epcExemptionReason) {
                EpcExemptionReason.ANNUAL_USE_LESS_THAN_4_MONTHS -> {
                    "propertyCompliance.epcTask.epcExemption.radios.annualUseLessThan4Months.label"
                }

                EpcExemptionReason.ANNUAL_ENERGY_CONSUMPTION_LESS_THAN_25_PERCENT -> {
                    "forms.epcExemptionReason.radios.annualEnergyConsumptionLessThan25Percent.label"
                }

                EpcExemptionReason.TEMPORARY_BUILDING -> {
                    "propertyCompliance.epcTask.epcExemption.radios.temporaryBuilding.label"
                }

                EpcExemptionReason.STANDALONE_SMALL_BUILDING -> {
                    "propertyCompliance.epcTask.epcExemption.radios.standaloneSmallBuilding.label"
                }

                EpcExemptionReason.DUE_FOR_DEMOLITION -> {
                    "propertyCompliance.epcTask.epcExemption.radios.dueForDemolition.label"
                }

                EpcExemptionReason.PROTECTED_ARCHITECTURAL_OR_HISTORICAL_MERIT -> {
                    "propertyCompliance.epcTask.epcExemption.radios.protectedArchitecturalOrHistoricalMerit.label"
                }
            }

        private fun convertMeesExemptionReason(meesExemptionReason: MeesExemptionReason): String =
            when (meesExemptionReason) {
                MeesExemptionReason.HIGH_COST -> {
                    "propertyCompliance.epcTask.meesExemptionReason.radios.highCost.label"
                }

                MeesExemptionReason.ALL_IMPROVEMENTS_MADE -> {
                    "propertyCompliance.epcTask.meesExemptionReason.radios.allImprovementsMade.label"
                }

                MeesExemptionReason.WALL_INSULATION -> {
                    "propertyCompliance.epcTask.meesExemptionReason.radios.wallInsulation.label"
                }

                MeesExemptionReason.THIRD_PARTY_CONSENT -> {
                    "propertyCompliance.epcTask.meesExemptionReason.radios.thirdPartyConsent.label"
                }

                MeesExemptionReason.PROPERTY_DEVALUATION -> {
                    "propertyCompliance.epcTask.meesExemptionReason.radios.propertyDevaluation.label"
                }

                MeesExemptionReason.NEW_LANDLORD -> {
                    "propertyCompliance.epcTask.meesExemptionReason.radios.newLandlord.label"
                }
            }

        // TODO PDJB-2006: Remove the ComplianceCertStatus conversion when the PROPERTY_REGISTRATION_PHASE_THREE flag is removed
        private fun convertComplianceCertStatus(complianceCertStatus: ComplianceCertStatus): String =
            when (complianceCertStatus) {
                ComplianceCertStatus.HAS_FAULTS -> "complianceActions.status.hasFaults"

                ComplianceCertStatus.PROVIDE_LATER -> "complianceActions.status.provideLater"

                ComplianceCertStatus.EXPIRED -> "complianceActions.status.expired"

                else -> throw IllegalStateException(
                    "${complianceCertStatus.name} should not be shown on the page and hence should not be converted to a message key",
                )
            }

        private fun convertUploadStatus(uploadStatus: FileUploadStatus): String =
            when (uploadStatus) {
                FileUploadStatus.SCANNED -> "uploads.checkUploads.status.scanned"
                FileUploadStatus.QUARANTINED -> "propertyCompliance.uploadedFile.virusScanPending"
                FileUploadStatus.DELETED -> "propertyCompliance.uploadedFile.virusScanFailed"
            }
    }
}
