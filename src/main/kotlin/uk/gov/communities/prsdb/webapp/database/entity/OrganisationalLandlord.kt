package uk.gov.communities.prsdb.webapp.database.entity

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Transient
import uk.gov.communities.prsdb.webapp.constants.enums.CharityRegulator
import uk.gov.communities.prsdb.webapp.constants.enums.LandlordType
import uk.gov.communities.prsdb.webapp.constants.enums.OrgType
import java.time.LocalDate

@Entity
@DiscriminatorValue("1")
class OrganisationalLandlord() : Landlord() {
    @get:Transient
    override val landlordType: LandlordType
        get() = LandlordType.ORGANISATION

    @Column(name = "organisation_landlord_name")
    override lateinit var name: String

    @ManyToOne
    @JoinColumn(name = "organisation_address_id")
    override lateinit var address: Address

    /**
     * This should not be used for sending emails, just for displaying the contact info for an organisation.
     */
    @Column(name = "organisation_email")
    lateinit var wholeOrgEmail: String

    @Column(name = "organisation_phone_number")
    lateinit var phoneNumber: String

    @Column(name = "organisation_registrant_name")
    lateinit var registrantName: String

    @Column(name = "organisation_registrant_date_of_birth")
    lateinit var registrantDateOfBirth: LocalDate

    @Column(name = "organisation_registrant_email")
    lateinit var registrantEmail: String

    @Column(name = "organisation_registrant_phone_number")
    lateinit var registrantPhoneNumber: String

    // Note that this has no relation to the companyNumber nullable col
    @Column(name = "organisation_is_company")
    var isCompany: Boolean = false

    // Note that this has no relation to the charityRegisteredWith & charityNumber nullable col
    @Column(name = "organisation_is_charity")
    var isCharity: Boolean = false

    // Note that this determines whether the organisation needs a lead trustee
    @Column(name = "organisation_is_trust")
    var isTrust: Boolean = false

    @Column(name = "organisation_company_number")
    var companyNumber: String? = null

    @Column(name = "organisation_charity_registered_with")
    var charityRegisteredWith: CharityRegulator? = null

    @Column(name = "organisation_charity_number")
    var charityNumber: String? = null

    @Column(name = "organisation_lead_trustee_name")
    var leadTrusteeName: String? = null

    @Column(name = "organisation_lead_trustee_date_of_birth")
    var leadTrusteeDateOfBirth: LocalDate? = null

    @Column(name = "organisation_lead_trustee_email")
    var leadTrusteeEmail: String? = null

    @Column(name = "organisation_lead_trustee_phone")
    var leadTrusteePhone: String? = null

    @ManyToOne
    @JoinColumn(name = "organisation_lead_trustee_address_id")
    var leadTrusteeAddress: Address? = null

    @Column(name = "organisation_main_contact_name")
    lateinit var mainContactName: String

    @Column(name = "organisation_main_contact_email")
    lateinit var mainContactEmail: String

    @Column(name = "organisation_main_contact_phone")
    lateinit var mainContactPhone: String

    // We eager fetch these as we're temporarily using them to get the email for an org landlord.
    // We assume one org user per landlord for now so this eager fetch will be cheap.
    // TODO: PDJB-1274: This eager fetch will no longer be needed once we support multiple users in an org. Remove it
    @OneToMany(mappedBy = "organisationalLandlord", fetch = FetchType.EAGER)
    private val organisationalLandlordUsers: MutableSet<OrganisationalLandlordUser> = mutableSetOf()

    // TODO PDJB-1274: The single-user assumption must be removed to support multiple organisation users.
    @get:Transient
    override val email: String
        get() = organisationalLandlordUsers.single().email

    internal fun addOrganisationalLandlordUser(organisationalLandlordUser: OrganisationalLandlordUser) {
        organisationalLandlordUsers.add(organisationalLandlordUser)
    }

    @OneToMany(mappedBy = "organisationalLandlord")
    @OrderBy("id ASC")
    private val governingBodyMemberList: MutableList<OrganisationGoverningBodyMember> = mutableListOf()

    @get:Transient
    val governingBodyMembers: List<OrganisationGoverningBodyMember>
        get() = governingBodyMemberList.toList()

    constructor(
        registrationNumber: RegistrationNumber,
        name: String,
        address: Address,
        email: String,
        phoneNumber: String,
        registrantName: String,
        registrantDateOfBirth: LocalDate,
        registrantEmail: String,
        registrantPhoneNumber: String,
        isCompany: Boolean,
        isCharity: Boolean,
        isTrust: Boolean,
        companyNumber: String?,
        charityRegisteredWith: CharityRegulator?,
        charityNumber: String?,
        leadTrusteeName: String?,
        leadTrusteeDateOfBirth: LocalDate?,
        leadTrusteeEmail: String?,
        leadTrusteePhone: String?,
        leadTrusteeAddress: Address?,
        mainContactName: String,
        mainContactEmail: String,
        mainContactPhone: String,
    ) : this() {
        this.registrationNumber = registrationNumber
        this.name = name
        this.address = address
        this.wholeOrgEmail = email
        this.phoneNumber = phoneNumber
        this.registrantName = registrantName
        this.registrantDateOfBirth = registrantDateOfBirth
        this.registrantEmail = registrantEmail
        this.registrantPhoneNumber = registrantPhoneNumber
        this.isCompany = isCompany
        this.isCharity = isCharity
        this.isTrust = isTrust
        this.companyNumber = companyNumber
        this.charityRegisteredWith = charityRegisteredWith
        this.charityNumber = charityNumber
        this.leadTrusteeName = leadTrusteeName
        this.leadTrusteeDateOfBirth = leadTrusteeDateOfBirth
        this.leadTrusteeEmail = leadTrusteeEmail
        this.leadTrusteePhone = leadTrusteePhone
        this.leadTrusteeAddress = leadTrusteeAddress
        this.mainContactName = mainContactName
        this.mainContactEmail = mainContactEmail
        this.mainContactPhone = mainContactPhone
    }

    @get:Transient
    val isRegisteredCompany: Boolean
        get() = companyNumber != null

    @get:Transient
    val isRegisteredCharity: Boolean
        get() = charityRegisteredWith != null

    @get:Transient
    val hasCharityNumber: Boolean
        get() = charityNumber != null

    @get:Transient
    val hasLeadTrustee: Boolean
        get() = isTrust == true

    @get:Transient
    val hasGoverningBody: Boolean
        get() = !isRegisteredCompany

    @get:Transient
    val organisationTypes: List<OrgType>
        get() =
            buildList {
                if (isCompany == true) add(OrgType.COMPANY)
                if (isCharity == true) add(OrgType.CHARITY)
                if (isTrust == true) add(OrgType.TRUST)
                if (isEmpty()) add(OrgType.NONE)
            }
}
