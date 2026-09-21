INSERT INTO prsdb_user (id, created_date)
VALUES ('urn:fdc:gov.uk:2022:UVWXY', '10/14/24');

INSERT INTO registration_number (id, created_date, number, type)
VALUES (1, '09/13/24', 2001001001, 1),
       (2, '3/26/25', 1006001004, 0),
       (3, '3/26/25', 1006001006, 0),
       (4, '3/26/25', 1006001008, 0),
       (5, '3/26/25', 1006001010, 0),
       (6, '3/26/25', 1006001012, 0),
       (7, '3/26/25', 1006001014, 0),
       (8, '3/26/25', 1006001016, 0),
       (9, '3/26/25', 1006001018, 0),
       (10, '3/26/25', 1006001020, 0),
       (11, '3/26/25', 1006001022, 0),
       (12, '3/26/25', 1006001024, 0),
       (13, '3/26/25', 1006001026, 0),
       (14, '3/26/25', 1006001028, 0),
       (15, '3/26/25', 1006001030, 0),
       (16, '3/26/25', 1006001032, 0),
       (17, '3/26/25', 1006001034, 0),
       (18, '3/26/25', 1006001036, 0),
       (19, '3/26/25', 1006001038, 0),
       (20, '3/26/25', 1006001040, 0),
       (21, '3/26/25', 1006001042, 0),
       (22, '3/26/25', 1006001044, 0);
SELECT setval(pg_get_serial_sequence('registration_number', 'id'), (SELECT MAX(id) FROM registration_number));

INSERT INTO address (id, created_date, last_modified_date, uprn, single_line_address, local_council_id, postcode)
VALUES  (1, '09/13/24', '09/13/24', 1, '1 Landlord Address', 2, 'EG1 1EG'),
        (2, '09/13/24', '09/13/24', 2, 'EPC Provide Later Occupied', 2, 'EG1 1EG'),
        (3, '09/13/24', '09/13/24', 3, 'EPC Provide Later Unoccupied', 2, 'EG1 1EG'),
        (4, '09/13/24', '09/13/24', 4, 'EPC Valid High Rating Occupied', 2, 'EG1 1EG'),
        (5, '09/13/24', '09/13/24', 5, 'EPC Valid High Rating Unoccupied', 2, 'EG1 1EG'),
        (6, '09/13/24', '09/13/24', 6, 'EPC Valid Low Exempt Occupied', 2, 'EG1 1EG'),
        (7, '09/13/24', '09/13/24', 7, 'EPC Valid Low Exempt Unoccupied', 2, 'EG1 1EG'),
        (8, '09/13/24', '09/13/24', 8, 'EPC Valid Low No Exempt Occupied', 2, 'EG1 1EG'),
        (9, '09/13/24', '09/13/24', 9, 'EPC Valid Low No Exempt Unoccupied', 2, 'EG1 1EG'),
        (10, '09/13/24', '09/13/24', 10, 'EPC Expired Tenancy Before High Occupied', 2, 'EG1 1EG'),
        (11, '09/13/24', '09/13/24', 11, 'EPC Expired Tenancy Before High Unoccupied', 2, 'EG1 1EG'),
        (12, '09/13/24', '09/13/24', 12, 'EPC Expired Tenancy Before Low Exempt Occupied', 2, 'EG1 1EG'),
        (13, '09/13/24', '09/13/24', 13, 'EPC Expired Tenancy Before Low Exempt Unoccupied', 2, 'EG1 1EG'),
        (14, '09/13/24', '09/13/24', 14, 'EPC Expired Tenancy Before Low No Exempt Occupied', 2, 'EG1 1EG'),
        (15, '09/13/24', '09/13/24', 15, 'EPC Expired Tenancy Before Low No Exempt Unoccupied', 2, 'EG1 1EG'),
        (16, '09/13/24', '09/13/24', 16, 'EPC Expired Not In Date Occupied', 2, 'EG1 1EG'),
        (17, '09/13/24', '09/13/24', 17, 'EPC Expired Not In Date Unoccupied', 2, 'EG1 1EG'),
        (18, '09/13/24', '09/13/24', 18, 'EPC No EPC Required Occupied', 2, 'EG1 1EG'),
        (19, '09/13/24', '09/13/24', 19, 'EPC No EPC Required Unoccupied', 2, 'EG1 1EG'),
        (20, '09/13/24', '09/13/24', 20, 'EPC No EPC Not Required Occupied', 2, 'EG1 1EG'),
        (21, '09/13/24', '09/13/24', 21, 'EPC No EPC Not Required Unoccupied', 2, 'EG1 1EG'),
        (22, '09/13/24', '09/13/24', 22, 'EPC Provide Later Occupied After Registration', 2, 'EG1 1EG');
