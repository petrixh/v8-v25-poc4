-- env/config/C2.sql: feature switches of test configuration C2, run by env/up.sh on sormas_db while the app is stopped.
-- Server-level rows of the featureconfiguration table (no region, district or disease); the app creates them at its first
-- start and reads them at start. docs/SERVER_CUSTOMIZATION.md: this table is where SORMAS features are switched.
-- On: campaigns, AEFI, environments, surveys, self reports, the GDPR consent popup at login, travel entries; case follow-up
-- (the case visits view), the area infrastructure type, external e-mails (the e-mail templates view) and documents (the
-- documents side component of cases, contacts, events and travel entries).
-- Off: external messages and SORMAS-to-SORMAS (they need outside systems). Idempotent: unchanged rows are not touched.
CREATE TEMP TABLE c2_switch(featuretype varchar, enabled boolean);
INSERT INTO c2_switch VALUES
  ('CAMPAIGNS', true), ('ADVERSE_EVENTS_FOLLOWING_IMMUNIZATION_MANAGEMENT', true), ('ENVIRONMENT_MANAGEMENT', true),
  ('SURVEYS', true), ('SELF_REPORTING', true), ('GDPR_CONSENT_POPUP', true), ('TRAVEL_ENTRIES', true),
  ('CASE_FOLLOWUP', true), ('INFRASTRUCTURE_TYPE_AREA', true), ('EXTERNAL_EMAILS', true), ('DOCUMENTS', true),
  ('EXTERNAL_MESSAGES', false), ('SORMAS_TO_SORMAS_ACCEPT_REJECT', false);
UPDATE featureconfiguration f SET enabled = s.enabled, changedate = now()
  FROM c2_switch s
  WHERE f.featuretype = s.featuretype AND f.region_id IS NULL AND f.district_id IS NULL AND f.disease IS NULL
    AND f.enabled IS DISTINCT FROM s.enabled;
-- Each switch must exist once and hold its value: the app ignores a feature without a row.
DO $$ BEGIN
  IF (SELECT count(*) FROM featureconfiguration f JOIN c2_switch s ON f.featuretype = s.featuretype AND f.enabled = s.enabled
      WHERE f.region_id IS NULL AND f.district_id IS NULL AND f.disease IS NULL) <> (SELECT count(*) FROM c2_switch) THEN
    RAISE EXCEPTION 'C2 feature switches: a server feature row is missing or does not hold its value';
  END IF;
END $$;
