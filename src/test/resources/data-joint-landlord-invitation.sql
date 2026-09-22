INSERT INTO prsdb_user (id, created_date)
VALUES ('urn:fdc:gov.uk:2022:UVWXY', '10/14/24'),
       ('urn:fdc:gov.uk:2022:ABCDE', '10/14/24'),
       ('ia-mock-user-12345', '10/14/24');

INSERT INTO registration_number (id, created_date, number, type)
VALUES (1, '09/13/24', 2001001001, 1),
       (2, '09/13/24', 1001001001, 0),
       (3, '09/13/24', 3001001001, 1),
       (4, '09/13/24', 1001001002, 0);
SELECT setval(pg_get_serial_sequence('registration_number', 'id'), (SELECT MAX(id) FROM registration_number));

INSERT INTO address (id, created_date, last_modified_date, uprn, single_line_address, local_council_id, postcode)
VALUES (1, '09/13/24', '09/13/24', 1, '1 Fictional Road', 2, 'EG1 1EG'),
       (2, '09/13/24', '09/13/24', 2, '2 Fake Way', 2, 'EG1 1EG'),
       (3, '09/13/24', '09/13/24', 3, '3 Test Lane', 2, 'EG1 1EG');
SELECT setval(pg_get_serial_sequence('address', 'id'), (SELECT MAX(id) FROM address));

INSERT INTO landlord (id, created_date, last_modified_date, registration_number_id, individual_address_id, individual_date_of_birth,
                      individual_is_active, individual_phone_number, individual_subject_identifier, individual_name, individual_email, individual_country_of_residence, individual_is_verified, individual_has_accepted_privacy_notice)
VALUES (1, '09/13/24', '09/13/24', 1, 1, '09/13/2000', true, 07111111111, 'urn:fdc:gov.uk:2022:UVWXY',
        'Invited User', 'invited.user@example.com', 'England or Wales', false, true),
       (2, '09/13/24', '09/13/24', 3, 1, '09/13/2000', true, 07111111111, 'urn:fdc:gov.uk:2022:ABCDE',
        'Original Landlord', 'original.landlord@example.com', 'England or Wales', false, true);
SELECT setval(pg_get_serial_sequence('landlord', 'id'), (SELECT MAX(id) FROM landlord));

INSERT INTO property_ownership (id, is_active, ownership_type, current_num_households, current_num_tenants,
                                registration_number_id, address_id, created_date, property_build_type,
                                num_bedrooms, bills_included_list, custom_bills_included, furnished_status,
                                rent_frequency, custom_rent_frequency, rent_amount, is_occupied, last_occupied_date, correspondence_email, correspondence_address_id)

-- Both properties are occupied with no licence, so under the new registration layout
-- (PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING) they render the licensing "provide later"
-- deadline, which requires last_occupied_date to be set.
-- property the default user is not yet invited to
VALUES (1, true, 1, 1, 2, 2, 2, current_date, 1,
        1, null, null, 2, 1, null, 123.12, true, current_date - INTERVAL '7 days', 'email@example.com', 1),
-- property the default user is primary landlord for
       (2, true, 1, 1, 4, 1, 3,  current_date, 1,
        1, null, null, 2, 1, null, 200.00, true, current_date - INTERVAL '7 days', 'email@example.com', 1);
SELECT setval(pg_get_serial_sequence('property_ownership', 'id'), (SELECT MAX(id) FROM property_ownership));

-- Every registered property has a compliance record (see PropertyDetailsController), so both
-- property ownerships need one for their property details pages to load.
INSERT INTO property_compliance (id, property_ownership_id, created_date, last_modified_date, gas_safety_cert_issue_date, has_gas_supply,
                                 electrical_safety_expiry_date, electrical_cert_type, epc_url, epc_expiry_date,
                                 tenancy_started_before_epc_expiry, epc_energy_rating, epc_exemption_reason, epc_mees_exemption_reason,
                                 has_fire_safety_declaration, has_keep_property_safe_declaration, has_responsibility_to_tenants_declaration)
VALUES (1, 1, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (2, 2, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true);
SELECT setval(pg_get_serial_sequence('property_compliance', 'id'), (SELECT MAX(id) FROM property_compliance));

INSERT INTO ownership_link (landlord_id, landlordship_id, created_date)
VALUES (2, 1, '2025-01-15'),
       (1, 2, '2025-01-15');

INSERT INTO local_council_user (subject_identifier, is_manager, local_council_id, created_date, last_modified_date,
                                name, email, has_accepted_privacy_notice)
VALUES ('ia-mock-user-12345', true, 2, '10/14/24', '10/14/24', 'Mock User IA', 'test@example.com', true);
SELECT setval(pg_get_serial_sequence('local_council_user', 'id'), (SELECT MAX(id) FROM local_council_user));

INSERT INTO joint_landlord_invitation (id, invited_email, registered_propertyid, token, inviting_landlord_name, created_date)
VALUES
    -- Pending invitation on property ownership 1 (owned by landlord 2/ABCDE, not the mock user)
    (1, 'invited@example.com', 1, 'aaaabbbb-cccc-dddd-eeee-ffff00001111', 'Original Landlord', current_date),
    -- Expired invitation on property ownership 2 (owned by mock user UVWXY) - created_date is old enough to be expired
    (2, 'expired@example.com', 2, 'aaaabbbb-cccc-dddd-eeee-ffff00003333', 'Alexander Smith', '01/01/2025'),
    -- Pending invitation on property ownership 2 (owned by mock user UVWXY)
    (3, 'pending@example.com', 2, 'aaaabbbb-cccc-dddd-eeee-ffff00004444', 'Alexander Smith', current_date);
SELECT setval(pg_get_serial_sequence('joint_landlord_invitation', 'id'), (SELECT MAX(id) FROM joint_landlord_invitation));