SELECT setval(pg_get_serial_sequence('address', 'id'), (SELECT MAX(id) FROM address));

INSERT INTO landlord (id, created_date, last_modified_date, registration_number_id, individual_address_id, individual_date_of_birth,
                      individual_is_active, individual_phone_number, individual_subject_identifier, individual_name, individual_email, individual_country_of_residence, individual_is_verified, individual_has_accepted_privacy_notice)
VALUES (1, '09/13/24', '09/13/24', 1, 1, '09/13/2000', true, 07111111111, 'urn:fdc:gov.uk:2022:UVWXY',
        'Alexander Smith', 'alex.surname@example.com', 'England or Wales', false, true);
SELECT setval(pg_get_serial_sequence('landlord', 'id'), (SELECT MAX(id) FROM landlord));

-- Occupied properties have current_num_tenants > 0, unoccupied have 0
-- Correspondence addresses
INSERT INTO address (id, single_line_address, postcode) VALUES
    (8500900001, 'Correspondence address for property row 1', 'CO1 1CO'),
    (8500900002, 'Correspondence address for property row 2', 'CO1 1CO'),
    (8500900003, 'Correspondence address for property row 3', 'CO1 1CO'),
    (8500900004, 'Correspondence address for property row 4', 'CO1 1CO'),
    (8500900005, 'Correspondence address for property row 5', 'CO1 1CO'),
    (8500900006, 'Correspondence address for property row 6', 'CO1 1CO'),
    (8500900007, 'Correspondence address for property row 7', 'CO1 1CO'),
    (8500900008, 'Correspondence address for property row 8', 'CO1 1CO'),
    (8500900009, 'Correspondence address for property row 9', 'CO1 1CO'),
    (8500900010, 'Correspondence address for property row 10', 'CO1 1CO'),
    (8500900011, 'Correspondence address for property row 11', 'CO1 1CO'),
    (8500900012, 'Correspondence address for property row 12', 'CO1 1CO'),
    (8500900013, 'Correspondence address for property row 13', 'CO1 1CO'),
    (8500900014, 'Correspondence address for property row 14', 'CO1 1CO'),
    (8500900015, 'Correspondence address for property row 15', 'CO1 1CO'),
    (8500900016, 'Correspondence address for property row 16', 'CO1 1CO'),
    (8500900017, 'Correspondence address for property row 17', 'CO1 1CO'),
    (8500900018, 'Correspondence address for property row 18', 'CO1 1CO'),
    (8500900019, 'Correspondence address for property row 19', 'CO1 1CO'),
    (8500900020, 'Correspondence address for property row 20', 'CO1 1CO'),
    (8500900021, 'Correspondence address for property row 21', 'CO1 1CO');

INSERT INTO property_ownership (id, is_active, ownership_type, current_num_households, current_num_tenants, registration_number_id, address_id, created_date, property_build_type,
                                num_bedrooms, bills_included_list, custom_bills_included, furnished_status, rent_frequency, custom_rent_frequency, rent_amount, last_occupied_date, is_occupied, correspondence_email, correspondence_address_id)
