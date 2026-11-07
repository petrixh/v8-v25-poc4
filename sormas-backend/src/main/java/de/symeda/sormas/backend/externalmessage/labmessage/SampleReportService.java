/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2022 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.backend.externalmessage.labmessage;

import java.util.Optional;

import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import de.symeda.sormas.backend.common.BaseAdoService;
import de.symeda.sormas.backend.common.DeletableAdo;

@Stateless
@LocalBean
public class SampleReportService extends BaseAdoService<SampleReport> {

	@EJB
	private TestReportService testReportService;

	public SampleReportService() {
		super(SampleReport.class);
	}

	/**
	 * Creates a default filter that should be used as the basis of queries in this service.
	 * This essentially removes {@link DeletableAdo#deleted} test reports from the queries.
	 */
	public Predicate createDefaultFilter(CriteriaBuilder cb, Root<TestReport> root) {
		return cb.isFalse(root.join(SampleReport.LAB_MESSAGE, JoinType.LEFT).get(DeletableAdo.DELETED));
	}

	@Override
	public boolean deletePermanent(SampleReport sampleReport) {

		Optional.ofNullable(sampleReport.getTestReports()).ifPresent(testReports -> testReports.forEach(t -> testReportService.deletePermanent(t)));

		super.deletePermanent(sampleReport);
		return false;
	}
}
