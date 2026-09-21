-- =============================================================================
-- Addresses for the QA cohort below, at reserved ids far above the range the
-- AddressBase/NGD loader allocates from. The cohort used to claim whichever existing
-- addresses were not yet used by an active property, which meant an unbounded scan of
-- the 35m-row address table on every boot and left each property in whichever council
-- happened to own the address it claimed.
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
SELECT 9000010000 + i, current_timestamp, null::bigint,
       i || ' QA Correspondence Way, Testville, QA1 1AA', 'QA1 1AA', i || '', 2
FROM generate_series(1, 9) AS s(i)
ON CONFLICT DO NOTHING;

INSERT INTO prsdb_user (id, created_date)
VALUES ('urn:fdc:gov.uk:2022:n93slCXHsxJ9rU6-AFM0jFIctYQjYf0KN9YVuJT-cao', '2024-10-15 00:00:00+00'),        -- Team-PRSDB+laadmin@softwire.com
       ('urn:fdc:gov.uk:2022:cgVX2oJWKHMwzm8Gzx25CSoVXixVS0rw32Sar4Om8vQ', '2024-10-15 00:00:00+00'),        -- Team-PRSDB+lauser@softwire.com
       ('urn:fdc:gov.uk:2022:_RNZomOzEjxF4o2NzxWskS062b7hTVWLFI8TYsmoWAk',
        '2025-02-19 12:01:07.575927+00'),                                                                    -- travis.woodward@communities.gov.uk
       ('urn:fdc:gov.uk:2022:DySqeEXIC4G2xauOirtTDcezwCPLZgQPUQZmQ-aIIMk', '2025-02-26 17:02:19.625996+00'), -- travis.woodward@softwire.com
       ('urn:fdc:gov.uk:2022:A9B5GpzhlOrNoGQM65oUESHL5i3O9fp0wjizEFVcCrU', '2025-03-06 15:32:59.529898+00'), -- alexander.read@softwire.com
       ('urn:fdc:gov.uk:2022:07lXHJeQwE0k5PZO7w_PQF425vT8T7e63MrvyPYNSoI', '2025-03-12 17:12:19.833105+00'), -- jasmin.conterio@softwire.com
       ('urn:fdc:gov.uk:2022:ListhqO1Hu6G90tyF_Rozj4F0YkLHreBnCQZ3JQSiEU',
        '2025-03-17 10:13:36.388805+00'),                                                                    -- kiran.randhawakukar@softwire.com
       ('urn:fdc:gov.uk:2022:mGHDySEVfCsvfvc6lVWf6Qt9Dv0ZxPQWKoEzcjnBlUo', '2024-10-15 00:00:00+00'),        -- Team-PRSDB+landlord@softwire.com
       ('urn:fdc:gov.uk:2022:ea8XwChQkjezm4MgGJIzI_HRm7l8IPPTIMT705UQXjI',
        '2025-02-27 13:56:15.745135+00'),                                                                    -- geetika.kejriwal@communities.gov.uk
       ('urn:fdc:gov.uk:2022:kob7zYIuzdrUxKTYq7160l_6Tj2ScXTPJ876jZVvAFA',
        '2025-02-27 13:58:02.81462+00'),                                                                     -- catherine.graham2@communities.gov.uk
       ('urn:fdc:gov.uk:2022:sgO5-g7fThIp2MhXMcvFo5N6ObnstGFVNSYFkghMd24',
        '2025-03-06 08:22:41.002251+00'),                                                                    -- Team-PRSDB+Unverified@softwire.com
       ('urn:fdc:gov.uk:2022:La9gwI6zvuzT3yvKjsKEH2cDbtL88wNbiqAeXQ0plEM',
        '2025-03-06 10:33:22.395944+00'),                                                                    -- team-prsdb+verified@softwire.com
       ('urn:fdc:gov.uk:2022:DXI5RSmCmbPQQhBAPCbw1nkL-Dauufg6VOWdR9TuYlk',
        '2025-04-01 11:15:40.136113+00'),                                                                    -- norris.orighoye@communities.gov.uk
       ('urn:fdc:gov.uk:2022:vgKfvjYRO1LnJkmBr7CkEV62g9WoDeD-sZZNt9GCiVU',
        '2025-04-02 12:52:16.995889+00'),                                                                    -- sharan.flora@communities.gov.uk
       ('urn:fdc:gov.uk:2022:pciqch9dYbtBx2rAhxvaCIEu00cQv3NFeIk5f4BesLo', '2025-04-02 13:01:55.29454+00'),  -- rowan.hill@softwire.com
       ('urn:fdc:gov.uk:2022:Q2BSE6pweSpQF8oSBhjHAIjEuLlkRJZzJQ4TO0c7wgI',
        '2025-04-22 10:55:55.704192+00'),                                                                    -- sandra.lila@communities.gov.uk
       ('urn:fdc:gov.uk:2022:GzFopg--2AyE6XtssVWwQTPELVQFupHJOjpONWS2uz0',
        '2025-05-01 10:33:22.395944+00'),                                                                    -- Team-PRSDB+systemoperator@softwire.com
       ('urn:fdc:gov.uk:2022:T0PqJH7B2o8y3t8-cCEsAk1tL8iSf-svJy-O5HvsynE',
        '2025-10-09 12:44:47.460558+00'),                                                                    -- chris.lightfoot@communities.gov.uk
       ('urn:fdc:gov.uk:2022:BqdyyKzMzY6miLk0NSjJZ8j4GHtmuLgL45KisrXMxMg',
        '2025-10-17 14:14:00.484077+00'),                                                                    -- Ned.FrederickCalas-Hathaway@softwire.com
       ('urn:fdc:gov.uk:2022:po6yDD8EFb0c0UfVVoEZHKQyN_mvBG81mcZPz1r83Ss',
        '2025-10-30 10:59:39.724707+00'),                                                                    -- dani.swift@communities.gov.uk
       ('urn:fdc:gov.uk:2022:zLxuwilkLOLLpD3tTmOcG_lE8BNj0NFyqjU17lzn6cI',
        '2025-11-10 12:13:21.344193+00'),                                                                    -- rebecca.coll@communities.gov.uk
       ('urn:fdc:gov.uk:2022:nzYcgBUq3Exgd00RvATgx6_nIUpEq5vO0mMeeNGoLI8',
        '2025-11-10 15:03:08.410190+00'),                                                                    -- shannon.okyemba-tsambou@communities.gov.uk
       ('urn:fdc:gov.uk:2022:mCqrvLgjky23tcKQNo4C4GjDn13sZNcVhdhfqqvimTc',
        '2025-05-01 10:33:22.395944+00'),                                                                    -- Lewis.Jones@communities.gov.uk
       ('urn:fdc:gov.uk:2022:V7SiTu5znvhYuTqkLgN0cOzaGrzkKpGBnrWj8BRQ34Y', '2025-12-01 10:33:22.395944+00'), -- Adam.Jennings@softwire.com
       ('d67bd3b1eb7ff61605ca55b4a9e998b9ba79af041d628d8765563886f787d340', '2026-05-05 00:00:00+00'),       -- Rebecca.Coll@communities.gov.uk
       ('c3abca0ffa7cb80189df88cffae9f6d4c4db33fdbd768af02071b76bed7c2384', '2026-05-05 00:00:00+00'),       -- Dani.Swift@communities.gov.uk
       ('72335159a5ae248253e1fc68434db663963d060ed61a62025463943f4882d5fa', '2026-05-05 00:00:00+00'),       -- Chris.Lightfoot@communities.gov.uk
       ('ab17166668da61c398a63740f21fc822401d4e189620745cec99a6d4f559e953', '2026-05-05 00:00:00+00'),       -- Lewis.Jones@communities.gov.uk
       ('7442a5af6972afba82cb61b66df4d2d2249cfc752af5336320d3e3f8cff9a324', '2026-05-05 00:00:00+00'),       -- Bill.Haigh@communities.gov.uk
       ('d3bc128e9145369b00a80ebc9ba8e9a035b91302a98d65ea110dc69f064f8a16', '2026-05-05 00:00:00+00'),       -- Jasmin.Conterio@communities.gov.uk
       ('e4ea31a38bb24eae34ac3186218c0084fce639a7fe3d36436f716535f45eafbe', '2026-05-05 00:00:00+00'),       -- Thomas.Hanmer@communities.gov.uk
       ('ae24b0d78eda0aa3cf8d51cb56f73ffd6e5678e2ccd44d3ddc4a2e2eb5e2f350', '2026-05-05 00:00:00+00'),       -- Rowan.Hill@communities.gov.uk
       ('a8df415dcb0356bd9ea1ac3f368a5603fc609e5ad4654e8f5b1c0415d4f0fb46', '2026-05-05 00:00:00+00'),       -- Alexander.Read@communities.gov.uk
       ('a7b19a3c6de8b210be76c44b1d2e3ef3eb59cf19402c20e5983e1ac371d9e696', '2026-05-05 00:00:00+00'),       -- Travis.Woodward@communities.gov.uk
       ('cb7d851c94b22400e90d6e6265c9867542e0d39fb22d35ddcc2baee1dcf43225', '2024-10-15 00:00:00+00'),       -- lcadmin.prsdb@softwire.com
       ('2488954246d8ffea9e419f3a2db5eb5b694e5859b123a008a533dbe8bf0aa16c', '2024-10-15 00:00:00+00'),       -- lcuser.prsdb@softwire.com
       ('urn:fdc:gov.uk:2022:ErdvdxjqbulqrJI9hDob1vE0BQ_BqVXlv-mWZwgBJgA', '2026-07-01 10:33:22.395944+00'), -- danielle.dias@madetech.com
       ('8d2125eb8dbe8146d91491783e13106694ed320224dc34fb56d86c8fba6b3bbb', '2026-07-01 10:33:22.395944+00'), -- danielle.dias@madetech.com
       ('a84d3882f2dd7b9bfe55a33cc035b29987d1affb92f6e556e12be513075302f3', '2026-07-02 10:00:00+00'),          -- benjamin.johnson@madetech.com
       ('urn:fdc:gov.uk:2022:qw2_iN4-Be1BkbYb8y-KyMuPfG7F49W_1fsa_V6iX9w', '2026-07-02 10:00:00+00'),          -- benjamin.johnson@madetech.com
       ('urn:fdc:gov.uk:2022:wkm6PXI5bMeyS-iMYW9FoBp86EdijRr6usu6qTaA3Cg', '2026-07-21 00:00:00+00'),          -- Andrew Wilkins
       ('4dfe12260771139d37db3b643569c5d1ca07ed069bd742928301caf629dcfb62', '2026-07-21 00:00:00+00'),          -- Andrew Wilkins
       ('urn:fdc:gov.uk:2022:r4pLmSY3oYBWPaJVhw8U_wgdfXp3e7cuLSjctVWEJWo', '2026-07-21 00:00:00+00'),          -- William Gledhill
       ('517d15e49a019d232e7eb9b4a0e556921f3ef90851dfced02ba2b67c3db1cd9f', '2026-07-21 00:00:00+00'),          -- William Gledhill
       ('urn:fdc:gov.uk:2022:Flwl0DlDFArbsXtBSpBeVNLJ_OhhmnGl7s4Lo-qg3QI', '2026-07-21 00:00:00+00'),          -- Sean Hennity
       ('fa1a153aeb8a7d4e9d01b51fb1f6f1561b8142e4b36d43b1e2af82478d6c9fbc', '2026-07-21 00:00:00+00'),          -- Sean Hennity
       ('5bb8cb895fb6b32d581e1959ff56c43cb2a986a06f2e20c8e3fe0b5a7def320b', '2026-07-21 00:00:00+00'),          -- Sandra Lila
       ('urn:fdc:gov.uk:2022:aOcUnYIpNfDHDfbexwINrPodEJLV5Fh451VDFOM5h_o', '2026-07-21 00:00:00+00'),          -- Denis Fazlji
       ('40a91189913b428bd7140363ca597c58bfc7639966d6ce91341387fcb91fe12c', '2026-07-21 00:00:00+00'),          -- Denis Fazlji
       ('urn:fdc:gov.uk:2022:kMCObIbxtdFvloXjqQUnFISalxA4bct23eJJxe3QDmI', '2026-07-21 00:00:00+00'),          -- Andreea Popescu
       ('dbbbbf7ac4628a07a44a3f6a8f982d1b3bfb581a78ab2cfd2236c04ad7cb375b', '2026-07-21 00:00:00+00'),          -- Andreea Popescu
       ('urn:fdc:gov.uk:2022:3vvs6mvUviJ6xMVqLKL2rS0BIrlszvGh4nRMdX3IFb8', '2026-07-21 00:00:00+00'),          -- Yvonne Andrews
       ('8e5a90c023294deba4df135f7725a6df17d8d52840e10359748f252510df754d', '2026-07-21 00:00:00+00'),          -- Yvonne Andrews
       ('urn:fdc:gov.uk:2022:s-DPDuNmTwvQsptmEwWMTkPMiO2MmDRrE8HF7AcDmZ8', '2026-07-21 00:00:00+00'),          -- Mobin Ibrahim Patel
       ('a48e61f59b20cb7d65628f1a542ccf371de1339e37fccb7e874ccb1ddc91b0c2', '2026-07-21 00:00:00+00'),          -- Mobin Ibrahim Patel
       ('urn:fdc:gov.uk:2022:Xj9nvDG2yHvw53ZsIBKJH_U2UNfMK7nq2iel4cxc6Ow', '2026-07-21 00:00:00+00'),          -- Sarah Warren
       ('df30c357a444cf8eb89169eca7779e7857b35361abddac3b77a91847288daf54', '2026-07-21 00:00:00+00'),          -- Sarah Warren
       ('urn:fdc:gov.uk:2022:9AAN88nNjxaMnuEL0mN45HoEITVd_aMBsrG05AFcdzY', '2026-08-13 00:00:00+00'),          -- Aimie Robinson
       ('836eb184ab487b6d745de607ad4fbdad2de29d33c31168672073b75262194cf2', '2026-08-13 00:00:00+00'),          -- Aimie Robinson
       ('urn:fdc:gov.uk:2022:HWihy8O1bH7nvqzL8zTP1RYQrPU3CxK6g6vYQvZ6tm4', '2026-08-25 00:00:00+00'),          -- Katrina.DiMuro@communities.gov.uk
       ('2336926fc37be0f0d3e7e6a50409fd7f14e6b5e3f23463859813ed3d3a2b286f', '2026-08-25 00:00:00+00'),          -- Katrina.DiMuro@communities.gov.uk
       ('urn:fdc:gov.uk:2022:lGpMoDhAAg0pe72lmsNzg_oFLH1qna1wyya5nSV-F2E', '2026-09-09 00:00:00+00'),          -- Victoria.Greenwell@communities.gov.uk
       ('782a604de0129b1a297f6b557d342b3a677ed786b93ddebe703583fa2f8307b5', '2026-09-09 00:00:00+00')           -- Victoria.Greenwell@communities.gov.uk
    ON CONFLICT DO NOTHING;


