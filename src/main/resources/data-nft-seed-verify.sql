-- Validate the reusable selected-address seed, not arbitrary in-progress user journeys.
WITH incomplete AS MATERIALIZED (
    SELECT lip.user_id, s.subject_identifier,
           s.serialized_state::jsonb #>> '{journeyData,select-address,address}' AS label,
           (s.serialized_state::jsonb ->> 'cachedAddresses')::jsonb AS cached_addresses
    FROM landlord_incomplete_properties lip
    JOIN saved_journey_state s ON s.id = lip.saved_journey_state_id
),
allocations AS MATERIALIZED (
    SELECT i.*, a.id AS address_id, a.single_line_address, a.local_council_id, a.postcode, a.is_active
    FROM incomplete i
    LEFT JOIN address a ON a.uprn = (i.cached_addresses -> 0 ->> 'uprn')::bigint
),
label_counts AS MATERIALIZED (
    SELECT a.single_line_address, count(*) AS matches
    FROM address a
    WHERE a.is_active
    GROUP BY a.single_line_address
)
SELECT
    (SELECT count(*) FROM property_ownership),
    (SELECT count(*) FROM landlord_incomplete_properties),
    (SELECT count(*) FROM landlord
     WHERE individual_subject_identifier = 'urn:fdc:gov.uk:2022:mGHDySEVfCsvfvc6lVWf6Qt9Dv0ZxPQWKoEzcjnBlUo'
       AND is_active AND individual_has_accepted_privacy_notice AND NOT individual_is_verified),
    (SELECT count(*) FROM address a
     JOIN label_counts labels ON labels.single_line_address = a.single_line_address AND labels.matches = 1
     WHERE a.id BETWEEN 9100000001 AND 9100000005
       AND upper(a.postcode) = 'EG1 2AA' AND a.local_council_id IS NOT NULL AND a.is_active
       AND a.uprn = a.id
       AND a.single_line_address = concat(a.id - 9100000000, ' PRSDB Performance Square, EG1 2AA')
       AND NOT EXISTS (SELECT 1 FROM property_ownership po WHERE po.address_id = a.id AND po.is_active)
       AND NOT EXISTS (SELECT 1 FROM allocations i WHERE i.address_id = a.id)),
    (SELECT count(*) FROM address a
     WHERE a.local_council_id IS NOT NULL AND a.is_active AND a.uprn IS NOT NULL
       AND a.id NOT BETWEEN 9100000001 AND 9100000005
       AND NOT EXISTS (SELECT 1 FROM property_ownership po WHERE po.address_id = a.id AND po.is_active)
       AND NOT EXISTS (SELECT 1 FROM allocations i WHERE i.address_id = a.id)),
    (SELECT count(*) FROM allocations i
     JOIN property_ownership po ON po.address_id = i.address_id AND po.is_active),
    (SELECT count(*) - count(DISTINCT label) FROM incomplete),
    (SELECT count(*) FROM allocations i
     WHERE i.address_id IS NULL OR NOT i.is_active OR i.local_council_id IS NULL
        OR i.user_id IS DISTINCT FROM i.subject_identifier
        OR jsonb_array_length(i.cached_addresses) IS DISTINCT FROM 1
        OR i.label IS DISTINCT FROM i.single_line_address
        OR i.cached_addresses -> 0 ->> 'singleLineAddress' IS DISTINCT FROM i.single_line_address
        OR (i.cached_addresses -> 0 ->> 'localCouncilId')::int IS DISTINCT FROM i.local_council_id
        OR i.cached_addresses -> 0 ->> 'postcode' IS DISTINCT FROM i.postcode),
    (SELECT count(*) FROM incomplete i
     JOIN label_counts labels ON labels.single_line_address = i.label
     WHERE labels.matches <> 1),
    (SELECT count(*) FROM pg_index i JOIN pg_class c ON c.oid = i.indrelid
     WHERE c.relnamespace = 'public'::regnamespace AND NOT i.indisvalid)
      + (SELECT count(*) FROM pg_constraint WHERE connamespace = 'public'::regnamespace AND NOT convalidated)
      + (SELECT count(*) FROM flyway_schema_history WHERE NOT success),
    (SELECT count(*) FROM pg_indexes WHERE schemaname = 'public'
     AND indexname IN ('property_ownership_single_line_address_gin_idx', 'property_ownership_single_line_address_gist_idx'));
