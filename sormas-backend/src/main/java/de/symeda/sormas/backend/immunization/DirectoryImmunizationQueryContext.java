package de.symeda.sormas.backend.immunization;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;

import de.symeda.sormas.backend.common.QueryContext;
import de.symeda.sormas.backend.immunization.entity.DirectoryImmunization;

public class DirectoryImmunizationQueryContext extends QueryContext<DirectoryImmunization, DirectoryImmunizationJoins> {

	protected DirectoryImmunizationQueryContext(CriteriaBuilder cb, CriteriaQuery<?> query, From<?, DirectoryImmunization> root) {
		super(cb, query, root, new DirectoryImmunizationJoins(root));
	}

	@Override
	protected Expression<?> createExpression(String name) {
		return null;
	}
}