INSERT INTO local_council_user (id, created_date, last_modified_date, subject_identifier, is_manager, local_council_id, email, name,
                                has_accepted_privacy_notice)
VALUES (1, '2024-10-15 00:00:00+00', '2024-10-15 00:00:00+00', 'cb7d851c94b22400e90d6e6265c9867542e0d39fb22d35ddcc2baee1dcf43225', true, 2,
        'lcadmin.prsdb@softwire.com', 'PRSDB LA Admin', true),
       (2, '2024-10-15 00:00:00+00', '2025-02-21 16:12:51.530782+00', '2488954246d8ffea9e419f3a2db5eb5b694e5859b123a008a533dbe8bf0aa16c',
        false, 2, 'lcuser.prsdb@softwire.com', 'PRSDB La User', true),
       (3, '2025-02-19 12:01:07.575927+00', null, 'a7b19a3c6de8b210be76c44b1d2e3ef3eb59cf19402c20e5983e1ac371d9e696', true, 2,
        'travis.woodward@communities.gov.uk', 'Travis Woodward', true),
       (19, '2025-10-09 12:44:47.460558+00', '2025-10-09 12:44:47.460558+00',
        '72335159a5ae248253e1fc68434db663963d060ed61a62025463943f4882d5fa', true, 2, 'chris.lightfoot@communities.gov.uk', 'Hello Name',
        true),
       (21, '2025-10-30 11:21:48.730479+00', null, 'c3abca0ffa7cb80189df88cffae9f6d4c4db33fdbd768af02071b76bed7c2384', true, 2,
        'dani.swift@communities.gov.uk', 'Dani Swift', true),
       (22, '2025-11-10 12:13:21.344193+00', null, 'd67bd3b1eb7ff61605ca55b4a9e998b9ba79af041d628d8765563886f787d340', true, 2,
        'rebecca.coll@communities.gov.uk', 'Rebecca Coll', true),
       (24, '2025-12-01 10:33:22.395944+00', null, 'ab17166668da61c398a63740f21fc822401d4e189620745cec99a6d4f559e953', true, 2,
        'Lewis.Jones@communities.gov.uk', 'Lewis Jones', true),
       (26, '2026-05-05 00:00:00+00', null, 'a8df415dcb0356bd9ea1ac3f368a5603fc609e5ad4654e8f5b1c0415d4f0fb46', true, 2,
        'Alexander.Read@communities.gov.uk', 'Alexander Read', true),
       (27, '2026-05-05 00:00:00+00', null, 'd3bc128e9145369b00a80ebc9ba8e9a035b91302a98d65ea110dc69f064f8a16', true, 2,
        'Jasmin.Conterio@communities.gov.uk', 'Jasmin Conterio', true),
       (28, '2026-05-05 00:00:00+00', null, 'ae24b0d78eda0aa3cf8d51cb56f73ffd6e5678e2ccd44d3ddc4a2e2eb5e2f350', true, 2,
        'Rowan.Hill@communities.gov.uk', 'Rowan Hill', true),
       (29, '2026-05-05 00:00:00+00', null, '7442a5af6972afba82cb61b66df4d2d2249cfc752af5336320d3e3f8cff9a324', true, 2,
        'Bill.Haigh@communities.gov.uk', 'Bill Haigh', true),
       (30, '2026-05-05 00:00:00+00', null, 'e4ea31a38bb24eae34ac3186218c0084fce639a7fe3d36436f716535f45eafbe', true, 2,
        'Thomas.Hanmer@communities.gov.uk', 'Thomas Hanmer', true),
       (31, '2026-07-01 00:00:00+00', null, '8d2125eb8dbe8146d91491783e13106694ed320224dc34fb56d86c8fba6b3bbb', true, 2,
        'danielle.dias@madetech.com', 'Danielle Dias', true),
       (32, '2026-07-02 10:00:00+00', null, 'a84d3882f2dd7b9bfe55a33cc035b29987d1affb92f6e556e12be513075302f3', true, 2,
        'benjamin.johnson@madetech.com', 'Ben Johnson', true),
       (33, '2026-07-21 00:00:00+00', null, '4dfe12260771139d37db3b643569c5d1ca07ed069bd742928301caf629dcfb62', true, 2,
        'andrew.wilkins@communities.gov.uk', 'Andrew Wilkins', true),
       (34, '2026-07-21 00:00:00+00', null, '517d15e49a019d232e7eb9b4a0e556921f3ef90851dfced02ba2b67c3db1cd9f', true, 2,
        'william.gledhill@communities.gov.uk', 'William Gledhill', true),
       (35, '2026-07-21 00:00:00+00', null, 'fa1a153aeb8a7d4e9d01b51fb1f6f1561b8142e4b36d43b1e2af82478d6c9fbc', true, 2,
        'sean.hennity@communities.gov.uk', 'Sean Hennity', true),
       (36, '2026-07-21 00:00:00+00', null, '5bb8cb895fb6b32d581e1959ff56c43cb2a986a06f2e20c8e3fe0b5a7def320b', true, 2,
        'sandra.lila@communities.gov.uk', 'Sandra Lila', true),
       (37, '2026-07-21 00:00:00+00', null, '40a91189913b428bd7140363ca597c58bfc7639966d6ce91341387fcb91fe12c', true, 2,
        'denis.fazlji@communities.gov.uk', 'Denis Fazlji', true),
       (38, '2026-07-21 00:00:00+00', null, 'dbbbbf7ac4628a07a44a3f6a8f982d1b3bfb581a78ab2cfd2236c04ad7cb375b', true, 2,
        'andreea.popescu@communities.gov.uk', 'Andreea Popescu', true),
       (39, '2026-07-21 00:00:00+00', null, '8e5a90c023294deba4df135f7725a6df17d8d52840e10359748f252510df754d', true, 2,
        'yvonne.andrews@communities.gov.uk', 'Yvonne Andrews', true),
       (40, '2026-07-21 00:00:00+00', null, 'a48e61f59b20cb7d65628f1a542ccf371de1339e37fccb7e874ccb1ddc91b0c2', true, 2,
        'mobin.patel@communities.gov.uk', 'Mobin Ibrahim Patel', true),
       (41, '2026-07-21 00:00:00+00', null, 'df30c357a444cf8eb89169eca7779e7857b35361abddac3b77a91847288daf54', true, 2,
        'sarah.warren@communities.gov.uk', 'Sarah Warren', true),
       (42, '2026-08-13 00:00:00+00', null, '836eb184ab487b6d745de607ad4fbdad2de29d33c31168672073b75262194cf2', true, 2,
        'Aimie.Robinson@communities.gov.uk', 'Aimie Robinson', true),
       (43, '2026-08-25 00:00:00+00', null, '2336926fc37be0f0d3e7e6a50409fd7f14e6b5e3f23463859813ed3d3a2b286f', true, 2,
        'Katrina.DiMuro@communities.gov.uk', 'Katrina DiMuro', true),
       (44, '2026-09-09 00:00:00+00', null, '782a604de0129b1a297f6b557d342b3a677ed786b93ddebe703583fa2f8307b5', true, 2,
        'Victoria.Greenwell@communities.gov.uk', 'Victoria Greenwell', true) ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('local_council_user', 'id'), (SELECT MAX(id) FROM local_council_user));

