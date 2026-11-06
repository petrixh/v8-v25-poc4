package de.symeda.sormas.backend.campaign.diagram;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.NotNull;

import de.symeda.sormas.backend.common.AdoServiceWithUserFilterAndJurisdiction;

@Stateless
@LocalBean
public class CampaignDiagramDefinitionService extends AdoServiceWithUserFilterAndJurisdiction<CampaignDiagramDefinition> {

	public CampaignDiagramDefinitionService() {
		super(CampaignDiagramDefinition.class);
	}

	@Override
	public Predicate createUserFilter(CriteriaBuilder cb, CriteriaQuery cq, From<?, CampaignDiagramDefinition> from) {
		return null;
	}

	public boolean diagramExists(@NotNull String diagramId) {
		return exists((cb, root, cq) -> cb.equal(root.get(CampaignDiagramDefinition.DIAGRAM_ID), diagramId));
	}

	public CampaignDiagramDefinition getByDiagramId(@NotNull String diagramId) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		ParameterExpression<String> param = cb.parameter(String.class, CampaignDiagramDefinition.DIAGRAM_ID);
		CriteriaQuery<CampaignDiagramDefinition> cq = cb.createQuery(getElementClass());
		Root<CampaignDiagramDefinition> from = cq.from(getElementClass());
		cq.where(cb.equal(from.get(CampaignDiagramDefinition.DIAGRAM_ID), param));
		TypedQuery<CampaignDiagramDefinition> q = em.createQuery(cq).setParameter(param, diagramId);
		CampaignDiagramDefinition entity = q.getResultList().stream().findFirst().orElse(null);
		return entity;
	}
}
