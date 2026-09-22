INSERT INTO prsdb_user (id, created_date)
VALUES ('urn:fdc:gov.uk:2022:UVWXY', '10/14/24');

INSERT INTO registration_number (id, created_date, number, type)
VALUES (1, '09/13/24', 2001001001, 1),
       (2, '3/26/25', 0006001004, 0),
       (3, '3/26/25', 7006001006, 0),
       (4, '3/26/25', 0006001008, 0),
       (5, '3/26/25', 0006001010, 0),
       (6, '3/26/25', 0006001012, 0),
       (7, '3/26/25', 0006001014, 0),
       (8, '3/26/25', 0006001016, 0),
       (9, '3/26/25', 0006001018, 0),
       (10, '3/26/25', 0006001020, 0),
       (11, '3/26/25', 0006001022, 0),
       (12, '3/26/25', 0006001024, 0);
SELECT setval(pg_get_serial_sequence('registration_number', 'id'), (SELECT MAX(id) FROM registration_number));

INSERT INTO address (id, created_date, last_modified_date, uprn, single_line_address, local_council_id, postcode)
VALUES  (1, '09/13/24', '09/13/24', 1, '1 Fictional Road', 2, 'EG1 1EG'),
        (2, '09/13/24', '09/13/24', 2, '2 Fake Way', 2, 'EG1 1EG'),
        (3, '09/13/24', '09/13/24', 3, '3 Imaginary Street', 2, 'EG1 1EG'),
        (4, '09/13/24', '09/13/24', 4, '4 Pretend Crescent', 2, 'EG1 1EG'),
        (5, '09/13/24', '09/13/24', 5, '5 Invented Lane', 2, 'EG1 1EG'),
        (6, '09/13/24', '09/13/24', 6, '6 Fabricated Avenue', 2, 'EG1 1EG'),
        (7, '09/13/24', '09/13/24', 7, '7 Mythical Drive', 2, 'EG1 1EG'),
        (8, '09/13/24', '09/13/24', 8, '8 Phantom Place', 2, 'EG1 1EG'),
        (9, '09/13/24', '09/13/24', 9, '9 Illusory Court', 2, 'EG1 1EG'),
        (10, '09/13/24', '09/13/24', 10, '10 Fictive Gardens', 2, 'EG1 1EG'),
        (11, '09/13/24', '09/13/24', 11, '11 Fantasy Close', 2, 'EG1 1EG'),
        (12, '09/13/24', '09/13/24', 12, '12 Unreal Terrace', 2, 'EG1 1EG');
SELECT setval(pg_get_serial_sequence('address', 'id'), (SELECT MAX(id) FROM address));

INSERT INTO landlord (id, created_date, last_modified_date, registration_number_id, individual_address_id, individual_date_of_birth,
                      individual_is_active, individual_phone_number, individual_subject_identifier, individual_name, individual_email, individual_country_of_residence, individual_is_verified, individual_has_accepted_privacy_notice)
VALUES (1, '09/13/24', '09/13/24', 1, 1, '09/13/2000', true, 07111111111, 'urn:fdc:gov.uk:2022:UVWXY',
        'Alexander Smith', 'alex.surname@example.com', 'England or Wales', false, true);
SELECT setval(pg_get_serial_sequence('landlord', 'id'), (SELECT MAX(id) FROM landlord));

INSERT INTO property_ownership (id, is_active, ownership_type, current_num_households, current_num_tenants, registration_number_id, address_id, created_date, property_build_type,
                                num_bedrooms, bills_included_list, custom_bills_included, furnished_status, rent_frequency, custom_rent_frequency, rent_amount, last_occupied_date, is_occupied, correspondence_email, correspondence_address_id)
VALUES (1, true, 1, 1, 2, 2, 2, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 1),
       (2, true, 1, 1, 2, 3, 3, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 1),
       (3, true, 1, 1, 2, 4, 4, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 1),
       (4, true, 1, 1, 2, 5, 5, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 1),
       (5, true, 1, 1, 2, 6, 6, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 1),
       (6, true, 1, 1, 2, 7, 7, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 1),
       (7, true, 1, 1, 2, 8, 8, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 1),
       (8, true, 1, 1, 2, 9, 9, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 1),
       (9, true, 1, 1, 2, 10, 10, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 1),
       (10, true, 1, 1, 2, 11, 11, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 1),
       (11, true, 1, 1, 2, 12, 12, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 1);

INSERT INTO ownership_link (landlord_id, landlordship_id, created_date)
VALUES (1, 1, '2025-01-15'), (1, 2, '2025-01-15'), (1, 3, '2025-01-15'), (1, 4, '2025-01-15'), (1, 5, '2025-01-15'), (1, 6, '2025-01-15'), (1, 7, '2025-01-15'), (1, 8, '2025-01-15'), (1, 9, '2025-01-15'), (1, 10, '2025-01-15'), (1, 11, '2025-01-15');

INSERT INTO property_compliance (id, property_ownership_id, created_date, last_modified_date, gas_safety_cert_issue_date, has_gas_supply, electrical_safety_expiry_date, electrical_cert_type, epc_url, epc_expiry_date, tenancy_started_before_epc_expiry, epc_energy_rating, epc_exemption_reason, epc_mees_exemption_reason, has_fire_safety_declaration, has_keep_property_safe_declaration, has_responsibility_to_tenants_declaration, gas_safety_cert_provide_later, electrical_safety_cert_provide_later)
VALUES
       (1, 1, '01/01/25', '01/01/25', null, true, null, null, null, null, null, null, null, null, true, true, true, null, null),
       (2, 2, '01/01/25', '01/01/25', null, true, null, null, null, null, null, null, null, null, true, true, true, null, null),
       (3, 3, '01/01/25', '01/01/25', null, true, null, null, null, null, null, null, null, null, true, true, true, null, null),
       (4, 4, '01/01/25', '01/01/25', null, true, null, null, null, null, null, null, null, null, true, true, true, null, null),
       (5, 5, '01/01/25', '01/01/25', null, true, null, null, null, null, null, null, null, null, true, true, true, null, null),
       (6, 6, '01/01/25', '01/01/25', null, true, null, null, null, null, null, null, null, null, true, true, true, null, null),
       (7, 7, '01/01/25', '01/01/25', null, true, null, null, null, null, null, null, null, null, true, true, true, null, null),
       (8, 8, '01/01/25', '01/01/25', null, true, null, null, null, null, null, null, null, null, true, true, true, null, null),
       (9, 9, '01/01/25', '01/01/25', null, true, null, null, null, null, null, null, null, null, true, true, true, null, null),
       (10, 10, '01/01/25', '01/01/25', null, true, null, null, null, null, null, null, null, null, true, true, true, null, null),
       (11, 11, '01/01/25', '01/01/25', null, true, null, null, null, null, null, null, null, null, true, true, true, null, null);