VALUES (1, true, 1, 1, 2, 2, 2, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 8500900001),
       (2, true, 1, 0, 0, 3, 3, current_date, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500900002),
       (3, true, 1, 1, 2, 4, 4, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 8500900003),
       (4, true, 1, 0, 0, 5, 5, current_date, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500900004),
       (5, true, 1, 1, 2, 6, 6, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 8500900005),
       (6, true, 1, 0, 0, 7, 7, current_date, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500900006),
       (7, true, 1, 1, 2, 8, 8, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 8500900007),
       (8, true, 1, 0, 0, 9, 9, current_date, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500900008),
       (9, true, 1, 1, 2, 10, 10, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 8500900009),
       (10, true, 1, 0, 0, 11, 11, current_date, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500900010),
       (11, true, 1, 1, 2, 12, 12, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 8500900011),
       (12, true, 1, 0, 0, 13, 13, current_date, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500900012),
       (13, true, 1, 1, 2, 14, 14, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 8500900013),
       (14, true, 1, 0, 0, 15, 15, current_date, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500900014),
       (15, true, 1, 1, 2, 16, 16, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 8500900015),
       (16, true, 1, 0, 0, 17, 17, current_date, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500900016),
       (17, true, 1, 1, 2, 18, 18, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 8500900017),
       (18, true, 1, 0, 0, 19, 19, current_date, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500900018),
       (19, true, 1, 1, 2, 20, 20, current_date, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 8500900019),
       (20, true, 1, 0, 0, 21, 21, current_date, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500900020);

-- 21: occupied AFTER registration (created_date in the past, last_occupied_date = current_date), so the provide-later
-- deadline has no dated deadline and shows the "within 28 days" message when the flag is on.
INSERT INTO property_ownership (id, is_active, ownership_type, current_num_households, current_num_tenants, registration_number_id, address_id, created_date, property_build_type,
                                num_bedrooms, bills_included_list, custom_bills_included, furnished_status, rent_frequency, custom_rent_frequency, rent_amount, last_occupied_date, is_occupied, correspondence_email, correspondence_address_id)
VALUES (21, true, 1, 1, 2, 22, 22, current_date - 100, 1, 1, null, null, 2, 1, null, 123.12, current_date, true, 'email@example.com', 8500900021);

INSERT INTO ownership_link (landlord_id, landlordship_id, created_date)
VALUES (1, 1, '2025-01-15'),
       (1, 2, '2025-01-15'),
       (1, 3, '2025-01-15'),
       (1, 4, '2025-01-15'),
       (1, 5, '2025-01-15'),
       (1, 6, '2025-01-15'),
       (1, 7, '2025-01-15'),
       (1, 8, '2025-01-15'),
       (1, 9, '2025-01-15'),
       (1, 10, '2025-01-15'),
       (1, 11, '2025-01-15'),
       (1, 12, '2025-01-15'),
       (1, 13, '2025-01-15'),
       (1, 14, '2025-01-15'),
       (1, 15, '2025-01-15'),
       (1, 16, '2025-01-15'),
       (1, 17, '2025-01-15'),
       (1, 18, '2025-01-15'),
       (1, 19, '2025-01-15'),
       (1, 20, '2025-01-15'),
       (1, 21, '2025-01-15');

