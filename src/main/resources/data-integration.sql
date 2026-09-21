-- =============================================================================
-- Addresses for the QA and metrics cohorts below, at reserved ids far above the range
-- the AddressBase/NGD loader allocates from. The cohorts used to claim whichever
-- existing addresses were not yet used by an active property, which meant an unbounded
-- scan of the 35m-row address table on every boot and left each property in whichever
-- council happened to own the address it claimed.
--
--   9000000001-9000000009  provide-later property record QA cohort (properties 49-57)
--   9000001001-9000001101  metrics test cohort 1 (properties 1201-1301)
--   9000002001-9000002100  metrics test cohort 2 (properties 1601-1700)
--
-- These rows are inert to the NGD loader, which is delta-based and keyed on uprn:
--   * uprn IS NULL, so its ON CONFLICT (uprn) DO UPDATE never matches them, and its
--     property_ownership refresh (WHERE a.uprn IN (...)) never overwrites them
--   * is_active, so its "delete unused inactive addresses" pass never considers them
--   * the address id sequence is deliberately NOT bumped past these ids, so the loader
--     carries on allocating from where it left off
-- A NULL uprn also keeps them out of the address lookup, which requires uprn IS NOT NULL.
-- =============================================================================
INSERT INTO address (id, created_date, uprn, single_line_address, postcode, building_number, local_council_id)
SELECT 9000000000 + i, current_timestamp, null::bigint,
       i || ' Provide Later Road, Testville, QA1 1AA', 'QA1 1AA', i || '', 2
FROM generate_series(1, 9) AS s(i)
UNION ALL
SELECT 9000001000 + i, current_timestamp, null::bigint,
       i || ' Metrics Property Street, MT2 2BB', 'MT2 2BB', i || '', 2
FROM generate_series(1, 101) AS s(i)
UNION ALL
SELECT 9000002000 + i, current_timestamp, null::bigint,
       i || ' Realistic Metrics Street, MT3 3CC', 'MT3 3CC', i || '', 2
FROM generate_series(1, 100) AS s(i)
UNION ALL
SELECT 9000010000 + i, current_timestamp, null::bigint,
       i || ' QA Correspondence Way, Testville, QA1 1AA', 'QA1 1AA', i || '', 2
FROM generate_series(1, 9) AS s(i)
UNION ALL
SELECT 9000011000 + i, current_timestamp, null::bigint,
       i || ' Metrics Correspondence Street, MT2 2BB', 'MT2 2BB', i || '', 2
FROM generate_series(1, 101) AS s(i)
UNION ALL
SELECT 9000012000 + i, current_timestamp, null::bigint,
       i || ' Realistic Correspondence Street, MT3 3CC', 'MT3 3CC', i || '', 2
FROM generate_series(1, 100) AS s(i)
ON CONFLICT DO NOTHING;

INSERT INTO prsdb_user (id, created_date)
VALUES ('urn:fdc:gov.uk:2022:n93slCXHsxJ9rU6-AFM0jFIctYQjYf0KN9YVuJT-cao', '2024-10-15 00:00:00+00'),
       ('urn:fdc:gov.uk:2022:cgVX2oJWKHMwzm8Gzx25CSoVXixVS0rw32Sar4Om8vQ', '2024-10-15 00:00:00+00'),
       ('urn:fdc:gov.uk:2022:_RNZomOzEjxF4o2NzxWskS062b7hTVWLFI8TYsmoWAk', '2025-02-19 12:01:07.575927+00'),
       ('urn:fdc:gov.uk:2022:DySqeEXIC4G2xauOirtTDcezwCPLZgQPUQZmQ-aIIMk', '2025-02-26 17:02:19.625996+00'),
       ('urn:fdc:gov.uk:2022:A9B5GpzhlOrNoGQM65oUESHL5i3O9fp0wjizEFVcCrU', '2025-03-06 15:32:59.529898+00'),
       ('urn:fdc:gov.uk:2022:07lXHJeQwE0k5PZO7w_PQF425vT8T7e63MrvyPYNSoI', '2025-03-12 17:12:19.833105+00'),
       ('urn:fdc:gov.uk:2022:ListhqO1Hu6G90tyF_Rozj4F0YkLHreBnCQZ3JQSiEU', '2025-03-17 10:13:36.388805+00'),
       ('urn:fdc:gov.uk:2022:mwfvbb5GgiDh0acjz9EDDQ7zwskWZzUSnWfavL70f6s', '2025-03-18 10:13:36.388805+00'),
       ('urn:fdc:gov.uk:2022:mGHDySEVfCsvfvc6lVWf6Qt9Dv0ZxPQWKoEzcjnBlUo', '2024-10-15 00:00:00+00'),
       ('urn:fdc:gov.uk:2022:sgO5-g7fThIp2MhXMcvFo5N6ObnstGFVNSYFkghMd24', '2025-03-06 08:22:41.002251+00'),
       ('urn:fdc:gov.uk:2022:La9gwI6zvuzT3yvKjsKEH2cDbtL88wNbiqAeXQ0plEM', '2025-03-06 10:33:22.395944+00'),
       ('urn:fdc:gov.uk:2022:GzFopg--2AyE6XtssVWwQTPELVQFupHJOjpONWS2uz0', '2025-05-01 10:33:22.395944+00'),
       ('7442a5af6972afba82cb61b66df4d2d2249cfc752af5336320d3e3f8cff9a324', '2026-05-05 00:00:00+00'), -- Bill.Haigh@communities.gov.uk
       ('d3bc128e9145369b00a80ebc9ba8e9a035b91302a98d65ea110dc69f064f8a16', '2026-05-05 00:00:00+00'), -- Jasmin.Conterio@communities.gov.uk
       ('e4ea31a38bb24eae34ac3186218c0084fce639a7fe3d36436f716535f45eafbe', '2026-05-05 00:00:00+00'), -- Thomas.Hanmer@communities.gov.uk
       ('ae24b0d78eda0aa3cf8d51cb56f73ffd6e5678e2ccd44d3ddc4a2e2eb5e2f350', '2026-05-05 00:00:00+00'), -- Rowan.Hill@communities.gov.uk
       ('a8df415dcb0356bd9ea1ac3f368a5603fc609e5ad4654e8f5b1c0415d4f0fb46', '2026-05-05 00:00:00+00'), -- Alexander.Read@communities.gov.uk
       ('a7b19a3c6de8b210be76c44b1d2e3ef3eb59cf19402c20e5983e1ac371d9e696', '2026-05-05 00:00:00+00'), -- Travis.Woodward@communities.gov.uk
       ('cb7d851c94b22400e90d6e6265c9867542e0d39fb22d35ddcc2baee1dcf43225', '2024-10-15 00:00:00+00'), -- lcadmin.prsdb@softwire.com
       ('2488954246d8ffea9e419f3a2db5eb5b694e5859b123a008a533dbe8bf0aa16c', '2024-10-15 00:00:00+00'), -- lcuser.prsdb@softwire.com
       ('urn:fdc:gov.uk:2022:ErdvdxjqbulqrJI9hDob1vE0BQ_BqVXlv-mWZwgBJgA', '2026-07-01 10:33:22.395944+00'),  -- danielle.dias@madetech.com
       ('8d2125eb8dbe8146d91491783e13106694ed320224dc34fb56d86c8fba6b3bbb', '2026-07-01 10:33:22.395944+00'),  -- danielle.dias@madetech.com
       ('a84d3882f2dd7b9bfe55a33cc035b29987d1affb92f6e556e12be513075302f3', '2026-07-02 10:00:00+00'), -- benjamin.johnson@madetech.com
       ('urn:fdc:gov.uk:2022:qw2_iN4-Be1BkbYb8y-KyMuPfG7F49W_1fsa_V6iX9w', '2026-07-02 10:00:00+00'), -- benjamin.johnson@madetech.com
       ('2336926fc37be0f0d3e7e6a50409fd7f14e6b5e3f23463859813ed3d3a2b286f', '2026-08-25 00:00:00+00'), -- Katrina.DiMuro@communities.gov.uk
       ('urn:fdc:gov.uk:2022:HWihy8O1bH7nvqzL8zTP1RYQrPU3CxK6g6vYQvZ6tm4', '2026-08-25 00:00:00+00') -- Katrina.DiMuro@communities.gov.uk
    ON CONFLICT DO NOTHING;


