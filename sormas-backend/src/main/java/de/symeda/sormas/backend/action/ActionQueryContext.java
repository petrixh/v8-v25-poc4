package de.symeda.sormas.backend.action;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;

import de.symeda.sormas.backend.common.QueryContext;

public class ActionQueryContext extends QueryContext<Action, ActionJoins> {

	protected ActionQueryContext(CriteriaBuilder cb, CriteriaQuery<?> query, From<?, Action> root) {
		this(cb, query, new ActionJoins(root));
	}

	public ActionQueryContext(CriteriaBuilder cb, CriteriaQuery<?> query, ActionJoins joins) {
		super(cb, query, joins.getRoot(), joins);
	}

	@Override
	protected Expression<?> createExpression(String name) {
		return null;
	}
}
