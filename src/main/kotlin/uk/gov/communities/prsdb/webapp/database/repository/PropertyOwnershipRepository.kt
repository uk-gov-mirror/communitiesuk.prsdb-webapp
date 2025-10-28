package uk.gov.communities.prsdb.webapp.database.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.RegistrationStatus
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership

interface PropertyOwnershipRepository : JpaRepository<PropertyOwnership, Long> {
    // The underscore tells JPA to access fields relating to the referenced table
    @Suppress("ktlint:standard:function-naming")
    fun existsByIsActiveTrueAndProperty_Id(id: Long): Boolean

    @Suppress("ktlint:standard:function-naming", "ktlint:standard:max-line-length")
    fun countByPrimaryLandlord_BaseUser_IdAndIsActiveTrueAndProperty_StatusAndCurrentNumTenantsIsGreaterThanAndIncompleteComplianceFormNotNull(
        userId: String,
        status: RegistrationStatus,
        currentNumTenantsIsGreaterThan: Int,
    ): Long

    // This returns all active PropertyOwnerships for a given landlord from their baseUser_Id with a particular RegistrationStatus
    @Suppress("ktlint:standard:function-naming")
    fun findAllByPrimaryLandlord_BaseUser_IdAndIsActiveTrueAndProperty_Status(
        userId: String,
        status: RegistrationStatus,
    ): List<PropertyOwnership>

    @Suppress("ktlint:standard:function-naming")
    fun findAllByPrimaryLandlord_BaseUser_Id(userId: String): List<PropertyOwnership>

    @Suppress("ktlint:standard:function-naming")
    fun findByRegistrationNumber_Number(registrationNumber: Long): PropertyOwnership?

    // This returns all active PropertyOwnerships for a given landlord from their landlord_Id with a particular RegistrationStatus
    @Suppress("ktlint:standard:function-naming")
    fun findAllByPrimaryLandlord_IdAndIsActiveTrueAndProperty_Status(
        landlordId: Long,
        status: RegistrationStatus,
    ): List<PropertyOwnership>

    fun findByIdAndIsActiveTrue(id: Long): PropertyOwnership?

    @Query(
        "SELECT po.* " +
            "FROM property_ownership po " +
            "JOIN registration_number r ON po.registration_number_id = r.id " +
            "WHERE po.is_active AND r.number = :searchPRN " +
            FILTERS,
        nativeQuery = true,
    )
    fun searchMatchingPRN(
        @Param("searchPRN") searchPRN: Long,
        @Param("laUserBaseId") laUserBaseId: String,
        @Param("restrictToLA") restrictToLA: Boolean = false,
        @Param("restrictToLicenses") restrictToLicenses: Collection<LicensingType> = LicensingType.entries,
        pageable: Pageable,
    ): Page<PropertyOwnership>

    @Query(
        "SELECT po.* " +
            "FROM property_ownership po " +
            "JOIN property p ON po.property_id = p.id " +
            "JOIN address a ON p.address_id = a.id " +
            "WHERE po.is_active AND a.uprn = :searchUPRN " +
            FILTERS,
        nativeQuery = true,
    )
    fun searchMatchingUPRN(
        @Param("searchUPRN") searchUPRN: Long,
        @Param("laUserBaseId") laUserBaseId: String,
        @Param("restrictToLA") restrictToLA: Boolean = false,
        @Param("restrictToLicenses") restrictToLicenses: Collection<LicensingType> = LicensingType.entries,
        pageable: Pageable,
    ): Page<PropertyOwnership>

    @Query(
        "SELECT po.* " +
            "FROM property_ownership po " +
            "WHERE po.single_line_address %>> :searchTerm " +
            "AND po.is_active " +
            FILTERS +
            "ORDER BY po.single_line_address <->>> :searchTerm",
        nativeQuery = true,
    )
    fun searchMatching(
        @Param("searchTerm") searchTerm: String,
        @Param("laUserBaseId") laUserBaseId: String,
        @Param("restrictToLA") restrictToLA: Boolean = false,
        @Param("restrictToLicenses") restrictToLicenses: Collection<LicensingType> = LicensingType.entries,
        pageable: Pageable,
    ): Page<PropertyOwnership>

    companion object {
        private const val NO_LICENCE_TYPE =
            "#{T(uk.gov.communities.prsdb.webapp.constants.enums.LicensingType).NO_LICENSING}"

        // Determines whether the property's address is in the LA user's LA
        private const val LA_FILTER =
            """
            AND ((SELECT a.local_authority_id 
                  FROM property p 
                  JOIN address a ON p.address_id = a.id 
                  WHERE po.property_id = p.id)
                 =
                 (SELECT la.id 
                  FROM local_authority la
                  JOIN local_authority_user lau ON la.id = lau.local_authority_id
                  WHERE lau.subject_identifier = :laUserBaseId)
                 OR NOT :restrictToLA) 
            """

        private const val LICENSE_FILTER =
            """
            AND ((SELECT l.license_type 
                  FROM license l
                  WHERE po.license_id = l.id)
                 IN :restrictToLicenses
                 OR po.license_id IS NULL 
                    AND :${NO_LICENCE_TYPE} IN :restrictToLicenses)
            """

        private const val FILTERS = LA_FILTER + LICENSE_FILTER
    }
}
