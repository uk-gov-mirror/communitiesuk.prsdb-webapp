package uk.gov.communities.prsdb.webapp.services

import jakarta.transaction.Transactional
import org.springframework.dao.QueryTimeoutException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.server.ResponseStatusException
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.MAX_ENTRIES_IN_PROPERTIES_SEARCH_PAGE
import uk.gov.communities.prsdb.webapp.constants.enums.FurnishedStatus
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.constants.enums.RegistrationNumberType
import uk.gov.communities.prsdb.webapp.constants.enums.RentFrequency
import uk.gov.communities.prsdb.webapp.database.entity.Address
import uk.gov.communities.prsdb.webapp.database.entity.Landlord
import uk.gov.communities.prsdb.webapp.database.entity.LettingAgentAccess
import uk.gov.communities.prsdb.webapp.database.entity.License
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import uk.gov.communities.prsdb.webapp.database.repository.LettingAgentAccessRepository
import uk.gov.communities.prsdb.webapp.database.repository.PropertyOwnershipRepository
import uk.gov.communities.prsdb.webapp.exceptions.RepositoryQueryTimeoutException
import uk.gov.communities.prsdb.webapp.exceptions.UpdateConflictException
import uk.gov.communities.prsdb.webapp.helpers.AddressHelper
import uk.gov.communities.prsdb.webapp.helpers.DateTimeHelper
import uk.gov.communities.prsdb.webapp.helpers.TransactionHelper.Companion.runAfterTransactionCommits
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.RegistrationNumberDataModel
import uk.gov.communities.prsdb.webapp.models.viewModels.searchResultModels.PropertySearchResultViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.RegisteredPropertyLandlordViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.RegisteredPropertyLocalCouncilViewModel
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@PrsdbWebService
class PropertyOwnershipService(
    private val propertyOwnershipRepository: PropertyOwnershipRepository,
    private val registrationNumberService: RegistrationNumberService,
    private val localCouncilDataService: LocalCouncilDataService,
    private val licenseService: LicenseService,
    private val backLinkService: BackUrlStorageService,
    private val jointLandlordOtherLandlordLeftEmailService: JointLandlordOtherLandlordLeftEmailService,
    private val userToLandlordService: UserToLandlordService,
    private val lettingAgentAccessService: LettingAgentAccessService,
    private val lettingAgentAccessRepository: LettingAgentAccessRepository,
    private val featureFlagManager: FeatureFlagManager,
    private val addressService: AddressService,
) {
    @Transactional
    fun createPropertyOwnership(
        ownershipType: OwnershipType,
        isOccupied: Boolean,
        numberOfHouseholds: Int,
        numberOfPeople: Int,
        landlords: MutableSet<Landlord>,
        propertyBuildType: PropertyType,
        address: Address,
        license: License? = null,
        isActive: Boolean = true,
        numBedrooms: Int?,
        billsIncludedList: String?,
        customBillsIncluded: String?,
        furnishedStatus: FurnishedStatus?,
        rentFrequency: RentFrequency?,
        customRentFrequency: String?,
        rentAmount: BigDecimal?,
        customPropertyType: String?,
        markedJointLandlord: Boolean = false,
        licenseProvideLater: Boolean? = null,
        tenancyProvideLater: Boolean? = null,
    ): PropertyOwnership {
        val registrationNumber = registrationNumberService.createRegistrationNumber(RegistrationNumberType.PROPERTY)
        val registeringLandlord = landlords.first()

        return propertyOwnershipRepository.save(
            PropertyOwnership(
                ownershipType = ownershipType,
                currentNumHouseholds = numberOfHouseholds,
                currentNumTenants = numberOfPeople,
                isOccupied = isOccupied,
                registrationNumber = registrationNumber,
                landlords = landlords,
                propertyBuildType = propertyBuildType,
                customPropertyType = customPropertyType,
                address = address,
                license = license,
                // TODO PDJB-1593: Use journey correspondence address and email when the flag is on; keep landlord defaults when off.
                // TODO PDJB-1733: Remove the flag-off correspondence defaults.
                correspondenceEmail = registeringLandlord.email,
                correspondenceAddress = registeringLandlord.address,
                isActive = isActive,
                numBedrooms = numBedrooms,
                billsIncludedList = billsIncludedList,
                customBillsIncluded = customBillsIncluded,
                furnishedStatus = furnishedStatus,
                rentFrequency = rentFrequency,
                customRentFrequency = customRentFrequency,
                rentAmount = rentAmount,
                markedJointLandlord = markedJointLandlord,
                licenseProvideLater = licenseProvideLater,
                tenancyProvideLater = tenancyProvideLater,
            ).apply {
                if (isOccupied) lastOccupiedDate = LocalDate.now(DateTimeHelper.UK_ZONE)
            },
        )
    }

    fun getPropertyOwnershipIfCurrentUserAuthorized(propertyOwnershipId: Long): PropertyOwnership {
        val propertyOwnership = getPropertyOwnership(propertyOwnershipId)
        val baseUserId = SecurityContextHolder.getContext().authentication.name
        val landlord = userToLandlordService.getCurrentLandlordForUserOrNull()

        val isLocalCouncil = localCouncilDataService.getIsLocalCouncilUser(baseUserId)

        if (isLocalCouncil) return propertyOwnership

        val isLandlord = landlord != null && propertyOwnership.landlords.any { it.id == landlord.id }

        if (isLandlord) return propertyOwnership

        throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "The current user is not authorised to view property ownership $propertyOwnershipId",
        )
    }

    fun getPropertyOwnership(propertyOwnershipId: Long): PropertyOwnership =
        retrievePropertyOwnershipById(propertyOwnershipId)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Property ownership $propertyOwnershipId not found",
            )

    fun getLastModifiedDate(propertyOwnershipId: Long): Instant = getPropertyOwnership(propertyOwnershipId).getMostRecentlyUpdated()

    fun getLettingAgentAccess(propertyOwnershipId: Long): LettingAgentAccess? {
        if (!hasLettingAgent(propertyOwnershipId)) return null

        return lettingAgentAccessRepository.findByPropertyOwnershipId(propertyOwnershipId)
    }

    fun hasLettingAgent(propertyOwnershipId: Long): Boolean {
        if (!featureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)) return false

        val propertyOwnership = getPropertyOwnership(propertyOwnershipId)
        val lettingAgentAccess = lettingAgentAccessRepository.findByPropertyOwnershipId(propertyOwnershipId)

        return hasLettingAgent(propertyOwnership, lettingAgentAccess)
    }

    fun getCurrentUserIsAuthorizedToEditRecord(propertyOwnershipId: Long): Boolean {
        if (isCurrentUserLandlord(propertyOwnershipId)) return true

        val lettingAgentAccess = getLettingAgentAccess(propertyOwnershipId) ?: return false
        return lettingAgentAccessService.isTokenAuthorisedInSession(lettingAgentAccess.token.toString())
    }

    fun throwIfCurrentUserNotAuthorizedToEdit(propertyOwnershipId: Long) {
        if (!getCurrentUserIsAuthorizedToEditRecord(propertyOwnershipId)) {
            val baseUserId = SecurityContextHolder.getContext().authentication.name
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User $baseUserId is not authorized to update property ownership $propertyOwnershipId",
            )
        }
    }

    fun isCurrentUserLandlord(propertyOwnershipId: Long): Boolean {
        val landlord = userToLandlordService.getCurrentLandlordForUserOrNull() ?: return false
        return getPropertyOwnership(propertyOwnershipId).landlords.any { it.id == landlord.id }
    }

    fun getRegisteredPropertiesForLandlordUser(
        landlord: Landlord,
        currentUrlFragment: String? = null,
    ): List<RegisteredPropertyLandlordViewModel> =
        retrieveAllActivePropertiesForLandlord(landlord).map { propertyOwnership ->
            RegisteredPropertyLandlordViewModel.fromPropertyOwnership(
                propertyOwnership,
                currentUrlKey = backLinkService.storeCurrentUrlReturningKey(currentUrlFragment),
            )
        }

    fun getRegisteredPropertiesForLandlord(
        landlordId: Long,
        currentUrlFragment: String? = null,
    ): List<RegisteredPropertyLocalCouncilViewModel> =
        propertyOwnershipRepository
            .findAllByOwnershipLinks_Landlord_IdAndIsActiveTrue(landlordId)
            .map { propertyOwnership ->
                RegisteredPropertyLocalCouncilViewModel.fromPropertyOwnership(
                    propertyOwnership,
                    currentUrlKey = backLinkService.storeCurrentUrlReturningKey(currentUrlFragment),
                )
            }

    fun retrievePropertyOwnership(registrationNumber: Long): PropertyOwnership? =
        propertyOwnershipRepository
            .findByRegistrationNumber_Number(registrationNumber)

    fun retrievePropertyOwnershipById(propertyOwnershipId: Long): PropertyOwnership? =
        propertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnershipId)

    fun searchForProperties(
        searchTerm: String,
        localCouncilBaseUserId: String,
        restrictToLocalCouncil: Boolean = false,
        restrictToLicenses: List<LicensingType> = LicensingType.entries,
        requestedPageIndex: Int = 0,
        pageSize: Int = MAX_ENTRIES_IN_PROPERTIES_SEARCH_PAGE,
    ): Page<PropertySearchResultViewModel> {
        val prn = RegistrationNumberDataModel.parseTypeOrNull(searchTerm, RegistrationNumberType.PROPERTY)
        val uprn = AddressHelper.parseUprnOrNull(searchTerm)
        val pageRequest = PageRequest.of(requestedPageIndex, pageSize)

        val matchingProperties =
            try {
                if (prn != null) {
                    propertyOwnershipRepository.searchMatchingPRN(
                        prn.number,
                        localCouncilBaseUserId,
                        restrictToLocalCouncil,
                        restrictToLicenses,
                        pageRequest,
                    )
                } else if (uprn != null) {
                    propertyOwnershipRepository.searchMatchingUPRN(
                        uprn,
                        localCouncilBaseUserId,
                        restrictToLocalCouncil,
                        restrictToLicenses,
                        pageRequest,
                    )
                } else {
                    propertyOwnershipRepository.searchMatching(
                        searchTerm,
                        localCouncilBaseUserId,
                        restrictToLocalCouncil,
                        restrictToLicenses,
                        pageRequest,
                    )
                }
            } catch (_: QueryTimeoutException) {
                throw RepositoryQueryTimeoutException("Property search with query '$searchTerm' timed out")
            }

        return matchingProperties.map {
            PropertySearchResultViewModel.fromPropertyOwnership(
                it,
                backLinkService.storeCurrentUrlReturningKey(),
            )
        }
    }

    @Transactional
    fun updateLicensing(
        id: Long,
        licensingType: LicensingType,
        licenceNumber: String?,
        initialLastModifiedDate: Instant,
    ) {
        val propertyOwnership = getPropertyOwnership(id)
        throwErrorIfLastModifiedDatesConflict(propertyOwnership, initialLastModifiedDate)
        val updatedLicence =
            licenseService.updateLicence(
                propertyOwnership.license,
                licensingType,
                licenceNumber,
            )
        propertyOwnership.license = updatedLicence
        propertyOwnership.licenseProvideLater = false
        propertyOwnershipRepository.save(propertyOwnership)
    }

    @Transactional
    fun updateOwnershipType(
        id: Long,
        ownershipType: OwnershipType,
        initialLastModifiedDate: Instant,
    ) {
        val propertyOwnership = getPropertyOwnership(id)
        throwErrorIfLastModifiedDatesConflict(propertyOwnership, initialLastModifiedDate)
        propertyOwnership.ownershipType = ownershipType
        propertyOwnershipRepository.save(propertyOwnership)
    }

    // TODO(PDJB-1340): delete this method when PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING is removed. It is only
    // used by the old (flag-off) occupancy update check-your-answers step (UpdateOccupancyCyaConfig); the
    // redesigned single-page update persists via updateIsOccupied instead.
    @Transactional
    fun updateOccupancy(
        id: Long,
        isOccupied: Boolean,
        numberOfHouseholds: Int,
        numberOfPeople: Int,
        numBedrooms: Int?,
        billsIncludedList: String?,
        customBillsIncluded: String?,
        furnishedStatus: FurnishedStatus?,
        rentFrequency: RentFrequency?,
        customRentFrequency: String?,
        rentAmount: BigDecimal?,
        initialLastModifiedDate: Instant,
    ) {
        val propertyOwnership = getPropertyOwnership(id)
        throwErrorIfLastModifiedDatesConflict(propertyOwnership, initialLastModifiedDate)
        val wasOccupied = propertyOwnership.isOccupied
        propertyOwnership.currentNumHouseholds = numberOfHouseholds
        propertyOwnership.currentNumTenants = numberOfPeople
        propertyOwnership.isOccupied = isOccupied
        propertyOwnership.numBedrooms = numBedrooms
        propertyOwnership.billsIncludedList = billsIncludedList
        propertyOwnership.customBillsIncluded = customBillsIncluded
        propertyOwnership.furnishedStatus = furnishedStatus
        propertyOwnership.rentFrequency = rentFrequency
        propertyOwnership.customRentFrequency = customRentFrequency
        propertyOwnership.rentAmount = rentAmount
        if (!wasOccupied && propertyOwnership.isOccupied) {
            propertyOwnership.lastOccupiedDate = LocalDate.now(DateTimeHelper.UK_ZONE)
        }
        if (!propertyOwnership.isOccupied) {
            propertyOwnership.propertyCompliance?.tenancyStartedBeforeEpcExpiry = null
        }
        propertyOwnershipRepository.save(propertyOwnership)
    }

    @Transactional
    fun updateIsOccupied(
        id: Long,
        isOccupied: Boolean,
        initialLastModifiedDate: Instant,
    ) {
        val propertyOwnership = getPropertyOwnership(id)
        throwErrorIfLastModifiedDatesConflict(propertyOwnership, initialLastModifiedDate)
        val wasOccupied = propertyOwnership.isOccupied
        propertyOwnership.isOccupied = isOccupied
        if (!wasOccupied && propertyOwnership.isOccupied) {
            // Becoming occupied defaults to "provide tenancy details later": flag the property so the record shows
            // the deadline prompt (lastOccupiedDate + 28 days) until the landlord provides the details.
            propertyOwnership.lastOccupiedDate = LocalDate.now(DateTimeHelper.UK_ZONE)
            propertyOwnership.tenancyProvideLater = true
        }
        if (wasOccupied && !propertyOwnership.isOccupied) {
            // Becoming unoccupied: tenancy details no longer apply, so clear them rather than holding onto stale data.
            clearTenancyDetails(propertyOwnership)
            propertyOwnership.tenancyProvideLater = null
        }
        if (!propertyOwnership.isOccupied) {
            propertyOwnership.propertyCompliance?.tenancyStartedBeforeEpcExpiry = null
        }
        propertyOwnershipRepository.save(propertyOwnership)
    }

    private fun clearTenancyDetails(propertyOwnership: PropertyOwnership) {
        propertyOwnership.currentNumHouseholds = 0
        propertyOwnership.currentNumTenants = 0
        propertyOwnership.billsIncludedList = null
        propertyOwnership.customBillsIncluded = null
        propertyOwnership.furnishedStatus = null
        propertyOwnership.rentFrequency = null
        propertyOwnership.customRentFrequency = null
        propertyOwnership.rentAmount = null
    }

    @Transactional
    fun updateTenancyDetails(
        id: Long,
        numberOfHouseholds: Int,
        numberOfPeople: Int,
        billsIncludedList: String?,
        customBillsIncluded: String?,
        furnishedStatus: FurnishedStatus,
        rentFrequency: RentFrequency,
        customRentFrequency: String?,
        rentAmount: BigDecimal,
        initialLastModifiedDate: Instant,
    ) {
        val propertyOwnership = getPropertyOwnership(id)
        throwErrorIfLastModifiedDatesConflict(propertyOwnership, initialLastModifiedDate)
        propertyOwnership.currentNumHouseholds = numberOfHouseholds
        propertyOwnership.currentNumTenants = numberOfPeople
        propertyOwnership.billsIncludedList = billsIncludedList
        propertyOwnership.customBillsIncluded = customBillsIncluded
        propertyOwnership.furnishedStatus = furnishedStatus
        propertyOwnership.rentFrequency = rentFrequency
        propertyOwnership.customRentFrequency = customRentFrequency
        propertyOwnership.rentAmount = rentAmount
        // The tenancy details are now provided, so the property record no longer needs to prompt for them.
        propertyOwnership.tenancyProvideLater = false
        propertyOwnershipRepository.save(propertyOwnership)
    }

    @Transactional
    fun updateHouseholdsAndTenants(
        id: Long,
        numberOfHouseholds: Int,
        numberOfPeople: Int,
        initialLastModifiedDate: Instant,
    ) {
        val propertyOwnership = getPropertyOwnership(id)
        throwErrorIfLastModifiedDatesConflict(propertyOwnership, initialLastModifiedDate)
        propertyOwnership.currentNumHouseholds = numberOfHouseholds
        propertyOwnership.currentNumTenants = numberOfPeople
        propertyOwnershipRepository.save(propertyOwnership)
    }

    @Transactional
    fun updateBedrooms(
        id: Long,
        numberOfBedrooms: Int,
        initialLastModifiedDate: Instant,
    ) {
        val propertyOwnership = getPropertyOwnership(id)
        throwErrorIfLastModifiedDatesConflict(propertyOwnership, initialLastModifiedDate)
        propertyOwnership.numBedrooms = numberOfBedrooms
        propertyOwnershipRepository.save(propertyOwnership)
    }

    @Transactional
    fun updateRentIncludesBills(
        id: Long,
        billsIncludedList: String?,
        customBillsIncluded: String?,
        initialLastModifiedDate: Instant,
    ) {
        val propertyOwnership = getPropertyOwnership(id)
        throwErrorIfLastModifiedDatesConflict(propertyOwnership, initialLastModifiedDate)
        propertyOwnership.billsIncludedList = billsIncludedList
        propertyOwnership.customBillsIncluded = customBillsIncluded
        propertyOwnershipRepository.save(propertyOwnership)
    }

    @Transactional
    fun updateFurnishedStatus(
        id: Long,
        furnishedStatus: FurnishedStatus,
        initialLastModifiedDate: Instant,
    ) {
        val propertyOwnership = getPropertyOwnership(id)
        throwErrorIfLastModifiedDatesConflict(propertyOwnership, initialLastModifiedDate)
        propertyOwnership.furnishedStatus = furnishedStatus
        propertyOwnershipRepository.save(propertyOwnership)
    }

    @Transactional
    fun updateCorrespondenceAddress(
        id: Long,
        address: AddressDataModel,
        initialLastModifiedDate: Instant,
    ) {
        val propertyOwnership = getPropertyOwnership(id)
        throwErrorIfLastModifiedDatesConflict(propertyOwnership, initialLastModifiedDate)
        propertyOwnership.correspondenceAddress = addressService.findOrCreateAddress(address)
        propertyOwnershipRepository.save(propertyOwnership)
    }

    @Transactional
    fun updateRentFrequencyAndAmount(
        id: Long,
        rentFrequency: RentFrequency,
        customRentFrequency: String?,
        rentAmount: BigDecimal,
        initialLastModifiedDate: Instant,
    ) {
        val propertyOwnership = getPropertyOwnership(id)
        throwErrorIfLastModifiedDatesConflict(propertyOwnership, initialLastModifiedDate)
        propertyOwnership.rentFrequency = rentFrequency
        propertyOwnership.customRentFrequency = customRentFrequency
        propertyOwnership.rentAmount = rentAmount
        propertyOwnershipRepository.save(propertyOwnership)
    }

    @Transactional
    fun addLandlordToPropertyOwnership(
        propertyOwnershipId: Long,
        landlord: Landlord,
    ) {
        val propertyOwnership = getPropertyOwnership(propertyOwnershipId)
        propertyOwnership.addLandlord(landlord)
        propertyOwnershipRepository.save(propertyOwnership)
    }

    @Transactional
    fun markAsJointLandlord(propertyOwnership: PropertyOwnership) {
        propertyOwnership.markedJointLandlord = true
        propertyOwnershipRepository.save(propertyOwnership)
    }

    @Transactional
    fun markAsNotJointLandlord(propertyOwnership: PropertyOwnership) {
        propertyOwnership.markedJointLandlord = false
        propertyOwnershipRepository.save(propertyOwnership)
    }

    fun retrieveAllActivePropertiesForLandlord(landlord: Landlord): List<PropertyOwnership> =
        propertyOwnershipRepository.findAllByOwnershipLinks_Landlord_IdAndIsActiveTrue(landlord.id)

    fun deletePropertyOwnership(propertyOwnershipId: Long) {
        propertyOwnershipRepository.deleteById(propertyOwnershipId)
    }

    fun deletePropertyOwnerships(propertyOwnerships: List<PropertyOwnership>) {
        propertyOwnershipRepository.deleteAll(propertyOwnerships)
    }

    @Transactional
    fun removeLandlord(
        propertyOwnership: PropertyOwnership,
        landlord: Landlord,
    ) {
        propertyOwnership.removeLandlord(landlord)
        propertyOwnershipRepository.save(propertyOwnership)

        runAfterTransactionCommits {
            jointLandlordOtherLandlordLeftEmailService.sendNotificationToRemainingLandlords(propertyOwnership, landlord)
        }
    }

    fun getNumberOfIncompleteCompliancesForLandlord(landlord: Landlord): Int {
        val propertyOwnerships = retrieveAllActivePropertiesForLandlord(landlord)
        return propertyOwnerships.count { it.isOccupied && it.propertyCompliance == null }
    }

    fun doesLandlordHaveRegisteredProperties(landlord: Landlord): Boolean =
        propertyOwnershipRepository.existsByOwnershipLinks_Landlord_IdAndIsActiveTrue(landlord.id)

    fun getPropertyCountForLandlord(landlord: Landlord): Long = propertyOwnershipRepository.countByOwnershipLinks_Landlord_Id(landlord.id)

    private fun throwErrorIfLastModifiedDatesConflict(
        propertyOwnership: PropertyOwnership,
        initialLastModifiedDate: Instant,
    ) {
        if (propertyOwnership.getMostRecentlyUpdated() != initialLastModifiedDate) {
            throw UpdateConflictException(
                "The property ownership record has been updated since this update session started.",
            )
        }
    }

    companion object {
        // This static method allows non web services to perform this check
        @JvmStatic
        fun hasLettingAgent(
            propertyOwnership: PropertyOwnership,
            lettingAgentAccess: LettingAgentAccess?,
        ): Boolean = propertyOwnership.isOccupied && lettingAgentAccess != null
    }
}
