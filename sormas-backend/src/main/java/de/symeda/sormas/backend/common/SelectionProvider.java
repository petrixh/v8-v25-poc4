package de.symeda.sormas.backend.common;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Selection;

@FunctionalInterface
public interface SelectionProvider<ADO extends AbstractDomainObject> extends Serializable {

	@SuppressWarnings("rawtypes")
	List<Selection<?>> provide(CriteriaBuilder cb, CriteriaQuery cq, From<?, ADO> from);
}
