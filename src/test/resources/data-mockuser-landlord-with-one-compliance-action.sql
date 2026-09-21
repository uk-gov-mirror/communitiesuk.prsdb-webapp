INSERT INTO prsdb_user (id, created_date)
VALUES ('urn:fdc:gov.uk:2022:UVWXY', '10/14/24');

INSERT INTO registration_number (id, created_date, number, type)
VALUES (1, '09/13/24', 2001001001, 1),
       (2, '3/26/25', 1001001001, 0),
       (3, '3/26/25', 1001001002, 0),
       (4, '3/26/25', 1001001003, 0);
SELECT setval(pg_get_serial_sequence('registration_number', 'id'), (SELECT MAX(id) FROM registration_number));

INSERT INTO address (id, created_date, last_modified_date, uprn, single_line_address, local_council_id, postcode)
VALUES  (1, '09/13/24', '09/13/24', 1, '1 Fictional Road', 2, 'EG1 1EG'),
        (2, '09/13/24', '09/13/24', 2, '2 Fake Way', 2, 'EG1 1EG'),
        (3, '09/13/24', '09/13/24', 3, '3 Imaginary Street', 2, 'EG1 1EG'),
        (4, '09/13/24', '09/13/24', 4, '4 Pretend Crescent', 2, 'EG1 1EG');
SELECT setval(pg_get_serial_sequence('address', 'id'), (SELECT MAX(id) FROM address));

INSERT INTO landlord (id, created_date, last_modified_date, registration_number_id, individual_address_id, individual_date_of_birth,
                      individual_is_active, individual_phone_number, individual_subject_identifier, individual_name, individual_email, individual_country_of_residence, individual_is_verified, individual_has_accepted_privacy_notice)
VALUES (1, '09/13/24', '09/13/24', 1, 1, '09/13/2000', true, 07111111111, 'urn:fdc:gov.uk:2022:UVWXY',
        'Alexander Smith', 'alex.surname@example.com', 'England or Wales', false, true);
SELECT setval(pg_get_serial_sequence('landlord', 'id'), (SELECT MAX(id) FROM landlord));

-- Correspondence addresses
INSERT INTO address (id, single_line_address, postcode) VALUES
    (8501100001, 'Correspondence address for property row 1', 'CO1 1CO'),
    (8501100002, 'Correspondence address for property row 2', 'CO1 1CO'),
    (8501100003, 'Correspondence address for property row 3', 'CO1 1CO');

INSERT INTO property_ownership (id, is_active, ownership_type, current_num_households, current_num_tenants, registration_number_id, address_id, created_date, property_build_type,
                                num_bedrooms, bills_included_list, custom_bills_included, furnished_status, rent_frequency, custom_rent_frequency, rent_amount, is_occupied, correspondence_email, correspondence_address_id)
VALUES (1, true, 1, 1, 2, 2, 2, current_date, 1, 1, null, null, 2, 1, null, 123.12, true, 'email@example.com', 8501100001),
       (2, true, 1, 1, 2, 3, 3, current_date, 1, 1, null, null, 2, 1, null, 123.12, true, 'email@example.com', 8501100002),
       (3, true, 1, 1, 2, 4, 4, current_date, 1, 1, null, null, 2, 1, null, 123.12, true, 'email@example.com', 8501100003);

INSERT INTO ownership_link (landlord_id, landlordship_id, created_date)
VALUES (1, 1, '2025-01-15'),
       (1, 2, '2025-01-15'),
       (1, 3, '2025-01-15');

INSERT INTO property_compliance (id, property_ownership_id, gas_safety_cert_issue_date, has_gas_supply, electrical_safety_expiry_date, epc_exemption_reason)
VALUES
       (1, 2, current_date, true, null, 0),
       (2, 3, current_date, true, current_date + 365, 0),
       (3, 1, current_date, true, current_date + 365, 0);