INSERT INTO local_council_user (id, created_date, last_modified_date, subject_identifier, is_manager, local_council_id, email, name,
                                has_accepted_privacy_notice)
VALUES (1, '2024-10-15 00:00:00+00', '2024-10-15 00:00:00+00', 'cb7d851c94b22400e90d6e6265c9867542e0d39fb22d35ddcc2baee1dcf43225', true, 2,  -- pragma: allowlist secret
        'lcadmin.prsdb@softwire.com', 'PRSDB LA Admin', true),
       (2, '2024-10-15 00:00:00+00', '2025-02-21 16:12:51.530782+00', '2488954246d8ffea9e419f3a2db5eb5b694e5859b123a008a533dbe8bf0aa16c',  -- pragma: allowlist secret
        false, 2, 'lcuser.prsdb@softwire.com', 'PRSDB La User', true),
       (3, '2025-02-19 12:01:07.575927+00', null, 'a7b19a3c6de8b210be76c44b1d2e3ef3eb59cf19402c20e5983e1ac371d9e696', true, 2, -- pragma: allowlist secret
        'travis.woodward@communities.gov.uk', 'Travis Woodward', true),
       (9, '2026-05-05 00:00:00+00', null, 'a8df415dcb0356bd9ea1ac3f368a5603fc609e5ad4654e8f5b1c0415d4f0fb46', true, 2, -- pragma: allowlist secret
        'Alexander.Read@communities.gov.uk', 'Alexander Read', true),
       (10, '2026-05-05 00:00:00+00', null, 'd3bc128e9145369b00a80ebc9ba8e9a035b91302a98d65ea110dc69f064f8a16', true, 2, -- pragma: allowlist secret
        'Jasmin.Conterio@communities.gov.uk', 'Jasmin Conterio', true),
       (11, '2026-05-05 00:00:00+00', null, 'ae24b0d78eda0aa3cf8d51cb56f73ffd6e5678e2ccd44d3ddc4a2e2eb5e2f350', true, 2, -- pragma: allowlist secret
        'Rowan.Hill@communities.gov.uk', 'Rowan Hill', true),
       (12, '2026-05-05 00:00:00+00', null, '7442a5af6972afba82cb61b66df4d2d2249cfc752af5336320d3e3f8cff9a324', true, 2, -- pragma: allowlist secret
        'Bill.Haigh@communities.gov.uk', 'Bill Haigh', true),
       (13, '2026-05-05 00:00:00+00', null, 'e4ea31a38bb24eae34ac3186218c0084fce639a7fe3d36436f716535f45eafbe', true, 2, -- pragma: allowlist secret
        'Thomas.Hanmer@communities.gov.uk', 'Thomas Hanmer', true),
       (14, '2026-07-01 00:00:00+00', null, '8d2125eb8dbe8146d91491783e13106694ed320224dc34fb56d86c8fba6b3bbb', true, 2, -- pragma: allowlist secret
        'danielle.dias@madetech.com', 'Danielle Dias', true),
       (15, '2026-07-02 10:00:00+00', null, 'a84d3882f2dd7b9bfe55a33cc035b29987d1affb92f6e556e12be513075302f3', true, 2, -- pragma: allowlist secret
        'benjamin.johnson@madetech.com', 'Ben Johnson', true),
       (16, '2026-08-25 00:00:00+00', null, '2336926fc37be0f0d3e7e6a50409fd7f14e6b5e3f23463859813ed3d3a2b286f', true, 2, -- pragma: allowlist secret
        'Katrina.DiMuro@communities.gov.uk', 'Katrina DiMuro', true)
    ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('local_council_user', 'id'), (SELECT MAX(id) FROM local_council_user));

INSERT INTO local_council_invitation (invited_email, inviting_council_id, token, created_date)
VALUES ('expired.invitation+1@example.com', 2, '1234abcd-5678-abcd-1234-567abcd11111', '05/05/2025'),
       ('expired.invitation+2@example.com', 2, '1234abcd-5678-abcd-1234-567abcd11112', '05/05/2025'),
       ('expired.invitation+3@example.com', 2, '1234abcd-5678-abcd-1234-567abcd11113', '05/05/2025'),
       ('expired.invitation+4@example.com', 2, '1234abcd-5678-abcd-1234-567abcd11114', '05/05/2025'),
       ('expired.invitation+5@example.com', 2, '1234abcd-5678-abcd-1234-567abcd11115', '05/05/2025'),
       ('expired.invitation+6@example.com', 2, '1234abcd-5678-abcd-1234-567abcd11116', '05/05/2025'),
       ('expired.invitation+7@example.com', 2, '1234abcd-5678-abcd-1234-567abcd11117', '05/05/2025'),
       ('expired.invitation+8@example.com', 2, '1234abcd-5678-abcd-1234-567abcd11118', '05/05/2025'),
       ('expired.invitation+9@example.com', 2, '1234abcd-5678-abcd-1234-567abcd11119', '05/05/2025'),
       ('expired.invitation+a@example.com', 2, '1234abcd-5678-abcd-1234-567abcd1111a', '05/05/2025') ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('local_council_invitation', 'id'), (SELECT MAX(id) FROM local_council_invitation));

