package uk.gov.communities.prsdb.webapp.testHelpers.mockObjects

import org.springframework.test.util.ReflectionTestUtils
import uk.gov.communities.prsdb.webapp.constants.ENGLAND_OR_WALES
import uk.gov.communities.prsdb.webapp.constants.enums.CharityRegulator
import uk.gov.communities.prsdb.webapp.constants.enums.FurnishedStatus
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.constants.enums.RegistrationNumberType
import uk.gov.communities.prsdb.webapp.constants.enums.RentFrequency
import uk.gov.communities.prsdb.webapp.database.entity.Address
import uk.gov.communities.prsdb.webapp.database.entity.IndividualLandlord
import uk.gov.communities.prsdb.webapp.database.entity.Landlord
import uk.gov.communities.prsdb.webapp.database.entity.License
import uk.gov.communities.prsdb.webapp.database.entity.LocalCouncil
import uk.gov.communities.prsdb.webapp.database.entity.OrganisationalLandlord
import uk.gov.communities.prsdb.webapp.database.entity.OrganisationalLandlordUser
import uk.gov.communities.prsdb.webapp.database.entity.OwnershipLink
import uk.gov.communities.prsdb.webapp.database.entity.Passcode
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import uk.gov.communities.prsdb.webapp.database.entity.PrsdbUser
import uk.gov.communities.prsdb.webapp.database.entity.RegistrationNumber
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.LandlordSearchResultDataModel
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLocalCouncilData.Companion.createLocalCouncil
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class MockLandlordData {
    companion object {
        fun createAddress(
            singleLineAddress: String = "1 Example Road, EG1 2AB",
            localCouncil: LocalCouncil? = createLocalCouncil(),
            uprn: Long? = null,
        ) = Address(AddressDataModel(singleLineAddress = singleLineAddress, uprn = uprn), localCouncil)

        fun createPrsdbUser(id: String = "") = MockPrsdbUserData.createPrsdbUser(id)

        var lastLandlordId = 0

        fun createIndividualLandlord(
            baseUser: PrsdbUser = createPrsdbUser(),
            name: String = "name",
            email: String = "example@email.com",
            phoneNumber: String = "07123456789",
            address: Address = createAddress(),
            registrationNumber: RegistrationNumber = RegistrationNumber(RegistrationNumberType.LANDLORD, 0L),
            countryOfResidence: String = ENGLAND_OR_WALES,
            isVerified: Boolean = true,
            hasAcceptedPrivacyNotice: Boolean = true,
            nonEnglandOrWalesAddress: String? = null,
            dateOfBirth: LocalDate? = null,
            createdDate: Instant = Instant.now(),
            propertyOwnerships: Set<PropertyOwnership> = emptySet(),
        ): IndividualLandlord {
            val landlord =
                IndividualLandlord(
                    baseUser = baseUser,
                    name = name,
                    email = email,
                    phoneNumber = phoneNumber,
                    address = address,
                    registrationNumber = registrationNumber,
                    countryOfResidence = countryOfResidence,
                    isVerified = isVerified,
                    hasAcceptedPrivacyNotice = hasAcceptedPrivacyNotice,
                    nonEnglandOrWalesAddress = nonEnglandOrWalesAddress,
                    dateOfBirth = dateOfBirth,
                )

            ReflectionTestUtils.setField(landlord, "createdDate", createdDate)
            ReflectionTestUtils.setField(
                landlord,
                "ownershipLinks",
                propertyOwnerships.map { OwnershipLink(landlord, it) }.toSet(),
            )

            val nextId = lastLandlordId + 1
            ReflectionTestUtils.setField(landlord, "id", nextId)
            lastLandlordId = nextId

            return landlord
        }

        fun createOrgLandlord(
            baseUser: PrsdbUser = createPrsdbUser(),
            name: String = "Organisation landlord",
            address: Address = createAddress(),
            email: String = "organisation@example.com",
            phoneNumber: String = "07123456789",
            registrationNumber: RegistrationNumber = RegistrationNumber(RegistrationNumberType.LANDLORD, 0L),
            registrantName: String = "Registrant name",
            registrantDateOfBirth: LocalDate = LocalDate.of(1990, 1, 1),
            registrantEmail: String = "registrant@example.com",
            registrantPhoneNumber: String = "07123456780",
            isCompany: Boolean = true,
            isCharity: Boolean = false,
            isTrust: Boolean = false,
            companyNumber: String? = "12345678",
            charityRegisteredWith: CharityRegulator? = null,
            charityNumber: String? = null,
            leadTrusteeName: String? = null,
            leadTrusteeDateOfBirth: LocalDate? = null,
            leadTrusteeEmail: String? = null,
            leadTrusteePhoneNumber: String? = null,
            leadTrusteeAddress: Address? = null,
            mainContactName: String = "Main contact",
            mainContactEmail: String = "main.contact@example.com",
            mainContactPhoneNumber: String = "07123456781",
            createdDate: Instant = Instant.now(),
            propertyOwnerships: Set<PropertyOwnership> = emptySet(),
        ): OrganisationalLandlord {
            val landlord =
                OrganisationalLandlord(
                    registrationNumber = registrationNumber,
                    name = name,
                    address = address,
                    email = email,
                    phoneNumber = phoneNumber,
                    registrantName = registrantName,
                    registrantDateOfBirth = registrantDateOfBirth,
                    registrantEmail = registrantEmail,
                    registrantPhoneNumber = registrantPhoneNumber,
                    isCompany = isCompany,
                    isCharity = isCharity,
                    isTrust = isTrust,
                    companyNumber = companyNumber,
                    charityRegisteredWith = charityRegisteredWith,
                    charityNumber = charityNumber,
                    leadTrusteeName = leadTrusteeName,
                    leadTrusteeDateOfBirth = leadTrusteeDateOfBirth,
                    leadTrusteeEmail = leadTrusteeEmail,
                    leadTrusteePhone = leadTrusteePhoneNumber,
                    leadTrusteeAddress = leadTrusteeAddress,
                    mainContactName = mainContactName,
                    mainContactEmail = mainContactEmail,
                    mainContactPhone = mainContactPhoneNumber,
                )
            OrganisationalLandlordUser(
                organisationalLandlord = landlord,
                baseUser = baseUser,
                name = registrantName,
                email = registrantEmail,
            )

            ReflectionTestUtils.setField(landlord, "createdDate", createdDate)
            ReflectionTestUtils.setField(
                landlord,
                "ownershipLinks",
                propertyOwnerships.map { OwnershipLink(landlord, it) }.toSet(),
            )

            val nextId = lastLandlordId + 1
            ReflectionTestUtils.setField(landlord, "id", nextId)
            lastLandlordId = nextId

            return landlord
        }

        fun createPropertyOwnership(
            ownershipType: OwnershipType = OwnershipType.FREEHOLD,
            currentNumHouseholds: Int = 0,
            currentNumTenants: Int = 0,
            isOccupied: Boolean = currentNumTenants > 0,
            registrationNumber: RegistrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456),
            landlords: MutableSet<Landlord> = mutableSetOf(createIndividualLandlord()),
            propertyBuildType: PropertyType = PropertyType.SEMI_DETACHED_HOUSE,
            address: Address = createAddress(),
            license: License? = null,
            correspondenceEmail: String = landlords.first().email,
            correspondenceAddress: Address = createAddress(),
            id: Long = 1,
            createdDate: Instant = Instant.now(),
            isActive: Boolean = true,
            numberOfBedrooms: Int? = null,
            billsIncludedList: String? = null,
            customBillsIncluded: String? = null,
            furnishedStatus: FurnishedStatus? = null,
            rentFrequency: RentFrequency? = null,
            customRentFrequency: String? = null,
            rentAmount: BigDecimal? = null,
            customPropertyType: String? = null,
            markedJointLandlord: Boolean = false,
            licenseProvideLater: Boolean = false,
            tenancyProvideLater: Boolean = false,
        ): PropertyOwnership {
            val propertyOwnership =
                PropertyOwnership(
                    ownershipType = ownershipType,
                    currentNumHouseholds = currentNumHouseholds,
                    currentNumTenants = currentNumTenants,
                    isOccupied = isOccupied,
                    registrationNumber = registrationNumber,
                    landlords = landlords,
                    propertyBuildType = propertyBuildType,
                    address = address,
                    license = license,
                    correspondenceEmail = correspondenceEmail,
                    correspondenceAddress = correspondenceAddress,
                    isActive = isActive,
                    numBedrooms = numberOfBedrooms,
                    billsIncludedList = billsIncludedList,
                    customBillsIncluded = customBillsIncluded,
                    furnishedStatus = furnishedStatus,
                    rentFrequency = rentFrequency,
                    customRentFrequency = customRentFrequency,
                    rentAmount = rentAmount,
                    customPropertyType = customPropertyType,
                    markedJointLandlord = markedJointLandlord,
                    licenseProvideLater = licenseProvideLater,
                    tenancyProvideLater = tenancyProvideLater,
                )

            ReflectionTestUtils.setField(propertyOwnership, "id", id)
            ReflectionTestUtils.setField(propertyOwnership, "createdDate", createdDate)

            val newOwnershipLinks = ReflectionTestUtils.getField(propertyOwnership, "ownershipLinks") as Set<*>
            landlords.forEach { landlord ->
                val linksForLandlord =
                    newOwnershipLinks.filterIsInstance<OwnershipLink>().filter { it.landlord == landlord }
                val existingOwnershipLinks =
                    (ReflectionTestUtils.getField(landlord, "ownershipLinks") as? Set<*>).orEmpty()
                ReflectionTestUtils.setField(
                    landlord,
                    "ownershipLinks",
                    (existingOwnershipLinks + linksForLandlord).toMutableSet(),
                )
            }

            return propertyOwnership
        }

        fun createOccupiedPropertyOwnership(
            ownershipType: OwnershipType = OwnershipType.FREEHOLD,
            currentNumHouseholds: Int = 2,
            currentNumTenants: Int = 1,
            registrationNumber: RegistrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456),
            landlords: MutableSet<Landlord> = mutableSetOf(createIndividualLandlord()),
            propertyBuildType: PropertyType = PropertyType.SEMI_DETACHED_HOUSE,
            address: Address = createAddress(),
            license: License? = null,
            isActive: Boolean = true,
            numberOfBedrooms: Int = 1,
            billsIncludedList: String? = "ELECTRICITY,WATER,SOMETHING_ELSE",
            customBillsIncluded: String? = "Cat sitting",
            furnishedStatus: FurnishedStatus = FurnishedStatus.FURNISHED,
            rentFrequency: RentFrequency = RentFrequency.OTHER,
            customRentFrequency: String? = "Fortnightly",
            rentAmount: BigDecimal = BigDecimal(200),
            id: Long = 1,
            lastOccupiedDate: LocalDate? = LocalDate.of(2025, 1, 1),
            createdDate: Instant = Instant.now(),
            licenseProvideLater: Boolean = false,
            tenancyProvideLater: Boolean = false,
        ): PropertyOwnership {
            val propertyOwnership =
                createPropertyOwnership(
                    id = id,
                    ownershipType = ownershipType,
                    currentNumHouseholds = currentNumHouseholds,
                    currentNumTenants = currentNumTenants,
                    isOccupied = true,
                    registrationNumber = registrationNumber,
                    landlords = landlords,
                    propertyBuildType = propertyBuildType,
                    address = address,
                    license = license,
                    isActive = isActive,
                    numberOfBedrooms = numberOfBedrooms,
                    billsIncludedList = billsIncludedList,
                    customBillsIncluded = customBillsIncluded,
                    furnishedStatus = furnishedStatus,
                    rentFrequency = rentFrequency,
                    customRentFrequency = customRentFrequency,
                    rentAmount = rentAmount,
                    createdDate = createdDate,
                    licenseProvideLater = licenseProvideLater,
                    tenancyProvideLater = tenancyProvideLater,
                )
            if (lastOccupiedDate != null) {
                propertyOwnership.lastOccupiedDate = lastOccupiedDate
            }
            return propertyOwnership
        }

        fun createUnoccupiedPropertyOwnership(
            id: Long = 1,
            licenseProvideLater: Boolean = false,
        ): PropertyOwnership =
            createPropertyOwnership(
                id = id,
                currentNumHouseholds = 0,
                currentNumTenants = 0,
                numberOfBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = null,
                customRentFrequency = null,
                rentAmount = null,
                licenseProvideLater = licenseProvideLater,
            )

        fun createPasscode(
            code: String = "ABCDEF",
            baseUser: PrsdbUser? = createPrsdbUser(),
        ) = Passcode(code, baseUser)

        fun createLandlordSearchResultDataModel(
            id: Long = 1,
            name: String = "landlord name",
            email: String = "landlord@test.org",
            phoneNumber: String = "01234567890",
            registrationNumber: Long = 123456,
            singleLineAddress: String = "123 Test Street, Test Town, TE1 1ST",
            propertyCount: Long = 5,
        ) = LandlordSearchResultDataModel(
            id = id,
            name = name,
            email = email,
            phoneNumber = phoneNumber,
            registrationNumber = registrationNumber,
            singleLineAddress = singleLineAddress,
            propertyCount = propertyCount,
        )
    }
}
