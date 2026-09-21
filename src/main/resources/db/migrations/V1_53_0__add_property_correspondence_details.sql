ALTER TABLE property_ownership
    ADD COLUMN correspondence_email VARCHAR(255),
    ADD COLUMN correspondence_address_id BIGINT;

-- Backfill correspondence details from the earliest current ownership link's landlord.
-- Email is sourced the same way as Landlord.email:
--   * individual landlord (landlord_type = 0)   -> landlord.individual_email
--   * organisation landlord (landlord_type = 1) -> the (single) organisational_landlord_user.email
-- Address is sourced the same way as Landlord.address:
--   * individual landlord   -> landlord.individual_address_id
--   * organisation landlord -> landlord.organisation_address_id
-- After we do this we can set these columns to NULL
-- If we ever do a squash of migrations, this can all be removed and we make correspondence not null when we define the table.
--
-- A placeholder email ('email@example.com') and the property's own address_id are used as
-- fallbacks for rows where the derived value cannot be resolved. This is defensive against
-- data anomalies that would otherwise fail the NOT NULL constraints added at the end of the
-- migration. The fallback covers:
--   * a property_ownership row with no ownership_link at all
--   * an organisational landlord with no organisational_landlord_user
--   * an individual landlord with a NULL individual_email
--   * an unexpected landlord_type value not matched by the CASE
WITH earliest_link AS (
    SELECT DISTINCT ON (ol.landlordship_id)
           ol.landlordship_id AS property_ownership_id,
           ol.landlord_id
    FROM ownership_link ol
    ORDER BY ol.landlordship_id, ol.created_date, ol.id
),
earliest_org_user AS (
    SELECT DISTINCT ON (olu.organisation_landlord_id)
           olu.organisation_landlord_id,
           olu.email
    FROM organisational_landlord_user olu
    ORDER BY olu.organisation_landlord_id, olu.created_date, olu.id
),
derived_correspondence AS (
    SELECT el.property_ownership_id,
           CASE l.landlord_type
               WHEN 0 THEN l.individual_email
               WHEN 1 THEN eou.email
           END AS email,
           CASE l.landlord_type
               WHEN 0 THEN l.individual_address_id
               WHEN 1 THEN l.organisation_address_id
           END AS address_id
    FROM earliest_link el
    JOIN landlord l ON l.id = el.landlord_id
    LEFT JOIN earliest_org_user eou ON eou.organisation_landlord_id = l.id
)
UPDATE property_ownership po
SET correspondence_email = COALESCE(dc.email, 'email@example.com'),
    correspondence_address_id = COALESCE(dc.address_id, po.address_id)
FROM derived_correspondence dc
WHERE dc.property_ownership_id = po.id;

-- Handle any property_ownership rows that had no ownership_link at all.
UPDATE property_ownership
SET correspondence_email = 'email@example.com',
    correspondence_address_id = address_id
WHERE correspondence_email IS NULL
   OR correspondence_address_id IS NULL;

ALTER TABLE property_ownership
    ADD CONSTRAINT fk_property_ownership_correspondence_address
        FOREIGN KEY (correspondence_address_id) REFERENCES address(id);

ALTER TABLE property_ownership
    ALTER COLUMN correspondence_email SET NOT NULL,
    ALTER COLUMN correspondence_address_id SET NOT NULL;
