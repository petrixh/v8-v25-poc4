-- env/seed/campaign-form.sql: one synthetic campaign form definition, linked to the seed campaign (C2 seed additions,
-- run by env/seed/extend.sh on sormas_db while the app is stopped).
-- SORMAS has no UI or REST endpoint that creates campaign form definitions (CampaignFormMetaFacade.saveCampaignFormMeta
-- has no caller in sormas-ui or sormas-rest); deployments load them into the campaignformmeta table. The form uses only
-- the element types of CampaignFormElementType (section, label, number, yes-no, text) and one translation for the
-- German UI (language code as Locale.toString(), CampaignFormBuilder). The link to the campaign is the row the campaign
-- form's "Campaign data forms" grid would add; the campaign's own row is not changed. Fixed UUID SCFM01.
-- Additions only: fails when the form already exists or the seed campaign is not there exactly once.
DO $$
DECLARE
  form_uuid constant varchar := 'SCFM01-SEED00-SEED00-SEED00';
  campaign bigint;
  form bigint;
BEGIN
  IF EXISTS (SELECT 1 FROM campaignformmeta WHERE uuid = form_uuid) THEN
    RAISE EXCEPTION 'campaign form %: already there', form_uuid;
  END IF;
  SELECT id INTO STRICT campaign FROM campaigns WHERE name = 'Seed campaign: vaccination drive' AND NOT deleted;
  INSERT INTO campaignformmeta (id, uuid, changedate, creationdate, formid, formname, languagecode, campaignformelements,
                                campaignformtranslations, sys_period)
  VALUES (nextval('entity_seq'), form_uuid, now(), now(), 'seed-vaccination-round', 'Seed form: vaccination round', 'en',
    '[
      {"type": "section", "id": "roundSection"},
      {"type": "label", "id": "roundLabel", "caption": "<h3>Vaccination round</h3>"},
      {"type": "number", "id": "childrenVaccinated", "caption": "Number of children vaccinated", "important": true},
      {"type": "number", "id": "childrenMissed", "caption": "Number of children missed"},
      {"type": "section", "id": "stockSection"},
      {"type": "yes-no", "id": "stockSufficient", "caption": "Vaccine stock sufficient?"},
      {"type": "text", "id": "remarks", "caption": "Remarks", "dependingOn": "stockSufficient", "dependingOnValues": ["no"]}
    ]'::json,
    '[
      {"languageCode": "de_DE", "translations": [
        {"elementId": "roundLabel", "caption": "<h3>Impfrunde</h3>"},
        {"elementId": "childrenVaccinated", "caption": "Anzahl geimpfter Kinder"},
        {"elementId": "childrenMissed", "caption": "Anzahl nicht erreichter Kinder"},
        {"elementId": "stockSufficient", "caption": "Impfstoffvorrat ausreichend?"},
        {"elementId": "remarks", "caption": "Bemerkungen"}
      ]}
    ]'::json,
    tstzrange(now(), NULL))
  RETURNING id INTO form;
  INSERT INTO campaign_campaignformmeta (campaign_id, campaignformmeta_id, sys_period) VALUES (campaign, form, tstzrange(now(), NULL));
END $$;
