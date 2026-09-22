package uk.gov.communities.prsdb.webapp.database.entity

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Transient
import uk.gov.communities.prsdb.webapp.constants.enums.LandlordType
import java.time.MonthDay

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "landlord_type", discriminatorType = DiscriminatorType.INTEGER)
// Hibernate creates subclass proxies for this inheritance root, so it and its persistent getters must remain open.
abstract class Landlord : ModifiableAuditableEntity() {
    @get:Transient
    abstract val landlordType: LandlordType

    @get:Transient
    abstract val name: String

    @get:Transient
    abstract val address: Address

    // TODO PDJB-1274: This method is temporary - a landlord will soon not have a single email
    @get:Transient
    abstract val email: String

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long = 0

    @OneToOne(optional = false)
    @JoinColumn(name = "registration_number_id", nullable = false, unique = true)
    open lateinit var registrationNumber: RegistrationNumber
        protected set

    @OneToMany(mappedBy = "landlord", orphanRemoval = true)
    private var ownershipLinks: MutableSet<OwnershipLink> = mutableSetOf()

    val landlordships: Set<PropertyOwnership> get() = ownershipLinks.map { it.propertyOwnership }.toSet()

    @Column(name = "anniversary_day")
    open var anniversaryDay: Int? = null
        protected set

    @Column(name = "anniversary_month")
    open var anniversaryMonth: Int? = null
        protected set

    val anniversary: MonthDay?
        get() {
            val day = anniversaryDay
            val month = anniversaryMonth
            return when {
                day == null && month == null -> null
                day != null && month != null -> MonthDay.of(month, day)
                else -> throw IllegalStateException(
                    "Landlord $id has an incomplete anniversary (day=$day, month=$month)",
                )
            }
        }

    fun setAnniversaryIfAbsent(monthDay: MonthDay) {
        if (anniversary == null) {
            anniversaryDay = monthDay.dayOfMonth
            anniversaryMonth = monthDay.monthValue
        }
    }
}