INSERT INTO registration_number (id, created_date, number, type)
VALUES (1, '2024-10-15 00:00:00+00', 2001001001, 1),
       (2, '2024-10-15 00:00:00+00', 3002001002, 1),
       (3, '2025-02-19 08:23:57.267183+00', 127959730689, 1),
       (4, '2025-02-19 13:41:13.782443+00', 116136809177, 1),
       (5, '2025-02-19 13:59:18.561124+00', 6136283775, 1),
       (6, '2025-02-20 11:50:45.723696+00', 105757165800, 1),
       (7, '2025-02-24 09:29:52.993571+00', 116726635893, 1),
       (8, '2025-02-24 10:01:14.5196+00', 61597584540, 1),
       (9, '2025-02-27 13:54:35.212499+00', 52836094838, 1),
       (10, '2025-02-27 13:55:19.931954+00', 49873217784, 1),
       (11, '2025-02-27 13:56:15.743199+00', 80551586002, 1),
       (12, '2025-02-27 13:58:02.810321+00', 72697323406, 1),
       (13, '2025-02-27 13:58:02.810321+00', 54697323416, 1),
       (14, '2025-04-01 10:42:02.484395+00', 34742750336, 1),
       (15, '2025-04-01 11:08:11.221142+00', 138260615999, 1),
       (16, '2025-04-02 12:49:10.383124+00', 8590019234, 1),
       (17, '2025-04-02 12:51:57.960029+00', 157739634401, 1),
       (18, '2025-04-03 13:19:34.895749+00', 134224000506, 1),
       (19, '2025-04-03 13:32:17.570957+00', 49069884578, 1),
       (20, '2025-04-07 14:24:48.883888+00', 62926068489, 1),
       (21, '2025-01-15 00:00:00+00', 83811499802, 0),
       (22, '2025-01-15 00:00:00+00', 40666195053, 0),
       (23, '2025-01-15 00:00:00+00', 150242309330, 0),
       (24, '2025-01-15 00:00:00+00', 150242309331, 0),
       (25, '2025-11-10 16:00:00+00', 172360128082, 1),
       (26, '2025-11-10 16:00:00+00', 128085862692, 1),
       (27, '2025-11-10 16:05:00+00', 44704329706, 1),
       (28, '2025-11-10 16:05:00+00', 4003001003, 1),
       (29, '2025-11-10 16:05:00+00', 5004001004, 1),
       (30, '2025-11-10 16:05:00+00', 6005001005, 1),
       (31, '2026-04-14 00:00:00+00', 210000000031, 0),
       (32, '2026-04-14 00:00:00+00', 210000000032, 0),
       (33, '2026-04-14 00:00:00+00', 210000000033, 0),
       (34, '2026-04-14 00:00:00+00', 210000000034, 0),
       (35, '2026-04-14 00:00:00+00', 210000000035, 0),
       (36, '2026-04-14 00:00:00+00', 210000000036, 0),
       (37, '2026-04-14 00:00:00+00', 210000000037, 0),
       (38, '2026-04-14 00:00:00+00', 210000000038, 0),
       (39, '2026-04-14 00:00:00+00', 210000000039, 0),
       (40, '2026-04-14 00:00:00+00', 210000000040, 0),
       (41, '2026-04-14 00:00:00+00', 210000000041, 0),
       (42, '2026-04-14 00:00:00+00', 210000000042, 0),
       (43, '2026-07-01 00:00:00+00', 210000000043, 1),
       (44, '2026-07-02 10:00:00+00', 210000000044, 1),
       (45, '2026-07-21 00:00:00+00', 210000000045, 1), -- Andrew Wilkins
       (46, '2026-07-21 00:00:00+00', 210000000046, 1), -- William Gledhill
       (47, '2026-07-21 00:00:00+00', 210000000047, 1), -- Sean Hennity
       (48, '2026-07-21 00:00:00+00', 210000000048, 1), -- Denis Fazlji
       (49, '2026-07-21 00:00:00+00', 210000000049, 1), -- Andreea Popescu
       (50, '2026-07-21 00:00:00+00', 210000000050, 1), -- Yvonne Andrews
       (51, '2026-07-21 00:00:00+00', 210000000051, 1), -- Mobin Ibrahim Patel
       (52, '2026-07-21 00:00:00+00', 210000000052, 1), -- Sarah Warren
       (53, '2026-08-13 00:00:00+00', 210000000053, 1), -- Aimie Robinson
       (54, '2026-08-25 00:00:00+00', 210000000054, 1), -- Katrina DiMuro
       (55, '2026-09-09 00:00:00+00', 210000000055, 1) ON CONFLICT DO NOTHING; -- Victoria Greenwell

