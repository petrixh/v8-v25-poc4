package de.symeda.sormas.backend.common;

import java.io.Serializable;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;

@FunctionalInterface
public interface FilterProvider<ADO extends AbstractDomainObject> extends Serializable {

	@SuppressWarnings("rawtypes")
	Predicate provide(CriteriaBuilder cb, CriteriaQuery cq, From<?, ADO> from);
}
