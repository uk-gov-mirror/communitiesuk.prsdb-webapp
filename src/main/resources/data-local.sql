INSERT INTO prsdb_user (id, created_date)
VALUES ('urn:fdc:gov.uk:2022:ABCDE', '09/13/24'),
       ('urn:fdc:gov.uk:2022:FGHIJ', '09/13/24'),
       ('urn:fdc:gov.uk:2022:KLMNO', '10/07/24'),
       ('urn:fdc:gov.uk:2022:UVWXY', '10/14/24'),
       ('urn:fdc:gov.uk:2022:PQRST', '10/09/24'),
       ('urn:fdc:gov.uk:2022:07lXHJeQwE0k5PZO7w_PQF425vT8T7e63MrvyPYNSoI', '10/07/24'),
       ('urn:fdc:gov.uk:2022:mwfvbb5GgiDh0acjz9EDDQ7zwskWZzUSnWfavL70f6s', '10/02/24'),
       ('urn:fdc:gov.uk:2022:mGHDySEVfCsvfvc6lVWf6Qt9Dv0ZxPQWKoEzcjnBlUo', '10/15/24'),
       ('urn:fdc:gov.uk:2022:n93slCXHsxJ9rU6-AFM0jFIctYQjYf0KN9YVuJT-cao', '10/15/24'),
       ('urn:fdc:gov.uk:2022:cgVX2oJWKHMwzm8Gzx25CSoVXixVS0rw32Sar4Om8vQ', '10/15/24'),
       ('urn:fdc:gov.uk:2022:A', '01/15/25'),
       ('urn:fdc:gov.uk:2022:B', '01/15/25'),
       ('urn:fdc:gov.uk:2022:C', '01/15/25'),
       ('urn:fdc:gov.uk:2022:D', '01/15/25'),
       ('urn:fdc:gov.uk:2022:E', '01/15/25'),
       ('urn:fdc:gov.uk:2022:F', '01/15/25'),
       ('urn:fdc:gov.uk:2022:G', '01/15/25'),
       ('urn:fdc:gov.uk:2022:H', '01/15/25'),
       ('urn:fdc:gov.uk:2022:I', '01/15/25'),
       ('urn:fdc:gov.uk:2022:J', '01/15/25'),
       ('urn:fdc:gov.uk:2022:K', '01/15/25'),
       ('urn:fdc:gov.uk:2022:L', '01/15/25'),
       ('urn:fdc:gov.uk:2022:M', '01/15/25'),
       ('urn:fdc:gov.uk:2022:N', '01/15/25'),
       ('urn:fdc:gov.uk:2022:O', '01/15/25'),
       ('urn:fdc:gov.uk:2022:P', '01/15/25'),
       ('urn:fdc:gov.uk:2022:Q', '01/15/25'),
       ('urn:fdc:gov.uk:2022:R', '01/15/25'),
       ('urn:fdc:gov.uk:2022:S', '01/15/25'),
       ('urn:fdc:gov.uk:2022:T', '01/15/25'),
       ('urn:fdc:gov.uk:2022:U', '01/15/25'),
       ('urn:fdc:gov.uk:2022:V', '01/15/25'),
       ('urn:fdc:gov.uk:2022:W', '01/15/25'),
       ('urn:fdc:gov.uk:2022:X', '01/15/25'),
       ('urn:fdc:gov.uk:2022:Y', '01/15/25'),
       ('urn:fdc:gov.uk:2022:Z', '01/15/25'),
       ('urn:fdc:gov.uk:2022:GzFopg--2AyE6XtssVWwQTPELVQFupHJOjpONWS2uz0', '05/01/25'),
       ('ia-mock-user-12345', '10/14/24'),
       ('urn:fdc:gov.uk:2022:ErdvdxjqbulqrJI9hDob1vE0BQ_BqVXlv-mWZwgBJgA', '07/01/26'), -- danielle.dias@madetech.com
       ('urn:fdc:gov.uk:2022:qw2_iN4-Be1BkbYb8y-KyMuPfG7F49W_1fsa_V6iX9w', '07/02/26'), -- benjamin.johnson@madetech.com
       ('urn:fdc:gov.uk:2022:ORG01', '07/23/26'),
       ('urn:fdc:gov.uk:2022:ORG02', '07/23/26'), -- Local org second admin (Beth Admin)
       ('urn:fdc:gov.uk:2022:ORG03', '07/23/26'), -- Local org editor (Carl Editor)
       ('urn:fdc:gov.uk:2022:OJhyoHBpqAWPIqCCe_n9eVA4HGvFfgXCQMHSAsKSiRw', '07/23/26'), -- Team-PRSDB+orglandlord@softwire.com
       ('urn:fdc:gov.uk:2022:i9iTdfNB4Vh3FCmIE7NZ7QKePVzq5L36yVGFdmwEbdk', '07/23/26'), -- Team-PRSDB+orglandlordeditor@softwire.com
       ('urn:fdc:gov.uk:2022:HWihy8O1bH7nvqzL8zTP1RYQrPU3CxK6g6vYQvZ6tm4', '08/25/26'); -- Katrina.DiMuro@communities.gov.uk


-- TODO PDJB-607: Replace One Login subject identifiers with Internal Access equivalents
INSERT INTO local_council_user (subject_identifier, is_manager, local_council_id, created_date, last_modified_date,
                                  name, email, has_accepted_privacy_notice)
VALUES ('urn:fdc:gov.uk:2022:KLMNO', true, 1, '10/07/24', '10/07/24', 'Ford Prefect', 'Ford.Prefect@test.com', true),
       ('ia-mock-user-12345', true, 1, '10/14/24', '10/14/24', 'Mock User IA', 'test@example.com', true),
       ('urn:fdc:gov.uk:2022:PQRST', false, 1, '10/09/24', '10/09/24', 'Arthur Dent', 'Arthur.Dent@test.com', true),
       ('urn:fdc:gov.uk:2022:07lXHJeQwE0k5PZO7w_PQF425vT8T7e63MrvyPYNSoI', true, 1, '10/09/24', '10/09/24',
        'Jasmin Conterio',
        'jasmin.conterio@softwire.com', true),
       ('urn:fdc:gov.uk:2022:mwfvbb5GgiDh0acjz9EDDQ7zwskWZzUSnWfavL70f6s', true, 1, '10/02/24', '10/02/24',
        'Isobel Ibironke',
        'isobel.ibironke@softwire.com', true),
       ('urn:fdc:gov.uk:2022:n93slCXHsxJ9rU6-AFM0jFIctYQjYf0KN9YVuJT-cao', true, 1, '10/15/24', '10/15/24',
        'PRSDB LA Admin',
        'Team-PRSDB+laadmin@softwire.com', true),
       ('urn:fdc:gov.uk:2022:cgVX2oJWKHMwzm8Gzx25CSoVXixVS0rw32Sar4Om8vQ', false, 1, '10/15/24', '10/15/24',
        'PRSDB La User',
        'Team-PRSDB+lauser@softwire.com', true),
       ('urn:fdc:gov.uk:2022:UVWXY', true, 1, '10/14/24', '10/14/24', 'Mock User', 'test@example.com', true),
       ('urn:fdc:gov.uk:2022:ErdvdxjqbulqrJI9hDob1vE0BQ_BqVXlv-mWZwgBJgA', true, 1, '07/01/26', '07/01/26', 'Danielle Dias', 'danielle.dias@madetech.com', true),
       ('urn:fdc:gov.uk:2022:qw2_iN4-Be1BkbYb8y-KyMuPfG7F49W_1fsa_V6iX9w', true, 1, '07/02/26', '07/02/26', 'Ben Johnson', 'benjamin.johnson@madetech.com', true),
       ('urn:fdc:gov.uk:2022:HWihy8O1bH7nvqzL8zTP1RYQrPU3CxK6g6vYQvZ6tm4', true, 1, '08/25/26', '08/25/26', 'Katrina DiMuro', 'Katrina.DiMuro@communities.gov.uk', true);

SELECT setval(pg_get_serial_sequence('local_council_user', 'id'), (SELECT MAX(id) FROM local_council_user));

INSERT INTO local_council_invitation (invited_email, inviting_council_id, token, invited_as_admin, created_date)
VALUES ('expired.invitation+a@example.com', 1, '1234abcd-5678-abcd-1234-567abcd1111a', false, '05/05/2025'),
       ('expired.invitation+b@example.com', 1, '1234abcd-5678-abcd-1234-567abcd1111b', false, '05/05/2025'),
       ('expired.invitation+c@example.com', 1, '1234abcd-5678-abcd-1234-567abcd1111c', false, '05/05/2025'),
       ('expired.invitation+d@example.com', 1, '1234abcd-5678-abcd-1234-567abcd1111d', false, '05/05/2025');

SELECT setval(pg_get_serial_sequence('local_council_invitation', 'id'), (SELECT MAX(id) FROM local_council_invitation));