SELECT setval(pg_get_serial_sequence('registration_number', 'id'), (SELECT MAX(id) FROM registration_number));

-- PDJB-1048 / PDJB-1305 provide-later + compliance-banner property record QA (landlord 1):
-- registration numbers for property_ownership 18-26
INSERT INTO registration_number (id, created_date, number, type)
VALUES (43, '2026-04-14 00:00:00+00', 210000000043, 0),
       (44, '2026-04-14 00:00:00+00', 210000000044, 0),
       (45, '2026-04-14 00:00:00+00', 210000000045, 0),
       (46, '2026-04-14 00:00:00+00', 210000000046, 0),
       (47, '2026-04-14 00:00:00+00', 210000000047, 0),
       (48, '2026-04-14 00:00:00+00', 210000000048, 0),
       (49, '2026-04-14 00:00:00+00', 210000000049, 0),
       (50, '2026-04-14 00:00:00+00', 210000000050, 0),
       (51, '2026-04-14 00:00:00+00', 210000000051, 0) ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('registration_number', 'id'), (SELECT MAX(id) FROM registration_number));

INSERT INTO landlord (id, registration_number_id, individual_address_id, created_date, individual_email, individual_non_england_or_wales_address, individual_is_active,
                      last_modified_date, individual_name, individual_phone_number, individual_subject_identifier, individual_date_of_birth, individual_country_of_residence, individual_is_verified,
                      individual_has_accepted_privacy_notice)
