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
        (2, '09/13/24', '09/13/24', 2, '2 Fake Way', 2,'EG1 1EG'),
        (3, '09/13/24', '09/13/24', 3, '3 Imaginary Street', 2,'EG1 1EG'),
        (4, '09/13/24', '09/13/24', 4, '4 Pretend Crescent', 2,'EG1 1EG');
SELECT setval(pg_get_serial_sequence('address', 'id'), (SELECT MAX(id) FROM address));

INSERT INTO landlord (id, created_date, last_modified_date, registration_number_id, individual_address_id, individual_date_of_birth,
                      individual_is_active, individual_phone_number, individual_subject_identifier, individual_name, individual_email, individual_country_of_residence, individual_is_verified, individual_has_accepted_privacy_notice)
VALUES (1, '09/13/24', '09/13/24', 1, 1, '09/13/2000', true, 07111111111, 'urn:fdc:gov.uk:2022:UVWXY',
        'Alexander Smith', 'alex.surname@example.com', 'England or Wales', false, true);
SELECT setval(pg_get_serial_sequence('landlord', 'id'), (SELECT MAX(id) FROM landlord));

INSERT INTO property_ownership (id, is_active, ownership_type, current_num_households, current_num_tenants, registration_number_id, address_id, created_date, property_build_type,
    num_bedrooms, bills_included_list, custom_bills_included, furnished_status, rent_frequency, custom_rent_frequency, rent_amount, is_occupied, correspondence_email, correspondence_address_id)
VALUES (1, true, 1, 1, 2, 2, 2, current_date, 1, 1, null, null, 2, 1, null, 123.12, true, 'email@example.com', 1),
       (2, true, 1, 1, 2, 3, 3, current_date, 1, 1, null, null, 2, 1, null, 123.12, true, 'email@example.com', 1),
       (3, true, 1, 1, 2, 4, 4, current_date, 1, 1, null, null, 2, 1, null, 123.12, true, 'email@example.com', 1);

INSERT INTO ownership_link (landlord_id, landlordship_id, created_date)
VALUES (1, 1, '2025-01-15'),
       (1, 2, '2025-01-15'),
       (1, 3, '2025-01-15');

INSERT INTO property_compliance (id, property_ownership_id, created_date, last_modified_date, gas_safety_cert_issue_date, has_gas_supply, electrical_safety_expiry_date, electrical_cert_type, epc_url, epc_expiry_date, tenancy_started_before_epc_expiry, epc_energy_rating, epc_exemption_reason, epc_mees_exemption_reason, has_fire_safety_declaration, has_keep_property_safe_declaration, has_responsibility_to_tenants_declaration)
VALUES
       (1, 2, '01/01/25', '01/01/25', null, true, null, null, 'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-1050-2867', '2013-02-28', false, 'g', null, null, true, true, true),
       (2, 3, '01/01/25', '01/01/25', current_date, true, current_date + 365, null, null, null, null, null, 0, null, true, true, true),
       (3, 1, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true);

INSERT INTO saved_journey_state (id, created_date, last_modified_date, journey_id, serialized_state, subject_identifier)
VALUES (1, current_date, current_date, '1', '{"journeyData":{"lookup-address":{"houseNameOrNumber":"6","postcode":"NW51tl"},"select-address":{"address":"4, Example Road, EG"},"property-type":{"customPropertyType":"","propertyType":"FLAT"}},"cachedAddresses":"[{\"singleLineAddress\":\"1, Example Road, EG\",\"localCouncilId\":2,\"uprn\":1123456,\"buildingNumber\":\"1\",\"postcode\":\"EG\"},{\"singleLineAddress\":\"2, Example Road, EG\",\"localCouncilId\":2,\"uprn\":2123456,\"buildingNumber\":\"2\",\"postcode\":\"EG\"},{\"singleLineAddress\":\"3, Example Road, EG\",\"localCouncilId\":2,\"uprn\":3123456,\"buildingNumber\":\"3\",\"postcode\":\"EG\"},{\"singleLineAddress\":\"4, Example Road, EG\",\"localCouncilId\":4,\"uprn\":4123456,\"buildingNumber\":\"4\",\"postcode\":\"EG\"},{\"singleLineAddress\":\"5, Example Road, EG\",\"localCouncilId\":5,\"uprn\":5123456,\"buildingNumber\":\"5\",\"postcode\":\"EG\"}]","isAddressAlreadyRegistered":"false"}', 'urn:fdc:gov.uk:2022:UVWXY'),
       (2, current_date, current_date, '2', '{"journeyData":{"lookup-address":{"houseNameOrNumber":"6","postcode":"NW51tl"},"select-address":{"address":"5, Example Road, EG"},"property-type":{"customPropertyType":"","propertyType":"FLAT"}},"cachedAddresses":"[{\"singleLineAddress\":\"1, Example Road, EG\",\"localCouncilId\":2,\"uprn\":1123456,\"buildingNumber\":\"1\",\"postcode\":\"EG\"},{\"singleLineAddress\":\"2, Example Road, EG\",\"localCouncilId\":2,\"uprn\":2123456,\"buildingNumber\":\"2\",\"postcode\":\"EG\"},{\"singleLineAddress\":\"3, Example Road, EG\",\"localCouncilId\":2,\"uprn\":3123456,\"buildingNumber\":\"3\",\"postcode\":\"EG\"},{\"singleLineAddress\":\"4, Example Road, EG\",\"localCouncilId\":4,\"uprn\":4123456,\"buildingNumber\":\"4\",\"postcode\":\"EG\"},{\"singleLineAddress\":\"5, Example Road, EG\",\"localCouncilId\":5,\"uprn\":5123456,\"buildingNumber\":\"5\",\"postcode\":\"EG\"}]","isAddressAlreadyRegistered":"false"}', 'urn:fdc:gov.uk:2022:UVWXY');

INSERT INTO landlord_incomplete_properties (user_id, saved_journey_state_id)
VALUES ('urn:fdc:gov.uk:2022:UVWXY', 1),
       ('urn:fdc:gov.uk:2022:UVWXY', 2);