INSERT INTO registration_number (id, created_date, number, type)
VALUES (1, '09/13/24', 2001001001, 1),
       (2, '09/13/24', 3002001002, 1),
       (3, '10/07/24', 4003001003, 1),
       (4, '10/14/24', 5004001004, 1),
       (5, '10/09/24', 6005001005, 1),
       (6, '12/10/24', 7006001006, 0),
       (7, '12/19/24', 8005001005, 1),
       (8, '01/15/25', 1001001001, 1),
       (9, '01/15/25', 1001001002, 1),
       (10, '01/15/25', 1001001003, 1),
       (11, '01/15/25', 1001001004, 1),
       (12, '01/15/25', 1001001005, 1),
       (13, '01/15/25', 1001001006, 1),
       (14, '01/15/25', 1001001007, 1),
       (15, '01/15/25', 1001001008, 1),
       (16, '01/15/25', 1001001009, 1),
       (17, '01/15/25', 1001001010, 1),
       (18, '01/15/25', 1001001011, 1),
       (19, '01/15/25', 1001001012, 1),
       (20, '01/15/25', 1001001013, 1),
       (21, '01/15/25', 1001001014, 1),
       (22, '01/15/25', 1001001015, 1),
       (23, '01/15/25', 1001001016, 1),
       (24, '01/15/25', 1001001017, 1),
       (25, '01/15/25', 1001001018, 1),
       (26, '01/15/25', 1001001019, 1),
       (27, '01/15/25', 1001001020, 1),
       (28, '01/15/25', 1001001021, 1),
       (29, '01/15/25', 1001001022, 1),
       (30, '01/15/25', 1001001023, 1),
       (31, '01/15/25', 1001001024, 1),
       (32, '01/15/25', 1001001025, 1),
       (33, '01/15/25', 100100106, 1),
       (34, '12/10/24', 0006001002, 0),
       (35, '12/10/24', 0006001003, 0),
       (36, '12/10/24', 0006001004, 0),
       (37, '12/10/24', 0006001005, 0),
       (38, '12/10/24', 0006001006, 0),
       (39, '02/02/25', 0006001007, 0),
       (40, '12/10/24', 0006001008, 0),
       (41, '12/10/24', 0006001009, 0),
       (42, '12/10/24', 0006001010, 0),
       (43, '12/10/24', 0006001011, 0),
       (44, '12/10/24', 0006001012, 0),
       (45, '12/10/24', 0006001013, 0),
       (46, '12/10/24', 0006001014, 0),
       (47, '12/10/24', 0006001015, 0),
       (48, '12/10/24', 0006001016, 0),
       (49, '12/10/24', 0006001017, 0),
       (50, '12/10/24', 0006001018, 0),
       (51, '12/10/24', 0006001019, 0),
       (52, '12/10/24', 0006001020, 0),
       (53, '12/10/24', 0006001021, 0),
       (54, '12/10/24', 0006001022, 0),
       (55, '12/10/24', 0006001023, 0),
       (56, '12/10/24', 0006001024, 0),
       (57, '12/10/24', 0006001025, 0),
       (58, '12/10/24', 0006001026, 0),
       (59, '12/10/24', 0006001027, 0),
       (60, '12/10/24', 0006001028, 0),
       (61, '12/10/24', 0006001029, 0),
       (62, '12/10/24', 0006001030, 0),
       (63, '12/10/24', 0006001031, 0),
       (64, '12/10/24', 0006001032, 0),
       (65, '2025-01-15 00:00:00+00', 83811499802, 0),
       (66, '2025-01-15 00:00:00+00', 40666195053, 0),
       (67, '2025-01-15 00:00:00+00', 150242309330, 0),
       (68, '2025-01-15', 1502423330, 0),
       (69, '2026-02-27', 1502423331, 0),
       (70, '01/15/25', 1502423332, 0),
       (71, '07/01/26', 1502423333, 1),
       (72, '07/02/26', 1502423334, 1),
       (73, '01/15/25', 1502423335, 0),
       (74, '01/15/25', 1502423336, 0),
       (75, '01/15/25', 1502423337, 0),
       (76, '01/15/25', 1502423338, 0),
       (77, '01/15/25', 1502423339, 0),
       (78, '01/15/25', 1502423340, 0),
       (79, '01/15/25', 1502423341, 0),
       (80, '01/15/25', 1502423342, 0),
       (81, '07/23/26', 1502423343, 1),
       (82, '07/23/26', 1502423344, 0),
       (83, '07/23/26', 1502423345, 0),
       (84, '08/19/26', 1502423346, 0),
       (85, '08/25/26', 1502423347, 1),
       (86, '01/15/25', 1502423348, 0),
       (87, '01/15/25', 1502423351, 0),
       (88, '01/15/25', 1502423352, 0),
       (89, '01/15/25', 1502423353, 0),
       (900, '07/23/26', 1502423349, 1), -- Test org landlord (Team-PRSDB+orglandlord)
       (901, '07/23/26', 1502423350, 1); -- Test editor org landlord (Team-PRSDB+orglandlordeditor)

SELECT setval(pg_get_serial_sequence('registration_number', 'id'), (SELECT MAX(id) FROM registration_number));

