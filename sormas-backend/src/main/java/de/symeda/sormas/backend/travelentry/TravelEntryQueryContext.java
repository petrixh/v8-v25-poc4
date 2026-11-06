package de.symeda.sormas.backend.travelentry;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;

import de.symeda.sormas.backend.common.QueryContext;

public class TravelEntryQueryContext extends QueryContext<TravelEntry, TravelEntryJoins> {

	public TravelEntryQueryContext(CriteriaBuilder cb, CriteriaQuery<?> query, From<?, TravelEntry> root) {
		this(cb, query, new TravelEntryJoins(root));
	}

	public TravelEntryQueryContext(CriteriaBuilder cb, CriteriaQuery<?> query, TravelEntryJoins joins) {
		super(cb, query, joins.getRoot(), joins);
	}

	@Override
	protected Expression<?> createExpression(String name) {
		return null;
	}
}
