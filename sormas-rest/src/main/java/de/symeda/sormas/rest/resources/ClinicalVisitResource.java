/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2023 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.rest.resources;

import java.util.Date;
import java.util.List;
import java.util.function.UnaryOperator;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CriteriaWithSorting;
import de.symeda.sormas.api.clinicalcourse.ClinicalVisitCriteria;
import de.symeda.sormas.api.clinicalcourse.ClinicalVisitDto;
import de.symeda.sormas.api.clinicalcourse.ClinicalVisitIndexDto;
import de.symeda.sormas.api.common.Page;
import de.symeda.sormas.rest.resources.base.EntityDtoResource;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

@Path("/clinicalvisits")
@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
@Consumes(MediaType.APPLICATION_JSON + "; charset=UTF-8")
public class ClinicalVisitResource extends EntityDtoResource<ClinicalVisitDto> {

	@GET
	@Path("/all/{since}")
	public List<ClinicalVisitDto> getAllVisits(@PathParam("since") long since) {
		return FacadeProvider.getClinicalVisitFacade().getAllActiveClinicalVisitsAfter(new Date(since));
	}

	@GET
	@Path("/all/{since}/{size}/{lastSynchronizedUuid}")
	public List<ClinicalVisitDto> getAllVisits(
		@PathParam("since") long since,
		@PathParam("size") int size,
		@PathParam("lastSynchronizedUuid") String lastSynchronizedUuid) {
		return FacadeProvider.getClinicalVisitFacade().getAllActiveClinicalVisitsAfter(new Date(since), size, lastSynchronizedUuid);
	}

	@POST
	@Path("/query")
	public List<ClinicalVisitDto> getByUuids(List<String> uuids) {
		return FacadeProvider.getClinicalVisitFacade().getByUuids(uuids);
	}

	@GET
	@Path("/uuids")
	public List<String> getAllActiveUuids() {
		return FacadeProvider.getClinicalVisitFacade().getAllActiveUuids();
	}

	@POST
	@Path("/indexList")
	public Page<ClinicalVisitIndexDto> getIndexList(
		@RequestBody CriteriaWithSorting<ClinicalVisitCriteria> criteriaWithSorting,
		@QueryParam("offset") int offset,
		@QueryParam("size") int size) {
		return FacadeProvider.getClinicalVisitFacade()
			.getIndexPage(criteriaWithSorting.getCriteria(), offset, size, criteriaWithSorting.getSortProperties());
	}

	@Override
	public UnaryOperator<ClinicalVisitDto> getSave() {
		return FacadeProvider.getClinicalVisitFacade()::saveClinicalVisit;
	}

	@Override
	public Response postEntityDtos(List<ClinicalVisitDto> clinicalVisitDtos) {
		return super.postEntityDtos(clinicalVisitDtos);
	}
}
