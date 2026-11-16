package uk.gov.communities.prsdb.webapp.services

import jakarta.transaction.Transactional
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.database.entity.PropertyManager
import uk.gov.communities.prsdb.webapp.database.repository.PropertyManagerRepository

@PrsdbWebService
class PropertyManagerService(
    private val propertyManagerRepository: PropertyManagerRepository,
) {
    @Transactional
    fun createPropertyManager(
        name: String,
        email: String,
    ): PropertyManager = propertyManagerRepository.save(PropertyManager(name, email))

    fun updatePropertyManager(
        propertyManager: PropertyManager,
        name: String,
        email: String,
    ) {
        propertyManager.name = name
        propertyManager.email = email
        propertyManagerRepository.save(propertyManager)
    }
}