INSERT INTO registration_number (id, created_date, number, type)
VALUES (1, '2024-10-15 00:00:00+00', 2001001001, 1),
       (2, '2024-10-15 00:00:00+00', 3002001002, 1),
       (3, '2025-02-19 08:23:57.267183+00', 127959730689, 1),
       (4, '2025-02-19 13:41:13.782443+00', 116136809177, 1),
       (5, '2025-02-19 13:59:18.561124+00', 6136283775, 1),
       (6, '2025-02-20 11:50:45.723696+00', 105757165800, 1),
       (7, '2025-02-24 09:29:52.993571+00', 116726635893, 1),
       (8, '2025-02-24 10:01:14.5196+00', 61597584540, 1),
       (9, '2025-02-24 10:01:14.5196+00', 54697323416, 0),
       (10, '2025-01-15 00:00:00+00', 83811499802, 0),
       (11, '2025-01-15 00:00:00+00', 40666195053, 0),
       (12, '2025-01-15 00:00:00+00', 150242309330, 0),
       (13, '2025-01-15 00:00:00+00', 150242309331, 0),
       (14, '2025-01-15 00:00:00+00', 150242309332, 0),
       (15, '2025-01-15 00:00:00+00', 150242309333, 0),
       (16, '2025-01-15 00:00:00+00', 150242309334, 0),
       (17, '2025-07-24 00:00:00+00', 150242309335, 0),
       (18, '2026-02-27 00:00:00+00', 150242309336, 0),
       (19, '2026-03-02 00:00:00+00', 210000000019, 0),
       (20, '2026-03-02 00:00:00+00', 210000000020, 0),
       (21, '2026-03-02 00:00:00+00', 210000000021, 0),
       (22, '2026-03-02 00:00:00+00', 210000000022, 0),
       (23, '2026-03-02 00:00:00+00', 210000000023, 0),
       (24, '2026-03-02 00:00:00+00', 210000000024, 0),
       (25, '2026-03-02 00:00:00+00', 210000000025, 0),
       (26, '2026-03-02 00:00:00+00', 210000000026, 0),
       (27, '2026-03-02 00:00:00+00', 210000000027, 0),
       (28, '2026-03-02 00:00:00+00', 210000000028, 0),
       (29, '2026-03-02 00:00:00+00', 210000000029, 0),
       (30, '2026-03-02 00:00:00+00', 210000000030, 0),
       (31, '2026-03-02 00:00:00+00', 210000000031, 0),
       (32, '2026-03-02 00:00:00+00', 210000000032, 0),
       (33, '2026-03-02 00:00:00+00', 210000000033, 0),
       (34, '2026-03-02 00:00:00+00', 210000000034, 0),
       (35, '2026-03-02 00:00:00+00', 210000000035, 0),
       (36, '2026-03-02 00:00:00+00', 210000000036, 0),
       (37, '2026-03-02 00:00:00+00', 210000000037, 0),
       (38, '2026-03-02 00:00:00+00', 210000000038, 0),
       (39, '2026-03-02 00:00:00+00', 210000000039, 0),
       (40, '2026-03-02 00:00:00+00', 210000000040, 0),
       (41, '2026-03-02 00:00:00+00', 210000000041, 0),
       (42, '2026-03-02 00:00:00+00', 210000000042, 0),
       (43, '2026-03-02 00:00:00+00', 210000000043, 0),
       (44, '2026-03-02 00:00:00+00', 210000000044, 0),
       (45, '2026-03-02 00:00:00+00', 210000000045, 0),
       (46, '2026-03-02 00:00:00+00', 210000000046, 0),
       (47, '2026-03-02 00:00:00+00', 210000000047, 0),
       (48, '2026-04-14 00:00:00+00', 210000000048, 0),
       (49, '2026-04-14 00:00:00+00', 210000000049, 0),
       (50, '2026-04-14 00:00:00+00', 210000000050, 0),
       (51, '2026-04-14 00:00:00+00', 210000000051, 0),
       (52, '2026-04-14 00:00:00+00', 210000000052, 0),
       (53, '2026-04-14 00:00:00+00', 210000000053, 0),
       (54, '2026-04-14 00:00:00+00', 210000000054, 0),
       (55, '2026-04-14 00:00:00+00', 210000000055, 0),
       (56, '2026-04-14 00:00:00+00', 210000000056, 0),
       (57, '2026-07-01 00:00:00+00', 210000000057, 1),
       (58, '2026-07-02 10:00:00+00', 210000000058, 1),
       (66, '2026-08-25 00:00:00+00', 210000000066, 1) ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('registration_number', 'id'), (SELECT MAX(id) FROM registration_number));

-- PDJB-1048 / PDJB-1305 provide-later + compliance-banner property record QA (landlord 1):
-- registration numbers for property_ownership 49-57
INSERT INTO registration_number (id, created_date, number, type)
VALUES (57, '2026-04-14 00:00:00+00', 210000000057, 0),
       (58, '2026-04-14 00:00:00+00', 210000000058, 0),
       (59, '2026-04-14 00:00:00+00', 210000000059, 0),
       (60, '2026-04-14 00:00:00+00', 210000000060, 0),
       (61, '2026-04-14 00:00:00+00', 210000000061, 0),
       (62, '2026-04-14 00:00:00+00', 210000000062, 0),
       (63, '2026-04-14 00:00:00+00', 210000000063, 0),
       (64, '2026-04-14 00:00:00+00', 210000000064, 0),
       (65, '2026-04-14 00:00:00+00', 210000000065, 0) ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('registration_number', 'id'), (SELECT MAX(id) FROM registration_number));

INSERT INTO landlord (id, registration_number_id, individual_address_id, created_date, individual_email, individual_non_england_or_wales_address, individual_is_active,
                      last_modified_date, individual_name, individual_phone_number, individual_subject_identifier, individual_date_of_birth, individual_country_of_residence, individual_is_verified,
                      individual_has_accepted_privacy_notice)
SELECT * FROM (VALUES (1, 1, 1, '2024-10-15 00:00:00+00'::timestamptz, 'Team-PRSDB+landlord@softwire.com', null::varchar, true, '2025-02-25 16:17:18.075473+00'::timestamptz, 'PRSD Landlord',
        '+447123456789', 'urn:fdc:gov.uk:2022:mGHDySEVfCsvfvc6lVWf6Qt9Dv0ZxPQWKoEzcjnBlUo', '1950-05-13'::date, 'England or Wales', false, true),
       (2, 2, 1, '2025-02-19 08:23:57.279777+00', 'travis.woodward@communities.gov.uk', null, true, null, 'LISA S C LOOSELEY',
        '07777777777', 'urn:fdc:gov.uk:2022:_RNZomOzEjxF4o2NzxWskS062b7hTVWLFI8TYsmoWAk', '1973-03-14', 'England or Wales', false, true),
       (3, 3, 1, '2025-02-19 13:41:13.861504+00', 'alexander.read@softwire.com', null, true, '2025-03-11 13:38:00.36893+00',
        'KENNETH DECERQUEIRA', '07777777777', 'urn:fdc:gov.uk:2022:A9B5GpzhlOrNoGQM65oUESHL5i3O9fp0wjizEFVcCrU', '1965-07-08',
        'England or Wales', false, true),
       (4, 4, 1, '2025-02-20 11:50:45.745273+00', 'kiran.randhawakukar@softwire.com', null, true, '2025-03-06 14:01:33.486684+00',
        'Not Kiran', '01234567890', 'urn:fdc:gov.uk:2022:ListhqO1Hu6G90tyF_Rozj4F0YkLHreBnCQZ3JQSiEU', '1965-07-08', 'England or Wales',
        false, true),
       (5, 5, 1, '2025-02-24 09:29:53.079945+00', 'jasmin.conterio@softwire.com', null, true, '2025-02-27 17:19:52.061638+00',
        'Jasmin Conterio', '01223 123 456', 'urn:fdc:gov.uk:2022:07lXHJeQwE0k5PZO7w_PQF425vT8T7e63MrvyPYNSoI', '1989-02-02',
        'England or Wales', false, true),
       (6, 6, 1, '2025-03-06 08:22:41.002251+00', 'Team-PRSDB+Unverified@softwire.com', null, true, '2025-03-11 13:47:42.800533+00',
        'Unverified Landlord', '07777777777', 'urn:fdc:gov.uk:2022:sgO5-g7fThIp2MhXMcvFo5N6ObnstGFVNSYFkghMd24', '1996-03-03',
        'England or Wales', false, true),
       (7, 7, 1, '2025-03-06 10:33:22.395944+00', 'team-prsdb+verified@softwire.com', null, true, null, 'KENNETH DECERQUEIRA',
        '07777777777', 'urn:fdc:gov.uk:2022:La9gwI6zvuzT3yvKjsKEH2cDbtL88wNbiqAeXQ0plEM', '1965-07-08', 'England or Wales', true, true),
       (8, 8, 1, '2025-02-27 13:58:02.81462+00', 'isobel.ibironke@softwire.com', null, true, null, 'Isobel Ibironke', '07123456789',
        'urn:fdc:gov.uk:2022:mwfvbb5GgiDh0acjz9EDDQ7zwskWZzUSnWfavL70f6s', '1995-08-4', 'England or Wales', false,
        true),
       (9, 57, 9073642, '2026-07-01 10:33:22.395944+00', 'danielle.dias@madetech.com', null, true, null, 'Danielle Dias',
        '07777777777', 'urn:fdc:gov.uk:2022:ErdvdxjqbulqrJI9hDob1vE0BQ_BqVXlv-mWZwgBJgA', '1990-01-01', 'England or Wales', true,
        true),
       (10, 58, 9073642, '2026-07-02 10:00:00+00', 'benjamin.johnson@madetech.com', null, true, null, 'Ben Johnson',
        '07777777777', 'urn:fdc:gov.uk:2022:qw2_iN4-Be1BkbYb8y-KyMuPfG7F49W_1fsa_V6iX9w', '1990-01-01', 'England or Wales', true,
        true),
       (11, 66, 9073642, '2026-08-25 00:00:00+00', 'Katrina.DiMuro@communities.gov.uk', null, true, null, 'Katrina DiMuro',
        '07777777777', 'urn:fdc:gov.uk:2022:HWihy8O1bH7nvqzL8zTP1RYQrPU3CxK6g6vYQvZ6tm4', '1990-01-01', 'England or Wales', true,
        true)) AS v (
           id, registration_number_id, individual_address_id, created_date, individual_email, individual_non_england_or_wales_address, individual_is_active,
           last_modified_date, individual_name, individual_phone_number, individual_subject_identifier, individual_date_of_birth, individual_country_of_residence, individual_is_verified,
           individual_has_accepted_privacy_notice)
