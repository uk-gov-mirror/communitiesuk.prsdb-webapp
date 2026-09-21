-- =============================================================================
-- Metrics demo seed data (System Operator > Metrics page)
-- =============================================================================
-- The deterministic "metrics demo" cohorts used to populate the Metrics dashboard
-- locally. Kept separate from data-local.sql so that:
--   * local dev loads it alongside data-local.sql (see spring.sql.init.data-locations
--     in application-local.yml), and
--   * the metrics integration test (MetricsSinglePageTests) can seed *only* this
--     script and assert against known, stable numbers.
--
-- It is self-contained: every row it references (addresses, users, registration
-- numbers, landlords, properties) is created here, so the script can be loaded on
-- its own. All inserts use fixed ids + ON CONFLICT DO NOTHING so it stays idempotent
-- under spring.sql.init mode: always and when loaded after data-local.sql locally.
-- =============================================================================

-- System operator matching the local mock One Login user (urn:fdc:gov.uk:2022:UVWXY).
-- Needed so the Metrics page authorises when this script is loaded on its own (the
-- SYSTEM_OPERATOR role is granted by looking up a system_operator row for the subject).
-- When loaded after data-local.sql locally these inserts no-op (the user already exists).
INSERT INTO prsdb_user (id, created_date)
VALUES ('urn:fdc:gov.uk:2022:UVWXY', TIMESTAMPTZ '2024-10-14 00:00:00+00')
ON CONFLICT DO NOTHING;

INSERT INTO system_operator (created_date, last_modified_date, subject_identifier)
VALUES (TIMESTAMPTZ '2025-02-19 12:01:07.575927+00', NULL, 'urn:fdc:gov.uk:2022:UVWXY')
ON CONFLICT (subject_identifier) DO NOTHING;

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
-- Like data-integration.sql, this seed inserts its own addresses (ids 10xx for landlords,
-- 12xx for properties) rather than referencing AddressBase/NGD data, which the local
-- database does not have.
-- =============================================================================
INSERT INTO prsdb_user (id, created_date)
SELECT 'metrics-test-user-' || i, TIMESTAMPTZ '2030-01-01 09:00:00+00'
FROM generate_series(1, 121) AS s(i)
ON CONFLICT DO NOTHING;

INSERT INTO address (id, created_date, single_line_address, local_council_id, postcode, building_number)
SELECT 1000 + i, TIMESTAMPTZ '2030-01-01 09:00:00+00',
       i || ' Metrics Landlord Street, MT1 1AA', NULL::integer, 'MT1 1AA', i || ''
FROM generate_series(1, 121) AS s(i)
UNION ALL
SELECT 1200 + i, TIMESTAMPTZ '2030-01-01 09:00:00+00' + make_interval(secs => (i - 1) * 86400),
       i || ' Metrics Property Street, MT2 2BB', NULL::integer, 'MT2 2BB', i || ''
FROM generate_series(1, 101) AS s(i)
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
       1000 + i, 1000 + i, DATE '1990-01-01', true, '07111111111', 'metrics-test-user-' || i,
       'Metrics Test Landlord ' || i, 'metrics.landlord.' || i || '@example.com', 'England or Wales', (i % 5 <> 0), true
FROM generate_series(1, 121) AS s(i)
ON CONFLICT DO NOTHING;

-- Correspondence addresses
INSERT INTO address (id, single_line_address, postcode) VALUES
    (8500400001, 'Correspondence address for property row 1', 'CO1 1CO'),
    (8500400002, 'Correspondence address for property row 2', 'CO1 1CO');

INSERT INTO address (id, created_date, single_line_address, local_council_id, postcode, building_number)
SELECT 8500410000 + i, TIMESTAMPTZ '2030-01-01 09:00:00+00',
       i || ' Metrics Correspondence Street, MT2 2BB', NULL::integer, 'MT2 2BB', i || ''
FROM generate_series(1, 101) AS s(i)
ON CONFLICT DO NOTHING;

INSERT INTO property_ownership (id, is_active, ownership_type, current_num_households, current_num_tenants,
                               registration_number_id, address_id, created_date, last_modified_date, license_id,
                               property_build_type, num_bedrooms, marked_joint_landlord, is_occupied, correspondence_email, correspondence_address_id)
SELECT 1200 + i, true, 1, 1, 2, 1200 + i, 1200 + i,
       TIMESTAMPTZ '2030-01-01 09:00:00+00' + make_interval(secs => (i - 1) * 86400),
       TIMESTAMPTZ '2030-01-01 09:00:00+00' + make_interval(secs => (i - 1) * 86400), NULL, 1, 2, false, true, 'email@example.com', 8500410000 + i
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
-- them. As in cohort 1, addresses are inserted (ids 14xx/16xx) because the local database
-- has no AddressBase/NGD addresses to reference.
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

