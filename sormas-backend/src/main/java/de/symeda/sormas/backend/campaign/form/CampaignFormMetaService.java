package de.symeda.sormas.backend.campaign.form;

import java.util.List;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import de.symeda.sormas.api.campaign.form.CampaignFormMetaReferenceDto;
import de.symeda.sormas.backend.campaign.Campaign;
import de.symeda.sormas.backend.common.AdoServiceWithUserFilterAndJurisdiction;

@Stateless
@LocalBean
public class CampaignFormMetaService extends AdoServiceWithUserFilterAndJurisdiction<CampaignFormMeta> {

	public CampaignFormMetaService() {
		super(CampaignFormMeta.class);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Predicate createUserFilter(CriteriaBuilder cb, CriteriaQuery cq, From<?, CampaignFormMeta> from) {
		return null;
	}

	public List<CampaignFormMetaReferenceDto> getCampaignFormMetasAsReferencesByCampaign(String uuid) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<CampaignFormMetaReferenceDto> cq = cb.createQuery(CampaignFormMetaReferenceDto.class);
		Root<Campaign> campaignRoot = cq.from(Campaign.class);
		Join<Campaign, CampaignFormMeta> campaignFormMetaJoin = campaignRoot.join(Campaign.CAMPAIGN_FORM_METAS);
		Predicate filter = cb.equal(campaignRoot.get(Campaign.UUID), uuid);
		cq = cq.where(filter);
		cq.multiselect(campaignFormMetaJoin.get(CampaignFormMeta.UUID), campaignFormMetaJoin.get(CampaignFormMeta.FORM_NAME));
		return em.createQuery(cq).getResultList();
	}
}