INSERT INTO address (id, created_date, last_modified_date, uprn, single_line_address, local_council_id, postcode, building_number, street_name)
VALUES (1, '09/13/24', '09/13/24', 1, '1 Fictional Road, FA1 1AA', 1, 'FA1 1AA', '1', 'Fictional Road'),
       (2, '09/13/24', '09/13/24', 2, '2 Fake Way', 1, 'FA1 1AB', '2', 'Fake Way'),
       (3, '09/13/24', '09/13/24', 3, '3 Imaginary Street', 1, 'FA1 1AC', '3', 'Imaginary Street'),
       (4, '09/13/24', '09/13/24', 4, '4 Pretend Crescent', 1, 'FA1 1AD', '4', 'Pretend Crescent'),
       (5, '09/13/24', '09/13/24', 5, '5 Mythical Place', 1, 'FA1 1AE', '5', 'Mythical Place'),
       (6, '12/10/2024', '12/10/2024', 1123456, '1, Example Road, EG1 1AA', 1, 'EG1 1AA', '1', 'Example Road'),
       (7, '09/13/24', '09/13/24', 6, '2 Fictional Road', 1, 'FA1 1AF', '2', 'Fictional Road'),
       (8, '09/13/24', '09/13/24', 7, '3 Fake Way', 2, 'FA1 1AG', '3', 'Fake Way'),
       (9, '09/13/24', '09/13/24', 100090154790, '4 Imaginary Street', 1, 'FA1 1AH', '4', 'Imaginary Street'),
       (10, '09/13/24', '09/13/24', 9, '5 Pretend Crescent', 1, 'FA1 1AJ', '5', 'Pretend Crescent'),
       (11, '09/13/24', '09/13/24', 10, '6 Mythical Place', 1, 'FA1 1AK', '6', 'Mythical Place'),
       (12, '09/13/24', '09/13/24', null, '6 Mythical Place', 1, 'FA1 1AL', '6', 'Mythical Place'),
       (13, '05/02/25', '05/02/25', 1013, '1 PRSDB Square, EG1 2AA', 1, 'EG1 2AA', '1', 'PRSDB Square'),
       (14, '05/02/25', '05/02/25', 1014, '2 PRSDB Square, EG1 2AA', 1, 'EG1 2AA', '2', 'PRSDB Square'),
       (15, '05/02/25', '05/02/25', 1015, '3 PRSDB Square, EG1 2AA', 1, 'EG1 2AA', '3', 'PRSDB Square'),
       (16, '05/02/25', '05/02/25', 1016, '4 PRSDB Square, EG1 2AA', 1, 'EG1 2AA', '4', 'PRSDB Square'),
       (17, '05/02/25', '05/02/25', 1017, '5 PRSDB Square, EG1 2AA', 1, 'EG1 2AA', '5', 'PRSDB Square'),
       (18, '05/02/25', '05/02/25', 1018, '1 PRSDB Square, EG1 2AF', 1, 'EG1 2AF', '1', 'PRSDB Square'),
       (19, '05/02/25', '05/02/25', 1019, '1 PRSDB Square, EG1 2AG', 1, 'EG1 2AG', '1', 'PRSDB Square'),
       (20, '05/02/25', '05/02/25', 1020, '1 PRSDB Square, EG1 2AH', 1, 'EG1 2AH', '1', 'PRSDB Square'),
       (21, '05/02/25', '05/02/25', 1021, '9 PRSDB Square, EG1 2AI', 1, 'EG1 2AI', '9', 'PRSDB Square'),
       (22, '05/02/25', '05/02/25', 1022, '10 PRSDB Square, EG1 2AJ', 1, 'EG1 2AJ', '10', 'PRSDB Square'),
       (23, '05/02/25', '05/02/25', 1023, '11 PRSDB Square, EG1 2AK', 1, 'EG1 2AK', '11', 'PRSDB Square'),
       (24, '05/02/25', '05/02/25', 1024, '12 PRSDB Square, EG1 2AL', 1, 'EG1 2AL', '12', 'PRSDB Square'),
       (25, '05/02/25', '05/02/25', 1025, '13 PRSDB Square, EG1 2AM', 1, 'EG1 2AM', '13', 'PRSDB Square'),
       (26, '05/02/25', '05/02/25', 1026, '14 PRSDB Square, EG1 2AN', 1, 'EG1 2AN', '14', 'PRSDB Square'),
       (27, '05/02/25', '05/02/25', 1027, '15 PRSDB Square, EG1 2AO', 1, 'EG1 2AO', '15', 'PRSDB Square'),
       (28, '05/02/25', '05/02/25', 1028, '16 PRSDB Square, EG1 2AP', 1, 'EG1 2AP', '16', 'PRSDB Square'),
       (29, '05/02/25', '05/02/25', 1029, '17 PRSDB Square, EG1 2AQ', 1, 'EG1 2AQ', '17', 'PRSDB Square'),
       (30, '05/02/25', '05/02/25', 1030, '18 PRSDB Square, EG1 2AR', 1, 'EG1 2AR', '18', 'PRSDB Square'),
       (31, '05/02/25', '05/02/25', 1031, '19 PRSDB Square, EG1 2AS', 1, 'EG1 2AS', '19', 'PRSDB Square'),
       (32, '05/02/25', '05/02/25', 1032, '20 PRSDB Square, EG1 2AT', 1, 'EG1 2AT', '20', 'PRSDB Square'),
       (33, '05/02/25', '05/02/25', 1033, '21 PRSDB Square, EG1 2AU', 1, 'EG1 2AU', '21', 'PRSDB Square'),
       (34, '05/02/25', '05/02/25', 1034, '22 PRSDB Square, EG1 2AV', 1, 'EG1 2AV', '22', 'PRSDB Square'),
       (35, '05/02/25', '05/02/25', 1035, '23 PRSDB Square, EG1 2AW', 2, 'EG1 2AW', '23', 'PRSDB Square'),
       (36, '05/02/25', '05/02/25', 1036, '24 PRSDB Square, EG1 2AX', 2, 'EG1 2AX', '24', 'PRSDB Square'),
       (37, '05/02/25', '05/02/25', 1037, '25 PRSDB Square, EG1 2AY', 2, 'EG1 2AY', '25', 'PRSDB Square'),
       (38, '2025-01-15 00:00:00+00', null, 100090154792, '5, PROVIDENCE WAY, WATERBEACH, CAMBRIDGE, CB25 9QH', 20, 'CB25 9QH', '5', 'PROVIDENCE WAY'),
       (39, '2025-01-15 00:00:00+00', null, 100090154788, '1, PROVIDENCE WAY, WATERBEACH, CAMBRIDGE, CB25 9QH', 20, 'CB25 9QH', '1', 'PROVIDENCE WAY'),
       (40, '2025-01-15 00:00:00+00', null, null, '2, PROVIDENCE WAY, WATERBEACH, CAMBRIDGE, CB25 9QH', 20, 'CB25 9QH', '2', 'PROVIDENCE WAY'),
       (46, '09/13/24', '09/13/24', 1043, '7 Deregister Lane, DR1 1AA', 1, 'DR1 1AA', '7', 'Deregister Lane'),
       (47, '09/13/24', '09/13/24', 5001, '10 Skipped Fields Road, SK1 1AA', 1, 'SK1 1AA', '10', 'Skipped Fields Road'),
       (48, '09/13/24', '09/13/24', 5002, '11 Complete Fields Road, CO1 1AA', 1, 'CO1 1AA', '11', 'Complete Fields Road'),
       (49, '09/13/24', '09/13/24', 5003, '12 Tenancy Skipped Road, TS1 1AA', 1, 'TS1 1AA', '12', 'Tenancy Skipped Road'),
       (50, '09/13/24', '09/13/24', 5004, '13 Licensing Skipped Road, LS1 1AA', 1, 'LS1 1AA', '13', 'Licensing Skipped Road'),
       (51, '09/13/24', '09/13/24', 5005, '14 Both Skipped Road, BS1 1AA', 1, 'BS1 1AA', '14', 'Both Skipped Road'),
       (52, '09/13/24', '09/13/24', 5006, '15 Gas Expired Road, GE1 1AA', 1, 'GE1 1AA', '15', 'Gas Expired Road'),
       (53, '09/13/24', '09/13/24', 5007, '16 Certs Expired Road, CE1 1AA', 1, 'CE1 1AA', '16', 'Certs Expired Road'),
       (54, '09/13/24', '09/13/24', 5008, '17 Gas Provide Later Road, GP1 1AA', 1, 'GP1 1AA', '17', 'Gas Provide Later Road');

INSERT INTO address (id, created_date, last_modified_date, uprn, single_line_address, local_council_id, postcode, building_name, street_name)
VALUES (41, '09/13/24', '09/13/24', 1038, 'Registered House, PRSDB Road, AA3 1AB ', 1, 'AA3 1AB ', 'Registered House', 'PRSDB Road'),
       (42, '09/13/24', '09/13/24', 1039, 'Stage House, PRSDB Road, AA3 1AB ', 1, 'AA3 1AB ', 'Stage House', 'PRSDB Road'),
       (43, '09/13/24', '09/13/24', 1040, 'Slate House, PRSDB Square, AA3 1AB ', 1, 'AA3 1AB ', 'Slate House', 'PRSDB Square'),
       (44, '09/13/24', '09/13/24', 1041, 'Grate House, PRSDB Road, AA3 1AB ', 1, 'AA3 1AB ', 'Grate House', 'PRSDB Road'),
       (45, '09/13/24', '09/13/24', 1042, 'Slate House, PRSDB Road, AA3 1AB ', 1, 'AA3 1AB ', 'Slate House', 'PRSDB Road'),
       (55, '07/23/26', '07/23/26', 5009, 'Org Landlord House, PRSDB Road, OL1 1AA', 1, 'OL1 1AA', 'Org Landlord House', 'PRSDB Road'),
       (56, '07/23/26', '07/23/26', 5010, 'Joint Org House, PRSDB Road, JO1 1AA', 1, 'JO1 1AA', 'Joint Org House', 'PRSDB Road'),
       (57, '08/19/26', '08/19/26', 5011, 'Letting Agent House, PRSDB Road, EG1 2AZ', 1, 'EG1 2AZ', 'Letting Agent House', 'PRSDB Road'),
       (58, '08/19/26', '08/19/26', 5012, 'Delegated At Registration House, PRSDB Road, EG1 2AZ', 1, 'EG1 2AZ', 'Delegated At Registration House', 'PRSDB Road'),
       (59, '01/15/25', '01/15/25', 5013, 'Succeeded Payment House, PRSDB Road, SP1 1AA', 1, 'SP1 1AA', 'Succeeded Payment House', 'PRSDB Road'),
       (60, '01/15/25', '01/15/25', 5015, 'Inactive House, PRSDB Road, IN1 1AA', 1, 'IN1 1AA', 'Inactive House', 'PRSDB Road');

SELECT setval(pg_get_serial_sequence('address', 'id'), (SELECT MAX(id) FROM address));


INSERT INTO landlord (id, created_date, last_modified_date, registration_number_id, individual_address_id, individual_date_of_birth,
                      is_active, individual_phone_number, individual_subject_identifier, individual_name, individual_email, individual_country_of_residence, individual_is_verified, individual_has_accepted_privacy_notice)
