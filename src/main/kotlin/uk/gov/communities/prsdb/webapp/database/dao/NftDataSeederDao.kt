package uk.gov.communities.prsdb.webapp.database.dao

import org.hibernate.StatelessSession
import uk.gov.communities.prsdb.webapp.constants.ENGLAND_OR_WALES
import uk.gov.communities.prsdb.webapp.constants.enums.FileUploadStatus
import uk.gov.communities.prsdb.webapp.database.entity.Address
import java.sql.Connection
import java.sql.PreparedStatement

class NftDataSeederDao(
    private val session: StatelessSession,
    private val connection: Connection,
) {
    fun prepareAddressStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO address 
            (created_date, last_modified_date, uprn, single_line_address, building_number, street_name, town_name, 
            postcode, local_council_id, is_active) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, true)
            """
        return connection.prepareStatement(query)
    }

    fun countAddresses(): Int {
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT count(*) FROM address").use { resultSet ->
                resultSet.next()
                return resultSet.getInt(1)
            }
        }
    }

    fun preparePrsdbUserStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO prsdb_user 
            (id, created_date) 
            VALUES (?, ?)
            """
        return connection.prepareStatement(query)
    }

    fun prepareSystemOperatorStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO system_operator 
            (created_date, last_modified_date, subject_identifier) 
            VALUES (?, ?, ?)
            """
        return connection.prepareStatement(query)
    }

    fun prepareLocalCouncilInvitationStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO local_council_invitation 
            (created_date, token, invited_email, invited_as_admin, inviting_council_id) 
            VALUES (?, ?, ?, ?, ?)
            """
        return connection.prepareStatement(query)
    }

    fun prepareLocalCouncilUserStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO local_council_user 
            (created_date, last_modified_date, subject_identifier, is_manager, name, email, local_council_id, has_accepted_privacy_notice) 
            VALUES (?, ?, ?, ?, ?, ?, ?, true)
            """
        return connection.prepareStatement(query)
    }

    fun prepareRegistrationNumberStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO registration_number 
            (id, created_date, number, type) 
            VALUES (?, ?, ?, ?)
            """
        return connection.prepareStatement(query)
    }

    fun prepareIndividualLandlordStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO landlord 
            (id, created_date, last_modified_date, individual_subject_identifier, individual_name, individual_email, individual_phone_number, individual_address_id, individual_date_of_birth, 
             registration_number_id, individual_is_verified, individual_country_of_residence, individual_is_active, individual_has_accepted_privacy_notice, landlord_type)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '${ENGLAND_OR_WALES}', true, true, 0)
            """
        return connection.prepareStatement(query)
    }

    fun prepareOrganisationLandlordStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO landlord 
            (id, created_date, last_modified_date, registration_number_id, landlord_type,
             organisation_landlord_name, organisation_address_id, organisation_email, organisation_phone_number,
             organisation_is_company, organisation_is_charity, organisation_is_trust,
             organisation_company_number, organisation_charity_registered_with, organisation_charity_number,
             organisation_lead_trustee_name, organisation_lead_trustee_date_of_birth, organisation_lead_trustee_email,
             organisation_lead_trustee_phone, organisation_lead_trustee_address_id,
             organisation_main_contact_name, organisation_main_contact_email, organisation_main_contact_phone,
             organisation_registrant_name, organisation_registrant_date_of_birth, organisation_registrant_email,
             organisation_registrant_phone_number)
            VALUES (?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
        return connection.prepareStatement(query)
    }

    fun prepareOrganisationalLandlordUserStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO organisational_landlord_user 
            (created_date, organisation_landlord_id, subject_identifier, name, email) 
            VALUES (?, ?, ?, ?, ?)
            """
        return connection.prepareStatement(query)
    }

    fun prepareOrganisationGoverningBodyMemberStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO organisation_governing_body_member 
            (created_date, last_modified_date, organisation_landlord_id, type, name, date_of_birth, address_id) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """
        return connection.prepareStatement(query)
    }

    fun prepareLicenceStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO license 
            (id, created_date, last_modified_date, license_type, license_number) 
            VALUES (?, ?, ?, ?, ?)
            """
        return connection.prepareStatement(query)
    }

    fun preparePropertyOwnershipStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO property_ownership 
            (id, created_date, last_modified_date, ownership_type, current_num_households, current_num_tenants, registration_number_id, 
             license_id, property_build_type, address_id, num_bedrooms,
             bills_included_list, custom_bills_included, furnished_status, rent_frequency, custom_rent_frequency, rent_amount, is_occupied, license_provide_later, tenancy_provide_later, correspondence_email, correspondence_address_id, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true)
            """
        return connection.prepareStatement(query)
    }

    fun prepareLandlordshipMembersStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO ownership_link
            (landlord_id, landlordship_id, created_date)
            VALUES (?, ?, ?)
            """
        return connection.prepareStatement(query)
    }

    fun prepareFileUploadStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO file_upload 
            (id, created_date, last_modified_date, object_key, e_tag, status, extension, file_name) 
            VALUES (?, ?, ?, ?, ?, ${FileUploadStatus.SCANNED.ordinal}, 'png', ?)
            """
        return connection.prepareStatement(query)
    }

    fun prepareGasSafetyFileUploadsStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO gas_safety_uploads 
            (property_compliance_id, gas_safety_file_uploads_id) 
            VALUES (?, ?)
            """
        return connection.prepareStatement(query)
    }

    fun prepareElectricalSafetyFileUploadsStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO electrical_safety_uploads 
            (property_compliance_id, electrical_safety_file_uploads_id) 
            VALUES (?, ?)
            """
        return connection.prepareStatement(query)
    }

    fun preparePropertyComplianceStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO property_compliance 
            (id, created_date, last_modified_date, property_ownership_id, gas_safety_cert_issue_date, has_gas_supply,
            electrical_safety_expiry_date,electrical_cert_type,
            epc_url, epc_expiry_date,
            tenancy_started_before_epc_expiry, epc_energy_rating, epc_exemption_reason, epc_mees_exemption_reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
        return connection.prepareStatement(query)
    }

    fun prepareReminderEmailSentStatement(): PreparedStatement {
        val query =
            """ 
            INSERT INTO reminder_email_sent 
            (id, last_reminder_email_sent_date) 
            VALUES (?, ?)
            """
        return connection.prepareStatement(query)
    }

    fun prepareSavedJourneyStateStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO saved_journey_state 
            (id, created_date, last_modified_date, journey_id, serialized_state, subject_identifier, reminder_email_sent_id) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """
        return connection.prepareStatement(query)
    }

    fun prepareLandlordIncompletePropertyStatement(): PreparedStatement {
        val query =
            """
            INSERT INTO landlord_incomplete_properties 
            (user_id, saved_journey_state_id) 
            VALUES (?, ?)
            """
        return connection.prepareStatement(query)
    }

    fun findAddresses(
        limit: Int,
        offset: Int,
        restrictToAvailable: Boolean = false,
    ): List<Address> {
        val query =
            """
            SELECT * FROM address a
            WHERE a.local_council_id IS NOT NULL
            AND (
                NOT EXISTS (
                    SELECT 1 FROM property_ownership po
                    WHERE po.is_active AND po.address_id = a.id
                )
                OR NOT :restrictToAvailable
            )
            ORDER BY a.id
            LIMIT :limit OFFSET :offset
            """
        return session
            .createNativeQuery(query, Address::class.java)
            .setParameter("restrictToAvailable", restrictToAvailable)
            .setParameter("limit", limit)
            .setParameter("offset", offset)
            .resultList
    }

    /**
     * Counts addresses that would be returned by [findAddresses]. The seeder uses this once to initialise the
     * in-memory counts used while it is the only database writer.
     */
    fun countAvailableAddresses(restrictToAvailable: Boolean): Int {
        val query =
            """
            SELECT count(*) FROM address a
            WHERE a.local_council_id IS NOT NULL
            AND (
                NOT EXISTS (
                    SELECT 1 FROM property_ownership po
                    WHERE po.is_active AND po.address_id = a.id
                )
                OR NOT ?
            )
            """
        connection.prepareStatement(query).use { statement ->
            statement.setBoolean(1, restrictToAvailable)
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                return resultSet.getInt(1)
            }
        }
    }

    fun findRegistrationNumbersIn(numbers: Set<Long>): List<Long> {
        val query = "SELECT number FROM registration_number WHERE number IN :numbers"
        return session.createNativeQuery(query, Long::class.java).setParameter("numbers", numbers).resultList
    }

    fun updateIdSequences() {
        val manuallyInsertedIdTables =
            listOf(
                "registration_number",
                "landlord",
                "license",
                "property_ownership",
                "file_upload",
                "property_compliance",
                "reminder_email_sent",
                "saved_journey_state",
            )
        manuallyInsertedIdTables.forEach { table ->
            val query = "SELECT setval(pg_get_serial_sequence('$table', 'id'), (SELECT COALESCE(MAX(id), 1) FROM $table))"
            session.createNativeQuery(query, Long::class.java).singleResult
        }
    }
}
