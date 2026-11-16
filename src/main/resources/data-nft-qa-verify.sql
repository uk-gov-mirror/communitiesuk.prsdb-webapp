WITH expected (id, address_id, registration_number_id) AS (
    VALUES (1, 1::bigint, 21), (2, 2, 22), (3, 3, 23), (4, 4, 24), (5, 5, 9000000911),
           (6, 7449161, 31), (7, 7449162, 32), (8, 7449163, 33),
           (9, 7449166, 34), (10, 7449167, 35), (11, 7449170, 36),
           (12, 7449175, 37), (13, 7449181, 38), (14, 7449182, 39),
           (15, 7449164, 40), (16, 7449168, 41), (17, 7449171, 42),
           (18, 9000000001, 9000000902), (19, 9000000002, 9000000903), (20, 9000000003, 9000000904),
           (21, 9000000004, 9000000905), (22, 9000000005, 9000000906), (23, 9000000006, 9000000907),
           (24, 9000000007, 9000000908), (25, 9000000008, 9000000909), (26, 9000000009, 9000000910)
)
SELECT (SELECT count(*) FROM property_ownership) AS properties,
       (SELECT count(*) FROM landlord_incomplete_properties) AS incomplete_properties,
       (SELECT count(*) FROM expected e WHERE NOT EXISTS (
           SELECT 1 FROM property_ownership po
           JOIN address a ON a.id = po.address_id
           JOIN address correspondence ON correspondence.id = po.correspondence_address_id
           JOIN registration_number rn ON rn.id = po.registration_number_id
           JOIN ownership_link ol ON ol.landlordship_id = po.id AND ol.landlord_id = 1
           JOIN landlord l ON l.id = ol.landlord_id
           WHERE po.id = e.id AND po.address_id = e.address_id
             AND po.registration_number_id = e.registration_number_id AND rn.type = 0 AND po.is_active
             AND a.local_council_id IS NOT NULL
             AND CASE
                 WHEN a.single_line_address LIKE '% QA Fixture Road, Testville, QA1 1AA'
                     THEN a.street_name = 'QA Fixture Road' AND a.town_name = 'Testville'
                 WHEN a.single_line_address LIKE '% Provide Later Road, Testville, QA1 1AA'
                     THEN a.street_name = 'Provide Later Road' AND a.town_name = 'Testville'
                 ELSE true
             END
       )) AS invalid_properties,
       (SELECT count(*) FROM landlord WHERE id IN (100, 101) AND landlord_type = 1) AS organisation_landlords,
       (SELECT count(*) FROM flyway_schema_history WHERE NOT success) AS failed_migrations,
       (SELECT count(*) FROM flyway_schema_history WHERE success) AS successful_migrations;
