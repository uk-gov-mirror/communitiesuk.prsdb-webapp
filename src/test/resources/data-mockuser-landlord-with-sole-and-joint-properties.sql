INSERT INTO prsdb_user (id, created_date)
VALUES ('urn:fdc:gov.uk:2022:UVWXY', '10/14/24'),
       ('urn:fdc:gov.uk:2022:ABCDE', '10/14/24');

INSERT INTO registration_number (id, created_date, number, type)
VALUES (1, '09/13/24', 2001001001, 1),
       (2, '09/13/24', 2001001002, 1),
       (3, '09/13/24', 1001001001, 0),
       (4, '09/13/24', 1001001002, 0);
SELECT setval(pg_get_serial_sequence('registration_number', 'id'), (SELECT MAX(id) FROM registration_number));

INSERT INTO address (id, created_date, last_modified_date, uprn, single_line_address, local_council_id, postcode)
VALUES (1, '09/13/24', '09/13/24', 1, '1 Fictional Road', 2, 'EG1 1EG'),
       (2, '09/13/24', '09/13/24', 2, '2 Fake Way', 2, 'EG1 1EG'),
       (3, '09/13/24', '09/13/24', 3, '3 Imaginary Street', 2, 'EG1 1EG');
SELECT setval(pg_get_serial_sequence('address', 'id'), (SELECT MAX(id) FROM address));

INSERT INTO landlord (id, created_date, last_modified_date, registration_number_id, individual_address_id, individual_date_of_birth,
                      individual_is_active, individual_phone_number, individual_subject_identifier, individual_name, individual_email, individual_country_of_residence, individual_is_verified, individual_has_accepted_privacy_notice)
VALUES (1, '09/13/24', '09/13/24', 1, 1, '09/13/2000', true, 07111111111, 'urn:fdc:gov.uk:2022:UVWXY',
        'Alexander Smith', 'alex.surname@example.com', 'England or Wales', true, true),
       (2, '09/13/24', '09/13/24', 2, 1, '09/13/2000', true, 07222222222, 'urn:fdc:gov.uk:2022:ABCDE',
        'Co Owner', 'co.owner@example.com', 'England or Wales', true, true);
SELECT setval(pg_get_serial_sequence('landlord', 'id'), (SELECT MAX(id) FROM landlord));

INSERT INTO property_ownership (id, is_active, ownership_type, current_num_households, current_num_tenants,
                                registration_number_id, address_id, property_build_type, num_bedrooms, rent_amount,
                                marked_joint_landlord, is_occupied, correspondence_email, correspondence_address_id)
VALUES (1, true, 1, 0, 0, 3, 2, 1, null, null, false, false, 'email@example.com', 1),
       (2, true, 1, 1, 2, 4, 3, 1, 1, 123.12, true, true, 'email@example.com', 1);
SELECT setval(pg_get_serial_sequence('property_ownership', 'id'), (SELECT MAX(id) FROM property_ownership));

INSERT INTO ownership_link (landlord_id, landlordship_id, created_date)
VALUES (1, 1, '2025-01-15'),
       (1, 2, '2025-01-15'),
       (2, 2, '2025-01-15');