SELECT * FROM (VALUES (1, 1, 1, '2024-10-15 00:00:00+00'::timestamptz, 'Team-PRSDB+landlord@softwire.com', null::varchar, true, '2025-02-25 16:17:18.075473+00'::timestamptz, 'PRSD Landlord',
        '+447123456789', 'urn:fdc:gov.uk:2022:mGHDySEVfCsvfvc6lVWf6Qt9Dv0ZxPQWKoEzcjnBlUo', '1950-05-13'::date, 'England or Wales', false, true),
       (2, 2, 1, '2025-02-19 08:23:57.279777+00', 'travis.woodward@communities.gov.uk', null, true, null, 'LISA S C LOOSELEY',
        '07777777777', 'urn:fdc:gov.uk:2022:_RNZomOzEjxF4o2NzxWskS062b7hTVWLFI8TYsmoWAk', '1973-03-14', 'England or Wales', true, true),
       (3, 3, 1, '2025-02-19 13:41:13.861504+00', 'alexander.read@softwire.com', null, true, '2025-03-11 13:38:00.36893+00',
        'KENNETH DECERQUEIRA', '07777777777', 'urn:fdc:gov.uk:2022:A9B5GpzhlOrNoGQM65oUESHL5i3O9fp0wjizEFVcCrU', '1965-07-08',
        'England or Wales', false, true),
       (4, 4, 1, '2025-02-20 11:50:45.745273+00', 'kiran.randhawakukar@softwire.com', null, true, '2025-03-06 14:01:33.486684+00',
        'Not Kiran', '01234567890', 'urn:fdc:gov.uk:2022:ListhqO1Hu6G90tyF_Rozj4F0YkLHreBnCQZ3JQSiEU', '1965-07-08', 'England or Wales',
        false, true),
       (5, 5, 1, '2025-02-24 09:29:53.079945+00', 'jasmin.conterio@softwire.com', null, true, '2025-02-27 17:19:52.061638+00',
        'Jasmin Conterio', '01223 123 456', 'urn:fdc:gov.uk:2022:07lXHJeQwE0k5PZO7w_PQF425vT8T7e63MrvyPYNSoI', '1989-02-02',
        'England or Wales', false, true),
       (7, 7, 1, '2025-02-27 13:56:15.745135+00', 'geetika.kejriwal@communities.gov.uk', null, true, '2025-02-27 14:34:33.323661+00',
        'LISA S C LOOSELEY', '+447123456789', 'urn:fdc:gov.uk:2022:ea8XwChQkjezm4MgGJIzI_HRm7l8IPPTIMT705UQXjI', '1973-03-14',
        'England or Wales', true, true),
       (9, 9, 1, '2025-02-27 13:58:02.81462+00', 'catherine.graham2@communities.gov.uk', null, true, null, 'LISA S C LOOSELEY',
        '+447123456789', 'urn:fdc:gov.uk:2022:kob7zYIuzdrUxKTYq7160l_6Tj2ScXTPJ876jZVvAFA', '1973-03-14', 'England or Wales', true, true),
       (10, 10, 1, '2025-03-06 08:22:41.002251+00', 'Team-PRSDB+Unverified@softwire.com', null, true, '2025-03-11 13:47:42.800533+00',
        'Unverified Landlord', '07777777777', 'urn:fdc:gov.uk:2022:sgO5-g7fThIp2MhXMcvFo5N6ObnstGFVNSYFkghMd24', '1996-03-03',
        'England or Wales', false, true),
       (11, 11, 1, '2025-03-06 10:33:22.395944+00', 'team-prsdb+verified@softwire.com', null, true, null, 'KENNETH DECERQUEIRA',
        '07777777777', 'urn:fdc:gov.uk:2022:La9gwI6zvuzT3yvKjsKEH2cDbtL88wNbiqAeXQ0plEM', '1965-07-08', 'England or Wales', true, true),
       (12, 12, 1, '2025-04-01 11:08:11.224604+00', 'norris.orighoye@communities.gov.uk', null, true, null, 'Norris Orighoye',
        '07777777777', 'urn:fdc:gov.uk:2022:DXI5RSmCmbPQQhBAPCbw1nkL-Dauufg6VOWdR9TuYlk', '1984-11-11', 'England or Wales', false, true),
       (13, 13, 1, '2025-04-02 12:49:10.39148+00', 'sharan.flora@communities.gov.uk', null, true, null, 'JULIE SYED HABIB EYLES-SPENCER',
        '07777777777', 'urn:fdc:gov.uk:2022:vgKfvjYRO1LnJkmBr7CkEV62g9WoDeD-sZZNt9GCiVU', '1967-03-01', 'England or Wales', true, true),
       (14, 14, 1, '2025-04-02 12:51:57.966276+00', 'rowan.hill@softwire.com', null, true, null, 'Rowan Hill', '07777777777',
        'urn:fdc:gov.uk:2022:pciqch9dYbtBx2rAhxvaCIEu00cQv3NFeIk5f4BesLo', '1908-03-27', 'England or Wales', false, true),
       (18, 18, 1, '2025-04-22 10:52:32.910331+00', 'sandra.lila@communities.gov.uk', null, true, null, 'LISA S C LOOSELEY', '07777777777',
        'urn:fdc:gov.uk:2022:Q2BSE6pweSpQF8oSBhjHAIjEuLlkRJZzJQ4TO0c7wgI', '1973-03-14', 'England or Wales', true, true),
       (19, 19, 1, '2025-10-09 12:44:47.460558+00', 'chris.lightfoot@communities.gov.uk', null, true, null, 'Hello Name', '07777777777',
        'urn:fdc:gov.uk:2022:T0PqJH7B2o8y3t8-cCEsAk1tL8iSf-svJy-O5HvsynE', '2001-01-01', 'England or Wales', false, true),
       (20, 20, 1, '2025-10-17 14:16:57.295418+00', 'Ned.FrederickCalas-Hathaway@softwire.com', null, true, null, 'Ned Calas-Hathaway',
        '07777777777', 'urn:fdc:gov.uk:2022:BqdyyKzMzY6miLk0NSjJZ8j4GHtmuLgL45KisrXMxMg', '1973-06-18', 'England or Wales', false, true),
       (21, 25, 1, '2025-11-07 11:15:00+00', 'dani.swift@communities.gov.uk', null, true, null, 'LISA S C LOOSELEY', '07427585544',
        'urn:fdc:gov.uk:2022:po6yDD8EFb0c0UfVVoEZHKQyN_mvBG81mcZPz1r83Ss', '1973-03-14', 'England or Wales', true, true),
       (22, 26, 1, '2025-11-10 15:30:00+00', 'shannon.okyemba-tsambou@communities.gov.uk', null, true, null, 'KENNETH DECERQUEIRA',
        '07432768528', 'urn:fdc:gov.uk:2022:nzYcgBUq3Exgd00RvATgx6_nIUpEq5vO0mMeeNGoLI8', '1965-07-08', 'England or Wales', true, true),
       (23, 27, 1, '2025-11-10 16:10:00+00', 'rebecca.coll@communities.gov.uk', null, true, null, 'Rebecca Coll', '07764123456',
        'urn:fdc:gov.uk:2022:zLxuwilkLOLLpD3tTmOcG_lE8BNj0NFyqjU17lzn6cI', '1989-03-08', 'England or Wales', false, true),
       (24, 28, 1, '2025-11-07 11:15:00+00', 'adam.jennings@softwire.com', null, true, null, 'KENNETH DECERQUEIRA', '07777777777',
        'urn:fdc:gov.uk:2022:V7SiTu5znvhYuTqkLgN0cOzaGrzkKpGBnrWj8BRQ34Y', '1965-07-08', 'England or Wales', true, true),
       (26, 30, 1, '2025-11-07 11:15:00+00', 'Lewis.Jones@communities.gov.uk', null, true, null, 'KENNETH DECERQUEIRA', '07777777777',
        'urn:fdc:gov.uk:2022:mCqrvLgjky23tcKQNo4C4GjDn13sZNcVhdhfqqvimTc', '1965-07-08', 'England or Wales', true, true),
       (27, 43, 1, '2026-07-01 10:33:22.395944+00', 'danielle.dias@madetech.com', null, true, null, 'Danielle Dias',
        '07777777777', 'urn:fdc:gov.uk:2022:ErdvdxjqbulqrJI9hDob1vE0BQ_BqVXlv-mWZwgBJgA', '1990-01-01', 'England or Wales', true,
        true),
       (28, 44, 1, '2026-07-02 10:00:00+00', 'benjamin.johnson@madetech.com', null, true, null, 'Ben Johnson',
        '07777777777', 'urn:fdc:gov.uk:2022:qw2_iN4-Be1BkbYb8y-KyMuPfG7F49W_1fsa_V6iX9w', '1990-01-01', 'England or Wales', true,
        true),
       (29, 45, 1, '2026-07-21 00:00:00+00', 'andrew.wilkins@communities.gov.uk', null, true, null, 'Andrew Wilkins',
        '07777777777', 'urn:fdc:gov.uk:2022:wkm6PXI5bMeyS-iMYW9FoBp86EdijRr6usu6qTaA3Cg', '1990-01-01', 'England or Wales', true, true),
       (30, 46, 1, '2026-07-21 00:00:00+00', 'william.gledhill@communities.gov.uk', null, true, null, 'William Gledhill',
        '07777777777', 'urn:fdc:gov.uk:2022:r4pLmSY3oYBWPaJVhw8U_wgdfXp3e7cuLSjctVWEJWo', '1990-01-01', 'England or Wales', true, true),
       (31, 47, 1, '2026-07-21 00:00:00+00', 'sean.hennity@communities.gov.uk', null, true, null, 'Sean Hennity',
        '07777777777', 'urn:fdc:gov.uk:2022:Flwl0DlDFArbsXtBSpBeVNLJ_OhhmnGl7s4Lo-qg3QI', '1990-01-01', 'England or Wales', true, true),
       (32, 48, 1, '2026-07-21 00:00:00+00', 'denis.fazlji@communities.gov.uk', null, true, null, 'Denis Fazlji',
        '07777777777', 'urn:fdc:gov.uk:2022:aOcUnYIpNfDHDfbexwINrPodEJLV5Fh451VDFOM5h_o', '1990-01-01', 'England or Wales', true, true),
       (33, 49, 1, '2026-07-21 00:00:00+00', 'andreea.popescu@communities.gov.uk', null, true, null, 'Andreea Popescu',
        '07777777777', 'urn:fdc:gov.uk:2022:kMCObIbxtdFvloXjqQUnFISalxA4bct23eJJxe3QDmI', '1990-01-01', 'England or Wales', true, true),
       (34, 50, 1, '2026-07-21 00:00:00+00', 'yvonne.andrews@communities.gov.uk', null, true, null, 'Yvonne Andrews',
        '07777777777', 'urn:fdc:gov.uk:2022:3vvs6mvUviJ6xMVqLKL2rS0BIrlszvGh4nRMdX3IFb8', '1990-01-01', 'England or Wales', true, true),
       (35, 51, 1, '2026-07-21 00:00:00+00', 'mobin.patel@communities.gov.uk', null, true, null, 'Mobin Ibrahim Patel',
        '07777777777', 'urn:fdc:gov.uk:2022:s-DPDuNmTwvQsptmEwWMTkPMiO2MmDRrE8HF7AcDmZ8', '1990-01-01', 'England or Wales', true, true),
       (36, 52, 1, '2026-07-21 00:00:00+00', 'sarah.warren@communities.gov.uk', null, true, null, 'Sarah Warren',
        '07777777777', 'urn:fdc:gov.uk:2022:Xj9nvDG2yHvw53ZsIBKJH_U2UNfMK7nq2iel4cxc6Ow', '1990-01-01', 'England or Wales', true,
        true),
       (37, 53, 1, '2026-08-13 00:00:00+00', 'Aimie.Robinson@communities.gov.uk', null, true, null, 'Aimie Robinson',
        '07777777777', 'urn:fdc:gov.uk:2022:9AAN88nNjxaMnuEL0mN45HoEITVd_aMBsrG05AFcdzY', '1990-01-01', 'England or Wales', true,
        true),
       (38, 54, 1, '2026-08-25 00:00:00+00', 'Katrina.DiMuro@communities.gov.uk', null, true, null, 'Katrina DiMuro',
        '07777777777', 'urn:fdc:gov.uk:2022:HWihy8O1bH7nvqzL8zTP1RYQrPU3CxK6g6vYQvZ6tm4', '1990-01-01', 'England or Wales', true,
        true),
       (39, 55, 1, '2026-09-09 00:00:00+00', 'Victoria.Greenwell@communities.gov.uk', null, true, null, 'Victoria Greenwell',
        '07777777777', 'urn:fdc:gov.uk:2022:lGpMoDhAAg0pe72lmsNzg_oFLH1qna1wyya5nSV-F2E', '1990-01-01', 'England or Wales', true,
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

-- Correspondence addresses
INSERT INTO address (id, single_line_address, postcode) VALUES
    (8500300001, 'Correspondence address for property row 1', 'CO1 1CO'),
    (8500300002, 'Correspondence address for property row 2', 'CO1 1CO'),
    (8500300003, 'Correspondence address for property row 3', 'CO1 1CO'),
    (8500300004, 'Correspondence address for property row 4', 'CO1 1CO'),
    (8500300005, 'Correspondence address for property row 5', 'CO1 1CO'),
    (8500300006, 'Correspondence address for property row 6', 'CO1 1CO'),
    (8500300007, 'Correspondence address for property row 7', 'CO1 1CO'),
    (8500300008, 'Correspondence address for property row 8', 'CO1 1CO'),
    (8500300009, 'Correspondence address for property row 9', 'CO1 1CO'),
    (8500300010, 'Correspondence address for property row 10', 'CO1 1CO'),
    (8500300011, 'Correspondence address for property row 11', 'CO1 1CO'),
    (8500300012, 'Correspondence address for property row 12', 'CO1 1CO'),
    (8500300013, 'Correspondence address for property row 13', 'CO1 1CO'),
    (8500300014, 'Correspondence address for property row 14', 'CO1 1CO'),
    (8500300015, 'Correspondence address for property row 15', 'CO1 1CO'),
    (8500300016, 'Correspondence address for property row 16', 'CO1 1CO'),
    (8500300017, 'Correspondence address for property row 17', 'CO1 1CO');

INSERT INTO property_ownership (id, is_active, ownership_type, current_num_households, current_num_tenants, registration_number_id,
                                address_id, created_date, last_modified_date,
                                property_build_type,
                                num_bedrooms, bills_included_list, custom_bills_included, furnished_status, rent_frequency,
                                custom_rent_frequency, rent_amount, custom_property_type, is_occupied, correspondence_email, correspondence_address_id)
VALUES (1, true, 1, 1, 2, 21, 1, '2024-10-15 00:00:00+00', null, 1, 1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500300001),
       (2, true, 0, 0, 0, 22, 2, '2025-01-15 00:00:00+00', null, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500300002),
       (3, true, 0, 0, 0, 23, 3, '2025-01-15 00:00:00+00', null, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500300003),
       (4, true, 0, 0, 0, 24, 4, '2025-01-15 00:00:00+00', null, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500300004),
       (5, true, 0, 0, 0, 25, 5, '2026-02-27 00:00:00+00', null, 4, null, null, null, null, null, null, null, 'End terrace', false, 'email@example.com', 8500300005),
       (6, true, 1, 1, 2, 31, 7449161, '2026-04-14 00:00:00+00', null, 1, 1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500300006),
       (7, true, 1, 1, 2, 32, 7449162, '2026-04-14 00:00:00+00', null, 1, 1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500300007),
       (8, true, 1, 1, 2, 33, 7449163, '2026-04-14 00:00:00+00', null, 1, 1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500300008),
       (9, true, 1, 1, 2, 34, 7449166, '2026-04-14 00:00:00+00', null, 1, 1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500300009),
       (10, true, 1, 1, 2, 35, 7449167, '2026-04-14 00:00:00+00', null, 1, 1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500300010),
       (11, true, 1, 1, 2, 36, 7449170, '2026-04-14 00:00:00+00', null, 1, 1, null, null, 2, 1, null, 123.12, null, true, 'email@example.com', 8500300011),
       (12, true, 0, 0, 0, 37, 7449175, '2026-04-14 00:00:00+00', null, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500300012),
       (13, true, 0, 0, 0, 38, 7449181, '2026-04-14 00:00:00+00', null, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500300013),
       (14, true, 0, 0, 0, 39, 7449182, '2026-04-14 00:00:00+00', null, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500300014),
       (15, true, 0, 0, 0, 40, 7449164, '2026-04-14 00:00:00+00', null, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500300015),
       (16, true, 0, 0, 0, 41, 7449168, '2026-04-14 00:00:00+00', null, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500300016),
       (17, true, 0, 0, 0, 42, 7449171, '2026-04-14 00:00:00+00', null, 1, null, null, null, null, null, null, null, null, false, 'email@example.com', 8500300017) ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('property_ownership', 'id'), (SELECT MAX(id) FROM property_ownership));

UPDATE property_ownership SET marked_joint_landlord = true WHERE id = 1;

-- =============================================================================
-- PDJB-1048 provide-later property record QA properties (landlord 1), ids 18-25.
-- For manual QA of the new-layout notification banners and "Provide this later"
-- rows behind PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING. Each property takes one
-- of the reserved QA addresses seeded at the top of this file, selected by rn. Occupied
-- properties set last_occupied_date so the "within 28 days" deadline renders.
-- Fixed ids + ON CONFLICT DO NOTHING keep this idempotent under sql.init mode: always.
--   18  occupied, licensing + tenancy skipped, compliance all provide-later -> COMBINED banner
--   19  occupied, everything provided, fully compliant          -> no banner (control)
--   20  occupied, tenancy skipped (licence held), compliant     -> TENANCY banner
--   21  occupied, licensing skipped (tenancy held), compliant   -> LICENSING banner
--   22  occupied, licensing + tenancy skipped, fully compliant  -> BOTH banner
--   23  unoccupied, licensing skipped                           -> licensing provide-later row (no banner)
-- PDJB-1305 compliance-banner QA (occupied, licensing + tenancy fully provided so only the
-- compliance banner shows):
--   24  gas cert expired, electrical + EPC valid                -> single "certificate expired" banner
--   25  gas cert + EPC expired, electrical valid                -> "multiple certificates expired" banner
--   26  gas cert "provide later", electrical + EPC valid        -> "add compliance certificates" (missing) banner
-- =============================================================================
INSERT INTO license (id, license_type, license_number)
VALUES (1, 1, 'LQA0000019'),
       (2, 1, 'LQA0000020') ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('license', 'id'), (SELECT MAX(id) FROM license));