INSERT INTO address (id, created_date, single_line_address, local_council_id, postcode, building_number)
SELECT 1400 + i,
       TIMESTAMPTZ '2028-01-01 00:00:00+00' + make_interval(secs => round((i - 1) * 25920000.0 / 119)::int),
       i || ' Realistic Landlord Way, RL1 1AA', NULL::integer, 'RL1 1AA', i || ''
FROM generate_series(1, 120) AS s(i)
UNION ALL
SELECT 1600 + i, TIMESTAMPTZ '2028-01-01 00:00:00+00',
       i || ' Realistic Property Road, RL2 2BB', NULL::integer, 'RL2 2BB', i || ''
FROM generate_series(1, 100) AS s(i)
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
       NULL, 1400 + i, 1400 + i, DATE '1985-06-15', true, '07222222222',
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
-- Correspondence addresses for metrics cohort 2 (one per property_ownership row).
INSERT INTO address (id, created_date, single_line_address, local_council_id, postcode, building_number)
SELECT 8500420000 + i, TIMESTAMPTZ '2028-01-01 00:00:00+00',
       i || ' Realistic Correspondence Road, MT3 3CC', NULL::integer, 'MT3 3CC', i || ''
FROM generate_series(1, 100) AS s(i)
ON CONFLICT DO NOTHING;

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
SELECT 1600 + i, true, 1, 1, 2, 1600 + i, 1600 + i, created, created, NULL, 1, 2, false, true, 'email@example.com', 8500420000 + i
FROM p
ON CONFLICT DO NOTHING;

INSERT INTO ownership_link (landlord_id, landlordship_id, created_date)
SELECT 1400 + i, 1600 + i, po.created_date
FROM generate_series(1, 100) AS s(i)
JOIN property_ownership po ON po.id = 1600 + i
ON CONFLICT DO NOTHING;

-- =============================================================================
-- Metrics test cohort 3: joint landlords joining existing properties
-- =============================================================================
-- The 2031 reporting period demonstrates both joint-landlord metrics cases:
--   * landlord 2002 joins a property that was registered before the period;
--   * landlords 2004 and 2005 join a property after it was registered in the period.
-- The older owner and property use 2029 dates to stay isolated from the 2030 cohort.
-- Every landlord gaining their first property during 2031 does so exactly two days
-- after registering. Query 1/1/2031 to 31/12/2031 to expect 4 registrations,
-- 4 verified landlords, 1 property, 4 landlords gaining a property, and
-- median / p90 / p95 = 2 days.
-- =============================================================================
INSERT INTO prsdb_user (id, created_date)
VALUES ('metrics-joint-user-1', TIMESTAMPTZ '2029-12-01 09:00:00+00'),
       ('metrics-joint-user-2', TIMESTAMPTZ '2031-01-01 09:00:00+00'),
       ('metrics-joint-user-3', TIMESTAMPTZ '2031-02-01 09:00:00+00'),
       ('metrics-joint-user-4', TIMESTAMPTZ '2031-02-04 09:00:00+00'),
       ('metrics-joint-user-5', TIMESTAMPTZ '2031-02-05 09:00:00+00')
ON CONFLICT DO NOTHING;

INSERT INTO address (id, created_date, single_line_address, local_council_id, postcode, building_number)
VALUES (2001, TIMESTAMPTZ '2029-12-01 09:00:00+00', '1 Joint Metrics Landlord Street, JM1 1AA', NULL, 'JM1 1AA', '1'),
       (2002, TIMESTAMPTZ '2031-01-01 09:00:00+00', '2 Joint Metrics Landlord Street, JM1 1AA', NULL, 'JM1 1AA', '2'),
       (2003, TIMESTAMPTZ '2031-02-01 09:00:00+00', '3 Joint Metrics Landlord Street, JM1 1AA', NULL, 'JM1 1AA', '3'),
       (2004, TIMESTAMPTZ '2031-02-04 09:00:00+00', '4 Joint Metrics Landlord Street, JM1 1AA', NULL, 'JM1 1AA', '4'),
       (2005, TIMESTAMPTZ '2031-02-05 09:00:00+00', '5 Joint Metrics Landlord Street, JM1 1AA', NULL, 'JM1 1AA', '5'),
       (2201, TIMESTAMPTZ '2029-12-03 09:00:00+00', '1 Joint Metrics Property Road, JM2 2BB', NULL, 'JM2 2BB', '1'),
       (2202, TIMESTAMPTZ '2031-02-03 09:00:00+00', '2 Joint Metrics Property Road, JM2 2BB', NULL, 'JM2 2BB', '2')
ON CONFLICT DO NOTHING;

