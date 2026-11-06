/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.backend.hospitalization;

import java.util.List;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import de.symeda.sormas.backend.caze.Case;
import de.symeda.sormas.backend.common.BaseAdoService;

@Stateless
@LocalBean
public class PreviousHospitalizationService extends BaseAdoService<PreviousHospitalization> {

	public PreviousHospitalizationService() {
		super(PreviousHospitalization.class);
	}

	public List<Object[]> getFirstPreviousHospitalizations(List<Long> caseIds) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
		Root<PreviousHospitalization> prevHospRoot = cq.from(getElementClass());
		Join<PreviousHospitalization, Hospitalization> hospitalizationJoin =
			prevHospRoot.join(PreviousHospitalization.HOSPITALIZATION, JoinType.LEFT);
		Root<Case> caseRoot = cq.from(Case.class);
		Join<Case, Hospitalization> caseHospitalizationJoin = caseRoot.join(Case.HOSPITALIZATION, JoinType.LEFT);

		cq.multiselect(caseRoot.get(Case.ID), prevHospRoot.get(PreviousHospitalization.ID));

		Expression<String> caseIdsExpression = caseRoot.get(Case.ID);
		cq.where(
			cb.and(
				caseIdsExpression.in(caseIds),
				cb.equal(hospitalizationJoin.get(Hospitalization.ID), caseHospitalizationJoin.get(Hospitalization.ID))));
		cq.orderBy(cb.asc(caseRoot.get(Case.ID)), cb.asc(prevHospRoot.get(PreviousHospitalization.ADMISSION_DATE)));

		return em.createQuery(cq).getResultList();
	}
}