-- Skip seeding these individual landlords for any user who is already registered as an organisation landlord user,
-- otherwise the same user would be linked to two landlords and UserToLandlordService would fail with "Multiple landlords were found".
WHERE NOT EXISTS (
    SELECT 1 FROM organisational_landlord_user olu
    WHERE olu.subject_identifier = v.individual_subject_identifier
)
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('landlord', 'id'), (SELECT MAX(id) FROM landlord));

-- Org landlord setup
INSERT INTO prsdb_user (id, created_date)
VALUES ('urn:fdc:gov.uk:2022:OJhyoHBpqAWPIqCCe_n9eVA4HGvFfgXCQMHSAsKSiRw', '2026-07-30 00:00:00+00') ON CONFLICT DO NOTHING;

INSERT INTO registration_number (id, created_date, number, type)
VALUES (900, '2026-07-30 00:00:00+00', 210000000900, 1) ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('registration_number', 'id'), (SELECT MAX(id) FROM registration_number));

INSERT INTO landlord (id, registration_number_id, landlord_type, created_date,
                      organisation_landlord_name, organisation_address_id, organisation_email, organisation_phone_number,
                      organisation_registrant_name, organisation_registrant_date_of_birth, organisation_registrant_email, organisation_registrant_phone_number,
                      organisation_is_company, organisation_is_charity, organisation_is_trust,
                      organisation_company_number, organisation_charity_registered_with, organisation_charity_number,
                      organisation_lead_trustee_name, organisation_lead_trustee_date_of_birth, organisation_lead_trustee_email,
                      organisation_lead_trustee_phone, organisation_lead_trustee_address_id,
                      organisation_main_contact_name, organisation_main_contact_email, organisation_main_contact_phone)
SELECT * FROM (VALUES (11, 900, 1, '2026-07-30 00:00:00+00'::timestamptz,
        'Test Organisation Landlord', 1, 'Team-PRSDB+orglandlord@softwire.com', '07777777777',
        'Test Registrant', '1980-01-01'::date, 'Team-PRSDB+orglandlord@softwire.com', '07777777778',
        true, true, true,
        '12345678', 0, '1234567',
        'Lead Trustee Name', '1975-06-15'::date, 'lead.trustee@example.com',
        '07777777779', 1,
        'Main Contact Name', 'main.contact@example.com', '07777777780')) AS v (
           id, registration_number_id, landlord_type, created_date,
           organisation_landlord_name, organisation_address_id, organisation_email, organisation_phone_number,
           organisation_registrant_name, organisation_registrant_date_of_birth, organisation_registrant_email, organisation_registrant_phone_number,
           organisation_is_company, organisation_is_charity, organisation_is_trust,
           organisation_company_number, organisation_charity_registered_with, organisation_charity_number,
           organisation_lead_trustee_name, organisation_lead_trustee_date_of_birth, organisation_lead_trustee_email,
           organisation_lead_trustee_phone, organisation_lead_trustee_address_id,
           organisation_main_contact_name, organisation_main_contact_email, organisation_main_contact_phone)
-- Skip creating this organisation landlord if its user is already registered as an individual landlord: the user link
-- below would be skipped for the same reason, so creating the landlord would leave an organisation landlord with no users.
WHERE NOT EXISTS (
    SELECT 1 FROM landlord l WHERE l.individual_subject_identifier = 'urn:fdc:gov.uk:2022:OJhyoHBpqAWPIqCCe_n9eVA4HGvFfgXCQMHSAsKSiRw'
)
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('landlord', 'id'), (SELECT MAX(id) FROM landlord));

INSERT INTO organisational_landlord_user (id, organisation_landlord_id, subject_identifier, name, email, created_date)
SELECT * FROM (VALUES (1, 11, 'urn:fdc:gov.uk:2022:OJhyoHBpqAWPIqCCe_n9eVA4HGvFfgXCQMHSAsKSiRw', 'Test Registrant', 'Team-PRSDB+orglandlord@softwire.com', '2026-07-30 00:00:00+00'::timestamptz)) AS v (id, organisation_landlord_id, subject_identifier, name, email, created_date)
-- Skip linking this user as an organisation landlord user if they are already registered as an individual landlord,
-- otherwise the same user would be linked to two landlords and UserToLandlordService would fail with "Multiple landlords were found".
WHERE NOT EXISTS (
    SELECT 1 FROM landlord l WHERE l.individual_subject_identifier = v.subject_identifier
)
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('organisational_landlord_user', 'id'), (SELECT MAX(id) FROM organisational_landlord_user));

INSERT INTO organisation_governing_body_member (id, organisation_landlord_id, type, name, date_of_birth, address_id, created_date)
SELECT * FROM (VALUES (1, 11, 1, 'Governing Body Trustee', '1985-03-20'::date, 1, '2026-07-30 00:00:00+00'::timestamptz)) AS v (id, organisation_landlord_id, type, name, date_of_birth, address_id, created_date)
-- Only create the governing body member if its organisation landlord was created above.
WHERE EXISTS (SELECT 1 FROM landlord l WHERE l.id = v.organisation_landlord_id)
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('organisation_governing_body_member', 'id'), (SELECT MAX(id) FROM organisation_governing_body_member));


INSERT INTO address (id, created_date, single_line_address, postcode, building_number, local_council_id)
SELECT 8500170000 + i, current_timestamp, i || ' Landlord Correspondence Way', 'EG1 1EG', i || '', 2
FROM generate_series(1, 48) AS s(i)
ON CONFLICT DO NOTHING;

INSERT INTO property_ownership (id, is_active, ownership_type, current_num_households, current_num_tenants, registration_number_id,
                                address_id, created_date, last_modified_date, property_build_type,
                                num_bedrooms, bills_included_list, custom_bills_included, furnished_status, rent_frequency,
                                custom_rent_frequency, rent_amount, custom_property_type, is_occupied, correspondence_email, correspondence_address_id)