-- rn doubles as the reserved QA address selector (9000000000 + rn), so every row below
-- gets a distinct address.
WITH new_properties (rn, id, registration_number_id, license_id, current_num_households, current_num_tenants,
                     furnished_status, rent_frequency, rent_amount, is_occupied, last_occupied_date,
                     license_provide_later, tenancy_provide_later) AS (
         VALUES (1, 18, 43, null, 0, 0, null, null, null, true, current_date - INTERVAL '7 days', true, true),
                (2, 19, 44, 1, 1, 2, 2, 1, 123.12, true, current_date - INTERVAL '7 days', false, false),
                (3, 20, 45, 2, 0, 0, null, null, null, true, current_date - INTERVAL '7 days', false, true),
                (4, 21, 46, null, 1, 2, 2, 1, 123.12, true, current_date - INTERVAL '7 days', true, false),
                (5, 22, 47, null, 0, 0, null, null, null, true, current_date - INTERVAL '7 days', true, true),
                (6, 23, 48, null, 0, 0, null, null, null, false, null, true, false),
                (7, 24, 49, null, 1, 2, 2, 1, 123.12, true, current_date - INTERVAL '7 days', false, false),
                (8, 25, 50, null, 1, 2, 2, 1, 123.12, true, current_date - INTERVAL '7 days', false, false),
                (9, 26, 51, null, 1, 2, 2, 1, 123.12, true, current_date - INTERVAL '7 days', false, false))
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
       (10, 1, '2025-01-15'),
       (11, 1, '2025-01-15')) AS v (landlord_id, landlordship_id, created_date)
-- Only insert links for landlords that were actually seeded, so that a landlord skipped above
-- (because the user is now an organisation landlord) drops only its own links rather than aborting
-- the whole statement with a foreign key violation.
WHERE EXISTS (SELECT 1 FROM landlord l WHERE l.id = v.landlord_id)
ON CONFLICT DO NOTHING;

-- PDJB-1048 / PDJB-1305 QA (landlord 1): ownership links for property_ownership 18-26
INSERT INTO ownership_link (landlord_id, landlordship_id, created_date)
SELECT * FROM (VALUES (1, 18, '2025-01-15'::timestamp),
       (1, 19, '2025-01-15'),
       (1, 20, '2025-01-15'),
       (1, 21, '2025-01-15'),
       (1, 22, '2025-01-15'),
       (1, 23, '2025-01-15'),
       (1, 24, '2025-01-15'),
       (1, 25, '2025-01-15'),
       (1, 26, '2025-01-15')) AS v (landlord_id, landlordship_id, created_date)
-- Only insert links for landlords that were actually seeded (see note above).
WHERE EXISTS (SELECT 1 FROM landlord l WHERE l.id = v.landlord_id)
ON CONFLICT DO NOTHING;

INSERT INTO property_compliance (id, property_ownership_id, created_date, last_modified_date, gas_safety_cert_issue_date, has_gas_supply,
                                 electrical_safety_expiry_date, electrical_cert_type, epc_url, epc_expiry_date,
                                 tenancy_started_before_epc_expiry, epc_energy_rating, epc_exemption_reason, epc_mees_exemption_reason,
                                 has_fire_safety_declaration, has_keep_property_safe_declaration, has_responsibility_to_tenants_declaration)
