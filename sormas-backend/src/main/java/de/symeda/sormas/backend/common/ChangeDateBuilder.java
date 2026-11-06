package de.symeda.sormas.backend.common;

import jakarta.persistence.criteria.From;

public interface ChangeDateBuilder<T extends ChangeDateBuilder> {

	<C> T add(From<?, C> path, String... joinFields);
}