VALUES (1, true, 1, 1, 2, 9, 1, '2024-10-15 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170001),
       (2, true, 0, 0, 0, 10, 2, '2025-01-15 00:00:00+00', null, 1,
        null, null, null, null, null, null, null, null, false, 'email@example.com', 8500170002),
       (3, true, 0, 0, 0, 11, 3, '2025-01-15 00:00:00+00', null, 1,
        null, null, null, null, null, null, null, null, false, 'email@example.com', 8500170003),
       (4, true, 0, 0, 0, 12, 4, '2025-01-15 00:00:00+00', null, 1,
        null, null, null, null, null, null, null, null, false, 'email@example.com', 8500170004),
       (5, true, 1, 1, 2, 13, 5, '2024-10-15 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170005),
       (6, true, 1, 1, 2, 14, 6, '2024-10-15 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170006),
       (7, true, 1, 1, 2, 15, 7, '2024-10-15 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170007),
       (8, true, 1, 1, 2, 16, 8, '2024-10-15 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170008),
       (9, true, 1, 1, 2, 17, 9, '2025-07-24 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170009),
       (10, true, 1, 1, 2, 18, 10, '2026-02-27 00:00:00+00', null, 4,
        1, null, null, 2, 1, null, 123.12, 'End terrace', true, 'email@example.com', 8500170010),
       (11, true, 1, 1, 2, 19, 7449159, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170011),
       (12, true, 1, 1, 2, 20, 7449160, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170012),
       (13, true, 1, 1, 2, 21, 7449165, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170013),
       (14, true, 1, 1, 2, 22, 7449169, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170014),
       (15, true, 1, 1, 2, 23, 7449173, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170015),
       (16, true, 1, 1, 2, 24, 7449174, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170016),
       (17, true, 1, 1, 2, 25, 7449179, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170017),
       (18, true, 1, 1, 2, 26, 7449180, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170018),
       (19, true, 1, 1, 2, 27, 7449185, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170019),
       (20, true, 1, 1, 2, 28, 7449193, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170020),
       (21, true, 1, 1, 2, 29, 7449194, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170021),
       (22, true, 1, 1, 2, 30, 7449198, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170022),
       (23, true, 1, 1, 2, 31, 7449199, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170023),
       (24, true, 1, 1, 2, 32, 7449203, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170024),
       (25, true, 1, 1, 2, 33, 7449206, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170025),
       (26, true, 1, 1, 2, 34, 7449208, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170026),
       (27, true, 1, 1, 2, 35, 7449213, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170027),
       (28, true, 1, 1, 2, 36, 7449214, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170028),
       (29, true, 1, 1, 2, 37, 7449217, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170029),
       (30, true, 1, 1, 2, 38, 7449222, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170030),
       (31, true, 1, 1, 2, 39, 7449226, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170031),
       (32, true, 1, 1, 2, 40, 7449227, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170032),
       (33, true, 1, 1, 2, 41, 7449231, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170033),
       (34, true, 1, 1, 2, 42, 7449235, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170034),
       (35, true, 1, 1, 2, 43, 7449239, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170035),
       (36, true, 1, 1, 2, 44, 7449250, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170036),
       (37, true, 1, 1, 2, 45, 7449252, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170037),
       (38, true, 1, 1, 2, 46, 7449253, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170038),
       (39, true, 1, 1, 2, 47, 7449254, '2026-03-02 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170039),
       (40, true, 1, 1, 2, 48, 7449161, '2026-04-14 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170040),
       (41, true, 1, 1, 2, 49, 7449162, '2026-04-14 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170041),
       (42, true, 1, 1, 2, 50, 7449163, '2026-04-14 00:00:00+00', null, 1,
        1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500170042),
       (43, true, 0, 0, 0, 51, 7449166, '2026-04-14 00:00:00+00', null, 1,
        null, null, null, null, null, null, null, null, false, 'email@example.com', 8500170043),
       (44, true, 0, 0, 0, 52, 7449167, '2026-04-14 00:00:00+00', null, 1,
        null, null, null, null, null, null, null, null, false, 'email@example.com', 8500170044),
       (45, true, 0, 0, 0, 53, 7449170, '2026-04-14 00:00:00+00', null, 1,
        null, null, null, null, null, null, null, null, false, 'email@example.com', 8500170045),
       (46, true, 0, 0, 0, 54, 7449175, '2026-04-14 00:00:00+00', null, 1,
        null, null, null, null, null, null, null, null, false, 'email@example.com', 8500170046),
       (47, true, 0, 0, 0, 55, 7449181, '2026-04-14 00:00:00+00', null, 1,
        null, null, null, null, null, null, null, null, false, 'email@example.com', 8500170047),
       (48, true, 0, 0, 0, 56, 7449182, '2026-04-14 00:00:00+00', null, 1,
        null, null, null, null, null, null, null, null, false, 'email@example.com', 8500170048) ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('property_ownership', 'id'), (SELECT MAX(id) FROM property_ownership));

UPDATE property_ownership SET marked_joint_landlord = true WHERE id = 1;

-- =============================================================================
-- PDJB-1048 provide-later property record QA properties (landlord 1), ids 49-56.
-- For manual QA of the new-layout notification banners and "Provide this later"
-- rows behind PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING. Each property takes one
-- of the reserved QA addresses seeded at the top of this file, selected by rn. Occupied
-- properties set last_occupied_date so the "within 28 days" deadline renders.
-- Fixed ids + ON CONFLICT DO NOTHING keep this idempotent under sql.init mode: always.
--   49  occupied, licensing + tenancy skipped, compliance all provide-later -> COMBINED banner
--   50  occupied, everything provided, fully compliant          -> no banner (control)
--   51  occupied, tenancy skipped (licence held), compliant     -> TENANCY banner
--   52  occupied, licensing skipped (tenancy held), compliant   -> LICENSING banner
--   53  occupied, licensing + tenancy skipped, fully compliant  -> BOTH banner
--   54  unoccupied, licensing skipped                           -> licensing provide-later row (no banner)
-- PDJB-1305 compliance-banner QA (occupied, licensing + tenancy fully provided so only the
-- compliance banner shows):
--   55  gas cert expired, electrical + EPC valid                -> single "certificate expired" banner
--   56  gas cert + EPC expired, electrical valid                -> "multiple certificates expired" banner
--   57  gas cert "provide later", electrical + EPC valid        -> "add compliance certificates" (missing) banner
-- =============================================================================
INSERT INTO license (id, license_type, license_number)
VALUES (1, 1, 'LQA0000050'),
       (2, 1, 'LQA0000051') ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('license', 'id'), (SELECT MAX(id) FROM license));

-- rn doubles as the reserved QA address selector (9000000000 + rn), so every row below
-- gets a distinct address.
WITH new_properties (rn, id, registration_number_id, license_id, current_num_households, current_num_tenants,
                     furnished_status, rent_frequency, rent_amount, is_occupied, last_occupied_date,
                     license_provide_later, tenancy_provide_later) AS (
         VALUES (1, 49, 57, null, 0, 0, null, null, null, true, current_date - INTERVAL '7 days', true, true),
                (2, 50, 58, 1, 1, 2, 2, 1, 123.12, true, current_date - INTERVAL '7 days', false, false),
                (3, 51, 59, 2, 0, 0, null, null, null, true, current_date - INTERVAL '7 days', false, true),
                (4, 52, 60, null, 1, 2, 2, 1, 123.12, true, current_date - INTERVAL '7 days', true, false),
                (5, 53, 61, null, 0, 0, null, null, null, true, current_date - INTERVAL '7 days', true, true),
                (6, 54, 62, null, 0, 0, null, null, null, false, null, true, false),
                (7, 55, 63, null, 1, 2, 2, 1, 123.12, true, current_date - INTERVAL '7 days', false, false),
                (8, 56, 64, null, 1, 2, 2, 1, 123.12, true, current_date - INTERVAL '7 days', false, false),
                (9, 57, 65, null, 1, 2, 2, 1, 123.12, true, current_date - INTERVAL '7 days', false, false))
INSERT INTO property_ownership (id, is_active, ownership_type, current_num_households, current_num_tenants, registration_number_id,
                                address_id, created_date, last_modified_date, license_id, property_build_type, num_bedrooms,
                                bills_included_list, custom_bills_included, furnished_status, rent_frequency, custom_rent_frequency,
                                rent_amount, custom_property_type, marked_joint_landlord, is_occupied, last_occupied_date,
                                license_provide_later, tenancy_provide_later, correspondence_email, correspondence_address_id)
SELECT np.id, true, 1, np.current_num_households, np.current_num_tenants, np.registration_number_id,
       9000000000 + np.rn, current_date, current_date, np.license_id, 1, 1,
       null, null, np.furnished_status, np.rent_frequency, null,
       np.rent_amount, null, false, np.is_occupied, np.last_occupied_date,
       np.license_provide_later, np.tenancy_provide_later, 'email@example.com', 9000010000 + np.rn
FROM new_properties np
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('property_ownership', 'id'), (SELECT MAX(id) FROM property_ownership));

INSERT INTO ownership_link (landlord_id, landlordship_id, created_date)
SELECT * FROM (VALUES (1, 1, '2025-01-15'::timestamp),
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
       (1, 47, '2025-01-15'),
       (1, 48, '2025-01-15'),
       (6, 1, '2025-01-15'),
       (7, 1, '2025-01-15')) AS v (landlord_id, landlordship_id, created_date)
-- Only insert links for landlords that were actually seeded, so that a landlord skipped above
-- (because the user is now an organisation landlord) drops only its own links rather than aborting
-- the whole statement with a foreign key violation.
WHERE EXISTS (SELECT 1 FROM landlord l WHERE l.id = v.landlord_id)
ON CONFLICT DO NOTHING;

-- PDJB-1048 / PDJB-1305 QA (landlord 1): ownership links for property_ownership 49-57
INSERT INTO ownership_link (landlord_id, landlordship_id, created_date)
SELECT * FROM (VALUES (1, 49, '2025-01-15'::timestamp),
       (1, 50, '2025-01-15'),
       (1, 51, '2025-01-15'),
       (1, 52, '2025-01-15'),
       (1, 53, '2025-01-15'),
       (1, 54, '2025-01-15'),
       (1, 55, '2025-01-15'),
       (1, 56, '2025-01-15'),
       (1, 57, '2025-01-15')) AS v (landlord_id, landlordship_id, created_date)
-- Only insert links for landlords that were actually seeded (see note above).
WHERE EXISTS (SELECT 1 FROM landlord l WHERE l.id = v.landlord_id)
ON CONFLICT DO NOTHING;

INSERT INTO system_operator (id, created_date, last_modified_date, subject_identifier)
VALUES (1, '2025-02-19 12:01:07.575927+00', null, 'urn:fdc:gov.uk:2022:_RNZomOzEjxF4o2NzxWskS062b7hTVWLFI8TYsmoWAk'),
       (2, '2025-02-26 17:02:19.625996+00', null, 'urn:fdc:gov.uk:2022:DySqeEXIC4G2xauOirtTDcezwCPLZgQPUQZmQ-aIIMk'),
       (3, '2025-03-06 15:32:59.529898+00', null, 'urn:fdc:gov.uk:2022:A9B5GpzhlOrNoGQM65oUESHL5i3O9fp0wjizEFVcCrU'),
       (4, '2025-03-12 17:12:19.833105+00', null, 'urn:fdc:gov.uk:2022:07lXHJeQwE0k5PZO7w_PQF425vT8T7e63MrvyPYNSoI'),
       (5, '2025-03-17 10:13:36.388805+00', null, 'urn:fdc:gov.uk:2022:ListhqO1Hu6G90tyF_Rozj4F0YkLHreBnCQZ3JQSiEU'),
       (6, '2025-03-18 10:13:36.388805+00', null, 'urn:fdc:gov.uk:2022:mwfvbb5GgiDh0acjz9EDDQ7zwskWZzUSnWfavL70f6s'),
       (7, '2025-05-01 12:01:07.575927+00', null, 'urn:fdc:gov.uk:2022:GzFopg--2AyE6XtssVWwQTPELVQFupHJOjpONWS2uz0'),
       (8, '2026-07-01 10:33:22.395944+00', null, 'urn:fdc:gov.uk:2022:ErdvdxjqbulqrJI9hDob1vE0BQ_BqVXlv-mWZwgBJgA'),
       (9, '2026-07-02 10:00:00+00', null, 'urn:fdc:gov.uk:2022:qw2_iN4-Be1BkbYb8y-KyMuPfG7F49W_1fsa_V6iX9w'),
       (10, '2026-08-25 00:00:00+00', null, 'urn:fdc:gov.uk:2022:HWihy8O1bH7nvqzL8zTP1RYQrPU3CxK6g6vYQvZ6tm4') ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('system_operator', 'id'), (SELECT MAX(id) FROM system_operator));

INSERT INTO property_compliance (id, property_ownership_id, created_date, last_modified_date, gas_safety_cert_issue_date, has_gas_supply,
                                 electrical_safety_expiry_date, electrical_cert_type, epc_url, epc_expiry_date,
                                 tenancy_started_before_epc_expiry, epc_energy_rating, epc_exemption_reason, epc_mees_exemption_reason,
                                 has_fire_safety_declaration, has_keep_property_safe_declaration, has_responsibility_to_tenants_declaration)
VALUES (1, 5, '01/01/25', '01/01/25', null, true, null, null, null, null, null, null, null, null, true, true, true),
       (2, 6, '01/01/25', '01/01/25', '1990-02-28', true, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2021-03-16',
        false, 'c', null, null, true, true, true),
       (3, 7, '01/01/25', '01/01/25', null, false, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-1050-2867', '2031-02-28',
        null, 'g', null, null, true, true, true),
       (4, 8, '01/01/25', '01/01/25', null, false, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0892-1563', '2030-12-03',
        null, 'c', null, null, true, true, true),
       (5, 9, '01/01/25', '01/01/25', null, false, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0892-1563', '2030-12-03',
        null, 'c', null, null, true, true, true),
       (6, 40, '2026-04-14', '2026-04-14', '2026-01-15', true, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0892-1563', '2031-06-15',
        null, 'c', null, null, true, true, true),
       (7, 41, '2026-04-14', '2026-04-14', null, false, null, null, null, null, null, null, 0, null, true, true, true),
       (8, 42, '2026-04-14', '2026-04-14', null, false, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-1050-2867', '2031-06-15',
        null, 'f', null, 0, true, true, true),
       (9, 43, '2026-04-14', '2026-04-14', '2026-01-15', true, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0892-1563', '2031-06-15',
        null, 'c', null, null, true, true, true),
       (10, 44, '2026-04-14', '2026-04-14', '2024-06-01', true, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2025-04-14',
        false, 'c', null, null, true, true, true),
       (11, 45, '2026-04-14', '2026-04-14', null, false, null, null, null, null, null, null, 0, null, true, true, true),
       (12, 46, '2026-04-14', '2026-04-14', null, false, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-1050-2867', '2031-06-15',
        null, 'f', null, 0, true, true, true),
       (13, 47, '2026-04-14', '2026-04-14', null, true, null, null, null, null, null, null, null, null, true, true, true),
       (14, 48, '2026-04-14', '2026-04-14', null, false, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-1050-2867', '2031-06-15',
        null, 'g', null, null, true, true, true),
       (15, 1, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (16, 2, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (17, 3, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (18, 4, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (19, 10, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (20, 11, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (21, 12, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (22, 13, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (23, 14, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (24, 15, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (25, 16, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (26, 17, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (27, 18, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (28, 19, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (29, 20, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (30, 21, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (31, 22, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (32, 23, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (33, 24, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (34, 25, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (35, 26, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (36, 27, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (37, 28, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (38, 29, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (39, 30, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (40, 31, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (41, 32, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (42, 33, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (43, 34, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (44, 35, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (45, 36, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (46, 37, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (47, 38, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (48, 39, '01/01/25', null, null, null, null, null, null, null, null, null, null, null, true, true, true) ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('property_compliance', 'id'), (SELECT MAX(id) FROM property_compliance));

-- PDJB-1048 provide-later property record QA (landlord 1): fully compliant records for property_ownership 50-53
-- (gas not required, valid electrical + EPC, all declarations) so they render the pure provide-later banner variant.
INSERT INTO property_compliance (id, property_ownership_id, created_date, last_modified_date, gas_safety_cert_issue_date, has_gas_supply,
                                 electrical_safety_expiry_date, electrical_cert_type, epc_url, epc_expiry_date,
                                 tenancy_started_before_epc_expiry, epc_energy_rating, epc_exemption_reason, epc_mees_exemption_reason,
                                 has_fire_safety_declaration, has_keep_property_safe_declaration, has_responsibility_to_tenants_declaration,
                                 gas_safety_cert_provide_later, electrical_safety_cert_provide_later, epc_provide_later)
VALUES (49, 50, current_date, current_date, null, false, '2035-01-01', null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2035-01-01',
        null, 'c', null, null, true, true, true, false, false, false),
       (50, 51, current_date, current_date, null, false, '2035-01-01', null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2035-01-01',
        null, 'c', null, null, true, true, true, false, false, false),
       (51, 52, current_date, current_date, null, false, '2035-01-01', null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2035-01-01',
        null, 'c', null, null, true, true, true, false, false, false),
       (52, 53, current_date, current_date, null, false, '2035-01-01', null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2035-01-01',
        null, 'c', null, null, true, true, true, false, false, false),
       -- PDJB-1305: scenario A (PO 49) compliance record with all three certs "provide later".
       (53, 49, current_date, current_date, null, true, null, null, null, null,
        null, null, null, null, true, true, true, true, true, true),
       -- PDJB-1305: PO 55 gas cert expired (issued 730 days ago), electrical + EPC valid.
       (54, 55, current_date, current_date, current_date - 730, true, current_date + 730, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', current_date + 730,
        null, 'c', null, null, true, true, true, false, false, false),
       -- PDJB-1305: PO 56 gas cert + EPC expired, electrical valid.
       (55, 56, current_date, current_date, current_date - 730, true, current_date + 730, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', current_date - 365,
        false, 'c', null, null, true, true, true, false, false, false),
       -- PDJB-1305: PO 57 gas cert "provide later", electrical + EPC valid -> "add compliance certificates" banner.
       (56, 57, current_date, current_date, null, true, current_date + 730, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', current_date + 730,
        null, 'c', null, null, true, true, true, true, false, false) ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('property_compliance', 'id'), (SELECT MAX(id) FROM property_compliance));

INSERT INTO passcode (passcode, created_date, last_modified_date, subject_identifier)
VALUES ('PRSD22', current_date, null, 'urn:fdc:gov.uk:2022:mGHDySEVfCsvfvc6lVWf6Qt9Dv0ZxPQWKoEzcjnBlUo'),
       ('PRSD23', current_date, null, 'urn:fdc:gov.uk:2022:_RNZomOzEjxF4o2NzxWskS062b7hTVWLFI8TYsmoWAk'),
       ('PRSD24', current_date, null, 'urn:fdc:gov.uk:2022:A9B5GpzhlOrNoGQM65oUESHL5i3O9fp0wjizEFVcCrU'),
       ('PRSD25', current_date, null, 'urn:fdc:gov.uk:2022:ListhqO1Hu6G90tyF_Rozj4F0YkLHreBnCQZ3JQSiEU'),
       ('PRSD26', current_date, null, 'urn:fdc:gov.uk:2022:07lXHJeQwE0k5PZO7w_PQF425vT8T7e63MrvyPYNSoI'),
       ('PRSD27', current_date, null, 'urn:fdc:gov.uk:2022:sgO5-g7fThIp2MhXMcvFo5N6ObnstGFVNSYFkghMd24'),
       ('PRSD29', current_date, null, 'urn:fdc:gov.uk:2022:La9gwI6zvuzT3yvKjsKEH2cDbtL88wNbiqAeXQ0plEM'),
       ('PRSD32', current_date, null, 'urn:fdc:gov.uk:2022:mwfvbb5GgiDh0acjz9EDDQ7zwskWZzUSnWfavL70f6s') ON CONFLICT DO NOTHING;

-- =============================================================================
-- Metrics test cohort 1: deterministic data (System Operator > Metrics page)
-- =============================================================================
-- Plain set-based INSERTs (not a DO block: the Spring spring.sql.init runner splits
-- on ';' and can't parse dollar-quoting). Fixed ids + ON CONFLICT DO NOTHING make it
-- idempotent under mode: always. Anchored in 2030 so it is isolated from the rest of
-- the 2024-2026 seed data -- query the 2030 reporting period to see only this cohort.
--
-- 121 landlords registered 2030-01-01. The first 101 each own one property whose
-- created_date is landlord.created_date + (i - 1) days, so "time to first property"
-- is 0..100 days, giving exact median/p90/p95 = 50/90/95 days (the service computes
-- rank = fraction * (n - 1) with linear interpolation). Landlords 102-121 own no
-- property, and every 5th landlord is unverified, so the totals are not all identical.
-- The day offset is added as absolute SECONDS (not a `days` interval) so it stays
-- exact across the Europe/London DST boundary on 2030-03-31.
--
-- Landlords all share an existing address (address_id 1, as the other seeded landlords
-- do), and each property takes a distinct reserved metrics address seeded at the top of
-- this file (9000001001 onwards).
-- =============================================================================
INSERT INTO prsdb_user (id, created_date)
SELECT 'metrics-test-user-' || i, TIMESTAMPTZ '2030-01-01 09:00:00+00'
FROM generate_series(1, 121) AS s(i)
ON CONFLICT DO NOTHING;

INSERT INTO registration_number (id, created_date, number, type)
SELECT 1000 + i, TIMESTAMPTZ '2030-01-01 09:00:00+00', 900000000000 + i, 1 -- landlord
FROM generate_series(1, 121) AS s(i)
UNION ALL
SELECT 1200 + i, TIMESTAMPTZ '2030-01-01 09:00:00+00' + make_interval(secs => (i - 1) * 86400), 900000100000 + i, 0 -- property
FROM generate_series(1, 101) AS s(i)
ON CONFLICT DO NOTHING;

INSERT INTO landlord (id, created_date, last_modified_date, registration_number_id, individual_address_id, individual_date_of_birth,
                      individual_is_active, individual_phone_number, individual_subject_identifier, individual_name, individual_email, individual_country_of_residence, individual_is_verified,
                      individual_has_accepted_privacy_notice)
SELECT 1000 + i, TIMESTAMPTZ '2030-01-01 09:00:00+00', TIMESTAMPTZ '2030-01-01 09:00:00+00',
       1000 + i, 1, DATE '1990-01-01', true, '07111111111', 'metrics-test-user-' || i,
       'Metrics Test Landlord ' || i, 'metrics.landlord.' || i || '@example.com', 'England or Wales', (i % 5 <> 0), true
FROM generate_series(1, 121) AS s(i)
ON CONFLICT DO NOTHING;

INSERT INTO property_ownership (id, is_active, ownership_type, current_num_households, current_num_tenants,
                               registration_number_id, address_id, created_date, last_modified_date, license_id,
                               property_build_type, num_bedrooms, marked_joint_landlord, is_occupied, correspondence_email, correspondence_address_id)
SELECT 1200 + i, true, 1, 1, 2, 1200 + i, 9000001000 + i,
       TIMESTAMPTZ '2030-01-01 09:00:00+00' + make_interval(secs => (i - 1) * 86400),
       TIMESTAMPTZ '2030-01-01 09:00:00+00' + make_interval(secs => (i - 1) * 86400), NULL, 1, 2, false, true, 'email@example.com', 9000011000 + i
FROM generate_series(1, 101) AS s(i)
ON CONFLICT DO NOTHING;

INSERT INTO ownership_link (landlord_id, landlordship_id, created_date)
SELECT 1000 + i, 1200 + i, po.created_date
FROM generate_series(1, 101) AS s(i)
JOIN property_ownership po ON po.id = 1200 + i
ON CONFLICT DO NOTHING;

-- =============================================================================
-- Metrics test cohort 2: deterministic "realistic" data (System Operator > Metrics)
-- =============================================================================
-- A second deterministic cohort giving lifelike but reproducible percentiles (the
-- previous version used random() and so changed on every load). 120 landlords register
-- across 2028; the first 100 each own one property and the last 20 own none, and ~60%
-- are verified, so the totals are not all identical. Time to first property is engineered
-- (sub-hour for ~half, hours for ~a third, days for the rest) so the dashboard shows
-- median = 22 minutes, p90 ~ 1 day, p95 ~ 2 days. The duration formatter shows each
-- non-zero unit down to minutes (e.g. 1 day 6 hours displays as "1 day, 6 hours"), so the
-- p90/p95 hours and minutes show too. Fixed sequential ids continuing above the existing
-- seed (landlords 14xx, properties 16xx) plus ON CONFLICT DO NOTHING keep it idempotent
-- under mode: always; the setval calls after all metrics inserts bump the sequences past
-- them (matching the rest of this file). Addresses come from the reserved block seeded at
-- the top of this file (as in cohort 1): landlords share address_id 1 and each property
-- takes a distinct reserved metrics address (9000002001 onwards).
--
-- Query the 2028 reporting period (From 1/1/2028 To 31/12/2028) to see only this cohort:
-- expect 120 registrations, 72 verified, 100 properties, 100 landlords with a property,
-- and median / p90 / p95 = 22 minutes / 1 day / 2 days.
-- =============================================================================
INSERT INTO prsdb_user (id, created_date)
SELECT 'metrics-realistic-user-' || i,
       TIMESTAMPTZ '2028-01-01 00:00:00+00' + make_interval(secs => round((i - 1) * 25920000.0 / 119)::int)
FROM generate_series(1, 120) AS s(i)
ON CONFLICT DO NOTHING;

INSERT INTO registration_number (id, created_date, number, type)
SELECT 1400 + i, TIMESTAMPTZ '2028-01-01 00:00:00+00' + make_interval(secs => round((i - 1) * 25920000.0 / 119)::int),
       900001000000 + i, 1 -- landlord
FROM generate_series(1, 120) AS s(i)
UNION ALL
SELECT 1600 + i, TIMESTAMPTZ '2028-01-01 00:00:00+00', 900001100000 + i, 0 -- property
FROM generate_series(1, 100) AS s(i)
ON CONFLICT DO NOTHING;

INSERT INTO landlord (id, created_date, last_modified_date, registration_number_id, individual_address_id, individual_date_of_birth,
                      individual_is_active, individual_phone_number, individual_subject_identifier, individual_name, individual_email, individual_country_of_residence, individual_is_verified,
                      individual_has_accepted_privacy_notice)
SELECT 1400 + i,
       TIMESTAMPTZ '2028-01-01 00:00:00+00' + make_interval(secs => round((i - 1) * 25920000.0 / 119)::int),
       NULL, 1400 + i, 1, DATE '1985-06-15', true, '07222222222',
       'metrics-realistic-user-' || i, 'Realistic Test Landlord ' || i, 'metrics.realistic.' || i || '@example.com',
       'England or Wales', (i % 5 < 3), true
FROM generate_series(1, 120) AS s(i)
ON CONFLICT DO NOTHING;

-- Property created_date = landlord.created_date + a deterministic "time to first property":
--   i<=50  -> 1..21 minutes      (sub-hour: ~half the cohort)
--   51..80 -> 23 minutes..20 hrs (hours)
--   81..90 -> 20..30 hours       (around the p90 = 1 day point)
--   91..100-> 1.4..2.8 days      (around the p95 = 2 days point)
-- These exact boundary values make median/p90/p95 land on 22 minutes / 1 day / 2 days.
INSERT INTO property_ownership (id, is_active, ownership_type, current_num_households, current_num_tenants,
                               registration_number_id, address_id, created_date, last_modified_date, license_id,
                               property_build_type, num_bedrooms, marked_joint_landlord, is_occupied, correspondence_email, correspondence_address_id)
WITH p AS (
    SELECT i,
           TIMESTAMPTZ '2028-01-01 00:00:00+00'
               + make_interval(secs => round((i - 1) * 25920000.0 / 119)::int)
               + make_interval(secs => (CASE
                     WHEN i <= 50 THEN round(60.0 * (1 + (i - 1) * 20.0 / 49))
                     WHEN i <= 80 THEN round(60.0 * (23 + (i - 51) * 1177.0 / 29))
                     WHEN i <= 90 THEN round(3600.0 * (20 + (i - 81) * 10.0 / 9))
                     ELSE              round(86400.0 * (1.4 + (i - 91) * 1.4 / 9))
                 END)::int) AS created
    FROM generate_series(1, 100) AS s(i)
)
SELECT 1600 + i, true, 1, 1, 2, 1600 + i, 9000002000 + i, created, created, NULL, 1, 2, false, true, 'email@example.com', 9000012000 + i
FROM p
ON CONFLICT DO NOTHING;

INSERT INTO ownership_link (landlord_id, landlordship_id, created_date)
SELECT 1400 + i, 1600 + i, po.created_date
FROM generate_series(1, 100) AS s(i)
JOIN property_ownership po ON po.id = 1600 + i
ON CONFLICT DO NOTHING;

-- Bump the sequences past the metrics seed ids so any later records get higher ids
-- (matching the setval pattern used after the other seed inserts above).
SELECT setval(pg_get_serial_sequence('registration_number', 'id'), (SELECT MAX(id) FROM registration_number));
SELECT setval(pg_get_serial_sequence('landlord', 'id'), (SELECT MAX(id) FROM landlord));
SELECT setval(pg_get_serial_sequence('property_ownership', 'id'), (SELECT MAX(id) FROM property_ownership));
