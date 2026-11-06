package de.symeda.sormas.backend.event;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;

import de.symeda.sormas.backend.common.QueryContext;

public class EventQueryContext extends QueryContext<Event, EventJoins> {

	public EventQueryContext(CriteriaBuilder cb, CriteriaQuery<?> query, From<?, Event> root) {
		this(cb, query, new EventJoins(root));
	}

	public EventQueryContext(CriteriaBuilder cb, CriteriaQuery<?> query, EventJoins eventJoins) {
		super(cb, query, eventJoins.getRoot(), eventJoins);
	}

	@Override
	protected Expression<?> createExpression(String name) {
		return null;
	}
}
