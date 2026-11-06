package de.symeda.sormas.backend.user;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;

import de.symeda.sormas.backend.common.QueryContext;

public class UserQueryContext extends QueryContext<User, UserJoins> {

	protected UserQueryContext(CriteriaBuilder cb, CriteriaQuery<?> query, From<?, User> root) {
		super(cb, query, root, new UserJoins(root));
	}

	@Override
	protected Expression<?> createExpression(String name) {
		return null;
	}

}