VALUES (1, 6, '2026-04-14', '2026-04-14', '2026-01-15', true, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0892-1563', '2031-06-15',
        null, 'c', null, null, true, true, true),
       (2, 7, '2026-04-14', '2026-04-14', '2024-06-01', true, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2025-04-14',
        false, 'c', null, null, true, true, true),
       (3, 8, '2026-04-14', '2026-04-14', null, false, null, null, null, null, null, null, 0, null, true, true, true),
       (4, 9, '2026-04-14', '2026-04-14', null, false, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-1050-2867', '2031-06-15',
        null, 'f', null, 0, true, true, true),
       (5, 10, '2026-04-14', '2026-04-14', null, true, null, null, null, null, null, null, null, null, true, true, true),
       (6, 11, '2026-04-14', '2026-04-14', null, false, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-1050-2867', '2031-06-15',
        null, 'g', null, null, true, true, true),
       (7, 12, '2026-04-14', '2026-04-14', '2026-01-15', true, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0892-1563', '2031-06-15',
        null, 'c', null, null, true, true, true),
       (8, 13, '2026-04-14', '2026-04-14', '2024-06-01', true, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2025-04-14',
        false, 'c', null, null, true, true, true),
       (9, 14, '2026-04-14', '2026-04-14', null, false, null, null, null, null, null, null, 0, null, true, true, true),
       (10, 15, '2026-04-14', '2026-04-14', null, false, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-1050-2867', '2031-06-15',
        null, 'f', null, 0, true, true, true),
       (11, 16, '2026-04-14', '2026-04-14', null, true, null, null, null, null, null, null, null, null, true, true, true),
       (12, 17, '2026-04-14', '2026-04-14', null, false, null, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-1050-2867', '2031-06-15',
        null, 'g', null, null, true, true, true),
       (13, 1, '2026-04-14', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (14, 2, '2026-04-14', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (15, 3, '2026-04-14', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (16, 4, '2026-04-14', null, null, null, null, null, null, null, null, null, null, null, true, true, true),
       (17, 5, '2026-04-14', null, null, null, null, null, null, null, null, null, null, null, true, true, true) ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('property_compliance', 'id'), (SELECT MAX(id) FROM property_compliance));

-- PDJB-1048 / PDJB-1305 provide-later + compliance-banner QA (landlord 1) compliance records.
-- 18-21: fully compliant records for property_ownership 19-22 (gas not required, valid electrical +
-- EPC, all declarations) so they render the pure provide-later banner variant.
-- 22: scenario A (PO 18) with all three certs "provide later" -> COMBINED, backed by data.
-- 23: PO 24 gas cert expired, electrical + EPC valid          -> single "certificate expired" banner.
-- 24: PO 25 gas cert + EPC expired, electrical valid          -> "multiple certificates expired" banner.
-- 25: PO 26 gas cert "provide later", electrical + EPC valid  -> "add compliance certificates" (missing) banner.
INSERT INTO property_compliance (id, property_ownership_id, created_date, last_modified_date, gas_safety_cert_issue_date, has_gas_supply,
                                 electrical_safety_expiry_date, electrical_cert_type, epc_url, epc_expiry_date,
                                 tenancy_started_before_epc_expiry, epc_energy_rating, epc_exemption_reason, epc_mees_exemption_reason,
                                 has_fire_safety_declaration, has_keep_property_safe_declaration, has_responsibility_to_tenants_declaration,
                                 gas_safety_cert_provide_later, electrical_safety_cert_provide_later, epc_provide_later)
VALUES (18, 19, current_date, current_date, null, false, '2035-01-01', null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2035-01-01',
        null, 'c', null, null, true, true, true, false, false, false),
       (19, 20, current_date, current_date, null, false, '2035-01-01', null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2035-01-01',
        null, 'c', null, null, true, true, true, false, false, false),
       (20, 21, current_date, current_date, null, false, '2035-01-01', null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2035-01-01',
        null, 'c', null, null, true, true, true, false, false, false),
       (21, 22, current_date, current_date, null, false, '2035-01-01', null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', '2035-01-01',
        null, 'c', null, null, true, true, true, false, false, false),
       (22, 18, current_date, current_date, null, true, null, null, null, null,
        null, null, null, null, true, true, true, true, true, true),
       (23, 24, current_date, current_date, current_date - 730, true, current_date + 730, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', current_date + 730,
        null, 'c', null, null, true, true, true, false, false, false),
       (24, 25, current_date, current_date, current_date - 730, true, current_date + 730, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', current_date - 365,
        false, 'c', null, null, true, true, true, false, false, false),
       (25, 26, current_date, current_date, null, true, current_date + 730, null,
        'https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832', current_date + 730,
        null, 'c', null, null, true, true, true, true, false, false) ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('property_compliance', 'id'), (SELECT MAX(id) FROM property_compliance));

INSERT INTO system_operator (id, created_date, last_modified_date, subject_identifier)
VALUES (1, '2025-02-19 12:01:07.575927+00', null,
        'urn:fdc:gov.uk:2022:_RNZomOzEjxF4o2NzxWskS062b7hTVWLFI8TYsmoWAk'), -- travis.woodward@communities.gov.uk
       (2, '2025-02-26 17:02:19.625996+00', null,
        'urn:fdc:gov.uk:2022:DySqeEXIC4G2xauOirtTDcezwCPLZgQPUQZmQ-aIIMk'), -- travis.woodward@softwire.com
       (4, '2025-03-06 15:32:59.529898+00', null,
        'urn:fdc:gov.uk:2022:A9B5GpzhlOrNoGQM65oUESHL5i3O9fp0wjizEFVcCrU'), -- alexander.read@softwire.com
       (6, '2025-03-12 17:12:19.833105+00', null,
        'urn:fdc:gov.uk:2022:07lXHJeQwE0k5PZO7w_PQF425vT8T7e63MrvyPYNSoI'), -- jasmin.conterio@softwire.com
       (7, '2025-03-17 10:13:36.388805+00', null,
        'urn:fdc:gov.uk:2022:ListhqO1Hu6G90tyF_Rozj4F0YkLHreBnCQZ3JQSiEU'), -- kiran.randhawakukar@softwire.com
       (9, '2025-04-01 11:15:40.136113+00', null,
        'urn:fdc:gov.uk:2022:DXI5RSmCmbPQQhBAPCbw1nkL-Dauufg6VOWdR9TuYlk'), -- norris.orighoye@communities.gov.uk
       (10, '2025-04-02 12:52:16.995889+00', null,
        'urn:fdc:gov.uk:2022:vgKfvjYRO1LnJkmBr7CkEV62g9WoDeD-sZZNt9GCiVU'), -- sharan.flora@communities.gov.uk
       (11, '2025-04-02 13:01:55.29454+00', null,
        'urn:fdc:gov.uk:2022:pciqch9dYbtBx2rAhxvaCIEu00cQv3NFeIk5f4BesLo'), -- rowan.hill@softwire.com
       (14, '2025-04-22 10:55:55.704192+00', null,
        'urn:fdc:gov.uk:2022:Q2BSE6pweSpQF8oSBhjHAIjEuLlkRJZzJQ4TO0c7wgI'), -- sandra.lila@communities.gov.uk
       (15, '2025-05-01 12:01:07.575927+00', null,
        'urn:fdc:gov.uk:2022:GzFopg--2AyE6XtssVWwQTPELVQFupHJOjpONWS2uz0'), -- Team-PRSDB+systemoperator@softwire.com
       (16, '2025-07-08 13:58:19.927000+00', null,
        'urn:fdc:gov.uk:2022:kob7zYIuzdrUxKTYq7160l_6Tj2ScXTPJ876jZVvAFA'), -- catherine.graham2@communities.gov.uk
       (17, '2025-10-09 12:44:47.460558+00', null,
        'urn:fdc:gov.uk:2022:T0PqJH7B2o8y3t8-cCEsAk1tL8iSf-svJy-O5HvsynE'), -- chris.lightfoot@communities.gov.uk
       (18, '2025-10-23 13:07:12.755421+00', null,
        'urn:fdc:gov.uk:2022:BqdyyKzMzY6miLk0NSjJZ8j4GHtmuLgL45KisrXMxMg'), -- Ned.FrederickCalas-Hathaway@softwire.com
       (19, '2025-11-10 16:02:36.605000+00', null,
        'urn:fdc:gov.uk:2022:nzYcgBUq3Exgd00RvATgx6_nIUpEq5vO0mMeeNGoLI8'), -- shannon.okyemba-tsambou@communities.gov.uk
       (20, '2025-11-10 16:03:02.159000+00', null,
        'urn:fdc:gov.uk:2022:zLxuwilkLOLLpD3tTmOcG_lE8BNj0NFyqjU17lzn6cI'), -- rebecca.coll@communities.gov.uk
       (21, '2025-11-10 16:10:00.000000+00', null,
        'urn:fdc:gov.uk:2022:mCqrvLgjky23tcKQNo4C4GjDn13sZNcVhdhfqqvimTc'), --Lewis.Jones@communities.gov.uk
       (22, '2025-12-01 10:33:22.395944+00', null,
        'urn:fdc:gov.uk:2022:V7SiTu5znvhYuTqkLgN0cOzaGrzkKpGBnrWj8BRQ34Y'), -- Adam.Jennings@softwire.com
       (23, '2026-07-01 10:33:22.395944+00', null,
        'urn:fdc:gov.uk:2022:ErdvdxjqbulqrJI9hDob1vE0BQ_BqVXlv-mWZwgBJgA'), -- danielle.dias@madetech.com
       (24, '2026-07-02 10:00:00+00', null,
        'urn:fdc:gov.uk:2022:qw2_iN4-Be1BkbYb8y-KyMuPfG7F49W_1fsa_V6iX9w'),  -- benjamin.johnson@madetech.com
       (25, '2026-07-21 00:00:00+00', null,
        'urn:fdc:gov.uk:2022:wkm6PXI5bMeyS-iMYW9FoBp86EdijRr6usu6qTaA3Cg'), -- Andrew Wilkins
       (26, '2026-07-21 00:00:00+00', null,
        'urn:fdc:gov.uk:2022:r4pLmSY3oYBWPaJVhw8U_wgdfXp3e7cuLSjctVWEJWo'), -- William Gledhill
       (27, '2026-07-21 00:00:00+00', null,
        'urn:fdc:gov.uk:2022:Flwl0DlDFArbsXtBSpBeVNLJ_OhhmnGl7s4Lo-qg3QI'), -- Sean Hennity
       (28, '2026-07-21 00:00:00+00', null,
        'urn:fdc:gov.uk:2022:aOcUnYIpNfDHDfbexwINrPodEJLV5Fh451VDFOM5h_o'), -- Denis Fazlji
       (29, '2026-07-21 00:00:00+00', null,
        'urn:fdc:gov.uk:2022:kMCObIbxtdFvloXjqQUnFISalxA4bct23eJJxe3QDmI'), -- Andreea Popescu
       (30, '2026-07-21 00:00:00+00', null,
        'urn:fdc:gov.uk:2022:3vvs6mvUviJ6xMVqLKL2rS0BIrlszvGh4nRMdX3IFb8'), -- Yvonne Andrews
       (31, '2026-07-21 00:00:00+00', null,
        'urn:fdc:gov.uk:2022:s-DPDuNmTwvQsptmEwWMTkPMiO2MmDRrE8HF7AcDmZ8'), -- Mobin Ibrahim Patel
       (32, '2026-07-21 00:00:00+00', null,
        'urn:fdc:gov.uk:2022:Xj9nvDG2yHvw53ZsIBKJH_U2UNfMK7nq2iel4cxc6Ow'),  -- Sarah Warren
       (33, '2026-08-13 00:00:00+00', null,
        'urn:fdc:gov.uk:2022:9AAN88nNjxaMnuEL0mN45HoEITVd_aMBsrG05AFcdzY'), -- Aimie Robinson
       (34, '2026-08-25 00:00:00+00', null,
        'urn:fdc:gov.uk:2022:HWihy8O1bH7nvqzL8zTP1RYQrPU3CxK6g6vYQvZ6tm4'), -- Katrina DiMuro
       (35, '2026-09-09 00:00:00+00', null,
        'urn:fdc:gov.uk:2022:lGpMoDhAAg0pe72lmsNzg_oFLH1qna1wyya5nSV-F2E')  -- Victoria Greenwell
    ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('system_operator', 'id'), (SELECT MAX(id) FROM system_operator));

