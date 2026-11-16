package uk.gov.communities.prsdb.webapp.services

import jakarta.transaction.Transactional
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.database.entity.Address
import uk.gov.communities.prsdb.webapp.database.entity.Freeholder
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import uk.gov.communities.prsdb.webapp.database.repository.FreeholderRepository
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.FreeholderDataModel

@PrsdbWebService
class FreeholderService(
    private val freeholderRepository: FreeholderRepository,
    private val addressService: AddressService,
) {
    // Updates the saved freeholders to match the list the user submitted, creating, updating and
    // deleting them one at a time.
    // Returns the newly added freeholders so they can be tracked by the journey and notified.
    // TODO PDJB-1993: use this to update freeholders.
    // TODO PDJB-2171: also return updated and removed freeholders so they can be tracked by the
    //  journey and notified.
    @Transactional
    fun updateFreeholders(
        propertyOwnership: PropertyOwnership,
        freeholders: List<FreeholderDataModel>,
    ): List<FreeholderDataModel> {
        val existingFreeholdersKeyedById =
            freeholderRepository.findAllByPropertyOwnership_Id(propertyOwnership.id).associateBy { it.id }
        val submittedIds = freeholders.mapNotNull { it.databaseId }.toSet()

        existingFreeholdersKeyedById.values.filter { it.id !in submittedIds }.forEach { removeFreeholder(it) }

        val added = mutableListOf<FreeholderDataModel>()

        freeholders.forEach { submitted ->
            // An id that no longer matches a saved row means it was deleted elsewhere, so treat it as
            // a new freeholder rather than losing the user's answers.
            val existingFreeholder = submitted.databaseId?.let { existingFreeholdersKeyedById[it] }
            when {
                existingFreeholder == null -> {
                    createFreeholder(propertyOwnership, submitted)
                    added.add(submitted)
                }
                detailsHaveChanged(existingFreeholder, submitted) -> updateFreeholder(existingFreeholder, submitted)
                // An unchanged freeholder matches no branch, so their row is left alone.
            }
        }

        return added
    }

    private fun detailsHaveChanged(
        existing: Freeholder,
        submitted: FreeholderDataModel,
    ): Boolean =
        existing.name != submitted.name ||
            existing.email != submitted.email ||
            addressHasChanged(existing.address, submitted.address)

    // Two looked up addresses can be compared on UPRN. A manually entered one has none, so fall back
    // to the address the user sees: the same single line address means they did not change their
    // answer, so the freeholder does not need contacting again.
    private fun addressHasChanged(
        existing: Address?,
        submitted: AddressDataModel?,
    ): Boolean =
        when {
            existing == null && submitted == null -> false
            existing == null || submitted == null -> true
            existing.uprn != null && submitted.uprn != null -> existing.uprn != submitted.uprn
            else -> existing.singleLineAddress != submitted.singleLineAddress
        }

    @Transactional
    fun createFreeholders(
        propertyOwnership: PropertyOwnership,
        freeholders: List<FreeholderDataModel>,
    ) {
        freeholders.forEach { createFreeholder(propertyOwnership, it) }
    }

    @Transactional
    fun createFreeholder(
        propertyOwnership: PropertyOwnership,
        freeholder: FreeholderDataModel,
    ) {
        freeholderRepository.save(
            Freeholder(
                propertyOwnership = propertyOwnership,
                name = freeholder.name,
                email = freeholder.email,
                address = freeholder.address?.let { addressService.findOrCreateAddress(it) },
            ),
        )
    }

    @Transactional
    fun updateFreeholder(
        freeholder: Freeholder,
        updatedDetails: FreeholderDataModel,
    ) {
        freeholder.name = updatedDetails.name
        freeholder.email = updatedDetails.email
        // Only re-resolve the address when it has changed: findOrCreateAddress saves a new row for
        // every manual address, so reassigning an unchanged one would leave a duplicate behind.
        if (addressHasChanged(freeholder.address, updatedDetails.address)) {
            freeholder.address = updatedDetails.address?.let { addressService.findOrCreateAddress(it) }
        }
        freeholderRepository.save(freeholder)
    }

    @Transactional
    fun removeFreeholder(freeholder: Freeholder) {
        freeholderRepository.delete(freeholder)
    }

    @Transactional
    fun clearFreeholders(propertyOwnership: PropertyOwnership) {
        freeholderRepository.deleteByPropertyOwnership(propertyOwnership)
    }

    // TODO PDJB-2016: remove this with the flag-off code paths, which never ask who the freeholder is.
    //  It keeps the invariant that an ownership type requiring freeholder records has at least one row.
    //  PDJB-1992 will remove the remaining flag-on caller in updateOwnershipType. The flag-off callers
    //  remain until the flag goes.
    @Transactional
    fun createPlaceholderFreeholder(propertyOwnership: PropertyOwnership) {
        freeholderRepository.save(Freeholder(propertyOwnership = propertyOwnership))
    }
}
