-- A :reference_date of CURRENT_DATE will count records between just before midnight last night and 14 days before that.
-- A :reference_date of make_date(2025, 09, 15) will count records from 1st September (2025-09-01 00:00:00.000000) - 14th September inclusive (2025-09-015 00:00:00.000000 is NOT included).
-- Total current registrations, and those in the last 2 weeks
-- Landlord registrations / updates
SELECT
    COUNT(*) FILTER (WHERE created_date < :reference_date) AS total_landlords ,
    COUNT(*) FILTER (WHERE created_date >= :reference_date - INTERVAL '14 DAYS' AND created_date < :reference_date) AS new_landlords_last_2_weeks,
    COUNT(*) FILTER (WHERE last_modified_date >= :reference_date - INTERVAL '14 DAYS' AND last_modified_date < :reference_date) AS updated_landlords_last_2_weeks
FROM
    landlord;

-- Property registrations / updates
SELECT
    COUNT(*) FILTER (WHERE created_date < :reference_date) AS total_property_ownerships,
    COUNT(*) FILTER (WHERE created_date >= :reference_date - INTERVAL '14 DAYS' AND created_date < :reference_date) AS new_property_ownerships_last_2_weeks,
    COUNT(*) FILTER (WHERE last_modified_date >= :reference_date - INTERVAL '14 DAYS' AND last_modified_date < :reference_date) AS updated_property_ownerships_last_2_weeks
FROM
    property_ownership;

-- Compliances added / updated
SELECT
    COUNT(*) FILTER (WHERE created_date < :reference_date) AS total_property_compliances,
    COUNT(*) FILTER (WHERE created_date >= :reference_date - INTERVAL '14 DAYS' AND created_date < :reference_date) AS new_property_compliances_last_2_weeks,
    COUNT(*) FILTER (WHERE last_modified_date >= :reference_date - INTERVAL '14 DAYS' AND last_modified_date < :reference_date) AS updated_property_compliances_last_2_weeks
FROM
    property_compliance;

-- Local council users added / updated
SELECT
    is_manager AS is_admin,
    COUNT(*) FILTER (WHERE created_date < :reference_date) AS total_lc_users,
    COUNT(*) FILTER (WHERE created_date >= :reference_date - INTERVAL '14 DAYS' AND created_date < :reference_date) AS new_lc_users_last_2_weeks,
    COUNT(*) FILTER (WHERE last_modified_date >= :reference_date - INTERVAL '14 DAYS' AND last_modified_date < :reference_date) AS updated_lc_users_last_2_weeks
FROM
    local_authority_user
WHERE local_authority_id != 2
GROUP BY is_manager;

-- Passcodes generated
-- Exclude Bath (local_authority_id = 2) for data about the "level of involvement" as this is not real data
-- Use total values to count overall transactions as cost per transaction?
SELECT
    COUNT(*) FILTER (WHERE created_date < :reference_date) AS total_passcodes_generated_on_db,
    COUNT(*) FILTER (WHERE created_date < :reference_date AND local_authority_id != 2) AS total_passcodes_not_bath,
    COUNT(*) FILTER (WHERE created_date >= :reference_date - INTERVAL '14 DAYS' AND created_date < :reference_date) AS new_passcodes_on_db_last_2_weeks,
    COUNT(*) FILTER (WHERE created_date >= :reference_date - INTERVAL '14 DAYS' AND created_date < :reference_date AND local_authority_id != 2) AS new_passcodes_on_db_last_2_weeks_not_bath
FROM
    passcode;
