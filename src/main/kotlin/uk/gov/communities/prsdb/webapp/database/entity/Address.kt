package uk.gov.communities.prsdb.webapp.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.Comment
import uk.gov.communities.prsdb.webapp.constants.MANUAL_ADDRESS_CHOSEN
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.services.NgdAddressLoader.Companion.DATA_PACKAGE_VERSION_COMMENT_PREFIX

@Entity
@Comment(DATA_PACKAGE_VERSION_COMMENT_PREFIX)
class Address() : ModifiableAuditableEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long = 0

    @Column(unique = true)
    var uprn: Long? = null
        private set

    @Column(nullable = false, length = SINGLE_LINE_ADDRESS_LENGTH)
    lateinit var singleLineAddress: String
        private set

    var organisation: String? = null
        private set

    @Column(length = 500)
    var subBuilding: String? = null
        private set

    var buildingName: String? = null
        private set

    var buildingNumber: String? = null
        private set

    var streetName: String? = null
        private set

    var locality: String? = null
        private set

    var townName: String? = null
        private set

    @Column(nullable = false)
    var postcode: String? = null
        private set

    @ManyToOne
    @JoinColumn(name = "local_authority_id")
    var localAuthority: LocalAuthority? = null
        private set

    @Column(nullable = false)
    var isActive: Boolean = true
        private set

    constructor(addressDataModel: AddressDataModel, localAuthority: LocalAuthority? = null) : this() {
        this.uprn = addressDataModel.uprn
        this.singleLineAddress = addressDataModel.singleLineAddress
        this.organisation = addressDataModel.organisation
        this.subBuilding = addressDataModel.subBuilding
        this.buildingName = addressDataModel.buildingName
        this.buildingNumber = addressDataModel.buildingNumber
        this.streetName = addressDataModel.streetName
        this.locality = addressDataModel.locality
        this.townName = addressDataModel.townName
        this.postcode = addressDataModel.postcode
        this.localAuthority = localAuthority
    }

    fun getSelectedAddress(): String = if (uprn == null) MANUAL_ADDRESS_CHOSEN else singleLineAddress

    companion object {
        const val SINGLE_LINE_ADDRESS_LENGTH = 1000
    }
}
