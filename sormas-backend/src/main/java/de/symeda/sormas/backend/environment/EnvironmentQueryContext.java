package de.symeda.sormas.backend.environment;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;

import de.symeda.sormas.backend.common.QueryContext;

public class EnvironmentQueryContext extends QueryContext<Environment, EnvironmentJoins> {

	public EnvironmentQueryContext(CriteriaBuilder cb, CriteriaQuery<?> query, From<?, Environment> root) {
		super(cb, query, root, new EnvironmentJoins(root));
	}

	public EnvironmentQueryContext(CriteriaBuilder cb, CriteriaQuery<?> query, EnvironmentJoins joins) {
		super(cb, query, joins.getRoot(), joins);
	}

	@Override
	protected Expression<?> createExpression(String name) {
		return null;
	}
}