VALUES (1, '09/13/24', '09/13/24', 1, 1, '09/13/2000', true, 07111111111, 'urn:fdc:gov.uk:2022:UVWXY',
        'Alexander Smith', 'alex.surname@example.com', 'England or Wales', true, true),
       (2, '09/13/24', '09/13/24', 2, 2, '08/13/2001', true, 07111111111, 'urn:fdc:gov.uk:2022:ABCDE',
        'Alexandra Davies', 'alexandra.q.davies@example.com', 'England or Wales', true, true),
       (3, '09/13/24', '09/13/24', 3, 3, '07/13/1997', true, 07111111111, 'urn:fdc:gov.uk:2022:PQRST',
        'Evan Alexandrescu', 'unrelatedemail@test.com', 'England or Wales', true, true),
       (4, '09/13/24', '09/13/24', 4, 4, '06/13/1989', true, 07111111111,
        'urn:fdc:gov.uk:2022:07lXHJeQwE0k5PZO7w_PQF425vT8T7e63MrvyPYNSoI', 'Tobias Evans', 'tobyevans@example.com', 'England or Wales', true, true),
       (5, '09/13/24', '09/13/24', 5, 5, '05/13/1950', true, 07111111111,
        'urn:fdc:gov.uk:2022:mwfvbb5GgiDh0acjz9EDDQ7zwskWZzUSnWfavL70f6s', 'Margaret Mary Smith',
        'mm.smith@example.com', 'England or Wales', true, true),
       (6, '12/19/24', '12/19/24', 7, 5, '06/13/1989', true, 07111111111,
        'urn:fdc:gov.uk:2022:mGHDySEVfCsvfvc6lVWf6Qt9Dv0ZxPQWKoEzcjnBlUo', 'PRSDB Landlord',
        'Team-PRSDB+landlord@softwire.com', 'England or Wales', true, true),
       (7, '01/15/25', '01/15/25', 6, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:FGHIJ', 'PRSDB',
        'test@example.com', 'England or Wales', true, true),
       (8, '01/15/25', '01/15/25', 8, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:A', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (9, '01/15/25', '01/15/25', 9, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:B', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (10, '01/15/25', '01/15/25', 10, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:C', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (11, '01/15/25', '01/15/25', 11, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:D', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (12, '01/15/25', '01/15/25', 12, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:E', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (13, '01/15/25', '01/15/25', 13, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:F', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (14, '01/15/25', '01/15/25', 14, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:G', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (15, '01/15/25', '01/15/25', 15, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:H', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (16, '01/15/25', '01/15/25', 16, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:I', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (17, '01/15/25', '01/15/25', 17, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:J', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (18, '01/15/25', '01/15/25', 18, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:K', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (19, '01/15/25', '01/15/25', 19, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:L', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (20, '01/15/25', '01/15/25', 20, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:M', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (21, '01/15/25', '01/15/25', 21, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:N', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (22, '01/15/25', '01/15/25', 22, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:O', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (23, '01/15/25', '01/15/25', 23, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:P', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (24, '01/15/25', '01/15/25', 24, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:Q', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (25, '01/15/25', '01/15/25', 25, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:R', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (26, '01/15/25', '01/15/25', 26, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:S', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (27, '01/15/25', '01/15/25', 27, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:T', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (28, '01/15/25', '01/15/25', 28, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:U', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (29, '01/15/25', '01/15/25', 29, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:V', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (30, '01/15/25', '01/15/25', 30, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:W', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (31, '01/15/25', '01/15/25', 31, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:X', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (32, '01/15/25', '01/15/25', 32, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:Y', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (33, '01/15/25', '01/15/25', 33, 5, '06/13/1989', true, 0, 'urn:fdc:gov.uk:2022:Z', 'PRSDB', 'test@example.com', 'England or Wales', true, true),
       (34, '07/01/26', '07/01/26', 71, 5, '01/01/1990', true, 07777777777, 'urn:fdc:gov.uk:2022:ErdvdxjqbulqrJI9hDob1vE0BQ_BqVXlv-mWZwgBJgA', 'Danielle Dias', 'danielle.dias@madetech.com', 'England or Wales', true, true),
       (35, '07/02/26', '07/02/26', 72, 5, '01/01/1990', true, 07777777777, 'urn:fdc:gov.uk:2022:qw2_iN4-Be1BkbYb8y-KyMuPfG7F49W_1fsa_V6iX9w', 'Ben Johnson', 'benjamin.johnson@madetech.com', 'England or Wales', true, true),
       (37, '08/25/26', '08/25/26', 85, 5, '01/01/1990', true, 07777777777, 'urn:fdc:gov.uk:2022:HWihy8O1bH7nvqzL8zTP1RYQrPU3CxK6g6vYQvZ6tm4', 'Katrina DiMuro', 'Katrina.DiMuro@communities.gov.uk', 'England or Wales', true, true);

-- TODO: PDJB-1295: Add some more
INSERT INTO landlord (id, created_date, last_modified_date, registration_number_id, landlord_type,
                      organisation_landlord_name, organisation_address_id, organisation_email,
                      organisation_phone_number, organisation_registrant_name,
                      organisation_registrant_date_of_birth, organisation_registrant_email,
                      organisation_registrant_phone_number, organisation_is_company,
                      organisation_is_charity, organisation_is_trust, organisation_company_number,
                      organisation_main_contact_name, organisation_main_contact_email,
                      organisation_main_contact_phone)
VALUES (36, '07/23/26', '07/23/26', 81, 1, 'Local Organisation Landlord', 5,
        'local-org-landlord@example.com', '07111111111', 'Local Registrant', '01/01/1990',
        'local-registrant@example.com', '07111111112', true, false, false, '12345678',
        'Local Main Contact', 'local-main-contact@example.com', '07111111113'),
       (100, '07/23/26', '07/23/26', 900, 1, 'Test Organisation Landlord', 5,
        'Team-PRSDB+orglandlord@softwire.com', '07777777777', 'Test Registrant', '01/01/1980',
        'Team-PRSDB+orglandlord@softwire.com', '07777777778', true, false, false, '87654321',
        'Main Contact Name', 'main.contact@example.com', '07777777780'),
       (101, '07/23/26', '07/23/26', 901, 1, 'Test Editor Organisation Landlord', 5,
        'Team-PRSDB+orglandlordeditor@softwire.com', '07777777781', 'Test Editor', '01/01/1985',
        'Team-PRSDB+orglandlordeditor@softwire.com', '07777777782', true, false, false, '87654322',
        'Editor Main Contact', 'editor.main.contact@example.com', '07777777783');

INSERT INTO organisational_landlord_user (organisation_landlord_id, subject_identifier, name, email, role, created_date)
VALUES (36, 'urn:fdc:gov.uk:2022:ORG01', 'Local Registrant', 'local-registrant@example.com', 0, '07/23/26'),
       (36, 'urn:fdc:gov.uk:2022:ORG02', 'Beth Admin', 'beth.admin@example.com', 0, '07/23/26'),
       (36, 'urn:fdc:gov.uk:2022:ORG03', 'Carl Editor', 'carl.editor@example.com', 1, '07/23/26'),
       (100, 'urn:fdc:gov.uk:2022:OJhyoHBpqAWPIqCCe_n9eVA4HGvFfgXCQMHSAsKSiRw', 'Test Registrant', 'Team-PRSDB+orglandlord@softwire.com', 0, '07/23/26'),
       (101, 'urn:fdc:gov.uk:2022:i9iTdfNB4Vh3FCmIE7NZ7QKePVzq5L36yVGFdmwEbdk', 'Test Editor', 'Team-PRSDB+orglandlordeditor@softwire.com', 1, '07/23/26');

INSERT INTO organisational_landlord_invitation (created_date, last_modified_date, token, invited_email, organisation_landlord_id,
                                               role, invitation_expired_email_sent, is_hidden)
VALUES (current_date, current_date, '1234abcd-5678-abcd-1234-567abcd2222a', 'pending-org-invite-a@example.com', 36,
        0, false, false),
       (current_date - 10, current_date - 10, '1234abcd-5678-abcd-1234-567abcd2222b', 'pending-org-invite-b@example.com', 36,
        1, false, false),
       (current_date - 20, current_date - 20, '1234abcd-5678-abcd-1234-567abcd2222c', 'pending-org-invite-c@example.com', 36,
        0, false, false),
       (current_date - 60, current_date - 60, '1234abcd-5678-abcd-1234-567abcd2222d', 'expired-org-invite-d@example.com', 36,
        0, false, false),
        (current_date - 5, current_date - 5, '1234abcd-5678-abcd-1234-567abcd2222e', 'hidden-org-invite-e@example.com', 36,
            0, false, true);

SELECT setval(pg_get_serial_sequence('organisational_landlord_invitation', 'id'),
              (SELECT MAX(id) FROM organisational_landlord_invitation));

SELECT setval(pg_get_serial_sequence('landlord', 'id'), (SELECT MAX(id) FROM landlord));

INSERT INTO license (id, license_type, license_number)
VALUES (1, 1, 'L12345678'),
       (2, 2, 'L12345678'),
       (3, 3, 'L12345678'),
       (4, 1, 'L12345678'),
       (5, 2, 'L12345678'),
       (6, 3, 'L12345678'),
       (7, 0, 'L12345678'),
       (8, 0, 'L12345678'),
       (9, 2, 'L99999999'),
       (10, 1, 'L10101010');

SELECT setval(pg_get_serial_sequence('license', 'id'), (SELECT MAX(id) FROM license));

INSERT INTO property_ownership (id, is_active, ownership_type_before_pdjb955, ownership_type, current_num_households, current_num_tenants, registration_number_id, address_id, created_date, last_modified_date, license_id, property_build_type,
                                num_bedrooms, bills_included_list, custom_bills_included, furnished_status, rent_frequency, custom_rent_frequency, rent_amount, custom_property_type, marked_joint_landlord, is_occupied, correspondence_email, correspondence_address_id, renewal_date)
VALUES (1, true, 1, 1, 1, 2, 6, 6, '01/15/25', '02/02/25', null, 1, 1, null, null, 2, 1, null, 123.12, null, false, true, 'email@example.com', 1, current_date + 365),
       (2, false, 1, 1, 1, 2, 34, 7, '01/15/25', '01/15/25', null, 1, 1, null, null, 2, 1, null, 123.12, null, false, true, 'email@example.com', 1, current_date + 365),
       (3, true, 1, 1, 1, 2, 35, 8, '01/15/25', '01/15/25', null, 1, 1, null, null, 2, 1, null, 123.12, null, false, true, 'email@example.com', 1, current_date + 365),
       (4, true, 1, 1, 1, 2, 36, 9, '01/15/25', '01/15/25', null, 1, 1, null, null, 2, 1, null, 123.12, null, false, true, 'email@example.com', 1, current_date + 365),
       (5, true, 1, 1, 1, 2, 37, 10, '01/15/25', '01/15/25', null, 1, 1, null, null, 2, 1, null, 123.12, null, false, true, 'email@example.com', 1, current_date + 365),
       (6, false, 1, 1, 1, 2, 38, 11, '01/15/25', '01/15/25', null, 1, 1, null, null, 2, 1, null, 123.12, null, false, true, 'email@example.com', 1, current_date + 365),
       (7, true, 1, 1, 0, 0, 39, 12, '02/02/25', '02/02/25', 1, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (8, true, 1, 1, 1, 1, 40, 13, '05/02/25', '01/15/25', null, 1, 1, null, null, 2, 1, null, 123.12, null, true, true, 'email@example.com', 1, current_date + 365),
       (9, true, 1, 1, 0, 0, 41, 14, '05/02/25', '01/15/25', null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (10, true, 1, 1, 1, 1, 42, 15, '05/02/25', '01/15/25', null, 1, 1, null, null, 2, 1, null, 123.12, null, false, true, 'email@example.com', 1, current_date + 365),
       (11, true, 1, 1, 0, 0, 43, 16, '05/02/25', '01/15/25', null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (12, true, 1, 1, 0, 0, 44, 17, '05/02/25', '01/15/25', null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (13, true, 1, 1, 0, 0, 45, 18, '05/02/25', '01/15/25', null, 1, null, null, null, null, null, null, null, null, true, false, 'email@example.com', 1, current_date + 365),
       (14, true, 1, 1, 0, 0, 46, 19, '05/02/25', '01/15/25', null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (15, true, 1, 1, 0, 0, 47, 20, '05/02/25', '01/15/25', null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (16, true, 1, 1, 0, 0, 48, 21, '05/02/25', '01/15/25', null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (17, true, 1, 1, 0, 0, 49, 22, '05/02/25', '01/15/25', null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (18, true, 1, 1, 0, 0, 50, 23, '05/02/25', '01/15/25', null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (19, true, 1, 1, 0, 0, 51, 24, '05/02/25', '01/15/25', null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (20, true, 1, 1, 0, 0, 52, 25, '05/02/25', '01/15/25', null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (21, true, 1, 1, 0, 0, 53, 26, '05/02/25', '01/15/25', null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (22, true, 1, 1, 0, 0, 54, 27, '05/02/25', '01/15/25', null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (23, true, 1, 1, 0, 0, 55, 28, '05/02/25', '01/15/25', null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (24, true, 1, 1, 0, 0, 56, 29, '05/02/25', '01/15/25', null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (25, true, 1, 1, 0, 0, 57, 30, '05/02/25', '01/15/25', null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (26, true, 1, 1, 0, 0, 58, 31, '05/02/25', '01/15/25', 2, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (27, true, 1, 1, 0, 0, 59, 32, '05/02/25', '01/15/25', 3, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (28, true, 1, 1, 0, 0, 60, 33, '05/02/25', '01/15/25', 4, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (29, true, 1, 1, 0, 0, 61, 34, '05/02/25', '01/15/25', 5, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (30, true, 1, 1, 0, 0, 62, 35, '05/02/25', '01/15/25', 6, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (31, true, 1, 1, 0, 0, 63, 36, '05/02/25', '01/15/25', 7, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (32, true, 1, 1, 0, 0, 64, 37, '05/02/25', '01/15/25', 8, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (33, true, 0, 2, 0, 0, 65, 38, '2025-01-15 00:00:00+00', null, null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (34, true, 0, 2, 0, 0, 66, 39, '2025-01-15 00:00:00+00', null, null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (35, true, 0, 2, 0, 0, 67, 40, '2025-01-15 00:00:00+00', null, null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (36, true, 0, 2, 0, 0, 68, 41, '2025-01-15', '01/15/25', null, 1, null, null, null, null, null, null, null, null, false, false, 'email@example.com', 1, current_date + 365),
       (37, true, 0, 2, 0, 0, 69, 42, '2026-02-27', '02/27/26', null, 4, null, null, null, null, null, null, null, 'End terrace', false, false, 'email@example.com', 1, current_date + 365),
       (38, true, 1, 1, 1, 1, 70, 46, '07/15/25', '07/15/25', null, 1, 1, null, null, 2, 1, null, 123.12, null, true, true, 'email@example.com', 1, current_date + 365);

INSERT INTO property_ownership (id, is_active, ownership_type_before_pdjb955, ownership_type, current_num_households, current_num_tenants, registration_number_id, address_id, created_date, last_modified_date, license_id, property_build_type,
                                num_bedrooms, bills_included_list, custom_bills_included, furnished_status, rent_frequency, custom_rent_frequency, rent_amount, custom_property_type, marked_joint_landlord, is_occupied, last_occupied_date, license_provide_later, tenancy_provide_later, correspondence_email, correspondence_address_id, renewal_date)
VALUES (39, true, 1, 1, 0, 0, 79, 47, current_date - INTERVAL '7 days', current_date - INTERVAL '7 days', null, 1, 1, null, null, null, null, null, null, null, false, true, current_date - INTERVAL '7 days', true, true, 'email@example.com', 1, current_date + 365),
       (40, true, 1, 1, 1, 2, 80, 48, '05/02/25', '05/02/25', 9, 1, 1, null, null, 2, 1, null, 123.12, null, false, true, current_date - INTERVAL '7 days', null, null, 'email@example.com', 1, current_date + 365),
       (41, true, 1, 1, 0, 0, 73, 49, '05/02/25', '05/02/25', 10, 1, 1, null, null, null, null, null, null, null, false, true, current_date - INTERVAL '7 days', null, true, 'email@example.com', 1, current_date + 365),
       (42, true, 1, 1, 1, 2, 74, 50, '05/02/25', '05/02/25', null, 1, 1, null, null, 2, 1, null, 123.12, null, false, true, current_date - INTERVAL '7 days', true, null, 'email@example.com', 1, current_date + 365),
       (43, true, 1, 1, 0, 0, 75, 51, '05/02/25', '05/02/25', null, 1, 1, null, null, null, null, null, null, null, false, true, current_date - INTERVAL '7 days', true, true, 'email@example.com', 1, current_date + 365),
       (44, true, 1, 1, 1, 2, 76, 52, '05/02/25', '05/02/25', null, 1, 1, null, null, 2, 1, null, 123.12, null, false, true, current_date - INTERVAL '7 days', false, false, 'email@example.com', 1, current_date + 365),
       (45, true, 1, 1, 1, 2, 77, 53, '05/02/25', '05/02/25', null, 1, 1, null, null, 2, 1, null, 123.12, null, false, true, current_date - INTERVAL '7 days', false, false, 'email@example.com', 1, current_date + 365),
       (46, true, 1, 1, 1, 2, 78, 54, '05/02/25', '05/02/25', null, 1, 1, null, null, 2, 1, null, 123.12, null, false, true, current_date - INTERVAL '7 days', false, false, 'email@example.com', 1, current_date + 365),
       (47, true, 1, 1, 0, 0, 82, 55, '07/23/26', '07/23/26', null, 1, null, null, null, null, null, null, null, null, false, false, null, false, false, 'email@example.com', 1, current_date + 365),
       (48, true, 1, 1, 0, 0, 83, 56, '07/23/26', '07/23/26', null, 1, null, null, null, null, null, null, null, null, true, false, null, false, false, 'email@example.com', 1, current_date + 365),
       (49, true, 1, 1, 1, 2, 84, 57, '08/19/26', '08/19/26', null, 1, 1, null, null, 2, 1, null, 123.12, null, false, true, null, false, false, 'email@example.com', 1, current_date + 365),
       (50, true, 1, 1, 0, 0, 86, 58, current_date - INTERVAL '7 days', current_date - INTERVAL '7 days', null, 1, 1, null, null, null, null, null, null, null, false, true, current_date - INTERVAL '7 days', true, true, 'email@example.com', 1, current_date + 365),
       (51, true, 1, 1, 1, 2, 87, 59, '01/15/25', '01/15/25', null, 1, 1, null, null, 2, 1, null, 123.12, null, false, true, null, false, false, 'email@example.com', 1, current_date + 365),
       -- PDJB-844: inactive, occupied property with every detail marked "provide later", for the reactivation journey
       (52, false, 1, 1, 1, 2, 88, 60, '01/15/25', '01/15/25', null, 1, 1, null, null, 2, 1, null, 123.12, null, false, true, current_date - INTERVAL '7 days', true, true, 'email@example.com', 1, current_date + 365),
       -- PDJB-2046: inactive property whose address has since been registered again by another landlord (property 3)
       (53, false, 1, 1, 0, 0, 89, 8, '01/15/25', '01/15/25', null, 1, null, null, null, null, null, null, null, null, false, false, null, false, false, 'email@example.com', 1, current_date + 365);

SELECT setval(pg_get_serial_sequence('property_ownership', 'id'), (SELECT MAX(id) FROM property_ownership));

-- Provide-later flags (PDJB-990) for the pre-existing skipped-licensing properties. Property 1 is occupied, so
-- it also needs a last_occupied_date to render the provide-later deadline.
UPDATE property_ownership SET license_provide_later = true, last_occupied_date = current_date - INTERVAL '7 days' WHERE id = 1;
UPDATE property_ownership SET license_provide_later = true WHERE id = 9;

INSERT INTO ownership_link (landlord_id, landlordship_id, created_date)
VALUES (1, 1, '2025-01-15'),
       (2, 2, '2025-01-15'),
       (4, 3, '2025-01-15'),
       (1, 4, '2025-01-15'),
       (1, 5, '2025-01-15'),
       (1, 6, '2025-01-15'),
       (1, 7, '2025-01-15'),
       (1, 8, '2025-01-15'),
       (2, 8, '2025-01-15'),
       (4, 8, '2025-01-15'),
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
       (1, 21, '2025-01-15'),
       (1, 22, '2025-01-15'),
       (1, 23, '2025-01-15'),
       (1, 24, '2025-01-15'),
       (1, 25, '2025-01-15'),
       (1, 26, '2025-01-15'),
       (1, 27, '2025-01-15'),
       (1, 28, '2025-01-15'),
       (1, 29, '2025-01-15'),
       (1, 30, '2025-01-15'),
       (1, 31, '2025-01-15'),
       (1, 32, '2025-01-15'),
       (1, 33, '2025-01-15'),
       (1, 34, '2025-01-15'),
       (1, 35, '2025-01-15'),
       (1, 36, '2025-01-15'),
       (1, 37, '2025-01-15'),
       (1, 38, '2025-01-15'),
       (1, 39, '2025-01-15'),
       (1, 40, '2025-01-15'),
       (1, 41, '2025-01-15'),
       (1, 42, '2025-01-15'),
       (1, 43, '2025-01-15'),
       (1, 44, '2025-01-15'),
       (1, 45, '2025-01-15'),
       (1, 46, '2025-01-15'),
       (2, 4, '2025-01-15'),
       (36, 47, '2025-07-23'),
       (1, 48, '2025-07-23'),
       (36, 48, '2025-07-23'),
       (1, 49, '2026-08-19'),
       (1, 50, '2025-01-15'),
       (1, 51, '2025-01-15'),
       (1, 52, '2025-01-15'),
       (1, 53, '2025-01-15')
ON CONFLICT DO NOTHING;

UPDATE landlord l
SET has_registered_property = true
WHERE EXISTS (SELECT 1 FROM ownership_link ol WHERE ol.landlord_id = l.id);

INSERT INTO system_operator (id, created_date, last_modified_date, subject_identifier)
VALUES (1,'2025-02-19 12:01:07.575927+00',null,'urn:fdc:gov.uk:2022:UVWXY'),
       (2,'2025-05-01 12:01:07.575927+00',null,'urn:fdc:gov.uk:2022:GzFopg--2AyE6XtssVWwQTPELVQFupHJOjpONWS2uz0');

SELECT setval(pg_get_serial_sequence('system_operator', 'id'), (SELECT MAX(id) FROM system_operator));

-- TODO PDJB-1673: Add some EIC example data (electrical_cert_type = 1 with an eic_issue_date), here and in the integration test fixtures
INSERT INTO property_compliance (id, property_ownership_id, created_date, last_modified_date, gas_safety_cert_issue_date, has_gas_supply, eicr_expiry_date, electrical_cert_type, epc_url, epc_expiry_date, tenancy_started_before_epc_expiry, epc_energy_rating, epc_exemption_reason, epc_mees_exemption_reason, has_fire_safety_declaration, has_keep_property_safe_declaration, has_responsibility_to_tenants_declaration)
VALUES
       (1, 8, '01/01/25', '01/01/25', null, true, null, null, null, null, null, null, null, null, true, true, true),
       (2, 9, '01/01/25', '01/01/25', '1990-02-28', true, null, null, 'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2021-03-16', false, 'c', null, null, true, true, true),
       (3, 10, '01/01/25', '01/01/25', null, false, null, null, 'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-1050-2867', '2031-02-28', null, 'g', null, null, true, true, true),
       (4, 11, '01/01/25', '01/01/25', null, false, null, null, 'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-1050-2867', '2031-02-28', null, 'g', null, 0, true, true, true),
       (5, 12, '01/01/25', null, null, false, null, null, null, null, null, null, 1, null, true, true, true),
       (6, 33, '01/01/25', null, null, false, null, null, null, null, null, null, 1, null, true, true, true),
       (7, 2, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (8, 3, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (9, 5, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (10, 6, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (11, 7, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (12, 13, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (13, 14, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (14, 15, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (15, 16, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (16, 17, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (17, 18, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (18, 19, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (19, 20, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (20, 21, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (21, 22, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (22, 23, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (23, 24, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (24, 25, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (25, 26, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (26, 27, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (27, 28, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (28, 29, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (29, 30, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (30, 31, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (31, 32, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (32, 34, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (33, 35, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (34, 36, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (35, 37, '01/01/25', null, '2023-06-15', true, '2024-03-01', 0, 'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2030-09-15', null, 'c', null, null, true, true, true),
       (36, 1, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (37, 4, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (38, 38, '01/01/25', '01/01/25', null, true, null, null, null, null, null, null, null, null, true, true, true),
       -- Fully compliant records for properties 41-43 (gas not required, electrical + EPC valid and
       -- in date, all declarations true) so hasComplianceIssue is false and the pure provide-later
       -- banner variants (TENANCY / LICENSING / BOTH) render instead of COMBINED.
       (39, 41, '01/01/25', '01/01/25', null, false, '2035-01-01', 0, 'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2035-01-01', null, 'c', null, null, true, true, true),
       (40, 42, '01/01/25', '01/01/25', null, false, '2035-01-01', 0, 'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2035-01-01', null, 'c', null, null, true, true, true),
       (41, 43, '01/01/25', '01/01/25', null, false, '2035-01-01', 0, 'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2035-01-01', null, 'c', null, null, true, true, true),
       (49, 50, '01/01/25', '01/01/25', null, false, '2035-01-01', 0, 'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2035-01-01', null, 'c', null, null, true, true, true),
       -- Compliance records for the org-landlord properties (PO 47, 48) so their details pages render.
       (47, 47, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (48, 48, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true) ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('property_compliance', 'id'), (SELECT MAX(id) FROM property_compliance));

-- PDJB-1305 compliance-banner QA compliance records.
--   42  scenario A (PO 39): all three certs "provide later" -> COMBINED banner, backed by data.
--   43  PO 44: gas cert expired, electrical + EPC valid      -> single "certificate expired" banner.
--   44  PO 45: gas cert + EPC expired, electrical valid      -> "multiple certificates expired" banner.
--   45  PO 46: gas cert "provide later", electrical + EPC valid -> "add compliance certificates" (missing) banner.
--   46  PO 40 (11 Complete Fields Road): all certs valid & in date, no provide-later -> no banner.
--   51  PO 52 (Inactive House): all three certs "provide later", for the PDJB-844 reactivation journey.
INSERT INTO property_compliance (id, property_ownership_id, created_date, last_modified_date, gas_safety_cert_issue_date, has_gas_supply,
                                 eicr_expiry_date, electrical_cert_type, epc_url, epc_expiry_date,
                                 tenancy_started_before_epc_expiry, epc_energy_rating, epc_exemption_reason, epc_mees_exemption_reason,
                                 has_fire_safety_declaration, has_keep_property_safe_declaration, has_responsibility_to_tenants_declaration,
                                 gas_safety_cert_provide_later, electrical_safety_cert_provide_later, epc_provide_later)
VALUES (42, 39, '01/01/25', '01/01/25', null, true, null, null, null, null,
        null, null, null, null, true, true, true, true, true, true),
       (43, 44, '01/01/25', '01/01/25', current_date - 730, true, current_date + 730, 0,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', current_date + 730,
        null, 'c', null, null, true, true, true, false, false, false),
       (44, 45, '01/01/25', '01/01/25', current_date - 730, true, current_date + 730, 0,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', current_date - 365,
        false, 'c', null, null, true, true, true, false, false, false),
       (45, 46, '01/01/25', '01/01/25', null, true, current_date + 730, 0,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', current_date + 730,
        null, 'c', null, null, true, true, true, true, false, false),
       (46, 40, '01/01/25', '01/01/25', current_date - 30, true, current_date + 730, 0,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', current_date + 730,
        null, 'c', null, null, true, true, true, false, false, false),
       (52, 49, '08/19/26', '08/19/26', null, false, null, null, null, null,
        null, null, null, null, true, true, true, false, false, false),
       (50, 51, '01/01/25', '01/01/25', current_date - 30, true, current_date + 730, 0,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', current_date + 730,
        null, 'c', null, null, true, true, true, false, false, false),
       (51, 52, '01/15/25', '01/15/25', null, true, null, null, null, null,
        null, null, null, null, true, true, true, true, true, true) ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('property_compliance', 'id'), (SELECT MAX(id) FROM property_compliance));


INSERT INTO file_upload (id, created_date, status, object_key, e_tag, version_id, extension, file_name)
VALUES (1, '09/13/24', 0, 'file-key-123', 'e-tag-123', 'version-id-123', 'pdf', 'gas-safety-cert.pdf'),
       (2, '09/13/24', 1, 'file-key-456', 'e-tag-456', 'version-id-456', 'pdf', 'gas-safety-cert-2.pdf'),
       (3, '01/01/25', 0, 'gas-cert-po8', 'e-tag-gas-8', 'version-gas-8', 'pdf', 'gas-cert-po8.pdf'),
       (4, '01/01/25', 0, 'eicr-cert-po8', 'e-tag-eicr-8', 'version-eicr-8', 'pdf', 'eicr-cert.pdf');
SELECT setval(pg_get_serial_sequence('file_upload', 'id'), (SELECT MAX(id) FROM file_upload));

-- Link file uploads to property_compliance via plural join tables (simulates the registration flow)

INSERT INTO virus_scan_callback (id, created_date, file_upload_id, encoded_callback_data)
VALUES (1, '09/13/24', 1, '{"type":"uk.gov.communities.prsdb.webapp.services.EmailNotificationData.OwnerEmailNotification","propertyOwnershipId":1,"certificateType":"GasSafetyCert"}'),
       (2, '09/13/24', 2, '{"type":"uk.gov.communities.prsdb.webapp.services.EmailNotificationData.OwnerEmailNotification","propertyOwnershipId":1,"certificateType":"GasSafetyCert"}');
SELECT setval(pg_get_serial_sequence('virus_scan_callback', 'id'), (SELECT MAX(id) FROM virus_scan_callback));

INSERT INTO reminder_email_sent (id,last_reminder_email_sent_date)
VALUES (1, current_date-1),
       (2, current_date-1);
SELECT setval(pg_get_serial_sequence('reminder_email_sent', 'id'), (SELECT MAX(id) FROM reminder_email_sent));

INSERT INTO saved_journey_state (id, created_date, last_modified_date, journey_id, serialized_state, subject_identifier,reminder_email_sent_id)
VALUES (1, current_date-22,current_date-22, 'example-incomplete-journey1', '{"journeyData":{"lookup-address":{"houseNameOrNumber":"1","postcode":"WC2R1LA"},"select-address":{"address":"1,SAVOYCOURT,LONDON,WC2R0EX"},"property-type":{"customPropertyType":"","propertyType":"DETACHED_HOUSE"}},"cachedAddresses":"[{\"singleLineAddress\":\"1,SAVOYCOURT,LONDON,WC2R0EX\",\"localCouncilId\":1,\"uprn\":1038,\"buildingNumber\":\"1\",\"streetName\":\"SAVOYCOURT\",\"townName\":\"LONDON\",\"postcode\":\"WC2R0EX\"}]","isAddressAlreadyRegistered":"false"}', 'urn:fdc:gov.uk:2022:UVWXY', 1),
       (2, current_date-22,current_date-29, 'example-incomplete-journey2', '{"journeyData":{"lookup-address":{"houseNameOrNumber":"1","postcode":"WC2R1LA"},"select-address":{"address":"1,SAVOYCOURT,LONDON,WC2R0EX"},"property-type":{"customPropertyType":"","propertyType":"DETACHED_HOUSE"}},"cachedAddresses":"[{\"singleLineAddress\":\"1,SAVOYCOURT,LONDON,WC2R0EX\",\"localCouncilId\":1,\"uprn\":1038,\"buildingNumber\":\"1\",\"streetName\":\"SAVOYCOURT\",\"townName\":\"LONDON\",\"postcode\":\"WC2R0EX\"}]","isAddressAlreadyRegistered":"false"}', 'urn:fdc:gov.uk:2022:UVWXY', null),
       (3, current_date-9,current_date-9, 'example-incomplete-journey3', '{"journeyData":{"lookup-address":{"houseNameOrNumber":"1","postcode":"WC2R1LA"},"select-address":{"address":"1,SAVOYCOURT,LONDON,WC2R0EX"},"property-type":{"customPropertyType":"","propertyType":"DETACHED_HOUSE"}},"cachedAddresses":"[{\"singleLineAddress\":\"1,SAVOYCOURT,LONDON,WC2R0EX\",\"localCouncilId\":1,\"uprn\":1038,\"buildingNumber\":\"1\",\"streetName\":\"SAVOYCOURT\",\"townName\":\"LONDON\",\"postcode\":\"WC2R0EX\"}]","isAddressAlreadyRegistered":"false"}', 'urn:fdc:gov.uk:2022:UVWXY', null),
       (4, current_date-29,current_date-29, 'example-incomplete-journey4', '{"journeyData":{"lookup-address":{"houseNameOrNumber":"1","postcode":"WC2R1LA"},"select-address":{"address":"1,SAVOYCOURT,LONDON,WC2R0EX"},"property-type":{"customPropertyType":"","propertyType":"DETACHED_HOUSE"}},"cachedAddresses":"[{\"singleLineAddress\":\"1,SAVOYCOURT,LONDON,WC2R0EX\",\"localCouncilId\":1,\"uprn\":1038,\"buildingNumber\":\"1\",\"streetName\":\"SAVOYCOURT\",\"townName\":\"LONDON\",\"postcode\":\"WC2R0EX\"}]","isAddressAlreadyRegistered":"false"}', 'urn:fdc:gov.uk:2022:UVWXY', 2),
       (5, current_date-23,current_date-23, 'example-incomplete-journey5', '{"journeyData":{"lookup-address":{"houseNameOrNumber":"1","postcode":"WC2R1LA"},"select-address":{"address":"1,SAVOYCOURT,LONDON,WC2R0EX"},"property-type":{"customPropertyType":"","propertyType":"DETACHED_HOUSE"}},"cachedAddresses":"[{\"singleLineAddress\":\"1,SAVOYCOURT,LONDON,WC2R0EX\",\"localCouncilId\":1,\"uprn\":1038,\"buildingNumber\":\"1\",\"streetName\":\"SAVOYCOURT\",\"townName\":\"LONDON\",\"postcode\":\"WC2R0EX\"}]","isAddressAlreadyRegistered":"false"}', 'urn:fdc:gov.uk:2022:UVWXY', null),
       (6, current_date, current_date, 'example-incomplete-journey-with-payments', '{"journeyData":{"lookup-address":{"houseNameOrNumber":"1","postcode":"PP1 1AA"},"select-address":{"address":"1, Pending Payments Road, PP1 1AA"},"property-type":{"customPropertyType":"","propertyType":"DETACHED_HOUSE"}},"cachedAddresses":"[{\"singleLineAddress\":\"1, Pending Payments Road, PP1 1AA\",\"localCouncilId\":1,\"uprn\":5014,\"buildingNumber\":\"1\",\"streetName\":\"Pending Payments Road\",\"postcode\":\"PP1 1AA\"}]","isAddressAlreadyRegistered":"false"}', 'urn:fdc:gov.uk:2022:UVWXY', null);

SELECT setval(pg_get_serial_sequence('saved_journey_state', 'id'), (SELECT MAX(id) FROM saved_journey_state));

INSERT INTO landlord_incomplete_properties (user_id, saved_journey_state_id)
VALUES ('urn:fdc:gov.uk:2022:UVWXY', 1),
       ('urn:fdc:gov.uk:2022:UVWXY', 2),
       ('urn:fdc:gov.uk:2022:UVWXY', 3),
       ('urn:fdc:gov.uk:2022:UVWXY', 4),
       ('urn:fdc:gov.uk:2022:UVWXY', 5),
       ('urn:fdc:gov.uk:2022:UVWXY', 6);

-- Payments in each status. The local GOV.UK Pay mock restores these from the database when they're first requested.
INSERT INTO payment (payment_id, created_date, amount_in_pence, reference, payment_created_at, quote_date, for_period_ending, status,
                     paying_user_id, associated_property_id, associated_incomplete_property_user_id,
                     associated_incomplete_property_saved_journey_state_id)
VALUES ('created-payment', current_date, 2000, 'created-payment-reference', current_date, current_date, current_date + 365, 0, 'urn:fdc:gov.uk:2022:UVWXY', null, 'urn:fdc:gov.uk:2022:UVWXY', 6),
       ('cancelling-payment', current_date, 2000, 'cancelling-payment-reference', current_date, current_date, current_date + 365, 6, 'urn:fdc:gov.uk:2022:UVWXY', null, 'urn:fdc:gov.uk:2022:UVWXY', 6),
       ('failed-payment', current_date, 2000, 'failed-payment-reference', current_date, current_date, current_date + 365, 3, 'urn:fdc:gov.uk:2022:UVWXY', null, 'urn:fdc:gov.uk:2022:UVWXY', 6),
       ('cancelled-payment', current_date, 2000, 'cancelled-payment-reference', current_date, current_date, current_date + 365, 4, 'urn:fdc:gov.uk:2022:UVWXY', null, 'urn:fdc:gov.uk:2022:UVWXY', 6),
       ('succeeded-payment', '01/15/25', 2000, 'succeeded-payment-reference', '01/15/25', '01/15/25', current_date + 365, 2, 'urn:fdc:gov.uk:2022:UVWXY', 51, null, null);

INSERT INTO joint_landlord_invitation (invited_email, registered_propertyid, token, inviting_landlord_name, created_date)
VALUES ('jl.pending.one@example.com', 8, '2234abcd-5678-abcd-1234-567abcd2222a', 'Inviting landlord', current_date),
       ('jl.pending.two@example.com', 8, '2234abcd-5678-abcd-1234-567abcd2222b', 'Inviting landlord', current_date - 10),
       ('jl.expired.one@example.com', 8, '2234abcd-5678-abcd-1234-567abcd2222c', 'Inviting landlord', current_date - 60),
       ('jl.expired.two@example.com', 8, '2234abcd-5678-abcd-1234-567abcd2222d', 'Inviting landlord', current_date - 90),
       ('jl.pending.three@example.com', 38, '2234abcd-5678-abcd-1234-567abcd3333a', 'Inviting landlord', current_date),
       ('jl.pending.four@example.com', 38, '2234abcd-5678-abcd-1234-567abcd3333b', 'Inviting landlord', current_date - 10),
       ('jl.pending.one@example.com', 13, '2234abcd-5678-abcd-1234-567abcd4444a', 'Inviting landlord', current_date),
       ('jl.pending.org@example.com', 48, '2234abcd-5678-abcd-1234-567abcd5555a', 'Local Organisation Landlord', current_date);

INSERT INTO letting_agent_access (invited_email, property_ownership_id, token, encoded_password, created_date)
VALUES ('letting.agent.one@example.com', 1, '3334abcd-5678-abcd-1234-567abcd1111a', NULL, current_date),
       -- encoded_password is a randomly-salted argon2id hash (for local testing only); the password 'Password123!' verifies against it
       ('letting.agent.two@example.com', 5, '3334abcd-5678-abcd-1234-567abcd2222b',
        '$argon2id$v=19$m=16384,t=2,p=1$6q7zmXD5tXZGyg9Ir8+TDQ$sbyg7lcP/hX+6CBQrqDbmnOOp1LtNEs5YhKKer8U9K4', current_date - 10),
       ('letting-agent@example.com', 49, '3334abcd-5678-abcd-1234-567abcd1111c', NULL, current_date),
       ('letting.agent.details@example.com', 39, '3334abcd-5678-abcd-1234-567abcd2222a', NULL, current_date),
       ('letting.agent.details@example.com', 40, '3334abcd-5678-abcd-1234-567abcd2222d', NULL, current_date),
       ('letting.agent.details@example.com', 50, '3334abcd-5678-abcd-1234-567abcd2222c', NULL, current_date);

-- Local-only property examples for the property-record freeholder cards.
INSERT INTO registration_number (id, created_date, number, type)
VALUES (20000, current_timestamp, 900000020000, 0),
       (20001, current_timestamp, 900000020001, 0),
       (20002, current_timestamp, 900000020002, 0)
ON CONFLICT DO NOTHING;

INSERT INTO address (id, created_date, uprn, single_line_address, local_council_id, postcode, building_name, building_number, street_name)
VALUES (20000, current_timestamp, NULL, 'Joint Freeholders House, PRSDB Road, JF1 1AA', 1, 'JF1 1AA', 'Joint Freeholders House', NULL, 'PRSDB Road'),
       (20001, current_timestamp, NULL, 'Non-Joint Freeholders House, PRSDB Road, NF1 1AA', 1, 'NF1 1AA', 'Non-Joint Freeholders House', NULL, 'PRSDB Road'),
       (20002, current_timestamp, NULL, '12 Joint Freeholder Road, JF1 2AA', 1, 'JF1 2AA', NULL, '12', 'Joint Freeholder Road'),
       (20003, current_timestamp, NULL, '12 Non-Joint Freeholder Road, NF1 2AA', 1, 'NF1 2AA', NULL, '12', 'Non-Joint Freeholder Road'),
       (20004, current_timestamp, NULL, 'No Freeholders House, PRSDB Road, NF1 3AA', 1, 'NF1 3AA', 'No Freeholders House', NULL, 'PRSDB Road')
ON CONFLICT DO NOTHING;

INSERT INTO property_ownership (id, is_active, ownership_type_before_pdjb955, ownership_type, current_num_households,
                                current_num_tenants, registration_number_id, address_id, created_date, last_modified_date,
                                license_id, property_build_type, num_bedrooms, bills_included_list, custom_bills_included,
                                furnished_status, rent_frequency, custom_rent_frequency, rent_amount, custom_property_type,
                                marked_joint_landlord, is_occupied, last_occupied_date, license_provide_later,
                                tenancy_provide_later, correspondence_email, correspondence_address_id, renewal_date)
VALUES (20000, true, 2, 3, 0, 0, 20000, 20000, current_timestamp, NULL, NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, false, false, NULL, false, false, 'email@example.com', 20000, current_date + 365),
       (20001, true, 1, 1, 0, 0, 20001, 20001, current_timestamp, NULL, NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, false, false, NULL, false, false, 'email@example.com', 20001, current_date + 365),
       (20002, true, 0, 2, 0, 0, 20002, 20004, current_timestamp, NULL, NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, false, false, NULL, false, false, 'email@example.com', 20004, current_date + 365)
ON CONFLICT DO NOTHING;

INSERT INTO ownership_link (landlord_id, landlordship_id, created_date)
VALUES (1, 20000, current_timestamp),
       (1, 20001, current_timestamp),
       (1, 20002, current_timestamp)
ON CONFLICT DO NOTHING;

INSERT INTO property_compliance (property_ownership_id, created_date, last_modified_date, has_gas_supply,
                                 has_fire_safety_declaration, has_keep_property_safe_declaration,
                                 has_responsibility_to_tenants_declaration)
VALUES (20000, current_timestamp, NULL, false, true, true, true),
       (20001, current_timestamp, NULL, false, true, true, true),
       (20002, current_timestamp, NULL, false, true, true, true)
ON CONFLICT (property_ownership_id) DO NOTHING;

INSERT INTO freeholder (name, email, address_id, property_ownership_id)
SELECT seeded.name, seeded.email, seeded.address_id, seeded.property_ownership_id
FROM (
    VALUES ('Jordan Joint'::varchar, 'joint.freeholder@example.com'::varchar, NULL::bigint, 20000::bigint),
           ('Casey Joint'::varchar, NULL::varchar, 20002::bigint, 20000::bigint),
           (NULL::varchar, NULL::varchar, NULL::bigint, 20000::bigint),
           ('Taylor Non-Joint'::varchar, 'nonjoint.freeholder@example.com'::varchar, NULL::bigint, 20001::bigint),
           ('Alex Non-Joint'::varchar, NULL::varchar, 20003::bigint, 20001::bigint),
           (NULL::varchar, NULL::varchar, NULL::bigint, 20001::bigint)
) AS seeded(name, email, address_id, property_ownership_id)
WHERE NOT EXISTS (
    SELECT 1
    FROM freeholder existing
    WHERE existing.property_ownership_id = seeded.property_ownership_id
      AND existing.name IS NOT DISTINCT FROM seeded.name
      AND existing.email IS NOT DISTINCT FROM seeded.email
      AND existing.address_id IS NOT DISTINCT FROM seeded.address_id
);

SELECT setval(pg_get_serial_sequence('registration_number', 'id'), (SELECT MAX(id) FROM registration_number));
SELECT setval(pg_get_serial_sequence('address', 'id'), (SELECT MAX(id) FROM address));
SELECT setval(pg_get_serial_sequence('property_ownership', 'id'), (SELECT MAX(id) FROM property_ownership));
