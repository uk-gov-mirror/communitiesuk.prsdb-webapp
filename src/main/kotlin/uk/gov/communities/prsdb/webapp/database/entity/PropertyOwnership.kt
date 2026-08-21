package uk.gov.communities.prsdb.webapp.database.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import uk.gov.communities.prsdb.webapp.constants.enums.FurnishedStatus
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.constants.enums.RentFrequency
import uk.gov.communities.prsdb.webapp.database.entity.Address.Companion.SINGLE_LINE_ADDRESS_LENGTH
import java.math.BigDecimal
import java.time.LocalDate

@Entity
class PropertyOwnership() : ModifiableAuditableEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(nullable = false)
    var isActive: Boolean = false

    @Column(nullable = false)
    lateinit var ownershipType: OwnershipType

    @Column(nullable = false)
    var currentNumHouseholds: Int = 0

    @Column(nullable = false)
    var currentNumTenants: Int = 0

    @Column(nullable = false)
    var isOccupied: Boolean = false

    @OneToOne(optional = false)
    @JoinColumn(name = "registration_number_id", nullable = false, unique = true)
    lateinit var registrationNumber: RegistrationNumber
        private set

    @OneToMany(mappedBy = "propertyOwnership", orphanRemoval = true, cascade = [CascadeType.ALL])
    private lateinit var ownershipLinks: MutableSet<OwnershipLink>

    val landlords: Set<Landlord> get() = ownershipLinks.map { it.landlord }.toSet()

    fun otherLandlordsTo(landlord: Landlord): Set<Landlord> = landlords.filter { it.id != landlord.id }.toSet()

    @Column(nullable = false)
    lateinit var propertyBuildType: PropertyType

    @ManyToOne(optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    lateinit var address: Address
        private set

    @OneToOne(optional = true, orphanRemoval = true)
    @JoinColumn(name = "license_id", nullable = true, unique = true)
    var license: License? = null

    @Column(nullable = false, insertable = false, updatable = false, length = SINGLE_LINE_ADDRESS_LENGTH)
    private lateinit var singleLineAddress: String

    @Column(insertable = false, updatable = false)
    private val localCouncilId: Int? = null

    // We use this generated duplicate of isActive to influence the query planner into using the GIST index (as opposed to the GIN index)
    // for searches where it's likely to be more efficient
    @Column(nullable = false, insertable = false, updatable = false)
    private val isActiveDuplicateForGistIndex: Boolean = false

    @OneToOne(mappedBy = "propertyOwnership", orphanRemoval = true)
    val propertyCompliance: PropertyCompliance? = null

    @OneToOne(mappedBy = "propertyOwnership", orphanRemoval = true)
    val lettingAgentAccess: LettingAgentAccess? = null

    @OneToMany(mappedBy = "registeredOwnership", orphanRemoval = true)
    private val jointLandlordInvitations: MutableSet<JointLandlordInvitation> = mutableSetOf()

    var numBedrooms: Int? = null

    var billsIncludedList: String? = null

    var customBillsIncluded: String? = null

    var furnishedStatus: FurnishedStatus? = null

    var rentFrequency: RentFrequency? = null

    var customRentFrequency: String? = null

    var customPropertyType: String? = null

    @Column(precision = 9, scale = 2)
    var rentAmount: BigDecimal? = null

    // this is a separate property to whether the property currently has any joint landlords.
    // this tracks whether the user indicated that there were joint landlords.
    // there may be pending invitations that haven't yet been accepted.
    // this is then surfaced to local councils that the user indicated that the property should have joint landlords.
    @Column(nullable = false)
    var markedJointLandlord: Boolean = false

    var lastOccupiedDate: LocalDate? = null

    var licenseProvideLater: Boolean? = null

    var tenancyProvideLater: Boolean? = null

    constructor(
        ownershipType: OwnershipType,
        currentNumHouseholds: Int,
        currentNumTenants: Int,
        isOccupied: Boolean,
        registrationNumber: RegistrationNumber,
        landlords: MutableSet<Landlord>,
        propertyBuildType: PropertyType,
        address: Address,
        license: License?,
        isActive: Boolean = true,
        numBedrooms: Int? = null,
        billsIncludedList: String? = null,
        customBillsIncluded: String? = null,
        furnishedStatus: FurnishedStatus? = null,
        rentFrequency: RentFrequency? = null,
        customRentFrequency: String? = null,
        rentAmount: BigDecimal? = null,
        customPropertyType: String? = null,
        lastOccupiedDate: LocalDate? = null,
        markedJointLandlord: Boolean = false,
        licenseProvideLater: Boolean? = null,
        tenancyProvideLater: Boolean? = null,
    ) : this() {
        this.ownershipType = ownershipType
        this.currentNumHouseholds = currentNumHouseholds
        this.currentNumTenants = currentNumTenants
        this.isOccupied = isOccupied
        this.registrationNumber = registrationNumber
        this.ownershipLinks = landlords.mapTo(mutableSetOf()) { landlord -> OwnershipLink(landlord, this) }
        this.propertyBuildType = propertyBuildType
        this.address = address
        this.license = license
        this.isActive = isActive
        this.numBedrooms = numBedrooms
        this.billsIncludedList = billsIncludedList
        this.customBillsIncluded = customBillsIncluded
        this.furnishedStatus = furnishedStatus
        this.rentFrequency = rentFrequency
        this.customRentFrequency = customRentFrequency
        this.rentAmount = rentAmount
        this.customPropertyType = customPropertyType
        this.lastOccupiedDate = lastOccupiedDate
        this.markedJointLandlord = markedJointLandlord
        this.licenseProvideLater = licenseProvideLater
        this.tenancyProvideLater = tenancyProvideLater
    }

    val licenseType: LicensingType
        get() {
            val storedLicense = license
            return when {
                licenseProvideLater == true -> LicensingType.PROVIDE_LATER
                storedLicense != null -> storedLicense.licenseType
                else -> LicensingType.NO_LICENSING
            }
        }

    val rentIncludesBills: Boolean
        get() = billsIncludedList != null

    fun isSolelyOwnedBy(landlord: Landlord): Boolean = ownershipLinks.singleOrNull()?.landlord?.id == landlord.id

    fun removeLandlord(landlord: Landlord) {
        ownershipLinks.removeIf { it.landlord.id == landlord.id }
    }

    fun addLandlord(landlord: Landlord) {
        ownershipLinks.add(OwnershipLink(landlord, this))
    }
}
