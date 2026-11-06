package de.symeda.sormas.backend.caze.porthealthinfo;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import de.symeda.sormas.backend.caze.Case;
import de.symeda.sormas.backend.common.AdoServiceWithUserFilterAndJurisdiction;
import de.symeda.sormas.backend.util.ModelConstants;
import de.symeda.sormas.backend.util.QueryHelper;

@Stateless
@LocalBean
public class PortHealthInfoService extends AdoServiceWithUserFilterAndJurisdiction<PortHealthInfo> {

	@PersistenceContext(unitName = ModelConstants.PERSISTENCE_UNIT_NAME)
	private EntityManager em;

	public PortHealthInfoService() {
		super(PortHealthInfo.class);
	}

	public PortHealthInfo getByCaseUuid(String caseUuid) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<PortHealthInfo> cq = cb.createQuery(PortHealthInfo.class);
		Root<Case> root = cq.from(Case.class);
		Join<Case, PortHealthInfo> portHealthJoin = root.join(Case.PORT_HEALTH_INFO, JoinType.LEFT);

		cq.select(portHealthJoin);
		cq.where(cb.equal(root.get(Case.UUID), caseUuid));
		return QueryHelper.getSingleResult(em, cq);
	}

	@Override
	public Predicate createUserFilter(CriteriaBuilder cb, CriteriaQuery cq, From<?, PortHealthInfo> from) {
		return null;
	}
}