INSERT INTO passcode (passcode, created_date, last_modified_date, subject_identifier)
VALUES ('PRSD22', current_date, null,
        'urn:fdc:gov.uk:2022:mGHDySEVfCsvfvc6lVWf6Qt9Dv0ZxPQWKoEzcjnBlUo'),                               -- Team-PRSDB+landlord@softwire.com
       ('PRSD23', current_date, null,
        'urn:fdc:gov.uk:2022:_RNZomOzEjxF4o2NzxWskS062b7hTVWLFI8TYsmoWAk'),                               -- travis.woodward@communities.gov.uk
       ('PRSD24', current_date, null, 'urn:fdc:gov.uk:2022:A9B5GpzhlOrNoGQM65oUESHL5i3O9fp0wjizEFVcCrU'), -- alexander.read@softwire.com
       ('PRSD25', current_date, null,
        'urn:fdc:gov.uk:2022:ListhqO1Hu6G90tyF_Rozj4F0YkLHreBnCQZ3JQSiEU'),                               -- kiran.randhawakukar@softwire.com
       ('PRSD26', current_date, null, 'urn:fdc:gov.uk:2022:07lXHJeQwE0k5PZO7w_PQF425vT8T7e63MrvyPYNSoI'), -- jasmin.conterio@softwire.com
       ('PRSD27', current_date, null,
        'urn:fdc:gov.uk:2022:sgO5-g7fThIp2MhXMcvFo5N6ObnstGFVNSYFkghMd24'),                               -- Team-PRSDB+Unverified@softwire.com
       ('PRSD29', current_date, null,
        'urn:fdc:gov.uk:2022:La9gwI6zvuzT3yvKjsKEH2cDbtL88wNbiqAeXQ0plEM'),                               -- team-prsdb+verified@softwire.com
       ('PRSD34', current_date, null,
        'urn:fdc:gov.uk:2022:ea8XwChQkjezm4MgGJIzI_HRm7l8IPPTIMT705UQXjI'),                               -- geetika.kejriwal@communities.gov.uk
       ('PRSD35', current_date, null,
        'urn:fdc:gov.uk:2022:kob7zYIuzdrUxKTYq7160l_6Tj2ScXTPJ876jZVvAFA'),                               -- catherine.graham2@communities.gov.uk
       ('PRSD37', current_date, null,
        'urn:fdc:gov.uk:2022:DXI5RSmCmbPQQhBAPCbw1nkL-Dauufg6VOWdR9TuYlk'),                               -- norris.orighoye@communities.gov.uk
       ('PRSD39', current_date, null, 'urn:fdc:gov.uk:2022:vgKfvjYRO1LnJkmBr7CkEV62g9WoDeD-sZZNt9GCiVU'), -- sharan.flora@communities.gov.uk
       ('PRSD42', current_date, null, 'urn:fdc:gov.uk:2022:pciqch9dYbtBx2rAhxvaCIEu00cQv3NFeIk5f4BesLo'), -- rowan.hill@softwire.com
       ('PRSD52', current_date, null, 'urn:fdc:gov.uk:2022:Q2BSE6pweSpQF8oSBhjHAIjEuLlkRJZzJQ4TO0c7wgI'), -- sandra.lila@communities.gov.uk
       ('PRSD53', current_date, null,
        'urn:fdc:gov.uk:2022:T0PqJH7B2o8y3t8-cCEsAk1tL8iSf-svJy-O5HvsynE'),                               -- chris.lightfoot@communities.gov.uk
       ('PRSD54', current_date, null,
        'urn:fdc:gov.uk:2022:BqdyyKzMzY6miLk0NSjJZ8j4GHtmuLgL45KisrXMxMg'),                               -- Ned.FrederickCalas-Hathaway@softwire.com
       ('PRSD55', current_date, null, 'urn:fdc:gov.uk:2022:po6yDD8EFb0c0UfVVoEZHKQyN_mvBG81mcZPz1r83Ss'), -- Dani
       ('PRSD56', current_date, null, 'urn:fdc:gov.uk:2022:nzYcgBUq3Exgd00RvATgx6_nIUpEq5vO0mMeeNGoLI8'), -- Shannon
       ('PRSD57', current_date, null, 'urn:fdc:gov.uk:2022:zLxuwilkLOLLpD3tTmOcG_lE8BNj0NFyqjU17lzn6cI'), -- Rebecca
       ('PRSD58', current_date, null, 'urn:fdc:gov.uk:2022:mCqrvLgjky23tcKQNo4C4GjDn13sZNcVhdhfqqvimTc'), -- Lewis
       ('PRSD59', current_date, null, 'urn:fdc:gov.uk:2022:V7SiTu5znvhYuTqkLgN0cOzaGrzkKpGBnrWj8BRQ34Y'),  -- Adam
       ('PRSD60', current_date, null, 'urn:fdc:gov.uk:2022:9AAN88nNjxaMnuEL0mN45HoEITVd_aMBsrG05AFcdzY') -- Aimie
    ON CONFLICT DO NOTHING;