-- Gas: occupied=HAS_FAULTS (has_gas=true, no issue date), unoccupied=EXPIRED (issue date far in past) to ensure properties show on page
-- EICR: all valid (expiry in future) so it doesn't interfere
-- EPC: varies per test case
INSERT INTO property_compliance (id, property_ownership_id, created_date, last_modified_date, gas_safety_cert_issue_date, has_gas_supply, electrical_safety_expiry_date, electrical_cert_type, epc_url, epc_expiry_date, tenancy_started_before_epc_expiry, epc_energy_rating, epc_exemption_reason, epc_mees_exemption_reason, has_fire_safety_declaration, has_keep_property_safe_declaration, has_responsibility_to_tenants_declaration, gas_safety_cert_provide_later, electrical_safety_cert_provide_later, epc_provide_later)
VALUES
       -- 1: Provide later, occupied
       (1, 1, '01/01/25', '01/01/25', null, true, current_date + 365, null, null, null, null, null, null, null, true, true, true, null, null, true),
       -- 2: Provide later, unoccupied
       (2, 2, '01/01/25', '01/01/25', current_date - 366, true, current_date + 365, null, null, null, null, null, null, null, true, true, true, null, null, true),
       -- 3: Valid, high rating, occupied
       (3, 3, '01/01/25', '01/01/25', null, true, current_date + 365, null, 'https://example.com/epc', current_date + 30, null, 'c', null, null, true, true, true, null, null, null),
       -- 4: Valid, high rating, unoccupied
       (4, 4, '01/01/25', '01/01/25', current_date - 366, true, current_date + 365, null, 'https://example.com/epc', current_date + 30, null, 'c', null, null, true, true, true, null, null, null),
       -- 5: Valid, low rating, has MEES exemption, occupied
       (5, 5, '01/01/25', '01/01/25', null, true, current_date + 365, null, 'https://example.com/epc', current_date + 30, null, 'g', null, 0, true, true, true, null, null, null),
       -- 6: Valid, low rating, has MEES exemption, unoccupied
       (6, 6, '01/01/25', '01/01/25', current_date - 366, true, current_date + 365, null, 'https://example.com/epc', current_date + 30, null, 'g', null, 0, true, true, true, null, null, null),
       -- 7: Valid, low rating, no exemption, occupied
       (7, 7, '01/01/25', '01/01/25', null, true, current_date + 365, null, 'https://example.com/epc', current_date + 30, null, 'g', null, null, true, true, true, null, null, null),
       -- 8: Valid, low rating, no exemption, unoccupied
       (8, 8, '01/01/25', '01/01/25', current_date - 366, true, current_date + 365, null, 'https://example.com/epc', current_date + 30, null, 'g', null, null, true, true, true, null, null, null),
       -- 9: Expired, tenancy before expiry, high rating, occupied
       (9, 9, '01/01/25', '01/01/25', null, true, current_date + 365, null, 'https://example.com/epc', current_date - 1, true, 'c', null, null, true, true, true, null, null, null),
       -- 10: Expired, tenancy before expiry, high rating, unoccupied
       (10, 10, '01/01/25', '01/01/25', current_date - 366, true, current_date + 365, null, 'https://example.com/epc', current_date - 1, null, 'c', null, null, true, true, true, null, null, null),
       -- 11: Expired, tenancy before expiry, low rating, has MEES exemption, occupied
       (11, 11, '01/01/25', '01/01/25', null, true, current_date + 365, null, 'https://example.com/epc', current_date - 1, true, 'g', null, 0, true, true, true, null, null, null),
       -- 12: Expired, tenancy before expiry, low rating, has MEES exemption, unoccupied
       (12, 12, '01/01/25', '01/01/25', current_date - 366, true, current_date + 365, null, 'https://example.com/epc', current_date - 1, null, 'g', null, 0, true, true, true, null, null, null),
       -- 13: Expired, tenancy before expiry, low rating, no exemption, occupied
       (13, 13, '01/01/25', '01/01/25', null, true, current_date + 365, null, 'https://example.com/epc', current_date - 1, true, 'g', null, null, true, true, true, null, null, null),
       -- 14: Expired, tenancy before expiry, low rating, no exemption, unoccupied
       (14, 14, '01/01/25', '01/01/25', current_date - 366, true, current_date + 365, null, 'https://example.com/epc', current_date - 1, null, 'g', null, null, true, true, true, null, null, null),
       -- 15: Expired, EPC not in date when tenancy began, occupied
       (15, 15, '01/01/25', '01/01/25', null, true, current_date + 365, null, 'https://example.com/epc', current_date - 1, false, 'c', null, null, true, true, true, null, null, null),
       -- 16: Expired, EPC not in date when tenancy began, unoccupied
       (16, 16, '01/01/25', '01/01/25', current_date - 366, true, current_date + 365, null, 'https://example.com/epc', current_date - 1, null, 'c', null, null, true, true, true, null, null, null),
       -- 17: No EPC, it is required, occupied
       (17, 17, '01/01/25', '01/01/25', null, true, current_date + 365, null, null, null, null, null, null, null, true, true, true, null, null, null),
       -- 18: No EPC, it is required, unoccupied
       (18, 18, '01/01/25', '01/01/25', current_date - 366, true, current_date + 365, null, null, null, null, null, null, null, true, true, true, null, null, null),
       -- 19: No EPC, not required (has exemption), occupied
       (19, 19, '01/01/25', '01/01/25', null, true, current_date + 365, null, null, null, null, null, 0, null, true, true, true, null, null, null),
       -- 20: No EPC, not required (has exemption), unoccupied
       (20, 20, '01/01/25', '01/01/25', current_date - 366, true, current_date + 365, null, null, null, null, null, 0, null, true, true, true, null, null, null),
       -- 21: Provide later, occupied after registration (no dated deadline)
       (21, 21, '01/01/25', '01/01/25', null, true, current_date + 365, null, null, null, null, null, null, null, true, true, true, null, null, true);
