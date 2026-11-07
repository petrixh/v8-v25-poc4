package de.symeda.sormas.backend.visit;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;

import de.symeda.sormas.backend.common.QueryContext;

public class VisitQueryContext extends QueryContext<Visit, VisitJoins> {

	public VisitQueryContext(CriteriaBuilder cb, CriteriaQuery<?> query, From<?, Visit> root) {
		this(cb, query, new VisitJoins(root, JoinType.LEFT));
	}

	public VisitQueryContext(CriteriaBuilder cb, CriteriaQuery<?> query, VisitJoins joins) {
		super(cb, query, joins.getRoot(), joins);
	}

	@Override
	protected Expression<?> createExpression(String name) {
		return null;
	}
}
