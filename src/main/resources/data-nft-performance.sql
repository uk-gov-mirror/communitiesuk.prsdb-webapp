-- Address lookup data for the Gatling registration journey in NFT (PostcodeLookup for EG1 2AA, house number 1).
-- Distinct labels avoid ambiguous selections alongside lower-ID QA addresses retained in older dumps.
-- The high IDs are excluded by NftDataSeederDao so these rows stay unallocated.
-- They use reserved ids and uprns far above the range the AddressBase/NGD loader allocates from.

INSERT INTO address (id, created_date, uprn, single_line_address, postcode, building_number, street_name, local_council_id)
VALUES (9100000001, current_timestamp, 9100000001, '1 PRSDB Performance Square, EG1 2AA', 'EG1 2AA', '1', 'PRSDB Performance Square', 1),
       (9100000002, current_timestamp, 9100000002, '2 PRSDB Performance Square, EG1 2AA', 'EG1 2AA', '2', 'PRSDB Performance Square', 1),
       (9100000003, current_timestamp, 9100000003, '3 PRSDB Performance Square, EG1 2AA', 'EG1 2AA', '3', 'PRSDB Performance Square', 1),
       (9100000004, current_timestamp, 9100000004, '4 PRSDB Performance Square, EG1 2AA', 'EG1 2AA', '4', 'PRSDB Performance Square', 1),
       (9100000005, current_timestamp, 9100000005, '5 PRSDB Performance Square, EG1 2AA', 'EG1 2AA', '5', 'PRSDB Performance Square', 1)
ON CONFLICT (id) DO UPDATE
SET single_line_address = EXCLUDED.single_line_address,
    street_name = EXCLUDED.street_name
WHERE address.uprn = EXCLUDED.uprn;
