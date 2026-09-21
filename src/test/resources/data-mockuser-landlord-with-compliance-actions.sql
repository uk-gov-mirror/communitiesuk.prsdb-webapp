INSERT INTO prsdb_user (id, created_date)
VALUES ('urn:fdc:gov.uk:2022:UVWXY', '10/14/24');

INSERT INTO registration_number (id, created_date, number, type)
VALUES (1, '09/13/24', 2001001001, 1),
       (2, '3/26/25', 0006001004, 0),
       (3, '3/26/25', 7006001006, 0),
       (4, '3/26/25', 0006001008, 0),
       (5, '3/26/25', 0006001010, 0),
       (6, '3/26/25', 0006001012, 0),
       (7, '3/26/25', 0006001014, 0);
SELECT setval(pg_get_serial_sequence('registration_number', 'id'), (SELECT MAX(id) FROM registration_number));

INSERT INTO address (id, created_date, last_modified_date, uprn, single_line_address, local_council_id, postcode)
VALUES  (1, '09/13/24', '09/13/24', 1, '1 Fictional Road', 2, 'EG1 1EG'),
        (2, '09/13/24', '09/13/24', 2, '2 Fake Way', 2, 'EG1 1EG'),
        (3, '09/13/24', '09/13/24', 3, '3 Imaginary Street', 2, 'EG1 1EG'),
        (4, '09/13/24', '09/13/24', 4, '4 Pretend Crescent', 2, 'EG1 1EG'),
        (5, '09/13/24', '09/13/24', 5, '5 Invented Lane', 2, 'EG1 1EG'),
        (6, '09/13/24', '09/13/24', 6, '6 Fabricated Avenue', 2, 'EG1 1EG'),
        (7, '09/13/24', '09/13/24', 7, '7 Mythical Drive', 2, 'EG1 1EG');
SELECT setval(pg_get_serial_sequence('address', 'id'), (SELECT MAX(id) FROM address));

INSERT INTO landlord (id, created_date, last_modified_date, registration_number_id, individual_address_id, individual_date_of_birth,
                      individual_is_active, individual_phone_number, individual_subject_identifier, individual_name, individual_email, individual_country_of_residence, individual_is_verified, individual_has_accepted_privacy_notice)
VALUES (1, '09/13/24', '09/13/24', 1, 1, '09/13/2000', true, 07111111111, 'urn:fdc:gov.uk:2022:UVWXY',
        'Alexander Smith', 'alex.surname@example.com', 'England or Wales', false, true);
SELECT setval(pg_get_serial_sequence('landlord', 'id'), (SELECT MAX(id) FROM landlord));

-- Correspondence addresses
INSERT INTO address (id, single_line_address, postcode) VALUES
    (8500700001, 'Correspondence address for property row 1', 'CO1 1CO'),
    (8500700002, 'Correspondence address for property row 2', 'CO1 1CO'),
    (8500700003, 'Correspondence address for property row 3', 'CO1 1CO'),
    (8500700004, 'Correspondence address for property row 4', 'CO1 1CO'),
    (8500700005, 'Correspondence address for property row 5', 'CO1 1CO'),
    (8500700006, 'Correspondence address for property row 6', 'CO1 1CO');

INSERT INTO property_ownership (id, is_active, ownership_type, current_num_households, current_num_tenants, registration_number_id, address_id, created_date, property_build_type,
                                num_bedrooms, bills_included_list, custom_bills_included, furnished_status, rent_frequency, custom_rent_frequency, rent_amount, last_occupied_date, is_occupied, correspondence_email, correspondence_address_id)
VALUES (1, true, 1, 1, 2, 2, 2, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 8500700001),
       (2, true, 1, 0, 0, 3, 3, current_date, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500700002),
       (3, true, 1, 1, 2, 4, 4, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 8500700003),
       (4, true, 1, 0, 0, 5, 5, current_date, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500700004),
       (5, true, 1, 1, 2, 6, 6, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 8500700005),
       (6, true, 1, 1, 2, 7, 7, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 8500700006);

INSERT INTO ownership_link (landlord_id, landlordship_id, created_date)
VALUES (1, 1, '2025-01-15'),
       (1, 2, '2025-01-15'),
       (1, 3, '2025-01-15'),
       (1, 4, '2025-01-15'),
       (1, 5, '2025-01-15'),
       (1, 6, '2025-01-15');

INSERT INTO property_compliance (id, property_ownership_id, created_date, last_modified_date, gas_safety_cert_issue_date, has_gas_supply, electrical_safety_expiry_date, electrical_cert_type, epc_url, epc_expiry_date, tenancy_started_before_epc_expiry, epc_energy_rating, epc_exemption_reason, epc_mees_exemption_reason, has_fire_safety_declaration, has_keep_property_safe_declaration, has_responsibility_to_tenants_declaration, gas_safety_cert_provide_later, electrical_safety_cert_provide_later)
VALUES
       (1, 3, '01/01/25', '01/01/25', null, true, current_date + 365, null, 'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-1050-2867', '2013-02-28', false, 'c', null, null, true, true, true, null, null),
       (2, 4, '01/01/25', '01/01/25', null, true, null, null, 'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-1050-2867', '2013-02-28', false, 'c', null, null, true, true, true, null, null),
       (3, 1, '01/01/25', '01/01/25', current_date, true, null, null, 'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-1050-2867', current_date+30, null, 'g', null, null, true, true, true, null, null),
       (4, 2, '01/01/25', '01/01/25', current_date, true, null, null, 'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-1050-2867', current_date+30, null, 'g', null, null, true, true, true, null, null),
       (5, 5, '01/01/25', '01/01/25', null, true, current_date + 365, null, 'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-1050-2867', current_date+30, null, 'c', null, null, true, true, true, true, true),
       (6, 6, '01/01/25', '01/01/25', current_date - 366, true, current_date - 1, null, 'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-1050-2867', current_date+30, null, 'c', null, null, true, true, true, null, null);
