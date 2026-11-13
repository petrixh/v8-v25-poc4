-- Pre-go-live data checks for the Vaadin 25 migration of SORMAS (docs/migration/handoff.md, section 4).
-- Read-only: both queries only count rows. Run them on the production database, or a copy of it, before the
-- new release goes live (schema version 649, unchanged by the migration), for example:
--   psql -U sormas_user -d sormas_db -f pre-go-live-checks.sql

-- Check 1 (section 4.2): rich text that loses formatting when it is edited and saved in the new application.
-- The new rich text editor removes on save: <font> tags (font face, size, colour), tables (the text stays),
-- horizontal rules, headings H4 to H6, images loaded from an address (embedded data: images stay),
-- javascript: links, scripts and event attributes (onclick=... and the like). Texts are unchanged until
-- someone edits and saves them. affected_texts above 0: users will see that formatting disappear the next
-- time they save such a text.
WITH texts (field, html) AS (
    SELECT 'action.description', description FROM action
    UNION ALL
    SELECT 'action.reply', reply FROM action
    UNION ALL
    SELECT 'diseaseconfiguration.casedefinitiontext', casedefinitiontext
        FROM diseaseconfiguration
)
SELECT field,
    count(*) FILTER (WHERE html ~* '<font\y') AS font_tags,
    count(*) FILTER (WHERE html ~* '<(table|hr)\y') AS tables_or_rules,
    count(*) FILTER (WHERE html ~* '<h[4-6]\y') AS headings_h4_to_h6,
    count(*) FILTER (WHERE html ~* '<img\y[^>]*\ysrc\s*=\s*(["'']\s*)?(?![\s"'']|data:)')
        AS images_by_address,
    count(*) FILTER (WHERE html ~* 'javascript:|<script\y|<[a-z][^>]*\son[a-z]+\s*=')
        AS scripts_or_event_attributes,
    count(*) FILTER (WHERE html ~* ('<font\y|<(table|hr)\y|<h[4-6]\y'
        || '|<img\y[^>]*\ysrc\s*=\s*(["'']\s*)?(?![\s"'']|data:)'
        || '|javascript:|<script\y|<[a-z][^>]*\son[a-z]+\s*=')) AS affected_texts,
    count(*) AS texts_checked
FROM texts
WHERE html IS NOT NULL AND html <> ''
GROUP BY field
ORDER BY field;

-- Check 2 (section 4.3): records with an empty link in the six columns the new persistence layer
-- (Hibernate 7) joins with an inner join. A record with NULL in one of them no longer opens in the new
-- application. Expected: 0 in every row. Where a row shows more than 0, fix the data or ask for the
-- mapping of that column to be relaxed before go-live.
SELECT 'environments.reportinguser_id' AS column_checked, count(*) AS empty_rows
    FROM environments WHERE reportinguser_id IS NULL
UNION ALL SELECT 'selfreports.address_id', count(*)
    FROM selfreports WHERE address_id IS NULL
UNION ALL SELECT 'eventparticipant.person_id', count(*)
    FROM eventparticipant WHERE person_id IS NULL
UNION ALL SELECT 'campaigns.creatinguser_id', count(*)
    FROM campaigns WHERE creatinguser_id IS NULL
UNION ALL SELECT 'sormastosormassharerequest.origininfo_id', count(*)
    FROM sormastosormassharerequest WHERE origininfo_id IS NULL
UNION ALL SELECT 'sharerequestinfo.sender_id', count(*)
    FROM sharerequestinfo WHERE sender_id IS NULL;