INSERT INTO registration_number (id, created_date, number, type)
VALUES (2001, TIMESTAMPTZ '2029-12-01 09:00:00+00', 900002000001, 1),
       (2002, TIMESTAMPTZ '2031-01-01 09:00:00+00', 900002000002, 1),
       (2003, TIMESTAMPTZ '2031-02-01 09:00:00+00', 900002000003, 1),
       (2004, TIMESTAMPTZ '2031-02-04 09:00:00+00', 900002000004, 1),
       (2005, TIMESTAMPTZ '2031-02-05 09:00:00+00', 900002000005, 1),
       (2201, TIMESTAMPTZ '2029-12-03 09:00:00+00', 900002200001, 0),
       (2202, TIMESTAMPTZ '2031-02-03 09:00:00+00', 900002200002, 0)
ON CONFLICT DO NOTHING;

INSERT INTO landlord (id, created_date, last_modified_date, registration_number_id, individual_address_id, individual_date_of_birth,
                      individual_is_active, individual_phone_number, individual_subject_identifier, individual_name, individual_email, individual_country_of_residence, individual_is_verified,
                      individual_has_accepted_privacy_notice)
VALUES (2001, TIMESTAMPTZ '2029-12-01 09:00:00+00', NULL, 2001, 2001, DATE '1980-01-01', true, '07300002001', 'metrics-joint-user-1', 'Joint Metrics Landlord 1', 'metrics.joint.1@example.com', 'England or Wales', true, true),
       (2002, TIMESTAMPTZ '2031-01-01 09:00:00+00', NULL, 2002, 2002, DATE '1980-01-02', true, '07300002002', 'metrics-joint-user-2', 'Joint Metrics Landlord 2', 'metrics.joint.2@example.com', 'England or Wales', true, true),
       (2003, TIMESTAMPTZ '2031-02-01 09:00:00+00', NULL, 2003, 2003, DATE '1980-01-03', true, '07300002003', 'metrics-joint-user-3', 'Joint Metrics Landlord 3', 'metrics.joint.3@example.com', 'England or Wales', true, true),
       (2004, TIMESTAMPTZ '2031-02-04 09:00:00+00', NULL, 2004, 2004, DATE '1980-01-04', true, '07300002004', 'metrics-joint-user-4', 'Joint Metrics Landlord 4', 'metrics.joint.4@example.com', 'England or Wales', true, true),
       (2005, TIMESTAMPTZ '2031-02-05 09:00:00+00', NULL, 2005, 2005, DATE '1980-01-05', true, '07300002005', 'metrics-joint-user-5', 'Joint Metrics Landlord 5', 'metrics.joint.5@example.com', 'England or Wales', true, true)
ON CONFLICT DO NOTHING;

INSERT INTO property_ownership (id, is_active, ownership_type, current_num_households, current_num_tenants,
                               registration_number_id, address_id, created_date, last_modified_date, license_id,
                               property_build_type, num_bedrooms, marked_joint_landlord, is_occupied, correspondence_email, correspondence_address_id)
VALUES (2201, true, 1, 1, 2, 2201, 2201, TIMESTAMPTZ '2029-12-03 09:00:00+00', NULL, NULL, 1, 2, true, true, 'email@example.com', 8500400001),
       (2202, true, 1, 1, 2, 2202, 2202, TIMESTAMPTZ '2031-02-03 09:00:00+00', NULL, NULL, 1, 2, true, true, 'email@example.com', 8500400002)
ON CONFLICT DO NOTHING;

INSERT INTO ownership_link (landlord_id, landlordship_id, created_date)
VALUES (2001, 2201, TIMESTAMPTZ '2029-12-03 09:00:00+00'),
       (2002, 2201, TIMESTAMPTZ '2031-01-03 09:00:00+00'),
       (2003, 2202, TIMESTAMPTZ '2031-02-03 09:00:00+00'),
       (2004, 2202, TIMESTAMPTZ '2031-02-06 09:00:00+00'),
       (2005, 2202, TIMESTAMPTZ '2031-02-07 09:00:00+00')
ON CONFLICT DO NOTHING;

-- Reset the sequences past the metrics cohorts so records created manually in the app
-- (e.g. while testing) get ids above the seeded ones rather than colliding with them.
SELECT setval(pg_get_serial_sequence('address', 'id'), (SELECT MAX(id) FROM address));
SELECT setval(pg_get_serial_sequence('registration_number', 'id'), (SELECT MAX(id) FROM registration_number));
SELECT setval(pg_get_serial_sequence('landlord', 'id'), (SELECT MAX(id) FROM landlord));
SELECT setval(pg_get_serial_sequence('property_ownership', 'id'), (SELECT MAX(id) FROM property_ownership));
