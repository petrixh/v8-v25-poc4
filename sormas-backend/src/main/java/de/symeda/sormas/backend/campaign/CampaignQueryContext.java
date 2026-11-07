package de.symeda.sormas.backend.campaign;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;

import de.symeda.sormas.backend.common.QueryContext;

public class CampaignQueryContext extends QueryContext<Campaign, CampaignJoins> {

	protected CampaignQueryContext(CriteriaBuilder cb, CriteriaQuery<?> query, From<?, Campaign> root) {
		this(cb, query, new CampaignJoins(root));
	}

	public CampaignQueryContext(CriteriaBuilder cb, CriteriaQuery<?> query, CampaignJoins joins) {
		super(cb, query, joins.getRoot(), joins);
	}

	@Override
	protected Expression<?> createExpression(String name) {
		return null;
	}
}
